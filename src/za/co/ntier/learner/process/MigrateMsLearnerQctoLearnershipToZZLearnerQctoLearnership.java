package za.co.ntier.learner.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
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

import za.co.ntier.api.model.X_ZZLearnerQCTOLearnership;

/**
 * Migrates the staged ms_learnerqctolearnership table (each learner's enrolment in a QCTO
 * learnership) into ZZLearnerQCTOLearnership. This is one of the 3 original tables
 * requested. See "Column Mapping - QCTO Learner Programmes" doc, tab
 * "LearnerQCTOLearnership" for the full column-by-column reasoning.
 *
 * <p>Must run AFTER: MigrateMsPersonToZZPerson/MigrateMsLearnerToZZLearner (ms_learner_xref),
 * MigrateMsQctoLearnershipToZZQctoLearnership, MigrateMsQctoProgrammeStatusToZZQctoProgrammeStatus,
 * MigrateMsGrantTypeToZZGrantType - every FK column below depends on one of those crosswalks.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzlearnerqctolearnership ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>Builds a new C_Location per row from physicaladdress1/2/3/physicalcode/
 * physicalprovinceid/physicalcityid (same MigrationSupport.createLocation() helper and
 * province/city crosswalks used by MigrateMsPersonToZZPerson) for ZZPhysicalLocation_ID.
 *
 * <p>AD_User actor columns (certificatecreatedby, terminatedcapturedby, extensioncapturedby,
 * registeredby, endorsedby, approvedby, nambconfirmationuser) are resolved via
 * ms_user.email -&gt; ad_user.email, link-only - NEVER creates an AD_User, same rule as
 * zzperson.ad_user_id.
 *
 * <p>The Section A/B columns AddZZLearnerQctoLearnershipColumns.java added (2026-07-16) are
 * set via the generic {@code PO.set_ValueOfColumn(String, Object)} API (no typed setter exists
 * yet on the generated model class - see MigrateMsProviderToZZProvider's Javadoc for why). See
 * "ZZLearnerQCTOLearnership - New Columns to Add.txt" for the full column-by-column reasoning
 * - same conversions as the Artisans process (Is_Approved via flagToYN, Previous_Employed/
 * WP_Agreement/Is_Terms_Employment/Emp_Contract/Emp_Contract_Copy via yesNoIdToFlag, Approved_By/
 * Namb_Confirmation_User via the ms_user email-match crosswalk, Employer_ID via the same
 * buildOrganisationToBPartnerCrosswalk as organisationid elsewhere - confirmed 4/4 join here).
 *
 * <p>FIXED 2026-07-16: ZZTerminationReason/ZZTerminationReasonText were previously swapped,
 * same bug and same fix as MigrateMsLearnerQctoArtisansToZZLearnerQctoArtisans - see that
 * class's Javadoc for the full explanation.
 *
 * <p>Still NOT handled (all documented as "Unmapped"/"Ignored" in the mapping doc):
 * <ul>
 *   <li>bi_registrationdate / bi_approvaldate - 0 of 21 rows populated; "BI_" prefixed columns
 *       exist ONLY on LearnerLearnership/LearnerQCTOLearnership in the whole MSSQL schema - a
 *       reporting-mirror pattern, not source-of-truth data.</li>
 *   <li>approvaldate / approvalby - confirmed an exact-value duplicate of dateapproved/
 *       approvedby (same BI-mirror family) - dateapproved/approvedby already cover this.</li>
 *   <li>previousqctolearnershipcode / previousqctolearnershiptitle - redundant text mirrors,
 *       no separate target column.</li>
 *   <li>physicalmunicipalityid / physicalurbanruralid / physicalsuburbid - C_Location has no
 *       matching fields.</li>
 *   <li>accountnumber - the MSSQL row's own UUID, not business data.</li>
 *   <li>financialyearid -&gt; ZZFinancialYear_ID - FinancialYear-to-C_Year crosswalk not
 *       yet designed (Open Item #3 in the mapping doc).</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerQctoLearnershipToZZLearnerQctoLearnership")
public class MigrateMsLearnerQctoLearnershipToZZLearnerQctoLearnership extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzlearnerqctolearnership WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZLearnerQCTOLearnership row(s) "
                    + "(plus their physical C_Location rows)...");
            DB.executeUpdateEx(
                    "CREATE TEMP TABLE tmp_lql_clear_locs AS "
                    + "SELECT zzphysicallocation_id AS loc_id FROM zzlearnerqctolearnership "
                    + "WHERE id IS NOT NULL AND zzphysicallocation_id IS NOT NULL", null, get_TrxName());
            DB.executeUpdateEx("DELETE FROM zzlearnerqctolearnership WHERE id IS NOT NULL", null, get_TrxName());
            DB.executeUpdateEx(
                    "DELETE FROM c_location WHERE c_location_id IN (SELECT loc_id FROM tmp_lql_clear_locs)",
                    null, get_TrxName());
            DB.executeUpdateEx("DROP TABLE tmp_lql_clear_locs", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        countryId = MigrationSupport.getSouthAfricaCountryId(get_TrxName());
        provinceCrosswalk = MigrationSupport.buildProvinceCrosswalk(get_TrxName());
        cityCrosswalk = MigrationSupport.buildCityCrosswalk(get_TrxName());

        Map<Integer, Integer> learnerCrosswalk = loadLearnerCrosswalk();
        Map<Integer, Integer> qctoLearnershipCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctolearnership", "zzqctolearnership_id", get_TrxName());
        Map<Integer, Integer> qctoProgrammeStatusCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctoprogrammestatus", "zzqctoprogrammestatus_id", get_TrxName());
        Map<Integer, Integer> grantTypeCrosswalk = MigrationSupport.buildIdCrosswalk("zzgranttype", "zzgranttype_id", get_TrxName());
        Map<Integer, Integer> msUserToAdUser = MigrationSupport.buildMsUserToAdUserCrosswalk(get_TrxName());
        // ZZSocioEconomicStatus/ZZSponsorship/ZZProject/ZZCertificateReasonForReprint/
        // ZZEnrolmentStatusReason/ZZOtherSeta/ZZSeta/ZZLearnerQCTOLearnershipType/
        // ZZQualificationRequirements are List references (AD_Reference_ID=17), NOT plain
        // String columns as originally assumed - same discovery as
        // MigrateMsLearnerQctoArtisansToZZLearnerQctoArtisans (see that class for the full
        // explanation). buildListValueCrosswalk() matches by name against each column's own
        // AD_Ref_List, returning the correct short Value code to store.
        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsocioeconomicstatus", 1000250, get_TrxName());
        Map<Integer, String> sponsorshipMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsponsorship", 1000251, get_TrxName());
        Map<Integer, String> projectMap = MigrationSupport.buildListValueCrosswalk("ms_lkpproject", 1000252, get_TrxName());
        Map<Integer, String> reasonForReprintMap = MigrationSupport.buildListValueCrosswalk("ms_lkpreasonforreprint", 1000253, get_TrxName());
        // KNOWN GAP (2026-07-16): ZZOtherSeta/ZZSeta's List (AD_Reference_ID=1000256) has ZERO
        // AD_Ref_List entries defined, even though ms_lkpseta has 41 real rows - so this
        // crosswalk will always resolve empty (safe no-op, not a crash) until someone adds the
        // 41 matching List entries, or the column is redesigned. NOT fixed here - populating
        // Application Dictionary List values is a bigger structural change than adding a
        // column, flagged for a decision rather than done silently.
        Map<Integer, String> setaMap = MigrationSupport.buildListValueCrosswalk("ms_lkpseta", 1000256, get_TrxName());
        Map<Integer, String> enrolmentStatusReasonMap = MigrationSupport.buildListValueCrosswalk("ms_lkpenrolmentstatusreason", 1000255, get_TrxName());
        // terminationReasonMap feeds ZZTerminationReason (also a List reference, 1000254) via
        // plain description text - this happens to be correct as-is because that specific
        // list's Value equals its Name for all 8 entries (confirmed directly), not because it's
        // a String column - ZZTerminationReasonText (the real String column) gets the raw
        // source text separately, see processOneRow().
        Map<Integer, String> terminationReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpterminationreason", get_TrxName());
        Map<Integer, String> learnerQctoLearnershipTypeMap = MigrationSupport.buildListValueCrosswalk("ms_lkplearnerqctolearnershiptype", 1000336, get_TrxName());
        Map<Integer, String> qualificationRequirementsMap = MigrationSupport.buildListValueCrosswalk("ms_lkpqualificationentryrequirements", 1000332, get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(get_TrxName());

        String sql =
                "SELECT id, learnerid, qctolearnershipid, agreementreferencenumber, commencementdate, "
                + "       completiondate, contractnumber, qctoprogrammestatusid, socioeconomicstatusid, "
                + "       sponsorshipid, projectid, isapproved, approvedby, dateapproved, certificatenumber, "
                + "       certificatecreatedby, "
                + "       datecertificatecreated, certificatereasonforreprintid, "
                + "       certificateprintingerrorreason, statuseffectivedate, belongtofasset, othersetaid, "
                + "       studentnumber, rpl, extensiondate, extensionreason, terminationdate, "
                + "       terminationreason, terminationreasonid, terminatedcapturedby, "
                + "       dateterminationcaptured, extensioncapturedby, dateextensioncaptured, "
                + "       registrationnumber, registrationdate, registeredby, enrolmentstatusreasonid, "
                + "       mostrecentregistrationdate, amountspend, isendorsed, endorsedby, dateendorsed, "
                + "       setaid, physicaladdress1, physicaladdress2, physicaladdress3, physicalcode, "
                + "       physicalprovinceid, physicalcityid, employmentstartdate, estimatecompletiondate, "
                + "       statuscomments, learnerqctolearnershiptypeid, previousqctolearnership, "
                + "       previousemployed, learneremployed, wpagreement, durationlearneremployed, "
                + "       istermsemployment, termsemployment, empcontract, empcontractcopy, "
                + "       responsibleseta, asspartner, regsaqa, curregnumber, qcto, occupation, "
                + "       nambconfirmation, nambconfirmationdate, nambconfirmationuser, employerid, "
                + "       granttypeid, qualificationentryrequirementsid, "
                + "       created, updated, isdeleted "
                + "FROM ms_learnerqctolearnership "
                + "WHERE NOT EXISTS (SELECT 1 FROM zzlearnerqctolearnership z WHERE z.id = ms_learnerqctolearnership.id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerQctoLearnershipRead");
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
                    processOneRow(rs, learnerCrosswalk, qctoLearnershipCrosswalk, qctoProgrammeStatusCrosswalk,
                            grantTypeCrosswalk, msUserToAdUser, socioEconomicStatusMap, sponsorshipMap, projectMap,
                            reasonForReprintMap, setaMap, enrolmentStatusReasonMap, terminationReasonMap,
                            learnerQctoLearnershipTypeMap, qualificationRequirementsMap, organisationToBPartnerCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_learnerqctolearnership rows (" + created
                            + " ZZLearnerQCTOLearnership created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_learnerqctolearnership row(s): " + created
                + " ZZLearnerQCTOLearnership created, " + errors.size() + " error(s).";
    }

    private Map<Integer, Integer> loadLearnerCrosswalk() {
        Map<Integer, Integer> result = new java.util.HashMap<>();
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
            Map<Integer, Integer> qctoLearnershipCrosswalk, Map<Integer, Integer> qctoProgrammeStatusCrosswalk,
            Map<Integer, Integer> grantTypeCrosswalk, Map<Integer, Integer> msUserToAdUser,
            Map<Integer, String> socioEconomicStatusMap, Map<Integer, String> sponsorshipMap,
            Map<Integer, String> projectMap, Map<Integer, String> reasonForReprintMap,
            Map<Integer, String> setaMap, Map<Integer, String> enrolmentStatusReasonMap,
            Map<Integer, String> terminationReasonMap, Map<Integer, String> learnerQctoLearnershipTypeMap,
            Map<Integer, String> qualificationRequirementsMap, Map<Integer, Integer> organisationToBPartnerCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerId = (Integer) rs.getObject("learnerid");
        Integer qctoLearnershipId = (Integer) rs.getObject("qctolearnershipid");
        Integer qctoProgrammeStatusId = (Integer) rs.getObject("qctoprogrammestatusid");
        Integer socioEconomicStatusId = (Integer) rs.getObject("socioeconomicstatusid");
        Integer sponsorshipId = (Integer) rs.getObject("sponsorshipid");
        Integer projectId = (Integer) rs.getObject("projectid");
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
        Integer learnerQctoLearnershipTypeId = (Integer) rs.getObject("learnerqctolearnershiptypeid");
        Integer previousQctoLearnership = (Integer) rs.getObject("previousqctolearnership");
        Integer grantTypeId = (Integer) rs.getObject("granttypeid");
        Integer qualificationRequirementsId = (Integer) rs.getObject("qualificationrequirementsid");
        Integer isApproved = (Integer) rs.getObject("isapproved");
        Integer approvedBy = (Integer) rs.getObject("approvedby");
        Integer previousEmployed = (Integer) rs.getObject("previousemployed");
        Integer wpAgreement = (Integer) rs.getObject("wpagreement");
        Integer isTermsEmployment = (Integer) rs.getObject("istermsemployment");
        Integer empContract = (Integer) rs.getObject("empcontract");
        Integer empContractCopy = (Integer) rs.getObject("empcontractcopy");
        Integer nambConfirmation = (Integer) rs.getObject("nambconfirmation");
        Integer nambConfirmationUser = (Integer) rs.getObject("nambconfirmationuser");
        Integer employerId = (Integer) rs.getObject("employerid");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        if (learnerId == null || learnerCrosswalk.get(learnerId) == null) {
            throw new AdempiereException(
                    "No matching ZZLearner for learnerid=" + learnerId + " (person/learner not migrated yet?)");
        }

        String trxName = Trx.createTrxName("MsLearnerQctoLearnershipMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            int physicalLocationId = MigrationSupport.createLocation(getCtx(), trxName, countryId,
                    provinceCrosswalk.get(provinceId), cityCrosswalk.get(cityId), null,
                    rs.getString("physicaladdress1"), rs.getString("physicaladdress2"),
                    rs.getString("physicaladdress3"), rs.getString("physicalcode"));

            X_ZZLearnerQCTOLearnership learnership = new X_ZZLearnerQCTOLearnership(getCtx(), 0, trxName);
            learnership.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            learnership.setIsActive(isDeleted == 0);
            learnership.setZZLearner_ID(learnerCrosswalk.get(learnerId));
            setIfResolved(qctoLearnershipCrosswalk, qctoLearnershipId, learnership::setZZQctoLearnership_ID);
            learnership.setZZAgreementReferenceNumber(rs.getString("agreementreferencenumber"));
            setTimestamp(rs.getTimestamp("commencementdate"), learnership::setZZCommencementDate);
            setTimestamp(rs.getTimestamp("completiondate"), learnership::setZZCompletionDate);
            learnership.setZZContractNumber(rs.getString("contractnumber"));
            setIfResolved(qctoProgrammeStatusCrosswalk, qctoProgrammeStatusId, learnership::setZZQctoProgrammeStatus_ID);
            learnership.setZZSocioEconomicStatus(socioEconomicStatusId == null ? null : socioEconomicStatusMap.get(socioEconomicStatusId));
            learnership.setZZSponsorship(sponsorshipId == null ? null : sponsorshipMap.get(sponsorshipId));
            learnership.setZZProject(projectId == null ? null : projectMap.get(projectId));
            // ZZFinancialYear_ID (financialyearid): NOT SET - see class Javadoc.
            setGeneric(learnership, "Is_Approved", MigrationSupport.flagToYN(isApproved));
            setPoIfResolved(learnership, "Approved_By", msUserToAdUser, approvedBy);
            setGeneric(learnership, "Date_Approved", rs.getTimestamp("dateapproved"));
            learnership.setZZCertificateNumber(rs.getString("certificatenumber"));
            setIfResolved(msUserToAdUser, certificateCreatedBy, learnership::setZZCertificateCreatedBy);
            setTimestamp(rs.getTimestamp("datecertificatecreated"), learnership::setZZDateCertificateCreated);
            learnership.setZZCertificateReasonForReprint(certificateReasonForReprintId == null ? null : reasonForReprintMap.get(certificateReasonForReprintId));
            learnership.setZZCertificatePrintingErrorReason(rs.getString("certificateprintingerrorreason"));
            setTimestamp(rs.getTimestamp("statuseffectivedate"), learnership::setZZStatusEffectiveDate);
            learnership.setZZBelongToFasset(MigrationSupport.flagToYN(belongToFasset));
            learnership.setZZOtherSeta(otherSetaId == null ? null : setaMap.get(otherSetaId));
            learnership.setZZStudentNumber(rs.getString("studentnumber"));
            // migrationrecordid: ignore - historic migration artifact.
            learnership.setZZRPL(MigrationSupport.flagToYN(rpl));
            // bi_registrationdate / bi_approvaldate: NOT SET - no target column, see class Javadoc.
            setTimestamp(rs.getTimestamp("extensiondate"), learnership::setZZExtensionDate);
            learnership.setZZExtensionReason(rs.getString("extensionreason"));
            setTimestamp(rs.getTimestamp("terminationdate"), learnership::setZZTerminationDate);
            // FIXED 2026-07-16 - these were swapped (see class Javadoc): ZZTerminationReason is
            // a List reference matching lkpTerminationReason verbatim (terminationreasonid's
            // resolved description), ZZTerminationReasonText is the raw free text.
            learnership.setZZTerminationReason(terminationReasonId == null ? null : terminationReasonMap.get(terminationReasonId));
            learnership.setZZTerminationReasonText(rs.getString("terminationreason"));
            setIfResolved(msUserToAdUser, terminatedCapturedBy, learnership::setZZTerminatedCapturedBy);
            setTimestamp(rs.getTimestamp("dateterminationcaptured"), learnership::setZZDateTerminationCaptured);
            setIfResolved(msUserToAdUser, extensionCapturedBy, learnership::setZZExtensionCapturedBy);
            setTimestamp(rs.getTimestamp("dateextensioncaptured"), learnership::setZZDateExtensionCaptured);
            learnership.setZZRegistrationNumber(rs.getString("registrationnumber"));
            setTimestamp(rs.getTimestamp("registrationdate"), learnership::setZZRegistrationDate);
            setIfResolved(msUserToAdUser, registeredBy, learnership::setZZRegisteredBy);
            learnership.setZZEnrolmentStatusReason(enrolmentStatusReasonId == null ? null : enrolmentStatusReasonMap.get(enrolmentStatusReasonId));
            setTimestamp(rs.getTimestamp("mostrecentregistrationdate"), learnership::setZZMostRecentRegistrationDate);
            learnership.setZZAmountSpend(rs.getString("amountspend"));
            learnership.setZZEndorsed(MigrationSupport.flagToYN(isEndorsed));
            setIfResolved(msUserToAdUser, endorsedBy, learnership::setZZEndorsedBy);
            setTimestamp(rs.getTimestamp("dateendorsed"), learnership::setZZDateEndorsed);
            learnership.setZZSeta(setaId == null ? null : setaMap.get(setaId));
            if (physicalLocationId > 0) {
                learnership.setZZPhysicalLocation_ID(physicalLocationId);
            }
            // physicalmunicipalityid / physicalurbanruralid / physicalsuburbid: NOT SET -
            // C_Location has no matching fields, see class Javadoc.
            setTimestamp(rs.getTimestamp("employmentstartdate"), learnership::setZZEmploymentStartDate);
            setTimestamp(rs.getTimestamp("estimatecompletiondate"), learnership::setZZEstimateCompletionDate);
            learnership.setZZStatusComments(rs.getString("statuscomments"));
            learnership.setZZLearnerQCTOLearnershipType(learnerQctoLearnershipTypeId == null ? null : learnerQctoLearnershipTypeMap.get(learnerQctoLearnershipTypeId));
            setIfResolved(qctoLearnershipCrosswalk, previousQctoLearnership, learnership::setZZPreviousQctoLearnership_ID);
            // previousqctolearnershipcode / previousqctolearnershiptitle: NOT SET - redundant
            // text mirrors, no separate target column, see class Javadoc.
            setGeneric(learnership, "Previous_Employed", MigrationSupport.yesNoIdToFlag(previousEmployed));
            setGeneric(learnership, "Learner_Employed", rs.getString("learneremployed"));
            setGeneric(learnership, "WP_Agreement", MigrationSupport.yesNoIdToFlag(wpAgreement));
            learnership.setZZDurationLearnerEmployed(rs.getString("durationlearneremployed"));
            setGeneric(learnership, "Is_Terms_Employment", MigrationSupport.yesNoIdToFlag(isTermsEmployment));
            setGeneric(learnership, "Terms_Employment", rs.getString("termsemployment"));
            setGeneric(learnership, "Emp_Contract", MigrationSupport.yesNoIdToFlag(empContract));
            setGeneric(learnership, "Emp_Contract_Copy", MigrationSupport.yesNoIdToFlag(empContractCopy));
            setGeneric(learnership, "Responsible_Seta", rs.getString("responsibleseta"));
            setGeneric(learnership, "Ass_Partner", rs.getString("asspartner"));
            setGeneric(learnership, "Reg_Saqa", rs.getString("regsaqa"));
            setGeneric(learnership, "Cur_Reg_Number", rs.getString("curregnumber"));
            setGeneric(learnership, "Qcto", rs.getString("qcto"));
            setGeneric(learnership, "Occupation", rs.getString("occupation"));
            // approvaldate / approvalby: NOT SET - confirmed exact-value duplicates of
            // dateapproved/approvedby, see class Javadoc.
            setIfResolved(grantTypeCrosswalk, grantTypeId, learnership::setZZGrantType_ID);
            learnership.setZZQualificationRequirements(qualificationRequirementsId == null ? null : qualificationRequirementsMap.get(qualificationRequirementsId));
            setGeneric(learnership, "Namb_Confirmation", MigrationSupport.flagToYN(nambConfirmation));
            setGeneric(learnership, "Namb_Confirmation_Date", rs.getTimestamp("nambconfirmationdate"));
            setPoIfResolved(learnership, "Namb_Confirmation_User", msUserToAdUser, nambConfirmationUser);
            setPoIfResolved(learnership, "Employer_ID", organisationToBPartnerCrosswalk, employerId);
            // accountnumber: NOT SET - the MSSQL row's own UUID, not business data.

            learnership.saveEx();
            int zzId = learnership.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzlearnerqctolearnership", "zzlearnerqctolearnership_id", zzId,
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
     * Section A/B columns, which have no typed setter on the generated model class yet). */
    private static void setGeneric(PO po, String columnName, Object value) {
        if (value != null) {
            po.set_ValueOfColumn(columnName, value);
        }
    }

    /** Same as {@link #setIfResolved(Map, Integer, java.util.function.IntConsumer)} but for a
     * new column with no typed setter, set via the generic PO API. */
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
        if (value != null) {
            setter.accept(value);
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_learnerqctolearnership.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learnerqctolearnership-errors-" + ts + ".txt");
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
