package za.co.ntier.wsp_atr.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;

final class WspAtrExportSheetWriter {
    private static final int MAX_ROWS_PER_SHEET_INCLUDING_HEADER = 1_000_000;
    private static final int MAX_DATA_ROWS_PER_SHEET = MAX_ROWS_PER_SHEET_INCLUDING_HEADER - 1;

    private static final Set<String> IGNORED_STANDARD_COLUMNS = Set.of(
            "IsActive",
            "Created",
            "CreatedBy",
            "Updated",
            "UpdatedBy",
            "UUID");

    private final ExportSubmittedWspAtrToXlsm process;
    private final WspAtrExportValueFormatter valueFormatter;

    WspAtrExportSheetWriter(ExportSubmittedWspAtrToXlsm process, WspAtrExportValueFormatter valueFormatter) {
        this.process = process;
        this.valueFormatter = valueFormatter;
    }

    int writeSheet(Workbook workbook, WspAtrExportTab exportTab) {
        List<PO> records = exportTab.getRowProvider().fetch(process, exportTab.getTabContext());
        List<WspAtrSheetColumn> columns = resolveColumns(exportTab);
        if (columns.isEmpty()) {
            throw new IllegalStateException("No exportable columns found for tab " + exportTab.getTabContext().getTabUu());
        }

        CellStyle headerStyle = createHeaderStyle(workbook);
        int totalRows = records.size();
        int offset = 0;
        int sheetSuffix = 1;

        while (offset < totalRows || (totalRows == 0 && sheetSuffix == 1)) {
            int rowsForSheet = Math.min(MAX_DATA_ROWS_PER_SHEET, totalRows - offset);
            Sheet sheet = workbook.createSheet(createSheetName(workbook, exportTab.getSheetName(), sheetSuffix));
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.size(); col++) {
                Cell headerCell = headerRow.createCell(col);
                headerCell.setCellValue(columns.get(col).getHeader());
                headerCell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (int i = 0; i < rowsForSheet; i++) {
                PO record = records.get(offset + i);
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < columns.size(); col++) {
                    columns.get(col).writeCell(valueFormatter, row.createCell(col), record);
                }
            }

            for (int col = 0; col < columns.size(); col++) {
                sheet.autoSizeColumn(col);
            }

            if (totalRows == 0) {
                break;
            }
            offset += rowsForSheet;
            sheetSuffix++;
        }

