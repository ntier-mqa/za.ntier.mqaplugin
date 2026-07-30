package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 3 (see "Additional Tables - Migration Plan.txt"): creates the brand new ZZLearningArea
 * reference/catalog table (131 rows expected, from ms_learningarea) - a small Value/Name lookup
 * used elsewhere as a foreign key (e.g. Phase 4's LearnerAETAssessments.LearningAreaID), same
 * "Provider_Type-style" shape the plan doc calls for.
 *
 * <p>Built via {@link AddColumnsSupport#createReferenceTableSchema} (Value/Name/id/standard
 * columns, physically created immediately) then 3 extra columns
 * (Registration_Start_Date/Registration_End_Date/Credits) added via
 * {@link AddColumnsSupport#addColumn} - same 2-step pattern Phase 1 used for reference-shaped
 * tables that need a few more columns than the bare Value/Name shape.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearningAreaTable")
public class AddZZLearningAreaTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearningArea";
    private static final String ENTITY_TYPE = "MQA Learner";
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

        MTable table = AddColumnsSupport.createReferenceTableSchema(getCtx(), TABLE_NAME,
                "Learning area reference (ms_learningarea): Value=learningareacode, "
                + "Name=learningareatitle", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(), this::addLog);

        AddColumnsSupport.addColumn(getCtx(), table, "Registration_Start_Date", DisplayType.DateTime, 7,
                "ms_learningarea.registrationstartdate", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Registration_End_Date", DisplayType.DateTime, 7,
                "ms_learningarea.registrationenddate", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Credits", DisplayType.Integer, 10,
                "ms_learningarea.credits", ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with Value/Name/id plus 3 extra columns.";
    }
}
