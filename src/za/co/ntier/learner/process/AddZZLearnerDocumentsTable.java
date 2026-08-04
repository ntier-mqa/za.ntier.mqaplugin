package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 4 (see "Phase 4 - LearnerDocuments Family - Mapping.txt"): creates the brand new
 * ZZLearnerDocuments table (349,897 source rows) - a generic document a learner uploaded, NOT
 * tied to any specific programme/learnership/artisans/skillsprogramme enrolment (unlike every
 * other "...Document" child table built so far in this project, which all link to a specific
 * enrolment row - this one links directly to the learner).
 *
 * <p>Uploaded_By/Updated_By (createdby/updatedby on the source) resolve via the same ms_user
 * email-match crosswalk as every other actor column in this project - named distinctly from the
 * standard system CreatedBy/UpdatedBy columns (which the migration process itself stamps to the
 * running user/historical timestamps) to avoid confusion between "who uploaded this file" and
 * "who ran the migration".
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerDocumentsTable")
public class AddZZLearnerDocumentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerDocuments";
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
                "A generic document a learner uploaded, not tied to any specific enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "learnerdocuments.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "learnerdocuments.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "learnerdocuments.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "learnerdocuments.filepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Uploaded_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learnerdocuments.createdby (ms_user email match - who uploaded "
                + "this file, distinct from the row's own system CreatedBy)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Updated_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learnerdocuments.updatedby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
