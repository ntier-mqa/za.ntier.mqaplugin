package za.ntier.process;

import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.co.ntier.api.model.I_ZZLinkAssessorQualification;
import za.co.ntier.api.model.I_ZZLinkAssessorSkillsProgramme;

@Process(name = "za.ntier.process.UpdateRecommendOrComment")
public class UpdateRecommendOrComment extends SvrProcess
{

	private String	p_Recommended	= null;
	private String	p_Comments		= null;
	private String	p_Reason		= null;

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();

			if (name.equalsIgnoreCase("ZZ_IsRecommend"))
			{
				p_Recommended = para.getParameterAsString();
			}
			else if (name.equalsIgnoreCase("Comments"))
			{
				p_Comments = para.getParameterAsString();
			}
			else if (name.equalsIgnoreCase("Reason"))
			{
				p_Reason = para.getParameterAsString();
			}
			else if (para.getParameter() != null)
			{
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int recordId = getRecord_ID();
		int tableId = getTable_ID();

		if (recordId <= 0 || tableId <= 0)
		{
			return "Error: No record selected.";
		}

		MTable table = MTable.get(getCtx(), tableId);
		if (table == null)
		{
			return "Error: Table not found.";
		}

		String tableName = table.getTableName();
		String baseTableName = tableName;

		// Dynamically determine the base table by stripping the view suffix
		if (tableName.toLowerCase().endsWith("_v"))
		{
			baseTableName = tableName.substring(0, tableName.length() - 2);
		}

		if (!baseTableName.equalsIgnoreCase(I_ZZLinkAssessorQualification.Table_Name)
			&& !baseTableName.equalsIgnoreCase(I_ZZLinkAssessorSkillsProgramme.Table_Name))
		{
			return "Process is intended to run from Assessor Qualification or Skills Programme only.";
		}

		MTable baseTable = MTable.get(getCtx(), baseTableName);
		if (baseTable == null)
		{
			return "Error: Base table " + baseTableName + " not found in dictionary.";
		}

		PO po = baseTable.getPO(recordId, get_TrxName());
		if (po == null)
		{
			return "Error: Record not found in base table " + baseTableName + " with ID " + recordId;
		}

		boolean updated = false;

		if (p_Recommended != null)
		{
			int colIndex = po.get_ColumnIndex(I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended);
			if (colIndex >= 0)
			{
				po.set_ValueNoCheck(po.get_ColumnName(colIndex), p_Recommended);
				updated = true;
			}
			else
			{
				log.warning("Could not find column for ZZ_IsRecommended.");
			}
		}
		else
		{
			// Clear Recommended if it's left empty
			int colIndex = po.get_ColumnIndex(I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended);
			if (colIndex >= 0)
			{
				po.set_ValueNoCheck(po.get_ColumnName(colIndex), null);
				updated = true;
			}
		}

		// Always update Comments, clearing if both fields are empty
		int colIndex = po.get_ColumnIndex(I_ZZLinkAssessorQualification.COLUMNNAME_Comments);
		if (colIndex >= 0)
		{
				StringBuilder finalComment = new StringBuilder();
				
				if (p_Reason != null && !p_Reason.trim().isEmpty())
				{
					finalComment.append(p_Reason.trim());
				}
				
				if (p_Comments != null && !p_Comments.trim().isEmpty())
				{
					if (finalComment.length() > 0)
					{
						finalComment.append("\n");
					}
					finalComment.append(p_Comments.trim());
				}
				
				po.set_ValueNoCheck(po.get_ColumnName(colIndex), finalComment.length() > 0 ? finalComment.toString() : null);
				updated = true;
			}
			else
			{
				log.warning("Could not find column for Comments.");
			}

		if (updated)
		{
			po.saveEx();
			return "Updated successfully.";
		}

		return "No updates were made.";
	}

}
