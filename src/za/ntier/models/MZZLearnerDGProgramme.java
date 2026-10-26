package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZLearnerDGProgramme;
import za.ntier.utils.ZZLearnerUtil;

/**
 * Custom Model class for the ZZLearnerDGProgramme table.
 */
public class MZZLearnerDGProgramme extends X_ZZLearnerDGProgramme
{
	private static final long serialVersionUID = 1L;

	public MZZLearnerDGProgramme(Properties ctx, int ZZLearnerDGProgramme_ID, String trxName)
	{
		super(ctx, ZZLearnerDGProgramme_ID, trxName);
	}

	public MZZLearnerDGProgramme(Properties ctx, int ZZLearnerDGProgramme_ID, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearnerDGProgramme_ID, trxName, virtualColumns);
	}

	public MZZLearnerDGProgramme(Properties ctx, String ZZLearnerDGProgramme_UU, String trxName)
	{
		super(ctx, ZZLearnerDGProgramme_UU, trxName);
	}

	public MZZLearnerDGProgramme(Properties ctx, String ZZLearnerDGProgramme_UU, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearnerDGProgramme_UU, trxName, virtualColumns);
	}

	public MZZLearnerDGProgramme(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		ZZLearnerUtil.onInterventionBeforeSave(this, newRecord);
		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		ZZLearnerUtil.onInterventionAfterSave(this, newRecord, success);
		return super.afterSave(newRecord, success);
	}

	/**
	 * Resolves learner ID or Passport number safely.
	 */
	public String getIDOrPassportNo()
	{
		return ZZLearnerUtil.getIDOrPassportNo(this, null);
	}

	public String getLearnerIDNo()
	{
		return getIDOrPassportNo();
	}

	/**
	 * Resolves learner first name from linked ZZPerson.
	 * Getter for @=ZZFirstName@ in mail templates.
	 */
	public String getZZFirstName()
	{
		return ZZLearnerUtil.getLearnerFirstName(this, null);
	}

	public String getLearnerFirstName()
	{
		return getZZFirstName();
	}

	/**
	 * Resolves learner surname from linked ZZPerson.
	 * Getter for @=ZZSurname@ or @=Surname@ in mail templates.
	 */
	public String getZZSurname()
	{
		return ZZLearnerUtil.getLearnerSurname(this, null);
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
	 * Getter for @=ProgrammeName@ in mail templates.
	 */
	public String getProgrammeName()
	{
		return ZZLearnerUtil.resolveProgrammeName(this);
	}

	/**
	 * Getter for @=ZZReason@ in mail templates.
	 */
	public String getZZReason()
	{
		String reason = getZZ_Reason();
		return reason != null ? reason : "";
	}

	/**
	 * Sends Learner Confirmation email to the SDP, attaching the landscape confirmation letter PDF.
	 */
	public void sendLearnerConfirmationToSDP()
	{
		ZZLearnerUtil.sendLearnerConfirmationToSDP(null, this);
	}

	/**
	 * Sends Learner Query email with query reason to the SDP.
	 */
	public void sendLearnerQueryToSDP()
	{
		ZZLearnerUtil.sendLearnerQueryToSDP(null, this);
	}
}
