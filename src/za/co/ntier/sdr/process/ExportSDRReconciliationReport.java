package za.co.ntier.sdr.process;

import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
 * <p>OUTPUT: written to /tmp/sdr-reconciliation-&lt;timestamp&gt;.xlsx on the server filesystem (same
 * "write to /tmp, fetch via server access" pattern already used for this project's error logs) - this
 * process has no way to hand a file directly to the browser.
 */
@Process(name = "za.co.ntier.sdr.process.ExportSDRReconciliationReport")
public class ExportSDRReconciliationReport extends SvrProcess {

    private static final String SOURCE_PREFIX = "mssdr_";
    private static final String TARGET_PREFIX = "sdr_";

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

        String filePath = writeExcel(rows, sourceTables.size(), targetTables.size(), matched, mismatched,
                notMigrated);

        return "SDR reconciliation: " + sourceTables.size() + " MS SQL (mssdr_) table(s), "
                + targetTables.size() + " Postgres (sdr_) table(s). " + matched + " matched, " + mismatched
                + " count mismatch, " + notMigrated + " not migrated. Report written to " + filePath;
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
        return mapping;
    }

    private String writeExcel(List<ReconRow> rows, int sourceTableCount, int targetTableCount, int matched,
            int mismatched, int notMigrated) throws Exception {
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

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filePath = "/tmp/sdr-reconciliation-" + ts + ".xlsx";
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
            return filePath;
        }
    }

    private void writeSummaryLine(Sheet sheet, int rowIdx, String label, int value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
