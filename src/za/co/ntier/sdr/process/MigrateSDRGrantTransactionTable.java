package za.co.ntier.sdr.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_granttransaction into
 * SDR_GrantTransaction - the MAIN table (447,404 source rows).
 *
 * <p>SDR_Organisation_ID resolves via a two-tier DERIVED text-parse of ReferenceNumber (format
 * "&lt;Year&gt;|&lt;GrantCode&gt;|&lt;OrgNumber&gt;", single-pipe delimited - the 3rd segment is the
 * org number, e.g. "L020815744"/"D590723237") using {@link SDRMigrationSupport#buildOrganisationCrosswalk}:
 * exact full-string match against SDR_Organisation.SDR_SDLNumber first, falling back to a 9-digit
 * numeric-suffix-only match (the leading letter is a registration-type code, not part of the org's
 * identity - CONFIRMED 99.4% of all rows resolve this way). SDR_ImportID/SDR_ProcessID/
 * SDR_TransactionID are all carried as plain integers, NOT resolved FKs (see mapping doc).
 *
 * <p>Processes in bounded batches with an explicit "id &gt; lastSeenId" keyset cursor (see the WSPATR
 * Annual Training Report / Levy fixes) so the loop always advances even for rows whose Organisation
 * lookup can't resolve.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantTransactionTable")
public class MigrateSDRGrantTransactionTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantTransaction";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRGrantTransactionTable first");
        }

        addLog("Building Organisation crosswalk (two-tier: exact SDLNumber, then numeric-suffix "
                + "fallback)...");
        SDRMigrationSupport.OrganisationCrosswalk organisationCrosswalk = SDRMigrationSupport
                .buildOrganisationCrosswalk(get_TrxName());
        addLog("Organisation crosswalk ready.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
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

            String sql = "SELECT t.* FROM mssdr_granttransaction t "
                    + "WHERE t.id > " + lastId + " "
                    + "AND NOT EXISTS (SELECT 1 FROM sdr_granttransaction s WHERE s.id = t.id) "
                    + "ORDER BY t.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRGrantTransactionRead");
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
                    try {
                        processOneRow(table, rs, organisationCrosswalk);
                        created++;
                    } catch (Exception e) {
                        logError(sourceId, e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_granttransaction rows (" + created
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

        writeErrorLogIfAny("migrate-sdr-granttransaction-errors");

        return "Processed " + processed + " mssdr_granttransaction row(s): " + created + " SDR_GrantTransaction "
                + "created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, SDRMigrationSupport.OrganisationCrosswalk
            organisationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");
        String referenceNumber = rs.getString("referencenumber");

        String orgNumber = null;
        if (referenceNumber != null) {
            String[] parts = referenceNumber.split("\\|", -1);
            if (parts.length >= 3) {
                orgNumber = parts[2];
            }
        }

        String trxName = Trx.createTrxName("SDRGrantTransactionMigrate");
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
            setIfPresent(po, "SDR_Organisation_ID", organisationCrosswalk.resolve(orgNumber));
            po.set_ValueOfColumn("SDR_ImportID", SDRMigrationSupport.toBD(rs.getInt("importid")));
            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            po.set_ValueOfColumn("SDR_TransactionID", SDRMigrationSupport.toBD(rs.getInt("transactionid")));
            po.set_ValueOfColumn("SDR_IsSRUVendor", SDRMigrationSupport.toBD(rs.getInt("issruvendor")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_granttransaction", "sdr_granttransaction_id", newId,
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
            errors.add("mssdr_granttransaction.id=" + sourceId + ": " + e.getMessage());
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
