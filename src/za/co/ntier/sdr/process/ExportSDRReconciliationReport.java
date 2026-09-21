package za.co.ntier.sdr.process;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.MAttachment;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Reconciliation report: compares every staged MS SQL source table (mssdr_*) against its migrated
 * Postgres target table (sdr_*), with row counts on both sides, exported as a real .xlsx workbook via
 * Apache POI (already a working dependency of this plugin - see the ~40 existing
 * za.co.ntier.wsp_atr.process/za.ntier.process classes that import org.apache.poi.*).
 *
 * <p>Tables are discovered DYNAMICALLY from information_schema.tables (both prefixes), not from a
 * hardcoded list of every table this migration touched - this makes the report self-correcting: any
 * mssdr_* table that genuinely has no migrated counterpart shows up as "NOT MIGRATED" in its own right,
 * rather than the report silently agreeing with whatever this project's own code already assumed.
 *
 * <p>MATCHING STRATEGY: the ~168 main-family tables (Person/Organisation/SDF/WSPATR/Levy/Grant/User-
 * Security/Misc) all follow a simple, verified-reliable naming convention - "mssdr_x" -&gt; "sdr_x", a
 * plain prefix swap with the remainder unchanged (every single Add*Table.java class in this project
 * used exactly this pattern). The 72 reference/catalog tables do NOT follow that pattern (e.g.
 * "mssdr_lkptitle" -&gt; "sdr_title", "mssdr_wsplearningprogramme" -&gt; "sdr_learningprogramme") - for
 * those, this class reuses {@link AddSDRReferenceTables#SPECS} (the same {target, source} pairs that
 * built them) rather than re-deriving or re-typing the mapping a second time.
 *
 * <p>OUTPUT: attached to this process run's own AD_PInstance record (2026-09-16, mirrors
 * BulkLoadSgDocuments' {@code new MAttachment(ctx, tableId, recordId, null, trxName)} pattern) - the
 * user downloads it from the paperclip/Attachment icon on the Process Audit window for this run,
 * rather than needing server filesystem access to fetch a /tmp path.
 */
@Process(name = "za.co.ntier.sdr.process.ExportSDRReconciliationReport")
public class ExportSDRReconciliationReport extends SvrProcess {

    private static final String SOURCE_PREFIX = "mssdr_";
    private static final String TARGET_PREFIX = "sdr_";
    private static final Set<String> EXCLUDED_ID_COLUMNS = new HashSet<>(
            Arrays.asList("id", "ad_client_id", "ad_org_id"));

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    private static final class ReconRow {
        String sourceTable;
        String targetTable;
        long sourceCount;
        Long targetCount;
    }

    @Override
    protected String doIt() throws Exception {
        Set<String> targetTables = queryTableNames(TARGET_PREFIX + "%");
        List<String> sourceTables = new ArrayList<>(queryTableNames(SOURCE_PREFIX + "%"));
        Collections.sort(sourceTables);

        Map<String, String> knownReferenceMapping = buildKnownReferenceMapping();

        List<ReconRow> rows = new ArrayList<>();
        int matched = 0;
        int mismatched = 0;
        int notMigrated = 0;

        for (String sourceTable : sourceTables) {
            ReconRow row = new ReconRow();
            row.sourceTable = sourceTable;
            row.sourceCount = countRows(sourceTable);

            String candidate = TARGET_PREFIX + sourceTable.substring(SOURCE_PREFIX.length());
            String targetTable = targetTables.contains(candidate) ? candidate
                    : knownReferenceMapping.get(sourceTable);
            if (targetTable != null && !targetTables.contains(targetTable)) {
                targetTable = null;
            }

            if (targetTable != null) {
                row.targetTable = targetTable;
                row.targetCount = countRows(targetTable);
                if (row.targetCount.longValue() == row.sourceCount) {
                    matched++;
                } else {
                    mismatched++;
                }
            } else {
                notMigrated++;
            }
            rows.add(row);
        }

        Set<String> sourceTableSet = new HashSet<>(sourceTables);
        String fileName = attachExcel(rows, sourceTables.size(), targetTables.size(), matched, mismatched,
                notMigrated, sourceTableSet);

        return "SDR reconciliation: " + sourceTables.size() + " MS SQL (mssdr_) table(s), "
                + targetTables.size() + " Postgres (sdr_) table(s). " + matched + " matched, " + mismatched
                + " count mismatch, " + notMigrated + " not migrated. Download " + fileName
                + " from the Attachment icon on this Process Audit record.";
    }

