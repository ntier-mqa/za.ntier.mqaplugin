package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZ_AccreditationAuditNotify;
import za.co.ntier.api.model.X_ZZ_AccreditationAuditNotifyLine;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_AC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_OC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_Skills;
import za.ntier.utils.MQAConstants;

/**
 * Flow 1 (Info Window): Creates a single Batch/Header and its lines based on
 * selection.
 * Flow 2 (Document): Triggers ZZ_WF_RunProcess, then generates
 * applicant emails on Snr Mgr Approval.
 */
@Process(name = "za.ntier.process.AccreditationAuditNotificationsProcess")
public class AccreditationAuditNotificationsProcess extends SvrProcess
{
	private static final String	ROLE_MGR_QA					= "Mgr - QA";
	private static final String	ROLE_SNR_MGR_QA				= "Snr Mgr - QA";

	/** Mail template UU for Snr Mgr Approval emails to applicants */
	private static final String	APPLICANT_MAIL_TEMPLATE_UU	= "51aeebae-fc16-4db8-823b-c038e0413d2f";

	@Parameter(name = "Approve")
	private String				pApprove					= "";

	@Parameter(name = "Recommend")
	private String				pRecommend					= "";

	private String				roleName					= null;

	@Override
	protected void prepare()
	{
		int roleId = Env.getAD_Role_ID(getCtx());
		if (roleId > 0)
		{
			roleName = DB.getSQLValueString(get_TrxName(), "SELECT Name FROM AD_Role WHERE AD_Role_ID=?", roleId);
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();

		if (pInstanceId > 0 && isFromInfoWindow(pInstanceId))
		{
			return createBatchFromInfoWindow(pInstanceId);
		}

		int recordId = getRecord_ID();
		if (recordId > 0)
		{
			X_ZZ_AccreditationAuditNotify header = new X_ZZ_AccreditationAuditNotify(getCtx(), recordId, get_TrxName());
			return processWorkflowAction(header);
		}

		return "Process must be executed from an Info Window selection or a Record.";
	}

	/**
	 * Checks if the process was triggered from an Info Window by verifying
	 * if the Process Instance ID exists in the selection tables.
	 *
	 * @param  pInstanceId The AD_PInstance_ID parameter context
	 * @return             true if records were selected via Info Window
	 */
	private boolean isFromInfoWindow(int pInstanceId)
	{
		int count = DB.getSQLValue(	get_TrxName(), "SELECT COUNT(*) FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?",
									pInstanceId);
		if (count > 0)
			return true;
		count = DB.getSQLValue(get_TrxName(), "SELECT COUNT(*) FROM T_Selection WHERE AD_PInstance_ID=?", pInstanceId);
		return count > 0;
	}

	/**
	 * Consolidates selected records from the QCTO App Info Window into a new
	 * parent ZZ_AccreditationAuditNotify batch document and its child lines.
	 *
	 * @param  pInstanceId The AD_PInstance_ID containing the T_Selection data
	 * @return             A success string indicating the document was created
	 * @throws Exception
	 */
	private String createBatchFromInfoWindow(int pInstanceId) throws Exception
	{
		List<PO> sourceRecords = getSelectedSourceRecords(pInstanceId);
		if (sourceRecords.isEmpty())
			return "No records were selected in the Info Window.";

		X_ZZ_AccreditationAuditNotify header = createHeader();
		int lineNo = 0;
		for (PO source : sourceRecords)
		{
			lineNo += 10;
			createLine(header, source, lineNo);
		}

		addLog(	header.getZZ_AccreditationAuditNotify_ID(), null, null,
				"Created document " + header.getDocumentNo() + " with " + sourceRecords.size() + " line(s).",
				X_ZZ_AccreditationAuditNotify.Table_ID, header.getZZ_AccreditationAuditNotify_ID());

		return "@Success@ - Batch Created successfully";
	}

	/**
	 * Handles a DocAction triggered directly on a specific batch record.
	 * Passes parameters to the Workflow Engine and optionally triggers
	 * applicant emails if the Snr Mgr approves the document.
	 *
	 * @param  header    The Batch document record
	 * @return           A success string
	 * @throws Exception
	 */
	private String processWorkflowAction(X_ZZ_AccreditationAuditNotify header) throws Exception
	{
		processDocAction(header);

		// Reload header to get updated status
		header.load(get_TrxName());

		if (ROLE_SNR_MGR_QA.equalsIgnoreCase(roleName) && "Y".equalsIgnoreCase(pApprove))
		{
			if (X_ZZ_AccreditationAuditNotify.ZZ_DOCSTATUS_Approved.equals(header.getZZ_DocStatus()))
			{
				sendApplicantApprovalMails(header);
			}
		}

		return "@Success@ - Workflow processed.";
	}

	/**
	 * Binds the current Approve/Recommend parameters and programmatically
	 * executes the standard ZZ_WF_RunProcess document workflow engine.
	 *
	 * @param header The Batch document record
	 */
	private void processDocAction(X_ZZ_AccreditationAuditNotify header)
	{
		String approveVal = "";
		String recommendVal = "";

		if (ROLE_SNR_MGR_QA.equalsIgnoreCase(roleName))
			approveVal = (pApprove != null) ? pApprove.trim() : "";
		else if (ROLE_MGR_QA.equalsIgnoreCase(roleName))
			recommendVal = (pRecommend != null) ? pRecommend.trim() : "";

		header.saveEx();

		int processId = MQAConstants.getWFRunProcessId(get_TrxName());

		MProcess proc = new MProcess(getCtx(), processId, get_TrxName());
		ProcessInfo pi = new ProcessInfo(proc.getName(), processId, header.get_Table_ID(), header.get_ID());
		pi.setClassName(proc.getClassname());

		String uu = header.get_ValueAsString("UU");
		if (Util.isEmpty(uu))
			uu = header.get_ValueAsString("ZZ_AccreditationAuditNotify_UU");
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

	/**
	 * Iterates through all lines of the batch and dynamically constructs and sends
	 * an applicant approval email using the configured R_MailText template.
	 * Dynamically supports Occupational Qualifications, Skills Programmes, and Accreditation
	 * Centers.
	 *
	 * @param header The Batch document record
	 */
	private void sendApplicantApprovalMails(X_ZZ_AccreditationAuditNotify header)
	{
		if (Util.isEmpty(APPLICANT_MAIL_TEMPLATE_UU))
		{
			log.warning("APPLICANT_MAIL_TEMPLATE_UU is missing - skipping applicant emails.");
			return;
		}

		MMailText mailText = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																										.setParameters(APPLICANT_MAIL_TEMPLATE_UU).firstOnly();
		if (mailText == null)
			return;

		String baseBody = mailText.getMailText(true);
		String subject = mailText.getMailHeader();
		MClient client = MClient.get(getCtx());

		List<X_ZZ_AccreditationAuditNotifyLine> lines = new Query(	getCtx(),
																	X_ZZ_AccreditationAuditNotifyLine.Table_Name, "ZZ_AccreditationAuditNotify_ID=?",
																	get_TrxName())
																					.setParameters(header.get_ID()).setOnlyActiveRecords(true).list();

		for (X_ZZ_AccreditationAuditNotifyLine line : lines)
		{
			String email = line.getEMail();
			if (Util.isEmpty(email))
				continue;

			String appTypeLine = "";
			if (line.get_ValueAsInt("ZZ_QCTO_Alloc_OC_ID") > 0)
			{
				appTypeLine = "Occupational Qualification: "	+ line.getZZ_SAQAIDOrSPID() + " - "
								+ line.getZZ_Qualification();
			}
			else if (line.get_ValueAsInt("ZZ_QCTO_Alloc_Skills_ID") > 0)
			{
				appTypeLine = "Skills Occupational Programme: " + line.getZZ_SAQAIDOrSPID() + " - "
								+ line.getZZ_Qualification();
			}
			else if (line.get_ValueAsInt("ZZ_QCTO_Alloc_AC_ID") > 0)
			{
				appTypeLine = "Accreditation Center: " + line.getZZ_SAQAIDOrSPID() + " - " + line.getZZ_Qualification();
			}

			String msg = baseBody;
			msg = msg.replace("<Name>", (line.getName() != null) ? line.getName() : "");

			String startDateStr = "TBD";
			if (header.getDateFrom() != null)
				startDateStr = new SimpleDateFormat("yyyy-MM-dd").format(header.getDateFrom());

			String endDateStr = "TBD";
			if (header.getDateTo() != null)
				endDateStr = new SimpleDateFormat("yyyy-MM-dd").format(header.getDateTo());

			msg = msg.replace("<Start Date>", startDateStr);
			msg = msg.replace("<End Date>", endDateStr);
			msg = msg.replace("@Name@", (line.getName() != null) ? line.getName() : "");

			String qualHtml = "<br/><p>" + appTypeLine + "</p><br/>";
			if (msg.contains("Regards"))
			{
				msg = msg.replace("Regards", qualHtml + "Regards");
			}
			else
			{
				msg += qualHtml;
			}

			if (!client.sendEMail(email, subject, msg, null, mailText.isHtml()))
			{
				log.warning("Failed to send applicant email to: " + email);
			}
			else
			{
				addLog("Email sent to applicant: " + email);
			}
		}
	}

	/**
	 * Reads selected IDs from T_Selection and T_Selection_InfoWindow, then
	 * dynamically resolves them into their actual PO representations
	 * (Occupational Certificate, Skills Programme, or Accreditation Center).
	 *
	 * @param  pInstanceId The AD_PInstance_ID
	 * @return             A list of populated Persistent Objects
	 */
	private List<PO> getSelectedSourceRecords(int pInstanceId)
	{
		List<PO> results = new ArrayList<>();
		Set<String> seen = new HashSet<>();

		String sqlSelect = "SELECT DISTINCT T_Selection_ID, T_Selection_UU FROM ("
							+ "  SELECT T_Selection_ID, T_Selection_UU FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?"
							+ "  UNION" + "  SELECT T_Selection_ID, NULL FROM T_Selection WHERE AD_PInstance_ID=?" + ") x";

		try (PreparedStatement pstmt = DB.prepareStatement(sqlSelect, get_TrxName()))
		{
			pstmt.setInt(1, pInstanceId);
			pstmt.setInt(2, pInstanceId);
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					int idKey = rs.getInt(1);
					String uuKey = rs.getString(2);
					resolveViewRow(idKey, uuKey, results, seen);
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error reading info window selection", e);
		}

		return results;
	}

	/**
	 * Looks up the active database view based on ID or UUID to locate the true
	 * underlying Record ID and the tab it came from, instantiating the correct PO.
	 *
	 * @param idKey   The View's Record ID
	 * @param uuKey   The View's UUID
	 * @param results The list to append the resolved PO to
	 * @param seen    A Set used to prevent processing duplicates
	 */
	private void resolveViewRow(int idKey, String uuKey, List<PO> results, Set<String> seen)
	{
		String viewSql = "SELECT zz_tab_name, zz_qcto_application_info_v_id FROM zz_qcto_application_info_v WHERE ";

		try (PreparedStatement pstmt = DB.prepareStatement(
															viewSql + (uuKey != null ? "zz_qcto_application_info_v_uu=?" : "zz_qcto_application_info_v_id=?"),
															get_TrxName()))
		{
			if (uuKey != null)
				pstmt.setString(1, uuKey);
			else
				pstmt.setInt(1, idKey);

			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String tabName = rs.getString(1);
					int trueRecordId = rs.getInt(2);

					PO record = null;

					if ("OCs".equalsIgnoreCase(tabName))
						record = new X_ZZ_QCTO_Alloc_OC(getCtx(), trueRecordId, get_TrxName());
					else if ("Skills".equalsIgnoreCase(tabName))
						record = new X_ZZ_QCTO_Alloc_Skills(getCtx(), trueRecordId, get_TrxName());
					else if ("ACs".equalsIgnoreCase(tabName))
						record = new X_ZZ_QCTO_Alloc_AC(getCtx(), trueRecordId, get_TrxName());

					if (record != null && record.get_ID() > 0)
					{
						String key = record.get_Table_ID() + "-" + record.get_ID();
						if (seen.add(key))
						{
							results.add(record);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Failed resolving view row", e);
		}
	}

	/**
	 * Generates and saves a new Empty Draft ZZ_AccreditationAuditNotify batch header.
	 *
	 * @return The newly created header record
	 */
	private X_ZZ_AccreditationAuditNotify createHeader()
	{
		X_ZZ_AccreditationAuditNotify header = new X_ZZ_AccreditationAuditNotify(getCtx(), 0, get_TrxName());
		header.setZZ_DocStatus(X_ZZ_AccreditationAuditNotify.ZZ_DOCSTATUS_Draft);
		header.setZZ_Submitter_ID(Env.getAD_User_ID(getCtx()));
		header.setZZ_Date_Submitted(new Timestamp(System.currentTimeMillis()));
		header.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
		header.saveEx();
		return header;
	}

	/**
	 * Generates a single child line linked to the Batch header, intelligently
	 * copying standard fields from the source application PO to the notification line.
	 *
	 * @param header The Parent batch header
	 * @param source The specific Application PO (OC, Skills, or AC)
	 * @param lineNo The sequence line number (e.g., 10, 20)
	 */
	private void createLine(X_ZZ_AccreditationAuditNotify header, PO source, int lineNo)
	{
		X_ZZ_AccreditationAuditNotifyLine line = new X_ZZ_AccreditationAuditNotifyLine(getCtx(), 0, get_TrxName());
		line.setZZ_AccreditationAuditNotify_ID(header.getZZ_AccreditationAuditNotify_ID());
		line.setLineNo(lineNo);
		line.setAD_Org_ID(header.getAD_Org_ID());

		if (source instanceof X_ZZ_QCTO_Alloc_OC)
		{
			line.set_ValueOfColumn("ZZ_QCTO_Alloc_OC_ID", source.get_ID());
		}
		else if (source instanceof X_ZZ_QCTO_Alloc_Skills)
		{
			line.set_ValueOfColumn("ZZ_QCTO_Alloc_Skills_ID", source.get_ID());
		}
		else if (source instanceof X_ZZ_QCTO_Alloc_AC)
		{
			line.set_ValueOfColumn("ZZ_QCTO_Alloc_AC_ID", source.get_ID());
		}

		// Copy all matching fields from source (OC/Skills/AC) to line
		copyField(source, line, "Name");
		copyField(source, line, "ZZSurname");
		copyField(source, line, "ZZLegalName");
		copyField(source, line, "EMail");
		copyField(source, line, "Address1");
		copyField(source, line, "Address2");
		copyField(source, line, "City");
		copyField(source, line, "Region");
		copyField(source, line, "Postalcode");
		copyField(source, line, "C_BPartner_ID");
		copyField(source, line, "ZZ_QCTO_Allocation_ID");
		copyField(source, line, "ZZ_Qualification");
		copyField(source, line, "ZZ_QualityPartner");
		copyField(source, line, "ZZ_SAQAIDOrSPID");
		copyField(source, line, "ZZ_NQF_Level");
		copyField(source, line, "ZZ_AllocationNo");
		copyField(source, line, "ZZ_AllocationMonth");
		copyField(source, line, "ZZ_DocStatus");
		copyField(source, line, "ZZ_Designation");
		copyField(source, line, "ZZ_ContactTitle");
		copyField(source, line, "ZZ_CIPCNumber");
		copyField(source, line, "ZZ_AltContactName");
		copyField(source, line, "ZZ_AltContactSurname");
		copyField(source, line, "ZZ_AltContactEmail");
		copyField(source, line, "ZZ_AltContactTitle");
		copyField(source, line, "ZZ_SiteVisitDate");

		line.saveEx();
	}

	/**
	 * Safely copy a column value from source PO to target PO if both have the
	 * column.
	 */
	private void copyField(PO source, PO target, String columnName)
	{
		int srcIdx = source.get_ColumnIndex(columnName);
		int tgtIdx = target.get_ColumnIndex(columnName);
		if (srcIdx >= 0 && tgtIdx >= 0)
		{
			Object value = source.get_Value(columnName);
			if (value != null)
			{
				target.set_ValueOfColumn(columnName, value);
			}
		}
	}
}
