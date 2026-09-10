package za.co.ntier.sdr.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisation into
 * SDR_Organisation (13,801 source rows). All 22 DHET* columns are already collapsed at the schema
 * level (see AddSDROrganisationTable) - this class simply copies the non-DHET column values, per the
 * mapping doc's confirmed decision.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationTable")
public class MigrateSDROrganisationTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_Organisation";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDROrganisationTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<String, Map<Integer, Integer>> xw = new HashMap<>();
        xw.put("regnumbertype", SDRMigrationSupport.buildIdCrosswalk("sdr_organisationregistrationnumbertype",
                "sdr_organisationregistrationnumbertype_id", get_TrxName()));
        xw.put("typeoforg", SDRMigrationSupport.buildIdCrosswalk("sdr_typeoforganisation",
                "sdr_typeoforganisation_id", get_TrxName()));
        xw.put("legalstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_legalstatus", "sdr_legalstatus_id",
                get_TrxName()));
        xw.put("partnership", SDRMigrationSupport.buildIdCrosswalk("sdr_partnership", "sdr_partnership_id",
                get_TrxName()));
        xw.put("siccode", SDRMigrationSupport.buildIdCrosswalk("sdr_siccode", "sdr_siccode_id", get_TrxName()));
        xw.put("orgsize", SDRMigrationSupport.buildIdCrosswalk("sdr_organisationsize", "sdr_organisationsize_id",
                get_TrxName()));
        xw.put("beestatus", SDRMigrationSupport.buildIdCrosswalk("sdr_beestatus", "sdr_beestatus_id",
                get_TrxName()));
        xw.put("levynumbertype", SDRMigrationSupport.buildIdCrosswalk("sdr_levynumbertype",
                "sdr_levynumbertype_id", get_TrxName()));
        xw.put("chambercode", SDRMigrationSupport.buildIdCrosswalk("sdr_chambercode", "sdr_chambercode_id",
                get_TrxName()));
        xw.put("subsector", SDRMigrationSupport.buildIdCrosswalk("sdr_subsector", "sdr_subsector_id",
                get_TrxName()));
        xw.put("orgtype", SDRMigrationSupport.buildIdCrosswalk("sdr_organisationtype", "sdr_organisationtype_id",
                get_TrxName()));
        xw.put("yesno", SDRMigrationSupport.buildIdCrosswalk("sdr_yesno", "sdr_yesno_id", get_TrxName()));
        addLog("Crosswalks ready.");

        String sql = "SELECT o.* FROM mssdr_organisation o "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisation s WHERE s.id = o.id) "
                + "ORDER BY o.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDROrganisationRead");
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
                    processOneRow(table, rs, xw);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 2000 == 0) {
                    addLog("Processed " + processed + " mssdr_organisation rows (" + created + " created, "
                            + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-organisation-errors");

        return "Processed " + processed + " mssdr_organisation row(s): " + created + " SDR_Organisation "
                + "created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<String, Map<Integer, Integer>> xw)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrganisationMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_SDLNumber", rs.getString("sdlnumber"));
            setIfPresent(po, "SDR_PossibleSDLNumber", rs.getString("possiblesdlnumber"));
            setIfPresent(po, "SDR_LegalName", rs.getString("legalname"));
            setIfPresent(po, "SDR_TradeName", rs.getString("tradename"));
            setIfPresent(po, "SDR_OrganisationRegistrationNumberType_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("regnumbertype"), rs.getInt("organisationregistrationnumbertypeid")));
            setIfPresent(po, "SDR_OrganisationRegistrationNumber", rs.getString("organisationregistrationnumber"));
            setIfPresent(po, "SDR_TypeofOrganisation_ID", SDRMigrationSupport.resolveLookup(xw.get("typeoforg"),
                    rs.getInt("typeoforganisationid")));
            setIfPresent(po, "SDR_LegalStatus_ID", SDRMigrationSupport.resolveLookup(xw.get("legalstatus"),
                    rs.getInt("legalstatusid")));
            setIfPresent(po, "SDR_Partnership_ID", SDRMigrationSupport.resolveLookup(xw.get("partnership"),
                    rs.getInt("partnershipid")));
            setIfPresent(po, "SDR_PhoneNumber", rs.getString("phonenumber"));
            setIfPresent(po, "SDR_FaxNumber", rs.getString("faxnumber"));
            setIfPresent(po, "SDR_SICCode_ID", SDRMigrationSupport.resolveLookup(xw.get("siccode"),
                    rs.getInt("siccodeid")));
            po.set_ValueOfColumn("SDR_NumberOfEmployees", SDRMigrationSupport.toBD(rs.getInt("numberofemployees")));
            setIfPresent(po, "SDR_TotalAnnualPayroll", rs.getBigDecimal("totalannualpayroll"));
            setIfPresent(po, "SDR_SARSNumber", rs.getString("sarsnumber"));
            setIfPresent(po, "SDR_CIPRONumber", rs.getString("cipronumber"));
            setIfPresent(po, "SDR_PAYENumber", rs.getString("payenumber"));
            setIfPresent(po, "SDR_UIFNumber", rs.getString("uifnumber"));
            setIfPresent(po, "SDR_OrganisationSize_ID", SDRMigrationSupport.resolveLookup(xw.get("orgsize"),
                    rs.getInt("organisationsizeid")));
            po.set_ValueOfColumn("SDR_CurrentVsNonCurrent",
                    SDRMigrationSupport.toBD(rs.getInt("currentvsnoncurrent")));
            po.set_ValueOfColumn("SDR_CurrentSetaRegion_ID",
                    SDRMigrationSupport.toBD(rs.getInt("currentsetaregionid")));
            setIfPresent(po, "SDR_BEEStatus_ID", SDRMigrationSupport.resolveLookup(xw.get("beestatus"),
                    rs.getInt("beestatusid")));
            setIfPresent(po, "SDR_LevyNumberType_ID", SDRMigrationSupport.resolveLookup(xw.get("levynumbertype"),
                    rs.getInt("levynumbertypeid")));
            po.set_ValueOfColumn("SDR_Communication", SDRMigrationSupport.toBD(rs.getInt("communication")));
            po.set_ValueOfColumn("SDR_ConfirmDetails", SDRMigrationSupport.toBD(rs.getInt("confirmdetails")));
            setIfPresent(po, "SDR_ChamberCode_ID", SDRMigrationSupport.resolveLookup(xw.get("chambercode"),
                    rs.getInt("chambercodeid")));
            setIfPresent(po, "SDR_Email", rs.getString("email"));
            po.set_ValueOfColumn("SDR_NumberOfEmployeesProfile",
                    SDRMigrationSupport.toBD(rs.getInt("numberofemployeesprofile")));
            po.set_ValueOfColumn("SDR_RegisterAs_ID", SDRMigrationSupport.toBD(rs.getInt("registerasid")));
            setIfPresent(po, "SDR_LegalStatusOther", rs.getString("legalstatusother"));
            po.set_ValueOfColumn("SDR_OrganisationRegNumberCode",
                    SDRMigrationSupport.toBD(rs.getInt("organisationregnumbercode")));
            po.set_ValueOfColumn("SDR_TerminatedEmployees",
                    SDRMigrationSupport.toBD(rs.getInt("terminatedemployees")));
            setIfPresent(po, "SDR_SubSector_ID", SDRMigrationSupport.resolveLookup(xw.get("subsector"),
                    rs.getInt("subsectorid")));
            setIfPresent(po, "SDR_OrganisationType_ID", SDRMigrationSupport.resolveLookup(xw.get("orgtype"),
                    rs.getInt("organisationtypeid")));
            setIfPresent(po, "SDR_UnionisedYesNo_ID", SDRMigrationSupport.resolveLookup(xw.get("yesno"),
                    rs.getInt("unionisedyesnoid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisation", "sdr_organisation_id", newId,
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
            errors.add("mssdr_organisation.id=" + sourceId + ": " + e.getMessage());
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
