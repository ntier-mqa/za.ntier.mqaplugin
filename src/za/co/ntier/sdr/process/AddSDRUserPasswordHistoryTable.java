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
 * SDR_UserPasswordHistory child table (29,778 source rows).
 *
 * <p>SECURITY - DELIBERATELY DOES NOT CARRY mssdr_userpasswordhistory.Password: CONFIRMED 2026-09-04
 * (user decision), even though this column looks properly hashed (a consistent 75-character length,
 * consistent with a real salted-hash/KDF format) - no password-shaped value is ever carried into any
 * SDR_ table or displayed in any window, regardless of hash-vs-plaintext status. This is a "don't carry
 * it at all" rule, not a "mask it in the UI" one. See the mapping doc's SECURITY FLAG section for the
 * full reasoning (this rule is even more important on {@link AddSDRLoginViolationsTable}, whose
 * equivalent column shows strong evidence of storing plaintext credentials).
 *
 * <p>Built anyway per that same decision, carrying only SDR_User_ID plus the standard Created/Updated
 * audit timestamps this engine always adds - "this user changed their password on this date", not the
 * value itself.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRUserPasswordHistoryTable")
public class AddSDRUserPasswordHistoryTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_UserPasswordHistory";
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
                "A password-change event for a SIMS user (mssdr_userpasswordhistory) - deliberately "
                + "carries NO password value, see class Javadoc SECURITY note", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_User_ID", DisplayType.TableDir, 10,
                "mssdr_userpasswordhistory.userid -> SDR_User. CONFIRMED 100% match (29,778/29,778)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 1 business column (password intentionally excluded).";
    }
}
