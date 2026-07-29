package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #4): creates the brand
 * new ZZLearnerLearnershipProvider table (provider parties linked to a learner's
 * LearnerLearnership enrolment).
 *
 * <p>ZZProvider_ID reuses the exact Search+1000319 reference already established for Provider FK
 * columns throughout this project (confirmed by reading the existing
 * ZZLearnerQCTOArtisans.ZZLeadSDProvider_ID column's own AD_Column row).
 *
 * <p>ZZLevy resolves via the SAME NEW 3-state (No/Yes/Not Applicable) List reference as
 * {@link AddZZLearnerLearnershipEmployerTable} - find-or-created (shared, not duplicated).
 *
 * <p>ZZLearnershipProviderType (learnershipprovidertypeid) held back as plain Integer, and
 * Provider_Contact_ID (providercontactid) NOT added - same reasoning as the Employer table's
 * ZZLearnershipEmployerType/Employer_Contact_ID, see that class's Javadoc.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipProviderTable")
public class AddZZLearnerLearnershipProviderTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnershipProvider";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";
    private static final int REFERENCE_ZZPROVIDER = 1000319;

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
                "Provider parties linked to a learner's LearnerLearnership enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerLearnership_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnershipprovider.learnerlearnershipid -> zzlearnerlearnership",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZProvider_ID", DisplayType.Search,
                REFERENCE_ZZPROVIDER, 10, "ms_learnerlearnershipprovider.providerid -> zzprovider",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnershipProviderType", DisplayType.Integer, 10,
                "ms_learnerlearnershipprovider.learnershipprovidertypeid (held back as unresolved - "
                + "see class Javadoc)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "ZZLevy", DisplayType.List, yesNoNaReferenceId, 1,
                "ms_learnerlearnershipprovider.levyyesnoid -> ms_lkpyesnonotapplicable", ENTITY_TYPE, get_TrxName());
        // "id" recon column already created by createNewTableSchema() - do NOT re-register it
        // here (that caused a duplicate-key AD_Column error on a live run, 2026-07-21).

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
