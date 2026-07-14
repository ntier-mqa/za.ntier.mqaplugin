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

import za.co.ntier.api.model.X_ZZGrantType;

/**
 * Migrates the staged ms_granttype table into ZZGrantType. See "Column Mapping - QCTO
 * Learner Programmes" doc, tab "GrantType".
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzgranttype ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>NOT handled (both are "Open Items" in the mapping doc, not yet resolvable):
 * <ul>
 *   <li>financialyearid -&gt; C_Year_ID: needs a FinancialYear-to-C_Year crosswalk strategy
 *       that hasn't been designed yet. Left unset.</li>
 *   <li>grantcodeid -&gt; ZZLkpGrantCode_ID: zzlkpgrantcode already exists in iDempiere but
 *       no crosswalk from MSSQL's GrantCode id to it has been worked out yet. Left unset.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsGrantTypeToZZGrantType")
public class MigrateMsGrantTypeToZZGrantType extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String SOURCE_TABLE = "ms_granttype";

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzgranttype WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZGrantType row(s)...");
            DB.executeUpdateEx("DELETE FROM zzgranttype WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        String sql = "SELECT id, grantname, grantdescription, grantpercentage, ispayable, minimumamount, "
                + "created, updated, isdeleted FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM zzgranttype z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsGrantTypeRead");
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
                            + " ZZGrantType created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created
                + " ZZGrantType created, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");
        String grantName = rs.getString("grantname");
        String grantDescription = rs.getString("grantdescription");
        java.math.BigDecimal grantPercentage = rs.getBigDecimal("grantpercentage");
        boolean isPayableVal = rs.getBoolean("ispayable");
        java.math.BigDecimal minimumAmount = rs.getBigDecimal("minimumamount");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsGrantTypeMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZGrantType grantType = new X_ZZGrantType(getCtx(), 0, trxName);
            grantType.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            grantType.setIsActive(isDeleted == 0);
            grantType.setZZGrantName(grantName);
            grantType.setZZGrantDescription(grantDescription);
            if (grantPercentage != null) {
                grantType.setZZGrantPercentage(grantPercentage);
            }
            grantType.setZZIsPayable(MigrationSupport.flagToYN(Boolean.valueOf(isPayableVal)));
            if (minimumAmount != null) {
                grantType.setZZMinimumAmount(minimumAmount);
            }
            // C_Year_ID (financialyearid) and ZZLkpGrantCode_ID (grantcodeid): NOT SET -
            // see class Javadoc "NOT handled".

            grantType.saveEx();
            int zzGrantTypeId = grantType.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzgranttype", "zzgranttype_id", zzGrantTypeId,
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
        File logFile = new File("/tmp/migrate-ms-granttype-errors-" + ts + ".txt");
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
