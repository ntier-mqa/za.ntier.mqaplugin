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
 * SDR_OrganisationTrainingCommittee child table (3,840 source rows).
 *
 * <p>SDR_Title_ID and SDR_Designation_ID both match their target tables by name - plain
 * DisplayType.TableDir. SDR_IDNumber is free text captured independently on this table, NOT linked to
 * SDR_Person.SDR_IDNo via FK - no crosswalk attempted (per mapping doc).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationTrainingCommitteeTable")
public class AddSDROrganisationTrainingCommitteeTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationTrainingCommittee";
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
                "An organisation's training committee member (mssdr_organisationtrainingcommittee)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationtrainingcommittee.organisationid -> SDR_Organisation", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Title_ID", DisplayType.TableDir, 10,
                "mssdr_organisationtrainingcommittee.titleid -> SDR_Title (shared catalog)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FirstName", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.firstname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Surname", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.surname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IDNumber", DisplayType.String, 50,
                "mssdr_organisationtrainingcommittee.idnumber (free text, captured independently - NOT "
                + "linked to SDR_Person.SDR_IDNo, no crosswalk attempted)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Initials", DisplayType.String, 10,
                "mssdr_organisationtrainingcommittee.initials", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Designation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationtrainingcommittee.designationid -> SDR_Designation", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DesignationDescription", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.designationdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TelephoneNumber", DisplayType.String, 50,
                "mssdr_organisationtrainingcommittee.telephonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CellPhoneNumber", DisplayType.String, 50,
                "mssdr_organisationtrainingcommittee.cellphonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FaxNumber", DisplayType.String, 50,
                "mssdr_organisationtrainingcommittee.faxnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Email", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NameOfUnion", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.nameofunion", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PositionInUnion", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.positioninunion", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PercWorkForce", DisplayType.String, 50,
                "mssdr_organisationtrainingcommittee.percworkforce", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ContactDetails", DisplayType.String, 250,
                "mssdr_organisationtrainingcommittee.contactdetails", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 14 business columns.";
    }
}
