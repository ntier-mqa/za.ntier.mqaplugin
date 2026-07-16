package za.co.ntier.learner.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.function.Consumer;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * Shared "add missing columns to an already-existing table, creating any missing small
 * Value/Name reference table along the way" engine. Factored out of AddZZProviderColumns
 * (2026-07-16) once AddZZWorkplaceApprovalColumns needed the identical machinery - see
 * either process class for the full write-up of the three real iDempiere framework classes
 * this mirrors (org.idempiere.process.CreateTable / org.compiere.process.TableCreateColumns /
 * org.compiere.process.ColumnSync) and why the physical DDL step below is needed at all
 * (MColumn.afterSave()/MTable.afterSave() do not touch the physical database by themselves).
 *
 * <p>Every method here is a direct, parameterised port of what started as instance methods on
 * AddZZProviderColumns - {@code Properties ctx}/{@code trxName}/{@code entityType} are passed
 * explicitly (same convention as {@link MigrationSupport}) rather than relying on SvrProcess,
 * and logging goes through a {@code Consumer<String>} (each caller passes {@code this::addLog}).
 */
final class AddColumnsSupport {

    private AddColumnsSupport() {
    }

    /** A plain (non-reference) column to add: name, DisplayType, field length, description. */
    static final class ColumnSpec {
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

    /** A Table Direct reference column, plus the staged MSSQL lookup table it's populated from. */
    static final class ReferenceColumnSpec {
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

    static MTable findTable(Properties ctx, String tableName, String trxName) {
        return new Query(ctx, MTable.Table_Name, "UPPER(TableName)=UPPER(?)", trxName)
                .setParameters(tableName)
                .first();
    }

    /**
     * Drop-and-recreate helper (see AddZZProviderColumns' class Javadoc for why this is
     * deliberately destructive): physically drops the column (saving/deleting an AD_Column
     * record does not touch the physical database by itself) and deletes its AD_Column
     * record, if it already exists.
     *
     * @return true if a column existed and was dropped, false if there was nothing to drop
     */
    static boolean dropColumnIfExists(MTable table, String columnName, String trxName, Consumer<String> logger) {
        MColumn existing = table.getColumn(columnName);
        if (existing == null) {
            return false;
        }
        DB.executeUpdateEx("ALTER TABLE " + table.getTableName() + " DROP COLUMN IF EXISTS " + columnName, trxName);
        existing.deleteEx(true, trxName);
        logger.accept(table.getTableName() + "." + columnName + " dropped (will be recreated).");
        return true;
    }

    /**
     * Adds one column to an EXISTING, already-physical table: Element/MColumn registration
     * (mirrors org.compiere.process.TableCreateColumns) then MColumn.getSQLAdd() + execute
     * (mirrors org.compiere.process.ColumnSync).
     */
    static void addColumn(Properties ctx, MTable table, String columnName, int referenceId, int fieldLength,
            String description, String entityType, String trxName, Consumer<String> logger) {
        M_Element element = M_Element.get(ctx, columnName, trxName);
        if (element == null) {
            element = new M_Element(ctx, columnName, entityType, trxName);
            element.setName(columnName.replace('_', ' ').trim());
            element.setDescription(description);
            element.saveEx();
        }

        MColumn column = new MColumn(table);
        column.set_TrxName(trxName);
        column.setEntityType(entityType);
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
        executeDdl(sql, trxName);
        logger.accept(table.getTableName() + "." + columnName + " added (" + sql + ")");
    }

    /**
     * Registers a brand new reference table (AD_Table + its standard system columns + key +
     * UUID + Value + Name + an "id" recon column) and then physically creates it in one shot
     * via MTable.getSQLCreate() - mirrors org.idempiere.process.CreateTable's Element/MColumn
     * pattern for the standard columns, plus the DDL step that class is missing. Deliberately
     * narrower in scope than CreateTable: no workflow columns, no translation table, no UUID
     * unique index/constraint - this is a small lookup table, not a document table.
     */
    static MTable createReferenceTableSchema(Properties ctx, String tableName, String description,
            String entityType, String accessLevel, String trxName, Consumer<String> logger) {
        MTable table = new MTable(ctx, 0, trxName);
        table.setTableName(tableName);
        table.setName(tableName.replace('_', ' ').trim());
        table.setDescription(description);
        table.setEntityType(entityType);
        table.setAccessLevel(accessLevel);
        table.setIsDeleteable(true);
        table.setIsChangeLog(true);
        table.saveEx();

        createStandardColumn(ctx, table, "AD_Client_ID", entityType, trxName);
        createStandardColumn(ctx, table, "AD_Org_ID", entityType, trxName);
        createStandardColumn(ctx, table, "Created", entityType, trxName);
        createStandardColumn(ctx, table, "CreatedBy", entityType, trxName);
        createStandardColumn(ctx, table, "Updated", entityType, trxName);
        createStandardColumn(ctx, table, "UpdatedBy", entityType, trxName);
        createStandardColumn(ctx, table, "IsActive", entityType, trxName);
        createKeyColumn(ctx, table, entityType, trxName);
        createUUIDColumn(ctx, table, entityType, trxName);
        createStandardColumn(ctx, table, "Value", entityType, trxName);
        createStandardColumn(ctx, table, "Name", entityType, trxName);
        createReconIdColumn(ctx, table, entityType, trxName);

        String createSql = table.getSQLCreate();
        if (createSql == null || createSql.trim().isEmpty()) {
            throw new AdempiereException("MTable.getSQLCreate() returned empty SQL for " + tableName);
        }
        executeDdl(createSql, trxName);
        logger.accept(tableName + " physically created (" + createSql + ")");
        return table;
    }

