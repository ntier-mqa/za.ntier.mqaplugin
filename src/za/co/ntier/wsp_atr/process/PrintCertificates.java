package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.Base64;
import java.nio.file.Files;

import za.co.ntier.api.model.I_ZZCompletedAssessments_v;

import org.adempiere.base.annotation.Process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.zkoss.zul.Filedownload;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

@Process(name = "za.co.ntier.wsp_atr.process.PrintCertificates")
public class PrintCertificates extends SvrProcess
{

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else
			{
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();
		if (pInstanceId <= 0)
		{
			return "No records selected.";
		}

		String sql = "SELECT ca." + I_ZZCompletedAssessments_v.COLUMNNAME_ZZCompletedAssessments_v_ID 
						+ ", ca." + I_ZZCompletedAssessments_v.COLUMNNAME_ZZLearnerLearnership_ID 
						+ ", ca." + I_ZZCompletedAssessments_v.COLUMNNAME_ZZLearnerSkillsProgramme_ID + " "
						+ "FROM T_Selection s "
						+ "JOIN " + I_ZZCompletedAssessments_v.Table_Name + " ca ON s.T_Selection_ID = ca." + I_ZZCompletedAssessments_v.COLUMNNAME_ZZCompletedAssessments_v_ID + " "
						+ "WHERE s.AD_PInstance_ID = ?";

		List<File> generatedPdfs = new ArrayList<>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, pInstanceId);
			rs = pstmt.executeQuery();

			while (rs.next())
			{
				int recordId = rs.getInt(I_ZZCompletedAssessments_v.COLUMNNAME_ZZCompletedAssessments_v_ID);
				int learnershipId = rs.getInt(I_ZZCompletedAssessments_v.COLUMNNAME_ZZLearnerLearnership_ID);
				int skillsId = rs.getInt(I_ZZCompletedAssessments_v.COLUMNNAME_ZZLearnerSkillsProgramme_ID);

				HashMap<String, Object> params = new HashMap<>();
				params.put("RECORD_ID", recordId);

				JasperPrint print = null;

				if (learnershipId > 0)
				{
					// It's a Learnership
					try (InputStream jasperStream = PrintCertificates.class.getResourceAsStream("/za/co/ntier/wsp_atr/report/jrxmls/Learnership_Certificate.jasper"))
					{
						if (jasperStream != null)
						{
							print = JasperFillManager.fillReport(jasperStream, params, Trx.get(get_TrxName(), false).getConnection());
						}
						else
						{
							log.warning("Could not find Learnership_Certificate.jasper on classpath");
						}
					}
				}
				else if (skillsId > 0)
				{
					// It's a Skills Programme
					try (InputStream jasperStream = PrintCertificates.class.getResourceAsStream("/za/co/ntier/wsp_atr/report/jrxmls/SkillProgramme_Certificate.jasper"))
					{
						if (jasperStream != null)
						{
							print = JasperFillManager.fillReport(jasperStream, params, Trx.get(get_TrxName(), false).getConnection());
						}
						else
						{
							log.warning("Could not find SkillProgramme_Certificate.jasper on classpath");
						}
					}
				}

				if (print != null && print.getPages().size() > 0)
				{
					File tempPdf = File.createTempFile("Certificate_" + recordId + "_", ".pdf");
					JasperExportManager.exportReportToPdfFile(print, tempPdf.getAbsolutePath());
					generatedPdfs.add(tempPdf);
				}
			}
		}
		finally
		{
			DB.close(rs, pstmt);
		}

		if (generatedPdfs.isEmpty())
		{
			return "No certificates were generated";
		}

		File finalPdf = null;
		if (generatedPdfs.size() == 1)
		{
			finalPdf = generatedPdfs.get(0);
		}
		else
		{
			finalPdf = File.createTempFile("Certificates_Merged_", ".pdf");
			try
			{
				Util.mergePdf(generatedPdfs, finalPdf);
			}
			catch (Exception e)
			{
				log.severe("Failed to merge PDFs: " + e.getMessage());
				return "Error merging certificates.";
			}
		}

		processUI.showReports(Arrays.asList(finalPdf));

		return generatedPdfs.size() + " Certificate(s) generated successfully.";
	}
}
