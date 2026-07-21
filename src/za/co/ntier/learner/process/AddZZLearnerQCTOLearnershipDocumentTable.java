package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerQCTOLearnershipDocument table (documents attached to a
 * learner's QCTO learnership enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt"
 * table #6. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOLearnershipDocumentTable")
public class AddZZLearnerQCTOLearnershipDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnershipDocument";
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

        ReferenceColumnSpec docTypeSpec = new ReferenceColumnSpec("QCTO_Learnership_Document_Type_ID",
                "ms_lkplearnerqctolearnershipdocumenttype", "id", "description",
                "ms_learnerqctolearnershipdocument.learnerqctolearnershipdocumenttypeid");
        String docTypeTableName = docTypeSpec.targetTableName();
        MTable docTypeTable = AddColumnsSupport.findTable(getCtx(), docTypeTableName, get_TrxName());
        if (docTypeTable == null) {
            docTypeTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), docTypeTableName,
                    "Reference values for " + docTypeSpec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), docTypeTable, docTypeSpec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + docTypeTableName + ".");
        } else {
            addLog(docTypeTableName + " already exists - left as-is (not re-populated).");
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Documents attached to a learner's QCTO learnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershipdocument.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "QCTO_Learnership_Document_Type_ID", DisplayType.TableDir, 10,
                docTypeSpec.description + " -> " + docTypeTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnerqctolearnershipdocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnerqctolearnershipdocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnerqctolearnershipdocument.filepath", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
