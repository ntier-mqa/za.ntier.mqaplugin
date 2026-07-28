package za.ntier.process;



import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.util.ProcessUtil;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import za.ntier.models.MZZPettyCashApplication;
import za.ntier.models.X_ZZ_Paycard;
import za.ntier.models.X_ZZ_Petty_Cash_Application;
import za.ntier.utils.Notifications;


@org.adempiere.base.annotation.Process
public class PettyCashApplication extends SvrProcess {
	private static final String PLEASE_UPLOAD_THE_AOR_AND_TICK_THE_CHECKBOX_WHEN_DONE = "Please upload the AOR and tick the checkbox when done.";
	private static final String YOUR_APPLICATION_WAS_REJECTED_PETTY_CASH_CARD_APPLICATION2 = "Your application was rejected, Petty Cash Card Application : ";
	private static final String YOUR_APPLICATION_WAS_APPROVED_PETTY_CASH_CARD_APPLICATION = "Your application was approved, Petty Cash Card Application : ";
	private static final String YOUR_APPLICATION_WAS_REJECTED_PETTY_CASH_CARD_APPLICATION = "Your application was rejected Petty Cash Card Application : ";
	private static final CLogger log = CLogger.getCLogger(ProcessUtil.class);
	private static final String PLEASE_APPROVE_OR_REJECT_THE_PETTY_CASH_CARD_APPLICATION = "Please Approve or Reject the Petty Cash Card Application : ";
	private static final String PETTY_CASH_CARD_APPLICATION = "Petty Cash Card Application ";
	public final static int MESSAGE_NEW_PETTYCASH_APP = 1000009;
	public final static int MESSAGE_LM_APPROVED_PETTYCASH_APP = 1000010;
	public final static int SNR_ADMIN_FIN_ROLE_ID = 1000003;
	public final static int FROM_EMAIL_USER_ID = MSysConfig.getIntValue("FROM_EMAIL_USER_ID",1000011);
	String p_ZZ_Approve_Rej_LM = "";
	String p_ZZ_Approve_Rej_SAF = "";

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("ZZ_Approve_Rej_LM"))
				p_ZZ_Approve_Rej_LM = (String)para[i].getParameter();
			else if (name.equals("ZZ_Approve_Rej_SAF"))
				p_ZZ_Approve_Rej_SAF = (String)para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}

	}

	@Override
	protected String doIt() throws Exception {
		int zz_Petty_Cash_Application_ID = getRecord_ID();
		MZZPettyCashApplication mZZPettyCashApplication = new MZZPettyCashApplication(getCtx(),zz_Petty_Cash_Application_ID,get_TrxName());
		if (mZZPettyCashApplication.getZZ_DocAction().equals(MZZPettyCashApplication.ZZ_DOCACTION_SubmitToLineManager)) {
			if (mZZPettyCashApplication.getLine_Manager_ID() <= 0) {
				return "Please select a Line Manager";
			}
			if (!mZZPettyCashApplication.isZZ_ID_Copy_Uploaded()) {
				return "Please upload your ID copy and tick the checkbox when done.";
			}
			mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_Submitted);
			mZZPettyCashApplication.setZZ_DocAction(MZZPettyCashApplication.DOCACTION_Approve);
			mZZPettyCashApplication.setZZ_Date_Submitted(new Timestamp(System.currentTimeMillis()));
			String subject = PETTY_CASH_CARD_APPLICATION + "awaiting LM Approval";
			String message = PLEASE_APPROVE_OR_REJECT_THE_PETTY_CASH_CARD_APPLICATION + mZZPettyCashApplication.getDocumentNo();
			int ad_Message_ID = MESSAGE_NEW_PETTYCASH_APP;
			Notifications.sendNotification(mZZPettyCashApplication.getLine_Manager_ID(),zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
		} else if (mZZPettyCashApplication.getZZ_DocAction().equals(MZZPettyCashApplication.ZZ_DOCACTION_ApproveDoNotApprove) && 
				mZZPettyCashApplication.getZZ_DocStatus().equals(MZZPettyCashApplication.ZZ_DOCSTATUS_Submitted)) {
			if (p_ZZ_Approve_Rej_LM.equals("Y")) {
				mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_InProgress);
				mZZPettyCashApplication.setZZ_DocAction(MZZPettyCashApplication.ZZ_DOCACTION_FinalApprovalDoNotApprove);
				mZZPettyCashApplication.setZZ_Date_LM_Approved(new Timestamp(System.currentTimeMillis()));
				String subject = PETTY_CASH_CARD_APPLICATION + "awaiting Finance Approval";
				String message = PLEASE_APPROVE_OR_REJECT_THE_PETTY_CASH_CARD_APPLICATION + mZZPettyCashApplication.getDocumentNo();
				int ad_Message_ID = MESSAGE_LM_APPROVED_PETTYCASH_APP;
				Notifications.sendNotificationToRole(SNR_ADMIN_FIN_ROLE_ID,zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
			} else {
				mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_NotApprovedByLM);
				mZZPettyCashApplication.setZZ_Date_Not_Approved_by_LM(new Timestamp(System.currentTimeMillis()));
				String subject = PETTY_CASH_CARD_APPLICATION + "Rejected by the LM";
				String message = YOUR_APPLICATION_WAS_REJECTED_PETTY_CASH_CARD_APPLICATION + mZZPettyCashApplication.getDocumentNo();
				int ad_Message_ID = MESSAGE_NEW_PETTYCASH_APP;
				Notifications.sendNotification(mZZPettyCashApplication.getCreatedBy(),zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
			}

		} else if (mZZPettyCashApplication.getZZ_DocAction().equals(MZZPettyCashApplication.ZZ_DOCACTION_FinalApprovalDoNotApprove) && 
				mZZPettyCashApplication.getZZ_DocStatus().equals(MZZPettyCashApplication.ZZ_DOCSTATUS_InProgress)) {
			if (p_ZZ_Approve_Rej_SAF.equals("Y")) {
				mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_Approved);
				mZZPettyCashApplication.setZZ_DocAction(MZZPettyCashApplication.ZZ_DOCACTION_Complete);
				mZZPettyCashApplication.setZZ_Date_Approved(new Timestamp(System.currentTimeMillis()));
				mZZPettyCashApplication.setZZ_Snr_Admin_Fin_ID(Env.getAD_User_ID(getCtx()));
				String subject = PETTY_CASH_CARD_APPLICATION + "Has been Approved by Finance";
				String message = YOUR_APPLICATION_WAS_APPROVED_PETTY_CASH_CARD_APPLICATION + mZZPettyCashApplication.getDocumentNo();
				int ad_Message_ID = MESSAGE_NEW_PETTYCASH_APP;
				Notifications.sendNotification(mZZPettyCashApplication.getCreatedBy(),zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
			} else {
				mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_NotApprovedBySnrAdminFinance);
				mZZPettyCashApplication.setZZ_Date_Not_Approved_by_Snr_Adm_Fin(new Timestamp(System.currentTimeMillis()));
				String subject = PETTY_CASH_CARD_APPLICATION + "Rejected by Finance";
				String message = YOUR_APPLICATION_WAS_REJECTED_PETTY_CASH_CARD_APPLICATION2 + mZZPettyCashApplication.getDocumentNo();
				int ad_Message_ID = MESSAGE_NEW_PETTYCASH_APP;
				Notifications.sendNotification(mZZPettyCashApplication.getCreatedBy(),zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
				Notifications.sendNotification(mZZPettyCashApplication.getLine_Manager_ID(),zz_Petty_Cash_Application_ID,subject,message,ad_Message_ID,getCtx(),get_TrxName(),X_ZZ_Petty_Cash_Application.Table_ID);
			}
		} else if (mZZPettyCashApplication.getZZ_DocAction().equals(MZZPettyCashApplication.ZZ_DOCACTION_Complete) && 
				mZZPettyCashApplication.getZZ_DocStatus().equals(MZZPettyCashApplication.ZZ_DOCSTATUS_Approved)) {
			if (!mZZPettyCashApplication.isZZ_AOR_Uploaded()) {
				return PLEASE_UPLOAD_THE_AOR_AND_TICK_THE_CHECKBOX_WHEN_DONE;
			}
			if (mZZPettyCashApplication.getZZ_PaycardNumber() == null) {
				return "Please enter the Paycard Number Before Completing";
			}
			boolean paycardExists = new Query(getCtx(), X_ZZ_Paycard.Table_Name, "ZZ_PaycardNumber=?", get_TrxName())
					.setParameters(mZZPettyCashApplication.getZZ_PaycardNumber())
					.setOnlyActiveRecords(false)
					.match();
			if (!paycardExists) {
				X_ZZ_Paycard zzPaycard = new X_ZZ_Paycard(getCtx(), 0, get_TrxName());
				zzPaycard.setAD_Org_ID(mZZPettyCashApplication.getAD_Org_ID());
				zzPaycard.setAD_User_ID(mZZPettyCashApplication.getAD_User_ID());
				zzPaycard.setZZ_PaycardNumber(mZZPettyCashApplication.getZZ_PaycardNumber());
				zzPaycard.saveEx();
			}
			mZZPettyCashApplication.setZZ_DocStatus(MZZPettyCashApplication.ZZ_DOCSTATUS_Completed);
			mZZPettyCashApplication.setZZ_Date_Completed(new Timestamp(System.currentTimeMillis()));
		}
		if (!mZZPettyCashApplication.save()) {
			if (log.isLoggable(Level.FINE)) log.fine("Problem Saving.  Please contact Support");
			return "Problem Saving.  Please contact Support";
		}


		return null;
	}




}
