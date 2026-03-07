package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.Query;
import org.compiere.util.DB;
import za.ntier.models.MZZSDR_Temp_Org;
import org.compiere.model.MMailText;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.model.MUser;
import org.compiere.model.MClient;

public class MZZOrgTransfer extends X_ZZ_Org_Transfer {

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
	        setZZ_DocAction("UP"); // Update
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

	        MZZSDR_Temp_Org tempOrg = new Query(getCtx(),
	        		MZZSDR_Temp_Org.Table_Name,
	                "ZZ_SDL_No=? AND IsActive='Y'",
	                get_TrxName())
	                .setParameters(sdl)
	                .first();

	        if (tempOrg == null)
	        {
	            log.saveError("Error", "No Organisation found for SDL Number");
	            return false;
	        }

	        if (tempOrg.getC_BPartner_ID() <= 0)
	        {
	            log.saveError("Error", "Organisation not linked to Business Partner");
	            return false;
	        }

	        setC_BPartner_ID(tempOrg.getC_BPartner_ID());
	        set_Value("ZZ_Organisation_Reg_No",
	                tempOrg.getZZ_Organisation_Reg_No());
	    }

	    return true;
	}
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// TODO Auto-generated method stub
		return super.afterSave(newRecord, success);
	}

}
