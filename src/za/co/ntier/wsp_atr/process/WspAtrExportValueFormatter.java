package za.co.ntier.wsp_atr.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

final class WspAtrExportValueFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ExportSubmittedWspAtrToXlsm process;

    WspAtrExportValueFormatter(ExportSubmittedWspAtrToXlsm process) {
        this.process = process;
    }

    void writeValue(Cell cell, MColumn column, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        int displayType = column != null ? column.getAD_Reference_ID() : DisplayType.String;
        if (value instanceof Timestamp) {
            cell.setCellValue(((Timestamp) value).toInstant().atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT));
            return;
        }

        if (column != null && isReferenceColumn(column)) {
            cell.setCellValue(resolveReferenceDisplay(process.getCtx(), column, value, process.get_TrxName()));
            return;
        }

        if (displayType == DisplayType.YesNo && value instanceof Boolean) {
            cell.setCellValue((Boolean) value ? "Yes" : "No");
            return;
        }

        if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
            return;
        }

        if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value ? "Y" : "N");
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }

    private boolean isReferenceColumn(MColumn column) {
        int displayType = column.getAD_Reference_ID();
        return displayType == DisplayType.Table
                || displayType == DisplayType.TableDir
                || displayType == DisplayType.Search
                || displayType == DisplayType.List;
    }

    private String resolveReferenceDisplay(Properties ctx, MColumn column, Object value, String trxName) {
        if (value == null) {
            return "";
        }

        int displayType = column.getAD_Reference_ID();
        if (displayType == DisplayType.List) {
            String rawValue = String.valueOf(value);
            int referenceId = column.getAD_Reference_Value_ID() > 0
                    ? column.getAD_Reference_Value_ID()
                    : column.getAD_Reference_ID();
            String listName = MRefList.getListName(ctx, referenceId, rawValue);
            if (!Util.isEmpty(listName, true)) {
                return listName;
            }
            return rawValue;
        }

        if (!(value instanceof Number)) {
            return String.valueOf(value);
        }

        int recordId = ((Number) value).intValue();
        if (recordId <= 0) {
            return "";
        }

        int referenceId = column.getAD_Reference_Value_ID();
        if (referenceId <= 0) {
            return String.valueOf(value);
        }

        MRefTable refTable = MRefTable.get(ctx, referenceId, trxName);
        if (refTable == null || refTable.getAD_Table_ID() <= 0) {
            return String.valueOf(value);
        }

        MTable referenceTable = MTable.get(ctx, refTable.getAD_Table_ID());
        if (referenceTable == null || referenceTable.getAD_Table_ID() <= 0) {
            return String.valueOf(value);
        }

        PO referencedRecord = referenceTable.getPO(recordId, trxName);
        if (referencedRecord == null) {
            return String.valueOf(value);
        }

        String resolvedValue = getStringValue(referencedRecord, "Value");
        String resolvedName = getStringValue(referencedRecord, "Name");

        if (!Util.isEmpty(resolvedValue, true) && !Util.isEmpty(resolvedName, true)) {
            return resolvedName;
        }
        if (!Util.isEmpty(resolvedName, true)) {
            return resolvedName;
        }
        if (!Util.isEmpty(resolvedValue, true)) {
            return resolvedValue;
        }

        return String.valueOf(value);
    }

    private String getStringValue(PO record, String columnName) {
        if (record == null || record.get_ColumnIndex(columnName) < 0) {
            return null;
        }

        Object value = record.get_Value(columnName);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
