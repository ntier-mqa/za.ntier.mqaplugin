package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyTransactionStatus
 * child table (87,008 source rows).
 *
 * <p>The source Creditor column is DROPPED entirely - CONFIRMED 2026-09-04 (user decision): blank/empty
 * string on all 87,008 rows, no data at all, not carried into this table.
 *
 * <p>SDR_PostingStatus and SDR_ErrorCode are UNMAPPED - CHECKED: constant 0 across all rows, no
 * variance in this data, carried as plain integers for completeness/future-proofing rather than as
 * lookups.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyTransactionStatusTable")
public class AddSDRLevyTransactionStatusTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyTransactionStatus";
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
                "A processing status record for a levy transaction (mssdr_levytransactionstatus) - the "
                + "source Creditor column is dropped entirely (always blank)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyTransaction_ID", DisplayType.TableDir, 10,
                "mssdr_levytransactionstatus.levytransactionid -> SDR_LevyTransaction", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DocumentNumber", DisplayType.String, 250,
                "mssdr_levytransactionstatus.documentnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostingStatus", DisplayType.Integer, 10,
                "mssdr_levytransactionstatus.postingstatus - UNMAPPED, CHECKED: constant 0 across all "
                + "87,008 rows, carried as a plain integer for completeness/future-proofing", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostingDescription", DisplayType.String, 250,
                "mssdr_levytransactionstatus.postingdescription (blank on all sampled rows, consistent "
                + "with PostingStatus always being the 'OK' case)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorCode", DisplayType.Integer, 10,
                "mssdr_levytransactionstatus.errorcode - UNMAPPED, same as PostingStatus, constant 0 "
                + "across all sampled rows", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorDescription", DisplayType.String, 250,
                "mssdr_levytransactionstatus.errordescription (constant 'OKAY' on all sampled rows)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FromDateTime", DisplayType.DateTime, 7,
                "mssdr_levytransactionstatus.fromdatetime", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ToDateTime", DisplayType.DateTime, 7,
                "mssdr_levytransactionstatus.todatetime", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
