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
import org.compiere.util.KeyNamePair;

/** Generated Model for M_Inventory
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="M_Inventory")
public class X_M_Inventory extends PO implements I_M_Inventory, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260813L;

    /** Standard Constructor */
    public X_M_Inventory (Properties ctx, int M_Inventory_ID, String trxName)
    {
      super (ctx, M_Inventory_ID, trxName);
      /** if (M_Inventory_ID == 0)
        {
			setC_DocType_ID (0);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
			setM_Inventory_ID (0);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setPosted (false);
			setProcessed (false);
			setZZ_AllowLineManageApproved (true);
// Y
			setZZ_AllowMgrFinConsumablesApproval (true);
// Y
			setZZ_AllowSdlLineMgrApproved (true);
// Y
			setZZ_AllowSnrAdminFinanceApproved (true);
// Y
			setZZ_Consumables_Signed_Uploaded (false);
// N
			setZZ_Is_Refreshment_Req (false);
// @ZZ_IS_REFRESHMENT_REQ@
        } */
    }

    /** Standard Constructor */
    public X_M_Inventory (Properties ctx, int M_Inventory_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, M_Inventory_ID, trxName, virtualColumns);
      /** if (M_Inventory_ID == 0)
        {
			setC_DocType_ID (0);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
			setM_Inventory_ID (0);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setPosted (false);
			setProcessed (false);
			setZZ_AllowLineManageApproved (true);
// Y
			setZZ_AllowMgrFinConsumablesApproval (true);
// Y
			setZZ_AllowSdlLineMgrApproved (true);
// Y
			setZZ_AllowSnrAdminFinanceApproved (true);
// Y
			setZZ_Consumables_Signed_Uploaded (false);
// N
			setZZ_Is_Refreshment_Req (false);
// @ZZ_IS_REFRESHMENT_REQ@
        } */
    }

    /** Standard Constructor */
    public X_M_Inventory (Properties ctx, String M_Inventory_UU, String trxName)
    {
      super (ctx, M_Inventory_UU, trxName);
      /** if (M_Inventory_UU == null)
        {
			setC_DocType_ID (0);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
			setM_Inventory_ID (0);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setPosted (false);
			setProcessed (false);
			setZZ_AllowLineManageApproved (true);
// Y
			setZZ_AllowMgrFinConsumablesApproval (true);
// Y
			setZZ_AllowSdlLineMgrApproved (true);
// Y
			setZZ_AllowSnrAdminFinanceApproved (true);
// Y
			setZZ_Consumables_Signed_Uploaded (false);
// N
			setZZ_Is_Refreshment_Req (false);
// @ZZ_IS_REFRESHMENT_REQ@
        } */
    }

    /** Standard Constructor */
    public X_M_Inventory (Properties ctx, String M_Inventory_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, M_Inventory_UU, trxName, virtualColumns);
      /** if (M_Inventory_UU == null)
        {
			setC_DocType_ID (0);
			setDocAction (null);
// CO
			setDocStatus (null);
// DR
			setDocumentNo (null);
			setIsApproved (false);
			setM_Inventory_ID (0);
			setMovementDate (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setPosted (false);
			setProcessed (false);
			setZZ_AllowLineManageApproved (true);
// Y
			setZZ_AllowMgrFinConsumablesApproval (true);
// Y
			setZZ_AllowSdlLineMgrApproved (true);
// Y
			setZZ_AllowSnrAdminFinanceApproved (true);
// Y
			setZZ_Consumables_Signed_Uploaded (false);
// N
			setZZ_Is_Refreshment_Req (false);
// @ZZ_IS_REFRESHMENT_REQ@
        } */
    }

    /** Load Constructor */
    public X_M_Inventory (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 1 - Org
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
      StringBuilder sb = new StringBuilder ("X_M_Inventory[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Trx Organization.
		@param AD_OrgTrx_ID Performing or initiating organization
	*/
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID)
	{
		if (AD_OrgTrx_ID < 1)
			set_Value (COLUMNNAME_AD_OrgTrx_ID, null);
		else
			set_Value (COLUMNNAME_AD_OrgTrx_ID, Integer.valueOf(AD_OrgTrx_ID));
	}

	/** Get Trx Organization.
		@return Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_OrgTrx_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Approval Amount.
		@param ApprovalAmt Document Approval Amount
	*/
	public void setApprovalAmt (BigDecimal ApprovalAmt)
	{
		set_Value (COLUMNNAME_ApprovalAmt, ApprovalAmt);
	}

	/** Get Approval Amount.
		@return Document Approval Amount
	  */
	public BigDecimal getApprovalAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ApprovalAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_C_Activity getC_Activity() throws RuntimeException
	{
		return (org.compiere.model.I_C_Activity)MTable.get(getCtx(), org.compiere.model.I_C_Activity.Table_ID)
			.getPO(getC_Activity_ID(), get_TrxName());
	}

	/** Set Activity.
		@param C_Activity_ID Business Activity
	*/
	public void setC_Activity_ID (int C_Activity_ID)
	{
		if (C_Activity_ID < 1)
			set_Value (COLUMNNAME_C_Activity_ID, null);
		else
			set_Value (COLUMNNAME_C_Activity_ID, Integer.valueOf(C_Activity_ID));
	}

	/** Get Activity.
		@return Business Activity
	  */
	public int getC_Activity_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Activity_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Campaign getC_Campaign() throws RuntimeException
	{
		return (org.compiere.model.I_C_Campaign)MTable.get(getCtx(), org.compiere.model.I_C_Campaign.Table_ID)
			.getPO(getC_Campaign_ID(), get_TrxName());
	}

	/** Set Campaign.
		@param C_Campaign_ID Marketing Campaign
	*/
	public void setC_Campaign_ID (int C_Campaign_ID)
	{
		if (C_Campaign_ID < 1)
			set_Value (COLUMNNAME_C_Campaign_ID, null);
		else
			set_Value (COLUMNNAME_C_Campaign_ID, Integer.valueOf(C_Campaign_ID));
	}

	/** Get Campaign.
		@return Marketing Campaign
	  */
	public int getC_Campaign_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Campaign_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ConversionType getC_ConversionType() throws RuntimeException
	{
		return (org.compiere.model.I_C_ConversionType)MTable.get(getCtx(), org.compiere.model.I_C_ConversionType.Table_ID)
			.getPO(getC_ConversionType_ID(), get_TrxName());
	}

	/** Set Currency Type.
		@param C_ConversionType_ID Currency Conversion Rate Type
	*/
	public void setC_ConversionType_ID (int C_ConversionType_ID)
	{
		if (C_ConversionType_ID < 1)
			set_Value (COLUMNNAME_C_ConversionType_ID, null);
		else
			set_Value (COLUMNNAME_C_ConversionType_ID, Integer.valueOf(C_ConversionType_ID));
	}

	/** Get Currency Type.
		@return Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ConversionType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException
	{
		return (org.compiere.model.I_C_Currency)MTable.get(getCtx(), org.compiere.model.I_C_Currency.Table_ID)
			.getPO(getC_Currency_ID(), get_TrxName());
	}

	/** Set Currency.
		@param C_Currency_ID The Currency for this record
	*/
	public void setC_Currency_ID (int C_Currency_ID)
	{
		if (C_Currency_ID < 1)
			set_Value (COLUMNNAME_C_Currency_ID, null);
		else
			set_Value (COLUMNNAME_C_Currency_ID, Integer.valueOf(C_Currency_ID));
	}

	/** Get Currency.
		@return The Currency for this record
	  */
	public int getC_Currency_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Currency_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
	{
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_ID)
			.getPO(getC_DocType_ID(), get_TrxName());
	}

	/** Set Document Type.
		@param C_DocType_ID Document type or rules
	*/
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0)
			set_Value (COLUMNNAME_C_DocType_ID, null);
		else
			set_Value (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Project getC_Project() throws RuntimeException
	{
		return (org.compiere.model.I_C_Project)MTable.get(getCtx(), org.compiere.model.I_C_Project.Table_ID)
			.getPO(getC_Project_ID(), get_TrxName());
	}

	/** Set Project.
		@param C_Project_ID Financial Project
	*/
	public void setC_Project_ID (int C_Project_ID)
	{
		if (C_Project_ID < 1)
			set_Value (COLUMNNAME_C_Project_ID, null);
		else
			set_Value (COLUMNNAME_C_Project_ID, Integer.valueOf(C_Project_ID));
	}

	/** Get Project.
		@return Financial Project
	  */
	public int getC_Project_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Project_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** CostingMethod AD_Reference_ID=122 */
	public static final int COSTINGMETHOD_AD_Reference_ID=122;
	/** Average PO = A */
	public static final String COSTINGMETHOD_AveragePO = "A";
	/** Fifo = F */
	public static final String COSTINGMETHOD_Fifo = "F";
	/** Average Invoice = I */
	public static final String COSTINGMETHOD_AverageInvoice = "I";
	/** Lifo = L */
	public static final String COSTINGMETHOD_Lifo = "L";
	/** Standard Costing = S */
	public static final String COSTINGMETHOD_StandardCosting = "S";
	/** User Defined = U */
	public static final String COSTINGMETHOD_UserDefined = "U";
	/** Last Invoice = i */
	public static final String COSTINGMETHOD_LastInvoice = "i";
	/** Last PO Price = p */
	public static final String COSTINGMETHOD_LastPOPrice = "p";
	/** _ = x */
	public static final String COSTINGMETHOD__ = "x";
	/** Set Costing Method.
		@param CostingMethod Indicates how Costs will be calculated
	*/
	public void setCostingMethod (String CostingMethod)
	{

		set_Value (COLUMNNAME_CostingMethod, CostingMethod);
	}

	/** Get Costing Method.
		@return Indicates how Costs will be calculated
	  */
	public String getCostingMethod()
	{
		return (String)get_Value(COLUMNNAME_CostingMethod);
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

	/** DocAction AD_Reference_ID=135 */
	public static final int DOCACTION_AD_Reference_ID=135;
	/** &lt;None&gt; = -- */
	public static final String DOCACTION_None = "--";
	/** Approve = AP */
	public static final String DOCACTION_Approve = "AP";
	/** Close = CL */
	public static final String DOCACTION_Close = "CL";
	/** Complete = CO */
	public static final String DOCACTION_Complete = "CO";
	/** Invalidate = IN */
	public static final String DOCACTION_Invalidate = "IN";
	/** Post = PO */
	public static final String DOCACTION_Post = "PO";
	/** Prepare = PR */
	public static final String DOCACTION_Prepare = "PR";
	/** Reverse - Accrual = RA */
	public static final String DOCACTION_Reverse_Accrual = "RA";
	/** Reverse - Correct = RC */
	public static final String DOCACTION_Reverse_Correct = "RC";
	/** Re-activate = RE */
	public static final String DOCACTION_Re_Activate = "RE";
	/** Reject = RJ */
	public static final String DOCACTION_Reject = "RJ";
	/** Void = VO */
	public static final String DOCACTION_Void = "VO";
	/** Wait Complete = WC */
	public static final String DOCACTION_WaitComplete = "WC";
	/** Unlock = XL */
	public static final String DOCACTION_Unlock = "XL";
	/** Set Document Action.
		@param DocAction The targeted status of the document
	*/
	public void setDocAction (String DocAction)
	{

		set_Value (COLUMNNAME_DocAction, DocAction);
	}

	/** Get Document Action.
		@return The targeted status of the document
	  */
	public String getDocAction()
	{
		return (String)get_Value(COLUMNNAME_DocAction);
	}

	/** DocStatus AD_Reference_ID=131 */
	public static final int DOCSTATUS_AD_Reference_ID=131;
	/** Unknown = ?? */
	public static final String DOCSTATUS_Unknown = "??";
	/** Approved = AP */
	public static final String DOCSTATUS_Approved = "AP";
	/** Closed = CL */
	public static final String DOCSTATUS_Closed = "CL";
	/** Completed = CO */
	public static final String DOCSTATUS_Completed = "CO";
	/** Drafted = DR */
	public static final String DOCSTATUS_Drafted = "DR";
	/** Invalid = IN */
	public static final String DOCSTATUS_Invalid = "IN";
	/** In Progress = IP */
	public static final String DOCSTATUS_InProgress = "IP";
	/** Not Approved = NA */
	public static final String DOCSTATUS_NotApproved = "NA";
	/** Reversed = RE */
	public static final String DOCSTATUS_Reversed = "RE";
	/** Voided = VO */
	public static final String DOCSTATUS_Voided = "VO";
	/** Waiting Confirmation = WC */
	public static final String DOCSTATUS_WaitingConfirmation = "WC";
	/** Waiting Payment = WP */
	public static final String DOCSTATUS_WaitingPayment = "WP";
	/** Set Document Status.
		@param DocStatus The current status of the document
	*/
	public void setDocStatus (String DocStatus)
	{

		set_Value (COLUMNNAME_DocStatus, DocStatus);
	}

	/** Get Document Status.
		@return The current status of the document
	  */
	public String getDocStatus()
	{
		return (String)get_Value(COLUMNNAME_DocStatus);
	}

	/** Set Document No.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_Value (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

    /** Get Record ID/ColumnName
        @return ID/ColumnName pair
      */
    public KeyNamePair getKeyNamePair()
    {
        return new KeyNamePair(get_ID(), getDocumentNo());
    }

	/** Set Generate List.
		@param GenerateList Generate List
	*/
	public void setGenerateList (String GenerateList)
	{
		set_Value (COLUMNNAME_GenerateList, GenerateList);
	}

	/** Get Generate List.
		@return Generate List
	  */
	public String getGenerateList()
	{
		return (String)get_Value(COLUMNNAME_GenerateList);
	}

	/** Set Approved.
		@param IsApproved Indicates if this document requires approval
	*/
	public void setIsApproved (boolean IsApproved)
	{
		set_Value (COLUMNNAME_IsApproved, Boolean.valueOf(IsApproved));
	}

	/** Get Approved.
		@return Indicates if this document requires approval
	  */
	public boolean isApproved()
	{
		Object oo = get_Value(COLUMNNAME_IsApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Phys.Inventory.
		@param M_Inventory_ID Parameters for a Physical Inventory
	*/
	public void setM_Inventory_ID (int M_Inventory_ID)
	{
		if (M_Inventory_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_Inventory_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_Inventory_ID, Integer.valueOf(M_Inventory_ID));
	}

	/** Get Phys.Inventory.
		@return Parameters for a Physical Inventory
	  */
	public int getM_Inventory_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Inventory_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set M_Inventory_UU.
		@param M_Inventory_UU M_Inventory_UU
	*/
	public void setM_Inventory_UU (String M_Inventory_UU)
	{
		set_Value (COLUMNNAME_M_Inventory_UU, M_Inventory_UU);
	}

	/** Get M_Inventory_UU.
		@return M_Inventory_UU	  */
	public String getM_Inventory_UU()
	{
		return (String)get_Value(COLUMNNAME_M_Inventory_UU);
	}

	public org.compiere.model.I_M_PerpetualInv getM_PerpetualInv() throws RuntimeException
	{
		return (org.compiere.model.I_M_PerpetualInv)MTable.get(getCtx(), org.compiere.model.I_M_PerpetualInv.Table_ID)
			.getPO(getM_PerpetualInv_ID(), get_TrxName());
	}

	/** Set Perpetual Inventory.
		@param M_PerpetualInv_ID Rules for generating physical inventory
	*/
	public void setM_PerpetualInv_ID (int M_PerpetualInv_ID)
	{
		if (M_PerpetualInv_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_PerpetualInv_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_PerpetualInv_ID, Integer.valueOf(M_PerpetualInv_ID));
	}

	/** Get Perpetual Inventory.
		@return Rules for generating physical inventory
	  */
	public int getM_PerpetualInv_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_PerpetualInv_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
	{
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_ID)
			.getPO(getM_Warehouse_ID(), get_TrxName());
	}

	/** Set Warehouse.
		@param M_Warehouse_ID Storage Warehouse and Service Point
	*/
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1)
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Movement Date.
		@param MovementDate Date a product was moved in or out of inventory
	*/
	public void setMovementDate (Timestamp MovementDate)
	{
		set_Value (COLUMNNAME_MovementDate, MovementDate);
	}

	/** Get Movement Date.
		@return Date a product was moved in or out of inventory
	  */
	public Timestamp getMovementDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_MovementDate);
	}

	/** Set Posted.
		@param Posted Posting status
	*/
	public void setPosted (boolean Posted)
	{
		set_Value (COLUMNNAME_Posted, Boolean.valueOf(Posted));
	}

	/** Get Posted.
		@return Posting status
	  */
	public boolean isPosted()
	{
		Object oo = get_Value(COLUMNNAME_Posted);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed()
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed On.
		@param ProcessedOn The date+time (expressed in decimal format) when the document has been processed
	*/
	public void setProcessedOn (BigDecimal ProcessedOn)
	{
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);
	}

	/** Get Processed On.
		@return The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ProcessedOn);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Process Now.
		@param Processing Process Now
	*/
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing()
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_M_Inventory getReversal() throws RuntimeException
	{
		return (org.compiere.model.I_M_Inventory)MTable.get(getCtx(), org.compiere.model.I_M_Inventory.Table_ID)
			.getPO(getReversal_ID(), get_TrxName());
	}

	/** Set Reversal ID.
		@param Reversal_ID ID of document reversal
	*/
	public void setReversal_ID (int Reversal_ID)
	{
		if (Reversal_ID < 1)
			set_Value (COLUMNNAME_Reversal_ID, null);
		else
			set_Value (COLUMNNAME_Reversal_ID, Integer.valueOf(Reversal_ID));
	}

	/** Get Reversal ID.
		@return ID of document reversal
	  */
	public int getReversal_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Reversal_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Update Quantities.
		@param UpdateQty Update Quantities
	*/
	public void setUpdateQty (String UpdateQty)
	{
		set_Value (COLUMNNAME_UpdateQty, UpdateQty);
	}

	/** Get Update Quantities.
		@return Update Quantities	  */
	public String getUpdateQty()
	{
		return (String)get_Value(COLUMNNAME_UpdateQty);
	}

	public org.compiere.model.I_C_ElementValue getUser1() throws RuntimeException
	{
		return (org.compiere.model.I_C_ElementValue)MTable.get(getCtx(), org.compiere.model.I_C_ElementValue.Table_ID)
			.getPO(getUser1_ID(), get_TrxName());
	}

	/** Set User Element List 1.
		@param User1_ID User defined list element #1
	*/
	public void setUser1_ID (int User1_ID)
	{
		if (User1_ID < 1)
			set_Value (COLUMNNAME_User1_ID, null);
		else
			set_Value (COLUMNNAME_User1_ID, Integer.valueOf(User1_ID));
	}

	/** Get User Element List 1.
		@return User defined list element #1
	  */
	public int getUser1_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User1_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ElementValue getUser2() throws RuntimeException
	{
		return (org.compiere.model.I_C_ElementValue)MTable.get(getCtx(), org.compiere.model.I_C_ElementValue.Table_ID)
			.getPO(getUser2_ID(), get_TrxName());
	}

	/** Set User Element List 2.
		@param User2_ID User defined list element #2
	*/
	public void setUser2_ID (int User2_ID)
	{
		if (User2_ID < 1)
			set_Value (COLUMNNAME_User2_ID, null);
		else
			set_Value (COLUMNNAME_User2_ID, Integer.valueOf(User2_ID));
	}

	/** Get User Element List 2.
		@return User defined list element #2
	  */
	public int getUser2_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_User2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Allow Line Manage Approved.
		@param ZZ_AllowLineManageApproved Choose to allow line manage join to approved workfllow
	*/
	public void setZZ_AllowLineManageApproved (boolean ZZ_AllowLineManageApproved)
	{
		set_ValueNoCheck (COLUMNNAME_ZZ_AllowLineManageApproved, Boolean.valueOf(ZZ_AllowLineManageApproved));
	}

	/** Get Allow Line Manage Approved.
		@return Choose to allow line manage join to approved workfllow
	  */
	public boolean isZZ_AllowLineManageApproved()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_AllowLineManageApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allow Mgr Finance Consumables Approval.
		@param ZZ_AllowMgrFinConsumablesApproval Allow Mgr Finance Consumables Approval
	*/
	public void setZZ_AllowMgrFinConsumablesApproval (boolean ZZ_AllowMgrFinConsumablesApproval)
	{
		set_ValueNoCheck (COLUMNNAME_ZZ_AllowMgrFinConsumablesApproval, Boolean.valueOf(ZZ_AllowMgrFinConsumablesApproval));
	}

	/** Get Allow Mgr Finance Consumables Approval.
		@return Allow Mgr Finance Consumables Approval	  */
	public boolean isZZ_AllowMgrFinConsumablesApproval()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_AllowMgrFinConsumablesApproval);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allow SDL Finance Manager Approval.
		@param ZZ_AllowSdlLineMgrApproved Allow SDL Finance Manager Approval
	*/
	public void setZZ_AllowSdlLineMgrApproved (boolean ZZ_AllowSdlLineMgrApproved)
	{
		set_ValueNoCheck (COLUMNNAME_ZZ_AllowSdlLineMgrApproved, Boolean.valueOf(ZZ_AllowSdlLineMgrApproved));
	}

	/** Get Allow SDL Finance Manager Approval.
		@return Allow SDL Finance Manager Approval	  */
	public boolean isZZ_AllowSdlLineMgrApproved()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_AllowSdlLineMgrApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Allow Snr Admin Finance Approved.
		@param ZZ_AllowSnrAdminFinanceApproved Choose to allow Snr Admin Finance join to approved workfllow
	*/
	public void setZZ_AllowSnrAdminFinanceApproved (boolean ZZ_AllowSnrAdminFinanceApproved)
	{
		set_ValueNoCheck (COLUMNNAME_ZZ_AllowSnrAdminFinanceApproved, Boolean.valueOf(ZZ_AllowSnrAdminFinanceApproved));
	}

	/** Get Allow Snr Admin Finance Approved.
		@return Choose to allow Snr Admin Finance join to approved workfllow
	  */
	public boolean isZZ_AllowSnrAdminFinanceApproved()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_AllowSnrAdminFinanceApproved);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Consumables Document Signed and Uploaded.
		@param ZZ_Consumables_Signed_Uploaded Consumables Document Signed and Uploaded
	*/
	public void setZZ_Consumables_Signed_Uploaded (boolean ZZ_Consumables_Signed_Uploaded)
	{
		set_Value (COLUMNNAME_ZZ_Consumables_Signed_Uploaded, Boolean.valueOf(ZZ_Consumables_Signed_Uploaded));
	}

	/** Get Consumables Document Signed and Uploaded.
		@return Consumables Document Signed and Uploaded	  */
	public boolean isZZ_Consumables_Signed_Uploaded()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Consumables_Signed_Uploaded);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Date Manager Finance Consumables.
		@param ZZ_Date_MFC_Approved Date Manager Finance Consumables
	*/
	public void setZZ_Date_MFC_Approved (Timestamp ZZ_Date_MFC_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_MFC_Approved, ZZ_Date_MFC_Approved);
	}

	/** Get Date Manager Finance Consumables.
		@return Date Manager Finance Consumables	  */
	public Timestamp getZZ_Date_MFC_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_MFC_Approved);
	}

	/** Set Date Manager Finance Not Approved.
		@param ZZ_Date_MFC_Not_Approved Date Manager Finance Not Approved
	*/
	public void setZZ_Date_MFC_Not_Approved (Timestamp ZZ_Date_MFC_Not_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_MFC_Not_Approved, ZZ_Date_MFC_Not_Approved);
	}

	/** Get Date Manager Finance Not Approved.
		@return Date Manager Finance Not Approved	  */
	public Timestamp getZZ_Date_MFC_Not_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_MFC_Not_Approved);
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

	/** Set Date Approved By SDL Finance Manager.
		@param ZZ_Date_SDL_Approved Date Approved By SDL Finance Manager
	*/
	public void setZZ_Date_SDL_Approved (Timestamp ZZ_Date_SDL_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_SDL_Approved, ZZ_Date_SDL_Approved);
	}

	/** Get Date Approved By SDL Finance Manager.
		@return Date Approved By SDL Finance Manager	  */
	public Timestamp getZZ_Date_SDL_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_SDL_Approved);
	}

	/** Set Date Not Approved By the SDL Finance Mgr.
		@param ZZ_Date_SDL_Not_Approved Date Not Approved By the SDL Finance Mgr
	*/
	public void setZZ_Date_SDL_Not_Approved (Timestamp ZZ_Date_SDL_Not_Approved)
	{
		set_Value (COLUMNNAME_ZZ_Date_SDL_Not_Approved, ZZ_Date_SDL_Not_Approved);
	}

	/** Get Date Not Approved By the SDL Finance Mgr.
		@return Date Not Approved By the SDL Finance Mgr	  */
	public Timestamp getZZ_Date_SDL_Not_Approved()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_SDL_Not_Approved);
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
	/** Recommended By Officer - QA Accreditation = R5 */
	public static final String ZZ_DOCSTATUS_RecommendedByOfficer_QAAccreditation = "R5";
	/** Recommended By Mgr - QA Accreditation = R6 */
	public static final String ZZ_DOCSTATUS_RecommendedByMgr_QAAccreditation = "R6";
	/** Recommended By Snr Mgr QA = R7 */
	public static final String ZZ_DOCSTATUS_RecommendedBySnrMgrQA = "R7";
	/** Recommended for Approval = RA */
	public static final String ZZ_DOCSTATUS_RecommendedForApproval = "RA";
	/** Recommended = RC */
	public static final String ZZ_DOCSTATUS_Recommended = "RC";
	/** Recommended By Senior Mgr SDR = RD */
	public static final String ZZ_DOCSTATUS_RecommendedBySeniorMgrSDR = "RD";
	/** Recommended for Evaluation = RE */
	public static final String ZZ_DOCSTATUS_RecommendedForEvaluation = "RE";
	/** Submitted to Snr Admin Finance = SA */
	public static final String ZZ_DOCSTATUS_SubmittedToSnrAdminFinance = "SA";
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

	/** Approved By Manager Finance Consumables = AC */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_ApprovedByManagerFinanceConsumables = "AC";
	/** Approved = AP */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Approved = "AP";
	/** Prepared for CEO = CF */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_PreparedForCEO = "CF";
	/** Completed = CO */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Completed = "CO";
	/** Draft = DR */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Draft = "DR";
	/** Error Importing = EE */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_ErrorImporting = "EE";
	/** Validation Error = ER */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_ValidationError = "ER";
	/** Evaluated = EV */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Evaluated = "EV";
	/** Importing = IG */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Importing = "IG";
	/** Imported = IM */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Imported = "IM";
	/** In Progress = IP */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_InProgress = "IP";
	/** Not Recommended By Senior Mgr SDR = N1 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommendedBySeniorMgrSDR = "N1";
	/** Not Recommended By Senior Mgr Finance = N2 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommendedBySeniorMgrFinance = "N2";
	/** Not Recommended By COO = N3 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommendedByCOO = "N3";
	/** Not Recommended By CFO = N4 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommendedByCFO = "N4";
	/** Not Recommended By CEO = N5 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommendedByCEO = "N5";
	/** Not Approved by Snr Manager = NA */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedBySnrManager = "NA";
	/** Not Approved By Manager Finance Consumables = NC */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedByManagerFinanceConsumables = "NC";
	/** Not Approved By SDL Finance Mgr = ND */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedBySDLFinanceMgr = "ND";
	/** Not Approved By IT Manager = NI */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedByITManager = "NI";
	/** Not Approved by LM = NL */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedByLM = "NL";
	/** Not Approved = NP */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApproved = "NP";
	/** Not Recommended = NR */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotRecommended = "NR";
	/** Not Approved by Snr Admin Finance = NS */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotApprovedBySnrAdminFinance = "NS";
	/** Not Verified = NV */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_NotVerified = "NV";
	/** Pending = PE */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Pending = "PE";
	/** Query = QR */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Query = "QR";
	/** Recommended By Senior Mgr Finance = R1 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedBySeniorMgrFinance = "R1";
	/** Recommended By COO = R2 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedByCOO = "R2";
	/** Recommended By CFO = R3 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedByCFO = "R3";
	/** Recommended By CEO = R4 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedByCEO = "R4";
	/** Recommended By Officer - QA Accreditation = R5 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedByOfficer_QAAccreditation = "R5";
	/** Recommended By Mgr - QA Accreditation = R6 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedByMgr_QAAccreditation = "R6";
	/** Recommended By Snr Mgr QA = R7 */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedBySnrMgrQA = "R7";
	/** Recommended for Approval = RA */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedForApproval = "RA";
	/** Recommended = RC */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Recommended = "RC";
	/** Recommended By Senior Mgr SDR = RD */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedBySeniorMgrSDR = "RD";
	/** Recommended for Evaluation = RE */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_RecommendedForEvaluation = "RE";
	/** Submitted to Snr Admin Finance = SA */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_SubmittedToSnrAdminFinance = "SA";
	/** Submitted to Manager Finance Consumables = SC */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_SubmittedToManagerFinanceConsumables = "SC";
	/** Submitted To SDL Finance Mgr = SD */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_SubmittedToSDLFinanceMgr = "SD";
	/** Submitted To IT Manager = SI */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_SubmittedToITManager = "SI";
	/** Submitted To IT Admin = ST */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_SubmittedToITAdmin = "ST";
	/** Submitted = SU */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Submitted = "SU";
	/** Transfer Out = TO */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_TransferOut = "TO";
	/** Updated by SDR Admin = UA */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_UpdatedBySDRAdmin = "UA";
	/** Uploaded = UP */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Uploaded = "UP";
	/** Delinked = UnSdfOrg */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Delinked = "UnSdfOrg";
	/** Validating = VA */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Validating = "VA";
	/** Verified = VE */
	public static final String ZZ_FINALWORKFLOWSTATEVALUE_Verified = "VE";
	/** Set Final Workflow State Value.
		@param ZZ_FinalWorkflowStateValue Value set to ZZ_DocStatus when reach to end of approve workflow
	*/
	public void setZZ_FinalWorkflowStateValue (String ZZ_FinalWorkflowStateValue)
	{

		set_Value (COLUMNNAME_ZZ_FinalWorkflowStateValue, ZZ_FinalWorkflowStateValue);
	}

	/** Get Final Workflow State Value.
		@return Value set to ZZ_DocStatus when reach to end of approve workflow
	  */
	public String getZZ_FinalWorkflowStateValue()
	{
		return (String)get_Value(COLUMNNAME_ZZ_FinalWorkflowStateValue);
	}

	/** Set Internal Request Report.
		@param ZZ_Internal_Request_Rpt Internal Request Report
	*/
	public void setZZ_Internal_Request_Rpt (String ZZ_Internal_Request_Rpt)
	{
		set_Value (COLUMNNAME_ZZ_Internal_Request_Rpt, ZZ_Internal_Request_Rpt);
	}

	/** Get Internal Request Report.
		@return Internal Request Report	  */
	public String getZZ_Internal_Request_Rpt()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Internal_Request_Rpt);
	}

	/** Set Is Refreshment Request.
		@param ZZ_Is_Refreshment_Req Is Refreshment Request
	*/
	public void setZZ_Is_Refreshment_Req (boolean ZZ_Is_Refreshment_Req)
	{
		set_Value (COLUMNNAME_ZZ_Is_Refreshment_Req, Boolean.valueOf(ZZ_Is_Refreshment_Req));
	}

	/** Get Is Refreshment Request.
		@return Is Refreshment Request	  */
	public boolean isZZ_Is_Refreshment_Req()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Is_Refreshment_Req);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_AD_User getZZ_Mgr_Fin_Consumables() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Mgr_Fin_Consumables_ID(), get_TrxName());
	}

	/** Set Manager Finance Consumables.
		@param ZZ_Mgr_Fin_Consumables_ID Manager Finance Consumables
	*/
	public void setZZ_Mgr_Fin_Consumables_ID (int ZZ_Mgr_Fin_Consumables_ID)
	{
		if (ZZ_Mgr_Fin_Consumables_ID < 1)
			set_Value (COLUMNNAME_ZZ_Mgr_Fin_Consumables_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Mgr_Fin_Consumables_ID, Integer.valueOf(ZZ_Mgr_Fin_Consumables_ID));
	}

	/** Get Manager Finance Consumables.
		@return Manager Finance Consumables	  */
	public int getZZ_Mgr_Fin_Consumables_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Mgr_Fin_Consumables_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_RequestedBy() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_RequestedBy_ID(), get_TrxName());
	}

	/** Set Requested By.
		@param ZZ_RequestedBy_ID Requested By
	*/
	public void setZZ_RequestedBy_ID (int ZZ_RequestedBy_ID)
	{
		if (ZZ_RequestedBy_ID < 1)
			set_Value (COLUMNNAME_ZZ_RequestedBy_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_RequestedBy_ID, Integer.valueOf(ZZ_RequestedBy_ID));
	}

	/** Get Requested By.
		@return Requested By	  */
	public int getZZ_RequestedBy_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_RequestedBy_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_Requester() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Requester_ID(), get_TrxName());
	}

	/** Set Requester.
		@param ZZ_Requester_ID Requester
	*/
	public void setZZ_Requester_ID (int ZZ_Requester_ID)
	{
		if (ZZ_Requester_ID < 1)
			set_Value (COLUMNNAME_ZZ_Requester_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Requester_ID, Integer.valueOf(ZZ_Requester_ID));
	}

	/** Get Requester.
		@return Requester	  */
	public int getZZ_Requester_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Requester_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_SDL_Fin_Mgr() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_SDL_Fin_Mgr_ID(), get_TrxName());
	}

	/** Set SDL Finance Mgr.
		@param ZZ_SDL_Fin_Mgr_ID SDL Finance Mgr
	*/
	public void setZZ_SDL_Fin_Mgr_ID (int ZZ_SDL_Fin_Mgr_ID)
	{
		if (ZZ_SDL_Fin_Mgr_ID < 1)
			set_Value (COLUMNNAME_ZZ_SDL_Fin_Mgr_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_SDL_Fin_Mgr_ID, Integer.valueOf(ZZ_SDL_Fin_Mgr_ID));
	}

	/** Get SDL Finance Mgr.
		@return SDL Finance Mgr	  */
	public int getZZ_SDL_Fin_Mgr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SDL_Fin_Mgr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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