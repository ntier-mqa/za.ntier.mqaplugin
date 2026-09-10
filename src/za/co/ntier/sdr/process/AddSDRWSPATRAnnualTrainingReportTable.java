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
 * SDR_WSPATRAnnualTrainingReport child table - the LARGEST table in this entire migration
 * (7,637,240 source rows).
 *
 * <p>SDR_TrainingStatus_ID, SDR_TrainingStatusReason_ID, SDR_YearEnrolled_ID and SDR_YearCompleted_ID
 * all target tables whose names don't match the column name ("SDR_TrainingStatus" != the target
 * "SDR_WSPAchievementStatus", etc.) - each needs an explicit AD_Reference/AD_Ref_Table override. The
 * two Year-shaped columns share the same target (SDR_Year), resolved once and reused.
 *
 * <p>Schema only - no data population. Given the row count, the eventual Migrate*-style data
 * population process for this table will need particular care around batching/performance.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRAnnualTrainingReportTable")
public class AddSDRWSPATRAnnualTrainingReportTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRAnnualTrainingReport";
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
                "An annual training report line for a WSPATR (mssdr_wspatrannualtrainingreport) - the "
                + "largest table in this migration by row count", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrannualtrainingreport.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrannualtrainingreport.wspatrimportid - UNMAPPED, opaque (same as every other "
                + "*ImportID column in this migration, no Import table exists to test against)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EmployeeNo", DisplayType.String, 50,
                "mssdr_wspatrannualtrainingreport.employeeno", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EmployeeName", DisplayType.String, 250,
                "mssdr_wspatrannualtrainingreport.employeename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgrammeType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrannualtrainingreport.learningprogrammetypeid -> SDR_LearningProgrammeType. "
                + "CONFIRMED 100%", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgramme_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrannualtrainingreport.learningprogrammeid -> SDR_LearningProgramme", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Qualification", DisplayType.String, 250,
                "mssdr_wspatrannualtrainingreport.qualification (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TrainingCost", DisplayType.Amount, 22,
                "mssdr_wspatrannualtrainingreport.trainingcost", ENTITY_TYPE, get_TrxName());

        int trainingStatusRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPAchievementStatus",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_TrainingStatus_ID", DisplayType.Table,
                trainingStatusRefId, 10,
                "mssdr_wspatrannualtrainingreport.trainingstatusid -> SDR_WSPAchievementStatus (4 rows: "
                + "Achieved/Drop_out/In Progress/Not_Achieved). CONFIRMED 100% match (7,637,240/7,637,240)",
                ENTITY_TYPE, get_TrxName());

        int trainingStatusReasonRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPDropOut",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_TrainingStatusReason_ID", DisplayType.Table,
                trainingStatusReasonRefId, 10,
                "mssdr_wspatrannualtrainingreport.trainingstatusreasonid -> SDR_WSPDropOut (8 rows). "
                + "CONFIRMED 100% of populated non-zero values (11,369/11,369) - most rows (7,625,871) are "
                + "the 0 sentinel, since a reason is only captured when training didn't simply succeed",
                ENTITY_TYPE, get_TrxName());

        int yearRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Year", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_YearEnrolled_ID", DisplayType.Table,
                yearRefId, 10,
                "mssdr_wspatrannualtrainingreport.yearenrolledid -> SDR_Year (shared). CONFIRMED 100% match "
                + "(7,637,240/7,637,240)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_YearCompleted_ID", DisplayType.Table,
                yearRefId, 10,
                "mssdr_wspatrannualtrainingreport.yearcompletedid -> SDR_Year. CONFIRMED 93.4% match "
                + "(7,133,272/7,637,240) - not every enrolment has completed yet", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 13 business columns.";
    }
}
