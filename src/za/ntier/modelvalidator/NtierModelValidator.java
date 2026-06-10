package za.ntier.modelvalidator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.compiere.model.MClient;
import org.compiere.model.MSequence;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MMailText;
import org.compiere.model.Query;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Util;

import za.co.ntier.api.model.MUser_New;
import za.co.ntier.api.model.I_ZZAssessorPerson;
import za.co.ntier.api.model.I_ZZLinkAssessorQualification;
import za.co.ntier.api.model.I_ZZLinkAssessorSkillsProgramme;
import za.co.ntier.api.model.I_ZZQualification;
import za.co.ntier.api.model.I_ZZSkillsProgramme;
import za.co.ntier.api.model.I_ZZ_Allocations;
import za.co.ntier.api.model.I_ZZ_NAMB_Alloc_TTC;
import za.co.ntier.api.model.I_ZZ_NAMB_Alloc_Trades;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_AC;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_OC;
import za.co.ntier.api.model.I_ZZ_QCTO_Alloc_Skills;
import za.co.ntier.api.model.I_ZZ_WPA_Application;
import za.co.ntier.api.model.X_ZZAssessorPerson;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_QAAuditAllocations;
import za.co.ntier.api.model.X_ZZ_WPA_Application;

public class NtierModelValidator implements ModelValidator
{
	private static CLogger	log				= CLogger.getCLogger(NtierModelValidator.class);
	private static final String ASSESSOR_APPROVAL_MAIL_TEMPLATE_UU = "fa008fdb-5ba8-4f28-87d5-8b15bfdb6e1f";
	private static final String ASSESSOR_APPROVED_MAIL_TEMPLATE_UU = "9f932747-a3e7-48ad-9c9f-5527b43c164d";
	private int				m_AD_Client_ID	= -1;
	private static final String ROLE_MGR_QA_AI = "635702d2-8ffb-4a31-a585-d2960d86383c";

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
					assessorPerson.setEndDate(Timestamp.valueOf(now.plusYears(3))); // Set end date to 3 year from now
					
