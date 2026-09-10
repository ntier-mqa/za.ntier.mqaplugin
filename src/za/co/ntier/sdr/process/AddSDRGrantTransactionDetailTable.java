package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new
 * SDR_GrantTransactionDetail child table (894,796 source rows).
 *
 * <p>SDR_AccountNumber is UNMAPPED - CHECKED: 0 matches against GrantAccount.AccountNumber (a GUID) -
 * this column is a plain GL code string in a completely different format, same conclusion as Levy's
 * equivalent column. Carried as text only.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTransactionDetailTable")
public class AddSDRGrantTransactionDetailTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTransactionDetail";
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
                "A detail line on a grant transaction (mssdr_granttransactiondetail)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantTransaction_ID", DisplayType.TableDir, 10,
                "mssdr_granttransactiondetail.granttransactionid -> SDR_GrantTransaction. CONFIRMED 100% "
                + "match (894,796/894,796)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionType", DisplayType.String, 250,
                "mssdr_granttransactiondetail.transactiontype", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsContraEntry", DisplayType.YesNo, 1,
                "mssdr_granttransactiondetail.iscontraentry (bit -> Y/N)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountNumber", DisplayType.String, 50,
                "mssdr_granttransactiondetail.accountnumber - UNMAPPED, CHECKED: 0 matches against "
                + "GrantAccount.AccountNumber (a GUID) - a plain GL code string, carried as text only",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionValue", DisplayType.Amount, 22,
                "mssdr_granttransactiondetail.transactionvalue", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
