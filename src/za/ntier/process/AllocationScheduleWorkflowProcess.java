package za.ntier.process;

import java.text.SimpleDateFormat;
import java.util.List;

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
import org.compiere.util.DB;
import org.compiere.util.EMail;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZ_AllocationSchedule;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_QA_Configuration;

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

		// Reload header to capture newly assigned dynamic statuses natively via active transaction.
		header.load(get_TrxName());

		if (ROLE_MGR_QA.equalsIgnoreCase(roleName) && "Y".equalsIgnoreCase(pApprove))
		{
			if (X_ZZ_AllocationSchedule.ZZ_DOCSTATUS_Approved.equals(header.getZZ_DocStatus()))
			{
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

		int processId = DB.getSQLValue(	get_TrxName(),
										"SELECT AD_Process_ID FROM AD_Process WHERE Classname='za.co.ntier.wf.process.ZZ_WF_RunProcess' AND IsActive='Y'");
		if (processId <= 0)
			throw new AdempiereException("Could not find active ZZ_WF_RunProcess by its Classname in AD_Process.");

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
		MClient client = MClient.get(getCtx());

		MAttachment qaAttachment = null;
		X_ZZ_QA_Configuration qaConfig = new Query(getCtx(), X_ZZ_QA_Configuration.Table_Name, "AD_Client_ID=?", get_TrxName())
																																.setParameters(Env.getAD_Client_ID(getCtx()))
																																.firstOnly();

		List<MAttachmentEntry> cachedAttachments = new java.util.ArrayList<>();
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

		List<EMail> pendingEmails = new java.util.ArrayList<>();
		List<Integer> pendingAllocIds = new java.util.ArrayList<>();
		List<String> pendingTargetEmails = new java.util.ArrayList<>();

		List<X_ZZ_Allocations> lines = new Query(	getCtx(), X_ZZ_Allocations.Table_Name,
													"ZZ_Organization_ID IN (SELECT ZZ_Organization_ID FROM ZZ_Organization WHERE ZZ_AllocationSchedule_ID=?)",
													get_TrxName()).setParameters(header.get_ID()).setOnlyActiveRecords(true).list();

		for (X_ZZ_Allocations line : lines)
		{

			// 1. Email Send Hook Mechanism
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

							if (startDate.equals(endDate))
							{
								dateStr = startDate;
							}
							else
							{
								dateStr = startDate + " to " + endDate;
							}

							if (startTime.equals(endTime))
							{
								timeStr = startTime;
							}
							else
							{
								timeStr = startTime + " to " + endTime;
							}
						}
						else
						{
							dateStr = startDate;
							timeStr = startTime;
						}
					}

					msg = msg.replace("<Dates>", dateStr);
					msg = msg.replace("<Time>", timeStr);

					EMail emailObj = client.createEMail(email, subject, msg, mailText.isHtml());
					if (emailObj != null)
					{
						for (MAttachmentEntry entry : cachedAttachments)
						{
							emailObj.addAttachment(entry.getData(), entry.getContentType(), entry.getName());
						}

						pendingEmails.add(emailObj);
						pendingAllocIds.add(line.get_ID());
						pendingTargetEmails.add(email);
					}
				}
			}

		}

		if (!pendingEmails.isEmpty())
		{
			final int currentUserId = Env.getAD_User_ID(getCtx());
			final java.util.Properties ctx = Env.getCtx();

			java.util.concurrent.CompletableFuture.runAsync(() -> {
				for (int i = 0; i < pendingEmails.size(); i++)
				{
					EMail emailObj = pendingEmails.get(i);
					int allocId = pendingAllocIds.get(i);
					String targetEmail = pendingTargetEmails.get(i);

					String sendResult = emailObj.send();
					String asyncTrx = Trx.createTrxName("EmailNotifyAsync");
					try
					{
						if (EMail.SENT_OK.equals(sendResult))
						{
							X_ZZ_Allocations alloc = new X_ZZ_Allocations(ctx, allocId, asyncTrx);
							alloc.setZZ_DocStatus("ST");
							alloc.saveEx();
						}
						else
						{
							org.compiere.model.MNote note = new org.compiere.model.MNote(ctx, 0, currentUserId, asyncTrx);
							note.setTextMsg("Failed to dispatch Email to " + targetEmail + ". Error: " + sendResult);
							note.setRecord(X_ZZ_Allocations.Table_ID, allocId);
							note.saveEx();
						}
						Trx.get(asyncTrx, false).commit();
					}
					catch (Exception e)
					{
						// Exception safely ignored in async background thread.
					}
					finally
					{
						Trx.get(asyncTrx, false).close();
					}
				}
			});
		}
	}
}
