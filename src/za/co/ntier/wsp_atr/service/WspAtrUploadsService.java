package za.co.ntier.wsp_atr.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.apps.BackgroundJob;
import org.compiere.model.MAttachment;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.zkoss.util.media.Media;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;
import za.co.ntier.wsp_atr.repo.WspAtrUploadsRepository;
import za.ntier.models.MZZWSPATRSubmitted;

public class WspAtrUploadsService {

    private final Properties ctx;
    private final WspAtrUploadsRepository repo;
    private final String generateReportProcessUU;
    private String submitButtonMsg = null;

    public String getSubmitButtonMsg() {
        return submitButtonMsg;
    }

    public void setSubmitButtonMsg(String submitButtonMsg) {
        this.submitButtonMsg = submitButtonMsg;
    }

    public WspAtrUploadsService(Properties ctx, WspAtrUploadsRepository repo, String generateReportProcessUU) {
        this.ctx = ctx;
        this.repo = repo;
        this.generateReportProcessUU = generateReportProcessUU;
    }

    public void uploadReplace(int submittedId, String uploadType, Media media) throws Exception {
        if (media == null)
            return;

        String filename = media.getName();
        if (Util.isEmpty(filename, true))
            throw new AdempiereException("File name is empty.");

        byte[] data = getMediaBytes(media);

        String trxName = Trx.createTrxName("WSPATRUploadDoc");
        Trx trx = Trx.get(trxName, true);

        try {
            MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(ctx, submittedId, trxName);
            if (submitted.get_ID() <= 0)
                throw new AdempiereException("Submitted record not found: " + submittedId);

            Integer existingUploadId = repo.findExistingUploadId(submittedId, uploadType, trxName);

            X_ZZ_WSP_ATR_Uploads up;
            if (existingUploadId != null) {
                up = new X_ZZ_WSP_ATR_Uploads(ctx, existingUploadId.intValue(), trxName);
                repo.deleteAttachment(X_ZZ_WSP_ATR_Uploads.Table_ID, up.get_ID(), trxName);

                up.setName(uploadType + " - " + filename + " - " + now());
                up.saveEx();
            } else {
                up = new X_ZZ_WSP_ATR_Uploads(ctx, 0, trxName);
                up.setZZ_WSP_ATR_Submitted_ID(submittedId);
                up.setZZ_WSP_ATR_Upload_Type(uploadType);
                up.setName(uploadType + " - " + filename + " - " + now());
                up.saveEx();
            }

            MAttachment att = new MAttachment(ctx, X_ZZ_WSP_ATR_Uploads.Table_ID, up.get_ID(), null, trxName);
            att.addEntry(filename, data);
            att.saveEx();

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    public void generateReport(int submittedId, boolean consolidatedSubmission, boolean onlySubLevyOrgs) {
        printLatestReportInBackground(submittedId, consolidatedSubmission, onlySubLevyOrgs);
    }

    public void printLatestReportInBackground(int submittedId, boolean consolidatedSubmission, boolean onlySubLevyOrgs) {
        int reportId = repo.findLatestReportIdForSubmitted(submittedId);
        // if (reportId <= 0) throw new AdempiereException("No ZZ_WSP_ATR_Report found
        // for Submitted ID " + submittedId);

        runProcessInBackgroundWithReportAndConsolidatedParam(
                generateReportProcessUU,
                "ZZ_WSP_ATR_Report_ID",
                reportId,
                "ZZ_ConsolidatedSubmission",
                consolidatedSubmission,
                "ZZ_Only_Sub_Levy_Orgs",
                onlySubLevyOrgs,
                submittedId);
    }

    private void runProcessInBackgroundWithReportAndConsolidatedParam(
            String adProcessUU,
            String intParamName,
            int intParamValue,
            String yesNoParamName,
            boolean yesNoParamValue,
            String yesNoParamNameOnlySubs,
            boolean yesNoParamValueOnlySubs,
            int recordIdForInstance) {

        MProcess proc = MProcess.get(ctx, adProcessUU);
        if (proc == null || proc.getAD_Process_ID() <= 0)
            throw new AdempiereException("Process not found (UU=" + adProcessUU + ")");

        ProcessInfo pi = new ProcessInfo(proc.getName(), proc.getAD_Process_ID());
        pi.setAD_User_ID(Env.getAD_User_ID(ctx));
        pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
        pi.setTable_ID(MTable.getTable_ID(X_ZZ_WSP_ATR_Report.Table_Name));
        pi.setRecord_ID(recordIdForInstance);
        pi.setAD_Process_UU(proc.getAD_Process_UU());

        MPInstance instance = new MPInstance(ctx, proc.getAD_Process_ID(), 0, recordIdForInstance, null);
        instance.setIsRunAsJob(true);
        instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_None);
        instance.saveEx();

        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

        Callback<Integer> createInstanceParaCallback = id -> {
            if (id > 0) {
                MPInstance instanceLater = new MPInstance(Env.getCtx(), id, null);

                MPInstancePara para1 = new MPInstancePara(instanceLater, 10);
                para1.setParameterName(intParamName);
                para1.setP_Number(intParamValue);
                para1.saveEx();

                MPInstancePara para2 = new MPInstancePara(instanceLater, 20);
                para2.setParameterName(yesNoParamName);
                para2.setP_String(yesNoParamValue ? "Y" : "N");
                para2.saveEx();
                
                MPInstancePara para3 = new MPInstancePara(instanceLater, 30);
                para3.setParameterName(yesNoParamNameOnlySubs);
                para3.setP_String(yesNoParamValueOnlySubs ? "Y" : "N");
                para3.saveEx();
            }
        };

        BackgroundJob.create(pi)
                .withContext(ctx)
                .withNotificationType(instance.getNotificationType())
                .withInitialDelay(250)
                .run(createInstanceParaCallback);
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static byte[] getMediaBytes(Media media) {
        try {
            if (media.isBinary())
                return media.getByteData();
            return media.getStringData().getBytes("UTF-8");
        } catch (Exception e) {
            throw new AdempiereException("Failed to read uploaded file: " + e.getMessage());
        }
    }

    public boolean isEligibleToSubmit(int submittedId) {
        submitButtonMsg = null;
        int clientId = Env.getAD_Client_ID(ctx);
        int orgId = repo.getSubmittedOrgId(submittedId);

        WspAtrUploadsRepository.SdrWindowConfig cfg = WspAtrUploadsRepository.getSdrWindowConfig(clientId);
        if (cfg == null)
            return false;

        Timestamp now = new Timestamp(System.currentTimeMillis());

        boolean inMainWindow = isBetween(now, cfg.subStart, cfg.subEnd);

        boolean inExtWindow =
                isBetween(now, cfg.extStart, cfg.extEnd) &&
                repo.isOrgInApprovedWspAtrExtensionBatch(orgId);

        if (!(inMainWindow || inExtWindow))
            return false;

        // A child flagged ZZ_Parent_Uploads = 'Y' may neither upload nor submit - the parent
        // includes its figures and submits on its behalf.
        if (repo.isChildWithParentUploads(orgId)) {
            submitButtonMsg = "Parent Organisation uploads and submits for this Organisation";
            return false;
        }

        // The other half of that rule: because those children cannot submit for themselves, the
        // parent must not submit until every one of them has actually contributed. Otherwise the
        // parent lodges a "consolidated" submission that silently omits them - the report builds
        // its child set from the same status list, so a child outside it simply disappears.
        // Returns empty for a non-parent submission, so this costs one query and no special case.
        List<String> pendingChildren = repo.findChildOrgsNotSubmitted(submittedId);
        if (!pendingChildren.isEmpty()) {
            submitButtonMsg = "Child has not submitted yet: " + describePendingChildren(pendingChildren);
            return false;
        }

       // boolean hasTemplate = true;
      //  boolean hasTemplate = repo.hasSubmittedTemplateAttachment(submittedId);
        boolean hasReport = repo.hasUploadTypeAttachment(submittedId,
                X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadWSP_ATRReport);
        if (hasReport) {
            return true; //  &&hasTemplate &&
        }

        // A parent that consolidates its children may submit without a WSP-ATR of its own - the
        // submission is then made up entirely of the children's returns. This is the same test
        // the report uses to decide whether to consolidate, so the two cannot disagree about
        // whether there is anything to submit.
        if (repo.isParentOrganisationTypeForSubmitted(submittedId)) {
            return true;
        }

        submitButtonMsg = "WSP-ATR report not uploaded";
        return false;
    }

    /**
     * Renders the blocking children for the Submit button's label. A parent can have many
     * children and the label sits inline in the grid, so only the first few SDL numbers are
     * named - enough for the SDF to know where to chase, without an unreadable row.
     */
    private static String describePendingChildren(List<String> sdlNumbers) {
        final int maxNamed = 3;
        if (sdlNumbers.size() <= maxNamed) {
            return String.join(", ", sdlNumbers);
        }
        return String.join(", ", sdlNumbers.subList(0, maxNamed))
                + " (+" + (sdlNumbers.size() - maxNamed) + " more)";
    }

    private boolean isBetween(Timestamp now, Timestamp start, Timestamp end) {
        if (start == null || end == null)
            return false;
        return !now.before(start) && !now.after(end);
    }

    public void submitWspAtr(int submittedId) {
        if (!isEligibleToSubmit(submittedId)) {
            throw new AdempiereException(
                    "Cannot submit: outside submission window, status not Uploaded, or required uploads missing.");
        }

        String trxName = Trx.createTrxName("WSPATRSubmit");
        Trx trx = Trx.get(trxName, true);
        try {
            Object[] params = { Env.getAD_User_ID(ctx), submittedId };
            DB.executeUpdateEx(
                    "UPDATE zz_wsp_atr_submitted "
                            + "SET zz_docstatus='SU', ZZ_DocAction='VE', submitteddate=now(), updated=now(), updatedby=? "
                            + "WHERE zz_wsp_atr_submitted_id=?",
                    params,
                    trxName);

            MZZWSPATRSubmitted submitted =
                    MZZWSPATRSubmitted.getSubmitted(ctx, submittedId, trxName);
            submitted.sendSuccessfulSubmissionEmail();

            trx.commit(true);
        } catch (Exception e) {
            try {
                trx.rollback();
                throw e;
            } catch (Exception e2) {
            }
        } finally {
            trx.close();
        }
    }
}
