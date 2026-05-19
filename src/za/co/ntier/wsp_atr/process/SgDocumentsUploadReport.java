package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Compares documents available on disk (SG data dump) vs what has been
 * uploaded into the WSP/ATR system for fiscal year 2026.
 *
 * Produces a three-tab Excel workbook:
 *   Tab 1 – Summary    : overall totals
 *   Tab 2 – By SDL     : files vs uploaded per SDL number
 *   Tab 3 – By Type    : files vs uploaded per document type
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.SgDocumentsUploadReport")
public class SgDocumentsUploadReport extends SvrProcess {

    private static final String BASE_DIR    = "/tmp/SG_Data_070526";
    private static final String FISCAL_YEAR = "2026";

    /** Directory name → upload type code (must match BulkLoadSgDocuments). */
    private static final Map<String, String> DIR_TO_TYPE = new LinkedHashMap<>();
    /** Upload type code → human-readable label. */
    private static final Map<String, String> TYPE_LABEL  = new LinkedHashMap<>();
    static {
        DIR_TO_TYPE.put("Authorisation Page",                "P");
        DIR_TO_TYPE.put("Cancelled Cheque",                  "C");
        DIR_TO_TYPE.put("Minutes of the Training Committee",  "S");
        DIR_TO_TYPE.put("Proof of Training",                 "T");

        TYPE_LABEL.put("P", "Authorisation Page");
        TYPE_LABEL.put("C", "Cancelled Cheque");
        TYPE_LABEL.put("S", "Minutes of the Training Committee");
        TYPE_LABEL.put("T", "Proof of Training");
        TYPE_LABEL.put("R", "WSP-ATR Report");
        TYPE_LABEL.put("A", "Attendance Register");
    }

    // ---- Counters collected while scanning the file system ------------------

    /** sdlNumber → docTypeCode → file count on disk */
    private final Map<String, Map<String, Integer>> diskBySdlAndType = new LinkedHashMap<>();

    /** sdlNumber → total files on disk */
    private final Map<String, Integer> diskBySdl  = new LinkedHashMap<>();

    /** docTypeCode → total files on disk */
    private final Map<String, Integer> diskByType = new LinkedHashMap<>();

    private int diskTotal = 0;

    // ---- Counters loaded from the database ----------------------------------

    /** sdlNumber → docTypeCode → upload count in DB */
    private final Map<String, Map<String, Integer>> dbBySdlAndType = new LinkedHashMap<>();

    /** sdlNumber → total uploads in DB */
    private final Map<String, Integer> dbBySdl  = new LinkedHashMap<>();

    /** docTypeCode → total uploads in DB */
    private final Map<String, Integer> dbByType = new LinkedHashMap<>();

    private int dbTotal = 0;

    // -------------------------------------------------------------------------

    @Override
    protected void prepare() { /* no parameters */ }

    @Override
    protected String doIt() throws Exception {
        File base = new File(BASE_DIR);
        if (!base.isDirectory())
            throw new IllegalStateException("Base directory not found: " + BASE_DIR);

        scanDisk(base);
        loadDbCounts();

        File report = buildReport();
        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(report);

        return "Report generated. Disk=" + diskTotal + "  DB=" + dbTotal;
    }

    // =========================================================================
    //  1. Scan the file system
    // =========================================================================

    private void scanDisk(File base) {
        for (File batchDir : dirs(base)) {
            for (File sdlDir : dirs(batchDir)) {
                String sdl = sdlDir.getName();
                for (File docTypeDir : dirs(sdlDir)) {
                    String typeCode = DIR_TO_TYPE.get(docTypeDir.getName());
                    if (typeCode == null) continue; // unknown type — ignore

                    int count = files(docTypeDir).length;
                    if (count == 0) continue;

                    // by SDL + type
                    diskBySdlAndType
                        .computeIfAbsent(sdl, k -> new LinkedHashMap<>())
                        .merge(typeCode, count, Integer::sum);

                    // by SDL
                    diskBySdl.merge(sdl, count, Integer::sum);

                    // by type
                    diskByType.merge(typeCode, count, Integer::sum);

                    diskTotal += count;
                }
            }
        }
    }

