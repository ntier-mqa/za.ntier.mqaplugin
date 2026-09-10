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
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATR MAIN table
 * (12,737 source rows) - a Workplace Skills Plan / Annual Training Report submission. This is the
 * largest family in the migration by row count (its children include a 7.6M-row table).
 *
 * <p>All 5 FK columns match their target tables by name - plain DisplayType.TableDir.
 * SDR_SubmittedBy/SDR_ApprovedBy/SDR_RejectedBy/SDR_ManualSubmissionBy/SDR_EvaluatedBy all follow the
 * audit-trail pattern (Search + REFERENCE_AD_USER against iDempiere's own AD_User, no separate
 * "SDR_User" table) - each is a workflow-stage column with its own population rate (SubmittedBy 28.3%,
 * ApprovedBy 21.0%, RejectedBy 0.2%, ManualSubmissionBy 0.2%, EvaluatedBy always 0/null currently) but
 * all built as the same lookup shape regardless.
 *
 * <p>Schema only - this class does NOT populate any rows. Data migration is a separate Migrate*-style
 * process, not yet written.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRTable")
public class AddSDRWSPATRTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATR";
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
                "A Workplace Skills Plan / Annual Training Report submission (mssdr_wspatr)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_wspatr.organisationid -> SDR_Organisation. CONFIRMED 100% match (12,737/12,737)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_wspatr.financialyearid -> SDR_FinancialYear. CONFIRMED 99.8% match (12,708/12,737)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPStatus_ID", DisplayType.TableDir, 10,
                "mssdr_wspatr.wspstatusid -> SDR_WSPStatus. CONFIRMED 99.8% match (12,708/12,737)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FormType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatr.formtypeid -> SDR_FormType (shared with Grant/Misc families). CONFIRMED 100% "
                + "match (12,737/12,737)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatr.organisationtypeid -> SDR_OrganisationType (shared). CONFIRMED 96.5% match "
                + "(12,293/12,737)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DueDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.duedate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CreatedDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.createddate (a second, source-side 'created' concept distinct from the audit "
                + "Created column - kept as its own column, not collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SubmittedDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.submitteddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_SubmittedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatr.submittedby -> AD_User (audit-trail pattern). CONFIRMED 28.3% (3,607/12,737) - "
                + "a workflow-stage column, low match rate expected", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ApprovedDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.approveddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ApprovedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatr.approvedby -> AD_User. CONFIRMED 21.0% (2,677/12,737)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RejectedDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.rejecteddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_RejectedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatr.rejectedby -> AD_User. CONFIRMED 0.2% (24/12,737) - rejections are rare",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ManualSubmissionDate", DisplayType.DateTime, 7,
                "mssdr_wspatr.manualsubmissiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ManualSubmissionBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatr.manualsubmissionby -> AD_User. CONFIRMED 0.2% (21/12,737)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateEvaluated", DisplayType.DateTime, 7,
                "mssdr_wspatr.dateevaluated", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_EvaluatedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_wspatr.evaluatedby -> AD_User. CHECKED: always 0/null (0/12,737) - same shape as the "
                + "effectively-dead VerifiedBy/EvaluatedBy columns on OrganisationBankingDetails - carried "
                + "as a LOOKUP that will simply resolve to null for now", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 17 business columns.";
    }
}
