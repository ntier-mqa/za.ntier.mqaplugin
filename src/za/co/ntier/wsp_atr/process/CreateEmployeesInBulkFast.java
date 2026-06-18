package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Fast bulk-create of ZZ_WSP_Employees records from the Biodata sheet.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Stream the entire Biodata sheet once into a {@code LinkedHashMap<value, name>},
 *       deduplicating in memory — zero DB calls during this phase.</li>
 *   <li>Iterate the map and insert each record as a straight saveEx() — no prior
 *       lookup needed because the caller is expected to have truncated the table first.</li>
 * </ol>
 *
 * <p>Assumes the ZZ_WSP_Employees table has been truncated before running.
 * For an incremental (non-destructive) run use {@link CreateEmployeesInBulk} instead.</p>
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.CreateEmployeesInBulkFast")
public class CreateEmployeesInBulkFast extends SvrProcess {

    private static final String BULK_UPLOAD_PATH =
            "/home/ntier/SG_wsp_120626/MQAWSPATRDataDump2026_01062026.xlsx";

    private static final Set<String> SDL_FILTER = new HashSet<>(java.util.Arrays.asList(
            // "L010712109"
    ));

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

        int employeeRefTableId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_Reference_ID FROM AD_Ref_Table WHERE AD_Ref_Table_UU = ?",
                EMPLOYEE_REF_TABLE_UU);
        if (employeeRefTableId <= 0) {
            throw new AdempiereException(
                    "Setup error: AD_Ref_Table not found for UU=" + EMPLOYEE_REF_TABLE_UU);
        }

        X_ZZ_WSP_ATR_Lookup_Mapping biodataHeader = findBiodataHeader();
        if (biodataHeader == null) {
            throw new AdempiereException(
                    "No active bulk mapping header found whose tab name contains 'Biodata'.");
        }

        EmployeeCollector collector = new EmployeeCollector(refService, this);

        // -----------------------------------------------------------------------
        // Phase 1: stream the sheet — collect unique employees into memory, no DB.
        // -----------------------------------------------------------------------
        addLog("Phase 1: reading Biodata sheet into memory...");
        LinkedHashMap<String, String> employees; // value → name
        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {

            String mappingName = biodataHeader.getZZ_Tab_Name();
            List<String> matchingSheets = reader.findMatchingSheets(mappingName);
            if (matchingSheets.isEmpty()) {
                throw new AdempiereException(
                        "No sheet matching mapping '" + mappingName + "' found in workbook.");
            }

            Map<Integer, AbstractMappingSheetImporter.ColumnMeta> metas =
                    collector.buildColumnMeta(getCtx(), biodataHeader, get_TrxName());
            if (metas.isEmpty()) {
                throw new AdempiereException("No column metadata found for Biodata mapping.");
            }

            List<AbstractMappingSheetImporter.ColumnMeta> employeeMetas =
                    collector.findEmployeeMetas(metas, employeeRefTableId);
            if (employeeMetas.isEmpty()) {
                throw new AdempiereException(
                        "No column in the Biodata mapping targets the ZZ_WSP_Employees "
                        + "reference table (AD_Ref_Table_UU=" + EMPLOYEE_REF_TABLE_UU + ").");
            }

            int sr = biodataHeader.getStart_Row() != null ? biodataHeader.getStart_Row().intValue() : 4;
            if (sr <= 0) sr = 4;
            final int startRow = sr;

            Set<Integer> wanted = collector.collectWantedColumns(metas);
            employees = new LinkedHashMap<>();
            final int[] emptyCounter = {0};
            final boolean[] stopAll  = {false};

            final AbstractMappingSheetImporter.ColumnMeta sdlMeta =
                    collector.findColumnByHeader(metas, "SDLNumber");

            for (final String sheetName : matchingSheets) {
                reader.streamSheet(sheetName, startRow, wanted, (rowIdx, cells) -> {
                    if (stopAll[0]) {
                        return StreamingXlsxReader.Action.STOP;
                    }

                    if (collector.isMappedRowEmpty(cells, metas.values())) {
                        emptyCounter[0]++;
                        if (emptyCounter[0] > EmployeeCollector.MAX_EMPTY_ROWS) {
                            return StreamingXlsxReader.Action.STOP;
                        }
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                    emptyCounter[0] = 0;

                    if (collector.shouldIgnoreRowMap(cells, metas.values())) {
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

                        if (Util.isEmpty(valueToUse, true)) {
                            continue;
                        }

                        // putIfAbsent — first occurrence wins; duplicates silently dropped.
                        employees.putIfAbsent(valueToUse, nameToUse != null ? nameToUse : valueToUse);
                    }

                    return StreamingXlsxReader.Action.CONTINUE;
                });
            }
        }

        addLog("Phase 1 complete: " + employees.size() + " unique employee value(s) collected.");

        // -----------------------------------------------------------------------
        // Phase 2: resolve MTable once, then insert straight — no lookups.
        // -----------------------------------------------------------------------
        addLog("Phase 2: inserting records...");

        MRefTable refTableCfg = MRefTable.get(getCtx(), employeeRefTableId, get_TrxName());
        if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0) {
            throw new AdempiereException("Cannot resolve MRefTable for employee ref table ID " + employeeRefTableId);
        }
        MTable refTable = MTable.get(getCtx(), refTableCfg.getAD_Table_ID());
        if (refTable == null || refTable.getAD_Table_ID() <= 0) {
            throw new AdempiereException("Cannot resolve MTable for employee ref table.");
        }

        boolean hasValueCol    = refTable.getColumn("Value")      != null;
        boolean hasNameCol     = refTable.getColumn("Name")       != null;
        boolean hasEntityType  = refTable.getColumn("EntityType") != null;

        int created = 0;
        List<ImportWspAtrMigrationFile.MigrationError> errors = new ArrayList<>();

        for (Map.Entry<String, String> entry : employees.entrySet()) {
            String value = entry.getKey();
            String name  = entry.getValue();

            try {
                PO refPO = refTable.getPO(0, get_TrxName());
                if (refPO == null) {
                    throw new AdempiereException("Cannot create PO for table " + refTable.getTableName());
                }
                if (hasValueCol)   refPO.set_ValueOfColumn("Value", value);
                if (hasNameCol)    refPO.set_ValueOfColumn("Name",  name);
                if (hasEntityType) refPO.set_ValueOfColumn("EntityType", "U");
                refPO.setAD_Org_ID(0);
                refPO.saveEx();
                created++;

                if (created % 1000 == 0) {
                    addLog("Inserted " + created + " of " + employees.size() + "...");
                    try {
                        DB.commit(true, get_TrxName());
                    } catch (java.sql.SQLException e) {
                        throw new AdempiereException("Batch commit failed after " + created + " records", e);
                    }
                }
            } catch (Exception e) {
                errors.add(new ImportWspAtrMigrationFile.MigrationError(
                        "ZZ_WSP_Employees", created + 1, "Value",
                        "Error inserting value='" + value + "': " + e.getMessage()));
                if (errors.size() >= 500) {
                    break;
                }
            }
        }

        if (!errors.isEmpty()) {
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File logFile = new File("/tmp/create-employees-fast-errors-" + ts + ".csv");
            try (java.io.PrintWriter out = new java.io.PrintWriter(
                    new java.io.BufferedWriter(new java.io.FileWriter(logFile)))) {
                out.println("Tab,Line,Column,Error");
                for (ImportWspAtrMigrationFile.MigrationError err : errors) {
                    out.println(csv(err.tab) + "," + err.lineNo + ","
                            + csv(err.column) + "," + csv(err.message));
                }
            }
            addLog("Error log written to: " + logFile.getAbsolutePath());
            if (processUI != null && logFile.exists()) {
                processUI.download(logFile);
            }
            throw new AdempiereException("Completed with " + errors.size()
                    + " error(s) after " + created + " inserts. See log file.");
        }

        return "Created " + created + " ZZ_WSP_Employees record(s) from "
                + employees.size() + " unique value(s) collected from Biodata sheet.";
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

    private String csv(String in) {
        return "\"" + (in == null ? "" : in.replace("\"", "\"\"")) + "\"";
    }

    // -------------------------------------------------------------------------
    // Inner collector — extends AbstractMappingSheetImporter for shared helpers.
    // Only used during Phase 1 (streaming); no DB calls made here.
    // -------------------------------------------------------------------------
    private static final class EmployeeCollector extends AbstractMappingSheetImporter {

        static final int MAX_EMPTY_ROWS = 10;

        EmployeeCollector(ReferenceLookupService refService, SvrProcess process) {
            super(refService, process);
        }

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
    }
}
