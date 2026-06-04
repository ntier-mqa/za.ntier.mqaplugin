package za.ntier.models;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.MClient;
import org.compiere.model.MImage;
import org.compiere.model.MMailText;
import org.compiere.model.MYear;
import org.compiere.model.Query;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;

import za.co.ntier.api.model.X_ZZ_WSP_ATR_EXTENSION;
import za.co.ntier.api.model.X_ZZ_WSP_ATR_EXTENSION_BATCH;

public class MZZWSPATRExtensionBatch extends X_ZZ_WSP_ATR_EXTENSION_BATCH
{

	private static final long	serialVersionUID								= 1L;

	private static final String	EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID	= "e936277c-81d9-49cf-8fcf-29096e734719";
	private static final String	APPROVAL_OF_EXTENSION_TO_SUBMIT_WSP_ATR_LETTER_TEMPLATE_UUID	= "1051b149-f2fa-4758-87f0-b3ac516ef17b";

	public MZZWSPATRExtensionBatch(Properties ctx, int ZZ_WSP_ATR_EXTENSION_BATCH_ID, String trxName)
	{
		super(ctx, ZZ_WSP_ATR_EXTENSION_BATCH_ID, trxName);
	}

	public MZZWSPATRExtensionBatch(Properties ctx, int ZZ_WSP_ATR_EXTENSION_BATCH_ID, String trxName,
			String... virtualColumns)
	{
		super(ctx, ZZ_WSP_ATR_EXTENSION_BATCH_ID, trxName, virtualColumns);
	}

	public MZZWSPATRExtensionBatch(Properties ctx, String ZZ_WSP_ATR_EXTENSION_BATCH_UU, String trxName)
	{
		super(ctx, ZZ_WSP_ATR_EXTENSION_BATCH_UU, trxName);
	}

	public MZZWSPATRExtensionBatch(Properties ctx, String ZZ_WSP_ATR_EXTENSION_BATCH_UU, String trxName,
			String... virtualColumns)
	{
		super(ctx, ZZ_WSP_ATR_EXTENSION_BATCH_UU, trxName, virtualColumns);
	}

	public MZZWSPATRExtensionBatch(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
	    boolean ok = super.afterSave(newRecord, success);

		if (ok && is_ValueChanged(COLUMNNAME_ZZ_DocStatus) && MZZWSPATRExtensionBatch.ZZ_DOCSTATUS_Approved.equals(getZZ_DocStatus()))
		{
	        try
	        {
	            updateLinkedSubmissionDueDates();
	        }
	        catch (Exception e)
	        {
	            log.log(Level.SEVERE, "Failed to update linked submission due dates: " + e.getMessage(), e);
	        }

	        try
	        {
	            sendFinalApprovalEmail();
	        }
	        catch (Exception e)
	        {
	            log.log(Level.SEVERE, "Failed to send extension batch approval email: " + e.getMessage(), e);
	        }
	    }

	    return ok;
	}
	
	private void updateLinkedSubmissionDueDates()
	{
	    Timestamp batchExtEndDate = getZZ_WSP_ATR_Ext_End_Date();
	    if (batchExtEndDate == null)
	    {
	        log.warning("Batch extension end date is null for batch ID=" + get_ID());
	        return;
	    }

	    List<X_ZZ_WSP_ATR_EXTENSION> extensions = new Query(
	            getCtx(),
	            X_ZZ_WSP_ATR_EXTENSION.Table_Name,
	            "ZZ_WSP_ATR_EXTENSION_BATCH_ID=? AND IsActive='Y'",
	            get_TrxName())
	        .setParameters(get_ID())
	        .list();

	    if (extensions == null || extensions.isEmpty())
	    {
	        log.warning("No related extensions found for batch ID=" + get_ID());
	        return;
	    }

	    Set<Integer> submittedIds = new HashSet<>();
	    for (X_ZZ_WSP_ATR_EXTENSION ext : extensions)
	    {
	        if (ext.getZZ_WSP_ATR_Submitted_ID() > 0)
	        {
	            submittedIds.add(ext.getZZ_WSP_ATR_Submitted_ID());
	        }
	    }

	    if (submittedIds.isEmpty())
	    {
	        log.info("No linked submitted records found for batch ID=" + get_ID());
	        return;
	    }

	    int updatedCount = 0;

	    for (Integer submittedId : submittedIds)
	    {
	        MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), submittedId, get_TrxName());
	        if (submitted.get_ID() <= 0 || !submitted.isActive())
	        {
	            continue;
	        }

