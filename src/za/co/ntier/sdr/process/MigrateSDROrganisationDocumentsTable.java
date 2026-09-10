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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisationdocuments
 * into SDR_OrganisationDocuments (15,927 source rows). Requires {@link MigrateSDROrganisationTable}
 * to have already run. SDR_DocumentRelates_ID stays a plain integer (per the mapping doc, no working
 * lookup target was found).
 *
 * <p>SDR_WSPATR_ID will resolve to null for every row until {@code MigrateSDRWSPATRTable} (Phase 5,
 * not yet written at the time this class was written) has actually loaded data into SDR_WSPATR - same
 * migration-ordering caveat as {@code MigrateSDROrganisationBankingDetailsTable}.SDR_SDF_ID. Recommend
 * running {@code MigrateSDRWSPATRTable} before this process for the cleanest result.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationDocumentsTable")
public class MigrateSDROrganisationDocumentsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationDocuments";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationDocumentsTable first");
        }

        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        if (wspatrCrosswalk.isEmpty()) {
            addLog("NOTE: SDR_WSPATR has no migrated rows yet - SDR_WSPATR_ID will be left unset for "
                    + "every row in this run.");
        }

        String sql = "SELECT d.* FROM mssdr_organisationdocuments d "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationdocuments s WHERE s.id = d.id) "
                + "ORDER BY d.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDROrgDocumentsRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer orgId = orgCrosswalk.get(rs.getInt("organisationid"));
                if (orgId == null) {
                    skippedNoOrg++;
                    continue;
                }
                try {
                    processOneRow(table, rs, orgId, wspatrCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 5000 == 0) {
                    addLog("Processed " + processed + " mssdr_organisationdocuments rows (" + created
                            + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-organisationdocuments-errors");

        return "Processed " + processed + " mssdr_organisationdocuments row(s): " + created + " "
                + "SDR_OrganisationDocuments created, " + skippedNoOrg + " skipped (no matching "
                + "SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<Integer, Integer> wspatrCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgDocumentsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            po.set_ValueOfColumn("SDR_DocumentRelates_ID",
                    SDRMigrationSupport.toBD(rs.getInt("documentrelatesid")));
            setIfPresent(po, "SDR_Comment", rs.getString("comment"));
            setIfPresent(po, "SDR_OriginalFileName", rs.getString("originalfilename"));
            setIfPresent(po, "SDR_SavedFileName", rs.getString("savedfilename"));
            setIfPresent(po, "SDR_FilePath", rs.getString("filepath"));
            // SDR_WSPATR_ID is DisplayType.Integer at the schema level (deferred until SDR_WSPATR
            // exists as a Table reference) - BigDecimal-backed regardless.
            setIfPresent(po, "SDR_WSPATR_ID", SDRMigrationSupport.toBD(
                    SDRMigrationSupport.resolveLookup(wspatrCrosswalk, rs.getInt("wspatrid"))));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationdocuments",
                        "sdr_organisationdocuments_id", newId, created, updated, trxName);
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
            errors.add("mssdr_organisationdocuments.id=" + sourceId + ": " + e.getMessage());
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
