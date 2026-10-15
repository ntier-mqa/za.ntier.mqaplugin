package za.ntier.models;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MSequence;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
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
import za.co.ntier.api.model.I_ZZLearnerLearnership;
import za.co.ntier.api.model.I_ZZLearnerQCTOArtisans;
import za.co.ntier.api.model.I_ZZLearnerQCTOLearnership;
import za.co.ntier.api.model.I_ZZLearnerQCTOSkillsProgramme;
import za.co.ntier.api.model.I_ZZLearnerSkillsProgramme;
import za.co.ntier.api.model.X_ZZLearner;
import za.co.ntier.api.model.X_ZZLearnerLearnership;
import za.co.ntier.api.model.X_ZZLearnerQCTOArtisans;
import za.co.ntier.api.model.X_ZZLearnerQCTOLearnership;
import za.co.ntier.api.model.X_ZZLearnerQCTOSkillsProgramme;
import za.co.ntier.api.model.X_ZZLearnerSkillsProgramme;
import za.co.ntier.api.model.X_ZZLearnership;
import za.co.ntier.api.model.X_ZZPerson;
import za.co.ntier.api.model.X_ZZQctoLearnership;
import za.co.ntier.api.model.X_ZZQctoSkillsProgramme;
import za.co.ntier.api.model.X_ZZQualification;
import za.co.ntier.api.model.X_ZZSkillsProgramme;

/**
 * Custom Model class for the ZZLearner table.
 */
public class MZZLearner extends X_ZZLearner
{
	private static final long	serialVersionUID						= 1L;

	private static final String	LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU	= "eac216ef-5331-4d12-980d-7d7710b9829c";
	private static final String	LEARNER_QUERY_MAIL_TEMPLATE_UU			= "ba58e3ff-6b42-4f4c-be01-cfbe1255803e";
	private static final String	LEARNER_REGISTRATION_LETTER_CC			= "LEARNER_REGISTRATION_LETTER_CC";

	public MZZLearner(Properties ctx, int ZZLearner_ID, String trxName)
	{
		super(ctx, ZZLearner_ID, trxName);
	}

