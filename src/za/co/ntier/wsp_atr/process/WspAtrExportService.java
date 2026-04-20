package za.co.ntier.wsp_atr.process;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;

final class WspAtrExportService {

    private static final String EXPORT_FILE_NAME_TEMPLATE = "MQAWSPATRDataDump %s File";

    private final ExportSubmittedWspAtrToXlsm process;
    private final WspAtrExportPlanBuilder planBuilder;
    private final WspAtrExportSheetWriter sheetWriter;

    WspAtrExportService(ExportSubmittedWspAtrToXlsm process) {
        this.process = process;
        this.planBuilder = new WspAtrExportPlanBuilder(process);
        this.sheetWriter = new WspAtrExportSheetWriter(process, new WspAtrExportValueFormatter(process));
    }

    WspAtrExportResult export() throws Exception {
        List<WspAtrExportTab> exportTabs = planBuilder.build();
        if (exportTabs.isEmpty()) {
            throw new IllegalStateException("No tabs configured for export");
        }

        Path exportPath = buildTimestampedTempXlsx(resolveExportFileNameBase());
        int totalRowsExported = 0;

        try (Workbook workbook = new XSSFWorkbook()) {
            for (WspAtrExportTab exportTab : exportTabs) {
                totalRowsExported += sheetWriter.writeSheet(workbook, exportTab);
            }

            try (OutputStream outputStream = Files.newOutputStream(exportPath)) {
                workbook.write(outputStream);
            }
        }

        return new WspAtrExportResult(exportPath, totalRowsExported, exportTabs.size());
    }

    private Path buildTimestampedTempXlsx(String baseName) {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String stem = baseName.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return tmpDir.resolve(stem + "_" + timestamp + ".xlsx");
    }

    private String resolveExportFileNameBase() {
        if (!process.hasFiscalYearFilter()) {
            return String.format(EXPORT_FILE_NAME_TEMPLATE, "Year");
        }

        String yearText = resolveFiscalYearText(process.getFiscalYearId());
        return String.format(EXPORT_FILE_NAME_TEMPLATE, yearText);
    }

    private String resolveFiscalYearText(int fiscalYearId) {
        PO year = new Query(process.getCtx(), "C_Year", "C_Year_ID=?", process.get_TrxName())
                .setParameters(fiscalYearId)
                .firstOnly();
        if (year == null) {
            return String.valueOf(fiscalYearId);
        }

        String yearValue = year.get_ValueAsString("Year");
        if (!Util.isEmpty(yearValue, true)) {
            return yearValue.trim();
        }

        Object nameValue = year.get_Value("Name");
        return nameValue == null ? String.valueOf(fiscalYearId) : String.valueOf(nameValue).trim();
    }
}
