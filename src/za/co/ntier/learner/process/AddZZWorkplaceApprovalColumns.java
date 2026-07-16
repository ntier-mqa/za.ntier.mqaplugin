package za.co.ntier.learner.process;

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
import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Adds the missing columns to the already-existing ZZWorkplaceApproval table as REAL
 * Application Dictionary AD_Column records - same treatment as {@link AddZZProviderColumns},
 * see "ZZWorkplaceApproval - New Columns to Add.txt" in the Learners Data Migration runbook
 * for the full column-by-column reasoning, and {@link AddColumnsSupport}'s Javadoc for the
 * shared table/column-creation engine both processes use. Does NOT create ZZWorkplaceApproval
 * itself, only adds columns to it.
 *
 * <p>Two of ms_workplaceapproval's reference columns are look-alikes of Provider columns but
 * are confirmed to point at DIFFERENT MSSQL source tables, so they get their own distinctly
 * named target table (Table Direct resolves purely by name - column name minus "_ID" must
 * equal the target table name):
 * <ul>
 *   <li>accreditationtypeid -&gt; lkpAccreditationType (generic: Learnership/Skills
 *       Programme/Both/etc), NOT Provider's lkpProviderAccreditationType. Column named
 *       Workplace_Accreditation_Type_ID -&gt; table Workplace_Accreditation_Type, so it can't
 *       collide with Provider's Accreditation_Type table.</li>
 *   <li>workplaceapprovalinternalexternalid -&gt; lkpWorkplaceApprovalInternalExternal, NOT
 *       Provider's lkpProviderInternalExternal. Column named
 *       Workplace_Approval_Internal_External_ID -&gt; table
 *       Workplace_Approval_Internal_External, so it can't collide with Provider's
 *       Provider_Internal_External table.</li>
 * </ul>
 *
 * <p>Two reference columns are deliberately shared, reusing whichever process created the
 * table first rather than being recreated:
 * <ul>
 *   <li>Quality_Assurance_Body_ID -&gt; same "Quality_Assurance_Body" table as
 *       {@link AddZZProviderColumns} (same MSSQL source, ms_lkpqualityassurancebody).</li>
 *   <li>Levy_ID -&gt; same "Levy" table as {@link AddZZProviderColumns} (same MSSQL source,
 *       ms_lkplevyfield - user confirmed 2026-07-16 the MSSQL reference table is lkpLevyField,
 *       resolving what had looked like a Yes/No flag from the 1/2-only value pattern).</li>
 * </ul>
 *
 * <p>Same drop-and-recreate behaviour as {@link AddZZProviderColumns}: every
 * ZZWorkplaceApproval column this process manages is dropped (physically and from the
 * Application Dictionary) and rebuilt fresh on every run, rather than left alone if already
 * present. Safe while ZZWorkplaceApproval holds no data; revisit before running against a
 * populated table. Reference tables are the one exception - left as-is if already present.
 *
 * <p><b>Scope (2026-07-16):</b>
 * <ul>
 *   <li>Section A - 12 plain columns, added as their real type. workplaceapprovalcode is
 *       included even though it is 100% empty (0 of 95 rows) in the currently staged data -
 *       user confirmed 2026-07-16 to add it anyway.</li>
 *   <li>Section B - 9 reference columns (Workplace_Approval_Type_ID,
 *       Workplace_Approval_Class_ID, Workplace_Approval_Status_ID, Quality_Assurance_Body_ID,
 *       Workplace_Accreditation_Type_ID, Workplace_Approval_Application_ID,
 *       Workplace_Approval_Internal_External_ID, Workplace_Approval_Body_ID, Levy_ID).</li>
 *   <li>C_BPartner_ID (organisationid's resolved target) - same resolution as Provider's:
 *       Table Direct reference to the existing C_BPartner table; the actual
 *       Organisation.SDLNumber -&gt; c_bpartner.zz_sdl_no lookup happens later, in the
 *       data-migration process, not here.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZWorkplaceApprovalColumns")
public class AddZZWorkplaceApprovalColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZWorkplaceApproval";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3"; // Client+Organization, matches ZZWorkplaceApproval itself

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        specs.add(new ColumnSpec("Workplace_Approval_Start_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.workplaceapprovalstartdate"));
        specs.add(new ColumnSpec("Workplace_Approval_End_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.workplaceapprovalenddate"));
        specs.add(new ColumnSpec("Dhet_Registration_Start_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.dhetregistrationstartdate"));
        specs.add(new ColumnSpec("Dhet_Registration_End_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.dhetregistrationenddate"));
        specs.add(new ColumnSpec("Dhet_Registration_Number", DisplayType.String, 4000,
                "ms_workplaceapproval.dhetregistrationnumber"));
        specs.add(new ColumnSpec("Is_Saica", DisplayType.YesNo, 1,
                "ms_workplaceapproval.issaica"));
        specs.add(new ColumnSpec("Web_Address", DisplayType.String, 4000,
                "ms_workplaceapproval.webaddress"));
        // Confirmed 2026-07-16: 0 of 95 staged rows have this populated. Added anyway per
        // user instruction ("Add the column, but it will have null values.").
        specs.add(new ColumnSpec("Workplace_Approval_Code", DisplayType.String, 4000,
                "ms_workplaceapproval.workplaceapprovalcode (always empty in staged data as of 2026-07-16)"));
        specs.add(new ColumnSpec("Not_Approved_Reason", DisplayType.String, 4000,
                "ms_workplaceapproval.notapprovedreason"));
        specs.add(new ColumnSpec("Application_Received_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.applicationreceiveddate"));
        specs.add(new ColumnSpec("Workplace_Approval_Alert_Email", DisplayType.String, 4000,
                "ms_workplaceapproval.workplaceapprovalalertemail"));
        specs.add(new ColumnSpec("Workplace_Approval_Review_Date", DisplayType.DateTime, 7,
                "ms_workplaceapproval.workplaceapprovalreviewdate"));
        return specs;
    }

    private static final List<ReferenceColumnSpec> REFERENCE_COLUMNS = buildReferenceColumnSpecs();

    private static List<ReferenceColumnSpec> buildReferenceColumnSpecs() {
        List<ReferenceColumnSpec> specs = new ArrayList<>();
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Type_ID", "ms_lkpworkplaceapprovaltype", "id", "description",
                "ms_workplaceapproval.workplaceapprovaltypeid"));
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Class_ID", "ms_lkpworkplaceapprovalclass", "id", "description",
                "ms_workplaceapproval.workplaceapprovalclassid"));
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Status_ID", "ms_lkpworkplaceapprovalstatus", "id", "description",
                "ms_workplaceapproval.workplaceapprovalstatusid"));
        // Shared with AddZZProviderColumns - same source table, reused as-is if it already exists.
        specs.add(new ReferenceColumnSpec("Quality_Assurance_Body_ID", "ms_lkpqualityassurancebody", "id", "description",
                "ms_workplaceapproval.qualityassurancebodyid"));
        // NOT the same table as Provider's Accreditation_Type_ID - see class Javadoc.
        specs.add(new ReferenceColumnSpec("Workplace_Accreditation_Type_ID", "ms_lkpaccreditationtype", "id", "description",
                "ms_workplaceapproval.accreditationtypeid (generic lkpAccreditationType, NOT lkpProviderAccreditationType)"));
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Application_ID", "ms_lkpworkplaceapprovalapplication", "id", "description",
                "ms_workplaceapproval.workplaceapprovalapplicationid"));
        // NOT the same table as Provider's Provider_Internal_External_ID - see class Javadoc.
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Internal_External_ID", "ms_lkpworkplaceapprovalinternalexternal", "id", "description",
                "ms_workplaceapproval.workplaceapprovalinternalexternalid (lkpWorkplaceApprovalInternalExternal, NOT lkpProviderInternalExternal)"));
        specs.add(new ReferenceColumnSpec("Workplace_Approval_Body_ID", "ms_lkpworkplaceapprovalbody", "id", "description",
                "ms_workplaceapproval.workplaceapprovalbody"));
        // Shared with AddZZProviderColumns - same source table, reused as-is if it already exists.
        specs.add(new ReferenceColumnSpec("Levy_ID", "ms_lkplevyfield", "id", "description",
                "ms_workplaceapproval.levy"));
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
        MTable waTable = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (waTable == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        int plainAdded = 0;
        int plainRecreated = 0;
        for (ColumnSpec spec : PLAIN_COLUMNS) {
            if (AddColumnsSupport.dropColumnIfExists(waTable, spec.columnName, get_TrxName(), this::addLog)) {
                plainRecreated++;
            } else {
                plainAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), waTable, spec.columnName, spec.referenceId, spec.fieldLength,
                    spec.description, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        int tablesCreated = 0;
        int refAdded = 0;
        int refRecreated = 0;
        for (ReferenceColumnSpec spec : REFERENCE_COLUMNS) {
            String targetTableName = spec.targetTableName();
            MTable targetTable = AddColumnsSupport.findTable(getCtx(), targetTableName, get_TrxName());
            if (targetTable == null) {
                targetTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), targetTableName,
                        "Reference values for " + spec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                        this::addLog);
                AddColumnsSupport.populateReferenceTable(getCtx(), targetTable, spec, get_TrxName(), this::addLog);
                addLog("Created and populated reference table " + targetTableName
                        + " from " + spec.sourceTable + ".");
                tablesCreated++;
            } else {
                addLog(targetTableName + " already exists - left as-is (not re-populated).");
            }

            if (AddColumnsSupport.dropColumnIfExists(waTable, spec.columnName, get_TrxName(), this::addLog)) {
                refRecreated++;
            } else {
                refAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), waTable, spec.columnName, DisplayType.TableDir, 10,
                    spec.description + " -> " + targetTableName, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        boolean bpartnerRecreated = AddColumnsSupport.dropColumnIfExists(waTable, "C_BPartner_ID",
                get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), waTable, "C_BPartner_ID", DisplayType.TableDir, 10,
                "ms_workplaceapproval.organisationid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": " + plainAdded + " plain column(s) added (" + plainRecreated + " recreated), "
                + tablesCreated + " reference table(s) created, " + refAdded + " reference column(s) added ("
                + refRecreated + " recreated), C_BPartner_ID " + (bpartnerRecreated ? "recreated" : "added") + ".";
    }
}
