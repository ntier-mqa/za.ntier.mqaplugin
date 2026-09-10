package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyGPProcessData
 * table (2,225 source rows). CONFIRMED 2026-09-04 (user decision): stage only, no read-only window
 * built for this table.
 *
 * <p>UNIQUE in this migration: the source table (mssdr_levygpprocessdata) has no "id" column at all and
 * nothing else references it, so the generated PK is a bare sequence with no recon/hash key - idempotent
 * re-run support isn't needed here. The standard "id" recon column this engine always adds will simply
 * stay unpopulated (0) for every row; harmless, just unused for this one table. The source also has no
 * isdeleted column - CONFIRMED always active, so IsActive is left at its standard 'Y' default rather
 * than derived from anything.
 *
 * <p>SDR_TransactionID and SDR_ProcessID are both UNMAPPED - checked against LevyProcess.ID (0% match,
 * same "opaque identifiers" correction as LevyTransaction's equivalent columns) and not tested against
 * anything else - genuinely unresolved.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyGPProcessDataTable")
public class AddSDRLevyGPProcessDataTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyGPProcessData";
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
                "A GP (General Purpose) levy process data line (mssdr_levygpprocessdata) - stage only, "
                + "no read-only window built; source has no 'id' column, bare generated PK", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionID", DisplayType.Integer, 10,
                "mssdr_levygpprocessdata.transactionid - UNMAPPED, checked against LevyProcess.ID (0% "
                + "match), genuinely unresolved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_levygpprocessdata.processid - UNMAPPED, same as SDR_TransactionID", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionDate", DisplayType.DateTime, 7,
                "mssdr_levygpprocessdata.transactiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainAccountNumber", DisplayType.String, 50,
                "mssdr_levygpprocessdata.mainaccountnumber (plain GL code, same shape as LevyAccount's GL "
                + "number columns, not independently cross-checked against them)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Total", DisplayType.Amount, 22,
                "mssdr_levygpprocessdata.total", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainDebitAmount", DisplayType.Amount, 22,
                "mssdr_levygpprocessdata.maindebitamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MainCreditAmount", DisplayType.Amount, 22,
                "mssdr_levygpprocessdata.maincreditamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraAccountNumber", DisplayType.String, 50,
                "mssdr_levygpprocessdata.contraaccountnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraDebitAmount", DisplayType.Amount, 22,
                "mssdr_levygpprocessdata.contradebitamount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContraCreditAmount", DisplayType.Amount, 22,
                "mssdr_levygpprocessdata.contracreditamount", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 10 business columns.";
    }
}
