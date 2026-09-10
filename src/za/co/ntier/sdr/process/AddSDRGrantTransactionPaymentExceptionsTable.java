package za.co.ntier.sdr.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new
 * SDR_GrantTransactionPaymentExceptions child table (5,062 source rows).
 *
 * <p>SDR_LastUser_ID follows the platform's audit-trail pattern (Search + REFERENCE_AD_USER). Requires
 * both {@link AddSDRGrantTransactionTable} and {@link AddSDRGrantTransactionStatusTable} to have
 * already run.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTransactionPaymentExceptionsTable")
public class AddSDRGrantTransactionPaymentExceptionsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTransactionPaymentExceptions";
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
                "A payment exception raised against a grant transaction "
                + "(mssdr_granttransactionpaymentexceptions)", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantTransactionStatus_ID", DisplayType.TableDir,
                10, "mssdr_granttransactionpaymentexceptions.granttransactionstatusid -> "
                + "SDR_GrantTransactionStatus. CONFIRMED 100% match (5,062/5,062)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantTransaction_ID", DisplayType.TableDir, 10,
                "mssdr_granttransactionpaymentexceptions.granttransactionid -> SDR_GrantTransaction. "
                + "CONFIRMED 100% match", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Creditor", DisplayType.String, 250,
                "mssdr_granttransactionpaymentexceptions.creditor (not independently re-tested against "
                + "Organisation this pass, same L/D-number shape expected)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DocumentNumber", DisplayType.String, 250,
                "mssdr_granttransactionpaymentexceptions.documentnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_granttransactionpaymentexceptions.processid - UNMAPPED, same opaque/"
                + "false-positive-risk treatment as GrantTransaction.ProcessID", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusCode", DisplayType.Integer, 10,
                "mssdr_granttransactionpaymentexceptions.statuscode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusDescription", DisplayType.String, 250,
                "mssdr_granttransactionpaymentexceptions.statusdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorCode", DisplayType.Integer, 10,
                "mssdr_granttransactionpaymentexceptions.errorcode (plain value, same treatment as "
                + "GrantTransactionStatus.ErrorCode)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ErrorDescription", DisplayType.String, 250,
                "mssdr_granttransactionpaymentexceptions.errordescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LastDateChanged", DisplayType.DateTime, 7,
                "mssdr_granttransactionpaymentexceptions.lastdatechanged", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_LastUser_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_granttransactionpaymentexceptions.lastuserid -> AD_User (audit-trail pattern). "
                + "CONFIRMED 100% match (5,062/5,062)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 11 business columns.";
    }
}
