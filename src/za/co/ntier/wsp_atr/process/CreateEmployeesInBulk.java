package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

/**
 * Bulk-creates ZZ_WSP_Employees reference records from the Biodata sheet of the
 * WSP/ATR migration spreadsheet.  Uses the same streaming reader and column-mapping
 * infrastructure as {@link ImportWspAtrMigrationFile} but skips all submission /
 * detail-line creation — it only ensures a ZZ_WSP_Employees row exists for every
 * unique employee identifier found in the sheet.
 *
 * <p>Safe to run multiple times; existing records are left untouched.</p>
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.CreateEmployeesInBulk")
public class CreateEmployeesInBulk extends SvrProcess {

    private static final String BULK_UPLOAD_PATH =
            "/home/ntier/SG_wsp_120626/MQAWSPATRDataDump2026_01062026.xlsx";

    /**
     * Quick-test SDL filter — same pattern as ImportWspAtrMigrationFile.
     * Leave empty for a full run.
     */
    private static final Set<String> SDL_FILTER = new HashSet<>(java.util.Arrays.asList(
            // "L010712109"
    ));

    /** AD_Ref_Table_UU that resolves to the ZZ_WSP_Employees reference table config. */
    private static final String EMPLOYEE_REF_TABLE_UU = "47ecb061-680b-4198-86e6-48c6d66fbb12";

    private final ReferenceLookupService refService = new ReferenceLookupService();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found or not a regular file: " + BULK_UPLOAD_PATH);
        }

        // Resolve the AD_Ref_Table_ID for ZZ_WSP_Employees once up front.
        int employeeRefTableId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_Reference_ID FROM AD_Ref_Table WHERE AD_Ref_Table_UU = ?",
                EMPLOYEE_REF_TABLE_UU);
        if (employeeRefTableId <= 0) {
            throw new AdempiereException(
                    "Setup error: AD_Ref_Table not found for UU=" + EMPLOYEE_REF_TABLE_UU
                    + ". Verify the ZZ_WSP_Employees reference table configuration.");
        }

        // Find the Biodata mapping header among all active bulk mappings.
        X_ZZ_WSP_ATR_Lookup_Mapping biodataHeader = findBiodataHeader();
        if (biodataHeader == null) {
            throw new AdempiereException(
                    "No active bulk mapping header found whose tab name contains 'Biodata'.");
        }

        EmployeeSheetProcessor processor = new EmployeeSheetProcessor(refService, this);

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {

            String mappingName = biodataHeader.getZZ_Tab_Name();
            List<String> matchingSheets = reader.findMatchingSheets(mappingName);
            if (matchingSheets.isEmpty()) {
                throw new AdempiereException(
                        "No sheet matching mapping '" + mappingName + "' found in workbook.");
            }

            Map<Integer, AbstractMappingSheetImporter.ColumnMeta> metas =
                    processor.buildColumnMeta(getCtx(), biodataHeader, get_TrxName());
            if (metas.isEmpty()) {
                throw new AdempiereException("No column metadata found for Biodata mapping.");
            }

            // Identify which ColumnMeta entries target ZZ_WSP_Employees.
            List<AbstractMappingSheetImporter.ColumnMeta> employeeMetas =
                    processor.findEmployeeMetas(metas, employeeRefTableId);
            if (employeeMetas.isEmpty()) {
                throw new AdempiereException(
                        "No column in the Biodata mapping targets the ZZ_WSP_Employees "
                        + "reference table (AD_Ref_Table_UU=" + EMPLOYEE_REF_TABLE_UU + ").");
            }

            int sr = biodataHeader.getStart_Row() != null ? biodataHeader.getStart_Row().intValue() : 4;
            if (sr <= 0) sr = 4;
            final int startRow = sr;

            Set<Integer> wanted = processor.collectWantedColumns(metas);

            final int[] created  = {0};
            final int[] skipped  = {0};
            final int[] emptyCounter = {0};
            final boolean[] stopAll  = {false};
            final List<ImportWspAtrMigrationFile.MigrationError> errors = new ArrayList<>();
            // Tracks employee values already handled in this run to avoid duplicate DB work.
            final Set<String> processedValues = new HashSet<>();

            final AbstractMappingSheetImporter.ColumnMeta sdlMeta =
                    processor.findColumnByHeader(metas, "SDLNumber");

            for (final String sheetName : matchingSheets) {
                reader.streamSheet(sheetName, startRow, wanted, (rowIdx, cells) -> {
                    if (stopAll[0]) {
                        return StreamingXlsxReader.Action.STOP;
                    }

                    if (processor.isMappedRowEmpty(cells, metas.values())) {
                        emptyCounter[0]++;
                        if (emptyCounter[0] > EmployeeSheetProcessor.MAX_EMPTY_ROWS) {
                            return StreamingXlsxReader.Action.STOP;
                        }
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                    emptyCounter[0] = 0;

                    if (processor.shouldIgnoreRowMap(cells, metas.values())) {
                        return StreamingXlsxReader.Action.CONTINUE;
                    }

                    if (!SDL_FILTER.isEmpty()) {
                        if (sdlMeta == null) {
                            return StreamingXlsxReader.Action.CONTINUE;
                        }
                        String sdlTxt = cells.get(sdlMeta.columnIndex);
                        if (Util.isEmpty(sdlTxt, true)
                                || !SDL_FILTER.contains(sdlTxt.trim())) {
                            return StreamingXlsxReader.Action.CONTINUE;
                        }
                    }

                    for (AbstractMappingSheetImporter.ColumnMeta empMeta : employeeMetas) {
                        String mainText  = cells.get(empMeta.columnIndex);
                        String valueText = (empMeta.valueColumnIndex != null)
                                ? cells.get(empMeta.valueColumnIndex.intValue()) : null;
                        String nameText  = (empMeta.nameColumnIndex  != null)
                                ? cells.get(empMeta.nameColumnIndex.intValue())  : null;

                        boolean allBlank = Util.isEmpty(mainText,  true)
                                && Util.isEmpty(valueText, true)
                                && Util.isEmpty(nameText,  true);
                        if (allBlank) {
                            continue;
                        }

                        // Mirror the value/name resolution from setValueFromTextOrCreate.
                        String cleanMain  = Util.isEmpty(mainText,  true) ? null : mainText.trim();
                        String cleanValue = Util.isEmpty(valueText, true) ? null : valueText.trim();
                        String cleanName  = Util.isEmpty(nameText,  true) ? null : nameText.trim();

                        String valueToUse = cleanValue;
                        String nameToUse  = cleanName;
                        if (valueToUse == null && empMeta.useValueForRef && cleanMain != null) {
                            valueToUse = cleanMain;
                        }
                        if (nameToUse == null && !empMeta.useValueForRef && cleanMain != null) {
                            nameToUse = cleanMain;
                        }
                        if (valueToUse == null && nameToUse == null && cleanMain != null) {
                            nameToUse = cleanMain;
                        }

                        // Strip all non-alphanumeric characters that Excel sometimes injects.
                        if (valueToUse != null) {
                            valueToUse = valueToUse.replaceAll("[^a-zA-Z0-9]", "");
                        }

                        String lookupKey = valueToUse != null ? valueToUse : nameToUse;
                        if (lookupKey == null) {
                            continue;
                        }
                        // Deduplicate within this run.
                        if (!processedValues.add(lookupKey.toUpperCase())) {
                            continue;
                        }

                        try {
                            boolean wasCreated = processor.findOrCreateEmployee(
                                    getCtx(), empMeta, valueToUse, nameToUse, cleanMain,
                                    get_TrxName());
                            if (wasCreated) {
                                created[0]++;
                                if (created[0] % 1000 == 0) {
                                    try {
                                        DB.commit(true, get_TrxName());
                                    } catch (java.sql.SQLException e2) {
                                        throw new AdempiereException(
                                                "Batch commit failed after " + created[0] + " records", e2);
                                    }
                                }
                            } else {
                                skipped[0]++;
                            }
                        } catch (Exception e) {
                            errors.add(new ImportWspAtrMigrationFile.MigrationError(
                                    sheetName, rowIdx + 1,
                                    processor.columnIndexToLetter(empMeta.columnIndex),
                                    "Error creating employee: " + e.getMessage()));
                            if (errors.size() >= EmployeeSheetProcessor.MAX_ERRORS) {
                                stopAll[0] = true;
                                return StreamingXlsxReader.Action.STOP;
                            }
                        }
                    }

                    return StreamingXlsxReader.Action.CONTINUE;
                });
            }

            if (!errors.isEmpty()) {
                File logFile = processor.writeErrorLog(errors);
                if (processUI != null && logFile != null && logFile.exists()) {
                    processUI.download(logFile);
                }
                throw new AdempiereException("Completed with " + errors.size()
                        + " error(s). See log file for details.");
            }

            return "Processed " + (created[0] + skipped[0]) + " unique employee value(s): "
                    + created[0] + " ZZ_WSP_Employees record(s) created, "
                    + skipped[0] + " already existed.";
        }
    }

    private X_ZZ_WSP_ATR_Lookup_Mapping findBiodataHeader() {
        List<X_ZZ_WSP_ATR_Lookup_Mapping> all =
                new Query(getCtx(), X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                        "ZZ_Is_For_Bulk = 'Y'", get_TrxName())
                .setOnlyActiveRecords(true)
                .setOrderBy("seqNo")
                .list();
        for (X_ZZ_WSP_ATR_Lookup_Mapping h : all) {
            if (h.getZZ_Tab_Name() != null
                    && h.getZZ_Tab_Name().toLowerCase().contains("biodata")) {
                return h;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Inner processor — extends AbstractMappingSheetImporter for shared helpers
    // -------------------------------------------------------------------------
    private static final class EmployeeSheetProcessor extends AbstractMappingSheetImporter {

        static final int MAX_EMPTY_ROWS = 10;
        static final int MAX_ERRORS     = 500;

        EmployeeSheetProcessor(ReferenceLookupService refService, SvrProcess process) {
            super(refService, process);
        }

        /** Not used — streaming path drives everything. */
        @Override
        public int importData(Properties ctx, Workbook wb, X_ZZ_WSP_ATR_Submitted submitted,
                              X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader, String trxName,
                              DataFormatter formatter) throws IllegalStateException, SQLException {
            return 0;
        }

        Map<Integer, ColumnMeta> buildColumnMeta(Properties ctx,
                                                  X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                                                  String trxName) {
            Map<Integer, ColumnMeta> metas = new HashMap<>();
            for (var det : loadDetails(mappingHeader, trxName)) {
                if (Util.isEmpty(det.getZZ_Column_Letter(), true)) {
                    continue;
                }
                ColumnMeta meta = new ColumnMeta();
                meta.columnIndex      = columnLetterToIndex(det.getZZ_Column_Letter());
                meta.headerName       = det.getZZ_Header_Name();
                meta.useValueForRef   = det.isZZ_Use_Value();
                meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();
                meta.mandatory        = det.isMandatory();
                meta.ignoreIfBlank    = det.isIgnore_If_Blank();
                String valueColLetter = det.getZZ_Value_Column_Letter();
                String nameColLetter  = det.getZZ_Name_Column_Letter();
                if (!Util.isEmpty(valueColLetter, true)) {
                    meta.valueColumnIndex = Integer.valueOf(columnLetterToIndex(valueColLetter));
                }
                if (!Util.isEmpty(nameColLetter, true)) {
                    meta.nameColumnIndex = Integer.valueOf(columnLetterToIndex(nameColLetter));
                }
                if (det.getAD_Column_ID() > 0) {
                    meta.column = new MColumn(ctx, det.getAD_Column_ID(), trxName);
                    if (Util.isEmpty(meta.column.getColumnName(), true)) {
                        svrProcess.addLog("Skipping detail — MColumn " + det.getAD_Column_ID()
                                + " not found (letter " + det.getZZ_Column_Letter() + ")");
                        continue;
                    }
                    int ref = meta.column.getAD_Reference_ID();
                    meta.isRefColumn = ref == DisplayType.Table
                            || ref == DisplayType.TableDir
                            || ref == DisplayType.Search;
                }
                metas.put(Integer.valueOf(meta.columnIndex), meta);
            }
            return metas;
        }

        /** Returns ColumnMeta entries whose reference column points to the employee ref table. */
        List<ColumnMeta> findEmployeeMetas(Map<Integer, ColumnMeta> metas, int employeeRefTableId) {
            List<ColumnMeta> result = new ArrayList<>();
            for (ColumnMeta meta : metas.values()) {
                if (meta.column == null || !meta.createIfNotExist || !meta.isRefColumn) {
                    continue;
                }
                if (meta.column.getAD_Reference_Value_ID() == employeeRefTableId) {
                    result.add(meta);
                }
            }
            return result;
        }

        /**
         * Looks up an existing ZZ_WSP_Employees record by Value/Name; creates it when not found.
         * Mirrors the lookup+create portion of {@link AbstractMappingSheetImporter#setValueFromTextOrCreate}
         * without requiring a parent PO.
         *
         * @return {@code true} if a new record was created, {@code false} if it already existed.
         */
        boolean findOrCreateEmployee(Properties ctx, ColumnMeta meta,
                                     String valueToUse, String nameToUse,
                                     String cleanMain, String trxName) {

            int adRefTableId = meta.column.getAD_Reference_Value_ID();
            MRefTable refTableCfg = MRefTable.get(ctx, adRefTableId, trxName);
            if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0) {
                return false;
            }
            MTable refTable = MTable.get(ctx, refTableCfg.getAD_Table_ID());
            if (refTable == null || refTable.getAD_Table_ID() <= 0) {
                return false;
            }
            String refTableName = refTable.getTableName();

            // Look up by Value first, then by Name.
            Integer foundId = null;
            if (meta.useValueForRef && !Util.isEmpty(valueToUse, true)) {
                foundId = findIdByColumn(ctx, refTableName, "Value", valueToUse, trxName);
            }
            if ((foundId == null || foundId <= 0) && cleanMain != null) {
                foundId = findIdByColumn(ctx, refTableName, "Name", cleanMain, trxName);
            }
            if (foundId != null && foundId > 0) {
                return false; // already exists
            }

            // Name column is configured but the cell is blank — skip rather than create a bad record.
            if (meta.nameColumnIndex != null && Util.isEmpty(nameToUse, true)) {
                return false;
            }

            // Ensure we have something for Name.
            if (nameToUse == null && meta.nameColumnIndex == null) {
                nameToUse = valueToUse != null ? valueToUse : cleanMain;
            }
            // Auto-generate a Value when no explicit value column is configured.
            if ((meta.valueColumnIndex == null || meta.valueColumnIndex < 0) && !meta.useValueForRef) {
                valueToUse = getNextAddedValue(refTableName, trxName);
            }
            if (valueToUse == null && nameToUse != null) {
                valueToUse = nameToUse;
            }

            PO refPO = refTable.getPO(0, trxName);
            if (refPO == null) {
                throw new AdempiereException("Cannot create record in " + refTableName);
            }
            if (refTable.getColumn("Value") != null && valueToUse != null) {
                refPO.set_ValueOfColumn("Value", valueToUse);
            }
            if (refTable.getColumn("Name") != null && nameToUse != null) {
                refPO.set_ValueOfColumn("Name", nameToUse);
            }
            if (refTable.getColumn("EntityType") != null) {
                refPO.set_ValueOfColumn("EntityType", "U");
            }
            refPO.setAD_Org_ID(0);
            refPO.saveEx();
            if (refPO.get_ID() <= 0) {
                throw new AdempiereException("saveEx returned no ID for new record in " + refTableName);
            }
            return true;
        }

        ColumnMeta findColumnByHeader(Map<Integer, ColumnMeta> metas, String headerName) {
            for (ColumnMeta meta : metas.values()) {
                if (headerName.equals(meta.headerName)) {
                    return meta;
                }
            }
            return null;
        }

        Set<Integer> collectWantedColumns(Map<Integer, ColumnMeta> metas) {
            Set<Integer> wanted = new HashSet<>();
            for (ColumnMeta meta : metas.values()) {
                wanted.add(Integer.valueOf(meta.columnIndex));
                if (meta.valueColumnIndex != null) wanted.add(meta.valueColumnIndex);
                if (meta.nameColumnIndex  != null) wanted.add(meta.nameColumnIndex);
            }
            return wanted;
        }

        boolean isMappedRowEmpty(Map<Integer, String> cells, Iterable<ColumnMeta> metas) {
            for (ColumnMeta meta : metas) {
                String txt = cells.get(meta.columnIndex);
                if (txt != null && !isBlankOrZero(txt)) {
                    return false;
                }
            }
            return true;
        }

        boolean shouldIgnoreRowMap(Map<Integer, String> cells, Iterable<ColumnMeta> metas) {
            for (ColumnMeta meta : metas) {
                if (!meta.ignoreIfBlank) {
                    continue;
                }
                if (isBlankOrZero(cells.get(meta.columnIndex))) {
                    return true;
                }
            }
            return false;
        }

        File writeErrorLog(List<ImportWspAtrMigrationFile.MigrationError> errors) throws Exception {
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File file = new File("/tmp/create-employees-errors-" + ts + ".csv");
            try (java.io.PrintWriter out = new java.io.PrintWriter(
                    new java.io.BufferedWriter(new java.io.FileWriter(file)))) {
                out.println("Tab,Line,Column,Error");
                for (ImportWspAtrMigrationFile.MigrationError err : errors) {
                    out.println(csv(err.tab) + "," + err.lineNo + ","
                            + csv(err.column) + "," + csv(err.message));
                }
            }
            svrProcess.addLog("Error log written to: " + file.getAbsolutePath());
            return file;
        }

        private String csv(String in) {
            return "\"" + (in == null ? "" : in.replace("\"", "\"\"")) + "\"";
        }
    }
}
