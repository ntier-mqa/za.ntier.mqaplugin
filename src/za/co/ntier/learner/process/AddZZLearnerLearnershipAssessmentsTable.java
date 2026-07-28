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
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #1): creates the brand
 * new ZZLearnerLearnershipAssessments table - unit standard assessment records for a learner's
 * LearnerLearnership enrolment. Near-identical shape to Phase 1's
 * ZZLearnerQCTOLearnershipAssessments/ZZLearnerQCTOArtisansAssessments, same engine.
 *
 * <p>ZZUnitStandard_ID resolves via Table Direct naming convention alone (AD_Table.TableName is
 * "ZZUnitStandard", confirmed) - the actual id crosswalk is an ordinal match on
 * zzunitstandard.zzmigrationcode at data-migration time (MigrationSupport.buildOrdinalCrosswalk),
 * same mechanism already used for zzqctomodule/zzqualification/etc. zzunitstandard itself is
 * already fully migrated (verified as part of Phase 2's "close out" task, see the mapping doc).
 *
 * <p>The "Assessment_Status" reference table (from ms_lkpassessmentstatus) is SHARED with the 3
 * Phase 1 tables that already created/use it (ZZLearnerQCTOArtisansAssessments/
 * ZZLearnerQCTOLearnershipAssessments/ZZLearnerQCTOSkillsProgrammeAssessments) - reused as-is,
 * not re-created.
 *
 * <p>4,091,083 source rows - by far the largest table this project has migrated data into so
 * far. This process only creates the table/columns (cheap); the corresponding Migrate* process
 * needs a batch/set-based SQL approach rather than a per-row Java PO loop.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipAssessmentsTable")
public class AddZZLearnerLearnershipAssessmentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnershipAssessments";
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
                "ms_learnerlearnershipassessments.assessmentstatusid (shared with the 3 Phase 1 Assessments tables)");
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
                "Unit standard assessment records for a learner's LearnerLearnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershipassessments.learnerlearnershipid -> zzlearnerlearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZUnitStandard_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershipassessments.unitstandardid -> ZZUnitStandard (ordinal crosswalk at data-migration time)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZRPL", DisplayType.List, 319, 1,
                "ms_learnerlearnershipassessments.rpl", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZAssessorPerson_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerlearnershipassessments.assessorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssessmentDate", DisplayType.DateTime, 7,
                "ms_learnerlearnershipassessments.assessmentdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZModerator_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerlearnershipassessments.moderatorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZModerationDate", DisplayType.DateTime, 7,
                "ms_learnerlearnershipassessments.moderationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Assessment_Status_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsPartialApproved", DisplayType.YesNo, 1,
                "ms_learnerlearnershipassessments.ispartialapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZPartialApprovedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerlearnershipassessments.partialapprovedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDatePartialApproved", DisplayType.DateTime, 7,
                "ms_learnerlearnershipassessments.datepartialapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsPreviouslyAchieved", DisplayType.YesNo, 1,
                "ms_learnerlearnershipassessments.ispreviouslyachieved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateAssessmentCaptured", DisplayType.DateTime, 7,
                "ms_learnerlearnershipassessments.dateassessmentcaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "id", DisplayType.Integer, 10,
                "recon column - source ms_learnerlearnershipassessments row id", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 13 business columns.";
    }
}
