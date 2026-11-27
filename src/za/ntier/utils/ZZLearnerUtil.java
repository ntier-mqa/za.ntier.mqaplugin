package za.ntier.utils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_R_MailText;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.EMail;
import org.compiere.util.Util;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import za.co.ntier.api.model.I_ZZLearner;
import za.co.ntier.api.model.I_ZZLearnerDGProgramme;
import za.co.ntier.api.model.I_ZZLearnerLearnership;
import za.co.ntier.api.model.I_ZZLearnerQCTOArtisans;
import za.co.ntier.api.model.I_ZZLearnerQCTOLearnership;
import za.co.ntier.api.model.I_ZZLearnerQCTOSkillsProgramme;
import za.co.ntier.api.model.I_ZZLearnerSkillsProgramme;
import za.co.ntier.api.model.I_ZZLearnership;
import za.co.ntier.api.model.X_ZZLearner;
import za.co.ntier.api.model.X_ZZLearnerDGProgramme;
import za.co.ntier.api.model.X_ZZLearnerLearnership;
import za.co.ntier.api.model.X_ZZLearnerQCTOArtisans;
import za.co.ntier.api.model.X_ZZLearnerQCTOLearnership;
import za.co.ntier.api.model.X_ZZLearnerQCTOSkillsProgramme;
import za.co.ntier.api.model.X_ZZLearnerSkillsProgramme;
import za.co.ntier.api.model.X_ZZLearnership;
import za.co.ntier.api.model.X_ZZPerson;
import za.co.ntier.api.model.X_ZZQctoLearnership;
import za.co.ntier.api.model.X_ZZQctoSkillsProgramme;
import za.co.ntier.api.model.X_ZZSkillsProgramme;
import za.co.ntier.api.model.X_ZZ_Program_Master_Data;

/**
 * Utility class for Learner workflow operations, sequence number generation,
 * confirmation letter PDF generation, and email dispatch on intervention tables.
 */
public class ZZLearnerUtil
{
	private static final CLogger log = CLogger.getCLogger(ZZLearnerUtil.class);

	public static final String LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU = "eac216ef-5331-4d12-980d-7d7710b9829c";
	public static final String LEARNER_QUERY_MAIL_TEMPLATE_UU = "ba58e3ff-6b42-4f4c-be01-cfbe1255803e";
	public static final String LEARNER_REGISTRATION_LETTER_CC = "LEARNER_REGISTRATION_LETTER_CC";

	private static final List<String> INTERVENTION_TABLES = List.of(
			I_ZZLearnerDGProgramme.Table_Name,
			I_ZZLearnerLearnership.Table_Name,
			I_ZZLearnerSkillsProgramme.Table_Name,
			I_ZZLearnerQCTOLearnership.Table_Name,
			I_ZZLearnerQCTOArtisans.Table_Name,
			I_ZZLearnerQCTOSkillsProgramme.Table_Name);

