package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantAccount catalog
 * table (415 source rows).
 *
 * <p>SDR_GrantCode is free text (e.g. "GDP", "ABT"), NOT an id - CHECKED: a case-insensitive trimmed
 * match against SDR_GrantCode's own description only resolves 357/415 (86.0%). CONFIRMED 2026-09-04
 * (user decision): skip the fuzzy-matched FK entirely, keep as plain text only rather than build a
 * lookup known to be incomplete.
 *
 * <p>SDR_AccountNumber is a NEWID()-generated GUID (uniqueidentifier), same pattern as
 * OrganisationBankingDetails.AccountNumber - shown anyway per that platform-wide precedent, despite not
 * being a meaningful value to a viewer.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantAccountTable")
public class AddSDRGrantAccountTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantAccount";
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
                "A grant GL account catalog entry (mssdr_grantaccount)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_grantaccount.financialyearid -> SDR_FinancialYear (shared)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantCode", DisplayType.String, 50,
                "mssdr_grantaccount.grantcode - free text, NOT an id. CHECKED: a fuzzy match against "
                + "SDR_GrantCode only resolves 86.0% (357/415). CONFIRMED 2026-09-04 (user decision): "
                + "skip the fuzzy FK entirely, kept as plain text only", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountNumber", DisplayType.String, 250,
                "mssdr_grantaccount.accountnumber - a NEWID()-generated GUID, same pattern as "
                + "OrganisationBankingDetails.AccountNumber. Shown anyway per that platform-wide precedent",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantDescription", DisplayType.String, 250,
                "mssdr_grantaccount.grantdescription", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
