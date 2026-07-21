package za.ntier.process;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZ_AllocationSchedule;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_QA_Configuration;

@Process(name = "za.ntier.process.AuditConfirmationReminderProcess")
public class AuditConfirmationReminderProcess extends SvrProcess
{
	private static final String	MAIL_TEMPLATE_UU	= "61aeebae-fc16-4db8-823b-c038e0413d3f";
	private static final String	MAIL_SUBJECT		= "Audit Notification - Follow up";
	private static final int	DEFAULT_DAYS		= 4;

	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{
		int daysAfterApproval = DEFAULT_DAYS;

		X_ZZ_QA_Configuration qaConfig = new Query(	getCtx(), X_ZZ_QA_Configuration.Table_Name,
													X_ZZ_QA_Configuration.COLUMNNAME_AD_Client_ID + "=?", get_TrxName())
																														.setParameters(Env.getAD_Client_ID(getCtx()))
																														.setOnlyActiveRecords(true)
																														.firstOnly();

		if (qaConfig != null)
		{
			int configured = qaConfig.getZZ_DaysUntilAuditConfirmReminder();
			if (configured > 0)
				daysAfterApproval = configured;
		}

		// Target window: schedules approved exactly N days ago
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, -daysAfterApproval);
		Timestamp targetStart = new Timestamp(cal.getTimeInMillis());

		cal.add(Calendar.DAY_OF_MONTH, 1);
		Timestamp targetEnd = new Timestamp(cal.getTimeInMillis());

		MMailText mailText = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																										.setParameters(MAIL_TEMPLATE_UU)
																										.firstOnly();

		if (mailText == null)
		{
			log.warning("Mail Template not found: " + MAIL_TEMPLATE_UU);
			return "Error: Mail Template not found.";
		}

		MClient client = MClient.get(getCtx());

		// Load attachments from QA Configuration
		List<MAttachmentEntry> cachedAttachments = new ArrayList<>();
		if (qaConfig != null && qaConfig.get_ID() > 0)
		{
			MAttachment qaAttachment = qaConfig.getAttachment();
			if (qaAttachment != null && qaAttachment.getEntries() != null)
			{
				for (MAttachmentEntry entry : qaAttachment.getEntries())
					cachedAttachments.add(entry);
			}
		}

		List<X_ZZ_AllocationSchedule> schedules = new Query(getCtx(), X_ZZ_AllocationSchedule.Table_Name,
															X_ZZ_AllocationSchedule.COLUMNNAME_ZZ_DocStatus + "=? AND "
																											+ X_ZZ_AllocationSchedule.COLUMNNAME_ZZ_ApprovedDate
																											+ " >= ? AND "
																											+ X_ZZ_AllocationSchedule.COLUMNNAME_ZZ_ApprovedDate
																											+ " < ?",
															get_TrxName())
																			.setParameters(	X_ZZ_AllocationSchedule.ZZ_DOCSTATUS_Approved, targetStart,
																							targetEnd)
																			.setOnlyActiveRecords(true)
																			.list();

		if (schedules.isEmpty())
			return "No Approved Schedules with ApprovedDate " + daysAfterApproval + " day(s) ago. Nothing to notify.";

		String baseBody = mailText.getMailText(true);
		boolean isHtml = mailText.isHtml();

		List<EMail> pendingEmails = new ArrayList<>();
		List<String> pendingTargetEmails = new ArrayList<>();
		List<String> pendingOrgNames = new ArrayList<>();
		int orgsSkipped = 0;

