package za.co.ntier.wsp_atr.process;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.compiere.util.Util;

public class ExcelErrorLogSheet {

    public static final String SHEET_NAME = "Errors";

    private static final int COL_TAB     = 0;
    private static final int COL_HEADER  = 1;
    private static final int COL_ROW     = 2;
    private static final int COL_COL     = 3;
    private static final int COL_LINK    = 4;
    private static final int COL_MESSAGE = 5;

    // Use WeakHashMap so workbooks can still be GC'd
    private static final Map<Workbook, CellStyle> HEADER_STYLE_CACHE = new WeakHashMap<>();
    private static final Map<Workbook, CellStyle> LINK_STYLE_CACHE   = new WeakHashMap<>();

    public Sheet getOrCreateErrorsSheet(Workbook wb) {
        Sheet s = wb.getSheet(SHEET_NAME);
        if (s == null) {
            s = wb.createSheet(SHEET_NAME);
            createHeaderRow(s, wb);
        } else if (s.getPhysicalNumberOfRows() == 0) {
            createHeaderRow(s, wb);
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

        CellStyle bold = getOrCreateHeaderStyle(wb);
        for (int i = 0; i <= COL_MESSAGE; i++) {
            hr.getCell(i).setCellStyle(bold);
        }

        s.setColumnWidth(COL_TAB, 18 * 256);
        s.setColumnWidth(COL_HEADER, 28 * 256);
        s.setColumnWidth(COL_ROW, 10 * 256);
        s.setColumnWidth(COL_COL, 10 * 256);
        s.setColumnWidth(COL_LINK, 30 * 256);
        s.setColumnWidth(COL_MESSAGE, 60 * 256);
    }

    public void appendError(Workbook wb,
                            String tabName,
                            String headerName,
                            int sheetRowIndex0,
                            int sheetColIndex0,
                            String message) {

        Sheet errSheet = getOrCreateErrorsSheet(wb);
        int nextRow = Math.max(errSheet.getLastRowNum() + 1, 1);

        Row r = errSheet.createRow(nextRow);

        String safeTab = Util.isEmpty(tabName, true) ? "" : tabName;
        String safeHeader = Util.isEmpty(headerName, true) ? "" : headerName;
        String safeMessage = Util.isEmpty(message, true) ? "" : message;

        int excelRow1 = sheetRowIndex0 + 1;
        String excelColLetter = CellReference.convertNumToColString(sheetColIndex0);

        r.createCell(COL_TAB).setCellValue(safeTab);
        r.createCell(COL_HEADER).setCellValue(safeHeader);
        r.createCell(COL_ROW).setCellValue(excelRow1);
        r.createCell(COL_COL).setCellValue(excelColLetter);

        Cell linkCell = r.createCell(COL_LINK);
        Sheet target = wb.getSheet(safeTab);

        if (target == null) {
            linkCell.setCellValue(excelColLetter + excelRow1);
        } else {
            String realName = target.getSheetName();
            String addr = "'" + realName.replace("'", "''") + "'!" + excelColLetter + excelRow1;

            linkCell.setCellValue(addr);

            CreationHelper ch = wb.getCreationHelper();
            Hyperlink link = ch.createHyperlink(HyperlinkType.DOCUMENT);
            link.setAddress(addr);

            linkCell.setHyperlink(link);
            linkCell.setCellStyle(getOrCreateLinkStyle(wb));
        }

        r.createCell(COL_MESSAGE).setCellValue(safeMessage);
    }

    private CellStyle getOrCreateHeaderStyle(Workbook wb) {
        CellStyle cached = HEADER_STYLE_CACHE.get(wb);
        if (cached != null) {
            return cached;
        }

        Font f = wb.createFont();
        f.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFont(f);

        HEADER_STYLE_CACHE.put(wb, style);
        return style;
    }

    private CellStyle getOrCreateLinkStyle(Workbook wb) {
        CellStyle cached = LINK_STYLE_CACHE.get(wb);
        if (cached != null) {
            return cached;
        }

        Font hf = wb.createFont();
        hf.setUnderline(Font.U_SINGLE);
        hf.setColor(org.apache.poi.ss.usermodel.IndexedColors.BLUE.getIndex());

        CellStyle style = wb.createCellStyle();
        style.setFont(hf);

        LINK_STYLE_CACHE.put(wb, style);
        return style;
    }
    
    public void appendTooManyErrors(Workbook wb) {

        Sheet errSheet = getOrCreateErrorsSheet(wb);
        int nextRow = Math.max(errSheet.getLastRowNum() + 1, 1);

        Row r = errSheet.createRow(nextRow);

        r.createCell(COL_TAB).setCellValue("");
        r.createCell(COL_HEADER).setCellValue("");
        r.createCell(COL_ROW).setCellValue("");
        r.createCell(COL_COL).setCellValue("");
        r.createCell(COL_LINK).setCellValue("");

        r.createCell(COL_MESSAGE).setCellValue(
            "Too many errors found. Validation stopped. Please correct the errors listed above and try again."
        );
    }
}