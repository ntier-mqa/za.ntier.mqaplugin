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

/** Generated Model for ZZ_WSP_ATR_Submitted
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Submitted")
public class X_ZZ_WSP_ATR_Submitted extends PO implements I_ZZ_WSP_ATR_Submitted, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260222L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_ID, trxName);
      /** if (ZZ_WSP_ATR_Submitted_ID == 0)
        {
			setZZ_IsQuery (false);
// N
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Submitted_ID == 0)
        {
			setZZ_IsQuery (false);
// N
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_UU, trxName);
      /** if (ZZ_WSP_ATR_Submitted_UU == null)
        {
			setZZ_IsQuery (false);
// N
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Submitted_UU == null)
        {
			setZZ_IsQuery (false);
// N
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Submitted[")
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

	/** Set File Name.
		@param FileName Name of the local file or URL
	*/
	public void setFileName (String FileName)
	{
		set_Value (COLUMNNAME_FileName, FileName);
	}

	/** Get File Name.
		@return Name of the local file or URL
	  */
	public String getFileName()
	{
		return (String)get_Value(COLUMNNAME_FileName);
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

	/** Set Submitted Date.
		@param SubmittedDate Submitted Date
	*/
	public void setSubmittedDate (Timestamp SubmittedDate)
	{
		set_Value (COLUMNNAME_SubmittedDate, SubmittedDate);
	}

	/** Get Submitted Date.
		@return Submitted Date	  */
	public Timestamp getSubmittedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_SubmittedDate);
	}

	/** Set SDF Organisation.
		@param ZZSdfOrganisation_ID Link Organisation And SDF
	*/
	public void setZZSdfOrganisation_ID (int ZZSdfOrganisation_ID)
	{
		if (ZZSdfOrganisation_ID < 1)
			set_Value (COLUMNNAME_ZZSdfOrganisation_ID, null);
		else
			set_Value (COLUMNNAME_ZZSdfOrganisation_ID, Integer.valueOf(ZZSdfOrganisation_ID));
	}

	/** Get SDF Organisation.
		@return Link Organisation And SDF
	  */
	public int getZZSdfOrganisation_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZSdfOrganisation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_AppRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_AppRejectedBy_ID(), get_TrxName());
	}

	/** Set App Rejected By.
		@param ZZ_AppRejectedBy_ID App Rejected By
	*/
	public void setZZ_AppRejectedBy_ID (int ZZ_AppRejectedBy_ID)
	{
		if (ZZ_AppRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_AppRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_AppRejectedBy_ID, Integer.valueOf(ZZ_AppRejectedBy_ID));
	}

	/** Get App Rejected By.
		@return App Rejected By	  */
	public int getZZ_AppRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_AppRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set App Rejected Date.
		@param ZZ_AppRejectedDate App Rejected Date
	*/
	public void setZZ_AppRejectedDate (Timestamp ZZ_AppRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_AppRejectedDate, ZZ_AppRejectedDate);
	}

	/** Get App Rejected Date.
		@return App Rejected Date	  */
	public Timestamp getZZ_AppRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_AppRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_ApprovedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_ApprovedBy_ID(), get_TrxName());
	}

	/** Set Approved By.
		@param ZZ_ApprovedBy_ID Approved By
	*/
	public void setZZ_ApprovedBy_ID (int ZZ_ApprovedBy_ID)
	{
		if (ZZ_ApprovedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_ApprovedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_ApprovedBy_ID, Integer.valueOf(ZZ_ApprovedBy_ID));
	}

	/** Get Approved By.
		@return Approved By	  */
	public int getZZ_ApprovedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_ApprovedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Approved Date.
		@param ZZ_ApprovedDate Approved Date
	*/
	public void setZZ_ApprovedDate (Timestamp ZZ_ApprovedDate)
	{
		set_Value (COLUMNNAME_ZZ_ApprovedDate, ZZ_ApprovedDate);
	}

	/** Get Approved Date.
		@return Approved Date	  */
	public Timestamp getZZ_ApprovedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_ApprovedDate);
	}

	public org.compiere.model.I_AD_User getZZ_Approved() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Approved_ID(), get_TrxName());
	}

	/** Set Approved By.
		@param ZZ_Approved_ID Approved By
	*/
	public void setZZ_Approved_ID (int ZZ_Approved_ID)
	{
		if (ZZ_Approved_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Approved_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Approved_ID, Integer.valueOf(ZZ_Approved_ID));
	}

	/** Get Approved By.
		@return Approved By	  */
	public int getZZ_Approved_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Approved_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_AD_User getZZ_EvalRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_EvalRejectedBy_ID(), get_TrxName());
	}

	/** Set Eval Rejected By.
		@param ZZ_EvalRejectedBy_ID Eval Rejected By
	*/
	public void setZZ_EvalRejectedBy_ID (int ZZ_EvalRejectedBy_ID)
	{
		if (ZZ_EvalRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_EvalRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_EvalRejectedBy_ID, Integer.valueOf(ZZ_EvalRejectedBy_ID));
	}

	/** Get Eval Rejected By.
		@return Eval Rejected By	  */
	public int getZZ_EvalRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_EvalRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Eval Rejected Date.
		@param ZZ_EvalRejectedDate Eval Rejected Date
	*/
	public void setZZ_EvalRejectedDate (Timestamp ZZ_EvalRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_EvalRejectedDate, ZZ_EvalRejectedDate);
	}

	/** Get Eval Rejected Date.
		@return Eval Rejected Date	  */
	public Timestamp getZZ_EvalRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_EvalRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_EvaluatedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_EvaluatedBy_ID(), get_TrxName());
	}

	/** Set Evaluated By.
		@param ZZ_EvaluatedBy_ID Evaluated By
	*/
	public void setZZ_EvaluatedBy_ID (int ZZ_EvaluatedBy_ID)
	{
		if (ZZ_EvaluatedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_EvaluatedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_EvaluatedBy_ID, Integer.valueOf(ZZ_EvaluatedBy_ID));
	}

	/** Get Evaluated By.
		@return Evaluated By	  */
	public int getZZ_EvaluatedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_EvaluatedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Evaluated Date.
		@param ZZ_EvaluatedDate Evaluated Date
	*/
	public void setZZ_EvaluatedDate (Timestamp ZZ_EvaluatedDate)
	{
		set_Value (COLUMNNAME_ZZ_EvaluatedDate, ZZ_EvaluatedDate);
	}

	/** Get Evaluated Date.
		@return Evaluated Date	  */
	public Timestamp getZZ_EvaluatedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_EvaluatedDate);
	}

	/** Set Generate WSP ATR Report.
		@param ZZ_Generate_WSP_ATR_Report Generate WSP ATR Report
	*/
	public void setZZ_Generate_WSP_ATR_Report (String ZZ_Generate_WSP_ATR_Report)
	{
		set_Value (COLUMNNAME_ZZ_Generate_WSP_ATR_Report, ZZ_Generate_WSP_ATR_Report);
	}

	/** Get Generate WSP ATR Report.
		@return Generate WSP ATR Report	  */
	public String getZZ_Generate_WSP_ATR_Report()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Generate_WSP_ATR_Report);
	}

	/** Set Import Submitted Data.
		@param ZZ_Import_Submitted_Data Import Submitted Data
	*/
	public void setZZ_Import_Submitted_Data (String ZZ_Import_Submitted_Data)
	{
		set_Value (COLUMNNAME_ZZ_Import_Submitted_Data, ZZ_Import_Submitted_Data);
	}

	/** Get Import Submitted Data.
		@return Import Submitted Data	  */
	public String getZZ_Import_Submitted_Data()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Import_Submitted_Data);
	}

	/** Set Is Query.
		@param ZZ_IsQuery Is Query
	*/
	public void setZZ_IsQuery (boolean ZZ_IsQuery)
	{
		set_Value (COLUMNNAME_ZZ_IsQuery, Boolean.valueOf(ZZ_IsQuery));
	}

	/** Get Is Query.
		@return Is Query	  */
	public boolean isZZ_IsQuery()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_IsQuery);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Query Comment.
		@param ZZ_QueryComment Query Comment
	*/
	public void setZZ_QueryComment (String ZZ_QueryComment)
	{
		set_Value (COLUMNNAME_ZZ_QueryComment, ZZ_QueryComment);
	}

	/** Get Query Comment.
		@return Query Comment	  */
	public String getZZ_QueryComment()
	{
		return (String)get_Value(COLUMNNAME_ZZ_QueryComment);
	}

	public org.compiere.model.I_AD_User getZZ_RecRejectedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_RecRejectedBy_ID(), get_TrxName());
	}

	/** Set Rec Rejected By.
		@param ZZ_RecRejectedBy_ID Rec Rejected By
	*/
	public void setZZ_RecRejectedBy_ID (int ZZ_RecRejectedBy_ID)
	{
		if (ZZ_RecRejectedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_RecRejectedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_RecRejectedBy_ID, Integer.valueOf(ZZ_RecRejectedBy_ID));
	}

	/** Get Rec Rejected By.
		@return Rec Rejected By	  */
	public int getZZ_RecRejectedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_RecRejectedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rec Rejected Date.
		@param ZZ_RecRejectedDate Rec Rejected Date
	*/
	public void setZZ_RecRejectedDate (Timestamp ZZ_RecRejectedDate)
	{
		set_Value (COLUMNNAME_ZZ_RecRejectedDate, ZZ_RecRejectedDate);
	}

	/** Get Rec Rejected Date.
		@return Rec Rejected Date	  */
	public Timestamp getZZ_RecRejectedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_RecRejectedDate);
	}

	public org.compiere.model.I_AD_User getZZ_RecommendedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_RecommendedBy_ID(), get_TrxName());
	}

	/** Set Recommended By.
		@param ZZ_RecommendedBy_ID Recommended By
	*/
	public void setZZ_RecommendedBy_ID (int ZZ_RecommendedBy_ID)
	{
		if (ZZ_RecommendedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_RecommendedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_RecommendedBy_ID, Integer.valueOf(ZZ_RecommendedBy_ID));
	}

	/** Get Recommended By.
		@return Recommended By	  */
	public int getZZ_RecommendedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_RecommendedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Recommended Date.
		@param ZZ_RecommendedDate Recommended Date
	*/
	public void setZZ_RecommendedDate (Timestamp ZZ_RecommendedDate)
	{
		set_Value (COLUMNNAME_ZZ_RecommendedDate, ZZ_RecommendedDate);
	}

	/** Get Recommended Date.
		@return Recommended Date	  */
	public Timestamp getZZ_RecommendedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_RecommendedDate);
	}

	/** Draft = DR */
	public static final String ZZ_WSP_ATR_STATUS_Draft = "DR";
	/** Error Importing = EE */
	public static final String ZZ_WSP_ATR_STATUS_ErrorImporting = "EE";
	/** Validation Error = ER */
	public static final String ZZ_WSP_ATR_STATUS_ValidationError = "ER";
	/** Imported = IM */
	public static final String ZZ_WSP_ATR_STATUS_Imported = "IM";
	/** Importing = IP */
	public static final String ZZ_WSP_ATR_STATUS_Importing = "IP";
	/** Submitted = SU */
	public static final String ZZ_WSP_ATR_STATUS_Submitted = "SU";
	/** Uploaded = UP */
	public static final String ZZ_WSP_ATR_STATUS_Uploaded = "UP";
	/** Validating = VA */
	public static final String ZZ_WSP_ATR_STATUS_Validating = "VA";
	/** Set Status.
		@param ZZ_WSP_ATR_Status Status
	*/
	public void setZZ_WSP_ATR_Status (String ZZ_WSP_ATR_Status)
	{

		set_Value (COLUMNNAME_ZZ_WSP_ATR_Status, ZZ_WSP_ATR_Status);
	}

	/** Get Status.
		@return Status	  */
	public String getZZ_WSP_ATR_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Status);
	}

	/** Set WSP/ATR Submitted File.
		@param ZZ_WSP_ATR_Submitted_ID WSP/ATR Submitted File
	*/
	public void setZZ_WSP_ATR_Submitted_ID (int ZZ_WSP_ATR_Submitted_ID)
	{
		if (ZZ_WSP_ATR_Submitted_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Submitted_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Submitted_ID, Integer.valueOf(ZZ_WSP_ATR_Submitted_ID));
	}

	/** Get WSP/ATR Submitted File.
		@return WSP/ATR Submitted File
	  */
	public int getZZ_WSP_ATR_Submitted_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Submitted_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Submitted_UU.
		@param ZZ_WSP_ATR_Submitted_UU ZZ_WSP_ATR_Submitted_UU
	*/
	public void setZZ_WSP_ATR_Submitted_UU (String ZZ_WSP_ATR_Submitted_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Submitted_UU, ZZ_WSP_ATR_Submitted_UU);
	}

	/** Get ZZ_WSP_ATR_Submitted_UU.
		@return ZZ_WSP_ATR_Submitted_UU	  */
	public String getZZ_WSP_ATR_Submitted_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Submitted_UU);
	}
}