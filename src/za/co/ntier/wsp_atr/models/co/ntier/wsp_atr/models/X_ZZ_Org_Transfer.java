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
package za.co.ntier.wsp_atr.models;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_Org_Transfer
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_Org_Transfer")
public class X_ZZ_Org_Transfer extends PO implements I_ZZ_Org_Transfer, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260228L;

    /** Standard Constructor */
    public X_ZZ_Org_Transfer (Properties ctx, int ZZ_Org_Transfer_ID, String trxName)
    {
      super (ctx, ZZ_Org_Transfer_ID, trxName);
      /** if (ZZ_Org_Transfer_ID == 0)
        {
			setName (null);
			setZZ_IsApproved (false);
// N
			setZZ_Org_Transfer_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Org_Transfer (Properties ctx, int ZZ_Org_Transfer_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Org_Transfer_ID, trxName, virtualColumns);
      /** if (ZZ_Org_Transfer_ID == 0)
        {
			setName (null);
			setZZ_IsApproved (false);
// N
			setZZ_Org_Transfer_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Org_Transfer (Properties ctx, String ZZ_Org_Transfer_UU, String trxName)
    {
      super (ctx, ZZ_Org_Transfer_UU, trxName);
      /** if (ZZ_Org_Transfer_UU == null)
        {
			setName (null);
			setZZ_IsApproved (false);
// N
			setZZ_Org_Transfer_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Org_Transfer (Properties ctx, String ZZ_Org_Transfer_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Org_Transfer_UU, trxName, virtualColumns);
      /** if (ZZ_Org_Transfer_UU == null)
        {
			setName (null);
			setZZ_IsApproved (false);
// N
			setZZ_Org_Transfer_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_Org_Transfer (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_Org_Transfer[")
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

	public org.compiere.model.I_AD_User getZZ_CEOApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CEOApprovedBy_ID(), get_TrxName());
	}

	/** Set CEO Approved By.
		@param ZZ_CEOApprovedBy_ID CEO Approved By
	*/
	public void setZZ_CEOApprovedBy_ID (int ZZ_CEOApprovedBy_ID)
	{
		if (ZZ_CEOApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_CEOApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CEOApprovedBy_ID, Integer.valueOf(ZZ_CEOApprovedBy_ID));
	}

	/** Get CEO Approved By.
		@return CEO Approved By	  */
	public int getZZ_CEOApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CEOApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set CEO Approved Date.
		@param ZZ_CEOApprovedDate CEO Approved Date
	*/
	public void setZZ_CEOApprovedDate (Timestamp ZZ_CEOApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_CEOApprovedDate, ZZ_CEOApprovedDate);
	}

	/** Get CEO Approved Date.
		@return CEO Approved Date	  */
	public Timestamp getZZ_CEOApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_CEOApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_CEORejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CEORejectedBy_ID(), get_TrxName());
	}

	/** Set CEO Rejected By.
		@param ZZ_CEORejectedBy_ID CEO Rejected By
	*/
	public void setZZ_CEORejectedBy_ID (int ZZ_CEORejectedBy_ID)
	{
		if (ZZ_CEORejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_CEORejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CEORejectedBy_ID, Integer.valueOf(ZZ_CEORejectedBy_ID));
	}

	/** Get CEO Rejected By.
		@return CEO Rejected By	  */
	public int getZZ_CEORejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CEORejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set CEO Rejected Date.
		@param ZZ_CEORejectedDate CEO Rejected Date
	*/
	public void setZZ_CEORejectedDate (Timestamp ZZ_CEORejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_CEORejectedDate, ZZ_CEORejectedDate);
	}

	/** Get CEO Rejected Date.
		@return CEO Rejected Date	  */
	public Timestamp getZZ_CEORejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_CEORejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_CFOApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CFOApprovedBy_ID(), get_TrxName());
	}

	/** Set CFO Approved By.
		@param ZZ_CFOApprovedBy_ID CFO Approved By
	*/
	public void setZZ_CFOApprovedBy_ID (int ZZ_CFOApprovedBy_ID)
	{
		if (ZZ_CFOApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_CFOApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CFOApprovedBy_ID, Integer.valueOf(ZZ_CFOApprovedBy_ID));
	}

	/** Get CFO Approved By.
		@return CFO Approved By	  */
	public int getZZ_CFOApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CFOApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set CFO Approved Date.
		@param ZZ_CFOApprovedDate CFO Approved Date
	*/
	public void setZZ_CFOApprovedDate (Timestamp ZZ_CFOApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_CFOApprovedDate, ZZ_CFOApprovedDate);
	}

	/** Get CFO Approved Date.
		@return CFO Approved Date	  */
	public Timestamp getZZ_CFOApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_CFOApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_CFORejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CFORejectedBy_ID(), get_TrxName());
	}

	/** Set CFO Rejected By.
		@param ZZ_CFORejectedBy_ID CFO Rejected By
	*/
	public void setZZ_CFORejectedBy_ID (int ZZ_CFORejectedBy_ID)
	{
		if (ZZ_CFORejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_CFORejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CFORejectedBy_ID, Integer.valueOf(ZZ_CFORejectedBy_ID));
	}

	/** Get CFO Rejected By.
		@return CFO Rejected By	  */
	public int getZZ_CFORejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CFORejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set CFO Rejected Date.
		@param ZZ_CFORejectedDate CFO Rejected Date
	*/
	public void setZZ_CFORejectedDate (Timestamp ZZ_CFORejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_CFORejectedDate, ZZ_CFORejectedDate);
	}

	/** Get CFO Rejected Date.
		@return CFO Rejected Date	  */
	public Timestamp getZZ_CFORejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_CFORejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_COOApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_COOApprovedBy_ID(), get_TrxName());
	}

	/** Set COO Approved By.
		@param ZZ_COOApprovedBy_ID COO Approved By
	*/
	public void setZZ_COOApprovedBy_ID (int ZZ_COOApprovedBy_ID)
	{
		if (ZZ_COOApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_COOApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_COOApprovedBy_ID, Integer.valueOf(ZZ_COOApprovedBy_ID));
	}

	/** Get COO Approved By.
		@return COO Approved By	  */
	public int getZZ_COOApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_COOApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set COO Approved Date.
		@param ZZ_COOApprovedDate COO Approved Date
	*/
	public void setZZ_COOApprovedDate (Timestamp ZZ_COOApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_COOApprovedDate, ZZ_COOApprovedDate);
	}

	/** Get COO Approved Date.
		@return COO Approved Date	  */
	public Timestamp getZZ_COOApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_COOApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_COORejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_COORejectedBy_ID(), get_TrxName());
	}

	/** Set COO Rejected By.
		@param ZZ_COORejectedBy_ID COO Rejected By
	*/
	public void setZZ_COORejectedBy_ID (int ZZ_COORejectedBy_ID)
	{
		if (ZZ_COORejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_COORejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_COORejectedBy_ID, Integer.valueOf(ZZ_COORejectedBy_ID));
	}

	/** Get COO Rejected By.
		@return COO Rejected By	  */
	public int getZZ_COORejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_COORejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set COO Rejected Date.
		@param ZZ_COORejectedDate COO Rejected Date
	*/
	public void setZZ_COORejectedDate (Timestamp ZZ_COORejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_COORejectedDate, ZZ_COORejectedDate);
	}

	/** Get COO Rejected Date.
		@return COO Rejected Date	  */
	public Timestamp getZZ_COORejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_COORejectedDate);
	}

	/** Set Date Requested.
		@param ZZ_DateRequested Date Requested
	*/
	public void setZZ_DateRequested (Timestamp ZZ_DateRequested)
	{
		set_Value (COLUMNNAME_ZZ_DateRequested, ZZ_DateRequested);
	}

	/** Get Date Requested.
		@return Date Requested	  */
	public Timestamp getZZ_DateRequested()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_DateRequested);
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
	/** Transfer Out = TO */
	public static final String ZZ_DOCSTATUS_TransferOut = "TO";
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

	/** Set From SETA.
		@param ZZ_FromSETA From SETA
	*/
	public void setZZ_FromSETA (String ZZ_FromSETA)
	{
		set_Value (COLUMNNAME_ZZ_FromSETA, ZZ_FromSETA);
	}

	/** Get From SETA.
		@return From SETA	  */
	public String getZZ_FromSETA()
	{
		return (String)get_Value(COLUMNNAME_ZZ_FromSETA);
	}

	/** Set Is Approved.
		@param ZZ_IsApproved Is Approved
	*/
	public void setZZ_IsApproved (boolean ZZ_IsApproved)
	{
		set_Value (COLUMNNAME_ZZ_IsApproved, Boolean.valueOf(ZZ_IsApproved));
	}

	/** Get Is Approved.
		@return Is Approved	  */
	public boolean isZZ_IsApproved()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_IsApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Org Transfer.
		@param ZZ_Org_Transfer_ID Org Transfer
	*/
	public void setZZ_Org_Transfer_ID (int ZZ_Org_Transfer_ID)
	{
		if (ZZ_Org_Transfer_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Org_Transfer_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Org_Transfer_ID, Integer.valueOf(ZZ_Org_Transfer_ID));
	}

	/** Get Org Transfer.
		@return Org Transfer	  */
	public int getZZ_Org_Transfer_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Org_Transfer_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_Org_Transfer_UU.
		@param ZZ_Org_Transfer_UU ZZ_Org_Transfer_UU
	*/
	public void setZZ_Org_Transfer_UU (String ZZ_Org_Transfer_UU)
	{
		set_Value (COLUMNNAME_ZZ_Org_Transfer_UU, ZZ_Org_Transfer_UU);
	}

	/** Get ZZ_Org_Transfer_UU.
		@return ZZ_Org_Transfer_UU	  */
	public String getZZ_Org_Transfer_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Org_Transfer_UU);
	}

	public org.compiere.model.I_AD_User getZZ_SnrMgrFinApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SnrMgrFinApprovedBy_ID(), get_TrxName());
	}

	/** Set Snr Mgr Fin Approved By.
		@param ZZ_SnrMgrFinApprovedBy_ID Snr Mgr Fin Approved By
	*/
	public void setZZ_SnrMgrFinApprovedBy_ID (int ZZ_SnrMgrFinApprovedBy_ID)
	{
		if (ZZ_SnrMgrFinApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SnrMgrFinApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SnrMgrFinApprovedBy_ID, Integer.valueOf(ZZ_SnrMgrFinApprovedBy_ID));
	}

	/** Get Snr Mgr Fin Approved By.
		@return Snr Mgr Fin Approved By	  */
	public int getZZ_SnrMgrFinApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SnrMgrFinApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Snr Mgr Fin Approved Date.
		@param ZZ_SnrMgrFinApprovedDate Snr Mgr Fin Approved Date
	*/
	public void setZZ_SnrMgrFinApprovedDate (Timestamp ZZ_SnrMgrFinApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_SnrMgrFinApprovedDate, ZZ_SnrMgrFinApprovedDate);
	}

	/** Get Snr Mgr Fin Approved Date.
		@return Snr Mgr Fin Approved Date	  */
	public Timestamp getZZ_SnrMgrFinApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SnrMgrFinApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_SnrMgrFinRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SnrMgrFinRejectedBy_ID(), get_TrxName());
	}

	/** Set Snr Mgr Fin Rejected By.
		@param ZZ_SnrMgrFinRejectedBy_ID Snr Mgr Fin Rejected By
	*/
	public void setZZ_SnrMgrFinRejectedBy_ID (int ZZ_SnrMgrFinRejectedBy_ID)
	{
		if (ZZ_SnrMgrFinRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SnrMgrFinRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SnrMgrFinRejectedBy_ID, Integer.valueOf(ZZ_SnrMgrFinRejectedBy_ID));
	}

	/** Get Snr Mgr Fin Rejected By.
		@return Snr Mgr Fin Rejected By	  */
	public int getZZ_SnrMgrFinRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SnrMgrFinRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Snr Mgr Fin Rejected Date.
		@param ZZ_SnrMgrFinRejectedDate Snr Mgr Fin Rejected Date
	*/
	public void setZZ_SnrMgrFinRejectedDate (Timestamp ZZ_SnrMgrFinRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_SnrMgrFinRejectedDate, ZZ_SnrMgrFinRejectedDate);
	}

	/** Get Snr Mgr Fin Rejected Date.
		@return Snr Mgr Fin Rejected Date	  */
	public Timestamp getZZ_SnrMgrFinRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SnrMgrFinRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_SnrMgrSDRApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SnrMgrSDRApprovedBy_ID(), get_TrxName());
	}

	/** Set Snr Mgr SDR Approved By.
		@param ZZ_SnrMgrSDRApprovedBy_ID Snr Mgr SDR Approved By
	*/
	public void setZZ_SnrMgrSDRApprovedBy_ID (int ZZ_SnrMgrSDRApprovedBy_ID)
	{
		if (ZZ_SnrMgrSDRApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SnrMgrSDRApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SnrMgrSDRApprovedBy_ID, Integer.valueOf(ZZ_SnrMgrSDRApprovedBy_ID));
	}

	/** Get Snr Mgr SDR Approved By.
		@return Snr Mgr SDR Approved By	  */
	public int getZZ_SnrMgrSDRApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SnrMgrSDRApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Snr Mgr SDR Approved Date.
		@param ZZ_SnrMgrSDRApprovedDate Snr Mgr SDR Approved Date
	*/
	public void setZZ_SnrMgrSDRApprovedDate (Timestamp ZZ_SnrMgrSDRApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_SnrMgrSDRApprovedDate, ZZ_SnrMgrSDRApprovedDate);
	}

	/** Get Snr Mgr SDR Approved Date.
		@return Snr Mgr SDR Approved Date	  */
	public Timestamp getZZ_SnrMgrSDRApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SnrMgrSDRApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_SnrMgrSDRRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SnrMgrSDRRejectedBy_ID(), get_TrxName());
	}

	/** Set Snr Mgr SDR Rejected By.
		@param ZZ_SnrMgrSDRRejectedBy_ID Snr Mgr SDR Rejected By
	*/
	public void setZZ_SnrMgrSDRRejectedBy_ID (int ZZ_SnrMgrSDRRejectedBy_ID)
	{
		if (ZZ_SnrMgrSDRRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SnrMgrSDRRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SnrMgrSDRRejectedBy_ID, Integer.valueOf(ZZ_SnrMgrSDRRejectedBy_ID));
	}

	/** Get Snr Mgr SDR Rejected By.
		@return Snr Mgr SDR Rejected By	  */
	public int getZZ_SnrMgrSDRRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SnrMgrSDRRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Snr Mgr SDR Rejected Date.
		@param ZZ_SnrMgrSDRRejectedDate Snr Mgr SDR Rejected Date
	*/
	public void setZZ_SnrMgrSDRRejectedDate (Timestamp ZZ_SnrMgrSDRRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_SnrMgrSDRRejectedDate, ZZ_SnrMgrSDRRejectedDate);
	}

	/** Get Snr Mgr SDR Rejected Date.
		@return Snr Mgr SDR Rejected Date	  */
	public Timestamp getZZ_SnrMgrSDRRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SnrMgrSDRRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_SubmitRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SubmitRejectedBy_ID(), get_TrxName());
	}

	/** Set Submit Rejected By.
		@param ZZ_SubmitRejectedBy_ID Submit Rejected By
	*/
	public void setZZ_SubmitRejectedBy_ID (int ZZ_SubmitRejectedBy_ID)
	{
		if (ZZ_SubmitRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SubmitRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SubmitRejectedBy_ID, Integer.valueOf(ZZ_SubmitRejectedBy_ID));
	}

	/** Get Submit Rejected By.
		@return Submit Rejected By	  */
	public int getZZ_SubmitRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SubmitRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Submit Rejected Date.
		@param ZZ_SubmitRejectedDate Submit Rejected Date
	*/
	public void setZZ_SubmitRejectedDate (Timestamp ZZ_SubmitRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_SubmitRejectedDate, ZZ_SubmitRejectedDate);
	}

	/** Get Submit Rejected Date.
		@return Submit Rejected Date	  */
	public Timestamp getZZ_SubmitRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SubmitRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_SubmittedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SubmittedBy_ID(), get_TrxName());
	}

	/** Set Submitted By.
		@param ZZ_SubmittedBy_ID Submitted By
	*/
	public void setZZ_SubmittedBy_ID (int ZZ_SubmittedBy_ID)
	{
		if (ZZ_SubmittedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_SubmittedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SubmittedBy_ID, Integer.valueOf(ZZ_SubmittedBy_ID));
	}

	/** Get Submitted By.
		@return Submitted By	  */
	public int getZZ_SubmittedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SubmittedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Submitted Date.
		@param ZZ_SubmittedDate Submitted Date
	*/
	public void setZZ_SubmittedDate (Timestamp ZZ_SubmittedDate)
	{
		set_Value (COLUMNNAME_ZZ_SubmittedDate, ZZ_SubmittedDate);
	}

	/** Get Submitted Date.
		@return Submitted Date	  */
	public Timestamp getZZ_SubmittedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_SubmittedDate);
	}

	/** Set To SETA.
		@param ZZ_ToSETA To SETA
	*/
	public void setZZ_ToSETA (String ZZ_ToSETA)
	{
		set_Value (COLUMNNAME_ZZ_ToSETA, ZZ_ToSETA);
	}

	/** Get To SETA.
		@return To SETA	  */
	public String getZZ_ToSETA()
	{
		return (String)get_Value(COLUMNNAME_ZZ_ToSETA);
	}

	/** Transfer In = IN */
	public static final String ZZ_TRANSFERTYPE_TransferIn = "IN";
	/** Internal Transfer = INT */
	public static final String ZZ_TRANSFERTYPE_InternalTransfer = "INT";
	/** Transfer Out = OUT */
	public static final String ZZ_TRANSFERTYPE_TransferOut = "OUT";
	/** Reversal = REV */
	public static final String ZZ_TRANSFERTYPE_Reversal = "REV";
	/** Set Transfer Type.
		@param ZZ_TransferType Transfer Type
	*/
	public void setZZ_TransferType (String ZZ_TransferType)
	{

		set_Value (COLUMNNAME_ZZ_TransferType, ZZ_TransferType);
	}

	/** Get Transfer Type.
		@return Transfer Type	  */
	public String getZZ_TransferType()
	{
		return (String)get_Value(COLUMNNAME_ZZ_TransferType);
	}
}