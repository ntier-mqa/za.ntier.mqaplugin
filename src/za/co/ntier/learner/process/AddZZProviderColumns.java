package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

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
 * <p>Three building blocks, all copied from real iDempiere framework code rather than
 * guessed - see the methods below for exactly which one each mirrors:
 * <ul>
 *   <li>org.idempiere.process.CreateTable.createColumn() - the per-standard-column
 *       (AD_Client_ID/AD_Org_ID/Created/.../Value/Name) switch statement that decides
 *       reference type, length, mandatory-ness etc. Mirrored in
 *       {@link #createStandardColumn(MTable, String)}.</li>
 *   <li>org.compiere.process.TableCreateColumns - the Element/MColumn creation pattern
 *       (get-or-create M_Element, then a new MColumn with Name/Description/AD_Reference_ID/
 *       FieldLength copied from it). Mirrored in {@link #addColumn}.</li>
 *   <li>org.compiere.process.ColumnSync - the "actually alter the physical table" step.
 *       Confirmed by reading MColumn.afterSave()/MTable.afterSave(): saving an AD_Column or
 *       AD_Table record does NOT touch the physical database by itself (afterSave only does
 *       cache/sequence bookkeeping) - so org.idempiere.process.CreateTable (which
 *       za.ntier.process.CreateLookupReferenceTables drives) only registers Application
 *       Dictionary metadata, it does NOT create the physical table. The actual DDL only
 *       happens via MColumn.getSQLAdd(table) (existing table, new column) or
 *       MTable.getSQLCreate() (brand new table, built from every AD_Column already
 *       registered against it) - both mirrored here in {@link #executeDdl}, so this process
 *       leaves every table it touches immediately usable, no manual "Sync Column" step
 *       needed afterward.</li>
 * </ul>
 *
 * <p>ZZProvider columns are DROP-and-RECREATE on every run (see
 * {@link #dropColumnIfExists}): if a column already exists it is deleted (physically and
 * from the Application Dictionary) and rebuilt fresh, rather than left alone. This is
 * deliberately destructive - added 2026-07-15 because an earlier test run had created some
 * of these columns as plain Integer, before this process used real Table Direct references,
 * and simply skipping "already exists" would have left the wrong definition in place. Safe
 * while ZZProvider holds no data (0 rows as of this writing); revisit before running against
 * a populated table. Reference tables (Provider_Type etc.) are the one exception - if one
 * already exists it is left as-is, not re-populated, since dropping it could lose real
 * lookup data a user might have added.
 *
 * <p><b>Scope (2026-07-15):</b>
 * <ul>
 *   <li>Section A - 13 plain columns, added as their real type.</li>
 *   <li>Section B - 9 reference columns (Provider_Type_ID, Provider_Class_ID,
 *       Provider_Accreditation_Status_ID, Quality_Assurance_Body_ID,
 *       Accreditation_Type_ID, Provider_Internal_External_ID, Accrediting_Council_ID,
 *       Provider_Application_ID, Saqa_QA_ID) - each gets its own small reference table
 *       created (Value/Name, plus an "id" recon column pointing back at the MSSQL source
     *   row - same convention used everywhere else in this migration project), populated
 *       from the already-staged ms_lkpXXX table, then the ZZProvider column is added as a
 *       real Table Direct reference to it. Saqa_QA is the one exception: Value=ETQAID (not
 *       the lookup table's own id) and Name=DataSupplier, per the confirmed routing in the
 *       mapping doc Section D - matched by ETQAID text, not by MSSQL row id.</li>
 *   <li>C_BPartner_ID (organisationid's resolved target) - added as a Table Direct
 *       reference to the already-existing standard C_BPartner table; nothing to create.
 *       Actually populating it (Organisation.SDLNumber -&gt; c_bpartner.zz_sdl_no matching)
 *       happens later, in the data-migration process, not here.</li>
 *   <li>"levy" is deliberately NOT included - still unresolved (see the mapping doc).</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZProviderColumns")
public class AddZZProviderColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZProvider";
    /** Matches the EntityType/AccessLevel already used by every existing ZZProvider column/table. */
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3"; // Client+Organization, matches ZZProvider itself

    // =================================================================
    // Section A - plain columns
    // =================================================================
    private static final class ColumnSpec {
        final String columnName;
        final int referenceId;
        final int fieldLength;
        final String description;

        ColumnSpec(String columnName, int referenceId, int fieldLength, String description) {
            this.columnName = columnName;
            this.referenceId = referenceId;
            this.fieldLength = fieldLength;
            this.description = description;
        }
    }

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

    // =================================================================
    // Section B - reference columns (target table created if missing)
    // =================================================================
    private static final class ReferenceColumnSpec {
        final String columnName;
        final String sourceTable;
        final String sourceValueCol;
        final String sourceNameCol;
        final String description;

        ReferenceColumnSpec(String columnName, String sourceTable, String sourceValueCol,
                String sourceNameCol, String description) {
            this.columnName = columnName;
            this.sourceTable = sourceTable;
            this.sourceValueCol = sourceValueCol;
            this.sourceNameCol = sourceNameCol;
            this.description = description;
        }

        /** "Provider_Type_ID" -&gt; "Provider_Type" - the new reference table's name, derived
         * (not hardcoded separately) so the Table Direct naming convention can never drift
         * out of sync with the column name. */
        String targetTableName() {
            return columnName.endsWith("_ID") ? columnName.substring(0, columnName.length() - 3) : columnName;
        }
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
        MTable providerTable = findTable(TABLE_NAME);
        if (providerTable == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        int plainAdded = 0;
        int plainRecreated = 0;
        for (ColumnSpec spec : PLAIN_COLUMNS) {
            if (dropColumnIfExists(providerTable, spec.columnName)) {
                plainRecreated++;
            } else {
                plainAdded++;
            }
            addColumn(providerTable, spec.columnName, spec.referenceId, spec.fieldLength, spec.description);
        }

        int tablesCreated = 0;
        int refAdded = 0;
        int refRecreated = 0;
        for (ReferenceColumnSpec spec : REFERENCE_COLUMNS) {
            String targetTableName = spec.targetTableName();
            MTable targetTable = findTable(targetTableName);
            if (targetTable == null) {
                targetTable = createReferenceTableSchema(targetTableName,
                        "Reference values for " + spec.description);
                populateReferenceTable(targetTable, spec);
                addLog("Created and populated reference table " + targetTableName
                        + " from " + spec.sourceTable + ".");
                tablesCreated++;
            } else {
                addLog(targetTableName + " already exists - left as-is (not re-populated).");
            }

            if (dropColumnIfExists(providerTable, spec.columnName)) {
                refRecreated++;
            } else {
                refAdded++;
            }
            addColumn(providerTable, spec.columnName, DisplayType.TableDir, 10,
                    spec.description + " -> " + targetTableName);
        }

        boolean bpartnerRecreated = dropColumnIfExists(providerTable, "C_BPartner_ID");
        addColumn(providerTable, "C_BPartner_ID", DisplayType.TableDir, 10,
                "ms_skillsdevelopmentprovider.organisationid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no");

        return TABLE_NAME + ": " + plainAdded + " plain column(s) added (" + plainRecreated + " recreated), "
                + tablesCreated + " reference table(s) created, " + refAdded + " reference column(s) added ("
                + refRecreated + " recreated), C_BPartner_ID " + (bpartnerRecreated ? "recreated" : "added") + ".";
    }

    /**
     * Per user instruction 2026-07-15 (an earlier test run had created some of these columns
     * as plain Integer, before this process was changed to use real Table Direct
     * references): every column this process manages is now dropped and recreated on every
     * run, rather than left alone if already present. This is destructive - any data already
     * in that column on any existing ZZProvider row is lost - but ZZProvider is still empty
     * (0 rows) as of this writing, and this process only ever touches column DEFINITIONS,
     * not data, so re-running it during schema iteration is the expected use. If ZZProvider
     * ever holds real data, this behaviour needs revisiting before running again.
     *
     * @return true if a column existed and was dropped, false if there was nothing to drop
     */
    private boolean dropColumnIfExists(MTable table, String columnName) {
        MColumn existing = table.getColumn(columnName);
        if (existing == null) {
            return false;
        }
        // Physically drop first - same as adding, saving/deleting an AD_Column record does
        // not touch the physical database by itself (see class Javadoc), so this process
        // does the DDL itself. No schema prefix, matches the framework's own
        // getSQLAdd()/getSQLCreate() convention (relies on the DB connection's search_path).
        DB.executeUpdateEx("ALTER TABLE " + table.getTableName() + " DROP COLUMN IF EXISTS " + columnName,
                get_TrxName());
        existing.deleteEx(true, get_TrxName());
        addLog(table.getTableName() + "." + columnName + " dropped (will be recreated).");
        return true;
    }

    private MTable findTable(String tableName) {
        return new Query(getCtx(), MTable.Table_Name, "UPPER(TableName)=UPPER(?)", get_TrxName())
                .setParameters(tableName)
                .first();
    }

    /**
     * Adds one column to an EXISTING, already-physical table: Element/MColumn registration
     * (mirrors org.compiere.process.TableCreateColumns) then MColumn.getSQLAdd() + execute
     * (mirrors org.compiere.process.ColumnSync).
     */
    private void addColumn(MTable table, String columnName, int referenceId, int fieldLength, String description) {
        M_Element element = M_Element.get(getCtx(), columnName);
        if (element == null) {
            element = new M_Element(getCtx(), columnName, ENTITY_TYPE, get_TrxName());
            element.setName(columnName.replace('_', ' ').trim());
            element.setDescription(description);
            element.saveEx();
        }

        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setDescription(element.getDescription());
        column.setAD_Element_ID(element.getAD_Element_ID());
        column.setIsMandatory(false);
        column.setAD_Reference_ID(referenceId);
        column.setFieldLength(fieldLength);
        column.setIsUpdateable(true);
        column.saveEx();

        String sql = column.getSQLAdd(table);
        if (sql == null || sql.trim().isEmpty()) {
            throw new AdempiereException("MColumn.getSQLAdd() returned empty SQL for " + table.getTableName() + "." + columnName);
        }
        executeDdl(sql);
        addLog(table.getTableName() + "." + columnName + " added (" + sql + ")");
    }

    /**
     * Registers a brand new reference table (AD_Table + its standard system columns + key +
     * UUID + Value + Name + an "id" recon column) and then physically creates it in one shot
     * via MTable.getSQLCreate() - mirrors org.idempiere.process.CreateTable's Element/MColumn
     * pattern for the standard columns, plus the DDL step that class is missing (see class
     * Javadoc). Deliberately narrower in scope than CreateTable: no workflow columns, no
     * translation table, no UUID unique index/constraint - this is a small lookup table, not
     * a document table.
     */
    private MTable createReferenceTableSchema(String tableName, String description) {
        MTable table = new MTable(getCtx(), 0, get_TrxName());
        table.setTableName(tableName);
        table.setName(tableName.replace('_', ' ').trim());
        table.setDescription(description);
        table.setEntityType(ENTITY_TYPE);
        table.setAccessLevel(ACCESS_LEVEL);
        table.setIsDeleteable(true);
        table.setIsChangeLog(true);
        table.saveEx();

        createStandardColumn(table, "AD_Client_ID");
        createStandardColumn(table, "AD_Org_ID");
        createStandardColumn(table, "Created");
        createStandardColumn(table, "CreatedBy");
        createStandardColumn(table, "Updated");
        createStandardColumn(table, "UpdatedBy");
        createStandardColumn(table, "IsActive");
        createKeyColumn(table);
        createUUIDColumn(table);
        createStandardColumn(table, "Value");
        createStandardColumn(table, "Name");
        createReconIdColumn(table);

        String createSql = table.getSQLCreate();
        if (createSql == null || createSql.trim().isEmpty()) {
            throw new AdempiereException("MTable.getSQLCreate() returned empty SQL for " + tableName);
        }
        executeDdl(createSql);
        addLog(tableName + " physically created (" + createSql + ")");
        return table;
    }

    /**
     * Mirrors org.idempiere.process.CreateTable.createColumn()'s per-standard-column switch
     * (reference type, length, mandatory-ness) - only for the subset of standard columns a
     * small lookup table needs. AD_Client_ID/AD_Org_ID/Created/CreatedBy/Updated/UpdatedBy
     * elements are expected to already exist (every table in this instance uses them); this
     * throws rather than silently creating a divergent element if one is somehow missing.
     */
    private void createStandardColumn(MTable table, String columnName) {
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(getCtx(), columnName, get_TrxName());
        if (element == null) {
            throw new AdempiereException("No system M_Element found for '" + columnName
                    + "' - expected a pre-seeded standard element");
        }

        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setDescription(element.getDescription());
        column.setHelp(element.getHelp());
        column.setAD_Element_ID(element.getAD_Element_ID());

        switch (columnName) {
        case "AD_Client_ID":
            column.setAD_Reference_ID(DisplayType.Search);
            column.setDefaultValue("@#AD_Client_ID@");
            column.setIsMandatory(true);
            column.setIsUpdateable(false);
            column.setReadOnlyLogic("1=1");
            break;
        case "AD_Org_ID":
            column.setAD_Reference_ID(DisplayType.TableDir);
            column.setDefaultValue("@AD_Org_ID@");
            column.setIsMandatory(true);
            column.setIsUpdateable(false);
            break;
        case "Created":
        case "Updated":
            column.setAD_Reference_ID(DisplayType.DateTime);
            column.setIsMandatory(true);
            column.setIsUpdateable(false);
            break;
        case "CreatedBy":
        case "UpdatedBy":
            column.setAD_Reference_ID(DisplayType.Search);
            column.setAD_Reference_Value_ID(REFERENCE_AD_USER);
            column.setIsMandatory(true);
            column.setIsUpdateable(false);
            break;
        case "IsActive":
            column.setAD_Reference_ID(DisplayType.YesNo);
            column.setIsMandatory(true);
            column.setIsUpdateable(true);
            column.setFieldLength(1);
            column.setDefaultValue("Y");
            break;
        case "Value":
            column.setAD_Reference_ID(DisplayType.String);
            column.setIsUpdateable(true);
            column.setFieldLength(40);
            break;
        case "Name":
            column.setAD_Reference_ID(DisplayType.String);
            column.setIsUpdateable(true);
            column.setIsMandatory(true);
            column.setIsIdentifier(true);
            // Longer than iDempiere's usual 60 for a Name column - some source
            // descriptions (e.g. SAQA Data Supplier names) run well past 60 characters.
            column.setFieldLength(255);
            break;
        default:
            throw new AdempiereException("createStandardColumn: unhandled column '" + columnName + "'");
        }

        column.saveEx();
    }

    private void createKeyColumn(MTable table) {
        String columnName = table.getTableName() + "_ID";
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(getCtx(), columnName);
        if (element == null) {
            element = new M_Element(getCtx(), columnName, ENTITY_TYPE, get_TrxName());
            element.setName(table.getName());
            element.setPrintName(table.getName());
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setAD_Element_ID(element.getAD_Element_ID());
        column.setIsKey(true);
        column.setAD_Reference_ID(DisplayType.ID);
        column.setIsMandatory(true);
        column.setFieldLength(22);
        column.saveEx();
    }

    /**
     * Deliberately does NOT create the unique index/constraint
     * org.idempiere.process.CreateTable also builds for the UUID column (MTableIndex +
     * MIndexColumn) - not required for basic PO save/read, only for uniqueness enforcement.
     * Can be added later via the standard Table &amp; Column window if wanted.
     */
    private void createUUIDColumn(MTable table) {
        String columnName = PO.getUUIDColumnName(table.getTableName());
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(getCtx(), columnName);
        if (element == null) {
            element = new M_Element(getCtx(), columnName, ENTITY_TYPE, get_TrxName());
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setAD_Element_ID(element.getAD_Element_ID());
        column.setAD_Reference_ID(DisplayType.UUID);
        column.setFieldLength(36);
        column.saveEx();
    }

    /**
     * "id" recon column - same name-and-purpose convention used everywhere else in this
     * migration project (see MigrationSupport.stampCreatedUpdated): traces a row on this new
     * reference table back to its ms_lkpXXX source row without needing a separate crosswalk
     * table. Plain Integer (not a dedicated "bigint" DisplayType - none exists in iDempiere,
     * and every source lookup table here has well under 50 rows, so NUMBER(10) is fine).
     */
    private void createReconIdColumn(MTable table) {
        if (table.getColumn("id") != null) {
            return;
        }
        M_Element element = M_Element.get(getCtx(), "id");
        if (element == null) {
            element = new M_Element(getCtx(), "id", ENTITY_TYPE, get_TrxName());
            element.setName("Source Id");
            element.setDescription("Source system row id (recon column)");
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setDescription(element.getDescription());
        column.setAD_Element_ID(element.getAD_Element_ID());
        column.setIsMandatory(false);
        column.setAD_Reference_ID(DisplayType.Integer);
        column.setFieldLength(10);
        column.setIsUpdateable(true);
        column.saveEx();
    }

    /**
     * Copies every row from the staged MSSQL lookup table into the freshly-created reference
     * table via the generic PO API (org.adempiere.model.GenericPO, obtained through
     * MTable.getPO(0, trxName)) - there's no generated model class for these brand new
     * tables to use typed setters with.
     */
    private void populateReferenceTable(MTable table, ReferenceColumnSpec spec) throws Exception {
        int adClientId = Env.getAD_Client_ID(getCtx());
        PreparedStatement pst = null;
        ResultSet rs = null;
        int count = 0;
        try {
            pst = DB.prepareStatement(
                    "SELECT " + spec.sourceValueCol + " AS value_col, " + spec.sourceNameCol + " AS name_col, id "
                    + "FROM " + spec.sourceTable, get_TrxName());
            rs = pst.executeQuery();
            while (rs.next()) {
                PO po = table.getPO(0, get_TrxName());
                po.set_ValueOfColumn("AD_Client_ID", adClientId);
                po.set_ValueOfColumn("AD_Org_ID", 0);
                po.set_ValueOfColumn("IsActive", "Y");
                Object valueObj = rs.getObject("value_col");
                po.set_ValueOfColumn("Value", valueObj == null ? null : String.valueOf(valueObj));
                po.set_ValueOfColumn("Name", rs.getString("name_col"));
                po.set_ValueOfColumn("id", rs.getInt("id"));
                po.saveEx();
                count++;
            }
        } finally {
            DB.close(rs, pst);
        }
        addLog(table.getTableName() + ": " + count + " row(s) loaded from " + spec.sourceTable + ".");
    }

    /** Executes DDL returned by MColumn.getSQLAdd()/MTable.getSQLCreate(), splitting on
     * DB.SQLSTATEMENT_SEPARATOR if the driver needed more than one statement - same handling
     * as org.compiere.process.ColumnSync. */
    private void executeDdl(String sql) {
        int result;
        if (sql.indexOf(DB.SQLSTATEMENT_SEPARATOR) == -1) {
            result = DB.executeUpdateEx(sql, get_TrxName());
        } else {
            result = 0;
            for (String statement : sql.split(DB.SQLSTATEMENT_SEPARATOR)) {
                result += DB.executeUpdateEx(statement, get_TrxName());
            }
        }
        if (result == -1) {
            throw new AdempiereException("DDL failed: " + sql);
        }
    }
}
