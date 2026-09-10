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
 * mssdr_organisationbankingdetails into SDR_OrganisationBankingDetails (1,390 source rows). Requires
 * {@link MigrateSDROrganisationTable} to have already run.
 *
 * <p>SDR_SDF_ID will resolve to null for every row until {@code MigrateSDRSDFTable} (Phase 4, not yet
 * written at the time this class was written) has actually loaded data into SDR_SDF - the crosswalk
 * naturally has zero entries until then. This is NOT the same as the mapping doc's documented 20.8%
 * partial-coverage rate (a real data-sparseness fact); it is a migration-ordering artifact. Since this
 * process is idempotent by skipping already-migrated rows, simply re-running it later will NOT
 * backfill SDR_SDF_ID on rows already migrated - re-run with a cleared SDR_OrganisationBankingDetails
 * table (or a dedicated backfill UPDATE, not yet written) if full SDF coverage is wanted after the
 * fact. Recommend running {@code MigrateSDRSDFTable} before this process for the cleanest result.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationBankingDetailsTable")
public class MigrateSDROrganisationBankingDetailsTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationBankingDetails";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationBankingDetailsTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> bankNameCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_bankname",
                "sdr_bankname_id", get_TrxName());
        Map<Integer, Integer> accountTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_accounttype",
                "sdr_accounttype_id", get_TrxName());
        Map<Integer, Integer> yesNoCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_yesno", "sdr_yesno_id",
                get_TrxName());
        Map<Integer, Integer> sdfCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdf", "sdr_sdf_id",
                get_TrxName());
        if (sdfCrosswalk.isEmpty()) {
            addLog("NOTE: SDR_SDF has no migrated rows yet - SDR_SDF_ID will be left unset for every row "
                    + "in this run. Re-run MigrateSDRSDFTable first, then re-migrate this table from a "
                    + "cleared state for full coverage.");
        }

        String sql = "SELECT b.* FROM mssdr_organisationbankingdetails b "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationbankingdetails s WHERE s.id = b.id) "
                + "ORDER BY b.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDROrgBankingDetailsRead");
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
                    processOneRow(table, rs, orgId, bankNameCrosswalk, accountTypeCrosswalk, yesNoCrosswalk,
                            sdfCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-organisationbankingdetails-errors");

        return "Processed " + processed + " mssdr_organisationbankingdetails row(s): " + created + " "
                + "SDR_OrganisationBankingDetails created, " + skippedNoOrg + " skipped (no matching "
                + "SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<Integer, Integer> bankNameCrosswalk,
            Map<Integer, Integer> accountTypeCrosswalk, Map<Integer, Integer> yesNoCrosswalk,
            Map<Integer, Integer> sdfCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgBankingDetailsMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            setIfPresent(po, "SDR_AccountHolder", rs.getString("accountholder"));
            setIfPresent(po, "SDR_BankName_ID", SDRMigrationSupport.resolveLookup(bankNameCrosswalk,
                    rs.getInt("banknameid")));
            setIfPresent(po, "SDR_AccountType_ID", SDRMigrationSupport.resolveLookup(accountTypeCrosswalk,
                    rs.getInt("accounttypeid")));
            setIfPresent(po, "SDR_BranchName", rs.getString("branchname"));
            setIfPresent(po, "SDR_BranchCode", rs.getString("branchcode"));
            setIfPresent(po, "SDR_Status", rs.getString("status"));
            po.set_ValueOfColumn("SDR_VerifiedBy", SDRMigrationSupport.toBD(rs.getInt("verifiedby")));
            po.set_ValueOfColumn("SDR_EvaluatedBy", SDRMigrationSupport.toBD(rs.getInt("evaluatedby")));
            setIfPresent(po, "SDR_VerifiedDate", rs.getTimestamp("verifieddate"));
            setIfPresent(po, "SDR_EvaluatedDate", rs.getTimestamp("evaluateddate"));
            setIfPresent(po, "SDR_AdminDetailsCorrect_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("admindetailscorrectid")));
            setIfPresent(po, "SDR_BankDetailsCorrect_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("bankdetailscorrectid")));
            setIfPresent(po, "SDR_BankDetailsChanged_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("bankdetailschangedid")));
            po.set_ValueOfColumn("SDR_NewRegCompany_ID", SDRMigrationSupport.toBD(rs.getInt("newregcompanyid")));
            setIfPresent(po, "SDR_RegistrationDate", rs.getTimestamp("registrationdate"));
            po.set_ValueOfColumn("SDR_ConfirmDetails", SDRMigrationSupport.toBD(rs.getInt("confirmdetails")));
            setIfPresent(po, "SDR_AccountNumber", rs.getString("accountnumber"));
            // SDR_SDF_ID is DisplayType.Integer at the schema level (deferred until SDR_SDF exists
            // as a Table reference) - BigDecimal-backed regardless.
            setIfPresent(po, "SDR_SDF_ID", SDRMigrationSupport.toBD(
                    SDRMigrationSupport.resolveLookup(sdfCrosswalk, rs.getInt("sdfid"))));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationbankingdetails",
                        "sdr_organisationbankingdetails_id", newId, created, updated, trxName);
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
            errors.add("mssdr_organisationbankingdetails.id=" + sourceId + ": " + e.getMessage());
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
