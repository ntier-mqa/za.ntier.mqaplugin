package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MSysConfig;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.compiere.util.Trx;
import za.co.ntier.api.model.X_ZZCompletedAssessments_v;

@Process(name = "za.co.ntier.wsp_atr.process.GenerateSORProcess")
public class GenerateSORProcess extends SvrProcess
{
	private static final String MQA_REPORT_JRXML_PATH = "MQA_REPORT_JRXML_PATH";

	@Override
	protected void prepare()
	{
		// No parameters to prepare, we rely directly on getRecord_ID()
	}

	@Override
	protected String doIt() throws Exception
	{
		int recordId = getRecord_ID();
		if (recordId <= 0)
		{
			throw new Exception("No Assessment Record ID provided.");
		}

		X_ZZCompletedAssessments_v assessment = new X_ZZCompletedAssessments_v(getCtx(), recordId, get_TrxName());
		int legacyLearnershipId = assessment.getZZLearnerLearnership_ID();
		int legacySkillsId = assessment.getZZLearnerSkillsProgramme_ID();

		String reportPath = MSysConfig.getValue(MQA_REPORT_JRXML_PATH, "/za/co/ntier/wsp_atr/report/jrxmls/", Env.getAD_Client_ID(getCtx()));
		if (!reportPath.endsWith("/"))
			reportPath += "/";

		HashMap<String, Object> params = new HashMap<>();
		params.put("RECORD_ID", recordId);

		String resourceDir = reportPath;
		if (resourceDir.startsWith("/"))
		{
			resourceDir = resourceDir.substring(1);
		}
		params.put("RESOURCE_DIR", resourceDir);

		String jasperFile = null;
		if (legacyLearnershipId > 0)
		{
			jasperFile = reportPath + "SOR_LegacyLearnership.jasper";
		}
		else if (legacySkillsId > 0)
		{
			jasperFile = reportPath + "SOR_LegacySkillsProgramme.jasper";
		}

		if (jasperFile == null)
		{
			throw new Exception("Could not determine program type for record: " + recordId);
		}

		File tempPdfFile = null;
		try (InputStream jasperStream = GenerateSORProcess.class.getResourceAsStream(jasperFile))
		{
			if (jasperStream == null)
			{
				throw new Exception("Could not find the Jasper report template on classpath: " + jasperFile);
			}

			params.put(JRParameter.REPORT_CLASS_LOADER, GenerateSORProcess.class.getClassLoader());

			JasperPrint print = JasperFillManager.fillReport(jasperStream, params, Trx.get(get_TrxName(), false).getConnection());
			if (print != null && print.getPages().size() > 0)
			{
				tempPdfFile = File.createTempFile("SOR_" + recordId + "_", ".pdf");
				byte[] pdfBytes = JasperExportManager.exportReportToPdf(print);
				try (FileOutputStream fos = new FileOutputStream(tempPdfFile))
				{
					fos.write(pdfBytes);
				}

				getProcessInfo().setExportFile(tempPdfFile);
			}
			else
			{
				throw new Exception("No pages were generated for this SOR.");
			}
		}

		return "SOR Generated Successfully.";
	}
}
