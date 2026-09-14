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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levytransactionstatus into
 * SDR_LevyTransactionStatus - child of LevyTransaction (87,008 source rows). Requires
 * {@link MigrateSDRLevyTransactionTable} to have already run. The source Creditor column is DROPPED
 * entirely (always blank, per user decision). SDR_PostingStatus/SDR_ErrorCode are UNMAPPED (constant 0
 * across all rows) and carried as plain integers.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyTransactionStatusTable")
public class MigrateSDRLevyTransactionStatusTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyTransactionStatus";
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
                    TABLE_NAME + " does not exist - run AddSDRLevyTransactionStatusTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> levyTransactionCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_levytransaction",
                "sdr_levytransaction_id", get_TrxName());

        String sql = "SELECT t.* FROM mssdr_levytransactionstatus t "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_levytransactionstatus s WHERE s.id = t.id) "
                + "ORDER BY t.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoLevyTransaction = 0;
        String readTrxName = Trx.createTrxName("SDRLevyTransactionStatusRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer levyTransactionId = levyTransactionCrosswalk.get(rs.getInt("levytransactionid"));
                if (levyTransactionId == null) {
                    skippedNoLevyTransaction++;
                    continue;
                }
                try {
                    processOneRow(table, rs, levyTransactionId);
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

        writeErrorLogIfAny("migrate-sdr-levytransactionstatus-errors");

        return "Processed " + processed + " mssdr_levytransactionstatus row(s): " + created + " "
                + "SDR_LevyTransactionStatus created, " + skippedNoLevyTransaction + " skipped (no matching "
                + "SDR_LevyTransaction), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int levyTransactionId) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRLevyTransactionStatusMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_LevyTransaction_ID", levyTransactionId);

            setIfPresent(po, "SDR_DocumentNumber", rs.getString("documentnumber"));
            po.set_ValueOfColumn("SDR_PostingStatus", SDRMigrationSupport.toBD(rs.getInt("postingstatus")));
            setIfPresent(po, "SDR_PostingDescription", rs.getString("postingdescription"));
            po.set_ValueOfColumn("SDR_ErrorCode", SDRMigrationSupport.toBD(rs.getInt("errorcode")));
            setIfPresent(po, "SDR_ErrorDescription", rs.getString("errordescription"));
            setIfPresent(po, "SDR_FromDateTime", rs.getTimestamp("fromdatetime"));
            setIfPresent(po, "SDR_ToDateTime", rs.getTimestamp("todatetime"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_levytransactionstatus",
                        "sdr_levytransactionstatus_id", newId, created, updated, trxName);
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
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_levytransactionstatus.id=" + sourceId + ": " + e.getMessage());
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
