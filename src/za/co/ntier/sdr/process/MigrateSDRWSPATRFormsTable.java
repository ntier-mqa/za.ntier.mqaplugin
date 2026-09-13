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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrforms into
 * SDR_WSPATRForms (32 source rows). Must run before
 * {@link MigrateSDRWSPATREvaluationVerificationChecklistTable}, which FKs to this table.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRFormsTable")
public class MigrateSDRWSPATRFormsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATRForms";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRWSPATRFormsTable first");
        }

        Map<Integer, Integer> financialYearCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_financialyear",
                "sdr_financialyear_id", get_TrxName());
        Map<Integer, Integer> formTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_formtype",
                "sdr_formtype_id", get_TrxName());

        String sql = "SELECT f.* FROM mssdr_wspatrforms f "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrforms s WHERE s.id = f.id) "
                + "ORDER BY f.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRFormsRead");
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
                    processOneRow(table, rs, financialYearCrosswalk, formTypeCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-wspatrforms-errors");

        return "Processed " + processed + " mssdr_wspatrforms row(s): " + created + " SDR_WSPATRForms "
                + "created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> financialYearCrosswalk,
            Map<Integer, Integer> formTypeCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRFormsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_FormName", rs.getString("formname"));
            setIfPresent(po, "SDR_FormDescription", rs.getString("formdescription"));
            setIfPresent(po, "SDR_FinancialYear_ID", SDRMigrationSupport.resolveLookup(financialYearCrosswalk,
                    rs.getInt("financialyearid")));
            setIfPresent(po, "SDR_FormType_ID", SDRMigrationSupport.resolveLookup(formTypeCrosswalk,
                    rs.getInt("formtypeid")));
            po.set_ValueOfColumn("SDR_IOrdinal", SDRMigrationSupport.toBD(rs.getInt("iordinal")));
            setIfPresent(po, "SDR_ExcelSheetName", rs.getString("excelsheetname"));
            setIfPresent(po, "SDR_ImportTableName", rs.getString("importtablename"));
            po.set_ValueOfColumn("SDR_ExcelStartRow", SDRMigrationSupport.toBD(rs.getInt("excelstartrow")));
            po.set_ValueOfColumn("SDR_ExcelEndColumn", SDRMigrationSupport.toBD(rs.getInt("excelendcolumn")));
            setIfPresent(po, "SDR_ImportReportProc", rs.getString("importreportproc"));
            setIfPresent(po, "SDR_ImportTemplate", rs.getString("importtemplate"));
            setIfPresent(po, "SDR_ExportReportProc", rs.getString("exportreportproc"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrforms", "sdr_wspatrforms_id", newId, created,
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
            errors.add("mssdr_wspatrforms.id=" + sourceId + ": " + e.getMessage());
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
