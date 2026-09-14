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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_granttransactionapproval into
 * SDR_GrantTransactionApproval (371 source rows). Not linked to a specific SDR_GrantTransaction row -
 * no such column exists in the source (see AddSDRGrantTransactionApprovalTable's Javadoc).
 *
 * <p>SDR_COORecommendedBy_ID/SDR_CFORecommendedBy_ID/SDR_CEOApprovedBy_ID follow the platform's
 * audit-trail pattern (Search + AD_User) - already renamed to their correct "_ID"-suffixed form by
 * FixSDRAuditColumnNames, and their raw values are plain passthroughs (already AD_User_ID-shaped), not
 * crosswalked. SDR_COORecommended_ID/SDR_CFORecommended_ID/SDR_CEOApproved_ID resolve against the
 * shared SDR_YesNo table.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantTransactionApprovalTable")
public class MigrateSDRGrantTransactionApprovalTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantTransactionApproval";
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
                    TABLE_NAME + " does not exist - run AddSDRGrantTransactionApprovalTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> grantCodeCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_grantcode",
                "sdr_grantcode_id", get_TrxName());
        Map<Integer, Integer> yesNoCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_yesno", "sdr_yesno_id",
                get_TrxName());

        String sql = "SELECT a.* FROM mssdr_granttransactionapproval a "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_granttransactionapproval s WHERE s.id = a.id) "
                + "ORDER BY a.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRGrantTransactionApprovalRead");
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
                    processOneRow(table, rs, grantCodeCrosswalk, yesNoCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-granttransactionapproval-errors");

        return "Processed " + processed + " mssdr_granttransactionapproval row(s): " + created + " "
                + "SDR_GrantTransactionApproval created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> grantCodeCrosswalk,
            Map<Integer, Integer> yesNoCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRGrantTransactionApprovalMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_GrantCode_ID", SDRMigrationSupport.resolveLookup(grantCodeCrosswalk,
                    rs.getInt("grantcodeid")));
            po.set_ValueOfColumn("SDR_ImportID", SDRMigrationSupport.toBD(rs.getInt("importid")));

            setIfPresent(po, "SDR_COORecommended_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("coorecommendedid")));
            setIfPresent(po, "SDR_COORecommendedBy_ID", rs.getInt("coorecommendedby"));
            setIfPresent(po, "SDR_DateCOORecommended", rs.getTimestamp("datecoorecommended"));
            setIfPresent(po, "SDR_CFORecommended_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("cforecommendedid")));
            setIfPresent(po, "SDR_CFORecommendedBy_ID", rs.getInt("cforecommendedby"));
            setIfPresent(po, "SDR_DateCFORecommended", rs.getTimestamp("datecforecommended"));
            setIfPresent(po, "SDR_CEOApproved_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("ceoapprovedid")));
            setIfPresent(po, "SDR_CEOApprovedBy_ID", rs.getInt("ceoapprovedby"));
            setIfPresent(po, "SDR_DateCEOApproved", rs.getTimestamp("dateceoapproved"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_granttransactionapproval",
                        "sdr_granttransactionapproval_id", newId, created, updated, trxName);
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
        if (value instanceof Integer && (Integer) value == 0) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_granttransactionapproval.id=" + sourceId + ": " + e.getMessage());
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
