package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantTypeAccount
 * child table (837 source rows). Requires {@link AddSDRGrantTypeTable} to have already run.
 *
 * <p>SDR_ChamberCode is UNMAPPED - CHECKED: 0% match against SDR_ChamberCode despite being int-typed,
 * same conclusion as LevyAccount.ChamberCode in the Levy family (Organisation's OWN ChamberCodeID
 * remains the only ChamberCode-shaped column that actually resolves).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTypeAccountTable")
public class AddSDRGrantTypeAccountTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTypeAccount";
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
                "A GL account entry for a grant type (mssdr_granttypeaccount)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantType_ID", DisplayType.TableDir, 10,
                "mssdr_granttypeaccount.granttypeid -> SDR_GrantType (self-family). CONFIRMED 100% match "
                + "(837/837)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ChamberCode", DisplayType.Integer, 10,
                "mssdr_granttypeaccount.chambercode - UNMAPPED, CHECKED: 0% match against SDR_ChamberCode "
                + "despite being int-typed - carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainGLAccountNumber", DisplayType.String, 50,
                "mssdr_granttypeaccount.mainglaccountnumber (plain GL code text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraGLAccountNumber", DisplayType.String, 50,
                "mssdr_granttypeaccount.contraglaccountnumber (plain GL code text)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
