package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyTransactionDetail
 * child table (703,156 source rows).
 *
 * <p>SDR_AccountNumber is UNMAPPED - CHECKED: does not resolve 1:1 to a specific SDR_LevyAccount row (a
 * naive join produces 55.2 million rows from 703,156, since the same GL account code is shared across
 * many different LevyAccount rows as their ContraGLAccountNumber). This is a plain GL coding value, not
 * a foreign key to one specific account - carried as text only.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyTransactionDetailTable")
public class AddSDRLevyTransactionDetailTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyTransactionDetail";
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
                "A detail line on a levy transaction (mssdr_levytransactiondetail)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyTransaction_ID", DisplayType.TableDir, 10,
                "mssdr_levytransactiondetail.levytransactionid -> SDR_LevyTransaction", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionType", DisplayType.String, 250,
                "mssdr_levytransactiondetail.transactiontype", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsContraEntry", DisplayType.YesNo, 1,
                "mssdr_levytransactiondetail.iscontraentry (bit -> Y/N)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountNumber", DisplayType.String, 50,
                "mssdr_levytransactiondetail.accountnumber - UNMAPPED, CHECKED: does not resolve 1:1 to a "
                + "specific SDR_LevyAccount row (a naive join fans out to 55.2 million rows) - a plain GL "
                + "coding value, carried as text only", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionValue", DisplayType.Amount, 22,
                "mssdr_levytransactiondetail.transactionvalue", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
