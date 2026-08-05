package za.ntier.process;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZAssessorPerson_v;
import za.co.ntier.api.model.X_ZZ_QA_Configuration;

@Process(name = "za.ntier.process.AssessorModeratorExpiryProcess")
public class AssessorModeratorExpiryProcess extends SvrProcess
{

	private static final String	MAIL_TEMPLATE_UU	= "d47f65bc-48ad-4b0c-8042-bd0188a981bd";
	private static final String	QA_CONFIG_UU		= "6e226090-2983-4f9a-b5c5-297ef5cad379";

	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{

		X_ZZ_QA_Configuration qaConfig = new Query(getCtx(), X_ZZ_QA_Configuration.Table_Name, "ZZ_QA_Configuration_UU=?", get_TrxName())
																																			.setParameters(QA_CONFIG_UU)
																																			.firstOnly();

		int monthsBeforeExpiry = 2; // Default
		if (qaConfig != null && qaConfig.getZZ_MonthsBeforeExpiryNotify() > 0)
		{
			monthsBeforeExpiry = qaConfig.getZZ_MonthsBeforeExpiryNotify();
		}

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.MONTH, monthsBeforeExpiry);

		Timestamp targetDateStart = new Timestamp(cal.getTimeInMillis());

		cal.add(Calendar.DAY_OF_MONTH, 1);
		Timestamp targetDateEnd = new Timestamp(cal.getTimeInMillis());

		// Only notify Assessor/Moderator records that are currently Approved
		List<X_ZZAssessorPerson_v> expiringRecords = new Query(	getCtx(), X_ZZAssessorPerson_v.Table_Name, "EndDate >= ? AND EndDate < ? AND ZZ_DocStatus=?",
																get_TrxName())
																				.setParameters(	targetDateStart,
																								targetDateEnd,
																								X_ZZAssessorPerson_v.ZZ_DOCSTATUS_Approved)
																				.setOnlyActiveRecords(true)
																				.list();

		if (expiringRecords.isEmpty())
		{
			return "No Assessor/Moderator records expiring in exactly " + monthsBeforeExpiry + " months (" + targetDateStart.toString().substring(0, 10) + ").";
		}

		MMailText mailText = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																										.setParameters(MAIL_TEMPLATE_UU)
																										.firstOnly();

		if (mailText == null)
		{
			log.warning("Expiry Mail Template not found with UUID: " + MAIL_TEMPLATE_UU);
			return "Error: Mail Template not found. Please set the correct UUID in AssessorModeratorExpiryProcess.";
		}

		MClient client = MClient.get(getCtx());
		int emailsSent = 0;

		for (X_ZZAssessorPerson_v record : expiringRecords)
		{
			String sdpEmail = null;
			String sdpName = null;
			int createdById = record.getCreatedBy();
			if (createdById > 0)
			{
				MUser sdpUser = new MUser(getCtx(), createdById, get_TrxName());
				sdpEmail = sdpUser.getEMail();
				sdpName = sdpUser.getName();
			}

			String role = record.getZZAssessorRole() != null ? record.getZZAssessorRole() : "Assessor/Moderator";

			String assessorNum = "";
			if (X_ZZAssessorPerson_v.ZZASSESSORROLE_Moderator.equals(role) && record.getZZ_Moderator() != null)
			{
				assessorNum = record.getZZ_Moderator();
			}
			else if (record.getZZ_Assessor() != null)
			{
				assessorNum = record.getZZ_Assessor();
			}
			else if (record.getDocumentNo() != null)
			{
				assessorNum = record.getDocumentNo(); // fallback
			}

			String endDateStr = record.getEndDate() != null ? record.getEndDate().toString().substring(0, 10) : "";

			String subject = mailText.getMailHeader();
			if (subject != null)
			{
				subject = subject	.replace("@Role@", role)
									.replace("@ZZ_Assessor@", assessorNum);
			}

			String assessorName = (record.getZZFirstName() != null ? record.getZZFirstName() : "") + " " + (record.getZZSurname() != null ? record.getZZSurname() : "");
			assessorName = assessorName.trim();

			String msg = mailText.getMailText(true);
			if (msg != null)
			{
				msg = msg.replace("@SDPName@", sdpName != null ? sdpName : ",");
				msg = msg.replace("@AssessorModeratorName@", assessorName);
				msg = msg.replace("@Role@", role);
				msg = msg.replace("@ZZ_Assessor@", assessorNum);
				msg = msg.replace("@EndDate@", endDateStr);
			}

			// Send to Primary SDP Admin (CreatedBy)
			if (!Util.isEmpty(sdpEmail))
			{
				if (client.sendEMail(sdpEmail, subject, msg, null, mailText.isHtml()))
				{
					addLog(record.get_ID(), null, null, "Notified SDP Admin: " + sdpEmail);
					emailsSent++;
				}
				else
				{
					log.log(Level.WARNING, "Failed to send expiry email to SDP Admin: " + sdpEmail);
				}
			}
		}

		return "@Success@ Processed " + expiringRecords.size() + " records. Emails sent: " + emailsSent;
	}
}
