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
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): migrates mssdr_person into SDR_Person
 * (923,166 source rows). Every LOOKUP column resolves via {@link SDRMigrationSupport#buildIdCrosswalk}
 * against the target reference table's own "id" recon column - all 16 of them are already-built,
 * fully-populated SDR_ catalogs (built by AddSDRReferenceTables), so this is a straightforward
 * row-by-row copy with FK resolution, unlike the Learner project's equivalent process (which also had
 * to fold physical/postal addresses into C_Location and pivot health ratings - neither applies here,
 * since SDR_PersonAddress and SDR_PersonHealthFunctioningStatusRating are both kept as genuine child
 * tables per the mapping doc's confirmed decisions).
 *
 * <p>SDR_ParentPerson_ID (self-referencing, only 40/923,166 rows populated) is backfilled in a single
 * UPDATE after the main loop, once every row has been inserted and therefore has its own "id"-&gt;PK
 * crosswalk entry available - mirrors the Learner project's identical ZZParentPerson_ID backfill
 * pattern in MigrateMsPersonToZZPerson.
 *
 * <p>Idempotent: rows already present in SDR_Person (matched via the "id" recon column) are skipped,
 * so this can be safely re-run/resumed after a partial failure. Each source row is processed in its
 * own short-lived transaction so one bad row can't roll back the rest of the run.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRPersonTable")
public class MigrateSDRPersonTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_Person";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRPersonTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<String, Map<Integer, Integer>> xw = new HashMap<>();
        xw.put("title", SDRMigrationSupport.buildIdCrosswalk("sdr_title", "sdr_title_id", get_TrxName()));
        xw.put("alternateidtype", SDRMigrationSupport.buildIdCrosswalk("sdr_alternateidtype",
                "sdr_alternateidtype_id", get_TrxName()));
        xw.put("gender", SDRMigrationSupport.buildIdCrosswalk("sdr_gender", "sdr_gender_id", get_TrxName()));
        xw.put("equity", SDRMigrationSupport.buildIdCrosswalk("sdr_equity", "sdr_equity_id", get_TrxName()));
        xw.put("disability", SDRMigrationSupport.buildIdCrosswalk("sdr_disability", "sdr_disability_id",
                get_TrxName()));
        xw.put("homelanguage", SDRMigrationSupport.buildIdCrosswalk("sdr_homelanguage", "sdr_homelanguage_id",
                get_TrxName()));
        xw.put("nationality", SDRMigrationSupport.buildIdCrosswalk("sdr_nationality", "sdr_nationality_id",
                get_TrxName()));
        xw.put("citizenresidentialstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_citizenresidentialstatus",
                "sdr_citizenresidentialstatus_id", get_TrxName()));
        xw.put("socioeconomicstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_socioeconomicstatus",
                "sdr_socioeconomicstatus_id", get_TrxName()));
        xw.put("schoolemis", SDRMigrationSupport.buildIdCrosswalk("sdr_schoolemis", "sdr_schoolemis_id",
                get_TrxName()));
        xw.put("lastschoolyear", SDRMigrationSupport.buildIdCrosswalk("sdr_lastschoolyear", "sdr_lastschoolyear_id",
                get_TrxName()));
        xw.put("statssaareacode", SDRMigrationSupport.buildIdCrosswalk("sdr_statssaareacode",
                "sdr_statssaareacode_id", get_TrxName()));
        xw.put("popiactstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_popiactstatus", "sdr_popiactstatus_id",
                get_TrxName()));
        xw.put("hassouthafrican", SDRMigrationSupport.buildIdCrosswalk("sdr_hassouthafrican",
                "sdr_hassouthafrican_id", get_TrxName()));
        xw.put("verified", SDRMigrationSupport.buildIdCrosswalk("sdr_verified", "sdr_verified_id", get_TrxName()));
        xw.put("immigrantstatus", SDRMigrationSupport.buildIdCrosswalk("sdr_immigrantstatus",
                "sdr_immigrantstatus_id", get_TrxName()));
        addLog("Crosswalks ready.");

        String sql = "SELECT p.* FROM mssdr_person p "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_person s WHERE s.id = p.id) "
                + "ORDER BY p.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        String readTrxName = Trx.createTrxName("SDRPersonRead");
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

                if (processed % 5000 == 0) {
                    addLog("Processed " + processed + " mssdr_person rows (" + created + " created, "
                            + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        addLog("Backfilling SDR_ParentPerson_ID from mssdr_person.parentpersonid...");
        int backfilled = DB.executeUpdateEx(
                "UPDATE sdr_person child SET sdr_parentperson_id = parent.sdr_person_id "
                + "FROM mssdr_person p "
                + "JOIN sdr_person parent ON parent.id = p.parentpersonid "
                + "WHERE child.id = p.id AND p.parentpersonid > 0",
                null, get_TrxName());

        writeErrorLogIfAny("migrate-sdr-person-errors");

        return "Processed " + processed + " mssdr_person row(s): " + created + " SDR_Person created, "
                + backfilled + " SDR_ParentPerson_ID backfilled, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<String, Map<Integer, Integer>> xw)
            throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRPersonMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);

            setIfPresent(po, "SDR_Title_ID", SDRMigrationSupport.resolveLookup(xw.get("title"),
                    rs.getInt("titleid")));
            setIfPresent(po, "SDR_FirstName", rs.getString("firstname"));
            setIfPresent(po, "SDR_MiddleName", rs.getString("middlename"));
            setIfPresent(po, "SDR_MiddleName2", rs.getString("middlename2"));
            setIfPresent(po, "Surname", rs.getString("surname"));
            setIfPresent(po, "SDR_Initials", rs.getString("initials"));
            setIfPresent(po, "SDR_IDNo", rs.getString("idno"));
            setIfPresent(po, "SDR_AlternateIDType_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("alternateidtype"), rs.getInt("alternateidtypeid")));
            setIfPresent(po, "Birthday", rs.getTimestamp("dateofbirth"));
            setIfPresent(po, "SDR_Gender_ID", SDRMigrationSupport.resolveLookup(xw.get("gender"),
                    rs.getInt("genderid")));
            setIfPresent(po, "SDR_Equity_ID", SDRMigrationSupport.resolveLookup(xw.get("equity"),
                    rs.getInt("equityid")));
            setIfPresent(po, "SDR_Disability_ID", SDRMigrationSupport.resolveLookup(xw.get("disability"),
                    rs.getInt("disabilityid")));
            setIfPresent(po, "SDR_HomeLanguage_ID", SDRMigrationSupport.resolveLookup(xw.get("homelanguage"),
                    rs.getInt("homelanguageid")));
            setIfPresent(po, "SDR_Nationality_ID", SDRMigrationSupport.resolveLookup(xw.get("nationality"),
                    rs.getInt("nationalityid")));
            setIfPresent(po, "SDR_CitizenResidentialStatus_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("citizenresidentialstatus"), rs.getInt("citizenresidentialstatusid")));
            setIfPresent(po, "SDR_SocioEconomicStatus_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("socioeconomicstatus"), rs.getInt("socioeconomicstatusid")));
            setIfPresent(po, "Phone", rs.getString("telephonenumber"));
            setIfPresent(po, "CellPhone", rs.getString("cellphonenumber"));
            setIfPresent(po, "Fax", rs.getString("faxnumber"));
            setIfPresent(po, "EMail", rs.getString("email"));
            setIfPresent(po, "SDR_SchoolEMIS_ID", SDRMigrationSupport.resolveLookup(xw.get("schoolemis"),
                    rs.getInt("schoolemisid")));
            setIfPresent(po, "SDR_LastSchoolYear_ID", SDRMigrationSupport.resolveLookup(xw.get("lastschoolyear"),
                    rs.getInt("lastschoolyearid")));
            setIfPresent(po, "SDR_STATSSAAreaCode_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("statssaareacode"), rs.getInt("statssaareacodeid")));
            setIfPresent(po, "SDR_POPIActStatus_ID", SDRMigrationSupport.resolveLookup(xw.get("popiactstatus"),
                    rs.getInt("popiactstatusid")));
            setIfPresent(po, "POPIActStatusDate", rs.getTimestamp("popiactstatusdate"));
            setIfPresent(po, "SDR_HasSouthAfrican_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("hassouthafrican"), rs.getInt("hassouthafricanid")));
            setIfPresent(po, "SDR_Verified_ID", SDRMigrationSupport.resolveLookup(xw.get("verified"),
                    rs.getInt("verifiedid")));
            setIfPresent(po, "SDR_HighestEducation", rs.getString("highesteducation"));
            setIfPresent(po, "SDR_CurrentOccupation", rs.getString("currentoccupation"));
            // Not routed through setIfPresent: 0 is a plain, potentially-genuine value for this
            // column (unlike the FK/lookup columns above, where 0 is the "not set" sentinel) - no
            // zero-skipping wanted here.
            po.set_ValueOfColumn("SDR_YearsInOccupation", rs.getInt("yearsinoccupation"));
            setIfPresent(po, "SDR_Experience", rs.getString("experience"));
            setIfPresent(po, "SDR_ImmigrantStatus_ID", SDRMigrationSupport.resolveLookup(
                    xw.get("immigrantstatus"), rs.getInt("immigrantstatusid")));
            // SDR_ParentPerson_ID intentionally left unset here - backfilled after the main loop,
            // once every row (including the parent) has a crosswalk entry.

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_person", "sdr_person_id", newId, created, updated,
                        trxName);
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
            errors.add("mssdr_person.id=" + sourceId + ": " + e.getMessage());
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
