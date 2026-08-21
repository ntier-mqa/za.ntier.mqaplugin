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
 * Phase 4 (see "Phase 4 - LearnerAET Family - Mapping.txt" Child 2): creates the brand new
 * ZZLearnerAETDocument table (145,566 source rows) - documents attached to a learner's AET
 * enrolment.
 *
 * <p>Document_Type resolves via a NEW SHARED reference table, "Learner_Programme_Document_Type"
 * (from lkpLearnerProgrammeDocumentType, 39 rows, only just staged - see the mapping doc's
 * "STAGING PROJECT" section). Named generically rather than "AET_Document_Type" since the source
 * table's own rows span many programme families beyond AET (saqacode tags include AET/MEDP/HET/
 * SCPPI/CAN/IIBTCP/EDS/QSP/SP) - future smaller families' Document children should find-and-reuse
 * this same table rather than duplicating it, same sharing pattern as Assessment_Status.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerAETDocumentTable")
public class AddZZLearnerAETDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerAETDocument";
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

        ReferenceColumnSpec docTypeSpec = new ReferenceColumnSpec("Learner_Programme_Document_Type_ID",
                "ms_lkplearnerprogrammedocumenttype", "id", "description",
                "learneraetdocument.learnerprogrammedocumenttypeid (shared across many programme "
                + "families, not AET-specific)");
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
                "Documents attached to a learner's AET enrolment", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerAET_ID", DisplayType.TableDir, 10,
                "learneraetdocument.learneraetid -> zzlearneraet", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Document_Type_ID", DisplayType.TableDir, 10,
                docTypeSpec.description + " -> " + docTypeTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "learneraetdocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "learneraetdocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "learneraetdocument.filepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Uploaded_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraetdocument.createdby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Updated_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraetdocument.updatedby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
