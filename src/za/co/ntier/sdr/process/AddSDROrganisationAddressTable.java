package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new
 * SDR_OrganisationAddress child table (13,111 source rows).
 *
 * <p>UNLIKE the main SDR_Organisation table, the DHET collapse does NOT apply here - the mapping doc
 * measured only 53-89% agreement between the current and DHET address fields (vs 99.9%+ on
 * Organisation's core fields), so collapsing would silently drop real, different address data for
 * roughly a third to half of all organisations. CONFIRMED 2026-09-04 (user decision): both the current
 * and DHET physical/postal blocks are kept as separate column sets.
 *
 * <p>None of the 10 geography lookup columns per block (Suburb/City/Municipality/UrbanRural/Province,
 * x2 for Physical/Postal, x2 again for the DHET mirror = 20 total) match their target tables by name -
 * every one needs an explicit AD_Reference/AD_Ref_Table override via
 * {@link AddColumnsSupport#findOrCreateTableReference}. Only 5 DISTINCT target tables are involved
 * (shared with Person's address lookups: SDR_Suburb/SDR_City/SDR_Municipality/SDR_UrbanRural/
 * SDR_Province), so each reference is resolved once and reused across all 4 blocks.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationAddressTable")
public class AddSDROrganisationAddressTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationAddress";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "3";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        MTable existing = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (existing != null) {
            addLog(TABLE_NAME + " already exists - not recreated.");
            return TABLE_NAME + " already exists - no action taken.";
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "An organisation's physical/postal address (mssdr_organisationaddress) - current AND "
                + "DHET blocks both kept (53-89% agreement measured, too low/variable to collapse safely)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationaddress.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());

        // Resolve the 5 shared geography reference overrides once, reused across current-physical,
        // current-postal, dhet-physical and dhet-postal blocks below.
        int suburbRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Suburb", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        int cityRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_City", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        int municipalityRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Municipality", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        int urbanRuralRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_UrbanRural", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        int provinceRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Province", ENTITY_TYPE,
                get_TrxName(), this::addLog);

        // -- current physical block --
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress1", DisplayType.String, 250,
                "mssdr_organisationaddress.physicaladdress1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress2", DisplayType.String, 250,
                "mssdr_organisationaddress.physicaladdress2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress3", DisplayType.String, 250,
                "mssdr_organisationaddress.physicaladdress3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalCode", DisplayType.String, 10,
                "mssdr_organisationaddress.physicalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_organisationaddress.physicalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_organisationaddress.physicalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalMunicipality_ID", DisplayType.Table,
                municipalityRefId, 10,
                "mssdr_organisationaddress.physicalmunicipalityid -> SDR_Municipality (0=not set)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalUrbanRural_ID", DisplayType.Table,
                urbanRuralRefId, 10, "mssdr_organisationaddress.physicalurbanruralid -> SDR_UrbanRural (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_organisationaddress.physicalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GPSCoordinates", DisplayType.String, 250,
                "mssdr_organisationaddress.gpscoordinates - kept as raw free text, not parsed into "
                + "separate latitude/longitude columns", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UsePhysicalAsPostal", DisplayType.YesNo, 1,
                "mssdr_organisationaddress.usephysicalaspostal", ENTITY_TYPE, get_TrxName());

        // -- current postal block --
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine1", DisplayType.String, 250,
                "mssdr_organisationaddress.postaladdressline1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine2", DisplayType.String, 250,
                "mssdr_organisationaddress.postaladdressline2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine3", DisplayType.String, 250,
                "mssdr_organisationaddress.postaladdressline3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalCode", DisplayType.String, 10,
                "mssdr_organisationaddress.postalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_organisationaddress.postalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_organisationaddress.postalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalMunicipality_ID", DisplayType.Table,
                municipalityRefId, 10,
                "mssdr_organisationaddress.postallmunicipalityid (source column name has a typo, sic) -> "
                + "SDR_Municipality (0=not set)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalUrbanRural_ID", DisplayType.Table,
                urbanRuralRefId, 10, "mssdr_organisationaddress.postalurbanruralid -> SDR_UrbanRural (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_organisationaddress.postalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());

        // -- DHET physical block (kept separate, not collapsed - see class Javadoc) --
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPhysicalAddress1", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetphysicaladdress1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPhysicalAddress2", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetphysicaladdress2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPhysicalAddress3", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetphysicaladdress3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPhysicalCode", DisplayType.String, 10,
                "mssdr_organisationaddress.dhetphysicalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPhysicalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_organisationaddress.dhetphysicalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPhysicalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_organisationaddress.dhetphysicalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPhysicalMunicipality_ID",
                DisplayType.Table, municipalityRefId, 10,
                "mssdr_organisationaddress.dhetphysicalmunicipalityid -> SDR_Municipality (0=not set)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPhysicalUrbanRural_ID",
                DisplayType.Table, urbanRuralRefId, 10,
                "mssdr_organisationaddress.dhetphysicalurbanruralid -> SDR_UrbanRural (0=not set)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPhysicalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_organisationaddress.dhetphysicalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETGPSCoordinates", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetgpscoordinates", ENTITY_TYPE, get_TrxName());

        // -- DHET postal block --
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPostalAddressLine1", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetpostaladdressline1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPostalAddressLine2", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetpostaladdressline2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPostalAddressLine3", DisplayType.String, 250,
                "mssdr_organisationaddress.dhetpostaladdressline3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DHETPostalCode", DisplayType.String, 10,
                "mssdr_organisationaddress.dhetpostalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPostalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_organisationaddress.dhetpostalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPostalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_organisationaddress.dhetpostalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPostalMunicipality_ID",
                DisplayType.Table, municipalityRefId, 10,
                "mssdr_organisationaddress.dhetpostallmunicipalityid (source column name has a typo, sic) -> "
                + "SDR_Municipality (0=not set)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPostalUrbanRural_ID", DisplayType.Table,
                urbanRuralRefId, 10, "mssdr_organisationaddress.dhetpostalurbanruralid -> SDR_UrbanRural (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DHETPostalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_organisationaddress.dhetpostalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 40 business columns.";
    }
}
