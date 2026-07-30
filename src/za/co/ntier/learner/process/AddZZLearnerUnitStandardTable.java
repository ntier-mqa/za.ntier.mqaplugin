package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 4 (see "Phase 4 - LearnerUnitStandard Family - Mapping.txt"): creates the brand new
 * ZZLearnerUnitStandard table (1,234,103 source rows) - a learner's enrolment against a single
 * unit standard. The first Phase 4 family tackled (chosen because it's the only one of the 3
 * large families with complete data top-to-bottom).
 *
 * <p>Extremely close shape to ZZLearnerLearnership (Phase 2's parent) - see the mapping doc's
 * "KEY FINDING" section. All reference IDs below are REUSED, nothing new: ZZProgrammeStatus
 * (1000249), ZZSocioEconomicStatus (1000250), ZZCertificateReasonForReprint (1000253),
 * ZZTerminationReason (1000254), ZZEnrolmentStatusReason (1000255) - all List(17).
 * ZZProvider_ID (1000319, Search) and Employer_ID (200175, Search -&gt; C_BPartner) resolve
 * directly on THIS table (unlike LearnerLearnership, which needed separate Provider/Employer
 * child tables) since providerid/employerid are both 100% populated here.
 *
 * <p>ZZUnitStandard_ID is added here as a plain TableDir column, but only resolves for ~52% of
 * rows at data-migration time (see mapping doc Section 1 - no staged MSSQL catalog source covers
 * the other 48% of distinct unitstandardid values used by this table) - accepted gap per user
 * decision 2026-07-21, not fixed here.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerUnitStandardTable")
public class AddZZLearnerUnitStandardTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerUnitStandard";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_ZZPROVIDER = 1000319;
    private static final int REFERENCE_C_BPARTNER_ALL = 200175;
    private static final int REFERENCE_PROGRAMMESTATUS = 1000249;
    private static final int REFERENCE_SOCIOECONOMICSTATUS = 1000250;
    private static final int REFERENCE_CERTIFICATEREASONFORREPRINT = 1000253;
    private static final int REFERENCE_TERMINATIONREASON = 1000254;
    private static final int REFERENCE_ENROLMENTSTATUSREASON = 1000255;

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
                "A learner's enrolment against a single unit standard", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "ms_learnerunitstandard.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZUnitStandard_ID", DisplayType.TableDir, 10,
                "ms_learnerunitstandard.unitstandardid -> ZZUnitStandard (only ~52% resolvable, see "
                + "mapping doc Section 1 - accepted gap)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCommencementDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.commencementdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCompletionDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.completiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZContractNumber", DisplayType.String, 4000,
                "ms_learnerunitstandard.contractnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZProgrammeStatus", DisplayType.List,
                REFERENCE_PROGRAMMESTATUS, 4000, "ms_learnerunitstandard.programmestatusid -> ms_lkpprogrammestatus",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZSocioEconomicStatus", DisplayType.List,
                REFERENCE_SOCIOECONOMICSTATUS, 4000,
                "ms_learnerunitstandard.socioeconomicstatusid -> ms_lkpsocioeconomicstatus", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZProvider_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerunitstandard.providerid -> zzprovider", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "ms_learnerunitstandard.employerid, resolved via "
                + "ms_organisation.sdlnumber = c_bpartner.zz_sdl_no", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsApproved", DisplayType.YesNo, 1,
                "ms_learnerunitstandard.isapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZApprovedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.approvedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateApproved", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.dateapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCertificateNumber", DisplayType.String, 4000,
                "ms_learnerunitstandard.certificatenumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZCertificateCreatedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.certificatecreatedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateCertificateCreated", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.datecertificatecreated", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZCertificateReasonForReprint",
                DisplayType.List, REFERENCE_CERTIFICATEREASONFORREPRINT, 4000,
                "ms_learnerunitstandard.certificatereasonforreprintid -> ms_lkpreasonforreprint", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCertificatePrintingErrorReason", DisplayType.String,
                4000, "ms_learnerunitstandard.certificateprintingerrorreason", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStatusEffectiveDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.statuseffectivedate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStudentNumber", DisplayType.String, 4000,
                "ms_learnerunitstandard.studentnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZExtensionDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.extensiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZExtensionReason", DisplayType.String, 4000,
                "ms_learnerunitstandard.extensionreason", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZTerminationDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.terminationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZTerminationReason", DisplayType.List,
                REFERENCE_TERMINATIONREASON, 4000,
                "ms_learnerunitstandard.terminationreasonid -> ms_lkpterminationreason", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZTerminatedCapturedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.terminatedcapturedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateTerminationCaptured", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.dateterminationcaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZExtensionCapturedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.extensioncapturedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateExtensionCaptured", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.dateextensioncaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegistrationDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.registrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZRegisteredBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.registeredby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZEnrolmentStatusReason", DisplayType.List,
                REFERENCE_ENROLMENTSTATUSREASON, 4000,
                "ms_learnerunitstandard.enrolmentstatusreasonid -> ms_lkpenrolmentstatusreason", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZMostRecentRegistrationDate", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.mostrecentregistrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsEndorsed", DisplayType.YesNo, 1,
                "ms_learnerunitstandard.isendorsed (always 0 in current data)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZEndorsedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "ms_learnerunitstandard.endorsedby (ms_user email match)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateEndorsed", DisplayType.DateTime, 7,
                "ms_learnerunitstandard.dateendorsed", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 34 business columns.";
    }
}
