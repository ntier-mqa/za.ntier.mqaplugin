package za.co.ntier.wsp_atr.process;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class WspAtrExportService {

    private static final String EXPORT_FILE_NAME = "ZZ_WSP_ATR_Submitted_Export";

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

        Path exportPath = uniqueTempXlsm(EXPORT_FILE_NAME);
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

    private Path uniqueTempXlsm(String baseName) throws IOException {
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        String stem = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path firstCandidate = tmpDir.resolve(stem + ".xlsm");

        for (int i = 0;; i++) {
            Path candidate = i == 0 ? firstCandidate : tmpDir.resolve(stem + "_" + i + ".xlsm");
            try {
                return Files.createFile(candidate);
            } catch (FileAlreadyExistsException ignore) {
            }
        }
    }
}
