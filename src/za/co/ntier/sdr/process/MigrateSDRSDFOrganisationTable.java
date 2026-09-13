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
 * Phase 4 (see "Phase 4 - SDF Family - Mapping.txt"): migrates mssdr_sdforganisation into
 * SDR_SDFOrganisation (4,808 source rows). Requires {@link MigrateSDRSDFTable} and
 * {@link MigrateSDROrganisationTable} to have already run.
 *
 * <p>SDR_ApprovedBy_ID/SDR_RejectedBy_ID resolve against iDempiere's own AD_User (Search reference), not a
 * crosswalk table - the raw source int is used directly (0 left unset via setIfPresent's zero-skip).
 * SDR_AppointmentProcedure_ID is a Table(18) reference (name mismatch with its target
 * SDR_SDFAppointmentProcedure) - resolved via the same generic id-crosswalk as any other lookup.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRSDFOrganisationTable")
public class MigrateSDRSDFOrganisationTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_SDFOrganisation";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRSDFOrganisationTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> sdfCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdf", "sdr_sdf_id",
                get_TrxName());
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
        Map<Integer, Integer> statusCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdfstatus",
                "sdr_sdfstatus_id", get_TrxName());
        Map<Integer, Integer> roleCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdfrole", "sdr_sdfrole_id",
                get_TrxName());
        Map<Integer, Integer> functionCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_sdffunction",
                "sdr_sdffunction_id", get_TrxName());
        Map<Integer, Integer> appointmentProcedureCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_sdfappointmentprocedure", "sdr_sdfappointmentprocedure_id", get_TrxName());
        addLog("Crosswalks ready: " + sdfCrosswalk.size() + " SDFs, " + orgCrosswalk.size() + " organisations.");

        String sql = "SELECT o.* FROM mssdr_sdforganisation o "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_sdforganisation s WHERE s.id = o.id) "
                + "ORDER BY o.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoParent = 0;
        String readTrxName = Trx.createTrxName("SDRSDFOrgRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer sdfId = sdfCrosswalk.get(rs.getInt("sdfid"));
                Integer orgId = orgCrosswalk.get(rs.getInt("organisationid"));
                if (sdfId == null || orgId == null) {
                    skippedNoParent++;
                    continue;
                }
                try {
                    processOneRow(table, rs, sdfId, orgId, statusCrosswalk, roleCrosswalk, functionCrosswalk,
                            appointmentProcedureCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-sdforganisation-errors");

        return "Processed " + processed + " mssdr_sdforganisation row(s): " + created + " "
                + "SDR_SDFOrganisation created, " + skippedNoParent + " skipped (no matching SDR_SDF/"
                + "SDR_Organisation), " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int sdfId, int orgId,
            Map<Integer, Integer> statusCrosswalk, Map<Integer, Integer> roleCrosswalk,
            Map<Integer, Integer> functionCrosswalk, Map<Integer, Integer> appointmentProcedureCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRSDFOrgMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_SDF_ID", sdfId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            setIfPresent(po, "SDR_StartDate", rs.getTimestamp("startdate"));
            setIfPresent(po, "SDR_EndDate", rs.getTimestamp("enddate"));
            setIfPresent(po, "SDR_SDFStatus_ID", SDRMigrationSupport.resolveLookup(statusCrosswalk,
                    rs.getInt("sdfstatusid")));
            setIfPresent(po, "SDR_SDFRole_ID", SDRMigrationSupport.resolveLookup(roleCrosswalk,
                    rs.getInt("sdfroleid")));
            setIfPresent(po, "SDR_DateApproved", rs.getTimestamp("dateapproved"));
            setIfPresent(po, "SDR_ApprovedBy_ID", rs.getInt("approvedby"));
            setIfPresent(po, "SDR_DateRejected", rs.getTimestamp("daterejected"));
            setIfPresent(po, "SDR_RejectedBy_ID", rs.getInt("rejectedby"));
            po.set_ValueOfColumn("SDR_ActingForEmployer", SDRMigrationSupport.toBD(rs.getInt("actingforemployer")));
            setIfPresent(po, "SDR_SDFFunction_ID", SDRMigrationSupport.resolveLookup(functionCrosswalk,
                    rs.getInt("sdffunctionid")));
            setIfPresent(po, "SDR_AppointmentProcedure_ID", SDRMigrationSupport.resolveLookup(
                    appointmentProcedureCrosswalk, rs.getInt("appointmentprocedureid")));
            setIfPresent(po, "SDR_AppointmentProcedureOther", rs.getString("appointmentprocedureother"));
            po.set_ValueOfColumn("SDR_ReplacingPrimarySDF", SDRMigrationSupport.toBD(rs.getInt("replacingprimarysdf")));
            po.set_ValueOfColumn("SDR_SecondarySDF", SDRMigrationSupport.toBD(rs.getInt("secondarysdf")));
            setIfPresent(po, "SDR_PreviousSDF", rs.getString("previoussdf"));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_sdforganisation", "sdr_sdforganisation_id", newId,
                        created, updated, trxName);
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
        if (value instanceof Integer && (Integer) value == 0) {
            return;
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_sdforganisation.id=" + sourceId + ": " + e.getMessage());
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
