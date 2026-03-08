package za.co.ntier.wsp_atr.process;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelErrorMarker {

    // Must be HashMap, not IdentityHashMap, because key is boxed Short value
    private final Map<Short, CellStyle> errorStylesByOriginal = new HashMap<>();

    // OK to keep identity-based per actual sheet instance
    private final Map<XSSFSheet, Drawing<?>> drawingCache = new IdentityHashMap<>();

    public void markError(Workbook wb, Sheet sheet, Row row, int colIndex, String message) {
        if (row == null) {
            return;
        }

        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }

        short origStyleIdx = cell.getCellStyle() != null ? cell.getCellStyle().getIndex() : 0;
        cell.setCellStyle(getOrCreateErrorStyle(wb, origStyleIdx));

        addOrReplaceComment(
            wb,
            sheet,
            cell,
            ExcelValidationCleaner.COMMENT_PREFIX
                + "origStyle=" + origStyleIdx + "\n"
                + "Invalid:\n" + message
        );
    }

    private CellStyle getOrCreateErrorStyle(Workbook wb, short originalStyleIdx) {
        CellStyle cached = errorStylesByOriginal.get(originalStyleIdx);
        if (cached != null) {
            return cached;
        }

        CellStyle base = wb.getCellStyleAt(originalStyleIdx);

        CellStyle cs = wb.createCellStyle();
        if (base != null) {
            cs.cloneStyleFrom(base);
        }

        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setFillForegroundColor(IndexedColors.ROSE.getIndex());

        Font baseFont = null;
        if (base != null) {
            baseFont = wb.getFontAt(base.getFontIndexAsInt());
        }

               
        Font f = wb.createFont();
        if (baseFont != null) {
            f.setBold(baseFont.getBold());
            f.setItalic(baseFont.getItalic());
            f.setStrikeout(baseFont.getStrikeout());
            f.setFontHeight(baseFont.getFontHeight());
            f.setFontName(baseFont.getFontName());
            f.setUnderline(baseFont.getUnderline());
            f.setTypeOffset(baseFont.getTypeOffset());
            f.setCharSet(baseFont.getCharSet());
        }
        f.setColor(IndexedColors.RED.getIndex());
        cs.setFont(f);

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

        Drawing<?> drawing = drawingCache.computeIfAbsent(xsheet, s -> s.createDrawingPatriarch());

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