package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 4 (see "Phase 4 - LearnerPostSchoolDetails Family - Mapping.txt"): creates the brand new
 * ZZLearnerPostSchoolDetails table (132,139 source rows) - a learner's post-school qualification
 * record. Very sparse: only learnerid is consistently populated on the source
 * (qualificationid/dateachieved/ofoid are each populated on well under 1% of rows).
 *
 * <p>ZZQualification_ID resolves via a direct join on zzqualification's own "id" recon column
 * (already fully migrated, Phase 3). OFO_Occupation (ofoid) is added as a plain, unresolved
 * Integer - zzlkpofooccupation has no "id" recon column and no staged MSSQL source table exists
 * to build a crosswalk from (same shape of gap as ZZUnitStandard_ID in the LearnerUnitStandard
 * family, but only affecting 153 of 132,139 rows here - not escalated as a decision point given
 * the low impact, see mapping doc).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerPostSchoolDetailsTable")
public class AddZZLearnerPostSchoolDetailsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerPostSchoolDetails";
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
                "A learner's post-school qualification record", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearner_ID", DisplayType.TableDir, 10,
                "ms_learnerpostschooldetails.learnerid -> zzlearner", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZQualification_ID", DisplayType.TableDir, 10,
                "ms_learnerpostschooldetails.qualificationid -> zzqualification", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZDateAchieved", DisplayType.DateTime, 7,
                "ms_learnerpostschooldetails.dateachieved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "OFO_Occupation", DisplayType.Integer, 10,
                "ms_learnerpostschooldetails.ofoid (unresolved - no MSSQL lookup table found "
                + "under any name searched, only affects 0.12% of rows)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
