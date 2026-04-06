package za.ntier.process;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZ_AllocationSchedule;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_QA_Configuration;
import za.ntier.utils.MQAConstants;

@Process(name = "za.ntier.process.AllocationScheduleWorkflowProcess")
public class AllocationScheduleWorkflowProcess extends SvrProcess
{

	private static final String	ROLE_MGR_QA					= "Mgr - QA";

	/** Mail template UU for Mgr QA Approval emails to organizations */
	private static final String	APPLICANT_MAIL_TEMPLATE_UU	= "61aeebae-fc16-4db8-823b-c038e0413d3f";

	@Parameter(name = "Approve")
	private String				pApprove					= "";

	@Parameter(name = "Recommend")
	private String				pRecommend					= "";

	private String				roleName					= null;

	@Override
	protected void prepare()
	{
		roleName = Env.getContext(getCtx(), "#AD_Role_Name");
	}

	@Override
	protected String doIt() throws Exception
	{
		int recordId = getRecord_ID();
		if (recordId <= 0)
		{
			throw new AdempiereException("Process must be executed from a Record.");
		}

		X_ZZ_AllocationSchedule header = new X_ZZ_AllocationSchedule(getCtx(), recordId, get_TrxName());
		return processWorkflowAction(header);
	}

	private String processWorkflowAction(X_ZZ_AllocationSchedule header) throws Exception
	{
		processDocAction(header);

		header.load(get_TrxName());

		if (ROLE_MGR_QA.equalsIgnoreCase(roleName) && "Y".equalsIgnoreCase(pApprove))
		{
			if (X_ZZ_AllocationSchedule.ZZ_DOCSTATUS_Approved.equals(header.getZZ_DocStatus()))
			{
				// IMPORTANT: Commit the current transaction to release DB locks
				// so background threads can update records immediately.
				Trx.get(get_TrxName(), false).commit();

				processChildRecordsAndEmails(header);
			}
		}

		return "@Success@";
	}

	private void processDocAction(X_ZZ_AllocationSchedule header)
	{
		String approveVal = (pApprove != null) ? pApprove.trim() : "";
		String recommendVal = (pRecommend != null) ? pRecommend.trim() : "";

		header.saveEx();

		int processId = MQAConstants.getWFRunProcessId(get_TrxName());

		MProcess proc = new MProcess(getCtx(), processId, get_TrxName());
		ProcessInfo pi = new ProcessInfo(proc.getName(), processId, header.get_Table_ID(), header.get_ID());
		pi.setClassName(proc.getClassname());

		String uu = header.get_ValueAsString("UU");
		if (Util.isEmpty(uu))
			uu = header.get_ValueAsString("ZZ_AllocationSchedule_UU");
		MPInstance instance = new MPInstance(getCtx(), processId, header.get_Table_ID(), header.get_ID(), uu);
		instance.saveEx();
		pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

		pi.setParameter(new ProcessInfoParameter[] {
														new ProcessInfoParameter("Approve", approveVal, "", "", ""),
															new ProcessInfoParameter("Recommend", recommendVal, "", "", "") });

		pi.setAD_User_ID(Env.getAD_User_ID(getCtx()));
		pi.setAD_Client_ID(Env.getAD_Client_ID(getCtx()));

		Trx processTrx = Trx.get(get_TrxName(), false);
		if (!ProcessUtil.startJavaProcess(getCtx(), pi, processTrx, false, getProcessInfo().getProcessUI()))
			throw new AdempiereException("Workflow execution failed.");
	}

