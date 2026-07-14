package za.co.ntier.learner.process;

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
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZWorkplaceApproval;

/**
 * Migrates the staged ms_workplaceapproval table into ZZWorkplaceApproval - a bare
 * number-only reference table used by the QCTO join tables (WA, SecondaryWA FKs). See
 * "Column Mapping - QCTO Learner Programmes" doc, tab "WorkplaceApproval".
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzworkplaceapproval ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>NOT handled - zzworkplaceapproval has no column to receive these (bare table, only
 * zzworkplaceapprovalnumber exists): organisationid, workplaceapprovaltypeid,
 * workplaceapprovalclassid, workplaceapprovalstatusid, workplaceapprovalstartdate/enddate,
 * qualityassurancebodyid, dhetregistration*, issaica, webaddress, accreditationtypeid,
 * workplaceapprovalcode, workplaceapprovalapplicationid, notapprovedreason,
 * applicationreceiveddate, workplaceapprovalinternalexternalid,
 * workplaceapprovalalertemail, workplaceapprovalbody, workplaceapprovalreviewdate, levy.
 * If the business wants any of these tracked, zzworkplaceapproval needs new columns first
 * (AD_Column change - out of scope here, needs sign-off).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsWorkplaceApprovalToZZWorkplaceApproval")
public class MigrateMsWorkplaceApprovalToZZWorkplaceApproval extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String SOURCE_TABLE = "ms_workplaceapproval";

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

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzworkplaceapproval WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZWorkplaceApproval row(s)...");
            DB.executeUpdateEx("DELETE FROM zzworkplaceapproval WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        String sql = "SELECT id, workplaceapprovalnumber, created, updated, isdeleted FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM zzworkplaceapproval z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsWorkplaceApprovalRead");
        Trx readTrx = Trx.get(readTrxName, true);
        int processed = 0;
        int created = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(rs);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " " + SOURCE_TABLE + " rows (" + created
                            + " ZZWorkplaceApproval created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created
                + " ZZWorkplaceApproval created, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");
        String workplaceApprovalNumber = rs.getString("workplaceapprovalnumber");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsWorkplaceApprovalMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZWorkplaceApproval wa = new X_ZZWorkplaceApproval(getCtx(), 0, trxName);
            wa.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            wa.setIsActive(isDeleted == 0);
            wa.setZZWorkplaceApprovalNumber(workplaceApprovalNumber);

            wa.saveEx();
            int zzWaId = wa.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzworkplaceapproval", "zzworkplaceapproval_id", zzWaId,
                        createdTs, DEFAULT_CREATED_BY, updatedTs, DEFAULT_CREATED_BY, sourceId, trxName);
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
            errors.add(SOURCE_TABLE + ".id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-workplaceapproval-errors-" + ts + ".txt");
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
