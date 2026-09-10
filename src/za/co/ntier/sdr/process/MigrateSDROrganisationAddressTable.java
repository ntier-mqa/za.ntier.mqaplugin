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
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): migrates mssdr_organisationaddress
 * into SDR_OrganisationAddress (13,111 source rows). Requires {@link MigrateSDROrganisationTable} to
 * have already run.
 *
 * <p>UNLIKE the main Organisation table, the DHET collapse does NOT apply here - both the current and
 * DHET physical/postal blocks are copied separately, per the mapping doc's confirmed decision. The 5
 * shared geography crosswalks (Suburb/City/Municipality/UrbanRural/Province) are built once and reused
 * across all 4 blocks (current-physical, current-postal, dhet-physical, dhet-postal).
 */
@Process(name = "za.co.ntier.sdr.process.MigrateSDROrganisationAddressTable")
public class MigrateSDROrganisationAddressTable extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String TABLE_NAME = "SDR_OrganisationAddress";
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
                    TABLE_NAME + " does not exist - run AddSDROrganisationAddressTable first");
        }

        addLog("Building lookup crosswalks...");
        Map<Integer, Integer> orgCrosswalk = SDRMigrationSupport.buildIdCrosswalk("sdr_organisation",
                "sdr_organisation_id", get_TrxName());
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
        addLog("Crosswalks ready: " + orgCrosswalk.size() + " organisations.");

        String sql = "SELECT a.* FROM mssdr_organisationaddress a "
                + "WHERE NOT EXISTS (SELECT 1 FROM sdr_organisationaddress s WHERE s.id = a.id) "
                + "ORDER BY a.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        int skippedNoOrg = 0;
        String readTrxName = Trx.createTrxName("SDROrgAddressRead");
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
                    processOneRow(table, rs, orgId, suburbCrosswalk, cityCrosswalk, municipalityCrosswalk,
                            urbanRuralCrosswalk, provinceCrosswalk);
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

        writeErrorLogIfAny("migrate-sdr-organisationaddress-errors");

        return "Processed " + processed + " mssdr_organisationaddress row(s): " + created + " "
                + "SDR_OrganisationAddress created, " + skippedNoOrg + " skipped (no matching SDR_Organisation), "
                + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, int orgId, Map<Integer, Integer> suburbCrosswalk,
            Map<Integer, Integer> cityCrosswalk, Map<Integer, Integer> municipalityCrosswalk,
            Map<Integer, Integer> urbanRuralCrosswalk, Map<Integer, Integer> provinceCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("SDROrgAddressMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", 0);
            po.setIsActive(isDeleted == 0);
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("SDR_Organisation_ID", orgId);

            // -- current physical block --
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
            setIfPresent(po, "SDR_GPSCoordinates", rs.getString("gpscoordinates"));

            int usePhysicalAsPostal = rs.getInt("usephysicalaspostal");
            if (!rs.wasNull()) {
                po.set_ValueOfColumn("SDR_UsePhysicalAsPostal",
                        SDRMigrationSupport.flagToYN(usePhysicalAsPostal));
            }

            // -- current postal block --
            setIfPresent(po, "SDR_PostalAddressLine1", rs.getString("postaladdressline1"));
            setIfPresent(po, "SDR_PostalAddressLine2", rs.getString("postaladdressline2"));
            setIfPresent(po, "SDR_PostalAddressLine3", rs.getString("postaladdressline3"));
            setIfPresent(po, "SDR_PostalCode", rs.getString("postalcode"));
            setIfPresent(po, "SDR_PostalSuburb_ID", SDRMigrationSupport.resolveLookup(suburbCrosswalk,
                    rs.getInt("postalsuburbid")));
            setIfPresent(po, "SDR_PostalCity_ID", SDRMigrationSupport.resolveLookup(cityCrosswalk,
                    rs.getInt("postalcityid")));
            setIfPresent(po, "SDR_PostalMunicipality_ID", SDRMigrationSupport.resolveLookup(
                    municipalityCrosswalk, rs.getInt("postallmunicipalityid")));
            setIfPresent(po, "SDR_PostalUrbanRural_ID", SDRMigrationSupport.resolveLookup(urbanRuralCrosswalk,
                    rs.getInt("postalurbanruralid")));
            setIfPresent(po, "SDR_PostalProvince_ID", SDRMigrationSupport.resolveLookup(provinceCrosswalk,
                    rs.getInt("postalprovinceid")));

            // -- DHET physical block --
            setIfPresent(po, "SDR_DHETPhysicalAddress1", rs.getString("dhetphysicaladdress1"));
            setIfPresent(po, "SDR_DHETPhysicalAddress2", rs.getString("dhetphysicaladdress2"));
            setIfPresent(po, "SDR_DHETPhysicalAddress3", rs.getString("dhetphysicaladdress3"));
            setIfPresent(po, "SDR_DHETPhysicalCode", rs.getString("dhetphysicalcode"));
            setIfPresent(po, "SDR_DHETPhysicalSuburb_ID", SDRMigrationSupport.resolveLookup(suburbCrosswalk,
                    rs.getInt("dhetphysicalsuburbid")));
            setIfPresent(po, "SDR_DHETPhysicalCity_ID", SDRMigrationSupport.resolveLookup(cityCrosswalk,
                    rs.getInt("dhetphysicalcityid")));
            setIfPresent(po, "SDR_DHETPhysicalMunicipality_ID", SDRMigrationSupport.resolveLookup(
                    municipalityCrosswalk, rs.getInt("dhetphysicalmunicipalityid")));
            setIfPresent(po, "SDR_DHETPhysicalUrbanRural_ID", SDRMigrationSupport.resolveLookup(
                    urbanRuralCrosswalk, rs.getInt("dhetphysicalurbanruralid")));
            setIfPresent(po, "SDR_DHETPhysicalProvince_ID", SDRMigrationSupport.resolveLookup(
                    provinceCrosswalk, rs.getInt("dhetphysicalprovinceid")));
            setIfPresent(po, "SDR_DHETGPSCoordinates", rs.getString("dhetgpscoordinates"));

            // -- DHET postal block --
            setIfPresent(po, "SDR_DHETPostalAddressLine1", rs.getString("dhetpostaladdressline1"));
            setIfPresent(po, "SDR_DHETPostalAddressLine2", rs.getString("dhetpostaladdressline2"));
            setIfPresent(po, "SDR_DHETPostalAddressLine3", rs.getString("dhetpostaladdressline3"));
            setIfPresent(po, "SDR_DHETPostalCode", rs.getString("dhetpostalcode"));
            setIfPresent(po, "SDR_DHETPostalSuburb_ID", SDRMigrationSupport.resolveLookup(suburbCrosswalk,
                    rs.getInt("dhetpostalsuburbid")));
            setIfPresent(po, "SDR_DHETPostalCity_ID", SDRMigrationSupport.resolveLookup(cityCrosswalk,
                    rs.getInt("dhetpostalcityid")));
            setIfPresent(po, "SDR_DHETPostalMunicipality_ID", SDRMigrationSupport.resolveLookup(
                    municipalityCrosswalk, rs.getInt("dhetpostallmunicipalityid")));
            setIfPresent(po, "SDR_DHETPostalUrbanRural_ID", SDRMigrationSupport.resolveLookup(
                    urbanRuralCrosswalk, rs.getInt("dhetpostalurbanruralid")));
            setIfPresent(po, "SDR_DHETPostalProvince_ID", SDRMigrationSupport.resolveLookup(
                    provinceCrosswalk, rs.getInt("dhetpostalprovinceid")));

            po.saveEx();
            int newId = po.get_ID();

            if (created != null || updated != null) {
                SDRMigrationSupport.stampCreatedUpdated("sdr_organisationaddress", "sdr_organisationaddress_id",
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
            errors.add("mssdr_organisationaddress.id=" + sourceId + ": " + e.getMessage());
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
