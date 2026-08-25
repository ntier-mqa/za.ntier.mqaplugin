package za.ntier.process;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;

@Process(name = "za.ntier.process.ResendWSPATRApprovalEmailBatchProcess")
public class ResendWSPATRApprovalEmailBatchProcess extends SvrProcess
{
	private static final int	DEFAULT_EXCLUDE_YEAR	= 2026;
	private static final int	DEFAULT_EXCLUDE_MONTH	= Calendar.JUNE;
	private static final int	DEFAULT_EXCLUDE_DAY		= 30;

	private Timestamp excludeApprovedDate = null;

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
			else if (name.equals("ExcludeApprovedDate"))
			{
				excludeApprovedDate = para[i].getParameterAsTimestamp();
			}
			else
			{
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
			}
		}

		if (excludeApprovedDate == null)
		{
			Calendar cal = Calendar.getInstance();
			cal.clear();
			cal.set(DEFAULT_EXCLUDE_YEAR, DEFAULT_EXCLUDE_MONTH, DEFAULT_EXCLUDE_DAY);
			excludeApprovedDate = new Timestamp(cal.getTimeInMillis());
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		// Exclude the whole day, regardless of the time component stored on ZZ_ApprovedDate
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(excludeApprovedDate.getTime());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Timestamp dayStart = new Timestamp(cal.getTimeInMillis());
		cal.add(Calendar.DAY_OF_MONTH, 1);
		Timestamp dayEnd = new Timestamp(cal.getTimeInMillis());

		List<X_ZZ_WSP_ATR_Submitted> submissions = new Query(	getCtx(), X_ZZ_WSP_ATR_Submitted.Table_Name,
																X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_DocStatus + "=? AND "
																		+ X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_ApprovedDate + " IS NOT NULL AND ("
																		+ X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_ApprovedDate + " < ? OR "
																		+ X_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_ApprovedDate + " >= ?)",
																get_TrxName())
																						.setParameters(	X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Approved,
																										dayStart, dayEnd)
																						.setOnlyActiveRecords(true)
																						.list();

		if (submissions.isEmpty())
			return "No approved WSP-ATR submissions found with an Approved Date other than " + dayStart.toString().substring(0, 10) + ".";

		int sentCount = 0;
		int failedCount = 0;

		for (X_ZZ_WSP_ATR_Submitted submission : submissions)
		{
			MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), submission.get_ID(), get_TrxName());
			try
			{
				submitted.resendApprovalEmail();
				sentCount++;
			}
			catch (Exception e)
			{
				failedCount++;
				log.log(Level.SEVERE, "Failed to resend approval email for ZZ_WSP_ATR_Submitted_ID=" + submission.get_ID(), e);
				addLog(submission.get_ID(), null, null, "Failed to resend approval email: " + e.getMessage());
			}
		}

		return "@Success@ " + sentCount + " approval email(s) resent, " + failedCount + " failed, out of " + submissions.size()
				+ " submission(s) with Approved Date other than " + dayStart.toString().substring(0, 10) + ".";
	}
}
