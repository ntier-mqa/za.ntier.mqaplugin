package za.co.ntier.wsp_atr.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ProcessUtil;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;
import za.ntier.utils.MQAConstants;

@Process(name = "za.co.ntier.wsp_atr.process.WSPATRBulkApproveProcess")
public class WSPATRBulkApproveProcess extends SvrProcess
{

	private int	m_updated	= 0;
	private int	m_failed	= 0;

	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();
		if (pInstanceId <= 0)
		{
			throw new AdempiereException("Process must be run from an Info Window");
		}

		int adProcessId = MQAConstants.getWFRunProcessId(get_TrxName());

		List<Integer> selectedIds = getSelectedRecords(pInstanceId);
		if (selectedIds.isEmpty())
		{
			return "No records selected.";
		}

		for (Integer recordId : selectedIds)
		{
			try
			{
				MZZWSPATRSubmitted submission = new MZZWSPATRSubmitted(getCtx(), recordId, get_TrxName());
				String docStatus = submission.getZZ_DocStatus();
				if (!X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForApproval.equals(docStatus))
				{
					addLog(recordId, null, null,
							"Record " + submission.getDocumentNo() + " not Recommended for Approval (RA). Skipping.",
							submission.get_Table_ID(), recordId);
					m_failed++;
					continue;
				}

				// Execute ZZ_WF_RunProcess for this record
				boolean success = runApprovalProcess(adProcessId, submission);
				if (success)
				{
					addLog(recordId, null, null, "Successfully approved " + submission.getDocumentNo(),
							submission.get_Table_ID(), recordId);
					m_updated++;
				}
				else
				{
					addLog(recordId, null, null, "Failed to approve " + submission.getDocumentNo(),
							submission.get_Table_ID(), recordId);
					m_failed++;
				}
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Error processing record ID " + recordId, e);
				addLog(recordId, null, null, "Error processing: " + e.getMessage());
				m_failed++;
			}
		}

		return "Bulk Approval Complete. Approved: " + m_updated + ", Skipped/Failed: " + m_failed;
	}

	private boolean runApprovalProcess(int adProcessId, MZZWSPATRSubmitted submission)
	{
		MProcess proc = new MProcess(getCtx(), adProcessId, get_TrxName());
		ProcessInfo pi = new ProcessInfo(proc.getName(), adProcessId, submission.get_Table_ID(), submission.get_ID());
		pi.setClassName(proc.getClassname());

		MPInstance instance = new MPInstance(Env.getCtx(), pi.getAD_Process_ID(), submission.get_Table_ID(),
				submission.get_ID(), submission.get_UUID());
		instance.saveEx();
		pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

		// Set Approve=Y
		ProcessInfoParameter piParam = new ProcessInfoParameter("Approve", "Y", "", "", "");
		pi.setParameter(new ProcessInfoParameter[] { piParam });

		pi.setAD_User_ID(Env.getAD_User_ID(getCtx()));
		pi.setAD_Client_ID(Env.getAD_Client_ID(getCtx()));

		Trx processTrx = null;
		try
		{
			// Using a separate transaction for each approval if needed, or join
			// the current one.
			processTrx = Trx.get(Trx.createTrxName("BulkApprove"), true);
			boolean runResult = ProcessUtil.startJavaProcess(getCtx(), pi, processTrx, false,
					getProcessInfo().getProcessUI());
			if (runResult)
			{
				processTrx.commit();
			}
			else
			{
				processTrx.rollback();
			}
			return runResult;
		}
		catch (Exception e)
		{
			if (processTrx != null)
			{
				processTrx.rollback();
			}
			return false;
		}
		finally
		{
			if (processTrx != null)
			{
				processTrx.close();
			}
		}
	}

	private List<Integer> getSelectedRecords(int pInstanceId)
	{
		final List<Integer> recordIds = new ArrayList<>();

		final String sql = "SELECT DISTINCT T_Selection_ID " + "FROM ("
				+ "   SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=? " + "   UNION "
				+ "   SELECT T_Selection_ID FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=? " + ") x "
				+ "ORDER BY T_Selection_ID";

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName()))
		{
			pstmt.setInt(1, pInstanceId);
			pstmt.setInt(2, pInstanceId);

			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					recordIds.add(rs.getInt(1));
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error getting selected records for AD_PInstance_ID=" + pInstanceId, e);
		}

		return recordIds;
	}
}
