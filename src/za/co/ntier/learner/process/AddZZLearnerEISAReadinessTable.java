package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerEISAReadiness table (FLC/SOR/SOWE readiness paperwork for a
 * learner's QCTO learnership enrolment, ahead of the actual EISA test) - Phase 1, see
 * "Phase 1 - New Tables - Mapping.txt" table #15. Same engine as
 * {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>MUST run BEFORE {@link AddZZLearnerEISAEnrolmentTable} - that table's
 * ZZLearnerEISAReadiness_ID column resolves via Table Direct naming convention against the
 * table this process creates, so it must already exist.
 *
 * <p>Confirmed with the business 2026-07-20 that this is conceptually distinct from
 * ZZLearnerQCTOLearnershipEISADetails (readiness paperwork vs the actual test date+result) -
 * both are built as separate tables.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerEISAReadinessTable")
public class AddZZLearnerEISAReadinessTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerEISAReadiness";
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

        ReferenceColumnSpec flcSpec = new ReferenceColumnSpec("EISA_FLC_ID", "ms_lkpeisaflc", "id", "description",
                "ms_learnereisareadiness.eisaflcid");
        String flcTableName = flcSpec.targetTableName();
        MTable flcTable = AddColumnsSupport.findTable(getCtx(), flcTableName, get_TrxName());
        if (flcTable == null) {
            flcTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), flcTableName,
                    "Reference values for " + flcSpec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), flcTable, flcSpec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + flcTableName + ".");
        } else {
            addLog(flcTableName + " already exists - left as-is (not re-populated).");
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "FLC/SOR/SOWE readiness paperwork for a learner's QCTO learnership enrolment, ahead of the EISA test",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnereisareadiness.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EISA_FLC_ID", DisplayType.TableDir, 10,
                flcSpec.description + " -> " + flcTableName, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FLC_Original_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.flcoriginalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FLC_Saved_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.flcsavedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FLC_File_Path", DisplayType.String, 4000,
                "ms_learnereisareadiness.flcfilepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOR_Original_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.sororiginalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOR_Saved_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.sorsavedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOR_File_Path", DisplayType.String, 4000,
                "ms_learnereisareadiness.sorfilepath", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FLC_SOR_Number", DisplayType.String, 4000,
                "ms_learnereisareadiness.flcsornumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FLC_SOR_Issue_Date", DisplayType.DateTime, 7,
                "ms_learnereisareadiness.flcsorissuedate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOWE_Original_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.soweoriginalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOWE_Saved_File_Name", DisplayType.String, 4000,
                "ms_learnereisareadiness.sowesavedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SOWE_File_Path", DisplayType.String, 4000,
                "ms_learnereisareadiness.sowefilepath", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 13 business columns.";
    }
}
