package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRFinance child
 * table (75,520 source rows).
 *
 * <p>SDR_FinanceValue is stored as text in the source despite being a currency-shaped value - CONFIRMED
 * not cast to numeric, carried as-is (String, not Amount).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRFinanceTable")
public class AddSDRWSPATRFinanceTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRFinance";
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
                "A finance line item on a WSPATR (mssdr_wspatrfinance)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrfinance.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrfinance.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Section", DisplayType.String, 250,
                "mssdr_wspatrfinance.section (free text, small fixed set of section labels - not checked "
                + "against a lookup, low priority as a display-only field)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinanceType", DisplayType.String, 250,
                "mssdr_wspatrfinance.financetype (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinanceValue", DisplayType.String, 250,
                "mssdr_wspatrfinance.financevalue - stored as text in the source despite being a "
                + "currency-shaped value, not cast to numeric, carried as-is", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
