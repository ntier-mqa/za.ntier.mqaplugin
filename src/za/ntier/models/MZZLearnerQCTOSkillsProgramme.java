package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZLearnerQCTOSkillsProgramme;
import za.ntier.utils.ZZLearnerUtil;

/**
 * Custom Model class for the ZZLearnerQCTOSkillsProgramme table.
 */
public class MZZLearnerQCTOSkillsProgramme extends X_ZZLearnerQCTOSkillsProgramme
{
	private static final long serialVersionUID = 1L;

	public MZZLearnerQCTOSkillsProgramme(Properties ctx, int ZZLearnerQCTOSkillsProgramme_ID, String trxName)
	{
		super(ctx, ZZLearnerQCTOSkillsProgramme_ID, trxName);
	}

	public MZZLearnerQCTOSkillsProgramme(Properties ctx, int ZZLearnerQCTOSkillsProgramme_ID, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearnerQCTOSkillsProgramme_ID, trxName, virtualColumns);
	}

	public MZZLearnerQCTOSkillsProgramme(Properties ctx, String ZZLearnerQCTOSkillsProgramme_UU, String trxName)
	{
		super(ctx, ZZLearnerQCTOSkillsProgramme_UU, trxName);
	}

	public MZZLearnerQCTOSkillsProgramme(Properties ctx, String ZZLearnerQCTOSkillsProgramme_UU, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearnerQCTOSkillsProgramme_UU, trxName, virtualColumns);
	}

	public MZZLearnerQCTOSkillsProgramme(Properties ctx, ResultSet rs, String trxName)
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
