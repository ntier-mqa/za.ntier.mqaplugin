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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levyimport into SDR_LevyImport -
 * a STANDALONE catalog (351,578 source rows), NOT a child of SDR_LevyTransaction despite an earlier
 * investigation pass's incorrect assumption (see AddSDRLevyImportTable's Javadoc). Requires
 * {@link MigrateSDRLevyAccountTable} to have already run.
 *
 * <p>SDR_Organisation_ID resolves via LNumber directly (no text-parsing needed, unlike
 * LevyTransaction.ReferenceNumber) against SDR_Organisation.SDR_SDLNumber via a string-keyed
 * crosswalk. CONFIRMED 100% of 3,443 distinct LNumbers match.
 *
 * <p>Processes in bounded batches (like the WSPATR Annual Training Report fix) given the row count.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyImportTable")
public class MigrateSDRLevyImportTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyImport";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRLevyImportTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> levyAccountCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_levyaccount",
                "sdr_levyaccount_id", get_TrxName());
        Map<String, Integer> organisationCrosswalk = SDRMigrationSupport.buildStringCrosswalk("sdr_organisation",
                "sdr_sdlnumber", "sdr_organisation_id", get_TrxName());
        addLog("Crosswalks ready: " + levyAccountCrosswalk.size() + " LevyAccounts, "
                + organisationCrosswalk.size() + " Organisations by SDLNumber.");

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

            String sql = "SELECT i.* FROM mssdr_levyimport i "
                    + "WHERE NOT EXISTS (SELECT 1 FROM sdr_levyimport s WHERE s.id = i.id) "
                    + "ORDER BY i.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRLevyImportRead");
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
                        processOneRow(table, rs, levyAccountCrosswalk, organisationCrosswalk);
                        created++;
                    } catch (Exception e) {
                        logError(rs.getInt("id"), e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_levyimport rows (" + created + " created, "
                                + errors.size() + " error(s))...");
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

        writeErrorLogIfAny("migrate-sdr-levyimport-errors");

        return "Processed " + processed + " mssdr_levyimport row(s): " + created + " SDR_LevyImport created, "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> levyAccountCrosswalk,
            Map<String, Integer> organisationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");
        String lNumber = rs.getString("lnumber");

        String trxName = Trx.createTrxName("SDRLevyImportMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_LevyAccount_ID", SDRMigrationSupport.resolveLookup(levyAccountCrosswalk,
                    rs.getInt("levyaccountid")));
            setIfPresent(po, "SDR_Amount", rs.getBigDecimal("amount"));
            setIfPresent(po, "SDR_Organisation_ID", SDRMigrationSupport.resolveLookup(organisationCrosswalk,
                    lNumber));
            setIfPresent(po, "SDR_LNumber", lNumber);
            setIfPresent(po, "SDR_ProcessDate", rs.getTimestamp("processdate"));
            setIfPresent(po, "SDR_ImportDate", rs.getTimestamp("importdate"));
            po.set_ValueOfColumn("SDR_SchemeYear", SDRMigrationSupport.toBD(rs.getInt("schemeyear")));
            int levyImportsId = rs.getInt("levyimportsid");
            if (!rs.wasNull()) {
                po.set_ValueOfColumn("SDR_LevyImportsID", SDRMigrationSupport.toBD(levyImportsId));
            }

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_levyimport", "sdr_levyimport_id", newId, created,
                        updated, trxName);
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
            errors.add("mssdr_levyimport.id=" + sourceId + ": " + e.getMessage());
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
