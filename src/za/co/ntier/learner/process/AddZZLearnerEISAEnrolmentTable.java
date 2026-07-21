package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerEISAEnrolment table (links a learner + their QCTO learnership
 * enrolment + an assessment centre + their readiness record to a single EISA test) - Phase 1,
 * see "Phase 1 - New Tables - Mapping.txt" table #14. Same engine as
 * {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>MUST run AFTER {@link AddZZLearnerEISAReadinessTable} - ZZLearnerEISAReadiness_ID below
 * resolves via Table Direct naming convention against that table, which must already exist.
 *
 * <p>ZZLearner_ID, ZZLearnerQCTOLearnership_ID, and ZZLearnerEISAReadiness_ID all resolve via
 * Table Direct naming convention alone (no explicit AD_Reference_Value_ID needed) - each
 * matches its target table's own AD_Table.TableName exactly. AC_ID reuses the same
 * pre-existing AD_Reference (1000335, Table) the existing ZZLearnerQCTOArtisans.ZZAC_ID column
 * already uses for ZZAssessmentCentre.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerEISAEnrolmentTable")
public class AddZZLearnerEISAEnrolmentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerEISAEnrolment";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
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

        MTable readinessTable = AddColumnsSupport.findTable(getCtx(), "ZZLearnerEISAReadiness", get_TrxName());
        if (readinessTable == null) {
            throw new org.adempiere.exceptions.AdempiereException(
                    "ZZLearnerEISAReadiness must be created first (run AddZZLearnerEISAReadinessTable) - "
                    + "ZZLearnerEISAReadiness_ID resolves via Table Direct naming convention against it.");
        }

        ReferenceColumnSpec achievementSpec = new ReferenceColumnSpec("EISA_Achievement_Value_ID",
                "ms_lkpeisaachievementvalue", "id", "description", "ms_learnereisaenrolment.eisaachievementvalueid");
        String achievementTableName = achievementSpec.targetTableName();
        MTable achievementTable = AddColumnsSupport.findTable(getCtx(), achievementTableName, get_TrxName());
        if (achievementTable == null) {
            achievementTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), achievementTableName,
                    "Reference values for " + achievementSpec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), achievementTable, achievementSpec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + achievementTableName + ".");
        } else {
            addLog(achievementTableName + " already exists - left as-is (not re-populated).");
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Links a learner + their QCTO learnership enrolment + an assessment centre + their readiness record to a single EISA test",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "ms_learnereisaenrolment.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnereisaenrolment.qctolearnershipid -> zzlearnerqctolearnership", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "AC_ID", DisplayType.Table,
                REFERENCE_ZZASSESSMENTCENTRE, 10, "ms_learnereisaenrolment.assessmentcentreid -> zzassessmentcentre",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerEISAReadiness_ID", DisplayType.TableDir, 10,
                "ms_learnereisaenrolment.eisareadinessid -> zzlearnereisareadiness", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EISA_Serial_Number", DisplayType.String, 4000,
                "ms_learnereisaenrolment.eisaserialnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EISA_Achievement_Value_ID", DisplayType.TableDir, 10,
                achievementSpec.description + " -> " + achievementTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Date_Assessed", DisplayType.DateTime, 7,
                "ms_learnereisaenrolment.dateassessed", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EISA_Percentage_Obtained", DisplayType.Integer, 10,
                "ms_learnereisaenrolment.eisapercentageobtained", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 8 business columns.";
    }
}
