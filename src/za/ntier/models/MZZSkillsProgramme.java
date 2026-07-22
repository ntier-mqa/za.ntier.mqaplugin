package za.ntier.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import za.co.ntier.api.model.X_ZZSkillsProgramme;

@SuppressWarnings("serial")
public class MZZSkillsProgramme extends X_ZZSkillsProgramme
{

	public MZZSkillsProgramme(Properties ctx, String ZZSkillsProgramme_UU, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZSkillsProgramme_UU, trxName, virtualColumns);
	}

	public MZZSkillsProgramme(Properties ctx, String ZZSkillsProgramme_UU, String trxName)
	{
		super(ctx, ZZSkillsProgramme_UU, trxName);
	}

	public MZZSkillsProgramme(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MZZSkillsProgramme(Properties ctx, int ZZSkillsProgramme_ID, String trxName, String[] virtualColumns)
	{
		super(ctx, ZZSkillsProgramme_ID, trxName, virtualColumns);
	}

	public MZZSkillsProgramme(Properties ctx, int ZZSkillsProgramme_ID, String trxName)
	{
		super(ctx, ZZSkillsProgramme_ID, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		if (newRecord)
		{
			Timestamp regStartDate = getRegistrationstartdate();
			Timestamp regEndDate = getRegistrationenddate();

			if (regStartDate != null && regEndDate != null)
			{
				if (regEndDate.before(regStartDate))
				{
					log.saveError("Error", "Registration end date cannot be before Registration start date");
					return false;
				}
			}
		}

		return super.beforeSave(newRecord);
	}
}
