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

        Sheet sheet = workbook.createSheet(exportTab.getSheetName());
        CellStyle headerStyle = createHeaderStyle(workbook);
        Row headerRow = sheet.createRow(0);

        for (int col = 0; col < columns.size(); col++) {
            Cell headerCell = headerRow.createCell(col);
            headerCell.setCellValue(columns.get(col).getHeader());
            headerCell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (PO record : records) {
            Row row = sheet.createRow(rowIndex++);
            for (int col = 0; col < columns.size(); col++) {
                columns.get(col).writeCell(valueFormatter, row.createCell(col), record);
            }
        }

        for (int col = 0; col < columns.size(); col++) {
            sheet.autoSizeColumn(col);
        }

        return records.size();
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
