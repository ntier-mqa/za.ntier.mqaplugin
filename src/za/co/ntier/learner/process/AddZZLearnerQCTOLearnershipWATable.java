package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Creates the brand new ZZLearnerQCTOLearnershipWA table (workplace approval link for a
 * learner's QCTO learnership enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt"
 * table #10. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>WA_ID reuses the SAME pre-existing AD_Reference (1000334, Table) the existing
 * ZZLearnerQCTOArtisans.ZZWA_ID column already uses for ZZWorkplaceApproval.
 *
 * <p>Levy uses List(17)+319 ("_YesNo"), same convention as
 * {@link AddZZLearnerQCTOLearnershipACTable}. Learnership_WA_Type is plain Integer - no MSSQL
 * lookup table found (mapping doc Open Item 3). WA_Contact_ID is NOT added - same unresolved
 * zz_formcontact question (mapping doc Open Item 2).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOLearnershipWATable")
public class AddZZLearnerQCTOLearnershipWATable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnershipWA";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final int REFERENCE_ZZWORKPLACEAPPROVAL = 1000334;
    private static final int REFERENCE_YESNO_LIST = 319;

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
                "Workplace approval link for a learner's QCTO learnership enrolment",
                ENTITY_TYPE, "3", get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershipwa.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "WA_ID", DisplayType.Table,
                REFERENCE_ZZWORKPLACEAPPROVAL, 10, "ms_learnerqctolearnershipwa.waid -> zzworkplaceapproval",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Learnership_WA_Type", DisplayType.Integer, 10,
                "ms_learnerqctolearnershipwa.learnershipwatypeid (no MSSQL lookup table found - plain Integer)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, REFERENCE_YESNO_LIST, 1,
                "ms_learnerqctolearnershipwa.levyyesnoid", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns (WA_Contact_ID not added - see class Javadoc).";
    }
}