    private Set<String> queryTableNames(String likePattern) {
        Set<String> names = new HashSet<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_name LIKE ?", get_TrxName());
            pstmt.setString(1, likePattern);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed listing tables matching " + likePattern, e);
        } finally {
            DB.close(rs, pstmt);
        }
        return names;
    }

    private long countRows(String tableName) {
        return DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM " + tableName);
    }

    /**
     * Reuses {@link AddSDRReferenceTables#SPECS} (the same {target, source} pairs used to build the 72
     * reference tables) rather than re-deriving or re-typing that irregular naming a second time.
     */
    private Map<String, String> buildKnownReferenceMapping() {
        Map<String, String> mapping = new HashMap<>();
        for (String[] spec : AddSDRReferenceTables.SPECS) {
            mapping.put(spec[1].toLowerCase(), spec[0].toLowerCase());
        }
        // mssdr_lkpsiccodechamber is deliberately NOT in AddSDRReferenceTables.SPECS (it's a
        // SICCode<->ChamberCode junction table, not a Value/Name lookup - see
        // AddSDRSICCodeChamberTable's Javadoc), so it needs its own explicit override here or it
        // would wrongly show as "NOT MIGRATED" despite sdr_siccodechamber existing and matching.
        mapping.put("mssdr_lkpsiccodechamber", "sdr_siccodechamber");
        return mapping;
    }

    private String attachExcel(List<ReconRow> rows, int sourceTableCount, int targetTableCount, int matched,
            int mismatched, int notMigrated, Set<String> knownSourceTables) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reconciliation");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SDR Migration Reconciliation - MS SQL (staged) vs Postgres (SDR_)");
            titleCell.setCellStyle(titleStyle);

            rowIdx++;
            writeSummaryLine(sheet, rowIdx++, "MS SQL (mssdr_) tables:", sourceTableCount);
            writeSummaryLine(sheet, rowIdx++, "Postgres (sdr_) tables:", targetTableCount);
            writeSummaryLine(sheet, rowIdx++, "Matched (counts equal):", matched);
            writeSummaryLine(sheet, rowIdx++, "Count mismatch:", mismatched);
            writeSummaryLine(sheet, rowIdx++, "Not migrated (no target table found):", notMigrated);
            rowIdx++;

            Row header = sheet.createRow(rowIdx++);
            String[] headers = { "MS SQL Table", "Postgres Table", "MS SQL Row Count", "Postgres Row Count",
                    "Difference", "Status" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ReconRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.sourceTable);
                row.createCell(1).setCellValue(r.targetTable != null ? r.targetTable : "(not migrated)");
                row.createCell(2).setCellValue(r.sourceCount);
                if (r.targetCount != null) {
                    row.createCell(3).setCellValue(r.targetCount);
                    row.createCell(4).setCellValue(r.targetCount - r.sourceCount);
                    row.createCell(5).setCellValue(r.targetCount.longValue() == r.sourceCount ? "OK" : "MISMATCH");
                } else {
                    row.createCell(3);
                    row.createCell(4);
                    row.createCell(5).setCellValue("NOT MIGRATED");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Set<String> usedSheetNames = new HashSet<>();
            usedSheetNames.add(sheet.getSheetName());
            for (ReconRow r : rows) {
                if (r.targetTable != null && r.targetCount != null && r.targetCount.longValue() != r.sourceCount) {
                    addMissingRowsSheet(workbook, headerStyle, r.sourceTable, r.targetTable, usedSheetNames,
                            knownSourceTables);
                }
            }

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "sdr-reconciliation-" + ts + ".xlsx";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            MAttachment attachment = new MAttachment(getCtx(), I_AD_PInstance.Table_ID, getAD_PInstance_ID(), null,
                    get_TrxName());
            attachment.addEntry(fileName, out.toByteArray());
            attachment.saveEx();

            return fileName;
        }
    }

    /**
     * For a MISMATCH table, dumps every source row whose id has no counterpart in the target table
     * into its own sheet - full column-for-column via ResultSetMetaData (no per-table column
     * knowledge needed here) - plus a derived "Likely Reason" column (see
     * {@link #computeLikelyReason}) explaining WHY each row didn't migrate, so a human doesn't have
     * to eyeball raw FK values to spot the sentinel/orphan pattern themselves.
     */
    private void addMissingRowsSheet(XSSFWorkbook workbook, CellStyle headerStyle, String sourceTable,
            String targetTable, Set<String> usedSheetNames, Set<String> knownSourceTables) throws Exception {
        String sql = "SELECT a.* FROM " + sourceTable + " a WHERE NOT EXISTS "
                + "(SELECT 1 FROM " + targetTable + " t WHERE t.id = a.id) ORDER BY a.id LIMIT 2000";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            rs = pstmt.executeQuery();
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            String[] columnNames = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = meta.getColumnName(i);
            }

            Sheet sheet = workbook.createSheet(uniqueSheetName(sourceTable, usedSheetNames));
            Row header = sheet.createRow(0);
            for (int i = 0; i < columnCount; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnNames[i]);
                cell.setCellStyle(headerStyle);
            }
            Cell reasonHeaderCell = header.createCell(columnCount);
            reasonHeaderCell.setCellValue("Likely Reason");
            reasonHeaderCell.setCellStyle(headerStyle);

            int rowIdx = 1;
            while (rs.next()) {
                Object[] values = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    values[i - 1] = rs.getObject(i);
                }
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columnCount; i++) {
                    row.createCell(i).setCellValue(values[i] == null ? "" : String.valueOf(values[i]));
                }
                row.createCell(columnCount).setCellValue(computeLikelyReason(columnNames, values, knownSourceTables));
            }
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Heuristic, dynamically-derived explanation for why a row has no migrated counterpart: scans
     * every *id-suffixed column (skipping id/ad_client_id/ad_org_id) and flags it as a likely cause
     * if its value is a non-positive "sentinel" (0/-1/null - this migration's convention for "no
     * parent") or, for a positive value, if it doesn't actually exist as an "id" in the plausibly-
     * named source table ("personid" -&gt; mssdr_person). The guessed source table name is checked
     * against {@code knownSourceTables} first so an unmatched guess (e.g. "roleid" -&gt;
     * mssdr_role, when the real table is mssdr_lkprole) is silently skipped rather than reported
     * wrong.
     */
    private String computeLikelyReason(String[] columnNames, Object[] values, Set<String> knownSourceTables) {
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < columnNames.length; i++) {
            String colName = columnNames[i].toLowerCase();
            if (!colName.endsWith("id") || EXCLUDED_ID_COLUMNS.contains(colName)) {
                continue;
            }
            Object value = values[i];
            if (value == null) {
                continue;
            }
            String valueStr = String.valueOf(value).trim();
            long numericValue;
            try {
                numericValue = Long.parseLong(valueStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (numericValue <= 0) {
                reasons.add(colName + "=" + numericValue + " (sentinel/no-parent value)");
                continue;
            }
            String guessedTable = SOURCE_PREFIX + colName.substring(0, colName.length() - 2);
            if (!knownSourceTables.contains(guessedTable)) {
                continue;
            }
            long count = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM " + guessedTable + " WHERE id = " + numericValue);
            if (count == 0) {
                reasons.add(colName + "=" + numericValue + " not found in " + guessedTable
                        + " (orphaned source reference)");
            }
        }
        return String.join("; ", reasons);
    }

    /** Excel sheet names: max 31 chars, must be unique within the workbook. */
    private String uniqueSheetName(String sourceTable, Set<String> usedSheetNames) {
        String base = "Missing_" + sourceTable;
        String candidate = base.length() > 31 ? base.substring(0, 31) : base;
        int suffix = 1;
        while (!usedSheetNames.add(candidate)) {
            String suffixStr = "_" + (++suffix);
            int cut = Math.min(base.length(), 31 - suffixStr.length());
            candidate = base.substring(0, cut) + suffixStr;
        }
        return candidate;
    }

    private void writeSummaryLine(Sheet sheet, int rowIdx, String label, int value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
