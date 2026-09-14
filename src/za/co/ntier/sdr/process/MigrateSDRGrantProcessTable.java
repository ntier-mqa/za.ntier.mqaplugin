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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_grantprocess into
 * SDR_GrantProcess (12,843 source rows) - low business value, no read-only window. Must run before
 * {@link MigrateSDRGrantGPProcessDataTable}, which FKs to it. SDR_ProcessID/SDR_ImportID/
 * SDR_LevyGrantProcessQueueStatus_ID are all UNMAPPED plain integers. SDR_GrantSentXML/
 * SDR_GrantReturnXML are carried unparsed as plain text.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantProcessTable")
public class MigrateSDRGrantProcessTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantProcess";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRGrantProcessTable first");
        }

        String sql = "SELECT p.* FROM mssdr_grantprocess p "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_grantprocess s WHERE s.id = p.id) "
                + "ORDER BY p.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRGrantProcessRead");
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

        writeErrorLogIfAny("migrate-sdr-grantprocess-errors");

        return "Processed " + processed + " mssdr_grantprocess row(s): " + created + " SDR_GrantProcess "
                + "created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRGrantProcessMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            po.set_ValueOfColumn("SDR_ImportID", SDRMigrationSupport.toBD(rs.getInt("importid")));
            setIfPresent(po, "SDR_GrantCode", rs.getString("grantcode"));
            po.set_ValueOfColumn("SDR_LevyGrantProcessQueueStatus_ID",
                    SDRMigrationSupport.toBD(rs.getInt("levygrantprocessqueuestatusid")));
            po.set_ValueOfColumn("SDR_StatusCode", SDRMigrationSupport.toBD(rs.getInt("statuscode")));
            setIfPresent(po, "SDR_StatusDescription", rs.getString("statusdescription"));
            setIfPresent(po, "SDR_GrantSentXML", rs.getString("grantsentxml"));
            setIfPresent(po, "SDR_GrantReturnXML", rs.getString("grantreturnxml"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_grantprocess", "sdr_grantprocess_id", newId, created,
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
            errors.add("mssdr_grantprocess.id=" + sourceId + ": " + e.getMessage());
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
