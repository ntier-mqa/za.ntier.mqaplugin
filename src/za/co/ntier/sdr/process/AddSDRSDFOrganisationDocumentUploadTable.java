package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 4 (see "Phase 4 - SDF Family - Mapping.txt"): creates the brand new
 * SDR_SDFOrganisationDocumentUpload child table (3,244 source rows). Metadata only, per the
 * platform-wide document-upload convention.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRSDFOrganisationDocumentUploadTable")
public class AddSDRSDFOrganisationDocumentUploadTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_SDFOrganisationDocumentUpload";
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
                "A document attached to an SDF's organisation appointment "
                + "(mssdr_sdforganisationdocumentupload) - metadata only, no file retrieval", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDFOrganisation_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisationdocumentupload.sdforganisationid -> SDR_SDFOrganisation. CONFIRMED "
                + "99.1% match (3,215/3,244) - small gap not investigated further given the size",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_sdforganisationdocumentupload.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_sdforganisationdocumentupload.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_sdforganisationdocumentupload.filepath (metadata only, not a working file path "
                + "from here)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
