package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new
 * SDR_OrganisationBankingDetailsDocumentUpload child table (1,282 source rows). All three of this
 * table's possible parent links are populated on every row (100% each) - CONFIRMED 2026-09-04, kept as
 * three separate columns matching source design, not deduplicated away.
 *
 * <p>SDR_SDF_ID carries the same "target table not built yet" caveat as
 * {@link AddSDROrganisationBankingDetailsTable}.SDR_SDF_ID - plain integer for now.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationBankingDetailsDocumentUploadTable")
public class AddSDROrganisationBankingDetailsDocumentUploadTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationBankingDetailsDocumentUpload";
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
                "A document upload attached to an organisation's banking details "
                + "(mssdr_organisationbankingdetailsdocumentupload) - metadata only, no file retrieval",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationBankingDetails_ID", DisplayType.TableDir,
                10, "mssdr_organisationbankingdetailsdocumentupload.organisationbankingdetailsid -> "
                + "SDR_OrganisationBankingDetails (treated as the primary parent)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationbankingdetailsdocumentupload.organisationid -> SDR_Organisation (redundant "
                + "convenience column, matching source design)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDF_ID", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetailsdocumentupload.sdfid -> SDR_SDF (not yet built, Phase 4) - "
                + "same partial-match caveat as OrganisationBankingDetails.SDF_ID, carried as a plain "
                + "integer for now", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_organisationbankingdetailsdocumentupload.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_organisationbankingdetailsdocumentupload.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_organisationbankingdetailsdocumentupload.filepath (metadata only, not a working "
                + "file path from here)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 6 business columns.";
    }
}
