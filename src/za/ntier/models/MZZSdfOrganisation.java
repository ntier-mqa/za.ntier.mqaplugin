package za.ntier.models;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MImage;
import org.compiere.model.MMailText;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;

import za.co.ntier.api.model.X_ZZSdfOrganisation;

public class MZZSdfOrganisation extends X_ZZSdfOrganisation {

    private static final long serialVersionUID = 1L;
    private static final CLogger log = CLogger.getCLogger(MZZSdfOrganisation.class);
    public static final int FROM_EMAIL_USER_ID = MSysConfig.getIntValue("FROM_EMAIL_USER_ID",1000011);
	private static final String SDF_APPROVAL_LETTER_TEMPLATE_UUID = "6be8b4db-3ce0-44b4-850b-4138deabfcfe";
	private static final String SDF_NOT_APPROVAL_LETTER_TEMPLATE_UUID = "1eade0de-1c9e-41ca-8e9c-ab3edc5b2a48";
	private static final String SDF_NOT_APPROVAL_EMAIL_BODY_UUID = "0fda3062-a8b0-43f6-aeef-006fe68989df";

    // Your fixed mail template UUID
//    private static final String SDF_APPROVED_MAILTEXT_UU = "00a3c0c0-93e6-40d1-ac00-962e92d0977e";

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

		if (!newRecord)
		{
			if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus))
			{

				Object oldStatus = get_ValueOld(COLUMNNAME_ZZ_DocStatus);
				String newStatus = getZZ_DocStatus();

				if (newStatus != null && !newStatus.equals(oldStatus))
				{
					// Send Approval
					if (ZZ_DOCSTATUS_Approved.equals(newStatus))
					{
						try
						{
							sendSdfApprovalLetter();
						}
						catch (Exception e)
						{
							log.severe("Failed to send SDF approval letter: " + e.getMessage());
						}
					}
					// Send Not Approval
					else if (ZZ_DOCSTATUS_NotApproved.equals(newStatus))
					{
						try
						{
							sendSdfNotApprovalLetter();
						}
						catch (Exception e)
						{
							log.severe("Failed to send SDF Not Approval letter: " + e.getMessage());
						}
					}
				}
			}
		}
        return super.afterSave(newRecord, success);
    }
    
    
  

    /**
     * Send email to the SDF linked to this organisation when approved.
     */
