package za.ntier.modelvalidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.model.MLocation;
import org.compiere.model.MMailText;
import org.compiere.model.MNote;
import org.compiere.model.MSequence;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_BPartner_Location;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import za.co.ntier.api.model.I_ZZAssessorPerson;
import za.co.ntier.api.model.I_ZZLinkAssessorQualification;
import za.co.ntier.api.model.I_ZZLinkAssessorSkillsProgramme;
import za.co.ntier.api.model.I_ZZQctoQualification;
import za.co.ntier.api.model.I_ZZQctoSkillsProgramme;
import za.co.ntier.api.model.I_ZZQualification;
import za.co.ntier.api.model.I_ZZSkillsProgramme;
import za.co.ntier.api.model.I_ZZ_Allocations;
import za.co.ntier.api.model.I_ZZ_NAMB_Alloc_TTC;
import za.co.ntier.api.model.I_ZZ_NAMB_Alloc_Trades;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_AC;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_OC;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_Skills;
import za.co.ntier.api.model.I_ZZ_WPA_Application;
import za.co.ntier.api.model.MUser_New;
import za.co.ntier.api.model.X_ZZAssessorPerson;
import za.co.ntier.api.model.X_ZZAssessorPerson_v;
import za.co.ntier.api.model.X_ZZLinkAssessorQualification;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_QAAuditAllocations;
import za.co.ntier.api.model.X_ZZ_WPA_Application;

public class NtierModelValidator implements ModelValidator
{
	private static CLogger	log				= CLogger.getCLogger(NtierModelValidator.class);
	private static final String ASSESSOR_APPROVAL_MAIL_TEMPLATE_UU = "fa008fdb-5ba8-4f28-87d5-8b15bfdb6e1f";
	private static final String ASSESSOR_APPROVED_MAIL_TEMPLATE_UU = "9f932747-a3e7-48ad-9c9f-5527b43c164d";
	private static final String MODERATOR_APPROVAL_MAIL_TEMPLATE_UU = "2355edbe-87d9-4a6f-b4d3-1ac733521a27";
	private static final String MODERATOR_APPROVED_MAIL_TEMPLATE_UU = "f8af2182-e4be-405a-9648-12fb4e1464f0";
	private static final String ASSESSOR_NOT_RECOMMENDED_MAIL_TEMPLATE_UU = "e9750b7e-5ccd-4e56-8c0f-e0f9d74bbdd2";
	private static final String MODERATOR_NOT_RECOMMENDED_MAIL_TEMPLATE_UU = "9b1d46cd-80bd-4014-8c7e-eaf8ba948057";
	private static final String ASSESSOR_SCOPE_EXT_APPROVAL_MAIL_TEMPLATE_UU = "ac15f34e-2327-4bf0-b8b6-c8ee9f03de77";
	private static final String ASSESSOR_SCOPE_EXT_APPROVED_MAIL_TEMPLATE_UU = "2da3098e-4e74-4881-88bb-cc91c6dc37d9";
	private static final String MODERATOR_SCOPE_EXT_APPROVAL_MAIL_TEMPLATE_UU = "384532c5-b0f5-43bd-a712-fa87fdaf0499";
	private static final String MODERATOR_SCOPE_EXT_APPROVED_MAIL_TEMPLATE_UU = "e27ca630-dd33-4946-af9f-cc16a4bf2d7b";
	private static final String ASSESSOR_SCOPE_EXT_NOT_RECOMMENDED_MAIL_TEMPLATE_UU = "5d83ad12-003b-4401-afa2-f244979b3553";
	private static final String MODERATOR_SCOPE_EXT_NOT_RECOMMENDED_MAIL_TEMPLATE_UU = "ee081c70-ea83-44b4-950a-7293e8f28c29";
	private int				m_AD_Client_ID	= -1;
	private static final String ROLE_MGR_QA_AI = "635702d2-8ffb-4a31-a585-d2960d86383c";
	private static final String ROLE_ASSESSOR = "Assessor";
	private static final String ROLE_MODERATOR = "Moderator";

	@Override
	public void initialize(ModelValidationEngine engine, MClient client)
	{
		if (client != null)
		{
			m_AD_Client_ID = client.getAD_Client_ID();
		}
		engine.addModelChange(X_ZZ_WPA_Application.Table_Name, this);
		engine.addModelChange(X_ZZAssessorPerson.Table_Name, this);
		
		engine.addModelChange(X_ZZ_Allocations.Table_Name, this);
		engine.addModelChange(X_ZZ_QAAuditAllocations.Table_Name, this);
		
	}

