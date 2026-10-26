package za.ntier.models;

import java.io.File;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZLearner;
import za.co.ntier.api.model.X_ZZPerson;
import za.ntier.utils.ZZLearnerUtil;

/**
 * Custom Model class for the ZZLearner table.
 */
public class MZZLearner extends X_ZZLearner
{
	private static final long serialVersionUID = 1L;

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
		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		return super.afterSave(newRecord, success);
	}

	/**
	 * Finds the programme child record in Draft status across all programme types.
	 */
	public PO findDraftProgramme()
	{
		return ZZLearnerUtil.findDraftProgramme(this);
	}

	/**
	 * Resolves the display programme name for confirmation letters.
	 */
	public String resolveProgrammeName(PO prog)
	{
		return ZZLearnerUtil.resolveProgrammeName(prog);
	}

	/**
	 * Generates the Registration Confirmation Letter PDF.
	 */
	public File generateRegistrationConfirmationLetterPdf(PO draftProg)
	{
		return ZZLearnerUtil.generateRegistrationConfirmationLetterPdf(this, draftProg);
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

	public String getLearnerIDNo()
	{
		return getIDOrPassportNo();
	}

	/**
	 * Resolves learner first name from linked ZZPerson.
	 * Also serves as getter for @=ZZFirstName@ in mail templates.
	 */
	public String getZZFirstName()
	{
		return ZZLearnerUtil.getLearnerFirstName(this);
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
		return ZZLearnerUtil.getLearnerSurname(this);
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
	public MUser resolveSDPUser()
	{
		return ZZLearnerUtil.resolveSDPUser(this, findDraftProgramme());
	}

	/**
	 * Sends Learner Confirmation email to the SDP, attaching the landscape confirmation letter PDF.
	 */
	public void sendLearnerConfirmationToSDP(PO draftProg)
	{
		ZZLearnerUtil.sendLearnerConfirmationToSDP(this, draftProg);
	}

	/**
	 * Sends Learner Query email with query reason to the SDP.
	 */
	public void sendLearnerQueryToSDP()
	{
		ZZLearnerUtil.sendLearnerQueryToSDP(this, findDraftProgramme());
	}
}
