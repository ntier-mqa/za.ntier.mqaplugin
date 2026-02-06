package za.co.ntier.wsp_atr.process;


import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.compiere.util.Util;

public class ExcelErrorLogSheet {

    public static final String SHEET_NAME = "Errors";

    // Columns in the Errors sheet
    private static final int COL_TAB      = 0;
    private static final int COL_HEADER   = 1; // mapping "Header Name"
    private static final int COL_ROW      = 2; // 1-based
    private static final int COL_COL      = 3; // Excel column letter
    private static final int COL_LINK     = 4; // hyperlink to the bad cell
    private static final int COL_MESSAGE  = 5;

    public Sheet getOrCreateErrorsSheet(Workbook wb) {
        Sheet s = wb.getSheet(SHEET_NAME);
        if (s == null) {
            s = wb.createSheet(SHEET_NAME);
            createHeaderRow(s, wb);
        } else {
            // ensure header exists (optional safety)
            if (s.getPhysicalNumberOfRows() == 0) {
                createHeaderRow(s, wb);
            }
        }
        return s;
    }

    private void createHeaderRow(Sheet s, Workbook wb) {
        Row hr = s.createRow(0);

        hr.createCell(COL_TAB).setCellValue("Tab Name");
        hr.createCell(COL_HEADER).setCellValue("Column Name");
        hr.createCell(COL_ROW).setCellValue("Row");
        hr.createCell(COL_COL).setCellValue("Column");
        hr.createCell(COL_LINK).setCellValue("Link");
        hr.createCell(COL_MESSAGE).setCellValue("Message");

        // Basic bold style
        Font f = wb.createFont();
        f.setBold(true);
        CellStyle bold = wb.createCellStyle();
        bold.setFont(f);

        for (int i = 0; i <= COL_MESSAGE; i++) {
            hr.getCell(i).setCellStyle(bold);
        }

        // Optional: widen columns a bit
        s.setColumnWidth(COL_TAB,  18 * 256);
        s.setColumnWidth(COL_HEADER, 28 * 256);
        s.setColumnWidth(COL_ROW,  10 * 256);
        s.setColumnWidth(COL_COL,  10 * 256);
        s.setColumnWidth(COL_LINK, 30 * 256);
        s.setColumnWidth(COL_MESSAGE, 60 * 256);
    }

    /** Append one error row with hyperlink to the actual sheet cell. */
    public void appendError(Workbook wb,
                            String tabName,
                            String headerName,
                            int sheetRowIndex0,
                            int sheetColIndex0,
                            String message) {

        Sheet errSheet = getOrCreateErrorsSheet(wb);
        int nextRow = errSheet.getLastRowNum() + 1;
        if (nextRow == 0) nextRow = 1; // if only header existed and lastRowNum=0, next is 1

        Row r = errSheet.createRow(nextRow);

        r.createCell(COL_TAB).setCellValue(Util.isEmpty(tabName, true) ? "" : tabName);
        r.createCell(COL_HEADER).setCellValue(Util.isEmpty(headerName, true) ? "" : headerName);

        int excelRow1 = sheetRowIndex0 + 1; // 1-based for user display
        String excelColLetter = CellReference.convertNumToColString(sheetColIndex0);

        r.createCell(COL_ROW).setCellValue(excelRow1);
        r.createCell(COL_COL).setCellValue(excelColLetter);

        // Hyperlink to the exact cell (internal link)
        String addr = "'" + tabName.replace("'", "''") + "'!" + excelColLetter + excelRow1;

        Cell linkCell = r.createCell(COL_LINK);
        linkCell.setCellValue(addr);

        CreationHelper ch = wb.getCreationHelper();
        Hyperlink link = ch.createHyperlink(HyperlinkType.DOCUMENT);
        link.setAddress(addr);
        linkCell.setHyperlink(link);

        // hyperlink style
        CellStyle hstyle = wb.createCellStyle();
        Font hf = wb.createFont();
        hf.setUnderline(Font.U_SINGLE);
        hf.setColor(IndexedColors.BLUE.getIndex());
        hstyle.setFont(hf);
        linkCell.setCellStyle(hstyle);

        r.createCell(COL_MESSAGE).setCellValue(Util.isEmpty(message, true) ? "" : message);
    }
}