					if (Util.isEmpty(assessorPerson.getZZ_Assessor()))
					{
						var assDocNo = assessorPerson.getDocumentNo();
						
						if (!Util.isEmpty(assDocNo))
						{
							var dateStr = now.format(DateTimeFormatter.ofPattern("ddMMyy"));
							if (assessorPerson.getZZAssessorRole().equals("Assessor"))
							{
								var assessorNo = "MQA/ASS" + assDocNo + "/" + dateStr;
								assessorPerson.setZZ_Assessor(assessorNo);
							}
							else if (assessorPerson.getZZAssessorRole().equals("Moderator"))
							{
								var moderatorNo = "MQA/MOD" + assDocNo + "/" + dateStr;
								assessorPerson.setZZ_Moderator(moderatorNo);
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

						if (!Util.isEmpty(assessorEmail))
						{
							var approvedMailTemplate = (MMailText) new Query(po.getCtx(), MMailText.Table_Name, "R_MailText_UU=?", po.get_TrxName())
																																					.setParameters(ASSESSOR_APPROVED_MAIL_TEMPLATE_UU)
																																					.first();

							if (approvedMailTemplate != null)
							{
								String fName = assessorUser.getZZFirstName() != null ? assessorUser.getZZFirstName().trim() : "";
								String sName = assessorUser.getZZSurname() != null ? assessorUser.getZZSurname().trim() : "";
								String assessorNameStr = (fName + " " + sName).replaceAll("\\s+", " ").trim();
								if (Util.isEmpty(assessorNameStr))
									assessorNameStr = "";

								String zzAssessor = assessorPerson.getZZ_Assessor();
								if (zzAssessor == null)
									zzAssessor = "";

								String startDateStr = "";
								if (assessorPerson.getStartDate() != null)
									startDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(assessorPerson.getStartDate());

								String endDateStr = "";
								if (assessorPerson.getEndDate() != null)
									endDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(assessorPerson.getEndDate());

								String qualifications = fetchLinkedItems(	assessorPerson.get_ID(), I_ZZLinkAssessorQualification.Table_Name,
																			I_ZZQualification.Table_Name, I_ZZQualification.COLUMNNAME_ZZQualification_ID, po
																																								.get_TrxName());
								String skillsProgrammes = fetchLinkedItems(	assessorPerson.get_ID(), I_ZZLinkAssessorSkillsProgramme.Table_Name,
																			I_ZZSkillsProgramme.Table_Name, I_ZZSkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID,
																			po.get_TrxName());

								String subject = approvedMailTemplate.getMailHeader();
								String msgBody = approvedMailTemplate.getMailText(true);

								if (msgBody != null)
								{
									msgBody = msgBody.replace("@AssessorName@", assessorNameStr);
									msgBody = msgBody.replace("@ZZ_Assessor@", zzAssessor);
									msgBody = msgBody.replace("@StartDate@", startDateStr);
									msgBody = msgBody.replace("@EndDate@", endDateStr);
									msgBody = msgBody.replace("@Qualifications@", qualifications);
									msgBody = msgBody.replace("@SkillsProgrammes@", skillsProgrammes);
								}

								MClient client = MClient.get(po.getCtx(), po.getAD_Client_ID());
								client.sendEMail(assessorEmail, subject, msgBody, null, approvedMailTemplate.isHtml());
							}
							else
							{
								log.severe("Mail template (UU: " + ASSESSOR_APPROVED_MAIL_TEMPLATE_UU + ") could not be found for approved assessor email.");
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

					var mailTemplate = (MMailText) new Query(po.getCtx(), MMailText.Table_Name, "R_MailText_UU=?", po.get_TrxName())
																																	.setParameters(ASSESSOR_APPROVAL_MAIL_TEMPLATE_UU)
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
										}

										client.sendEMail(user.getEMail(), subject, msgBody, null, mailTemplate.isHtml());
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

	private String fetchLinkedItems(int assessorPersonId, String linkTableName, String masterTableName, String masterKeyCol, String trxName)
	{
		StringBuilder sb = new StringBuilder();
		String sql = "SELECT m.Value, m.Name FROM " + linkTableName + " l "
				+ "INNER JOIN " + masterTableName + " m ON (l." + masterKeyCol + " = m." + masterKeyCol + ") "
				+ "WHERE l." + I_ZZAssessorPerson.COLUMNNAME_ZZAssessorPerson_ID + " = ? AND l.IsActive = 'Y' AND m.IsActive = 'Y'";
		try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName))
		{
			pstmt.setInt(1, assessorPersonId);
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String val = rs.getString(1);
					String name = rs.getString(2);
					String formatted = "";
					if (val != null && !val.isBlank()) formatted += val.trim();
					if (name != null && !name.isBlank())
					{
						if (!formatted.isEmpty()) formatted += " ";
						formatted += name.trim();
					}
					
					if (!formatted.isBlank())
					{
						if (sb.length() > 0) sb.append("<br>");
						sb.append(formatted);
					}
				}
			}
		}
		catch (Exception e)
		{
			log.severe("Error fetching linked items from " + linkTableName + ": " + e.getMessage());
		}
		return sb.toString();
	}

	@Override
	public String docValidate(PO po, int timing)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * @Override public String docValidate(PO po, int timing) { log.info("PO: " +
	 * po.toString() + "   - timing : " + timing); if
	 * (po.get_TableName().equals(X_M_InOut.Table_Name )) { if (timing ==
	 * TIMING_AFTER_COMPLETE) { MInOut_New mInOut = new MInOut_New(po.getCtx(),
	 * po.get_ID(), po.get_TrxName()); X_ZZ_StockPile x_ZZ_StockPile = new
	 * X_ZZ_StockPile(po.getCtx(), mInOut.getZZ_StockPile_ID(), po.get_TrxName());
	 * BigDecimal deliveredQty = BigDecimal.ZERO; MInOut_New[] m_InOuts =
	 * mInOut.getMInOutsForStockPile(); for (MInOut_New mInOut_New:m_InOuts) {
	 * MInOutLine[] m_InOutLines = mInOut_New.getLines(); for (MInOutLine
	 * mInOutLine:m_InOutLines) { deliveredQty =
	 * deliveredQty.add(mInOutLine.getMovementQty()); } }
	 * x_ZZ_StockPile.setZZ_Used_Tonnage(deliveredQty); x_ZZ_StockPile.saveEx(); }
	 * } return null; }
	 */

}
