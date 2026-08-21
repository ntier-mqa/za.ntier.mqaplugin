package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Phase 4 (see "Phase 4 - LearnerWorkExperience Family - Mapping.txt"): creates the brand new
 * ZZLearnerWorkExperience table (14,370 source rows) - a learner's work experience record.
 *
 * <p>ZZWorkExperienceStatus_ID is a NEW reference table ("Work_Experience_Status"), built from
 * the newly-staged ms_lkpworkexperiencestatus (4 rows - see the mapping doc's reference to Phase
 * 4 Step 21's lookup-table staging project). ZZLevy reuses the shared "Yes_No_Not_Applicable"
 * List reference find-or-created in Phase 2. Employer_ID/Alternate_Employer_ID reuse the
 * Search+200175 (C_BPartner) reference used throughout this project.
 *
 * <p>Institution_ID and Work_Experience_Type are added as plain unresolved Integer - no lookup
 * table found under any name searched (ms_lkpinstitutiontype is a 3-row TYPE catalog that
 * doesn't match institutionid's actual range). Qualification is ALSO plain unresolved Integer,
 * deliberately NOT joined to zzqualification like LearnerPostSchoolDetails did - qualificationid
 * here ranges far outside zzqualification's valid 1-222 range (see mapping doc), so it's
 * evidently a different, larger catalog; forcing the obvious-looking join would silently produce
 * mostly-wrong results.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerWorkExperienceTable")
public class AddZZLearnerWorkExperienceTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerWorkExperience";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_C_BPARTNER_ALL = 200175;
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

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

        ReferenceColumnSpec statusSpec = new ReferenceColumnSpec("Work_Experience_Status_ID",
                "ms_lkpworkexperiencestatus", "id", "description",
                "ms_learnerworkexperience.workexperiencestatusid -> ms_lkpworkexperiencestatus");
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

        int yesNoNaReferenceId = AddColumnsSupport.findOrCreateListReferenceFromDescriptions(getCtx(),
                YES_NO_NA_REFERENCE_NAME, "Shared No/Yes/Not Applicable list (ms_lkpyesnonotapplicable)",
                ENTITY_TYPE, get_TrxName(), "ms_lkpyesnonotapplicable", "description", this::addLog);

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "A learner's work experience record", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "ms_learnerworkexperience.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZReferenceNumber", DisplayType.String, 4000,
                "ms_learnerworkexperience.referencenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStartDate", DisplayType.DateTime, 7,
                "ms_learnerworkexperience.startdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEndDate", DisplayType.DateTime, 7,
                "ms_learnerworkexperience.enddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerworkexperience.employerid, resolved via "
                + "ms_organisation.sdlnumber = c_bpartner.zz_sdl_no", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Alternate_Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerworkexperience.alternateemployerid, same crosswalk",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, yesNoNaReferenceId,
                1, "ms_learnerworkexperience.levyyesnoid -> ms_lkpyesnonotapplicable", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZWorkExperienceStatus_ID", DisplayType.TableDir, 10,
                statusSpec.description + " -> " + statusTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmpContract", DisplayType.YesNo, 1,
                "ms_learnerworkexperience.empcontract", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmpContractCopy", DisplayType.YesNo, 1,
                "ms_learnerworkexperience.empcontractcopy", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZResponsibleSeta", DisplayType.String, 4000,
                "ms_learnerworkexperience.responsibleseta", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegSaqa", DisplayType.String, 4000,
                "ms_learnerworkexperience.regsaqa", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQCTO", DisplayType.String, 4000,
                "ms_learnerworkexperience.qcto", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCurRegNumber", DisplayType.String, 4000,
                "ms_learnerworkexperience.curregnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssPartner", DisplayType.String, 4000,
                "ms_learnerworkexperience.asspartner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDocumentsReceived", DisplayType.YesNo, 1,
                "ms_learnerworkexperience.documentsreceived", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZPreviouslyEmployed", DisplayType.YesNo, 1,
                "ms_learnerworkexperience.prevemployed", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZWPAgreement", DisplayType.YesNo, 1,
                "ms_learnerworkexperience.wpagreement", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZOccupation", DisplayType.String, 4000,
                "ms_learnerworkexperience.occupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Institution", DisplayType.Integer, 10,
                "ms_learnerworkexperience.institutionid (unresolved - no lookup table found)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Work_Experience_Type", DisplayType.Integer, 10,
                "ms_learnerworkexperience.workexperienceid (unresolved - no lookup table found)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Qualification", DisplayType.Integer, 10,
                "ms_learnerworkexperience.qualificationid (unresolved - range doesn't fit zzqualification, "
                + "see class Javadoc)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 21 business columns.";
    }
}
