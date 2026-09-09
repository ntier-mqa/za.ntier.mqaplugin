package za.co.ntier.wsp_atr.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;

@Process(name = "za.co.ntier.wsp_atr.process.WSPATRUndoSubmissionProcess")
public class WSPATRUndoSubmissionProcess extends SvrProcess
{

	/**
	 * Looks up this process' own AD_Process_ID by Classname, so callers don't
	 * need to hardcode an ID that differs between environments (mirrors
	 * MQAConstants.getWFRunProcessId).
	 */
	public static int getProcessId(String trxName)
	{
		int processId = DB.getSQLValue(trxName,
				"SELECT AD_Process_ID FROM AD_Process WHERE Classname=? AND IsActive='Y'",
				WSPATRUndoSubmissionProcess.class.getName());
		if (processId <= 0)
			throw new AdempiereException(
					"Could not find active AD_Process for " + WSPATRUndoSubmissionProcess.class.getName());
		return processId;
	}

	private List<Integer>	p_recordIds	= null;

	private int	m_reset		= 0;
	private int	m_skipped	= 0;

	@Override
	protected void prepare()
	{
		p_recordIds = getSelectedOrCurrentRecords();
	}

	@Override
	protected String doIt() throws Exception
	{
		if (p_recordIds.isEmpty())
		{
			return "No records selected.";
		}

		for (Integer recordId : p_recordIds)
		{
			try
			{
				MZZWSPATRSubmitted submission = new MZZWSPATRSubmitted(getCtx(), recordId, get_TrxName());
				if (submission.get_ID() != recordId)
				{
					addLog(recordId, null, null, "Record not found. Skipping.");
					m_skipped++;
					continue;
				}

				String docStatus = submission.getZZ_DocStatus();
				if (!X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Submitted.equals(docStatus))
				{
					addLog(recordId, null, null,
							"Record " + submission.getDocumentNo() + " is not Submitted (SU). Current status: "
									+ docStatus + ". Skipping.",
							submission.get_Table_ID(), recordId);
					m_skipped++;
					continue;
				}

				submission.setZZ_DocStatus(X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft);
				submission.setZZ_DocAction(null);
				submission.saveEx();

				addLog(recordId, null, null, "Reset " + submission.getDocumentNo() + " to Draft.",
						submission.get_Table_ID(), recordId);
				m_reset++;
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Error resetting record ID " + recordId, e);
				addLog(recordId, null, null, "Error processing: " + e.getMessage());
				m_skipped++;
			}
		}

		return "Undo Submission Complete. Reset to Draft: " + m_reset + ", Skipped: " + m_skipped;
	}

	/**
	 * Supports running from an Info Window selection (T_Selection /
	 * T_Selection_InfoWindow), or as a single-record process (getRecord_ID()),
	 * so it can be wired up either as an Info Window action or a normal
	 * process button without code changes.
	 */
	private List<Integer> getSelectedOrCurrentRecords()
	{
		List<Integer> ids = new ArrayList<>();

		int pInstanceId = getAD_PInstance_ID();
		if (pInstanceId > 0)
		{
			int[] selectionIds = DB.getIDsEx(get_TrxName(),
					"SELECT DISTINCT T_Selection_ID FROM ("
							+ "  SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?"
							+ "  UNION"
							+ "  SELECT T_Selection_ID FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?"
							+ ") x",
					pInstanceId, pInstanceId);
			for (int id : selectionIds)
				ids.add(id);
		}

		if (ids.isEmpty() && getRecord_ID() > 0)
		{
			ids.add(getRecord_ID());
		}

		return ids;
	}
}
