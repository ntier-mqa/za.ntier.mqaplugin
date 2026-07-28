/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package za.ntier.models;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ZZ_Petty_Cash_Advance_Hdr
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_Petty_Cash_Advance_Hdr")
public class X_ZZ_Petty_Cash_Advance_Hdr extends PO implements I_ZZ_Petty_Cash_Advance_Hdr, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260728L;

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Advance_Hdr (Properties ctx, int ZZ_Petty_Cash_Advance_Hdr_ID, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Advance_Hdr_ID, trxName);
      /** if (ZZ_Petty_Cash_Advance_Hdr_ID == 0)
        {
			setLine_Manager_ID (0);
			setZZ_Petty_Cash_Advance_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Advance_Hdr (Properties ctx, int ZZ_Petty_Cash_Advance_Hdr_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Advance_Hdr_ID, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Advance_Hdr_ID == 0)
        {
			setLine_Manager_ID (0);
			setZZ_Petty_Cash_Advance_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Advance_Hdr (Properties ctx, String ZZ_Petty_Cash_Advance_Hdr_UU, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Advance_Hdr_UU, trxName);
      /** if (ZZ_Petty_Cash_Advance_Hdr_UU == null)
        {
			setLine_Manager_ID (0);
			setZZ_Petty_Cash_Advance_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Advance_Hdr (Properties ctx, String ZZ_Petty_Cash_Advance_Hdr_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Advance_Hdr_UU, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Advance_Hdr_UU == null)
        {
			setLine_Manager_ID (0);
			setZZ_Petty_Cash_Advance_Hdr_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_Petty_Cash_Advance_Hdr (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_ZZ_Petty_Cash_Advance_Hdr[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Document No.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	public org.compiere.model.I_AD_User getLine_Manager() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getLine_Manager_ID(), get_TrxName());
	}

	/** Set Line Manager.
		@param Line_Manager_ID Line Manager
	*/
	public void setLine_Manager_ID (int Line_Manager_ID)
	{
		if (Line_Manager_ID < 1)
			set_Value (COLUMNNAME_Line_Manager_ID, null);
		else
			set_Value (COLUMNNAME_Line_Manager_ID, Integer.valueOf(Line_Manager_ID));
	}

	/** Get Line Manager.
		@return Line Manager	  */
	public int getLine_Manager_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line_Manager_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Total Amount.
		@param TotalAmt Total Amount
	*/
	public void setTotalAmt (BigDecimal TotalAmt)
	{
		set_ValueNoCheck (COLUMNNAME_TotalAmt, TotalAmt);
	}

	/** Get Total Amount.
		@return Total Amount
	  */
	public BigDecimal getTotalAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_TotalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Advance Balance.
		@param ZZ_Advance_Balance Advance Balance
	*/
	public void setZZ_Advance_Balance (BigDecimal ZZ_Advance_Balance)
	{
		set_Value (COLUMNNAME_ZZ_Advance_Balance, ZZ_Advance_Balance);
	}

	/** Get Advance Balance.
		@return Advance Balance	  */
	public BigDecimal getZZ_Advance_Balance()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Advance_Balance);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Credit Card Number.
		@param ZZ_Credit_Card_No Credit Card Number
	*/
	public void setZZ_Credit_Card_No (String ZZ_Credit_Card_No)
	{
		set_Value (COLUMNNAME_ZZ_Credit_Card_No, ZZ_Credit_Card_No);
	}

	/** Get Credit Card Number.
		@return Credit Card Number	  */
	public String getZZ_Credit_Card_No()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Credit_Card_No);
	}

	/** Set Date Approved.
		@param ZZ_Date_Approved Date Approved
	*/
	public void setZZ_Date_Approved (Timestamp ZZ_Date_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_Approved, ZZ_Date_Approved);
	}

	/** Get Date Approved.
		@return Date Approved	  */
	public Timestamp getZZ_Date_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Approved);
	}

	/** Set Date Completed.
		@param ZZ_Date_Completed Date Completed
	*/
	public void setZZ_Date_Completed (Timestamp ZZ_Date_Completed)
	{
		set_Value (COLUMNNAME_ZZ_Date_Completed, ZZ_Date_Completed);
	}

	/** Get Date Completed.
		@return Date Completed	  */
	public Timestamp getZZ_Date_Completed()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Completed);
	}

	/** Set Date LM Approved.
		@param ZZ_Date_LM_Approved Date LM Approved
	*/
	public void setZZ_Date_LM_Approved (Timestamp ZZ_Date_LM_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_LM_Approved, ZZ_Date_LM_Approved);
	}

	/** Get Date LM Approved.
		@return Date LM Approved	  */
	public Timestamp getZZ_Date_LM_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_LM_Approved);
	}

	/** Set Date Not Approved by LM.
		@param ZZ_Date_Not_Approved_by_LM Date Not Approved by LM
	*/
	public void setZZ_Date_Not_Approved_by_LM (Timestamp ZZ_Date_Not_Approved_by_LM)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Approved_by_LM, ZZ_Date_Not_Approved_by_LM);
	}

	/** Get Date Not Approved by LM.
		@return Date Not Approved by LM	  */
	public Timestamp getZZ_Date_Not_Approved_by_LM()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Approved_by_LM);
	}

	/** Set Date Not Approved by Snr Admin Finance.
		@param ZZ_Date_Not_Approved_by_Snr_Adm_Fin Date Not Approved by Snr Admin Finance
	*/
	public void setZZ_Date_Not_Approved_by_Snr_Adm_Fin (Timestamp ZZ_Date_Not_Approved_by_Snr_Adm_Fin)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Approved_by_Snr_Adm_Fin, ZZ_Date_Not_Approved_by_Snr_Adm_Fin);
	}

	/** Get Date Not Approved by Snr Admin Finance.
		@return Date Not Approved by Snr Admin Finance	  */
	public Timestamp getZZ_Date_Not_Approved_by_Snr_Adm_Fin()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Approved_by_Snr_Adm_Fin);
	}

	/** Set Date Submitted.
		@param ZZ_Date_Submitted Date Submitted
	*/
	public void setZZ_Date_Submitted (Timestamp ZZ_Date_Submitted)
	{
		set_Value (COLUMNNAME_ZZ_Date_Submitted, ZZ_Date_Submitted);
	}

	/** Get Date Submitted.
		@return Date Submitted	  */
	public Timestamp getZZ_Date_Submitted()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Submitted);
	}

	/** Exec Approve = AE */
	public static final String ZZ_DOCACTION_ExecApprove = "AE";
	/** Approve/Do Not Approve = AP */
	public static final String ZZ_DOCACTION_ApproveDoNotApprove = "AP";
	/** Complete = CO */
	public static final String ZZ_DOCACTION_Complete = "CO";
	/** Evaluate = EV */
	public static final String ZZ_DOCACTION_Evaluate = "EV";
	/** Final Approval/Do not Approve = FA */
	public static final String ZZ_DOCACTION_FinalApprovalDoNotApprove = "FA";
	/** PrepareCEO = PC */
	public static final String ZZ_DOCACTION_PrepareCEO = "PC";
	/** Refer Back = RB */
	public static final String ZZ_DOCACTION_ReferBack = "RB";
	/** Recommend = RE */
	public static final String ZZ_DOCACTION_Recommend = "RE";
	/** Re-Submit = RS */
	public static final String ZZ_DOCACTION_Re_Submit = "RS";
	/** Submit = S1 */
	public static final String ZZ_DOCACTION_Submit = "S1";
	/** System Only (No manual action) = S2 */
	public static final String ZZ_DOCACTION_SystemOnlyNoManualAction = "S2";
	/** Submit to Manager Finance Consumables = SC */
	public static final String ZZ_DOCACTION_SubmitToManagerFinanceConsumables = "SC";
	/** Submit to SDL Finance Mgr = SD */
	public static final String ZZ_DOCACTION_SubmitToSDLFinanceMgr = "SD";
	/** Submit to Snr Mgr LP = SL */
	public static final String ZZ_DOCACTION_SubmitToSnrMgrLP = "SL";
	/** Submit to Snr Mgr Ops = SO */
	public static final String ZZ_DOCACTION_SubmitToSnrMgrOps = "SO";
	/** Submit to Snr Mgr Projects = SP */
	public static final String ZZ_DOCACTION_SubmitToSnrMgrProjects = "SP";
	/** Submit to Snr Mgr QA = SQ */
	public static final String ZZ_DOCACTION_SubmitToSnrMgrQA = "SQ";
	/** Submit to Recommender = SR */
	public static final String ZZ_DOCACTION_SubmitToRecommender = "SR";
	/** Submit to Snr Mgr SRU = SS */
	public static final String ZZ_DOCACTION_SubmitToSnrMgrSRU = "SS";
	/** Submit to Line Manager = SU */
	public static final String ZZ_DOCACTION_SubmitToLineManager = "SU";
	/** Update = UP */
	public static final String ZZ_DOCACTION_Update = "UP";
	/** Verify = VE */
	public static final String ZZ_DOCACTION_Verify = "VE";
	/** Set Document Action.
		@param ZZ_DocAction Document Action
	*/
	public void setZZ_DocAction (String ZZ_DocAction)
	{

		set_Value (COLUMNNAME_ZZ_DocAction, ZZ_DocAction);
	}

	/** Get Document Action.
		@return Document Action	  */
	public String getZZ_DocAction()
	{
		return (String)get_Value(COLUMNNAME_ZZ_DocAction);
	}

	/** Approved By Manager Finance Consumables = AC */
	public static final String ZZ_DOCSTATUS_ApprovedByManagerFinanceConsumables = "AC";
	/** Approved = AP */
	public static final String ZZ_DOCSTATUS_Approved = "AP";
	/** Prepared for CEO = CF */
	public static final String ZZ_DOCSTATUS_PreparedForCEO = "CF";
	/** Completed = CO */
	public static final String ZZ_DOCSTATUS_Completed = "CO";
	/** Draft = DR */
	public static final String ZZ_DOCSTATUS_Draft = "DR";
	/** Error Importing = EE */
	public static final String ZZ_DOCSTATUS_ErrorImporting = "EE";
	/** Validation Error = ER */
	public static final String ZZ_DOCSTATUS_ValidationError = "ER";
	/** Evaluated = EV */
	public static final String ZZ_DOCSTATUS_Evaluated = "EV";
	/** Importing = IG */
	public static final String ZZ_DOCSTATUS_Importing = "IG";
	/** Imported = IM */
	public static final String ZZ_DOCSTATUS_Imported = "IM";
	/** In Progress = IP */
	public static final String ZZ_DOCSTATUS_InProgress = "IP";
	/** Not Recommended By Senior Mgr SDR = N1 */
	public static final String ZZ_DOCSTATUS_NotRecommendedBySeniorMgrSDR = "N1";
	/** Not Recommended By Senior Mgr Finance = N2 */
	public static final String ZZ_DOCSTATUS_NotRecommendedBySeniorMgrFinance = "N2";
	/** Not Recommended By COO = N3 */
	public static final String ZZ_DOCSTATUS_NotRecommendedByCOO = "N3";
	/** Not Recommended By CFO = N4 */
	public static final String ZZ_DOCSTATUS_NotRecommendedByCFO = "N4";
	/** Not Recommended By CEO = N5 */
	public static final String ZZ_DOCSTATUS_NotRecommendedByCEO = "N5";
	/** Not Approved by Snr Manager = NA */
	public static final String ZZ_DOCSTATUS_NotApprovedBySnrManager = "NA";
	/** Not Approved By Manager Finance Consumables = NC */
	public static final String ZZ_DOCSTATUS_NotApprovedByManagerFinanceConsumables = "NC";
	/** Not Approved By SDL Finance Mgr = ND */
	public static final String ZZ_DOCSTATUS_NotApprovedBySDLFinanceMgr = "ND";
	/** Not Approved By IT Manager = NI */
	public static final String ZZ_DOCSTATUS_NotApprovedByITManager = "NI";
	/** Not Approved by LM = NL */
	public static final String ZZ_DOCSTATUS_NotApprovedByLM = "NL";
	/** Not Approved = NP */
	public static final String ZZ_DOCSTATUS_NotApproved = "NP";
	/** Not Recommended = NR */
	public static final String ZZ_DOCSTATUS_NotRecommended = "NR";
	/** Not Approved by Snr Admin Finance = NS */
	public static final String ZZ_DOCSTATUS_NotApprovedBySnrAdminFinance = "NS";
	/** Not Verified = NV */
	public static final String ZZ_DOCSTATUS_NotVerified = "NV";
	/** Pending = PE */
	public static final String ZZ_DOCSTATUS_Pending = "PE";
	/** Query = QR */
	public static final String ZZ_DOCSTATUS_Query = "QR";
	/** Recommended By Senior Mgr Finance = R1 */
	public static final String ZZ_DOCSTATUS_RecommendedBySeniorMgrFinance = "R1";
	/** Recommended By COO = R2 */
	public static final String ZZ_DOCSTATUS_RecommendedByCOO = "R2";
	/** Recommended By CFO = R3 */
	public static final String ZZ_DOCSTATUS_RecommendedByCFO = "R3";
	/** Recommended By CEO = R4 */
	public static final String ZZ_DOCSTATUS_RecommendedByCEO = "R4";
	/** Recommended for Approval = RA */
	public static final String ZZ_DOCSTATUS_RecommendedForApproval = "RA";
	/** Recommended = RC */
	public static final String ZZ_DOCSTATUS_Recommended = "RC";
	/** Recommended By Senior Mgr SDR = RD */
	public static final String ZZ_DOCSTATUS_RecommendedBySeniorMgrSDR = "RD";
	/** Recommended for Evaluation = RE */
	public static final String ZZ_DOCSTATUS_RecommendedForEvaluation = "RE";
	/** Submitted to Manager Finance Consumables = SC */
	public static final String ZZ_DOCSTATUS_SubmittedToManagerFinanceConsumables = "SC";
	/** Submitted To SDL Finance Mgr = SD */
	public static final String ZZ_DOCSTATUS_SubmittedToSDLFinanceMgr = "SD";
	/** Submitted To IT Manager = SI */
	public static final String ZZ_DOCSTATUS_SubmittedToITManager = "SI";
	/** Submitted To IT Admin = ST */
	public static final String ZZ_DOCSTATUS_SubmittedToITAdmin = "ST";
	/** Submitted = SU */
	public static final String ZZ_DOCSTATUS_Submitted = "SU";
	/** Transfer Out = TO */
	public static final String ZZ_DOCSTATUS_TransferOut = "TO";
	/** Updated by SDR Admin = UA */
	public static final String ZZ_DOCSTATUS_UpdatedBySDRAdmin = "UA";
	/** Uploaded = UP */
	public static final String ZZ_DOCSTATUS_Uploaded = "UP";
	/** Delinked = UnSdfOrg */
	public static final String ZZ_DOCSTATUS_Delinked = "UnSdfOrg";
	/** Validating = VA */
	public static final String ZZ_DOCSTATUS_Validating = "VA";
	/** Verified = VE */
	public static final String ZZ_DOCSTATUS_Verified = "VE";
	/** Set Document Status.
		@param ZZ_DocStatus Document Status
	*/
	public void setZZ_DocStatus (String ZZ_DocStatus)
	{

		set_Value (COLUMNNAME_ZZ_DocStatus, ZZ_DocStatus);
	}

	/** Get Document Status.
		@return Document Status	  */
	public String getZZ_DocStatus()
	{
		return (String)get_Value(COLUMNNAME_ZZ_DocStatus);
	}

	/** Set Petty Cash Advance.
		@param ZZ_Petty_Cash_Advance_Hdr_ID Petty Cash Advance
	*/
	public void setZZ_Petty_Cash_Advance_Hdr_ID (int ZZ_Petty_Cash_Advance_Hdr_ID)
	{
		if (ZZ_Petty_Cash_Advance_Hdr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_ID, Integer.valueOf(ZZ_Petty_Cash_Advance_Hdr_ID));
	}

	/** Get Petty Cash Advance.
		@return Petty Cash Advance	  */
	public int getZZ_Petty_Cash_Advance_Hdr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_Petty_Cash_Advance_Hdr_UU.
		@param ZZ_Petty_Cash_Advance_Hdr_UU ZZ_Petty_Cash_Advance_Hdr_UU
	*/
	public void setZZ_Petty_Cash_Advance_Hdr_UU (String ZZ_Petty_Cash_Advance_Hdr_UU)
	{
		set_Value (COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_UU, ZZ_Petty_Cash_Advance_Hdr_UU);
	}

	/** Get ZZ_Petty_Cash_Advance_Hdr_UU.
		@return ZZ_Petty_Cash_Advance_Hdr_UU	  */
	public String getZZ_Petty_Cash_Advance_Hdr_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_UU);
	}

	/** Set Petty Cash Required By.
		@param ZZ_Petty_Cash_Required_By Petty Cash Required By
	*/
	public void setZZ_Petty_Cash_Required_By (Timestamp ZZ_Petty_Cash_Required_By)
	{
		set_Value (COLUMNNAME_ZZ_Petty_Cash_Required_By, ZZ_Petty_Cash_Required_By);
	}

	/** Get Petty Cash Required By.
		@return Petty Cash Required By	  */
	public Timestamp getZZ_Petty_Cash_Required_By()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Petty_Cash_Required_By);
	}

	public org.compiere.model.I_AD_User getZZ_Snr_Admin_Fin() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Snr_Admin_Fin_ID(), get_TrxName());
	}

	/** Set Snr Admin Finance User.
		@param ZZ_Snr_Admin_Fin_ID Snr Admin Finance User
	*/
	public void setZZ_Snr_Admin_Fin_ID (int ZZ_Snr_Admin_Fin_ID)
	{
		if (ZZ_Snr_Admin_Fin_ID < 1)
			set_Value (COLUMNNAME_ZZ_Snr_Admin_Fin_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Snr_Admin_Fin_ID, Integer.valueOf(ZZ_Snr_Admin_Fin_ID));
	}

	/** Get Snr Admin Finance User.
		@return Snr Admin Finance User	  */
	public int getZZ_Snr_Admin_Fin_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Snr_Admin_Fin_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}