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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrnonemployees into
 * SDR_WSPATRNonEmployees. Requires {@link MigrateSDRWSPATRTable} to have already run. The Done/Planned
 * pairs (LearningProgrammeType, LearningProgramme, NonEmpStatus, TargetBen) each resolve against the
 * same crosswalk reused for both sides of the pair.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRNonEmployeesTable")
public class MigrateSDRWSPATRNonEmployeesTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATRNonEmployees";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRWSPATRNonEmployeesTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> learningProgrammeTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_learningprogrammetype", "sdr_learningprogrammetype_id", get_TrxName());
        Map<Integer, Integer> learningProgrammeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_learningprogramme", "sdr_learningprogramme_id", get_TrxName());
        Map<Integer, Integer> nonEmpStatusCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspnonemployeestatus", "sdr_wspnonemployeestatus_id", get_TrxName());
        Map<Integer, Integer> targetBenCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wsptargetbeneficiary",
                "sdr_wsptargetbeneficiary_id", get_TrxName());

        String sql = "SELECT n.* FROM mssdr_wspatrnonemployees n "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrnonemployees s WHERE s.id = n.id) "
                + "ORDER BY n.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRNonEmpRead");
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
                    processOneRow(table, rs, wspatrId, learningProgrammeTypeCrosswalk, learningProgrammeCrosswalk,
                            nonEmpStatusCrosswalk, targetBenCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-wspatrnonemployees-errors");

        return "Processed " + processed + " mssdr_wspatrnonemployees row(s): " + created + " "
                + "SDR_WSPATRNonEmployees created, " + skippedNoWspatr + " skipped (no matching SDR_WSPATR), "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId,
            Map<Integer, Integer> learningProgrammeTypeCrosswalk, Map<Integer, Integer> learningProgrammeCrosswalk,
            Map<Integer, Integer> nonEmpStatusCrosswalk, Map<Integer, Integer> targetBenCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRNonEmpMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_WSPATR_ID", wspatrId);

            po.set_ValueOfColumn("SDR_WSPATRImport_ID", SDRMigrationSupport.toBD(rs.getInt("wspatrimportid")));

            setIfPresent(po, "SDR_LearningProgrammeTypeDone_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeTypeCrosswalk, rs.getInt("learningprogrammetypedoneid")));
            setIfPresent(po, "SDR_LearningProgrammeDone_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeCrosswalk, rs.getInt("learningprogrammedoneid")));
            setIfPresent(po, "SDR_LPOtherDone", rs.getString("lpotherdone"));
            setIfPresent(po, "SDR_NonEmpStatusDone_ID", SDRMigrationSupport.resolveLookup(nonEmpStatusCrosswalk,
                    rs.getInt("nonempstatusdoneid")));
            setIfPresent(po, "SDR_TargetBenDone_ID", SDRMigrationSupport.resolveLookup(targetBenCrosswalk,
                    rs.getInt("targetbendoneid")));
            po.set_ValueOfColumn("SDR_Male", SDRMigrationSupport.toBD(rs.getInt("male")));
            po.set_ValueOfColumn("SDR_Female", SDRMigrationSupport.toBD(rs.getInt("female")));
            po.set_ValueOfColumn("SDR_African", SDRMigrationSupport.toBD(rs.getInt("african")));
            po.set_ValueOfColumn("SDR_Coloured", SDRMigrationSupport.toBD(rs.getInt("coloured")));
            po.set_ValueOfColumn("SDR_Indian", SDRMigrationSupport.toBD(rs.getInt("indian")));
            po.set_ValueOfColumn("SDR_White", SDRMigrationSupport.toBD(rs.getInt("white")));
            po.set_ValueOfColumn("SDR_DisabledDone", SDRMigrationSupport.toBD(rs.getInt("disableddone")));

            setIfPresent(po, "SDR_LearningProgrammeTypePlanned_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeTypeCrosswalk, rs.getInt("learningprogrammetypeplannedid")));
            setIfPresent(po, "SDR_LearningProgrammePlanned_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeCrosswalk, rs.getInt("learningprogrammeplannedid")));
            setIfPresent(po, "SDR_LPOtherPlanned", rs.getString("lpotherplanned"));
            setIfPresent(po, "SDR_NonEmpStatusPlanned_ID", SDRMigrationSupport.resolveLookup(nonEmpStatusCrosswalk,
                    rs.getInt("nonempstatusplannedid")));
            setIfPresent(po, "SDR_TargetBenPlanned_ID", SDRMigrationSupport.resolveLookup(targetBenCrosswalk,
                    rs.getInt("targetbenplannedid")));
            po.set_ValueOfColumn("SDR_TotalPlanned", SDRMigrationSupport.toBD(rs.getInt("totalplanned")));
            po.set_ValueOfColumn("SDR_DisabledPlanned", SDRMigrationSupport.toBD(rs.getInt("disabledplanned")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrnonemployees", "sdr_wspatrnonemployees_id",
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
            errors.add("mssdr_wspatrnonemployees.id=" + sourceId + ": " + e.getMessage());
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
