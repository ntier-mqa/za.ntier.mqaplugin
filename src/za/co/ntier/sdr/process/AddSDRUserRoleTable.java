package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): creates the brand new SDR_UserRole
 * child table (8,200 source rows). Both FK columns match their target tables by name - plain
 * DisplayType.TableDir.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRUserRoleTable")
public class AddSDRUserRoleTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_UserRole";
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
                "A role assignment for a SIMS user (mssdr_userrole)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_User_ID", DisplayType.TableDir, 10,
                "mssdr_userrole.userid -> SDR_User. CONFIRMED 99.96% match (8,197/8,200)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Role_ID", DisplayType.TableDir, 10,
                "mssdr_userrole.roleid -> SDR_Role (shared). CONFIRMED 99.99% match (8,199/8,200)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 2 business columns.";
    }
}
