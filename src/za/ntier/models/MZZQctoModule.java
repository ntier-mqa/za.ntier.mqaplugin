package za.ntier.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZQctoModule;

@SuppressWarnings("serial")
public class MZZQctoModule extends X_ZZQctoModule
{

	public MZZQctoModule(Properties ctx, String ZZQctoModule_UU, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZQctoModule_UU, trxName, virtualColumns);
	}

	public MZZQctoModule(Properties ctx, String ZZQctoModule_UU, String trxName)
	{
		super(ctx, ZZQctoModule_UU, trxName);
	}

	public MZZQctoModule(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MZZQctoModule(Properties ctx, int ZZQctoModule_ID, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZQctoModule_ID, trxName, virtualColumns);
	}

	public MZZQctoModule(Properties ctx, int ZZQctoModule_ID, String trxName)
	{
		super(ctx, ZZQctoModule_ID, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		Timestamp regStartDate = getRegistrationstartdate();
		Timestamp regEndDate = getRegistrationenddate();
		Timestamp lastEnrolmentDate = getZZLastEnrolmentDate();
		Timestamp lastAchievementDate = getZZLastAchievementDate();

		if (newRecord || is_ValueChanged(COLUMNNAME_Registrationenddate) || is_ValueChanged(COLUMNNAME_Registrationstartdate))
		{
			if (regStartDate != null && regEndDate != null && regEndDate.before(regStartDate))
			{
				log.saveError("Error", "Registration end date cannot be before Registration start date");
				return false;
			}
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_ZZLastEnrolmentDate) || is_ValueChanged(COLUMNNAME_Registrationstartdate))
		{
			if (regStartDate != null && lastEnrolmentDate != null && lastEnrolmentDate.before(regStartDate))
			{
				log.saveError("Error", "Last enrollment date cannot be before Registration start date");
				return false;
			}
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_ZZLastAchievementDate) || is_ValueChanged(COLUMNNAME_Registrationstartdate))
		{
			if (regStartDate != null && lastAchievementDate != null && lastAchievementDate.before(regStartDate))
			{
				log.saveError("Error", "Last achievement date cannot be before Registration start date");
				return false;
			}
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_ZZLastAchievementDate) || is_ValueChanged(COLUMNNAME_ZZLastEnrolmentDate))
		{
			if (lastAchievementDate != null && lastEnrolmentDate != null && lastAchievementDate.before(lastEnrolmentDate))
			{
				log.saveError("Error", "Last achievement date cannot be before Last enrollment date");
				return false;
			}
		}

		return super.beforeSave(newRecord);
	}
}
