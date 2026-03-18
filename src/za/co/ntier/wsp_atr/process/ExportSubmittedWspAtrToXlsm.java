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
import java.util.List;
import java.util.Properties;

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
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

@Process(name = "za.co.ntier.wsp_atr.process.ExportSubmittedWspAtrToXlsm")
public class ExportSubmittedWspAtrToXlsm extends SvrProcess {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    protected void prepare() {
    }

    @Override
    protected String doIt() throws Exception {
        MTable submittedTable = MTable.get(getCtx(), I_ZZ_WSP_ATR_Submitted.Table_Name);
        if (submittedTable == null || submittedTable.getAD_Table_ID() <= 0) {
            throw new IllegalStateException("Unable to resolve table " + I_ZZ_WSP_ATR_Submitted.Table_Name);
        }

        List<MColumn> columns = getExportColumns(submittedTable.getAD_Table_ID());
        if (columns.isEmpty()) {
            throw new IllegalStateException("No active columns found for " + I_ZZ_WSP_ATR_Submitted.Table_Name);
        }

        List<PO> submittedRows = new Query(getCtx(), I_ZZ_WSP_ATR_Submitted.Table_Name, null, get_TrxName())
                .setOrderBy(X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_WSP_ATR_Submitted_ID)
                .list();

        Path exportPath = uniqueTempXlsm("ZZ_WSP_ATR_Submitted_Export");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("ZZ_WSP_ATR_Submitted");
            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.size(); col++) {
                Cell headerCell = headerRow.createCell(col);
                headerCell.setCellValue(columns.get(col).getColumnName());
                headerCell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PO submittedRow : submittedRows) {
                X_ZZ_WSP_ATR_Submitted submitted = (X_ZZ_WSP_ATR_Submitted) submittedRow;
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < columns.size(); col++) {
                    MColumn column = columns.get(col);
                    Cell cell = row.createCell(col);
                    writeCellValue(cell, column, submitted);
                }
            }

            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            try (OutputStream os = Files.newOutputStream(exportPath)) {
                workbook.write(os);
            }
        }

        if (processUI != null) {
            processUI.download(exportPath.toFile());
        }

        return "Exported " + submittedRows.size() + " records to " + exportPath.getFileName();
    }

    private List<MColumn> getExportColumns(int adTableId) {
        List<MColumn> columns = new ArrayList<>();
        List<PO> columnRows = new Query(getCtx(), MColumn.Table_Name,
                MColumn.COLUMNNAME_AD_Table_ID + "=? AND " + MColumn.COLUMNNAME_IsActive + "='Y'", get_TrxName())
                .setParameters(adTableId)
                .setOrderBy(MColumn.COLUMNNAME_AD_Column_ID + ", " + MColumn.COLUMNNAME_AD_Column_ID)
                .list();

        for (PO columnRow : columnRows) {
            MColumn column = (MColumn) columnRow;
            if (column.isVirtualColumn()) {
                continue;
            }
            columns.add(column);
        }
        return columns;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private void writeCellValue(Cell cell, MColumn column, PO record) {
        Object value = record.get_Value(column.getColumnName());
        if (value == null) {
            cell.setCellValue("");
            return;
        }

        int displayType = column.getAD_Reference_ID();
        if (value instanceof Timestamp) {
            cell.setCellValue(formatTimestamp((Timestamp) value));
            return;
        }

        if (isReferenceColumn(column)) {
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
}
