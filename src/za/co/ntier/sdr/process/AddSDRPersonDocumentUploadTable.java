package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): creates the brand new
 * SDR_PersonDocumentUpload child table (1,740 source rows). Metadata-only, per the platform-wide
 * document-upload convention confirmed across this whole migration: filename/date/uploader only,
 * no file preview or retrieval - the source FilePath points at a live SIMS web app filesystem
 * this migration has no access to (samples were ID/licence document scans, no document-type
 * column exists in the source to distinguish them).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRPersonDocumentUploadTable")
public class AddSDRPersonDocumentUploadTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_PersonDocumentUpload";
    private static final String ENTITY_TYPE = "U";
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
                "A person document upload record (mssdr_persondocumentupload) - metadata only, "
                + "no file retrieval (source FilePath points at a live SIMS server)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Person_ID", DisplayType.TableDir, 10,
                "mssdr_persondocumentupload.personid -> SDR_Person", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_persondocumentupload.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_persondocumentupload.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_persondocumentupload.filepath (metadata only, not a working file path from here)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
