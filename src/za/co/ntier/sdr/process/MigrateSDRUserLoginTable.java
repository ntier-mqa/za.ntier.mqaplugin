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
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): migrates mssdr_userlogin into
 * SDR_UserLogin - the LARGEST table in this family (669,335 source rows). Requires
 * {@link MigrateSDRUserTable} to have already run.
 *
 * <p>UNIQUE in this family: the source has NO audit columns at all (no DateCreated/CreatedBy/
 * UpdatedBy/IsDeleted - confirmed in the mapping doc) - a much lighter-weight table than everything
 * else in this migration. No isactive derivation, no Created/Updated stamping: IsActive is left at its
 * standard 'Y' default and Created/Updated are left as whatever PO.saveEx() naturally stamps (the
 * migration run time).
 *
 * <p>Processes in bounded batches with an explicit "id &gt; lastSeenId" keyset cursor given the row
 * count (see the WSPATR Annual Training Report / Levy / Grant fixes).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRUserLoginTable")
public class MigrateSDRUserLoginTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_UserLogin";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRUserLoginTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> userCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_user", "sdr_user_id",
                get_TrxName());
        addLog("Crosswalks ready: " + userCrosswalk.size() + " Users.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
        int skippedNoUser = 0;
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

            String sql = "SELECT l.* FROM mssdr_userlogin l "
                    + "WHERE l.id > " + lastId + " "
                    + "AND NOT EXISTS (SELECT 1 FROM sdr_userlogin s WHERE s.id = l.id) "
                    + "ORDER BY l.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRUserLoginRead");
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
                    Integer userId = userCrosswalk.get(rs.getInt("userid"));
                    if (userId == null) {
                        skippedNoUser++;
                        continue;
                    }
                    try {
                        processOneRow(table, rs, userId);
                        created++;
                    } catch (Exception e) {
                        logError(sourceId, e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_userlogin rows (" + created + " created, "
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

        writeErrorLogIfAny("migrate-sdr-userlogin-errors");

        return "Processed " + processed + " mssdr_userlogin row(s): " + created + " SDR_UserLogin created, "
                + skippedNoUser + " skipped (no matching SDR_User), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int userId) throws Exception {
        int sourceId = rs.getInt("id");

        String trxName = Trx.createTrxName("SDRUserLoginMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_User_ID", userId);

            setIfPresent(po, "SDR_DateLoggedIn", rs.getTimestamp("dateloggedin"));

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
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_userlogin.id=" + sourceId + ": " + e.getMessage());
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
