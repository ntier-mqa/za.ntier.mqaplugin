package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MTable;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Added 2026-09-16 (see [[AddSDRReferenceTables]]): creates the brand new SDR_SICCodeChamber
 * junction table (45 source rows, mssdr_lkpsiccodechamber). Confirmed via AddSDRReferenceTables'
 * own shape-check log that this source table is NOT a Value/Name lookup - it is a plain
 * SICCode&lt;-&gt;ChamberCode mapping (id, siccodeid, chambercodeid, audit columns) - so it is
 * built here as a proper two-FK table instead, mirroring AddSDRGrantTypeAccountTable's shape.
 * Requires SDR_SICCode and SDR_ChamberCode (both already created by AddSDRReferenceTables) to
 * exist first - column names below rely on the standard TableDir naming convention to resolve.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRSICCodeChamberTable")
public class AddSDRSICCodeChamberTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_SICCodeChamber";
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
                "SIC code / chamber code mapping (mssdr_lkpsiccodechamber)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SICCode_ID", DisplayType.TableDir, 10,
                "mssdr_lkpsiccodechamber.siccodeid -> SDR_SICCode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ChamberCode_ID", DisplayType.TableDir, 10,
                "mssdr_lkpsiccodechamber.chambercodeid -> SDR_ChamberCode", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 2 business columns.";
    }
}
