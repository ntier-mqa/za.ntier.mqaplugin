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
 * registeredby, endorsedby) are resolved via ms_user.email -&gt; ad_user.email, link-only -
 * NEVER creates an AD_User, same rule as zzperson.ad_user_id.
 *
 * <p>NOT handled (all documented as "Unmapped" in the mapping doc):
 * <ul>
 *   <li>isapproved / approvedby / dateapproved - no target column.</li>
 *   <li>bi_registrationdate / bi_approvaldate - no target column.</li>
 *   <li>previousqctolearnershipcode / previousqctolearnershiptitle - redundant text mirrors,
 *       no separate target column.</li>
 *   <li>previousemployed / learneremployed / wpagreement / istermsemployment /
 *       termsemployment / empcontract / empcontractcopy / responsibleseta / asspartner /
 *       regsaqa / curregnumber / qcto / occupation - no target column for any of these.</li>
 *   <li>approvaldate / approvalby - distinct duplicates of dateapproved/approvedby above,
 *       same reason.</li>
 *   <li>nambconfirmation / nambconfirmationdate / nambconfirmationuser - no target column.</li>
 *   <li>physicalmunicipalityid / physicalurbanruralid / physicalsuburbid - C_Location has no
 *       matching fields.</li>
 *   <li>accountnumber - internal MSSQL row identifier, not needed.</li>
 *   <li>employerid - no target column exists anywhere for this on any of the 3 join
 *       tables.</li>
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
        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildDescriptionMap("ms_lkpsocioeconomicstatus", get_TrxName());
        Map<Integer, String> sponsorshipMap = MigrationSupport.buildDescriptionMap("ms_lkpsponsorship", get_TrxName());
        Map<Integer, String> projectMap = MigrationSupport.buildDescriptionMap("ms_lkpproject", get_TrxName());
        Map<Integer, String> reasonForReprintMap = MigrationSupport.buildDescriptionMap("ms_lkpreasonforreprint", get_TrxName());
        Map<Integer, String> setaMap = MigrationSupport.buildDescriptionMap("ms_lkpseta", get_TrxName());
        Map<Integer, String> enrolmentStatusReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpenrolmentstatusreason", get_TrxName());
        Map<Integer, String> terminationReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpterminationreason", get_TrxName());
        Map<Integer, String> learnerQctoLearnershipTypeMap = MigrationSupport.buildDescriptionMap("ms_lkplearnerqctolearnershiptype", get_TrxName());
        Map<Integer, String> qualificationRequirementsMap = MigrationSupport.buildDescriptionMap("ms_lkpqualificationentryrequirements", get_TrxName());

        String sql =
                "SELECT id, learnerid, qctolearnershipid, agreementreferencenumber, commencementdate, "
                + "       completiondate, contractnumber, qctoprogrammestatusid, socioeconomicstatusid, "
                + "       sponsorshipid, projectid, certificatenumber, certificatecreatedby, "
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
                + "       durationlearneremployed, granttypeid, qualificationentryrequirementsid, "
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
                            learnerQctoLearnershipTypeMap, qualificationRequirementsMap);
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
            Map<Integer, String> qualificationRequirementsMap) throws Exception {
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
            // isapproved / approvedby / dateapproved: NOT SET - no target column, see class Javadoc.
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
            learnership.setZZTerminationReason(rs.getString("terminationreason"));
            learnership.setZZTerminationReasonText(terminationReasonId == null ? null : terminationReasonMap.get(terminationReasonId));
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
            // previousemployed / learneremployed / wpagreement: NOT SET, see class Javadoc.
            learnership.setZZDurationLearnerEmployed(rs.getString("durationlearneremployed"));
            // istermsemployment / termsemployment / empcontract / empcontractcopy /
            // responsibleseta / asspartner / regsaqa / curregnumber / qcto / occupation:
            // NOT SET - no target column, see class Javadoc.
            // approvaldate / approvalby: NOT SET - duplicates of dateapproved/approvedby, see class Javadoc.
            setIfResolved(grantTypeCrosswalk, grantTypeId, learnership::setZZGrantType_ID);
            learnership.setZZQualificationRequirements(qualificationRequirementsId == null ? null : qualificationRequirementsMap.get(qualificationRequirementsId));
            // nambconfirmation / nambconfirmationdate / nambconfirmationuser: NOT SET - no
            // target column, see class Javadoc.
            // employerid: NOT SET - no target column exists anywhere, see class Javadoc.
            // accountnumber: NOT SET - internal MSSQL row identifier, not needed.

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
