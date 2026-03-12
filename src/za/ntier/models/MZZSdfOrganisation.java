package za.ntier.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import java.util.List;


import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MImage;
import org.compiere.model.MMailText;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PO;  // Required for getAllIDs
import org.compiere.model.Query;
import org.compiere.util.DB;

import com.lowagie.text.Document;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import org.compiere.util.CLogger;

import org.compiere.util.Env;

import za.co.ntier.api.model.X_ZZSdfOrganisation;

public class MZZSdfOrganisation extends X_ZZSdfOrganisation {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MZZSdfOrganisation.class);
    public static final int FROM_EMAIL_USER_ID = MSysConfig.getIntValue("FROM_EMAIL_USER_ID",1000011);
	private static final String SDF_APPROVAL_LETTER_TEMPLATE_UUID = "6be8b4db-3ce0-44b4-850b-4138deabfcfe";

    // Your fixed mail template UUID
    private static final String SDF_APPROVED_MAILTEXT_UU = "00a3c0c0-93e6-40d1-ac00-962e92d0977e";

    public MZZSdfOrganisation(Properties ctx, int ZZSdfOrganisation_ID, String trxName) {
        super(ctx, ZZSdfOrganisation_ID, trxName);
    }

    public MZZSdfOrganisation(Properties ctx, int ZZSdfOrganisation_ID, String trxName, String... virtualColumns) {
        super(ctx, ZZSdfOrganisation_ID, trxName, virtualColumns);
    }

    public MZZSdfOrganisation(Properties ctx, String ZZSdfOrganisation_UU, String trxName) {
        super(ctx, ZZSdfOrganisation_UU, trxName);
    }

    public MZZSdfOrganisation(Properties ctx, String ZZSdfOrganisation_UU, String trxName, String... virtualColumns) {
        super(ctx, ZZSdfOrganisation_UU, trxName, virtualColumns);
    }

    public MZZSdfOrganisation(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }
    
    /**
     * Returns Primary or Secondary SDF text
     */
    public String getSdfType() {

        if (isZZSecondarySdf())
            return "Secondary";

        return "Primary";
    }


    @Override
    protected boolean afterSave(boolean newRecord, boolean success) {
        if (!success) {
            return false;
        }

        if (!newRecord) {
            if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus)
                    && ZZ_DOCSTATUS_Approved.equals(getZZ_DocStatus())) {

                Object oldStatus = get_ValueOld(COLUMNNAME_ZZ_DocStatus);
                if (oldStatus != null && !ZZ_DOCSTATUS_Approved.equals(oldStatus)) {
                    MClient client = MClient.get(Env.getCtx());
                    sendSdfApprovedMail(client);
                    try {
                        sendSdfApprovalLetter();
                    } catch (Exception e) {
                        log.severe("Failed to send SDF approval letter: " + e.getMessage());
                    }
                }
                
            }
        }
        return super.afterSave(newRecord, success);
    }
    
    
  

    /**
     * Send email to the SDF linked to this organisation when approved.
     */
    private void sendSdfApprovedMail(MClient client) {
        // 1) Find SDF user
        int adUserId = getSdfUserId();
        if (adUserId <= 0) {
            log.warning("No SDF AD_User found for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
            return;
        }

        MUser user = MUser.get(getCtx(), adUserId);
        if (user == null) {
            log.warning("MUser not found for AD_User_ID=" + adUserId);
            return;
        }

        String to = user.getEMail();
        if (to == null || to.trim().isEmpty()) {
            log.warning("SDF user has no email. AD_User_ID=" + adUserId);
            return;
        }

        // 2) Load mail text by UU
        MMailText mText = new MMailText(getCtx(), SDF_APPROVED_MAILTEXT_UU, get_TrxName());
        if (mText.get_ID() <= 0) {
            log.warning("Mail text not found for UU=" + SDF_APPROVED_MAILTEXT_UU);
            return;
        }

        mText.setUser(user);
        try {
            mText.setPO(this, true); // if your version has (PO, boolean)
        } catch (Throwable t) {
            mText.setPO(this);       // fallback for older versions
        }

        String subject = mText.getMailHeader();
        String message = mText.getMailText(true); // html

        if (subject == null || subject.trim().isEmpty()) {
            subject = "SDF Organisation Approved";
        }


        // 3) Send via client

        
        MUser from = MUser.get(Env.getCtx(), FROM_EMAIL_USER_ID);
        
        boolean sent = client.sendEMail(from, user, subject, message, null, mText.isHtml());
        
        if (!sent) {
            log.warning("Failed to send SDF approval email to " + to
                    + " for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
        } else {
            log.info("SDF approval email sent to " + to
                    + " for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
        }
    }

    /**
     * FROM adempiere.zzsdforganisation orglink
     * JOIN adempiere.zzsdf sdf ON orglink.zzsdf_id = sdf.zzsdf_id
     * JOIN adempiere.ad_user usr ON sdf.ad_user_id = usr.ad_user_id
     */
    public int getSdfUserId() {
        String sql =
                "SELECT u.ad_user_id " +
                "FROM adempiere.zzsdforganisation orglink " +
                "JOIN adempiere.zzsdf sdf ON orglink.zzsdf_id = sdf.zzsdf_id " +
                "JOIN adempiere.ad_user u ON sdf.ad_user_id = u.ad_user_id " +
                "WHERE orglink.zzsdforganisation_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setInt(1, getZZSdfOrganisation_ID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            log.severe("Error getting SDF AD_User_ID: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }
        return 0;
    }
    
    public String getSdfUserName() {

        String sql =
            "SELECT u.name " +
            "FROM adempiere.zzsdforganisation orglink " +
            "JOIN adempiere.zzsdf sdf ON orglink.zzsdf_id = sdf.zzsdf_id " +
            "JOIN adempiere.ad_user u ON sdf.ad_user_id = u.ad_user_id " +
            "WHERE orglink.zzsdforganisation_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setInt(1, getZZSdfOrganisation_ID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception e) {
            log.severe("Error getting SDF Name: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }
        return null;
    }

    public String getOrganisationName() {

        int bpId = getC_BPartner_ID();
        if (bpId <= 0)
            return null;

        MBPartner bp = MBPartner.get(getCtx(), bpId);
        if (bp == null)
            return null;

        return bp.getName();
    }
    
    private void sendSdfApprovalLetter() throws Exception {

        int adUserId = getSdfUserId();
        if (adUserId <= 0) {
            log.warning("No recipient AD_User found");
            return;
        }

        MUser toUser = MUser.get(getCtx(), adUserId);

        if (toUser.getEMail() == null || toUser.getEMail().isEmpty()) {
            log.warning("Recipient has no email address");
            return;
        }

        // Load approval letter template
        MMailText mailText =
            new MMailText(getCtx(), SDF_APPROVAL_LETTER_TEMPLATE_UUID, get_TrxName());

        if (mailText.get_ID() <= 0) {
            log.severe("Approval mail template not found");
            return;
        }

        try {
            mailText.setPO(this, true);
        } catch (Throwable t) {
            mailText.setPO(this);
        }

        String html = mailText.getMailText(true);
        String subject = mailText.getMailHeader();

        if (subject == null || subject.trim().isEmpty())
            subject = "SDF Approval Letter";

        // Replace custom tokens
        //html = html.replace("@SdfName@", getSdfUserName());
        //html = html.replace("@SdfEmail@", toUser.getEMail());
        //html = html.replace("@OrgName@", getOrganisationName());
        //html = html.replace("@SDLNumber@", getSdlNumber());
        //html = html.replace("@ApprovalDate@", new Timestamp(System.currentTimeMillis()).toString());

        // Create PDF
        File pdf = createPDF(html, "SDF_Approval_" + getSdlNumber());

        // Sender
        MUser fromUser = MUser.get(getCtx(), FROM_EMAIL_USER_ID);
        MClient client = MClient.get(getCtx());

        boolean sent =
            client.sendEMail(fromUser, toUser, subject, html, pdf, true);

        if (!sent)
            log.severe("Failed to send SDF approval letter email");
        else
            log.info("SDF approval letter email sent successfully");
    }
    
	private File createPDF(String html,String fileName) throws Exception
	{
		File pdfFile = File.createTempFile(fileName + "_", ".pdf");

		Document document = new Document();
		PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

		document.open();

		HTMLWorker worker = new HTMLWorker(document);
		worker.parse(new StringReader(html));

		document.close();
		worker.close();

		return pdfFile;
	}

	public String getLogo()
	{
		String IMAGE_UUID = "bfdc53c3-bb63-4047-874f-ff8802d629c2";

		MImage image = new Query(getCtx(),
				MImage.Table_Name,
				"AD_Image_UU=?",
				get_TrxName())
				.setParameters(IMAGE_UUID)
				.first();

		if (image == null)
		{
			log.warning("Logo image not found for UUID=" + IMAGE_UUID);
			return "";
		}

		byte[] data = image.getData();
		if (data == null || data.length == 0)
			return "";

		String base64 = Base64.getEncoder().encodeToString(data);

		return "data:image/jpeg;base64," + base64;
	}


	public String getApprovalDate()
	{
		java.text.SimpleDateFormat sdf =
				new java.text.SimpleDateFormat("dd MMM yyyy HH:mm");

		return sdf.format(new java.util.Date());
	}
    
    /**
     * Get SDL number from linked Business Partner (C_BPartner.Value)
     */
    public String getSdlNumber() {
        int bpId = getC_BPartner_ID();
        if (bpId <= 0) {
            return null;
        }
        MBPartner bp = MBPartner.get(getCtx(), bpId);
        if (bp == null) {
            return null;
        }
        return bp.getValue();   // this is your zz_sdl_no
    }

}



