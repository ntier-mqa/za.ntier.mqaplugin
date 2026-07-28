package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #5): creates the brand
 * new ZZLearnerLearnershipHistory table (a change-history/audit-trail table: each row records an
 * old/new pair of Provider/Employer values for one LearnerLearnership enrolment). Same shape as
 * Phase 1's {@link AddZZLearnerQCTOLearnershipHistoryTable}, Provider+Employer here instead of
 * Provider+WorkplaceApproval+AssessmentCentre.
 *
 * <p>None of the 4 Old/New columns resolve via Table Direct naming convention, so all 4 reuse the
 * SAME pre-existing AD_Reference entries already established elsewhere in this project:
 * <ul>
 *   <li>ZZProvider - AD_Reference_ID=1000319, Search(30).</li>
 *   <li>C_BPartner (all) - AD_Reference_ID=200175, Search(30) (same as Employer_ID on
 *       ZZLearnerLearnershipEmployer/ZZLearnerQCTOArtisans/ZZLearnerQCTOLearnership).</li>
 * </ul>
 * Smallest of the 5 Phase 2 children (397 source rows) - lowest risk to build/test first.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipHistoryTable")
public class AddZZLearnerLearnershipHistoryTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnershipHistory";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_ZZPROVIDER = 1000319;
    private static final int REFERENCE_C_BPARTNER_ALL = 200175;

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
                "Change history of Provider/Employer for a learner's LearnerLearnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershiphistory.learnerlearnershipid -> zzlearnerlearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_Provider_Old_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerlearnershiphistory.leadprovideridold -> zzprovider",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_Provider_New_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerlearnershiphistory.leadprovideridnew -> zzprovider",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_Employer_Old_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerlearnershiphistory.leademployeridold -> c_bpartner",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_Employer_New_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerlearnershiphistory.leademployeridnew -> c_bpartner",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "id", DisplayType.Integer, 10,
                "recon column - source ms_learnerlearnershiphistory row id", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
