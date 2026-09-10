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
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): migrates mssdr_persondocumentupload into
 * SDR_PersonDocumentUpload (1,740 source rows) - metadata only (filename/date/person), no file
 * retrieval. Requires {@link MigrateSDRPersonTable} to have already run.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRPersonDocumentUploadTable")
public class MigrateSDRPersonDocumentUploadTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_PersonDocumentUpload";
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
                    TABLE_NAME + " does not exist - run AddSDRPersonDocumentUploadTable first");
        }

        Map<Integer, Integer> personCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_person",
                "sdr_person_id", get_TrxName());

        String sql = "SELECT d.* FROM mssdr_persondocumentupload d "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_persondocumentupload s WHERE s.id = d.id) "
                + "ORDER BY d.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoPerson = 0;
        String readTrxName = Trx.createTrxName("SDRPersonDocUploadRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer personId = personCrosswalk.get(rs.getInt("personid"));
                if (personId == null) {
                    skippedNoPerson++;
                    continue;
                }
                try {
                    processOneRow(table, rs, personId);
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

        writeErrorLogIfAny("migrate-sdr-persondocumentupload-errors");

        return "Processed " + processed + " mssdr_persondocumentupload row(s): " + created
                + " SDR_PersonDocumentUpload created, " + skippedNoPerson + " skipped (no matching "
                + "SDR_Person), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int personId) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRPersonDocUploadMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Person_ID", personId);

            setIfPresent(po, "SDR_OriginalFileName", rs.getString("originalfilename"));
            setIfPresent(po, "SDR_SavedFileName", rs.getString("savedfilename"));
            setIfPresent(po, "SDR_FilePath", rs.getString("filepath"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_persondocumentupload",
                        "sdr_persondocumentupload_id", newId, created, updated, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private static void setIfPresent(PO po, String columnName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_persondocumentupload.id=" + sourceId + ": " + e.getMessage());
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
