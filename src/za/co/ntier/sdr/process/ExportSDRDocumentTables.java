package za.co.ntier.sdr.process;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Phase 10 step 1 (file attachment loading - see user request 2026-09-16): discovery pass that
 * finds every SDR_ table carrying a filepath/filename-shaped column, before any attachment-loading
 * code is written. Confirmed from the Table and Column screenshots the user supplied that these
 * columns don't follow one single naming convention across tables (SDR_OrganisationDocuments has
 * only SDR_FilePath; SDR_WSPATRDocumentUploads has SDR_FilePath + SDR_OriginalFileName +
 * presumably SDR_SavedFileName) - this scans information_schema dynamically rather than assuming a
 * table list, the same reasoning as {@link ExportSDRReconciliationReport}.
 *
 * <p>OUTPUT: attached to this process run's own AD_PInstance record (same pattern as
 * {@link ExportSDRReconciliationReport}) - one summary row per matching table (which columns
 * matched, total rows, rows with a non-blank filepath value).
 */
@Process(name = "za.co.ntier.sdr.process.ExportSDRDocumentTables")
public class ExportSDRDocumentTables extends SvrProcess {

    private static final String TARGET_PREFIX = "sdr_";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    private static final class TableInfo {
        String tableName;
        List<String> matchingColumns = new ArrayList<>();
        long totalRows;
        String filePathColumn;
        long rowsWithFilePath;
    }

    @Override
    protected String doIt() throws Exception {
        Map<String, TableInfo> tables = new LinkedHashMap<>();

        String sql = "SELECT table_name, column_name FROM information_schema.columns "
                + "WHERE table_name LIKE 'sdr\\_%' ESCAPE '\\' "
                + "AND (column_name ILIKE '%filepath%' OR column_name ILIKE '%filename%') "
                + "ORDER BY table_name, ordinal_position";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String columnName = rs.getString("column_name");
                TableInfo info = tables.computeIfAbsent(tableName, t -> {
                    TableInfo i = new TableInfo();
                    i.tableName = t;
                    return i;
                });
                info.matchingColumns.add(columnName);
                // sdr_savedfilename (the GUID-based stored name) is the confirmed match key against
                // the on-disk files (2026-09-16) - sdr_filepath is known-stale (old server paths).
                if (columnName.toLowerCase().contains("savedfilename")) {
                    info.filePathColumn = columnName;
                } else if (info.filePathColumn == null && columnName.toLowerCase().contains("filepath")) {
                    info.filePathColumn = columnName;
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed listing filepath/filename columns", e);
        } finally {
            DB.close(rs, pstmt);
        }

        for (TableInfo info : tables.values()) {
            info.totalRows = DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM " + info.tableName);
            if (info.filePathColumn != null) {
                info.rowsWithFilePath = DB.getSQLValueEx(get_TrxName(),
                        "SELECT COUNT(*) FROM " + info.tableName + " WHERE " + info.filePathColumn
                                + " IS NOT NULL AND " + info.filePathColumn + " <> ''");
            }
        }

        String fileName = attachExcel(tables.values());

        return "Found " + tables.size() + " SDR_ table(s) with a filepath/filename-shaped column. "
                + "Download " + fileName + " from the Attachment icon on this Process Audit record.";
    }

    private String attachExcel(java.util.Collection<TableInfo> tables) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("SDR Document Tables");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);

            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            String[] headers = { "Table", "Matching Columns", "Match Key Column Used", "Total Rows",
                    "Rows With Non-Blank Match Key" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (TableInfo info : tables) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(info.tableName);
                row.createCell(1).setCellValue(String.join(", ", info.matchingColumns));
                row.createCell(2).setCellValue(info.filePathColumn != null ? info.filePathColumn : "(none)");
                row.createCell(3).setCellValue(info.totalRows);
                row.createCell(4).setCellValue(info.rowsWithFilePath);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "sdr-document-tables-" + ts + ".xlsx";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            MAttachment attachment = new MAttachment(getCtx(), I_AD_PInstance.Table_ID, getAD_PInstance_ID(), null,
                    get_TrxName());
            attachment.addEntry(fileName, out.toByteArray());
            attachment.saveEx();

            return fileName;
        }
    }
}
