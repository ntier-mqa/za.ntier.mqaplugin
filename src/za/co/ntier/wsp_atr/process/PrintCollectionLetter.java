package za.co.ntier.wsp_atr.process;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcess;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.ntier.report.jasper.DazzleReportStarter;

@Process(name = "za.co.ntier.wsp_atr.process.PrintCollectionLetter")
public class PrintCollectionLetter extends SvrProcess
{

	private String p_ProgrammeType = "";

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			if (para.getParameter() == null)
			{
				continue;
			}

			if ("p_ProgrammeType".equals(para.getParameterName()))
			{
				p_ProgrammeType = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{

		String processValue = "Learnership".equalsIgnoreCase(p_ProgrammeType)
																				? "LearnershipCollectionLetter"
																				: "SkillsProgrammeCollectionLetter";

		int processId = DB.getSQLValue(	get_TrxName(),
										"SELECT AD_Process_ID FROM AD_Process WHERE Value=? AND AD_Client_ID IN (0,?)",
										processValue, Env.getAD_Client_ID(getCtx()));

		if (processId <= 0)
		{
			throw new AdempiereException("Could not find AD_Process with Search Key: " + processValue);
		}

		MProcess process = new MProcess(getCtx(), processId, get_TrxName());
		ProcessInfo pi = new ProcessInfo(process.getName(), processId);
		pi.setAD_User_ID(getAD_User_ID());
		pi.setAD_Client_ID(getAD_Client_ID());
		pi.setParameter(getParameter());

		DazzleReportStarter starter = new DazzleReportStarter();
		starter.startProcess(getCtx(), pi, Trx.get(get_TrxName(), false));

		return "Report launched successfully.";
	}

}
