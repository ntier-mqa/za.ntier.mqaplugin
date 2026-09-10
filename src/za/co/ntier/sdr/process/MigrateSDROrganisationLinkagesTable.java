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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisationlinkages
 * into SDR_OrganisationLinkages (596 source rows, self-referencing). Requires
 * {@link MigrateSDROrganisationTable} to have already fully run - both Parent/ChildOrganisation_ID
 * resolve via the same SDR_Organisation crosswalk used everywhere else in this family.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationLinkagesTable")
public class MigrateSDROrganisationLinkagesTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationLinkages";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationLinkagesTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> financialYearCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_financialyear",
                "sdr_financialyear_id", get_TrxName());
        Map<Integer, Integer> financialYearEndCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_financialyearend", "sdr_financialyearend_id", get_TrxName());

        String sql = "SELECT l.* FROM mssdr_organisationlinkages l "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationlinkages s WHERE s.id = l.id) "
                + "ORDER BY l.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDROrgLinkagesRead");
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
                    processOneRow(table, rs, orgCrosswalk, financialYearCrosswalk, financialYearEndCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-organisationlinkages-errors");

        return "Processed " + processed + " mssdr_organisationlinkages row(s): " + created + " "
                + "SDR_OrganisationLinkages created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> orgCrosswalk,
            Map<Integer, Integer> financialYearCrosswalk, Map<Integer, Integer> financialYearEndCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgLinkagesMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_ParentOrganisation_ID", SDRMigrationSupport.resolveLookup(orgCrosswalk,
                    rs.getInt("parentorganisationid")));
            setIfPresent(po, "SDR_ChildOrganisation_ID", SDRMigrationSupport.resolveLookup(orgCrosswalk,
                    rs.getInt("childorganisationid")));
            setIfPresent(po, "SDR_LinkStartDate", rs.getTimestamp("linkstartdate"));
            setIfPresent(po, "SDR_LinkEndDate", rs.getTimestamp("linkenddate"));
            po.set_ValueOfColumn("SDR_UploadDocument_ID", SDRMigrationSupport.toBD(rs.getInt("uploaddocumentid")));
            po.set_ValueOfColumn("SDR_RemoveDocument_ID", SDRMigrationSupport.toBD(rs.getInt("removedocumentid")));
            setIfPresent(po, "SDR_FinancialYear_ID", SDRMigrationSupport.resolveLookup(financialYearCrosswalk,
                    rs.getInt("financialyearid")));
            setIfPresent(po, "SDR_FinancialYearStart_ID", SDRMigrationSupport.resolveLookup(
                    financialYearCrosswalk, rs.getInt("financialyearstartid")));
            setIfPresent(po, "SDR_FinancialYearEnd_ID", SDRMigrationSupport.resolveLookup(
                    financialYearEndCrosswalk, rs.getInt("financialyearendid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationlinkages", "sdr_organisationlinkages_id",
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
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_organisationlinkages.id=" + sourceId + ": " + e.getMessage());
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
