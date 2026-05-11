package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.compiere.util.DB;

/**
 * Queries ZZ_WSP_ATR_Submitted and produces the same SDL × WSPStatus pivot
 * as SummariseMigrationTab so the two outputs can be compared side-by-side.
 *
 * Join path:
 *   ZZ_WSP_ATR_Submitted.ZZSdfOrganisation_ID
 *     → ZZSdfOrganisation.C_BPartner_ID
 *       → C_BPartner.Value  (SDL number)
 *       → C_BPartner.Name   (Legal Name)
 *   ZZ_WSP_ATR_Submitted.ZZ_DocStatus
 *     → AD_Ref_List.Name    (human-readable status, same text as the Excel column)
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.SummariseSubmittedWspStatus")
public class SummariseSubmittedWspStatus extends SvrProcess {

    // Reference list UUID that backs ZZ_DocStatus — same value used in ImportWspAtrMigrationFile.
    private static final String WSP_STATUS_REF_UU = "98479fb5-df5d-440d-86aa-92d77a320857";

    private static final String BLANK_SDL = "(blank)";

    private static final String SQL =
            "SELECT bp.Value                               AS sdl_number," +
            "       bp.Name                                AS legal_name," +
            "       COALESCE(rl.Name, s.ZZ_DocStatus)     AS wsp_status," +
            "       COUNT(*)                               AS cnt" +
            "  FROM ZZ_WSP_ATR_Submitted s" +
            "  JOIN ZZSdfOrganisation org ON org.ZZSdfOrganisation_ID = s.ZZSdfOrganisation_ID" +
            "  JOIN C_BPartner         bp  ON bp.C_BPartner_ID         = org.C_BPartner_ID" +
            "  LEFT JOIN AD_Ref_List   rl  ON rl.Value                 = s.ZZ_DocStatus" +
            "                             AND rl.AD_Reference_ID = (" +
            "                                 SELECT AD_Reference_ID FROM AD_Reference" +
            "                                  WHERE AD_Reference_UU = '" + WSP_STATUS_REF_UU + "')" +
            " GROUP BY bp.Value, bp.Name, COALESCE(rl.Name, s.ZZ_DocStatus)" +
            " ORDER BY bp.Value";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {

        // sdlNumber -> (statusName -> count)
        Map<String, Map<String, Integer>> pivot = new LinkedHashMap<>();
        // sdlNumber -> legal name
        Map<String, String> legalNames = new LinkedHashMap<>();
        // all unique statuses, sorted alphabetically
        Map<String, Void> statusSet = new TreeMap<>();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(SQL, get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String sdl    = rs.getString("sdl_number");
                String legal  = rs.getString("legal_name");
                String status = rs.getString("wsp_status");
                int    cnt    = rs.getInt("cnt");

                if (sdl    == null || sdl.trim().isEmpty())    sdl    = BLANK_SDL;
                if (legal  == null) legal  = "";
                if (status == null || status.trim().isEmpty()) status = "(none)";
                sdl    = sdl.trim();
                status = status.trim();

                statusSet.put(status, null);
                legalNames.putIfAbsent(sdl, legal);
                pivot.computeIfAbsent(sdl, k -> new LinkedHashMap<>())
                     .merge(status, cnt, Integer::sum);
            }
        } finally {
            DB.close(rs, pstmt);
        }

        if (pivot.isEmpty())
            return "No records found in ZZ_WSP_ATR_Submitted.";

        List<String> statuses = new ArrayList<>(statusSet.keySet());

        File out = buildExcel(pivot, legalNames, statuses);

        if (getProcessInfo().getProcessUI() != null)
            getProcessInfo().getProcessUI().download(out);

        return "Summary complete. " + pivot.size() + " SDL number(s), "
                + statuses.size() + " status(es).";
    }

    // -------------------------------------------------------------------------

    private File buildExcel(Map<String, Map<String, Integer>> pivot,
                            Map<String, String> legalNames,
                            List<String> statuses) throws Exception {

        File tmp = File.createTempFile("DB_WSP_Status_Summary_", ".xlsx");

        final int STATUS_OFFSET = 2; // col 0 = SDL Number, col 1 = Legal Name

        try (XSSFWorkbook wb = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tmp)) {

            Sheet sheet = wb.createSheet("WSP Status Summary (DB)");

            CellStyle headerStyle = makeHeaderStyle(wb);
            CellStyle totalStyle  = makeTotalStyle(wb);
            CellStyle numberStyle = makeNumberStyle(wb);
            CellStyle grandStyle  = makeGrandTotalStyle(wb);

            // ---- Header row ----
            Row hdr = sheet.createRow(0);
            setStyledCell(hdr, 0, "SDL Number", headerStyle);
            setStyledCell(hdr, 1, "Legal Name", headerStyle);
            for (int c = 0; c < statuses.size(); c++)
                setStyledCell(hdr, STATUS_OFFSET + c, statuses.get(c), headerStyle);
            setStyledCell(hdr, STATUS_OFFSET + statuses.size(), "Total", headerStyle);

            // ---- Data rows ----
            int rowNum = 1;
            int[] colTotals = new int[statuses.size()];
            int grandTotal  = 0;

            for (Map.Entry<String, Map<String, Integer>> entry : pivot.entrySet()) {
                String sdl    = entry.getKey();
                Map<String, Integer> counts = entry.getValue();

                Row row = sheet.createRow(rowNum++);
                CellStyle sdlStyle = sdl.equals(BLANK_SDL) ? totalStyle : null;
                setStyledCell(row, 0, sdl, sdlStyle);
                setStyledCell(row, 1, legalNames.getOrDefault(sdl, ""), sdlStyle);

                int rowTotal = 0;
                for (int c = 0; c < statuses.size(); c++) {
                    int cnt = counts.getOrDefault(statuses.get(c), 0);
                    setNumberCell(row, STATUS_OFFSET + c, cnt, numberStyle);
                    colTotals[c] += cnt;
                    rowTotal     += cnt;
                }
                grandTotal += rowTotal;
                setNumberCell(row, STATUS_OFFSET + statuses.size(), rowTotal, totalStyle);
            }

            // ---- TOTAL row ----
            Row totRow = sheet.createRow(rowNum);
            setStyledCell(totRow, 0, "TOTAL", grandStyle);
            setStyledCell(totRow, 1, "",      grandStyle);
            for (int c = 0; c < statuses.size(); c++)
                setNumberCell(totRow, STATUS_OFFSET + c, colTotals[c], grandStyle);
            setNumberCell(totRow, STATUS_OFFSET + statuses.size(), grandTotal, grandStyle);

            // ---- Auto-size columns ----
            for (int c = 0; c <= STATUS_OFFSET + statuses.size(); c++)
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
        s.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
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
        s.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
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
