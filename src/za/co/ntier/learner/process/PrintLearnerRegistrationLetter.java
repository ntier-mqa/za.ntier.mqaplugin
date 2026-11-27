package za.co.ntier.learner.process;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Util;

import za.co.ntier.api.model.I_ZZLearner;
import za.ntier.utils.ZZLearnerUtil;

/**
 * Generates individual Registration Confirmation Letter PDFs for learners selected
 * on the Learner Dashboard.
 */
@Process(name = "za.co.ntier.learner.process.PrintLearnerRegistrationLetter")
public class PrintLearnerRegistrationLetter extends SvrProcess
{
	public static final String PARAM_LEARNER_IDS = "ZZLearner_IDs";

	private String p_LearnerIds = "";

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			if (para.getParameter() != null && PARAM_LEARNER_IDS.equals(para.getParameterName()))
			{
				p_LearnerIds = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		List<Integer> learnerIds = parseLearnerIds(p_LearnerIds);
		if (learnerIds.isEmpty() && getRecord_ID() > 0)
		{
			learnerIds.add(getRecord_ID());
		}
		if (learnerIds.isEmpty())
		{
			throw new AdempiereException("Please select at least one learner to print the Registration Letter.");
		}

		List<File> pdfFiles = ZZLearnerUtil.generateRegistrationConfirmationLetterPdfs(getCtx(), learnerIds, get_TrxName());
		getProcessInfo().setSerializableObject(new ArrayList<>(pdfFiles));
		getProcessInfo().setExportFile(pdfFiles.get(0));
		getProcessInfo().setExportFileExtension("pdf");
		return "Generated " + pdfFiles.size() + " Registration Confirmation Letter PDF(s).";
	}

	private static List<Integer> parseLearnerIds(String rawIds)
	{
		if (Util.isEmpty(rawIds, true))
		{
			return new ArrayList<>();
		}

		Set<Integer> ids = new LinkedHashSet<>();
		for (String token : rawIds.split(","))
		{
			String trimmed = token.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			try
			{
				int id = Integer.parseInt(trimmed);
				if (id > 0)
				{
					ids.add(id);
				}
			}
			catch (NumberFormatException e)
			{
				throw new AdempiereException("Invalid " + I_ZZLearner.COLUMNNAME_ZZLearner_ID + " value: " + trimmed);
			}
		}
		return new ArrayList<>(ids);
	}
}
