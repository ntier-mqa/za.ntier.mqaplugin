package za.ntier.process;

import java.sql.ResultSet;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;

import za.co.ntier.api.model.X_ZZQctoLearnershipModule;

@Process
public class CopyQctoLearnershipModules extends SvrProcess
{

	private int p_Source_ZZQctoLearnership_ID = 0;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
			{
				;
			}
			else if (name.equals("ZZQctoLearnership_ID"))
			{
				p_Source_ZZQctoLearnership_ID = para[i].getParameterAsInt();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int targetLearnership_ID = getRecord_ID();
		if (targetLearnership_ID == 0)
		{
			throw new AdempiereUserError("Please save the record first.");
		}
		if (p_Source_ZZQctoLearnership_ID == 0)
		{
			throw new AdempiereUserError("Please select a Source Learnership.");
		}

		// Get all lines from the source
		List<X_ZZQctoLearnershipModule> sourceLines = new Query(getCtx(), X_ZZQctoLearnershipModule.Table_Name, "ZZQctoLearnership_ID=?", get_TrxName())
																																						.setParameters(p_Source_ZZQctoLearnership_ID)
																																						.list();

		int count = 0;
		for (X_ZZQctoLearnershipModule sourceLine : sourceLines)
		{
			// Create new line linked to the target learnership
			X_ZZQctoLearnershipModule newLine = new X_ZZQctoLearnershipModule(getCtx(), (ResultSet) null, get_TrxName());
			newLine.setAD_Org_ID(sourceLine.getAD_Org_ID());
			newLine.setZZQctoLearnership_ID(targetLearnership_ID);

			// Copy data fields
			if (sourceLine.getZZQctoModule_ID() > 0)
			{
				newLine.setZZQctoModule_ID(sourceLine.getZZQctoModule_ID());
			}
			newLine.setZZModuleType(sourceLine.getZZModuleType());
			newLine.setZZMigrationCode(sourceLine.getZZMigrationCode());
			newLine.setZZMigrateValues(sourceLine.getZZMigrateValues());

			if (newLine.save())
			{
				count++;
			}
		}

		return "Copied " + count + " modules successfully.";
	}
}
