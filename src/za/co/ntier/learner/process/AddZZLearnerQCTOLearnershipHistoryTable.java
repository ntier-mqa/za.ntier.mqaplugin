package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Creates the brand new ZZLearnerQCTOLearnershipHistory table (a change-history/audit-trail
 * table: each row records an old/new pair of Provider/WorkplaceApproval/AssessmentCentre values
 * for one QCTO learnership enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt" table
 * #8. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>None of the 6 Old/New columns resolve via Table Direct naming convention (e.g.
 * "Lead_SD_Provider_Old" doesn't match "ZZProvider") so all 6 reuse the SAME pre-existing
 * AD_Reference entries the original ZZLearnerQCTOArtisans FK columns already use (confirmed by
 * reading those columns' own AD_Column rows directly) rather than creating new, redundant
 * references to the same 3 tables:
 * <ul>
 *   <li>ZZProvider - AD_Reference_ID=1000319, Search(30) (matches ZZLeadSDProvider_ID's own
 *       reference).</li>
 *   <li>ZZWorkplaceApproval - AD_Reference_ID=1000334, Table(18) (matches ZZWA_ID's own
 *       reference).</li>
 *   <li>ZZAssessmentCentre - AD_Reference_ID=1000335, Table(18) (matches ZZAC_ID's own
 *       reference).</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOLearnershipHistoryTable")
public class AddZZLearnerQCTOLearnershipHistoryTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnershipHistory";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final int REFERENCE_ZZPROVIDER = 1000319;
    private static final int REFERENCE_ZZWORKPLACEAPPROVAL = 1000334;
    private static final int REFERENCE_ZZASSESSMENTCENTRE = 1000335;

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
                "Change history of Provider/WorkplaceApproval/AssessmentCentre for a learner's QCTO learnership enrolment",
                ENTITY_TYPE, "3", get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershiphistory.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_SD_Provider_Old_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerqctolearnershiphistory.leadsdprovideridold -> zzprovider",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_SD_Provider_New_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerqctolearnershiphistory.leadsdprovideridnew -> zzprovider",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_WA_Old_ID", DisplayType.Table,
                REFERENCE_ZZWORKPLACEAPPROVAL, 10, "ms_learnerqctolearnershiphistory.leadwaidold -> zzworkplaceapproval",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Lead_WA_New_ID", DisplayType.Table,
                REFERENCE_ZZWORKPLACEAPPROVAL, 10, "ms_learnerqctolearnershiphistory.leadwaidnew -> zzworkplaceapproval",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "AC_Old_ID", DisplayType.Table,
                REFERENCE_ZZASSESSMENTCENTRE, 10, "ms_learnerqctolearnershiphistory.acidold -> zzassessmentcentre",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "AC_New_ID", DisplayType.Table,
                REFERENCE_ZZASSESSMENTCENTRE, 10, "ms_learnerqctolearnershiphistory.acidnew -> zzassessmentcentre",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
