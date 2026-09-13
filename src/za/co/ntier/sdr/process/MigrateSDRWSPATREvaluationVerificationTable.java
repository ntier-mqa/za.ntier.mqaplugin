package za.co.ntier.sdr.process;

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
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrevaluationverification
 * into SDR_WSPATREvaluationVerification (3,195 source rows). Requires {@link MigrateSDRWSPATRTable} to
 * have already run. SDR_VerifiedBy_ID/SDR_EvaluatedBy_ID/SDR_BoardApprovalBy_ID resolve against
 * iDempiere's own AD_User (Search reference).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATREvaluationVerificationTable")
public class MigrateSDRWSPATREvaluationVerificationTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATREvaluationVerification";
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
            throw new IllegalStateException(
                    TABLE_NAME + " does not exist - run AddSDRWSPATREvaluationVerificationTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> verificationStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspatrevaluationverificationstatus", "sdr_wspatrevaluationverificationstatus_id",
                get_TrxName());
        Map<Integer, Integer> evaluationStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspatrevaluationstatus", "sdr_wspatrevaluationstatus_id", get_TrxName());
        Map<Integer, Integer> approvalStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspatrevaluationapprovalstatus", "sdr_wspatrevaluationapprovalstatus_id", get_TrxName());

        String sql = "SELECT e.* FROM mssdr_wspatrevaluationverification e "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrevaluationverification s WHERE s.id = e.id) "
                + "ORDER BY e.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATREvalVerifRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer wspatrId = wspatrCrosswalk.get(rs.getInt("wspatrid"));
                if (wspatrId == null) {
                    skippedNoWspatr++;
                    continue;
                }
                try {
                    processOneRow(table, rs, wspatrId, verificationStatusCrosswalk, evaluationStatusCrosswalk,
                            approvalStatusCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-wspatrevaluationverification-errors");

        return "Processed " + processed + " mssdr_wspatrevaluationverification row(s): " + created + " "
                + "SDR_WSPATREvaluationVerification created, " + skippedNoWspatr + " skipped (no matching "
                + "SDR_WSPATR), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId,
            Map<Integer, Integer> verificationStatusCrosswalk, Map<Integer, Integer> evaluationStatusCrosswalk,
            Map<Integer, Integer> approvalStatusCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATREvalVerifMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_WSPATR_ID", wspatrId);

            setIfPresent(po, "SDR_WSPATREvaluationVerificationStatus_ID", SDRMigrationSupport.resolveLookup(
                    verificationStatusCrosswalk, rs.getInt("wspatrevaluationverificationstatusid")));
            setIfPresent(po, "SDR_WSPATREvaluationStatus_ID", SDRMigrationSupport.resolveLookup(
                    evaluationStatusCrosswalk, rs.getInt("wspatrevaluationstatusid")));
            setIfPresent(po, "SDR_WSPATREvaluationApprovalStatus_ID", SDRMigrationSupport.resolveLookup(
                    approvalStatusCrosswalk, rs.getInt("wspatrevaluationapprovalstatusid")));
            setIfPresent(po, "SDR_VerificationComment", rs.getString("verificationcomment"));
            setIfPresent(po, "SDR_EvaluationComment", rs.getString("evaluationcomment"));
            setIfPresent(po, "SDR_ApprovalComment", rs.getString("approvalcomment"));
            setIfPresent(po, "SDR_VerifiedBy_ID", rs.getInt("verifiedby"));
            setIfPresent(po, "SDR_DateVerified", rs.getTimestamp("dateverified"));
            setIfPresent(po, "SDR_EvaluatedBy_ID", rs.getInt("evaluatedby"));
            setIfPresent(po, "SDR_DateEvaluated", rs.getTimestamp("dateevaluated"));
            setIfPresent(po, "SDR_BoardApprovalBy_ID", rs.getInt("boardapprovalby"));
            setIfPresent(po, "SDR_DateBoardApproval", rs.getTimestamp("dateboardapproval"));
            setIfPresent(po, "SDR_BulkBoardApprovalDate", rs.getTimestamp("bulkboardapprovaldate"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrevaluationverification",
                        "sdr_wspatrevaluationverification_id", newId, created, updated, trxName);
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
            errors.add("mssdr_wspatrevaluationverification.id=" + sourceId + ": " + e.getMessage());
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
