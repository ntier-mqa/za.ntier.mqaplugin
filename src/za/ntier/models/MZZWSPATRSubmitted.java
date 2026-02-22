package za.ntier.models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.compiere.model.PO;  // Required for getAllIDs
import java.util.Properties;
import java.util.List;
import org.compiere.model.Query;
import org.compiere.model.MMailText;
import java.io.File;
import java.io.FileOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import java.io.StringReader;
import java.math.BigDecimal;

import org.compiere.util.DB;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.model.MUser;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MImage;
import java.util.Base64;



public class MZZWSPATRSubmitted extends X_ZZ_WSP_ATR_Submitted {
	
	// Checklist reference search keys
	public static final String CL_HTVF = "9";
	public static final String CL_WSP_TOTAL = "10";
	public static final String CL_ATR_TOTAL = "11";
	public static final String CL_DEVIATION_PCT = "12";
	public static final String CL_COMPARE = "13";
	public static final String CL_DEVIATION = "14";
	
    // Your fixed mail template UUID
    private static final String WSP_ATRQuery_TEMPLATE_UUID = "c981b4f2-a103-4e62-a79f-f7401620bebe";
    
    public static MZZWSPATRSubmitted getSubmitted(Properties ctx,
            int submittedId,
            String trxName)
    	{
    		return new MZZWSPATRSubmitted(ctx, submittedId, trxName);
		}

	public MZZWSPATRSubmitted(Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_Submitted_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Submitted_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_Submitted_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Submitted_UU, trxName, virtualColumns);    
	    
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// TODO Auto-generated method stub
		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// TODO Auto-generated method stub
	    // Call parent first
	    boolean ok = super.afterSave(newRecord, success);

	    // Only create checklist on new record
	    if (ok && newRecord)
	        createVerificationChecklist();

	 // If user ticked IsQuery = true, generate PDF and send email
	    if (ok && is_ValueChanged("ZZ_IsQuery") && isZZ_IsQuery()) {
	        try {
	            sendQueryEmailWithPDF();
	        } catch (Exception e) {
	            log.severe("Failed to send query email: " + e.getMessage());
	        }
	    }
	    return ok;
	}
	
	
	private void createVerificationChecklist() {
	    // Load checklist template rows in numeric order of Value
		List<PO> poList = new Query(getCtx(),
		        X_ZZ_WSP_ATR_Checklist_Ref.Table_Name,
		        X_ZZ_WSP_ATR_Checklist_Ref.COLUMNNAME_IsActive + "='Y'",
		        get_TrxName())
		    .setOrderBy("CAST(Value AS integer)")
		    .list();   // <-- returns List<PO>

		int lineNo = 10;
		for (PO po : poList) {
		    X_ZZ_WSP_ATR_Checklist_Ref ref = 
		        new X_ZZ_WSP_ATR_Checklist_Ref(po.getCtx(), po.get_ID(), po.get_TrxName());

		    MZZWSPATRVeriChecklist line = new MZZWSPATRVeriChecklist(getCtx(), 0, get_TrxName());
		    line.setZZ_WSP_ATR_Submitted_ID(getZZ_WSP_ATR_Submitted_ID());
		    line.setzz_wsp_atr_checklist_ref_ID(ref.getzz_wsp_atr_checklist_ref_ID());
		    line.setLineNo(lineNo);
		    lineNo += 10;
		    line.setZZ_Checklist_No(ref.getValue());
		    line.setName(ref.getName());
		    line.setZZ_Information_Completed(false);
		    line.saveEx();
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
    
    public String getTradingAs() {

        MBPartner bp = getBusinessPartner(); // reuse helper
        if (bp == null)
            return null;

        return bp.getName2();
    }

    private MZZSdfOrganisation getSdfOrganisation() {

        int orgId = getZZSdfOrganisation_ID();
        if (orgId <= 0)
            return null;

        return new MZZSdfOrganisation(getCtx(), orgId, get_TrxName());
    }
    
    private MBPartner getBusinessPartner() {

        MZZSdfOrganisation sdfOrg = getSdfOrganisation();
        if (sdfOrg == null)
            return null;

        int bpId = sdfOrg.getC_BPartner_ID();
        if (bpId <= 0)
            return null;

        return new MBPartner(getCtx(), bpId, get_TrxName());
    }
    
    public String getOrganisationName() {
        MBPartner bp = getBusinessPartner();
        return bp != null ? bp.getName() : null;
    }

    public String getSdlNumber() {
        MBPartner bp = getBusinessPartner();
        return bp != null ? bp.getValue() : null;
    }
	
	
	private void sendQueryEmailWithPDF() throws Exception {

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

	    // Load template
	    MMailText mailText =
	        new MMailText(getCtx(), WSP_ATRQuery_TEMPLATE_UUID, get_TrxName());

	    if (mailText.get_ID() <= 0) {
	        log.severe("Mail template not found");
	        return;
	    }

	    try {
	        mailText.setPO(this, true);
	    } catch (Throwable t) {
	        mailText.setPO(this);
	    }

	    String html = mailText.getMailText(true);
	            
	   // html = html.replace("@Logo@", getLogoBase64());

	    String subject = mailText.getMailHeader();
	    if (subject == null || subject.trim().isEmpty())
	        subject = "WSP-ATR Query Notification";

	    
	    File pdf = createPDF(html);

	    // Sender
	    MUser fromUser =
	        MUser.get(getCtx(), Env.getAD_User_ID(getCtx()));

	    MClient client = MClient.get(getCtx());

	    boolean sent =
	        client.sendEMail(fromUser, toUser, subject, html, pdf, true);

	    if (!sent)
	        log.severe("Failed to send query email");
	    else
	        log.info("Query email sent successfully");
	}



	
	public String getQueryReasons()
	{
	    StringBuilder reasons = new StringBuilder();

	    List<MZZWSPATRVeriChecklist> list =
	        new Query(getCtx(),
	                  MZZWSPATRVeriChecklist.Table_Name,
	                  "ZZ_WSP_ATR_Submitted_ID=? AND ZZ_Information_Completed='N'",
	                  get_TrxName())
	        .setParameters(get_ID())
	        .list();

	    for (MZZWSPATRVeriChecklist c : list)
	        reasons.append(c.getName()).append("<br/>");

	    return reasons.toString();
	}
	
	private File createPDF(String html) throws Exception
	{
	    File pdfFile = File.createTempFile("QueryLetter_", ".pdf");

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
	
	
	public String getSentDateTime()
	{
	    java.text.SimpleDateFormat sdf =
	        new java.text.SimpleDateFormat("dd MMM yyyy HH:mm");

	    return sdf.format(new java.util.Date());
	}
	
	
	public void updateChecklistTotal(String checklistValue, BigDecimal total)
	{
	    MZZWSPATRVeriChecklist ver =
	        new Query(getCtx(),
	            MZZWSPATRVeriChecklist.Table_Name,
	            "ZZ_WSP_ATR_Submitted_ID=? AND Value=?",
	            get_TrxName())
	        .setParameters(getZZ_WSP_ATR_Submitted_ID(), checklistValue)
	        .first();

	    if (ver != null)
	    {
	        ver.set_ValueOfColumn("ZZ_TotalNo", total);
	        ver.saveEx();
	    }
	    else
	    {
	        log.warning("Checklist row not found for value=" + checklistValue);
	    }
	}


}
