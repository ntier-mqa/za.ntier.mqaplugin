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
package za.ntier.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for M_Inventory
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_M_Inventory 
{

    /** TableName=M_Inventory */
    public static final String Table_Name = "M_Inventory";

    /** AD_Table_ID=321 */
    public static final int Table_ID = 321;

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 1 - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(1);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_OrgTrx_ID */
    public static final String COLUMNNAME_AD_OrgTrx_ID = "AD_OrgTrx_ID";

	/** Set Trx Organization.
	  * Performing or initiating organization
	  */
	public void setAD_OrgTrx_ID (int AD_OrgTrx_ID);

	/** Get Trx Organization.
	  * Performing or initiating organization
	  */
	public int getAD_OrgTrx_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Unit.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Unit.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name ApprovalAmt */
    public static final String COLUMNNAME_ApprovalAmt = "ApprovalAmt";

	/** Set Approval Amount.
	  * Document Approval Amount
	  */
	public void setApprovalAmt (BigDecimal ApprovalAmt);

	/** Get Approval Amount.
	  * Document Approval Amount
	  */
	public BigDecimal getApprovalAmt();

    /** Column name C_Activity_ID */
    public static final String COLUMNNAME_C_Activity_ID = "C_Activity_ID";

	/** Set Activity.
	  * Business Activity
	  */
	public void setC_Activity_ID (int C_Activity_ID);

	/** Get Activity.
	  * Business Activity
	  */
	public int getC_Activity_ID();

	public org.compiere.model.I_C_Activity getC_Activity() throws RuntimeException;

    /** Column name C_Campaign_ID */
    public static final String COLUMNNAME_C_Campaign_ID = "C_Campaign_ID";

	/** Set Campaign.
	  * Marketing Campaign
	  */
	public void setC_Campaign_ID (int C_Campaign_ID);

	/** Get Campaign.
	  * Marketing Campaign
	  */
	public int getC_Campaign_ID();

	public org.compiere.model.I_C_Campaign getC_Campaign() throws RuntimeException;

    /** Column name C_ConversionType_ID */
    public static final String COLUMNNAME_C_ConversionType_ID = "C_ConversionType_ID";

	/** Set Currency Type.
	  * Currency Conversion Rate Type
	  */
	public void setC_ConversionType_ID (int C_ConversionType_ID);

	/** Get Currency Type.
	  * Currency Conversion Rate Type
	  */
	public int getC_ConversionType_ID();

	public org.compiere.model.I_C_ConversionType getC_ConversionType() throws RuntimeException;

    /** Column name C_Currency_ID */
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";

	/** Set Currency.
	  * The Currency for this record
	  */
	public void setC_Currency_ID (int C_Currency_ID);

	/** Get Currency.
	  * The Currency for this record
	  */
	public int getC_Currency_ID();

	public org.compiere.model.I_C_Currency getC_Currency() throws RuntimeException;

    /** Column name C_DocType_ID */
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";

	/** Set Document Type.
	  * Document type or rules
	  */
	public void setC_DocType_ID (int C_DocType_ID);

	/** Get Document Type.
	  * Document type or rules
	  */
	public int getC_DocType_ID();

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException;

    /** Column name C_Project_ID */
    public static final String COLUMNNAME_C_Project_ID = "C_Project_ID";

	/** Set Project.
	  * Financial Project
	  */
	public void setC_Project_ID (int C_Project_ID);

	/** Get Project.
	  * Financial Project
	  */
	public int getC_Project_ID();

	public org.compiere.model.I_C_Project getC_Project() throws RuntimeException;

    /** Column name CostingMethod */
    public static final String COLUMNNAME_CostingMethod = "CostingMethod";

	/** Set Costing Method.
	  * Indicates how Costs will be calculated
	  */
	public void setCostingMethod (String CostingMethod);

	/** Get Costing Method.
	  * Indicates how Costs will be calculated
	  */
	public String getCostingMethod();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name DocAction */
    public static final String COLUMNNAME_DocAction = "DocAction";

	/** Set Document Action.
	  * The targeted status of the document
	  */
	public void setDocAction (String DocAction);

	/** Get Document Action.
	  * The targeted status of the document
	  */
	public String getDocAction();

    /** Column name DocStatus */
    public static final String COLUMNNAME_DocStatus = "DocStatus";

	/** Set Document Status.
	  * The current status of the document
	  */
	public void setDocStatus (String DocStatus);

	/** Get Document Status.
	  * The current status of the document
	  */
	public String getDocStatus();

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name GenerateList */
    public static final String COLUMNNAME_GenerateList = "GenerateList";

	/** Set Generate List.
	  * Generate List
	  */
	public void setGenerateList (String GenerateList);

	/** Get Generate List.
	  * Generate List
	  */
	public String getGenerateList();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsApproved */
    public static final String COLUMNNAME_IsApproved = "IsApproved";

	/** Set Approved.
	  * Indicates if this document requires approval
	  */
	public void setIsApproved (boolean IsApproved);

	/** Get Approved.
	  * Indicates if this document requires approval
	  */
	public boolean isApproved();

    /** Column name Line_Manager_ID */
    public static final String COLUMNNAME_Line_Manager_ID = "Line_Manager_ID";

	/** Set Line Manager	  */
	public void setLine_Manager_ID (int Line_Manager_ID);

	/** Get Line Manager	  */
	public int getLine_Manager_ID();

	public org.compiere.model.I_AD_User getLine_Manager() throws RuntimeException;

    /** Column name M_Inventory_ID */
    public static final String COLUMNNAME_M_Inventory_ID = "M_Inventory_ID";

	/** Set Phys.Inventory.
	  * Parameters for a Physical Inventory
	  */
	public void setM_Inventory_ID (int M_Inventory_ID);

	/** Get Phys.Inventory.
	  * Parameters for a Physical Inventory
	  */
	public int getM_Inventory_ID();

    /** Column name M_Inventory_UU */
    public static final String COLUMNNAME_M_Inventory_UU = "M_Inventory_UU";

	/** Set M_Inventory_UU	  */
	public void setM_Inventory_UU (String M_Inventory_UU);

	/** Get M_Inventory_UU	  */
	public String getM_Inventory_UU();

    /** Column name M_PerpetualInv_ID */
    public static final String COLUMNNAME_M_PerpetualInv_ID = "M_PerpetualInv_ID";

	/** Set Perpetual Inventory.
	  * Rules for generating physical inventory
	  */
	public void setM_PerpetualInv_ID (int M_PerpetualInv_ID);

	/** Get Perpetual Inventory.
	  * Rules for generating physical inventory
	  */
	public int getM_PerpetualInv_ID();

	public org.compiere.model.I_M_PerpetualInv getM_PerpetualInv() throws RuntimeException;

    /** Column name M_Warehouse_ID */
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";

	/** Set Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID);

	/** Get Warehouse.
	  * Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID();

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException;

    /** Column name MovementDate */
    public static final String COLUMNNAME_MovementDate = "MovementDate";

	/** Set Movement Date.
	  * Date a product was moved in or out of inventory
	  */
	public void setMovementDate (Timestamp MovementDate);

	/** Get Movement Date.
	  * Date a product was moved in or out of inventory
	  */
	public Timestamp getMovementDate();

    /** Column name Posted */
    public static final String COLUMNNAME_Posted = "Posted";

	/** Set Posted.
	  * Posting status
	  */
	public void setPosted (boolean Posted);

	/** Get Posted.
	  * Posting status
	  */
	public boolean isPosted();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name ProcessedOn */
    public static final String COLUMNNAME_ProcessedOn = "ProcessedOn";

	/** Set Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public void setProcessedOn (BigDecimal ProcessedOn);

	/** Get Processed On.
	  * The date+time (expressed in decimal format) when the document has been processed
	  */
	public BigDecimal getProcessedOn();

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name Reversal_ID */
    public static final String COLUMNNAME_Reversal_ID = "Reversal_ID";

	/** Set Reversal ID.
	  * ID of document reversal
	  */
	public void setReversal_ID (int Reversal_ID);

	/** Get Reversal ID.
	  * ID of document reversal
	  */
	public int getReversal_ID();

	public org.compiere.model.I_M_Inventory getReversal() throws RuntimeException;

    /** Column name UpdateQty */
    public static final String COLUMNNAME_UpdateQty = "UpdateQty";

	/** Set Update Quantities	  */
	public void setUpdateQty (String UpdateQty);

	/** Get Update Quantities	  */
	public String getUpdateQty();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name User1_ID */
    public static final String COLUMNNAME_User1_ID = "User1_ID";

	/** Set User Element List 1.
	  * User defined list element #1
	  */
	public void setUser1_ID (int User1_ID);

	/** Get User Element List 1.
	  * User defined list element #1
	  */
	public int getUser1_ID();

	public org.compiere.model.I_C_ElementValue getUser1() throws RuntimeException;

    /** Column name User2_ID */
    public static final String COLUMNNAME_User2_ID = "User2_ID";

	/** Set User Element List 2.
	  * User defined list element #2
	  */
	public void setUser2_ID (int User2_ID);

	/** Get User Element List 2.
	  * User defined list element #2
	  */
	public int getUser2_ID();

	public org.compiere.model.I_C_ElementValue getUser2() throws RuntimeException;

    /** Column name ZZ_AllowLineManageApproved */
    public static final String COLUMNNAME_ZZ_AllowLineManageApproved = "ZZ_AllowLineManageApproved";

	/** Set Allow Line Manage Approved.
	  * Choose to allow line manage join to approved workfllow
	  */
	public void setZZ_AllowLineManageApproved (boolean ZZ_AllowLineManageApproved);

	/** Get Allow Line Manage Approved.
	  * Choose to allow line manage join to approved workfllow
	  */
	public boolean isZZ_AllowLineManageApproved();

    /** Column name ZZ_AllowMgrFinConsumablesApproval */
    public static final String COLUMNNAME_ZZ_AllowMgrFinConsumablesApproval = "ZZ_AllowMgrFinConsumablesApproval";

	/** Set Allow Mgr Finance Consumables Approval	  */
	public void setZZ_AllowMgrFinConsumablesApproval (boolean ZZ_AllowMgrFinConsumablesApproval);

	/** Get Allow Mgr Finance Consumables Approval	  */
	public boolean isZZ_AllowMgrFinConsumablesApproval();

    /** Column name ZZ_AllowSdlLineMgrApproved */
    public static final String COLUMNNAME_ZZ_AllowSdlLineMgrApproved = "ZZ_AllowSdlLineMgrApproved";

	/** Set Allow SDL Finance Manager Approval	  */
	public void setZZ_AllowSdlLineMgrApproved (boolean ZZ_AllowSdlLineMgrApproved);

	/** Get Allow SDL Finance Manager Approval	  */
	public boolean isZZ_AllowSdlLineMgrApproved();

    /** Column name ZZ_AllowSnrAdminFinanceApproved */
    public static final String COLUMNNAME_ZZ_AllowSnrAdminFinanceApproved = "ZZ_AllowSnrAdminFinanceApproved";

	/** Set Allow Snr Admin Finance Approved.
	  * Choose to allow Snr Admin Finance join to approved workfllow
	  */
	public void setZZ_AllowSnrAdminFinanceApproved (boolean ZZ_AllowSnrAdminFinanceApproved);

	/** Get Allow Snr Admin Finance Approved.
	  * Choose to allow Snr Admin Finance join to approved workfllow
	  */
	public boolean isZZ_AllowSnrAdminFinanceApproved();

    /** Column name ZZ_Consumables_Signed_Uploaded */
    public static final String COLUMNNAME_ZZ_Consumables_Signed_Uploaded = "ZZ_Consumables_Signed_Uploaded";

	/** Set Consumables Document Signed and Uploaded	  */
	public void setZZ_Consumables_Signed_Uploaded (boolean ZZ_Consumables_Signed_Uploaded);

	/** Get Consumables Document Signed and Uploaded	  */
	public boolean isZZ_Consumables_Signed_Uploaded();

    /** Column name ZZ_Date_Approved */
    public static final String COLUMNNAME_ZZ_Date_Approved = "ZZ_Date_Approved";

	/** Set Date Approved	  */
	public void setZZ_Date_Approved (Timestamp ZZ_Date_Approved);

	/** Get Date Approved	  */
	public Timestamp getZZ_Date_Approved();

    /** Column name ZZ_Date_Completed */
    public static final String COLUMNNAME_ZZ_Date_Completed = "ZZ_Date_Completed";

	/** Set Date Completed	  */
	public void setZZ_Date_Completed (Timestamp ZZ_Date_Completed);

	/** Get Date Completed	  */
	public Timestamp getZZ_Date_Completed();

    /** Column name ZZ_Date_LM_Approved */
    public static final String COLUMNNAME_ZZ_Date_LM_Approved = "ZZ_Date_LM_Approved";

	/** Set Date LM Approved	  */
	public void setZZ_Date_LM_Approved (Timestamp ZZ_Date_LM_Approved);

	/** Get Date LM Approved	  */
	public Timestamp getZZ_Date_LM_Approved();

    /** Column name ZZ_Date_MFC_Approved */
    public static final String COLUMNNAME_ZZ_Date_MFC_Approved = "ZZ_Date_MFC_Approved";

	/** Set Date Manager Finance Consumables	  */
	public void setZZ_Date_MFC_Approved (Timestamp ZZ_Date_MFC_Approved);

	/** Get Date Manager Finance Consumables	  */
	public Timestamp getZZ_Date_MFC_Approved();

    /** Column name ZZ_Date_MFC_Not_Approved */
    public static final String COLUMNNAME_ZZ_Date_MFC_Not_Approved = "ZZ_Date_MFC_Not_Approved";

	/** Set Date Manager Finance Not Approved	  */
	public void setZZ_Date_MFC_Not_Approved (Timestamp ZZ_Date_MFC_Not_Approved);

	/** Get Date Manager Finance Not Approved	  */
	public Timestamp getZZ_Date_MFC_Not_Approved();

    /** Column name ZZ_Date_Not_Approved_by_LM */
    public static final String COLUMNNAME_ZZ_Date_Not_Approved_by_LM = "ZZ_Date_Not_Approved_by_LM";

	/** Set Date Not Approved by LM	  */
	public void setZZ_Date_Not_Approved_by_LM (Timestamp ZZ_Date_Not_Approved_by_LM);

	/** Get Date Not Approved by LM	  */
	public Timestamp getZZ_Date_Not_Approved_by_LM();

    /** Column name ZZ_Date_Not_Approved_by_Snr_Adm_Fin */
    public static final String COLUMNNAME_ZZ_Date_Not_Approved_by_Snr_Adm_Fin = "ZZ_Date_Not_Approved_by_Snr_Adm_Fin";

	/** Set Date Not Approved by Snr Admin Finance	  */
	public void setZZ_Date_Not_Approved_by_Snr_Adm_Fin (Timestamp ZZ_Date_Not_Approved_by_Snr_Adm_Fin);

	/** Get Date Not Approved by Snr Admin Finance	  */
	public Timestamp getZZ_Date_Not_Approved_by_Snr_Adm_Fin();

    /** Column name ZZ_Date_SDL_Approved */
    public static final String COLUMNNAME_ZZ_Date_SDL_Approved = "ZZ_Date_SDL_Approved";

	/** Set Date Approved By SDL Finance Manager	  */
	public void setZZ_Date_SDL_Approved (Timestamp ZZ_Date_SDL_Approved);

	/** Get Date Approved By SDL Finance Manager	  */
	public Timestamp getZZ_Date_SDL_Approved();

    /** Column name ZZ_Date_SDL_Not_Approved */
    public static final String COLUMNNAME_ZZ_Date_SDL_Not_Approved = "ZZ_Date_SDL_Not_Approved";

	/** Set Date Not Approved By the SDL Finance Mgr	  */
	public void setZZ_Date_SDL_Not_Approved (Timestamp ZZ_Date_SDL_Not_Approved);

	/** Get Date Not Approved By the SDL Finance Mgr	  */
	public Timestamp getZZ_Date_SDL_Not_Approved();

    /** Column name ZZ_Date_Submitted */
    public static final String COLUMNNAME_ZZ_Date_Submitted = "ZZ_Date_Submitted";

	/** Set Date Submitted	  */
	public void setZZ_Date_Submitted (Timestamp ZZ_Date_Submitted);

	/** Get Date Submitted	  */
	public Timestamp getZZ_Date_Submitted();

    /** Column name ZZ_DocAction */
    public static final String COLUMNNAME_ZZ_DocAction = "ZZ_DocAction";

	/** Set Document Action	  */
	public void setZZ_DocAction (String ZZ_DocAction);

	/** Get Document Action	  */
	public String getZZ_DocAction();

    /** Column name ZZ_DocStatus */
    public static final String COLUMNNAME_ZZ_DocStatus = "ZZ_DocStatus";

	/** Set Document Status	  */
	public void setZZ_DocStatus (String ZZ_DocStatus);

	/** Get Document Status	  */
	public String getZZ_DocStatus();

    /** Column name ZZ_FinalWorkflowStateValue */
    public static final String COLUMNNAME_ZZ_FinalWorkflowStateValue = "ZZ_FinalWorkflowStateValue";

	/** Set Final Workflow State Value.
	  * Value set to ZZ_DocStatus when reach to end of approve workflow
	  */
	public void setZZ_FinalWorkflowStateValue (String ZZ_FinalWorkflowStateValue);

	/** Get Final Workflow State Value.
	  * Value set to ZZ_DocStatus when reach to end of approve workflow
	  */
	public String getZZ_FinalWorkflowStateValue();

    /** Column name ZZ_Internal_Request_Rpt */
    public static final String COLUMNNAME_ZZ_Internal_Request_Rpt = "ZZ_Internal_Request_Rpt";

	/** Set Internal Request Report	  */
	public void setZZ_Internal_Request_Rpt (String ZZ_Internal_Request_Rpt);

	/** Get Internal Request Report	  */
	public String getZZ_Internal_Request_Rpt();

    /** Column name ZZ_Is_Refreshment_Req */
    public static final String COLUMNNAME_ZZ_Is_Refreshment_Req = "ZZ_Is_Refreshment_Req";

	/** Set Is Refreshment Request	  */
	public void setZZ_Is_Refreshment_Req (boolean ZZ_Is_Refreshment_Req);

	/** Get Is Refreshment Request	  */
	public boolean isZZ_Is_Refreshment_Req();

    /** Column name ZZ_Mgr_Fin_Consumables_ID */
    public static final String COLUMNNAME_ZZ_Mgr_Fin_Consumables_ID = "ZZ_Mgr_Fin_Consumables_ID";

	/** Set Manager Finance Consumables	  */
	public void setZZ_Mgr_Fin_Consumables_ID (int ZZ_Mgr_Fin_Consumables_ID);

	/** Get Manager Finance Consumables	  */
	public int getZZ_Mgr_Fin_Consumables_ID();

	public org.compiere.model.I_AD_User getZZ_Mgr_Fin_Consumables() throws RuntimeException;

    /** Column name ZZ_RequestedBy_ID */
    public static final String COLUMNNAME_ZZ_RequestedBy_ID = "ZZ_RequestedBy_ID";

	/** Set Requested By	  */
	public void setZZ_RequestedBy_ID (int ZZ_RequestedBy_ID);

	/** Get Requested By	  */
	public int getZZ_RequestedBy_ID();

	public org.compiere.model.I_AD_User getZZ_RequestedBy() throws RuntimeException;

    /** Column name ZZ_Requester_ID */
    public static final String COLUMNNAME_ZZ_Requester_ID = "ZZ_Requester_ID";

	/** Set Requester	  */
	public void setZZ_Requester_ID (int ZZ_Requester_ID);

	/** Get Requester	  */
	public int getZZ_Requester_ID();

	public org.compiere.model.I_AD_User getZZ_Requester() throws RuntimeException;

    /** Column name ZZ_SDL_Fin_Mgr_ID */
    public static final String COLUMNNAME_ZZ_SDL_Fin_Mgr_ID = "ZZ_SDL_Fin_Mgr_ID";

	/** Set SDL Finance Mgr	  */
	public void setZZ_SDL_Fin_Mgr_ID (int ZZ_SDL_Fin_Mgr_ID);

	/** Get SDL Finance Mgr	  */
	public int getZZ_SDL_Fin_Mgr_ID();

	public org.compiere.model.I_AD_User getZZ_SDL_Fin_Mgr() throws RuntimeException;

    /** Column name ZZ_Snr_Admin_Fin_ID */
    public static final String COLUMNNAME_ZZ_Snr_Admin_Fin_ID = "ZZ_Snr_Admin_Fin_ID";

	/** Set Snr Admin Finance User	  */
	public void setZZ_Snr_Admin_Fin_ID (int ZZ_Snr_Admin_Fin_ID);

	/** Get Snr Admin Finance User	  */
	public int getZZ_Snr_Admin_Fin_ID();

	public org.compiere.model.I_AD_User getZZ_Snr_Admin_Fin() throws RuntimeException;
}
