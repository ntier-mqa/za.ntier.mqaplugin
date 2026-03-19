package za.ntier.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.Query;

import za.co.ntier.api.model.X_ZZ_WSP_ATR_EXTENSION;
import za.co.ntier.api.model.X_ZZ_WSP_ATR_EXTENSION_BATCH;

public class MZZWSPATRExtensionBatch extends X_ZZ_WSP_ATR_EXTENSION_BATCH
{

	private static final long	serialVersionUID								= 1L;

	private static final String	EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID	= "e936277c-81d9-49cf-8fcf-29096e734719";

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

	    if (ok && is_ValueChanged(COLUMNNAME_ZZ_DocStatus) && "AP".equals(getZZ_DocStatus()))
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
			log.severe("Mail template not found for Extension Batch Approval UUID: "
					+ EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID);
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

		for (X_ZZ_WSP_ATR_EXTENSION ext : extensions)
		{
			try
			{
				mailText.setPO(ext, true);
			}
			catch (Throwable t)
			{
				mailText.setPO(ext);
			}

			String html = mailText.getMailText(true);
			String subject = mailText.getMailHeader();
			if (subject == null || subject.trim().isEmpty())
			{
				subject = "WSP-ATR Extension Application Update: Approval Confirmation";
			}

			// Collect emails specific to this single extension and remove
			// duplicates
			Set<String> emailsForExt = new HashSet<>();
			if (ext.getZZ_SDF_EMAIL() != null && !ext.getZZ_SDF_EMAIL().trim().isEmpty())
			{
				emailsForExt.add(ext.getZZ_SDF_EMAIL().trim().toLowerCase());
			}
			if (ext.getZZ_SOR_EMAIL() != null && !ext.getZZ_SOR_EMAIL().trim().isEmpty())
			{
				emailsForExt.add(ext.getZZ_SOR_EMAIL().trim().toLowerCase());
			}

			if (emailsForExt.isEmpty())
			{
				log.warning("No recipient emails (SDF or SOR) found related to Extension ID: " + ext.get_ID());
				continue;
			}

			// Send the personalized email configured for this specific
			// extension
			for (String email : emailsForExt)
			{
				if (email != null && !email.isBlank())
				{
					boolean sent = client.sendEMail(email, subject, html, null, true);
					if (sent)
					{
						successCount++;
					}
					else
					{
						log.warning("Failed to dispatch Extension Notification to " + email);
					}
				}
				else
				{
					log.warning("WSP Extension Notification Error: No valid Email for extension " + ext.get_ID());
				}
			}
		}

		log.info("Successfully sent " + successCount + " Extension email notifications for Batch ID: " + batchId);
	}

}
