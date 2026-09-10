package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantGPProcessData
 * table (50,139 source rows) - low business value. UNLIKE Levy's equivalent table
 * (SDR_LevyGPProcessData, which has no source "id" column at all), this table DOES have a real "id"
 * column in the source - the standard recon "id" column here is meaningful, no special-case decision
 * needed. Requires {@link AddSDRGrantProcessTable} to have already run.
 *
 * <p>SDR_ReferenceNumber carries the same composite "&lt;Year&gt;|&lt;GrantCode&gt;|&lt;OrgNumber&gt;"
 * format as GrantTransaction.ReferenceNumber (a redundant copy of the same fact, not independently
 * re-verified for exact match rate this pass) - resolved the same way, via SDR_Organisation_ID.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantGPProcessDataTable")
public class AddSDRGrantGPProcessDataTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantGPProcessData";
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
                "A GP (General Purpose) grant process data line (mssdr_grantgpprocessdata) - low "
                + "business value", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionID", DisplayType.Integer, 10,
                "mssdr_grantgpprocessdata.transactionid - UNMAPPED, opaque, same as elsewhere", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_grantgpprocessdata.processid - UNMAPPED, opaque, same as elsewhere", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportID", DisplayType.Integer, 10,
                "mssdr_grantgpprocessdata.importid - UNMAPPED, opaque, same as elsewhere", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantProcess_ID", DisplayType.TableDir, 10,
                "mssdr_grantgpprocessdata.grantprocessid -> SDR_GrantProcess (self-family). CONFIRMED 100% "
                + "match (50,139/50,139) - a genuinely real, clean crosswalk", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReferenceNumber", DisplayType.String, 250,
                "mssdr_grantgpprocessdata.referencenumber - kept verbatim, same composite format as "
                + "GrantTransaction.ReferenceNumber, source of the Organisation crosswalk below",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_grantgpprocessdata.referencenumber (DERIVED: same two-tier resolution as "
                + "GrantTransaction) -> SDR_Organisation - a redundant copy of the same fact, not "
                + "independently re-verified for exact match rate this pass", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionDate", DisplayType.DateTime, 7,
                "mssdr_grantgpprocessdata.transactiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainAccountNumber", DisplayType.String, 50,
                "mssdr_grantgpprocessdata.mainaccountnumber (plain GL code, same shape as "
                + "GrantTransactionDetail.AccountNumber, not a real FK)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Total", DisplayType.Amount, 22,
                "mssdr_grantgpprocessdata.total", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainDebitAmount", DisplayType.Amount, 22,
                "mssdr_grantgpprocessdata.maindebitamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainCreditAmount", DisplayType.Amount, 22,
                "mssdr_grantgpprocessdata.maincreditamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraAccountNumber", DisplayType.String, 50,
                "mssdr_grantgpprocessdata.contraaccountnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraDebitAmount", DisplayType.Amount, 22,
                "mssdr_grantgpprocessdata.contradebitamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraCreditAmount", DisplayType.Amount, 22,
                "mssdr_grantgpprocessdata.contracreditamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsProcessed", DisplayType.Integer, 10,
                "mssdr_grantgpprocessdata.isprocessed (int flag) - CHECKED: 50,134/50,139 (99.99%) are 1, "
                + "only 5 rows are 0 - near-constant but not entirely so, kept as a real column", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 15 business columns.";
    }
}
