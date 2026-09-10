package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): creates the brand new SDR_UserLogin
 * child table - the LARGEST table in this family (669,335 source rows). Very simple: just a login-event
 * timestamp per user, no other business columns at all. The source has no audit columns whatsoever (no
 * DateCreated/CreatedBy/UpdatedBy/IsDeleted) - a lighter-weight table than everything else in this
 * migration; the standard system columns this engine always adds are still present, just won't be
 * derived from anything during the eventual data-population step.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRUserLoginTable")
public class AddSDRUserLoginTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_UserLogin";
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
                "A login event for a SIMS user (mssdr_userlogin) - the largest table in this family, no "
                + "audit columns at all in the source", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_User_ID", DisplayType.TableDir, 10,
                "mssdr_userlogin.userid -> SDR_User. CONFIRMED 100% match (669,335/669,335)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateLoggedIn", DisplayType.DateTime, 7,
                "mssdr_userlogin.dateloggedin", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 2 business columns.";
    }
}
