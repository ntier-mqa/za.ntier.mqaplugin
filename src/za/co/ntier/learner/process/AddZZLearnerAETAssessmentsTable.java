package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 4 (see "Phase 4 - LearnerAET Family - Mapping.txt" Child 1): creates the brand new
 * ZZLearnerAETAssessments table (351,197 source rows) - unit standard assessment records for a
 * learner's AET enrolment.
 *
 * <p>ZZLearningArea_ID resolves via Table Direct naming convention alone (matches the existing
 * ZZLearningArea table, built Phase 3 for exactly this - learningareaid's 1-94 range fits inside
 * ZZLearningArea's 1-132).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerAETAssessmentsTable")
public class AddZZLearnerAETAssessmentsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerAETAssessments";
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

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Unit standard assessment records for a learner's AET enrolment", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerAET_ID", DisplayType.TableDir, 10,
                "learneraetassessments.learneraetid -> zzlearneraet", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearningArea_ID", DisplayType.TableDir, 10,
                "learneraetassessments.learningareaid -> ZZLearningArea", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsRPL", DisplayType.YesNo, 1,
                "learneraetassessments.isrpl", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZAssessmentDate", DisplayType.DateTime, 7,
                "learneraetassessments.assessmentdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZIsCompetent", DisplayType.YesNo, 1,
                "learneraetassessments.iscompetent", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZCertificateDate", DisplayType.DateTime, 7,
                "learneraetassessments.certificatedate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Assessed_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraetassessments.createdby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Updated_By", DisplayType.Table,
                REFERENCE_AD_USER, 10, "learneraetassessments.updatedby (ms_user email match)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
