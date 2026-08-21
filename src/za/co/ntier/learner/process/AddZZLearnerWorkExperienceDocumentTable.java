package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 4 (see "Phase 4 - LearnerWorkExperience Family - Mapping.txt"): creates the brand new
 * ZZLearnerWorkExperienceDocument table (240 source rows) - documents attached to a learner's
 * work experience record. Simplest Document child built so far - no document type column exists
 * on this source at all (unlike LearnerAETDocument's shared Learner_Programme_Document_Type).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerWorkExperienceDocumentTable")
public class AddZZLearnerWorkExperienceDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerWorkExperienceDocument";
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

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Documents attached to a learner's work experience record", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerWorkExperience_ID", DisplayType.TableDir, 10,
                "ms_learnerworkexperiencedocument.learnerworkexperienceid -> zzlearnerworkexperience",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnerworkexperiencedocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnerworkexperiencedocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnerworkexperiencedocument.filepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Uploaded_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerworkexperiencedocument.createdby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Updated_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerworkexperiencedocument.updatedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
