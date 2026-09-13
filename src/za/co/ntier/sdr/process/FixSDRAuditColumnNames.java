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
 * One-off naming-convention fix (identified 2026-09-11, in another thread): every FK/reference-shaped
 * SDR_ column (TableDir/Table/Search) must end in "_ID" per platform convention - the ONLY exemptions
 * are iDempiere's own standard CreatedBy/UpdatedBy columns. A handful of audit-trail-style Search
 * columns (ApprovedBy, RejectedBy, etc., all resolving against AD_User the same way CreatedBy/
 * UpdatedBy do) were built without the suffix before this rule was caught - this drops and recreates
 * each one under its correct name.
 *
 * <p>Safe: all 4 affected tables (SDR_WSPATR, SDR_WSPATREvaluationVerification, SDR_SDFOrganisation,
 * SDR_GrantTransactionApproval) were CONFIRMED to have zero migrated rows at the time this fix was
 * written (2026-09-11) - dropping these columns loses nothing. SDR_LastUser_ID (on
 * SDR_GrantTransactionPaymentExceptions) already had the correct suffix and needs no fix.
 *
 * <p>Idempotent: {@link AddColumnsSupport#dropColumnIfExists} is a no-op if the old column is already
 * gone (e.g. re-running after this fix already succeeded), and
 * {@link AddColumnsSupport#addColumn(java.util.Properties, MTable, String, int, int, int, String, String, String, java.util.function.Consumer)}
 * skips if the new column already exists.
 */
@Process(name = "za.co.ntier.sdr.process.FixSDRAuditColumnNames")
public class FixSDRAuditColumnNames extends SvrProcess {

    private static final String ENTITY_TYPE = "U";

    // {tableName, oldColumnName, newColumnName, description}
    private static final String[][] FIXES = {
            {"SDR_WSPATR", "SDR_SubmittedBy", "SDR_SubmittedBy_ID", "mssdr_wspatr.submittedby -> AD_User"},
            {"SDR_WSPATR", "SDR_ApprovedBy", "SDR_ApprovedBy_ID", "mssdr_wspatr.approvedby -> AD_User"},
            {"SDR_WSPATR", "SDR_RejectedBy", "SDR_RejectedBy_ID", "mssdr_wspatr.rejectedby -> AD_User"},
            {"SDR_WSPATR", "SDR_ManualSubmissionBy", "SDR_ManualSubmissionBy_ID",
                    "mssdr_wspatr.manualsubmissionby -> AD_User"},
            {"SDR_WSPATR", "SDR_EvaluatedBy", "SDR_EvaluatedBy_ID", "mssdr_wspatr.evaluatedby -> AD_User"},
            {"SDR_WSPATREvaluationVerification", "SDR_VerifiedBy", "SDR_VerifiedBy_ID",
                    "mssdr_wspatrevaluationverification.verifiedby -> AD_User"},
            {"SDR_WSPATREvaluationVerification", "SDR_EvaluatedBy", "SDR_EvaluatedBy_ID",
                    "mssdr_wspatrevaluationverification.evaluatedby -> AD_User"},
            {"SDR_WSPATREvaluationVerification", "SDR_BoardApprovalBy", "SDR_BoardApprovalBy_ID",
                    "mssdr_wspatrevaluationverification.boardapprovalby -> AD_User"},
            {"SDR_SDFOrganisation", "SDR_ApprovedBy", "SDR_ApprovedBy_ID",
                    "mssdr_sdforganisation.approvedby -> AD_User"},
            {"SDR_SDFOrganisation", "SDR_RejectedBy", "SDR_RejectedBy_ID",
                    "mssdr_sdforganisation.rejectedby -> AD_User"},
            {"SDR_GrantTransactionApproval", "SDR_COORecommendedBy", "SDR_COORecommendedBy_ID",
                    "mssdr_granttransactionapproval.coorecommendedby -> AD_User"},
            {"SDR_GrantTransactionApproval", "SDR_CFORecommendedBy", "SDR_CFORecommendedBy_ID",
                    "mssdr_granttransactionapproval.cforecommendedby -> AD_User"},
            {"SDR_GrantTransactionApproval", "SDR_CEOApprovedBy", "SDR_CEOApprovedBy_ID",
                    "mssdr_granttransactionapproval.ceoapprovedby -> AD_User"},
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        for (String[] fix : FIXES) {
            String tableName = fix[0];
            String oldColumn = fix[1];
            String newColumn = fix[2];
            String description = fix[3];

            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            if (table == null) {
                throw new IllegalStateException("FixSDRAuditColumnNames: table '" + tableName + "' does not exist");
            }

            AddColumnsSupport.dropColumnIfExists(table, oldColumn, get_TrxName(), this::addLog);
            AddColumnsSupport.addColumn(getCtx(), table, newColumn, DisplayType.Search, REFERENCE_AD_USER, 10,
                    description, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        return "Processed " + FIXES.length + " audit-trail column rename(s) across 4 tables - see log for "
                + "per-column detail.";
    }
}
