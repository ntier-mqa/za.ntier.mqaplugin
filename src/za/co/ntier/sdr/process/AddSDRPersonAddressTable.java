package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): creates the brand new
 * SDR_PersonAddress child table (922,350 source rows, 23 people have more than one row - user
 * decision 2026-09-03: keep all rows, no tie-break/dedup).
 *
 * <p>None of the 10 physical/postal address lookup columns (PhysicalSuburb_ID, PhysicalCity_ID,
 * ... PostalProvince_ID) match their target tables by name (targets are the generic SDR_Suburb/
 * SDR_City/SDR_Municipality/SDR_UrbanRural/SDR_Province, shared with other families) - every one
 * needs an explicit AD_Reference/AD_Ref_Table override via
 * {@link AddColumnsSupport#findOrCreateTableReference} (user decision 2026-09-05). Only 5 DISTINCT
 * target tables are involved (Suburb/City/Municipality/UrbanRural/Province), so each reference is
 * resolved once and reused for both its Physical* and Postal* column.
 *
 * <p>Schema only - no data population. Unlike the Learner project's zzperson (which folds
 * physical/postal into a single c_location FK per MLocation convention), this table keeps
 * physical/postal as plain columns + plain lookup FKs - see mapping doc's DESIGN NOTE for why no
 * c_location abstraction is needed here.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRPersonAddressTable")
public class AddSDRPersonAddressTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_PersonAddress";
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
                "A person's physical/postal address (mssdr_personaddress) - not guaranteed 1:1 "
                + "with SDR_Person, 23 people have more than one row",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Person_ID", DisplayType.TableDir, 10,
                "mssdr_personaddress.personid -> SDR_Person", ENTITY_TYPE, get_TrxName());

        // Resolve the 5 shared geography reference overrides once, reused for both
        // Physical* and Postal* columns below.
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

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress1", DisplayType.String, 250,
                "mssdr_personaddress.physicaladdress1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress2", DisplayType.String, 250,
                "mssdr_personaddress.physicaladdress2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalAddress3", DisplayType.String, 250,
                "mssdr_personaddress.physicaladdress3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhysicalCode", DisplayType.String, 10,
                "mssdr_personaddress.physicalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_personaddress.physicalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_personaddress.physicalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalMunicipality_ID", DisplayType.Table,
                municipalityRefId, 10, "mssdr_personaddress.physicalmunicipalityid -> SDR_Municipality (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalUrbanRural_ID", DisplayType.Table,
                urbanRuralRefId, 10, "mssdr_personaddress.physicalurbanruralid -> SDR_UrbanRural (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PhysicalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_personaddress.physicalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UsePhysicalAsPostal", DisplayType.YesNo, 1,
                "mssdr_personaddress.usephysicalaspostal", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine1", DisplayType.String, 250,
                "mssdr_personaddress.postaladdressline1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine2", DisplayType.String, 250,
                "mssdr_personaddress.postaladdressline2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalAddressLine3", DisplayType.String, 250,
                "mssdr_personaddress.postaladdressline3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostalCode", DisplayType.String, 10,
                "mssdr_personaddress.postalcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalSuburb_ID", DisplayType.Table,
                suburbRefId, 10, "mssdr_personaddress.postalsuburbid -> SDR_Suburb (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalCity_ID", DisplayType.Table,
                cityRefId, 10, "mssdr_personaddress.postalcityid -> SDR_City (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalMunicipality_ID", DisplayType.Table,
                municipalityRefId, 10, "mssdr_personaddress.postalmunicipalityid -> SDR_Municipality (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalUrbanRural_ID", DisplayType.Table,
                urbanRuralRefId, 10, "mssdr_personaddress.postalurbanruralid -> SDR_UrbanRural (0=not set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PostalProvince_ID", DisplayType.Table,
                provinceRefId, 10, "mssdr_personaddress.postalprovinceid -> SDR_Province (0=not set)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 19 business columns.";
    }
}
