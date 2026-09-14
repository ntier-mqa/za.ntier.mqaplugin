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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levytransactiondetail into
 * SDR_LevyTransactionDetail - child of LevyTransaction (703,156 source rows, the largest table in this
 * family). Requires {@link MigrateSDRLevyTransactionTable} to have already run. SDR_AccountNumber is
 * UNMAPPED (a shared GL code, not a per-row FK to SDR_LevyAccount - a naive join fans out to 55.2M
 * rows, see the mapping doc) and carried as plain text.
 *
 * <p>Processes in bounded batches (like the WSPATR Annual Training Report fix) given the row count.
 *
 * <p>CORRECTED 2026-09-14: the first batching attempt paged purely off "WHERE NOT EXISTS (...) ORDER
 * BY id LIMIT n", relying on successfully-created rows shrinking the candidate set each pass. That
 * breaks if any row's SDR_LevyTransaction_ID lookup never resolves (a permanent skip, never inserted,
 * so NOT EXISTS never excludes it) - once the number of such permanently-unresolvable rows reaches the
 * batch size, every batch re-selects the same window forever with zero forward progress (observed
 * live: still running after over an hour on a table that should take ~15-20 minutes). Fixed by adding
 * an explicit "id > lastSeenId" keyset cursor that always advances past every row actually read,
 * whether it was created or skipped - NOT EXISTS is kept only to avoid recreating rows already
 * migrated by a prior run of this same idempotent process.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyTransactionDetailTable")
public class MigrateSDRLevyTransactionDetailTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyTransactionDetail";
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
                    TABLE_NAME + " does not exist - run AddSDRLevyTransactionDetailTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> levyTransactionCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_levytransaction",
                "sdr_levytransaction_id", get_TrxName());
        addLog("Crosswalks ready: " + levyTransactionCrosswalk.size() + " LevyTransactions.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
        int skippedNoLevyTransaction = 0;
        long lastId = 0;
        boolean more = true;
        while (more) {
            int batchLimit = BATCH_SIZE;
            if (maxRows > 0) {
                long remaining = maxRows - processed;
                if (remaining <= 0) {
                    break;
                }
                batchLimit = (int) Math.min(BATCH_SIZE, remaining);
            }

            String sql = "SELECT d.* FROM mssdr_levytransactiondetail d "
                    + "WHERE d.id > " + lastId + " "
                    + "AND NOT EXISTS (SELECT 1 FROM sdr_levytransactiondetail s WHERE s.id = d.id) "
                    + "ORDER BY d.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRLevyTransactionDetailRead");
            Trx readTrx = Trx.get(readTrxName, true);
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = DB.prepareStatement(sql, readTrxName);
                pstmt.setFetchSize(2000);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    rowsInBatch++;
                    processed++;
                    int sourceId = rs.getInt("id");
                    lastId = sourceId;
                    Integer levyTransactionId = levyTransactionCrosswalk.get(rs.getInt("levytransactionid"));
                    if (levyTransactionId == null) {
                        skippedNoLevyTransaction++;
                        continue;
                    }
                    try {
                        processOneRow(table, rs, levyTransactionId);
                        created++;
                    } catch (Exception e) {
                        logError(sourceId, e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_levytransactiondetail rows (" + created
                                + " created, " + errors.size() + " error(s))...");
                    }
                }
            } finally {
                DB.close(rs, pstmt);
                readTrx.rollback();
                readTrx.close();
            }

            if (rowsInBatch < batchLimit) {
                more = false;
            }
        }

        writeErrorLogIfAny("migrate-sdr-levytransactiondetail-errors");

        return "Processed " + processed + " mssdr_levytransactiondetail row(s): " + created + " "
                + "SDR_LevyTransactionDetail created, " + skippedNoLevyTransaction + " skipped (no matching "
                + "SDR_LevyTransaction), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int levyTransactionId) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRLevyTransactionDetailMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_LevyTransaction_ID", levyTransactionId);

            setIfPresent(po, "SDR_TransactionType", rs.getString("transactiontype"));
            int isContraEntry = rs.getInt("iscontraentry");
            if (!rs.wasNull()) {
                po.set_ValueOfColumn("SDR_IsContraEntry", SDRMigrationSupport.flagToYN(isContraEntry));
            }
            setIfPresent(po, "SDR_AccountNumber", rs.getString("accountnumber"));
            setIfPresent(po, "SDR_TransactionValue", rs.getBigDecimal("transactionvalue"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_levytransactiondetail",
                        "sdr_levytransactiondetail_id", newId, created, updated, trxName);
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
            errors.add("mssdr_levytransactiondetail.id=" + sourceId + ": " + e.getMessage());
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
