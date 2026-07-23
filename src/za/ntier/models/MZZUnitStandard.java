package za.ntier.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZUnitStandard;

@SuppressWarnings("serial")
public class MZZUnitStandard extends X_ZZUnitStandard
{

	public MZZUnitStandard(Properties ctx, String ZZUnitStandard_UU, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZUnitStandard_UU, trxName, virtualColumns);
	}

	public MZZUnitStandard(Properties ctx, String ZZUnitStandard_UU, String trxName)
	{
		super(ctx, ZZUnitStandard_UU, trxName);
	}

	public MZZUnitStandard(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MZZUnitStandard(Properties ctx, int ZZUnitStandard_ID, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZUnitStandard_ID, trxName, virtualColumns);
	}

	public MZZUnitStandard(Properties ctx, int ZZUnitStandard_ID, String trxName)
	{
		super(ctx, ZZUnitStandard_ID, trxName);
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		Timestamp regStartDate = getRegistrationstartdate();
		Timestamp lastEnrolmentDate = getZZLastEnrolmentDate();
		Timestamp lastAchievementDate = getZZLastAchievementDate();

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

		return super.beforeSave(newRecord);
	}

}
