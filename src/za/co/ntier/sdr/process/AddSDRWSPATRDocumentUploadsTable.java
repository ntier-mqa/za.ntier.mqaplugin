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
 * SDR_WSPATRDocumentUploads child table (14,343 source rows). This is WSPATR's OWN document-uploads
 * table, separate from SDR_OrganisationDocuments (Organisation family) - both reference Organisation
 * directly and both have their own DocumentRelatesID column, but they resolve DIFFERENTLY: this
 * column CONFIRMED 100% match (14,343/14,343) against SDR_WSPATRDocumentRelates, UNLIKE
 * OrganisationDocuments.DocumentRelatesID (only 9.6% match against the same lookup table). Two
 * identically-named columns on different tables, verified independently rather than assumed to behave
 * the same way.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRDocumentUploadsTable")
public class AddSDRWSPATRDocumentUploadsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRDocumentUploads";
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
                "A document attached directly to an organisation's WSPATR submission "
                + "(mssdr_wspatrdocumentuploads) - metadata only, no file retrieval", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrdocumentuploads.organisationid -> SDR_Organisation. CONFIRMED 100% match "
                + "(14,343/14,343)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrdocumentuploads.financialyearid -> SDR_FinancialYear. CONFIRMED 100% match",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Comment", DisplayType.String, 2000,
                "mssdr_wspatrdocumentuploads.comment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_wspatrdocumentuploads.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_wspatrdocumentuploads.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_wspatrdocumentuploads.filepath (metadata only, not a working file path from here)",
                ENTITY_TYPE, get_TrxName());

        int documentRelatesRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPATRDocumentRelates",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_DocumentRelates_ID", DisplayType.Table,
                documentRelatesRefId, 10,
                "mssdr_wspatrdocumentuploads.documentrelatesid -> SDR_WSPATRDocumentRelates. CONFIRMED 100% "
                + "match (14,343/14,343) - UNLIKE the same-named column on OrganisationDocuments (only "
                + "9.6% match against the same lookup table) - this is WSPATR's own column, resolves "
                + "cleanly here", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
