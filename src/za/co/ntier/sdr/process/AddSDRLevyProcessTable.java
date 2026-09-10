package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyProcess table (108
 * source rows). CONFIRMED 2026-09-04 (user decision): stage only, no read-only window built for this
 * table - schema is still built for completeness and possible future use.
 *
 * <p>SDR_ProcessID and SDR_LevyGrantProcessQueueStatus_ID are both UNMAPPED - neither matches any table
 * in this migration (the queue-status column was a retroactive addition found missing from the mapping
 * doc during the Grant family pass; same genuinely-unresolved conclusion as the identical column on
 * GrantProcess).
 *
 * <p>SDR_LevySentXML/SDR_LevyReturnXML are large free-text XML blobs (same structure as
 * GrantProcess.GrantSentXML) containing per-transaction detail for a whole batch - not parsed as part
 * of this migration since LevyTransaction's own ReferenceNumber already gives a working, simpler
 * crosswalk; carried as unparsed TextLong (CLOB) for reference only.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyProcessTable")
public class AddSDRLevyProcessTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyProcess";
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
                "A levy batch process record (mssdr_levyprocess) - stage only, no read-only window built",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_levyprocess.processid - UNMAPPED, does not match anything else in this scope, no "
                + "evidence it's a real FK", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyGrantProcessQueueStatus_ID", DisplayType.Integer,
                10, "mssdr_levyprocess.levygrantprocessqueuestatusid - UNMAPPED, CHECKED: no matching "
                + "lookup table exists anywhere in the staged lkp* tables - genuinely unresolved, same "
                + "conclusion as the identical column on GrantProcess", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusCode", DisplayType.Integer, 10,
                "mssdr_levyprocess.statuscode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusDescription", DisplayType.String, 250,
                "mssdr_levyprocess.statusdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevySentXML", DisplayType.TextLong, 0,
                "mssdr_levyprocess.levysentxml - large free-text XML blob containing per-transaction "
                + "batch detail, not parsed, carried unparsed for reference only", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyReturnXML", DisplayType.TextLong, 0,
                "mssdr_levyprocess.levyreturnxml - same as SDR_LevySentXML, unparsed", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
