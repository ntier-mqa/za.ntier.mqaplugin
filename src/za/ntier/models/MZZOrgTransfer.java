package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.EMail;

import za.co.ntier.api.model.MBPartner_New;

public class MZZOrgTransfer extends X_ZZ_Org_Transfer {

	private static final String INTER_SETA_TRANSFER_FROM_MQA_DEFAULT_CC = "INTER_SETA_TRANSFER_FROM_MQA_DEFAULT_CC";
	private static final String INTER_SETA_TRANSFER_FROM_MAILTEXT_UU  = "0a9d567f-49ac-4662-b4c3-315018a4475e";


	public MZZOrgTransfer(Properties ctx, int ZZ_Org_Transfer_ID, String trxName) {
		super(ctx, ZZ_Org_Transfer_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, int ZZ_Org_Transfer_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_Org_Transfer_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, String ZZ_Org_Transfer_UU, String trxName) {
		super(ctx, ZZ_Org_Transfer_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, String ZZ_Org_Transfer_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_Org_Transfer_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	
	private void sendEmailToMgrSDR(MClient client) {

	    //  Get MailText ID from UUID
	    String mailTextUUID = "cc606c26-3bb9-4a62-89f9-b9c03d0aae9c";

	    int mailTextID = DB.getSQLValueEx(
	        get_TrxName(),
	        "SELECT r_mailtext_id FROM adempiere.r_mailtext WHERE r_mailtext_uu = ?",
	        mailTextUUID
	    );

	    if (mailTextID <= 0) {
	        log.warning("MailText not found for UUID=" + mailTextUUID);
	        return;
	    }

	    // Load MailText
	    MMailText mText = new MMailText(getCtx(), mailTextID, get_TrxName());
	    if (mText.get_ID() <= 0) {
	        log.warning("Unable to load MailText ID=" + mailTextID);
	        return;
	    }

	    // 3️ Get Mgr SDR user email
	    int AD_User_ID = getMgrSDRUserID();
	    if (AD_User_ID <= 0) {
	        log.warning("Mgr SDR user not found for record " + get_ID());
	        return;
	    }

	    String to = DB.getSQLValueStringEx(
	        get_TrxName(),
	        "SELECT email FROM adempiere.ad_user WHERE ad_user_id = ?",
	        AD_User_ID
	    );

	    if (to == null || to.trim().isEmpty()) {
	        log.warning("Mgr SDR has no email. AD_User_ID=" + AD_User_ID);
	        return;
	    }

	    //  Generate message body
	    String message = mText.getMailText(true);

	    // Optional: attach this PO for token resolution (if supported)
	    try {
	        mText.setPO(this, true);
	    } catch (Throwable t) {
	        try {
	            mText.setPO(this);
	        } catch (Throwable ignore) {}
	    }

	    //  Subject from template
	    String subject = mText.getMailHeader();

	    //  Send email using MClient
	    boolean sent = client.sendEMail(to, subject, message, null, true);

	    if (!sent)
	        log.warning("Failed to send email to Mgr SDR: " + to);
	    else
	        log.info("Email sent successfully to Mgr SDR: " + to);
	}
	
	private int getMgrSDRUserID() {
	    // Find the first active user with the role "Mgr - SDR"
	    String sql = "SELECT u.AD_User_ID FROM AD_User_Roles ur "
	               + "JOIN AD_User u ON ur.AD_User_ID=u.AD_User_ID "
	               + "JOIN AD_Role r ON ur.AD_Role_ID=r.AD_Role_ID "
	               + "WHERE r.Name='Mgr - SDR' AND u.IsActive='Y' "
	               + "ORDER BY u.AD_User_ID LIMIT 1";
	    int userId = DB.getSQLValueEx(get_TrxName(), sql);
	    return userId;
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		
	    boolean docsComplete =
	            isZZ_MotivationUploaded()
	            && isZZ_SignedISTUploaded();

	    boolean docsChanged =
	            is_ValueChanged("ZZ_MotivationUploaded") ||
	            is_ValueChanged("ZZ_SignedISTUploaded");

	    if (docsComplete
	            && docsChanged
	            && ("DR".equals(getZZ_DocStatus()) || getZZ_DocStatus() == null))
	    {
	        setZZ_DocStatus("IP"); // In Progress
	        setZZ_DocAction("S1"); // Submit
	    }

		
	    if (newRecord || is_ValueChanged("ZZ_SDL_No"))
	    {
	        String sdl = get_ValueAsString("ZZ_SDL_No");

	        if (sdl == null || sdl.trim().isEmpty())
	        {
	            log.saveError("Error", "SDL Number is mandatory");
	            return false;
	        }

	        setC_BPartner_ID(0);
	        set_Value("ZZ_Organisation_Reg_No", null);

	        // 1️⃣ Check SDR Temp Organisation table first
	        MZZSDR_Temp_Org tempOrg = new Query(getCtx(),
	                MZZSDR_Temp_Org.Table_Name,
	                "ZZ_SDL_No=? AND IsActive='Y'",
	                get_TrxName())
	                .setParameters(sdl)
	                .first();

	        if (tempOrg != null && tempOrg.getC_BPartner_ID() > 0)
	        {
	            setC_BPartner_ID(tempOrg.getC_BPartner_ID());
	            set_Value("ZZ_Organisation_Reg_No", tempOrg.getZZ_Organisation_Reg_No());
	            return true;
	        }

	     // 2️⃣ Fallback: check Business Partner table
	        MBPartner_New bp = new Query(getCtx(),
	                "C_BPartner",
	                "Value=? AND IsActive='Y'",
	                get_TrxName())
	                .setParameters(sdl)
	                .first();

	        if (bp != null)
	        {
	            setC_BPartner_ID(bp.getC_BPartner_ID());

	            // Map BP registration number to your field
	            set_Value("ZZ_Organisation_Reg_No",
	                    bp.get_ValueAsString("referenceno"));

	            return true;
	        }
	        // 3️⃣ Nothing found
	        log.saveError("Error", "No Organisation found for SDL Number");
	        return false;
	    }
	    
	    return true;

	}
	
	public String getBPName()
	{
	    int bpId = getC_BPartner_ID();
	    if (bpId <= 0)
	        return "";

	    MBPartner_New bp = new MBPartner_New(getCtx(), bpId, get_TrxName());
	    return bp.getName();
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{

		if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus)
			&& ZZ_DOCSTATUS_Approved.equals(getZZ_DocStatus()))
		{
			sendEmailAfterApproval();
		}

		return super.afterSave(newRecord, success);
	}

	private void sendEmailAfterApproval()
	{
		String applicantEmail = getEMail();
		if (applicantEmail == null || applicantEmail.trim().isEmpty())
		{
			log.warning("No applicant email found for Org Transfer record " + get_ID());
			return;
		}

		int mailTextID = DB.getSQLValueEx(
											get_TrxName(),
											"SELECT r_mailtext_id FROM adempiere.r_mailtext WHERE r_mailtext_uu = ?",
											INTER_SETA_TRANSFER_FROM_MAILTEXT_UU);

		MMailText mText = new MMailText(getCtx(), mailTextID, get_TrxName());

		if (mText.get_ID() <= 0)
		{
			log.warning("Temp Org mail text not found. UU=" + INTER_SETA_TRANSFER_FROM_MAILTEXT_UU);
			return;
		}

		try
		{
			mText.setPO(this, true);
		}
		catch (Throwable t)
		{
			try
			{
				mText.setPO(this);
			}
			catch (Throwable ignore)
			{}
		}

		String message = mText.getMailText(true);
		String subject = mText.getMailHeader();

		MClient client = MClient.get(getCtx());
		EMail email = client.createEMail(applicantEmail, subject, message, mText.isHtml());
		if (email != null)
		{
			String ccEmails = MSysConfig.getValue(INTER_SETA_TRANSFER_FROM_MQA_DEFAULT_CC);
			if (ccEmails != null && !ccEmails.trim().isEmpty())
			{
				String[] emails = ccEmails.split(",");
				for (String cc : emails)
				{
					if (cc != null && !cc.trim().isEmpty())
					{
						email.addCc(cc.trim());
					}
				}
			}
			String msg = email.send();
			boolean sent = EMail.SENT_OK.equals(msg);
			if (!sent)
				log.warning("Failed to send Inter-SETA Transfer approval email to " + applicantEmail + ": " + msg);
			else
				log.info("Inter-SETA Transfer approval email sent to " + applicantEmail);
		}
	}

}
