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

import za.co.ntier.api.model.X_ZZWorkplaceApproval;

/**
 * Migrates the staged ms_workplaceapproval table into ZZWorkplaceApproval. See "Column
 * Mapping - QCTO Learner Programmes" doc, tab "WorkplaceApproval", and
 * "ZZWorkplaceApproval - New Columns to Add.txt" for the full column-by-column reasoning
 * behind the columns added below (2026-07-16).
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzworkplaceapproval ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>Same generic-PO-setter approach as {@link MigrateMsProviderToZZProvider} for every new
 * Section A/B column (no typed setter exists yet on the generated model class) - see that
 * class's Javadoc for the full reasoning.
 *
 * <p>Reference columns resolved via MigrationSupport.buildIdCrosswalk() against the reference
 * tables AddZZWorkplaceApprovalColumns created or reused. Quality_Assurance_Body_ID and
 * Levy_ID crosswalks resolve against the SAME reference tables Provider uses (built once,
 * shared) - not rebuilt here. C_BPartner_ID (from organisationid) uses the same
 * MigrationSupport.buildOrganisationToBPartnerCrosswalk as Provider.
 *
 * <p>Is_Saica converted via {@link MigrationSupport#flagToYN(Integer)} - same reasoning as
 * Provider's Is_Saica (confirmed 0/null only in the staged data, not the lkpYesNo id
 * convention).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsWorkplaceApprovalToZZWorkplaceApproval")
public class MigrateMsWorkplaceApprovalToZZWorkplaceApproval extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String SOURCE_TABLE = "ms_workplaceapproval";

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzworkplaceapproval WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZWorkplaceApproval row(s)...");
            DB.executeUpdateEx("DELETE FROM zzworkplaceapproval WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, Integer> typeCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_type", "workplace_approval_type_id", get_TrxName());
        Map<Integer, Integer> classCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_class", "workplace_approval_class_id", get_TrxName());
        Map<Integer, Integer> statusCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_status", "workplace_approval_status_id", get_TrxName());
        Map<Integer, Integer> qualityAssuranceBodyCrosswalk = MigrationSupport.buildIdCrosswalk("quality_assurance_body", "quality_assurance_body_id", get_TrxName());
        Map<Integer, Integer> workplaceAccreditationTypeCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_accreditation_type", "workplace_accreditation_type_id", get_TrxName());
        Map<Integer, Integer> applicationCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_application", "workplace_approval_application_id", get_TrxName());
        Map<Integer, Integer> internalExternalCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_internal_external", "workplace_approval_internal_external_id", get_TrxName());
        Map<Integer, Integer> bodyCrosswalk = MigrationSupport.buildIdCrosswalk("workplace_approval_body", "workplace_approval_body_id", get_TrxName());
        Map<Integer, Integer> levyCrosswalk = MigrationSupport.buildIdCrosswalk("levy", "levy_id", get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(get_TrxName());

        String sql = "SELECT id, workplaceapprovalnumber, organisationid, workplaceapprovaltypeid, "
                + "       workplaceapprovalclassid, workplaceapprovalstatusid, workplaceapprovalstartdate, "
                + "       workplaceapprovalenddate, qualityassurancebodyid, dhetregistrationstartdate, "
                + "       dhetregistrationenddate, dhetregistrationnumber, issaica, webaddress, "
                + "       accreditationtypeid, workplaceapprovalcode, workplaceapprovalapplicationid, "
                + "       notapprovedreason, applicationreceiveddate, workplaceapprovalinternalexternalid, "
                + "       workplaceapprovalalertemail, workplaceapprovalbody, workplaceapprovalreviewdate, levy, "
                + "       created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM zzworkplaceapproval z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsWorkplaceApprovalRead");
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
                    processOneRow(rs, typeCrosswalk, classCrosswalk, statusCrosswalk, qualityAssuranceBodyCrosswalk,
                            workplaceAccreditationTypeCrosswalk, applicationCrosswalk, internalExternalCrosswalk,
                            bodyCrosswalk, levyCrosswalk, organisationToBPartnerCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " " + SOURCE_TABLE + " rows (" + created
                            + " ZZWorkplaceApproval created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created
                + " ZZWorkplaceApproval created, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs, Map<Integer, Integer> typeCrosswalk, Map<Integer, Integer> classCrosswalk,
            Map<Integer, Integer> statusCrosswalk, Map<Integer, Integer> qualityAssuranceBodyCrosswalk,
            Map<Integer, Integer> workplaceAccreditationTypeCrosswalk, Map<Integer, Integer> applicationCrosswalk,
            Map<Integer, Integer> internalExternalCrosswalk, Map<Integer, Integer> bodyCrosswalk,
            Map<Integer, Integer> levyCrosswalk, Map<Integer, Integer> organisationToBPartnerCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        String workplaceApprovalNumber = rs.getString("workplaceapprovalnumber");
        Integer organisationId = (Integer) rs.getObject("organisationid");
        Integer workplaceApprovalTypeId = (Integer) rs.getObject("workplaceapprovaltypeid");
        Integer workplaceApprovalClassId = (Integer) rs.getObject("workplaceapprovalclassid");
        Integer workplaceApprovalStatusId = (Integer) rs.getObject("workplaceapprovalstatusid");
        Integer qualityAssuranceBodyId = (Integer) rs.getObject("qualityassurancebodyid");
        Integer issaica = (Integer) rs.getObject("issaica");
        Integer accreditationTypeId = (Integer) rs.getObject("accreditationtypeid");
        Integer workplaceApprovalApplicationId = (Integer) rs.getObject("workplaceapprovalapplicationid");
        Integer workplaceApprovalInternalExternalId = (Integer) rs.getObject("workplaceapprovalinternalexternalid");
        Integer workplaceApprovalBodyId = (Integer) rs.getObject("workplaceapprovalbody");
        Integer levyId = (Integer) rs.getObject("levy");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsWorkplaceApprovalMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZWorkplaceApproval wa = new X_ZZWorkplaceApproval(getCtx(), 0, trxName);
            wa.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            wa.setIsActive(isDeleted == 0);
            wa.setZZWorkplaceApprovalNumber(workplaceApprovalNumber);

            // Section A - plain columns
            setGeneric(wa, "Workplace_Approval_Start_Date", rs.getTimestamp("workplaceapprovalstartdate"));
            setGeneric(wa, "Workplace_Approval_End_Date", rs.getTimestamp("workplaceapprovalenddate"));
            setGeneric(wa, "Dhet_Registration_Start_Date", rs.getTimestamp("dhetregistrationstartdate"));
            setGeneric(wa, "Dhet_Registration_End_Date", rs.getTimestamp("dhetregistrationenddate"));
            setGeneric(wa, "Dhet_Registration_Number", rs.getString("dhetregistrationnumber"));
            setGeneric(wa, "Is_Saica", MigrationSupport.flagToYN(issaica));
            setGeneric(wa, "Web_Address", rs.getString("webaddress"));
            setGeneric(wa, "Workplace_Approval_Code", rs.getString("workplaceapprovalcode"));
            setGeneric(wa, "Not_Approved_Reason", rs.getString("notapprovedreason"));
            setGeneric(wa, "Application_Received_Date", rs.getTimestamp("applicationreceiveddate"));
            setGeneric(wa, "Workplace_Approval_Alert_Email", rs.getString("workplaceapprovalalertemail"));
            setGeneric(wa, "Workplace_Approval_Review_Date", rs.getTimestamp("workplaceapprovalreviewdate"));

            // Section B - reference columns
            setIfResolved(wa, "Workplace_Approval_Type_ID", typeCrosswalk, workplaceApprovalTypeId);
            setIfResolved(wa, "Workplace_Approval_Class_ID", classCrosswalk, workplaceApprovalClassId);
            setIfResolved(wa, "Workplace_Approval_Status_ID", statusCrosswalk, workplaceApprovalStatusId);
            setIfResolved(wa, "Quality_Assurance_Body_ID", qualityAssuranceBodyCrosswalk, qualityAssuranceBodyId);
            setIfResolved(wa, "Workplace_Accreditation_Type_ID", workplaceAccreditationTypeCrosswalk, accreditationTypeId);
            setIfResolved(wa, "Workplace_Approval_Application_ID", applicationCrosswalk, workplaceApprovalApplicationId);
            setIfResolved(wa, "Workplace_Approval_Internal_External_ID", internalExternalCrosswalk, workplaceApprovalInternalExternalId);
            setIfResolved(wa, "Workplace_Approval_Body_ID", bodyCrosswalk, workplaceApprovalBodyId);
            setIfResolved(wa, "Levy_ID", levyCrosswalk, levyId);

            // organisationid -> C_BPartner_ID
            setIfResolved(wa, "C_BPartner_ID", organisationToBPartnerCrosswalk, organisationId);

            wa.saveEx();
            int zzWaId = wa.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzworkplaceapproval", "zzworkplaceapproval_id", zzWaId,
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
        File logFile = new File("/tmp/migrate-ms-workplaceapproval-errors-" + ts + ".txt");
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
