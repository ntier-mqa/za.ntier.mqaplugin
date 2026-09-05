package za.co.ntier.sdr.process;

import java.util.Properties;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.co.ntier.learner.process.AddColumnsSupport;
import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Phase 1 (see "SDR Migration Plan (Family and Phase Breakdown).txt" and the 8 per-family
 * mapping docs under ~/MQA/SDR_Migration): creates all 72 distinct SDR_ reference/catalog
 * tables identified across every family's mapping doc, and populates each one with a
 * straight copy from its staged mssdr_lkp* (or, for SDR_LearningProgramme,
 * mssdr_wsplearningprogramme) source table.
 *
 * <p>The 72 target names below were derived by extracting every "LOOKUP -&gt; SDR_X" reference
 * across all 8 mapping docs (not just each doc's own "REFERENCE TABLES NEEDED" summary,
 * which turned out to have omitted 3 real ones - SDR_OFOSpecialization, SDR_Year,
 * SDR_FormType - caught only by cross-checking against the column-level mapping lines
 * directly), then excluding every name that turned out to be a full business/main/catalog
 * table from its own family (e.g. SDR_Person, SDR_Organisation, SDR_GrantType,
 * SDR_WSPATR) rather than a small shared lookup - those get built later, in the main-table
 * phase, via {@link AddColumnsSupport#createNewTableSchema}, not this reference-table path.
 * Every remaining name was verified to exist as a real staged source table
 * (information_schema.tables) with the id/description shape {@link #SPECS} assumes, before
 * being hardcoded here - see the mapping docs' change logs for 2026-09-04.
 *
 * <p>Every one of these 72 tables shares the exact same shape - a plain Value/Name lookup, no
 * per-table customisation - so this is ONE data-driven process rather than 72 near-identical
 * AddZZ*Table-style classes (deliberate departure from the Learner project's one-class-per-table
 * convention, agreed 2026-09-04: that convention earns its keep when a table needs bespoke
 * extra columns, the way AddZZLearningAreaTable did - none of these 72 do).
 *
 * <p>Built on {@link AddColumnsSupport#createReferenceTableSchema} /
 * {@link AddColumnsSupport#populateReferenceTable} - both widened from package-private to
 * public 2026-09-04 for this cross-package reuse, in place, rather than duplicating ~600
 * lines of DDL/AD_Column machinery into a second copy inside this new package.
 *
 * <p>Idempotent: {@link AddColumnsSupport#findTable} skips any table that already exists -
 * schema creation AND population are skipped together as one unit. If a table exists but
 * was only partially populated by an earlier failed run, truncate it manually before
 * re-running; this process does not attempt to reconcile partial data.
 *
 * <p>ENTITY_TYPE is "U" (User Maintained, AD_EntityType_ID=100) - per user instruction
 * 2026-09-05, reusing this pre-existing core system entity type rather than inventing a new
 * "MQA SDR" one (which needed its own AD_EntityType row registered before MTable.getPO()
 * could resolve it - see AddColumnsSupport#ensureEntityType's Javadoc for how that surfaced;
 * that helper stays available for future use but isn't needed here now).
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRReferenceTables")
public class AddSDRReferenceTables extends SvrProcess {

    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "3";

    /** {target table name, source table, source Value column, source Name column, description}. */
    private static final String[][] SPECS = {
        // --- Person family (23 - Phase 2 mapping doc) ---
        {"SDR_Title", "mssdr_lkptitle", "id", "description", "Title reference (mssdr_lkptitle)"},
        {"SDR_Gender", "mssdr_lkpgender", "id", "description", "Gender reference (mssdr_lkpgender)"},
        {"SDR_Equity", "mssdr_lkpequity", "id", "description", "Equity reference (mssdr_lkpequity)"},
        {"SDR_Disability", "mssdr_lkpdisability", "id", "description", "Disability reference (mssdr_lkpdisability)"},
        {"SDR_HomeLanguage", "mssdr_lkphomelanguage", "id", "description", "Home language reference (mssdr_lkphomelanguage)"},
        {"SDR_Nationality", "mssdr_lkpnationality", "id", "description", "Nationality reference (mssdr_lkpnationality)"},
        {"SDR_CitizenResidentialStatus", "mssdr_lkpcitizenresidentialstatus", "id", "description", "Citizen residential status reference (mssdr_lkpcitizenresidentialstatus)"},
        {"SDR_SocioEconomicStatus", "mssdr_lkpsocioeconomicstatus", "id", "description", "Socio-economic status reference (mssdr_lkpsocioeconomicstatus)"},
        {"SDR_AlternateIDType", "mssdr_lkpalternateidtype", "id", "description", "Alternate ID type reference (mssdr_lkpalternateidtype)"},
        {"SDR_SchoolEMIS", "mssdr_lkpschoolemis", "id", "description", "School EMIS reference (mssdr_lkpschoolemis)"},
        {"SDR_LastSchoolYear", "mssdr_lkplastschoolyear", "id", "description", "Last school year reference (mssdr_lkplastschoolyear)"},
        {"SDR_STATSSAAreaCode", "mssdr_lkpstatssaareacode", "id", "description", "Stats SA area code reference (mssdr_lkpstatssaareacode)"},
        {"SDR_POPIActStatus", "mssdr_lkppopiactstatus", "id", "description", "POPI Act status reference (mssdr_lkppopiactstatus)"},
        {"SDR_HasSouthAfrican", "mssdr_lkphassouthafrican", "id", "description", "Has South African ID reference (mssdr_lkphassouthafrican)"},
        {"SDR_Verified", "mssdr_lkpverified", "id", "description", "Verified status reference (mssdr_lkpverified)"},
        {"SDR_ImmigrantStatus", "mssdr_lkpimmigrantstatus", "id", "description", "Immigrant status reference (mssdr_lkpimmigrantstatus)"},
        {"SDR_Suburb", "mssdr_lkpsuburb", "id", "description", "Suburb reference (mssdr_lkpsuburb)"},
        {"SDR_City", "mssdr_lkpcity", "id", "description", "City reference (mssdr_lkpcity)"},
        {"SDR_Municipality", "mssdr_lkpmunicipality", "id", "description", "Municipality reference (mssdr_lkpmunicipality)"},
        {"SDR_UrbanRural", "mssdr_lkpurbanrural", "id", "description", "Urban/rural reference (mssdr_lkpurbanrural)"},
        {"SDR_Province", "mssdr_lkpprovince", "id", "description", "Province reference (mssdr_lkpprovince)"},
        {"SDR_HealthFunctioningStatus", "mssdr_lkphealthfunctioningstatus", "id", "description", "Health functioning status reference (mssdr_lkphealthfunctioningstatus)"},
        {"SDR_HealthFunctioningRating", "mssdr_lkphealthfunctioningrating", "id", "description", "Health functioning rating reference (mssdr_lkphealthfunctioningrating)"},

        // --- Organisation family (17 new - Phase 3 mapping doc) ---
        {"SDR_OrganisationRegistrationNumberType", "mssdr_lkporganisationregistrationnumbertype", "id", "description", "Organisation registration number type reference"},
        {"SDR_TypeofOrganisation", "mssdr_lkptypeoforganisation", "id", "description", "Type of organisation reference"},
        {"SDR_LegalStatus", "mssdr_lkplegalstatus", "id", "description", "Legal status reference"},
        {"SDR_Partnership", "mssdr_lkppartnership", "id", "description", "Partnership reference"},
        {"SDR_SICCode", "mssdr_lkpsiccode", "id", "description", "SIC code reference"},
        {"SDR_OrganisationSize", "mssdr_lkporganisationsize", "id", "description", "Organisation size reference"},
        {"SDR_BEEStatus", "mssdr_lkpbeestatus", "id", "description", "BEE status reference (empty in source at time of writing)"},
        {"SDR_LevyNumberType", "mssdr_lkplevynumbertype", "id", "description", "Levy number type reference"},
        {"SDR_ChamberCode", "mssdr_lkpchambercode", "id", "description", "Chamber code reference"},
        {"SDR_SubSector", "mssdr_lkpsubsector", "id", "description", "Sub-sector reference"},
        {"SDR_OrganisationType", "mssdr_lkporganisationtype", "id", "description", "Organisation type reference (distinct from TypeofOrganisation)"},
        {"SDR_Designation", "mssdr_lkpdesignation", "id", "description", "Designation reference"},
        {"SDR_BankName", "mssdr_lkpbankname", "id", "description", "Bank name reference"},
        {"SDR_AccountType", "mssdr_lkpaccounttype", "id", "description", "Bank account type reference"},
        {"SDR_YesNo", "mssdr_lkpyesno", "id", "description", "Shared Yes/No reference - platform-wide '*YesNoID' convention"},
        {"SDR_FinancialYearEnd", "mssdr_lkpfinancialyearend", "id", "description", "Financial year end reference (empty in source at time of writing)"},
        {"SDR_WSPATRDocumentRelates", "mssdr_lkpwspatrdocumentrelates", "id", "description", "WSPATR document-relates reference"},

        // --- SDF family (5 - Phase 4 mapping doc) ---
        {"SDR_SDFHighestEducation", "mssdr_lkpsdfhighesteducation", "id", "description", "SDF highest education reference"},
        {"SDR_SDFStatus", "mssdr_lkpsdfstatus", "id", "description", "SDF status reference"},
        {"SDR_SDFRole", "mssdr_lkpsdfrole", "id", "description", "SDF role reference"},
        {"SDR_SDFFunction", "mssdr_lkpsdffunction", "id", "description", "SDF function reference"},
        {"SDR_SDFAppointmentProcedure", "mssdr_lkpsdfappointmentprocedure", "id", "description", "SDF appointment procedure reference"},

        // --- WSPATR family (19 new - Phase 5 mapping doc) ---
        {"SDR_WSPStatus", "mssdr_lkpwspstatus", "id", "description", "WSP status reference"},
        {"SDR_LearningProgrammeType", "mssdr_lkpwsplearningprogramme", "id", "description", "Learning programme TYPE reference (broad category, e.g. Bachelors_Degree/Bursary/Internship - distinct from SDR_LearningProgramme)"},
        {"SDR_LearningProgramme", "mssdr_wsplearningprogramme", "programmecode", "title", "Learning programme reference (specific named programme instances, e.g. 'MQA Qualification' - NOT an lkp*-shaped table, so Value/Name are its own programmecode/title columns rather than id/description)"},
        {"SDR_WSPAchievementStatus", "mssdr_lkpwspachievementstatus", "id", "description", "WSP achievement status reference (Achieved/Drop_out/In Progress/Not_Achieved)"},
        {"SDR_WSPDropOut", "mssdr_lkpwspdropout", "id", "description", "WSP drop-out reason reference"},
        {"SDR_WSPProvince", "mssdr_lkpwspprovince", "id", "description", "WSP-specific province reference (built for completeness; WSPATRBioData.ProvinceID uses the shared SDR_Province instead)"},
        {"SDR_WSPMunicipality", "mssdr_lkpwspmunicipality", "id", "description", "WSP-specific municipality reference (used in place of the shared SDR_Municipality for WSPATRBioData - measurably more complete there)"},
        {"SDR_WSPQualificationType", "mssdr_lkpwspqualificationtype", "id", "description", "WSP qualification type reference"},
        {"SDR_WSPManagementEquity", "mssdr_lkpwspmanagementequity", "id", "description", "WSP management equity reference"},
        {"SDR_WSPATREvaluationVerificationStatus", "mssdr_lkpwspatrevaluationverificationstatus", "id", "description", "WSPATR evaluation verification status reference"},
        {"SDR_WSPATREvaluationStatus", "mssdr_lkpwspatrevaluationstatus", "id", "description", "WSPATR evaluation status reference"},
        {"SDR_WSPATREvaluationApprovalStatus", "mssdr_lkpwspatrevaluationapprovalstatus", "id", "description", "WSPATR evaluation approval status reference"},
        {"SDR_WSPATREvaluationVerificationChecklistType", "mssdr_lkpwspatrevaluationverificationchecklisttype", "id", "description", "WSPATR evaluation verification checklist type reference"},
        {"SDR_WSPATREvaluationVerificationDeviation", "mssdr_lkpwspatrevaluationverificationdeviation", "id", "description", "WSPATR evaluation verification deviation reference"},
        {"SDR_WSPScarceReason", "mssdr_lkpwspscarcereason", "id", "description", "WSP scarce-skills reason reference (also used for WSPATRHTFV's Primary/First/SecondReasonID)"},
        {"SDR_WSPNonEmployeeStatus", "mssdr_lkpwspnonemployeestatus", "id", "description", "WSP non-employee status reference"},
        {"SDR_WSPTargetBeneficiary", "mssdr_lkpwsptargetbeneficiary", "id", "description", "WSP target beneficiary reference"},
        {"SDR_WSPTopUpSkills", "mssdr_lkpwsptopupskills", "id", "description", "WSP top-up skills reference"},
        {"SDR_WSPAppointment", "mssdr_lkpwspappointment", "id", "description", "WSP appointment/employment-status reference (also the confirmed target for WSPATRBioData.EmpStatusID, 99.995% match)"},

        // --- Levy family (3 - Phase 6 mapping doc) ---
        {"SDR_FinancialYear", "mssdr_lkpfinancialyear", "id", "description", "Financial year reference - shared across Grant/Levy/Organisation/WSPATR/Misc"},
        {"SDR_LevyField", "mssdr_lkplevyfield", "id", "description", "Levy field reference"},
        {"SDR_SETA", "mssdr_lkpseta", "id", "description", "SETA reference"},

        // --- Grant family (1 new - Phase 7 mapping doc; GrantType/GrantAccount/GrantProcess/
        //     GrantTransaction* are full business/catalog tables, built separately) ---
        {"SDR_GrantCode", "mssdr_lkpgrantcode", "id", "description", "Grant code reference"},

        // --- User/Security family (1 new - Phase 8 mapping doc) ---
        {"SDR_Role", "mssdr_lkprole", "id", "description", "User role reference"},

        // --- Cross-cutting catalogs used by WSPATR but never listed in any family's own
        //     "REFERENCE TABLES NEEDED" section - caught only by auditing every column-level
        //     "LOOKUP -> SDR_X" line directly across all 8 docs (2026-09-04) ---
        {"SDR_OFOSpecialization", "mssdr_lkpofospecialization", "id", "description", "OFO specialisation reference - confirmed 100% match on WSPATRBioData/HTFV/TopUpSkills/WorkplaceSkillsPlan.OFOSpecialisationID despite the source table's US-spelling name"},
        {"SDR_Year", "mssdr_lkpyear", "id", "description", "Generic year reference - used by WSPATRAnnualTrainingReport's YearEnrolled/YearCompleted and WSPATRBioData's BirthYear/EmpStartYear"},
        {"SDR_FormType", "mssdr_lkpformtype", "id", "description", "Form type reference - used by WSPATR.formTypeID and WSPATRForms.FormTypeID"},
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        Properties ctx = getCtx();
        String trxName = get_TrxName();
        int created = 0;
        int skipped = 0;

        for (String[] spec : SPECS) {
            String targetTable = spec[0];
            String sourceTable = spec[1];
            String valueCol = spec[2];
            String nameCol = spec[3];
            String description = spec[4];

            MTable existing = AddColumnsSupport.findTable(ctx, targetTable, trxName);
            if (existing != null) {
                addLog(targetTable + " already exists - not recreated.");
                skipped++;
                continue;
            }

            MTable table = AddColumnsSupport.createReferenceTableSchema(ctx, targetTable, description,
                    ENTITY_TYPE, ACCESS_LEVEL, trxName, this::addLog);

            ReferenceColumnSpec refSpec = new ReferenceColumnSpec(targetTable + "_ID", sourceTable, valueCol,
                    nameCol, description);
            AddColumnsSupport.populateReferenceTable(ctx, table, refSpec, trxName, this::addLog);
            created++;
        }

        return "SDR reference tables: created " + created + ", skipped " + skipped + " (already existed).";
    }
}
