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
 * SDR_LoginViolations table (92,278 source rows) - STANDALONE, no FK to SDR_User. This table logs
 * attempts against usernames that may not even correspond to a real account, so a hard FK wouldn't make
 * sense - CONFIRMED 2026-09-04 (user decision): SDR_UserName stays plain text, a case-insensitive
 * trimmed match against SDR_User.SDR_UserName was tested and only resolves 71.5% (with minor fan-out
 * since UserName isn't guaranteed unique) - not built as an approximate FK.
 *
 * <p>SECURITY - CRITICAL, DELIBERATELY DOES NOT CARRY mssdr_loginviolations.Password UNDER ANY
 * CIRCUMSTANCES: CHECKED 2026-09-04, this column shows strong evidence of storing PLAINTEXT passwords
 * (variable length 9-20 characters, dictionary-word-shaped prefixes) - NOT a hash-shaped fixed-length
 * value like {@link AddSDRUserPasswordHistoryTable}'s equivalent column. Never carried into any SDR_
 * table, never displayed in any window. The already-staged mssdr_loginviolations.password column is
 * left as-is in Postgres for now (purging it was deliberately scoped out of this migration - see the
 * mapping doc's SECURITY FLAG section; this is NOT an assessment that the exposure is low-risk).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLoginViolationsTable")
public class AddSDRLoginViolationsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LoginViolations";
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
                "A logged login violation, matched by username text only, not a User FK "
                + "(mssdr_loginviolations) - deliberately carries NO password value under any "
                + "circumstances, see class Javadoc SECURITY note", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UserName", DisplayType.String, 250,
                "mssdr_loginviolations.username - plain text only. CONFIRMED 2026-09-04 (user decision): "
                + "no LOOKUP attempted against SDR_User (a text match only resolves 71.5% with minor "
                + "fan-out)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IPAddress", DisplayType.String, 50,
                "mssdr_loginviolations.ipaddress", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateLoggedIn", DisplayType.DateTime, 7,
                "mssdr_loginviolations.dateloggedin", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsSuccessfulLogin", DisplayType.Integer, 10,
                "mssdr_loginviolations.issuccessfullogin (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsADUser", DisplayType.Integer, 10,
                "mssdr_loginviolations.isaduser (tinyint flag)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns (password intentionally excluded).";
    }
}
