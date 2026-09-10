package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new
 * SDR_WSPATRBulkApprovalDocumentUpload child table (215 source rows).
 *
 * <p>SDR_WSPATRIDs is NOT a foreign key despite the name - CHECKED 2026-09-04: it's a comma-separated
 * LIST of WSPATR.id values (e.g. "8850,8851"). CONFIRMED 2026-09-04 (user decision): kept as free text,
 * no join-table parsing.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRBulkApprovalDocumentUploadTable")
public class AddSDRWSPATRBulkApprovalDocumentUploadTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRBulkApprovalDocumentUpload";
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
                "A bulk-approval document upload covering multiple WSPATRs "
                + "(mssdr_wspatrbulkapprovaldocumentupload) - metadata only, no file retrieval", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRIDs", DisplayType.String, 2000,
                "mssdr_wspatrbulkapprovaldocumentupload.wspatrids - NOT a foreign key: a comma-separated "
                + "list of WSPATR.id values (e.g. '8850,8851'). CONFIRMED 2026-09-04 (user decision): kept "
                + "as free text, no join-table parsing", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_wspatrbulkapprovaldocumentupload.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_wspatrbulkapprovaldocumentupload.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_wspatrbulkapprovaldocumentupload.filepath (metadata only, not a working file path "
                + "from here)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
