package za.co.ntier.wsp_atr.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class ExcelErrorMarker {

    // cache: originalStyleIndex -> derivedErrorStyle
    private final Map<Short, CellStyle> errorStylesByOriginal = new HashMap<>();

    public void markError(Workbook wb, Sheet sheet, Row row, int colIndex, String message) {
        if (row == null) return;

        Cell cell = row.getCell(colIndex);
        if (cell == null) cell = row.createCell(colIndex);

        short origStyleIdx = cell.getCellStyle() != null ? cell.getCellStyle().getIndex() : 0;

        CellStyle style = getOrCreateErrorStyle(wb, origStyleIdx);
        cell.setCellStyle(style);

        addOrReplaceComment(wb, sheet, cell,
                ExcelValidationCleaner.COMMENT_PREFIX
                        + "origStyle=" + origStyleIdx + "\n"
                        + "Invalid:\n" + message);
    }

    private CellStyle getOrCreateErrorStyle(Workbook wb, short originalStyleIdx) {
        CellStyle cached = errorStylesByOriginal.get(originalStyleIdx);
        if (cached != null) return cached;

        CellStyle base = wb.getCellStyleAt(originalStyleIdx);
        CellStyle cs = wb.createCellStyle();
        if (base != null) {
            cs.cloneStyleFrom(base);
        }

        // Fill light red
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setFillForegroundColor(IndexedColors.ROSE.getIndex());

        // Red font (clone base font if possible)
        Font baseFont = (base != null) ? wb.getFontAt(base.getFontIndexAsInt()) : null;
        Font f = wb.createFont();
        if (baseFont != null) {
            f.setBold(baseFont.getBold());
            f.setItalic(baseFont.getItalic());
            f.setFontHeight(baseFont.getFontHeight());
            f.setFontName(baseFont.getFontName());
            f.setUnderline(baseFont.getUnderline());
        }
        f.setColor(IndexedColors.RED.getIndex());
        cs.setFont(f);

        // Border
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN);
        cs.setBorderRight(BorderStyle.THIN);

        errorStylesByOriginal.put(originalStyleIdx, cs);
        return cs;
    }

    private void addOrReplaceComment(Workbook wb, Sheet sheet, Cell cell, String text) {
        if (!(wb instanceof XSSFWorkbook) || !(sheet instanceof XSSFSheet) || !(cell instanceof XSSFCell)) {
            return;
        }

        XSSFCell xcell = (XSSFCell) cell;
        XSSFSheet xsheet = (XSSFSheet) sheet;

        CreationHelper factory = wb.getCreationHelper();

        Comment existing = xcell.getCellComment();
        if (existing != null) {
            existing.setString(factory.createRichTextString(text));
            existing.setAuthor("WSP/ATR Validator");
            return;
        }

        Drawing<?> drawing = xsheet.createDrawingPatriarch();

        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 4);

        Comment comment = drawing.createCellComment(anchor);
        comment.setString(factory.createRichTextString(text));
        comment.setAuthor("WSP/ATR Validator");

        xcell.setCellComment(comment);
    }
}