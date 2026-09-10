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
 * SDR_OrganisationContacts child table (3,382 source rows).
 *
 * <p>SDR_Title_ID and SDR_Province_ID both match their target tables by name and are shared catalogs
 * already built for the Person family - plain DisplayType.TableDir, no override needed.
 * SDR_Designation here is plain free text (unlike SDR_OrganisationTrainingCommittee's
 * SDR_Designation_ID, which is a real lookup) - matches the mapping doc's column shape exactly.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationContactsTable")
public class AddSDROrganisationContactsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationContacts";
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
                "An organisation's contact person (mssdr_organisationcontacts)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationcontacts.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Title_ID", DisplayType.TableDir, 10,
                "mssdr_organisationcontacts.titleid -> SDR_Title (shared catalog with Person)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FirstName", DisplayType.String, 250,
                "mssdr_organisationcontacts.firstname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Surname", DisplayType.String, 250,
                "mssdr_organisationcontacts.surname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Designation", DisplayType.String, 250,
                "mssdr_organisationcontacts.designation (free text, not a lookup here)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TelephoneNumber", DisplayType.String, 50,
                "mssdr_organisationcontacts.telephonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CellPhoneNumber", DisplayType.String, 50,
                "mssdr_organisationcontacts.cellphonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FaxNumber", DisplayType.String, 50,
                "mssdr_organisationcontacts.faxnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Email", DisplayType.String, 250,
                "mssdr_organisationcontacts.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Province_ID", DisplayType.TableDir, 10,
                "mssdr_organisationcontacts.provinceid -> SDR_Province (shared catalog)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 9 business columns.";
    }
}
