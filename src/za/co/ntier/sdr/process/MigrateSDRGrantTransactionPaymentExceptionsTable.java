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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_granttransactionpaymentexceptions
 * into SDR_GrantTransactionPaymentExceptions - child of both GrantTransactionStatus and
 * GrantTransaction (5,062 source rows). Requires {@link MigrateSDRGrantTransactionTable} and
 * {@link MigrateSDRGrantTransactionStatusTable} to have already run.
 *
 * <p>Unlike GrantTransactionStatus.Creditor, this table's own Creditor column is kept as plain text
 * only (NOT resolved to a SDR_Organisation FK - not independently re-tested for this table, per the
 * mapping doc). SDR_LastUser_ID follows the platform's audit-trail pattern (Search + AD_User) - the
 * raw value already correlates directly to AD_User_ID, so it's a plain passthrough, not a crosswalk.
 *
 * <p>CORRECTED 2026-09-14: unlike every other table in this migration, mssdr_granttransactionpaymentexceptions
 * has NO generic created/updated/isdeleted staging columns at all - confirmed the hard way ("The
 * column name created was not found in this ResultSet" on every single row). This table's mapping doc
 * section only lists its own bespoke LastDateChanged/LastUser audit columns, no isactive-from-isdeleted
 * derivation. Fixed by dropping the created/updated read-and-stamp and the isdeleted-driven
 * setIsActive() call entirely - IsActive is left at its standard 'Y' default.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantTransactionPaymentExceptionsTable")
public class MigrateSDRGrantTransactionPaymentExceptionsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantTransactionPaymentExceptions";
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
                    TABLE_NAME + " does not exist - run AddSDRGrantTransactionPaymentExceptionsTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> grantTransactionStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_granttransactionstatus", "sdr_granttransactionstatus_id", get_TrxName());
        Map<Integer, Integer> grantTransactionCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_granttransaction", "sdr_granttransaction_id", get_TrxName());

        String sql = "SELECT e.* FROM mssdr_granttransactionpaymentexceptions e "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_granttransactionpaymentexceptions s WHERE s.id = e.id) "
                + "ORDER BY e.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoParent = 0;
        String readTrxName = Trx.createTrxName("SDRGrantTransactionPaymentExceptionsRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer grantTransactionStatusId = grantTransactionStatusCrosswalk.get(
                        rs.getInt("granttransactionstatusid"));
                Integer grantTransactionId = grantTransactionCrosswalk.get(rs.getInt("granttransactionid"));
                if (grantTransactionStatusId == null || grantTransactionId == null) {
                    skippedNoParent++;
                    continue;
                }
                try {
                    processOneRow(table, rs, grantTransactionStatusId, grantTransactionId);
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

        writeErrorLogIfAny("migrate-sdr-granttransactionpaymentexceptions-errors");

        return "Processed " + processed + " mssdr_granttransactionpaymentexceptions row(s): " + created + " "
                + "SDR_GrantTransactionPaymentExceptions created, " + skippedNoParent + " skipped (no matching "
                + "parent), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int grantTransactionStatusId, int grantTransactionId)
            throws Exception {
        int sourceId = rs.getInt("id");

        String trxName = Trx.createTrxName("SDRGrantTransactionPaymentExceptionsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_GrantTransactionStatus_ID", grantTransactionStatusId);
            po.set_ValueOfColumn("SDR_GrantTransaction_ID", grantTransactionId);

            setIfPresent(po, "SDR_Creditor", rs.getString("creditor"));
            setIfPresent(po, "SDR_DocumentNumber", rs.getString("documentnumber"));
            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            po.set_ValueOfColumn("SDR_StatusCode", SDRMigrationSupport.toBD(rs.getInt("statuscode")));
            setIfPresent(po, "SDR_StatusDescription", rs.getString("statusdescription"));
            setIfPresent(po, "SDR_ErrorCode", SDRMigrationSupport.toBD(rs.getInt("errorcode")));
            setIfPresent(po, "SDR_ErrorDescription", rs.getString("errordescription"));
            setIfPresent(po, "SDR_LastDateChanged", rs.getTimestamp("lastdatechanged"));
            setIfPresent(po, "SDR_LastUser_ID", rs.getInt("lastuserid"));

            po.saveEx();

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
        if (value instanceof Integer && (Integer) value == 0) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_granttransactionpaymentexceptions.id=" + sourceId + ": " + e.getMessage());
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
