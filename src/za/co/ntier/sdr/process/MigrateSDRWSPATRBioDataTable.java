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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): migrates mssdr_wspatrbiodata into
 * SDR_WSPATRBioData - the SECOND-LARGEST table in this migration (1,949,395 source rows). Requires
 * {@link MigrateSDRWSPATRTable} to have already run.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRWSPATRBioDataTable")
public class MigrateSDRWSPATRBioDataTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_WSPATRBioData";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRWSPATRBioDataTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> wspatrCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspatr",
                "sdr_wspatr_id", get_TrxName());
        Map<Integer, Integer> yearCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_year", "sdr_year_id",
                get_TrxName());
        Map<Integer, Integer> genderCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_gender",
                "sdr_gender_id", get_TrxName());
        Map<Integer, Integer> equityCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_equity",
                "sdr_equity_id", get_TrxName());
        Map<Integer, Integer> yesNoCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_yesno", "sdr_yesno_id",
                get_TrxName());
        Map<Integer, Integer> hasSouthAfricanCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_hassouthafrican", "sdr_hassouthafrican_id", get_TrxName());
        Map<Integer, Integer> provinceCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_province",
                "sdr_province_id", get_TrxName());
        Map<Integer, Integer> wspMunicipalityCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspmunicipality", "sdr_wspmunicipality_id", get_TrxName());
        Map<Integer, Integer> wspQualTypeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspqualificationtype", "sdr_wspqualificationtype_id", get_TrxName());
        Map<Integer, Integer> learningProgrammeCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_learningprogramme", "sdr_learningprogramme_id", get_TrxName());
        Map<Integer, Integer> wspAppointmentCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_wspappointment",
                "sdr_wspappointment_id", get_TrxName());
        Map<Integer, Integer> managementEquityCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_wspmanagementequity", "sdr_wspmanagementequity_id", get_TrxName());
        Map<Integer, Integer> ofoSpecialisationCrosswalk = SDRMigrationSupport.buildIdCrosswalk(
                "sdr_ofospecialization", "sdr_ofospecialization_id", get_TrxName());
        addLog("Crosswalks ready: " + wspatrCrosswalk.size() + " WSPATRs.");

        String sql = "SELECT b.* FROM mssdr_wspatrbiodata b "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_wspatrbiodata s WHERE s.id = b.id) "
                + "ORDER BY b.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoWspatr = 0;
        String readTrxName = Trx.createTrxName("SDRWSPATRBioDataRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(2000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer wspatrId = wspatrCrosswalk.get(rs.getInt("wspatrid"));
                if (wspatrId == null) {
                    skippedNoWspatr++;
                    continue;
                }
                try {
                    processOneRow(table, rs, wspatrId, yearCrosswalk, genderCrosswalk, equityCrosswalk,
                            yesNoCrosswalk, hasSouthAfricanCrosswalk, provinceCrosswalk, wspMunicipalityCrosswalk,
                            wspQualTypeCrosswalk, learningProgrammeCrosswalk, wspAppointmentCrosswalk,
                            managementEquityCrosswalk, ofoSpecialisationCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 100000 == 0) {
                    addLog("Processed " + processed + " mssdr_wspatrbiodata rows (" + created + " created, "
                            + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-wspatrbiodata-errors");

        return "Processed " + processed + " mssdr_wspatrbiodata row(s): " + created + " SDR_WSPATRBioData "
                + "created, " + skippedNoWspatr + " skipped (no matching SDR_WSPATR), " + errors.size()
                + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int wspatrId, Map<Integer, Integer> yearCrosswalk,
            Map<Integer, Integer> genderCrosswalk, Map<Integer, Integer> equityCrosswalk,
            Map<Integer, Integer> yesNoCrosswalk, Map<Integer, Integer> hasSouthAfricanCrosswalk,
            Map<Integer, Integer> provinceCrosswalk, Map<Integer, Integer> wspMunicipalityCrosswalk,
            Map<Integer, Integer> wspQualTypeCrosswalk, Map<Integer, Integer> learningProgrammeCrosswalk,
            Map<Integer, Integer> wspAppointmentCrosswalk, Map<Integer, Integer> managementEquityCrosswalk,
            Map<Integer, Integer> ofoSpecialisationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRWSPATRBioDataMigrate");
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
            setIfPresent(po, "SDR_BirthYear_ID", SDRMigrationSupport.resolveLookup(yearCrosswalk,
                    rs.getInt("birthyearid")));
            setIfPresent(po, "SDR_Gender_ID", SDRMigrationSupport.resolveLookup(genderCrosswalk,
                    rs.getInt("genderid")));
            setIfPresent(po, "SDR_Race_ID", SDRMigrationSupport.resolveLookup(equityCrosswalk,
                    rs.getInt("raceid")));
            setIfPresent(po, "SDR_Disabled_ID", SDRMigrationSupport.resolveLookup(yesNoCrosswalk,
                    rs.getInt("disabledid")));
            setIfPresent(po, "SDR_SACitizen_ID", SDRMigrationSupport.resolveLookup(hasSouthAfricanCrosswalk,
                    rs.getInt("sacitizenid")));
            setIfPresent(po, "SDR_Province_ID", SDRMigrationSupport.resolveLookup(provinceCrosswalk,
                    rs.getInt("provinceid")));
            setIfPresent(po, "SDR_Municipality_ID", SDRMigrationSupport.resolveLookup(wspMunicipalityCrosswalk,
                    rs.getInt("municipalityid")));
            setIfPresent(po, "SDR_HighestQualType_ID", SDRMigrationSupport.resolveLookup(wspQualTypeCrosswalk,
                    rs.getInt("highestqualtypeid")));
            setIfPresent(po, "SDR_LearningProgramme_ID", SDRMigrationSupport.resolveLookup(
                    learningProgrammeCrosswalk, rs.getInt("learningprogrammeid")));
            setIfPresent(po, "SDR_Qualification", rs.getString("qualification"));
            setIfPresent(po, "SDR_EmpStatus_ID", SDRMigrationSupport.resolveLookup(wspAppointmentCrosswalk,
                    rs.getInt("empstatusid")));
            setIfPresent(po, "SDR_EmpStartYear_ID", SDRMigrationSupport.resolveLookup(yearCrosswalk,
                    rs.getInt("empstartyearid")));
            setIfPresent(po, "SDR_ManagementEquity_ID", SDRMigrationSupport.resolveLookup(
                    managementEquityCrosswalk, rs.getInt("managementequityid")));
            setIfPresent(po, "SDR_OrgStructure", rs.getString("orgstructure"));
            setIfPresent(po, "SDR_PostRef", rs.getString("postref"));
            setIfPresent(po, "SDR_JobTitle", rs.getString("jobtitle"));
            setIfPresent(po, "SDR_OFOSpecialisation_ID", SDRMigrationSupport.resolveLookup(
                    ofoSpecialisationCrosswalk, rs.getInt("ofospecialisationid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_wspatrbiodata", "sdr_wspatrbiodata_id", newId,
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
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return;
        }
        po.set_ValueOfColumn(columnName, value);
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("mssdr_wspatrbiodata.id=" + sourceId + ": " + e.getMessage());
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
