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
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): migrates
 * mssdr_personhealthfunctioningstatusrating into SDR_PersonHealthFunctioningStatusRating (5,504,896
 * source rows - the largest table in the Person family). Kept as a genuine child/detail table, NOT
 * pivoted, per the mapping doc's confirmed decision. Requires {@link MigrateSDRPersonTable} to have
 * already run.
 *
 * <p>Given the row count, this uses a larger fetch size than the other Person-family migrations and
 * logs progress more frequently.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRPersonHealthFunctioningStatusRatingTable")
public class MigrateSDRPersonHealthFunctioningStatusRatingTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_PersonHealthFunctioningStatusRating";
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
                    TABLE_NAME + " does not exist - run AddSDRPersonHealthFunctioningStatusRatingTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> personCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_person",
                "sdr_person_id", get_TrxName());
        Map<Integer, Integer> statusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_healthfunctioningstatus", "sdr_healthfunctioningstatus_id", get_TrxName());
        Map<Integer, Integer> ratingCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_healthfunctioningrating", "sdr_healthfunctioningrating_id", get_TrxName());
        addLog("Crosswalks ready: " + personCrosswalk.size() + " persons.");

        String sql = "SELECT h.* FROM mssdr_personhealthfunctioningstatusrating h "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_personhealthfunctioningstatusrating s WHERE s.id = h.id) "
                + "ORDER BY h.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoPerson = 0;
        String readTrxName = Trx.createTrxName("SDRPersonHealthRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(2000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer personId = personCrosswalk.get(rs.getInt("personid"));
                if (personId == null) {
                    skippedNoPerson++;
                    continue;
                }
                try {
                    processOneRow(table, rs, personId, statusCrosswalk, ratingCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 50000 == 0) {
                    addLog("Processed " + processed + " mssdr_personhealthfunctioningstatusrating rows ("
                            + created + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-personhealthfunctioningstatusrating-errors");

        return "Processed " + processed + " mssdr_personhealthfunctioningstatusrating row(s): " + created
                + " SDR_PersonHealthFunctioningStatusRating created, " + skippedNoPerson + " skipped (no "
                + "matching SDR_Person), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int personId, Map<Integer, Integer> statusCrosswalk,
            Map<Integer, Integer> ratingCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRPersonHealthMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Person_ID", personId);

            Integer statusId = SDRMigrationSupport.resolveLookup(statusCrosswalk,
                    rs.getInt("healthfunctioningstatusid"));
            if (statusId != null) {
                po.set_ValueOfColumn("SDR_HealthFunctioningStatus_ID", statusId);
            }
            Integer ratingId = SDRMigrationSupport.resolveLookup(ratingCrosswalk,
                    rs.getInt("healthfunctioningratingid"));
            if (ratingId != null) {
                po.set_ValueOfColumn("SDR_HealthFunctioningRating_ID", ratingId);
            }

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_personhealthfunctioningstatusrating",
                        "sdr_personhealthfunctioningstatusrating_id", newId, created, updated, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_personhealthfunctioningstatusrating.id=" + sourceId + ": " + e.getMessage());
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
