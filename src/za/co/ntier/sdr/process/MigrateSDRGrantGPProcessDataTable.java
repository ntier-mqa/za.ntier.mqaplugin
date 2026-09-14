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
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): migrates mssdr_grantgpprocessdata into
 * SDR_GrantGPProcessData (50,139 source rows) - low business value. Requires
 * {@link MigrateSDRGrantProcessTable} to have already run.
 *
 * <p>UNLIKE Levy's equivalent table (SDR_LevyGPProcessData), this source table DOES have a real "id"
 * column, so the usual idempotent "WHERE NOT EXISTS" pattern applies normally here - no special-case
 * handling needed.
 *
 * <p>NOTE: this source table's isdeleted column is stored as nchar(10) TEXT ('0'/'1'), not tinyint
 * like every other table in this migration - read as a String and compared explicitly, not via
 * getInt(), per the mapping doc's explicit callout.
 *
 * <p>SDR_Organisation_ID resolves via the same two-tier {@link SDRMigrationSupport#buildOrganisationCrosswalk}
 * as GrantTransaction.ReferenceNumber - SDR_ReferenceNumber carries the identical composite format.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRGrantGPProcessDataTable")
public class MigrateSDRGrantGPProcessDataTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_GrantGPProcessData";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRGrantGPProcessDataTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> grantProcessCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_grantprocess",
                "sdr_grantprocess_id", get_TrxName());
        SDRMigrationSupport.OrganisationCrosswalk organisationCrosswalk = SDRMigrationSupport
                .buildOrganisationCrosswalk(get_TrxName());
        addLog("Crosswalks ready: " + grantProcessCrosswalk.size() + " GrantProcesses.");

        String sql = "SELECT g.* FROM mssdr_grantgpprocessdata g "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_grantgpprocessdata s WHERE s.id = g.id) "
                + "ORDER BY g.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRGrantGPProcessDataRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(2000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(table, rs, grantProcessCrosswalk, organisationCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 10000 == 0) {
                    addLog("Processed " + processed + " mssdr_grantgpprocessdata rows (" + created
                            + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-grantgpprocessdata-errors");

        return "Processed " + processed + " mssdr_grantgpprocessdata row(s): " + created + " "
                + "SDR_GrantGPProcessData created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> grantProcessCrosswalk,
            SDRMigrationSupport.OrganisationCrosswalk organisationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        String isDeletedText = rs.getString("isdeleted");
        boolean isDeleted = isDeletedText != null && "1".equals(isDeletedText.trim());
        String referenceNumber = rs.getString("referencenumber");

        String orgNumber = null;
        if (referenceNumber != null) {
            String[] parts = referenceNumber.split("\\|", -1);
            if (parts.length >= 3) {
                orgNumber = parts[2];
            }
        }

        String trxName = Trx.createTrxName("SDRGrantGPProcessDataMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(!isDeleted);
            po.set_ValueOfColumn("id", sourceId);

            po.set_ValueOfColumn("SDR_TransactionID", SDRMigrationSupport.toBD(rs.getInt("transactionid")));
            po.set_ValueOfColumn("SDR_ProcessID", SDRMigrationSupport.toBD(rs.getInt("processid")));
            po.set_ValueOfColumn("SDR_ImportID", SDRMigrationSupport.toBD(rs.getInt("importid")));
            setIfPresent(po, "SDR_GrantProcess_ID", SDRMigrationSupport.resolveLookup(grantProcessCrosswalk,
                    rs.getInt("grantprocessid")));
            setIfPresent(po, "SDR_ReferenceNumber", referenceNumber);
            setIfPresent(po, "SDR_Organisation_ID", organisationCrosswalk.resolve(orgNumber));
            setIfPresent(po, "SDR_TransactionDate", rs.getTimestamp("transactiondate"));
            setIfPresent(po, "SDR_MainAccountNumber", rs.getString("mainaccountnumber"));
            setIfPresent(po, "SDR_Total", rs.getBigDecimal("total"));
            setIfPresent(po, "SDR_MainDebitAmount", rs.getBigDecimal("maindebitamount"));
            setIfPresent(po, "SDR_MainCreditAmount", rs.getBigDecimal("maincreditamount"));
            setIfPresent(po, "SDR_ContraAccountNumber", rs.getString("contraaccountnumber"));
            setIfPresent(po, "SDR_ContraDebitAmount", rs.getBigDecimal("contradebitamount"));
            setIfPresent(po, "SDR_ContraCreditAmount", rs.getBigDecimal("contracreditamount"));
            po.set_ValueOfColumn("SDR_IsProcessed", SDRMigrationSupport.toBD(rs.getInt("isprocessed")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_grantgpprocessdata", "sdr_grantgpprocessdata_id",
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
            errors.add("mssdr_grantgpprocessdata.id=" + sourceId + ": " + e.getMessage());
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
