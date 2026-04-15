package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.I_ZZ_QAAudit;
import za.co.ntier.api.model.X_ZZ_QAAudit;

@Process(name = "za.ntier.process.MarkAuditSentProcess")
public class MarkAuditSentProcess extends SvrProcess
{

	@Override
	protected void prepare()
	{
		// No parameters mapped
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();
		List<Integer> auditIds = getSelectedAuditIds(pInstanceId);

		if (auditIds.isEmpty())
		{
			return "No records were selected.";
		}

		int count = 0;

		for (Integer auditId : auditIds)
		{
			X_ZZ_QAAudit auditHeader = new X_ZZ_QAAudit(getCtx(), auditId, get_TrxName());
			auditHeader.setZZ_isSentToQCTONAMB(true);
			auditHeader.saveEx();
			count++;

			addLog(auditHeader.get_ID(), null, null, "Marked as Sent: " + auditHeader.getZZLegalName(), I_ZZ_QAAudit.Table_ID, auditHeader.get_ID());
		}

		return "Marked " + count + " QA Audits as sent to QCTO/NAMB.";
	}

	private List<Integer> getSelectedAuditIds(int pInstanceId)
	{
		List<Integer> results = new ArrayList<>();

		String sqlSelect = "SELECT T_Selection_ID FROM ("
							+ "  SELECT T_Selection_ID FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=? "
							+ "  UNION "
							+ "  SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=? "
							+ ") x";

		try (PreparedStatement pstmt = DB.prepareStatement(sqlSelect, get_TrxName()))
		{
			pstmt.setInt(1, pInstanceId);
			pstmt.setInt(2, pInstanceId);
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					results.add(rs.getInt(1));
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error reading info window selection", e);
		}

		return results;
	}
}
