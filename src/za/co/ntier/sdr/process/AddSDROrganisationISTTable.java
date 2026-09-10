package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new SDR_OrganisationIST
 * child table. Source table (mssdr_organisationist) is CURRENTLY EMPTY (0 rows) - CONFIRMED 2026-09-04
 * (user decision): build the table now for schema completeness, ready the moment the source system
 * populates it.
 *
 * <p>No lookup investigation was done for this table's several *_ID-shaped columns (ISTTransferTypeID,
 * ISTTransferStatusID, RequestingPersonTitleID, CurrentSetaID, NewSetaID, ChangeReasonID,
 * NewSICCodeID) - per the mapping doc, "not spending investigation time resolving its lookups until it
 * actually has data to test against". All are carried as plain integers, not wired as TableDir/Table
 * lookups, until a future pass revisits this once real data exists.
 *
 * <p>Schema only - no data population (table is empty in the source anyway).
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationISTTable")
public class AddSDROrganisationISTTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationIST";
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
                "An organisation's Inter-SETA Transfer record (mssdr_organisationist) - source table is "
                + "currently empty, built now for schema completeness", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationist.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SubmissionNumber", DisplayType.String, 250,
                "mssdr_organisationist.submissionnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ISTTransferType_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.isttransfertypeid - not yet resolved (no data to test against)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ISTTransferStatus_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.isttransferstatusid - not yet resolved (no data to test against)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateSetaReceivedApplication", DisplayType.DateTime, 7,
                "mssdr_organisationist.datesetareceivedapplication", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateApplicationSubmitted", DisplayType.DateTime, 7,
                "mssdr_organisationist.dateapplicationsubmitted", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateOrganisationSignedIST", DisplayType.DateTime, 7,
                "mssdr_organisationist.dateorganisationsignedist", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RequestingPersonTitle_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.requestingpersontitleid - not yet resolved (no data to test against)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RequestingPersonName", DisplayType.String, 250,
                "mssdr_organisationist.requestingpersonname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RequestingPersonSurname", DisplayType.String, 250,
                "mssdr_organisationist.requestingpersonsurname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RequestingPersonCapacity", DisplayType.String, 250,
                "mssdr_organisationist.requestingpersoncapacity", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TelephoneNumber", DisplayType.String, 50,
                "mssdr_organisationist.telephonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FaxNumber", DisplayType.String, 50,
                "mssdr_organisationist.faxnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Email", DisplayType.String, 250,
                "mssdr_organisationist.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CurrentSeta_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.currentsetaid - not yet resolved (no data to test against)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NewSeta_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.newsetaid - not yet resolved (no data to test against)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ChangeReason_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.changereasonid - not yet resolved (no data to test against)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransferMotivation", DisplayType.String, 2000,
                "mssdr_organisationist.transfermotivation (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NewSICCode_ID", DisplayType.Integer, 10,
                "mssdr_organisationist.newsiccodeid - not yet resolved (no data to test against)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateAdvisementLetterSent", DisplayType.DateTime, 7,
                "mssdr_organisationist.dateadvisementlettersent", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FollowUpComments", DisplayType.String, 2000,
                "mssdr_organisationist.followupcomments (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateSentToDOL", DisplayType.DateTime, 7,
                "mssdr_organisationist.datesenttodol", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateCheckHugeFileISTIn", DisplayType.DateTime, 7,
                "mssdr_organisationist.datecheckhugefileistin", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateRequestSentToDeloitte", DisplayType.DateTime, 7,
                "mssdr_organisationist.daterequestsenttodeloitte", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ISTSuccessfulPosted", DisplayType.Integer, 10,
                "mssdr_organisationist.istsuccessfulposted (flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateISTSuccessfullyProcessed", DisplayType.DateTime,
                7, "mssdr_organisationist.dateistsuccessfullyprocessed", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_organisationist.originalfilename - metadata only, no file retrieval", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_organisationist.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_organisationist.filepath (metadata only, not a working file path from here)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 27 business columns.";
    }
}
