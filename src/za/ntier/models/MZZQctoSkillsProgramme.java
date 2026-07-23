package za.ntier.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZQctoSkillsProgramme;

@SuppressWarnings("serial")
public class MZZQctoSkillsProgramme extends X_ZZQctoSkillsProgramme
{

	public MZZQctoSkillsProgramme(Properties ctx, String ZZQctoSkillsProgramme_UU, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZQctoSkillsProgramme_UU, trxName, virtualColumns);
	}

	public MZZQctoSkillsProgramme(Properties ctx, String ZZQctoSkillsProgramme_UU, String trxName)
	{
		super(ctx, ZZQctoSkillsProgramme_UU, trxName);
	}

	public MZZQctoSkillsProgramme(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MZZQctoSkillsProgramme(Properties ctx, int ZZQctoSkillsProgramme_ID, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZQctoSkillsProgramme_ID, trxName, virtualColumns);
	}

	public MZZQctoSkillsProgramme(Properties ctx, int ZZQctoSkillsProgramme_ID, String trxName)
	{
		super(ctx, ZZQctoSkillsProgramme_ID, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		Timestamp regStartDate = getRegistrationstartdate();
		Timestamp regEndDate = getRegistrationenddate();
		Timestamp lastEnrolmentDate = getZZLastEnrolmentDate();

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

		return super.beforeSave(newRecord);
	}
}
