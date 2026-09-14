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
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): migrates mssdr_userloginattempt into
 * SDR_UserLoginAttempt (64,395 source rows). Requires {@link MigrateSDRUserTable} to have already run.
 * SDR_UnlockedByUser_ID resolves against the SAME SDR_User crosswalk as SDR_User_ID (a second,
 * admin-side FK to the same target table) - CONFIRMED only 0.2% populated (130/64,395), expected since
 * admin-unlock is a rare event.
 *
 * <p>CORRECTED 2026-09-14: this table, like UserLogin and LoginViolations, turns out to have NO
 * generic created/updated/isdeleted columns at all (confirmed the hard way: "The column name created
 * was not found in this ResultSet" on every row) - not called out explicitly in the mapping doc for
 * this table. IsActive is left at its standard 'Y' default and Created/Updated are left as whatever
 * PO.saveEx() naturally stamps (the migration run time).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRUserLoginAttemptTable")
public class MigrateSDRUserLoginAttemptTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_UserLoginAttempt";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRUserLoginAttemptTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> userCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_user", "sdr_user_id",
                get_TrxName());

        String sql = "SELECT a.* FROM mssdr_userloginattempt a "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_userloginattempt s WHERE s.id = a.id) "
                + "ORDER BY a.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoUser = 0;
        String readTrxName = Trx.createTrxName("SDRUserLoginAttemptRead");
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
                    processOneRow(table, rs, userId, userCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-userloginattempt-errors");

        return "Processed " + processed + " mssdr_userloginattempt row(s): " + created + " "
                + "SDR_UserLoginAttempt created, " + skippedNoUser + " skipped (no matching SDR_User), "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int userId, Map<Integer, Integer> userCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");

        String trxName = Trx.createTrxName("SDRUserLoginAttemptMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_User_ID", userId);

            setIfPresent(po, "SDR_IPAddress", rs.getString("ipaddress"));
            setIfPresent(po, "SDR_DateLoggedIn", rs.getTimestamp("dateloggedin"));
            po.set_ValueOfColumn("SDR_UnlockedViaAdmin", SDRMigrationSupport.toBD(rs.getInt("unlockedviaadmin")));
            setIfPresent(po, "SDR_UnlockedDate", rs.getTimestamp("unlockeddate"));
            setIfPresent(po, "SDR_UnlockedByUser_ID", SDRMigrationSupport.resolveLookup(userCrosswalk,
                    rs.getInt("unlockedbyuserid")));

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

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_userloginattempt.id=" + sourceId + ": " + e.getMessage());
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
