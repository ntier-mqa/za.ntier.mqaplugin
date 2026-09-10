package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new SDR_Organisation
 * MAIN table (13,801 source rows).
 *
 * <p>All 22 DHET* columns on the source table are collapsed into their non-DHET counterparts per the
 * mapping doc's decision (99.9%+ agreement confirmed) - only one set of columns is built here. This is
 * the opposite of OrganisationAddress, where the DHET block is kept separate (much lower agreement).
 *
 * <p>SDR_UnionisedYesNo_ID is the one FK column that doesn't match its target table by name (target is
 * the shared SDR_YesNo, not "SDR_UnionisedYesNo") - needs an explicit AD_Reference/AD_Ref_Table override
 * via {@link AddColumnsSupport#findOrCreateTableReference}. SDR_CurrentSetaRegion_ID and SDR_RegisterAs_ID
 * are UNMAPPED per the mapping doc (no matching lookup table found for either) - carried as plain integers,
 * not wired as lookups.
 *
 * <p>Schema only - this class does NOT populate any rows. Data migration (mssdr_organisation -&gt;
 * SDR_Organisation, with FK resolution) is a separate Migrate*-style process, not yet written.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationTable")
public class AddSDROrganisationTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_Organisation";
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
                "An organisation record (mssdr_organisation) - all 22 DHET* columns collapsed into "
                + "their non-DHET counterparts per mapping doc (99.9%+ agreement confirmed)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDLNumber", DisplayType.String, 50,
                "mssdr_organisation.sdlnumber - the organisation's core business key, target of every "
                + "Grant/Levy ReferenceNumber crosswalk", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PossibleSDLNumber", DisplayType.String, 50,
                "mssdr_organisation.possiblesdlnumber (nullable, sparsely populated)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LegalName", DisplayType.String, 250,
                "mssdr_organisation.legalname (DHETLegalName collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TradeName", DisplayType.String, 250,
                "mssdr_organisation.tradename (DHETTradeName collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationRegistrationNumberType_ID",
                DisplayType.TableDir, 10,
                "mssdr_organisation.organisationregistrationnumbertypeid -> "
                + "SDR_OrganisationRegistrationNumberType (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationRegistrationNumber", DisplayType.String,
                250, "mssdr_organisation.organisationregistrationnumber (DHET collapsed)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TypeofOrganisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.typeoforganisationid -> SDR_TypeofOrganisation (DHET collapsed)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LegalStatus_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.legalstatusid -> SDR_LegalStatus. CONFIRMED 99.9% identical to "
                + "DHETLegalStatusID (13,791/13,801)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Partnership_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.partnershipid -> SDR_Partnership (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PhoneNumber", DisplayType.String, 50,
                "mssdr_organisation.phonenumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FaxNumber", DisplayType.String, 50,
                "mssdr_organisation.faxnumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SICCode_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.siccodeid -> SDR_SICCode (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NumberOfEmployees", DisplayType.Integer, 10,
                "mssdr_organisation.numberofemployees (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TotalAnnualPayroll", DisplayType.Amount, 22,
                "mssdr_organisation.totalannualpayroll (money, DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SARSNumber", DisplayType.String, 50,
                "mssdr_organisation.sarsnumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CIPRONumber", DisplayType.String, 50,
                "mssdr_organisation.cipronumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PAYENumber", DisplayType.String, 50,
                "mssdr_organisation.payenumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UIFNumber", DisplayType.String, 50,
                "mssdr_organisation.uifnumber (DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationSize_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.organisationsizeid -> SDR_OrganisationSize (DHET collapsed)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CurrentVsNonCurrent", DisplayType.Integer, 10,
                "mssdr_organisation.currentvsnoncurrent (tinyint flag, DHET collapsed)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CurrentSetaRegion_ID", DisplayType.Integer, 10,
                "mssdr_organisation.currentsetaregionid - UNMAPPED, no matching table found (either current or "
                + "DHET version) - carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_BEEStatus_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.beestatusid -> SDR_BEEStatus (lookup itself is empty in the source - "
                + "resolves to NULL until the source system populates it)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyNumberType_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.levynumbertypeid -> SDR_LevyNumberType", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Communication", DisplayType.Integer, 10,
                "mssdr_organisation.communication (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ConfirmDetails", DisplayType.Integer, 10,
                "mssdr_organisation.confirmdetails (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ChamberCode_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.chambercodeid -> SDR_ChamberCode. CONFIRMED 100% match (11,301/11,301 of "
                + "populated non-zero values) - Organisation's OWN ChamberCodeID, distinct from "
                + "LevyAccount.ChamberCode (confirmed NOT a lookup in the Levy family)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Email", DisplayType.String, 250,
                "mssdr_organisation.email", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NumberOfEmployeesProfile", DisplayType.Integer, 10,
                "mssdr_organisation.numberofemployeesprofile", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RegisterAs_ID", DisplayType.Integer, 10,
                "mssdr_organisation.registerasid - UNMAPPED, no matching table found - carried as a plain value",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LegalStatusOther", DisplayType.String, 250,
                "mssdr_organisation.legalstatusother", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationRegNumberCode", DisplayType.Integer, 10,
                "mssdr_organisation.organisationregnumbercode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TerminatedEmployees", DisplayType.Integer, 10,
                "mssdr_organisation.terminatedemployees", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SubSector_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.subsectorid -> SDR_SubSector", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrganisationType_ID", DisplayType.TableDir, 10,
                "mssdr_organisation.organisationtypeid -> SDR_OrganisationType. CONFIRMED 2026-09-04 (user "
                + "decision): kept as a column distinct from SDR_TypeofOrganisation_ID above - both genuinely "
                + "populated, backed by two different lookup tables", ENTITY_TYPE, get_TrxName());

        // SDR_UnionisedYesNo_ID -> shared SDR_YesNo table (platform-wide "*YesNoID" convention). Column name
        // doesn't match its target ("SDR_UnionisedYesNo" != "SDR_YesNo"), so needs an explicit override.
        int yesNoRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_YesNo", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_UnionisedYesNo_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_organisation.unionisedyesnoid -> SDR_YesNo. CONFIRMED 100% match (1,254/1,254 of "
                + "populated non-zero values); 741 rows have literal 0 (distinct from NULL) that doesn't "
                + "resolve to either candidate lookup's id range - left unresolved for that subset",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 35 business columns.";
    }
}
