package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import za.co.ntier.api.model.X_ZZLearnerLearnership;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@Process(name = "za.ntier.process.ApproveLearnerQualificationProcess")
public class ApproveLearnerQualificationProcess extends SvrProcess
{

	@Override
	protected void prepare()
	{
		// No parameters to prepare for now
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();

		// Ensure it was run from an Info Window / Selection
		if (pInstanceId <= 0)
		{
			return "Process must be executed from an Info Window selection.";
		}

		String sqlSelect = "SELECT DISTINCT T_Selection_ID FROM ("
							+ "  SELECT T_Selection_ID FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?"
							+ "  UNION "
							+ "  SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?"
							+ ") x";

		int count = 0;
		try (PreparedStatement pstmt = DB.prepareStatement(sqlSelect, get_TrxName()))
		{
			pstmt.setInt(1, pInstanceId);
			pstmt.setInt(2, pInstanceId);

			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					int viewId = rs.getInt(1);

					String viewSql = "SELECT ad_table_id, record_id FROM zzcompletedassessments_v WHERE zzcompletedassessments_v_id = ?";
					try (PreparedStatement pstmtView = DB.prepareStatement(viewSql, get_TrxName()))
					{
						pstmtView.setInt(1, viewId);
						try (ResultSet rsView = pstmtView.executeQuery())
						{
							if (rsView.next())
							{
								int ad_table_id = rsView.getInt(1);
								int record_id = rsView.getInt(2);

								if (ad_table_id > 0 && record_id > 0)
								{
									PO record = MTable.get(getCtx(), ad_table_id).getPO(record_id, get_TrxName());
									if (record != null)
									{
										// The status of the qual will change to ‘Completed’
										record.set_ValueOfColumn(X_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus, X_ZZLearnerLearnership.ZZ_DOCSTATUS_Completed);
										record.saveEx();
										count++;
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error reading Info Window selection", e);
			return "Error: " + e.getMessage();
		}

		if (count == 0)
		{
			return "No records were selected or updated.";
		}

		return "Successfully Approved " + count + " Learner Qualification(s)!";
	}
}
