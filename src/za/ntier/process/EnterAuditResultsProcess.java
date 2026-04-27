package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.I_ZZ_QAAudit;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_QAAudit;
import za.co.ntier.api.model.X_ZZ_QAAuditAllocations;

@Process(name = "za.ntier.process.EnterAuditResultsProcess")
public class EnterAuditResultsProcess extends SvrProcess
{

	@Override
	protected void prepare()
	{
		// No parameters to map
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

		int countHeader = 0;
		int countChild = 0;

		for (Integer orgId : orgIds)
		{
			X_ZZ_Organization org = new X_ZZ_Organization(getCtx(), orgId, get_TrxName());

			// 1. Create QA Audit Header
			X_ZZ_QAAudit auditHeader = new X_ZZ_QAAudit(getCtx(), 0, get_TrxName());
			auditHeader.setZZ_Organization_ID(orgId);

			if (org.getZZLegalName() != null)
			{
				auditHeader.setZZLegalName(org.getZZLegalName());
			}

			if (org.getZZ_AuditLead_IDs() != null)
			{
				auditHeader.setZZ_AuditLead_IDs(org.getZZ_AuditLead_IDs());
			}

			auditHeader.saveEx();
			countHeader++;

			// 2. Query Allocations for this Organization
			List<X_ZZ_Allocations> allocations = new Query(getCtx(), X_ZZ_Allocations.Table_Name, "ZZ_Organization_ID=?", get_TrxName())
																																		.setParameters(orgId)
																																		.list();

			// 3. Create Allocation children under the new Header
			for (X_ZZ_Allocations alloc : allocations)
			{
				X_ZZ_QAAuditAllocations auditAlloc = new X_ZZ_QAAuditAllocations(getCtx(), 0, get_TrxName());

				PO.copyValues(alloc, auditAlloc);

				auditAlloc.setZZ_QAAudit_ID(auditHeader.get_ID());
				auditAlloc.setZZ_DocStatus(X_ZZ_QAAuditAllocations.ZZ_DOCSTATUS_Draft);

				auditAlloc.saveEx();
				countChild++;
			}

			org.setZZ_IsAuditResultsEntered(true);
			org.saveEx();

			addLog(	auditHeader.get_ID(), null, null, "Created " + allocations.size() + " allocations for " + org.getZZLegalName(), I_ZZ_QAAudit.Table_ID,
					auditHeader.get_ID());
		}

		return "Created " + countHeader + " QA Audit and " + countChild + " Allocation lines.";
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
