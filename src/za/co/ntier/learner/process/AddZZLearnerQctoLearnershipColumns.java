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
 * Adds the missing columns to the already-existing ZZLearnerQCTOLearnership table as REAL
 * Application Dictionary AD_Column records - same treatment as
 * {@link AddZZLearnerQctoArtisansColumns} (which see for the full write-up of the
 * AddColumnsSupport engine and the general approach for these already-built join tables), see
 * "ZZLearnerQCTOLearnership - New Columns to Add.txt" in the Learners Data Migration runbook
 * for the full column-by-column reasoning.
 *
 * <p>ms_learnerqctolearnership is shaped almost identically to ms_learnerqctoartisans for the
 * columns in scope here - same set of dead/orphan flags, free-text fields, and actor/Employer
 * columns, confirmed independently against THIS table's own staged data (only 21 rows, much
 * smaller than Artisans' 1230):
 * <ul>
 *   <li>4 columns (wpagreement, istermsemployment, empcontract, empcontractcopy) are `int` and
 *       confirmed to only ever contain 1/2/null across all 21 staged rows (lkpYesNo encoding).
 *       previousemployed is also `int` and named/shaped the same way, but every one of the 21
 *       rows happens to be "1" - it isn't independently re-confirmed to also take "2" on THIS
 *       table, but is treated consistently with the same column's confirmed Yes/No meaning on
 *       ms_learnerqctoartisans.</li>
 *   <li>isapproved, nambconfirmation, nambconfirmationdate, nambconfirmationuser, approvedby -
 *       same actor/flag family as Artisans, added the same way (Search/AD_User for the two
 *       actor columns).</li>
 *   <li>employerid - CONFIRMED (2026-07-16, by joining the staged data) to match
 *       ms_organisation.id 4/4 times. Same Employer_ID treatment as Artisans (Search reference,
 *       AD_Reference_Value_ID pointing at "C_BPartner (all)", 200175 - NOT the "C_BPartner_ID"
 *       name, for the same reason documented on AddZZLearnerQctoArtisansColumns).</li>
 * </ul>
 *
 * <p><b>Deliberately NOT added</b> - this table has a few extra "dead duplicate" columns
 * Artisans didn't have, on top of the same exclusions Artisans has (extractrecordid-equivalent,
 * previousqctolearnershipcode/title, accountnumber):
 * <ul>
 *   <li>approvaldate / approvalby - confirmed (2026-07-16) to be an exact-value duplicate of
 *       dateapproved/approvedby: every populated approvaldate row has the identical timestamp
 *       to dateapproved on the same row, and approvalby is 0 of 21 populated even where
 *       approvedby is. MSSQL naming convention match: "BI_" prefixed columns
 *       (bi_registrationdate, bi_approvaldate, see below) exist ONLY on
 *       LearnerLearnership/LearnerQCTOLearnership in the whole MSSQL schema - a
 *       "Business Intelligence" reporting-mirror pattern, not source-of-truth data.</li>
 *   <li>bi_registrationdate / bi_approvaldate - 0 of 21 staged rows populated, and per the
 *       "BI_" naming convention above, a reporting mirror of registrationdate/approvaldate, not
 *       independent data. Not added.</li>
 *   <li>accountnumber - the MSSQL row's own UUID, not business data (same as Artisans).</li>
 *   <li>previousqctolearnershipcode / previousqctolearnershiptitle - redundant free-text
 *       mirrors of the already-resolved previousqctolearnership -&gt;
 *       zzpreviousqctolearnership_id FK (same as Artisans' previouslearnershipcode/title).</li>
 * </ul>
 *
 * <p>Also confirmed (2026-07-16, same discovery as Artisans, NOT a column-creation item since
 * both targets already exist): zzterminationreason is a List column matching lkpTerminationReason
 * verbatim - terminationreasonid's real target; zzterminationreasontext (already existing) is
 * the free-text terminationreason column's real target. The workbook had these backwards, same
 * as Artisans - corrected in the docs, needs a fix in the data-migration process later.
 *
 * <p>Same drop-and-recreate behaviour as the other AddZZ*Columns processes.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQctoLearnershipColumns")
public class AddZZLearnerQctoLearnershipColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOLearnership";
    private static final String ENTITY_TYPE = "MQA Learner";

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        specs.add(new ColumnSpec("Is_Approved", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.isapproved (5 of 21 staged rows populated as of 2026-07-16)"));
        specs.add(new ColumnSpec("Date_Approved", DisplayType.DateTime, 7,
                "ms_learnerqctolearnership.dateapproved"));
        specs.add(new ColumnSpec("Previous_Employed", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.previousemployed (only value \"1\" present in this table's 21 staged rows - treated consistently with the confirmed 1/2 lkpYesNo pattern on ms_learnerqctoartisans)"));
        specs.add(new ColumnSpec("Learner_Employed", DisplayType.String, 4000,
                "ms_learnerqctolearnership.learneremployed (free text, same messy shape as on Artisans)"));
        specs.add(new ColumnSpec("WP_Agreement", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.wpagreement (confirmed only 1/2/null across all 21 staged rows)"));
        specs.add(new ColumnSpec("Is_Terms_Employment", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.istermsemployment (confirmed only 1/2/null across all 21 staged rows)"));
        specs.add(new ColumnSpec("Terms_Employment", DisplayType.String, 4000,
                "ms_learnerqctolearnership.termsemployment (free text)"));
        specs.add(new ColumnSpec("Emp_Contract", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.empcontract (confirmed only 1/2/null across all 21 staged rows)"));
        specs.add(new ColumnSpec("Emp_Contract_Copy", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.empcontractcopy (confirmed only 1/2/null across all 21 staged rows)"));
        specs.add(new ColumnSpec("Responsible_Seta", DisplayType.String, 4000,
                "ms_learnerqctolearnership.responsibleseta (free text)"));
        specs.add(new ColumnSpec("Ass_Partner", DisplayType.String, 4000,
                "ms_learnerqctolearnership.asspartner (free text)"));
        specs.add(new ColumnSpec("Reg_Saqa", DisplayType.String, 4000,
                "ms_learnerqctolearnership.regsaqa (free text)"));
        specs.add(new ColumnSpec("Cur_Reg_Number", DisplayType.String, 4000,
                "ms_learnerqctolearnership.curregnumber (free text registration number)"));
        specs.add(new ColumnSpec("Qcto", DisplayType.String, 4000,
                "ms_learnerqctolearnership.qcto (free text)"));
        specs.add(new ColumnSpec("Occupation", DisplayType.String, 4000,
                "ms_learnerqctolearnership.occupation (free text - same OFO-code-format mismatch noted on Artisans, kept as plain text)"));
        specs.add(new ColumnSpec("Namb_Confirmation", DisplayType.YesNo, 1,
                "ms_learnerqctolearnership.nambconfirmation (0 of 21 staged rows populated as of 2026-07-16, added anyway)"));
        specs.add(new ColumnSpec("Namb_Confirmation_Date", DisplayType.DateTime, 7,
                "ms_learnerqctolearnership.nambconfirmationdate (0 of 21 staged rows populated as of 2026-07-16, added anyway)"));
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
        // ZZCertificateCreatedBy/ZZRegisteredBy/ZZEndorsedBy columns on this table.
        int actorRecreated = 0;
        actorRecreated += addActorColumn(table, "Approved_By",
                "ms_learnerqctolearnership.approvedby (5 of 21 staged rows populated as of 2026-07-16)") ? 1 : 0;
        actorRecreated += addActorColumn(table, "Namb_Confirmation_User",
                "ms_learnerqctolearnership.nambconfirmationuser (0 of 21 staged rows populated as of 2026-07-16, added anyway)") ? 1 : 0;

        // Employer_ID - CONFIRMED 2026-07-16 to match ms_organisation.id (4/4 join).
        boolean employerRecreated = AddColumnsSupport.dropColumnIfExists(table, "Employer_ID", get_TrxName(),
                this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Employer_ID", DisplayType.Search, 200175, 10,
                "ms_learnerqctolearnership.employerid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no (same as organisationid elsewhere)",
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
