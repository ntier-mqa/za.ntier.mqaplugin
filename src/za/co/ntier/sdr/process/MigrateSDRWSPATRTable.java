package za.co.ntier.sdr.process;

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
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatr into SDR_WSPATR
 * (12,737 source rows). Requires {@link MigrateSDROrganisationTable} to have already run.
 *
 * <p>SDR_SubmittedBy_ID/SDR_ApprovedBy_ID/SDR_RejectedBy_ID/SDR_ManualSubmissionBy_ID/
 * SDR_EvaluatedBy_ID resolve against iDempiere's own AD_User (Search reference) - the raw source int
 * is used directly (0 left unset via setIfPresent's zero-skip).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRTable")
public class MigrateSDRWSPATRTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATR";
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

        MTable table = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (table == null) {
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRWSPATRTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<String, Map<Integer, Integer>> xw = new HashMap<>();
        xw.put("org", SDRMigrationSupport.buildIdCrosswalk("sdr_organisation", "sdr_organisation_id",
                get_TrxName()));
        xw.put("financialyear", SDRMigrationSupport.buildIdCrosswalk("sdr_financialyear",
                "sdr_financialyear_id", get_TrxName()));
        xw.put("wspstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_wspstatus", "sdr_wspstatus_id",
                get_TrxName()));
        xw.put("formtype", SDRMigrationSupport.buildIdCrosswalk("sdr_formtype", "sdr_formtype_id",
                get_TrxName()));
        xw.put("orgtype", SDRMigrationSupport.buildIdCrosswalk("sdr_organisationtype",
                "sdr_organisationtype_id", get_TrxName()));
        addLog("Crosswalks ready.");

        String sql = "SELECT w.* FROM mssdr_wspatr w "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatr s WHERE s.id = w.id) "
                + "ORDER BY w.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer orgId = xw.get("org").get(rs.getInt("organisationid"));
                if (orgId == null) {
                    skippedNoOrg++;
                    continue;
                }
                try {
                    processOneRow(table, rs, orgId, xw);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-wspatr-errors");

        return "Processed " + processed + " mssdr_wspatr row(s): " + created + " SDR_WSPATR created, "
                + skippedNoOrg + " skipped (no matching SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<String, Map<Integer, Integer>> xw)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            setIfPresent(po, "SDR_FinancialYear_ID", SDRMigrationSupport.resolveLookup(xw.get("financialyear"),
                    rs.getInt("financialyearid")));
            setIfPresent(po, "SDR_WSPStatus_ID", SDRMigrationSupport.resolveLookup(xw.get("wspstatus"),
                    rs.getInt("wspstatusid")));
            setIfPresent(po, "SDR_FormType_ID", SDRMigrationSupport.resolveLookup(xw.get("formtype"),
                    rs.getInt("formtypeid")));
            setIfPresent(po, "SDR_OrganisationType_ID", SDRMigrationSupport.resolveLookup(xw.get("orgtype"),
                    rs.getInt("organisationtypeid")));
            setIfPresent(po, "SDR_DueDate", rs.getTimestamp("duedate"));
            setIfPresent(po, "SDR_CreatedDate", rs.getTimestamp("createddate"));
            setIfPresent(po, "SDR_SubmittedDate", rs.getTimestamp("submitteddate"));
            setIfPresent(po, "SDR_SubmittedBy_ID", rs.getInt("submittedby"));
            setIfPresent(po, "SDR_ApprovedDate", rs.getTimestamp("approveddate"));
            setIfPresent(po, "SDR_ApprovedBy_ID", rs.getInt("approvedby"));
            setIfPresent(po, "SDR_RejectedDate", rs.getTimestamp("rejecteddate"));
            setIfPresent(po, "SDR_RejectedBy_ID", rs.getInt("rejectedby"));
            setIfPresent(po, "SDR_ManualSubmissionDate", rs.getTimestamp("manualsubmissiondate"));
            setIfPresent(po, "SDR_ManualSubmissionBy_ID", rs.getInt("manualsubmissionby"));
            setIfPresent(po, "SDR_DateEvaluated", rs.getTimestamp("dateevaluated"));
            setIfPresent(po, "SDR_EvaluatedBy_ID", rs.getInt("evaluatedby"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatr", "sdr_wspatr_id", newId, created, updated,
                        trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private static void setIfPresent(PO po, String columnName, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Integer && (Integer) value == 0) {
            return;
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_wspatr.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny(String fileNamePrefix) {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/" + fileNamePrefix + "-" + ts + ".txt");
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
