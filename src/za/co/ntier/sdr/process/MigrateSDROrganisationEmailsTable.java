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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisationemails into
 * SDR_OrganisationEmails (11,645 source rows). Requires {@link MigrateSDROrganisationTable} to have
 * already run. SDR_GrantPaidLetter_ID/SDR_DGApplication_ID stay plain integers (both confirmed always
 * 0/null, effectively dead columns).
 *
 * <p>SDR_WSPATR_ID carries the same "will be null until SDR_WSPATR has data" caveat as
 * {@link MigrateSDROrganisationDocumentsTable}.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationEmailsTable")
public class MigrateSDROrganisationEmailsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationEmails";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationEmailsTable first");
        }

        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        if (wspatrCrosswalk.isEmpty()) {
            addLog("NOTE: SDR_WSPATR has no migrated rows yet - SDR_WSPATR_ID will be left unset for "
                    + "every row in this run.");
        }

        String sql = "SELECT e.* FROM mssdr_organisationemails e "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationemails s WHERE s.id = e.id) "
                + "ORDER BY e.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDROrgEmailsRead");
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
                    addLog("Processed " + processed + " mssdr_organisationemails rows (" + created
                            + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-organisationemails-errors");

        return "Processed " + processed + " mssdr_organisationemails row(s): " + created + " "
                + "SDR_OrganisationEmails created, " + skippedNoOrg + " skipped (no matching "
                + "SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<Integer, Integer> wspatrCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgEmailsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            setIfPresent(po, "SDR_ToAddress", rs.getString("toaddress"));
            setIfPresent(po, "SDR_FromAddress", rs.getString("fromaddress"));
            setIfPresent(po, "SDR_Subject", rs.getString("subject"));
            setIfPresent(po, "SDR_Body", rs.getString("body"));
            setIfPresent(po, "SDR_OriginalFileName", rs.getString("originalfilename"));
            setIfPresent(po, "SDR_SavedFileName", rs.getString("savedfilename"));
            setIfPresent(po, "SDR_FilePath", rs.getString("filepath"));
            po.set_ValueOfColumn("SDR_IsSuccessful", SDRMigrationSupport.toBD(rs.getInt("issuccessful")));
            setIfPresent(po, "SDR_MessageStatus", rs.getString("messagestatus"));
            po.set_ValueOfColumn("SDR_GrantPaidLetter_ID",
                    SDRMigrationSupport.toBD(rs.getInt("grantpaidletterid")));
            setIfPresent(po, "SDR_EmailSentError", rs.getString("emailsenterror"));
            po.set_ValueOfColumn("SDR_DGApplication_ID", SDRMigrationSupport.toBD(rs.getInt("dgapplicationid")));
            // SDR_WSPATR_ID was upgraded to a Table(18) reference by UpgradeOrganisationWSPATRLinks -
            // CONFIRMED via AD_Column 2026-09-11 - Integer-backed, NOT routed through toBD().
            setIfPresent(po, "SDR_WSPATR_ID", SDRMigrationSupport.resolveLookup(wspatrCrosswalk,
                    rs.getInt("wspatrid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationemails", "sdr_organisationemails_id",
                        newId, created, updated, trxName);
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
            errors.add("mssdr_organisationemails.id=" + sourceId + ": " + e.getMessage());
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
