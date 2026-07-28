package za.co.ntier.learner.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZLearnerLearnership;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt"): migrates the staged
 * ms_learnerlearnership table (73,930 rows) into ZZLearnerLearnership. Extremely close shape to
 * {@link MigrateMsLearnerQctoLearnershipToZZLearnerQctoLearnership} - most of this class mirrors
 * that one directly, adjusted for the differences documented below.
 *
 * <p>Must run AFTER: MigrateMsPersonToZZPerson/MigrateMsLearnerToZZLearner (ms_learner_xref),
 * a learnership catalog migration (zzlearnership must have "id" populated),
 * MigrateMsGrantTypeToZZGrantType (ZZGrantType_ID will resolve empty until then, still added).
 *
 * <p>Builds a new C_Location per row from physicaladdress1/2/3/physicalcode/
 * physicalprovinceid/physicalcityid (same MigrationSupport.createLocation() helper used
 * throughout this project) for ZZPhysicalLocation_ID.
 *
 * <p>DIFFERENCES from the QCTOLearnership sibling:
 * <ul>
 *   <li>ZZLearnerLearnershipType resolves via ms_lkplearnerqctolearnershiptype (List 1000257) -
 *       confirmed by comparing actual List values ("Beneficiation Learnership"/"Learnership"/
 *       "Non MQA Learnership"/"RPL") against ms_lkplearnershiptype's completely different values
 *       ("Qualification Electives" etc) - the latter is NOT the right source despite the more
 *       obvious name match, see the mapping doc's Section 1 for the full reasoning.</li>
 *   <li>ZZTerminationReason has NO separate "...Text" column on this table (unlike
 *       QCTOLearnership) - only the List reference exists, populated the same way (matched by
 *       description against AD_Ref_List, which happens to equal Name for all 8 entries).</li>
 *   <li>No Employer_ID/qualification-requirements/NAMB-confirmation columns on this table (those
 *       are QCTO-specific) - Employer_ID lives on the Employer CHILD table instead.</li>
 *   <li>ZZIsRPL/ZZIsEndorsed (duplicate-looking columns alongside ZZRPL/ZZEndorsed) - per user
 *       decision, BOTH members of each pair get the same source value.</li>
 *   <li>ZZWPAgreement/ZZEmpContract/ZZEmpContractCopy use yesNoIdToFlag() (CORRECTED after
 *       finding this exact precedent on the sibling - see AddZZLearnerLearnershipColumns'
 *       Javadoc for the full correction note).</li>
 *   <li>ZZIsTermsEmployment uses the NEW 3-state "Yes_No_Not_Applicable" List reference
 *       (find-or-created by AddZZLearnerLearnershipColumns/AddZZLearnerLearnershipEmployerTable)
 *       via buildListValueCrosswalk, rather than yesNoIdToFlag - a deliberate, approved
 *       divergence from the sibling's simpler binary treatment (see mapping doc Question 2).</li>
 * </ul>
 *
 * <p>Still NOT handled (same "Unmapped"/"Deferred" reasoning as the mapping doc):
 * <ul>
 *   <li>bi_registrationdate / bi_approvaldate, responsibleseta / curregnumber - look like
 *       redundant denormalised duplicates, skipped.</li>
 *   <li>previouslearnershipcode / previouslearnershiptitle - redundant text mirrors of
 *       previouslearnership (the real FK, which IS resolved).</li>
 *   <li>physicalmunicipalityid / physicalurbanruralid / physicalsuburbid - C_Location has no
 *       matching fields.</li>
 *   <li>accountnumber - the MSSQL row's own UUID, not business data.</li>
 *   <li>setalearnershiptypeid / agentid / asspartner / regsaqa - genuinely unclear, no lookup
 *       table found, deferred per user decision.</li>
 *   <li>ZZ_FinYear_ID (financialyearid) - FinancialYear-to-C_Year crosswalk not yet designed -
 *       same universal open item as every other Migrate* process in this project.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerLearnershipToZZLearnerLearnership")
