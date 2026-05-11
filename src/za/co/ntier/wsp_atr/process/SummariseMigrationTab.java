package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.SummariseMigrationTab")
public class SummariseMigrationTab extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    private static final String BIODATA_SHEET    = "BioData";
    private static final String COL_SDL          = "SDLNumber";
    private static final String COL_STATUS       = "WSPStatus";
    private static final String BLANK_SDL        = "(blank)";
    private static final int    MAX_EMPTY        = 10;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile())
            throw new AdempiereException("File not found: " + BULK_UPLOAD_PATH);

        // sdlNumber -> (status -> count).  LinkedHashMap preserves SDL insertion order.
        Map<String, Map<String, Integer>> pivot = new LinkedHashMap<>();
        // All unique statuses, sorted alphabetically.
        Map<String, Void> statusSet = new TreeMap<>();

        int[] sdlColRef   = {-1};
        int[] statColRef  = {-1};
        int[] emptyStreak = {0};

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {
            if (!reader.hasSheet(BIODATA_SHEET))
                throw new AdempiereException("Sheet '" + BIODATA_SHEET + "' not found in file.");

            reader.streamSheet(BIODATA_SHEET, 0, null, (rowIdx, cells) -> {

                if (rowIdx == 0) {
                    // Locate SDL and status columns by header name.
                    for (Map.Entry<Integer, String> e : cells.entrySet()) {
                        String h = e.getValue().trim();
                        if (COL_SDL.equalsIgnoreCase(h))    sdlColRef[0]  = e.getKey();
                        if (COL_STATUS.equalsIgnoreCase(h)) statColRef[0] = e.getKey();
                    }
                    if (sdlColRef[0] < 0)
                        throw new AdempiereException("Column '" + COL_SDL + "' not found in BioData header row.");
                    if (statColRef[0] < 0)
                        throw new AdempiereException("Column '" + COL_STATUS + "' not found in BioData header row.");
                    return StreamingXlsxReader.Action.CONTINUE;
                }

                if (sdlColRef[0] < 0) return StreamingXlsxReader.Action.STOP; // header never found

                // Stop after MAX_EMPTY consecutive empty rows.
                if (cells.isEmpty()) {
                    if (++emptyStreak[0] > MAX_EMPTY) return StreamingXlsxReader.Action.STOP;
                    return StreamingXlsxReader.Action.CONTINUE;
                }
                emptyStreak[0] = 0;

                String sdl    = cells.getOrDefault(sdlColRef[0],  "").trim();
                String status = cells.getOrDefault(statColRef[0], "").trim();

                if (sdl.isEmpty())    sdl    = BLANK_SDL;
                if (status.isEmpty()) status = "(none)";

                statusSet.put(status, null);
                pivot.computeIfAbsent(sdl, k -> new LinkedHashMap<>())
                     .merge(status, 1, Integer::sum);

                return StreamingXlsxReader.Action.CONTINUE;
            });
        }

        if (pivot.isEmpty())
            return "No data rows found in '" + BIODATA_SHEET + "'.";

        List<String> statuses = new ArrayList<>(statusSet.keySet());

        File out = buildExcel(pivot, statuses);

        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(out);

        return "Summary complete. " + pivot.size() + " SDL number(s), "
                + statuses.size() + " status(es).";
    }

    // -------------------------------------------------------------------------

    private File buildExcel(Map<String, Map<String, Integer>> pivot,
                            List<String> statuses) throws Exception {

        File tmp = File.createTempFile("BioData_WSP_Status_Summary_", ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            Sheet sheet = wb.createSheet("WSP Status Summary");

            CellStyle headerStyle  = makeHeaderStyle(wb);
            CellStyle totalStyle   = makeTotalStyle(wb);
            CellStyle numberStyle  = makeNumberStyle(wb);
            CellStyle grandStyle   = makeGrandTotalStyle(wb);

            // ---- Header row ----
            Row hdr = sheet.createRow(0);
            setStyledCell(hdr, 0, "SDL Number", headerStyle);
            for (int c = 0; c < statuses.size(); c++)
                setStyledCell(hdr, c + 1, statuses.get(c), headerStyle);
            setStyledCell(hdr, statuses.size() + 1, "Total", headerStyle);

            // ---- Data rows ----
            int rowNum = 1;
            int[] colTotals = new int[statuses.size()];
            int grandTotal  = 0;

            for (Map.Entry<String, Map<String, Integer>> entry : pivot.entrySet()) {
                String sdl        = entry.getKey();
                Map<String, Integer> counts = entry.getValue();

                Row row  = sheet.createRow(rowNum++);
                CellStyle sdlStyle = sdl.equals(BLANK_SDL) ? totalStyle : null;
                setStyledCell(row, 0, sdl, sdlStyle);

                int rowTotal = 0;
                for (int c = 0; c < statuses.size(); c++) {
                    int cnt = counts.getOrDefault(statuses.get(c), 0);
                    setNumberCell(row, c + 1, cnt, numberStyle);
                    colTotals[c] += cnt;
                    rowTotal     += cnt;
                }
                grandTotal += rowTotal;
                setNumberCell(row, statuses.size() + 1, rowTotal, totalStyle);
            }

            // ---- TOTAL row ----
            Row totRow = sheet.createRow(rowNum);
            setStyledCell(totRow, 0, "TOTAL", grandStyle);
            for (int c = 0; c < statuses.size(); c++)
                setNumberCell(totRow, c + 1, colTotals[c], grandStyle);
            setNumberCell(totRow, statuses.size() + 1, grandTotal, grandStyle);

            // ---- Auto-size columns ----
            for (int c = 0; c <= statuses.size() + 1; c++)
                sheet.autoSizeColumn(c);

            wb.write(fos);
        }

        return tmp;
    }

    // -------------------------------------------------------------------------  styles

    private CellStyle makeHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        setBorder(s);
        return s;
    }

    private CellStyle makeTotalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(s);
        return s;
    }

    private CellStyle makeGrandTotalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(s);
        return s;
    }

    private CellStyle makeNumberStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        setBorder(s);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private void setBorder(CellStyle s) {
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void setStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        if (style != null) cell.setCellStyle(style);
    }

    private void setNumberCell(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) cell.setCellStyle(style);
    }
}
