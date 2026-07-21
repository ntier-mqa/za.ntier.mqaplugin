package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerQCTOArtisansAssessments table (module assessment records for a
 * learner's QCTO artisan enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt" table
 * #3. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>ZZQCTOModule_ID resolves via Table Direct naming convention alone - the existing module
 * catalog's AD_Table.TableName is "ZZQctoModule" (confirmed 2026-07-16, already fully migrated
 * with all 579 rows via the same zzmigrationcode ordinal mechanism as
 * zzqualification/zzqctolearnership/etc - no new reference table needed here, this process
 * only ADDS the column; the actual crosswalk is built at data-migration time via
 * MigrationSupport.buildOrdinalCrosswalk).
 *
 * <p>The "Assessment_Status" reference table (from ms_lkpassessmentstatus) is SHARED with
 * {@link AddZZLearnerQCTOLearnershipAssessmentsColumns} and
 * {@link AddZZLearnerQCTOSkillsProgrammeAssessmentsTable}.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOArtisansAssessmentsTable")
public class AddZZLearnerQCTOArtisansAssessmentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOArtisansAssessments";
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
                "ms_learnerqctoartisansassessments.assessmentstatusid (shared with QCTOLearnership/QCTOSkillsProgramme Assessments)");
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
                "Module assessment records for a learner's QCTO artisan enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOArtisans_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoartisansassessments.learnerqctoartisansid -> zzlearnerqctoartisans",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQCTOModule_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoartisansassessments.qctomoduleid -> ZZQctoModule (ordinal crosswalk at data-migration time)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Assessment_Status_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 3 business columns.";
    }
}
