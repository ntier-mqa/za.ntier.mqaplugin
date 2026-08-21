package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Phase 4 (see "Phase 4 - LearnerInternship Family - Mapping.txt"): creates the brand new
 * ZZLearnerInternship table (9,145 source rows) - a learner's internship placement record.
 *
 * <p>Builds 12 NEW reference tables (same TableDir/id-description mechanism as
 * Work_Experience_Status in the LearnerWorkExperience family): Internship_Type,
 * Internship_Status, Internship_Qualification_Type, Internship_Disciplines (closes Outstanding
 * Issues item 1.3 - now that ms_lkpinternshipdisciplines is staged, its 30 rows fully cover the
 * 23 distinct values actually used), Type_Of_Placement, Placement_Status,
 * Highest_Education_Level, Year_Of_Study, NQF_Level, SIC_Code, Graduate_Intern, and the SHARED
 * Financial_Year table (closes the "universal ZZ_FinYear_ID gap" flagged in Outstanding Issues -
 * available for any future family with the same gap, not just this one).
 *
 * <p>Employer_ID/Alternate_Employer_ID/Placement_Employer_ID reuse the Search+200175
 * (C_BPartner) reference. ZZSponsorship/ZZSocioEconomicStatus/ZZTerminationReason/
 * ZZEnrolmentStatusReason reuse the shared List references first built for LearnerAET.
 * ZZLevy reuses the shared Yes_No_Not_Applicable List.
 *
 * <p>OFO_Occupation, Institution, Qualification, and Training_Provider_Public_Private are all
 * added as plain unresolved Integer - see the mapping doc's OPEN ITEMS for why each was rejected
 * (in particular, OFO_Occupation was investigated in depth: zzlkpofooccupation.value is not
 * unique, so a code-based join would produce ambiguous/silently-wrong matches). Physical
 * municipality/urban-rural/province/suburb/city are NOT mapped at all - no set-based equivalent
 * of Phase 2's per-row MigrationSupport.createLocation() pattern exists yet in Phase 4.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerInternshipTable")
public class AddZZLearnerInternshipTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerInternship";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_C_BPARTNER_ALL = 200175;
    private static final int REFERENCE_SPONSORSHIP = 1000251;
    private static final int REFERENCE_SOCIOECONOMICSTATUS = 1000250;
    private static final int REFERENCE_TERMINATIONREASON = 1000254;
    private static final int REFERENCE_ENROLMENTSTATUSREASON = 1000255;
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

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

        ReferenceColumnSpec internshipTypeSpec = new ReferenceColumnSpec("Internship_Type_ID",
                "ms_lkpinternshiptype", "id", "description",
                "ms_learnerinternship.internshiptypeid -> ms_lkpinternshiptype");
        ReferenceColumnSpec internshipStatusSpec = new ReferenceColumnSpec("Internship_Status_ID",
                "ms_lkpinternshipstatus", "id", "description",
                "ms_learnerinternship.internshipstatusid -> ms_lkpinternshipstatus");
        ReferenceColumnSpec internshipQualTypeSpec = new ReferenceColumnSpec(
                "Internship_Qualification_Type_ID", "ms_lkpinternshipqualificationtype", "id", "description",
                "ms_learnerinternship.internshipqualificationtypeid -> ms_lkpinternshipqualificationtype");
        ReferenceColumnSpec internshipDisciplinesSpec = new ReferenceColumnSpec("Internship_Disciplines_ID",
                "ms_lkpinternshipdisciplines", "id", "description",
                "ms_learnerinternship.internshipdisciplinesid -> ms_lkpinternshipdisciplines");
        ReferenceColumnSpec typeOfPlacementSpec = new ReferenceColumnSpec("Type_Of_Placement_ID",
                "ms_lkptypeofplacement", "id", "description",
                "ms_learnerinternship.typeofplacementid -> ms_lkptypeofplacement");
        ReferenceColumnSpec placementStatusSpec = new ReferenceColumnSpec("Placement_Status_ID",
                "ms_lkpplacementstatus", "id", "description",
                "ms_learnerinternship.placementstatusid -> ms_lkpplacementstatus");
        ReferenceColumnSpec highestEducationLevelSpec = new ReferenceColumnSpec("Highest_Education_Level_ID",
                "ms_lkphighesteducationlevel", "id", "description",
                "ms_learnerinternship.highesteducationlevelid -> ms_lkphighesteducationlevel");
        ReferenceColumnSpec yearOfStudySpec = new ReferenceColumnSpec("Year_Of_Study_ID",
                "ms_lkpyearofstudy", "id", "description",
                "ms_learnerinternship.yearofstudyid -> ms_lkpyearofstudy");
        ReferenceColumnSpec nqfLevelSpec = new ReferenceColumnSpec("NQF_Level_ID",
                "ms_lkpnqflevel", "id", "description",
                "ms_learnerinternship.nqflevelid -> ms_lkpnqflevel");
        ReferenceColumnSpec sicCodeSpec = new ReferenceColumnSpec("SIC_Code_ID",
                "ms_lkpsiccode", "id", "description",
                "ms_learnerinternship.siccodeid -> ms_lkpsiccode");
        ReferenceColumnSpec graduateInternSpec = new ReferenceColumnSpec("Graduate_Intern_ID",
                "ms_lkpgraduateintern", "id", "description",
                "ms_learnerinternship.graduateinternid -> ms_lkpgraduateintern");
        ReferenceColumnSpec financialYearSpec = new ReferenceColumnSpec("Financial_Year_ID",
                "ms_lkpfinancialyear", "id", "description",
                "ms_learnerinternship.financialyearid -> ms_lkpfinancialyear (SHARED - closes the "
                + "universal ZZ_FinYear_ID gap, reusable by future families)");

        ReferenceColumnSpec[] specs = { internshipTypeSpec, internshipStatusSpec, internshipQualTypeSpec,
                internshipDisciplinesSpec, typeOfPlacementSpec, placementStatusSpec, highestEducationLevelSpec,
                yearOfStudySpec, nqfLevelSpec, sicCodeSpec, graduateInternSpec, financialYearSpec };
        for (ReferenceColumnSpec spec : specs) {
            String targetTableName = spec.targetTableName();
            MTable refTable = AddColumnsSupport.findTable(getCtx(), targetTableName, get_TrxName());
            if (refTable == null) {
                refTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), targetTableName,
                        "Reference values for " + spec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                        this::addLog);
                AddColumnsSupport.populateReferenceTable(getCtx(), refTable, spec, get_TrxName(), this::addLog);
                addLog("Created and populated reference table " + targetTableName + ".");
            } else {
                addLog(targetTableName + " already exists - left as-is (not re-populated).");
            }
        }

        int sponsorshipReferenceId = REFERENCE_SPONSORSHIP;
        int socioEconomicStatusReferenceId = REFERENCE_SOCIOECONOMICSTATUS;
        int terminationReasonReferenceId = REFERENCE_TERMINATIONREASON;
        int enrolmentStatusReasonReferenceId = REFERENCE_ENROLMENTSTATUSREASON;
        int yesNoNaReferenceId = AddColumnsSupport.findOrCreateListReferenceFromDescriptions(getCtx(),
                YES_NO_NA_REFERENCE_NAME, "Shared No/Yes/Not Applicable list (ms_lkpyesnonotapplicable)",
                ENTITY_TYPE, get_TrxName(), "ms_lkpyesnonotapplicable", "description", this::addLog);

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "A learner's internship placement record", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "ms_learnerinternship.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, internshipTypeSpec.columnName, DisplayType.TableDir, 10,
                internshipTypeSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, internshipStatusSpec.columnName, DisplayType.TableDir, 10,
                internshipStatusSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, internshipQualTypeSpec.columnName, DisplayType.TableDir,
                10, internshipQualTypeSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, internshipDisciplinesSpec.columnName,
                DisplayType.TableDir, 10, internshipDisciplinesSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, typeOfPlacementSpec.columnName, DisplayType.TableDir, 10,
                typeOfPlacementSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, placementStatusSpec.columnName, DisplayType.TableDir, 10,
                placementStatusSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, highestEducationLevelSpec.columnName, DisplayType.TableDir,
                10, highestEducationLevelSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, yearOfStudySpec.columnName, DisplayType.TableDir, 10,
                yearOfStudySpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, nqfLevelSpec.columnName, DisplayType.TableDir, 10,
                nqfLevelSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, sicCodeSpec.columnName, DisplayType.TableDir, 10,
                sicCodeSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, graduateInternSpec.columnName, DisplayType.TableDir, 10,
                graduateInternSpec.description, ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, financialYearSpec.columnName, DisplayType.TableDir, 10,
                financialYearSpec.description, ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerinternship.employerid, resolved via "
                + "ms_organisation.sdlnumber = c_bpartner.zz_sdl_no", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Alternate_Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerinternship.alternateemployerid, same crosswalk",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Placement_Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerinternship.placementemployerid, same crosswalk",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZSponsorship", DisplayType.List,
                sponsorshipReferenceId, 4000, "ms_learnerinternship.sponsorshipid -> ms_lkpsponsorship "
                + "(shared List reference, first built for LearnerAET)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZSocioEconomicStatus", DisplayType.List,
                socioEconomicStatusReferenceId, 4000, "ms_learnerinternship.socioeconomicstatusid -> "
                + "ms_lkpsocioeconomicstatus (shared List reference)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZTerminationReason", DisplayType.List,
                terminationReasonReferenceId, 4000, "ms_learnerinternship.terminationreasonid -> "
                + "ms_lkpterminationreason (shared List reference)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZEnrolmentStatusReason", DisplayType.List,
                enrolmentStatusReasonReferenceId, 4000, "ms_learnerinternship.enrolmentstatusreasonid -> "
                + "ms_lkpenrolmentstatusreason (shared List reference)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, yesNoNaReferenceId,
                1, "ms_learnerinternship.levyyesnoid -> ms_lkpyesnonotapplicable", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Registered_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerinternship.registeredby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Terminated_Captured_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerinternship.terminatedcapturedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZInternshipStartDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.internshipstartdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZInternshipEndDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.internshipenddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZExtensionDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.extensiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZTerminationDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.terminationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateTerminationCaptured", DisplayType.DateTime, 7,
                "ms_learnerinternship.dateterminationcaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateExtensionCaptured", DisplayType.DateTime, 7,
                "ms_learnerinternship.dateextensioncaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegistrationDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.registrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCompletionDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.completiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZMostRecentRegistrationDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.mostrecentregistrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZActualTerminatedDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.actualterminateddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQualificationAchievementDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.qualificationachievementdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmploymentStartDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.employmentstartdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmploymentEndDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.employmentenddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZContractDate", DisplayType.DateTime, 7,
                "ms_learnerinternship.contractdate", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsIndustrySpecific", DisplayType.YesNo, 1,
                "ms_learnerinternship.isindustryspecific", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIndustryNonSpecific", DisplayType.YesNo, 1,
                "ms_learnerinternship.industrynonspecific", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZContract", DisplayType.YesNo, 1,
                "ms_learnerinternship.contract", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmpContract", DisplayType.YesNo, 1,
                "ms_learnerinternship.empcontract", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEmpContractCopy", DisplayType.YesNo, 1,
                "ms_learnerinternship.empcontractcopy", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDocumentsReceived", DisplayType.YesNo, 1,
                "ms_learnerinternship.documentsreceived", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZPreviouslyEmployed", DisplayType.YesNo, 1,
                "ms_learnerinternship.prevemployed", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZWPAgreement", DisplayType.YesNo, 1,
                "ms_learnerinternship.wpagreement", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ContractNumber", DisplayType.String, 4000,
                "ms_learnerinternship.contractnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ServiceLevelAgreementNumber", DisplayType.String, 4000,
                "ms_learnerinternship.servicelevelagreementnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "NonSpecificSicCode", DisplayType.String, 4000,
                "ms_learnerinternship.nonspecificsiccode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZResponsibleSeta", DisplayType.String, 4000,
                "ms_learnerinternship.responsibleseta", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssPartner", DisplayType.String, 4000,
                "ms_learnerinternship.asspartner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegSaqa", DisplayType.String, 4000,
                "ms_learnerinternship.regsaqa", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCurRegNumber", DisplayType.String, 4000,
                "ms_learnerinternship.curregnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQCTO", DisplayType.String, 4000,
                "ms_learnerinternship.qcto", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZOccupation", DisplayType.String, 4000,
                "ms_learnerinternship.occupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ExtensionReason", DisplayType.String, 4000,
                "ms_learnerinternship.extensionreason", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "TerminationReasonText", DisplayType.String, 4000,
                "ms_learnerinternship.terminationreason (free text, distinct from the "
                + "ZZTerminationReason List FK above which resolves terminationreasonid)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EmployerContractNumber", DisplayType.String, 4000,
                "ms_learnerinternship.employercontractnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EmployerContactNumber", DisplayType.String, 4000,
                "ms_learnerinternship.employercontactnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "FET", DisplayType.String, 4000,
                "ms_learnerinternship.fet", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "HET", DisplayType.String, 4000,
                "ms_learnerinternship.het", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Physical_Address1", DisplayType.String, 4000,
                "ms_learnerinternship.physicaladdress1", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Physical_Address2", DisplayType.String, 4000,
                "ms_learnerinternship.physicaladdress2", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Physical_Address3", DisplayType.String, 4000,
                "ms_learnerinternship.physicaladdress3", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Physical_Postal_Code", DisplayType.String, 4000,
                "ms_learnerinternship.physicalcode", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "Institution", DisplayType.Integer, 10,
                "ms_learnerinternship.institutionid (unresolved - no lookup table found)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "OFO_Occupation", DisplayType.Integer, 10,
                "ms_learnerinternship.ofooccupationid (unresolved - zzlkpofooccupation.value is not "
                + "unique, code-based join would be ambiguous, see class Javadoc)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Qualification", DisplayType.Integer, 10,
                "ms_learnerinternship.qualificationid (unresolved - range doesn't fit zzqualification)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Training_Provider_Public_Private", DisplayType.Integer,
                10, "ms_learnerinternship.trainingproviderpublicprivateid (unresolved - no lookup table found)",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 12 new reference tables and its business columns.";
    }
}
