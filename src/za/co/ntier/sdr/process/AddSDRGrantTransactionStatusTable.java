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
 * SDR_GrantTransactionStatus child table (451,557 source rows).
 *
 * <p>IMPORTANT DIFFERENCE FROM LEVY: unlike LevyTransactionStatus.Creditor (confirmed always blank and
 * dropped entirely), THIS Creditor column IS populated (only 107/451,557 blank) and holds the same L/D
 * organisation-number values as GrantTransaction.ReferenceNumber - so it's kept AND resolved to
 * SDR_Organisation via the same two-tier strategy (exact match first, numeric-suffix fallback), expected
 * to resolve ~99% given it carries the same underlying values. Don't assume a same-named column behaves
 * the same way across tables - this is the opposite of Levy's identical-shaped column.
 *
 * <p>SDR_ErrorCode is UNMAPPED but, UNLIKE Levy's equivalent (constant 0), has real variance here (0
 * dominant at 98.9%, but other codes appear with meaningful counts) - no lookup table found, carried as
 * a plain integer, worth revisiting if a Grant-specific error-code catalog turns up later.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTransactionStatusTable")
public class AddSDRGrantTransactionStatusTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTransactionStatus";
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
                "A processing status record for a grant transaction (mssdr_granttransactionstatus)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantTransaction_ID", DisplayType.TableDir, 10,
                "mssdr_granttransactionstatus.granttransactionid -> SDR_GrantTransaction. CONFIRMED 100% "
                + "match (451,557/451,557)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Creditor", DisplayType.String, 250,
                "mssdr_granttransactionstatus.creditor - kept verbatim (populated, only 107/451,557 blank), "
                + "also the source of the Organisation crosswalk below", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_granttransactionstatus.creditor (DERIVED: same two-tier resolution as "
                + "GrantTransaction.ReferenceNumber - holds the same L/D-number values) -> "
                + "SDR_Organisation. Expected to resolve ~99%, not independently re-verified at that depth",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DocumentNumber", DisplayType.String, 250,
                "mssdr_granttransactionstatus.documentnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostingStatus", DisplayType.Integer, 10,
                "mssdr_granttransactionstatus.postingstatus - UNMAPPED, CHECKED: constant 0 across all "
                + "451,557 rows, same as Levy's equivalent column", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostingDescription", DisplayType.String, 250,
                "mssdr_granttransactionstatus.postingdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorCode", DisplayType.Integer, 10,
                "mssdr_granttransactionstatus.errorcode - UNMAPPED, CHECKED: UNLIKE Levy's equivalent "
                + "(constant 0), this column has real variance (0 dominant at 98.9%, other codes appear "
                + "with meaningful counts) - no lookup table found, carried as a plain integer", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorDescription", DisplayType.String, 250,
                "mssdr_granttransactionstatus.errordescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FromDateTime", DisplayType.DateTime, 7,
                "mssdr_granttransactionstatus.fromdatetime", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ToDateTime", DisplayType.DateTime, 7,
                "mssdr_granttransactionstatus.todatetime", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 10 business columns.";
    }
}
