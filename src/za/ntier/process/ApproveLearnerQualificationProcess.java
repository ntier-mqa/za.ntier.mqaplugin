package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import za.co.ntier.api.model.X_ZZLearnerLearnership;
import org.compiere.model.MSequence;
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

		int verifiedCount = 0;
		int completedCount = 0;
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
										String currentStatus = (String) record.get_Value(X_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus);

										if (X_ZZLearnerLearnership.ZZ_DOCSTATUS_Draft.equals(currentStatus))
										{
											record.set_ValueOfColumn(	X_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus,
																		X_ZZLearnerLearnership.ZZ_DOCSTATUS_Verified);
											record.saveEx();
											verifiedCount++;
										}
										else if (X_ZZLearnerLearnership.ZZ_DOCSTATUS_Verified.equals(currentStatus))
										{
											// Check if a certificate number already exists so we
											// don't overwrite it on reprint/re-approval
											String existingCertNo = (String) record.get_Value(X_ZZLearnerLearnership.COLUMNNAME_ZZCertificateNumber);
											if (existingCertNo == null || existingCertNo.trim().isEmpty())
											{
												// Get the full formatted sequence (including
												// Prefix) directly from the Sequence window
												int seqId = DB.getSQLValue(	get_TrxName(),
																			"SELECT AD_Sequence_ID FROM AD_Sequence WHERE Name='ZZCertificateNumber' AND AD_Client_ID IN (0,?)",
																			getAD_Client_ID());
												if (seqId > 0)
												{
													MSequence seq = new MSequence(getCtx(), seqId, get_TrxName());
													String certNo = MSequence.getDocumentNoFromSeq(seq, get_TrxName(), record);
													record.set_ValueOfColumn(X_ZZLearnerLearnership.COLUMNNAME_ZZCertificateNumber, certNo);
												}
											}

											// The status of the qual will change to ‘Completed’
											record.set_ValueOfColumn(	X_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus,
																		X_ZZLearnerLearnership.ZZ_DOCSTATUS_Completed);
											
											if (record.get_Value(X_ZZLearnerLearnership.COLUMNNAME_ZZApprovalDate) == null)
											{
												record.set_ValueOfColumn(	X_ZZLearnerLearnership.COLUMNNAME_ZZApprovalDate, 
																			new Timestamp(System.currentTimeMillis()));
											}

											record.saveEx();
											completedCount++;
										}
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

		if (verifiedCount == 0 && completedCount == 0)
		{
			return "No records were selected or updated.";
		}

		StringBuilder msg = new StringBuilder();
		if (verifiedCount > 0)
		{
			msg.append(verifiedCount).append(" record(s) changed to Verified. ");
		}
		if (completedCount > 0)
		{
			msg.append(completedCount).append(" record(s) changed to Completed.");
		}

		return msg.toString().trim();
	}
}
