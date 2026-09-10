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
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): migrates mssdr_personaddress into
 * SDR_PersonAddress (922,350 source rows, not guaranteed 1:1 with SDR_Person - 23 people have more
 * than one row, all kept per the mapping doc's confirmed decision). Requires
 * {@link MigrateSDRPersonTable} to have already run.
 *
 * <p>The 5 shared geography crosswalks (Suburb/City/Municipality/UrbanRural/Province) are built once
 * and reused for both the physical and postal column blocks.
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDRPersonAddressTable")
public class MigrateSDRPersonAddressTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_PersonAddress";
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
            throw new IllegalStateException(TABLE_NAME + " does not exist - run AddSDRPersonAddressTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> personCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_person",
                "sdr_person_id", get_TrxName());
        Map<Integer, Integer> suburbCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_suburb",
                "sdr_suburb_id", get_TrxName());
        Map<Integer, Integer> cityCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_city", "sdr_city_id",
                get_TrxName());
        Map<Integer, Integer> municipalityCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_municipality",
                "sdr_municipality_id", get_TrxName());
        Map<Integer, Integer> urbanRuralCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_urbanrural",
                "sdr_urbanrural_id", get_TrxName());
        Map<Integer, Integer> provinceCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_province",
                "sdr_province_id", get_TrxName());
        addLog("Crosswalks ready: " + personCrosswalk.size() + " persons.");

        String sql = "SELECT a.* FROM mssdr_personaddress a "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_personaddress s WHERE s.id = a.id) "
                + "ORDER BY a.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoPerson = 0;
        String readTrxName = Trx.createTrxName("SDRPersonAddressRead");
        Trx readTrx = Trx.get(readTrxName, true);
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                Integer personId = personCrosswalk.get(rs.getInt("personid"));
                if (personId == null) {
                    skippedNoPerson++;
                    continue;
                }
                try {
                    processOneRow(table, rs, personId, suburbCrosswalk, cityCrosswalk, municipalityCrosswalk,
                            urbanRuralCrosswalk, provinceCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 5000 == 0) {
                    addLog("Processed " + processed + " mssdr_personaddress rows (" + created + " created, "
                            + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny("migrate-sdr-personaddress-errors");

        return "Processed " + processed + " mssdr_personaddress row(s): " + created + " SDR_PersonAddress "
                + "created, " + skippedNoPerson + " skipped (no matching SDR_Person), " + errors.size()
                + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int personId, Map<Integer, Integer> suburbCrosswalk,
            Map<Integer, Integer> cityCrosswalk, Map<Integer, Integer> municipalityCrosswalk,
            Map<Integer, Integer> urbanRuralCrosswalk, Map<Integer, Integer> provinceCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDRPersonAddressMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Person_ID", personId);

            setIfPresent(po, "SDR_PhysicalAddress1", rs.getString("physicaladdress1"));
            setIfPresent(po, "SDR_PhysicalAddress2", rs.getString("physicaladdress2"));
            setIfPresent(po, "SDR_PhysicalAddress3", rs.getString("physicaladdress3"));
            setIfPresent(po, "SDR_PhysicalCode", rs.getString("physicalcode"));
            setIfPresent(po, "SDR_PhysicalSuburb_ID", SDRMigrationSupport.resolveLookup(suburbCrosswalk,
                    rs.getInt("physicalsuburbid")));
            setIfPresent(po, "SDR_PhysicalCity_ID", SDRMigrationSupport.resolveLookup(cityCrosswalk,
                    rs.getInt("physicalcityid")));
            setIfPresent(po, "SDR_PhysicalMunicipality_ID", SDRMigrationSupport.resolveLookup(
                    municipalityCrosswalk, rs.getInt("physicalmunicipalityid")));
            setIfPresent(po, "SDR_PhysicalUrbanRural_ID", SDRMigrationSupport.resolveLookup(urbanRuralCrosswalk,
                    rs.getInt("physicalurbanruralid")));
            setIfPresent(po, "SDR_PhysicalProvince_ID", SDRMigrationSupport.resolveLookup(provinceCrosswalk,
                    rs.getInt("physicalprovinceid")));

            int usePhysicalAsPostal = rs.getInt("usephysicalaspostal");
            if (!rs.wasNull()) {
                po.set_ValueOfColumn("SDR_UsePhysicalAsPostal",
                        SDRMigrationSupport.flagToYN(usePhysicalAsPostal));
            }

            setIfPresent(po, "SDR_PostalAddressLine1", rs.getString("postaladdressline1"));
            setIfPresent(po, "SDR_PostalAddressLine2", rs.getString("postaladdressline2"));
            setIfPresent(po, "SDR_PostalAddressLine3", rs.getString("postaladdressline3"));
            setIfPresent(po, "SDR_PostalCode", rs.getString("postalcode"));
            setIfPresent(po, "SDR_PostalSuburb_ID", SDRMigrationSupport.resolveLookup(suburbCrosswalk,
                    rs.getInt("postalsuburbid")));
            setIfPresent(po, "SDR_PostalCity_ID", SDRMigrationSupport.resolveLookup(cityCrosswalk,
                    rs.getInt("postalcityid")));
            setIfPresent(po, "SDR_PostalMunicipality_ID", SDRMigrationSupport.resolveLookup(
                    municipalityCrosswalk, rs.getInt("postalmunicipalityid")));
            setIfPresent(po, "SDR_PostalUrbanRural_ID", SDRMigrationSupport.resolveLookup(urbanRuralCrosswalk,
                    rs.getInt("postalurbanruralid")));
            setIfPresent(po, "SDR_PostalProvince_ID", SDRMigrationSupport.resolveLookup(provinceCrosswalk,
                    rs.getInt("postalprovinceid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_personaddress", "sdr_personaddress_id", newId,
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
            errors.add("mssdr_personaddress.id=" + sourceId + ": " + e.getMessage());
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
