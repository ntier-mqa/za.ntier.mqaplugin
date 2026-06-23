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

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (para.getParameter() == null)
			{
				continue;
			}

			if (name.equalsIgnoreCase("Recommended") || name.equalsIgnoreCase("ZZ_IsRecommended"))
			{
				p_Recommended = para.getParameterAsString();
			}
			else if (name.equalsIgnoreCase("Comments") || name.equalsIgnoreCase("ZZ_Comments"))
			{
				p_Comments = para.getParameterAsString();
			}
			else
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
			int colIndex = po.get_ColumnIndex("ZZ_IsRecommended");
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

		if (p_Comments != null)
		{
			int colIndex = po.get_ColumnIndex("Comments");
			if (colIndex >= 0)
			{
				po.set_ValueNoCheck(po.get_ColumnName(colIndex), p_Comments);
				updated = true;
			}
			else
			{
				log.warning("Could not find column for Comments.");
			}
		}

		if (updated)
		{
			po.saveEx();
			return "Updated successfully.";
		}

		return "No updates were made.";
	}

}