	        Timestamp currentDueDate = submitted.getZZ_Submission_Due_Date();

	        // only move forward, never backward
	        if (currentDueDate == null || batchExtEndDate.after(currentDueDate))
	        {
	            submitted.setZZ_Submission_Due_Date(batchExtEndDate);
	            submitted.saveEx();
	            updatedCount++;
	        }
	    }

	    log.info("Updated " + updatedCount + " linked submitted due date(s) for approved extension batch ID=" + get_ID());
	}

	/**
	 * Send email to all SDF and SOR emails associated with child extensions.
	 */
	public void sendFinalApprovalEmail() throws Exception
	{
		int batchId = get_ID();

		MMailText mailText = new MMailText(getCtx(), EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID, get_TrxName());

		if (mailText.get_ID() <= 0)
		{
			log.severe(	"Mail template not found for Extension Batch Approval UUID: "
						+ EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID);
			return;
		}

		MMailText pdfMailText = new MMailText(getCtx(), APPROVAL_OF_EXTENSION_TO_SUBMIT_WSP_ATR_LETTER_TEMPLATE_UUID, get_TrxName());
		if (pdfMailText.get_ID() <= 0) {
			log.severe("PDF Mail template not found for UUID: " + APPROVAL_OF_EXTENSION_TO_SUBMIT_WSP_ATR_LETTER_TEMPLATE_UUID);
			return;
		}

		List<X_ZZ_WSP_ATR_EXTENSION> extensions = new Query(getCtx(), X_ZZ_WSP_ATR_EXTENSION.Table_Name,
															"ZZ_WSP_ATR_EXTENSION_BATCH_ID=?", get_TrxName()).setParameters(batchId).list();

		if (extensions.isEmpty())
		{
			log.warning("No related extensions found for Extension Batch ID: " + batchId);
			return;
		}

		MClient client = MClient.get(getCtx());
		int successCount = 0;

		String messageTemplate = mailText.getMailText(true);
		String subjectTemplate = mailText.getMailHeader();

		if (messageTemplate == null)
			messageTemplate = "";
		if (subjectTemplate == null || subjectTemplate.trim().isEmpty())
			subjectTemplate = "Request for Extension Has Been Approved (@OrgName@)";

		String pdfTemplate = pdfMailText.getMailText(true);
		if (pdfTemplate == null) pdfTemplate = "";

		int sdrConfigId = new Query(getCtx(), X_ZZ_SDR_Configuration.Table_Name, "IsActive='Y'", get_TrxName())
			.setOrderBy("Created DESC")
			.firstId();

		X_ZZ_SDR_Configuration sdrConfig = null;
		if (sdrConfigId > 0) {
			sdrConfig = new X_ZZ_SDR_Configuration(getCtx(), sdrConfigId, get_TrxName());
		}

		String sdrYear = "";
		String sdrExpiryDate = "";
		if (sdrConfig != null) {
			if (sdrConfig.getZZ_FinYear_ID() > 0) {
				MYear mYear = new MYear(getCtx(), sdrConfig.getZZ_FinYear_ID(), get_TrxName());
				sdrYear = mYear.getFiscalYear();
				if (sdrYear == null) sdrYear = "";
			}
			if (sdrConfig.getZZ_WSP_ATR_Ext_End_Date() != null) {
				sdrExpiryDate = new SimpleDateFormat("dd MMM yyyy").format(sdrConfig.getZZ_WSP_ATR_Ext_End_Date());
			}
		}
		String currentDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

		for (X_ZZ_WSP_ATR_EXTENSION ext : extensions)
		{
			String orgName = ext.getZZ_Organisation_Name();
			String sdlNo = ext.getZZ_SDL_No();
			String documentNo = ext.getDocumentNo();

			orgName = orgName != null ? orgName : "";
			sdlNo = sdlNo != null ? sdlNo : "";
			documentNo = documentNo != null ? documentNo : "";

			String baseMessage = messageTemplate
												.replace("@OrgName@", orgName)
												.replace("@SDLNo@", sdlNo)
												.replace("@DocumentNo@", documentNo);

			String baseSubject = subjectTemplate
												.replace("@OrgName@", orgName);

			String sdfEmail = ext.getZZ_SDF_EMAIL();
			String sorEmail = ext.getZZ_SOR_EMAIL();

			String basePdfHtml = pdfTemplate
					.replace("%OrganisationName%", orgName)
					.replace("%SdlNumber%", sdlNo)
					.replace("%SDRYear%", sdrYear)
					.replace("%SDRExpiryDate%", sdrExpiryDate)
					.replace("%CurrentDate%", currentDate);

			String logoFilePath = getLogoAsFilePath();
			if (logoFilePath != null && !logoFilePath.isEmpty()) {
				basePdfHtml = basePdfHtml.replace("%Logo%", logoFilePath);
			} else {
				basePdfHtml = basePdfHtml.replace("%Logo%", "");
			}

			String sdfName = (ext.getZZ_SDF_FirstName() != null ? ext.getZZ_SDF_FirstName() : "")	+ " " +
								(ext.getZZ_SDF_Surname() != null ? ext.getZZ_SDF_Surname() : "");

			String sdfPdfHtml = basePdfHtml
				.replace("%UserName%", sdfName.trim())
				.replace("%sdfUserEmail%", (sdfEmail != null) ? sdfEmail.trim() : "");

			File sdfPdfFile = createPDF(sdfPdfHtml, "SDF_Extension_" + sdlNo + "_" + System.currentTimeMillis());

			if (sdfEmail != null && !sdfEmail.isBlank())
			{
				String sdfMessage = baseMessage.replace("@Name@", sdfName.trim());

				boolean sent = client.sendEMail(sdfEmail.trim(), baseSubject, sdfMessage, sdfPdfFile, true);
				if (sent)
					successCount++;
				else
					log.warning("Failed to dispatch Extension Notification to " + sdfEmail);
			}

			if (sorEmail != null && !sorEmail.isBlank() && !sorEmail.trim().equalsIgnoreCase(sdfEmail != null ? sdfEmail.trim() : ""))
			{
				String sorName = (ext.getZZ_SOR_FirstName() != null ? ext.getZZ_SOR_FirstName() : "")	+ " " +
									(ext.getZZ_SOR_Surname() != null ? ext.getZZ_SOR_Surname() : "");
				String sorMessage = baseMessage.replace("@Name@", sorName.trim());

				boolean sent = client.sendEMail(sorEmail.trim(), baseSubject, sorMessage, sdfPdfFile, true);
				if (sent)
					successCount++;
				else
					log.warning("Failed to dispatch Extension Notification to " + sorEmail);
			}
		}

		log.info("Successfully sent " + successCount + " Extension email notifications for Batch ID: " + batchId);
	}

	private File createPDF(String html, String fileName) throws Exception
	{
		File pdfFile = File.createTempFile(fileName + "_", ".pdf");
		pdfFile.deleteOnExit();

		try (var fos = new FileOutputStream(pdfFile))
		{
			Document document = new Document();
			PdfWriter.getInstance(document, fos);
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

				var elements = HTMLWorker.parseToList(new StringReader(safeHtml), null);

				boolean hasContent = false;
				if (elements != null && !elements.isEmpty())
				{
					for (var obj : elements)
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
			finally
			{
				if (document.isOpen())
				{
					document.close();
				}
			}
		}

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
			tempFile.deleteOnExit();

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
}
