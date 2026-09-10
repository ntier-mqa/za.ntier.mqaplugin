package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 1b/Misc (see "Phase 1b - Misc Family - Mapping.txt"): creates the brand new SDR_QueryReasons
 * catalog table (23 source rows) - WSPATR/WSP submission query reasons (e.g. "Missing SDF Signature").
 * Thematically part of the WSPATR evaluation workflow despite being staged as its own "misc" table -
 * CONFIRMED 2026-09-04 (user decision): grouped with WSPATR in the Application Dictionary/menu rather
 * than standalone (a placement decision only, doesn't affect this column mapping).
 *
 * <p>SDR_CRMLetterGrantType_ID is UNMAPPED - CHECKED: constant value 1 across all 23 rows, zero
 * variance. GrantType.ID=1 ("Learnerships Grant") has no obvious thematic connection to WSP/ATR
 * signature-related query reasons, and no dedicated "CRM letter type" table exists anywhere in the 168
 * source tables - carried as a plain integer, not a resolved FK.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRQueryReasonsTable")
public class AddSDRQueryReasonsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_QueryReasons";
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
                "A WSPATR/WSP submission query reason (mssdr_queryreasons)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Name", DisplayType.String, 250,
                "mssdr_queryreasons.name", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Description", DisplayType.String, 250,
                "mssdr_queryreasons.description", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CRMLetterGrantType_ID", DisplayType.Integer, 10,
                "mssdr_queryreasons.crmlettergranttypeid - UNMAPPED, CHECKED: constant value 1 across all "
                + "23 rows, zero variance, no dedicated 'CRM letter type' table exists - carried as a "
                + "plain integer", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_queryreasons.financialyearid -> SDR_FinancialYear (shared). Values seen (24-27) fall "
                + "within the confirmed catalog's range", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
