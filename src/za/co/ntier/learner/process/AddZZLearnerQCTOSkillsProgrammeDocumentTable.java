package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Creates the brand new ZZLearnerQCTOSkillsProgrammeDocument table (documents attached to a
 * learner's QCTO skills programme enrolment) - Phase 1, see
 * "Phase 1 - New Tables - Mapping.txt" table #12. Same engine as
 * {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>Unlike its QCTOArtisan/QCTOLearnership Document siblings, no MSSQL lookup table was found
 * for LearnerQCTOSkillsProgrammeDocumentTypeID under any name searched (mapping doc Open Item
 * 3) - stored as plain Integer rather than a Table Direct reference.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOSkillsProgrammeDocumentTable")
public class AddZZLearnerQCTOSkillsProgrammeDocumentTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOSkillsProgrammeDocument";
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
                "Documents attached to a learner's QCTO skills programme enrolment",
                ENTITY_TYPE, "3", get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOSkillsProgramme_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoskillsprogrammedocument.learnerqctoskillsprogrammeid -> zzlearnerqctoskillsprogramme",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "QCTO_SkillsProgramme_Document_Type", DisplayType.Integer, 10,
                "ms_learnerqctoskillsprogrammedocument.learnerqctoskillsprogrammedocumenttypeid (no MSSQL lookup table found - plain Integer)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Original_File_Name", DisplayType.String, 4000,
                "ms_learnerqctoskillsprogrammedocument.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Saved_File_Name", DisplayType.String, 4000,
                "ms_learnerqctoskillsprogrammedocument.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "File_Path", DisplayType.String, 4000,
                "ms_learnerqctoskillsprogrammedocument.filepath", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
