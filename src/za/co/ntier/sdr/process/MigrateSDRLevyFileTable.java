package za.co.ntier.sdr.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): migrates mssdr_levyfile into SDR_LevyFile - a
 * one-off reconciliation manifest, a single source row, built anyway per user decision.
 *
 * <p>Same "no per-row recon key" shape as {@link MigrateSDRLevyGPProcessDataTable}: the source has no
 * "id" column, so this process is idempotent at the whole-table level (refuses to run again if
 * SDR_LevyFile already has any rows) rather than per-row. SDR_ReceiptDate/SDR_PostedDate and the
 * currency-looking columns (Levy/Discretionary/Administration/Interest/Penalties/Total/SETATransfer/
 * Unknown) are all stored as text in the source and carried as-is, not cast. SDR_LNumber is kept
 * verbatim but NOT resolved to a SDR_Organisation FK on this table (the mapping doc doesn't call for
 * one here, unlike LevyTransaction/LevyImport).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLevyFileTable")
public class MigrateSDRLevyFileTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LevyFile";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRLevyFileTable first");
        }

        int existingCount = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM sdr_levyfile");
        if (existingCount > 0) {
            return TABLE_NAME + " already has " + existingCount + " row(s) - skipping. This table has no "
                    + "per-row recon key, so it can only be migrated once (see class Javadoc).";
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> setaCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_seta", "sdr_seta_id",
                get_TrxName());

        String sql = "SELECT * FROM mssdr_levyfile" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRLevyFileRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(10);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(table, rs, setaCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-levyfile-errors");

        return "Processed " + processed + " mssdr_levyfile row(s): " + created + " SDR_LevyFile created, "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> setaCrosswalk) throws Exception {
        String trxName = Trx.createTrxName("SDRLevyFileMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);

            setIfPresent(po, "SDR_ReceiptDate", rs.getString("receiptdate"));
            setIfPresent(po, "SDR_SETA_ID", SDRMigrationSupport.resolveLookup(setaCrosswalk, rs.getInt("setaid")));
            setIfPresent(po, "SDR_LNumber", rs.getString("lnumber"));
            setIfPresent(po, "SDR_PostedDate", rs.getString("posteddate"));
            setIfPresent(po, "SDR_Levy", rs.getString("levy"));
            setIfPresent(po, "SDR_Discretionary", rs.getString("discretionary"));
            setIfPresent(po, "SDR_Administration", rs.getString("administration"));
            setIfPresent(po, "SDR_Interest", rs.getString("interest"));
            setIfPresent(po, "SDR_Penalties", rs.getString("penalties"));
            setIfPresent(po, "SDR_Total", rs.getString("total"));
            setIfPresent(po, "SDR_SETATransfer", rs.getString("setatransfer"));
            setIfPresent(po, "SDR_Unknown", rs.getString("unknown"));
            po.set_ValueOfColumn("SDR_SchemeYear", SDRMigrationSupport.toBD(rs.getInt("schemeyear")));

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
            errors.add("mssdr_levyfile row #" + rowNumber + ": " + e.getMessage());
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
