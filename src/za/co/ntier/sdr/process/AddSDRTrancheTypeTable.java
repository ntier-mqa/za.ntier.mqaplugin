package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 1b/Misc (see "Phase 1b - Misc Family - Mapping.txt"): creates the brand new SDR_TrancheType
 * catalog table (a single source row: SiteID=1, Code="GRT", Value="Grant"). This is the LAST table of
 * all 168 in the original source inventory - closes out schema-building for the entire migration.
 *
 * <p>SDR_Site_ID is UNMAPPED - CHECKED: no matching "Site" table or lookup found anywhere in the 168
 * source tables; only 1 row exists, giving no data-driven way to investigate further - carried as a
 * plain integer.
 *
 * <p>SDR_AccountNumber holds the NIL GUID (00000000-0000-0000-0000-000000000000) - a different case
 * from the NEWID()-generated real GUIDs seen elsewhere (Organisation/GrantAccount): this one reads as
 * an explicit "not set" placeholder, not a generated identifier. Carried as-is anyway for completeness,
 * same "show it anyway" precedent set for those other GUID columns.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRTrancheTypeTable")
public class AddSDRTrancheTypeTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_TrancheType";
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
                "A tranche type catalog entry (mssdr_tranchetype) - the last table of all 168 in the "
                + "original source inventory", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Site_ID", DisplayType.Integer, 10,
                "mssdr_tranchetype.siteid - UNMAPPED, CHECKED: no matching 'Site' table found anywhere in "
                + "the 168 source tables - carried as a plain integer", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountNumber", DisplayType.String, 250,
                "mssdr_tranchetype.accountnumber - the NIL GUID "
                + "(00000000-0000-0000-0000-000000000000), an explicit 'not set' placeholder rather than "
                + "a generated identifier - carried as-is for completeness", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Code", DisplayType.String, 250,
                "mssdr_tranchetype.code", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Value", DisplayType.String, 250,
                "mssdr_tranchetype.value", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
