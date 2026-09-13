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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new
 * SDR_WSPATREvaluationVerification child table (3,195 source rows). The 3 status-type FK columns all
 * match their target tables by name - plain DisplayType.TableDir. SDR_VerifiedBy_ID/SDR_EvaluatedBy_ID/
 * SDR_BoardApprovalBy_ID follow the platform's audit-trail pattern (Search + REFERENCE_AD_USER).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATREvaluationVerificationTable")
public class AddSDRWSPATREvaluationVerificationTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATREvaluationVerification";
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
                "A WSPATR's evaluation/verification workflow record (mssdr_wspatrevaluationverification)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverification.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATREvaluationVerificationStatus_ID",
                DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverification.wspatrevaluationverificationstatusid -> "
                + "SDR_WSPATREvaluationVerificationStatus (6 rows). CONFIRMED 100%", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATREvaluationStatus_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverification.wspatrevaluationstatusid -> SDR_WSPATREvaluationStatus "
                + "(5 rows). CONFIRMED 88.5% (2,827/3,195, nullable)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATREvaluationApprovalStatus_ID",
                DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverification.wspatrevaluationapprovalstatusid -> "
                + "SDR_WSPATREvaluationApprovalStatus (4 rows). CONFIRMED 83.3% (2,662/3,195, nullable)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_VerificationComment", DisplayType.String, 2000,
                "mssdr_wspatrevaluationverification.verificationcomment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EvaluationComment", DisplayType.String, 2000,
                "mssdr_wspatrevaluationverification.evaluationcomment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ApprovalComment", DisplayType.String, 2000,
                "mssdr_wspatrevaluationverification.approvalcomment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_VerifiedBy_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatrevaluationverification.verifiedby -> AD_User. CONFIRMED 99.9% (3,192/3,195)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateVerified", DisplayType.DateTime, 7,
                "mssdr_wspatrevaluationverification.dateverified", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_EvaluatedBy_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatrevaluationverification.evaluatedby -> AD_User. CONFIRMED 88.5% (2,827/3,195)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateEvaluated", DisplayType.DateTime, 7,
                "mssdr_wspatrevaluationverification.dateevaluated", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_BoardApprovalBy_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatrevaluationverification.boardapprovalby -> AD_User. CONFIRMED 83.3% (2,662/3,195)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateBoardApproval", DisplayType.DateTime, 7,
                "mssdr_wspatrevaluationverification.dateboardapproval", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_BulkBoardApprovalDate", DisplayType.DateTime, 7,
                "mssdr_wspatrevaluationverification.bulkboardapprovaldate", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 14 business columns.";
    }
}
