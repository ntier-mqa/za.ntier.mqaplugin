package za.co.ntier.wsp_atr.process;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.base.annotation.Process;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;

@Process(name = "za.co.ntier.wsp_atr.process.ExportSubmittedWspAtrToXlsm")
public class ExportSubmittedWspAtrToXlsm extends SvrProcess {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String SUBMITTED_WINDOW_UU = "406eaf0a-7d74-4942-9429-07f09ffeed85";
    private static final String SUBMITTED_TAB_UU = "b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65";
    private static final Set<String> IGNORED_STANDARD_COLUMNS = Set.of(
            "IsActive",
            "Created",
            "CreatedBy",
            "Updated",
            "UpdatedBy",
            "UUID");

    @Override
    protected void prepare() {
    }

    @Override
    protected String doIt() throws Exception {
        List<ExportTabDefinition> tabDefinitions = buildTabDefinitions();
        if (tabDefinitions.isEmpty()) {
            throw new IllegalStateException("No tabs configured for export");
        }

        Path exportPath = uniqueTempXlsm("ZZ_WSP_ATR_Submitted_Export");
        int totalRowsExported = 0;

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (ExportTabDefinition tabDefinition : tabDefinitions) {
                TabRuntimeContext runtimeContext = resolveTabRuntimeContext(tabDefinition);
                List<SheetColumn> columns = resolveSheetColumns(tabDefinition, runtimeContext);
                if (columns.isEmpty()) {
                    throw new IllegalStateException("No exportable columns found for tab " + tabDefinition.tabUu);
                }

                List<PO> records = tabDefinition.recordProvider.fetch(this, runtimeContext);
                totalRowsExported += records.size();
                writeSheet(workbook, headerStyle, tabDefinition.sheetName, columns, records);
            }

            try (OutputStream os = Files.newOutputStream(exportPath)) {
                workbook.write(os);
            }
        }

        if (processUI != null) {
            processUI.download(exportPath.toFile());
        }