	/**
	 * Common beforeSave logic for learner intervention tables.
	 * Sets the registration date and generates ReferenceNo directly on the intervention record when status changes to Registered.
	 */
	public static boolean onInterventionBeforeSave(PO prog, boolean newRecord)
	{
		if (prog == null)
		{
			return true;
		}

		if (prog.is_ValueChanged(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus) || newRecord)
		{
			String status = prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus);
			if (newRecord && Util.isEmpty(status))
			{
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus) >= 0)
				{
					prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus, X_ZZLearnerDGProgramme.ZZ_DOCSTATUS_Draft);
					status = X_ZZLearnerDGProgramme.ZZ_DOCSTATUS_Draft;
				}
			}
			if (X_ZZLearner.ZZ_DOCSTATUS_Registered.equals(status))
			{
				Timestamp now = new Timestamp(System.currentTimeMillis());

				// 1. Set ZZRegistrationDate if column exists
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) >= 0)
				{
					if (prog.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) == null)
					{
						prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate, now);
					}
				}

				// 2. Set ZZ_DateRegistered if column exists
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered) >= 0)
				{
					if (prog.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered) == null)
					{
						prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered, now);
					}
				}

				// 3. Set ReferenceNo directly on the intervention record if column exists
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo) >= 0)
				{
					if (Util.isEmpty(prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo)))
					{
						int learnerId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID);
						X_ZZLearner learner = (learnerId > 0) ? new X_ZZLearner(prog.getCtx(), learnerId, prog.get_TrxName()) : null;
						String id = (learner != null) ? getIDOrPassportNo(learner) : "";
						if (Util.isEmpty(id) && learner != null)
						{
							id = String.valueOf(learner.get_ID());
						}
						String seqNo = getNextSequenceNo(prog, learner);
						String refNo = !Util.isEmpty(seqNo) ? (id + "/" + seqNo) : id;
						prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo, refNo);
					}
				}
			}
			else if (X_ZZLearner.ZZ_DOCSTATUS_NotApproved.equals(status) || X_ZZLearner.ZZ_DOCSTATUS_NotRecommended.equals(status))
			{
				Timestamp now = new Timestamp(System.currentTimeMillis());
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateNotRegistered) >= 0)
				{
					if (prog.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateNotRegistered) == null)
					{
						prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateNotRegistered, now);
					}
				}
			}
		}
		return true;
	}

	/**
	 * Common afterSave logic for learner intervention tables.
	 * On Registered: sends confirmation email with PDF to SDP.
	 * On Query: sends query email to SDP.
	 */
	public static boolean onInterventionAfterSave(PO prog, boolean newRecord, boolean success)
	{
		if (success && prog.is_ValueChanged(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus))
		{
			String status = prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus);
			int learnerId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID);
			X_ZZLearner learner = (learnerId > 0) ? new X_ZZLearner(prog.getCtx(), learnerId, prog.get_TrxName()) : null;
			if (X_ZZLearner.ZZ_DOCSTATUS_Registered.equals(status))
			{
				processInterventionRegistered(learner, prog);
			}
			else if (X_ZZLearner.ZZ_DOCSTATUS_Query.equals(status))
			{ 
				String reason = null;
				if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason) >= 0)
				{
					reason = prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason);
				}
				processInterventionQuery(learner, prog, reason);
			}
		}
		return true;
	}

	/**
	 * Handles workflow transition to Registered on an intervention record.
	 * Ensures ReferenceNo and ZZRegistrationDate are set on the intervention record and sends confirmation email with landscape PDF.
	 */
	public static void processInterventionRegistered(X_ZZLearner learner, PO prog)
	{
		if (prog == null)
		{
			return;
		}

		boolean modified = false;

		// 1. Auto-generate ReferenceNo on intervention record if not already present
		if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo) >= 0)
		{
			if (Util.isEmpty(prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo)))
			{
				String id = (learner != null) ? getIDOrPassportNo(learner) : getIDOrPassportNo(prog, null);
				if (Util.isEmpty(id) && learner != null)
				{
					id = String.valueOf(learner.get_ID());
				}
				String seqNo = getNextSequenceNo(prog, learner);
				String refNo = !Util.isEmpty(seqNo) ? (id + "/" + seqNo) : id;
				prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo, refNo);
				modified = true;
			}
		}

		// 2. Set ZZRegistrationDate and ZZ_DateRegistered on programme record if columns exist
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) >= 0)
		{
			if (prog.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) == null)
			{
				prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate, now);
				modified = true;
			}
		}
		if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered) >= 0)
		{
			if (prog.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered) == null)
			{
				prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered, now);
				modified = true;
			}
		}

		if (modified)
		{
			prog.saveEx(prog.get_TrxName());
		}

		// 3. Send confirmation email to SDP with landscape PDF
		sendLearnerConfirmationToSDP(learner, prog);
	}

	/**
	 * Handles workflow transition to Query on an intervention record.
	 * Saves the query reason on the intervention and sends query notification email to the SDP.
	 */
	public static void processInterventionQuery(X_ZZLearner learner, PO prog, String reason)
	{
		if (prog == null)
		{
			return;
		}

		if (prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason) >= 0 && !Util.isEmpty(reason))
		{
			if (!reason.equals(prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason)))
			{
				prog.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason, reason);
				prog.saveEx(prog.get_TrxName());
			}
		}

		sendLearnerQueryToSDP(learner, prog);
	}

	/**
	 * Retrieves the next sequence number for the intervention record with fallback to learner table sequence.
	 */
	public static String getNextSequenceNo(PO prog, X_ZZLearner learner)
	{
		String seqNo = null;
		if (prog != null)
		{
			try
			{
				seqNo = MSequence.getDocumentNo(prog.getAD_Client_ID(), prog.get_TableName(), prog.get_TrxName(), prog);
			}
			catch (Exception e)
			{
				log.fine("No sequence configured for " + prog.get_TableName() + ": " + e.getMessage());
			}
		}
		if (Util.isEmpty(seqNo) && learner != null)
		{
			try
			{
				seqNo = MSequence.getDocumentNo(learner.getAD_Client_ID(), learner.get_TableName(), learner.get_TrxName(), learner);
			}
			catch (Exception e)
			{
				log.fine("No sequence configured for " + learner.get_TableName() + ": " + e.getMessage());
			}
		}
		return seqNo;
	}

	/**
	 * Sends Learner Confirmation email to the SDP, attaching the landscape confirmation letter PDF.
	 */
	public static void sendLearnerConfirmationToSDP(PO prog)
	{
		sendLearnerConfirmationToSDP(null, prog);
	}

	/**
	 * Sends Learner Confirmation email to the SDP, attaching the landscape confirmation letter PDF.
	 */
	public static void sendLearnerConfirmationToSDP(X_ZZLearner learner, PO prog)
	{
		try
		{
			MUser sdpUser = resolveSDPUser(learner, prog);
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for intervention record: " + (prog != null ? prog.get_ID() : "null"));
				return;
			}

			Properties ctx = prog != null ? prog.getCtx() : learner.getCtx();
			String trxName = prog != null ? prog.get_TrxName() : learner.get_TrxName();
			int clientId = prog != null ? prog.getAD_Client_ID() : learner.getAD_Client_ID();

			MMailText mailTemplate = new Query(ctx, MMailText.Table_Name,
					I_R_MailText.COLUMNNAME_R_MailText_UU + "=?", trxName)
					.setParameters(LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU)
					.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Confirmation mail template not found for UUID: " + LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU);
				return;
			}

