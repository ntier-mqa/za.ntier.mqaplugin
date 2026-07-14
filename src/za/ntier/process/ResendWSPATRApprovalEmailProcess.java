package za.ntier.process;

import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.ntier.models.MZZWSPATRSubmitted;

@Process(name = "za.ntier.process.ResendWSPATRApprovalEmailProcess")
public class ResendWSPATRApprovalEmailProcess extends SvrProcess
{

	private int recordId = 0;

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
			else
			{
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			}
		}
		recordId = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (recordId <= 0)
		{
			throw new IllegalArgumentException("Error: No WSP-ATR record selected.");
		}

		MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), recordId, get_TrxName());
		if (submitted.get_ID() <= 0)
		{
			throw new IllegalArgumentException("Error: WSP-ATR record not found.");
		}

		try
		{
			submitted.resendApprovalEmail();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Failed to resend approval email", e);
			throw new Exception("Failed to resend approval email: " + e.getMessage(), e);
		}

		return "Approval email has been sent successfully.";
	}
}
