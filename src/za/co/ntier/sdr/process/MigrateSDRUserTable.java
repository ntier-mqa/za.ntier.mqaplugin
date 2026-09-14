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
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): migrates mssdr_user into SDR_User -
 * the MAIN table (6,418 source rows), the SIMS application's own user accounts (distinct from
 * iDempiere's own AD_User). SDR_IsActive/SDR_IsADUser/SDR_IsDevUser are all plain tinyint flags
 * (DisplayType.Integer, not YesNo), toBD-wrapped. SDR_LoginObjectID is carried as free text despite the
 * name (confirmed not an FK, currently 100% unpopulated).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRUserTable")
public class MigrateSDRUserTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_User";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRUserTable first");
        }

        String sql = "SELECT u.* FROM mssdr_user u "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_user s WHERE s.id = u.id) "
                + "ORDER BY u.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRUserRead");
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

        writeErrorLogIfAny("migrate-sdr-user-errors");

        return "Processed " + processed + " mssdr_user row(s): " + created + " SDR_User created, "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRUserMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_FirstName", rs.getString("firstname"));
            setIfPresent(po, "SDR_Surname", rs.getString("surname"));
            setIfPresent(po, "SDR_IDNo", rs.getString("idno"));
            setIfPresent(po, "SDR_TelephoneNumber", rs.getString("telephonenumber"));
            setIfPresent(po, "SDR_CellPhoneNumber", rs.getString("cellphonenumber"));
            setIfPresent(po, "SDR_FaxNumber", rs.getString("faxnumber"));
            setIfPresent(po, "SDR_Email", rs.getString("email"));
            setIfPresent(po, "SDR_UserName", rs.getString("username"));
            setIfPresent(po, "SDR_PasswordExpiryDate", rs.getTimestamp("passwordexpirydate"));
            po.set_ValueOfColumn("SDR_IsActive", SDRMigrationSupport.toBD(rs.getInt("isactive")));
            po.set_ValueOfColumn("SDR_IsADUser", SDRMigrationSupport.toBD(rs.getInt("isaduser")));
            po.set_ValueOfColumn("SDR_IsDevUser", SDRMigrationSupport.toBD(rs.getInt("isdevuser")));
            setIfPresent(po, "SDR_LoginObjectID", rs.getString("loginobjectid"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_user", "sdr_user_id", newId, created, updated,
                        trxName);
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
            errors.add("mssdr_user.id=" + sourceId + ": " + e.getMessage());
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
