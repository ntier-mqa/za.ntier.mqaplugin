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
import org.compiere.model.Query;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;

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
            String fallbackDisplay = resolveKnownTableDirDisplay(column, recordId, trxName);
            return Util.isEmpty(fallbackDisplay, true) ? String.valueOf(value) : fallbackDisplay;
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

        if (displayType == DisplayType.Table || displayType == DisplayType.Search) {
            String displayColumn = resolveRefTableDisplayColumn(referenceId, trxName);
            String displayColumnValue = getStringValue(referencedRecord, displayColumn);
            if (!Util.isEmpty(displayColumnValue, true)) {
                return displayColumnValue;
            }
        }

        String resolvedValue = getStringValue(referencedRecord, "Value");
        String resolvedName = getStringValue(referencedRecord, "Name");

        if (!Util.isEmpty(resolvedValue, true) && !Util.isEmpty(resolvedName, true)) {
            if (I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZSdfOrganisation_ID
                    .equalsIgnoreCase(column.getColumnName())) {
                return resolvedValue + " - " + resolvedName;
            }
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

    private String resolveRefTableDisplayColumn(int referenceId, String trxName) {
        if (referenceId <= 0) {
            return null;
        }

        PO refTableRow = new Query(process.getCtx(), "AD_Ref_Table", "AD_Reference_ID=?", trxName)
                .setParameters(referenceId)
                .firstOnly();
        if (refTableRow == null) {
            return null;
        }

        int displayColumnId = refTableRow.get_ValueAsInt("AD_Display");
        if (displayColumnId <= 0) {
            return null;
        }

        MColumn displayColumn = MColumn.get(process.getCtx(), displayColumnId);
        if (displayColumn == null) {
            return null;
        }

        String displayColumnName = displayColumn.getColumnName();
        if (Util.isEmpty(displayColumnName, true)) {
            return null;
        }
        return displayColumnName.trim();
    }

    private String resolveKnownTableDirDisplay(MColumn column, int recordId, String trxName) {
        if (recordId <= 0 || column == null) {
            return null;
        }

        String columnName = column.getColumnName();
        if (Util.isEmpty(columnName, true)) {
            return null;
        }

        if ("ZZ_FinYear_ID".equalsIgnoreCase(columnName) || "C_Year_ID".equalsIgnoreCase(columnName)) {
            PO year = new Query(process.getCtx(), "C_Year", "C_Year_ID=?", trxName)
                    .setParameters(recordId)
                    .firstOnly();
            if (year == null) {
                return null;
            }

            String fiscalYear = getStringValue(year, "FiscalYear");
            if (!Util.isEmpty(fiscalYear, true)) {
                return fiscalYear;
            }
            String yearValue = getStringValue(year, "Year");
            if (!Util.isEmpty(yearValue, true)) {
                return yearValue;
            }
            return getStringValue(year, "Name");
        }

        return null;
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
