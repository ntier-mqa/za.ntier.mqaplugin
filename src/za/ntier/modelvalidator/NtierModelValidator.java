package za.ntier.modelvalidator;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.compiere.model.MClient;
import org.compiere.model.MSequence;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

import za.co.ntier.api.model.I_ZZAssessorPerson;
import za.co.ntier.api.model.I_ZZ_WPA_Application;
import za.co.ntier.api.model.X_ZZAssessorPerson;
import za.co.ntier.api.model.X_ZZ_WPA_Application;

public class NtierModelValidator implements ModelValidator
{
	private static CLogger	log				= CLogger.getCLogger(NtierModelValidator.class);
	private int				m_AD_Client_ID	= -1;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client)
	{
		if (client != null)
		{
			m_AD_Client_ID = client.getAD_Client_ID();
		}
		engine.addModelChange(X_ZZ_WPA_Application.Table_Name, this);
		engine.addModelChange(X_ZZAssessorPerson.Table_Name, this);
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
				if (X_ZZAssessorPerson.ZZ_DOCSTATUS_Approved.equals(newStatus))
				{
					var assessorPerson = new X_ZZAssessorPerson(po.getCtx(), po.get_ID(), po.get_TrxName());
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
				}
			}
		}
		return null;
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