    // =========================================================================
    //  2. Load upload counts from the database
    // =========================================================================

    private void loadDbCounts() {
        String sql =
            "SELECT bp.value AS sdl, u.zz_wsp_atr_upload_type, COUNT(*) AS cnt " +
            "FROM zz_wsp_atr_uploads u " +
            "JOIN zz_wsp_atr_submitted s  ON s.zz_wsp_atr_submitted_id = u.zz_wsp_atr_submitted_id " +
            "JOIN zzsdforganisation org   ON org.zzsdforganisation_id   = s.zzsdforganisation_id " +
            "JOIN c_bpartner bp           ON bp.c_bpartner_id           = org.c_bpartner_id " +
            "JOIN c_year y                ON y.c_year_id                = s.zz_finyear_id " +
            "WHERE y.fiscalyear = '" + FISCAL_YEAR + "' " +
            "GROUP BY bp.value, u.zz_wsp_atr_upload_type " +
            "ORDER BY bp.value, u.zz_wsp_atr_upload_type";

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs  = pst.executeQuery();
            while (rs.next()) {
                String sdl      = rs.getString("sdl");
                String typeCode = rs.getString("zz_wsp_atr_upload_type");
                int    cnt      = rs.getInt("cnt");

                // by SDL + type
                dbBySdlAndType
                    .computeIfAbsent(sdl, k -> new LinkedHashMap<>())
                    .merge(typeCode, cnt, Integer::sum);

                // by SDL
                dbBySdl.merge(sdl, cnt, Integer::sum);

                // by type
                dbByType.merge(typeCode, cnt, Integer::sum);

                dbTotal += cnt;
            }
        } catch (Exception e) {
            addLog("DB query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
    }

    // =========================================================================
    //  3. Build the Excel workbook
    // =========================================================================

    private File buildReport() throws Exception {
        File tmp = File.createTempFile("SgDocumentsUploadReport_", ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            Styles s = new Styles(wb);

            writeSummarySheet(wb.createSheet("Summary"),    s);
            writeSdlSheet    (wb.createSheet("By SDL"),     s);
            writeTypeSheet   (wb.createSheet("By Type"),    s);

            wb.write(fos);
        }
        return tmp;
    }

    // ---- Tab 1: Summary -----------------------------------------------------

    private void writeSummarySheet(Sheet sh, Styles s) {
        int r = 0;

        Row title = sh.createRow(r++);
        cell(title, 0, "SG Documents Upload Report — Fiscal Year " + FISCAL_YEAR, s.title);
        sh.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
        r++; // blank row

        Row hdr = sh.createRow(r++);
        cell(hdr, 0, "Metric",              s.hdr);
        cell(hdr, 1, "Files on Disk",       s.hdr);
        cell(hdr, 2, "Uploaded to DB",      s.hdr);
        cell(hdr, 3, "Difference",          s.hdr);

        // Overall totals
        int diff = diskTotal - dbTotal;
        Row totRow = sh.createRow(r++);
        cell   (totRow, 0, "Total Documents",  null);
        numCell(totRow, 1, diskTotal,          null);
        numCell(totRow, 2, dbTotal,            null);
        numCell(totRow, 3, diff,               diff == 0 ? s.good : s.bad);

        // Totals per SDL (summary counts)
        r++;
        Row sdlHdr = sh.createRow(r++);
        cell(sdlHdr, 0, "Unique SDL Numbers on Disk",    s.subHdr);
        cell(sdlHdr, 1, "SDL Numbers with Uploads in DB",s.subHdr);

        Row sdlRow = sh.createRow(r++);
        numCell(sdlRow, 0, diskBySdl.size(), null);
        numCell(sdlRow, 1, dbBySdl.size(),   null);

        // Totals per type (summary counts)
        r++;
        Row typeHdr = sh.createRow(r++);
        cell(typeHdr, 0, "Document Type",    s.subHdr);
        cell(typeHdr, 1, "Files on Disk",    s.subHdr);
        cell(typeHdr, 2, "Uploaded to DB",   s.subHdr);
        cell(typeHdr, 3, "Difference",       s.subHdr);

        for (Map.Entry<String, String> e : TYPE_LABEL.entrySet()) {
            String code  = e.getKey();
            String label = e.getValue();
            int d = diskByType.getOrDefault(code, 0);
            int u = dbByType.getOrDefault(code, 0);
            int dif = d - u;
            Row row = sh.createRow(r++);
            cell   (row, 0, label, null);
            numCell(row, 1, d,     null);
            numCell(row, 2, u,     null);
            numCell(row, 3, dif,   dif == 0 && d > 0 ? s.good : (dif != 0 ? s.bad : null));
        }

        for (int c = 0; c < 4; c++) sh.autoSizeColumn(c);
    }

    // ---- Tab 2: By SDL ------------------------------------------------------

    private void writeSdlSheet(Sheet sh, Styles s) {
        // Collect all SDLs from both disk and DB
        List<String> allSdls = new ArrayList<>();
        for (String sdl : diskBySdl.keySet())
            if (!allSdls.contains(sdl)) allSdls.add(sdl);
        for (String sdl : dbBySdl.keySet())
            if (!allSdls.contains(sdl)) allSdls.add(sdl);
        allSdls.sort(String::compareTo);

        int r = 0;
        Row hdr = sh.createRow(r++);
        cell(hdr, 0, "SDL Number",       s.hdr);
        cell(hdr, 1, "Files on Disk",    s.hdr);
        cell(hdr, 2, "Uploaded to DB",   s.hdr);
        cell(hdr, 3, "Difference",       s.hdr);

        // Breakdown columns by type
        int col = 4;
        Map<String, Integer> typeColMap = new LinkedHashMap<>();
        for (String typeCode : TYPE_LABEL.keySet()) {
            if (diskByType.containsKey(typeCode) || dbByType.containsKey(typeCode)) {
                String label = TYPE_LABEL.getOrDefault(typeCode, typeCode);
                cell(hdr, col,   "Disk – " + label, s.hdr);
                cell(hdr, col+1, "DB – "  + label,  s.hdr);
                typeColMap.put(typeCode, col);
                col += 2;
            }
        }

        for (String sdl : allSdls) {
            int disk = diskBySdl.getOrDefault(sdl, 0);
            int db   = dbBySdl.getOrDefault(sdl, 0);
            int diff = disk - db;

            Row row = sh.createRow(r++);
            cell   (row, 0, sdl,  null);
            numCell(row, 1, disk, null);
            numCell(row, 2, db,   null);
            numCell(row, 3, diff, diff == 0 && disk > 0 ? s.good : (diff != 0 ? s.bad : null));

            // Per-type breakdown
            Map<String, Integer> diskTypes = diskBySdlAndType.getOrDefault(sdl, new HashMap<>());
            Map<String, Integer> dbTypes   = dbBySdlAndType.getOrDefault(sdl, new HashMap<>());
            for (Map.Entry<String, Integer> e : typeColMap.entrySet()) {
                String typeCode = e.getKey();
                int    startCol = e.getValue();
                numCell(row, startCol,   diskTypes.getOrDefault(typeCode, 0), null);
                numCell(row, startCol+1, dbTypes.getOrDefault(typeCode, 0),   null);
            }
        }

        // Totals row
        Row totRow = sh.createRow(r);
        cell   (totRow, 0, "TOTAL",     s.subHdr);
        numCell(totRow, 1, diskTotal,   s.subHdr);
        numCell(totRow, 2, dbTotal,     s.subHdr);
        numCell(totRow, 3, diskTotal - dbTotal, diskTotal == dbTotal ? s.good : s.bad);

        for (int c = 0; c < col; c++) sh.autoSizeColumn(c);
    }

    // ---- Tab 3: By Type -----------------------------------------------------

    private void writeTypeSheet(Sheet sh, Styles s) {
        int r = 0;
        Row hdr = sh.createRow(r++);
        cell(hdr, 0, "Type Code",        s.hdr);
        cell(hdr, 1, "Document Type",    s.hdr);
        cell(hdr, 2, "Files on Disk",    s.hdr);
        cell(hdr, 3, "Uploaded to DB",   s.hdr);
        cell(hdr, 4, "Difference",       s.hdr);
        cell(hdr, 5, "% Uploaded",       s.hdr);

        int totDisk = 0, totDb = 0;

        for (Map.Entry<String, String> e : TYPE_LABEL.entrySet()) {
            String code  = e.getKey();
            String label = e.getValue();
            int disk = diskByType.getOrDefault(code, 0);
            int db   = dbByType.getOrDefault(code, 0);
            int diff = disk - db;
            double pct = disk > 0 ? (db * 100.0 / disk) : 0.0;

            if (disk == 0 && db == 0) continue; // skip types with no activity

            Row row = sh.createRow(r++);
            cell   (row, 0, code,              null);
            cell   (row, 1, label,             null);
            numCell(row, 2, disk,              null);
            numCell(row, 3, db,                null);
            numCell(row, 4, diff,              diff == 0 && disk > 0 ? s.good : (diff != 0 ? s.bad : null));
            pctCell(row, 5, pct,               diff == 0 && disk > 0 ? s.good : (diff != 0 ? s.bad : null));

            totDisk += disk;
            totDb   += db;
        }

        // Totals row
        r++;
        Row totRow = sh.createRow(r);
        double totPct = totDisk > 0 ? (totDb * 100.0 / totDisk) : 0.0;
        cell   (totRow, 0, "",                         s.subHdr);
        cell   (totRow, 1, "TOTAL",                    s.subHdr);
        numCell(totRow, 2, totDisk,                    s.subHdr);
        numCell(totRow, 3, totDb,                      s.subHdr);
        numCell(totRow, 4, totDisk - totDb,            totDisk == totDb ? s.good : s.bad);
        pctCell(totRow, 5, totPct,                     totDisk == totDb ? s.good : s.bad);

        for (int c = 0; c < 6; c++) sh.autoSizeColumn(c);
    }

    // =========================================================================
    //  Styles
    // =========================================================================

    private static class Styles {
        final CellStyle hdr;
        final CellStyle subHdr;
        final CellStyle title;
        final CellStyle good;
        final CellStyle bad;

        Styles(XSSFWorkbook wb) {
            hdr    = hdrStyle(wb, IndexedColors.DARK_BLUE.getIndex(),  IndexedColors.WHITE.getIndex(), true);
            subHdr = hdrStyle(wb, IndexedColors.GREY_50_PERCENT.getIndex(), IndexedColors.WHITE.getIndex(), true);
            title  = hdrStyle(wb, IndexedColors.DARK_BLUE.getIndex(),  IndexedColors.WHITE.getIndex(), false);
            good   = fillStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());
            bad    = fillStyle(wb, IndexedColors.ROSE.getIndex());
        }

        private static CellStyle hdrStyle(XSSFWorkbook wb, short bg, short fg, boolean border) {
            CellStyle s = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            f.setColor(fg);
            s.setFont(f);
            s.setFillForegroundColor(bg);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            if (border) addBorders(s);
            return s;
        }

        private static CellStyle fillStyle(XSSFWorkbook wb, short color) {
            CellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(color);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            addBorders(s);
            return s;
        }

        private static void addBorders(CellStyle s) {
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
        }
    }

    // =========================================================================
    //  Cell helpers
    // =========================================================================

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private static void numCell(Row row, int col, int value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) c.setCellStyle(style);
    }

    private static void pctCell(Row row, int col, double pct, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(String.format("%.1f%%", pct));
        if (style != null) c.setCellStyle(style);
    }

    // =========================================================================
    //  File system helpers
    // =========================================================================

    private static File[] dirs(File parent) {
        File[] d = parent.listFiles(File::isDirectory);
        return d != null ? d : new File[0];
    }

    private static File[] files(File parent) {
        File[] f = parent.listFiles(File::isFile);
        return f != null ? f : new File[0];
    }
}
