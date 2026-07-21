package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerQCTOArtisanDocument table (documents attached to a learner's
 * QCTO artisan trade programme enrolment) - a child of the already-migrated
 * ZZLearnerQCTOArtisans, Phase 1 of "Additional Tables - Migration Plan.txt". See
 * "Phase 1 - New Tables - Mapping.txt" table #1 for the full column-by-column reasoning.
 *
 * <p>Uses {@link AddColumnsSupport#createNewTableSchema}/{@link AddColumnsSupport#registerColumn}/
 * {@link AddColumnsSupport#finalizeNewTable} (2026-07-20) - the new-MAIN-table engine, distinct
 * from {@link AddColumnsSupport#createReferenceTableSchema} (small Value/Name lookup tables)
 * and the plain {@link AddColumnsSupport#addColumn} (adding one column to an ALREADY-physical
 * table) used by the earlier AddZZ*Columns processes. Every business column is registered
 * first (no DDL yet), then the whole table is physically created in one shot via
 * finalizeNewTable - MTable.getSQLCreate() only picks up columns already saved at the time
 * it's called.
 *
 * <p>ZZLearnerQCTOArtisans_ID resolves via Table Direct naming convention alone (no explicit
 * AD_Reference_Value_ID needed) since "ZZLearnerQCTOArtisans_ID" minus "_ID" matches the
 * existing ZZLearnerQCTOArtisans table's own AD_Table.TableName exactly.
 *
 * <p>QCTO_Artisan_Document_Type_ID's target reference table is created first if missing
 * (Value/Name shape, populated from ms_lkplearnerqctoartisandocumenttype) - same pattern as
 * every AddZZ*Columns process's Section B reference columns.
 *
 * <p>NOT idempotent by design: if the table already exists, this process logs and returns
 * without touching it (unlike AddZZ*Columns' drop-and-recreate columns, a whole TABLE is not
 * something to silently rebuild - there could be real migrated data in it by the time this is
 * re-run).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOArtisanDocumentTable")
public class AddZZLearnerQCTOArtisanDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOArtisanDocument";
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

        ReferenceColumnSpec docTypeSpec = new ReferenceColumnSpec("QCTO_Artisan_Document_Type_ID",
                "ms_lkplearnerqctoartisandocumenttype", "id", "description",
                "ms_learnerqctoartisandocument.learnerqctoartisandocumenttypeid");
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
                "Documents attached to a learner's QCTO artisan trade programme enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOArtisans_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoartisandocument.learnerqctoartisanid -> zzlearnerqctoartisans",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "QCTO_Artisan_Document_Type_ID", DisplayType.TableDir, 10,
                docTypeSpec.description + " -> " + docTypeTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnerqctoartisandocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnerqctoartisandocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnerqctoartisandocument.filepath", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
