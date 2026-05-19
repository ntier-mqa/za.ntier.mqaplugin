package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;

/**
 * Bulk-loads documents from the SG data dump into WSP/ATR upload records.
 *
 * Expected directory layout:
 *   BASE_DIR / <batch_folder> / <sdl_number> / <doc_type_folder> / <file>
 *
 * New upload type codes introduced for types not previously in the system:
 *   P = Upload Authorisation Page
 *   C = Upload Cancelled Cheque
 *   T = Upload Proof of Training
 *   S = Upload Signed Minutes  (existing)
 *
 * Process parameter:
 *   ClearUploads (Y/N) — when Y, deletes all existing 2026 upload records
 *                        and their attachments before loading.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.BulkLoadSgDocuments")
public class BulkLoadSgDocuments extends SvrProcess {

    private static final String BASE_DIR    = "/tmp/SG_Data_070526";
    private static final String FISCAL_YEAR = "2026";

    /** Maps directory names found in the dump to upload type codes. */
    private static final Map<String, String> DIR_TO_TYPE = new HashMap<>();
    static {
        DIR_TO_TYPE.put("Authorisation Page",                "P");
        DIR_TO_TYPE.put("Cancelled Cheque",                  "C");
        DIR_TO_TYPE.put("Minutes of the Training Committee",  "S");
        DIR_TO_TYPE.put("Proof of Training",                 "T");
    }

    private boolean clearUploads = false;
    private int loaded  = 0;
    private int skipped = 0;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter p : getParameter()) {
            if ("ClearUploads".equalsIgnoreCase(p.getParameterName())) {
                clearUploads = "Y".equals(p.getParameter());
            } else {
                org.compiere.model.MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        File base = new File(BASE_DIR);
        if (!base.isDirectory())
            throw new IllegalStateException("Base directory not found: " + BASE_DIR);

        if (clearUploads)
            clearAllUploads();

        for (File batchDir : dirs(base)) {
            for (File sdlDir : dirs(batchDir)) {
                processSDL(sdlDir.getName(), sdlDir);
            }
        }

        return "Loaded=" + loaded + "  Skipped=" + skipped;
    }

    // -------------------------------------------------------------------------
    //  Clear
    // -------------------------------------------------------------------------

    /**
     * Deletes all ZZ_WSP_ATR_Uploads records (and their attachments) that
     * belong to a submission in fiscal year 2026.
     */
    private void clearAllUploads() {
        addLog("ClearUploads=Y — removing all existing uploads for fiscal year " + FISCAL_YEAR + " ...");

        String trxName = Trx.createTrxName("SGDocClear");
        Trx trx = Trx.get(trxName, true);
        try {
            // 1. Attachment headers (binary data stored directly in ad_attachment)
            DB.executeUpdateEx(
                "DELETE FROM ad_attachment " +
                "WHERE ad_table_id = " + X_ZZ_WSP_ATR_Uploads.Table_ID + " " +
                "AND record_id IN (" +
                "  SELECT u.zz_wsp_atr_uploads_id FROM zz_wsp_atr_uploads u " +
                "  JOIN zz_wsp_atr_submitted s ON s.zz_wsp_atr_submitted_id = u.zz_wsp_atr_submitted_id " +
                "  JOIN c_year y ON y.c_year_id = s.zz_finyear_id " +
                "  WHERE y.fiscalyear = '" + FISCAL_YEAR + "'" +
                ")",
                null, trxName);

            // 2. Upload records
            int deleted = DB.executeUpdateEx(
                "DELETE FROM zz_wsp_atr_uploads " +
                "WHERE zz_wsp_atr_submitted_id IN (" +
                "  SELECT s.zz_wsp_atr_submitted_id FROM zz_wsp_atr_submitted s " +
                "  JOIN c_year y ON y.c_year_id = s.zz_finyear_id " +
                "  WHERE y.fiscalyear = '" + FISCAL_YEAR + "'" +
                ")",
                null, trxName);

            trx.commit(true);
            addLog("Cleared " + deleted + " upload record(s).");

        } catch (Exception e) {
            trx.rollback();
            throw new AdempiereException("Failed to clear uploads: " + e.getMessage(), e);
        } finally {
            trx.close();
        }
    }

    // -------------------------------------------------------------------------
    //  Load
    // -------------------------------------------------------------------------

    private void processSDL(String sdlNumber, File sdlDir) {
        int submittedId = findSubmittedId(sdlNumber);
        if (submittedId <= 0) {
            addLog("No " + FISCAL_YEAR + " submission for SDL " + sdlNumber + " — skipped");
            skipped++;
            return;
        }

        for (File docTypeDir : dirs(sdlDir)) {
            String uploadType = DIR_TO_TYPE.get(docTypeDir.getName());
            if (uploadType == null) {
                addLog("Unknown doc type '" + docTypeDir.getName() + "' for " + sdlNumber + " — skipped");
                skipped++;
                continue;
            }
            for (File file : files(docTypeDir)) {
                loadFile(submittedId, uploadType, file, sdlNumber);
            }
        }
    }

    private void loadFile(int submittedId, String uploadType, File file, String sdlNumber) {
        String trxName = Trx.createTrxName("SGDocLoad");
        Trx trx = Trx.get(trxName, true);
        String step = "reading file";
        try {
            byte[] data     = Files.readAllBytes(file.toPath());
            String filename = file.getName();

            step = "saving upload record (ZZ_WSP_ATR_Uploads)";
            X_ZZ_WSP_ATR_Uploads up = new X_ZZ_WSP_ATR_Uploads(getCtx(), 0, trxName);
            up.setZZ_WSP_ATR_Submitted_ID(submittedId);
            up.setZZ_WSP_ATR_Upload_Type(uploadType);
            up.setName(uploadType + " - " + filename + " - " + now());
            up.saveEx();

            step = "saving attachment (MAttachment)";
            MAttachment att = new MAttachment(getCtx(), X_ZZ_WSP_ATR_Uploads.Table_ID, up.get_ID(), null, trxName);
            att.addEntry(filename, data);
            att.saveEx();

            trx.commit(true);
            loaded++;
            addLog("Loaded: SDL=" + sdlNumber + " type=" + uploadType + " file=" + filename);

        } catch (Exception e) {
            trx.rollback();
            skipped++;
            addLog("ERROR [step=" + step + "] SDL=" + sdlNumber
                    + " type=" + uploadType
                    + " file=" + file.getName()
                    + " — " + rootCauseChain(e));
        } finally {
            trx.close();
        }
    }

    /**
     * Walks the full exception chain and appends the stack trace of the root
     * cause so that internally-swallowed DB errors are always visible.
     */
    private static String rootCauseChain(Throwable t) {
        // 1. Build the cause chain message
        StringBuilder sb = new StringBuilder();
        Throwable current = t;
        while (current != null) {
            if (sb.length() > 0) sb.append(" → ");
            String msg = current.getMessage();
            sb.append(current.getClass().getSimpleName())
              .append(": ")
              .append(msg != null ? msg.trim() : "(no message)");
            Throwable cause = current.getCause();
            if (cause == current) break;
            current = cause;
        }

        // 2. Append the full stack trace so internally-swallowed DB errors
        //    (e.g. iDempiere re-wrapping PSQLException as "SaveError") are visible
        sb.append("\n");
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        sb.append(sw.toString());

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    //  Lookup
    // -------------------------------------------------------------------------

    /**
     * Finds the ZZ_WSP_ATR_Submitted_ID for this SDL number and fiscal year 2026.
     * SDL number is stored as C_BPartner.Value.
     */
    private int findSubmittedId(String sdlNumber) {
        return DB.getSQLValueEx(null,
                "SELECT s.zz_wsp_atr_submitted_id " +
                "FROM zz_wsp_atr_submitted s " +
                "JOIN zzsdforganisation org ON org.zzsdforganisation_id = s.zzsdforganisation_id " +
                "JOIN c_bpartner bp ON bp.c_bpartner_id = org.c_bpartner_id " +
                "JOIN c_year y ON y.c_year_id = s.zz_finyear_id " +
                "WHERE bp.value = ? " +
                "AND y.fiscalyear = '" + FISCAL_YEAR + "' " +
                "ORDER BY s.created DESC " +
                "LIMIT 1",
                sdlNumber);
    }

    // -------------------------------------------------------------------------
    //  Helpers
    // -------------------------------------------------------------------------

    private static File[] dirs(File parent) {
        File[] d = parent.listFiles(File::isDirectory);
        return d != null ? d : new File[0];
    }

    private static File[] files(File parent) {
        File[] f = parent.listFiles(File::isFile);
        return f != null ? f : new File[0];
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
