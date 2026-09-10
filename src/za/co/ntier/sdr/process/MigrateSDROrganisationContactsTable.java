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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisationcontacts
 * into SDR_OrganisationContacts (3,382 source rows). Requires {@link MigrateSDROrganisationTable} to
 * have already run.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationContactsTable")
public class MigrateSDROrganisationContactsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationContacts";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationContactsTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> titleCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_title",
                "sdr_title_id", get_TrxName());
        Map<Integer, Integer> provinceCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_province",
                "sdr_province_id", get_TrxName());

        String sql = "SELECT c.* FROM mssdr_organisationcontacts c "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationcontacts s WHERE s.id = c.id) "
                + "ORDER BY c.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDROrgContactsRead");
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
                    processOneRow(table, rs, orgId, titleCrosswalk, provinceCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-organisationcontacts-errors");

        return "Processed " + processed + " mssdr_organisationcontacts row(s): " + created + " "
                + "SDR_OrganisationContacts created, " + skippedNoOrg + " skipped (no matching "
                + "SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<Integer, Integer> titleCrosswalk,
            Map<Integer, Integer> provinceCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgContactsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            setIfPresent(po, "SDR_Title_ID", SDRMigrationSupport.resolveLookup(titleCrosswalk,
                    rs.getInt("titleid")));
            setIfPresent(po, "SDR_FirstName", rs.getString("firstname"));
            setIfPresent(po, "SDR_Surname", rs.getString("surname"));
            setIfPresent(po, "SDR_Designation", rs.getString("designation"));
            setIfPresent(po, "SDR_TelephoneNumber", rs.getString("telephonenumber"));
            setIfPresent(po, "SDR_CellPhoneNumber", rs.getString("cellphonenumber"));
            setIfPresent(po, "SDR_FaxNumber", rs.getString("faxnumber"));
            setIfPresent(po, "SDR_Email", rs.getString("email"));
            setIfPresent(po, "SDR_Province_ID", SDRMigrationSupport.resolveLookup(provinceCrosswalk,
                    rs.getInt("provinceid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationcontacts", "sdr_organisationcontacts_id",
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
            errors.add("mssdr_organisationcontacts.id=" + sourceId + ": " + e.getMessage());
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
