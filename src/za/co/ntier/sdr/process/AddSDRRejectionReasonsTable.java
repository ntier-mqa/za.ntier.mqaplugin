package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 1b/Misc (see "Phase 1b - Misc Family - Mapping.txt"): creates the brand new
 * SDR_RejectionReasons catalog table (a single source row: "2023 WSP/ATR Non Approval"). Same shape
 * and same CRMLetterGrantTypeID=1 constant as {@link AddSDRQueryReasonsTable} - almost certainly the
 * same underlying concept/table design, just a much less frequently used one. Grouped with WSPATR in
 * the Application Dictionary, same placement decision as QueryReasons.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRRejectionReasonsTable")
public class AddSDRRejectionReasonsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_RejectionReasons";
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
                "A WSPATR/WSP submission rejection reason (mssdr_rejectionreasons)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Name", DisplayType.String, 250,
                "mssdr_rejectionreasons.name", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Description", DisplayType.String, 250,
                "mssdr_rejectionreasons.description", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CRMLetterGrantType_ID", DisplayType.Integer, 10,
                "mssdr_rejectionreasons.crmlettergranttypeid - UNMAPPED, same constant/zero-variance "
                + "reasoning as SDR_QueryReasons.SDR_CRMLetterGrantType_ID - carried as a plain integer",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_rejectionreasons.financialyearid -> SDR_FinancialYear (shared). Confirmed for the "
                + "one row present (value 24)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
