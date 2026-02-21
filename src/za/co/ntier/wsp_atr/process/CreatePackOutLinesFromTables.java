package za.co.ntier.wsp_atr.process;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.compiere.model.X_AD_Package_Exp_Detail;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 * Create pack out lines (AD_Package_Exp_Detail) for a given list of table names
 * and link them to an existing pack out header (AD_Package_Exp_ID=1000077).
 *
 * Creates lines as:
 * - DBType = ALL
 * - Type   = DS
 * - SQLStatement = select * from <TableName>
 */
@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.CreatePackOutLinesFromTables")
public class CreatePackOutLinesFromTables extends SvrProcess {

    // hard-linked header as per request
    private static final int PACKOUT_HEADER_ID = 1000085;

    // Parameter name in AD_Process_Para
    private static final String PARAM_TABLE_LIST = "TableList";

    private String p_tableList;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            if (para == null || para.getParameterName() == null)
                continue;

            if (PARAM_TABLE_LIST.equalsIgnoreCase(para.getParameterName())) {
                p_tableList = (String) para.getParameter();
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (Util.isEmpty(p_tableList, true)) {
            throw new IllegalArgumentException("Please provide TableList (comma/newline separated table names).");
        }

        Set<String> tableNames = parseTableNames(p_tableList);
        if (tableNames.isEmpty()) {
            throw new IllegalArgumentException("No valid table names found in TableList.");
        }

        int maxLine = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(Line),0) FROM AD_Package_Exp_Detail WHERE AD_Package_Exp_ID=?",
                PACKOUT_HEADER_ID);

        int line = maxLine;
        int created = 0;
        int skipped = 0;
        int notFound = 0;

        List<String> notFoundTables = new ArrayList<>();
        List<String> skippedTables = new ArrayList<>();

        for (String tableName : tableNames) {
            // Resolve AD_Table_ID (optional but helpful)
            Integer adTableId = null;
            try {
                adTableId = DB.getSQLValueEx(get_TrxName(),
                        "SELECT AD_Table_ID FROM AD_Table WHERE UPPER(TableName)=UPPER(?)",
                        tableName);
                if (adTableId != null && adTableId.intValue() <= 0) {
                    adTableId = null;
                }
            } catch (Exception ignore) {
                adTableId = null;
            }

            if (adTableId == null) {
                // If you want to allow unknown tables, you can remove this block
                notFound++;
                notFoundTables.add(tableName);
                continue;
            }

            String sql = "select * from " + tableName;

            // Duplicate check (by SQLStatement or AD_Table_ID)
            int dup = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM AD_Package_Exp_Detail "
                    + "WHERE AD_Package_Exp_ID=? AND (UPPER(SQLStatement)=UPPER(?) OR AD_Table_ID=?)",
                    PACKOUT_HEADER_ID, sql, adTableId);

            if (dup > 0) {
                skipped++;
                skippedTables.add(tableName);
                continue;
            }

            line += 10;

            X_AD_Package_Exp_Detail d = new X_AD_Package_Exp_Detail(getCtx(), 0, get_TrxName());
            d.setAD_Org_ID(0);
            d.setAD_Package_Exp_ID(PACKOUT_HEADER_ID);
            d.setLine(line);

            // Recommended defaults (match typical packout usage)
            d.setDBType(X_AD_Package_Exp_Detail.DBTYPE_AllDatabaseTypes);   // "ALL"
            d.setType(X_AD_Package_Exp_Detail.TYPE_Table);             // "DS"
          //  d.setSQLStatement(sql);

            // Optional but useful
            d.setAD_Table_ID(adTableId);
            d.setProcessed(false);
            d.setProcessing(false);

            // Save
            d.saveEx();
            created++;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("PackOut Header AD_Package_Exp_ID=").append(PACKOUT_HEADER_ID)
           .append(" | Created=").append(created)
           .append(" | Skipped=").append(skipped)
           .append(" | NotFound=").append(notFound);

        if (!notFoundTables.isEmpty()) {
            msg.append("\nNot found in AD_Table: ").append(String.join(", ", notFoundTables));
        }
        if (!skippedTables.isEmpty()) {
            msg.append("\nSkipped (already exists): ").append(String.join(", ", skippedTables));
        }

        return msg.toString();
    }

    private Set<String> parseTableNames(String raw) {
        // Accept: comma, semicolon, newline, tab, multiple spaces
        String normalized = raw.replace("\r", "\n");
        String[] tokens = normalized.split("[,;\\n\\t ]+");

        Set<String> out = new LinkedHashSet<>();
        for (String t : tokens) {
            if (Util.isEmpty(t, true))
                continue;
            out.add(t.trim());
        }
        return out;
    }
}