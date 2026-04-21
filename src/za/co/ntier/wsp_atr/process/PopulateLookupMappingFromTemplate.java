package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;

/**
 * PopulateLookupMappingFromTemplate
 *
 * - Reads the uploaded WSP/ATR XLSM template.
 * - For each sheet/tab (excluding "Welcome" and "Lookup" tabs):
 *      * Creates/updates header row in ZZ_WSP_ATR_Lookup_Mapping with ZZ_Tab_Name = sheet name.
 *      * Reads header row (row 1 / index 0) and for each non-empty header:
 *          - Creates/updates ZZ_WSP_ATR_Lookup_Mapping_Detail:
 *              * ZZ_Header_Name = header text
 *              * AD_Table_ID = reference table (if found) guessed as ZZ_<Header>_Ref
 *                otherwise left null.
 */
@org.adempiere.base.annotation.Process(
        name = "za.ntier.wsp_atr.process.PopulateLookupMappingFromTemplate"
)
public class PopulateLookupMappingFromTemplate extends SvrProcess {

    @Parameter(name = "FileName")
    private String filePath;   // Path to XLSM/XLSX template on server

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            // Just validate unknown parameters; FileName is injected by @Parameter
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {

        if (Util.isEmpty(filePath, true)) {
            throw new AdempiereException("FileName parameter is empty. Please select the template file.");
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found or not a regular file: " + filePath);
        }

        int headersCreated = 0;
        int headersUpdated = 0;
        int detailsCreated = 0;
        int detailsUpdated = 0;

        // Raise POI's per-record byte-array cap before opening the file.
        // The default is 100 MB; some XLSM templates exceed this, causing:
        //   "the maximum length for this record type is 100,000,000"
        // -1 means "use built-in default" (still 100 MB), so we use MAX_VALUE instead.
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        Workbook workbook;
        try (InputStream is = new FileInputStream(file)) {
            workbook = WorkbookFactory.create(is);
        } finally {
            // Restore the default limit so other code in this JVM is not affected
            IOUtils.setByteArrayMaxOverride(-1);
        }

        DataFormatter formatter = new DataFormatter();

        // Loop all sheets
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            boolean isHidden = false;
            if (workbook instanceof XSSFWorkbook) {
                XSSFWorkbook xwb = (XSSFWorkbook) workbook;
                if (xwb.isSheetHidden(i) || xwb.isSheetVeryHidden(i)) {
                    isHidden = true;
                }
            } else if (workbook instanceof HSSFWorkbook) {
                HSSFWorkbook hwb = (HSSFWorkbook) workbook;
                if (hwb.isSheetHidden(i) || hwb.isSheetVeryHidden(i)) {
                    isHidden = true;
                }
            }
            if (isHidden) {
                Sheet hiddenSheet = workbook.getSheetAt(i);
                addLog("Skipping hidden sheet: " + hiddenSheet.getSheetName());
                continue;
            }
            // --------------------------------

            Sheet sheet = workbook.getSheetAt(i);
            if (sheet == null) {
                continue;
            }

            String sheetName = sheet.getSheetName();
            if (Util.isEmpty(sheetName, true)) {
                continue;
            }

            String sNameLower = sheetName.toLowerCase(Locale.ROOT).trim();
            // Exclude "Welcome" and "Lookup" tabs
            if ("welcome".equals(sNameLower) || sNameLower.startsWith("lookup")) {
                addLog("Skipping sheet: " + sheetName);
                continue;
            }
           

            // Header record in ZZ_WSP_ATR_Lookup_Mapping
            X_ZZ_WSP_ATR_Lookup_Mapping header = (X_ZZ_WSP_ATR_Lookup_Mapping) new Query(
                    getCtx(),
                    I_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                    I_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_Tab_Name + "=? AND " +
                    		I_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_Is_For_Bulk + " = 'Y'"
                    ,
                    get_TrxName()
            ).setParameters(sheetName)
             .first();

            boolean isNewHeader = false;
            if (header == null) {
                header = new X_ZZ_WSP_ATR_Lookup_Mapping(getCtx(), 0, get_TrxName());
                header.setZZ_Tab_Name(sheetName);
                header.setZZ_Is_For_Bulk(true);
                header.saveEx();
                headersCreated++;
                isNewHeader = true;
            } else {
                // Nothing else to update for now, but count as updated for reporting
                headersUpdated++;
            }

            addLog("Processing sheet '" + sheetName + "' ("
                    + (isNewHeader ? "new" : "existing") + " header ID=" + header.get_ID() + ")");
            
            int headerRowIndex = findHeaderRow(sheet);
            Row headerRow = sheet.getRow(headerRowIndex);

            if (headerRow == null) {
                addLog("Sheet '" + sheetName + "' has no detectable header row, skipping details");
                continue;
            }


           

            int lastCellNum = headerRow.getLastCellNum();
            if (lastCellNum <= 0) {
                addLog("Sheet '" + sheetName + "' has empty header row, skipping details");
                continue;
            }

            for (int col = 0; col < lastCellNum; col++) {
                Cell headerCell = headerRow.getCell(col);
                if (headerCell == null) {
                    continue;
                }

                String headerText = formatter.formatCellValue(headerCell).trim();
                if (Util.isEmpty(headerText, true)) {
                    continue;
                }

                // Find or create detail row for this header
                X_ZZ_WSP_ATR_Lookup_Mapping_Detail detail =
                        (X_ZZ_WSP_ATR_Lookup_Mapping_Detail) new Query(
                                getCtx(),
                                I_ZZ_WSP_ATR_Lookup_Mapping_Detail.Table_Name,
                                I_ZZ_WSP_ATR_Lookup_Mapping_Detail.COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID + "=? AND "
                                        + I_ZZ_WSP_ATR_Lookup_Mapping_Detail.COLUMNNAME_ZZ_Header_Name + "=?",
                                get_TrxName()
                        )
                        .setParameters(header.get_ID(), headerText)
                        .first();

                boolean isNewDetail = false;
                boolean isCopiedFromTemplate = false;
                if (detail == null) {
                    String colLetter = toExcelColumnLetter(headerCell.getColumnIndex());
                    // Try to copy from a matching non-bulk detail for the same tab
                    X_ZZ_WSP_ATR_Lookup_Mapping_Detail templateDetail = findNonBulkDetail(sheetName, headerText);
                    if (templateDetail != null) {
                        detail = new X_ZZ_WSP_ATR_Lookup_Mapping_Detail(getCtx(), 0, get_TrxName());
                        detail.setZZ_WSP_ATR_Lookup_Mapping_ID(header.get_ID());
                        detail.setZZ_Header_Name(templateDetail.getZZ_Header_Name());
                        detail.setZZ_Column_Letter(colLetter);
                        detail.setAD_Table_ID(templateDetail.getAD_Table_ID());
                        detail.setAD_Column_ID(templateDetail.getAD_Column_ID());
                        detail.setIgnore_If_Blank(templateDetail.isIgnore_If_Blank());
                        detail.setIsMandatory(templateDetail.isMandatory());
                        detail.setZZ_Create_If_Not_Exists(templateDetail.isZZ_Create_If_Not_Exists());
                        detail.setZZ_Is_Formula(templateDetail.isZZ_Is_Formula());
                        detail.setZZ_Name_Column_Letter(templateDetail.getZZ_Name_Column_Letter());
                        detail.setZZ_Row_No(templateDetail.getZZ_Row_No());
                        detail.setZZ_Use_Value(templateDetail.isZZ_Use_Value());
                        detail.setZZ_Value_Column_Letter(templateDetail.getZZ_Value_Column_Letter());
                        isCopiedFromTemplate = true;
                        addLog("Header '" + headerText + "' on sheet '" + sheetName
                                + "' copied from non-bulk template detail, column=" + colLetter);
                    } else {
                        detail = new X_ZZ_WSP_ATR_Lookup_Mapping_Detail(getCtx(), 0, get_TrxName());
                        detail.setZZ_WSP_ATR_Lookup_Mapping_ID(header.get_ID());
                        detail.setZZ_Header_Name(headerText);
                        detail.setZZ_Column_Letter(colLetter);
                    }
                    isNewDetail = true;
                }

                if (!isCopiedFromTemplate) {
                    // Try to guess reference table name from header
                    Integer adTableId = findReferenceTableIdForHeader(headerText);
                    if (adTableId != null && adTableId > 0 && detail.getAD_Table_ID() <= 0) {
                        detail.setAD_Table_ID(adTableId);
                        addLog("Header '" + headerText + "' on sheet '" + sheetName
                                + "' mapped to AD_Table_ID=" + adTableId);
                    } else if (!isNewDetail || detail.getAD_Table_ID() <= 0) {
                        detail.setAD_Table_ID(0);
                        addLog("Header '" + headerText + "' on sheet '" + sheetName
                                + "' has no matching reference table (left blank).");
                    }
                }

                if (isNewDetail) {
                    detail.saveEx();
                    detailsCreated++;
                } else {
                    detail.saveEx();
                    detailsUpdated++;
                }
            }
        }

