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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_SDR_Temp_Branch
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_SDR_Temp_Branch")
public class X_ZZ_SDR_Temp_Branch extends PO implements I_ZZ_SDR_Temp_Branch, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260225L;

    /** Standard Constructor */
    public X_ZZ_SDR_Temp_Branch (Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName)
    {
      super (ctx, ZZ_SDR_Temp_Branch_ID, trxName);
      /** if (ZZ_SDR_Temp_Branch_ID == 0)
        {
			setName (null);
			setZZ_SDR_Temp_Branch_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Temp_Branch (Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_SDR_Temp_Branch_ID, trxName, virtualColumns);
      /** if (ZZ_SDR_Temp_Branch_ID == 0)
        {
			setName (null);
			setZZ_SDR_Temp_Branch_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Temp_Branch (Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName)
    {
      super (ctx, ZZ_SDR_Temp_Branch_UU, trxName);
      /** if (ZZ_SDR_Temp_Branch_UU == null)
        {
			setName (null);
			setZZ_SDR_Temp_Branch_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Temp_Branch (Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_SDR_Temp_Branch_UU, trxName, virtualColumns);
      /** if (ZZ_SDR_Temp_Branch_UU == null)
        {
			setName (null);
			setZZ_SDR_Temp_Branch_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_SDR_Temp_Branch (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_SDR_Temp_Branch[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set Business Partner.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get Business Partner.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Cellphonenumber.
		@param Cellphonenumber Cellphonenumber
	*/
	public void setCellphonenumber (String Cellphonenumber)
	{
		set_Value (COLUMNNAME_Cellphonenumber, Cellphonenumber);
	}

	/** Get Cellphonenumber.
		@return Cellphonenumber	  */
	public String getCellphonenumber()
	{
		return (String)get_Value(COLUMNNAME_Cellphonenumber);
	}

	/** Set Contact Name.
		@param ContactName Business Partner Contact Name
	*/
	public void setContactName (String ContactName)
	{
		set_ValueNoCheck (COLUMNNAME_ContactName, ContactName);
	}

	/** Get Contact Name.
		@return Business Partner Contact Name
	  */
	public String getContactName()
	{
		return (String)get_Value(COLUMNNAME_ContactName);
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

	/** Set EMail Address.
		@param EMail Electronic Mail Address
	*/
	public void setEMail (String EMail)
	{
		set_Value (COLUMNNAME_EMail, EMail);
	}

	/** Get EMail Address.
		@return Electronic Mail Address
	  */
	public String getEMail()
	{
		return (String)get_Value(COLUMNNAME_EMail);
	}

	/** Set Comment/Help.
		@param Help Comment or Hint
	*/
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment/Help.
		@return Comment or Hint
	  */
	public String getHelp()
	{
		return (String)get_Value(COLUMNNAME_Help);
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

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	/** Set Branch Name.
		@param ZZ_BranchName Branch Name
	*/
	public void setZZ_BranchName (String ZZ_BranchName)
	{
		set_Value (COLUMNNAME_ZZ_BranchName, ZZ_BranchName);
	}

	/** Get Branch Name.
		@return Branch Name	  */
	public String getZZ_BranchName()
	{
		return (String)get_Value(COLUMNNAME_ZZ_BranchName);
	}

	public org.compiere.model.I_AD_User getZZ_Completed() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Completed_ID(), get_TrxName());
	}

	/** Set Completed By.
		@param ZZ_Completed_ID Completed By
	*/
	public void setZZ_Completed_ID (int ZZ_Completed_ID)
	{
		if (ZZ_Completed_ID < 1)
			set_Value (COLUMNNAME_ZZ_Completed_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Completed_ID, Integer.valueOf(ZZ_Completed_ID));
	}

	/** Get Completed By.
		@return Completed By	  */
	public int getZZ_Completed_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Completed_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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
	/** Completed = CO */
	public static final String ZZ_DOCSTATUS_Completed = "CO";
	/** Draft = DR */
	public static final String ZZ_DOCSTATUS_Draft = "DR";
	/** Evaluated = EV */
	public static final String ZZ_DOCSTATUS_Evaluated = "EV";
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
	/** Not Recommended = NR */
	public static final String ZZ_DOCSTATUS_NotRecommended = "NR";
	/** Not Approved by Snr Admin Finance = NS */
	public static final String ZZ_DOCSTATUS_NotApprovedBySnrAdminFinance = "NS";
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
	/** Delinked = UnSdfOrg */
	public static final String ZZ_DOCSTATUS_Delinked = "UnSdfOrg";
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

	/** Set Landline No.
		@param ZZ_Landline_No Landline No
	*/
	public void setZZ_Landline_No (String ZZ_Landline_No)
	{
		set_Value (COLUMNNAME_ZZ_Landline_No, ZZ_Landline_No);
	}

	/** Get Landline No.
		@return Landline No	  */
	public String getZZ_Landline_No()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Landline_No);
	}

	/** Set SDR Temporary Branch.
		@param ZZ_SDR_Temp_Branch_ID SDR Temporary Branch
	*/
	public void setZZ_SDR_Temp_Branch_ID (int ZZ_SDR_Temp_Branch_ID)
	{
		if (ZZ_SDR_Temp_Branch_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_SDR_Temp_Branch_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_SDR_Temp_Branch_ID, Integer.valueOf(ZZ_SDR_Temp_Branch_ID));
	}

	/** Get SDR Temporary Branch.
		@return SDR Temporary Branch	  */
	public int getZZ_SDR_Temp_Branch_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SDR_Temp_Branch_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_SDR_Temp_Branch_UU.
		@param ZZ_SDR_Temp_Branch_UU ZZ_SDR_Temp_Branch_UU
	*/
	public void setZZ_SDR_Temp_Branch_UU (String ZZ_SDR_Temp_Branch_UU)
	{
		set_Value (COLUMNNAME_ZZ_SDR_Temp_Branch_UU, ZZ_SDR_Temp_Branch_UU);
	}

	/** Get ZZ_SDR_Temp_Branch_UU.
		@return ZZ_SDR_Temp_Branch_UU	  */
	public String getZZ_SDR_Temp_Branch_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_SDR_Temp_Branch_UU);
	}

	/** Set Temp Levy No.
		@param ZZ_TempLevyNo Temp Levy No
	*/
	public void setZZ_TempLevyNo (String ZZ_TempLevyNo)
	{
		set_Value (COLUMNNAME_ZZ_TempLevyNo, ZZ_TempLevyNo);
	}

	/** Get Temp Levy No.
		@return Temp Levy No	  */
	public String getZZ_TempLevyNo()
	{
		return (String)get_Value(COLUMNNAME_ZZ_TempLevyNo);
	}

	/** Set Trading As.
		@param ZZ_TradingAs Trading As
	*/
	public void setZZ_TradingAs (String ZZ_TradingAs)
	{
		set_Value (COLUMNNAME_ZZ_TradingAs, ZZ_TradingAs);
	}

	/** Get Trading As.
		@return Trading As	  */
	public String getZZ_TradingAs()
	{
		return (String)get_Value(COLUMNNAME_ZZ_TradingAs);
	}
}