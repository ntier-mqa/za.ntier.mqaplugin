package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #3): creates the brand
 * new ZZLearnerLearnershipEmployer table (employer parties linked to a learner's
 * LearnerLearnership enrolment).
 *
 * <p>Employer_ID reuses the exact Search+200175 ("C_BPartner (all)") reference already
 * established for ZZLearnerQCTOArtisans/ZZLearnerQCTOLearnership's own Employer_ID columns -
 * "Employer" doesn't match the C_BPartner table name so Table Direct naming can't resolve it.
 *
 * <p>ZZLevy resolves via a NEW 3-state (No/Yes/Not Applicable) List reference, find-or-created
 * by {@link AddZZLearnerLearnershipColumns} (shared with this table's own parent process run and
 * with {@link AddZZLearnerLearnershipProviderTable}'s own Levy column) - confirmed via
 * ms_lkpyesnonotapplicable content, NOT the project's usual binary Yes/No List+319, since this
 * source data genuinely uses a 3rd state.
 *
 * <p>ZZLearnershipEmployerType (learnershipemployertypeid) held back as plain Integer per user
 * decision 2026-07-21 - the "Is Lead Employer" Yes/No hypothesis was investigated but not
 * adopted; see the mapping doc's Section 3/Question 3.
 *
 * <p>Employer_Contact_ID (employercontactid) NOT added - same unresolved "zz_formcontact
 * question" as Phase 1's AC/SDProvider/WA contact columns (re-investigated for Phase 2: the
 * existing ZZ_FormContact table is keyed to a completely different business object -
 * zz_application_form - and has no relationship to this source column).
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipEmployerTable")
public class AddZZLearnerLearnershipEmployerTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnershipEmployer";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
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

        int yesNoNaReferenceId = AddColumnsSupport.findOrCreateListReferenceFromDescriptions(getCtx(),
                YES_NO_NA_REFERENCE_NAME, "Shared No/Yes/Not Applicable list (ms_lkpyesnonotapplicable)",
                ENTITY_TYPE, get_TrxName(), "ms_lkpyesnonotapplicable", "description", this::addLog);

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Employer parties linked to a learner's LearnerLearnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershipemployer.learnerlearnershipid -> zzlearnerlearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "Employer_ID", DisplayType.Search, 200175, 10,
                "ms_learnerlearnershipemployer.employerid, resolved via ms_organisation.sdlnumber = "
                + "c_bpartner.zz_sdl_no (same as elsewhere in this project)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnershipEmployerType", DisplayType.Integer, 10,
                "ms_learnerlearnershipemployer.learnershipemployertypeid (held back as unresolved - "
                + "see class Javadoc)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, yesNoNaReferenceId, 1,
                "ms_learnerlearnershipemployer.levyyesnoid -> ms_lkpyesnonotapplicable", ENTITY_TYPE, get_TrxName());
        // "id" recon column already created by createNewTableSchema() - do NOT re-register it
        // here (that caused a duplicate-key AD_Column error on a live run, 2026-07-21).

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
