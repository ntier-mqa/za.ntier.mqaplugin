package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Creates the brand new ZZLearnershipDocument table (documents attached to the Learnership
 * CATALOG record itself, not a per-learner enrolment - same pattern as the already-existing
 * ZZLearnershipUnitStandard) - Phase 1, see "Phase 1 - New Tables - Mapping.txt" table #13.
 * Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}. Only 2 rows in MSSQL - trivial
 * volume.
 *
 * <p>ZZLearnership_ID resolves via Table Direct naming convention alone - matches the existing
 * ZZLearnership catalog table's AD_Table.TableName exactly.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnershipDocumentTable")
public class AddZZLearnershipDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnershipDocument";
    private static final String ENTITY_TYPE = "MQA Learner";

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
                "Documents attached to the Learnership catalog record",
                ENTITY_TYPE, "3", get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnershipdocument.learnershipid -> zzlearnership", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnershipdocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnershipdocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnershipdocument.filepath", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
