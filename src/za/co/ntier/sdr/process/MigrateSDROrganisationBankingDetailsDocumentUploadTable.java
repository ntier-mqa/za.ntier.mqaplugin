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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates
 * mssdr_organisationbankingdetailsdocumentupload into SDR_OrganisationBankingDetailsDocumentUpload
 * (1,282 source rows). Requires {@link MigrateSDROrganisationTable} and
 * {@link MigrateSDROrganisationBankingDetailsTable} to have already run.
 *
 * <p>SDR_SDF_ID carries the same "will be null until SDR_SDF has data" caveat as
 * {@link MigrateSDROrganisationBankingDetailsTable} - see that class's Javadoc.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationBankingDetailsDocumentUploadTable")
public class MigrateSDROrganisationBankingDetailsDocumentUploadTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationBankingDetailsDocumentUpload";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationBankingDetailsDocumentUploadTable "
                    + "first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> bankingDetailsCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_organisationbankingdetails", "sdr_organisationbankingdetails_id", get_TrxName());
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> sdfCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdf", "sdr_sdf_id",
                get_TrxName());

        String sql = "SELECT d.* FROM mssdr_organisationbankingdetailsdocumentupload d "
                + "WHERE NOT EXISTS "
                + "(SELECT 1 FROM sdr_organisationbankingdetailsdocumentupload s WHERE s.id = d.id) "
                + "ORDER BY d.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoParent = 0;
        String readTrxName = Trx.createTrxName("SDROrgBankingDocUploadRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer bankingDetailsId = bankingDetailsCrosswalk.get(rs.getInt("organisationbankingdetailsid"));
                Integer orgId = orgCrosswalk.get(rs.getInt("organisationid"));
                if (bankingDetailsId == null || orgId == null) {
                    skippedNoParent++;
                    continue;
                }
                try {
                    processOneRow(table, rs, bankingDetailsId, orgId, sdfCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-organisationbankingdetailsdocumentupload-errors");

        return "Processed " + processed + " mssdr_organisationbankingdetailsdocumentupload row(s): " + created
                + " SDR_OrganisationBankingDetailsDocumentUpload created, " + skippedNoParent + " skipped (no "
                + "matching parent), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int bankingDetailsId, int orgId,
            Map<Integer, Integer> sdfCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgBankingDocUploadMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_OrganisationBankingDetails_ID", bankingDetailsId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            // SDR_SDF_ID is DisplayType.Integer at the schema level (deferred until SDR_SDF exists
            // as a Table reference) - BigDecimal-backed regardless.
            setIfPresent(po, "SDR_SDF_ID", SDRMigrationSupport.toBD(
                    SDRMigrationSupport.resolveLookup(sdfCrosswalk, rs.getInt("sdfid"))));
            setIfPresent(po, "SDR_OriginalFileName", rs.getString("originalfilename"));
            setIfPresent(po, "SDR_SavedFileName", rs.getString("savedfilename"));
            setIfPresent(po, "SDR_FilePath", rs.getString("filepath"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationbankingdetailsdocumentupload",
                        "sdr_organisationbankingdetailsdocumentupload_id", newId, created, updated, trxName);
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
            errors.add("mssdr_organisationbankingdetailsdocumentupload.id=" + sourceId + ": " + e.getMessage());
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
