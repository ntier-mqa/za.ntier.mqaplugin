package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ColumnSpec;

/**
 * Adds the missing columns to the already-existing ZZLearnerQCTOArtisans table as REAL
 * Application Dictionary AD_Column records - same drop-and-recreate/AddColumnsSupport pattern
 * as {@link AddZZProviderColumns}/{@link AddZZWorkplaceApprovalColumns}/
 * {@link AddZZAssessmentCentreColumns}, see "ZZLearnerQCTOArtisans - New Columns to Add.txt" in
 * the Learners Data Migration runbook for the full column-by-column reasoning.
 *
 * <p>UNLIKE the previous three tables, ZZLearnerQCTOArtisans is NOT a bare table - it already
 * has ~60 custom columns (built long before this migration project). Of the ~25 columns the
 * mapping doc marked "Unmapped", most turned out to be either genuinely dead data (0 rows
 * populated - excluded here, see below) or redundant text mirrors of an already-resolved FK
 * (also excluded - no column added, see the mapping doc). The remaining ones, confirmed by
 * querying the live staged data (2026-07-16) rather than guessing from the column name alone:
 * <ul>
 *   <li>5 columns (previousemployed, wpagreement, istermsemployment, empcontract,
 *       empcontractcopy) are `int` but only ever contain 1/2/null across all 1230 staged
 *       rows - the same lkpYesNo encoding (1=No, 2=Yes) confirmed elsewhere in this project.
 *       Added as Yes/No.</li>
 *   <li>isapproved, nambconfirmation, and nambconfirmationdate are structurally the same kind
 *       of flag/date (smallint/date) but are almost entirely empty in the current data (0/1230,
 *       1/1230, 1/1230 respectively) - added anyway, same call as
 *       Workplace_Approval_Code/Assessment_Centre_Code.</li>
 *   <li>learneremployed, termsemployment, responsibleseta, asspartner, regsaqa, curregnumber,
 *       qcto, occupation are free text - added as plain String. "occupation" contains
 *       OFO-code-looking values (e.g. "651302") but does NOT match zzlkpofooccupation.Value's
 *       format (e.g. "2021-226102") when checked directly - so it's kept as plain text rather
 *       than a guessed Table Direct reference, consistent with the Qualification tab's own
 *       still-unresolved ofooccupationid crosswalk.</li>
 *   <li>approvedby and nambconfirmationuser are actor columns (ad_user, by MSSQL row id) - same
 *       family as the already-existing zzenrolledby/zzregisteredby/zzcertificatecreatedby
 *       columns on this same table. Added the same way those were built: AD_Reference_ID=
 *       Search (30), AD_Reference_Value_ID=SystemIDs.REFERENCE_AD_USER (110) - confirmed by
 *       reading those columns' own AD_Column rows rather than guessing. The actual ms_user
 *       email-match resolution happens later, in the data-migration process, same as the
 *       existing actor columns.</li>
 *   <li>employerid is CONFIRMED (2026-07-16, by joining the staged data) to match
 *       ms_organisation.id 1085/1085 times - the exact same Organisation concept used for
 *       organisationid elsewhere in this project. Since this table has no existing
 *       organisationid/C_BPartner_ID column, and "Employer_ID" (stripped of "_ID") does not
 *       match the C_BPartner table name, Table Direct's naming convention can't resolve it -
 *       so this uses an explicit AD_Reference_Value_ID pointing at the "C_BPartner (all)"
 *       reference (200175, unfiltered - confirmed by reading its AD_Ref_Table row) instead of
 *       reusing the plain "C_BPartner_ID" name (which would silently share the exact same
 *       M_Element, and therefore the same generic label, as the unrelated C_BPartner_ID
 *       columns on ZZProvider/ZZWorkplaceApproval/ZZAssessmentCentre). Actual resolution
 *       (Organisation.SDLNumber -&gt; c_bpartner.zz_sdl_no) happens later, same as
 *       organisationid elsewhere.</li>
 * </ul>
 *
 * <p><b>Deliberately NOT added</b> (see mapping doc for the full reasoning):
 * <ul>
 *   <li>extractrecordid - 0 of 1230 rows populated, same "dead historic artifact" category as
 *       migrationrecordid/accountnumber (not this table's own migrationrecordid - it doesn't
 *       have one - but the same kind of column).</li>
 *   <li>previouslearnershipcode / previouslearnershiptitle - redundant free-text mirrors of the
 *       already-resolved previouslearnership -&gt; zzpreviouslearnership_id FK. Real data, but a
 *       deliberate decision not to duplicate it, not a gap.</li>
 *   <li>terminationreasonid - NOT actually unmapped: its target, zzterminationreason, ALREADY
 *       exists as a List column (AD_Element description literally says "Map to
 *       TerminationReasonID on old database"), and its 8 List values match lkpTerminationReason
 *       verbatim (confirmed 2026-07-16). The mapping doc previously had this backwards (had
 *       terminationreason, the free-text column, feeding zzterminationreason) - corrected in
 *       the doc; zzterminationreasontext (also already existing, previously undocumented) is
 *       the free-text column's real target. No new AD_Column needed either way - this is a
 *       data-migration-process fix, out of scope for this class.</li>
 * </ul>
 *
 * <p>Same drop-and-recreate behaviour as the other three processes: every column this process
 * manages is dropped and rebuilt fresh on every run.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQctoArtisansColumns")
public class AddZZLearnerQctoArtisansColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOArtisans";
    private static final String ENTITY_TYPE = "MQA Learner";

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        specs.add(new ColumnSpec("Is_Approved", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.isapproved (0 of 1230 staged rows populated as of 2026-07-16, added anyway)"));
        specs.add(new ColumnSpec("Date_Approved", DisplayType.DateTime, 7,
                "ms_learnerqctoartisans.dateapproved"));
        specs.add(new ColumnSpec("Previous_Employed", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.previousemployed (confirmed only 1/2/null across staged data)"));
        specs.add(new ColumnSpec("Learner_Employed", DisplayType.String, 4000,
                "ms_learnerqctoartisans.learneremployed (free text - mixed dates/numbers/\"never worked\")"));
        specs.add(new ColumnSpec("WP_Agreement", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.wpagreement (confirmed only 1/2/null across staged data)"));
        specs.add(new ColumnSpec("Is_Terms_Employment", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.istermsemployment (confirmed only 1/2/null across staged data)"));
        specs.add(new ColumnSpec("Terms_Employment", DisplayType.String, 4000,
                "ms_learnerqctoartisans.termsemployment (free text, e.g. \"Contract\")"));
        specs.add(new ColumnSpec("Emp_Contract", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.empcontract (confirmed only 1/2/null across staged data)"));
        specs.add(new ColumnSpec("Emp_Contract_Copy", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.empcontractcopy (confirmed only 1/2/null across staged data)"));
        specs.add(new ColumnSpec("Responsible_Seta", DisplayType.String, 4000,
                "ms_learnerqctoartisans.responsibleseta (free text, e.g. \"MQA\")"));
        specs.add(new ColumnSpec("Ass_Partner", DisplayType.String, 4000,
                "ms_learnerqctoartisans.asspartner (free text, e.g. \"MQA\")"));
        specs.add(new ColumnSpec("Reg_Saqa", DisplayType.String, 4000,
                "ms_learnerqctoartisans.regsaqa (free text, e.g. \"MQA\")"));
        specs.add(new ColumnSpec("Cur_Reg_Number", DisplayType.String, 4000,
                "ms_learnerqctoartisans.curregnumber (free text registration number)"));
        specs.add(new ColumnSpec("Qcto", DisplayType.String, 4000,
                "ms_learnerqctoartisans.qcto (free text, e.g. \"NAMB\"/\"MQA\")"));
        specs.add(new ColumnSpec("Occupation", DisplayType.String, 4000,
                "ms_learnerqctoartisans.occupation (OFO-code-looking free text, e.g. \"651302\" - does NOT match zzlkpofooccupation.Value's format, kept as plain text)"));
        specs.add(new ColumnSpec("Namb_Confirmation", DisplayType.YesNo, 1,
                "ms_learnerqctoartisans.nambconfirmation (only 1 of 1230 staged rows populated as of 2026-07-16, added anyway)"));
        specs.add(new ColumnSpec("Namb_Confirmation_Date", DisplayType.DateTime, 7,
                "ms_learnerqctoartisans.nambconfirmationdate (only 1 of 1230 staged rows populated as of 2026-07-16, added anyway)"));
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

        int plainAdded = 0;
        int plainRecreated = 0;
        for (ColumnSpec spec : PLAIN_COLUMNS) {
            if (AddColumnsSupport.dropColumnIfExists(table, spec.columnName, get_TrxName(), this::addLog)) {
                plainRecreated++;
            } else {
                plainAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), table, spec.columnName, spec.referenceId, spec.fieldLength,
                    spec.description, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        // Actor columns - same Search/AD_User convention as the already-existing
        // ZZEnrolledBy/ZZCertificateCreatedBy columns on this table (confirmed by reading them).
        int actorRecreated = 0;
        actorRecreated += addActorColumn(table, "Approved_By",
                "ms_learnerqctoartisans.approvedby (0 of 1230 staged rows populated as of 2026-07-16, added anyway)") ? 1 : 0;
        actorRecreated += addActorColumn(table, "Namb_Confirmation_User",
                "ms_learnerqctoartisans.nambconfirmationuser (0 of 1230 staged rows populated as of 2026-07-16, added anyway)") ? 1 : 0;

        // Employer_ID - CONFIRMED 2026-07-16 to match ms_organisation.id (1085/1085 join).
        // Explicit AD_Reference_Value_ID (not pure Table Direct naming) since "Employer" does
        // not match the C_BPartner table name - see class Javadoc.
        boolean employerRecreated = AddColumnsSupport.dropColumnIfExists(table, "Employer_ID", get_TrxName(),
                this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Employer_ID", DisplayType.Search, 200175, 10,
                "ms_learnerqctoartisans.employerid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no (same as organisationid elsewhere)",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": " + plainAdded + " plain column(s) added (" + plainRecreated + " recreated), "
                + "2 actor column(s) added (" + actorRecreated + " recreated), Employer_ID "
                + (employerRecreated ? "recreated" : "added") + ".";
    }

    private boolean addActorColumn(MTable table, String columnName, String description) {
        boolean recreated = AddColumnsSupport.dropColumnIfExists(table, columnName, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, columnName, DisplayType.Search, REFERENCE_AD_USER, 10,
                description, ENTITY_TYPE, get_TrxName(), this::addLog);
        return recreated;
    }
}