		for (X_ZZ_AllocationSchedule schedule : schedules)
		{
			List<X_ZZ_Organization> pendingOrgs = new Query(getCtx(), X_ZZ_Organization.Table_Name,
															X_ZZ_Organization.COLUMNNAME_ZZ_AllocationSchedule_ID	+ "=? AND "
																									+ X_ZZ_Organization.COLUMNNAME_ZZ_AuditConfirmation
																									+ " IS NULL",
															get_TrxName())
																			.setParameters(schedule.get_ID())
																			.setOnlyActiveRecords(true)
																			.list();

			for (X_ZZ_Organization org : pendingOrgs)
			{
				List<X_ZZ_Allocations> allocLines = new Query(	getCtx(), X_ZZ_Allocations.Table_Name,
																X_ZZ_Allocations.COLUMNNAME_ZZ_Organization_ID + "=?", get_TrxName())
																																		.setParameters(org.get_ID())
																																		.setOnlyActiveRecords(true)
																																		.list();

				if (allocLines.isEmpty())
				{
					orgsSkipped++;
					addLog(org.get_ID(), null, null, "Skipped (no allocation lines): " + org.getZZLegalName());
					continue;
				}

				Set<String> emailsSeen = new HashSet<>();
				Set<String> allocNosSeen = new LinkedHashSet<>();
				for (X_ZZ_Allocations line : allocLines)
				{
					String email = line.getEMail();
					if (!Util.isEmpty(email))
						emailsSeen.add(email.trim().toLowerCase());

					String allocNo = line.getZZ_AllocationNo();
					if (!Util.isEmpty(allocNo))
						allocNosSeen.add(allocNo.trim());
				}

				if (emailsSeen.isEmpty())
				{
					orgsSkipped++;
					addLog(org.get_ID(), null, null, "Skipped (no email on allocations): " + org.getZZLegalName());
					continue;
				}

				String orgName = org.getZZLegalName() != null ? org.getZZLegalName() : "";

				String auditDates = "TBD";
				if (org.getDateFrom() != null)
				{
					SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMMM yyyy");
					String startDate = dateFmt.format(org.getDateFrom());
					if (org.getDateTo() != null)
					{
						String endDate = dateFmt.format(org.getDateTo());
						auditDates = startDate.equals(endDate) ? startDate : startDate + " to " + endDate;
					}
					else
					{
						auditDates = startDate;
					}
				}

				String scope = buildScope(allocLines, isHtml);

				String msg = baseBody;
				if (msg != null)
				{
					msg = msg	.replace("<OrgName>", orgName)
								.replace("<Dates>", auditDates)
								.replace("<Scope>", scope);
				}

				for (String email : emailsSeen)
				{
					EMail emailObj = client.createEMail(email, MAIL_SUBJECT, msg, isHtml);
					if (emailObj != null)
					{
						for (MAttachmentEntry entry : cachedAttachments)
							emailObj.addAttachment(entry.getData(), entry.getContentType(), entry.getName());

						pendingEmails.add(emailObj);
						pendingTargetEmails.add(email);
						pendingOrgNames.add(orgName);
					}
				}
			}
		}

		if (pendingEmails.isEmpty())
			return "@Success@ No pending reminder emails to dispatch.";

		final int totalCount = pendingEmails.size();
		final AtomicInteger sentCount = new AtomicInteger(0);
		final AtomicInteger failedCount = new AtomicInteger(0);
		final AtomicInteger completedCount = new AtomicInteger(0);
		final List<String> failedEntries = Collections.synchronizedList(new ArrayList<>());

