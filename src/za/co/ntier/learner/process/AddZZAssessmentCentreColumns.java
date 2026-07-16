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
 * Adds the missing columns to the already-existing ZZAssessmentCentre table as REAL
 * Application Dictionary AD_Column records - same treatment as {@link AddZZProviderColumns}
 * and {@link AddZZWorkplaceApprovalColumns}, see "ZZAssessmentCentre - New Columns to Add.txt"
 * in the Learners Data Migration runbook for the full column-by-column reasoning, and
 * {@link AddColumnsSupport}'s Javadoc for the shared table/column-creation engine all three
 * processes use. Does NOT create ZZAssessmentCentre itself, only adds columns to it.
 *
 * <p>ms_assessmentcentre is shaped almost identically to ms_skillsdevelopmentprovider (same
 * accreditation/DHET/SAQA/levy columns, same reference-column family), which means MOST of its
 * reference columns point at the SAME MSSQL source table as an already-built Provider or
 * WorkplaceApproval reference table, and are simply reused (not rebuilt) rather than each
 * getting their own:
 * <ul>
 *   <li>Quality_Assurance_Body_ID -&gt; same "Quality_Assurance_Body" table as Provider/
 *       WorkplaceApproval (ms_lkpqualityassurancebody).</li>
 *   <li>Saqa_QA_ID -&gt; same "Saqa_QA" table as Provider (ms_lkpsaqadatasuppliers).</li>
 *   <li>Accrediting_Council_ID -&gt; same "Accrediting_Council" table as Provider
 *       (ms_lkpprovideraccreditingcouncil) - there is no AssessmentCentre-specific accrediting
 *       council table in MSSQL, confirmed by querying INFORMATION_SCHEMA directly.</li>
 *   <li>Levy_ID -&gt; same "Levy" table as Provider/WorkplaceApproval (ms_lkplevyfield).</li>
 *   <li>Workplace_Accreditation_Type_ID -&gt; same table as WorkplaceApproval's column of the
 *       same name (ms_lkpaccreditationtype, the GENERIC accreditation type table - Learnership/
 *       Skills Programme/Both/etc). The column is named the same as WorkplaceApproval's on
 *       purpose (not e.g. "AssessmentCentre_Accreditation_Type_ID") so Table Direct's naming
 *       convention points both at the one shared table - the "Workplace_" prefix is a
 *       historical artifact of which table got there first, not a meaningful distinction.</li>
 * </ul>
 * The remaining reference columns get their OWN new table, since AssessmentCentre has its own
 * distinct MSSQL lookup tables for these concepts (confirmed via INFORMATION_SCHEMA):
 * AssessmentCentre_Type_ID, AssessmentCentre_Class_ID,
 * AssessmentCentre_Accreditation_Status_ID, AssessmentCentre_Application_ID,
 * AssessmentCentre_Internal_External_ID.
 *
 * <p>Assessment_Centre_Code is included even though it is 100% empty (0 of 7 rows) in the
 * currently staged data - added anyway, consistent with the same call made for
 * Workplace_Approval_Code (user confirmed 2026-07-16 "Add the column, but it will have null
 * values.").
 *
 * <p>Same drop-and-recreate behaviour as the other two processes: every ZZAssessmentCentre
 * column this process manages is dropped and rebuilt fresh on every run. Reference tables are
 * left as-is if they already exist (whether newly created here or reused from Provider/
 * WorkplaceApproval).
 *
 * <p><b>Scope (2026-07-16):</b>
 * <ul>
 *   <li>Section A - 14 plain columns.</li>
 *   <li>Section B - 10 reference columns (AssessmentCentre_Type_ID, AssessmentCentre_Class_ID,
 *       AssessmentCentre_Accreditation_Status_ID, Quality_Assurance_Body_ID, Saqa_QA_ID,
 *       AssessmentCentre_Application_ID, Workplace_Accreditation_Type_ID,
 *       AssessmentCentre_Internal_External_ID, Accrediting_Council_ID, Levy_ID).</li>
 *   <li>C_BPartner_ID (organisationid's resolved target) - same resolution as Provider's.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZAssessmentCentreColumns")
public class AddZZAssessmentCentreColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZAssessmentCentre";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3"; // Client+Organization, matches ZZAssessmentCentre itself

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        specs.add(new ColumnSpec("Accreditation_Start_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.accreditationstartdate"));
        specs.add(new ColumnSpec("Accreditation_End_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.accreditationenddate"));
        specs.add(new ColumnSpec("Assessment_Centre_Accreditation_Number", DisplayType.String, 4000,
                "ms_assessmentcentre.assessmentcentreaccreditationnumber (mandatory on the source, always populated)"));
        specs.add(new ColumnSpec("Saqa_Code", DisplayType.String, 4000,
                "ms_assessmentcentre.saqacode"));
        specs.add(new ColumnSpec("Saqa_Provider_Code", DisplayType.String, 4000,
                "ms_assessmentcentre.saqaprovidercode"));
        specs.add(new ColumnSpec("Dhet_Registration_Start_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.dhetregistrationstartdate"));
        specs.add(new ColumnSpec("Dhet_Registration_End_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.dhetregistrationenddate"));
        specs.add(new ColumnSpec("Dhet_Registration_Number", DisplayType.String, 4000,
                "ms_assessmentcentre.dhetregistrationnumber"));
        specs.add(new ColumnSpec("Is_Saica", DisplayType.YesNo, 1,
                "ms_assessmentcentre.issaica"));
        specs.add(new ColumnSpec("Web_Address", DisplayType.String, 4000,
                "ms_assessmentcentre.webaddress"));
        // Confirmed pattern: 0 of 7 staged rows have this populated. Added anyway, same call as
        // Workplace_Approval_Code (see class Javadoc).
        specs.add(new ColumnSpec("Assessment_Centre_Code", DisplayType.String, 4000,
                "ms_assessmentcentre.assessmentcentrecode (always empty in staged data as of 2026-07-16)"));
        specs.add(new ColumnSpec("Application_Received_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.applicationreceiveddate"));
        specs.add(new ColumnSpec("Assessment_Centre_Alert_Email", DisplayType.String, 4000,
                "ms_assessmentcentre.assessmentcentrealertemail"));
        specs.add(new ColumnSpec("Accrediting_Council_Other", DisplayType.String, 4000,
                "ms_assessmentcentre.accreditingcouncilother (free-text companion to accreditingcouncil)"));
        specs.add(new ColumnSpec("Accreditation_Review_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.accreditationreviewdate"));
        specs.add(new ColumnSpec("Status_Effective_Date", DisplayType.DateTime, 7,
                "ms_assessmentcentre.statuseffectivedate"));
        return specs;
    }

    private static final List<ReferenceColumnSpec> REFERENCE_COLUMNS = buildReferenceColumnSpecs();

    private static List<ReferenceColumnSpec> buildReferenceColumnSpecs() {
        List<ReferenceColumnSpec> specs = new ArrayList<>();
        specs.add(new ReferenceColumnSpec("AssessmentCentre_Type_ID", "ms_lkpassessmentcentretype", "id", "description",
                "ms_assessmentcentre.assessmentcentretypeid"));
        specs.add(new ReferenceColumnSpec("AssessmentCentre_Class_ID", "ms_lkpassessmentcentreclass", "id", "description",
                "ms_assessmentcentre.assessmentcentreclassid"));
        specs.add(new ReferenceColumnSpec("AssessmentCentre_Accreditation_Status_ID", "ms_lkpassessmentcentreaccreditationstatus", "id", "description",
                "ms_assessmentcentre.assessmentcentreaccreditationstatusid"));
        // Shared with Provider/WorkplaceApproval - same source table, reused as-is.
        specs.add(new ReferenceColumnSpec("Quality_Assurance_Body_ID", "ms_lkpqualityassurancebody", "id", "description",
                "ms_assessmentcentre.qualityassurancebodyid"));
        // Shared with Provider - same source table, reused as-is. Value=ETQAID (text),
        // Name=DataSupplier, matched by ETQAID not the lookup table's own id (mapping doc
        // Section D) - same nuance as Provider's Saqa_QA_ID.
        specs.add(new ReferenceColumnSpec("Saqa_QA_ID", "ms_lkpsaqadatasuppliers", "etqaid", "datasupplier",
                "ms_assessmentcentre.saqaqaid (stores lkpSAQADataSuppliers.ETQAID, not its id)"));
        specs.add(new ReferenceColumnSpec("AssessmentCentre_Application_ID", "ms_lkpassessmentcentreapplication", "id", "description",
                "ms_assessmentcentre.assessmentcentreapplicationid"));
        // Shared with WorkplaceApproval - same source table (generic lkpAccreditationType, NOT
        // lkpProviderAccreditationType), reused as-is. Named the same as WorkplaceApproval's
        // column on purpose - see class Javadoc.
        specs.add(new ReferenceColumnSpec("Workplace_Accreditation_Type_ID", "ms_lkpaccreditationtype", "id", "description",
                "ms_assessmentcentre.accreditationtypeid (generic lkpAccreditationType, shared with zzworkplaceapproval)"));
        specs.add(new ReferenceColumnSpec("AssessmentCentre_Internal_External_ID", "ms_lkpassessmentcentreinternalexternal", "id", "description",
                "ms_assessmentcentre.assessmentcentreinternalexternalid"));
        // Shared with Provider - same source table (lkpProviderAccreditingCouncil - no
        // AssessmentCentre-specific accrediting council table exists in MSSQL), reused as-is.
        specs.add(new ReferenceColumnSpec("Accrediting_Council_ID", "ms_lkpprovideraccreditingcouncil", "id", "description",
                "ms_assessmentcentre.accreditingcouncil"));
        // Shared with Provider/WorkplaceApproval - same source table, reused as-is.
        specs.add(new ReferenceColumnSpec("Levy_ID", "ms_lkplevyfield", "id", "description",
                "ms_assessmentcentre.levy"));
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
        MTable acTable = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (acTable == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        int plainAdded = 0;
        int plainRecreated = 0;
        for (ColumnSpec spec : PLAIN_COLUMNS) {
            if (AddColumnsSupport.dropColumnIfExists(acTable, spec.columnName, get_TrxName(), this::addLog)) {
                plainRecreated++;
            } else {
                plainAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), acTable, spec.columnName, spec.referenceId, spec.fieldLength,
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

            if (AddColumnsSupport.dropColumnIfExists(acTable, spec.columnName, get_TrxName(), this::addLog)) {
                refRecreated++;
            } else {
                refAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), acTable, spec.columnName, DisplayType.TableDir, 10,
                    spec.description + " -> " + targetTableName, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        boolean bpartnerRecreated = AddColumnsSupport.dropColumnIfExists(acTable, "C_BPartner_ID",
                get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), acTable, "C_BPartner_ID", DisplayType.TableDir, 10,
                "ms_assessmentcentre.organisationid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": " + plainAdded + " plain column(s) added (" + plainRecreated + " recreated), "
                + tablesCreated + " reference table(s) created, " + refAdded + " reference column(s) added ("
                + refRecreated + " recreated), C_BPartner_ID " + (bpartnerRecreated ? "recreated" : "added") + ".";
    }
}
