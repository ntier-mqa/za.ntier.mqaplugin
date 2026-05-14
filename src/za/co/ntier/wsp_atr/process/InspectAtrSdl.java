package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Focused diagnostic for a single SDL on the ATR tab. Dumps every Excel row
 * and every DB row containing Total_Training_Cost so we can see exactly why
 * Excel-side and DB-side sums differ.
 *
 * Output: one workbook with three sheets:
 *   - Summary    : row counts and strict/lenient sums for both sides
 *   - Excel Rows : sheet name, row index, raw text, parsed numeric, regex pass
 *   - DB Rows    : detail PK, raw text, parsed numeric, strict-regex pass
 *
 * The strict regex matches what ReconcileWspAtrImport's DB SUM uses
 * (^-?[0-9]+(\.[0-9]+)?$). The "parsed" column applies the lenient
 * Excel-side cleanup (strip leading apostrophe, currency symbols, commas,
 * spaces) before parsing — what the reconciler uses on the spreadsheet.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.InspectAtrSdl")
public class InspectAtrSdl extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final String DEFAULT_SDL      = "L010712109";
    private static final String ATR_TAB_NAME     = "ATR";       // mapping name
    private static final String NUMERIC_COL      = "Total_Training_Cost";
    private static final String SDL_HEADER       = "SDLNumber";
    private static final int    DEFAULT_START_ROW = 4;
    private static final int    MAX_EMPTY        = 10;
    private static final String STRICT_REGEX     = "^-?[0-9]+(\\.[0-9]+)?$";

    private String sdlParam = DEFAULT_SDL;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter p : getParameter()) {
            String name = p.getParameterName();
            if ("SDL_Number".equalsIgnoreCase(name) || "ZZ_SDL_Number".equalsIgnoreCase(name)) {
                if (p.getParameter() != null && !p.getParameter().toString().trim().isEmpty()) {
                    sdlParam = p.getParameter().toString().trim();
                }
            } else {
                org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile())
            throw new AdempiereException("File not found: " + BULK_UPLOAD_PATH);

        addLog("Inspecting SDL " + sdlParam + " in " + BULK_UPLOAD_PATH);

        // ---- Resolve ATR column layout from mapping detail (matches importer & reconciler) ----
        Layout layout = resolveLayout();
        if (layout.sdlColIdx < 0)
            throw new AdempiereException("Could not resolve SDLNumber column letter for ATR tab.");
        if (layout.numericColIdx < 0)
            throw new AdempiereException("Could not resolve " + NUMERIC_COL + " column letter for ATR tab.");
        addLog("ATR layout: SDLNumber=" + indexToLetter(layout.sdlColIdx)
                + ", " + NUMERIC_COL + "=" + indexToLetter(layout.numericColIdx)
                + ", startRow=" + layout.startRow);

        // ---- Read Excel rows for this SDL ----
        List<ExcelRow> excelRows = new ArrayList<>();
        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {
            List<String> sheets = reader.findMatchingSheets(ATR_TAB_NAME);
            if (sheets.isEmpty())
                throw new AdempiereException("No sheet matched mapping name '" + ATR_TAB_NAME + "'.");
            final int dataStartIdx = Math.max(0, layout.startRow);
            for (String sheetName : sheets) {
                final int[] emptyStreak = {0};
                reader.streamSheet(sheetName, dataStartIdx, null, (rowIdx, cells) -> {
                    if (cells.isEmpty()) {
                        if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                    emptyStreak[0] = 0;
                    String rowSdl = cells.getOrDefault(layout.sdlColIdx, "").trim();
                    if (sdlParam.equalsIgnoreCase(rowSdl)) {
                        ExcelRow er = new ExcelRow();
                        er.sheet  = sheetName;
                        er.rowIdx = rowIdx + 1; // 1-based for humans
                        er.raw    = cells.getOrDefault(layout.numericColIdx, "");
                        er.parsed = parseNumber(er.raw);
                        er.strictMatch = strictMatch(er.raw);
                        excelRows.add(er);
                    }
                    return StreamingXlsxReader.Action.CONTINUE;
                });
            }
        }

        // ---- Read DB rows for this SDL ----
        List<DbRow> dbRows = queryDb(sdlParam);

        // ---- Build diagnostic workbook ----
        File out = buildReport(layout, excelRows, dbRows);
        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(out);

        return "Inspected SDL " + sdlParam + ": " + excelRows.size() + " Excel row(s), "
                + dbRows.size() + " DB row(s).";
    }

    // -------------------------------------------------------------------------  layout

    private static class Layout {
        int sdlColIdx     = -1;
        int numericColIdx = -1;
        int startRow      = DEFAULT_START_ROW;
    }

    private Layout resolveLayout() {
        Layout L = new Layout();
        // Pull Start_Row from the mapping (defaults to DEFAULT_START_ROW if missing).
        int sr = DB.getSQLValueEx(null,
                "SELECT COALESCE(Start_Row, 0) FROM ZZ_WSP_ATR_Lookup_Mapping " +
                " WHERE ZZ_Tab_Name=? AND IsActive='Y' FETCH FIRST 1 ROWS ONLY",
                ATR_TAB_NAME);
        if (sr > 0) L.startRow = sr;

        String sql =
            "SELECT d.ZZ_Column_Letter, d.ZZ_Header_Name, c.ColumnName " +
            "  FROM ZZ_WSP_ATR_Lookup_Mapping_Detail d " +
            "  JOIN ZZ_WSP_ATR_Lookup_Mapping m " +
            "    ON m.ZZ_WSP_ATR_Lookup_Mapping_ID = d.ZZ_WSP_ATR_Lookup_Mapping_ID " +
            "  LEFT JOIN AD_Column c ON c.AD_Column_ID = d.AD_Column_ID " +
            " WHERE m.ZZ_Tab_Name = ? AND d.IsActive='Y'";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            pst.setString(1, ATR_TAB_NAME);
            rs = pst.executeQuery();
            while (rs.next()) {
                String letter   = rs.getString(1);
                String hdrName  = rs.getString(2);
                String colName  = rs.getString(3);
                int idx = columnLetterToIndex(letter);
                if (idx < 0) continue;
                if (SDL_HEADER.equalsIgnoreCase(hdrName) || SDL_HEADER.equalsIgnoreCase(colName))
                    L.sdlColIdx = idx;
                if (NUMERIC_COL.equalsIgnoreCase(hdrName) || NUMERIC_COL.equalsIgnoreCase(colName))
                    L.numericColIdx = idx;
            }
        } catch (Exception e) {
            log.warning("Layout resolve failed: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return L;
    }

    // -------------------------------------------------------------------------  db read

    private static class DbRow {
        int    id;
        String raw;
        double parsed;
        boolean strictMatch;
    }

    private List<DbRow> queryDb(String sdl) {
        List<DbRow> out = new ArrayList<>();
        String sql =
            "SELECT c.ZZ_WSP_ATR_ATR_Detail_ID, c.Total_Training_Cost::text AS raw" +
            "  FROM ZZ_WSP_ATR_ATR_Detail c" +
            "  JOIN ZZ_WSP_ATR_Submitted s ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID" +
            "  JOIN ZZSdfOrganisation  org ON org.ZZSdfOrganisation_ID = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner          bp ON bp.C_BPartner_ID          = org.C_BPartner_ID" +
            " WHERE bp.Value = ?" +
            " ORDER BY c.ZZ_WSP_ATR_ATR_Detail_ID";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            pst.setString(1, sdl);
            rs = pst.executeQuery();
            while (rs.next()) {
                DbRow d = new DbRow();
                d.id     = rs.getInt(1);
                d.raw    = rs.getString("raw");
                d.parsed = parseNumber(d.raw);
                d.strictMatch = strictMatch(d.raw);
                out.add(d);
            }
        } catch (Exception e) {
            log.warning("DB read failed: " + e.getMessage());
            addLog("DB read failed: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    // -------------------------------------------------------------------------  report

    private static class ExcelRow {
        String  sheet;
        int     rowIdx;
        String  raw;
        double  parsed;
        boolean strictMatch;
    }

    private File buildReport(Layout layout, List<ExcelRow> excelRows, List<DbRow> dbRows) throws Exception {
        File tmp = File.createTempFile("InspectAtrSdl_" + sdlParam + "_", ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            CellStyle hdr  = headerStyle(wb);
            CellStyle good = fillStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle bad  = fillStyle(wb, IndexedColors.ROSE.getIndex());

            writeSummary(wb.createSheet("Summary"),    excelRows, dbRows, hdr, good, bad);
            writeExcel  (wb.createSheet("Excel Rows"), excelRows, hdr, bad);
            writeDb     (wb.createSheet("DB Rows"),    dbRows,    hdr, bad);

            wb.write(fos);
        }
        return tmp;
    }

    private void writeSummary(Sheet sh, List<ExcelRow> e, List<DbRow> d,
                              CellStyle hdr, CellStyle good, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"Metric", "Excel", "DB", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        long eRows = e.size();
        long dRows = d.size();
        long eStrictRows = e.stream().filter(x -> x.strictMatch).count();
        long dStrictRows = d.stream().filter(x -> x.strictMatch).count();
        double eStrictSum  = e.stream().filter(x -> x.strictMatch).mapToDouble(x -> x.parsed).sum();
        double dStrictSum  = d.stream().filter(x -> x.strictMatch).mapToDouble(x -> x.parsed).sum();
        double eLenientSum = e.stream().mapToDouble(x -> x.parsed).sum();
        double dLenientSum = d.stream().mapToDouble(x -> x.parsed).sum();
        long eDirty = eRows - eStrictRows;
        long dDirty = dRows - dStrictRows;

        int r = 1;
        addSummaryRow(sh, r++, "SDL Number",                       sdlParam,                  sdlParam,                  "",  null);
        addSummaryRow(sh, r++, "Total rows",                       String.valueOf(eRows),     String.valueOf(dRows),     String.valueOf(eRows - dRows), (eRows == dRows) ? good : bad);
        addSummaryRow(sh, r++, "Rows matching strict numeric regex", String.valueOf(eStrictRows), String.valueOf(dStrictRows), String.valueOf(eStrictRows - dStrictRows), (eStrictRows == dStrictRows) ? good : bad);
        addSummaryRow(sh, r++, "Rows NOT matching strict regex (dirty)", String.valueOf(eDirty), String.valueOf(dDirty), String.valueOf(eDirty - dDirty), (eDirty == 0 && dDirty == 0) ? good : bad);
        addSummaryRow(sh, r++, "SUM (strict only)",                fmt(eStrictSum),           fmt(dStrictSum),           fmt(eStrictSum - dStrictSum), (Math.abs(eStrictSum - dStrictSum) < 0.01) ? good : bad);
        addSummaryRow(sh, r++, "SUM (lenient: strip R/$/,/space/')", fmt(eLenientSum),         fmt(dLenientSum),          fmt(eLenientSum - dLenientSum), (Math.abs(eLenientSum - dLenientSum) < 0.01) ? good : bad);

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    private void addSummaryRow(Sheet sh, int r, String metric, String e, String d, String diff, CellStyle style) {
        Row row = sh.createRow(r);
        cell(row, 0, metric, style);
        cell(row, 1, e,      style);
        cell(row, 2, d,      style);
        cell(row, 3, diff,   style);
    }

    private void writeExcel(Sheet sh, List<ExcelRow> rows, CellStyle hdr, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"Sheet", "Excel Row #", "Raw Value", "Parsed (lenient)", "Strict Regex?"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (ExcelRow er : rows) {
            CellStyle s = er.strictMatch ? null : bad;
            Row row = sh.createRow(r++);
            cell   (row, 0, er.sheet, s);
            cell   (row, 1, String.valueOf(er.rowIdx), s);
            cell   (row, 2, er.raw, s);
            numCell(row, 3, er.parsed, s);
            cell   (row, 4, er.strictMatch ? "yes" : "NO", s);
        }
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    private void writeDb(Sheet sh, List<DbRow> rows, CellStyle hdr, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"Detail ID", "Raw Value", "Parsed (lenient)", "Strict Regex?"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (DbRow d : rows) {
            CellStyle s = d.strictMatch ? null : bad;
            Row row = sh.createRow(r++);
            cell   (row, 0, String.valueOf(d.id), s);
            cell   (row, 1, d.raw, s);
            numCell(row, 2, d.parsed, s);
            cell   (row, 3, d.strictMatch ? "yes" : "NO", s);
        }
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    // -------------------------------------------------------------------------  helpers

    /** Strict: matches what the reconciler's DB SUM accepts. */
    private static boolean strictMatch(String s) {
        if (s == null) return false;
        return s.trim().matches(STRICT_REGEX);
    }

    /** Lenient parse, matches WspAtrImportUtil.parseBigDecimal: handles US and European decimals. */
    private static double parseNumber(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        if (t.charAt(0) == '\'') t = t.substring(1).trim();
        t = t.replace("R", "").replace("$", "")
             .replace(" ", "").replace("\u00A0", "");
        if (t.isEmpty()) return 0;

        int lastDot   = t.lastIndexOf('.');
        int lastComma = t.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {
                t = t.replace(".", "");
                t = t.replace(',', '.');
            } else {
                t = t.replace(",", "");
            }
        } else if (lastComma >= 0) {
            if (t.matches("-?\\d{1,3}(,\\d{3})+")) {
                t = t.replace(",", "");
            } else {
                t = t.replace(',', '.');
            }
        }

        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return 0; }
    }

    private static int columnLetterToIndex(String letter) {
        if (letter == null) return -1;
        String s = letter.trim().toUpperCase();
        if (s.isEmpty()) return -1;
        int idx = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 'A' || c > 'Z') return -1;
            idx = idx * 26 + (c - 'A' + 1);
        }
        return idx - 1;
    }

    private static String indexToLetter(int idx) {
        if (idx < 0) return "(unmapped)";
        StringBuilder sb = new StringBuilder();
        int n = idx + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    // -------------------------------------------------------------------------  cell helpers

    private void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private void numCell(Row row, int col, double value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        border(s);
        return s;
    }

    private CellStyle fillStyle(XSSFWorkbook wb, short colorIdx) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(colorIdx);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        border(s);
        return s;
    }

    private void border(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }
}