        workbook.close();

        String summary = "Lookup mapping populated from template.\n"
                + "Headers created: " + headersCreated
                + ", updated: " + headersUpdated
                + "; Details created: " + detailsCreated
                + ", updated: " + detailsUpdated;

        addLog(summary);
        return summary;
    }

    /**
     * Try to find AD_Table_ID for a header name using ZZ_<Header>_Ref pattern.
     * Example:
     *   "Province"   -> "ZZ_Province_Ref"
     *   "NQF Level"  -> "ZZ_NQF_Level_Ref"
     */
    private Integer findReferenceTableIdForHeader(String headerText) {
        String tableName = buildTableNameFromHeader(headerText);
        if (Util.isEmpty(tableName, true)) {
            return null;
        }

        MTable t = new Query(getCtx(), MTable.Table_Name, "TableName=?", get_TrxName())
                .setParameters(tableName)
                .first();

        if (t != null && t.getAD_Table_ID() > 0) {
            return t.getAD_Table_ID();
        }
        return null;
    }

    /**
     * Builds the AD table name from a column header.
     * Example: "Province"  -> "ZZ_Province_Ref"
     *          "NQF Level" -> "ZZ_NQF_Level_Ref"
     */
    private String buildTableNameFromHeader(String header) {
        if (header == null) {
            return null;
        }
        String h = header.trim();

        // Replace spaces and illegal chars with underscore, keep existing underscores
        h = h.replaceAll("[^A-Za-z0-9_]", "_");
        h = h.replaceAll("_+", "_"); // collapse multiple underscores
        if (!h.isEmpty() && Character.isDigit(h.charAt(0))) {
            h = "_" + h;
        }

        return "ZZ_" + h + "_Ref";
    }
    
    private int countNonEmptyCells(Row row) {
        if (row == null)
            return 0;
        int lastCell = row.getLastCellNum();
        if (lastCell < 0)
            return 0;

        int count = 0;
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            if (cell == null)
                continue;
            switch (cell.getCellType()) {
                case STRING:
                    if (!Util.isEmpty(cell.getStringCellValue(), true))
                        count++;
                    break;
                case NUMERIC:
                case BOOLEAN:
                case FORMULA:
                    count++;
                    break;
                default:
                    break;
            }
        }
        return count;
    }

    /**
     * Find the “user” header row.
     * - Row 0 is the technical header.
     * - Header row is the first later row with enough non-empty cells.
     */
    private int findHeaderRow(Sheet sheet) {
        if (sheet == null)
            return 0;

        Row row0 = sheet.getRow(0);
        int baseCount = countNonEmptyCells(row0);
        if (baseCount <= 0)
            return 0;

        int threshold = Math.max(1, baseCount / 2);
        int maxRowToCheck = Math.min(sheet.getLastRowNum(), 10); // look only near the top

        for (int r = 1; r <= maxRowToCheck; r++) {
            Row row = sheet.getRow(r);
            int cnt = countNonEmptyCells(row);
            if (cnt >= threshold) {
                return r;   // this is the user header row
            }
        }

        // Fallback if nothing matched
        return 0;
    }
    
    /**
     * Finds a detail record linked to the non-bulk header for the given tab name and header text.
     * Returns null if no non-bulk header or no matching detail exists.
     */
    private X_ZZ_WSP_ATR_Lookup_Mapping_Detail findNonBulkDetail(String tabName, String headerText) {
        X_ZZ_WSP_ATR_Lookup_Mapping nonBulkHeader =
                (X_ZZ_WSP_ATR_Lookup_Mapping) new Query(
                        getCtx(),
                        I_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                        I_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_Tab_Name + "=? AND "
                                + I_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_Is_For_Bulk + "='N'",
                        get_TrxName()
                )
                .setParameters(tabName)
                .first();

        if (nonBulkHeader == null) {
            return null;
        }

        return (X_ZZ_WSP_ATR_Lookup_Mapping_Detail) new Query(
                getCtx(),
                I_ZZ_WSP_ATR_Lookup_Mapping_Detail.Table_Name,
                I_ZZ_WSP_ATR_Lookup_Mapping_Detail.COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID + "=? AND "
                        + I_ZZ_WSP_ATR_Lookup_Mapping_Detail.COLUMNNAME_ZZ_Header_Name + "=?",
                get_TrxName()
        )
        .setParameters(nonBulkHeader.get_ID(), headerText)
        .first();
    }

    /**
     * Convert zero-based column index to Excel column letters.
     * 0  -> A
     * 25 -> Z
     * 26 -> AA
     * 27 -> AB
     * 55 -> BC
     */
    private String toExcelColumnLetter(int columnIndex) {
        StringBuilder column = new StringBuilder();
        int index = columnIndex;

        while (index >= 0) {
            column.insert(0, (char) ('A' + (index % 26)));
            index = (index / 26) - 1;
        }

        return column.toString();
    }


}
