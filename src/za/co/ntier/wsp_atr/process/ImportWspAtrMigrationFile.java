package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.api.model.MBPartner_New;
import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.co.ntier.wsp_atr.form.WspAtrSubmittedADForm;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRATRDetail;
import za.ntier.models.MZZWSPATRHTVF;
import za.ntier.models.MZZWSPATRSubmitted;
import za.ntier.models.MZZWSPATRWSP;

/**
 * Streaming bulk import for the WSP/ATR migration spreadsheet.
 *
 * <p>This used to load the whole workbook with {@code WorkbookFactory.create()}
 * which collapses on multi-million-row source files. The implementation now
 * uses {@link StreamingXlsxReader} so the workbook is processed row-by-row;
 * memory usage is bounded by the unique-string table plus a single row of
 * cells. Two passes are made over the file:</p>
 * <ol>
 *   <li>Validation pass — checks mandatory fields and reference resolvability,
 *       and incidentally captures the workbook-wide single organisation,
 *       financial year and submission status.</li>
 *   <li>Import pass — creates the target POs.</li>
 * </ol>
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ImportWspAtrMigrationFile")
public class ImportWspAtrMigrationFile extends SvrProcess {

    private static final String BULK_UPLOAD_PATH = "/home/ntier/SG_Data_070526/MQAWSPATRDataDump2026.xlsx";
    //private static final String BULK_UPLOAD_PATH = "/tmp/bulkupload.xlsx";

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

        List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = loadMappings();
        if (headers.isEmpty()) {
            throw new AdempiereException("No active WSP/ATR mapping header records found.");
        }

        MigrationSheetProcessor processor = new MigrationSheetProcessor(refService, this);

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {

            // Pass 1: validate + capture workbook-wide single org / fin year / WSP status.
            MigrationSheetProcessor.ValidationResult vr =
                    processor.validateWorkbook(getCtx(), reader, headers, get_TrxName());

            if (!vr.errors.isEmpty()) {
                File logFile = processor.writeErrorLog(vr.errors);
                if (processUI != null && logFile != null && logFile.exists()) {
                    processUI.download(logFile);
                }
                throw new AdempiereException("Validation failed with " + vr.errors.size()
                        + " error(s). Download the generated log file and correct the source spreadsheet.");
            }
            if (vr.finYearId <= 0) {
                throw new AdempiereException("Could not resolve financial year from FinancialYear column in workbook.");
            }
            if (vr.wspStatus == null) {
                throw new AdempiereException("Could not resolve WSP status from WSPStatus column in workbook.");
            }

            // Pass 2: stream again and create the POs.
            Map<Integer, Integer> importedBySubmittedId = processor.importWorkbook(
                    getCtx(), reader, headers, get_TrxName(), file.getName(),
                    vr.singleOrgId, vr.finYearId, vr.wspStatus);

            List<MigrationError> importErrors = processor.getImportErrors();
            if (!importErrors.isEmpty()) {
                File logFile = processor.writeErrorLog(importErrors);
                if (processUI != null && logFile != null && logFile.exists()) {
                    processUI.download(logFile);
                }
                throw new AdempiereException("Import failed with " + importErrors.size()
                        + " error(s). Download the generated log file and correct the source spreadsheet.");
            }

            int total = 0;
            for (Map.Entry<Integer, Integer> e : importedBySubmittedId.entrySet()) {
                total += e.getValue();
                int submittedId = e.getKey();
                MZZWSPATRATRDetail.updateATRAndDeviation(getCtx(), submittedId, get_TrxName());
                MZZWSPATRHTVF.updateHTVFTotal(getCtx(), submittedId, get_TrxName());
                MZZWSPATRWSP.updateWSPTotal(getCtx(), submittedId, get_TrxName());

                MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), submittedId, get_TrxName());
                addLog("Imported " + e.getValue() + " row(s) for " + submitted.getOrganisationName());
            }

            return "Imported " + total + " row(s) across " + importedBySubmittedId.size() + " submission(s).";
        }
    }

    private List<X_ZZ_WSP_ATR_Lookup_Mapping> loadMappings() {
        return new Query(getCtx(), X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name, "ZZ_Is_For_Bulk = 'Y'", get_TrxName())
                .setOnlyActiveRecords(true)
                .setOrderBy("seqNo")
                .list();
    }

    /** Error captured during validation or import. */
    static final class MigrationError {
        final String tab;
        final int lineNo;
        final String column;
        final String message;

        MigrationError(String tab, int lineNo, String column, String message) {
            this.tab = tab;
            this.lineNo = lineNo;
            this.column = column;
            this.message = message;
        }
    }

    // ------------------------------------------------------------------------
    // The processor: extends AbstractMappingSheetImporter for the POI-free
    // helpers (setValueFromText, findIdByColumn, columnLetterToIndex, ...) but
    // never calls into its Row/Sheet/Workbook-typed methods.
    // ------------------------------------------------------------------------
    private static final class MigrationSheetProcessor extends AbstractMappingSheetImporter {

        private static final int MAX_ERRORS = 500;
        private static final int MAX_EMPTY_ROWS = 10;

        private static final String WSP_STATUS_REF_UU = "98479fb5-df5d-440d-86aa-92d77a320857";
        private static final String DEFAULT_SDF_UU    = "06baf540-649a-40d2-84db-75b907bb9d99";

        private final Map<Integer, Map<Integer, ColumnMeta>> columnMetaCache = new HashMap<>();
        private final List<MigrationError> importErrors = new ArrayList<>();

        MigrationSheetProcessor(ReferenceLookupService refService, SvrProcess process) {
            super(refService, process);
        }

        List<MigrationError> getImportErrors() {
            return importErrors;
        }

        // ---- Single Workbook-method we still satisfy from IWspAtrSheetImporter ----
        @Override
        public int importData(Properties ctx, Workbook wb, X_ZZ_WSP_ATR_Submitted submitted,
                              X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader, String trxName, DataFormatter formatter) {
            // This streaming processor doesn't use the per-sheet entry point —
            // importWorkbook below drives everything.
            return 0;
        }

        // -------------------- result of validation --------------------
        static final class ValidationResult {
            final List<MigrationError> errors = new ArrayList<>();
            Integer singleOrgId;       // null when workbook contains >1 distinct org
            int finYearId;             // 0 until resolved
            String wspStatus;          // null until resolved
            // Detection bookkeeping:
            String detectedSdl;        // SDL that yielded singleOrgId
            boolean multipleOrgsDetected;
        }

        // ============================================================
        // Validation pass
        // ============================================================
        ValidationResult validateWorkbook(final Properties ctx,
                                          StreamingXlsxReader reader,
                                          List<X_ZZ_WSP_ATR_Lookup_Mapping> mappingHeaders,
                                          final String trxName) throws Exception {
            final ValidationResult result = new ValidationResult();

            for (final X_ZZ_WSP_ATR_Lookup_Mapping header : mappingHeaders) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                final String mappingName = header.getZZ_Tab_Name();
                List<String> matchingSheets = reader.findMatchingSheets(mappingName);
                if (matchingSheets.isEmpty()) {
                    throw new AdempiereException(
                            "No sheet matching mapping '" + mappingName + "' found in workbook");
                }

                final Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                if (metas.isEmpty()) {
                    continue;
                }

                for (final String sheetName : matchingSheets) {

                final ColumnMeta orgMeta     = findColumnByHeader(metas, "SDLNumber");
                final ColumnMeta finYearMeta = findColumnByHeader(metas, "FinancialYear");
                final ColumnMeta statusMeta  = findColumnByHeader(metas, "WSPStatus");

                int sr = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (sr <= 0) sr = 4;
                final int startRow = sr;

                final Set<Integer> wanted = collectWantedColumns(metas);
                final int[] emptyCounter = {0};
                final boolean[] stopAll = {false};

                reader.streamSheet(sheetName, startRow, wanted, (rowIdx, cells) -> {
                    if (stopAll[0]) {
                        return StreamingXlsxReader.Action.STOP;
                    }
                    if (isMappedRowEmpty(cells, metas.values())) {
                        emptyCounter[0]++;
                        if (emptyCounter[0] > MAX_EMPTY_ROWS) {
                            return StreamingXlsxReader.Action.STOP;
                        }
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                    emptyCounter[0] = 0;

                    if (shouldIgnoreRowMap(cells, metas.values())) {
                        return StreamingXlsxReader.Action.CONTINUE;
                    }

                    // Validate mandatory + references for this row.
                    for (ColumnMeta meta : metas.values()) {
                        if (meta.column == null) {
                            continue; // lookup-only entry (SDLNumber, FinancialYear, WSPStatus)
                        }
                        String txt = cells.get(meta.columnIndex);
                        String colLetter = columnIndexToLetter(meta.columnIndex);

                        if (meta.mandatory && Util.isEmpty(txt, true)) {
                            result.errors.add(new MigrationError(sheetName, rowIdx + 1, colLetter,
                                    "Mandatory field is missing (" + meta.column.getColumnName() + ")"));
                            if (result.errors.size() >= MAX_ERRORS) {
                                stopAll[0] = true;
                                return StreamingXlsxReader.Action.STOP;
                            }
                            continue;
                        }
                        if (Util.isEmpty(txt, true)) {
                            continue;
                        }
                        int ref = meta.column.getAD_Reference_ID();
                        boolean isRef = (ref == DisplayType.Table
                                       || ref == DisplayType.TableDir
                                       || ref == DisplayType.Search);
                        if (isRef && !meta.createIfNotExist) {
                            Integer id = tryResolveRefId(ctx, meta.column, txt, meta.useValueForRef, trxName);
                            if (id == null || id <= 0) {
                                result.errors.add(new MigrationError(sheetName, rowIdx + 1, colLetter,
                                        "Reference not found for value '" + txt + "' (" + meta.column.getColumnName() + ")"));
                                if (result.errors.size() >= MAX_ERRORS) {
                                    stopAll[0] = true;
                                    return StreamingXlsxReader.Action.STOP;
                                }
                            }
                        }
                    }

                    // Piggyback workbook-wide value detection on this same scan.
                    detectSingleOrg(ctx, cells, orgMeta, result, trxName);
                    detectFinYear(ctx, cells, finYearMeta, result, trxName);
                    detectWspStatus(cells, statusMeta, result, trxName);

                    return StreamingXlsxReader.Action.CONTINUE;
                });
                } // end for matchingSheets
            }

            return result;
        }

        // ============================================================
        // Import pass
        // ============================================================
        Map<Integer, Integer> importWorkbook(final Properties ctx,
                                             StreamingXlsxReader reader,
                                             List<X_ZZ_WSP_ATR_Lookup_Mapping> mappingHeaders,
                                             final String trxName,
                                             final String sourceFileName,
                                             final Integer singleOrgId,
                                             final int finYearId,
                                             final String wspStatus) throws Exception {

            importErrors.clear();
            final Map<Integer, Integer> importedBySubmittedId = new LinkedHashMap<>();
            final Map<Integer, Integer> submittedIdByOrgId = new HashMap<>();
            final Map<Integer, X_ZZ_WSP_ATR_Submitted> submittedPoCache = new HashMap<>();
            final int[] rowsSaved = {0};

            for (final X_ZZ_WSP_ATR_Lookup_Mapping header : mappingHeaders) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                final String mappingName = header.getZZ_Tab_Name();
                List<String> matchingSheets = reader.findMatchingSheets(mappingName);
                if (matchingSheets.isEmpty()) {
                    throw new AdempiereException(
                            "No sheet matching mapping '" + mappingName + "' found in workbook");
                }

                final Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                if (metas.isEmpty()) {
                    continue;
                }
                final ColumnMeta orgMeta = findColumnByHeader(metas, "SDLNumber");

                int sr = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (sr <= 0) sr = 4;
                final int startRow = sr;

                for (final String sheetName : matchingSheets) {

                // Resume support: skip ahead past any rows already committed for this tab.
                int lastProcessedLine = getLastProcessedLine(ctx, trxName, sourceFileName, sheetName);
                final int effectiveStartRow = Math.max(startRow, lastProcessedLine);
                if (effectiveStartRow > startRow) {
                    svrProcess.addLog("Tab " + sheetName + ": resuming from line "
                            + (effectiveStartRow + 1) + " (last committed line was " + lastProcessedLine + ")");
                }

                final Set<Integer> wanted = collectWantedColumns(metas);
                final int[] emptyCounter = {0};
                final boolean[] stopAll = {false};

                reader.streamSheet(sheetName, effectiveStartRow, wanted, (rowIdx, cells) -> {
                    if (stopAll[0]) {
                        return StreamingXlsxReader.Action.STOP;
                    }
                    if (isMappedRowEmpty(cells, metas.values())) {
                        emptyCounter[0]++;
                        if (emptyCounter[0] > MAX_EMPTY_ROWS) {
                            return StreamingXlsxReader.Action.STOP;
                        }
                        return StreamingXlsxReader.Action.CONTINUE;
                    }
                    emptyCounter[0] = 0;

                    if (shouldIgnoreRowMap(cells, metas.values())) {
                        return StreamingXlsxReader.Action.CONTINUE;
                    }

                    Integer orgId = resolveOrgIdForRow(ctx, cells, orgMeta, singleOrgId,
                            metas, trxName, sheetName, rowIdx + 1);
                    if (orgId == null) {
                        svrProcess.addLog("Tab " + sheetName + " row " + (rowIdx + 1)
                                + ": no SDL number — skipping remainder of tab");
                        return StreamingXlsxReader.Action.STOP;
                    }
                    Integer submittedId = submittedIdByOrgId.get(orgId);
                    if (submittedId == null) {
                        submittedId = getOrCreateSubmitted(ctx, orgId, finYearId, wspStatus, trxName, sourceFileName);
                        submittedIdByOrgId.put(orgId, submittedId);
                    }

                    final int finalSubmittedId = submittedId;
                    X_ZZ_WSP_ATR_Submitted submittedPO = submittedPoCache.computeIfAbsent(
                            finalSubmittedId, id -> new X_ZZ_WSP_ATR_Submitted(ctx, id, trxName));
                    PO line = newTargetPO(ctx, submittedPO, header, trxName);
                    if (line.get_ColumnIndex("Row_No") >= 0) {
                        line.set_ValueOfColumn("Row_No", Integer.valueOf(rowIdx + 1));
                    }

                    for (ColumnMeta meta : metas.values()) {
                        if (meta.column == null) {
                            continue; // lookup-only entry — not a PO column
                        }
                        String mainText = cells.get(meta.columnIndex);

                        if (meta.createIfNotExist && meta.isRefColumn) {
                            String valueText = (meta.valueColumnIndex != null)
                                    ? cells.get(meta.valueColumnIndex.intValue()) : null;
                            String nameText  = (meta.nameColumnIndex  != null)
                                    ? cells.get(meta.nameColumnIndex.intValue())  : null;
                            boolean allBlank = Util.isEmpty(mainText, true)
                                    && Util.isEmpty(valueText, true)
                                    && Util.isEmpty(nameText,  true);
                            if (allBlank) {
                                continue;
                            }
                            setValueFromTextOrCreate(ctx, line, meta, mainText, valueText, nameText, trxName);
                        } else {
                            if (Util.isEmpty(mainText, true)) {
                                continue;
                            }
                            String err = setValueFromText(ctx, line, meta.column, mainText, meta.useValueForRef, trxName);
                            if (err != null) {
                                importErrors.add(new MigrationError(sheetName, rowIdx + 1,
                                        columnIndexToLetter(meta.columnIndex), err));
                            }
                        }
                    }

                    line.setAD_Org_ID(0);
                    line.saveEx();
                    recordProgress(ctx, trxName, sourceFileName, sheetName, rowIdx + 1);
                    importedBySubmittedId.merge(finalSubmittedId, Integer.valueOf(1), Integer::sum);
                    rowsSaved[0]++;
                    if (rowsSaved[0] % 1000 == 0) {
                        try {
                            DB.commit(true, trxName);
                        } catch (java.sql.SQLException e) {
                            throw new AdempiereException("Batch commit failed after " + rowsSaved[0] + " rows", e);
                        }
                        svrProcess.addLog("Committed batch after " + rowsSaved[0] + " rows");
                    }

                    return StreamingXlsxReader.Action.CONTINUE;
                });
                } // end for matchingSheets
            }

            return importedBySubmittedId;
        }

        // ============================================================
        // Workbook-wide single-org / finYear / wspStatus detection
        // ============================================================
        private void detectSingleOrg(Properties ctx, Map<Integer, String> cells,
                                     ColumnMeta orgMeta, ValidationResult result, String trxName) {
            if (orgMeta == null || result.multipleOrgsDetected) {
                return;
            }
            String orgTxt = cells.get(orgMeta.columnIndex);
            if (Util.isEmpty(orgTxt, true)) {
                return;
            }
            String trimmed = orgTxt.trim();
            if (result.detectedSdl != null && result.detectedSdl.equalsIgnoreCase(trimmed)) {
                return; // already seen — no DB hit needed
            }
            int orgIdVal = lookupOrgIdBySdl(ctx, trimmed, trxName);
            if (orgIdVal <= 0) {
                return;
            }
            if (result.detectedSdl == null) {
                result.detectedSdl = trimmed;
                result.singleOrgId = Integer.valueOf(orgIdVal);
            } else if (result.singleOrgId != null
                    && result.singleOrgId.intValue() != orgIdVal) {
                result.multipleOrgsDetected = true;
                result.singleOrgId = null;
            }
        }

        private void detectFinYear(Properties ctx, Map<Integer, String> cells,
                                   ColumnMeta finYearMeta, ValidationResult result, String trxName) {
            if (finYearMeta == null || result.finYearId > 0) {
                return;
            }
            String txt = cells.get(finYearMeta.columnIndex);
            if (Util.isEmpty(txt, true)) {
                return;
            }
            int finId = DB.getSQLValueEx(trxName,
                    "SELECT C_Year_ID FROM C_Year WHERE FiscalYear = ? AND AD_Client_ID = ?",
                    txt.trim(), Env.getAD_Client_ID(ctx));
            if (finId > 0) {
                result.finYearId = finId;
            }
        }

        private void detectWspStatus(Map<Integer, String> cells, ColumnMeta statusMeta,
                                     ValidationResult result, String trxName) {
            if (statusMeta == null || result.wspStatus != null) {
                return;
            }
            String txt = cells.get(statusMeta.columnIndex);
            if (Util.isEmpty(txt, true)) {
                return;
            }
            if (txt.trim().equals("Created") ) {
            	txt = "Draft";
            	
            }
            String value = DB.getSQLValueStringEx(trxName,
                    "SELECT rl.Value FROM AD_Ref_List rl"
                    + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                    + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                    + " AND rl.Name = ?",
                    txt.trim());
            if (!Util.isEmpty(value, true)) {
                result.wspStatus = value;
            }
        }

        // ============================================================
        // Per-row helpers
        // ============================================================
        private Integer resolveOrgIdForRow(Properties ctx,
                                           Map<Integer, String> cells,
                                           ColumnMeta orgMeta,
                                           Integer singleOrgId,
                                           Map<Integer, ColumnMeta> metas,
                                           String trxName,
                                           String tab,
                                           int lineNo) {
            if (orgMeta != null) {
                String sdlNumber = cells.get(orgMeta.columnIndex);
                if (Util.isEmpty(sdlNumber, true)) {
                    return null;
                }
                String sdl = sdlNumber.trim();
                int orgId = lookupOrgIdBySdl(ctx, sdl, trxName);
                if (orgId > 0) {
                    return Integer.valueOf(orgId);
                }
                // ZZSdfOrganisation not found — create one if the BPartner exists.
                int bpId = DB.getSQLValueEx(trxName,
                        "SELECT C_BPartner_ID FROM C_BPartner WHERE Value = ? AND AD_Client_ID = ?",
                        sdl, Env.getAD_Client_ID(ctx));
                if (bpId <= 0) {
                    MBPartner_New bp = new MBPartner_New(ctx, 0, trxName);
                    bp.setValue(sdl);
                    String legalName = getCellByHeader(cells, metas, "Legal Name");
                    bp.setName((legalName != null && !legalName.isBlank()) ? legalName.trim() : sdl);
                    String tradeName = getCellByHeader(cells, metas, "Trade Name");
                    if (tradeName != null && !tradeName.isBlank()) {
                        bp.setName2(tradeName.trim());
                    }
                    bp.setDescription("Created by ImportWspAtrMigrationFile");
                    bp.setC_BP_Group_ID(1000018); // UNKNOWN GROUP
                    bp.setIsVendor(true);
                    bp.setIsCustomer(false);
                    bp.setIsEmployee(false);
                    bp.setIsProspect(false);
                    bp.setAD_Org_ID(0);
                    bp.saveEx();
                    bpId = bp.get_ID();
                    svrProcess.addLog("Tab " + tab + " row " + lineNo + ": created new BPartner for SDL " + sdl);
                }
                X_ZZSdfOrganisation newOrg = new X_ZZSdfOrganisation(ctx, 0, trxName);
                newOrg.setC_BPartner_ID(bpId);
                newOrg.setZZActingForEmployer(false);
                newOrg.setZZReplacingPrimarySDF(false);
                newOrg.setZZSecondarySdf(false);
                newOrg.setZZSdf_ID(DB.getSQLValue(trxName,
                        "Select s.ZZSDF_ID from ZZSdf s where ZZSdf_UU='" + DEFAULT_SDF_UU + "'"));
                newOrg.setAD_Org_ID(0);
                newOrg.saveEx();
                return Integer.valueOf(newOrg.get_ID());
            }
            if (singleOrgId != null && singleOrgId.intValue() > 0) {
                return singleOrgId;
            }
            throw new AdempiereException(
                    "Tab " + tab + " has no organisation mapping and workbook contains multiple organisations.");
        }

        private int getOrCreateSubmitted(Properties ctx, int orgId, int finYearId,
                                         String wspStatus, String trxName, String sourceFileName) {
            int existingId = DB.getSQLValueEx(trxName,
                    "SELECT ZZ_WSP_ATR_Submitted_ID FROM ZZ_WSP_ATR_Submitted "
                    + "WHERE ZZSDFOrganisation_ID=? AND ZZ_FinYear_ID=? "
                    + "ORDER BY SubmittedDate DESC NULLS LAST, ZZ_WSP_ATR_Submitted_ID DESC FETCH FIRST 1 ROWS ONLY",
                    Integer.valueOf(orgId), Integer.valueOf(finYearId));

            int clientId = Env.getAD_Client_ID(ctx);
            MZZWSPATRSubmitted submitted = existingId > 0
                    ? new MZZWSPATRSubmitted(ctx, existingId, trxName)
                    : new MZZWSPATRSubmitted(ctx, 0, trxName);

            if (existingId <= 0) {
                submitted.setName("WSP/ATR Migration "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
            }
            submitted.setFileName(sourceFileName);
            submitted.setZZ_Import_Submitted_Data("N");
            submitted.setZZSdfOrganisation_ID(orgId);
            submitted.setZZ_DocAction(null);
            submitted.setZZ_DocStatus(wspStatus);
            submitted.setZZ_FinYear_ID(finYearId);
            submitted.setZZ_Submission_Due_Date(
                    WspAtrSubmittedADForm.getWSPATR_Due_Date(clientId, orgId, trxName));
            submitted.setAD_Org_ID(0);
            submitted.saveEx();

            WspAtrSubmittedADForm.rebuildSubLevyOrgLinks(submitted.get_ID(), trxName);
            return submitted.get_ID();
        }

        private int lookupOrgIdBySdl(Properties ctx, String sdl, String trxName) {
            return DB.getSQLValueEx(trxName,
                    "SELECT o.ZZSdfOrganisation_ID FROM ZZSdfOrganisation o"
                    + " JOIN C_BPartner bp ON bp.C_BPartner_ID = o.C_BPartner_ID"
                    + " WHERE bp.Value = ? AND o.AD_Client_ID = ? AND o.IsActive = 'Y'"
                    + " FETCH FIRST 1 ROWS ONLY",
                    sdl, Env.getAD_Client_ID(ctx));
        }

        private String getCellByHeader(Map<Integer, String> cells,
                                       Map<Integer, ColumnMeta> metas,
                                       String headerName) {
            for (ColumnMeta meta : metas.values()) {
                if (headerName.equals(meta.headerName)) {
                    return cells.get(meta.columnIndex);
                }
            }
            return null;
        }

        private ColumnMeta findColumnByHeader(Map<Integer, ColumnMeta> metas, String headerName) {
            for (ColumnMeta meta : metas.values()) {
                if (headerName.equals(meta.headerName)) {
                    return meta;
                }
            }
            return null;
        }

        private Set<Integer> collectWantedColumns(Map<Integer, ColumnMeta> metas) {
            Set<Integer> wanted = new HashSet<>();
            for (ColumnMeta meta : metas.values()) {
                wanted.add(Integer.valueOf(meta.columnIndex));
                if (meta.valueColumnIndex != null) {
                    wanted.add(meta.valueColumnIndex);
                }
                if (meta.nameColumnIndex != null) {
                    wanted.add(meta.nameColumnIndex);
                }
            }
            return wanted;
        }

        /**
         * Map-based equivalent of {@code AbstractMappingSheetImporter#isRowCompletelyEmpty}.
         * Treats blank text and zero-valued numeric strings as empty, matching the
         * original behaviour where numeric 0 cells were considered empty.
         */
        private boolean isMappedRowEmpty(Map<Integer, String> cells, Iterable<ColumnMeta> metas) {
            for (ColumnMeta meta : metas) {
                String txt = cells.get(meta.columnIndex);
                if (txt != null && !isBlankOrZero(txt)) {
                    return false;
                }
            }
            return true;
        }

        /** Map-based equivalent of {@code shouldIgnoreRowBecauseOfIgnoreIfBlank}. */
        private boolean shouldIgnoreRowMap(Map<Integer, String> cells, Iterable<ColumnMeta> metas) {
            for (ColumnMeta meta : metas) {
                if (!meta.ignoreIfBlank) {
                    continue;
                }
                String txt = cells.get(meta.columnIndex);
                if (isBlankOrZero(txt)) {
                    return true;
                }
            }
            return false;
        }

        // ============================================================
        // Column metadata cache
        // ============================================================
        private Map<Integer, ColumnMeta> buildColumnMeta(Properties ctx,
                                                        X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                                                        String trxName) {
            Map<Integer, ColumnMeta> cached = columnMetaCache.get(mappingHeader.get_ID());
            if (cached != null) {
                return cached;
            }
            List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> details = loadDetails(mappingHeader, trxName);
            Map<Integer, ColumnMeta> metas = new HashMap<>();
            for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
                if (Util.isEmpty(det.getZZ_Column_Letter(), true)) {
                    continue;
                }
                ColumnMeta meta = new ColumnMeta();
                meta.columnIndex = columnLetterToIndex(det.getZZ_Column_Letter());
                meta.headerName = det.getZZ_Header_Name();
                meta.useValueForRef = det.isZZ_Use_Value();
                meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();
                meta.mandatory = det.isMandatory();
                meta.ignoreIfBlank = det.isIgnore_If_Blank();
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
            columnMetaCache.put(mappingHeader.get_ID(), metas);
            return metas;
        }

        // ============================================================
        // Resume bookkeeping
        // ============================================================
        private int getLastProcessedLine(Properties ctx, String trxName, String sourceFile, String tabName) {
            int val = DB.getSQLValue(trxName,
                    "SELECT COALESCE(MAX(LineNo), 0) FROM ZZ_WSP_ATR_Migration_Progress"
                    + " WHERE AD_Client_ID = ? AND SourceFile = ? AND TabName = ?",
                    Env.getAD_Client_ID(ctx), sourceFile, tabName);
            return val < 0 ? 0 : val;
        }

        private void recordProgress(Properties ctx, String trxName, String sourceFile, String tabName, int lineNo) {
            DB.executeUpdateEx(
                    "INSERT INTO ZZ_WSP_ATR_Migration_Progress"
                    + " (AD_Client_ID, SourceFile, TabName, LineNo, ProcessedAt)"
                    + " VALUES (?, ?, ?, ?, NOW())"
                    + " ON CONFLICT DO NOTHING",
                    new Object[]{Env.getAD_Client_ID(ctx), sourceFile, tabName, Integer.valueOf(lineNo)},
                    trxName);
        }

        // ============================================================
        // Error CSV writer
        // ============================================================
        File writeErrorLog(List<MigrationError> errors) throws Exception {
            File file = File.createTempFile("wspatr-migration-errors-", ".csv");
            try (java.io.FileWriter fw = new java.io.FileWriter(file);
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
                 java.io.PrintWriter out = new java.io.PrintWriter(bw)) {
                out.println("Tab,Line,Column,Error");
                for (MigrationError err : errors) {
                    out.println(toCsv(err.tab) + "," + err.lineNo + "," + toCsv(err.column) + "," + toCsv(err.message));
                }
            }
            return file;
        }

        private String toCsv(String in) {
            String safe = in == null ? "" : in.replace("\"", "\"\"");
            return "\"" + safe + "\"";
        }
    }
}
