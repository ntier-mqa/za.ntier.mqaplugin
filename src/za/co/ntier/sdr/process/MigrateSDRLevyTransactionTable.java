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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levytransaction into
 * SDR_LevyTransaction - the MAIN table (351,578 source rows).
 *
 * <p>SDR_Organisation_ID resolves via a DERIVED text-parse of ReferenceNumber (format
 * "&lt;Year&gt;||&lt;LNumber&gt;") - the L-number is extracted with regex "L\d+$" and matched against
 * SDR_Organisation.SDR_SDLNumber via a string-keyed crosswalk (not the usual staged-id crosswalk).
 * CONFIRMED 100% match (351,578/351,578) in the mapping doc, but this migration still leaves the
 * column unset rather than skipping the row if a given row's parse/match somehow fails, since
 * SDR_Organisation_ID is an informational lookup here, not something the row's own existence depends
 * on. SDR_ImportID/SDR_ProcessID/SDR_TransactionID are all carried as plain integers, NOT resolved FKs
 * - CONFIRMED false-positive crosswalks (see AddSDRLevyTransactionTable's Javadoc).
 *
 * <p>Processes in bounded batches (like the WSPATR Annual Training Report fix) given the row count.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyTransactionTable")
public class MigrateSDRLevyTransactionTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyTransaction";
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final Pattern L_NUMBER_PATTERN = Pattern.compile("L\\d+$");

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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRLevyTransactionTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<String, Integer> organisationCrosswalk = SDRMigrationSupport.buildStringCrosswalk("sdr_organisation",
                "sdr_sdlnumber", "sdr_organisation_id", get_TrxName());
        addLog("Crosswalks ready: " + organisationCrosswalk.size() + " Organisations by SDLNumber.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
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

            String sql = "SELECT t.* FROM mssdr_levytransaction t "
                    + "WHERE NOT EXISTS (SELECT 1 FROM sdr_levytransaction s WHERE s.id = t.id) "
                    + "ORDER BY t.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRLevyTransactionRead");
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
                    try {
                        processOneRow(table, rs, organisationCrosswalk);
                        created++;
                    } catch (Exception e) {
                        logError(rs.getInt("id"), e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_levytransaction rows (" + created
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

        writeErrorLogIfAny("migrate-sdr-levytransaction-errors");

        return "Processed " + processed + " mssdr_levytransaction row(s): " + created + " SDR_LevyTransaction "
                + "created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<String, Integer> organisationCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");
        String referenceNumber = rs.getString("referencenumber");

        String lNumber = null;
        if (referenceNumber != null) {
            Matcher m = L_NUMBER_PATTERN.matcher(referenceNumber);
            if (m.find()) {
                lNumber = m.group();
            }
        }

        String trxName = Trx.createTrxName("SDRLevyTransactionMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_TransactionType", rs.getString("transactiontype"));
            setIfPresent(po, "SDR_TransactionDate", rs.getTimestamp("transactiondate"));
            setIfPresent(po, "SDR_ReferenceNumber", referenceNumber);
            setIfPresent(po, "SDR_ReferenceText", rs.getString("referencetext"));
            setIfPresent(po, "SDR_Organisation_ID", SDRMigrationSupport.resolveLookup(organisationCrosswalk,
                    lNumber));
            po.set_ValueOfColumn("SDR_ImportID", SDRMigrationSupport.toBD(rs.getInt("importid")));
            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            po.set_ValueOfColumn("SDR_TransactionID", SDRMigrationSupport.toBD(rs.getInt("transactionid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_levytransaction", "sdr_levytransaction_id", newId,
                        created, updated, trxName);
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
            errors.add("mssdr_levytransaction.id=" + sourceId + ": " + e.getMessage());
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