		ExecutorService pool = Executors.newFixedThreadPool(15);
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i = 0; i < totalCount; i++)
		{
			final EMail emailObj = pendingEmails.get(i);
			final String targetEmail = pendingTargetEmails.get(i);
			final String orgName = pendingOrgNames.get(i);

			futures.add(CompletableFuture.runAsync(() -> {
				try
				{
					int maxRetries = 3;
					long retryDelayMs = 2000;
					String sendResult = null;
					boolean sent = false;

					for (int attempt = 1; attempt <= maxRetries; attempt++)
					{
						if (attempt > 1)
							addLog(0, null, null, "RE-ATTEMPT (" + attempt + "/" + maxRetries + ") for " + orgName + " [ " + targetEmail + " ]");

						sendResult = emailObj.send();
						if (EMail.SENT_OK.equals(sendResult))
						{
							sent = true;
							break;
						}

						if (attempt < maxRetries)
						{
							try
							{
								Thread.sleep(retryDelayMs);
							}
							catch (InterruptedException e)
							{
								Thread.currentThread().interrupt();
								break;
							}
						}
					}

					if (sent)
					{
						sentCount.incrementAndGet();
					}
					else
					{
						failedEntries.add(orgName + " [ " + targetEmail + " ] (" + sendResult + ")");
						failedCount.incrementAndGet();
					}
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Email dispatch error for org=" + orgName, e);
					failedCount.incrementAndGet();
				}
				finally
				{
					completedCount.incrementAndGet();
				}
			}, pool));
		}

		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		while (!allDone.isDone())
		{
			int done = completedCount.get();
			int pct = (totalCount > 0) ? (done * 100) / totalCount : 100;
			statusUpdate(String.format(	"Sending reminders: %d/%d (%d%%) — Sent: %d, Failed: %d",
										done, totalCount, pct, sentCount.get(), failedCount.get()));
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException ignored)
			{
				break;
			}
		}

		pool.shutdown();
		try
		{
			if (!pool.awaitTermination(30, TimeUnit.SECONDS))
				pool.shutdownNow();
		}
		catch (InterruptedException e)
		{
			pool.shutdownNow();
		}

		statusUpdate(String.format(	"Reminder dispatch complete: %d sent, %d failed out of %d",
									sentCount.get(), failedCount.get(), totalCount));

		for (String failure : failedEntries)
			addLog(0, null, null, "FAILED: " + failure);

		return "@Success@	"	+ sentCount.get() + " reminder(s) sent, " + failedCount.get() + " email(s) failed, " + orgsSkipped
				+ " org(s) skipped (no email).";
	}

	private String buildScope(List<X_ZZ_Allocations> allocLines, boolean isHtml)
	{
		List<String> ocQuals = new ArrayList<>();
		List<String> skillsQuals = new ArrayList<>();
		List<String> acQuals = new ArrayList<>();
		List<String> tradesQuals = new ArrayList<>();
		List<String> ttcQuals = new ArrayList<>();

		for (X_ZZ_Allocations line : allocLines)
		{
			if (line.getZZ_QCTO_Alloc_OC_ID() > 0 && !Util.isEmpty(line.getZZ_Qualification()))
				ocQuals.add(line.getZZ_Qualification());
			else if (line.getZZ_QCTO_Alloc_Skills_ID() > 0 && !Util.isEmpty(line.getZZ_Qualification()))
				skillsQuals.add(line.getZZ_Qualification());
			else if (line.getZZ_QCTO_Alloc_AC_ID() > 0 && !Util.isEmpty(line.getZZ_Qualification()))
				acQuals.add(line.getZZ_Qualification());
			else if (line.getZZ_NAMB_Alloc_Trades_ID() > 0 && !Util.isEmpty(line.getZZ_Qualification()))
				tradesQuals.add(line.getZZ_Qualification());
			else if (line.getZZ_NAMB_Alloc_TTC_ID() > 0 && !Util.isEmpty(line.getZZ_Qualification()))
				ttcQuals.add(line.getZZ_Qualification());
		}

		StringBuilder sb = new StringBuilder();

		if (isHtml)
		{
			if (!ocQuals.isEmpty())
			{
				sb.append("&emsp;<b>Occupational Qualification</b><br/>");
				for (int j = 0; j < ocQuals.size(); j++)
					sb.append("&emsp;&emsp;").append(j + 1).append(". ").append(ocQuals.get(j)).append("<br/>");
			}
			if (!skillsQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("<br/>");
				sb.append("&emsp;<b>Skills Occupational Programme</b><br/>");
				for (int j = 0; j < skillsQuals.size(); j++)
					sb.append("&emsp;&emsp;").append(j + 1).append(". ").append(skillsQuals.get(j)).append("<br/>");
			}
			if (!acQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("<br/>");
				sb.append("&emsp;<b>Accreditation Center</b><br/>");
				for (int j = 0; j < acQuals.size(); j++)
					sb.append("&emsp;&emsp;").append(j + 1).append(". ").append(acQuals.get(j)).append("<br/>");
			}
			if (!tradesQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("<br/>");
				sb.append("&emsp;<b>Trades</b><br/>");
				for (int j = 0; j < tradesQuals.size(); j++)
					sb.append("&emsp;&emsp;").append(j + 1).append(". ").append(tradesQuals.get(j)).append("<br/>");
			}
			if (!ttcQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("<br/>");
				sb.append("&emsp;<b>Trade Test Center</b><br/>");
				for (int j = 0; j < ttcQuals.size(); j++)
					sb.append("&emsp;&emsp;").append(j + 1).append(". ").append(ttcQuals.get(j)).append("<br/>");
			}
		}
		else
		{
			if (!ocQuals.isEmpty())
			{
				sb.append("\tOccupational Qualification\n");
				for (int j = 0; j < ocQuals.size(); j++)
					sb.append("\t\t").append(j + 1).append(". ").append(ocQuals.get(j)).append("\n");
			}
			if (!skillsQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("\tSkills Occupational Programme\n");
				for (int j = 0; j < skillsQuals.size(); j++)
					sb.append("\t\t").append(j + 1).append(". ").append(skillsQuals.get(j)).append("\n");
			}
			if (!acQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("\tAccreditation Center\n");
				for (int j = 0; j < acQuals.size(); j++)
					sb.append("\t\t").append(j + 1).append(". ").append(acQuals.get(j)).append("\n");
			}
			if (!tradesQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("\tTrades\n");
				for (int j = 0; j < tradesQuals.size(); j++)
					sb.append("\t\t").append(j + 1).append(". ").append(tradesQuals.get(j)).append("\n");
			}
			if (!ttcQuals.isEmpty())
			{
				if (sb.length() > 0)
					sb.append("\n");
				sb.append("\tTrade Test Center\n");
				for (int j = 0; j < ttcQuals.size(); j++)
					sb.append("\t\t").append(j + 1).append(". ").append(ttcQuals.get(j)).append("\n");
			}
		}

		return sb.toString();
	}
}