//			mailTemplate.setUser(sdpUser);
//			if (learner != null)
//			{
//				mailTemplate.setPO(learner);
//			}
//			else if (prog != null)
//			{
//				mailTemplate.setPO(prog);
//			}

			File pdfAttachment = generateRegistrationConfirmationLetterPdf(learner, prog);

			String subject = replaceTokens(mailTemplate.getMailHeader(), learner, prog, null);
			String body = replaceTokens(mailTemplate.getMailText(), learner, prog, null);

			sendEmailWithCC(ctx, clientId, trxName, sdpUser.getEMail(),
					subject, body, mailTemplate.isHtml(), pdfAttachment);

			log.info("Learner confirmation email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail()
					+ ") for intervention " + (prog != null ? prog.get_TableName() + "_" + prog.get_ID() : "")
					+ (pdfAttachment != null ? " with attachment " + pdfAttachment.getName() : " without attachment"));
		}
		catch (Exception e)
		{
			log.severe("Failed to send learner confirmation email to SDP: " + e.getMessage());
		}
	}

	/**
	 * Sends Learner Query email with query reason to the SDP.
	 */
	public static void sendLearnerQueryToSDP(PO prog)
	{
		sendLearnerQueryToSDP(null, prog);
	}

	/**
	 * Sends Learner Query email with query reason to the SDP.
	 */
	public static void sendLearnerQueryToSDP(X_ZZLearner learner, PO prog)
	{
		try
		{
			MUser sdpUser = resolveSDPUser(learner, prog);
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for intervention record: " + (prog != null ? prog.get_ID() : "null"));
				return;
			}

			Properties ctx = prog != null ? prog.getCtx() : learner.getCtx();
			String trxName = prog != null ? prog.get_TrxName() : learner.get_TrxName();
			int clientId = prog != null ? prog.getAD_Client_ID() : learner.getAD_Client_ID();

			MMailText mailTemplate = new Query(ctx, MMailText.Table_Name,
					I_R_MailText.COLUMNNAME_R_MailText_UU + "=?", trxName)
					.setParameters(LEARNER_QUERY_MAIL_TEMPLATE_UU)
					.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Query mail template not found for UUID: " + LEARNER_QUERY_MAIL_TEMPLATE_UU);
				return;
			}

