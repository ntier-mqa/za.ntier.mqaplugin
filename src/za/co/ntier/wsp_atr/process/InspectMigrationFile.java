package za.co.ntier.wsp_atr.process;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Util;

@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.InspectMigrationFile")
public class InspectMigrationFile extends SvrProcess {

    private static final String EXCEL_PASSWORD = "Learning2026";
    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final int MAX_CONSECUTIVE_EMPTY = 10;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found: " + BULK_UPLOAD_PATH);
        }

        DataFormatter formatter = new DataFormatter();
        int totalTabs = 0;

        try (Workbook workbook = openWorkbook(file)) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String tabName = sheet.getSheetName();

                int headerCount = countHeaders(sheet, formatter);
                int recordCount = countRecords(sheet, formatter);

                addLog("Tab: " + tabName
                        + "  |  Headers: " + headerCount
                        + "  |  Records: " + recordCount);
                totalTabs++;
            }
        }

        return "Inspected " + totalTabs + " tab(s).";
    }

    /** Count non-empty cells in the first row (row index 0). */
    private int countHeaders(Sheet sheet, DataFormatter formatter) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return 0;
        }
        int count = 0;
        for (int c = headerRow.getFirstCellNum(); c <= headerRow.getLastCellNum(); c++) {
            if (!Util.isEmpty(getCellText(headerRow, c, formatter), true)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count non-empty data rows starting from row index 1.
     * Stops after MAX_CONSECUTIVE_EMPTY consecutive empty/null rows.
     */
    private int countRecords(Sheet sheet, DataFormatter formatter) {
        int recordCount = 0;
        int consecutiveEmpty = 0;

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row, formatter)) {
                consecutiveEmpty++;
                if (consecutiveEmpty > MAX_CONSECUTIVE_EMPTY) {
                    break;
                }
            } else {
                consecutiveEmpty = 0;
                recordCount++;
            }
        }
        return recordCount;
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); c++) {
            if (!Util.isEmpty(getCellText(row, c, formatter), true)) {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Row row, int colIndex, DataFormatter formatter) {
        if (row == null || colIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return formatter.formatCellValue(cell, row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator());
            } catch (Exception e) {
                return "";
            }
        }
        return formatter.formatCellValue(cell);
    }

    private Workbook openWorkbook(File file) throws Exception {
        // Try streaming directly first — avoids loading the whole file into memory.
        try (InputStream is = new FileInputStream(file)) {
            return WorkbookFactory.create(is);
        } catch (EncryptedDocumentException e) {
            // Encrypted workbook: must buffer so we can decrypt, then stream the result.
            IOUtils.setByteArrayMaxOverride(600 * 1024 * 1024);
            byte[] data;
            try (InputStream is = new FileInputStream(file)) {
                data = org.apache.commons.io.IOUtils.toByteArray(is);
            }
            try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs =
                    new org.apache.poi.poifs.filesystem.POIFSFileSystem(new ByteArrayInputStream(data))) {

                org.apache.poi.poifs.crypt.EncryptionInfo info = new org.apache.poi.poifs.crypt.EncryptionInfo(fs);
                org.apache.poi.poifs.crypt.Decryptor decryptor = org.apache.poi.poifs.crypt.Decryptor.getInstance(info);

                if (!decryptor.verifyPassword(EXCEL_PASSWORD)) {
                    throw new AdempiereException("Invalid Excel password for workbook");
                }

                try (InputStream decryptedStream = decryptor.getDataStream(fs)) {
                    return WorkbookFactory.create(decryptedStream);
                }
            }
        }
    }
}
