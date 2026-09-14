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
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): migrates mssdr_userpasswordhistory into
 * SDR_UserPasswordHistory (29,778 source rows). Requires {@link MigrateSDRUserTable} to have already
 * run.
 *
 * <p>SECURITY: deliberately does NOT read or carry mssdr_userpasswordhistory.password under any
 * circumstances - see the mapping doc's SECURITY FLAG section and AddSDRUserPasswordHistoryTable's
 * Javadoc. Only SDR_User_ID plus the original Created/Updated timestamps are carried ("this user
 * changed their password on this date").
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRUserPasswordHistoryTable")
public class MigrateSDRUserPasswordHistoryTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_UserPasswordHistory";
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
                    TABLE_NAME + " does not exist - run AddSDRUserPasswordHistoryTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> userCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_user", "sdr_user_id",
                get_TrxName());

        String sql = "SELECT h.id, h.created, h.updated, h.isdeleted, h.userid FROM mssdr_userpasswordhistory h "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_userpasswordhistory s WHERE s.id = h.id) "
                + "ORDER BY h.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoUser = 0;
        String readTrxName = Trx.createTrxName("SDRUserPasswordHistoryRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer userId = userCrosswalk.get(rs.getInt("userid"));
                if (userId == null) {
                    skippedNoUser++;
                    continue;
                }
                try {
                    processOneRow(table, rs, userId);
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

        writeErrorLogIfAny("migrate-sdr-userpasswordhistory-errors");

        return "Processed " + processed + " mssdr_userpasswordhistory row(s): " + created + " "
                + "SDR_UserPasswordHistory created, " + skippedNoUser + " skipped (no matching SDR_User), "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int userId) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRUserPasswordHistoryMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_User_ID", userId);

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_userpasswordhistory", "sdr_userpasswordhistory_id",
                        newId, created, updated, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_userpasswordhistory.id=" + sourceId + ": " + e.getMessage());
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
