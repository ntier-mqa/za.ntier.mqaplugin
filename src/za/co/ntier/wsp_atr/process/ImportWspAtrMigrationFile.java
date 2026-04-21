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

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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

    @Parameter(name = "FileName")
    private String filePath;

    private final ReferenceLookupService refService = new ReferenceLookupService();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (Util.isEmpty(filePath, true)) {
            throw new AdempiereException("FileName parameter is empty. Please select the migration spreadsheet file.");
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found or not a regular file: " + filePath);
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

            int total = 0;
            for (Map.Entry<Integer, Integer> e : importedBySubmittedId.entrySet()) {
                total += e.getValue();
                int submittedId = e.getKey();
                MZZWSPATRATRDetail.updateATRAndDeviation(getCtx(), submittedId, get_TrxName());
                MZZWSPATRHTVF.updateHTVFTotal(getCtx(), submittedId, get_TrxName());
                MZZWSPATRWSP.updateWSPTotal(getCtx(), submittedId, get_TrxName());

                DB.executeUpdateEx(
                        "UPDATE ZZ_WSP_ATR_Submitted SET ZZ_DocStatus=? WHERE ZZ_WSP_ATR_Submitted_ID=?",
                        new Object[] { X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported, submittedId },
                        get_TrxName());

                MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), submittedId, get_TrxName());
                addLog("Imported " + e.getValue() + " row(s) for " + submitted.getOrganisationName());
            }

            return "Imported " + total + " row(s) across " + importedBySubmittedId.size() + " submission(s).";
        }
    }

    private List<X_ZZ_WSP_ATR_Lookup_Mapping> loadMappings() {
        return new Query(getCtx(), X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name, null, get_TrxName())
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
        final String message;

        private MigrationError(String tab, int lineNo, String message) {
            this.tab = tab;
            this.lineNo = lineNo;
            this.message = message;
        }
    }

    private static final class MigrationSheetProcessor extends AbstractMappingSheetImporter {

        private static final int MAX_ERRORS = 500;

        private MigrationSheetProcessor(ReferenceLookupService refService, SvrProcess process) {
            super(refService, process);
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
                        String txt = getCellText(row, meta.columnIndex, formatter);
                        if (meta.mandatory && Util.isEmpty(txt, true)) {
                            errors.add(new MigrationError(sheet.getSheetName(), r + 1,
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
                                errors.add(new MigrationError(sheet.getSheetName(), r + 1,
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

            Map<Integer, Integer> importedBySubmittedId = new LinkedHashMap<>();
            Integer singleOrgId = resolveSingleOrgId(ctx, wb, headers, trxName, formatter);

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

                    Integer orgId = resolveOrgIdForRow(ctx, row, orgMeta, singleOrgId, formatter, trxName, sheet.getSheetName(), r + 1);
                    int submittedId = getOrCreateSubmitted(ctx, orgId, trxName, sourceFileName);

                    PO line = newTargetPO(ctx, new X_ZZ_WSP_ATR_Submitted(ctx, submittedId, trxName), header, trxName);
                    if (line.get_ColumnIndex("Row_No") >= 0) {
                        line.set_ValueOfColumn("Row_No", Integer.valueOf(r + 1));
                    }

                    for (ColumnMeta meta : metas.values()) {
                        String mainText = getCellText(row, meta.columnIndex, formatter);
                        if (Util.isEmpty(mainText, true)) {
                            continue;
                        }
                        setValueFromText(ctx, line, meta.column, mainText, meta.useValueForRef, trxName);
                    }

                    line.saveEx();
                    importedBySubmittedId.put(submittedId, importedBySubmittedId.getOrDefault(submittedId, 0) + 1);
                }
            }

            return importedBySubmittedId;
        }

        private Integer resolveSingleOrgId(Properties ctx,
                                           Workbook wb,
                                           List<X_ZZ_WSP_ATR_Lookup_Mapping> headers,
                                           String trxName,
                                           DataFormatter formatter) {
            Integer detected = null;
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
                    Integer orgId = tryResolveRefId(ctx, orgMeta.column, orgTxt, orgMeta.useValueForRef, trxName);
                    if (orgId == null || orgId.intValue() <= 0) {
                        continue;
                    }
                    if (detected == null) {
                        detected = orgId;
                    } else if (detected.intValue() != orgId.intValue()) {
                        return null;
                    }
                }
            }
            return detected;
        }

        private ColumnMeta findOrgMeta(Map<Integer, ColumnMeta> metas) {
            for (ColumnMeta meta : metas.values()) {
                if (meta.column != null && X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZSdfOrganisation_ID.equals(meta.column.getColumnName())) {
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
                                           String trxName,
                                           String tab,
                                           int lineNo) {
            if (orgMeta != null) {
                String orgTxt = getCellText(row, orgMeta.columnIndex, formatter);
                Integer orgId = tryResolveRefId(ctx, orgMeta.column, orgTxt, orgMeta.useValueForRef, trxName);
                if (orgId != null && orgId.intValue() > 0) {
                    return orgId;
                }
                throw new AdempiereException("Could not resolve organisation for tab " + tab + " line " + lineNo);
            }
            if (singleOrgId != null && singleOrgId.intValue() > 0) {
                return singleOrgId;
            }
            throw new AdempiereException("Tab " + tab + " has no organisation mapping and workbook contains multiple organisations.");
        }

        private int getOrCreateSubmitted(Properties ctx, int orgId, String trxName, String sourceFileName) {
            int existingId = DB.getSQLValueEx(trxName,
                    "SELECT ZZ_WSP_ATR_Submitted_ID FROM ZZ_WSP_ATR_Submitted "
                    + "WHERE ZZSDFOrganisation_ID=? AND COALESCE(ZZ_Import_Submitted_Data,'N')='N' "
                    + "ORDER BY SubmittedDate DESC NULLS LAST, ZZ_WSP_ATR_Submitted_ID DESC FETCH FIRST 1 ROWS ONLY",
                    Integer.valueOf(orgId));

            int clientId = Env.getAD_Client_ID(ctx);
            MZZWSPATRSubmitted submitted = existingId > 0
                    ? new MZZWSPATRSubmitted(ctx, existingId, trxName)
                    : new MZZWSPATRSubmitted(ctx, 0, trxName);

            submitted.setName("WSP/ATR Migration " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
            submitted.setFileName(sourceFileName);
            submitted.setZZ_Import_Submitted_Data("N");
            submitted.setZZSdfOrganisation_ID(orgId);
            submitted.setZZ_DocAction(null);
            submitted.setZZ_DocStatus(X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing);
            submitted.setZZ_FinYear_ID(WspAtrSubmittedADForm.getFiscalYear(clientId));
            submitted.setZZ_Submission_Due_Date(WspAtrSubmittedADForm.getWSPATR_Due_Date(clientId, orgId));
            submitted.saveEx();

            WspAtrSubmittedADForm.rebuildSubLevyOrgLinks(submitted.get_ID(), trxName);
            return submitted.get_ID();
        }

        private Map<Integer, ColumnMeta> buildColumnMeta(Properties ctx,
                                                         X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                                                         String trxName) {
            List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> details = loadDetails(mappingHeader, trxName);
            Map<Integer, ColumnMeta> metas = new HashMap<>();
            for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
                if (Util.isEmpty(det.getZZ_Column_Letter(), true)) {
                    continue;
                }
                ColumnMeta meta = new ColumnMeta();
                meta.columnIndex = columnLetterToIndex(det.getZZ_Column_Letter());
                meta.column = new MColumn(ctx, det.getAD_Column_ID(), trxName);
                meta.useValueForRef = det.isZZ_Use_Value();
                meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();
                meta.mandatory = det.isMandatory();
                meta.ignoreIfBlank = det.isIgnore_If_Blank();
                meta.headerName = det.getZZ_Header_Name();
                metas.put(Integer.valueOf(meta.columnIndex), meta);
            }
            return metas;
        }

        @Override
        public int importData(Properties ctx, Workbook wb, X_ZZ_WSP_ATR_Submitted submitted,
                              X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader, String trxName, DataFormatter formatter) {
            return 0;
        }

        private File writeErrorLog(List<MigrationError> errors) throws Exception {
            File file = File.createTempFile("wspatr-migration-errors-", ".csv");
            try (java.io.FileWriter fw = new java.io.FileWriter(file);
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
                 java.io.PrintWriter out = new java.io.PrintWriter(bw)) {
                out.println("Tab,Line,Error");
                for (MigrationError err : errors) {
                    out.println(toCsv(err.tab) + "," + err.lineNo + "," + toCsv(err.message));
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
