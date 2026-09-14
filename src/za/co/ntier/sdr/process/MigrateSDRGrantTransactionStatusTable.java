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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_granttransactionstatus into
 * SDR_GrantTransactionStatus - child of GrantTransaction (451,557 source rows). Requires
 * {@link MigrateSDRGrantTransactionTable} to have already run.
 *
 * <p>IMPORTANT DIFFERENCE FROM LEVY: unlike LevyTransactionStatus.Creditor (always blank, dropped
 * entirely), THIS Creditor column IS populated (only 107/451,557 blank) and holds the SAME L/D
 * organisation-number value directly (not embedded in composite text, unlike
 * GrantTransaction.ReferenceNumber) - resolved via the same two-tier
 * {@link SDRMigrationSupport#buildOrganisationCrosswalk}.
 *
 * <p>Processes in bounded batches with an explicit "id &gt; lastSeenId" keyset cursor given the row
 * count.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantTransactionStatusTable")
public class MigrateSDRGrantTransactionStatusTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantTransactionStatus";
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
                    TABLE_NAME + " does not exist - run AddSDRGrantTransactionStatusTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> grantTransactionCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_granttransaction", "sdr_granttransaction_id", get_TrxName());
        SDRMigrationSupport.OrganisationCrosswalk organisationCrosswalk = SDRMigrationSupport
                .buildOrganisationCrosswalk(get_TrxName());
        addLog("Crosswalks ready: " + grantTransactionCrosswalk.size() + " GrantTransactions.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
        int skippedNoGrantTransaction = 0;
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

            String sql = "SELECT t.* FROM mssdr_granttransactionstatus t "
                    + "WHERE t.id > " + lastId + " "
                    + "AND NOT EXISTS (SELECT 1 FROM sdr_granttransactionstatus s WHERE s.id = t.id) "
                    + "ORDER BY t.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRGrantTransactionStatusRead");
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
                    Integer grantTransactionId = grantTransactionCrosswalk.get(rs.getInt("granttransactionid"));
                    if (grantTransactionId == null) {
                        skippedNoGrantTransaction++;
                        continue;
                    }
                    try {
                        processOneRow(table, rs, grantTransactionId, organisationCrosswalk);
                        created++;
                    } catch (Exception e) {
                        logError(sourceId, e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_granttransactionstatus rows (" + created
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

        writeErrorLogIfAny("migrate-sdr-granttransactionstatus-errors");

        return "Processed " + processed + " mssdr_granttransactionstatus row(s): " + created + " "
                + "SDR_GrantTransactionStatus created, " + skippedNoGrantTransaction + " skipped (no matching "
                + "SDR_GrantTransaction), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int grantTransactionId,
            SDRMigrationSupport.OrganisationCrosswalk organisationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");
        String creditor = rs.getString("creditor");

        String trxName = Trx.createTrxName("SDRGrantTransactionStatusMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_GrantTransaction_ID", grantTransactionId);

            setIfPresent(po, "SDR_Creditor", creditor);
            setIfPresent(po, "SDR_Organisation_ID", organisationCrosswalk.resolve(creditor));
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
                SDRMigrationSupport.stampCreatedUpdated("sdr_granttransactionstatus",
                        "sdr_granttransactionstatus_id", newId, created, updated, trxName);
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
            errors.add("mssdr_granttransactionstatus.id=" + sourceId + ": " + e.getMessage());
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
