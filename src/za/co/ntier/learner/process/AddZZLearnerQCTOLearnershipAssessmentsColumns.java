package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Special case, NOT the usual AddZZ*Columns/AddZZ*Table pattern: ZZLearnerQCTOLearnershipAssessments
 * (Phase 1 table #5, see "Phase 1 - New Tables - Mapping.txt") already exists as a PHYSICAL
 * table (17 columns: the standard system columns, key, UUID, plus 8 business columns -
 * zzlearnerqctolearnership_id, zzqctomodule_id, zzrpl, zzassessorperson_id, zzassessmentdate,
 * zzmoderator_id, zzmoderationdate, zzdateassessmentcaptured), but confirmed 2026-07-20 to have
 * ZERO AD_Column rows registered in the Application Dictionary at all - it must have been
 * created by raw DDL or an external import that bypassed AD_Column entirely, unlike every
 * other table in this project.
 *
 * <p>This process does TWO different things, deliberately NOT treated the same way:
 * <ul>
 *   <li>The 9 already-physical standard/key/UUID columns and 8 already-physical business
 *       columns are BACKFILLED (AD_Column metadata registered via
 *       {@link AddColumnsSupport#backfillStandardColumns}/{@link AddColumnsSupport#registerColumn}
 *       - no DDL, since the physical column already exists; calling the normal addColumn()
 *       would generate an "ALTER TABLE ADD COLUMN" for a column that's already there and
 *       fail).</li>
 *   <li>The 6 genuinely NEW columns this table needs (an "id" recon column, plus
 *       Assessment_Status_ID/Is_Partial_Approved/Partial_Approved_By/Date_Partial_Approved/
 *       Is_Previously_Achieved - the same 5 "still needed" columns already identified in the
 *       mapping doc) are added the normal way (metadata + real ALTER TABLE), same as every
 *       AddZZ*Columns process.
 * </ul>
 *
 * <p>The "Assessment_Status" reference table (from ms_lkpassessmentstatus) is SHARED with
 * {@link AddZZLearnerQCTOArtisansAssessmentsTable} and
 * {@link AddZZLearnerQCTOSkillsProgrammeAssessmentsTable}.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOLearnershipAssessmentsColumns")
public class AddZZLearnerQCTOLearnershipAssessmentsColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnershipAssessments";
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
        MTable table = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (table == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        // Backfill AD_Column metadata for the columns that already physically exist - no DDL.
        AddColumnsSupport.backfillStandardColumns(getCtx(), table, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershipassessments.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQCTOModule_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershipassessments.qctomoduleid -> ZZQctoModule (ordinal crosswalk at data-migration time)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRPL", DisplayType.YesNo, 1,
                "ms_learnerqctolearnershipassessments.rpl", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZAssessorPerson_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerqctolearnershipassessments.assessorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssessmentDate", DisplayType.DateTime, 7,
                "ms_learnerqctolearnershipassessments.assessmentdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZModerator_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10, "ms_learnerqctolearnershipassessments.moderatorid (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZModerationDate", DisplayType.DateTime, 7,
                "ms_learnerqctolearnershipassessments.moderationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateAssessmentCaptured", DisplayType.DateTime, 7,
                "ms_learnerqctolearnershipassessments.dateassessmentcaptured", ENTITY_TYPE, get_TrxName());
        addLog("Backfilled AD_Column metadata for " + TABLE_NAME + "'s 17 already-physical columns.");

        // Genuinely new columns - real ALTER TABLE needed.
        ReferenceColumnSpec statusSpec = new ReferenceColumnSpec("Assessment_Status_ID",
                "ms_lkpassessmentstatus", "id", "description",
                "ms_learnerqctolearnershipassessments.assessmentstatusid (shared with QCTOArtisans/QCTOSkillsProgramme Assessments)");
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

        AddColumnsSupport.addColumn(getCtx(), table, "id", DisplayType.Integer, 10,
                "recon column - source ms_learnerqctolearnershipassessments row id", ENTITY_TYPE, get_TrxName(),
                this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Assessment_Status_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Is_Partial_Approved", DisplayType.YesNo, 1,
                "ms_learnerqctolearnershipassessments.ispartialapproved", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Partial_Approved_By", DisplayType.Search, REFERENCE_AD_USER, 10,
                "ms_learnerqctolearnershipassessments.partialapprovedby (ms_user email match)", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Date_Partial_Approved", DisplayType.DateTime, 7,
                "ms_learnerqctolearnershipassessments.datepartialapproved", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Is_Previously_Achieved", DisplayType.YesNo, 1,
                "ms_learnerqctolearnershipassessments.ispreviouslyachieved", ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": backfilled 17 existing columns, added 6 new columns.";
    }
}
