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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrannualtrainingreport into
 * SDR_WSPATRAnnualTrainingReport - the LARGEST table in this entire migration (7,637,240 source rows).
 * Requires {@link MigrateSDRWSPATRTable} to have already run.
 *
 * <p>Given the row count, this uses a larger fetch size and logs progress every 100,000 rows rather
 * than the usual 5,000/50,000 used elsewhere.
 *
 * <p>CORRECTED 2026-09-13: a first run of this process (as a background job) failed after ~2 hours
 * with "This ResultSet is closed" - the single read connection/cursor originally used to stream all
 * 7.6M rows was held open for the entire run and got reclaimed (pool max-lifetime / idle timeout)
 * partway through. Fixed by processing in bounded batches: each batch opens its own short-lived read
 * connection against the same idempotent "WHERE NOT EXISTS" query with a LIMIT, so no single
 * connection needs to survive more than a few minutes. Already-migrated rows are naturally excluded
 * by the NOT EXISTS check, so no offset/keyset bookkeeping is needed between batches.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRAnnualTrainingReportTable")
public class MigrateSDRWSPATRAnnualTrainingReportTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATRAnnualTrainingReport";
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
                    TABLE_NAME + " does not exist - run AddSDRWSPATRAnnualTrainingReportTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> learningProgrammeTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_learningprogrammetype", "sdr_learningprogrammetype_id", get_TrxName());
        Map<Integer, Integer> learningProgrammeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_learningprogramme", "sdr_learningprogramme_id", get_TrxName());
        Map<Integer, Integer> trainingStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspachievementstatus", "sdr_wspachievementstatus_id", get_TrxName());
        Map<Integer, Integer> trainingStatusReasonCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspdropout",
                "sdr_wspdropout_id", get_TrxName());
        Map<Integer, Integer> yearCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_year", "sdr_year_id",
                get_TrxName());
        addLog("Crosswalks ready: " + wspatrCrosswalk.size() + " WSPATRs.");

        final int BATCH_SIZE = 25000;

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        boolean more = true;
        while (more) {
            int batchLimit = BATCH_SIZE;
            if (maxRows > 0) {
                long remaining = maxRows - processed;
                if (remaining <= 0) {
                    break;
                }
                batchLimit = (int) Math.min(BATCH_SIZE, remaining);
            }

            String sql = "SELECT a.* FROM mssdr_wspatrannualtrainingreport a "
                    + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrannualtrainingreport s WHERE s.id = a.id) "
                    + "ORDER BY a.id LIMIT " + batchLimit;

            int rowsInBatch = 0;
            String readTrxName = Trx.createTrxName("SDRWSPATRAnnualTrainingRead");
            Trx readTrx = Trx.get(readTrxName, true);
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = DB.prepareStatement(sql, readTrxName);
                pstmt.setFetchSize(2000);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    rowsInBatch++;
                    processed++;
                    Integer wspatrId = wspatrCrosswalk.get(rs.getInt("wspatrid"));
                    if (wspatrId == null) {
                        skippedNoWspatr++;
                        continue;
                    }
                    try {
                        processOneRow(table, rs, wspatrId, learningProgrammeTypeCrosswalk,
                                learningProgrammeCrosswalk, trainingStatusCrosswalk, trainingStatusReasonCrosswalk,
                                yearCrosswalk);
                        created++;
                    } catch (Exception e) {
                        logError(rs.getInt("id"), e);
                    }

                    if (processed % 100000 == 0) {
                        addLog("Processed " + processed + " mssdr_wspatrannualtrainingreport rows (" + created
                                + " created, " + errors.size() + " error(s))...");
                    }
                }
            } finally {
                DB.close(rs, pstmt);
                readTrx.rollback();
                readTrx.close();
            }

            if (rowsInBatch < batchLimit) {
                more = false;
            }
        }

        writeErrorLogIfAny("migrate-sdr-wspatrannualtrainingreport-errors");

        return "Processed " + processed + " mssdr_wspatrannualtrainingreport row(s): " + created + " "
                + "SDR_WSPATRAnnualTrainingReport created, " + skippedNoWspatr + " skipped (no matching "
                + "SDR_WSPATR), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId,
            Map<Integer, Integer> learningProgrammeTypeCrosswalk, Map<Integer, Integer> learningProgrammeCrosswalk,
            Map<Integer, Integer> trainingStatusCrosswalk, Map<Integer, Integer> trainingStatusReasonCrosswalk,
            Map<Integer, Integer> yearCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRAnnualTrainingMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_WSPATR_ID", wspatrId);

            po.set_ValueOfColumn("SDR_WSPATRImport_ID", SDRMigrationSupport.toBD(rs.getInt("wspatrimportid")));
            setIfPresent(po, "SDR_EmployeeNo", rs.getString("employeeno"));
            setIfPresent(po, "SDR_EmployeeName", rs.getString("employeename"));
            setIfPresent(po, "SDR_LearningProgrammeType_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeTypeCrosswalk, rs.getInt("learningprogrammetypeid")));
            setIfPresent(po, "SDR_LearningProgramme_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeCrosswalk, rs.getInt("learningprogrammeid")));
            setIfPresent(po, "SDR_Qualification", rs.getString("qualification"));
            setIfPresent(po, "SDR_TrainingCost", rs.getBigDecimal("trainingcost"));
            setIfPresent(po, "SDR_TrainingStatus_ID", SDRMigrationSupport.resolveLookup(trainingStatusCrosswalk,
                    rs.getInt("trainingstatusid")));
            setIfPresent(po, "SDR_TrainingStatusReason_ID", SDRMigrationSupport.resolveLookup(
                    trainingStatusReasonCrosswalk, rs.getInt("trainingstatusreasonid")));
            setIfPresent(po, "SDR_YearEnrolled_ID", SDRMigrationSupport.resolveLookup(yearCrosswalk,
                    rs.getInt("yearenrolledid")));
            setIfPresent(po, "SDR_YearCompleted_ID", SDRMigrationSupport.resolveLookup(yearCrosswalk,
                    rs.getInt("yearcompletedid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrannualtrainingreport",
                        "sdr_wspatrannualtrainingreport_id", newId, created, updated, trxName);
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
            errors.add("mssdr_wspatrannualtrainingreport.id=" + sourceId + ": " + e.getMessage());
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
