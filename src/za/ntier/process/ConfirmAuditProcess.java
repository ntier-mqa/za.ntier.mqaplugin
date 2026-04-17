package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.I_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;

@Process(name = "za.ntier.process.ConfirmAuditProcess")
public class ConfirmAuditProcess extends SvrProcess
{

	@Parameter(name = "IsConfirmed")
	private boolean	pIsConfirmed	= false;

	@Parameter(name = "IsCancelled")
	private boolean	pIsCancelled	= false;

	@Override
	protected void prepare()
	{
		// parameters are mapped via annotations
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();
		List<Integer> orgIds = getSelectedOrgIds(pInstanceId);

		if (orgIds.isEmpty())
		{
			return "No records were selected.";
		}

		String status = null;
		String actionStr = null;
		if (pIsConfirmed)
		{
			status = X_ZZ_Allocations.ZZ_DOCSTATUS_AuditConfirmed;
			actionStr = "Confirmed";
		}
		else if (pIsCancelled)
		{
			status = X_ZZ_Allocations.ZZ_DOCSTATUS_AuditCancelled;
			actionStr = "Cancelled";
		}

		if (status == null)
		{
			return "Please select either the Confirmed or Cancelled.";
		}

		int count = 0;

		for (Integer orgId : orgIds)
		{
			List<X_ZZ_Allocations> allocations = new Query(getCtx(), X_ZZ_Allocations.Table_Name, "ZZ_Organization_ID=?", get_TrxName())
																																		.setParameters(orgId)
																																		.list();

			int orgAllocCount = 0;
			for (X_ZZ_Allocations alloc : allocations)
			{
				alloc.setZZ_DocStatus(status);
				alloc.saveEx();
				orgAllocCount++;
			}

			X_ZZ_Organization org = new X_ZZ_Organization(getCtx(), orgId, get_TrxName());
			addLog(orgId, null, null, actionStr + " " + orgAllocCount + " allocations for org " + org.getZZLegalName(), I_ZZ_Organization.Table_ID, orgId);
			count += orgAllocCount;
		}

		return actionStr + " a total of " + count + " allocations.";
	}

	private List<Integer> getSelectedOrgIds(int pInstanceId)
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