	@Override
	public int getAD_Client_ID()
	{
		return m_AD_Client_ID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception
	{
		if (type == ModelValidator.TYPE_AFTER_CHANGE && X_ZZ_WPA_Application.Table_Name.equals(po.get_TableName()))
		{
			var ColStatus = I_ZZ_WPA_Application.COLUMNNAME_ZZ_DocStatus;
			var ColWpaNumber = I_ZZ_WPA_Application.COLUMNNAME_ZZ_WPA_Number;

			if (po.is_ValueChanged(ColStatus))
			{
				var newStatus = po.get_ValueAsString(ColStatus);
				if (X_ZZ_WPA_Application.ZZ_DOCSTATUS_Approved.equals(newStatus))
				{
					var currentValue = po.get_ValueAsString(ColWpaNumber);
					if (Util.isEmpty(currentValue))
					{
						var nextSeqNo = MSequence.getDocumentNo(
																	po.getAD_Client_ID(),
																	X_ZZ_WPA_Application.Table_Name,
																	po.get_TrxName(),
																	po);
						if (!Util.isEmpty(nextSeqNo))
						{
							po.set_ValueNoCheck(ColWpaNumber, nextSeqNo);
							po.saveEx();
						}
						else
						{
							log.severe("Failed to fetch sequence for ZZ_WPA_Application. Ensure AD_Sequence is configured.");
						}
					}
				}
			}
		}
		
		if (type == ModelValidator.TYPE_AFTER_CHANGE && X_ZZAssessorPerson.Table_Name.equals(po.get_TableName()))
		{
			var ColStatus = I_ZZAssessorPerson.COLUMNNAME_ZZ_DocStatus;

			if (po.is_ValueChanged(ColStatus))
			{
				var newStatus = po.get_ValueAsString(ColStatus);
				var assessorPerson = new X_ZZAssessorPerson(po.getCtx(), po.get_ID(), po.get_TrxName());
				if (X_ZZAssessorPerson.ZZ_DOCSTATUS_Approved.equals(newStatus))
				{
					var now = LocalDateTime.now();
					assessorPerson.setStartDate(Timestamp.valueOf(now));
					assessorPerson.setEndDate(Timestamp.valueOf(now.plusYears(3))); // Set end date
																					// to 3 year
																					// from now

					boolean generateRegistrationNo = false;
					if (ROLE_ASSESSOR.equals(assessorPerson.getZZAssessorRole()) && Util.isEmpty(assessorPerson.getZZ_Assessor()))
						generateRegistrationNo = true;
					else if (ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()) && Util.isEmpty(assessorPerson.getZZ_Moderator()))
						generateRegistrationNo = true;

					if (generateRegistrationNo)
					{
						int parentId = assessorPerson.getParent_ID();
						if (parentId > 0)
						{
							// It's a scope extension, copy from parent
							X_ZZAssessorPerson parentPerson = new X_ZZAssessorPerson(po.getCtx(), parentId, po.get_TrxName());
							if (parentPerson != null)
							{
								assessorPerson.setZZ_Assessor(parentPerson.getZZ_Assessor());
								assessorPerson.setZZ_Moderator(parentPerson.getZZ_Moderator());
							}
						}
						else
						{
							// New registration
							var assDocNo = assessorPerson.getDocumentNo();
							if (!Util.isEmpty(assDocNo))
							{
								var dateStr = now.format(DateTimeFormatter.ofPattern("ddMMyy"));
								if (ROLE_ASSESSOR.equals(assessorPerson.getZZAssessorRole()))
								{
									var assessorNo = "MQA/ASS" + assDocNo + "/" + dateStr;
									assessorPerson.setZZ_Assessor(assessorNo);
								}
								else if (ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()))
								{
									var moderatorNo = "MQA/MOD" + assDocNo + "/" + dateStr;
									assessorPerson.setZZ_Moderator(moderatorNo);
								}
							}
						}
					}

					assessorPerson.saveEx();

					// Send email after approval
					int assessorUserId = assessorPerson.getAD_User_ID();
					if (assessorUserId > 0)
					{
						MUser_New assessorUser = new MUser_New(po.getCtx(), assessorUserId, po.get_TrxName());
						String assessorEmail = assessorUser.getEMail();

						// Fetch the SDP user (creator of the document) for notification purposes
						int createdByUserId = assessorPerson.getCreatedBy();
						MUser_New createdByUser = new MUser_New(po.getCtx(), createdByUserId, po.get_TrxName());
						String sdpEmail = createdByUser.getEMail();

						if (!Util.isEmpty(assessorEmail) || !Util.isEmpty(sdpEmail))
						{
							String fName = assessorUser.getZZFirstName() != null ? assessorUser.getZZFirstName().trim() : "";
							String sName = assessorUser.getZZSurname() != null ? assessorUser.getZZSurname().trim() : "";
							String assessorNameStr = (fName + " " + sName).replaceAll("\\s+", " ").trim();
							if (Util.isEmpty(assessorNameStr))
								assessorNameStr = "";

							String zzAssessor = assessorPerson.getZZ_Assessor();
							if (zzAssessor == null)
								zzAssessor = "";

							String zzModerator = assessorPerson.getZZ_Moderator();
							if (zzModerator == null)
								zzModerator = "";

							boolean isScopeExt = assessorPerson.getParent_ID() > 0;
							String templateUU = ROLE_ASSESSOR.equals(assessorPerson.getZZAssessorRole())
									? (isScopeExt ? ASSESSOR_SCOPE_EXT_APPROVED_MAIL_TEMPLATE_UU : ASSESSOR_APPROVED_MAIL_TEMPLATE_UU)
									: (isScopeExt ? MODERATOR_SCOPE_EXT_APPROVED_MAIL_TEMPLATE_UU : MODERATOR_APPROVED_MAIL_TEMPLATE_UU);

							var approvedMailTemplate = (MMailText) new Query(po.getCtx(), MMailText.Table_Name, "R_MailText_UU=?", po.get_TrxName())
																																					.setParameters(templateUU)
																																					.first();

							if (approvedMailTemplate != null)
							{
								String startDateStr = "";
								if (assessorPerson.getStartDate() != null)
									startDateStr = new SimpleDateFormat("dd MMMM yyyy").format(assessorPerson.getStartDate());

								String endDateStr = "";
								if (assessorPerson.getEndDate() != null)
									endDateStr = new SimpleDateFormat("dd MMMM yyyy").format(assessorPerson.getEndDate());

								String qualifications = fetchRecommendedItems(assessorPerson, I_ZZLinkAssessorQualification.Table_Name,
																			I_ZZQualification.Table_Name, I_ZZQualification.COLUMNNAME_ZZQualification_ID,
																			I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended, I_ZZQualification.COLUMNNAME_ZZSaqaQualificationCode, I_ZZQualification.COLUMNNAME_ZZSaqaQualificationTitle, po.get_TrxName());
								
								String qctoQuals = fetchRecommendedItems(assessorPerson, I_ZZLinkAssessorQualification.Table_Name,
																			I_ZZQctoQualification.Table_Name, I_ZZQctoQualification.COLUMNNAME_ZZQctoQualification_ID,
																			I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended, I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationCode, I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationTitle, po.get_TrxName());
								
								if (!Util.isEmpty(qctoQuals)) {
									qualifications = Util.isEmpty(qualifications) ? qctoQuals : qualifications + "<br>" + qctoQuals;
								}

								String skillsProgrammes = fetchRecommendedItems(assessorPerson, I_ZZLinkAssessorSkillsProgramme.Table_Name,
																			I_ZZSkillsProgramme.Table_Name, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID,
																			I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, po.get_TrxName());

								String qctoSkills = fetchRecommendedItems(assessorPerson, I_ZZLinkAssessorSkillsProgramme.Table_Name,
																			I_ZZQctoSkillsProgramme.Table_Name, I_ZZQctoSkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID,
																			I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended, I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode, I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, po.get_TrxName());

								if (!Util.isEmpty(qctoSkills)) {
									skillsProgrammes = Util.isEmpty(skillsProgrammes) ? qctoSkills : skillsProgrammes + "<br>" + qctoSkills;
								}

								String subject = approvedMailTemplate.getMailHeader();
								String msgBody = approvedMailTemplate.getMailText(true);

								if (msgBody != null)
								{
									msgBody = msgBody.replace("@AssessorName@", assessorNameStr);
									msgBody = msgBody.replace("@ModeratorName@", assessorNameStr);
									msgBody = msgBody.replace("@ZZ_Assessor@", zzAssessor);
									msgBody = msgBody.replace("@ZZ_Moderator@", zzModerator);
									msgBody = msgBody.replace("@StartDate@", startDateStr);
									msgBody = msgBody.replace("@EndDate@", endDateStr);
									msgBody = msgBody.replace("@Qualifications@", qualifications);
									msgBody = msgBody.replace("@SkillsProgrammes@", skillsProgrammes);
								}

								MClient client = MClient.get(po.getCtx(), po.getAD_Client_ID());
								
								if (!Util.isEmpty(sdpEmail))
								{
									File pdfAttachment = null;
									try
									{
										HashMap<String, Object> jasperParams = new HashMap<>();
										jasperParams.put("HeaderImagePath", NtierModelValidator.class.getResource(
																													"/za/co/ntier/wsp_atr/report/jrxmls/MQA_Address_Logo_Header.png"));
										jasperParams.put("FooterImagePath", NtierModelValidator.class.getResource(
																													"/za/co/ntier/wsp_atr/report/jrxmls/MQA-Footer-Asse-Mod-Approval.png"));
										jasperParams.put("AssessorFullName", (assessorNameStr).trim());
										jasperParams.put("IDNumber", assessorUser.getZZ_ID_Passport_No() != null	? assessorUser.getZZ_ID_Passport_No()
																													: assessorUser.getZZOtherIDNo());
										
										MBPartner bp = (MBPartner) createdByUser.getC_BPartner();
										String bpName = bp != null ? bp.getName() : "";

										jasperParams.put("SDPName", bpName);

										X_C_BPartner_Location bpLoc = (X_C_BPartner_Location) MTable.get(Env.getCtx(), X_C_BPartner_Location.Table_ID).getPO(
																																								"C_BPartner_ID = "
																																								+ bp.getC_BPartner_ID(),
																																								null);

										String locationStr = "";
										if (bpLoc != null)
										{
											MLocation loc = MLocation.get(po.getCtx(), bpLoc.getC_Location_ID(), po.get_TrxName());
											if (loc != null)
												locationStr = loc.toString();
										}
										jasperParams.put("SDPLinkedBPLocation", locationStr);
										jasperParams.put("AssessorShortName", (fName).trim());
										jasperParams.put("RegistrationTitle", ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()) ? "MODERATOR REGISTRATION" : "ASSESSOR REGISTRATION");
										jasperParams.put("RegistrationRole", ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()) ? "a moderator" : "an assessor");
										jasperParams.put("RegistrationNumber", Util.isEmpty(zzAssessor) ? zzModerator : zzAssessor);
										jasperParams.put("DateOfRegistration", startDateStr);
										jasperParams.put("EndDate", endDateStr);
										jasperParams.put("Qualifications", qualifications);
										jasperParams.put("SkillsProgrammes", skillsProgrammes);
										jasperParams.put("AnnexureDataSource", buildAnnexureDataSource(assessorPerson, po.get_TrxName()));

										String letterPrefix = ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()) ? "Moderator_Approval_Letter"
																															: "Assessor_Approval_Letter";
										String jasperName = ROLE_MODERATOR.equals(assessorPerson.getZZAssessorRole()) ? "ModeratorApprovalLetter"
																															: "AssessorApprovalLetter";

										try (InputStream jasperStream = NtierModelValidator.class
																									.getResourceAsStream("/za/co/ntier/wsp_atr/report/jrxmls/" + jasperName + ".jasper"))
										{
											if (jasperStream == null)
												throw new IOException(jasperName + ".jasper not found on classpath");

											JasperPrint jasperPrint = JasperFillManager.fillReport(
																									jasperStream, jasperParams, new JREmptyDataSource(1));

											File tempDir = new File(System.getProperty("java.io.tmpdir"), "approval_" + System.currentTimeMillis());
											tempDir.mkdirs();
											pdfAttachment = new File(tempDir, letterPrefix + ".pdf");
											JasperExportManager.exportReportToPdfFile(
																						jasperPrint, pdfAttachment.getAbsolutePath());
										}

										client.sendEMail(sdpEmail, subject, msgBody, pdfAttachment, approvedMailTemplate.isHtml());
									}
									catch (Exception pdfEx)
									{
										log.severe("Failed to generate approval letter PDF: " + pdfEx.getMessage());
										client.sendEMail(sdpEmail, subject, msgBody, null, approvedMailTemplate.isHtml());
									}
									finally
									{
										if (pdfAttachment != null && pdfAttachment.exists())
										{
											File parentDir = pdfAttachment.getParentFile();
											pdfAttachment.delete();
											if (parentDir != null && parentDir.exists() && parentDir.getName().startsWith("approval_"))
												parentDir.delete();
										}
									}
								}


							}
							else
							{
								log.severe("Mail template could not be found for approved assessor/moderator email.");
							}
						}
					}
				}

				if (X_ZZAssessorPerson.ZZ_DOCSTATUS_Approved.equals(newStatus) || X_ZZAssessorPerson.ZZ_DOCSTATUS_NotApproved.equals(newStatus))
				{
					String notRecQuals = fetchNotRecommendedItems(assessorPerson, I_ZZLinkAssessorQualification.Table_Name,
																	I_ZZQualification.Table_Name, I_ZZQualification.COLUMNNAME_ZZQualification_ID,
																	I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended,
																	I_ZZQualification.COLUMNNAME_ZZSaqaQualificationCode, I_ZZQualification.COLUMNNAME_ZZSaqaQualificationTitle, po.get_TrxName());

					String notRecQctoQuals = fetchNotRecommendedItems(assessorPerson, I_ZZLinkAssessorQualification.Table_Name,
																		I_ZZQctoQualification.Table_Name,
																		I_ZZQctoQualification.COLUMNNAME_ZZQctoQualification_ID,
																		I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended,
																		I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationCode, I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationTitle, po.get_TrxName());

					if (!Util.isEmpty(notRecQctoQuals))
					{
						notRecQuals = Util.isEmpty(notRecQuals) ? notRecQctoQuals : notRecQuals + "<br>" + notRecQctoQuals;
					}

					String notRecSkills = fetchNotRecommendedItems(assessorPerson, I_ZZLinkAssessorSkillsProgramme.Table_Name,
																	I_ZZSkillsProgramme.Table_Name, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID,
																	I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended,
																	I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, po.get_TrxName());

					String notRecQctoSkills = fetchNotRecommendedItems(assessorPerson, I_ZZLinkAssessorSkillsProgramme.Table_Name,
																		I_ZZQctoSkillsProgramme.Table_Name,
																		I_ZZQctoSkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID,
																		I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended,
																		I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode, I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, po.get_TrxName());

					if (!Util.isEmpty(notRecQctoSkills))
					{
						notRecSkills = Util.isEmpty(notRecSkills) ? notRecQctoSkills : notRecSkills + "<br>" + notRecQctoSkills;
					}

					if (!Util.isEmpty(notRecQuals) || !Util.isEmpty(notRecSkills))
					{
						int assessorUserIdNotRec = assessorPerson.getAD_User_ID();
						if (assessorUserIdNotRec > 0)
						{
							MUser_New assessorUser = new MUser_New(po.getCtx(), assessorUserIdNotRec, po.get_TrxName());

							int createdByUserId = assessorPerson.getCreatedBy();
							MUser_New createdByUser = new MUser_New(po.getCtx(), createdByUserId, po.get_TrxName());
							String recipientEmail = createdByUser.getEMail();

							if (!Util.isEmpty(recipientEmail))
							{
								boolean isScopeExt = assessorPerson.getParent_ID() > 0;
								String notRecTemplateUU = ROLE_ASSESSOR.equals(assessorPerson.getZZAssessorRole())
										? (isScopeExt ? ASSESSOR_SCOPE_EXT_NOT_RECOMMENDED_MAIL_TEMPLATE_UU : ASSESSOR_NOT_RECOMMENDED_MAIL_TEMPLATE_UU)
										: (isScopeExt ? MODERATOR_SCOPE_EXT_NOT_RECOMMENDED_MAIL_TEMPLATE_UU : MODERATOR_NOT_RECOMMENDED_MAIL_TEMPLATE_UU);

								var notRecMailTemplate = (MMailText) new Query(po.getCtx(), MMailText.Table_Name, "R_MailText_UU=?", po.get_TrxName())
																																						.setParameters(notRecTemplateUU)
																																						.first();

								if (notRecMailTemplate != null)
								{
									String fName = assessorUser.getZZFirstName() != null ? assessorUser.getZZFirstName().trim() : "";
									String sName = assessorUser.getZZSurname() != null ? assessorUser.getZZSurname().trim() : "";
									String assessorNameStr = (fName + " " + sName).replaceAll("\\s+", " ").trim();
									if (Util.isEmpty(assessorNameStr))
										assessorNameStr = "";

									String zzAssessor = assessorPerson.getZZ_Assessor();
									if (zzAssessor == null)
										zzAssessor = "";

									String zzModerator = assessorPerson.getZZ_Moderator();
									if (zzModerator == null)
										zzModerator = "";

									String subject = notRecMailTemplate.getMailHeader();
									String msgBody = notRecMailTemplate.getMailText(true);

									if (msgBody != null)
									{
										msgBody = msgBody.replace("@AssessorName@", assessorNameStr);
										msgBody = msgBody.replace("@ModeratorName@", assessorNameStr);
										msgBody = msgBody.replace("@ZZ_Assessor@", zzAssessor);
										msgBody = msgBody.replace("@ZZ_Moderator@", zzModerator);
										msgBody = msgBody.replace("@NotRecommendedQualifications@", notRecQuals);
										msgBody = msgBody.replace("@NotRecommendedSkillsProgrammes@", notRecSkills);
									}

									MClient client = MClient.get(po.getCtx(), po.getAD_Client_ID());
									client.sendEMail(recipientEmail, subject, msgBody, null, notRecMailTemplate.isHtml());
								}
								else
								{
									log.warning("Mail template for 'Not Recommended' items not found (UU: " + notRecTemplateUU + ")");
								}
							}
						}
					}
				}

				if (X_ZZAssessorPerson.ZZ_DOCSTATUS_Recommended.equals(newStatus))
				{
					String sql = "SELECT u.AD_User_ID FROM AD_User u "
									+ "INNER JOIN AD_User_Roles ur ON (u.AD_User_ID = ur.AD_User_ID) "
									+ "INNER JOIN AD_Role r ON (ur.AD_Role_ID = r.AD_Role_ID) "
									+ "WHERE r.AD_Role_UU = '" + ROLE_MGR_QA_AI + "'"
									+ "AND u.NotificationType IN ('B', 'E') "
									+ "AND u.IsActive = 'Y' AND ur.IsActive = 'Y' AND r.IsActive = 'Y'";

					boolean isScopeExt = assessorPerson.getParent_ID() > 0;
					String templateUU = ROLE_ASSESSOR.equals(assessorPerson.getZZAssessorRole())
							? (isScopeExt ? ASSESSOR_SCOPE_EXT_APPROVAL_MAIL_TEMPLATE_UU : ASSESSOR_APPROVAL_MAIL_TEMPLATE_UU)
							: (isScopeExt ? MODERATOR_SCOPE_EXT_APPROVAL_MAIL_TEMPLATE_UU : MODERATOR_APPROVAL_MAIL_TEMPLATE_UU);

					var mailTemplate = (MMailText) new Query(po.getCtx(), MMailText.Table_Name, "R_MailText_UU=?", po.get_TrxName())
																																	.setParameters(templateUU)
																																	.first();

					if (mailTemplate != null)
					{
						try (PreparedStatement pstmt = DB.prepareStatement(sql, po.get_TrxName()))
						{
							try (ResultSet rs = pstmt.executeQuery())
							{
								MClient client = MClient.get(po.getCtx(), po.getAD_Client_ID());

								String assessorNameStr = "";
								int assessorUserId = assessorPerson.getAD_User_ID();
								if (assessorUserId > 0)
								{
									MUser_New assessorUser = new MUser_New(po.getCtx(), assessorUserId, po.get_TrxName());
									String fName = assessorUser.getZZFirstName() != null ? assessorUser.getZZFirstName().trim() : "";
									String mName = assessorUser.getZZMiddleName() != null ? assessorUser.getZZMiddleName().trim() : "";
									String sName = assessorUser.getZZSurname() != null ? assessorUser.getZZSurname().trim() : "";
									assessorNameStr = (fName + " " + mName + " " + sName).replaceAll("\\s+", " ").trim();
								}

								if (Util.isEmpty(assessorNameStr))
									assessorNameStr = "";

								while (rs.next())
								{
									int adUserId = rs.getInt(1);
									MUser user = new MUser(po.getCtx(), adUserId, po.get_TrxName());
									if (user.getEMail() != null && !user.getEMail().isBlank())
									{
										String subject = mailTemplate.getMailHeader();
										String msgBody = mailTemplate.getMailText(true);
										if (msgBody != null)
										{
											msgBody = msgBody.replace("@Name@", user.getName() != null ? user.getName() : "");
											msgBody = msgBody.replace("@AssessorName@", assessorNameStr);
											msgBody = msgBody.replace("@ModeratorName@", assessorNameStr);
										}

										client.sendEMail(user.getEMail(), subject, msgBody, null, mailTemplate.isHtml());

										var noteMsgBody = msgBody;
										if (noteMsgBody != null && noteMsgBody.contains("Regards,"))
										{
											noteMsgBody = noteMsgBody	.substring(0, noteMsgBody.lastIndexOf("Regards,"))
																		.replaceAll("(?i)<br\\s*/?>\\s*$", "")
																		.stripTrailing();
										}

										MNote note = new MNote(	po.getCtx(), 0, user.getAD_User_ID(),
																X_ZZAssessorPerson_v.Table_ID, po.get_ID(),
																subject, noteMsgBody, po.get_TrxName());
										note.setAD_Org_ID(po.getAD_Org_ID());
										note.saveEx();
									}
								}
							}
						}
						catch (Exception e)
						{
							log.severe("Error sending assessor registration approval email: " + e.getMessage());
						}
					}
					else
					{
						log.severe("Mail template (UU: " + ASSESSOR_APPROVAL_MAIL_TEMPLATE_UU + ") could not be found.");
					}
				}
			}
		}
		
		if (type == ModelValidator.TYPE_AFTER_CHANGE	&&
			(X_ZZ_Allocations.Table_Name.equals(po.get_TableName()) ||
				X_ZZ_QAAuditAllocations.Table_Name.equals(po.get_TableName())))
		{
			updateRelatedAllocationStatus(po);
		}
		return null;
	}

	private void updateRelatedAllocationStatus(PO po)
	{
		String colStatus = I_ZZ_Allocations.COLUMNNAME_ZZ_DocStatus;
		if (!po.is_ValueChanged(colStatus))
			return;

		String newStatus = po.get_ValueAsString(colStatus);
		if (Util.isEmpty(newStatus))
			return;

		updateIfPresent(po, I_ZZ_Allocations.COLUMNNAME_ZZ_QCTO_Alloc_OC_ID, I_ZZ_QCTO_Alloc_OC.Table_Name, colStatus, newStatus);
		updateIfPresent(po, I_ZZ_Allocations.COLUMNNAME_ZZ_QCTO_Alloc_Skills_ID, I_ZZ_QCTO_Alloc_Skills.Table_Name, colStatus, newStatus);
		updateIfPresent(po, I_ZZ_Allocations.COLUMNNAME_ZZ_QCTO_Alloc_AC_ID, I_ZZ_QCTO_Alloc_AC.Table_Name, colStatus, newStatus);
		updateIfPresent(po, I_ZZ_Allocations.COLUMNNAME_ZZ_NAMB_Alloc_Trades_ID, I_ZZ_NAMB_Alloc_Trades.Table_Name, colStatus, newStatus);
		updateIfPresent(po, I_ZZ_Allocations.COLUMNNAME_ZZ_NAMB_Alloc_TTC_ID, I_ZZ_NAMB_Alloc_TTC.Table_Name, colStatus, newStatus);
	}

	private void updateIfPresent(PO po, String idColumn, String tableName, String statusColumn, String newStatus)
	{
		int recordId = po.get_ValueAsInt(idColumn);
		if (recordId <= 0)
			return;

		MTable table = MTable.get(po.getCtx(), tableName);
		if (table == null)
			return;

		PO relatedPO = table.getPO(recordId, po.get_TrxName());
		if (relatedPO != null)
		{
			relatedPO.set_ValueNoCheck(statusColumn, newStatus);
			relatedPO.saveEx();
		}
	}

	private String fetchRecommendedItems(X_ZZAssessorPerson assessorPerson, String linkTableName, String masterTableName, String masterKeyCol, String isRecommendedCol,
											String codeCol, String titleCol, String trxName)
	{
		return fetchItemsInternal(assessorPerson, linkTableName, masterTableName, masterKeyCol, isRecommendedCol, codeCol, titleCol, true, trxName);
	}

	private String fetchNotRecommendedItems(X_ZZAssessorPerson assessorPerson, String linkTableName, String masterTableName, String masterKeyCol, String isRecommendedCol,
											String codeCol, String titleCol, String trxName)
	{
		return fetchItemsInternal(assessorPerson, linkTableName, masterTableName, masterKeyCol, isRecommendedCol, codeCol, titleCol, false, trxName);
	}

	private List<Map<String, String>> fetchItemsAsList(X_ZZAssessorPerson assessorPerson, String linkTableName, String masterTableName, String masterKeyCol,
														String isRecommendedCol,
														String codeCol, String titleCol, boolean fetchRecommended, String trxName)
	{
		List<Map<String, String>> list = new ArrayList<>();
		String selectClause = "SELECT DISTINCT m." + codeCol + ", m." + titleCol + (fetchRecommended ? "" : ", l.Comments");
		String recommendedCondition = fetchRecommended
														? "AND (l." + isRecommendedCol + " = '" + X_ZZLinkAssessorQualification.ZZ_ISRECOMMENDED_Yes + "' OR l."
															+ isRecommendedCol + " IS NULL) "
														: "AND l." + isRecommendedCol + " = '" + X_ZZLinkAssessorQualification.ZZ_ISRECOMMENDED_No + "' ";

		String sql;
		if (fetchRecommended)
		{
			sql = selectClause	+ " FROM " + linkTableName + " l "
							+ "INNER JOIN " + masterTableName + " m ON (l." + masterKeyCol + " = m." + masterKeyCol + ") "
							+ "WHERE l." + I_ZZAssessorPerson.COLUMNNAME_ZZAssessorPerson_ID + " IN "
							+ "(SELECT ZZAssessorPerson_ID FROM " + I_ZZAssessorPerson.Table_Name + " WHERE (ZZAssessorPerson_ID = ? OR Parent_ID = ?) "
							+ "AND (ZZ_DocStatus = '" + X_ZZAssessorPerson.ZZ_DOCSTATUS_Approved + "' OR ZZAssessorPerson_ID = ?)) "
							+ "AND l.IsActive = 'Y' AND m.IsActive = 'Y' "
							+ recommendedCondition;
		}
		else
		{
			sql = selectClause	+ " FROM " + linkTableName + " l "
							+ "INNER JOIN " + masterTableName + " m ON (l." + masterKeyCol + " = m." + masterKeyCol + ") "
							+ "WHERE l." + I_ZZAssessorPerson.COLUMNNAME_ZZAssessorPerson_ID + " = ? AND l.IsActive = 'Y' AND m.IsActive = 'Y' "
							+ recommendedCondition;
		}

		try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName))
		{
			if (fetchRecommended)
			{
				int ultimateParentId = assessorPerson.getParent_ID() > 0 ? assessorPerson.getParent_ID() : assessorPerson.get_ID();
				pstmt.setInt(1, ultimateParentId);
				pstmt.setInt(2, ultimateParentId);
				pstmt.setInt(3, assessorPerson.get_ID());
			}
			else
			{
				pstmt.setInt(1, assessorPerson.get_ID());
			}
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String val = rs.getString(1);
					String name = rs.getString(2);
					String comments = fetchRecommended ? null : rs.getString(3);

					Map<String, String> row = new HashMap<>();
					row.put("Code", val != null ? val.trim() : "");
					row.put("Title", name != null ? name.trim() : "");
					row.put("Comments", comments != null ? comments.trim() : "");
					list.add(row);
				}
			}
		}
		catch (Exception e)
		{
			log.severe("Error fetching items from " + linkTableName + ": " + e.getMessage());
		}
		return list;
	}

	private String fetchItemsInternal(X_ZZAssessorPerson assessorPerson, String linkTableName, String masterTableName, String masterKeyCol, String isRecommendedCol,
										String codeCol, String titleCol, boolean fetchRecommended, String trxName)
	{
		List<Map<String, String>> items = fetchItemsAsList(assessorPerson, linkTableName, masterTableName, masterKeyCol, isRecommendedCol, codeCol, titleCol,
															fetchRecommended, trxName);
		StringBuilder sb = new StringBuilder();

		for (Map<String, String> row : items)
		{
			String val = row.get("Code");
			String name = row.get("Title");
			String comments = row.get("Comments");

			String formatted = "";
			if (!val.isBlank())
				formatted += val;
			if (!name.isBlank())
			{
				if (!formatted.isEmpty())
					formatted += fetchRecommended ? " " : " - ";
				formatted += name;
			}

			if (!fetchRecommended && !comments.isBlank())
			{
				formatted += " (<b>Reason:</b> " + comments + ")";
			}

			if (!formatted.isBlank())
			{
				if (sb.length() > 0)
					sb.append("<br>");
				sb.append(formatted);
			}
		}
		return sb.toString();
	}

	@Override
	public String docValidate(PO po, int timing)
	{
		return null;
	}

	private JRMapCollectionDataSource buildAnnexureDataSource(X_ZZAssessorPerson assessorPerson, String trxName)
	{
		List<Map<String, ?>> list = new ArrayList<>();

		list.addAll(fetchItemsAsList(assessorPerson,
				I_ZZLinkAssessorQualification.Table_Name, I_ZZQualification.Table_Name,
				I_ZZQualification.COLUMNNAME_ZZQualification_ID,
				I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended,
				I_ZZQualification.COLUMNNAME_ZZSaqaQualificationCode,
				I_ZZQualification.COLUMNNAME_ZZSaqaQualificationTitle, true, trxName));

		list.addAll(fetchItemsAsList(assessorPerson,
				I_ZZLinkAssessorQualification.Table_Name, I_ZZQctoQualification.Table_Name,
				I_ZZQctoQualification.COLUMNNAME_ZZQctoQualification_ID,
				I_ZZLinkAssessorQualification.COLUMNNAME_ZZ_isRecommended,
				I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationCode,
				I_ZZQctoQualification.COLUMNNAME_ZZSaqaQualificationTitle, true, trxName));

		list.addAll(fetchItemsAsList(assessorPerson,
				I_ZZLinkAssessorSkillsProgramme.Table_Name, I_ZZSkillsProgramme.Table_Name,
				I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID,
				I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended,
				I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode,
				I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, true, trxName));

		list.addAll(fetchItemsAsList(assessorPerson,
				I_ZZLinkAssessorSkillsProgramme.Table_Name, I_ZZQctoSkillsProgramme.Table_Name,
				I_ZZQctoSkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID,
				I_ZZLinkAssessorSkillsProgramme.COLUMNNAME_ZZ_isRecommended,
				I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeCode,
				I_ZZQctoSkillsProgramme.COLUMNNAME_ZZSkillsProgrammeTitle, true, trxName));

		return new JRMapCollectionDataSource(list);
	}

}
