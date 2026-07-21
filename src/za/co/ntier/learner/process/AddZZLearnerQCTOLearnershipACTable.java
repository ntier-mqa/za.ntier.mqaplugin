package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Creates the brand new ZZLearnerQCTOLearnershipAC table (assessment centre link for a
 * learner's QCTO learnership enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt"
 * table #4. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable}.
 *
 * <p>AC_ID needs an explicit AD_Reference_Value_ID (not naming-convention Table Direct) since
 * "AC" alone doesn't match "ZZAssessmentCentre" - reused the SAME AD_Reference (1000335, Table)
 * already used by the existing ZZLearnerQCTOArtisans.ZZAC_ID column (confirmed by reading its
 * AD_Column row directly), rather than creating a second, redundant reference to the same
 * table.
 *
 * <p>Levy uses AD_Reference_ID=List(17) + AD_Reference_Value_ID=319 ("_YesNo", value Y/N) -
 * NOT DisplayType.YesNo(20) - matching the exact convention the existing
 * ZZLearnerQCTOArtisans.ZZ*Levy columns already use (confirmed by reading them directly),
 * since this is the same "Levy" concept on a sibling table.
 *
 * <p>Learnership_AC_Type is stored as plain Integer - no MSSQL lookup table found for
 * LearnershipACTypeID under any name searched (see mapping doc Open Item 3). AC_Contact_ID is
 * NOT added - same unresolved zz_formcontact question as the original migration (mapping doc
 * Open Item 2).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOLearnershipACTable")
public class AddZZLearnerQCTOLearnershipACTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnershipAC";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final int REFERENCE_ZZASSESSMENTCENTRE = 1000335;
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
                "Assessment centre link for a learner's QCTO learnership enrolment",
                ENTITY_TYPE, "3", get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerqctolearnershipac.learnerqctolearnershipid -> zzlearnerqctolearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZAC_ID", DisplayType.Table,
                REFERENCE_ZZASSESSMENTCENTRE, 10, "ms_learnerqctolearnershipac.acid -> zzassessmentcentre",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Learnership_AC_Type", DisplayType.Integer, 10,
                "ms_learnerqctolearnershipac.learnershipactypeid (no MSSQL lookup table found - plain Integer)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, REFERENCE_YESNO_LIST, 1,
                "ms_learnerqctolearnershipac.levyyesnoid", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns (AC_Contact_ID not added - see class Javadoc).";
    }
}
