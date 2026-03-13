package za.co.ntier.wsp_atr.process;

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

public class ReferenceLookupService {

    /**
     * Given AD_Column_ID and text from Excel, find the referenced record ID.
     * Uses AD_Reference_Value_ID -> AD_Ref_Table -> AD_Table, and either Value or Name.
     *
     * @param ctx      context
     * @param column   column definition (Table display type)
     * @param text     excel text
     * @param useValue if true, match on Value, else match on Name
     * @param trxName  trx
     * @return ID or null if not found
     */
    public Integer lookupReferenceId(Properties ctx,
                                     MColumn column,
                                     String text,
                                     boolean useValue,
                                     String trxName) {
        if (Util.isEmpty(text, true))
            return null;

        text = text.trim();
        int displayType = column.getAD_Reference_ID();
        if (displayType != DisplayType.Table && displayType != DisplayType.TableDir &&
        		displayType != DisplayType.Search) {
            throw new AdempiereException("Column " + column.getColumnName()
                    + " is not a Table/TableDir reference (AD_Reference_ID="
                    + displayType + ")");
        }

        int adRefValueId = column.getAD_Reference_Value_ID();
        if (adRefValueId <= 0) {
            throw new AdempiereException("Column " + column.getColumnName()
                    + " has no AD_Reference_Value_ID configured");
        }

        MRefTable refTable = MRefTable.get(ctx, adRefValueId);
        if (refTable == null || refTable.getAD_Table_ID() <= 0) {
            throw new AdempiereException("No AD_Ref_Table found for AD_Reference_ID="
                    + adRefValueId);
        }

        MTable refMTable = MTable.get(ctx, refTable.getAD_Table_ID());
        if (refMTable == null || refMTable.getAD_Table_ID() <= 0) {
            throw new AdempiereException("Referenced table not found for AD_Reference_ID="
                    + adRefValueId);
        }

        String tableName = refMTable.getTableName();
        String where = useValue
                ? "UPPER(TRIM(Value))=UPPER(?)"
                : "UPPER(TRIM(Name))=UPPER(?)";

        int id = new Query(ctx, tableName, where, trxName)
                .setParameters(text)
                .firstId();

        return (id <= 0) ? null : id;
    }
}
