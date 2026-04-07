package za.co.ntier.wsp_atr.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;

final class WspAtrExportSheetWriter {
    private static final int MAX_ROWS_PER_SHEET_INCLUDING_HEADER = 1_000_000;
    private static final int MAX_DATA_ROWS_PER_SHEET = MAX_ROWS_PER_SHEET_INCLUDING_HEADER - 1;

    private static final Set<String> IGNORED_STANDARD_COLUMNS = Set.of(
            "IsActive",
            "Created",
            "CreatedBy",
            "Updated",
            "UpdatedBy",
            "UUID");

    private final ExportSubmittedWspAtrToXlsm process;
    private final WspAtrExportValueFormatter valueFormatter;

    WspAtrExportSheetWriter(ExportSubmittedWspAtrToXlsm process, WspAtrExportValueFormatter valueFormatter) {
        this.process = process;
        this.valueFormatter = valueFormatter;
    }

    int writeSheet(Workbook workbook, WspAtrExportTab exportTab) {
        List<PO> records = exportTab.getRowProvider().fetch(process, exportTab.getTabContext());
        List<WspAtrSheetColumn> columns = resolveColumns(exportTab);
        if (columns.isEmpty()) {
            throw new IllegalStateException("No exportable columns found for tab " + exportTab.getTabContext().getTabUu());
        }

        CellStyle headerStyle = createHeaderStyle(workbook);
        int totalRows = records.size();
        int offset = 0;
        int sheetSuffix = 1;

        while (offset < totalRows || (totalRows == 0 && sheetSuffix == 1)) {
            int rowsForSheet = Math.min(MAX_DATA_ROWS_PER_SHEET, totalRows - offset);
            Sheet sheet = workbook.createSheet(createSheetName(workbook, exportTab.getSheetName(), sheetSuffix));
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.size(); col++) {
                Cell headerCell = headerRow.createCell(col);
                headerCell.setCellValue(columns.get(col).getHeader());
                headerCell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (int i = 0; i < rowsForSheet; i++) {
                PO record = records.get(offset + i);
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < columns.size(); col++) {
                    columns.get(col).writeCell(valueFormatter, row.createCell(col), record);
                }
            }

            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            if (totalRows == 0) {
                break;
            }
            offset += rowsForSheet;
            sheetSuffix++;
        }

        return totalRows;
    }

    private String createSheetName(Workbook workbook, String baseSheetName, int sheetSuffix) {
        String suffix = sheetSuffix <= 1 ? "" : "_" + sheetSuffix;
        int maxBaseLength = Math.max(1, 31 - suffix.length());
        String base = baseSheetName.length() > maxBaseLength
                ? baseSheetName.substring(0, maxBaseLength)
                : baseSheetName;
        String candidate = base + suffix;
        int collision = 1;
        while (workbook.getSheet(candidate) != null) {
            String collisionSuffix = suffix + "_" + collision++;
            int collisionBaseLength = Math.max(1, 31 - collisionSuffix.length());
            String collisionBase = baseSheetName.length() > collisionBaseLength
                    ? baseSheetName.substring(0, collisionBaseLength)
                    : baseSheetName;
            candidate = collisionBase + collisionSuffix;
        }
        return candidate;
    }

    private List<WspAtrSheetColumn> resolveColumns(WspAtrExportTab exportTab) {
        Map<String, WspAtrSheetColumn> actualColumns = new LinkedHashMap<>();
        TabContext tabContext = exportTab.getTabContext();

        List<PO> fieldRows = new Query(process.getCtx(), "AD_Field",
                "AD_Tab_ID=? AND IsActive='Y' AND IsDisplayed='Y'",
                process.get_TrxName())
                        .setParameters(tabContext.getAdTabId())
                        .setOrderBy("SeqNo, AD_Field_ID")
                        .list();

        for (PO fieldRow : fieldRows) {
            int columnId = fieldRow.get_ValueAsInt("AD_Column_ID");
            if (columnId <= 0) {
                continue;
            }

            MColumn column = new MColumn(process.getCtx(), columnId, process.get_TrxName());
            if (column.getAD_Column_ID() <= 0 || column.isVirtualColumn() || shouldIgnoreColumn(tabContext, column)) {
                continue;
            }

            actualColumns.putIfAbsent(normalize(column.getColumnName()), new WspAtrTableColumn(column));
        }

        List<WspAtrSheetColumn> resolvedColumns = new ArrayList<>();
        if (exportTab.isIncludeDocumentNo()
                && !actualColumns.containsKey(normalize(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo))) {
            resolvedColumns.add(new WspAtrSyntheticColumn(
                    I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo,
                    exportTab.getDocumentNoProvider()));
        }

        resolvedColumns.addAll(actualColumns.values());
        return resolvedColumns;
    }

    private boolean shouldIgnoreColumn(TabContext tabContext, MColumn column) {
        String columnName = column.getColumnName();
        if (Util.isEmpty(columnName, true)) {
            return true;
        }

        if (IGNORED_STANDARD_COLUMNS.stream().anyMatch(ignored -> ignored.equalsIgnoreCase(columnName))) {
            return true;
        }

        String tableName = tabContext.getTable().getTableName();
        return columnName.equalsIgnoreCase(tableName + "_ID")
                || columnName.equalsIgnoreCase(tableName + "_UU");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }
}
