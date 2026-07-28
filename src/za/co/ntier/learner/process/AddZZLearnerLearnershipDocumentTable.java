package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #2): creates the brand
 * new ZZLearnerLearnershipDocument table (documents attached to a learner's LearnerLearnership
 * enrolment). Same engine as {@link AddZZLearnerLearnershipAssessmentsTable}.
 *
 * <p>Unlike the QCTO family's equivalent (AddZZLearnerQCTOLearnershipDocumentTable), no MSSQL
 * lookup table was found under any name searched for learnerlearnershipdocumenttypeid - added as
 * plain Integer (unresolved), same fallback already used elsewhere in this project for columns
 * with no discoverable crosswalk source (e.g. Phase 1's LearnershipACType/WAType/SDProviderType).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipDocumentTable")
public class AddZZLearnerLearnershipDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnershipDocument";
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
                "Documents attached to a learner's LearnerLearnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershipdocument.learnerlearnershipid -> zzlearnerlearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Document_Type", DisplayType.Integer, 10,
                "ms_learnerlearnershipdocument.learnerlearnershipdocumenttypeid (unresolved - "
                + "no MSSQL lookup table found under any name searched)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnerlearnershipdocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnerlearnershipdocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnerlearnershipdocument.filepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "id", DisplayType.Integer, 10,
                "recon column - source ms_learnerlearnershipdocument row id", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