        return "Exported " + totalRowsExported + " record(s) across " + tabDefinitions.size()
                + " tab(s) to " + exportPath.getFileName();
    }

    private List<ExportTabDefinition> buildTabDefinitions() {
        List<ExportTabDefinition> tabDefinitions = new ArrayList<>();
        tabDefinitions.add(ExportTabDefinition.levelZeroSubmittedTab());
        return tabDefinitions;
    }

    private TabRuntimeContext resolveTabRuntimeContext(ExportTabDefinition definition) {
        PO adTab = new Query(getCtx(), "AD_Tab", "AD_Tab_UU=? AND IsActive='Y'", get_TrxName())
                .setParameters(definition.tabUu)
                .first();
        if (adTab == null) {
            throw new IllegalStateException("Unable to resolve AD_Tab_UU=" + definition.tabUu);
        }

        int windowId = adTab.get_ValueAsInt("AD_Window_ID");
        PO adWindow = new Query(getCtx(), "AD_Window", "AD_Window_ID=? AND AD_Window_UU=? AND IsActive='Y'", get_TrxName())
                .setParameters(windowId, definition.windowUu)
                .first();
        if (adWindow == null) {
            throw new IllegalStateException("AD_Tab_UU=" + definition.tabUu + " does not belong to AD_Window_UU="
                    + definition.windowUu);
        }

        int tableId = adTab.get_ValueAsInt("AD_Table_ID");
        MTable table = MTable.get(getCtx(), tableId);
        if (table == null || table.getAD_Table_ID() <= 0) {
            throw new IllegalStateException("Unable to resolve AD_Table_ID=" + tableId + " for AD_Tab_UU=" + definition.tabUu);
        }

        return new TabRuntimeContext(adTab.get_ID(), table);
    }

    private List<SheetColumn> resolveSheetColumns(ExportTabDefinition definition, TabRuntimeContext runtimeContext) {
        Map<String, SheetColumn> orderedColumns = new LinkedHashMap<>();

        List<PO> fieldRows = new Query(getCtx(), "AD_Field", "AD_Tab_ID=? AND IsActive='Y' AND IsDisplayed='Y'", get_TrxName())
                .setParameters(runtimeContext.adTabId)
                .setOrderBy("SeqNo, AD_Field_ID")
                .list();

        for (PO fieldRow : fieldRows) {
            int columnId = fieldRow.get_ValueAsInt("AD_Column_ID");
            if (columnId <= 0) {
                continue;
            }

            MColumn column = new MColumn(getCtx(), columnId, get_TrxName());
            if (column.getAD_Column_ID() <= 0 || column.isVirtualColumn() || shouldIgnoreColumn(runtimeContext.table, column)) {
                continue;
            }

            orderedColumns.putIfAbsent(normalizeColumnKey(column.getColumnName()), new TableColumn(column));
        }

        for (String mandatoryColumnName : definition.mandatoryTableColumns) {
            PO mandatoryColumnRow = new Query(getCtx(), MColumn.Table_Name,
                    MColumn.COLUMNNAME_AD_Table_ID + "=? AND " + MColumn.COLUMNNAME_ColumnName + "=? AND "
                            + MColumn.COLUMNNAME_IsActive + "='Y'",
                    get_TrxName())
                            .setParameters(runtimeContext.table.getAD_Table_ID(), mandatoryColumnName)
                            .firstOnly();
            if (mandatoryColumnRow == null) {
                continue;
            }

            MColumn mandatoryColumn = mandatoryColumnRow instanceof MColumn
                    ? (MColumn) mandatoryColumnRow
                    : new MColumn(getCtx(), mandatoryColumnRow.get_ID(), get_TrxName());
            if (mandatoryColumn.getAD_Column_ID() <= 0 || shouldIgnoreColumn(runtimeContext.table, mandatoryColumn)) {
                continue;
            }

            orderedColumns.putIfAbsent(normalizeColumnKey(mandatoryColumnName), new TableColumn(mandatoryColumn));
        }

        for (SyntheticColumn syntheticColumn : definition.syntheticColumns) {
            orderedColumns.putIfAbsent(normalizeColumnKey(syntheticColumn.getHeader()), syntheticColumn);
        }

        return new ArrayList<>(orderedColumns.values());
    }

    private boolean shouldIgnoreColumn(MTable table, MColumn column) {
        String columnName = column.getColumnName();
        if (Util.isEmpty(columnName, true)) {
            return true;
        }

        if (IGNORED_STANDARD_COLUMNS.stream().anyMatch(ignoredColumn -> ignoredColumn.equalsIgnoreCase(columnName))) {
            return true;
        }

        String tableName = table.getTableName();
        return columnName.equalsIgnoreCase(tableName + "_ID")
                || columnName.equalsIgnoreCase(tableName + "_UU");
    }

    private String normalizeColumnKey(String columnName) {
        return columnName == null ? "" : columnName.trim().toUpperCase();
    }

    private void writeSheet(Workbook workbook, CellStyle headerStyle, String sheetName, List<SheetColumn> columns, List<PO> records) {
        Sheet sheet = workbook.createSheet(sheetName);
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
                Cell cell = row.createCell(col);
                columns.get(col).writeCell(this, cell, record);
            }
        }

        for (int col = 0; col < columns.size(); col++) {
            sheet.autoSizeColumn(col);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private void writeResolvedValue(Cell cell, MColumn column, Object value) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        int displayType = column != null ? column.getAD_Reference_ID() : DisplayType.String;
        if (value instanceof Timestamp) {
            cell.setCellValue(formatTimestamp((Timestamp) value));
            return;
        }

        if (column != null && isReferenceColumn(column)) {
            cell.setCellValue(resolveReferenceDisplay(getCtx(), column, value, get_TrxName()));
            return;
        }

        if (displayType == DisplayType.YesNo && value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
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
                return rawValue + " - " + listName;
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
            return resolvedValue + " - " + resolvedName;
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

    private String formatTimestamp(Timestamp timestamp) {
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).format(TIMESTAMP_FORMAT);
    }

    private Path uniqueTempXlsm(String baseName) throws IOException {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String stem = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path firstCandidate = tmpDir.resolve(stem + ".xlsm");

        for (int i = 0;; i++) {
            Path candidate = i == 0 ? firstCandidate : tmpDir.resolve(stem + "_" + i + ".xlsm");
            try {
                return Files.createFile(candidate);
            } catch (FileAlreadyExistsException ignore) {
            }
        }
    }

    private interface SheetColumn {
        String getHeader();

        void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record);
    }

    private interface RecordProvider {
        List<PO> fetch(ExportSubmittedWspAtrToXlsm process, TabRuntimeContext runtimeContext);
    }

    private interface SyntheticValueProvider {
        Object getValue(PO record);
    }

    private static final class ExportTabDefinition {
        private final String windowUu;
        private final String tabUu;
        private final String sheetName;
        private final RecordProvider recordProvider;
        private final List<SyntheticColumn> syntheticColumns;
        private final Set<String> mandatoryTableColumns;

        private ExportTabDefinition(String windowUu, String tabUu, String sheetName, RecordProvider recordProvider,
                List<SyntheticColumn> syntheticColumns, Set<String> mandatoryTableColumns) {
            this.windowUu = windowUu;
            this.tabUu = tabUu;
            this.sheetName = sheetName;
            this.recordProvider = recordProvider;
            this.syntheticColumns = syntheticColumns;
            this.mandatoryTableColumns = mandatoryTableColumns;
        }

        private static ExportTabDefinition levelZeroSubmittedTab() {
            return new ExportTabDefinition(
                    SUBMITTED_WINDOW_UU,
                    SUBMITTED_TAB_UU,
                    I_ZZ_WSP_ATR_Submitted.Table_Name,
                    (process, runtimeContext) -> new Query(process.getCtx(), runtimeContext.table.getTableName(), null,
                            process.get_TrxName())
                                    .setOrderBy(runtimeContext.table.getTableName() + "_ID")
                                    .list(),
                    List.of(new SyntheticColumn(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo,
                            record -> record.get_Value(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo))),
                    new LinkedHashSet<>(List.of(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo)));
        }
    }

    private static final class TabRuntimeContext {
        private final int adTabId;
        private final MTable table;

        private TabRuntimeContext(int adTabId, MTable table) {
            this.adTabId = adTabId;
            this.table = table;
        }
    }

    private static final class TableColumn implements SheetColumn {
        private final MColumn column;

        private TableColumn(MColumn column) {
            this.column = column;
        }

        @Override
        public String getHeader() {
            return column.getColumnName();
        }

        @Override
        public void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record) {
            process.writeResolvedValue(cell, column, record.get_Value(column.getColumnName()));
        }
    }

    private static final class SyntheticColumn implements SheetColumn {
        private final String header;
        private final SyntheticValueProvider valueProvider;

        private SyntheticColumn(String header, SyntheticValueProvider valueProvider) {
            this.header = header;
            this.valueProvider = valueProvider;
        }

        @Override
        public String getHeader() {
            return header;
        }

        @Override
        public void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record) {
            process.writeResolvedValue(cell, null, valueProvider.getValue(record));
        }
    }
}