	public MZZLearner(Properties ctx, int ZZLearner_ID, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearner_ID, trxName, virtualColumns);
	}

	public MZZLearner(Properties ctx, String ZZLearner_UU, String trxName)
	{
		super(ctx, ZZLearner_UU, trxName);
	}

	public MZZLearner(Properties ctx, String ZZLearner_UU, String trxName, String... virtualColumns)
	{
		super(ctx, ZZLearner_UU, trxName, virtualColumns);
	}

	public MZZLearner(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		// Auto-generate ReferenceNo when status is Registered
		if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus) || newRecord)
		{
			if (ZZ_DOCSTATUS_Registered.equals(getZZ_DocStatus()))
			{
				if (Util.isEmpty(getReferenceNo()))
				{
					String id = getIDOrPassportNo();
					if (!Util.isEmpty(id))
					{
						String seqNo = MSequence.getDocumentNo(getAD_Client_ID(), get_TableName(), get_TrxName(), this);
						setReferenceNo(id + "/" + seqNo);
					}
				}
			}
		}

		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		if (success && is_ValueChanged(COLUMNNAME_ZZ_DocStatus))
		{
			String status = getZZ_DocStatus();
			if (ZZ_DOCSTATUS_Registered.equals(status))
			{
				PO draftProg = setProgrammeRegistrationDate();
				sendLearnerConfirmationToSDP(draftProg);
			}
			else if (ZZ_DOCSTATUS_Query.equals(status))
			{
				sendLearnerQueryToSDP();
			}
		}

		return super.afterSave(newRecord, success);
	}

	/**
	 * Sets ZZRegistrationDate on the learner's draft programme record upon status changing to Registered.
	 * Returns the draft programme record.
	 */
	private PO setProgrammeRegistrationDate()
	{
		PO draftProg = findDraftProgramme();
		if (draftProg != null)
		{
			Timestamp now = new Timestamp(System.currentTimeMillis());
			if (draftProg.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) >= 0)
			{
				draftProg.set_ValueOfColumn(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate, now);
			}
			
			draftProg.saveEx(get_TrxName());
		}
		return draftProg;
	}

	/**
	 * Finds the programme child record in Draft status ('DR') across all 5 programme types.
	 */
	private PO findDraftProgramme()
	{
		int learnerId = get_ID();
		if (learnerId <= 0)
		{
			return null;
		}

		// 1. Check ZZLearnerLearnership
		PO prog = new Query(getCtx(), I_ZZLearnerLearnership.Table_Name,
				I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID + "=? AND " + I_ZZLearnerLearnership.COLUMNNAME_ZZ_DocStatus + "=?",
				get_TrxName())
				.setParameters(learnerId, X_ZZLearnerLearnership.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 2. Check ZZLearnerSkillsProgramme
		prog = new Query(getCtx(), I_ZZLearnerSkillsProgramme.Table_Name,
				I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=? AND " + I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZ_DocStatus + "=?",
				get_TrxName())
				.setParameters(learnerId, X_ZZLearnerSkillsProgramme.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 3. Check ZZLearnerQCTOLearnership
		prog = new Query(getCtx(), I_ZZLearnerQCTOLearnership.Table_Name,
				I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZLearner_ID + "=? AND " + I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZ_DocStatus + "=?",
				get_TrxName())
				.setParameters(learnerId, X_ZZLearnerQCTOLearnership.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 4. Check ZZLearnerQCTOArtisans
		prog = new Query(getCtx(), I_ZZLearnerQCTOArtisans.Table_Name,
				I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZLearner_ID + "=? AND " + I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZ_DocStatus + "=?",
				get_TrxName())
				.setParameters(learnerId, X_ZZLearnerQCTOArtisans.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// 5. Check ZZLearnerQCTOSkillsProgramme
		prog = new Query(getCtx(), I_ZZLearnerQCTOSkillsProgramme.Table_Name,
				I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=? AND " + I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZ_DocStatus + "=?",
				get_TrxName())
				.setParameters(learnerId, X_ZZLearnerQCTOSkillsProgramme.ZZ_DOCSTATUS_Draft)
				.first();
		if (prog != null)
			return prog;

		// Fallback: check latest record from any programme table if none is strictly in Draft
		prog = new Query(getCtx(), I_ZZLearnerLearnership.Table_Name,
				I_ZZLearnerLearnership.COLUMNNAME_ZZLearner_ID + "=?", get_TrxName())
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerLearnership.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(getCtx(), I_ZZLearnerSkillsProgramme.Table_Name,
				I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=?", get_TrxName())
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerSkillsProgramme.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(getCtx(), I_ZZLearnerQCTOLearnership.Table_Name,
				I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZLearner_ID + "=?", get_TrxName())
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOLearnership.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(getCtx(), I_ZZLearnerQCTOArtisans.Table_Name,
				I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZLearner_ID + "=?", get_TrxName())
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOArtisans.COLUMNNAME_Created + " DESC")
				.first();
		if (prog != null)
			return prog;

		prog = new Query(getCtx(), I_ZZLearnerQCTOSkillsProgramme.Table_Name,
				I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZLearner_ID + "=?", get_TrxName())
				.setParameters(learnerId)
				.setOrderBy(I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_Created + " DESC")
				.first();
		return prog;
	}

	/**
	 * Resolves the display programme name for the confirmation letter body.
	 */
	private String resolveProgrammeName(PO prog)
	{
		if (prog == null)
		{
			return "learning programme";
		}

		if (I_ZZLearnerLearnership.Table_Name.equals(prog.get_TableName()))
		{
			int lsId = prog.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZLearnership_ID);
			if (lsId > 0)
			{
				X_ZZLearnership ls = new X_ZZLearnership(getCtx(), lsId, get_TrxName());
				String qualTitle = "";
				if (ls.getZZQualification_ID() > 0)
				{
					X_ZZQualification q = new X_ZZQualification(getCtx(), ls.getZZQualification_ID(), get_TrxName());
					qualTitle = q.getZZSaqaQualificationTitle();
				}
				if (Util.isEmpty(qualTitle))
				{
					qualTitle = ls.getZZLearnershipTitle();
				}
				return !Util.isEmpty(qualTitle) ? "learnership (" + qualTitle + ")" : "learnership";
			}
			return "learnership";
		}
		else if (I_ZZLearnerSkillsProgramme.Table_Name.equals(prog.get_TableName()))
		{
			int spId = prog.get_ValueAsInt(I_ZZLearnerSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID);
			if (spId > 0)
			{
				X_ZZSkillsProgramme sp = new X_ZZSkillsProgramme(getCtx(), spId, get_TrxName());
				String title = sp.getZZSkillsProgrammeTitle();
				return !Util.isEmpty(title) ? "skills programme (" + title + ")" : "skills programme";
			}
			return "skills programme";
		}
		else if (I_ZZLearnerQCTOLearnership.Table_Name.equals(prog.get_TableName()))
		{
			int qlsId = prog.get_ValueAsInt(I_ZZLearnerQCTOLearnership.COLUMNNAME_ZZQctoLearnership_ID);
			if (qlsId > 0)
			{
				X_ZZQctoLearnership qls = new X_ZZQctoLearnership(getCtx(), qlsId, get_TrxName());
				String title = qls.getZZLearnershipTitle();
				return !Util.isEmpty(title) ? "learnership (" + title + ")" : "learnership";
			}
			return "learnership";
		}
		else if (I_ZZLearnerQCTOArtisans.Table_Name.equals(prog.get_TableName()))
		{
			int qartId = prog.get_ValueAsInt(I_ZZLearnerQCTOArtisans.COLUMNNAME_ZZQctoLearnership_ID);
			if (qartId > 0)
			{
				X_ZZQctoLearnership qls = new X_ZZQctoLearnership(getCtx(), qartId, get_TrxName());
				String title = qls.getZZLearnershipTitle();
				return !Util.isEmpty(title) ? "artisan (" + title + ")" : "artisan";
			}
			return "artisan";
		}
		else if (I_ZZLearnerQCTOSkillsProgramme.Table_Name.equals(prog.get_TableName()))
		{
			int qspId = prog.get_ValueAsInt(I_ZZLearnerQCTOSkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID);
			if (qspId > 0)
			{
				X_ZZQctoSkillsProgramme qsp = new X_ZZQctoSkillsProgramme(getCtx(), qspId, get_TrxName());
				String title = qsp.getZZSkillsProgrammeTitle();
				return !Util.isEmpty(title) ? "skills programme (" + title + ")" : "skills programme";
			}
			return "skills programme";
		}

		return "learning programme";
	}

	/**
	 * Generates the Registration Confirmation Letter in landscape orientation and returns the PDF file.
	 */
	private File generateRegistrationConfirmationLetterPdf(PO draftProg)
	{
		try
		{
			int sdpId = draftProg != null ? draftProg.get_ValueAsInt(I_ZZLearnerLearnership.COLUMNNAME_ZZ_SDP_ID) : 0;
			if (sdpId <= 0)
			{
				sdpId = getZZ_SDPAdmin_ID();
			}

			String sdpName = "";
			String sdpSearchKey = "";
			if (sdpId > 0)
			{
				MBPartner sdpBp = new MBPartner(getCtx(), sdpId, get_TrxName());
				if (sdpBp != null && sdpBp.get_ID() > 0)
				{
					sdpName = sdpBp.getName() != null ? sdpBp.getName() : "";
					sdpSearchKey = sdpBp.getValue() != null ? sdpBp.getValue() : "";
				}
			}

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			String datePrinted = sdf.format(new Date());

			String progName = resolveProgrammeName(draftProg);
			String companyName = (sdpName + " " + sdpSearchKey).trim();

			Timestamp startTs = null;
			if (draftProg != null && draftProg.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZCommencementDate) >= 0)
			{
				startTs = (Timestamp) draftProg.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZCommencementDate);
			}
			Timestamp endTs = null;
			if (draftProg != null && draftProg.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZCompletionDate) >= 0)
			{
				endTs = (Timestamp) draftProg.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZCompletionDate);
			}
			Timestamp regTs = null;
			if (draftProg != null && draftProg.get_ColumnIndex(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate) >= 0)
			{
				regTs = (Timestamp) draftProg.get_Value(I_ZZLearnerLearnership.COLUMNNAME_ZZRegistrationDate);
			}
			if (regTs == null)
			{
				regTs = getZZ_DateRegistered();
			}
			if (regTs == null)
			{
				regTs = new Timestamp(System.currentTimeMillis());
			}

			Map<String, Object> jasperParams = new HashMap<>();
			jasperParams.put("SDPName", sdpName);
			jasperParams.put("SDPSearchKey", sdpSearchKey);
			jasperParams.put("DatePrinted", datePrinted);
			jasperParams.put("ProgrammeName", progName);
			jasperParams.put("CompanyName", companyName);

			final String reportPath = "/za/co/ntier/wsp_atr/report/jrxmls/";

			URL headerUrl = getClass().getResource(reportPath + "MQA_Address_Logo_Header.png");
			if (headerUrl != null)
			{
				jasperParams.put("HeaderImagePath", headerUrl);
			}

			URL footerUrl = getClass().getResource(reportPath + "Footer_Mining_Future_Skills.png");
			if (footerUrl != null)
			{
				jasperParams.put("FooterImagePath", footerUrl);
			}

			jasperParams.put("RESOURCE_DIR", "za/co/ntier/wsp_atr/report/jrxmls/");
			jasperParams.put(JRParameter.REPORT_CLASS_LOADER, getClass().getClassLoader());

			List<Map<String, ?>> rows = new ArrayList<>();
			Map<String, Object> row = new HashMap<>();
			row.put("LearnerName", getZZFirstName());
			row.put("LearnerSurname", getZZSurname());
			row.put("LearnerIDNo", getLearnerIDNo());
			row.put("StartDate", startTs != null ? sdf.format(startTs) : "");
			row.put("EndDate", endTs != null ? sdf.format(endTs) : "");
			row.put("RegistrationDate", sdf.format(regTs));
			rows.add(row);

			JasperReport jasperReport = null;
			InputStream jasperStream = getClass().getResourceAsStream(reportPath + "Learner_Registration_Confirmation_Letter.jasper");
			if (jasperStream != null)
			{
				try
				{
					jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
				}
				catch (Exception e)
				{
					log.warning("Could not load precompiled .jasper: " + e.getMessage());
				}
			}
			if (jasperReport == null)
			{
				InputStream jrxmlStream = getClass().getResourceAsStream(reportPath + "Learner_Registration_Confirmation_Letter.jrxml");
				if (jrxmlStream == null)
				{
					log.severe("Could not find Learner_Registration_Confirmation_Letter (.jasper or .jrxml) on classpath");
					return null;
				}
				jasperReport = JasperCompileManager.compileReport(jrxmlStream);
			}

			JasperPrint print = JasperFillManager.fillReport(jasperReport, jasperParams, new JRMapCollectionDataSource(rows));
			File tempDir = new File(System.getProperty("java.io.tmpdir"), "reg_confirm_" + System.currentTimeMillis());
			tempDir.mkdirs();

			String idNo = getLearnerIDNo();
			if (Util.isEmpty(idNo))
			{
				idNo = String.valueOf(get_ID());
			}
			File pdfFile = new File(tempDir, "Registration_Confirmation_Letter_" + idNo + ".pdf");
			JasperExportManager.exportReportToPdfFile(print, pdfFile.getAbsolutePath());
			return pdfFile;
		}
		catch (Exception e)
		{
			log.severe("Failed to generate Registration Confirmation Letter PDF: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Resolves learner ID or Passport number safely (from virtual column or linked ZZPerson).
	 */
	public String getIDOrPassportNo()
	{
		String id = getZZ_ID_Passport_No();
		if (Util.isEmpty(id) && getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			id = person.getZZ_ID_Passport_No();
		}
		return id != null ? id : "";
	}

	public String getLearnerIDNo()
	{
		return getIDOrPassportNo();
	}

	/**
	 * Resolves learner first name from linked ZZPerson.
	 * Also serves as getter for @=ZZFirstName@ in mail templates.
	 */
	public String getZZFirstName()
	{
		if (getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			return person.getZZFirstName() != null ? person.getZZFirstName() : "";
		}
		return "";
	}

	public String getLearnerFirstName()
	{
		return getZZFirstName();
	}

	/**
	 * Resolves learner surname from linked ZZPerson.
	 * Also serves as getter for @=ZZSurname@ or @=Surname@ in mail templates.
	 */
	public String getZZSurname()
	{
		if (getZZPerson_ID() > 0)
		{
			X_ZZPerson person = new X_ZZPerson(getCtx(), getZZPerson_ID(), get_TrxName());
			return person.getSurname() != null ? person.getSurname() : "";
		}
		return "";
	}

	public String getSurname()
	{
		return getZZSurname();
	}

	public String getLearnerSurname()
	{
		return getZZSurname();
	}

	/**
	 * Getter for @=ZZReason@ in mail templates (matches template property without underscore).
	 */
	public String getZZReason()
	{
		String reason = getZZ_Reason();
		return reason != null ? reason : "";
	}

	/**
	 * Resolves the SDP Admin user directly from ZZ_SDPAdmin_ID with fallback to record creator.
	 */
	private MUser resolveSDPUser()
	{
		int sdpUserId = getZZ_SDPAdmin_ID();
		if (sdpUserId <= 0)
		{
			sdpUserId = getCreatedBy();
		}

		return MUser.get(getCtx(), sdpUserId);
	}

	/**
	 * Sends Learner Confirmation email to the SDP, attaching the landscape confirmation letter PDF.
	 */
	private void sendLearnerConfirmationToSDP(PO draftProg)
	{
		try
		{
			MUser sdpUser = resolveSDPUser();
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for learner: " + get_ID());
				return;
			}

			MMailText mailTemplate = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																												.setParameters(LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU)
																												.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Confirmation mail template not found for UUID: " + LEARNER_CONFIRMATION_MAIL_TEMPLATE_UU);
				return;
			}

			mailTemplate.setUser(sdpUser);
			mailTemplate.setPO(this);

			File pdfAttachment = generateRegistrationConfirmationLetterPdf(draftProg);
			sendEmailWithCC(sdpUser.getEMail(), mailTemplate, pdfAttachment);
			log.info("Learner confirmation email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail() + ") for learner " + get_ID()
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
	private void sendLearnerQueryToSDP()
	{
		try
		{
			MUser sdpUser = resolveSDPUser();
			if (sdpUser == null || Util.isEmpty(sdpUser.getEMail()))
			{
				log.warning("No valid SDP user or email found for learner: " + get_ID());
				return;
			}

			MMailText mailTemplate = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																												.setParameters(LEARNER_QUERY_MAIL_TEMPLATE_UU)
																												.first();

			if (mailTemplate == null)
			{
				log.severe("Learner Query mail template not found for UUID: " + LEARNER_QUERY_MAIL_TEMPLATE_UU);
				return;
			}

			mailTemplate.setUser(sdpUser);
			mailTemplate.setPO(this);

			sendEmailWithCC(sdpUser.getEMail(), mailTemplate, null);
			log.info("Learner query email sent to SDP user " + sdpUser.getName() + " (" + sdpUser.getEMail() + ") for learner " + get_ID());
		}
		catch (Exception e)
		{
			log.severe("Failed to send learner query email to SDP: " + e.getMessage());
		}
	}

	private void sendEmailWithCC(String toEmail, MMailText mailTemplate, File attachment)
	{
		MClient client = MClient.get(getCtx(), getAD_Client_ID());
		EMail email = client.createEMail(toEmail, mailTemplate.getMailHeader(), mailTemplate.getMailText(), mailTemplate.isHtml());
		if (email == null)
		{
			log.warning("Failed to create email for learner: " + get_ID());
			return;
		}

		if (attachment != null && attachment.exists())
		{
			email.addAttachment(attachment);
		}

		String ccEmails = MSysConfig.getValue(LEARNER_REGISTRATION_LETTER_CC, getAD_Client_ID());
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
			log.warning("Failed to send email for learner " + get_ID() + ": " + msg);
		}
	}
}
