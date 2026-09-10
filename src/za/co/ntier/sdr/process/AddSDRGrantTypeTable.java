package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantType catalog
 * table (530 source rows). Must be built before {@link AddSDRGrantTypeAccountTable}, which FKs to it.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTypeTable")
public class AddSDRGrantTypeTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantType";
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
                "A grant type catalog entry (mssdr_granttype)", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_granttype.financialyearid -> SDR_FinancialYear (shared)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantCode_ID", DisplayType.TableDir, 10,
                "mssdr_granttype.grantcodeid -> SDR_GrantCode (shared)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantName", DisplayType.String, 250,
                "mssdr_granttype.grantname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantDescription", DisplayType.String, 250,
                "mssdr_granttype.grantdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantPercentage", DisplayType.Number, 22,
                "mssdr_granttype.grantpercentage", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsPayable", DisplayType.YesNo, 1,
                "mssdr_granttype.ispayable (bit -> Y/N)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MinimumAmount", DisplayType.Amount, 22,
                "mssdr_granttype.minimumamount", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