//			mailTemplate.setUser(sdpUser);
//			if (learner != null)
//			{
//				mailTemplate.setPO(learner);
//			}
//			else if (prog != null)
//			{
//				mailTemplate.setPO(prog);
//			}

			String reason = getZZReason(learner, prog);
			String subject = replaceTokens(mailTemplate.getMailHeader(), learner, prog, reason);
			String body = replaceTokens(mailTemplate.getMailText(), learner, prog, reason);

			sendEmailWithCC(ctx, clientId, trxName, sdpUser.getEMail(),
					subject, body, mailTemplate.isHtml(), null);

			log.info("Learner query email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail()
					+ ") for intervention " + (prog != null ? prog.get_TableName() + "_" + prog.get_ID() : ""));
		}
		catch (Exception e)
		{
			log.severe("Failed to send learner query email to SDP: " + e.getMessage());
		}
	}

	private record LearnerProgramme(X_ZZLearner learner, PO prog) {}

	/**
	 * Generates the Registration Confirmation Letter in landscape orientation and returns the PDF file.
	 */
	public static File generateRegistrationConfirmationLetterPdf(X_ZZLearner learner, PO prog)
	{
		try
		{
			Properties ctx = prog != null ? prog.getCtx() : learner.getCtx();
			String trxName = prog != null ? prog.get_TrxName() : learner.get_TrxName();
			return createRegistrationLetterPdf(ctx, learner, prog, trxName);
		}
		catch (Exception e)
		{
			log.severe("Failed to generate Registration Confirmation Letter PDF: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Generates one Registration Confirmation Letter PDF per selected learner
	 * using each learner's latest Registered programme.
	 */
	public static List<File> generateRegistrationConfirmationLetterPdfs(Properties ctx, List<Integer> learnerIds, String trxName)
	{
		List<Integer> uniqueIds = learnerIds == null ? List.of() : learnerIds.stream()
				.filter(id -> id != null && id > 0)
				.distinct()
				.toList();
		if (uniqueIds.isEmpty())
		{
			throw new AdempiereException("Please select at least one learner to print the Registration Letter.");
		}

		List<LearnerProgramme> selections = new ArrayList<>();
		List<String> unregisteredLearners = new ArrayList<>();
		for (int learnerId : uniqueIds)
		{
			X_ZZLearner learner = new Query(ctx, I_ZZLearner.Table_Name,
					I_ZZLearner.COLUMNNAME_ZZLearner_ID + "=?", trxName)
					.setParameters(learnerId)
					.setOnlyActiveRecords(true)
					.first();
			if (learner == null)
			{
				throw new AdempiereException("Selected learner (ID=" + learnerId + ") could not be found or is inactive.");
			}

			PO prog = findLatestRegisteredProgramme(learner);
			if (prog == null)
			{
				unregisteredLearners.add(formatLearnerLabel(learner));
			}
			else
			{
				selections.add(new LearnerProgramme(learner, prog));
			}
		}

		if (!unregisteredLearners.isEmpty())
		{
			throw new AdempiereException(
					"The following selected learner(s) do not have a Registered programme: "
							+ String.join(", ", unregisteredLearners));
		}

		List<File> pdfFiles = new ArrayList<>();
		for (LearnerProgramme item : selections)
		{
			File pdfFile;
			try
			{
				pdfFile = createRegistrationLetterPdf(ctx, item.learner(), item.prog(), trxName);
			}
			catch (Exception e)
			{
				throw new AdempiereException("Failed to generate Registration Confirmation Letter PDF: " + e.getMessage(), e);
			}
			if (pdfFile == null || !pdfFile.exists())
			{
				throw new AdempiereException("Failed to generate Registration Confirmation Letter PDF for "
						+ formatLearnerLabel(item.learner()));
			}
			pdfFiles.add(pdfFile);
		}
		return pdfFiles;
	}

	private static File createRegistrationLetterPdf(Properties ctx, X_ZZLearner learner, PO prog, String trxName)
			throws Exception
	{
		int sdpId = resolveSdpId(ctx, learner, prog);
		String sdpName = "";
		String sdpSearchKey = "";
		if (sdpId > 0)
		{
			MBPartner sdpBp = new MBPartner(ctx, sdpId, trxName);
			if (sdpBp.get_ID() > 0)
			{
				sdpName = sdpBp.getName() != null ? sdpBp.getName() : "";
				sdpSearchKey = sdpBp.getValue() != null ? sdpBp.getValue() : "";
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Map<String, Object> jasperParams = new HashMap<>();
		jasperParams.put("SDPName", sdpName);
		jasperParams.put("SDPSearchKey", sdpSearchKey);
		jasperParams.put("DatePrinted", sdf.format(new Date()));
		jasperParams.put("ProgrammeName", resolveProgrammeName(prog));
		jasperParams.put("CompanyName", (sdpName + " " + sdpSearchKey).trim());

		final String reportPath = "/za/co/ntier/wsp_atr/report/jrxmls/";
		URL headerUrl = ZZLearnerUtil.class.getResource(reportPath + "MQA_Address_Logo_Header.png");
		if (headerUrl != null)
		{
			jasperParams.put("HeaderImagePath", headerUrl);
		}
		URL footerUrl = ZZLearnerUtil.class.getResource(reportPath + "Footer_Mining_Future_Skills.png");
		if (footerUrl != null)
		{
			jasperParams.put("FooterImagePath", footerUrl);
		}
		jasperParams.put("RESOURCE_DIR", "za/co/ntier/wsp_atr/report/jrxmls/");
		jasperParams.put(JRParameter.REPORT_CLASS_LOADER, ZZLearnerUtil.class.getClassLoader());

		String idNo = getIDOrPassportNo(prog, learner);
		Timestamp startTs = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZCommencementDate);
		Timestamp endTs = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZCompletionDate);
		Timestamp regTs = resolveRegistrationTimestamp(learner, prog);

		Map<String, Object> row = new HashMap<>();
		row.put("LearnerName", getLearnerFirstName(prog, learner));
		row.put("LearnerSurname", getLearnerSurname(prog, learner));
		row.put("LearnerIDNo", idNo);
		row.put("StartDate", startTs != null ? sdf.format(startTs) : "");
		row.put("EndDate", endTs != null ? sdf.format(endTs) : "");
		row.put("RegistrationDate", sdf.format(regTs));

		JasperReport jasperReport = loadRegistrationLetterReport(reportPath);
		if (jasperReport == null)
		{
			return null;
		}

		JasperPrint print = JasperFillManager.fillReport(jasperReport, jasperParams, new JRMapCollectionDataSource(List.of(row)));
		File tempDir = new File(System.getProperty("java.io.tmpdir"),
				"reg_confirm_" + System.currentTimeMillis() + "_" + System.nanoTime());
		tempDir.mkdirs();

		if (Util.isEmpty(idNo) && prog != null)
		{
			idNo = String.valueOf(prog.get_ID());
		}
		if (Util.isEmpty(idNo) && learner != null)
		{
			idNo = String.valueOf(learner.get_ID());
		}
		String safeIdNo = Util.isEmpty(idNo) ? "learner" : idNo.replaceAll("[\\\\/:*?\"<>|]", "_");

		File pdfFile = new File(tempDir, "Registration_Confirmation_Letter_" + safeIdNo + ".pdf");
		JasperExportManager.exportReportToPdfFile(print, pdfFile.getAbsolutePath());
		return pdfFile;
	}

	private static JasperReport loadRegistrationLetterReport(String reportPath) throws Exception
	{
		try (InputStream jasperStream = ZZLearnerUtil.class.getResourceAsStream(reportPath + "Learner_Registration_Confirmation_Letter.jasper"))
		{
			if (jasperStream != null)
			{
				try
				{
					return (JasperReport) JRLoader.loadObject(jasperStream);
				}
				catch (Exception e)
				{
					log.warning("Could not load precompiled .jasper: " + e.getMessage());
				}
			}
		}
		try (InputStream jrxmlStream = ZZLearnerUtil.class.getResourceAsStream(reportPath + "Learner_Registration_Confirmation_Letter.jrxml"))
		{
			if (jrxmlStream == null)
			{
				log.severe("Could not find Learner_Registration_Confirmation_Letter (.jasper or .jrxml) on classpath");
				return null;
			}
			return JasperCompileManager.compileReport(jrxmlStream);
		}
	}

	private static int resolveSdpId(Properties ctx, X_ZZLearner learner, PO prog)
	{
		int sdpId = (prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_SDP_ID) >= 0)
				? prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZ_SDP_ID) : 0;
		if (sdpId <= 0 && learner != null && learner.getZZ_SDPAdmin_ID() > 0 && ctx != null)
		{
			MUser adminUser = MUser.get(ctx, learner.getZZ_SDPAdmin_ID());
			if (adminUser != null)
			{
				sdpId = adminUser.getC_BPartner_ID();
			}
		}
		return sdpId;
	}

	private static Timestamp getTimestampColumn(PO po, String columnName)
	{
		if (po != null && po.get_ColumnIndex(columnName) >= 0)
		{
			return (Timestamp) po.get_Value(columnName);
		}
		return null;
	}

	private static Timestamp resolveRegistrationTimestamp(X_ZZLearner learner, PO prog)
	{
		Timestamp regTs = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate);
		if (regTs == null)
		{
			regTs = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered);
		}
		if (regTs == null && learner != null)
		{
			regTs = learner.getZZ_DateRegistered();
		}
		return regTs != null ? regTs : new Timestamp(System.currentTimeMillis());
	}

	private static String formatLearnerLabel(X_ZZLearner learner)
	{
		String name = (getLearnerFirstName(learner) + " " + getLearnerSurname(learner)).trim();
		String idNo = getIDOrPassportNo(learner);
		if (!name.isEmpty() && !idNo.isEmpty())
		{
			return name + " (" + idNo + ")";
		}
		if (!name.isEmpty())
		{
			return name;
		}
		return !idNo.isEmpty() ? idNo : "ID=" + learner.get_ID();
	}

	/**
	 * Resolves the SDP Admin user from the programme record or learner with fallback to record creator.
	 */
	public static MUser resolveSDPUser(X_ZZLearner learner, PO prog)
	{
		int sdpUserId = 0;
		if (learner != null)
		{
			sdpUserId = learner.getZZ_SDPAdmin_ID();
		}

		if (sdpUserId <= 0 && prog != null)
		{
			if (prog.get_ColumnIndex(I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZLeadSDProviderContact_ID) >= 0)
			{
				sdpUserId = prog.get_ValueAsInt(I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZLeadSDProviderContact_ID);
			}
			if (sdpUserId <= 0 && prog.get_ColumnIndex(I_ZZLearner.COLUMNNAME_ZZ_SDPAdmin_ID) >= 0)
			{
				sdpUserId = prog.get_ValueAsInt(I_ZZLearner.COLUMNNAME_ZZ_SDPAdmin_ID);
			}
			if (sdpUserId <= 0 && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_SDP_ID) >= 0)
			{
				int sdpBpId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZ_SDP_ID);
				if (sdpBpId > 0)
				{
					MUser user = new Query(prog.getCtx(), MUser.Table_Name,
							MUser.COLUMNNAME_C_BPartner_ID + "=? AND " + MUser.COLUMNNAME_IsActive + "='Y'", prog.get_TrxName())
							.setParameters(sdpBpId)
							.setOrderBy(MUser.COLUMNNAME_Created + " ASC")
							.first();
					if (user != null)
					{
						sdpUserId = user.get_ID();
					}
				}
			}
			if (sdpUserId <= 0)
			{
				sdpUserId = prog.getCreatedBy();
			}
		}

		if (sdpUserId <= 0 && learner != null)
		{
			sdpUserId = learner.getCreatedBy();
		}

		Properties ctx = prog != null ? prog.getCtx() : (learner != null ? learner.getCtx() : null);
		return (sdpUserId > 0 && ctx != null) ? MUser.get(ctx, sdpUserId) : null;
	}

	/**
	 * Resolves the display programme name for the confirmation letter body.
	 */
	public static String resolveProgrammeName(PO prog)
	{
		if (prog == null)
		{
			return "learning programme";
		}

		Properties ctx = prog.getCtx();
		String trxName = prog.get_TrxName();

		if (I_ZZLearnerLearnership.Table_Name.equals(prog.get_TableName()))
		{
			int lsId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearnership_ID);
			if (lsId > 0)
			{
				X_ZZLearnership ls = new X_ZZLearnership(ctx, lsId, trxName);
				return selectedProgrammeName(ls, "learnership", ls.getZZLearnershipTitle());
			}
			return "learnership";
		}
		else if (I_ZZLearnerSkillsProgramme.Table_Name.equals(prog.get_TableName()))
		{
			int spId = prog.get_ValueAsInt(I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID);
			if (spId > 0)
			{
				X_ZZSkillsProgramme sp = new X_ZZSkillsProgramme(ctx, spId, trxName);
				String title = sp.getZZSkillsProgrammeTitle();
				return selectedProgrammeName(sp, "skills programme", title);
			}
			return "skills programme";
		}
		else if (I_ZZLearnerQCTOLearnership.Table_Name.equals(prog.get_TableName()))
		{
			int qlsId = prog.get_ValueAsInt(I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZQctoLearnership_ID);
			if (qlsId > 0)
			{
				X_ZZQctoLearnership qls = new X_ZZQctoLearnership(ctx, qlsId, trxName);
				String title = qls.getZZLearnershipTitle();
				return selectedProgrammeName(qls, "learnership", title);
			}
			return "learnership";
		}
		else if (I_ZZLearnerQCTOArtisans.Table_Name.equals(prog.get_TableName()))
		{
			int qartId = prog.get_ValueAsInt(I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZQctoLearnership_ID);
			if (qartId > 0)
			{
				X_ZZQctoLearnership qls = new X_ZZQctoLearnership(ctx, qartId, trxName);
				String title = qls.getZZLearnershipTitle();
				return selectedProgrammeName(qls, "artisan", title);
			}
			return "artisan";
		}
		else if (I_ZZLearnerQCTOSkillsProgramme.Table_Name.equals(prog.get_TableName()))
		{
			int qspId = prog.get_ValueAsInt(I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID);
			if (qspId > 0)
			{
				X_ZZQctoSkillsProgramme qsp = new X_ZZQctoSkillsProgramme(ctx, qspId, trxName);
				String title = qsp.getZZSkillsProgrammeTitle();
				return selectedProgrammeName(qsp, "skills programme", title);
			}
			return "skills programme";
		}
		else if (I_ZZLearnerDGProgramme.Table_Name.equals(prog.get_TableName()))
		{
			return resolveDGProgrammeName(prog);
		}

		return "learning programme";
	}

	/** Resolve the programme selected by the revised DG registration process. */
	private static String resolveDGProgrammeName(PO prog)
	{
		int definitionId = prog.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_Program_Master_Data_ID);
		if (definitionId <= 0)
			definitionId = prog.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_DGProgram_ID);
		if (definitionId <= 0)
			return "discretionary grant";

		X_ZZ_Program_Master_Data definition = new X_ZZ_Program_Master_Data(prog.getCtx(), definitionId, prog.get_TrxName());
		String name = definition.getTitle();
		if (Util.isEmpty(name, true))
			name = definition.getName();
		if (Util.isEmpty(name, true))
			name = "discretionary grant";
		if (!definition.isEnterQualification())
			return name;

		int programmeId = prog.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_zzlearnerprogram_id);
		if (programmeId <= 0)
			return name;

		String type = prog.get_ValueAsString(I_ZZLearnerDGProgramme.COLUMNNAME_ZZProgramType);
		PO selected;
		String label;
		String title;
		if (X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_Learnership.equals(type))
		{
			X_ZZLearnership ls = new X_ZZLearnership(prog.getCtx(), programmeId, prog.get_TrxName());
			selected = ls;
			label = "learnership";
			title = ls.getZZLearnershipTitle();
		}
		else if (X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_QCTOLearnership.equals(type)
				|| X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_QCTOArtisans.equals(type))
		{
			X_ZZQctoLearnership ls = new X_ZZQctoLearnership(prog.getCtx(), programmeId, prog.get_TrxName());
			selected = ls;
			label = X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_QCTOArtisans.equals(type) ? "artisan" : "learnership";
			title = ls.getZZLearnershipTitle();
		}
		else if (X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_SkillsProgramme.equals(type))
		{
			X_ZZSkillsProgramme sp = new X_ZZSkillsProgramme(prog.getCtx(), programmeId, prog.get_TrxName());
			selected = sp;
			label = "skills programme";
			title = sp.getZZSkillsProgrammeTitle();
		}
		else if (X_ZZLearnerDGProgramme.ZZPROGRAMTYPE_QCTOSkillsProgramme.equals(type))
		{
			X_ZZQctoSkillsProgramme sp = new X_ZZQctoSkillsProgramme(prog.getCtx(), programmeId, prog.get_TrxName());
			selected = sp;
			label = "skills programme";
			title = sp.getZZSkillsProgrammeTitle();
		}
		else
			return name;

		if (selected.get_ID() <= 0)
			return name;
		if (Util.isEmpty(title, true))
			title = prog.get_ValueAsString(I_ZZLearnerDGProgramme.COLUMNNAME_ZZProgramTitle);
		return selectedProgrammeName(selected, label, title);
	}

	/** Format the title and level of the programme selected during registration. */
	private static String selectedProgrammeName(PO selected, String label, String title)
	{
		String level = selected.get_ValueAsString(I_ZZLearnership.COLUMNNAME_ZZNqfLevel);
		if (Util.isEmpty(title, true))
			return label;
		String description = title.trim();
		if (!Util.isEmpty(level, true))
		{
			String levelText = level.trim().toUpperCase(Locale.ROOT)
					.replaceFirst("^(?:LEVEL\\s*)?0*(\\d+)$", "LEVEL $1");
			description += " " + levelText;
		}
		return label + " (" + description + ")";
	}

	/**
	 * Finds the latest Registered programme child record across all 6 intervention tables.
	 */
	public static PO findLatestRegisteredProgramme(X_ZZLearner learner)
	{
		if (learner == null || learner.get_ID() <= 0)
		{
			return null;
		}

		String whereClause = I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID + "=? AND "
				+ I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus + "=?";
		String orderBy = "COALESCE(" + I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate + ", "
				+ I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered + ", "
				+ I_ZZLearnerLearnership.COLUMNNAME_Created + ") DESC, "
				+ I_ZZLearnerLearnership.COLUMNNAME_Created + " DESC";

		PO latestProg = null;
		Timestamp latestTs = null;
		for (String tableName : INTERVENTION_TABLES)
		{
			PO candidate = new Query(learner.getCtx(), tableName, whereClause, learner.get_TrxName())
					.setParameters(learner.get_ID(), X_ZZLearner.ZZ_DOCSTATUS_Registered)
					.setOnlyActiveRecords(true)
					.setOrderBy(orderBy)
					.first();
			if (candidate == null)
			{
				continue;
			}
			Timestamp candidateTs = getEffectiveRegistrationTimestamp(candidate);
			if (latestProg == null || isLaterProgramme(candidate, candidateTs, latestProg, latestTs))
			{
				latestProg = candidate;
				latestTs = candidateTs;
			}
		}
		return latestProg;
	}

	private static Timestamp getEffectiveRegistrationTimestamp(PO prog)
	{
		Timestamp ts = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate);
		if (ts == null)
		{
			ts = getTimestampColumn(prog, I_ZZLearnerLearnership.COLUMNNAME_ZZ_DateRegistered);
		}
		return ts != null ? ts : prog.getCreated();
	}

	private static boolean isLaterProgramme(PO candidate, Timestamp candidateTs, PO current, Timestamp currentTs)
	{
		Comparator<Timestamp> nullsFirstTs = Comparator.nullsFirst(Comparator.naturalOrder());
		int cmp = nullsFirstTs.compare(candidateTs, currentTs);
		if (cmp != 0)
		{
			return cmp > 0;
		}
		return nullsFirstTs.compare(candidate.getCreated(), current.getCreated()) > 0;
	}

	/**
	 * Finds the programme child record in Draft status across all 5 programme types.
	 */
	public static PO findDraftProgramme(X_ZZLearner learner)
	{
		if (learner == null || learner.get_ID() <= 0)
		{
			return null;
		}

		int learnerId = learner.get_ID();
		Properties ctx = learner.getCtx();
		String trxName = learner.get_TrxName();

		String whereClause = I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID + "=? AND "
				+ I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus + " IN (?, ?)";

		// 1. Check ZZLearnerLearnership
		PO prog = new Query(ctx, I_ZZLearnerLearnership.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerLearnership.ZZ_DOCSTATUS_Created, X_ZZLearnerLearnership.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 2. Check ZZLearnerSkillsProgramme
		prog = new Query(ctx, I_ZZLearnerSkillsProgramme.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerSkillsProgramme.ZZ_DOCSTATUS_Created, X_ZZLearnerSkillsProgramme.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 3. Check ZZLearnerQCTOLearnership
		prog = new Query(ctx, I_ZZLearnerQCTOLearnership.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerQCTOLearnership.ZZ_DOCSTATUS_Created, X_ZZLearnerQCTOLearnership.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 4. Check ZZLearnerQCTOArtisans
		prog = new Query(ctx, I_ZZLearnerQCTOArtisans.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerQCTOArtisans.ZZ_DOCSTATUS_Created, X_ZZLearnerQCTOArtisans.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 5. Check ZZLearnerQCTOSkillsProgramme
		prog = new Query(ctx, I_ZZLearnerQCTOSkillsProgramme.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerQCTOSkillsProgramme.ZZ_DOCSTATUS_Created, X_ZZLearnerQCTOSkillsProgramme.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 6. Check ZZLearnerDGProgramme
		prog = new Query(ctx, I_ZZLearnerDGProgramme.Table_Name, whereClause, trxName)
				.setParameters(learnerId, X_ZZLearnerDGProgramme.ZZ_DOCSTATUS_Created, X_ZZLearnerDGProgramme.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// Fallback: check latest record from any programme table if none is strictly in Draft/Created
		prog = new Query(ctx, I_ZZLearnerLearnership.Table_Name,
				I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerLearnership.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(ctx, I_ZZLearnerSkillsProgramme.Table_Name,
				I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerSkillsProgramme.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(ctx, I_ZZLearnerQCTOLearnership.Table_Name,
				I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOLearnership.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(ctx, I_ZZLearnerQCTOArtisans.Table_Name,
				I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOArtisans.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(ctx, I_ZZLearnerQCTOSkillsProgramme.Table_Name,
				I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(ctx, I_ZZLearnerDGProgramme.Table_Name,
				I_ZZLearnerDGProgramme.COLUMNNAME_ZZLearner_ID + "=?", trxName)
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerDGProgramme.COLUMNNAME_Created + " DESC")
				.first();
		return prog;
	}

	/**
	 * Resolves learner ID or Passport number safely (from virtual column or linked ZZPerson).
	 */
	public static String getIDOrPassportNo(X_ZZLearner learner)
	{
		return getIDOrPassportNo(null, learner);
	}

	public static String getIDOrPassportNo(PO prog, X_ZZLearner learner)
	{
		if (learner == null && prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID) >= 0)
		{
			int learnerId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID);
			if (learnerId > 0)
			{
				learner = new X_ZZLearner(prog.getCtx(), learnerId, prog.get_TrxName());
			}
		}
		if (learner == null)
		{
			return "";
		}
		String id = learner.getZZ_ID_Passport_No();
		if (Util.isEmpty(id) && learner.getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(learner.getCtx(), learner.getZZPerson_ID(), learner.get_TrxName());
			id = person.getZZ_ID_Passport_No();
		}
		return id != null ? id : "";
	}

	/**
	 * Resolves learner first name from linked ZZPerson.
	 */
	public static String getLearnerFirstName(X_ZZLearner learner)
	{
		return getLearnerFirstName(null, learner);
	}

	public static String getLearnerFirstName(PO prog, X_ZZLearner learner)
	{
		if (learner == null && prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID) >= 0)
		{
			int learnerId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID);
			if (learnerId > 0)
			{
				learner = new X_ZZLearner(prog.getCtx(), learnerId, prog.get_TrxName());
			}
		}
		if (learner != null && learner.getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(learner.getCtx(), learner.getZZPerson_ID(), learner.get_TrxName());
			return person.getZZFirstName() != null ? person.getZZFirstName() : "";
		}
		return "";
	}

	/**
	 * Resolves learner surname from linked ZZPerson.
	 */
	public static String getLearnerSurname(X_ZZLearner learner)
	{
		return getLearnerSurname(null, learner);
	}

	public static String getLearnerSurname(PO prog, X_ZZLearner learner)
	{
		if (learner == null && prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID) >= 0)
		{
			int learnerId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID);
			if (learnerId > 0)
			{
				learner = new X_ZZLearner(prog.getCtx(), learnerId, prog.get_TrxName());
			}
		}
		if (learner != null && learner.getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(learner.getCtx(), learner.getZZPerson_ID(), learner.get_TrxName());
			return person.getSurname() != null ? person.getSurname() : "";
		}
		return "";
	}

	/**
	 * Resolves the query reason from the intervention record or learner record.
	 */
	public static String getZZReason(X_ZZLearner learner, PO prog)
	{
		String reason = null;
		if (prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason) >= 0)
		{
			reason = prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ZZ_Reason);
		}
		if (Util.isEmpty(reason) && learner != null)
		{
			reason = learner.getZZ_Reason();
		}
		return reason != null ? reason : "";
	}

	/**
	 * Replaces contextual tokens in email subject and body.
	 */
	public static String replaceTokens(String text, X_ZZLearner learner, PO prog, String reason)
	{
		if (Util.isEmpty(text) || text.indexOf('@') < 0)
		{
			return text;
		}

		String firstName = getLearnerFirstName(prog, learner);
		String surname = getLearnerSurname(prog, learner);
		String idOrPass = getIDOrPassportNo(prog, learner);
		String refNo = "";
		if (prog != null && prog.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo) >= 0)
		{
			refNo = prog.get_ValueAsString(I_ZZLearnerLearnership.COLUMNNAME_ReferenceNo);
		}
		if (Util.isEmpty(refNo) && learner != null && learner.getReferenceNo() != null)
		{
			refNo = learner.getReferenceNo();
		}

		String progName = resolveProgrammeName(prog);
		String actualReason = !Util.isEmpty(reason) ? reason : getZZReason(learner, prog);

		text = text.replace("@ZZFirstName@", firstName).replace("@=ZZFirstName@", firstName);
		text = text.replace("@ZZSurname@", surname).replace("@=ZZSurname@", surname);
		text = text.replace("@Surname@", surname).replace("@=Surname@", surname);
		text = text.replace("@IDOrPassportNo@", idOrPass).replace("@=IDOrPassportNo@", idOrPass);
		text = text.replace("@ZZ_ID_Passport_No@", idOrPass).replace("@=ZZ_ID_Passport_No@", idOrPass);
		text = text.replace("@ReferenceNo@", refNo).replace("@=ReferenceNo@", refNo);
		text = text.replace("@ProgrammeName@", progName).replace("@=ProgrammeName@", progName);
		text = text.replace("@ZZReason@", actualReason).replace("@=ZZReason@", actualReason);
		text = text.replace("@ZZ_Reason@", actualReason).replace("@=ZZ_Reason@", actualReason);

		return text;
	}

	/**
	 * Sends an email with optional CC recipients and optional attachment.
	 */
	public static void sendEmailWithCC(Properties ctx, int clientId, String trxName, String toEmail,
			String subject, String body, boolean isHtml, File attachment)
	{
		MClient client = MClient.get(ctx, clientId);
		EMail email = client.createEMail(toEmail, subject, body, isHtml);
		if (email == null)
		{
			log.warning("Failed to create email for toEmail: " + toEmail);
			return;
		}

		if (attachment != null && attachment.exists())
		{
			email.addAttachment(attachment);
		}

		String ccEmails = MSysConfig.getValue(LEARNER_REGISTRATION_LETTER_CC, clientId);
		if (!Util.isEmpty(ccEmails))
		{
			for (String cc : ccEmails.split(","))
			{
				if (!Util.isEmpty(cc, true))
				{
					email.addCc(cc.trim());
				}
			}
		}

		String msg = email.send();
		if (!EMail.SENT_OK.equals(msg))
		{
			log.warning("Failed to send email to " + toEmail + ": " + msg);
		}
	}
}
