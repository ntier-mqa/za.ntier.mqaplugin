package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;

/**
 * Dynamic replacement for ReconcileWspAtrImport.
 *
 * Instead of maintaining hardcoded per-table column lists, this class inspects
 * ZZ_WSP_ATR_Lookup_Mapping_Detail → AD_Column for each tab and classifies
 * every mapped column automatically:
 *
 *   - DisplayType.isNumeric() (Amount, Quantity, Number, Integer) → SUM
 *   - Table / TableDir / Search with a resolvable reference table → count by Name
 *   - List with AD_Reference_Value_ID → count by AD_Ref_List.Name
 *
 * New tabs and new columns are picked up as soon as they exist in the mapping,
 * with no code change required.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.DynamicReconcileWspAtrImport")
public class DynamicReconcileWspAtrImport extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final String SDL_HEADER        = "SDLNumber";
    private static final String BLANK_SDL         = "(blank)";
    private static final String BLANK_LABEL       = "(none)";
    private static final int    DEFAULT_START_ROW = 4;
    private static final int    MAX_EMPTY         = 10;
    /** Excel hard limit: rows are 0-indexed, so the last writable index is 1 048 575. */
    private static final int    MAX_EXCEL_ROW     = 1_048_575;

    /**
     * Metadata for one data-bearing column discovered via AD_Column.
     * Exactly one of isNumeric / isTableRef / isList will be true.
     */
    private static class ColMeta {
        String  columnName;
        String  headerName;           // for report labels / logging
        int     excelColIdx   = -1;
        boolean isNumeric;            // DisplayType.isNumeric() → SUM
        boolean isTableRef;           // Table / TableDir / Search → join ref table by integer PK
        boolean isList;               // List → join AD_Ref_List by value string
        String  refTableName;         // resolved for isTableRef
        int     adReferenceValueId;   // AD_Column.AD_Reference_Value_ID (AD_Reference_ID for isList)
    }

    private static class TabMeta {
        String       tabName;
        String       dbTable;
        int          startRow          = DEFAULT_START_ROW;
        int          sdlColIdx         = -1;
        List<String> matchedSheets     = Collections.emptyList();
        List<ColMeta> numericCols      = new ArrayList<>();
        List<ColMeta> referenceCols    = new ArrayList<>();
        int[]        ignoreIfBlankColIdx = new int[0];
    }

    private static class TabStats {
        long     count;
        double[] sums;
        /** ColMeta.columnName → (normalised label → row count) */
        Map<String, Map<String, Long>> categoryCounts = new LinkedHashMap<>();
        TabStats(int numericCount) { sums = new double[numericCount]; }
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

        List<TabMeta> tabs = loadMappings();
        if (tabs.isEmpty())
            throw new AdempiereException("No active mappings found in ZZ_WSP_ATR_Lookup_Mapping (ZZ_Is_For_Bulk='Y').");

        Map<String, Map<String, TabStats>> excelStats = new LinkedHashMap<>();
        Map<String, Map<String, TabStats>> dbStats    = new LinkedHashMap<>();
        Map<String, String> legalNames                = loadLegalNames();

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {
            for (TabMeta tab : tabs) {
                tab.matchedSheets = reader.findMatchingSheets(tab.tabName);
                excelStats.put(tab.tabName, scanExcel(reader, tab));
                dbStats   .put(tab.tabName, queryDb(tab));
            }
        }

        File report = buildReport(tabs, excelStats, dbStats, legalNames);
        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(report);

        return "Dynamic reconciliation complete. " + tabs.size() + " tab(s) compared.";
    }

    // -------------------------------------------------------------------------  inputs

    private List<TabMeta> loadMappings() {
        List<TabMeta> out = new ArrayList<>();
        String sql =
            "SELECT m.ZZ_Tab_Name, t.TableName, m.Start_Row, m.ZZ_WSP_ATR_Lookup_Mapping_ID " +
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
                TabMeta tab = new TabMeta();
                tab.tabName = rs.getString(1);
                tab.dbTable = rs.getString(2);
                int sr = rs.getInt(3);
                tab.startRow = (rs.wasNull() || sr <= 0) ? DEFAULT_START_ROW : sr;
                int mappingId = rs.getInt(4);
                loadColMetas(tab, mappingId);
                out.add(tab);
            }
        } catch (Exception e) {
            log.warning("Could not load mappings: " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        return out;
    }

    /**
     * Populates tab.numericCols and tab.referenceCols by joining
     * ZZ_WSP_ATR_Lookup_Mapping_Detail to AD_Column and inspecting the display type.
     */
    private void loadColMetas(TabMeta tab, int mappingId) {
        String sql =
            "SELECT d.ZZ_Column_Letter, d.ZZ_Header_Name, d.AD_Column_ID, " +
            "       c.ColumnName, c.AD_Reference_ID, c.AD_Reference_Value_ID, " +
            "       d.ignore_if_blank " +
            "  FROM ZZ_WSP_ATR_Lookup_Mapping_Detail d " +
            "  LEFT JOIN AD_Column c ON c.AD_Column_ID = d.AD_Column_ID " +
            " WHERE d.ZZ_WSP_ATR_Lookup_Mapping_ID = ? AND d.IsActive='Y' " +
            " ORDER BY d.ZZ_Column_Letter";
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<Integer> ignoreIfBlankIdxs = new ArrayList<>();
        try {
            pst = DB.prepareStatement(sql, null);
            pst.setInt(1, mappingId);
            rs = pst.executeQuery();
            while (rs.next()) {
                String letter      = rs.getString(1);
                String headerName  = rs.getString(2);
                int    adColumnId  = rs.getInt(3);
                String colName     = rs.getString(4);
                int    displayType = rs.getInt(5);
                int    refValueId  = rs.getInt(6);
                String ignoreFlag  = rs.getString(7);

                int colIdx = columnLetterToIndex(letter);
                if (colIdx < 0) continue;

                if (SDL_HEADER.equalsIgnoreCase(headerName) || SDL_HEADER.equalsIgnoreCase(colName))
                    tab.sdlColIdx = colIdx;

                if ("Y".equalsIgnoreCase(ignoreFlag))
                    ignoreIfBlankIdxs.add(colIdx);

                // Skip rows with no AD_Column link — those columns carry no data dictionary metadata
                if (adColumnId <= 0 || colName == null) continue;

                ColMeta col = new ColMeta();
                col.columnName         = colName;
                col.headerName         = (headerName != null) ? headerName : colName;
                col.excelColIdx        = colIdx;
                col.adReferenceValueId = refValueId;

                if (DisplayType.isNumeric(displayType)) {
                    col.isNumeric = true;
                    tab.numericCols.add(col);
                } else if (displayType == DisplayType.Table
                        || displayType == DisplayType.TableDir
                        || displayType == DisplayType.Search) {
                    col.isTableRef   = true;
                    col.refTableName = resolveRefTableName(refValueId, displayType, colName);
                    if (col.refTableName != null) {
                        tab.referenceCols.add(col);
                    } else {
                        addLog("Skipping " + tab.tabName + "." + colName + ": could not resolve reference table");
                    }
                } else if (displayType == DisplayType.List && refValueId > 0) {
                    col.isList = true;
                    tab.referenceCols.add(col);
                }
            }
        } catch (Exception e) {
            log.warning("Could not load column metadata for mapping " + mappingId + ": " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
        tab.ignoreIfBlankColIdx = ignoreIfBlankIdxs.stream().mapToInt(Integer::intValue).toArray();
        addLog("Tab " + tab.tabName + ": " + tab.numericCols.size() + " numeric, "
                + tab.referenceCols.size() + " reference, SDL@" + indexToLetter(tab.sdlColIdx));
    }

    /**
     * Resolves the DB table name for a Table / TableDir / Search column.
     * Mirrors AbstractMappingSheetImporter.resolveRefTableName().
     */
    private String resolveRefTableName(int adReferenceValueId, int displayType, String columnName) {
        if (adReferenceValueId > 0) {
            MRefTable refTableCfg = MRefTable.get(Env.getCtx(), adReferenceValueId, null);
            if (refTableCfg != null && refTableCfg.getAD_Table_ID() > 0) {
                MTable refTable = MTable.get(Env.getCtx(), refTableCfg.getAD_Table_ID());
                if (refTable != null && refTable.getAD_Table_ID() > 0)
                    return refTable.getTableName();
            }
        }
        // TableDir: find the table whose PK column matches this column name
        if (displayType == DisplayType.TableDir && columnName != null) {
            String tableName = DB.getSQLValueStringEx(null,
                "SELECT t.TableName FROM AD_Table t " +
                "JOIN AD_Column c ON c.AD_Table_ID = t.AD_Table_ID " +
                "WHERE c.ColumnName = ? AND c.IsKey = 'Y' AND t.IsActive = 'Y' " +
                "ORDER BY t.TableName FETCH FIRST 1 ROWS ONLY",
                columnName);
            if (tableName != null && !tableName.isEmpty())
                return tableName;
        }
        return null;
    }

    private Map<String, String> loadLegalNames() {
        Map<String, String> out = new HashMap<>();
        String sql =
            "SELECT bp.Value, bp.Name FROM ZZSdfOrganisation org " +
            "  JOIN C_BPartner bp ON bp.C_BPartner_ID = org.C_BPartner_ID";
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

    private Map<String, TabStats> scanExcel(StreamingXlsxReader reader, TabMeta tab) throws Exception {
        Map<String, TabStats> out = new LinkedHashMap<>();
        if (tab.matchedSheets.isEmpty()) return out;
        if (tab.sdlColIdx < 0) {
            addLog("Skipping " + tab.tabName + ": SDLNumber column not found in mapping detail.");
            return out;
        }
        int dataStartIdx = Math.max(0, tab.startRow);
        for (String sheetName : tab.matchedSheets)
            scanOneSheet(reader, sheetName, tab, dataStartIdx, out);
        return out;
    }

    private void scanOneSheet(StreamingXlsxReader reader, String sheetName, TabMeta tab,
                              int dataStartIdx, Map<String, TabStats> out) throws Exception {
        final int[] emptyStreak = {0};
        reader.streamSheet(sheetName, dataStartIdx, null, (rowIdx, cells) -> {
            if (cells.isEmpty()) {
                if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                return StreamingXlsxReader.Action.CONTINUE;
            }
            emptyStreak[0] = 0;

            for (int idx : tab.ignoreIfBlankColIdx) {
                if (isBlankOrZero(cells.get(idx))) return StreamingXlsxReader.Action.CONTINUE;
            }

            String sdl = cells.getOrDefault(tab.sdlColIdx, "").trim();
            if (sdl.isEmpty()) sdl = BLANK_SDL;

            TabStats st = out.computeIfAbsent(sdl, k -> new TabStats(tab.numericCols.size()));
            st.count++;

            for (int i = 0; i < tab.numericCols.size(); i++) {
                int ci = tab.numericCols.get(i).excelColIdx;
                if (ci >= 0) st.sums[i] += parseNumber(cells.get(ci));
            }
            for (ColMeta col : tab.referenceCols) {
                if (col.excelColIdx < 0) continue;
                String raw = cells.getOrDefault(col.excelColIdx, "").trim();
                if (raw.isEmpty()) raw = BLANK_LABEL;
                st.categoryCounts
                  .computeIfAbsent(col.columnName, k -> new LinkedHashMap<>())
                  .merge(normaliseCategoryLabel(raw), 1L, Long::sum);
            }
            return StreamingXlsxReader.Action.CONTINUE;
        });
    }

    // -------------------------------------------------------------------------  db side

    /**
     * Queries row counts and numeric sums from the DB for this tab, then runs
     * one category-count query per reference column.
     */
    private Map<String, TabStats> queryDb(TabMeta tab) {
        Map<String, TabStats> out = new LinkedHashMap<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT bp.Value AS sdl, COUNT(*) AS cnt");
        for (ColMeta col : tab.numericCols) {
            // Cast to text + regex so varchar-stored numeric columns (e.g. ZZ_Finance_Value)
            // are handled the same way as in the Excel parser.
            String raw = "TRIM(CAST(c." + col.columnName + " AS TEXT))";
            sql.append(", COALESCE(SUM(CASE")
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]+(\\.[0-9]+)?$'")
               .append(" THEN ").append(raw).append("::numeric")
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(,[0-9]{3})+\\.[0-9]+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '')::numeric")
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(,[0-9]{3})+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '')::numeric")
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]{1,3}(\\.[0-9]{3})+,[0-9]+$'")
               .append(" THEN REPLACE(REPLACE(").append(raw).append(", '.', ''), ',', '.')::numeric")
               .append(" WHEN ").append(raw).append(" ~ '^-?[0-9]+,[0-9]+$'")
               .append(" THEN REPLACE(").append(raw).append(", ',', '.')::numeric")
               .append(" ELSE 0 END),0) AS ").append(col.columnName);
        }
        sql.append("  FROM ").append(tab.dbTable).append(" c")
           .append("  JOIN ZZ_WSP_ATR_Submitted s ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID")
           .append("  JOIN ZZSdfOrganisation org   ON org.ZZSdfOrganisation_ID  = s.ZZSdfOrganisation_ID")
           .append("  JOIN C_BPartner bp            ON bp.C_BPartner_ID          = org.C_BPartner_ID")
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
                TabStats st = new TabStats(tab.numericCols.size());
                st.count = rs.getLong("cnt");
                for (int i = 0; i < tab.numericCols.size(); i++) {
                    BigDecimal bd = rs.getBigDecimal(tab.numericCols.get(i).columnName);
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

        for (ColMeta col : tab.referenceCols)
            queryDbReference(tab, col, out);

        return out;
    }

    /**
     * Queries category counts for one reference column.
     *
     * For Table / TableDir / Search columns the FK is an integer ID; we join to the
     * resolved reference table and use its Name column.
     *
     * For List columns the FK is a string value; we join to AD_Ref_List (by
     * AD_Reference_ID + Value) and use its Name column.
     */
    private void queryDbReference(TabMeta tab, ColMeta col, Map<String, TabStats> out) {
        String labelExpr;
        String joinClause;

        if (col.isList) {
            labelExpr  = "LOWER(COALESCE(rl.Name, CAST(c." + col.columnName + " AS TEXT)))";
            joinClause = "LEFT JOIN AD_Ref_List rl" +
                         "    ON rl.AD_Reference_ID = " + col.adReferenceValueId +
                         "   AND rl.Value = c." + col.columnName;
        } else {
            // Table / TableDir / Search: PK is <refTable>_ID by iDempiere convention
            String refPk = col.refTableName + "_ID";
            labelExpr  = "LOWER(COALESCE(r.Name, CAST(c." + col.columnName + " AS TEXT)))";
            joinClause = "LEFT JOIN " + col.refTableName + " r" +
                         "    ON r." + refPk + " = c." + col.columnName;
        }

        String sql =
            "SELECT bp.Value AS sdl, " + labelExpr + " AS label, COUNT(*) AS cnt" +
            "  FROM " + tab.dbTable + " c" +
            "  JOIN ZZ_WSP_ATR_Submitted s ON s.ZZ_WSP_ATR_Submitted_ID = c.ZZ_WSP_ATR_Submitted_ID" +
            "  JOIN ZZSdfOrganisation org   ON org.ZZSdfOrganisation_ID  = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner bp            ON bp.C_BPartner_ID          = org.C_BPartner_ID" +
            "  " + joinClause +
            " WHERE c.IsActive='Y'" +
            " GROUP BY bp.Value, " + labelExpr;

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(sql, null);
            rs = pst.executeQuery();
            while (rs.next()) {
                String sdl = rs.getString("sdl");
                if (sdl == null || sdl.trim().isEmpty()) sdl = BLANK_SDL;
                String label = rs.getString("label");
                if (label == null || label.isEmpty()) label = BLANK_LABEL;
                label = normaliseCategoryLabel(label);
                long cnt = rs.getLong("cnt");
                out.computeIfAbsent(sdl.trim(), k -> new TabStats(tab.numericCols.size()))
                   .categoryCounts
                   .computeIfAbsent(col.columnName, k -> new LinkedHashMap<>())
                   .merge(label, cnt, Long::sum);
            }
        } catch (Exception e) {
            log.warning("DB reference query failed for " + tab.dbTable + "." + col.columnName + ": " + e.getMessage());
            addLog("DB reference query failed for " + tab.dbTable + "." + col.columnName + ": " + e.getMessage());
        } finally {
            DB.close(rs, pst);
        }
    }

    // -------------------------------------------------------------------------  report

    private File buildReport(List<TabMeta> tabs,
                             Map<String, Map<String, TabStats>> excel,
                             Map<String, Map<String, TabStats>> db,
                             Map<String, String> legalNames) throws Exception {
        File tmp = File.createTempFile("WSP_ATR_DynReconcile_", ".xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            CellStyle hdr = headerStyle(wb);
            CellStyle ok  = fillStyle(wb, IndexedColors.LIGHT_GREEN.getIndex());
            CellStyle bad = fillStyle(wb, IndexedColors.ROSE.getIndex());

            writeSummary      (wb.createSheet("Summary"),       tabs, excel, db, hdr, ok, bad);
            writeRowCounts    (wb.createSheet("Row Counts"),    tabs, excel, db, legalNames, hdr, ok, bad);
            writeNumericAggs  (wb.createSheet("Numeric Sums"),  tabs, excel, db, legalNames, hdr, ok, bad);
            writeDiscrepancies(wb.createSheet("Discrepancies"), tabs, excel, db, legalNames, hdr, bad);

            wb.write(fos);
        }
        return tmp;
    }

    private void writeSummary(Sheet sh, List<TabMeta> tabs,
                              Map<String, Map<String, TabStats>> excel,
                              Map<String, Map<String, TabStats>> db,
                              CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"Tab", "DB Table", "Matched Sheets",
                         "Excel Rows", "DB Rows", "Diff",
                         "SDLs Excel", "SDLs DB", "SDLs Mismatched", "Status"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        int r = 1;
        for (TabMeta tab : tabs) {
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

    private void writeRowCounts(Sheet sh, List<TabMeta> tabs,
                                Map<String, Map<String, TabStats>> excel,
                                Map<String, Map<String, TabStats>> db,
                                Map<String, String> legal,
                                CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Excel Count", "DB Count", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        String   baseName = sh.getSheetName();
        int[]    pageNum  = {2};
        Sheet    current  = sh;
        int r = 1;
        for (TabMeta tab : tabs) {
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                if (r > MAX_EXCEL_ROW) {
                    current = nextSheet(current, baseName, pageNum, cols, hdr);
                    r = 1;
                }
                long ec   = e.containsKey(sdl) ? e.get(sdl).count : 0;
                long dc   = d.containsKey(sdl) ? d.get(sdl).count : 0;
                long diff = ec - dc;
                CellStyle style = (diff == 0) ? null : bad;

                Row row = current.createRow(r++);
                cell(row, 0, sdl, style);
                cell(row, 1, legal.getOrDefault(sdl, ""), style);
                cell(row, 2, tab.tabName, style);
                cell(row, 3, String.valueOf(ec), style);
                cell(row, 4, String.valueOf(dc), style);
                cell(row, 5, String.valueOf(diff), style != null ? bad : ok);
            }
        }
        for (int i = 0; i < cols.length; i++) current.autoSizeColumn(i);
    }

    /**
     * Writes the Numeric Sums sheet.
     *
     * Numeric columns (Amount / Quantity / Number / Integer detected from AD_Column)
     * produce one row per SDL showing Excel sum vs DB sum.
     *
     * Reference columns (Table / TableDir / Search / List detected from AD_Column)
     * produce one row per SDL per distinct label (the Name from the reference table
     * or AD_Ref_List) showing Excel count vs DB count.
     *
     * No hardcoded column lists required — coverage extends automatically to
     * any column present in the mapping detail with a supported display type.
     */
    private void writeNumericAggs(Sheet sh, List<TabMeta> tabs,
                                  Map<String, Map<String, TabStats>> excel,
                                  Map<String, Map<String, TabStats>> db,
                                  Map<String, String> legal,
                                  CellStyle hdr, CellStyle ok, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Column", "Excel Sum", "DB Sum", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        String   baseName = sh.getSheetName();
        int[]    pageNum  = {2};
        Sheet    current  = sh;
        int r = 1;
        for (TabMeta tab : tabs) {
            if (tab.numericCols.isEmpty() && tab.referenceCols.isEmpty()) continue;
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                TabStats es = e.get(sdl);
                TabStats ds = d.get(sdl);

                // One row per numeric (SUM) column
                for (int i = 0; i < tab.numericCols.size(); i++) {
                    if (r > MAX_EXCEL_ROW) {
                        current = nextSheet(current, baseName, pageNum, cols, hdr);
                        r = 1;
                    }
                    double ev   = round2(es != null ? es.sums[i] : 0);
                    double dv   = round2(ds != null ? ds.sums[i] : 0);
                    double diff = round2(ev - dv);
                    CellStyle style = (diff == 0.0) ? null : bad;

                    Row row = current.createRow(r++);
                    cell   (row, 0, sdl, style);
                    cell   (row, 1, legal.getOrDefault(sdl, ""), style);
                    cell   (row, 2, tab.tabName, style);
                    cell   (row, 3, tab.numericCols.get(i).columnName, style);
                    numCell(row, 4, ev, style);
                    numCell(row, 5, dv, style);
                    numCell(row, 6, diff, diff != 0.0 ? bad : ok);
                }

                // One row per (reference column, label) — counts from Name in the ref table
                for (ColMeta col : tab.referenceCols) {
                    Map<String, Long> em = (es != null) ? es.categoryCounts.get(col.columnName) : null;
                    Map<String, Long> dm = (ds != null) ? ds.categoryCounts.get(col.columnName) : null;
                    TreeSet<String> labels = new TreeSet<>();
                    if (em != null) labels.addAll(em.keySet());
                    if (dm != null) labels.addAll(dm.keySet());

                    for (String label : labels) {
                        if (r > MAX_EXCEL_ROW) {
                            current = nextSheet(current, baseName, pageNum, cols, hdr);
                            r = 1;
                        }
                        long ec   = (em != null && em.get(label) != null) ? em.get(label) : 0L;
                        long dc   = (dm != null && dm.get(label) != null) ? dm.get(label) : 0L;
                        long diff = ec - dc;
                        CellStyle style = (diff == 0) ? null : bad;

                        Row row = current.createRow(r++);
                        cell   (row, 0, sdl, style);
                        cell   (row, 1, legal.getOrDefault(sdl, ""), style);
                        cell   (row, 2, tab.tabName, style);
                        cell   (row, 3, "COUNT(" + col.columnName + "=" + label + ")", style);
                        numCell(row, 4, ec, style);
                        numCell(row, 5, dc, style);
                        numCell(row, 6, diff, diff != 0 ? bad : ok);
                    }
                }
            }
        }
        for (int i = 0; i < cols.length; i++) current.autoSizeColumn(i);
    }

    private void writeDiscrepancies(Sheet sh, List<TabMeta> tabs,
                                    Map<String, Map<String, TabStats>> excel,
                                    Map<String, Map<String, TabStats>> db,
                                    Map<String, String> legal,
                                    CellStyle hdr, CellStyle bad) {
        Row h = sh.createRow(0);
        String[] cols = {"SDL Number", "Legal Name", "Tab", "Metric", "Excel", "DB", "Diff"};
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);

        List<String[]> rows = new ArrayList<>();
        for (TabMeta tab : tabs) {
            Map<String, TabStats> e = excel.getOrDefault(tab.tabName, Collections.emptyMap());
            Map<String, TabStats> d = db   .getOrDefault(tab.tabName, Collections.emptyMap());
            TreeSet<String> sdls = new TreeSet<>();
            sdls.addAll(e.keySet()); sdls.addAll(d.keySet());

            for (String sdl : sdls) {
                long ec = e.containsKey(sdl) ? e.get(sdl).count : 0;
                long dc = d.containsKey(sdl) ? d.get(sdl).count : 0;
                if (ec != dc)
                    rows.add(row(sdl, legal, tab.tabName, "ROW COUNT",
                            String.valueOf(ec), String.valueOf(dc), String.valueOf(ec - dc)));

                TabStats es = e.get(sdl);
                TabStats ds = d.get(sdl);
                for (int i = 0; i < tab.numericCols.size(); i++) {
                    double ev   = round2(es != null ? es.sums[i] : 0);
                    double dv   = round2(ds != null ? ds.sums[i] : 0);
                    double diff = round2(ev - dv);
                    if (diff != 0.0)
                        rows.add(row(sdl, legal, tab.tabName, tab.numericCols.get(i).columnName,
                                String.valueOf(ev), String.valueOf(dv), String.valueOf(diff)));
                }
                for (ColMeta col : tab.referenceCols) {
                    Map<String, Long> em = (es != null) ? es.categoryCounts.get(col.columnName) : null;
                    Map<String, Long> dm = (ds != null) ? ds.categoryCounts.get(col.columnName) : null;
                    TreeSet<String> labels = new TreeSet<>();
                    if (em != null) labels.addAll(em.keySet());
                    if (dm != null) labels.addAll(dm.keySet());
                    for (String label : labels) {
                        long ecount = (em != null && em.get(label) != null) ? em.get(label) : 0L;
                        long dcount = (dm != null && dm.get(label) != null) ? dm.get(label) : 0L;
                        if (ecount != dcount)
                            rows.add(row(sdl, legal, tab.tabName,
                                    "COUNT(" + col.columnName + "=" + label + ")",
                                    String.valueOf(ecount), String.valueOf(dcount),
                                    String.valueOf(ecount - dcount)));
                    }
                }
            }
        }

        String   baseName = sh.getSheetName();
        int[]    pageNum  = {2};
        Sheet    current  = sh;
        int r = 1;
        for (String[] rowData : rows) {
            if (r > MAX_EXCEL_ROW) {
                current = nextSheet(current, baseName, pageNum, cols, hdr);
                r = 1;
            }
            Row row = current.createRow(r++);
            for (int c = 0; c < rowData.length; c++) cell(row, c, rowData[c], bad);
        }
        for (int i = 0; i < cols.length; i++) current.autoSizeColumn(i);
    }

    private static String[] row(String sdl, Map<String, String> legal, String tab,
                                 String metric, String excel, String db, String diff) {
        return new String[]{sdl, legal.getOrDefault(sdl, ""), tab, metric, excel, db, diff};
    }

    // -------------------------------------------------------------------------  sheet overflow helper

    /**
     * Auto-sizes the current sheet, then creates the next continuation sheet
     * (e.g. "Numeric Sums (2)") with an identical header row and returns it.
     * {@code pageNum[0]} is incremented in place.
     */
    private Sheet nextSheet(Sheet current, String baseName, int[] pageNum,
                            String[] cols, CellStyle hdr) {
        for (int i = 0; i < cols.length; i++) current.autoSizeColumn(i);
        Sheet next = current.getWorkbook().createSheet(baseName + " (" + pageNum[0]++ + ")");
        Row h = next.createRow(0);
        for (int i = 0; i < cols.length; i++) cell(h, i, cols[i], hdr);
        return next;
    }

    // -------------------------------------------------------------------------  parsing helpers

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean isBlankOrZero(String txt) {
        if (txt == null) return true;
        String s = txt.trim();
        if (s.isEmpty()) return true;
        s = s.replace(" ", "").replace(",", "");
        try {
            return new BigDecimal(s).compareTo(BigDecimal.ZERO) == 0;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static double parseNumber(String s) {
        if (s == null) return 0;
        String t = s.trim();
        if (t.isEmpty()) return 0;
        if (t.charAt(0) == '\'') t = t.substring(1).trim();
        t = t.replace("R", "").replace("$", "").replace(" ", "").replace("\u00A0", "");
        if (t.isEmpty()) return 0;

        int lastDot   = t.lastIndexOf('.');
        int lastComma = t.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) { t = t.replace(".", ""); t = t.replace(',', '.'); }
            else                     { t = t.replace(",", ""); }
        } else if (lastComma >= 0) {
            if (t.matches("-?\\d{1,3}(,\\d{3})+")) t = t.replace(",", "");
            else                                    t = t.replace(',', '.');
        }
        try { return Double.parseDouble(t); }
        catch (NumberFormatException e) { return 0; }
    }

    private static String normaliseCategoryLabel(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (s.length() > 1 && s.endsWith("s"))
            s = s.substring(0, s.length() - 1);
        return s;
    }

    // -------------------------------------------------------------------------  column letter helpers

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

    // -------------------------------------------------------------------------  cell / style helpers

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

    private void numCell(Row row, int col, long value, CellStyle style) {
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
