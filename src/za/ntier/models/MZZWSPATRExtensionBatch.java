package za.ntier.models;

import java.sql.ResultSet;
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

		// Check if workflow node has finalized (Status = Approved)
		if (ok && is_ValueChanged(COLUMNNAME_ZZ_DocStatus) && getZZ_DocStatus() != null
				&& getZZ_DocStatus().equals("AP"))
		{
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

	/**
	 * Send email to all SDF and SOR emails associated with child extensions.
	 */
	public void sendFinalApprovalEmail() throws Exception
	{
		int batchId = get_ID();

		// 1. Fetch Mail Template
		MMailText mailText = new MMailText(getCtx(), EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID, get_TrxName());

		if (mailText.get_ID() <= 0)
		{
			log.severe("Mail template not found for Extension Batch Approval UUID: "
					+ EXTENSION_BATCH_FINAL_APPROVAL_TEMPLATE_UUID);
			return;
		}

		try
		{
			mailText.setPO(this, true); // Populate context variables
		}
		catch (Throwable t)
		{
			mailText.setPO(this);
		}

		String html = mailText.getMailText(true);
		String subject = mailText.getMailHeader();
		if (subject == null || subject.trim().isEmpty())
		{
			subject = "WSP-ATR Extension Application Update: Approval Confirmation";
		}

		// 2. Fetch all related child records from ZZ_WSP_ATR_EXTENSION
		List<X_ZZ_WSP_ATR_EXTENSION> extensions = new Query(getCtx(), X_ZZ_WSP_ATR_EXTENSION.Table_Name,
				"ZZ_WSP_ATR_EXTENSION_BATCH_ID=?", get_TrxName()).setParameters(batchId).list();

		// 3. Collect emails and remove duplicates
		Set<String> uniqueEmails = new HashSet<>();
		for (X_ZZ_WSP_ATR_EXTENSION ext : extensions)
		{
			if (ext.getZZ_SDF_EMAIL() != null && !ext.getZZ_SDF_EMAIL().trim().isEmpty())
			{
				uniqueEmails.add(ext.getZZ_SDF_EMAIL().trim().toLowerCase());
			}
			if (ext.getZZ_SOR_EMAIL() != null && !ext.getZZ_SOR_EMAIL().trim().isEmpty())
			{
				uniqueEmails.add(ext.getZZ_SOR_EMAIL().trim().toLowerCase());
			}
		}

		if (uniqueEmails.isEmpty())
		{
			log.warning("No recipient emails (SDF or SOR) found related to Extension Batch ID: " + batchId);
			return;
		}

		MClient client = MClient.get(getCtx());
		int successCount = 0;

		for (String email : uniqueEmails)
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
					log.warning("Failed to dispatch Extension Batch Notification to " + email);
				}
			}
			else
			{
				log.warning(
						"WSP Extension Batch Notification Error: No matching AD_User account found for Email " + email);
			}
		}

		log.info("Successfully send " + successCount + " Extension Batch email notifications for Batch ID: " + batchId);
	}
}
