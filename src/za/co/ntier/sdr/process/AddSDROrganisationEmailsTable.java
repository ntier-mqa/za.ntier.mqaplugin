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
 * SDR_OrganisationEmails child table (11,645 source rows).
 *
 * <p>SDR_GrantPaidLetter_ID and SDR_DGApplication_ID are CHECKED 2026-09-04: always 0/null across all
 * rows, effectively dead columns, carried as plain integers with no resolution attempted.
 *
 * <p>SDR_WSPATR_ID carries the same "target table not built yet" caveat as
 * {@link AddSDROrganisationDocumentsTable}.SDR_WSPATR_ID (CONFIRMED 100% match, 10,772/10,772 of
 * non-null values, but SDR_WSPATR doesn't exist until Phase 5) - plain integer for now.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationEmailsTable")
public class AddSDROrganisationEmailsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationEmails";
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
                "An email sent regarding an organisation (mssdr_organisationemails) - metadata only, "
                + "no file retrieval for its attachment block", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationemails.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ToAddress", DisplayType.String, 250,
                "mssdr_organisationemails.toaddress", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FromAddress", DisplayType.String, 250,
                "mssdr_organisationemails.fromaddress", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Subject", DisplayType.String, 250,
                "mssdr_organisationemails.subject", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Body", DisplayType.String, 2000,
                "mssdr_organisationemails.body (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_organisationemails.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_organisationemails.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_organisationemails.filepath (metadata only, not a working file path from here)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsSuccessful", DisplayType.Integer, 10,
                "mssdr_organisationemails.issuccessful (int flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MessageStatus", DisplayType.String, 250,
                "mssdr_organisationemails.messagestatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantPaidLetter_ID", DisplayType.Integer, 10,
                "mssdr_organisationemails.grantpaidletterid - CHECKED: always 0/null across all rows, "
                + "effectively a dead column", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EmailSentError", DisplayType.String, 2000,
                "mssdr_organisationemails.emailsenterror (free text, nullable)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DGApplication_ID", DisplayType.Integer, 10,
                "mssdr_organisationemails.dgapplicationid - same as GrantPaidLetter_ID, always 0/null",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.Integer, 10,
                "mssdr_organisationemails.wspatrid -> SDR_WSPATR (not yet built, Phase 5) - crosswalk "
                + "CONFIRMED 100% match (10,772/10,772 of non-null values) but carried as a plain integer "
                + "until SDR_WSPATR exists and this column can be upgraded to a proper Table reference",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 14 business columns.";
    }
}
