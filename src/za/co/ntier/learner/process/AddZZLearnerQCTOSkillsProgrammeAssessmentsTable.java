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
 * Creates the brand new ZZLearnerQCTOSkillsProgrammeAssessments table (module assessment
 * records for a learner's QCTO skills programme enrolment) - Phase 1, see
 * "Phase 1 - New Tables - Mapping.txt" table #11. Same engine and same ZZQCTOModule_ID/
 * Assessment_Status_ID resolution as {@link AddZZLearnerQCTOArtisansAssessmentsTable} - see
 * that class's Javadoc.
 *
 * <p>Assessor_ID/Moderator_ID/Partial_Approved_By are actor columns (ms_user email match at
 * data-migration time, same as every other actor column in this project) - Search(30) +
 * SystemIDs.REFERENCE_AD_USER(110), same convention as the existing
 * ZZLearnerQCTOArtisans.ZZEnrolledBy etc.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOSkillsProgrammeAssessmentsTable")
public class AddZZLearnerQCTOSkillsProgrammeAssessmentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOSkillsProgrammeAssessments";
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
                "ms_learnerqctoskillsprogrammeassessments.assessmentstatusid (shared with QCTOArtisans/QCTOLearnership Assessments)");
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
                "Module assessment records for a learner's QCTO skills programme enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOSkillsProgramme_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoskillsprogrammeassessments.learnerqctoskillsprogrammeid -> zzlearnerqctoskillsprogramme",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQCTOModule_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoskillsprogrammeassessments.qctomoduleid -> ZZQctoModule (ordinal crosswalk at data-migration time)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRPL", DisplayType.YesNo, 1,
                "ms_learnerqctoskillsprogrammeassessments.rpl", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Assessor_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerqctoskillsprogrammeassessments.assessorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Assessment_Date", DisplayType.DateTime, 7,
                "ms_learnerqctoskillsprogrammeassessments.assessmentdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Moderator_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerqctoskillsprogrammeassessments.moderatorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Moderation_Date", DisplayType.DateTime, 7,
                "ms_learnerqctoskillsprogrammeassessments.moderationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Assessment_Status_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Is_Partial_Approved", DisplayType.YesNo, 1,
                "ms_learnerqctoskillsprogrammeassessments.ispartialapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Partial_Approved_By", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerqctoskillsprogrammeassessments.partialapprovedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Date_Partial_Approved", DisplayType.DateTime, 7,
                "ms_learnerqctoskillsprogrammeassessments.datepartialapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Is_Previously_Achieved", DisplayType.YesNo, 1,
                "ms_learnerqctoskillsprogrammeassessments.ispreviouslyachieved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Date_Assessment_Captured", DisplayType.DateTime, 7,
                "ms_learnerqctoskillsprogrammeassessments.dateassessmentcaptured", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 12 business columns.";
    }
}
