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
import org.compiere.model.MProcessPara;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZAssessmentCentre;

/**
 * Migrates the staged ms_assessmentcentre table into ZZAssessmentCentre. See "Column Mapping
 * - QCTO Learner Programmes" doc, tab "AssessmentCentre", and
 * "ZZAssessmentCentre - New Columns to Add.txt" for the full column-by-column reasoning
 * behind the columns added below (2026-07-16).
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzassessmentcentre ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>Same generic-PO-setter approach as {@link MigrateMsProviderToZZProvider} for every new
 * Section A/B column - see that class's Javadoc for the full reasoning.
 *
 * <p>5 of AssessmentCentre's 10 reference columns REUSE the exact same reference tables
 * Provider/WorkplaceApproval already built (same MSSQL source table) rather than a separate
 * one - Quality_Assurance_Body_ID, Saqa_QA_ID, Workplace_Accreditation_Type_ID,
 * Accrediting_Council_ID, Levy_ID - see AddZZAssessmentCentreColumns' Javadoc for why. Saqa_QA_ID
 * uses MigrationSupport.buildValueCrosswalk (matched by ETQAID text), same as Provider's.
 * C_BPartner_ID (from organisationid) uses the same MigrationSupport.
 * buildOrganisationToBPartnerCrosswalk as Provider/WorkplaceApproval.
 *
 * <p>Is_Saica converted via {@link MigrationSupport#flagToYN(Integer)} - same reasoning as
 * Provider's Is_Saica.
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsAssessmentCentreToZZAssessmentCentre")
public class MigrateMsAssessmentCentreToZZAssessmentCentre extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String SOURCE_TABLE = "ms_assessmentcentre";

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

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzassessmentcentre WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZAssessmentCentre row(s)...");
            DB.executeUpdateEx("DELETE FROM zzassessmentcentre WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, Integer> typeCrosswalk = MigrationSupport.buildIdCrosswalk("assessmentcentre_type", "assessmentcentre_type_id", get_TrxName());
        Map<Integer, Integer> classCrosswalk = MigrationSupport.buildIdCrosswalk("assessmentcentre_class", "assessmentcentre_class_id", get_TrxName());
        Map<Integer, Integer> accreditationStatusCrosswalk = MigrationSupport.buildIdCrosswalk("assessmentcentre_accreditation_status", "assessmentcentre_accreditation_status_id", get_TrxName());
        Map<Integer, Integer> qualityAssuranceBodyCrosswalk = MigrationSupport.buildIdCrosswalk("quality_assurance_body", "quality_assurance_body_id", get_TrxName());
        Map<String, Integer> saqaQaCrosswalk = MigrationSupport.buildValueCrosswalk("saqa_qa", "saqa_qa_id", get_TrxName());
        Map<Integer, Integer> applicationCrosswalk = MigrationSupport.buildIdCrosswalk("assessmentcentre_application", "assessmentcentre_application_id", get_TrxName());
        Map<Integer, Integer> workplaceAccreditationTypeCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_accreditation_type", "workplace_accreditation_type_id", get_TrxName());
        Map<Integer, Integer> internalExternalCrosswalk = MigrationSupport.buildIdCrosswalk("assessmentcentre_internal_external", "assessmentcentre_internal_external_id", get_TrxName());
        Map<Integer, Integer> accreditingCouncilCrosswalk = MigrationSupport.buildIdCrosswalk("accrediting_council", "accrediting_council_id", get_TrxName());
        Map<Integer, Integer> levyCrosswalk = MigrationSupport.buildIdCrosswalk("levy", "levy_id", get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(get_TrxName());

        String sql = "SELECT id, qctoassessmentcentrenumber, organisationid, assessmentcentretypeid, "
                + "       assessmentcentreclassid, assessmentcentreaccreditationstatusid, accreditationstartdate, "
                + "       accreditationenddate, assessmentcentreaccreditationnumber, qualityassurancebodyid, "
                + "       saqacode, saqaprovidercode, dhetregistrationstartdate, dhetregistrationenddate, "
                + "       dhetregistrationnumber, issaica, webaddress, saqaqaid, assessmentcentrecode, "
                + "       assessmentcentreapplicationid, accreditationtypeid, applicationreceiveddate, "
                + "       assessmentcentreinternalexternalid, assessmentcentrealertemail, accreditingcouncil, "
                + "       accreditingcouncilother, accreditationreviewdate, levy, statuseffectivedate, "
                + "       created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM zzassessmentcentre z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsAssessmentCentreRead");
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
                    processOneRow(rs, typeCrosswalk, classCrosswalk, accreditationStatusCrosswalk,
                            qualityAssuranceBodyCrosswalk, saqaQaCrosswalk, applicationCrosswalk,
                            workplaceAccreditationTypeCrosswalk, internalExternalCrosswalk, accreditingCouncilCrosswalk,
                            levyCrosswalk, organisationToBPartnerCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " " + SOURCE_TABLE + " rows (" + created
                            + " ZZAssessmentCentre created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created
                + " ZZAssessmentCentre created, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs, Map<Integer, Integer> typeCrosswalk, Map<Integer, Integer> classCrosswalk,
            Map<Integer, Integer> accreditationStatusCrosswalk, Map<Integer, Integer> qualityAssuranceBodyCrosswalk,
            Map<String, Integer> saqaQaCrosswalk, Map<Integer, Integer> applicationCrosswalk,
            Map<Integer, Integer> workplaceAccreditationTypeCrosswalk, Map<Integer, Integer> internalExternalCrosswalk,
            Map<Integer, Integer> accreditingCouncilCrosswalk, Map<Integer, Integer> levyCrosswalk,
            Map<Integer, Integer> organisationToBPartnerCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        String qctoAssessmentCentreNumber = rs.getString("qctoassessmentcentrenumber");
        Integer organisationId = (Integer) rs.getObject("organisationid");
        Integer assessmentCentreTypeId = (Integer) rs.getObject("assessmentcentretypeid");
        Integer assessmentCentreClassId = (Integer) rs.getObject("assessmentcentreclassid");
        Integer assessmentCentreAccreditationStatusId = (Integer) rs.getObject("assessmentcentreaccreditationstatusid");
        Integer qualityAssuranceBodyId = (Integer) rs.getObject("qualityassurancebodyid");
        Integer issaica = (Integer) rs.getObject("issaica");
        Integer saqaQaId = (Integer) rs.getObject("saqaqaid");
        Integer assessmentCentreApplicationId = (Integer) rs.getObject("assessmentcentreapplicationid");
        Integer accreditationTypeId = (Integer) rs.getObject("accreditationtypeid");
        Integer assessmentCentreInternalExternalId = (Integer) rs.getObject("assessmentcentreinternalexternalid");
        Integer accreditingCouncilId = (Integer) rs.getObject("accreditingcouncil");
        Integer levyId = (Integer) rs.getObject("levy");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsAssessmentCentreMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZAssessmentCentre ac = new X_ZZAssessmentCentre(getCtx(), 0, trxName);
            ac.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            ac.setIsActive(isDeleted == 0);
            ac.setZZQctoAssessmentCentreNumber(qctoAssessmentCentreNumber);

            // Section A - plain columns
            setGeneric(ac, "Accreditation_Start_Date", rs.getTimestamp("accreditationstartdate"));
            setGeneric(ac, "Accreditation_End_Date", rs.getTimestamp("accreditationenddate"));
            setGeneric(ac, "Assessment_Centre_Accreditation_Number", rs.getString("assessmentcentreaccreditationnumber"));
            setGeneric(ac, "Saqa_Code", rs.getString("saqacode"));
            setGeneric(ac, "Saqa_Provider_Code", rs.getString("saqaprovidercode"));
            setGeneric(ac, "Dhet_Registration_Start_Date", rs.getTimestamp("dhetregistrationstartdate"));
            setGeneric(ac, "Dhet_Registration_End_Date", rs.getTimestamp("dhetregistrationenddate"));
            setGeneric(ac, "Dhet_Registration_Number", rs.getString("dhetregistrationnumber"));
            setGeneric(ac, "Is_Saica", MigrationSupport.flagToYN(issaica));
            setGeneric(ac, "Web_Address", rs.getString("webaddress"));
            setGeneric(ac, "Assessment_Centre_Code", rs.getString("assessmentcentrecode"));
            setGeneric(ac, "Application_Received_Date", rs.getTimestamp("applicationreceiveddate"));
            setGeneric(ac, "Assessment_Centre_Alert_Email", rs.getString("assessmentcentrealertemail"));
            setGeneric(ac, "Accrediting_Council_Other", rs.getString("accreditingcouncilother"));
            setGeneric(ac, "Accreditation_Review_Date", rs.getTimestamp("accreditationreviewdate"));
            setGeneric(ac, "Status_Effective_Date", rs.getTimestamp("statuseffectivedate"));

            // Section B - reference columns
            setIfResolved(ac, "AssessmentCentre_Type_ID", typeCrosswalk, assessmentCentreTypeId);
            setIfResolved(ac, "AssessmentCentre_Class_ID", classCrosswalk, assessmentCentreClassId);
            setIfResolved(ac, "AssessmentCentre_Accreditation_Status_ID", accreditationStatusCrosswalk, assessmentCentreAccreditationStatusId);
            setIfResolved(ac, "Quality_Assurance_Body_ID", qualityAssuranceBodyCrosswalk, qualityAssuranceBodyId);
            setIfResolvedByValue(ac, "Saqa_QA_ID", saqaQaCrosswalk, saqaQaId == null ? null : String.valueOf(saqaQaId));
            setIfResolved(ac, "AssessmentCentre_Application_ID", applicationCrosswalk, assessmentCentreApplicationId);
            setIfResolved(ac, "Workplace_Accreditation_Type_ID", workplaceAccreditationTypeCrosswalk, accreditationTypeId);
            setIfResolved(ac, "AssessmentCentre_Internal_External_ID", internalExternalCrosswalk, assessmentCentreInternalExternalId);
            setIfResolved(ac, "Accrediting_Council_ID", accreditingCouncilCrosswalk, accreditingCouncilId);
            setIfResolved(ac, "Levy_ID", levyCrosswalk, levyId);

            // organisationid -> C_BPartner_ID
            setIfResolved(ac, "C_BPartner_ID", organisationToBPartnerCrosswalk, organisationId);

            ac.saveEx();
            int zzAcId = ac.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzassessmentcentre", "zzassessmentcentre_id", zzAcId,
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

    private static void setGeneric(PO po, String columnName, Object value) {
        if (value != null) {
            po.set_ValueOfColumn(columnName, value);
        }
    }

    private static void setIfResolved(PO po, String columnName, Map<Integer, Integer> crosswalk, Integer sourceId) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            po.set_ValueOfColumn(columnName, targetId);
        }
    }

    private static void setIfResolvedByValue(PO po, String columnName, Map<String, Integer> crosswalk, String sourceValue) {
        if (sourceValue == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceValue.trim());
        if (targetId != null) {
            po.set_ValueOfColumn(columnName, targetId);
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add(SOURCE_TABLE + ".id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-assessmentcentre-errors-" + ts + ".txt");
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
