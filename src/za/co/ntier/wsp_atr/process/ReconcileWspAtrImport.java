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
        // HTVF: sum of vacancies per province
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_HTVF", new String[]{
            "ZZ_Vacancies_EC_Cnt","ZZ_Vacancies_FS_Cnt","ZZ_Vacancies_GP_Cnt",
            "ZZ_Vacancies_KZN_Cnt","ZZ_Vacancies_LP_Cnt","ZZ_Vacancies_MP_Cnt",
            "ZZ_Vacancies_NP_Cnt","ZZ_Vacancies_NW_Cnt","ZZ_Vacancies_WC_Cnt"});
        // Non-Employee Skills Training: sum of demographic training counts
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_Non_Employees_Training", new String[]{
            "ZZ_African","ZZ_Coloured","ZZ_Indian","ZZ_Male","ZZ_Female",
            "ZZ_Disabled_Done","ZZ_Disabled_Planned","ZZ_Total_Done","ZZ_Total_Planned"});
        // Contractors: sum of trained/planned per occupational group
        NUMERIC_BY_TABLE.put("ZZ_WSP_ATR_Contractors", new String[]{
            "ZZ_Managers_Trained","ZZ_Managers_Planned",
            "ZZ_Professionals_Trained","ZZ_Professionals_Planned",
            "ZZ_Technicians_Trained","ZZ_Technicians_Planned",
            "ZZ_Clerical_Trained","ZZ_Clerical_Planned",
            "ZZ_Service_Trained","ZZ_Service_Planned",
            "ZZ_Skilled_Workers_Trained","ZZ_Skilled_Workers_Planned",
            "ZZ_Plant_Trained","ZZ_Plant_Planned",
            "ZZ_Elementary_Trained","ZZ_Elementary_Planned",
            "ZZ_Learners_Trained","ZZ_Learners_Planned",
            "ZZ_Total_Trained"});
    }

    /**
     * One FK column on a detail table that we want to group counts by.
     * {@code lookupTable} is the table referenced by the FK; its PK is assumed to
     * be {@code <lookupTable>_ID} and its display label column is "Name" — the
     * iDempiere convention used throughout the ZZ_*_Ref tables.
     */
    private static class CategoryCol {
        final String fkColumn;    // e.g. "Gender_ID"
        final String lookupTable; // e.g. "ZZ_Gender_Ref"
        CategoryCol(String fk, String lookup) { fkColumn = fk; lookupTable = lookup; }
    }

    /** DB table (case-insensitive) → reference FK columns to count rows by category. */
    private static final Map<String, CategoryCol[]> CATEGORY_BY_TABLE = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        CATEGORY_BY_TABLE.put("ZZ_WSP_ATR_Biodata_Detail", new CategoryCol[]{
            new CategoryCol("Gender_ID",     "ZZ_Gender_Ref"),
            new CategoryCol("Disabled_ID",   "ZZ_No_Yes_Ref"),
            new CategoryCol("Race_ID",       "ZZ_Equity_Ref"),
            new CategoryCol("SA_Citizen_ID", "ZZ_No_Yes_Ref"),
        });
        // HTVF: count rows per primary scarce reason
        CATEGORY_BY_TABLE.put("ZZ_WSP_ATR_HTVF", new CategoryCol[]{
            new CategoryCol("ZZ_Scarce_Reason_ID", "ZZ_Scarce_Reason_Ref"),
        });
        // Top-up Skills Survey: count rows per top-up skill type
        CATEGORY_BY_TABLE.put("ZZ_WSP_ATR_TopUp_Skills", new CategoryCol[]{
            new CategoryCol("ZZ_TopUpSkill_ID", "ZZ_Topup_Skills_Ref"),
        });
        // Non-Employee Skills Training: count rows by employment status and target beneficiary
        CATEGORY_BY_TABLE.put("ZZ_WSP_ATR_Non_Employees_Training", new CategoryCol[]{
            new CategoryCol("ZZ_Non_Emp_Status_Done_ID",   "ZZ_WSP_Non_Employee_Status_Ref"),
            new CategoryCol("ZZ_Target_Ben_Done_ID",       "ZZ_Target_Beneficiary_Ref"),
        });
        // Contractors: count rows per learning programme type
        CATEGORY_BY_TABLE.put("ZZ_WSP_ATR_Contractors", new CategoryCol[]{
            new CategoryCol("ZZ_Learning_Programme_Type_ID", "ZZ_Qualification_Type_Details_Ref"),
        });
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
        int[]        ignoreIfBlankColIdx = new int[0]; // columns flagged ignore_if_blank=Y
        CategoryCol[] categoryCols  = new CategoryCol[0]; // FK + lookup table per category
        int[]         categoryColIdx = new int[0];        // aligned Excel column indexes
    }

    private static class TabStats {
        long     count;
        double[] sums;
        /** DB col name → (category label lower-cased) → row count. */
        Map<String, Map<String, Long>> categoryCounts = new LinkedHashMap<>();
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

        // Pre-load canonical status name lookup (lower-cased AD_Ref_List.Name → canonical Name)
        // so Excel-side status text is normalised to whatever the DB stores via AD_Ref_List.
        Map<String, String> statusNormalizer = loadStatusNormalizer();

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {
            for (TabConfig tab : tabs) {
                tab.matchedSheets = reader.findMatchingSheets(tab.tabName);
                resolveColumnLayout(tab);
                excelStats.put(tab.tabName, scanExcel(reader, tab));
                dbStats   .put(tab.tabName, queryDb(tab));
                if (tab.wspStatusColIdx >= 0 && statusTab == null) {
                    statusTab = tab;
                    excelStatus = scanExcelStatus(reader, tab, statusNormalizer);
                }
            }
        }

        Map<String, Map<String, Long>> dbStatus = (statusTab != null) ? queryDbStatus(statusTab) : Collections.emptyMap();

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
                CategoryCol[] cats = CATEGORY_BY_TABLE.get(tc.dbTable);
                tc.categoryCols  = (cats != null) ? cats : new CategoryCol[0];
                tc.matchedSheets  = Collections.emptyList();
                tc.sdlColIdx       = -1;
                tc.wspStatusColIdx = -1;
                tc.numericColIdx  = new int[tc.numericCols.length];
                Arrays.fill(tc.numericColIdx, -1);
                tc.categoryColIdx = new int[tc.categoryCols.length];
                Arrays.fill(tc.categoryColIdx, -1);
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
            "SELECT d.ZZ_Column_Letter, d.ZZ_Header_Name, c.ColumnName, d.ignore_if_blank " +
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
            java.util.Set<Integer> ignoreIfBlank = new java.util.LinkedHashSet<>();
            while (rs.next()) {
                String letter   = rs.getString(1);
                String hdrName  = rs.getString(2);
                String colName  = rs.getString(3);
                String ignoreFlag = rs.getString(4);
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
                for (int i = 0; i < tab.categoryCols.length; i++) {
                    String fk = tab.categoryCols[i].fkColumn;
                    if (fk.equalsIgnoreCase(colName) || fk.equalsIgnoreCase(hdrName)) {
                        tab.categoryColIdx[i] = idx;
                    }
                }
                if ("Y".equalsIgnoreCase(ignoreFlag)) {
                    ignoreIfBlank.add(idx);
                }
            }
            tab.ignoreIfBlankColIdx = ignoreIfBlank.stream().mapToInt(Integer::intValue).toArray();
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

        // Mapping's Start_Row is passed straight to streamSheet (0-based skip),
        // same convention the importer uses — rows 0..startRow-1 are headers/metadata.
        final int dataStartIdx = Math.max(0, tab.startRow);
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

            // Mirror the importer's ignore_if_blank filter: if any flagged column
            // is blank or numerically zero, the importer skips this row, so we
            // must too — otherwise the Excel sum includes rows the DB doesn't.
            for (int idx : tab.ignoreIfBlankColIdx) {
                if (isBlankOrZero(cells.get(idx))) {
                    return StreamingXlsxReader.Action.CONTINUE;
                }
            }

            String sdl = cells.getOrDefault(tab.sdlColIdx, "").trim();
            if (sdl.isEmpty()) sdl = BLANK_SDL;

            TabStats st = out.computeIfAbsent(sdl, k -> new TabStats(tab.numericCols.length));
            st.count++;
            for (int i = 0; i < tab.numericColIdx.length; i++) {
                int colIdx = tab.numericColIdx[i];
                if (colIdx >= 0) st.sums[i] += parseNumber(cells.get(colIdx));
            }
            for (int i = 0; i < tab.categoryColIdx.length; i++) {
                int colIdx = tab.categoryColIdx[i];
                if (colIdx < 0) continue;
                String raw = cells.get(colIdx);
                if (raw == null) raw = "";
                String key = raw.trim();
                if (key.isEmpty()) key = BLANK_STATUS; // re-use the blank sentinel
                key = key.toLowerCase();
                st.categoryCounts
                  .computeIfAbsent(tab.categoryCols[i].fkColumn, k -> new LinkedHashMap<>())
                  .merge(key, 1L, Long::sum);
            }
            return StreamingXlsxReader.Action.CONTINUE;
        });
    }

    /**
     * Tolerant numeric parse. Mirrors WspAtrImportUtil.parseBigDecimal so the
     * reconciler interprets Excel cells the same way the importer does.
     * Handles US ("1,234.56") and European ("1.234,56" or "19509,35") formats,
     * currency symbols, spaces, and leading "force-text" apostrophes.
     */
    /** Round to 2 decimals (cents) — eliminates accumulated double-sum noise. */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Mirrors {@code AbstractMappingSheetImporter.isBlankOrZero}: blank text,
     * or a numeric-looking string that evaluates to zero, both count as blank.
     */
    private static boolean isBlankOrZero(String txt) {
        if (txt == null) return true;
        String s = txt.trim();
        if (s.isEmpty()) return true;
        s = s.replace(" ", "").replace(",", "");
        try {
            return new java.math.BigDecimal(s)
                    .compareTo(java.math.BigDecimal.ZERO) == 0;
        } catch (Exception ignore) {
            return false;
        }
    }

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

    /**
     * Builds SDL → (status → count) from the WSPStatus column of the BioData tab.
     * Status text is normalised case-insensitively against AD_Ref_List.Name so it
     * matches what the importer's detectWspStatus() would have stored.
     */
    private Map<String, Map<String, Long>> scanExcelStatus(StreamingXlsxReader reader, TabConfig tab,
                                                            Map<String, String> normalizer) throws Exception {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        if (tab.matchedSheets.isEmpty() || tab.sdlColIdx < 0 || tab.wspStatusColIdx < 0) return out;

        // Mapping's Start_Row is passed straight to streamSheet (0-based skip),
        // same convention the importer uses — rows 0..startRow-1 are headers/metadata.
        final int dataStartIdx = Math.max(0, tab.startRow);
        for (String sheetName : tab.matchedSheets) {
            final int[] emptyStreak = {0};
            reader.streamSheet(sheetName, dataStartIdx, null, (rowIdx, cells) -> {
                if (cells.isEmpty()) {
                    if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                    return StreamingXlsxReader.Action.CONTINUE;
                }
                emptyStreak[0] = 0;

                // Skip rows the importer would have ignored (ignore_if_blank columns).
                for (int idx : tab.ignoreIfBlankColIdx) {
                    if (isBlankOrZero(cells.get(idx))) {
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                }

                String sdl = cells.getOrDefault(tab.sdlColIdx, "").trim();
                String status = cells.getOrDefault(tab.wspStatusColIdx, "").trim();
                if (sdl.isEmpty()) sdl = BLANK_SDL;
                status = canonicaliseStatus(status, normalizer);

                out.computeIfAbsent(sdl, k -> new TreeMap<>())
                   .merge(status, 1L, Long::sum);
                return StreamingXlsxReader.Action.CONTINUE;
            });
        }
        return out;
    }

    /**
     * Normalises raw Excel status text to the canonical AD_Ref_List.Name.
     * Applies the same "Created" → "Draft" remap as the importer.
     * Unrecognised values fall through unchanged so they're still visible in the report.
     */
    private String canonicaliseStatus(String raw, Map<String, String> normalizer) {
        if (raw == null || raw.isEmpty()) return BLANK_STATUS;
        String key = raw.toLowerCase();
        // Same remap as ImportWspAtrMigrationFile.detectWspStatus.
        if ("created".equals(key)) key = "draft";
        String canonical = normalizer.get(key);
        return (canonical != null) ? canonical : raw;
    }

    /**
     * lower(rl.Name) → canonical rl.Name for the WSP status reference. Loaded once.
     */
    private Map<String, String> loadStatusNormalizer() {
        Map<String, String> out = new HashMap<>();
        String sql =
            "SELECT rl.Name FROM AD_Ref_List rl" +
            "  JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID" +
            " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'" +
            "   AND rl.IsActive='Y'";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null && !name.trim().isEmpty()) {
                    out.put(name.trim().toLowerCase(), name.trim());
                }
            }
        } catch (Exception e) {
            log.warning("Could not load WSP status reference list: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
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
        // the Excel-side parseNumber: plain numbers, European single-decimal
        // ("75,44"), US thousands ("1,234,567"), US thousands+decimal
        // ("1,234,567.89"), and European with dot-thousands+comma-decimal
        // ("1.234.567,89") are all summed. Anything else contributes 0.
        for (String nc : tab.numericCols) {
            String raw = "TRIM(CAST(c." + nc + " AS TEXT))";
            sql.append(", COALESCE(SUM(CASE")
               // 1) Plain US:  19509.35 / 905590.00 / 57
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]+(\\.[0-9]+)?$'")
               .append(" THEN ").append(raw).append("::numeric")
               // 2) US thousands + decimal: 1,234,567.89  (must precede the looser patterns below)
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(,[0-9]{3})+\\.[0-9]+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '')::numeric")
               // 3) US thousands only: 1,234 / 1,234,567  (3-digit groups → thousands, never decimal)
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(,[0-9]{3})+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '')::numeric")
               // 4) European dot-thousands + comma decimal: 1.234.567,89
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(\\.[0-9]{3})+,[0-9]+$'")
               .append(" THEN REPLACE(REPLACE(").append(raw).append(", '.', ''), ',', '.')::numeric")
               // 5) European single-decimal: 75,44 / 19509,35  (catch-all for comma-as-decimal)
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]+,[0-9]+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '.')::numeric")
               .append(" ELSE 0 END),0) AS ").append(nc);
        }
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

        // Category counts: one query per category FK column. Joins to that
        // column's lookup table to fetch the human-readable Name.
        for (CategoryCol cat : tab.categoryCols) {
            queryDbCategory(tab, cat, out);
        }
        return out;
    }

    /**
     * Run one category-count query for {@code cat} on {@code tab.dbTable},
     * populating each SDL's {@link TabStats#categoryCounts}. SDLs that didn't
     * appear in the main numeric query get a fresh TabStats entry on-the-fly.
     */
    private void queryDbCategory(TabConfig tab, CategoryCol cat, Map<String, TabStats> out) {
        String lookupPk = cat.lookupTable + "_ID";
        String sql =
            "SELECT bp.Value AS sdl," +
            "       LOWER(COALESCE(r.Name, CAST(c." + cat.fkColumn + " AS TEXT))) AS label," +
            "       COUNT(*) AS cnt" +
            "  FROM " + tab.dbTable + " c" +
            "  JOIN ZZ_WSP_ATR_Submitted s   ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID" +
            "  JOIN ZZSdfOrganisation org    ON org.ZZSdfOrganisation_ID  = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner         bp    ON bp.C_BPartner_ID          = org.C_BPartner_ID" +
            "  LEFT JOIN " + cat.lookupTable + " r ON r." + lookupPk + " = c." + cat.fkColumn +
            " WHERE c.IsActive='Y'" +
            " GROUP BY bp.Value, COALESCE(r.Name, CAST(c." + cat.fkColumn + " AS TEXT))";

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String sdl = rs.getString("sdl");
                if (sdl == null || sdl.trim().isEmpty()) sdl = BLANK_SDL;
                String label = rs.getString("label");
                if (label == null || label.isEmpty()) label = BLANK_STATUS;
                long cnt = rs.getLong("cnt");
                TabStats st = out.computeIfAbsent(sdl.trim(),
                        k -> new TabStats(tab.numericCols.length));
                st.categoryCounts
                  .computeIfAbsent(cat.fkColumn, k -> new LinkedHashMap<>())
                  .merge(label, cnt, Long::sum);
            }
        } catch (Exception e) {
            log.warning("DB category query failed for " + tab.dbTable + "." + cat.fkColumn
                    + ": " + e.getMessage());
            addLog("DB category query failed for " + tab.dbTable + "." + cat.fkColumn
                    + ": " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
    }

    /**
     * SDL → (status display name → count) of rows in the BioData detail table,
     * joined back through Submitted to read ZZ_DocStatus. Excel counts one row
     * per person on the BioData tab, so we must count detail rows (not Submitted
     * rows) to compare like-for-like.
     */
    private Map<String, Map<String, Long>> queryDbStatus(TabConfig statusTab) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        String sql =
            "SELECT bp.Value AS sdl," +
            "       COALESCE(rl.Name, s.ZZ_DocStatus) AS status," +
            "       COUNT(*) AS cnt" +
            "  FROM " + statusTab.dbTable + " c" +
            "  JOIN ZZ_WSP_ATR_Submitted s   ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID" +
            "  JOIN ZZSdfOrganisation org    ON org.ZZSdfOrganisation_ID = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner         bp    ON bp.C_BPartner_ID         = org.C_BPartner_ID" +
            "  LEFT JOIN AD_Ref_List   rl    ON rl.Value = s.ZZ_DocStatus" +
            "                              AND rl.AD_Reference_ID = (" +
            "                                  SELECT AD_Reference_ID FROM AD_Reference" +
            "                                   WHERE AD_Reference_UU = '" + WSP_STATUS_REF_UU + "')" +
            " WHERE c.IsActive='Y'" +
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
            if (tab.numericCols.length == 0 && tab.categoryCols.length == 0) continue;
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                TabStats es = e.get(sdl);
                TabStats ds = d.get(sdl);
                // Numeric sums.
                for (int i = 0; i < tab.numericCols.length; i++) {
                    double ev = round2(es != null ? es.sums[i] : 0);
                    double dv = round2(ds != null ? ds.sums[i] : 0);
                    double diff = round2(ev - dv);
                    CellStyle style = (diff == 0.0) ? null : bad;

                    Row row = sh.createRow(r++);
                    cell   (row, 0, sdl, style);
                    cell   (row, 1, legal.getOrDefault(sdl, ""), style);
                    cell   (row, 2, tab.tabName, style);
                    cell   (row, 3, tab.numericCols[i], style);
                    numCell(row, 4, ev, style);
                    numCell(row, 5, dv, style);
                    numCell(row, 6, diff, style != null ? bad : ok);
                }
                // Category counts: one row per (category col, label) seen on either side.
                for (CategoryCol cat : tab.categoryCols) {
                    Map<String, Long> em = (es != null) ? es.categoryCounts.get(cat.fkColumn) : null;
                    Map<String, Long> dm = (ds != null) ? ds.categoryCounts.get(cat.fkColumn) : null;
                    TreeSet<String> labels = new TreeSet<>();
                    if (em != null) labels.addAll(em.keySet());
                    if (dm != null) labels.addAll(dm.keySet());
                    for (String label : labels) {
                        long ec = (em != null && em.get(label) != null) ? em.get(label) : 0L;
                        long dc = (dm != null && dm.get(label) != null) ? dm.get(label) : 0L;
                        long diffL = ec - dc;
                        CellStyle style = (diffL == 0) ? null : bad;

                        Row row = sh.createRow(r++);
                        cell   (row, 0, sdl, style);
                        cell   (row, 1, legal.getOrDefault(sdl, ""), style);
                        cell   (row, 2, tab.tabName, style);
                        cell   (row, 3, "COUNT(" + cat.fkColumn + "=" + label + ")", style);
                        numCell(row, 4, ec, style);
                        numCell(row, 5, dc, style);
                        numCell(row, 6, diffL, style != null ? bad : ok);
                    }
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
                    double ev = round2(e.containsKey(sdl) ? e.get(sdl).sums[i] : 0);
                    double dv = round2(d.containsKey(sdl) ? d.get(sdl).sums[i] : 0);
                    double diff = round2(ev - dv);
                    if (diff != 0.0) {
                        rows.add(new String[]{
                            sdl, legal.getOrDefault(sdl, ""), tab.tabName,
                            "SUM(" + tab.numericCols[i] + ")",
                            fmt(ev), fmt(dv), fmt(diff)});
                    }
                }
                for (CategoryCol cat : tab.categoryCols) {
                    Map<String, Long> em = e.containsKey(sdl) ? e.get(sdl).categoryCounts.get(cat.fkColumn) : null;
                    Map<String, Long> dm = d.containsKey(sdl) ? d.get(sdl).categoryCounts.get(cat.fkColumn) : null;
                    TreeSet<String> labels = new TreeSet<>();
                    if (em != null) labels.addAll(em.keySet());
                    if (dm != null) labels.addAll(dm.keySet());
                    for (String label : labels) {
                        long ec2 = (em != null && em.get(label) != null) ? em.get(label) : 0L;
                        long dc2 = (dm != null && dm.get(label) != null) ? dm.get(label) : 0L;
                        if (ec2 != dc2) {
                            rows.add(new String[]{
                                sdl, legal.getOrDefault(sdl, ""), tab.tabName,
                                "COUNT(" + cat.fkColumn + "=" + label + ")",
                                String.valueOf(ec2), String.valueOf(dc2), String.valueOf(ec2 - dc2)});
                        }
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
