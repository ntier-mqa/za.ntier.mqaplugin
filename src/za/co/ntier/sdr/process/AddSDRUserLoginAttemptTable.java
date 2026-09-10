package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): creates the brand new
 * SDR_UserLoginAttempt child table (64,395 source rows).
 *
 * <p>SDR_UnlockedByUser_ID is a second, admin-side FK to SDR_User (NOT iDempiere's AD_User - this
 * points at the same SIMS-user business table as SDR_User_ID above) - its column name doesn't match
 * the target table ("SDR_UnlockedByUser" != "SDR_User"), so needs an explicit override. SDR_User has no
 * generic "Name" column (same shape as SDR_Person/SDR_Organisation/SDR_SDF/SDR_WSPATRForms) -
 * SDR_UserName (100% populated) is used as the display column.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRUserLoginAttemptTable")
public class AddSDRUserLoginAttemptTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_UserLoginAttempt";
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
                "A login attempt for a SIMS user, including lockout/unlock detail (mssdr_userloginattempt)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_User_ID", DisplayType.TableDir, 10,
                "mssdr_userloginattempt.userid -> SDR_User. CONFIRMED 100% match (64,395/64,395)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IPAddress", DisplayType.String, 50,
                "mssdr_userloginattempt.ipaddress", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateLoggedIn", DisplayType.DateTime, 7,
                "mssdr_userloginattempt.dateloggedin", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UnlockedViaAdmin", DisplayType.Integer, 10,
                "mssdr_userloginattempt.unlockedviaadmin (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UnlockedDate", DisplayType.DateTime, 7,
                "mssdr_userloginattempt.unlockeddate", ENTITY_TYPE, get_TrxName());

        int userRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_User", "SDR_UserName",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_UnlockedByUser_ID", DisplayType.Table,
                userRefId, 10,
                "mssdr_userloginattempt.unlockedbyuserid -> SDR_User (a different admin unlocking this "
                + "account). CONFIRMED 0.2% (130/64,395) - low rate makes sense, admin-unlock is a rare "
                + "event relative to total login-attempt volume", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
