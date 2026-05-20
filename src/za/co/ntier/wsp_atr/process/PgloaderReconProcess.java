package za.co.ntier.wsp_atr.process;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.PgloaderReconProcess")
public class PgloaderReconProcess extends SvrProcess {

    private static final String MSSQL_URL =
        "jdbc:sqlserver://ntierdev.thruhere.net:1400;encrypt=true;trustServerCertificate=true;databaseName=MQA";
    private static final String MSSQL_USER = "SA";
    private static final String MSSQL_PASS = "Dazzle123";

    private static final String SOURCE_LABEL = "mssql://SA@ntierdev.thruhere.net:1400/MQA";
    private static final String TARGET_LABEL = "postgresql://postgres@mqauat.thruhere.net:5401/mqa";

    @Override
    protected void prepare() {}

    @Override
    protected String doIt() throws Exception {
        List<ReconEntry> entries = new ArrayList<>();

        entries.add(buildEntry(
            "WSPLearningProgramme",
            "dbo.WSPLearningProgramme",
            "adempiere.zz_wsplearningprogramme"
        ));

        Path excelPath = writeReconExcel(entries);

        if (processUI != null) {
            processUI.download(excelPath.toFile());
        }

        long srcTotal = entries.stream().mapToLong(e -> e.sourceCount).sum();
        long tgtTotal = entries.stream().mapToLong(e -> e.targetCount).sum();
        boolean allMatch = entries.stream().allMatch(e -> e.sourceCount == e.targetCount);

        return String.format(
            "Recon complete — Source: %d | Target: %d | Diff: %d | Status: %s",
            srcTotal, tgtTotal, srcTotal - tgtTotal, allMatch ? "MATCH" : "MISMATCH"
        );
    }

    private ReconEntry buildEntry(String label, String mssqlTable, String pgTable) throws Exception {
        long sourceCount = countMssql("SELECT COUNT(*) FROM " + mssqlTable);
        long targetCount = countPostgres("SELECT COUNT(*) FROM " + pgTable);
        return new ReconEntry(label, mssqlTable, pgTable, sourceCount, targetCount);
    }

    private long countMssql(String sql) throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        try (Connection conn = DriverManager.getConnection(MSSQL_URL, MSSQL_USER, MSSQL_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private long countPostgres(String sql) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        } finally {
            DB.close(rs, pstmt);
        }
    }

    // ─── Excel ──────────────────────────────────────────────────────────────

    private Path writeReconExcel(List<ReconEntry> entries) throws Exception {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path out = tmpDir.resolve("PgloaderRecon_" + ts + ".xlsx");

        try (Workbook wb = new XSSFWorkbook();
             OutputStream os = Files.newOutputStream(out)) {

            Sheet sheet = wb.createSheet("Reconciliation");
            Styles s = new Styles(wb);

            int r = 0;

            // Title
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(22);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("pgloader Migration Reconciliation Report");
            titleCell.setCellStyle(s.title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Blank row
            sheet.createRow(r++);

            // Run metadata
            addLabelValue(sheet, r++, s.label, s.value, "Generated",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            addLabelValue(sheet, r++, s.label, s.value, "Source DB", SOURCE_LABEL);
            addLabelValue(sheet, r++, s.label, s.value, "Target DB", TARGET_LABEL);

            // Blank row
            sheet.createRow(r++);

            // Column headers
            String[] headers = {
                "Table", "Source Table (MSSQL)", "Target Table (PostgreSQL)",
                "Source Count", "Target Count", "Difference", "Status"
            };
            Row hdrRow = sheet.createRow(r++);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = hdrRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(s.colHeader);
            }

            // Data rows
            for (ReconEntry entry : entries) {
                Row row = sheet.createRow(r++);
                long diff = entry.sourceCount - entry.targetCount;
                boolean match = diff == 0;

                row.createCell(0).setCellValue(entry.label);
                row.createCell(1).setCellValue(entry.mssqlTable);
                row.createCell(2).setCellValue(entry.pgTable);
                row.createCell(3).setCellValue(entry.sourceCount);
                row.createCell(4).setCellValue(entry.targetCount);
                row.createCell(5).setCellValue(diff);
                Cell statusCell = row.createCell(6);
                statusCell.setCellValue(match ? "MATCH" : "MISMATCH");
                statusCell.setCellStyle(match ? s.pass : s.fail);
            }

            // Blank row
            sheet.createRow(r++);

            // Overall status footer
            long totalSrc = entries.stream().mapToLong(e -> e.sourceCount).sum();
            long totalTgt = entries.stream().mapToLong(e -> e.targetCount).sum();
            boolean overallMatch = totalSrc == totalTgt;

            Row summaryRow = sheet.createRow(r++);
            Cell summaryLabel = summaryRow.createCell(0);
            summaryLabel.setCellValue("OVERALL STATUS");
            summaryLabel.setCellStyle(s.colHeader);
            sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 5));

            Cell summaryStatus = summaryRow.createCell(6);
            summaryStatus.setCellValue(overallMatch ? "PASS" : "FAIL");
            summaryStatus.setCellStyle(overallMatch ? s.pass : s.fail);

            // Auto-size
            for (int c = 0; c < headers.length; c++) {
                sheet.autoSizeColumn(c);
            }

            wb.write(os);
        }

        return out;
    }

    private void addLabelValue(Sheet sheet, int rowIdx, CellStyle labelStyle, CellStyle valueStyle,
                                String label, String value) {
        Row row = sheet.createRow(rowIdx);
        Cell lbl = row.createCell(0);
        lbl.setCellValue(label + ":");
        lbl.setCellStyle(labelStyle);
        Cell val = row.createCell(1);
        val.setCellValue(value);
        val.setCellStyle(valueStyle);
    }

    // ─── Style holder ────────────────────────────────────────────────────────

    private static final class Styles {
        final CellStyle title;
        final CellStyle label;
        final CellStyle value;
        final CellStyle colHeader;
        final CellStyle pass;
        final CellStyle fail;

        Styles(Workbook wb) {
            Font boldLg = wb.createFont();
            boldLg.setBold(true);
            boldLg.setFontHeightInPoints((short) 14);

            Font bold = wb.createFont();
            bold.setBold(true);

            title = wb.createCellStyle();
            title.setFont(boldLg);
            title.setAlignment(HorizontalAlignment.CENTER);
            title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title.setFont(titleFont);

            label = wb.createCellStyle();
            label.setFont(bold);

            value = wb.createCellStyle();

            colHeader = wb.createCellStyle();
            colHeader.setFont(bold);
            colHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            colHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            colHeader.setBorderBottom(BorderStyle.THIN);
            colHeader.setBorderTop(BorderStyle.THIN);

            pass = wb.createCellStyle();
            pass.setFont(bold);
            pass.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            pass.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pass.setAlignment(HorizontalAlignment.CENTER);

            fail = wb.createCellStyle();
            fail.setFont(bold);
            fail.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            fail.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            fail.setAlignment(HorizontalAlignment.CENTER);
        }
    }

    // ─── Data model ──────────────────────────────────────────────────────────

    private static final class ReconEntry {
        final String label;
        final String mssqlTable;
        final String pgTable;
        final long sourceCount;
        final long targetCount;

        ReconEntry(String label, String mssqlTable, String pgTable, long sourceCount, long targetCount) {
            this.label = label;
            this.mssqlTable = mssqlTable;
            this.pgTable = pgTable;
            this.sourceCount = sourceCount;
            this.targetCount = targetCount;
        }
    }
}
