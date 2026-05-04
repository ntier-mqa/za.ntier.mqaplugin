package za.ntier.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Export C_InvoiceBatch lines to CSV in the required format.
 *
 * Columns:
 *   Invoice Batch Reference | Invoice No. | Invoice Date | Month | Year | SDL No | Organisation Name | MG Grant Amount
 *
 * Trigger this process from a button on C_InvoiceBatch.
 * It will read the batch lines and download a CSV.
 *
 * Download is done via: processUI.download(new File(exportFile));
 */
@org.adempiere.base.annotation.Process(name="za.ntier.process.ExportInvoiceBatchToCsv")
public class ExportInvoiceBatchToCsv extends SvrProcess {

    private int m_Record_ID;
    private String p_FileName;

    @Override
    protected void prepare() {
        m_Record_ID = getRecord_ID();
        for (ProcessInfoParameter p : getParameter()) {
            if (p.getParameter() == null) continue;
            switch (p.getParameterName()) {
                case "FileName":
                    p_FileName = (String) p.getParameter();
                    break;
                default:
                    // ignore unknown params
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (m_Record_ID <= 0) {
            return "No C_InvoiceBatch selected.";
        }

        // Invoice Batch Reference
        String batchRef = DB.getSQLValueString(get_TrxName(),
                "SELECT COALESCE(DocumentNo, '') FROM C_InvoiceBatch WHERE C_InvoiceBatch_ID=?",
                m_Record_ID);
        if (batchRef == null || batchRef.trim().isEmpty()) {
            batchRef = "Batch-" + m_Record_ID;
        }
        String description = DB.getSQLValueString(get_TrxName(),
                "SELECT COALESCE(Description, '') FROM C_InvoiceBatch WHERE C_InvoiceBatch_ID=?",
                m_Record_ID);

        // Determine year/month — from description first, then from linked header record
        String yearMonth = extractYearMonth(description);
        String monthStr  = extractMonth(description);
        if (yearMonth == null) {
            int hdrId = DB.getSQLValue(get_TrxName(),
                    "SELECT COALESCE(ZZ_Monthly_Levy_Files_Hdr_ID,0) FROM C_InvoiceBatch WHERE C_InvoiceBatch_ID=?",
                    m_Record_ID);
            if (hdrId > 0) {
                monthStr = DB.getSQLValueString(get_TrxName(),
                        "SELECT ZZ_Month FROM ZZ_Monthly_Levy_Files_Hdr WHERE ZZ_Monthly_Levy_Files_Hdr_ID=?",
                        hdrId);
                String fiscalYear = DB.getSQLValueString(get_TrxName(),
                        "SELECT cy.FiscalYear FROM C_Year cy " +
                        "JOIN ZZ_Monthly_Levy_Files_Hdr h ON h.C_Year_ID = cy.C_Year_ID " +
                        "WHERE h.ZZ_Monthly_Levy_Files_Hdr_ID=?",
                        hdrId);
                if (fiscalYear != null && monthStr != null)
                    yearMonth = fiscalYear + monthStr;
                else if (fiscalYear != null)
                    yearMonth = fiscalYear;
            }
        }

        // Lines query (join invoice & BP)
        final String sql =
                "SELECT " +
                "  COALESCE(inv.DocumentNo, '') AS InvoiceNo, " +                 // 1
                "  l.DateInvoiced, " +                                            // 2
                "  bp.Value AS SDLNo, " +                                         // 3
                "  bp.Name AS OrgName, " +                                        // 4
                "  l.LineTotalAmt " +                                             // 5
                "FROM C_InvoiceBatchLine l " +
                "LEFT JOIN C_Invoice inv ON inv.C_Invoice_ID = l.C_Invoice_ID " +
                "LEFT JOIN C_BPartner bp ON bp.C_BPartner_ID = l.C_BPartner_ID " +
                "WHERE l.IsActive='Y' AND l.C_InvoiceBatch_ID=? " +
                "ORDER BY l.Line";

        List<List<Object>> rows = DB.getSQLArrayObjectsEx(get_TrxName(), sql, new Object[]{m_Record_ID});
        if (rows == null || rows.isEmpty()) {
            return "No lines found for batch " + batchRef + " (ID=" + m_Record_ID + ").";
        }

        // Output filename
        String baseName = (p_FileName != null && !p_FileName.trim().isEmpty())
                ? p_FileName.trim()
                : ("MG_" + (yearMonth != null ? yearMonth : "") + "_" + batchRef.replaceAll("[^A-Za-z0-9_-]", "_"));
       // if (!baseName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
       //    baseName += ".csv";
       // }

      
        Path exportPath = uniqueTempCsv(baseName);

        // CSV header + rows
        try (BufferedWriter w = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {

            // header
            writeRow(w,
                    "Invoice Batch Reference",
                    "Invoice No.",
                    "Invoice Date",
                    "Month",
                    "Year",
                    "SDL No",
                    "Organisation Name",
                    "MG Grant Amount"
            );

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            for (List<Object> r : rows) {
                String invoiceNo = safeStr(r.get(0));
                Timestamp ts = (Timestamp) r.get(1);
                String invoiceDate = (ts != null) ? df.format(ts) : "";

                
                String yearStr = "";
                if (ts != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTimeInMillis(ts.getTime());
                    int month = cal.get(java.util.Calendar.MONTH) + 1;
                    int year = cal.get(java.util.Calendar.YEAR);
                   // monthStr = Integer.toString(month);
                    yearStr = Integer.toString(year);
                }

                String sdl = safeStr(r.get(2));
                String org = safeStr(r.get(3));

                BigDecimal amt = (r.get(4) instanceof BigDecimal) ? (BigDecimal) r.get(4)
                        : (r.get(4) != null ? new java.math.BigDecimal(r.get(4).toString()) : java.math.BigDecimal.ZERO);

                writeRow(w,
                        batchRef,
                        invoiceNo,
                        invoiceDate,
                        monthStr,
                        yearStr,
                        sdl,
                        org,
                        amt.toPlainString()
                );
            }
            w.flush();
        }

        if (processUI != null) {
            processUI.download(exportPath.toFile());
        }

        return "CSV exported: " + exportPath;
    }

    // --- helpers ---
    
    public static Path uniqueTempCsv(String baseName) throws IOException {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String stem = "Export_" + baseName;           // e.g. Export_MG_202507_IVB100094
        Path p = tmpDir.resolve(stem + ".csv");

        // Ensure uniqueness without random gibberish
        for (int i = 0; ; i++) {
            Path candidate = (i == 0) ? p : tmpDir.resolve(stem + "_" + i + ".csv");
            try {
                return Files.createFile(candidate);   // CREATE_NEW semantics
            } catch (FileAlreadyExistsException ignore) {
                // try next suffix
            }
        }
    }
    
    public static String extractYearMonth(String text) {
        if (text == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "Year:\\s*(\\d{4})\\s*Month:\\s*([A-Za-z]+|\\d{1,2})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String year  = matcher.group(1);
            String month = resolveMonth(matcher.group(2));
            return month != null ? year + month : year;
        }
        return null;
    }

    public static String extractMonth(String text) {
        if (text == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "Year:\\s*\\d{4}\\s*Month:\\s*([A-Za-z]+|\\d{1,2})");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return resolveMonth(matcher.group(1));
        }
        return null;
    }

    private static String resolveMonth(String raw) {
        if (raw == null) return null;
        if (raw.matches("\\d{1,2}"))
            return String.format("%02d", Integer.parseInt(raw));
        switch (raw.substring(0, Math.min(3, raw.length())).toLowerCase(java.util.Locale.ROOT)) {
            case "jan": return "01"; case "feb": return "02"; case "mar": return "03";
            case "apr": return "04"; case "may": return "05"; case "jun": return "06";
            case "jul": return "07"; case "aug": return "08"; case "sep": return "09";
            case "oct": return "10"; case "nov": return "11"; case "dec": return "12";
            default:    return null;
        }
    }

    private static String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    private static void writeRow(BufferedWriter w, String... cols) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csv(cols[i]));
        }
        sb.append('\n');
        w.write(sb.toString());
    }

    private static String csv(String s) {
        if (s == null) return "";
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String val = s.replace("\"", "\"\"");
        return needQuote ? ("\"" + val + "\"") : val;
    }
}
