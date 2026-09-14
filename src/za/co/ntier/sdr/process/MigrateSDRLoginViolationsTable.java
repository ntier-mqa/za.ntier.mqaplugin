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
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): migrates mssdr_loginviolations into
 * SDR_LoginViolations (92,278 source rows) - STANDALONE, no FK to SDR_User (this table logs attempts
 * against usernames that may not correspond to a real account, so a hard FK wouldn't make sense - a
 * text match was tested and only resolves 71.5%, not built as an approximate FK per user decision).
 *
 * <p>SECURITY - CRITICAL: deliberately does NOT read or carry mssdr_loginviolations.password under any
 * circumstances - this column shows strong evidence of storing PLAINTEXT passwords, unlike
 * UserPasswordHistory's properly-hashed equivalent. The SELECT below names every column explicitly
 * (not "SELECT *") specifically so the password column can never be touched, even indirectly. See the
 * mapping doc's SECURITY FLAG section and AddSDRLoginViolationsTable's Javadoc.
 *
 * <p>CORRECTED 2026-09-14: mssdr_loginviolations, like mssdr_userlogin, turns out to have NO generic
 * created/updated/isdeleted columns at all (confirmed via a hard SQL failure: "column v.created does
 * not exist") - the mapping doc didn't call this out explicitly for this table the way it did for
 * UserLogin, but the source table shape is the same. IsActive is left at its standard 'Y' default and
 * Created/Updated are left as whatever PO.saveEx() naturally stamps (the migration run time).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRLoginViolationsTable")
public class MigrateSDRLoginViolationsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_LoginViolations";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRLoginViolationsTable first");
        }

        String sql = "SELECT v.id, v.username, v.ipaddress, v.dateloggedin, v.issuccessfullogin, "
                + "v.isaduser FROM mssdr_loginviolations v "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_loginviolations s WHERE s.id = v.id) "
                + "ORDER BY v.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRLoginViolationsRead");
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
                    logError(rs.getInt("id"), e);
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-loginviolations-errors");

        return "Processed " + processed + " mssdr_loginviolations row(s): " + created + " "
                + "SDR_LoginViolations created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");

        String trxName = Trx.createTrxName("SDRLoginViolationsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_UserName", rs.getString("username"));
            setIfPresent(po, "SDR_IPAddress", rs.getString("ipaddress"));
            setIfPresent(po, "SDR_DateLoggedIn", rs.getTimestamp("dateloggedin"));
            po.set_ValueOfColumn("SDR_IsSuccessfulLogin", SDRMigrationSupport.toBD(rs.getInt("issuccessfullogin")));
            po.set_ValueOfColumn("SDR_IsADUser", SDRMigrationSupport.toBD(rs.getInt("isaduser")));

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
            errors.add("mssdr_loginviolations.id=" + sourceId + ": " + e.getMessage());
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
