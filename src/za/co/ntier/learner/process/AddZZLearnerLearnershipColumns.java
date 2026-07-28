package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ColumnSpec;

/**
 * Phase 2 (see "Additional Tables - Migration Plan.txt" and "Phase 2 - LearnerLearnership
 * Family - Mapping.txt"): adds the missing columns to the already-existing ZZLearnerLearnership
 * table. Unlike ZZProvider/ZZWorkplaceApproval/ZZAssessmentCentre, this table's EXISTING 51
 * columns already have correct AD_Column metadata (built before this project touched it,
 * including working List(17) crosswalks for ZZProgrammeStatus/ZZSocioEconomicStatus/etc reusing
 * reference IDs 1000249-1000257) - this process only adds the ~20 genuinely missing columns
 * identified by comparing all 92 source ms_learnerlearnership columns against the target.
 *
 * <p>Per user decisions 2026-07-21 (see the mapping doc's "OPEN QUESTIONS" section):
 * <ul>
 *   <li>ZZIsRPL/ZZRPL and ZZIsEndorsed/ZZEndorsed (both already existed, duplicate-looking) -
 *       both members of each pair get populated with the same value at data-migration time;
 *       nothing to add here, this process doesn't touch either pair.</li>
 *   <li>ZZIsTermsEmployment resolves via a NEW 3-state List reference (No/Yes/Not Applicable,
 *       sourced from ms_lkpyesnonotapplicable) rather than collapsing to the existing binary
 *       Yes/No List+319 - shared with the Levy columns on the Employer/Provider children (see
 *       AddZZLearnerLearnershipEmployerTable/AddZZLearnerLearnershipProviderTable).</li>
 *   <li>learnershipprovidertypeid/learnershipemployertypeid's "Lead Provider/Employer" Yes/No
 *       hypothesis was NOT adopted - held back as plain Integer on the children instead
 *       (nothing on the parent).</li>
 *   <li>The ~15 further ambiguous parent columns: added per this class's own best-guess
 *       disposition as documented in the mapping doc - bi_registrationdate/bi_approvaldate and
 *       responsibleseta/curregnumber SKIPPED (look like redundant denormalised duplicates);
 *       setalearnershiptypeid/agentid/asspartner/regsaqa DEFERRED (genuinely unclear, no lookup
 *       table found); everything else added below.</li>
 * </ul>
 *
 * <p><b>CORRECTION (2026-07-21, same day, before this was ever run):</b> the mapping doc
 * originally recommended ZZWPAgreement/ZZEmpContract/ZZEmpContractCopy as unresolved plain
 * Integer columns ("no MSSQL lookup table found"). Further research while writing the sibling
 * Migrate* class turned up MigrationSupport.yesNoIdToFlag() - an existing, already-used helper
 * for exactly this "*YesNoId" (1=No, 2=Yes) column shape, confirmed via AD_Column to be exactly
 * how ZZLearnerQCTOLearnership's own WP_Agreement/Emp_Contract/Emp_Contract_Copy/
 * Is_Terms_Employment/Previous_Employed/Is_Approved columns are all typed (plain YesNo(20), not
 * List+319). Corrected here before ever running against the DB: all 3 now YesNo(20), matching
 * that sibling precedent exactly, populated via yesNoIdToFlag() in the Migrate* process (source
 * data has a 3rd value, 0, on this table unlike the sibling - yesNoIdToFlag() already tolerates
 * that by design, leaving it unset rather than guessing).
 *
 * <p>ZZGrantType_ID resolves via Table Direct naming convention alone (matches the existing
 * ZZGrantType table exactly, same column name already used on ZZLearnerQctoLearnership/
 * ZZLearnerQctoSkillsProgramme) - no new reference needed. NOTE: ZZGrantType itself is
 * currently EMPTY (0 rows) even though its source ms_granttype has 528 rows -
 * MigrateMsGrantTypeToZZGrantType must be run before the LearnerLearnership Migrate* process for
 * this column to actually resolve any values; the column is still added now regardless.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerLearnershipColumns")
public class AddZZLearnerLearnershipColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerLearnership";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        specs.add(new ColumnSpec("id", DisplayType.Integer, 10,
                "recon column - source ms_learnerlearnership row id"));
        specs.add(new ColumnSpec("ZZCompletionDate", DisplayType.DateTime, 7,
                "ms_learnerlearnership.completiondate"));
        specs.add(new ColumnSpec("ZZExtensionDate", DisplayType.DateTime, 7,
                "ms_learnerlearnership.extensiondate"));
        specs.add(new ColumnSpec("ZZExtensionReason", DisplayType.String, 4000,
                "ms_learnerlearnership.extensionreason"));
        specs.add(new ColumnSpec("ZZDurationLearnerEmployed", DisplayType.String, 4000,
                "ms_learnerlearnership.durationlearneremployed"));
        specs.add(new ColumnSpec("ZZTermsEmployment", DisplayType.String, 4000,
                "ms_learnerlearnership.termsemployment"));
        specs.add(new ColumnSpec("ZZOccupation", DisplayType.String, 4000,
                "ms_learnerlearnership.occupation"));
        specs.add(new ColumnSpec("ZZNonFundedReason", DisplayType.String, 4000,
                "ms_learnerlearnership.nonfundedreason"));
        specs.add(new ColumnSpec("ZZQCTO", DisplayType.String, 4000,
                "ms_learnerlearnership.qcto (source data is messy free text - no crosswalk attempted)"));
        specs.add(new ColumnSpec("ZZIsApproved", DisplayType.YesNo, 1,
                "ms_learnerlearnership.isapproved"));
        specs.add(new ColumnSpec("ZZDateApproved", DisplayType.DateTime, 7,
                "ms_learnerlearnership.dateapproved"));
        specs.add(new ColumnSpec("ZZApprovalDate", DisplayType.DateTime, 7,
                "ms_learnerlearnership.approvaldate (separate from dateapproved - source has two "
                + "distinct approval-tracking column pairs, both kept rather than guessing which is authoritative)"));
        // CORRECTED after further research (see class Javadoc "CORRECTION" note): these are
        // "*YesNoId"-shaped columns (1=No, 2=Yes) exactly like Employer/Provider's levyyesnoid,
        // NOT unresolved raw integers - MigrationSupport.yesNoIdToFlag() already exists and is
        // already used for this exact column shape on the sibling QCTOLearnership table.
        specs.add(new ColumnSpec("ZZWPAgreement", DisplayType.YesNo, 1,
                "ms_learnerlearnership.wpagreement -> MigrationSupport.yesNoIdToFlag()"));
        specs.add(new ColumnSpec("ZZEmpContract", DisplayType.YesNo, 1,
                "ms_learnerlearnership.empcontract -> MigrationSupport.yesNoIdToFlag()"));
        specs.add(new ColumnSpec("ZZEmpContractCopy", DisplayType.YesNo, 1,
                "ms_learnerlearnership.empcontractcopy -> MigrationSupport.yesNoIdToFlag()"));
        return specs;
    }

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        MTable table = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (table == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        for (ColumnSpec spec : PLAIN_COLUMNS) {
            AddColumnsSupport.addColumn(getCtx(), table, spec.columnName, spec.referenceId, spec.fieldLength,
                    spec.description, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        // Table(18)+AD_User - matches the majority *_By column convention already on this table
        // (ZZEndorsedBy/ZZExtensionCapturedBy/ZZRegisteredBy/ZZTerminatedCapturedBy).
        AddColumnsSupport.addColumn(getCtx(), table, "ZZApprovedBy", DisplayType.Table, REFERENCE_AD_USER, 10,
                "ms_learnerlearnership.approvedby (ms_user email match)", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "ZZApprovalBy", DisplayType.Table, REFERENCE_AD_USER, 10,
                "ms_learnerlearnership.approvalby (separate from approvedby - see ZZApprovalDate note above)",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        // Table Direct naming convention alone resolves this against the existing ZZGrantType
        // table - see class Javadoc for the "currently empty" caveat.
        AddColumnsSupport.addColumn(getCtx(), table, "ZZGrantType_ID", DisplayType.TableDir, 10,
                "ms_learnerlearnership.granttypeid -> ZZGrantType", ENTITY_TYPE, get_TrxName(), this::addLog);

        // Shared 3-state (No/Yes/Not Applicable) List reference - find-or-create so the
        // Employer/Provider children's Levy columns (built separately) can reuse the same one.
        int yesNoNaReferenceId = AddColumnsSupport.findOrCreateListReferenceFromDescriptions(getCtx(),
                YES_NO_NA_REFERENCE_NAME, "Shared No/Yes/Not Applicable list (ms_lkpyesnonotapplicable)",
                ENTITY_TYPE, get_TrxName(), "ms_lkpyesnonotapplicable", "description", this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "ZZIsTermsEmployment", DisplayType.List, yesNoNaReferenceId, 1,
                "ms_learnerlearnership.istermsemployment -> ms_lkpyesnonotapplicable (same 1/2-only value "
                + "pattern as levyyesnoid)", ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": " + (PLAIN_COLUMNS.size() + 3) + " new column(s) added.";
    }
}
