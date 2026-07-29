package za.ntier.process;

import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;

import za.co.ntier.api.model.X_ZZ_QAAudit;
import za.co.ntier.api.model.X_ZZ_QAAuditAllocations;

/**
 * Process to update QA Audit Allocation statuses from the QA Audit Header.
 * Maps 'Recommended' -> 'Approved' and 'Not Recommended' -> 'Not Approved'.
 */
@Process(name = "za.ntier.process.UpdateQAAuditAllocationsProcess")
public class UpdateQAAuditAllocationsProcess extends SvrProcess
{

	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{
		int recordId = getRecord_ID();
		if (recordId <= 0)
		{
			throw new AdempiereException("This process must be run from a specific QA Audit Header record.");
		}

		X_ZZ_QAAudit header = new X_ZZ_QAAudit(getCtx(), recordId, get_TrxName());
		if (header.get_ID() == 0)
		{
			throw new AdempiereException("QA Audit Header not found.");
		}

		// Query Allocations for this QA Audit Header
		List<X_ZZ_QAAuditAllocations> childLines = new Query(getCtx(), X_ZZ_QAAuditAllocations.Table_Name, "ZZ_QAAudit_ID=?", get_TrxName())
																																			.setParameters(recordId)
																																			.list();

		int approvedCount = 0;
		int notApprovedCount = 0;
		int unchangedCount = 0;

		Timestamp now = new Timestamp(System.currentTimeMillis());
		int userId = getAD_User_ID();

		for (X_ZZ_QAAuditAllocations line : childLines)
		{
			String currentStatus = line.getZZ_DocStatus();
			if (X_ZZ_QAAuditAllocations.ZZ_DOCSTATUS_Recommended.equals(currentStatus))
			{
				line.setZZ_DocStatus(X_ZZ_QAAuditAllocations.ZZ_DOCSTATUS_Approved);
				line.setZZ_ApprovedBy_ID(userId);
				line.setZZ_ApprovedDate(now);
				line.setZZ_Date_Not_Approved(null);
				line.saveEx();
				approvedCount++;
			}
			else if (X_ZZ_QAAuditAllocations.ZZ_DOCSTATUS_NotRecommended.equals(currentStatus))
			{
				line.setZZ_DocStatus(X_ZZ_QAAuditAllocations.ZZ_DOCSTATUS_NotApproved);
				line.setZZ_ApprovedBy_ID(userId);
				line.setZZ_Date_Not_Approved(now);
				line.setZZ_ApprovedDate(null);
				line.saveEx();
				notApprovedCount++;
			}
			else
			{
				unchangedCount++;
			}
		}

		StringBuilder details = new StringBuilder();
		if (approvedCount > 0)
		{
			details.append("Approved: ").append(approvedCount);
		}
		if (notApprovedCount > 0)
		{
			if (details.length() > 0)
				details.append(", ");
			details.append("Not Approved: ").append(notApprovedCount);
		}
		if (unchangedCount > 0)
		{
			if (details.length() > 0)
				details.append(", ");
			details.append("Unchanged: ").append(unchangedCount);
		}

		StringBuilder message = new StringBuilder("Process completed. ")
																		.append("Updated status for ").append(approvedCount + notApprovedCount).append(
																																						" allocation lines.");
		if (details.length() > 0)
		{
			message.append(" (").append(details).append(")");
		}

		return message.toString();
	}
}
