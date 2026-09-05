package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): creates the brand new
 * SDR_PersonHealthFunctioningStatusRating child table (5,504,896 source rows - the largest table
 * in the Person family). CONFIRMED 2026-09-04 (user decision): kept as a genuine child/detail
 * table (one row per person per status type, matching the source structure), NOT pivoted into 6
 * wide columns on SDR_Person the way the Learner project's zzperson did - no 6-column pivot/
 * aggregate step needed for a read-only reporting table.
 *
 * <p>Both FK columns (SDR_HealthFunctioningStatus_ID, SDR_HealthFunctioningRating_ID) match
 * their target tables by name - plain DisplayType.TableDir, no reference override needed.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRPersonHealthFunctioningStatusRatingTable")
public class AddSDRPersonHealthFunctioningStatusRatingTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_PersonHealthFunctioningStatusRating";
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
                "A person's health functioning rating, one row per status type "
                + "(mssdr_personhealthfunctioningstatusrating) - kept as a child table, not "
                + "pivoted, per user decision 2026-09-04",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Person_ID", DisplayType.TableDir, 10,
                "mssdr_personhealthfunctioningstatusrating.personid -> SDR_Person", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HealthFunctioningStatus_ID", DisplayType.TableDir, 10,
                "mssdr_personhealthfunctioningstatusrating.healthfunctioningstatusid -> "
                + "SDR_HealthFunctioningStatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HealthFunctioningRating_ID", DisplayType.TableDir, 10,
                "mssdr_personhealthfunctioningstatusrating.healthfunctioningratingid -> "
                + "SDR_HealthFunctioningRating", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 3 business columns.";
    }
}
