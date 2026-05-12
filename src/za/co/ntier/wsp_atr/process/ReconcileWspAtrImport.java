package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
 * Reconciles imported WSP/ATR data against the source Excel file.
 *
 * Tab list and start rows are pulled from ZZ_WSP_ATR_Lookup_Mapping so the
 * reconciler stays aligned with whatever the importer uses. Sheets are
 * resolved via StreamingXlsxReader.findMatchingSheets — so a mapping name
 * like "ATR" matches workbook sheets "ATR_1", "ATR_2", etc.
 *
 * For each tab the reconciler compares row counts per SDL Number and sums of
 * a small set of numeric columns. Output is a multi-sheet workbook
 * (Summary, Row Counts, Numeric Sums, Discrepancies) delivered via
 * processUI.download().
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ReconcileWspAtrImport")
public class ReconcileWspAtrImport extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final String SDL_HEADER       = "SDLNumber";
    private static final String WSP_STATUS_HEADER = "WSPStatus";
    private static final String WSP_STATUS_REF_UU = "98479fb5-df5d-440d-86aa-92d77a320857";
    private static final String BLANK_SDL        = "(blank)";
    private static final String BLANK_STATUS     = "(none)";
    private static final int    DEFAULT_START_ROW = 4;
    private static final int    MAX_EMPTY        = 10;

    /** DB table (case-insensitive) → numeric columns to SUM. */
    private static final Map<String, String[]> NUMERIC_BY_TABLE = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_WSP", new String[]{
            "ZZ_African","ZZ_Coloured","ZZ_Indian","ZZ_White",
            "ZZ_Male","ZZ_Female","ZZ_Disabled"});
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_Finance",    new String[]{"ZZ_Finance_Value"});
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_ATR_Detail", new String[]{"Total_Training_Cost"});
    }

    private static class TabConfig {
        String       tabName;         // mapping name, used to resolve sheets
        String       dbTable;         // resolved from AD_Table_ID
        int          startRow;        // 1-based data start row from mapping
        String[]     numericCols;     // empty if none
        List<String> matchedSheets;   // resolved at runtime via findMatchingSheets
        int          sdlColIdx;       // 0-based column index for SDLNumber (from mapping detail)
        int          wspStatusColIdx; // 0-based column index for WSPStatus (-1 if not on this tab)
        int[]        numericColIdx;   // 0-based column indexes, aligned with numericCols
    }

    private static class TabStats {
        long     count;
        double[] sums;
        TabStats(int n) { this.sums = new double[n]; }
    }

    @Override
    protected void prepare() {
        for (ProcessInfoParameter p : getParameter())
            org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile())
            throw new AdempiereException("File not found: " + BULK_UPLOAD_PATH);

        List<TabConfig> tabs = loadMappings();
        if (tabs.isEmpty())
            throw new AdempiereException("No active mappings found in ZZ_WSP_ATR_Lookup_Mapping (ZZ_Is_For_Bulk='Y').");

        Map<String, Map<String, TabStats>> excelStats = new LinkedHashMap<>();
        Map<String, Map<String, TabStats>> dbStats    = new LinkedHashMap<>();
        Map<String, String> legalNames                = loadLegalNames();

        // sdl -> (status -> count) — for BioData status reconciliation
        Map<String, Map<String, Long>> excelStatus = new LinkedHashMap<>();
        TabConfig statusTab = null;

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {
            for (TabConfig tab : tabs) {
                tab.matchedSheets = reader.findMatchingSheets(tab.tabName);
                resolveColumnLayout(tab);
                excelStats.put(tab.tabName, scanExcel(reader, tab));
                dbStats   .put(tab.tabName, queryDb(tab));
                if (tab.wspStatusColIdx >= 0 && statusTab == null) {
                    statusTab = tab;
                    excelStatus = scanExcelStatus(reader, tab);
                }
            }
        }

        Map<String, Map<String, Long>> dbStatus = (statusTab != null) ? queryDbStatus() : Collections.emptyMap();

        File report = buildReport(tabs, excelStats, dbStats, legalNames,
                                  statusTab, excelStatus, dbStatus);
        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(report);

        return "Reconciliation complete. " + tabs.size() + " tab(s) compared.";
    }

    // -------------------------------------------------------------------------  inputs

    private List<TabConfig> loadMappings() {
        List<TabConfig> out = new ArrayList<>();
        String sql =
            "SELECT m.ZZ_Tab_Name, t.TableName, m.Start_Row " +
            "  FROM ZZ_WSP_ATR_Lookup_Mapping m " +
            "  JOIN AD_Table t ON t.AD_Table_ID = m.AD_Table_ID " +
            " WHERE m.IsActive='Y' AND m.ZZ_Is_For_Bulk='Y' AND m.AD_Table_ID > 0 " +
            " ORDER BY m.ZZ_Tab_Name";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                TabConfig tc = new TabConfig();
                tc.tabName = rs.getString(1);
                tc.dbTable = rs.getString(2);
                int sr = rs.getInt(3);
                tc.startRow = (rs.wasNull() || sr <= 0) ? DEFAULT_START_ROW : sr;
                String[] nums = NUMERIC_BY_TABLE.get(tc.dbTable);
                tc.numericCols    = (nums != null) ? nums : new String[0];
                tc.matchedSheets  = Collections.emptyList();
                tc.sdlColIdx       = -1;
                tc.wspStatusColIdx = -1;
                tc.numericColIdx  = new int[tc.numericCols.length];
                Arrays.fill(tc.numericColIdx, -1);
                out.add(tc);
            }
        } catch (Exception e) {
            log.warning("Could not load mappings: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    /**
     * Resolves the Excel column letter for SDLNumber and each numeric column
     * from ZZ_WSP_ATR_Lookup_Mapping_Detail — the same source the importer uses
     * to pin columns by position (instead of guessing the Excel header text).
     */
    private void resolveColumnLayout(TabConfig tab) {
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
            pst.setString(1, tab.tabName);
            rs = pst.executeQuery();
            while (rs.next()) {
                String letter   = rs.getString(1);
                String hdrName  = rs.getString(2);
                String colName  = rs.getString(3);
                int idx = columnLetterToIndex(letter);
                if (idx < 0) continue;

                if (SDL_HEADER.equalsIgnoreCase(hdrName) || SDL_HEADER.equalsIgnoreCase(colName)) {
                    tab.sdlColIdx = idx;
                }
                if (WSP_STATUS_HEADER.equalsIgnoreCase(hdrName) || WSP_STATUS_HEADER.equalsIgnoreCase(colName)) {
                    tab.wspStatusColIdx = idx;
                }
                for (int i = 0; i < tab.numericCols.length; i++) {
                    if (tab.numericCols[i].equalsIgnoreCase(colName)
                            || tab.numericCols[i].equalsIgnoreCase(hdrName)) {
                        tab.numericColIdx[i] = idx;
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Could not resolve column layout for " + tab.tabName + ": " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        addLog("Layout " + tab.tabName + ": SDLNumber=" + indexToLetter(tab.sdlColIdx)
                + ", " + namedIndexes(tab));
    }

    private String namedIndexes(TabConfig tab) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tab.numericCols.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(tab.numericCols[i]).append('=').append(indexToLetter(tab.numericColIdx[i]));
        }
        return sb.toString();
    }

    /** "A"→0, "B"→1, "AA"→26. Returns -1 on null/empty/invalid. */
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

    /** Inverse for diagnostic logging. */
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

    private Map<String, String> loadLegalNames() {
        Map<String, String> out = new HashMap<>();
        String sql =
            "SELECT bp.Value, bp.Name FROM ZZSdfOrganisation org "
          + "  JOIN C_BPartner bp ON bp.C_BPartner_ID = org.C_BPartner_ID";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.trim().isEmpty()) out.put(v.trim(), rs.getString(2));
            }
        } catch (Exception e) {
            log.warning("Could not load legal names: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    // -------------------------------------------------------------------------  excel side

    private Map<String, TabStats> scanExcel(StreamingXlsxReader reader, TabConfig tab) throws Exception {
        Map<String, TabStats> out = new LinkedHashMap<>();
        if (tab.matchedSheets.isEmpty()) return out;
        if (tab.sdlColIdx < 0) {
            addLog("Skipping " + tab.tabName + ": SDLNumber column not found in mapping detail.");
            return out;
        }

        final int dataStartIdx = Math.max(0, tab.startRow - 1);
        for (String sheetName : tab.matchedSheets)
            scanOneSheet(reader, sheetName, tab, dataStartIdx, out);
        return out;
    }

    private void scanOneSheet(StreamingXlsxReader reader, String sheetName, TabConfig tab,
                              int dataStartIdx, Map<String, TabStats> out) throws Exception {
        final int[] emptyStreak = {0};

        reader.streamSheet(sheetName, dataStartIdx, null, (rowIdx, cells) -> {
            if (cells.isEmpty()) {
                if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                return StreamingXlsxReader.Action.CONTINUE;
            }
            emptyStreak[0] = 0;

            String sdl = cells.getOrDefault(tab.sdlColIdx, "").trim();
            if (sdl.isEmpty()) sdl = BLANK_SDL;

            TabStats st = out.computeIfAbsent(sdl, k -> new TabStats(tab.numericCols.length));
            st.count++;
            for (int i = 0; i < tab.numericColIdx.length; i++) {
                int colIdx = tab.numericColIdx[i];
                if (colIdx >= 0) st.sums[i] += parseNumber(cells.get(colIdx));
            }
            return StreamingXlsxReader.Action.CONTINUE;
        });
    }

    /**
     * Tolerant numeric parse: strips a leading apostrophe (Excel "force-text"
     * artefact), any whitespace, thousands separators, and currency symbols
     * before parsing. Anything else returns 0.
     */
    private static double parseNumber(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        if (t.charAt(0) == '\'') t = t.substring(1).trim();
        t = t.replace(",", "").replace(" ", "").replace("R", "").replace("$", "");
        if (t.isEmpty()) return 0;
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return 0; }
    }

    /**
     * Builds SDL → (status → count) from the WSPStatus column of the BioData tab.
     */
    private Map<String, Map<String, Long>> scanExcelStatus(StreamingXlsxReader reader, TabConfig tab) throws Exception {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        if (tab.matchedSheets.isEmpty() || tab.sdlColIdx < 0 || tab.wspStatusColIdx < 0) return out;

        final int dataStartIdx = Math.max(0, tab.startRow - 1);
        for (String sheetName : tab.matchedSheets) {
            final int[] emptyStreak = {0};
            reader.streamSheet(sheetName, dataStartIdx, null, (rowIdx, cells) -> {
                if (cells.isEmpty()) {
                    if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                    return StreamingXlsxReader.Action.CONTINUE;
                }
                emptyStreak[0] = 0;

                String sdl = cells.getOrDefault(tab.sdlColIdx, "").trim();
                String status = cells.getOrDefault(tab.wspStatusColIdx, "").trim();
                if (sdl.isEmpty())    sdl    = BLANK_SDL;
                if (status.isEmpty()) status = BLANK_STATUS;

                out.computeIfAbsent(sdl, k -> new TreeMap<>())
                   .merge(status, 1L, Long::sum);
                return StreamingXlsxReader.Action.CONTINUE;
            });
        }
        return out;
    }

    // -------------------------------------------------------------------------  db side

    private Map<String, TabStats> queryDb(TabConfig tab) {
        Map<String, TabStats> out = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT bp.Value AS sdl, COUNT(*) AS cnt");
        // Cast to text + regex-match before ::numeric so non-numeric values in
        // varchar columns (e.g. zz_finance_value) don't kill the query. Matches
        // the Excel-side parseNumber: anything that isn't a clean signed number
        // contributes 0.
        for (String nc : tab.numericCols)
            sql.append(", COALESCE(SUM(CASE WHEN TRIM(CAST(c.")
               .append(nc).append(" AS TEXT)) ~ '^-?[0-9]+(\\.[0-9]+)?$'")
               .append(" THEN TRIM(CAST(c.").append(nc).append(" AS TEXT))::numeric")
               .append(" ELSE 0 END),0) AS ").append(nc);
        sql.append("  FROM ").append(tab.dbTable).append(" c")
           .append("  JOIN ZZ_WSP_ATR_Submitted s ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID")
           .append("  JOIN ZZSdfOrganisation org ON org.ZZSdfOrganisation_ID  = s.ZZSdfOrganisation_ID")
           .append("  JOIN C_BPartner         bp ON bp.C_BPartner_ID          = org.C_BPartner_ID")
           .append(" WHERE c.IsActive='Y'")
           .append(" GROUP BY bp.Value");

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql.toString(), null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String sdl = rs.getString("sdl");
                if (sdl == null || sdl.trim().isEmpty()) sdl = BLANK_SDL;
                TabStats st = new TabStats(tab.numericCols.length);
                st.count = rs.getLong("cnt");
                for (int i = 0; i < tab.numericCols.length; i++) {
                    BigDecimal bd = rs.getBigDecimal(tab.numericCols[i]);
                    if (bd != null) st.sums[i] = bd.doubleValue();
                }
                out.put(sdl.trim(), st);
            }
        } catch (Exception e) {
            log.warning("DB query failed for " + tab.dbTable + ": " + e.getMessage());
            addLog("DB query failed for " + tab.dbTable + ": " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    /**
     * SDL → (status display name → count) from ZZ_WSP_ATR_Submitted.
     * Status display name is resolved via AD_Ref_List under the WSP status
     * reference UUID (same lookup used by the importer's detectWspStatus()).
     */
    private Map<String, Map<String, Long>> queryDbStatus() {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        String sql =
            "SELECT bp.Value AS sdl," +
            "       COALESCE(rl.Name, s.ZZ_DocStatus) AS status," +
            "       COUNT(*) AS cnt" +
            "  FROM ZZ_WSP_ATR_Submitted s" +
            "  JOIN ZZSdfOrganisation org ON org.ZZSdfOrganisation_ID = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner         bp  ON bp.C_BPartner_ID         = org.C_BPartner_ID" +
            "  LEFT JOIN AD_Ref_List   rl  ON rl.Value = s.ZZ_DocStatus" +
            "                             AND rl.AD_Reference_ID = (" +
            "                                 SELECT AD_Reference_ID FROM AD_Reference" +
            "                                  WHERE AD_Reference_UU = '" + WSP_STATUS_REF_UU + "')" +
            " GROUP BY bp.Value, COALESCE(rl.Name, s.ZZ_DocStatus)";

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String sdl    = rs.getString("sdl");
                String status = rs.getString("status");
                long   cnt    = rs.getLong("cnt");
                if (sdl == null || sdl.trim().isEmpty())       sdl    = BLANK_SDL;
                if (status == null || status.trim().isEmpty()) status = BLANK_STATUS;
                out.computeIfAbsent(sdl.trim(), k -> new TreeMap<>())
                   .merge(status.trim(), cnt, Long::sum);
            }
        } catch (Exception e) {
            log.warning("DB status query failed: " + e.getMessage());
            addLog("DB status query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    // -------------------------------------------------------------------------  report

    private File buildReport(List<TabConfig> tabs,
                             Map<String, Map<String, TabStats>> excel,
                             Map<String, Map<String, TabStats>> db,
                             Map<String, String> legalNames,
                             TabConfig statusTab,
                             Map<String, Map<String, Long>> excelStatus,
                             Map<String, Map<String, Long>> dbStatus) throws Exception {

        File tmp = File.createTempFile("WSP_ATR_Reconciliation_", ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            CellStyle hdr   = headerStyle(wb);
            CellStyle ok    = fillStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle bad   = fillStyle(wb, IndexedColors.ROSE.getIndex());
            CellStyle plain = borderStyle(wb);

            writeSummary       (wb.createSheet("Summary"),       tabs, excel, db, hdr, ok, bad);
            writeRowCounts     (wb.createSheet("Row Counts"),    tabs, excel, db, legalNames, hdr, ok, bad);
            writeNumericAggs   (wb.createSheet("Numeric Sums"),  tabs, excel, db, legalNames, hdr, ok, bad);
            writeDiscrepancies (wb.createSheet("Discrepancies"), tabs, excel, db, legalNames, hdr, bad);
            if (statusTab != null)
                writeBioDataStatus(wb.createSheet("BioData Status"), statusTab,
                                   excelStatus, dbStatus, legalNames, hdr, ok, bad);

            wb.write(fos);
        }
        return tmp;
    }

    private void writeSummary(Sheet sh, List<TabConfig> tabs,
                              Map<String, Map<String, TabStats>> excel,
                              Map<String, Map<String, TabStats>> db,
                              CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"Tab", "DB Table", "Matched Sheets", "Excel Rows", "DB Rows", "Diff",
                         "SDLs Excel", "SDLs DB", "SDLs Mismatched", "Status"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (TabConfig tab : tabs) {
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());

            long eTotal = 0, dTotal = 0;
            for (TabStats s : e.values()) eTotal += s.count;
            for (TabStats s : d.values()) dTotal += s.count;
            long diff = eTotal - dTotal;

            TreeSet<String> allSdls = new TreeSet<>();
            allSdls.addAll(e.keySet()); allSdls.addAll(d.keySet());
            int mismatched = 0;
            for (String sdl : allSdls) {
                long ec = e.containsKey(sdl) ? e.get(sdl).count : 0;
                long dc = d.containsKey(sdl) ? d.get(sdl).count : 0;
                if (ec != dc) mismatched++;
            }

            Row row = sh.createRow(r++);
            cell(row, 0, tab.tabName, null);
            cell(row, 1, tab.dbTable, null);
            cell(row, 2, String.join(", ", tab.matchedSheets), null);
            cell(row, 3, String.valueOf(eTotal), null);
            cell(row, 4, String.valueOf(dTotal), null);
            cell(row, 5, String.valueOf(diff), diff == 0 ? null : bad);
            cell(row, 6, String.valueOf(e.size()), null);
            cell(row, 7, String.valueOf(d.size()), null);
            cell(row, 8, String.valueOf(mismatched), mismatched == 0 ? null : bad);
            cell(row, 9, (diff == 0 && mismatched == 0) ? "MATCH" : "MISMATCH",
                    (diff == 0 && mismatched == 0) ? ok : bad);
        }
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    private void writeRowCounts(Sheet sh, List<TabConfig> tabs,
                                Map<String, Map<String, TabStats>> excel,
                                Map<String, Map<String, TabStats>> db,
                                Map<String, String> legal,
                                CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Excel Count", "DB Count", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (TabConfig tab : tabs) {
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                long ec = e.containsKey(sdl) ? e.get(sdl).count : 0;
                long dc = d.containsKey(sdl) ? d.get(sdl).count : 0;
                long diff = ec - dc;
                CellStyle style = (diff == 0) ? null : bad;

                Row row = sh.createRow(r++);
                cell(row, 0, sdl, style);
                cell(row, 1, legal.getOrDefault(sdl, ""), style);
                cell(row, 2, tab.tabName, style);
                cell(row, 3, String.valueOf(ec), style);
                cell(row, 4, String.valueOf(dc), style);
                cell(row, 5, String.valueOf(diff), style != null ? bad : ok);
            }
        }
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    private void writeNumericAggs(Sheet sh, List<TabConfig> tabs,
                                  Map<String, Map<String, TabStats>> excel,
                                  Map<String, Map<String, TabStats>> db,
                                  Map<String, String> legal,
                                  CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Column", "Excel Sum", "DB Sum", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (TabConfig tab : tabs) {
            if (tab.numericCols.length == 0) continue;
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                TabStats es = e.get(sdl);
                TabStats ds = d.get(sdl);
                for (int i = 0; i < tab.numericCols.length; i++) {
                    double ev = es != null ? es.sums[i] : 0;
                    double dv = ds != null ? ds.sums[i] : 0;
                    double diff = ev - dv;
                    CellStyle style = (Math.abs(diff) == 0) ? null : bad;

                    Row row = sh.createRow(r++);
                    cell   (row, 0, sdl, style);
                    cell   (row, 1, legal.getOrDefault(sdl, ""), style);
                    cell   (row, 2, tab.tabName, style);
                    cell   (row, 3, tab.numericCols[i], style);
                    numCell(row, 4, ev, style);
                    numCell(row, 5, dv, style);
                    numCell(row, 6, diff, style != null ? bad : ok);
                }
            }
        }
        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    private void writeDiscrepancies(Sheet sh, List<TabConfig> tabs,
                                    Map<String, Map<String, TabStats>> excel,
                                    Map<String, Map<String, TabStats>> db,
                                    Map<String, String> legal,
                                    CellStyle hdr, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Metric", "Excel", "DB", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        List<String[]> rows = new ArrayList<>();
        for (TabConfig tab : tabs) {
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                long ec = e.containsKey(sdl) ? e.get(sdl).count : 0;
                long dc = d.containsKey(sdl) ? d.get(sdl).count : 0;
                if (ec != dc) {
                    rows.add(new String[]{
                        sdl, legal.getOrDefault(sdl, ""), tab.tabName,
                        "Row Count", String.valueOf(ec), String.valueOf(dc), String.valueOf(ec - dc)});
                }
                for (int i = 0; i < tab.numericCols.length; i++) {
                    double ev = e.containsKey(sdl) ? e.get(sdl).sums[i] : 0;
                    double dv = d.containsKey(sdl) ? d.get(sdl).sums[i] : 0;
                    if (ev != dv) {
                        rows.add(new String[]{
                            sdl, legal.getOrDefault(sdl, ""), tab.tabName,
                            "SUM(" + tab.numericCols[i] + ")",
                            fmt(ev), fmt(dv), fmt(ev - dv)});
                    }
                }
            }
        }

        int r = 1;
        for (String[] d : rows) {
            Row row = sh.createRow(r++);
            for (int i = 0; i < d.length; i++) cell(row, i, d[i], bad);
        }
        if (rows.isEmpty()) cell(sh.createRow(1), 0, "No discrepancies found.", null);

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    /**
     * Side-by-side BioData WSPStatus vs DB ZZ_DocStatus reconciliation.
     * One row per (SDL, Status) appearing in either source. Mismatches highlighted.
     */
    private void writeBioDataStatus(Sheet sh, TabConfig statusTab,
                                    Map<String, Map<String, Long>> excelStatus,
                                    Map<String, Map<String, Long>> dbStatus,
                                    Map<String, String> legal,
                                    CellStyle hdr, CellStyle ok, CellStyle bad) {

        // Union of all SDLs and all statuses across both sources for stable ordering.
        TreeSet<String> sdls     = new TreeSet<>();
        TreeSet<String> statuses = new TreeSet<>();
        sdls.addAll(excelStatus.keySet()); sdls.addAll(dbStatus.keySet());
        for (Map<String, Long> m : excelStatus.values()) statuses.addAll(m.keySet());
        for (Map<String, Long> m : dbStatus.values())    statuses.addAll(m.keySet());

        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Status", "Excel Count", "DB Count", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        long eTot = 0, dTot = 0;
        for (String sdl : sdls) {
            Map<String, Long> e = excelStatus.getOrDefault(sdl, Collections.emptyMap());
            Map<String, Long> d = dbStatus   .getOrDefault(sdl, Collections.emptyMap());
            TreeSet<String> rowStatuses = new TreeSet<>();
            rowStatuses.addAll(e.keySet()); rowStatuses.addAll(d.keySet());

            for (String status : rowStatuses) {
                long ec = e.getOrDefault(status, 0L);
                long dc = d.getOrDefault(status, 0L);
                long diff = ec - dc;
                CellStyle style = (diff == 0) ? null : bad;

                Row row = sh.createRow(r++);
                cell(row, 0, sdl, style);
                cell(row, 1, legal.getOrDefault(sdl, ""), style);
                cell(row, 2, status, style);
                cell(row, 3, String.valueOf(ec), style);
                cell(row, 4, String.valueOf(dc), style);
                cell(row, 5, String.valueOf(diff), style != null ? bad : ok);
                eTot += ec; dTot += dc;
            }
        }

        // Footer with totals.
        Row tot = sh.createRow(r);
        cell(tot, 0, "TOTAL", hdr);
        cell(tot, 1, "(tab: " + statusTab.tabName + ")", hdr);
        cell(tot, 2, "", hdr);
        cell(tot, 3, String.valueOf(eTot), hdr);
        cell(tot, 4, String.valueOf(dTot), hdr);
        cell(tot, 5, String.valueOf(eTot - dTot), (eTot == dTot) ? ok : bad);

        for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
    }

    // -------------------------------------------------------------------------  cell helpers

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

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

    private CellStyle borderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
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
