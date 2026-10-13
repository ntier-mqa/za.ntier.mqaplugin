package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.EMail;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZLearner;
import za.co.ntier.api.model.X_ZZPerson;

/**
 * Custom Model class for the ZZLearner table.
 */
public class MZZLearner extends X_ZZLearner
{
	private static final long	serialVersionUID						= 1L;

	private static final String	LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU	= "eac216ef-5331-4d12-980d-7d7710b9829c";
	private static final String	LEARNER_QUERY_MAIL_TEMPLATE_UU			= "ba58e3ff-6b42-4f4c-be01-cfbe1255803e";
	private static final String	LEARNER_REGISTRATION_LETTER_CC			= "LEARNER_REGISTRATION_LETTER_CC";

	public MZZLearner(Properties ctx, int ZZLearner_ID, String trxName)
	{
		super(ctx, ZZLearner_ID, trxName);
	}

	public MZZLearner(Properties ctx, int ZZLearner_ID, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearner_ID, trxName, virtualColumns);
	}

	public MZZLearner(Properties ctx, String ZZLearner_UU, String trxName)
	{
		super(ctx, ZZLearner_UU, trxName);
	}

	public MZZLearner(Properties ctx, String ZZLearner_UU, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearner_UU, trxName, virtualColumns);
	}

	public MZZLearner(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		// Auto-generate ReferenceNo when status is Registered
		if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus) || newRecord)
		{
			if (ZZ_DOCSTATUS_Registered.equals(getZZ_DocStatus()))
			{
				if (Util.isEmpty(getReferenceNo()))
				{
					String id = getIDOrPassportNo();
					if (!Util.isEmpty(id))
					{
						String seqNo = MSequence.getDocumentNo(getAD_Client_ID(), get_TableName(), get_TrxName(), this);
						setReferenceNo(id + "/" + seqNo);
					}
				}
			}
		}

		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		if (success && is_ValueChanged(COLUMNNAME_ZZ_DocStatus))
		{
			String status = getZZ_DocStatus();
			if (ZZ_DOCSTATUS_Registered.equals(status))
			{
				sendLearnerConfirmationToSDP();
			}
			else if (ZZ_DOCSTATUS_Query.equals(status))
			{
				sendLearnerQueryToSDP();
			}
		}

		return super.afterSave(newRecord, success);
	}

	/**
	 * Resolves learner ID or Passport number safely (from virtual column or linked ZZPerson).
	 */
	public String getIDOrPassportNo()
	{
		String id = getZZ_ID_Passport_No();
		if (Util.isEmpty(id) && getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			id = person.getZZ_ID_Passport_No();
		}
		return id != null ? id : "";
	}

	/**
	 * Resolves learner first name from linked ZZPerson.
	 * Also serves as getter for @=ZZFirstName@ in mail templates.
	 */
	public String getZZFirstName()
	{
		if (getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			return person.getZZFirstName() != null ? person.getZZFirstName() : "";
		}
		return "";
	}

	public String getLearnerFirstName()
	{
		return getZZFirstName();
	}

	/**
	 * Resolves learner surname from linked ZZPerson.
	 * Also serves as getter for @=ZZSurname@ or @=Surname@ in mail templates.
	 */
	public String getZZSurname()
	{
		if (getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			return person.getSurname() != null ? person.getSurname() : "";
		}
		return "";
	}

	public String getSurname()
	{
		return getZZSurname();
	}

	public String getLearnerSurname()
	{
		return getZZSurname();
	}

	/**
	 * Getter for @=ZZReason@ in mail templates (matches template property without underscore).
	 */
	public String getZZReason()
	{
		String reason = getZZ_Reason();
		return reason != null ? reason : "";
	}

	/**
	 * Resolves the SDP Admin user directly from ZZ_SDPAdmin_ID with fallback to record creator.
	 */
	private MUser resolveSDPUser()
	{
		int sdpUserId = getZZ_SDPAdmin_ID();
		if (sdpUserId <= 0)
		{
			sdpUserId = getCreatedBy();
		}

		return MUser.get(getCtx(), sdpUserId);
	}

	/**
	 * Sends Learner Confirmation email to the SDP.
	 */
	private void sendLearnerConfirmationToSDP()
	{
		try
		{
			MUser sdpUser = resolveSDPUser();
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for learner: " + get_ID());
				return;
			}

			MMailText mailTemplate = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																												.setParameters(LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU)
																												.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Confirmation mail template not found for UUID: " + LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU);
				return;
			}

			mailTemplate.setUser(sdpUser);
			mailTemplate.setPO(this);

			sendEmailWithCC(sdpUser.getEMail(), mailTemplate);
			log.info("Learner confirmation email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail() + ") for learner " + get_ID());
		}
		catch (Exception e)
		{
			log.severe("Failed to send learner confirmation email to SDP: " + e.getMessage());
		}
	}

	/**
	 * Sends Learner Query email with query reason to the SDP.
	 */
	private void sendLearnerQueryToSDP()
	{
		try
		{
			MUser sdpUser = resolveSDPUser();
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for learner: " + get_ID());
				return;
			}

			MMailText mailTemplate = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																												.setParameters(LEARNER_QUERY_MAIL_TEMPLATE_UU)
																												.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Query mail template not found for UUID: " + LEARNER_QUERY_MAIL_TEMPLATE_UU);
				return;
			}

			mailTemplate.setUser(sdpUser);
			mailTemplate.setPO(this);

			sendEmailWithCC(sdpUser.getEMail(), mailTemplate);
			log.info("Learner query email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail() + ") for learner " + get_ID());
		}
		catch (Exception e)
		{
			log.severe("Failed to send learner query email to SDP: " + e.getMessage());
		}
	}

	private void sendEmailWithCC(String toEmail, MMailText mailTemplate)
	{
		MClient client = MClient.get(getCtx(), getAD_Client_ID());
		EMail email = client.createEMail(toEmail, mailTemplate.getMailHeader(), mailTemplate.getMailText(), mailTemplate.isHtml());
		if (email == null)
		{
			log.warning("Failed to create email for learner: " + get_ID());
			return;
		}

		String ccEmails = MSysConfig.getValue(LEARNER_REGISTRATION_LETTER_CC, getAD_Client_ID());
		if (!Util.isEmpty(ccEmails))
		{
			for (String cc : ccEmails.split(","))
			{
				if (!Util.isEmpty(cc, true))
				{
					email.addCc(cc.trim());
				}
			}
		}

		String msg = email.send();
		if (!EMail.SENT_OK.equals(msg))
		{
			log.warning("Failed to send email for learner " + get_ID() + ": " + msg);
		}
	}
}
