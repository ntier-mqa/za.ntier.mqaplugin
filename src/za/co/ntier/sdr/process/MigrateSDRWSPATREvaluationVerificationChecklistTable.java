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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates
 * mssdr_wspatrevaluationverificationchecklist into SDR_WSPATREvaluationVerificationChecklist (54,281
 * source rows). Requires {@link MigrateSDRWSPATRTable} and {@link MigrateSDRWSPATRFormsTable} to have
 * already run. SDR_Grant_ID stays a plain integer (source column is always NULL, exact target table
 * still unconfirmed - see mapping doc).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATREvaluationVerificationChecklistTable")
public class MigrateSDRWSPATREvaluationVerificationChecklistTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATREvaluationVerificationChecklist";
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
                    TABLE_NAME + " does not exist - run AddSDRWSPATREvaluationVerificationChecklistTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> checklistTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspatrevaluationverificationchecklisttype",
                "sdr_wspatrevaluationverificationchecklisttype_id", get_TrxName());
        Map<Integer, Integer> wspatrFormCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatrforms",
                "sdr_wspatrforms_id", get_TrxName());
        Map<Integer, Integer> yesNoCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_yesno", "sdr_yesno_id",
                get_TrxName());
        Map<Integer, Integer> deviationCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspatrevaluationverificationdeviation", "sdr_wspatrevaluationverificationdeviation_id",
                get_TrxName());

        String sql = "SELECT c.* FROM mssdr_wspatrevaluationverificationchecklist c "
                + "WHERE NOT EXISTS "
                + "(SELECT 1 FROM sdr_wspatrevaluationverificationchecklist s WHERE s.id = c.id) "
                + "ORDER BY c.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRChecklistRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(2000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer wspatrId = wspatrCrosswalk.get(rs.getInt("wspatrid"));
                if (wspatrId == null) {
                    skippedNoWspatr++;
                    continue;
                }
                try {
                    processOneRow(table, rs, wspatrId, checklistTypeCrosswalk, wspatrFormCrosswalk, yesNoCrosswalk,
                            deviationCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 25000 == 0) {
                    addLog("Processed " + processed + " mssdr_wspatrevaluationverificationchecklist rows ("
                            + created + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-wspatrevaluationverificationchecklist-errors");

        return "Processed " + processed + " mssdr_wspatrevaluationverificationchecklist row(s): " + created
                + " SDR_WSPATREvaluationVerificationChecklist created, " + skippedNoWspatr + " skipped (no "
                + "matching SDR_WSPATR), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId,
            Map<Integer, Integer> checklistTypeCrosswalk, Map<Integer, Integer> wspatrFormCrosswalk,
            Map<Integer, Integer> yesNoCrosswalk, Map<Integer, Integer> deviationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRChecklistMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_WSPATR_ID", wspatrId);

            setIfPresent(po, "SDR_WSPATREvaluationVerificationChecklistType_ID", SDRMigrationSupport.resolveLookup(
                    checklistTypeCrosswalk, rs.getInt("wspatrevaluationverificationchecklisttypeid")));
            setIfPresent(po, "SDR_WSPATRForm_ID", SDRMigrationSupport.resolveLookup(wspatrFormCrosswalk,
                    rs.getInt("wspatrformid")));
            po.set_ValueOfColumn("SDR_Grant_ID", SDRMigrationSupport.toBD(rs.getInt("grantid")));
            setIfPresent(po, "SDR_ConfirmInformationYesNo_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("confirminformationyesnoid")));
            setIfPresent(po, "SDR_InformationOutstandingYesNo_ID", SDRMigrationSupport.resolveLookup(
                    yesNoCrosswalk, rs.getInt("informationoutstandingyesnoid")));
            setIfPresent(po, "SDR_Comment", rs.getString("comment"));
            setIfPresent(po, "SDR_WSPATREvaluationVerificationDeviation_ID", SDRMigrationSupport.resolveLookup(
                    deviationCrosswalk, rs.getInt("wspatrevaluationverificationdeviationid")));
            setIfPresent(po, "SDR_OriginalFileName", rs.getString("originalfilename"));
            setIfPresent(po, "SDR_SavedFileName", rs.getString("savedfilename"));
            setIfPresent(po, "SDR_FilePath", rs.getString("filepath"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrevaluationverificationchecklist",
                        "sdr_wspatrevaluationverificationchecklist_id", newId, created, updated, trxName);
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
            errors.add("mssdr_wspatrevaluationverificationchecklist.id=" + sourceId + ": " + e.getMessage());
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
