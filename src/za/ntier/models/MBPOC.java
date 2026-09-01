package za.ntier.models;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;

import za.co.ntier.api.model.I_ZZLearnership;
import za.co.ntier.api.model.I_ZZQctoLearnership;
import za.co.ntier.api.model.X_C_BP_Learnerships;
import za.co.ntier.api.model.X_C_BP_OC;

public class MBPOC extends X_C_BP_OC
{

	private static final long serialVersionUID = 1L;

	public MBPOC(Properties ctx, int C_BP_OC_ID, String trxName)
	{
		super(ctx, C_BP_OC_ID, trxName);
	}

	public MBPOC(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		if (!success)
		{
			return false;
		}

		// Only create linked learnerships if the Qualification is Accredited
		if (ZZ_STATUS_Accredited.equals(getZZ_Status()))
		{
			if (newRecord || is_ValueChanged(COLUMNNAME_ZZQualification_ID) || is_ValueChanged(COLUMNNAME_ZZ_Status))
			{
				syncLearnerships(getZZQualification_ID(), I_ZZLearnership.Table_Name, COLUMNNAME_ZZQualification_ID, false);
			}

			if (newRecord || is_ValueChanged(COLUMNNAME_ZZQctoQualification_ID) || is_ValueChanged(COLUMNNAME_ZZ_Status))
			{
				syncLearnerships(getZZQctoQualification_ID(), I_ZZQctoLearnership.Table_Name, COLUMNNAME_ZZQctoQualification_ID, true);
			}
		}

		return true;
	}

	private void syncLearnerships(int qualificationId, String masterTableName, String fkColumnName, boolean isQcto)
	{
		if (qualificationId <= 0)
		{
			return;
		}

		List<PO> masterLearnerships = new Query(getCtx(), masterTableName, fkColumnName + "=?", get_TrxName())
																												.setParameters(qualificationId)
																												.list();

		for (PO master : masterLearnerships)
		{
			// Check if the learnership link already exists for this qualification
			String linkWhereClause = X_C_BP_Learnerships.COLUMNNAME_C_BP_OC_ID + "=? AND " + 
					(isQcto ? X_C_BP_Learnerships.COLUMNNAME_ZZQctoLearnership_ID + "=?" : X_C_BP_Learnerships.COLUMNNAME_ZZLearnership_ID + "=?");
			boolean exists = new Query(getCtx(), X_C_BP_Learnerships.Table_Name, linkWhereClause, get_TrxName())
					.setParameters(get_ID(), master.get_ID())
					.match();

			if (exists)
			{
				continue;
			}

			X_C_BP_Learnerships learnership = new X_C_BP_Learnerships(getCtx(), 0, get_TrxName());

			learnership.setC_BPartner_ID(getC_BPartner_ID());
			learnership.setC_BP_OC_ID(get_ID());

			if (isQcto)
			{
				learnership.setZZQctoLearnership_ID(master.get_ID());
				learnership.setZZQctoQualification_ID(qualificationId);
			}
			else
			{
				learnership.setZZLearnership_ID(master.get_ID());
				learnership.setZZQualification_ID(qualificationId);
			}

			learnership.saveEx();
		}
	}
}
