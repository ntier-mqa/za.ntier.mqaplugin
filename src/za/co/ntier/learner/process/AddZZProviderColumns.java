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
 * Adds the missing columns to the already-existing ZZProvider table as REAL Application
 * Dictionary AD_Column records (not just a physical ALTER TABLE) - see "ZZProvider - New
 * Columns to Add.txt" in the Learners Data Migration runbook for the full column-by-column
 * reasoning. Does NOT create ZZProvider itself, only adds columns to it.
 *
 * <p>Where a reference column's target lookup table doesn't exist yet in iDempiere (only as
 * raw staged MSSQL data, e.g. ms_lkpprovidertype), this process creates that target table
 * too - a small Value/Name reference table, one per reference column - then points the new
 * ZZProvider column at it as a proper Table Direct (AD_Reference_ID=19) reference. Per user
 * instruction 2026-07-15: "create the ad_column records with the references etc, so if the
 * reference table is missing create that first."
 *
 * <p>The actual table/column-creation engine (mirroring org.idempiere.process.CreateTable /
 * org.compiere.process.TableCreateColumns / org.compiere.process.ColumnSync - see
 * {@link AddColumnsSupport}'s Javadoc for the full write-up of what each of those does and
 * doesn't do) lives in {@link AddColumnsSupport}, shared with
 * {@link AddZZWorkplaceApprovalColumns} (factored out 2026-07-16 once a second table needed
 * the identical machinery).
 *
 * <p>ZZProvider columns are DROP-and-RECREATE on every run: if a column already exists it is
 * deleted (physically and from the Application Dictionary) and rebuilt fresh, rather than
 * left alone. This is deliberately destructive - added 2026-07-15 because an earlier test run
 * had created some of these columns as plain Integer, before this process used real Table
 * Direct references, and simply skipping "already exists" would have left the wrong
 * definition in place. Safe while ZZProvider holds no data (0 rows as of this writing);
 * revisit before running against a populated table. Reference tables (Provider_Type etc.) are
 * the one exception - if one already exists it is left as-is, not re-populated, since
 * dropping it could lose real lookup data a user might have added.
 *
 * <p><b>Scope (2026-07-16):</b>
 * <ul>
 *   <li>Section A - 13 plain columns, added as their real type.</li>
 *   <li>Section B - 10 reference columns (Provider_Type_ID, Provider_Class_ID,
 *       Provider_Accreditation_Status_ID, Quality_Assurance_Body_ID,
 *       Accreditation_Type_ID, Provider_Internal_External_ID, Accrediting_Council_ID,
 *       Provider_Application_ID, Saqa_QA_ID, Levy_ID) - each gets its own small reference
 *       table created (Value/Name, plus an "id" recon column pointing back at the MSSQL
 *       source row), populated from the already-staged ms_lkpXXX table, then the ZZProvider
 *       column is added as a real Table Direct reference to it. Saqa_QA is one exception:
 *       Value=ETQAID (not the lookup table's own id) and Name=DataSupplier, per the confirmed
 *       routing in the mapping doc Section D - matched by ETQAID text, not by MSSQL row id.
 *       Levy_ID is the other: RESOLVED 2026-07-16 (user confirmed the MSSQL reference table is
 *       lkpLevyField, NOT a Yes/No flag as first guessed from the 1/2-only value pattern) -
 *       its target table "Levy" is shared with {@link AddZZWorkplaceApprovalColumns}' own
 *       Levy_ID column (same source table, reused as-is if already created by either
 *       process).</li>
 *   <li>C_BPartner_ID (organisationid's resolved target) - added as a Table Direct
 *       reference to the already-existing standard C_BPartner table; nothing to create.
 *       Actually populating it (Organisation.SDLNumber -&gt; c_bpartner.zz_sdl_no matching)
 *       happens later, in the data-migration process, not here.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZProviderColumns")
public class AddZZProviderColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZProvider";
    /** Matches the EntityType/AccessLevel already used by every existing ZZProvider column/table. */
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3"; // Client+Organization, matches ZZProvider itself

    private static final List<ColumnSpec> PLAIN_COLUMNS = buildPlainColumnSpecs();

    private static List<ColumnSpec> buildPlainColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();
        // String=4000/DateTime=7/YesNo=1 - matches the exact FieldLength convention already
        // used by every other custom column on this table and its sibling QCTO tables
        // (confirmed via AD_Column before writing this).
        specs.add(new ColumnSpec("Accreditation_Start_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationstartdate"));
        specs.add(new ColumnSpec("Accreditation_End_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationenddate"));
        specs.add(new ColumnSpec("Saqa_Code", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.saqacode"));
        specs.add(new ColumnSpec("Saqa_Provider_Code", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.saqaprovidercode"));
        specs.add(new ColumnSpec("Dhet_Registration_Start_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.dhetregistrationstartdate"));
        specs.add(new ColumnSpec("Dhet_Registration_End_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.dhetregistrationenddate"));
        specs.add(new ColumnSpec("Dhet_Registration_Number", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.dhetregistrationnumber"));
        specs.add(new ColumnSpec("Is_Saica", DisplayType.YesNo, 1,
                "ms_skillsdevelopmentprovider.issaica"));
        specs.add(new ColumnSpec("Web_Address", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.webaddress"));
        specs.add(new ColumnSpec("Qcto_Provider_Number", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.qctoprovidernumber"));
        specs.add(new ColumnSpec("Application_Received_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.applicationreceiveddate"));
        specs.add(new ColumnSpec("Provider_Alert_Email", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.provideralertemail"));
        specs.add(new ColumnSpec("Accreditation_Review_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationreviewdate"));
        return specs;
    }

    private static final List<ReferenceColumnSpec> REFERENCE_COLUMNS = buildReferenceColumnSpecs();

    private static List<ReferenceColumnSpec> buildReferenceColumnSpecs() {
        List<ReferenceColumnSpec> specs = new ArrayList<>();
        specs.add(new ReferenceColumnSpec("Provider_Type_ID", "ms_lkpprovidertype", "id", "description",
                "ms_skillsdevelopmentprovider.providertypeid"));
        specs.add(new ReferenceColumnSpec("Provider_Class_ID", "ms_lkpproviderclass", "id", "description",
                "ms_skillsdevelopmentprovider.providerclassid"));
        specs.add(new ReferenceColumnSpec("Provider_Accreditation_Status_ID", "ms_lkpprovideraccreditationstatus", "id", "description",
                "ms_skillsdevelopmentprovider.provideraccreditationstatusid"));
        specs.add(new ReferenceColumnSpec("Quality_Assurance_Body_ID", "ms_lkpqualityassurancebody", "id", "description",
                "ms_skillsdevelopmentprovider.qualityassurancebodyid"));
        specs.add(new ReferenceColumnSpec("Accreditation_Type_ID", "ms_lkpprovideraccreditationtype", "id", "description",
                "ms_skillsdevelopmentprovider.accreditationtypeid"));
        specs.add(new ReferenceColumnSpec("Provider_Internal_External_ID", "ms_lkpproviderinternalexternal", "id", "description",
                "ms_skillsdevelopmentprovider.providerinternalexternalid"));
        specs.add(new ReferenceColumnSpec("Accrediting_Council_ID", "ms_lkpprovideraccreditingcouncil", "id", "description",
                "ms_skillsdevelopmentprovider.accreditingcouncil"));
        specs.add(new ReferenceColumnSpec("Provider_Application_ID", "ms_lkpproviderapplication", "id", "description",
                "ms_skillsdevelopmentprovider.providerapplicationid"));
        // Saqa_QA is the one exception: Value=ETQAID (text), Name=DataSupplier - matched by
        // ETQAID, NOT by lkpSAQADataSuppliers' own id column. See mapping doc Section D.
        specs.add(new ReferenceColumnSpec("Saqa_QA_ID", "ms_lkpsaqadatasuppliers", "etqaid", "datasupplier",
                "ms_skillsdevelopmentprovider.saqaqaid (stores lkpSAQADataSuppliers.ETQAID, not its id)"));
        // RESOLVED 2026-07-16 - user confirmed the MSSQL reference table is lkpLevyField (NOT
        // a Yes/No flag, despite ms_skillsdevelopmentprovider.levy only ever containing 1/2/
        // null - that was a coincidence, not the lkpYesNo encoding). Target table "Levy" is
        // shared with AddZZWorkplaceApprovalColumns' own Levy_ID column.
        specs.add(new ReferenceColumnSpec("Levy_ID", "ms_lkplevyfield", "id", "description",
                "ms_skillsdevelopmentprovider.levy"));
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
        MTable providerTable = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (providerTable == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        int plainAdded = 0;
        int plainRecreated = 0;
        for (ColumnSpec spec : PLAIN_COLUMNS) {
            if (AddColumnsSupport.dropColumnIfExists(providerTable, spec.columnName, get_TrxName(), this::addLog)) {
                plainRecreated++;
            } else {
                plainAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), providerTable, spec.columnName, spec.referenceId, spec.fieldLength,
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

            if (AddColumnsSupport.dropColumnIfExists(providerTable, spec.columnName, get_TrxName(), this::addLog)) {
                refRecreated++;
            } else {
                refAdded++;
            }
            AddColumnsSupport.addColumn(getCtx(), providerTable, spec.columnName, DisplayType.TableDir, 10,
                    spec.description + " -> " + targetTableName, ENTITY_TYPE, get_TrxName(), this::addLog);
        }

        boolean bpartnerRecreated = AddColumnsSupport.dropColumnIfExists(providerTable, "C_BPartner_ID",
                get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), providerTable, "C_BPartner_ID", DisplayType.TableDir, 10,
                "ms_skillsdevelopmentprovider.organisationid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + ": " + plainAdded + " plain column(s) added (" + plainRecreated + " recreated), "
                + tablesCreated + " reference table(s) created, " + refAdded + " reference column(s) added ("
                + refRecreated + " recreated), C_BPartner_ID " + (bpartnerRecreated ? "recreated" : "added") + ".";
    }
}
