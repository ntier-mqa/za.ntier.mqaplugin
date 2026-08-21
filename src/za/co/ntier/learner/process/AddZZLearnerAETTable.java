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
 * Phase 4 (see "Phase 4 - LearnerAET Family - Mapping.txt"): creates the brand new ZZLearnerAET
 * table (127,422 source rows) - a learner's AET (Adult Education and Training) programme
 * enrolment. Close sibling of ZZLearnerLearnership/ZZLearnerUnitStandard - most crosswalks
 * REUSED (ZZSponsorship/ZZSocioEconomicStatus/ZZTerminationReason/ZZEnrolmentStatusReason List
 * references, ZZProvider_ID/Employer_ID Search references, ms_user email match, ZZGrantType_ID
 * via buildIdCrosswalk).
 *
 * <p>ZZAET_ID resolves via Table Direct naming convention alone (matches the existing
 * ZZAETProgramme table, built Phase 3 - aetid's 1-21 range matches the "aet" source table's own
 * id range exactly).
 *
 * <p>ZZStatus/ZZAETTimePeriod are NEW reference tables (AET_Status/AET_Time_Period), built via
 * the standard createReferenceTableSchema/populateReferenceTable engine, from lkpAETStatus/
 * lkpAETTimePeriod - both only just staged (see the mapping doc's "STAGING PROJECT" section: a
 * full audit found 197 of 245 lkp* tables in MSSQL had never been staged; a comprehensive
 * pgloader run fixed this, incidentally resolving several other gaps already flagged/worked
 * around in earlier phases too, not yet retrofitted).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerAETTable")
public class AddZZLearnerAETTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerAET";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_ZZPROVIDER = 1000319;
    private static final int REFERENCE_C_BPARTNER_ALL = 200175;
    private static final int REFERENCE_SPONSORSHIP = 1000251;
    private static final int REFERENCE_SOCIOECONOMICSTATUS = 1000250;
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

        MTable statusTable = createOrReuseReferenceTable(new ReferenceColumnSpec("AET_Status_ID",
                "ms_lkpaetstatus", "id", "description", "learneraet.statusid -> ms_lkpaetstatus"));
        MTable timePeriodTable = createOrReuseReferenceTable(new ReferenceColumnSpec("AET_Time_Period_ID",
                "ms_lkpaettimeperiod", "id", "description", "learneraet.aettimeperiodid -> ms_lkpaettimeperiod"));

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "A learner's AET (Adult Education and Training) programme enrolment", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "learneraet.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAET_ID", DisplayType.TableDir, 10,
                "learneraet.aetid -> ZZAETProgramme", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStartDate", DisplayType.DateTime, 7,
                "learneraet.startdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZEndDate", DisplayType.DateTime, 7,
                "learneraet.enddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStatus_ID", DisplayType.TableDir, 10,
                "learneraet.statusid -> AET_Status", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZSponsorship", DisplayType.List,
                REFERENCE_SPONSORSHIP, 4000, "learneraet.sponsorshipid -> ms_lkpsponsorship", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZStatusEffectiveDate", DisplayType.DateTime, 7,
                "learneraet.statuseffectivedate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCompletionDate", DisplayType.DateTime, 7,
                "learneraet.completiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZProvider_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "learneraet.providerid -> zzprovider", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Employer_ID", DisplayType.Search,
                REFERENCE_C_BPARTNER_ALL, 10, "learneraet.employerid, resolved via ms_organisation.sdlnumber "
                + "= c_bpartner.zz_sdl_no", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZSocioEconomicStatus", DisplayType.List,
                REFERENCE_SOCIOECONOMICSTATUS, 4000, "learneraet.socioeconomicstatusid -> "
                + "ms_lkpsocioeconomicstatus", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZExtensionDate", DisplayType.DateTime, 7,
                "learneraet.extensiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZExtensionReason", DisplayType.String, 4000,
                "learneraet.extensionreason", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZTerminationDate", DisplayType.DateTime, 7,
                "learneraet.terminationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZTerminationReason", DisplayType.List,
                REFERENCE_TERMINATIONREASON, 4000, "learneraet.terminationreasonid -> ms_lkpterminationreason",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateExtensionCaptured", DisplayType.DateTime, 7,
                "learneraet.dateextensioncaptured", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegistrationDate", DisplayType.DateTime, 7,
                "learneraet.registrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZRegisteredBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraet.registeredby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZEnrolmentStatusReason", DisplayType.List,
                REFERENCE_ENROLMENTSTATUSREASON, 4000, "learneraet.enrolmentstatusreasonid -> "
                + "ms_lkpenrolmentstatusreason", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZMostRecentRegistrationDate", DisplayType.DateTime, 7,
                "learneraet.mostrecentregistrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZActualTerminatedDate", DisplayType.DateTime, 7,
                "learneraet.actualterminateddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZRegistrationNumber", DisplayType.String, 4000,
                "learneraet.registrationnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZGrantType_ID", DisplayType.TableDir, 10,
                "learneraet.granttypeid -> zzgranttype (currently empty, resolves once "
                + "MigrateMsGrantTypeToZZGrantType runs)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZSetMISReg", DisplayType.YesNo, 1,
                "learneraet.setmisreg (SET MIS Registration - reported to SETA MIS system)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZSetMISComp", DisplayType.YesNo, 1,
                "learneraet.setmiscomp (SET MIS Completion)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZTerminatedCapturedBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraet.terminatedcapturedby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZElementaryCompletionDate", DisplayType.DateTime, 7,
                "learneraet.elementarycompletiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZDeclaredCompetentBy", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraet.declaredcompetentby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAETTimePeriod_ID", DisplayType.TableDir, 10,
                "learneraet.aettimeperiodid -> AET_Time_Period", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        addLog("Reference tables used: " + statusTable.getTableName() + ", " + timePeriodTable.getTableName());
        return TABLE_NAME + " created with 27 business columns.";
    }

    private MTable createOrReuseReferenceTable(ReferenceColumnSpec spec) throws Exception {
        String targetTableName = spec.targetTableName();
        MTable targetTable = AddColumnsSupport.findTable(getCtx(), targetTableName, get_TrxName());
        if (targetTable == null) {
            targetTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), targetTableName,
                    "Reference values for " + spec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), targetTable, spec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + targetTableName + ".");
        } else {
            addLog(targetTableName + " already exists - left as-is (not re-populated).");
        }
        return targetTable;
    }
}