        return totalRows;
    }

    private String createSheetName(Workbook workbook, String baseSheetName, int sheetSuffix) {
        String suffix = sheetSuffix <= 1 ? "" : "_" + sheetSuffix;
        int maxBaseLength = Math.max(1, 31 - suffix.length());
        String base = baseSheetName.length() > maxBaseLength
                ? baseSheetName.substring(0, maxBaseLength)
                : baseSheetName;
        String candidate = base + suffix;
        int collision = 1;
        while (workbook.getSheet(candidate) != null) {
            String collisionSuffix = suffix + "_" + collision++;
            int collisionBaseLength = Math.max(1, 31 - collisionSuffix.length());
            String collisionBase = baseSheetName.length() > collisionBaseLength
                    ? baseSheetName.substring(0, collisionBaseLength)
                    : baseSheetName;
            candidate = collisionBase + collisionSuffix;
        }
        return candidate;
    }

    private List<WspAtrSheetColumn> resolveColumns(WspAtrExportTab exportTab) {
        Map<String, WspAtrSheetColumn> actualColumns = new LinkedHashMap<>();
        Map<Integer, String> fieldHeaderCache = new HashMap<>();
        TabContext tabContext = exportTab.getTabContext();

        List<PO> fieldRows = new Query(process.getCtx(), "AD_Field",
                "AD_Tab_ID=? AND IsActive='Y' AND IsDisplayed='Y'",
                process.get_TrxName())
                        .setParameters(tabContext.getAdTabId())
                        .setOrderBy("SeqNo, AD_Field_ID")
                        .list();

        for (PO fieldRow : fieldRows) {
            int columnId = fieldRow.get_ValueAsInt("AD_Column_ID");
            if (columnId <= 0) {
                continue;
            }

            MColumn column = new MColumn(process.getCtx(), columnId, process.get_TrxName());
            if (column.getAD_Column_ID() <= 0 || column.isVirtualColumn() || shouldIgnoreColumn(tabContext, column)) {
                continue;
            }

            String header = fieldHeaderCache.computeIfAbsent(fieldRow.get_ID(),
                    key -> resolveFieldHeader(fieldRow, column));
            actualColumns.putIfAbsent(normalize(column.getColumnName()), new WspAtrTableColumn(column, header));
        }

        List<WspAtrSheetColumn> resolvedColumns = new ArrayList<>();
        if (tabContext.getTabLevel() > 0) {
            resolvedColumns.addAll(resolveSharedSubmissionColumns());
        }

        if (exportTab.isIncludeDocumentNo()
                && !actualColumns.containsKey(normalize(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo))) {
            resolvedColumns.add(new WspAtrSyntheticColumn(
                    I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo,
                    exportTab.getDocumentNoProvider()));
        }

        resolvedColumns.addAll(actualColumns.values());
        return resolvedColumns;
    }

    private List<WspAtrSheetColumn> resolveSharedSubmissionColumns() {
        Map<Integer, PO> submittedCache = new HashMap<>();
        Map<Integer, PO> organisationCache = new HashMap<>();

        List<WspAtrSheetColumn> columns = new ArrayList<>();
        columns.add(new WspAtrSyntheticColumn("Legal Name", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "LegalName", "legalname", "OrgName", "orgname", "Name", "name")));
        columns.add(new WspAtrSyntheticColumn("Trade  Name", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "TradingName", "tradingname", "TradeName", "tradename", "OrgName", "orgname", "Name", "name")));
        columns.add(new WspAtrSyntheticColumn("SDLNumber", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "ZZ_SDL_No", "zz_sdl_no", "SDLNo", "sdlno", "Value", "value")));
        columns.add(new WspAtrSyntheticColumn("FinancialYear", record ->
                resolveFinancialYear(resolveSubmitted(record, submittedCache))));
        columns.add(new WspAtrSyntheticColumn("WSPStatus", record ->
                resolveWspStatus(resolveSubmitted(record, submittedCache))));
        columns.add(new WspAtrSyntheticColumn("Organisation Size", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "OrganisationSize", "organisation_size", "ZZ_OrganisationSize", "zz_organisationsize")));
        columns.add(new WspAtrSyntheticColumn("Organisation Sub Sector", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "SubSector", "subsector", "OrganisationSubSector", "organisation_sub_sector", "ZZ_SubSector")));
        columns.add(new WspAtrSyntheticColumn("Org Province", record ->
                getOrganisationField(resolveOrganisation(record, submittedCache, organisationCache),
                        "Province", "province", "OrgProvince", "orgprovince", "ZZ_Province")));
        return columns;
    }

    private PO resolveSubmitted(PO childRecord, Map<Integer, PO> submittedCache) {
        if (childRecord == null) {
            return null;
        }
        int submittedId = childRecord.get_ValueAsInt(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_WSP_ATR_Submitted_ID);
        if (submittedId <= 0) {
            return null;
        }
        return submittedCache.computeIfAbsent(submittedId, id ->
                MTable.get(process.getCtx(), I_ZZ_WSP_ATR_Submitted.Table_ID).getPO(id, process.get_TrxName()));
    }

    private PO resolveOrganisation(PO childRecord, Map<Integer, PO> submittedCache, Map<Integer, PO> organisationCache) {
        PO submitted = resolveSubmitted(childRecord, submittedCache);
        if (submitted == null) {
            return null;
        }
        int orgId = submitted.get_ValueAsInt(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZSdfOrganisation_ID);
        if (orgId <= 0) {
            return null;
        }
        return organisationCache.computeIfAbsent(orgId, id -> new Query(process.getCtx(),
                "zzsdforganisation_v", "zzsdforganisation_v_id=?", process.get_TrxName())
                        .setParameters(id)
                        .firstOnly());
    }

    private String resolveFinancialYear(PO submitted) {
        if (submitted == null) {
            return "";
        }
        int yearId = submitted.get_ValueAsInt(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_FinYear_ID);
        if (yearId <= 0) {
            return "";
        }
        PO year = new Query(process.getCtx(), "C_Year", "C_Year_ID=?", process.get_TrxName())
                .setParameters(yearId)
                .firstOnly();
        if (year == null) {
            return "";
        }
        return firstNonBlank(year, "FiscalYear", "Year", "Name");
    }

    private String resolveWspStatus(PO submitted) {
        if (submitted == null) {
            return "";
        }
        String status = firstNonBlank(submitted,
                I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_DocStatus,
                I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_WSP_ATR_Status);
        if (Util.isEmpty(status, true)) {
            return "";
        }
        String normalized = status.trim();
        if ("SU".equalsIgnoreCase(normalized)) {
            return "Submitted";
        }
        if ("AP".equalsIgnoreCase(normalized)) {
            return "Approved";
        }
        if ("QR".equalsIgnoreCase(normalized)) {
            return "Query";
        }
        if ("UP".equalsIgnoreCase(normalized)) {
            return "Uploaded";
        }
        return normalized;
    }

    private String getOrganisationField(PO orgRecord, String... candidates) {
        if (orgRecord == null) {
            return "";
        }
        String value = firstNonBlank(orgRecord, candidates);
        return value == null ? "" : value;
    }

    private String firstNonBlank(PO record, String... columnNames) {
        if (record == null || columnNames == null) {
            return null;
        }
        for (String columnName : columnNames) {
            if (Util.isEmpty(columnName, true) || record.get_ColumnIndex(columnName) < 0) {
                continue;
            }
            Object value = record.get_Value(columnName);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private String resolveFieldHeader(PO fieldRow, MColumn column) {
        PO translatedField = new Query(process.getCtx(), "AD_Field_Trl", "AD_Field_ID=? AND AD_Language=?",
                process.get_TrxName())
                        .setParameters(fieldRow.get_ID(), "en_ZA")
                        .firstOnly();
        if (translatedField != null) {
            String translatedName = translatedField.get_ValueAsString("Name");
            if (!Util.isEmpty(translatedName, true)) {
                return translatedName.trim();
            }
        }

        String fieldName = fieldRow.get_ValueAsString("Name");
        if (!Util.isEmpty(fieldName, true)) {
            return fieldName.trim();
        }

        return column.getColumnName();
    }

    private boolean shouldIgnoreColumn(TabContext tabContext, MColumn column) {
        String columnName = column.getColumnName();
        if (Util.isEmpty(columnName, true)) {
            return true;
        }

        if (IGNORED_STANDARD_COLUMNS.stream().anyMatch(ignored -> ignored.equalsIgnoreCase(columnName))) {
            return true;
        }

        String tableName = tabContext.getTable().getTableName();
        return columnName.equalsIgnoreCase(tableName + "_ID")
                || columnName.equalsIgnoreCase(tableName + "_UU");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }
}
