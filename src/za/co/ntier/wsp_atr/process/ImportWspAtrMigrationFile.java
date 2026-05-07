package za.co.ntier.wsp_atr.process;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
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

@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ImportWspAtrMigrationFile")
public class ImportWspAtrMigrationFile extends SvrProcess {

    private static final String EXCEL_PASSWORD = "Learning2026";

    private static final String BULK_UPLOAD_PATH = "/tmp/bulkupload.xlsx";

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

        DataFormatter formatter = new DataFormatter();
        MigrationSheetProcessor processor = new MigrationSheetProcessor(refService, this);
        List<MigrationError> errors;
        try (Workbook workbook = openWorkbook(file)) {
            errors = processor.validateWorkbook(getCtx(), workbook, headers, get_TrxName(), formatter);

            if (!errors.isEmpty()) {
                File logFile = processor.writeErrorLog(errors);
                if (processUI != null && logFile != null && logFile.exists()) {
                    processUI.download(logFile);
                }
                throw new AdempiereException("Validation failed with " + errors.size()
                        + " error(s). Download the generated log file and correct the source spreadsheet.");
            }

            Map<Integer, Integer> importedBySubmittedId = processor.importWorkbook(
                    getCtx(), workbook, headers, get_TrxName(), formatter, file.getName());

            // If any reference lookups failed during import, write and offer the error file for download then abort
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

    private Workbook openWorkbook(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            byte[] data = org.apache.commons.io.IOUtils.toByteArray(is);
            return openWorkbookAuto(data, EXCEL_PASSWORD);
        }
    }