//    private void sendSdfApprovedMail(MClient client) {
//        // 1) Find SDF user
//        int adUserId = getSdfUserId();
//        if (adUserId <= 0) {
//            log.warning("No SDF AD_User found for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
//            return;
//        }
//
//        MUser user = MUser.get(getCtx(), adUserId);
//        if (user == null) {
//            log.warning("MUser not found for AD_User_ID=" + adUserId);
//            return;
//        }
//
//        String to = user.getEMail();
//        if (to == null || to.trim().isEmpty()) {
//            log.warning("SDF user has no email. AD_User_ID=" + adUserId);
//            return;
//        }
//
//        // 2) Load mail text by UU
//        MMailText mText = new MMailText(getCtx(), SDF_APPROVED_MAILTEXT_UU, get_TrxName());
//        if (mText.get_ID() <= 0) {
//            log.warning("Mail text not found for UU=" + SDF_APPROVED_MAILTEXT_UU);
//            return;
//        }
//
//        mText.setUser(user);
//        try {
//            mText.setPO(this, true); // if your version has (PO, boolean)
//        } catch (Throwable t) {
//            mText.setPO(this);       // fallback for older versions
//        }
//
//        String subject = mText.getMailHeader();
//        String message = mText.getMailText(true); // html
//
//        if (subject == null || subject.trim().isEmpty()) {
//            subject = "SDF Organisation Approved";
//        }
//
//
//        // 3) Send via client
//
//        
//        MUser from = MUser.get(Env.getCtx(), FROM_EMAIL_USER_ID);
//        
//        boolean sent = client.sendEMail(from, user, subject, message, null, mText.isHtml());
//        
//        if (!sent) {
//            log.warning("Failed to send SDF approval email to " + to
//                    + " for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
//        } else {
//            log.info("SDF approval email sent to " + to
//                    + " for ZZSdfOrganisation_ID=" + getZZSdfOrganisation_ID());
//        }
//    }

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
    
	private void sendSdfApprovalLetter() throws Exception
	{
		// Approval uses the exact same template for both Email Body and PDF content
		sendSdfLetter(SDF_APPROVAL_LETTER_TEMPLATE_UUID, SDF_APPROVAL_LETTER_TEMPLATE_UUID, "SDF_Approval_");
	}

	private void sendSdfNotApprovalLetter() throws Exception
	{
		// Not Approval uses separate templates
		sendSdfLetter(SDF_NOT_APPROVAL_EMAIL_BODY_UUID, SDF_NOT_APPROVAL_LETTER_TEMPLATE_UUID, "SDF_Not_Approval_");
	}

	private void sendSdfLetter(String emailBodyUuid, String pdfContentUuid, String pdfPrefix) throws Exception
	{
		int adUserId = getSdfUserId();
		if (adUserId <= 0)
		{
			log.warning("No recipient AD_User found for SDF letter.");
			return;
		}

		MUser toUser = MUser.get(getCtx(), adUserId);

		if (toUser.getEMail() == null || toUser.getEMail().isEmpty())
		{
			log.warning("Recipient has no email address");
			return;
		}

		// Load Email Body Template
		MMailText bodyMailText = new MMailText(getCtx(), emailBodyUuid, get_TrxName());
		if (bodyMailText.get_ID() <= 0)
		{
			log.severe("Email body template not found for UUID: " + emailBodyUuid);
			return;
		}

		// Load PDF Content Template
		MMailText pdfMailText = new MMailText(getCtx(), pdfContentUuid, get_TrxName());
		if (pdfMailText.get_ID() <= 0)
		{
			log.severe("PDF template not found for UUID: " + pdfContentUuid);
			return;
		}

		try
		{
			bodyMailText.setPO(this, true);
		}
		catch (Throwable t)
		{
			bodyMailText.setPO(this);
		}
		try
		{
			pdfMailText.setPO(this, true);
		}
		catch (Throwable t)
		{
			pdfMailText.setPO(this);
		}

		String html = bodyMailText.getMailText(true);
		String subject = bodyMailText.getMailHeader();

		if (subject == null || subject.trim().isEmpty())
			subject = "SDF Letter";

		String pdfHtml = pdfMailText.getMailText(true);

		String sdfName = getSdfUserName();
		String orgName = getOrganisationName();
		String sdlNumber = getSdlNumber();
		String appDate = getApprovalDate();
		String sdfEmail = toUser.getEMail();

		// Replace custom tokens in BOTH htmls
		if (sdfName != null)
		{
			html = html.replace("%SdfUserName%", sdfName);
			pdfHtml = pdfHtml.replace("%SdfUserName%", sdfName);
		}
		if (sdfEmail != null)
		{
			html = html.replace("%SdfEmail%", sdfEmail);
			pdfHtml = pdfHtml.replace("%SdfEmail%", sdfEmail);
		}
		if (orgName != null)
		{
			html = html.replace("%OrganisationName%", orgName);
			pdfHtml = pdfHtml.replace("%OrganisationName%", orgName);
		}
		if (sdlNumber != null)
		{
			html = html.replace("%SdlNumber%", sdlNumber);
			pdfHtml = pdfHtml.replace("%SdlNumber%", sdlNumber);
		}
		if (appDate != null)
		{
			html = html.replace("%ApprovalDate%", appDate);
			pdfHtml = pdfHtml.replace("%ApprovalDate%", appDate);
		}
		String logoFilePath = getLogoAsFilePath();
		if (logoFilePath != null && !logoFilePath.isEmpty())
		{
			pdfHtml = pdfHtml.replace("%Logo%", logoFilePath);
		}
		else
		{
			pdfHtml = pdfHtml.replace("%Logo%", "");
		}

		String logoBase64 = getLogo();
		if (logoBase64 != null && !logoBase64.isEmpty())
		{
			html = html.replace("%Logo%", logoBase64);
		}

		File pdf = createPDF(pdfHtml, pdfPrefix + (sdlNumber != null ? sdlNumber : "Unknown"));

		// Sender
		MUser fromUser = MUser.get(getCtx(), FROM_EMAIL_USER_ID);
		MClient client = MClient.get(getCtx());

		boolean sent = client.sendEMail(fromUser, toUser, subject, html, pdf, true);

		if (!sent)
			log.severe("Failed to send SDF letter email");
		else
			log.info("SDF letter email sent successfully");
	}
    
	private File createPDF(String html, String fileName) throws Exception
	{
		File pdfFile = File.createTempFile(fileName + "_", ".pdf");
		pdfFile.deleteOnExit(); // Ensure cleanup of temporary PDFs from the OS

		Document document = new Document();
		PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
		document.open();

		try
		{
			String safeHtml = html	.replaceAll("(?i)<br>", "<br/>")
									.replaceAll("(?i)<hr>", "<hr/>")
									.replaceAll("(?s)<style[^>]*>.*?</style>", "");

			Matcher m = Pattern.compile("(?is)<body[^>]*>(.*?)</body>").matcher(safeHtml);
			if (m.find())
			{
				safeHtml = m.group(1);
			}

			List elements = HTMLWorker.parseToList(new StringReader(safeHtml), null);

			boolean hasContent = false;
			if (elements != null && !elements.isEmpty())
			{
				for (Object obj : elements)
				{
					document.add((Element) obj);
					hasContent = true;
				}
			}

			if (!hasContent)
			{
				document.add(new Paragraph("No valid HTML content could be rendered."));
				String plainText = html.replaceAll("(?s)<[^>]*>", " ").trim();
				if (!plainText.isEmpty())
				{
					document.add(new Paragraph(plainText));
				}
			}
		}
		catch (Exception e)
		{
			log.severe("Error parsing HTML to PDF: " + e.getMessage());
			document.add(new Paragraph("Document generation error: " + e.getMessage()));

			String plainText = html.replaceAll("(?s)<[^>]*>", " ").trim();
			if (!plainText.isEmpty())
			{
				document.add(new Paragraph(plainText));
			}
		}

		document.close();

		return pdfFile;
	}

	public String getLogoAsFilePath()
	{
		String IMAGE_UUID = "bfdc53c3-bb63-4047-874f-ff8802d629c2";
		MImage image = new Query(getCtx(), MImage.Table_Name, "AD_Image_UU=?", get_TrxName())
																								.setParameters(IMAGE_UUID).first();
		if (image == null)
			return "";

		byte[] data = image.getData();
		if (data == null || data.length == 0)
			return "";

		try
		{
			File tempFile = File.createTempFile("logo_", ".jpg");
			tempFile.deleteOnExit(); // Ensure cleanup from OS temporary directory

			try (FileOutputStream fos = new FileOutputStream(tempFile))
			{
				fos.write(data);
			}
			return tempFile.getAbsolutePath();
		}
		catch (Exception e)
		{
			log.severe("Could not save logo to temp file: " + e.getMessage());
			return "";
		}
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
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm");
		return sdf.format(new Date());
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



