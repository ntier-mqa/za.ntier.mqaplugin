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

import za.co.ntier.api.model.X_ZZLearnerQCTOSkillsProgramme;

/**
 * Migrates the staged ms_learnerqctoskillsprogramme table (each learner's enrolment in a
 * QCTO skills programme) into ZZLearnerQCTOSkillsProgramme. This is one of the 3 original
 * tables requested. See "Column Mapping - QCTO Learner Programmes" doc, tab
 * "LearnerQCTOSkillsProgramme" for the full column-by-column reasoning.
 *
 * <p>Must run AFTER: MigrateMsPersonToZZPerson/MigrateMsLearnerToZZLearner (ms_learner_xref),
 * MigrateMsQctoSkillsProgrammeToZZQctoSkillsProgramme, MigrateMsQctoProgrammeStatusToZZQctoProgrammeStatus,
 * MigrateMsProviderToZZProvider, MigrateMsWorkplaceApprovalToZZWorkplaceApproval,
 * MigrateMsAssessmentCentreToZZAssessmentCentre, MigrateMsGrantTypeToZZGrantType - every FK
 * column below depends on one of those crosswalks.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzlearnerqctoskillsprogramme ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>AD_User actor columns (certificatecreatedby, endorsementcreatedby, terminatedcapturedby,
 * extensioncapturedby, registeredby, endorsedby) are resolved via ms_user.email -&gt;
 * ad_user.email, link-only - NEVER creates an AD_User, same rule as zzperson.ad_user_id.
 *
 * <p>NOT handled (all documented as "Unmapped" in the mapping doc):
 * <ul>
 *   <li>isapproved / approvedby / dateapproved - no target column.</li>
 *   <li>accountnumber - internal MSSQL row identifier, not needed.</li>
 *   <li>employerid - no target column exists anywhere for this on any of the 3 join
 *       tables.</li>
 *   <li>financialyearid -&gt; ZZFinancialYear_ID - FinancialYear-to-C_Year crosswalk not
 *       yet designed (Open Item #3 in the mapping doc).</li>
 *   <li>ZZLearnerLP (target-only column) - no source column identified for this yet.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerQctoSkillsProgrammeToZZLearnerQctoSkillsProgramme")
public class MigrateMsLearnerQctoSkillsProgrammeToZZLearnerQctoSkillsProgramme extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzlearnerqctoskillsprogramme WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZLearnerQCTOSkillsProgramme row(s)...");
            DB.executeUpdateEx("DELETE FROM zzlearnerqctoskillsprogramme WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, Integer> learnerCrosswalk = loadLearnerCrosswalk();
        Map<Integer, Integer> qctoSkillsProgrammeCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctoskillsprogramme", "zzqctoskillsprogramme_id", get_TrxName());
        Map<Integer, Integer> qctoProgrammeStatusCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctoprogrammestatus", "zzqctoprogrammestatus_id", get_TrxName());
        Map<Integer, Integer> providerCrosswalk = MigrationSupport.buildIdCrosswalk("zzprovider", "zzprovider_id", get_TrxName());
        Map<Integer, Integer> waCrosswalk = MigrationSupport.buildIdCrosswalk("zzworkplaceapproval", "zzworkplaceapproval_id", get_TrxName());
        Map<Integer, Integer> acCrosswalk = MigrationSupport.buildIdCrosswalk("zzassessmentcentre", "zzassessmentcentre_id", get_TrxName());
        Map<Integer, Integer> grantTypeCrosswalk = MigrationSupport.buildIdCrosswalk("zzgranttype", "zzgranttype_id", get_TrxName());
        Map<Integer, Integer> msUserToAdUser = MigrationSupport.buildMsUserToAdUserCrosswalk(get_TrxName());
        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildDescriptionMap("ms_lkpsocioeconomicstatus", get_TrxName());
        Map<Integer, String> sponsorshipMap = MigrationSupport.buildDescriptionMap("ms_lkpsponsorship", get_TrxName());
        Map<Integer, String> projectMap = MigrationSupport.buildDescriptionMap("ms_lkpproject", get_TrxName());
        Map<Integer, String> reasonForReprintMap = MigrationSupport.buildDescriptionMap("ms_lkpreasonforreprint", get_TrxName());
        Map<Integer, String> enrolmentStatusReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpenrolmentstatusreason", get_TrxName());
        Map<Integer, String> terminationReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpterminationreason", get_TrxName());

        String sql =
                "SELECT id, learnerid, qctoskillsprogrammeid, qctoskillsprogrammereferencenumber, "
                + "       commencementdate, completiondate, contractnumber, qctoprogrammestatusid, "
                + "       socioeconomicstatusid, sponsorshipid, projectid, sdproviderid, waid, acid, "
                + "       certificatenumber, certificatecreatedby, datecertificatecreated, "
                + "       certificatereasonforreprintid, certificateprintingerrorreason, "
                + "       statuseffectivedate, studentnumber, endorsementnumber, endorsementcreatedby, "
                + "       dateendorsementcreated, endorsementreasonforreprintid, "
                + "       endorsementprintingerrorreason, extensiondate, extensionreason, terminationdate, "
                + "       terminationreason, terminationreasonid, terminatedcapturedby, "
                + "       dateterminationcaptured, extensioncapturedby, dateextensioncaptured, "
                + "       registrationdate, registeredby, enrolmentstatusreasonid, "
                + "       mostrecentregistrationdate, isendorsed, endorsedby, dateendorsed, "
                + "       registrationnumber, estimatecompletiondate, granttypeid, created, updated, isdeleted "
                + "FROM ms_learnerqctoskillsprogramme "
                + "WHERE NOT EXISTS (SELECT 1 FROM zzlearnerqctoskillsprogramme z WHERE z.id = ms_learnerqctoskillsprogramme.id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerQctoSkillsProgrammeRead");
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
                    processOneRow(rs, learnerCrosswalk, qctoSkillsProgrammeCrosswalk, qctoProgrammeStatusCrosswalk,
                            providerCrosswalk, waCrosswalk, acCrosswalk, grantTypeCrosswalk, msUserToAdUser,
                            socioEconomicStatusMap, sponsorshipMap, projectMap, reasonForReprintMap,
                            enrolmentStatusReasonMap, terminationReasonMap);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_learnerqctoskillsprogramme rows (" + created
                            + " ZZLearnerQCTOSkillsProgramme created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_learnerqctoskillsprogramme row(s): " + created
                + " ZZLearnerQCTOSkillsProgramme created, " + errors.size() + " error(s).";
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
            Map<Integer, Integer> qctoSkillsProgrammeCrosswalk, Map<Integer, Integer> qctoProgrammeStatusCrosswalk,
            Map<Integer, Integer> providerCrosswalk, Map<Integer, Integer> waCrosswalk,
            Map<Integer, Integer> acCrosswalk, Map<Integer, Integer> grantTypeCrosswalk,
            Map<Integer, Integer> msUserToAdUser, Map<Integer, String> socioEconomicStatusMap,
            Map<Integer, String> sponsorshipMap, Map<Integer, String> projectMap,
            Map<Integer, String> reasonForReprintMap, Map<Integer, String> enrolmentStatusReasonMap,
            Map<Integer, String> terminationReasonMap) throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerId = (Integer) rs.getObject("learnerid");
        Integer qctoSkillsProgrammeId = (Integer) rs.getObject("qctoskillsprogrammeid");
        Integer qctoProgrammeStatusId = (Integer) rs.getObject("qctoprogrammestatusid");
        Integer socioEconomicStatusId = (Integer) rs.getObject("socioeconomicstatusid");
        Integer sponsorshipId = (Integer) rs.getObject("sponsorshipid");
        Integer projectId = (Integer) rs.getObject("projectid");
        Integer sdProviderId = (Integer) rs.getObject("sdproviderid");
        Integer waId = (Integer) rs.getObject("waid");
        Integer acId = (Integer) rs.getObject("acid");
        Integer certificateCreatedBy = (Integer) rs.getObject("certificatecreatedby");
        Integer certificateReasonForReprintId = (Integer) rs.getObject("certificatereasonforreprintid");
        Integer endorsementCreatedBy = (Integer) rs.getObject("endorsementcreatedby");
        Integer endorsementReasonForReprintId = (Integer) rs.getObject("endorsementreasonforreprintid");
        Integer terminationReasonId = (Integer) rs.getObject("terminationreasonid");
        Integer terminatedCapturedBy = (Integer) rs.getObject("terminatedcapturedby");
        Integer extensionCapturedBy = (Integer) rs.getObject("extensioncapturedby");
        Integer registeredBy = (Integer) rs.getObject("registeredby");
        Integer enrolmentStatusReasonId = (Integer) rs.getObject("enrolmentstatusreasonid");
        Integer isEndorsed = (Integer) rs.getObject("isendorsed");
        Integer endorsedBy = (Integer) rs.getObject("endorsedby");
        Integer grantTypeId = (Integer) rs.getObject("granttypeid");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        if (learnerId == null || learnerCrosswalk.get(learnerId) == null) {
            throw new AdempiereException(
                    "No matching ZZLearner for learnerid=" + learnerId + " (person/learner not migrated yet?)");
        }

        String trxName = Trx.createTrxName("MsLearnerQctoSkillsProgrammeMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZLearnerQCTOSkillsProgramme skillsProgramme = new X_ZZLearnerQCTOSkillsProgramme(getCtx(), 0, trxName);
            skillsProgramme.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            skillsProgramme.setIsActive(isDeleted == 0);
            skillsProgramme.setZZLearner_ID(learnerCrosswalk.get(learnerId));
            setIfResolved(qctoSkillsProgrammeCrosswalk, qctoSkillsProgrammeId, skillsProgramme::setZZQctoSkillsProgramme_ID);
            skillsProgramme.setZZQctoSkillsProgrammeReferenceNumber(rs.getString("qctoskillsprogrammereferencenumber"));
            setTimestamp(rs.getTimestamp("commencementdate"), skillsProgramme::setZZCommencementDate);
            setTimestamp(rs.getTimestamp("completiondate"), skillsProgramme::setZZCompletionDate);
            skillsProgramme.setZZContractNumber(rs.getString("contractnumber"));
            setIfResolved(qctoProgrammeStatusCrosswalk, qctoProgrammeStatusId, skillsProgramme::setZZQctoProgrammeStatus_ID);
            skillsProgramme.setZZSocioEconomicStatus(socioEconomicStatusId == null ? null : socioEconomicStatusMap.get(socioEconomicStatusId));
            skillsProgramme.setZZSponsorship(sponsorshipId == null ? null : sponsorshipMap.get(sponsorshipId));
            skillsProgramme.setZZProject(projectId == null ? null : projectMap.get(projectId));
            // ZZFinancialYear_ID (financialyearid): NOT SET - see class Javadoc.
            setIfResolved(providerCrosswalk, sdProviderId, skillsProgramme::setZZSDProvider_ID);
            setIfResolved(waCrosswalk, waId, skillsProgramme::setZZWA_ID);
            setIfResolved(acCrosswalk, acId, skillsProgramme::setZZAC_ID);
            // isapproved / approvedby / dateapproved: NOT SET - no target column, see class Javadoc.
            skillsProgramme.setZZCertificateNumber(rs.getString("certificatenumber"));
            setIfResolved(msUserToAdUser, certificateCreatedBy, skillsProgramme::setZZCertificateCreatedBy);
            setTimestamp(rs.getTimestamp("datecertificatecreated"), skillsProgramme::setZZDateCertificateCreated);
            skillsProgramme.setZZCertificateReasonForReprint(certificateReasonForReprintId == null ? null : reasonForReprintMap.get(certificateReasonForReprintId));
            skillsProgramme.setZZCertificatePrintingErrorReason(rs.getString("certificateprintingerrorreason"));
            setTimestamp(rs.getTimestamp("statuseffectivedate"), skillsProgramme::setZZStatusEffectiveDate);
            skillsProgramme.setZZStudentNumber(rs.getString("studentnumber"));
            skillsProgramme.setZZEndorsementNumber(rs.getString("endorsementnumber"));
            setIfResolved(msUserToAdUser, endorsementCreatedBy, skillsProgramme::setZZEndorsementCreatedBy);
            setTimestamp(rs.getTimestamp("dateendorsementcreated"), skillsProgramme::setZZDateEndorsementCreated);
            skillsProgramme.setZZEndorsementReasonForReprint(endorsementReasonForReprintId == null ? null : reasonForReprintMap.get(endorsementReasonForReprintId));
            skillsProgramme.setZZEndorsementPrintingErrorReason(rs.getString("endorsementprintingerrorreason"));
            setTimestamp(rs.getTimestamp("extensiondate"), skillsProgramme::setZZExtensionDate);
            skillsProgramme.setZZExtensionReason(rs.getString("extensionreason"));
            setTimestamp(rs.getTimestamp("terminationdate"), skillsProgramme::setZZTerminationDate);
            skillsProgramme.setZZTerminationReason(rs.getString("terminationreason"));
            skillsProgramme.setZZTerminationReasonText(terminationReasonId == null ? null : terminationReasonMap.get(terminationReasonId));
            setIfResolved(msUserToAdUser, terminatedCapturedBy, skillsProgramme::setZZTerminatedCapturedBy);
            setTimestamp(rs.getTimestamp("dateterminationcaptured"), skillsProgramme::setZZDateTerminationCaptured);
            setIfResolved(msUserToAdUser, extensionCapturedBy, skillsProgramme::setZZExtensionCapturedBy);
            setTimestamp(rs.getTimestamp("dateextensioncaptured"), skillsProgramme::setZZDateExtensionCaptured);
            setTimestamp(rs.getTimestamp("registrationdate"), skillsProgramme::setZZRegistrationDate);
            setIfResolved(msUserToAdUser, registeredBy, skillsProgramme::setZZRegisteredBy);
            skillsProgramme.setZZEnrolmentStatusReason(enrolmentStatusReasonId == null ? null : enrolmentStatusReasonMap.get(enrolmentStatusReasonId));
            setTimestamp(rs.getTimestamp("mostrecentregistrationdate"), skillsProgramme::setZZMostRecentRegistrationDate);
            skillsProgramme.setZZEndorsed(MigrationSupport.flagToYN(isEndorsed));
            setIfResolved(msUserToAdUser, endorsedBy, skillsProgramme::setZZEndorsedBy);
            setTimestamp(rs.getTimestamp("dateendorsed"), skillsProgramme::setZZDateEndorsed);
            skillsProgramme.setZZRegistrationNumber(rs.getString("registrationnumber"));
            setTimestamp(rs.getTimestamp("estimatecompletiondate"), skillsProgramme::setZZEstimateCompletionDate);
            setIfResolved(grantTypeCrosswalk, grantTypeId, skillsProgramme::setZZGrantType_ID);
            // ZZLearnerLP: NOT SET - no source column identified yet, see class Javadoc.
            // employerid: NOT SET - no target column exists anywhere, see class Javadoc.
            // accountnumber: NOT SET - internal MSSQL row identifier, not needed.

            skillsProgramme.saveEx();
            int zzId = skillsProgramme.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzlearnerqctoskillsprogramme", "zzlearnerqctoskillsprogramme_id", zzId,
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
            errors.add("ms_learnerqctoskillsprogramme.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learnerqctoskillsprogramme-errors-" + ts + ".txt");
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
