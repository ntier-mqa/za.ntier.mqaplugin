package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Phase 4 (see "Phase 4 - LearnerUnitStandard Family - Mapping.txt" Section 2): creates the
 * brand new ZZLearnerUnitStandardAssessments table (1,180,150 source rows) - assessment records
 * for a learner's unit standard enrolment. Simpler shape than the other Assessments tables (no
 * partial-approval/previously-achieved columns exist on this source).
 *
 * <p>The "Assessment_Status" reference table (from ms_lkpassessmentstatus) is SHARED with the 4
 * Assessments tables that already created/use it (ZZLearnerQCTOArtisansAssessments/
 * ZZLearnerQCTOLearnershipAssessments/ZZLearnerQCTOSkillsProgrammeAssessments from Phase 1,
 * ZZLearnerLearnershipAssessments from Phase 2) - reused as-is, not re-created.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerUnitStandardAssessmentsTable")
public class AddZZLearnerUnitStandardAssessmentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerUnitStandardAssessments";
    private static final String ENTITY_TYPE = "MQA Learner";
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

        ReferenceColumnSpec statusSpec = new ReferenceColumnSpec("Assessment_Status_ID",
                "ms_lkpassessmentstatus", "id", "description",
                "ms_learnerunitstandardassessments.assessmentstatusid (shared with 4 other Assessments tables)");
        String statusTableName = statusSpec.targetTableName();
        MTable statusTable = AddColumnsSupport.findTable(getCtx(), statusTableName, get_TrxName());
        if (statusTable == null) {
            statusTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), statusTableName,
                    "Reference values for " + statusSpec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), statusTable, statusSpec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + statusTableName + ".");
        } else {
            addLog(statusTableName + " already exists - left as-is (not re-populated).");
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Assessment records for a learner's unit standard enrolment", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerUnitStandard_ID", DisplayType.TableDir, 10,
                "ms_learnerunitstandardassessments.learnerunitstandardid -> zzlearnerunitstandard",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZRPL", DisplayType.List, 319, 1,
                "ms_learnerunitstandardassessments.rpl", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZAssessorPerson_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandardassessments.assessorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssessmentDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandardassessments.assessmentdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZModerator_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandardassessments.moderatorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZModerationDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandardassessments.moderationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Assessment_Status_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
