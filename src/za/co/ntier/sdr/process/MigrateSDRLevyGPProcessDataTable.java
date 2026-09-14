package za.co.ntier.sdr.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levygpprocessdata into
 * SDR_LevyGPProcessData (2,225 source rows) - stage only, no read-only window built.
 *
 * <p>UNIQUE in this migration: the source table has no "id" column at all and nothing else
 * references it, so (per AddSDRLevyGPProcessDataTable's Javadoc) the target's PK is a bare generated
 * sequence with no recon/hash key - the usual "WHERE NOT EXISTS (... WHERE s.id = source.id)"
 * idempotency pattern can't apply here (every row's recon "id" column would stay 0, so the standard
 * pattern would only ever allow ONE row through on a re-run). Instead this process is idempotent at
 * the whole-table level: it refuses to run again if SDR_LevyGPProcessData already has any rows. The
 * source also has no isdeleted column - CONFIRMED always active, so IsActive is left at its default.
 * SDR_TransactionID/SDR_ProcessID are both UNMAPPED plain integers (0% match against LevyProcess.ID).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyGPProcessDataTable")
public class MigrateSDRLevyGPProcessDataTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyGPProcessData";
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
                    TABLE_NAME + " does not exist - run AddSDRLevyGPProcessDataTable first");
        }

        int existingCount = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM sdr_levygpprocessdata");
        if (existingCount > 0) {
            return TABLE_NAME + " already has " + existingCount + " row(s) - skipping. This table has no "
                    + "per-row recon key, so it can only be migrated once (see class Javadoc).";
        }

        String sql = "SELECT * FROM mssdr_levygpprocessdata" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRLevyGPProcessDataRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(table, rs);
                    created++;
                } catch (Exception e) {
                    logError(processed, e);
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-levygpprocessdata-errors");

        return "Processed " + processed + " mssdr_levygpprocessdata row(s): " + created + " "
                + "SDR_LevyGPProcessData created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs) throws Exception {
        String trxName = Trx.createTrxName("SDRLevyGPProcessDataMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);

            po.set_ValueOfColumn("SDR_TransactionID", SDRMigrationSupport.toBD(rs.getInt("transactionid")));
            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            setIfPresent(po, "SDR_TransactionDate", rs.getTimestamp("transactiondate"));
            setIfPresent(po, "SDR_MainAccountNumber", rs.getString("mainaccountnumber"));
            setIfPresent(po, "SDR_Total", rs.getBigDecimal("total"));
            setIfPresent(po, "SDR_MainDebitAmount", rs.getBigDecimal("maindebitamount"));
            setIfPresent(po, "SDR_MainCreditAmount", rs.getBigDecimal("maincreditamount"));
            setIfPresent(po, "SDR_ContraAccountNumber", rs.getString("contraaccountnumber"));
            setIfPresent(po, "SDR_ContraDebitAmount", rs.getBigDecimal("contradebitamount"));
            setIfPresent(po, "SDR_ContraCreditAmount", rs.getBigDecimal("contracreditamount"));

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
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int rowNumber, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_levygpprocessdata row #" + rowNumber + ": " + e.getMessage());
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
