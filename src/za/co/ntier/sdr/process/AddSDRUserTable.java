package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 8 (see "Phase 8 - User-Security Family - Mapping.txt"): creates the brand new SDR_User MAIN
 * table (6,418 source rows) - the SIMS application's own user accounts, a completely different concept
 * from iDempiere's own AD_User accounts used for the audit-trail (CreatedBy/UpdatedBy/ApprovedBy etc.)
 * columns across every other family in this migration.
 *
 * <p>NAMING NOTE: the mapping doc calls the standard system row-active flag "isactive_record" in prose
 * to disambiguate it from this source table's OWN "IsActive" business column (an account-enabled/
 * disabled flag, a completely different concept - soft-delete vs account status). No actual renaming
 * was needed in code: the framework's standard column is the bare "IsActive" (added automatically by
 * {@link AddColumnsSupport#createNewTableSchema}), while this table's business column is prefixed
 * "SDR_IsActive" - already a distinct column name, no collision.
 *
 * <p>SECURITY: this table intentionally carries NO password/credential columns - see the mapping doc's
 * SECURITY FLAG section. Not applicable to mssdr_user itself (it has no password column), but the same
 * rule governs every other table in this family.
 *
 * <p>Schema only - this class does NOT populate any rows. Data migration is a separate Migrate*-style
 * process, not yet written.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRUserTable")
public class AddSDRUserTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_User";
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
                "A SIMS application user account (mssdr_user) - distinct from iDempiere's own AD_User",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FirstName", DisplayType.String, 250,
                "mssdr_user.firstname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Surname", DisplayType.String, 250,
                "mssdr_user.surname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IDNo", DisplayType.String, 50,
                "mssdr_user.idno", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TelephoneNumber", DisplayType.String, 50,
                "mssdr_user.telephonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CellPhoneNumber", DisplayType.String, 50,
                "mssdr_user.cellphonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FaxNumber", DisplayType.String, 50,
                "mssdr_user.faxnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Email", DisplayType.String, 250,
                "mssdr_user.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UserName", DisplayType.String, 250,
                "mssdr_user.username", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PasswordExpiryDate", DisplayType.DateTime, 7,
                "mssdr_user.passwordexpirydate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsActive", DisplayType.Integer, 10,
                "mssdr_user.isactive (tinyint) - the source's own account-enabled/disabled flag, distinct "
                + "from the standard system IsActive column (soft-delete)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsADUser", DisplayType.Integer, 10,
                "mssdr_user.isaduser (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsDevUser", DisplayType.Integer, 10,
                "mssdr_user.isdevuser (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LoginObjectID", DisplayType.String, 250,
                "mssdr_user.loginobjectid - free text, NOT an FK despite the name (likely an AD/LDAP "
                + "object identifier for AD-authenticated users) - CHECKED: 100% empty/unpopulated "
                + "currently", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 13 business columns.";
    }
}
