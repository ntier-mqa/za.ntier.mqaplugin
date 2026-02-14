package za.ntier.process;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;

@org.adempiere.base.annotation.Process(
        name = "za.co.ntier.process.CreateDemographicCountColumns")
public class CreateDemographicCountColumns extends SvrProcess {

    @Parameter(name = "AD_Table_ID")
    private int p_AD_Table_ID = 0;

    /**
     * Optional: set your EntityType (e.g. "U" or your plugin entity type)
     */
    @Parameter(name = "EntityType")
    private String p_EntityType = "U";

    /**
     * If Y: also ALTER TABLE and create physical columns in PostgreSQL.
     * If N: dictionary only (AD_Element + AD_Column).
     */
    @Parameter(name = "CreateDBColumn")
    private String p_CreateDBColumn = "N";

    @Override
    protected void prepare() {
        // @Parameter handles it
    }

    @Override
    protected String doIt() throws Exception {
        if (p_AD_Table_ID <= 0) {
            throw new AdempiereException("Please select AD_Table_ID");
        }

        
        MTable table = new MTable (getCtx(), p_AD_Table_ID,get_TrxName());;
        if (table == null || table.getAD_Table_ID() <= 0) {
            throw new AdempiereException("Invalid AD_Table_ID=" + p_AD_Table_ID);
        }
        if (!table.isActive()) {
            throw new AdempiereException("Selected table is not active: " + table.getTableName());
        }

        final boolean createDbColumn = "Y".equalsIgnoreCase(p_CreateDBColumn);

        List<ColDef> defs = buildColumnDefinitions();

        int createdElements = 0;
        int createdColumns = 0;
        int createdDbCols = 0;

        for (ColDef def : defs) {
            // 1) Ensure System Element exists
        	M_Element element = getElement(def.dbColumnName);
            if (element == null) {
                element = new M_Element(getCtx(), 0, get_TrxName());
                element.setColumnName(def.dbColumnName);
                element.setName(def.name);
                element.setPrintName(def.name);
                element.setDescription(def.name);
                element.setEntityType(p_EntityType);
                element.saveEx();
                createdElements++;
            }

            // 2) Ensure AD_Column exists on selected table
            MColumn col = getColumn(table, def.dbColumnName);
            if (col == null) {
                col = new MColumn(table);
                col.setEntityType(p_EntityType);

                col.setAD_Element_ID(element.getAD_Element_ID());
                col.setColumnName(def.dbColumnName);
                col.setName(def.name);
                col.setDescription(def.name);

                // Count column settings
                col.setAD_Reference_ID(DisplayType.Integer); // integer count
                col.setFieldLength(10);
                col.setIsMandatory(false);
                col.setIsUpdateable(true);
                col.setIsAlwaysUpdateable(false);
                col.setIsKey(false);
                col.setIsParent(false);
                col.setIsIdentifier(false);
                col.setDefaultValue("0");

                col.saveEx();
                createdColumns++;
            }

            // 3) Optionally create physical DB column (PostgreSQL)
            if (createDbColumn) {
                if (!dbColumnExists_Postgres(table.getTableName(), def.dbColumnName)) {
                    addDbColumn_Postgres(table.getTableName(), def.dbColumnName);
                    createdDbCols++;
                }
            }
        }

        return "Done for table " + table.getTableName()
                + " | Elements created=" + createdElements
                + " | AD_Columns created=" + createdColumns
                + (createDbColumn ? (" | DB columns created=" + createdDbCols) : " | DB columns NOT created");
    }

    // ---------------------------------------------------------------------
    // Definitions (from your screenshot)
    // ---------------------------------------------------------------------

    private List<ColDef> buildColumnDefinitions() {
        List<ColDef> list = new ArrayList<>();

        // Race / Gender
        list.add(new ColDef("ZZ_African_Male_Cnt", "African Male Count"));
        list.add(new ColDef("ZZ_African_Female_Cnt", "African Female Count"));

        list.add(new ColDef("ZZ_Coloured_Male_Cnt", "Coloured Male Count"));
        list.add(new ColDef("ZZ_Coloured_Female_Cnt", "Coloured Female Count"));

        list.add(new ColDef("ZZ_Indian_Male_Cnt", "Indian Male Count"));
        list.add(new ColDef("ZZ_Indian_Female_Cnt", "Indian Female Count"));

        list.add(new ColDef("ZZ_White_Male_Cnt", "White Male Count"));
        list.add(new ColDef("ZZ_White_Female_Cnt", "White Female Count"));

        // Total
        list.add(new ColDef("ZZ_Total_Male_Cnt", "Total Male Count"));
        list.add(new ColDef("ZZ_Total_Female_Cnt", "Total Female Count"));

        // Other
        list.add(new ColDef("ZZ_Disabled_Cnt", "Disabled Count"));
        list.add(new ColDef("ZZ_NonSA_Cnt", "Non-SA Count"));

        // Age Groups
        list.add(new ColDef("ZZ_Age_U35_Cnt", "Age <35 Count"));
        list.add(new ColDef("ZZ_Age_35_55_Cnt", "Age 35-55 Count"));
        list.add(new ColDef("ZZ_Age_O55_Cnt", "Age >55 Count"));

        return list;
    }

    private static class ColDef {
        final String dbColumnName;
        final String name;

        ColDef(String dbColumnName, String name) {
            this.dbColumnName = dbColumnName;
            this.name = name;
        }
    }

    // ---------------------------------------------------------------------
    // Element & Column helpers
    // ---------------------------------------------------------------------

    private M_Element getElement(String columnName) {
        // MElement.get(ctx, columnName) exists in iDempiere; but to be safe use Query.
        return new Query(getCtx(), M_Element.Table_Name, "UPPER(ColumnName)=UPPER(?)", get_TrxName())
                .setParameters(columnName)
                .first();
    }

    private MColumn getColumn(MTable table, String columnName) {
        return new Query(getCtx(), MColumn.Table_Name,
                "AD_Table_ID=? AND UPPER(ColumnName)=UPPER(?)", get_TrxName())
                .setParameters(table.getAD_Table_ID(), columnName)
                .first();
    }

    // ---------------------------------------------------------------------
    // PostgreSQL physical column helpers
    // ---------------------------------------------------------------------

    private boolean dbColumnExists_Postgres(String tableName, String columnName) {
        // PostgreSQL folds unquoted identifiers to lowercase, so compare case-insensitively.
        final String sql =
                "SELECT COUNT(1) " +
                "FROM information_schema.columns " +
                "WHERE lower(table_name)=lower(?) " +
                "  AND lower(column_name)=lower(?)";

        int cnt = DB.getSQLValueEx(get_TrxName(), sql, tableName, columnName);
        return cnt > 0;
    }

    private void addDbColumn_Postgres(String tableName, String columnName) {
        // Use INTEGER for counts; default 0 (nullable unless you want NOT NULL).
        final String sql = "ALTER TABLE " + tableName
                + " ADD COLUMN IF NOT EXISTS " + columnName + " INTEGER DEFAULT 0";

        DB.executeUpdateEx(sql, get_TrxName());
    }
}