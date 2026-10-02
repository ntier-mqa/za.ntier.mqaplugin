package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.sql.Timestamp;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MAttachment;
import org.compiere.model.MSysConfig;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import za.co.ntier.api.model.I_ZZCompletedAssessments_v;
import za.co.ntier.api.model.X_ZZLearnerLearnership;
import za.co.ntier.api.model.X_ZZLearnerSkillsProgramme;
import za.co.ntier.api.model.X_ZZ_CertificateReprints;
import za.co.ntier.api.model.I_ZZLearnerLearnership;

@Process(name = "za.co.ntier.wsp_atr.process.PrintCertificates")
public class PrintCertificates extends SvrProcess
{
	private static final String MQA_REPORT_JRXML_PATH = "MQA_REPORT_JRXML_PATH";

	private String				zzReIssueReason			= null;
	private int					zzRequestedBy_ID		= 0;
	private String				zzCertifiedIDCopy		= null;
	private String				zzAffidavit				= null;
	private boolean				isReprintProcess		= false;

	private void attachFile(MAttachment attachment, String filePath, String prefix, int recordId)
	{
		if (filePath == null)
			return;

		File file = new File(filePath);
		if (!file.exists())
			return;

		String name = file.getName();
		String ext = name.lastIndexOf('.') > 0 ? name.substring(name.lastIndexOf('.')) : "";
		File renamedFile = new File(file.getParent(), prefix + "_" + recordId + ext);

		if (file.renameTo(renamedFile))
		{
			attachment.addEntry(renamedFile);
		}
		else
		{
			attachment.addEntry(file);
		}
	}

	@Override
	protected void prepare()
	{
		if (getProcessInfo().getTitle() != null && getProcessInfo().getTitle().toLowerCase().contains("reprint"))
		{
			isReprintProcess = true;
		}

		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals(X_ZZ_CertificateReprints.COLUMNNAME_ZZReIssueReason))
			{
				zzReIssueReason = (String) para[i].getParameter();
				isReprintProcess = true;
			}
			else if (name.equals(X_ZZ_CertificateReprints.COLUMNNAME_ZZ_RequestedBy_ID))
			{
				zzRequestedBy_ID = para[i].getParameterAsInt();
				isReprintProcess = true;
			}
			else if (name.equals("ZZCertifiedIDCopy"))
			{
				zzCertifiedIDCopy = (String) para[i].getParameter();
				isReprintProcess = true;
			}
			else if (name.equals("ZZAffidavit"))
			{
				zzAffidavit = (String) para[i].getParameter();
				isReprintProcess = true;
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
		int pInstanceId = getAD_PInstance_ID();
		if (pInstanceId <= 0)
		{
			return "No records selected.";
		}
		
		if (isReprintProcess)
		{
			int selectionCount = DB.getSQLValue(get_TrxName(), "SELECT COUNT(1) FROM T_Selection WHERE AD_PInstance_ID = ?", pInstanceId);
			if (selectionCount > 1)
			{
				throw new IllegalArgumentException("You can only reprint one certificate at a time. Please select only one record.");
			}
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

				Timestamp dateOfIssue = null;
				if (learnershipId > 0)
				{
					X_ZZLearnerLearnership learnership = new X_ZZLearnerLearnership(getCtx(), learnershipId, get_TrxName());
					boolean isPrinted = learnership.isPrinted();

					if (!isReprintProcess && isPrinted)
					{
						throw new IllegalArgumentException("A selected certificate is already printed. Please use the 'Reprint' button.");
					}
					if (isReprintProcess && !isPrinted)
					{
						throw new IllegalArgumentException("A selected certificate has not been printed yet. Please use the normal 'Print' button.");
					}

					dateOfIssue = learnership.getZZDateOfIssue();
					if (dateOfIssue == null)
					{
						dateOfIssue = new Timestamp(System.currentTimeMillis());
						learnership.setZZDateOfIssue(dateOfIssue);
						learnership.setZZIssueUser_ID(getAD_User_ID());
					}
					learnership.setIsPrinted(true);
					learnership.saveEx();
				}
				else if (skillsId > 0)
				{
					X_ZZLearnerSkillsProgramme skillsProg = new X_ZZLearnerSkillsProgramme(getCtx(), skillsId, get_TrxName());
					boolean isPrinted = skillsProg.isPrinted();

					if (!isReprintProcess && isPrinted)
					{
						throw new IllegalArgumentException("A selected certificate is already printed. Please use the 'Reprint' button.");
					}
					if (isReprintProcess && !isPrinted)
					{
						throw new IllegalArgumentException("A selected certificate has not been printed yet. Please use the normal 'Print' button.");
					}

					dateOfIssue = skillsProg.getZZDateOfIssue();
					if (dateOfIssue == null)
					{
						dateOfIssue = new Timestamp(System.currentTimeMillis());
						skillsProg.setZZDateOfIssue(dateOfIssue);
						skillsProg.setZZIssueUser_ID(getAD_User_ID());
					}
					skillsProg.setIsPrinted(true);
					skillsProg.saveEx();
				}

				HashMap<String, Object> params = new HashMap<>();
				params.put("RECORD_ID", recordId);
				if (dateOfIssue != null)
				{
					params.put(I_ZZLearnerLearnership.COLUMNNAME_ZZDateOfIssue, dateOfIssue);
				}

				JasperPrint print = null;

				String reportPath = MSysConfig.getValue(MQA_REPORT_JRXML_PATH, "/za/co/ntier/wsp_atr/report/jrxmls/", Env.getAD_Client_ID(getCtx()));
				if (!reportPath.endsWith("/")) reportPath += "/";

				if (learnershipId > 0)
				{
					// It's a Learnership
					try (InputStream jasperStream = PrintCertificates.class.getResourceAsStream(reportPath + "Learnership_Certificate.jasper"))
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
					try (InputStream jasperStream = PrintCertificates.class.getResourceAsStream(reportPath + "SkillProgramme_Certificate.jasper"))
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

					if (isReprintProcess)
					{
						X_ZZ_CertificateReprints reprint = new X_ZZ_CertificateReprints(getCtx(), 0, get_TrxName());
						if (learnershipId > 0)
						{
							reprint.setZZLearnerLearnership_ID(learnershipId);
						}
						else if (skillsId > 0)
						{
							reprint.setZZLearnerSkillsProgramme_ID(skillsId);
						}
						reprint.setZZReprintDate(new Timestamp(System.currentTimeMillis()));
						reprint.setZZReprintedBy(getAD_User_ID());

						if (zzReIssueReason != null)
						{
							reprint.setZZReIssueReason(zzReIssueReason);
						}
						if (zzRequestedBy_ID > 0)
						{
							reprint.setZZ_RequestedBy_ID(zzRequestedBy_ID);
						}
						
						reprint.setIsCertifiedIDAttached(zzCertifiedIDCopy != null);
						reprint.setIsAffidavitAttached(zzAffidavit != null);

						reprint.saveEx();

						// Commit to allow MAttachment to validate the newly inserted record
						commitEx();

						if (zzCertifiedIDCopy != null || zzAffidavit != null)
						{
							MAttachment attachment = reprint.createAttachment();

							attachFile(attachment, zzCertifiedIDCopy, "CertifiedIDCopy", recordId);
							attachFile(attachment, zzAffidavit, "Affidavit", recordId);

							if (attachment.getEntryCount() > 0)
							{
								attachment.saveEx();
							}
						}
					}
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

		return generatedPdfs.size() + " Certificate(s) generated.";
	}
}
