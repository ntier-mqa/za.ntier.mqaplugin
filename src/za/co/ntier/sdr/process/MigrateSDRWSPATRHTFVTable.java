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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrhtfv into
 * SDR_WSPATRHTFV (4,657 source rows). Requires {@link MigrateSDRWSPATRTable} to have already run.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRHTFVTable")
public class MigrateSDRWSPATRHTFVTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATRHTFV";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRWSPATRHTFVTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> ofoSpecialisationCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_ofospecialization", "sdr_ofospecialization_id", get_TrxName());
        Map<Integer, Integer> scarceReasonCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspscarcereason",
                "sdr_wspscarcereason_id", get_TrxName());

        String sql = "SELECT h.* FROM mssdr_wspatrhtfv h "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrhtfv s WHERE s.id = h.id) "
                + "ORDER BY h.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRHTFVRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer wspatrId = wspatrCrosswalk.get(rs.getInt("wspatrid"));
                if (wspatrId == null) {
                    skippedNoWspatr++;
                    continue;
                }
                try {
                    processOneRow(table, rs, wspatrId, ofoSpecialisationCrosswalk, scarceReasonCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-wspatrhtfv-errors");

        return "Processed " + processed + " mssdr_wspatrhtfv row(s): " + created + " SDR_WSPATRHTFV "
                + "created, " + skippedNoWspatr + " skipped (no matching SDR_WSPATR), " + errors.size()
                + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId,
            Map<Integer, Integer> ofoSpecialisationCrosswalk, Map<Integer, Integer> scarceReasonCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRHTFVMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_WSPATR_ID", wspatrId);

            po.set_ValueOfColumn("SDR_WSPATRImport_ID", SDRMigrationSupport.toBD(rs.getInt("wspatrimportid")));
            setIfPresent(po, "SDR_OFOSpecialisation_ID", SDRMigrationSupport.resolveLookup(
                    ofoSpecialisationCrosswalk, rs.getInt("ofospecialisationid")));
            setIfPresent(po, "SDR_PrimaryReason_ID", SDRMigrationSupport.resolveLookup(scarceReasonCrosswalk,
                    rs.getInt("primaryreasonid")));
            setIfPresent(po, "SDR_FirstReason_ID", SDRMigrationSupport.resolveLookup(scarceReasonCrosswalk,
                    rs.getInt("firstreasonid")));
            setIfPresent(po, "SDR_SecondReason_ID", SDRMigrationSupport.resolveLookup(scarceReasonCrosswalk,
                    rs.getInt("secondreasonid")));
            setIfPresent(po, "SDR_Comments", rs.getString("comments"));

            po.set_ValueOfColumn("SDR_EC", SDRMigrationSupport.toBD(rs.getInt("ec")));
            po.set_ValueOfColumn("SDR_FS", SDRMigrationSupport.toBD(rs.getInt("fs")));
            po.set_ValueOfColumn("SDR_GP", SDRMigrationSupport.toBD(rs.getInt("gp")));
            po.set_ValueOfColumn("SDR_KZN", SDRMigrationSupport.toBD(rs.getInt("kzn")));
            po.set_ValueOfColumn("SDR_LP", SDRMigrationSupport.toBD(rs.getInt("lp")));
            po.set_ValueOfColumn("SDR_MP", SDRMigrationSupport.toBD(rs.getInt("mp")));
            po.set_ValueOfColumn("SDR_NP", SDRMigrationSupport.toBD(rs.getInt("np")));
            po.set_ValueOfColumn("SDR_NW", SDRMigrationSupport.toBD(rs.getInt("nw")));
            po.set_ValueOfColumn("SDR_WC", SDRMigrationSupport.toBD(rs.getInt("wc")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrhtfv", "sdr_wspatrhtfv_id", newId, created,
                        updated, trxName);
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
            errors.add("mssdr_wspatrhtfv.id=" + sourceId + ": " + e.getMessage());
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