public class MigrateMsLearnerLearnershipToZZLearnerLearnership extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

    private int countryId;
    private Map<Integer, Integer> provinceCrosswalk;
    private Map<Integer, Integer> cityCrosswalk;

    private final List<String> errors = new ArrayList<>();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        int personMigrated = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM ms_learner_xref");
        if (personMigrated <= 0) {
            addLog("WARNING: ms_learner_xref is empty - run MigrateMsPersonToZZPerson and "
                    + "MigrateMsLearnerToZZLearner first, or every learnerid on this table "
                    + "will resolve to nothing.");
        }

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzlearnerlearnership WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZLearnerLearnership row(s) "
                    + "(plus their physical C_Location rows)...");
            DB.executeUpdateEx(
                    "CREATE TEMP TABLE tmp_ll_clear_locs AS "
                    + "SELECT zzphysicallocation_id AS loc_id FROM zzlearnerlearnership "
                    + "WHERE id IS NOT NULL AND zzphysicallocation_id IS NOT NULL", null, get_TrxName());
            DB.executeUpdateEx("DELETE FROM zzlearnerlearnership WHERE id IS NOT NULL", null, get_TrxName());
            DB.executeUpdateEx(
                    "DELETE FROM c_location WHERE c_location_id IN (SELECT loc_id FROM tmp_ll_clear_locs)",
                    null, get_TrxName());
            DB.executeUpdateEx("DROP TABLE tmp_ll_clear_locs", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        countryId = MigrationSupport.getSouthAfricaCountryId(get_TrxName());
        provinceCrosswalk = MigrationSupport.buildProvinceCrosswalk(get_TrxName());
        cityCrosswalk = MigrationSupport.buildCityCrosswalk(get_TrxName());

        Map<Integer, Integer> learnerCrosswalk = loadLearnerCrosswalk();
        Map<Integer, Integer> learnershipCrosswalk = MigrationSupport.buildIdCrosswalk("zzlearnership", "zzlearnership_id", get_TrxName());
        Map<Integer, Integer> grantTypeCrosswalk = MigrationSupport.buildIdCrosswalk("zzgranttype", "zzgranttype_id", get_TrxName());
        Map<Integer, Integer> msUserToAdUser = MigrationSupport.buildMsUserToAdUserCrosswalk(get_TrxName());

        Map<Integer, String> programmeStatusMap = MigrationSupport.buildListValueCrosswalk("ms_lkpprogrammestatus", 1000249, get_TrxName());
        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsocioeconomicstatus", 1000250, get_TrxName());
        Map<Integer, String> sponsorshipMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsponsorship", 1000251, get_TrxName());
        Map<Integer, String> projectMap = MigrationSupport.buildListValueCrosswalk("ms_lkpproject", 1000252, get_TrxName());
        Map<Integer, String> reasonForReprintMap = MigrationSupport.buildListValueCrosswalk("ms_lkpreasonforreprint", 1000253, get_TrxName());
        // KNOWN GAP (carried over from Phase 1): ZZOtherSeta/ZZSeta's List (1000256) has ZERO
        // AD_Ref_List entries defined, even though ms_lkpseta has 41 real rows - crosswalk
        // resolves empty (safe no-op) until someone adds the matching List entries.
        Map<Integer, String> setaMap = MigrationSupport.buildListValueCrosswalk("ms_lkpseta", 1000256, get_TrxName());
        Map<Integer, String> enrolmentStatusReasonMap = MigrationSupport.buildListValueCrosswalk("ms_lkpenrolmentstatusreason", 1000255, get_TrxName());
        Map<Integer, String> terminationReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpterminationreason", get_TrxName());
        // Confirmed 2026-07-21: NOT ms_lkplearnershiptype (different values entirely) - see
        // class Javadoc.
        Map<Integer, String> learnerLearnershipTypeMap = MigrationSupport.buildListValueCrosswalk("ms_lkplearnerqctolearnershiptype", 1000257, get_TrxName());

        int yesNoNaReferenceId = AddColumnsSupport.findListReference(getCtx(), YES_NO_NA_REFERENCE_NAME, get_TrxName());
        if (yesNoNaReferenceId == 0) {
            throw new AdempiereException("List reference '" + YES_NO_NA_REFERENCE_NAME
                    + "' not found - run AddZZLearnerLearnershipColumns first.");
        }
        Map<Integer, String> yesNoNaMap = MigrationSupport.buildListValueCrosswalk(
                "ms_lkpyesnonotapplicable", yesNoNaReferenceId, get_TrxName());

        String sql =
                "SELECT id, learnerid, learnershipid, agreementreferencenumber, commencementdate, "
                + "       completiondate, contractnumber, programmestatusid, socioeconomicstatusid, "
                + "       sponsorshipid, projectid, isapproved, approvedby, dateapproved, certificatenumber, "
                + "       certificatecreatedby, datecertificatecreated, certificatereasonforreprintid, "
                + "       certificateprintingerrorreason, statuseffectivedate, belongtofasset, othersetaid, "
                + "       studentnumber, rpl, extensiondate, extensionreason, terminationreasonid, "
                + "       terminatedcapturedby, dateterminationcaptured, extensioncapturedby, "
                + "       dateextensioncaptured, registrationnumber, registrationdate, registeredby, "
                + "       enrolmentstatusreasonid, mostrecentregistrationdate, amountspend, isendorsed, "
                + "       endorsedby, dateendorsed, setaid, physicaladdress1, physicaladdress2, "
                + "       physicaladdress3, physicalcode, physicalprovinceid, physicalcityid, "
                + "       employmentstartdate, estimatecompletiondate, statuscomments, "
                + "       learnerlearnershiptypeid, previouslearnership, previousemployed, learneremployed, "
                + "       wpagreement, durationlearneremployed, istermsemployment, termsemployment, "
                + "       empcontract, empcontractcopy, qcto, occupation, approvalby, approvaldate, "
                + "       granttypeid, nonfundedreason, created, updated, isdeleted "
                + "FROM ms_learnerlearnership "
                + "WHERE NOT EXISTS (SELECT 1 FROM zzlearnerlearnership z WHERE z.id = ms_learnerlearnership.id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerLearnershipRead");
        Trx readTrx = Trx.get(readTrxName, true);
        int processed = 0;
        int created = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(rs, learnerCrosswalk, learnershipCrosswalk, grantTypeCrosswalk, msUserToAdUser,
                            programmeStatusMap, socioEconomicStatusMap, sponsorshipMap, projectMap,
                            reasonForReprintMap, setaMap, enrolmentStatusReasonMap, terminationReasonMap,
                            learnerLearnershipTypeMap, yesNoNaMap);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_learnerlearnership rows (" + created
                            + " ZZLearnerLearnership created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_learnerlearnership row(s): " + created
                + " ZZLearnerLearnership created, " + errors.size() + " error(s).";
    }

    private Map<Integer, Integer> loadLearnerCrosswalk() {
        Map<Integer, Integer> result = new HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT ms_learner_id, zzlearner_id FROM ms_learner_xref", get_TrxName());
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("ms_learner_id"), rs.getInt("zzlearner_id"));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading ms_learner_xref", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    private void processOneRow(ResultSet rs, Map<Integer, Integer> learnerCrosswalk,
            Map<Integer, Integer> learnershipCrosswalk, Map<Integer, Integer> grantTypeCrosswalk,
            Map<Integer, Integer> msUserToAdUser, Map<Integer, String> programmeStatusMap,
            Map<Integer, String> socioEconomicStatusMap, Map<Integer, String> sponsorshipMap,
            Map<Integer, String> projectMap, Map<Integer, String> reasonForReprintMap,
            Map<Integer, String> setaMap, Map<Integer, String> enrolmentStatusReasonMap,
            Map<Integer, String> terminationReasonMap, Map<Integer, String> learnerLearnershipTypeMap,
            Map<Integer, String> yesNoNaMap) throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerId = (Integer) rs.getObject("learnerid");
        Integer learnershipId = (Integer) rs.getObject("learnershipid");
        Integer programmeStatusId = (Integer) rs.getObject("programmestatusid");
        Integer socioEconomicStatusId = (Integer) rs.getObject("socioeconomicstatusid");
        Integer sponsorshipId = (Integer) rs.getObject("sponsorshipid");
        Integer projectId = (Integer) rs.getObject("projectid");
        Integer isApproved = (Integer) rs.getObject("isapproved");
        Integer approvedBy = (Integer) rs.getObject("approvedby");
        Integer certificateCreatedBy = (Integer) rs.getObject("certificatecreatedby");
        Integer certificateReasonForReprintId = (Integer) rs.getObject("certificatereasonforreprintid");
        Integer belongToFasset = (Integer) rs.getObject("belongtofasset");
        Integer otherSetaId = (Integer) rs.getObject("othersetaid");
        Integer rpl = (Integer) rs.getObject("rpl");
        Integer terminationReasonId = (Integer) rs.getObject("terminationreasonid");
        Integer terminatedCapturedBy = (Integer) rs.getObject("terminatedcapturedby");
        Integer extensionCapturedBy = (Integer) rs.getObject("extensioncapturedby");
        Integer registeredBy = (Integer) rs.getObject("registeredby");
        Integer enrolmentStatusReasonId = (Integer) rs.getObject("enrolmentstatusreasonid");
        Integer isEndorsed = (Integer) rs.getObject("isendorsed");
        Integer endorsedBy = (Integer) rs.getObject("endorsedby");
        Integer setaId = (Integer) rs.getObject("setaid");
        Integer provinceId = (Integer) rs.getObject("physicalprovinceid");
        Integer cityId = (Integer) rs.getObject("physicalcityid");
        Integer learnerLearnershipTypeId = (Integer) rs.getObject("learnerlearnershiptypeid");
        Integer previousLearnership = (Integer) rs.getObject("previouslearnership");
        Integer previousEmployed = (Integer) rs.getObject("previousemployed");
        Integer wpAgreement = (Integer) rs.getObject("wpagreement");
        Integer isTermsEmployment = (Integer) rs.getObject("istermsemployment");
        Integer empContract = (Integer) rs.getObject("empcontract");
        Integer empContractCopy = (Integer) rs.getObject("empcontractcopy");
        Integer approvalBy = (Integer) rs.getObject("approvalby");
        Integer grantTypeId = (Integer) rs.getObject("granttypeid");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        if (learnerId == null || learnerCrosswalk.get(learnerId) == null) {
            throw new AdempiereException(
                    "No matching ZZLearner for learnerid=" + learnerId + " (person/learner not migrated yet?)");
        }

        String trxName = Trx.createTrxName("MsLearnerLearnershipMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            int physicalLocationId = MigrationSupport.createLocation(getCtx(), trxName, countryId,
                    provinceCrosswalk.get(provinceId), cityCrosswalk.get(cityId), null,
                    rs.getString("physicaladdress1"), rs.getString("physicaladdress2"),
                    rs.getString("physicaladdress3"), rs.getString("physicalcode"));

            X_ZZLearnerLearnership ll = new X_ZZLearnerLearnership(getCtx(), 0, trxName);
            ll.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            ll.setIsActive(isDeleted == 0);
            ll.setZZLearner_ID(learnerCrosswalk.get(learnerId));
            setIfResolved(learnershipCrosswalk, learnershipId, ll::setZZLearnership_ID);
            ll.setZZAgreementReferenceNumber(rs.getString("agreementreferencenumber"));
            setTimestamp(rs.getTimestamp("commencementdate"), ll::setZZCommencementDate);
            ll.setZZContractNumber(rs.getString("contractnumber"));
            ll.setZZProgrammeStatus(programmeStatusId == null ? null : programmeStatusMap.get(programmeStatusId));
            ll.setZZSocioEconomicStatus(socioEconomicStatusId == null ? null : socioEconomicStatusMap.get(socioEconomicStatusId));
            ll.setZZSponsorship(sponsorshipId == null ? null : sponsorshipMap.get(sponsorshipId));
            ll.setZZProject(projectId == null ? null : projectMap.get(projectId));
            // ZZ_FinYear_ID (financialyearid): NOT SET - see class Javadoc.
            ll.setZZCertificateNumber(rs.getString("certificatenumber"));
            setIfResolved(msUserToAdUser, certificateCreatedBy, ll::setZZCertificateCreatedBy);
            setTimestamp(rs.getTimestamp("datecertificatecreated"), ll::setZZDateCertificateCreated);
            ll.setZZCertificateReasonForReprint(certificateReasonForReprintId == null ? null : reasonForReprintMap.get(certificateReasonForReprintId));
            ll.setZZCertificatePrintingErrorReason(rs.getString("certificateprintingerrorreason"));
            setTimestamp(rs.getTimestamp("statuseffectivedate"), ll::setZZStatusEffectiveDate);
            ll.setZZBelongToFasset(MigrationSupport.flagToYN(belongToFasset));
            ll.setZZOtherSeta(otherSetaId == null ? null : setaMap.get(otherSetaId));
            ll.setZZStudentNumber(rs.getString("studentnumber"));
            // Both members of the ZZIsRPL/ZZRPL pair get the same value - user decision.
            setGeneric(ll, "ZZIsRPL", MigrationSupport.flagToYN(rpl));
            ll.setZZRPL(MigrationSupport.flagToYN(rpl));
            ll.setZZTerminationReason(terminationReasonId == null ? null : terminationReasonMap.get(terminationReasonId));
            setIfResolved(msUserToAdUser, terminatedCapturedBy, ll::setZZTerminatedCapturedBy);
            setTimestamp(rs.getTimestamp("dateterminationcaptured"), ll::setZZDateTerminationCaptured);
            setIfResolved(msUserToAdUser, extensionCapturedBy, ll::setZZExtensionCapturedBy);
            setTimestamp(rs.getTimestamp("dateextensioncaptured"), ll::setZZDateExtensionCaptured);
            ll.setZZRegistrationNumber(rs.getString("registrationnumber"));
            setTimestamp(rs.getTimestamp("registrationdate"), ll::setZZRegistrationDate);
            setIfResolved(msUserToAdUser, registeredBy, ll::setZZRegisteredBy);
            ll.setZZEnrolmentStatusReason(enrolmentStatusReasonId == null ? null : enrolmentStatusReasonMap.get(enrolmentStatusReasonId));
            setTimestamp(rs.getTimestamp("mostrecentregistrationdate"), ll::setZZMostRecentRegistrationDate);
            ll.setZZAmountSpend(rs.getString("amountspend"));
            // Both members of the ZZIsEndorsed/ZZEndorsed pair get the same value - user decision.
            setGeneric(ll, "ZZIsEndorsed", MigrationSupport.flagToYN(isEndorsed));
            ll.setZZEndorsed(MigrationSupport.flagToYN(isEndorsed));
            setIfResolved(msUserToAdUser, endorsedBy, ll::setZZEndorsedBy);
            setTimestamp(rs.getTimestamp("dateendorsed"), ll::setZZDateEndorsed);
            ll.setZZSeta(setaId == null ? null : setaMap.get(setaId));
            if (physicalLocationId > 0) {
                ll.setZZPhysicalLocation_ID(physicalLocationId);
            }
            // physicalmunicipalityid / physicalurbanruralid / physicalsuburbid: NOT SET -
            // C_Location has no matching fields.
            setTimestamp(rs.getTimestamp("employmentstartdate"), ll::setZZEmploymentStartDate);
            setTimestamp(rs.getTimestamp("estimatecompletiondate"), ll::setZZEstimateCompletionDate);
            ll.setZZStatusComments(rs.getString("statuscomments"));
            ll.setZZLearnerLearnershipType(learnerLearnershipTypeId == null ? null : learnerLearnershipTypeMap.get(learnerLearnershipTypeId));
            setIfResolved(learnershipCrosswalk, previousLearnership, ll::setZZPreviousLearnership_ID);
            // previouslearnershipcode / previouslearnershiptitle: NOT SET - redundant text
            // mirrors of previouslearnership, which IS resolved above.
            ll.setZZPreviousEmployed(MigrationSupport.yesNoIdToFlag(previousEmployed));
            ll.setZZLearnerEmployed(rs.getString("learneremployed"));

            // New columns (added by AddZZLearnerLearnershipColumns) - generic PO setter, no
            // typed setter exists yet on the generated model class.
            setGeneric(ll, "id", sourceId);
            setGeneric(ll, "ZZCompletionDate", rs.getTimestamp("completiondate"));
            setGeneric(ll, "ZZExtensionDate", rs.getTimestamp("extensiondate"));
            setGeneric(ll, "ZZExtensionReason", rs.getString("extensionreason"));
            setGeneric(ll, "ZZDurationLearnerEmployed", rs.getString("durationlearneremployed"));
            setGeneric(ll, "ZZTermsEmployment", rs.getString("termsemployment"));
            setGeneric(ll, "ZZOccupation", rs.getString("occupation"));
            setGeneric(ll, "ZZNonFundedReason", rs.getString("nonfundedreason"));
            setGeneric(ll, "ZZQCTO", rs.getString("qcto"));
            setGeneric(ll, "ZZIsApproved", MigrationSupport.flagToYN(isApproved));
            setPoIfResolved(ll, "ZZApprovedBy", msUserToAdUser, approvedBy);
            setGeneric(ll, "ZZDateApproved", rs.getTimestamp("dateapproved"));
            setPoIfResolved(ll, "ZZApprovalBy", msUserToAdUser, approvalBy);
            setGeneric(ll, "ZZApprovalDate", rs.getTimestamp("approvaldate"));
            setGeneric(ll, "ZZWPAgreement", MigrationSupport.yesNoIdToFlag(wpAgreement));
            setGeneric(ll, "ZZEmpContract", MigrationSupport.yesNoIdToFlag(empContract));
            setGeneric(ll, "ZZEmpContractCopy", MigrationSupport.yesNoIdToFlag(empContractCopy));
            setGeneric(ll, "ZZIsTermsEmployment", isTermsEmployment == null ? null : yesNoNaMap.get(isTermsEmployment));
            setPoIfResolved(ll, "ZZGrantType_ID", grantTypeCrosswalk, grantTypeId);
            // bi_registrationdate/bi_approvaldate, responsibleseta/curregnumber: NOT SET -
            // redundant denormalised duplicates, see class Javadoc.
            // setalearnershiptypeid/agentid/asspartner/regsaqa: NOT SET - deferred, genuinely
            // unclear, no lookup table found.
            // accountnumber: NOT SET - the MSSQL row's own UUID, not business data.

            ll.saveEx();
            int zzId = ll.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzlearnerlearnership", "zzlearnerlearnership_id", zzId,
                        createdTs, DEFAULT_CREATED_BY, updatedTs, DEFAULT_CREATED_BY, sourceId, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private static void setIfResolved(Map<Integer, Integer> crosswalk, Integer sourceId, java.util.function.IntConsumer setter) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            setter.accept(targetId);
        }
    }

    /** Sets a column via the generic PO API, only if the value is non-null (used for the new
     * columns, which have no typed setter on the generated model class yet). */
    private static void setGeneric(PO po, String columnName, Object value) {
        if (value != null) {
            po.set_ValueOfColumn(columnName, value);
        }
    }

    private static void setPoIfResolved(PO po, String columnName, Map<Integer, Integer> crosswalk, Integer sourceId) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            po.set_ValueOfColumn(columnName, targetId);
        }
    }

    private static void setTimestamp(Timestamp value, java.util.function.Consumer<Timestamp> setter) {
        if (value != null && setter != null) {
            setter.accept(value);
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_learnerlearnership.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learnerlearnership-errors-" + ts + ".txt");
        try (PrintWriter out = new PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(logFile)))) {
            for (String err : errors) {
                out.println(err);
            }
            addLog("Error log written to: " + logFile.getAbsolutePath()
                    + (errors.size() >= MAX_LOGGED_ERRORS ? " (truncated at " + MAX_LOGGED_ERRORS + ")" : ""));
        } catch (Exception e) {
            addLog("WARN: could not write error log: " + e.getMessage());
        }
    }
}
