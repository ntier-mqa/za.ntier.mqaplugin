package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 2 (see "Phase 2 - Person Family - Mapping.txt"): creates the brand new SDR_Person MAIN
 * table (923,166 source rows) - a person record, migrated independently of the already-existing
 * Learner-project zzperson table despite 99.997% population overlap (user decision 2026-09-03,
 * see mapping doc's ARCHITECTURAL DECISION note).
 *
 * <p>Every *_ID column below whose name matches its target table (e.g. SDR_Title_ID -&gt;
 * SDR_Title) uses plain DisplayType.TableDir, resolved by iDempiere's own naming convention.
 * The one exception is SDR_ParentPerson_ID (self-referencing - "SDR_ParentPerson" doesn't match
 * "SDR_Person"), which needs an explicit AD_Reference/AD_Ref_Table override via
 * {@link AddColumnsSupport#findOrCreateTableReference} (user decision 2026-09-05: build proper
 * references for non-matching FK columns rather than leaving them as plain integers, so every
 * lookup renders as a real name in the read-only window).
 *
 * <p>Schema only - this class does NOT populate any rows. Data migration (mssdr_person -&gt;
 * SDR_Person, with FK resolution) is a separate Migrate*-style process, not yet written.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRPersonTable")
public class AddSDRPersonTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_Person";
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
                "A person record (mssdr_person) - migrated independently of the Learner "
                + "project's zzperson despite population overlap, see mapping doc",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Title_ID", DisplayType.TableDir, 10,
                "mssdr_person.titleid -> SDR_Title", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FirstName", DisplayType.String, 250,
                "mssdr_person.firstname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MiddleName", DisplayType.String, 250,
                "mssdr_person.middlename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MiddleName2", DisplayType.String, 250,
                "mssdr_person.middlename2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Surname", DisplayType.String, 250,
                "mssdr_person.surname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Initials", DisplayType.String, 10,
                "mssdr_person.initials", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IDNo", DisplayType.String, 50,
                "mssdr_person.idno", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AlternateIDType_ID", DisplayType.TableDir, 10,
                "mssdr_person.alternateidtypeid -> SDR_AlternateIDType", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Birthday", DisplayType.DateTime, 7,
                "mssdr_person.dateofbirth", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Gender_ID", DisplayType.TableDir, 10,
                "mssdr_person.genderid -> SDR_Gender", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Equity_ID", DisplayType.TableDir, 10,
                "mssdr_person.equityid -> SDR_Equity", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Disability_ID", DisplayType.TableDir, 10,
                "mssdr_person.disabilityid -> SDR_Disability", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HomeLanguage_ID", DisplayType.TableDir, 10,
                "mssdr_person.homelanguageid -> SDR_HomeLanguage", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Nationality_ID", DisplayType.TableDir, 10,
                "mssdr_person.nationalityid -> SDR_Nationality", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CitizenResidentialStatus_ID", DisplayType.TableDir, 10,
                "mssdr_person.citizenresidentialstatusid -> SDR_CitizenResidentialStatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SocioEconomicStatus_ID", DisplayType.TableDir, 10,
                "mssdr_person.socioeconomicstatusid -> SDR_SocioEconomicStatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Phone", DisplayType.String, 50,
                "mssdr_person.telephonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "CellPhone", DisplayType.String, 50,
                "mssdr_person.cellphonenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Fax", DisplayType.String, 50,
                "mssdr_person.faxnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EMail", DisplayType.String, 50,
                "mssdr_person.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SchoolEMIS_ID", DisplayType.TableDir, 10,
                "mssdr_person.schoolemisid -> SDR_SchoolEMIS", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LastSchoolYear_ID", DisplayType.TableDir, 10,
                "mssdr_person.lastschoolyearid -> SDR_LastSchoolYear", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_STATSSAAreaCode_ID", DisplayType.TableDir, 10,
                "mssdr_person.statssaareacodeid -> SDR_STATSSAAreaCode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_POPIActStatus_ID", DisplayType.TableDir, 10,
                "mssdr_person.popiactstatusid -> SDR_POPIActStatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "POPIActStatusDate", DisplayType.DateTime, 7,
                "mssdr_person.popiactstatusdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HasSouthAfrican_ID", DisplayType.TableDir, 10,
                "mssdr_person.hassouthafricanid -> SDR_HasSouthAfrican", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Verified_ID", DisplayType.TableDir, 10,
                "mssdr_person.verifiedid -> SDR_Verified (effectively unused, kept for completeness "
                + "per user decision 2026-09-03)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HighestEducation", DisplayType.String, 500,
                "mssdr_person.highesteducation (free text, not an id)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CurrentOccupation", DisplayType.String, 500,
                "mssdr_person.currentoccupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_YearsInOccupation", DisplayType.Number, 10,
                "mssdr_person.yearsinoccupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Experience", DisplayType.String, 2000,
                "mssdr_person.experience", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImmigrantStatus_ID", DisplayType.TableDir, 10,
                "mssdr_person.immigrantstatusid -> SDR_ImmigrantStatus", ENTITY_TYPE, get_TrxName());

        // Self-referencing FK: "SDR_ParentPerson" (derived from the column name) does not match
        // "SDR_Person" (the actual target table), so Table Direct's naming convention can't
        // resolve it - needs an explicit AD_Reference/AD_Ref_Table override. The target table
        // (SDR_Person itself) already exists as an AD_Table record at this point (saveEx() ran
        // inside createNewTableSchema above), even though its physical table isn't created until
        // finalizeNewTable below - findOrCreateTableReference only needs the AD_Table row.
        int parentPersonRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), TABLE_NAME, ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ParentPerson_ID", DisplayType.Table,
                parentPersonRefId, 10,
                "mssdr_person.parentpersonid -> SDR_Person (self-referencing, only 40/923,166 rows "
                + "populated, all resolve to a real row - see mapping doc)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 33 business columns.";
    }
}