    private Workbook openWorkbookAuto(byte[] data, String password) throws Exception {
        IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(data));
        } catch (EncryptedDocumentException e) {
            try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs =
                    new org.apache.poi.poifs.filesystem.POIFSFileSystem(new ByteArrayInputStream(data))) {

                org.apache.poi.poifs.crypt.EncryptionInfo info = new org.apache.poi.poifs.crypt.EncryptionInfo(fs);
                org.apache.poi.poifs.crypt.Decryptor decryptor = org.apache.poi.poifs.crypt.Decryptor.getInstance(info);

                if (!decryptor.verifyPassword(password)) {
                    throw new AdempiereException("Invalid Excel password for workbook");
                }

                try (InputStream decryptedStream = decryptor.getDataStream(fs)) {
                    return WorkbookFactory.create(decryptedStream);
                }
            }
        }
    }

    private static final class MigrationError {
        final String tab;
        final int lineNo;
        final String column;
        final String message;

        private MigrationError(String tab, int lineNo, String column, String message) {
            this.tab = tab;
            this.lineNo = lineNo;
            this.column = column;
            this.message = message;
        }
    }

    private static final class MigrationSheetProcessor extends AbstractMappingSheetImporter {

        private static final int MAX_ERRORS = 500;

        private final Map<Integer, Map<Integer, ColumnMeta>> columnMetaCache = new HashMap<>();
        private final List<MigrationError> importErrors = new ArrayList<>();

        private MigrationSheetProcessor(ReferenceLookupService refService, SvrProcess process) {
            super(refService, process);
        }

        List<MigrationError> getImportErrors() {
            return importErrors;
        }

        private List<MigrationError> validateWorkbook(Properties ctx,
                                                     Workbook wb,
                                                     List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                                     String trxName,
                                                     DataFormatter formatter) {
            List<MigrationError> errors = new ArrayList<>();

            for (X_ZZ_WSP_ATR_Lookup_Mapping header : headers) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                Sheet sheet = getSheetOrThrow(wb, header);
                Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                if (metas.isEmpty()) {
                    continue;
                }

                int startRow = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (startRow <= 0) {
                    startRow = 4;
                }

                int emptyRowsInARow = 0;
                for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    if (isRowCompletelyEmpty(row, metas.values())) {
                        emptyRowsInARow++;
                        if (emptyRowsInARow > 10) {
                            break;
                        }
                        continue;
                    }
                    emptyRowsInARow = 0;

                    if (shouldIgnoreRowBecauseOfIgnoreIfBlank(row, metas.values(), formatter)) {
                        continue;
                    }

                    for (ColumnMeta meta : metas.values()) {
                        if (meta.column == null) {
                            continue; // lookup-only entry (SDLNumber, FinancialYear, WSPStatus)
                        }
                        String txt = getCellText(row, meta.columnIndex, formatter);
                        String colLetter = columnIndexToLetter(meta.columnIndex);
                        if (meta.mandatory && Util.isEmpty(txt, true)) {
                            errors.add(new MigrationError(sheet.getSheetName(), r + 1, colLetter,
                                    "Mandatory field is missing (" + meta.column.getColumnName() + ")"));
                            if (errors.size() >= MAX_ERRORS) {
                                return errors;
                            }
                            continue;
                        }

                        if (Util.isEmpty(txt, true)) {
                            continue;
                        }

                        int ref = meta.column.getAD_Reference_ID();
                        boolean isRef = (ref == DisplayType.Table || ref == DisplayType.TableDir || ref == DisplayType.Search);
                        if (isRef && !meta.createIfNotExist) {
                            Integer id = tryResolveRefId(ctx, meta.column, txt, meta.useValueForRef, trxName);
                            if (id == null || id <= 0) {
                                errors.add(new MigrationError(sheet.getSheetName(), r + 1, colLetter,
                                        "Reference not found for value '" + txt + "' (" + meta.column.getColumnName() + ")"));
                                if (errors.size() >= MAX_ERRORS) {
                                    return errors;
                                }
                            }
                        }
                    }
                }
            }

            return errors;
        }

        private Map<Integer, Integer> importWorkbook(Properties ctx,
                                                     Workbook wb,
                                                     List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                                     String trxName,
                                                     DataFormatter formatter,
                                                     String sourceFileName) {

            importErrors.clear();
            Map<Integer, Integer> importedBySubmittedId = new LinkedHashMap<>();
            Map<Integer, Integer> submittedIdByOrgId = new HashMap<>();
            // Cache the X_ZZ_WSP_ATR_Submitted PO by ID so newTargetPO does not
            // reload it from the database on every single row.
            Map<Integer, X_ZZ_WSP_ATR_Submitted> submittedPoCache = new HashMap<>();
            int rowsSaved = 0;
            Integer singleOrgId = resolveSingleOrgId(ctx, wb, headers, trxName, formatter);
            int finYearId = resolveFinYearId(ctx, wb, headers, trxName, formatter);
            String wspStatus = resolveWspStatus(ctx, wb, headers, trxName, formatter);

            for (X_ZZ_WSP_ATR_Lookup_Mapping header : headers) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }

                Sheet sheet = getSheetOrThrow(wb, header);
                Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                if (metas.isEmpty()) {
                    continue;
                }

                ColumnMeta orgMeta = findOrgMeta(metas);

                int startRow = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (startRow <= 0) {
                    startRow = 4;
                }

                // Resume support: find the last committed line for this tab and skip ahead.
                // lastProcessedLine is stored as r+1 (1-based), so the next r to process is lastProcessedLine.
                int lastProcessedLine = getLastProcessedLine(ctx, trxName, sourceFileName, sheet.getSheetName());
                int effectiveStartRow = Math.max(startRow, lastProcessedLine);
                if (effectiveStartRow > startRow) {
                    svrProcess.addLog("Tab " + sheet.getSheetName() + ": resuming from line "
                            + (effectiveStartRow + 1) + " (last committed line was " + lastProcessedLine + ")");
                }

                int emptyRowsInARow = 0;
                for (int r = effectiveStartRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    if (isRowCompletelyEmpty(row, metas.values())) {
                        emptyRowsInARow++;
                        if (emptyRowsInARow > 10) {
                            break;
                        }
                        continue;
                    }
                    emptyRowsInARow = 0;

                    if (shouldIgnoreRowBecauseOfIgnoreIfBlank(row, metas.values(), formatter)) {
                        continue;
                    }

                    Integer orgId = resolveOrgIdForRow(ctx, row, orgMeta, singleOrgId, formatter, metas, trxName, sheet.getSheetName(), r + 1);
                    if (orgId == null) {
                        svrProcess.addLog("Tab " + sheet.getSheetName() + " row " + (r + 1) + ": no SDL number — skipping remainder of tab");
                        break;
                    }
                    Integer submittedId = submittedIdByOrgId.get(orgId);
                    if (submittedId == null) {
                        submittedId = getOrCreateSubmitted(ctx, orgId, finYearId, wspStatus, trxName, sourceFileName);
                        submittedIdByOrgId.put(orgId, submittedId);
                    }

                    X_ZZ_WSP_ATR_Submitted submittedPO = submittedPoCache.computeIfAbsent(
                            submittedId, id -> new X_ZZ_WSP_ATR_Submitted(ctx, id, trxName));
                    PO line = newTargetPO(ctx, submittedPO, header, trxName);
                    if (line.get_ColumnIndex("Row_No") >= 0) {
                        line.set_ValueOfColumn("Row_No", Integer.valueOf(r + 1));
                    }

                    for (ColumnMeta meta : metas.values()) {
                        if (meta.column == null) {
                            continue; // lookup-only entry (SDLNumber, FinancialYear, WSPStatus) — not a PO column
                        }
                        String mainText = getCellText(row, meta.columnIndex, formatter);

                        if (meta.createIfNotExist && meta.isRefColumn) {
                            // Read optional Value/Name from their own columns when configured
                            String valueText = (meta.valueColumnIndex != null)
                                    ? getCellText(row, meta.valueColumnIndex, formatter) : null;
                            String nameText  = (meta.nameColumnIndex  != null)
                                    ? getCellText(row, meta.nameColumnIndex,  formatter) : null;

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
                                importErrors.add(new MigrationError(sheet.getSheetName(), r + 1,
                                        columnIndexToLetter(meta.columnIndex), err));
                            }
                        }
                    }

                    line.saveEx();
                    recordProgress(ctx, trxName, sourceFileName, sheet.getSheetName(), r + 1);
                    importedBySubmittedId.put(submittedId, importedBySubmittedId.getOrDefault(submittedId, 0) + 1);
                    rowsSaved++;
                    if (rowsSaved % 1000 == 0) {
                        try {
                            DB.commit(true, trxName);
                        } catch (java.sql.SQLException e) {
                            throw new AdempiereException("Batch commit failed after " + rowsSaved + " rows", e);
                        }
                        svrProcess.addLog("Committed batch after " + rowsSaved + " rows");
                    }
                }
            }

            return importedBySubmittedId;
        }

        private Integer resolveSingleOrgId(Properties ctx,
                                           Workbook wb,
                                           List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                           String trxName,
                                           DataFormatter formatter) {
            // We only need to know whether the whole workbook contains exactly one
            // organisation.  Stop as soon as we see a second distinct SDL value —
            // no need to scan every row of every sheet.
            String detectedSdl = null;
            Integer detectedId  = null;

            for (X_ZZ_WSP_ATR_Lookup_Mapping header : headers) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                Sheet sheet = getSheetOrThrow(wb, header);
                Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                ColumnMeta orgMeta = findOrgMeta(metas);
                if (orgMeta == null) {
                    continue;
                }
                int startRow = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (startRow <= 0) {
                    startRow = 4;
                }
                for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    String orgTxt = getCellText(row, orgMeta.columnIndex, formatter);
                    if (Util.isEmpty(orgTxt, true)) {
                        continue;
                    }
                    orgTxt = orgTxt.trim();
                    if (detectedSdl != null && detectedSdl.equalsIgnoreCase(orgTxt)) {
                        continue; // same SDL as already detected — no DB query needed
                    }
                    int orgIdVal = DB.getSQLValueEx(trxName,
                            "SELECT o.ZZSdfOrganisation_ID FROM ZZSdfOrganisation o"
                            + " JOIN C_BPartner bp ON bp.C_BPartner_ID = o.C_BPartner_ID"
                            + " WHERE bp.Value = ? AND o.AD_Client_ID = ? AND o.IsActive = 'Y'"
                            + " FETCH FIRST 1 ROWS ONLY",
                            orgTxt, Env.getAD_Client_ID(ctx));
                    if (orgIdVal <= 0) {
                        continue;
                    }
                    if (detectedId == null) {
                        detectedId  = orgIdVal;
                        detectedSdl = orgTxt;
                    } else if (detectedId.intValue() != orgIdVal) {
                        return null; // multiple orgs — stop immediately, no need to scan further
                    }
                }
            }
            return detectedId;
        }

        private int resolveFinYearId(Properties ctx,
                                     Workbook wb,
                                     List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                     String trxName,
                                     DataFormatter formatter) {
            for (X_ZZ_WSP_ATR_Lookup_Mapping header : headers) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                Sheet sheet = getSheetOrThrow(wb, header);
                Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                ColumnMeta finYearMeta = findFinYearMeta(metas);
                if (finYearMeta == null) {
                    continue;
                }
                int startRow = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (startRow <= 0) {
                    startRow = 4;
                }
                for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    String finYearTxt = getCellText(row, finYearMeta.columnIndex, formatter);
                    if (Util.isEmpty(finYearTxt, true)) {
                        continue;
                    }
                    int finYearId = DB.getSQLValueEx(trxName,
                            "SELECT C_Year_ID FROM C_Year WHERE FiscalYear = ? AND AD_Client_ID = ?",
                            finYearTxt.trim(), Env.getAD_Client_ID(ctx));
                    if (finYearId > 0) {
                        return finYearId;
                    }
                }
            }
            throw new AdempiereException("Could not resolve financial year from FinancialYear column in workbook.");
        }

        private String resolveWspStatus(Properties ctx,
                                        Workbook wb,
                                        List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                        String trxName,
                                        DataFormatter formatter) {
            for (X_ZZ_WSP_ATR_Lookup_Mapping header : headers) {
                if (header.getAD_Table_ID() <= 0 || !header.isZZ_Is_For_Bulk()) {
                    continue;
                }
                Sheet sheet = getSheetOrThrow(wb, header);
                Map<Integer, ColumnMeta> metas = buildColumnMeta(ctx, header, trxName);
                ColumnMeta statusMeta = findWspStatusMeta(metas);
                if (statusMeta == null) {
                    continue;
                }
                int startRow = header.getStart_Row() != null ? header.getStart_Row().intValue() : 4;
                if (startRow <= 0) {
                    startRow = 4;
                }
                for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    String statusTxt = getCellText(row, statusMeta.columnIndex, formatter);
                    if (Util.isEmpty(statusTxt, true)) {
                        continue;
                    }
                    String value = DB.getSQLValueStringEx(trxName,
                            "SELECT rl.Value FROM AD_Ref_List rl"
                            + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                            + " WHERE r.AD_Reference_UU = '98479fb5-df5d-440d-86aa-92d77a320857'"
                            + " AND rl.Name = ?",
                            statusTxt.trim());
                    if (!Util.isEmpty(value, true)) {
                        return value;
                    }
                }
            }
            throw new AdempiereException("Could not resolve WSP status from WSPStatus column in workbook.");
        }

        private ColumnMeta findWspStatusMeta(Map<Integer, ColumnMeta> metas) {
            for (ColumnMeta meta : metas.values()) {
                if ("WSPStatus".equals(meta.headerName)) {
                    return meta;
                }
            }
            return null;
        }

        private ColumnMeta findFinYearMeta(Map<Integer, ColumnMeta> metas) {
            for (ColumnMeta meta : metas.values()) {
                if ("FinancialYear".equals(meta.headerName)) {
                    return meta;
                }
            }
            return null;
        }

        private String getCellValueByHeader(Row row, Map<Integer, ColumnMeta> metas, String header, DataFormatter formatter) {
            for (ColumnMeta meta : metas.values()) {
                if (header.equals(meta.headerName)) {
                    return getCellText(row, meta.columnIndex, formatter);
                }
            }
            return null;
        }

        private ColumnMeta findOrgMeta(Map<Integer, ColumnMeta> metas) {
            for (ColumnMeta meta : metas.values()) {
                if ("SDLNumber".equals(meta.headerName)) {
                    return meta;
                }
            }
            return null;
        }

        private Integer resolveOrgIdForRow(Properties ctx,
                                           Row row,
                                           ColumnMeta orgMeta,
                                           Integer singleOrgId,
                                           DataFormatter formatter,
                                           Map<Integer, ColumnMeta> metas,
                                           String trxName,
                                           String tab,
                                           int lineNo) {
            if (orgMeta != null) {
                String sdlNumber = getCellText(row, orgMeta.columnIndex, formatter);
                if (Util.isEmpty(sdlNumber, true)) {
                    return null;
                }
                int orgId = DB.getSQLValueEx(trxName,
                        "SELECT o.ZZSdfOrganisation_ID FROM ZZSdfOrganisation o"
                        + " JOIN C_BPartner bp ON bp.C_BPartner_ID = o.C_BPartner_ID"
                        + " WHERE bp.Value = ? AND o.AD_Client_ID = ? AND o.IsActive = 'Y'"
                        + " FETCH FIRST 1 ROWS ONLY",
                        sdlNumber.trim(), Env.getAD_Client_ID(ctx));
                if (orgId > 0) {
                    return orgId;
                }
                // ZZSdfOrganisation not found — create one if the BPartner exists
                int bpId = DB.getSQLValueEx(trxName,
                        "SELECT C_BPartner_ID FROM C_BPartner WHERE Value = ? AND AD_Client_ID = ?",
                        sdlNumber.trim(), Env.getAD_Client_ID(ctx));
                if (bpId <= 0) {
                    // BPartner does not exist — create it from the SDL number
                    MBPartner_New bp = new MBPartner_New(ctx, 0, trxName);
                    bp.setValue(sdlNumber.trim());
                    String legalName = getCellValueByHeader(row, metas, "Legal Name", formatter);
                    bp.setName(legalName != null && !legalName.isBlank() ? legalName.trim() : sdlNumber.trim());
                    String tradeName = getCellValueByHeader(row, metas, "Trade Name", formatter);
                    if (tradeName != null && !tradeName.isBlank()) { bp.setName2(tradeName.trim()); }
                    bp.setDescription("Created by ImportWspAtrMigrationFile");
                    bp.setC_BP_Group_ID(1000018); // UNKNOWN GROUP
                    bp.setIsVendor(true);
                    bp.setIsCustomer(false);
                    bp.setIsEmployee(false);
                    bp.setIsProspect(false);
                    bp.saveEx();
                    bpId = bp.get_ID();
                    svrProcess.addLog("Tab " + tab + " row " + lineNo + ": created new BPartner for SDL " + sdlNumber.trim());
                }
                X_ZZSdfOrganisation newOrg = new X_ZZSdfOrganisation(ctx, 0, trxName);
                newOrg.setC_BPartner_ID(bpId);
                newOrg.setZZActingForEmployer(false);
                newOrg.setZZReplacingPrimarySDF(false);
                newOrg.setZZSecondarySdf(false);
                newOrg.setZZSdf_ID(DB.getSQLValue(trxName, "Select s.ZZSDF_ID from ZZSdf s where ZZSdf_UU='06baf540-649a-40d2-84db-75b907bb9d99'"));
                newOrg.saveEx();
                return newOrg.get_ID();
            }
            if (singleOrgId != null && singleOrgId.intValue() > 0) {
                return singleOrgId;
            }
            throw new AdempiereException("Tab " + tab + " has no organisation mapping and workbook contains multiple organisations.");
        }

        private int getOrCreateSubmitted(Properties ctx, int orgId, int finYearId, String wspStatus, String trxName, String sourceFileName) {
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
                submitted.setName("WSP/ATR Migration " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
            }
            submitted.setFileName(sourceFileName);
            submitted.setZZ_Import_Submitted_Data("N");
            submitted.setZZSdfOrganisation_ID(orgId);
            submitted.setZZ_DocAction(null);
            submitted.setZZ_DocStatus(wspStatus);
            submitted.setZZ_FinYear_ID(finYearId);
            submitted.setZZ_Submission_Due_Date(WspAtrSubmittedADForm.getWSPATR_Due_Date(clientId, orgId, trxName));
            submitted.saveEx();

            WspAtrSubmittedADForm.rebuildSubLevyOrgLinks(submitted.get_ID(), trxName);
            return submitted.get_ID();
        }

        private Map<Integer, ColumnMeta> buildColumnMeta(Properties ctx,
                                                         X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                                                         String trxName) {
            Map<Integer, ColumnMeta> cached = columnMetaCache.get(mappingHeader.get_ID());
            if (cached != null)
                return cached;
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
                    meta.valueColumnIndex = columnLetterToIndex(valueColLetter);
                }
                if (!Util.isEmpty(nameColLetter, true)) {
                    meta.nameColumnIndex = columnLetterToIndex(nameColLetter);
                }
                if (det.getAD_Column_ID() > 0) {
                    meta.column = new MColumn(ctx, det.getAD_Column_ID(), trxName);
                    if (Util.isEmpty(meta.column.getColumnName(), true)) {
                        svrProcess.addLog("Skipping detail — MColumn " + det.getAD_Column_ID() + " not found (letter " + det.getZZ_Column_Letter() + ")");
                        continue;
                    }
                    int ref = meta.column.getAD_Reference_ID();
                    meta.isRefColumn = ref == DisplayType.Table
                            || ref == DisplayType.TableDir
                            || ref == DisplayType.Search;
                }
                // meta.column may be null for lookup-only entries (SDLNumber, FinancialYear, WSPStatus)
                // — those are resolved by the special finder methods and never passed to setValueFromText
                metas.put(Integer.valueOf(meta.columnIndex), meta);
            }
            columnMetaCache.put(mappingHeader.get_ID(), metas);
            return metas;
        }

        @Override
        public int importData(Properties ctx, Workbook wb, X_ZZ_WSP_ATR_Submitted submitted,
                              X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader, String trxName, DataFormatter formatter) {
            return 0;
        }

        /**
         * Returns the highest LineNo already committed for the given file+tab combination,
         * or 0 if nothing has been recorded yet.  LineNo values are 1-based (r + 1).
         */
        private int getLastProcessedLine(Properties ctx, String trxName, String sourceFile, String tabName) {
            int val = DB.getSQLValue(trxName,
                    "SELECT COALESCE(MAX(LineNo), 0) FROM ZZ_WSP_ATR_Migration_Progress"
                    + " WHERE AD_Client_ID = ? AND SourceFile = ? AND TabName = ?",
                    Env.getAD_Client_ID(ctx), sourceFile, tabName);
            return val < 0 ? 0 : val;
        }

        /**
         * Inserts a progress record for the given line inside the current transaction.
         * The record is committed together with the surrounding data batch.
         * ON CONFLICT DO NOTHING makes it safe to call even if the row already exists.
         */
        private void recordProgress(Properties ctx, String trxName, String sourceFile, String tabName, int lineNo) {
            DB.executeUpdateEx(
                    "INSERT INTO ZZ_WSP_ATR_Migration_Progress"
                    + " (AD_Client_ID, SourceFile, TabName, LineNo, ProcessedAt)"
                    + " VALUES (?, ?, ?, ?, NOW())"
                    + " ON CONFLICT DO NOTHING",
                    new Object[]{Env.getAD_Client_ID(ctx), sourceFile, tabName, Integer.valueOf(lineNo)},
                    trxName);
        }

        private File writeErrorLog(List<MigrationError> errors) throws Exception {
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
