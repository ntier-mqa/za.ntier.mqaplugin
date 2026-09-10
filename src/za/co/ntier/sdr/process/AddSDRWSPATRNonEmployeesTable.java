package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new
 * SDR_WSPATRNonEmployees child table (19,070 source rows) - a Done/Planned pair of non-employee
 * training records, mirroring WSPATRContractors' Trained/Planned shape but for non-employees.
 *
 * <p>The LearningProgrammeType/LearningProgramme lookups are resolved once each and reused between the
 * "Done" and "Planned" columns (same target tables).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRNonEmployeesTable")
public class AddSDRWSPATRNonEmployeesTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRNonEmployees";
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
                "A non-employee training record (Done/Planned) on a WSPATR (mssdr_wspatrnonemployees)",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrnonemployees.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());

        int learningProgrammeTypeRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(),
                "SDR_LearningProgrammeType", ENTITY_TYPE, get_TrxName(), this::addLog);
        int learningProgrammeRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_LearningProgramme",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        int nonEmpStatusRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPNonEmployeeStatus",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        int targetBenRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPTargetBeneficiary",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        // -- Done block --
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_LearningProgrammeTypeDone_ID",
                DisplayType.Table, learningProgrammeTypeRefId, 10,
                "mssdr_wspatrnonemployees.learningprogrammetypedoneid -> SDR_LearningProgrammeType", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_LearningProgrammeDone_ID", DisplayType.Table,
                learningProgrammeRefId, 10,
                "mssdr_wspatrnonemployees.learningprogrammedoneid -> SDR_LearningProgramme. CONFIRMED 73.8% "
                + "(14,074/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LPOtherDone", DisplayType.String, 250,
                "mssdr_wspatrnonemployees.lpotherdone (free text, populated when the programme isn't in "
                + "the catalog)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_NonEmpStatusDone_ID", DisplayType.Table,
                nonEmpStatusRefId, 10,
                "mssdr_wspatrnonemployees.nonempstatusdoneid -> SDR_WSPNonEmployeeStatus (2 rows). "
                + "CONFIRMED 87.6% (16,706/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_TargetBenDone_ID", DisplayType.Table,
                targetBenRefId, 10,
                "mssdr_wspatrnonemployees.targetbendoneid -> SDR_WSPTargetBeneficiary (5 rows). CONFIRMED "
                + "87.7% (16,717/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Male", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.male (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Female", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.female (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_African", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.african (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Coloured", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.coloured (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Indian", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.indian (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_White", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.white (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DisabledDone", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.disableddone (headcount)", ENTITY_TYPE, get_TrxName());

        // -- Planned block --
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_LearningProgrammeTypePlanned_ID",
                DisplayType.Table, learningProgrammeTypeRefId, 10,
                "mssdr_wspatrnonemployees.learningprogrammetypeplannedid -> SDR_LearningProgrammeType",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_LearningProgrammePlanned_ID",
                DisplayType.Table, learningProgrammeRefId, 10,
                "mssdr_wspatrnonemployees.learningprogrammeplannedid -> SDR_LearningProgramme. CONFIRMED "
                + "64.2% (12,249/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LPOtherPlanned", DisplayType.String, 250,
                "mssdr_wspatrnonemployees.lpotherplanned (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_NonEmpStatusPlanned_ID", DisplayType.Table,
                nonEmpStatusRefId, 10,
                "mssdr_wspatrnonemployees.nonempstatusplannedid -> SDR_WSPNonEmployeeStatus. CONFIRMED "
                + "74.1% (14,131/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_TargetBenPlanned_ID", DisplayType.Table,
                targetBenRefId, 10,
                "mssdr_wspatrnonemployees.targetbenplannedid -> SDR_WSPTargetBeneficiary. CONFIRMED 74.0% "
                + "(14,110/19,070)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TotalPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.totalplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DisabledPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrnonemployees.disabledplanned (headcount)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 21 business columns.";
    }
}
