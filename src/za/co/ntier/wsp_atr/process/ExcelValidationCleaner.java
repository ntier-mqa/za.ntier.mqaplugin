package za.co.ntier.wsp_atr.process;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class ExcelValidationCleaner {

    public static final String ERRORS_SHEET_NAME = "Errors";
    public static final String COMMENT_PREFIX = "WSPATR_VALIDATION|";

    public void resetWorkbookBeforeValidation(Workbook wb) {
        removeErrorsSheet(wb);
        clearAllValidationMarkers(wb);
    }

    private void removeErrorsSheet(Workbook wb) {
        int idx = wb.getSheetIndex(ERRORS_SHEET_NAME);
        if (idx >= 0) {
            wb.removeSheetAt(idx);
        }
    }

    private void clearAllValidationMarkers(Workbook wb) {
        if (!(wb instanceof XSSFWorkbook)) return;

        XSSFWorkbook xwb = (XSSFWorkbook) wb;

        for (int s = 0; s < xwb.getNumberOfSheets(); s++) {
            Sheet sheet = xwb.getSheetAt(s);

            // Skip the Errors sheet if it exists for some reason
            if (ERRORS_SHEET_NAME.equalsIgnoreCase(sheet.getSheetName())) {
                continue;
            }

            clearMarkersOnSheet(xwb, sheet);
        }
    }

    private void clearMarkersOnSheet(XSSFWorkbook wb, Sheet sheet) {
        if (!(sheet instanceof XSSFSheet)) return;

        for (Row row : sheet) {
            if (row == null) continue;

            for (Cell cell : row) {
                if (cell == null) continue;

                Comment c = cell.getCellComment();
                if (c == null) continue;

                String txt = c.getString() != null ? c.getString().getString() : null;
                if (txt == null || !txt.startsWith(COMMENT_PREFIX)) continue;

                // restore style if embedded
                short origStyleIdx = parseOrigStyleIndex(txt);
                if (origStyleIdx >= 0 && origStyleIdx < wb.getNumCellStyles()) {
                    CellStyle original = wb.getCellStyleAt(origStyleIdx);
                    if (original != null) {
                        cell.setCellStyle(original);
                    }
                }

                // remove comment
                cell.removeCellComment();
            }
        }
    }

    // COMMENT_PREFIX + "origStyle=12\nInvalid:\n..."
    private short parseOrigStyleIndex(String commentText) {
        try {
            int p = commentText.indexOf("origStyle=");
            if (p < 0) return -1;
            int start = p + "origStyle=".length();

            int end = commentText.indexOf('\n', start);
            String num = (end > start) ? commentText.substring(start, end) : commentText.substring(start);

            return Short.parseShort(num.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}