	private void processChildRecordsAndEmails(X_ZZ_AllocationSchedule header)
	{
		MMailText mailText = null;

		if (!Util.isEmpty(APPLICANT_MAIL_TEMPLATE_UU))
		{
			mailText = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																									.setParameters(APPLICANT_MAIL_TEMPLATE_UU).firstOnly();
		}
		else
		{
			log.warning("APPLICANT_MAIL_TEMPLATE_UU is missing - applicant emails will not be constructed.");
		}

		String baseBody = mailText != null ? mailText.getMailText(true) : null;
		String subject = mailText != null ? mailText.getMailHeader() : null;
		boolean isHtml = mailText != null && mailText.isHtml();
		MClient client = MClient.get(getCtx());

		MAttachment qaAttachment = null;
		X_ZZ_QA_Configuration qaConfig = new Query(getCtx(), X_ZZ_QA_Configuration.Table_Name, "AD_Client_ID=?", get_TrxName())
																																.setParameters(Env.getAD_Client_ID(getCtx()))
																																.firstOnly();

		List<MAttachmentEntry> cachedAttachments = new ArrayList<>();
		if (qaConfig != null && qaConfig.get_ID() > 0)
		{
			qaAttachment = qaConfig.getAttachment();
			if (qaAttachment != null && qaAttachment.getEntries() != null)
			{
				for (MAttachmentEntry entry : qaAttachment.getEntries())
				{
					cachedAttachments.add(entry);
				}
			}
		}

		List<EMail> pendingEmails = new ArrayList<>();
		List<Integer> pendingAllocIds = new ArrayList<>();
		List<String> pendingTargetEmails = new ArrayList<>();
		List<String> pendingOrgNames = new ArrayList<>();

		List<X_ZZ_Allocations> lines = new Query(	getCtx(), X_ZZ_Allocations.Table_Name,
													"ZZ_Organization_ID IN (SELECT ZZ_Organization_ID FROM ZZ_Organization WHERE ZZ_AllocationSchedule_ID=?)",
													get_TrxName()).setParameters(header.get_ID()).setOnlyActiveRecords(true).list();

		for (X_ZZ_Allocations line : lines)
		{
			if (mailText != null)
			{
				String email = line.getEMail();
				if (!Util.isEmpty(email))
				{
					String msg = baseBody;

					String appTypeLine = "";
					if (line.get_ValueAsInt("ZZ_QCTO_Alloc_OC_ID") > 0)
					{
						appTypeLine = "Occupational Qualification- " + line.getZZ_Qualification();
					}
					else if (line.get_ValueAsInt("ZZ_QCTO_Alloc_Skills_ID") > 0)
					{
						appTypeLine = "Skills Occupational Programme- " + line.getZZ_Qualification();
					}
					else if (line.get_ValueAsInt("ZZ_QCTO_Alloc_AC_ID") > 0)
					{
						appTypeLine = "Accreditation Center- " + line.getZZ_Qualification();
					}

					X_ZZ_Organization orgTab = new X_ZZ_Organization(getCtx(), line.getZZ_Organization_ID(), get_TrxName());

					String orgName = orgTab.getZZLegalName() != null ? orgTab.getZZLegalName() : "";
					msg = msg.replace("<OrgName>", orgName);
					msg = msg.replace("<Scope>", appTypeLine);

					String dateStr = "TBD";
					String timeStr = "TBD";

					if (orgTab.getDateFrom() != null)
					{
						SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMMM yyyy");
						SimpleDateFormat timeFmt = new SimpleDateFormat("hh'h'mma");

						String startDate = dateFmt.format(orgTab.getDateFrom());
						String startTime = timeFmt.format(orgTab.getDateFrom()).toLowerCase();

						if (orgTab.getDateTo() != null)
						{
							String endDate = dateFmt.format(orgTab.getDateTo());
							String endTime = timeFmt.format(orgTab.getDateTo()).toLowerCase();

							dateStr = startDate.equals(endDate) ? startDate : startDate + " to " + endDate;
							timeStr = startTime.equals(endTime) ? startTime : startTime + " to " + endTime;
						}
						else
						{
							dateStr = startDate;
							timeStr = startTime;
						}
					}

					msg = msg.replace("<Dates>", dateStr);
					msg = msg.replace("<Time>", timeStr);

					EMail emailObj = client.createEMail(email, subject, msg, isHtml);
					if (emailObj != null)
					{
						for (MAttachmentEntry entry : cachedAttachments)
						{
							emailObj.addAttachment(entry.getData(), entry.getContentType(), entry.getName());
						}

						pendingEmails.add(emailObj);
						pendingAllocIds.add(line.get_ID());
						pendingTargetEmails.add(email);
						pendingOrgNames.add(orgName);
					}
				}
			}
		}

		if (pendingEmails.isEmpty())
			return;

		final java.util.Properties ctx = Env.getCtx();
		final int totalCount = pendingEmails.size();
		final AtomicInteger sentCount = new AtomicInteger(0);
		final AtomicInteger failedCount = new AtomicInteger(0);
		final AtomicInteger completedCount = new AtomicInteger(0);
		final List<String> failedAllocNos = Collections.synchronizedList(new ArrayList<>());

		// Thread pool for parallel email dispatch
		ExecutorService pool = Executors.newFixedThreadPool(15);
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (int i = 0; i < totalCount; i++)
		{
			final EMail emailObj = pendingEmails.get(i);
			final int allocId = pendingAllocIds.get(i);
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
						{
							addLog(0, null, null, "RE-ATTEMPT (" + attempt + "/" + maxRetries + ") for " + orgName + " [ Target: " + targetEmail + " ]");
						}

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
						String trxName = Trx.createTrxName("EmailBulk");
						try
						{
							X_ZZ_Allocations alloc = new X_ZZ_Allocations(ctx, allocId, trxName);
							alloc.setZZ_DocStatus("ST"); // Audit Notification Sent
							alloc.saveEx();
							Trx.get(trxName, false).commit();
							sentCount.incrementAndGet();
						}
						catch (Exception e)
						{
							log.log(Level.WARNING, "Failed to update status for allocId=" + allocId, e);
							failedCount.incrementAndGet();
						}
						finally
						{
							Trx tr = Trx.get(trxName, false);
							if (tr != null)
								tr.close();
						}
					}
					else
					{
						X_ZZ_Allocations alloc = new X_ZZ_Allocations(ctx, allocId, null);
						failedAllocNos.add(String.format(	"%s - %s [ Email: %s ] (%s)",
															orgName,
															alloc.getZZ_AllocationNo(),
															(Util.isEmpty(targetEmail) ? "MISSING" : targetEmail),
															sendResult));
						failedCount.incrementAndGet();
					}
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Internal email dispatch error for allocId=" + allocId, e);
					failedCount.incrementAndGet();
				}
				finally
				{
					completedCount.incrementAndGet();
				}
			}, pool));
		}

		// Main thread
		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		while (!allDone.isDone())
		{
			int done = completedCount.get();
			int pct = (totalCount > 0) ? (done * 100) / totalCount : 100;
			statusUpdate(String.format(	"Sending emails: %d/%d (%d%%) — Sent: %d, Failed: %d",
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

		statusUpdate(String.format(	"Email dispatch complete: %d sent, %d failed out of %d",
									sentCount.get(), failedCount.get(), totalCount));

		for (String failure : failedAllocNos)
		{
			addLog(0, null, null, "FAILED: " + failure);
		}
	}
}
