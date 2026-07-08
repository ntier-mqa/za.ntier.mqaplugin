package za.ntier.process;

import java.sql.ResultSet;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;

import za.co.ntier.api.model.X_ZZQctoSkillsProgrammeModule;

@Process
public class CopyQctoSkillsProgrammeModules extends SvrProcess
{

	private int p_Source_ZZQctoSkillsProgramme_ID = 0;

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
			else if (name.equals(X_ZZQctoSkillsProgrammeModule.COLUMNNAME_ZZQctoSkillsProgramme_ID))
			{
				p_Source_ZZQctoSkillsProgramme_ID = para[i].getParameterAsInt();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int targetSkillsProgramme_ID = getRecord_ID();
		if (targetSkillsProgramme_ID == 0)
		{
			throw new AdempiereUserError("Please save the record first.");
		}
		if (p_Source_ZZQctoSkillsProgramme_ID == 0)
		{
			throw new AdempiereUserError("Please select a Source Skills Programme.");
		}

		// Get all lines from the source
		List<X_ZZQctoSkillsProgrammeModule> sourceLines = new Query(getCtx(), X_ZZQctoSkillsProgrammeModule.Table_Name,
																	X_ZZQctoSkillsProgrammeModule.COLUMNNAME_ZZQctoSkillsProgramme_ID + "=?",
																	get_TrxName())
																					.setParameters(p_Source_ZZQctoSkillsProgramme_ID)
																					.list();

		int count = 0;
		for (X_ZZQctoSkillsProgrammeModule sourceLine : sourceLines)
		{
			// Create new line linked to the target skills programme
			X_ZZQctoSkillsProgrammeModule newLine = new X_ZZQctoSkillsProgrammeModule(getCtx(), (ResultSet) null, get_TrxName());
			newLine.setAD_Org_ID(sourceLine.getAD_Org_ID());
			newLine.setZZQctoSkillsProgramme_ID(targetSkillsProgramme_ID);

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
