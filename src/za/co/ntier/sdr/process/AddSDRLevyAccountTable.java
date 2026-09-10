package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyAccount catalog
 * table (156 source rows). Must be built before {@link AddSDRLevyImportTable}, which FKs to it.
 *
 * <p>SDR_ChamberCode is UNMAPPED - CHECKED: 0% match against SDR_ChamberCode despite being an
 * int-typed, superficially FK-shaped column - carried as a plain integer, no crosswalk found (unlike
 * Organisation's OWN ChamberCodeID, which does resolve at 100% - same column name, different table,
 * different answer).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyAccountTable")
public class AddSDRLevyAccountTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyAccount";
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
                "A levy GL account catalog entry (mssdr_levyaccount)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_levyaccount.financialyearid -> SDR_FinancialYear (shared). CONFIRMED 100% match "
                + "(156/156)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyField_ID", DisplayType.TableDir, 10,
                "mssdr_levyaccount.levyfieldid -> SDR_LevyField. CONFIRMED 100% match (156/156)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ChamberCode", DisplayType.Integer, 10,
                "mssdr_levyaccount.chambercode - UNMAPPED, CHECKED: 0% match against SDR_ChamberCode "
                + "despite being int-shaped - carried as a plain value, no crosswalk found", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainGLAccountNumber", DisplayType.String, 50,
                "mssdr_levyaccount.mainglaccountnumber (plain GL code text, e.g. '00-01-3015')", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraGLAccountNumber", DisplayType.String, 50,
                "mssdr_levyaccount.contraglaccountnumber (plain GL code text - the same value repeats "
                + "across many rows, a shared contra account, not a per-row unique identifier)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
