package za.co.ntier.wsp_atr.process;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRATRDetail;
import za.ntier.models.MZZWSPATRHTVF;
import za.ntier.models.MZZWSPATRSubmitted;
import za.ntier.models.MZZWSPATRWSP;

public class WspAtrImportService {

    private final ReferenceLookupService refService = new ReferenceLookupService();

    public int importSubmitted(Properties ctx,
                               int submittedId,
                               String trxName,
                               SvrProcess process) throws Exception {

        if (submittedId <= 0) {
            throw new AdempiereException("No WSP/ATR Submitted record selected");
        }

        X_ZZ_WSP_ATR_Submitted submitted =
                new X_ZZ_WSP_ATR_Submitted(ctx, submittedId, trxName);

        Workbook wb = null;
        try {
            wb = loadWorkbook(submitted);

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
                    ctx,
                    X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                    null,
                    trxName)
                    .setOnlyActiveRecords(true)
                    .list();

            if (headers == null || headers.isEmpty()) {
                throw new AdempiereException("No WSP/ATR mapping header records defined");
            }

            int totalImported = 0;

            for (X_ZZ_WSP_ATR_Lookup_Mapping mapHeader : headers) {
                if (mapHeader.getAD_Table_ID() <= 0) {
                    continue;
                }

                IWspAtrSheetImporter importer = new ColumnModeSheetImporter(refService, process);
               // importer.setLog(process.getLog());

                try {
                    int count = importer.importData(
                            ctx,
                            wb,
                            submitted,
                            mapHeader,
                            trxName,
                            formatter,
                            evaluator
                    );
                    importer.importData(ctx, wb, submitted, mapHeader, trxName,  formatter,evaluator);
                    totalImported += count;

                } catch (Exception e) {
                    DB.rollback(true, trxName);

                    process.addLog("ERROR in Importer : " + e.getMessage());
                   // process.getLog().log(Level.SEVERE, "ERROR in Importer : " + e.getMessage(), e);
                    throw e;
                }
            }

            MZZWSPATRATRDetail.updateATRAndDeviation(ctx, submittedId, trxName);
            MZZWSPATRHTVF.updateHTVFTotal(ctx, submittedId, trxName);
            MZZWSPATRWSP.updateWSPTotal(ctx, submittedId, trxName);

            MZZWSPATRSubmitted mZZWSPATRSubmitted =
                    new MZZWSPATRSubmitted(ctx, submittedId, trxName);

            process.addLog(
                    "The WSP-ATR import for "
                    + mZZWSPATRSubmitted.getOrganisationName()
                    + " with SDL Number "
                    + mZZWSPATRSubmitted.getSdlNumber()
                    + " was successful."
            );

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
            return WorkbookFactory.create(is);
        }
    }
}
