package za.co.ntier.wsp_atr.process;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRATRDetail;
import za.ntier.models.MZZWSPATRHTVF;
import za.ntier.models.MZZWSPATRSubmitted;
import za.ntier.models.MZZWSPATRWSP;

public class WspAtrImportService {

    private final ReferenceLookupService refService = new ReferenceLookupService();
    private static final String EXCEL_PASSWORD = "Learning2026";

    
    //private static final CLogger log = CLogger.getCLogger(WspAtrImportService.class);
    

    public int importSubmitted(Properties ctx,
                               int submittedId,
                               String trxName,
                               SvrProcess process) throws Exception {

        if (submittedId <= 0) {
            throw new AdempiereException("No WSP/ATR Submitted record selected");
        }

        logHeap(process, "IMPORT SERVICE START");

        X_ZZ_WSP_ATR_Submitted submitted =
                new X_ZZ_WSP_ATR_Submitted(ctx, submittedId, trxName);

        Workbook wb = null;
        try {
            wb = loadWorkbook(submitted);
            logHeap(process, "IMPORT AFTER LOAD WORKBOOK");

            DataFormatter formatter = new DataFormatter();
            List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
                    ctx,
                    X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                    null,
                    trxName)
                    .setOnlyActiveRecords(true)
                    .setOrderBy("seqNo")
                    .list();

            if (headers == null || headers.isEmpty()) {
                throw new AdempiereException("No WSP/ATR mapping header records defined");
            }

            int totalImported = 0;
            IWspAtrSheetImporter importer = new ColumnModeSheetImporter(refService, process);
            for (X_ZZ_WSP_ATR_Lookup_Mapping mapHeader : headers) {
                if (mapHeader.getAD_Table_ID() <= 0) {
                    continue;
                }
                if (mapHeader.isZZ_Is_For_Bulk()) {
                    continue;
                }
                logHeap(process, "BEFORE IMPORT TAB: " + mapHeader.getZZ_Tab_Name());
               // importer.setLog(process.getLog());

                try {
                    int count = importer.importData(
                            ctx,
                            wb,
                            submitted,
                            mapHeader,
                            trxName,
                            formatter
                    );
                    totalImported += count;
                    logHeap(process, "AFTER IMPORT TAB: " + mapHeader.getZZ_Tab_Name());

                } catch (Exception e) {
                    DB.rollback(true, trxName);

                    process.addLog("ERROR in Importer : " + e.getMessage());
                   // process.getLog().log(Level.SEVERE, "ERROR in Importer : " + e.getMessage(), e);
                    throw e;
                }
            }

            logHeap(process, "BEFORE TOTALS UPDATE");
            MZZWSPATRATRDetail.updateATRAndDeviation(ctx, submittedId, trxName);
            MZZWSPATRHTVF.updateHTVFTotal(ctx, submittedId, trxName);
            MZZWSPATRWSP.updateWSPTotal(ctx, submittedId, trxName);
            logHeap(process, "AFTER TOTALS UPDATE");
            MZZWSPATRSubmitted mZZWSPATRSubmitted =
                    new MZZWSPATRSubmitted(ctx, submittedId, trxName);

            process.addLog(
                    "The WSP-ATR import for "
                    + mZZWSPATRSubmitted.getOrganisationName()
                    + " with SDL Number "
                    + mZZWSPATRSubmitted.getSdlNumber()
                    + " was successful."
            );
            logHeap(process, "IMPORT SERVICE END");
            return totalImported;

        } finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (Exception ignore) {}
            }
        }
    }

  
    
    private Workbook loadWorkbook(X_ZZ_WSP_ATR_Submitted submitted) throws Exception {

        MAttachment attachment = MAttachment.get(
                Env.getCtx(),
                X_ZZ_WSP_ATR_Submitted.Table_ID,
                submitted.getZZ_WSP_ATR_Submitted_ID());

        if (attachment == null || attachment.getEntryCount() <= 0) {
            throw new AdempiereException("No attachment found for WSP/ATR Submitted record.");
        }

        MAttachmentEntry[] entries = attachment.getEntries();
        
        MAttachmentEntry selectedEntry = null;

        if (entries != null) {
            for (MAttachmentEntry entry : entries) {
                if (entry == null) {
                    continue;
                }

                String name = entry.getName();
                if (Util.isEmpty(name, true)) {
                    continue;
                }

                if (name.toUpperCase().startsWith("ERROR")) {
                    continue;
                }

                selectedEntry = entry;
                break;
            }
        }

        if (selectedEntry == null && entries != null && entries.length > 0) {
            selectedEntry = entries[0];
        }

        if (selectedEntry == null) {
            throw new AdempiereException("Attachment has no valid entries.");
        }

        try (InputStream is = selectedEntry.getInputStream()) {
            if (is == null) {
                throw new AdempiereException(
                        "Could not open attachment stream for file " + selectedEntry.getName());
            }

            IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);

            byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);

            return openWorkbookAuto(bytes, EXCEL_PASSWORD);
        }
    }
    
    
    private Workbook openWorkbookAuto(byte[] data, String password) throws Exception {
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(data));
        } catch (org.apache.poi.EncryptedDocumentException e) {
            try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs =
                         new org.apache.poi.poifs.filesystem.POIFSFileSystem(new ByteArrayInputStream(data))) {

                org.apache.poi.poifs.crypt.EncryptionInfo info =
                        new org.apache.poi.poifs.crypt.EncryptionInfo(fs);
                org.apache.poi.poifs.crypt.Decryptor decryptor =
                        org.apache.poi.poifs.crypt.Decryptor.getInstance(info);

                if (!decryptor.verifyPassword(password)) {
                    throw new AdempiereException("Invalid Excel password for workbook");
                }

                try (InputStream decryptedStream = decryptor.getDataStream(fs)) {
                    return WorkbookFactory.create(decryptedStream);
                }
            } catch (AdempiereException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new AdempiereException("Failed to decrypt/open Excel workbook: " + ex.getMessage(), ex);
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed to open Excel workbook: " + e.getMessage(), e);
        }
    }
    
    
    
  
    
    private void logHeap(SvrProcess process, String label) {
    	return;
        /* For debugging
        Runtime rt = Runtime.getRuntime();

        long used  = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long total = rt.totalMemory() / 1024 / 1024;
        long max   = rt.maxMemory() / 1024 / 1024;
        long free  = rt.freeMemory() / 1024 / 1024;

        String msg = label +
                " | usedMB=" + used +
                " totalMB=" + total +
                " maxMB=" + max +
                " freeMB=" + free;

        // visible in Process Monitor
        process.addLog(msg);

        // visible in server log
        log.warning(msg);
        */
    }

}