    /**
     * Mirrors org.idempiere.process.CreateTable.createColumn()'s per-standard-column switch
     * (reference type, length, mandatory-ness) - only for the subset of standard columns a
     * small lookup table needs. AD_Client_ID/AD_Org_ID/Created/CreatedBy/Updated/UpdatedBy
     * elements are expected to already exist (every table in this instance uses them); this
     * throws rather than silently creating a divergent element if one is somehow missing.
     */
    private static void createStandardColumn(Properties ctx, MTable table, String columnName, String entityType,
            String trxName) {
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(ctx, columnName, trxName);
        if (element == null) {
            throw new AdempiereException("No system M_Element found for '" + columnName
                    + "' - expected a pre-seeded standard element");
        }

        MColumn column = new MColumn(table);
        column.set_TrxName(trxName);
        column.setEntityType(entityType);
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

    private static void createKeyColumn(Properties ctx, MTable table, String entityType, String trxName) {
        String columnName = table.getTableName() + "_ID";
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(ctx, columnName, trxName);
        if (element == null) {
            element = new M_Element(ctx, columnName, entityType, trxName);
            element.setName(table.getName());
            element.setPrintName(table.getName());
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(trxName);
        column.setEntityType(entityType);
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
    private static void createUUIDColumn(Properties ctx, MTable table, String entityType, String trxName) {
        String columnName = PO.getUUIDColumnName(table.getTableName());
        if (table.getColumn(columnName) != null) {
            return;
        }
        M_Element element = M_Element.get(ctx, columnName, trxName);
        if (element == null) {
            element = new M_Element(ctx, columnName, entityType, trxName);
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(trxName);
        column.setEntityType(entityType);
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
    private static void createReconIdColumn(Properties ctx, MTable table, String entityType, String trxName) {
        if (table.getColumn("id") != null) {
            return;
        }
        M_Element element = M_Element.get(ctx, "id", trxName);
        if (element == null) {
            element = new M_Element(ctx, "id", entityType, trxName);
            element.setName("Source Id");
            element.setDescription("Source system row id (recon column)");
            element.saveEx();
        }
        MColumn column = new MColumn(table);
        column.set_TrxName(trxName);
        column.setEntityType(entityType);
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
     * MTable.getPO(0, trxName)) - there's no generated model class for these brand new tables
     * to use typed setters with.
     */
    static void populateReferenceTable(Properties ctx, MTable table, ReferenceColumnSpec spec, String trxName,
            Consumer<String> logger) throws Exception {
        int adClientId = Env.getAD_Client_ID(ctx);
        PreparedStatement pst = null;
        ResultSet rs = null;
        int count = 0;
        try {
            pst = DB.prepareStatement(
                    "SELECT " + spec.sourceValueCol + " AS value_col, " + spec.sourceNameCol + " AS name_col, id "
                    + "FROM " + spec.sourceTable, trxName);
            rs = pst.executeQuery();
            while (rs.next()) {
                PO po = table.getPO(0, trxName);
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
        logger.accept(table.getTableName() + ": " + count + " row(s) loaded from " + spec.sourceTable + ".");
    }

    /** Executes DDL returned by MColumn.getSQLAdd()/MTable.getSQLCreate(), splitting on
     * DB.SQLSTATEMENT_SEPARATOR if the driver needed more than one statement - same handling
     * as org.compiere.process.ColumnSync. */
    static void executeDdl(String sql, String trxName) {
        int result;
        if (sql.indexOf(DB.SQLSTATEMENT_SEPARATOR) == -1) {
            result = DB.executeUpdateEx(sql, trxName);
        } else {
            result = 0;
            for (String statement : sql.split(DB.SQLSTATEMENT_SEPARATOR)) {
                result += DB.executeUpdateEx(statement, trxName);
            }
        }
        if (result == -1) {
            throw new AdempiereException("DDL failed: " + sql);
        }
    }
}
