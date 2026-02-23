package za.co.ntier.wsp_atr.service;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
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

	public WspAtrUploadsService(Properties ctx, WspAtrUploadsRepository repo, String generateReportProcessUU) {
		this.ctx = ctx;
		this.repo = repo;
		this.generateReportProcessUU = generateReportProcessUU;
	}

	public void uploadReplace(int submittedId, String uploadType, Media media) throws Exception {
		if (media == null) return;

		String filename = media.getName();
		if (Util.isEmpty(filename, true)) throw new AdempiereException("File name is empty.");

		byte[] data = getMediaBytes(media);

		String trxName = Trx.createTrxName("WSPATRUploadDoc");
		Trx trx = Trx.get(trxName, true);

		try {
			MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(ctx, submittedId, trxName);
			if (submitted.get_ID() <= 0) throw new AdempiereException("Submitted record not found: " + submittedId);

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

	public void printLatestReportInBackground(int submittedId) {
		int reportId = repo.findLatestReportIdForSubmitted(submittedId);
		//if (reportId <= 0) throw new AdempiereException("No ZZ_WSP_ATR_Report found for Submitted ID " + submittedId);

		runProcessInBackgroundWithIntParam(generateReportProcessUU, "ZZ_WSP_ATR_Report_ID", reportId, submittedId);
	}

	private void runProcessInBackgroundWithIntParam(String adProcessUU, String paramName, int paramValue, int recordIdForInstance) {
		MProcess proc = MProcess.get(ctx, adProcessUU);
		if (proc == null || proc.getAD_Process_ID() <= 0) throw new AdempiereException("Process not found (UU=" + adProcessUU + ")");

		ProcessInfo pi = new ProcessInfo(proc.getName(), proc.getAD_Process_ID());
		pi.setAD_User_ID(Env.getAD_User_ID(ctx));
		pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
		pi.setTable_ID(MTable.getTable_ID(X_ZZ_WSP_ATR_Report.Table_Name));
		pi.setRecord_ID(recordIdForInstance);
		pi.setAD_Process_UU(proc.getAD_Process_UU());

		MPInstance instance = new MPInstance(ctx, proc.getAD_Process_ID(), 0, recordIdForInstance, null);
		instance.setIsRunAsJob(true);
		instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
		instance.saveEx();

		pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

		Callback<Integer> createInstanceParaCallback = id -> {
			if (id > 0) {
				MPInstance instanceLater = new MPInstance(Env.getCtx(), id, null);
				MPInstancePara para = new MPInstancePara(instanceLater, 10);
				para.setParameterName(paramName);
				para.setP_Number(paramValue);
				para.saveEx();
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
			if (media.isBinary()) return media.getByteData();
			return media.getStringData().getBytes("UTF-8");
		} catch (Exception e) {
			throw new AdempiereException("Failed to read uploaded file: " + e.getMessage());
		}
	}

	public boolean isEligibleToSubmit(int submittedId) {
		int clientId = Env.getAD_Client_ID(ctx);
		int orgId = repo.getSubmittedOrgId(submittedId);

		WspAtrUploadsRepository.SdrWindowConfig cfg = repo.getSdrWindowConfig(clientId);
		if (cfg == null) return false;

		Timestamp now = new Timestamp(System.currentTimeMillis());

		boolean inMainWindow = isBetween(now, cfg.subStart, cfg.subEnd);

		boolean inExtWindow =
				isBetween(now, cfg.extStart, cfg.extEnd) &&
				repo.isOrgInApprovedWspAtrExtensionBatch(orgId);

		if (!(inMainWindow || inExtWindow))
			return false;

		// must be Uploaded status
		//if (!repo.isSubmissionStatusUploaded(submittedId))
		//	return false;

		// must have 4 uploads:
		boolean hasTemplate = repo.hasSubmittedTemplateAttachment(submittedId);
		boolean hasReport = repo.hasUploadTypeAttachment(submittedId,
				za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadWSP_ATRReport);
		boolean hasMinutes = repo.hasUploadTypeAttachment(submittedId,
				za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadSignedMinutes);
		boolean hasAttendance = repo.hasUploadTypeAttachment(submittedId,
				za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadAttendanceRegister);

		return hasTemplate && hasReport && hasMinutes && hasAttendance;
	}

	private boolean isBetween(Timestamp now, Timestamp start, Timestamp end) {
		if (start == null || end == null) return false;
		return !now.before(start) && !now.after(end);
	}

	public void submitWspAtr(int submittedId) {
		if (!isEligibleToSubmit(submittedId)) {
			throw new AdempiereException("Cannot submit: outside submission window, status not Uploaded, or required uploads missing.");
		}

		String trxName = Trx.createTrxName("WSPATRSubmit");
		Trx trx = Trx.get(trxName, true);
		try {
			// TODO: set your real submission status column/value
			Object[] params = {Env.getAD_User_ID(ctx),submittedId};
			DB.executeUpdateEx(
					"UPDATE zz_wsp_atr_submitted " +
							"SET zz_wsp_atr_status='SU',ZZ_DocAction = 'S1', updated=now(), updatedby=? " +
							"WHERE zz_wsp_atr_submitted_id=?",
							params,
							trxName
					);

			// OPTIONAL: action log
			// repo.insertActionLog(submittedId, "SUBMIT", null, trxName);
			MZZWSPATRSubmitted submitted =
			        MZZWSPATRSubmitted.getSubmitted(ctx, submittedId,trxName );
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