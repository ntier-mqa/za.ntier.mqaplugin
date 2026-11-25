package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MSysConfig;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

/**
 * Renders the Mandatory Grant Statement for one company as a PDF export file.
 * Used by the SDF front end (Linked Organisations → Statement); the backend report is unchanged.
 * Record_ID is the company's C_BPartner_ID; parameters are DateFrom and DateTo.
 */
@Process(name = "za.co.ntier.wsp_atr.process.GenerateMandatoryGrantStatement")
public class GenerateMandatoryGrantStatement extends SvrProcess
{
	public static final String PARAM_DateFrom = "DateFrom";
	public static final String PARAM_DateTo = "DateTo";

	private static final String MQA_REPORT_JRXML_PATH = "MQA_REPORT_JRXML_PATH";
	private static final String REPORT_NAME = "Mandatory_Grant_Statement_Report.jasper";

	private Timestamp dateFrom;
	private Timestamp dateTo;

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			if (PARAM_DateFrom.equals(para.getParameterName()))
				dateFrom = para.getParameterAsTimestamp();
			else if (PARAM_DateTo.equals(para.getParameterName()))
				dateTo = para.getParameterAsTimestamp();
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int bpartnerId = getRecord_ID();
		if (bpartnerId <= 0)
			throw new Exception("No company was provided for the statement.");
		if (dateFrom == null || dateTo == null)
			throw new Exception("Start Date and End Date are required.");
		if (dateFrom.after(dateTo))
			throw new Exception("Start Date must be on or before End Date.");

		// The report compares each levy month as the 1st of that month, so a mid-month
		// start date would drop its own month. Round it down to include it.
		LocalDate from = dateFrom.toLocalDateTime().toLocalDate().withDayOfMonth(1);

		String reportPath = MSysConfig.getValue(MQA_REPORT_JRXML_PATH, "/za/co/ntier/wsp_atr/report/jrxmls/", Env.getAD_Client_ID(getCtx()));
		if (!reportPath.endsWith("/"))
			reportPath += "/";
		String resourceDir = reportPath.startsWith("/") ? reportPath.substring(1) : reportPath;

		HashMap<String, Object> params = new HashMap<>();
		params.put("P_C_BPartner_ID", bpartnerId);
		params.put("P_DateFrom", Timestamp.valueOf(from.atStartOfDay()));
		params.put("P_DateTo", dateTo);
		params.put("RESOURCE_DIR", resourceDir);
		params.put(JRParameter.REPORT_CLASS_LOADER, GenerateMandatoryGrantStatement.class.getClassLoader());

		String jasperFile = reportPath + REPORT_NAME;
		try (InputStream jasperStream = GenerateMandatoryGrantStatement.class.getResourceAsStream(jasperFile))
		{
			if (jasperStream == null)
				throw new Exception("Could not find the Jasper report template on classpath: " + jasperFile);

			JasperPrint print = JasperFillManager.fillReport(jasperStream, params, Trx.get(get_TrxName(), false).getConnection());
			if (print == null || print.getPages().isEmpty())
				throw new Exception("No levy data was found for the selected period.");

			File pdf = File.createTempFile("GrantStatement_" + bpartnerId + "_", ".pdf");
			Files.write(pdf.toPath(), JasperExportManager.exportReportToPdf(print));
			getProcessInfo().setExportFile(pdf);
		}

		return "Grant Statement generated.";
	}
}
