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
package za.co.ntier.wsp_atr.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for ZZ_Org_Transfer
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_Org_Transfer 
{

    /** TableName=ZZ_Org_Transfer */
    public static final String Table_Name = "ZZ_Org_Transfer";

    /** AD_Table_ID=1000213 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

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

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner.
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner.
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException;

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

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

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

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();

    /** Column name ZZ_CEOApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_CEOApprovedBy_ID = "ZZ_CEOApprovedBy_ID";

	/** Set CEO Approved By	  */
	public void setZZ_CEOApprovedBy_ID (int ZZ_CEOApprovedBy_ID);

	/** Get CEO Approved By	  */
	public int getZZ_CEOApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_CEOApprovedBy() throws RuntimeException;

    /** Column name ZZ_CEOApprovedDate */
    public static final String COLUMNNAME_ZZ_CEOApprovedDate = "ZZ_CEOApprovedDate";

	/** Set CEO Approved Date	  */
	public void setZZ_CEOApprovedDate (Timestamp ZZ_CEOApprovedDate);

	/** Get CEO Approved Date	  */
	public Timestamp getZZ_CEOApprovedDate();

    /** Column name ZZ_CEORejectedBy_ID */
    public static final String COLUMNNAME_ZZ_CEORejectedBy_ID = "ZZ_CEORejectedBy_ID";

	/** Set CEO Rejected By	  */
	public void setZZ_CEORejectedBy_ID (int ZZ_CEORejectedBy_ID);

	/** Get CEO Rejected By	  */
	public int getZZ_CEORejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_CEORejectedBy() throws RuntimeException;

    /** Column name ZZ_CEORejectedDate */
    public static final String COLUMNNAME_ZZ_CEORejectedDate = "ZZ_CEORejectedDate";

	/** Set CEO Rejected Date	  */
	public void setZZ_CEORejectedDate (Timestamp ZZ_CEORejectedDate);

	/** Get CEO Rejected Date	  */
	public Timestamp getZZ_CEORejectedDate();

    /** Column name ZZ_CFOApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_CFOApprovedBy_ID = "ZZ_CFOApprovedBy_ID";

	/** Set CFO Approved By	  */
	public void setZZ_CFOApprovedBy_ID (int ZZ_CFOApprovedBy_ID);

	/** Get CFO Approved By	  */
	public int getZZ_CFOApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_CFOApprovedBy() throws RuntimeException;

    /** Column name ZZ_CFOApprovedDate */
    public static final String COLUMNNAME_ZZ_CFOApprovedDate = "ZZ_CFOApprovedDate";

	/** Set CFO Approved Date	  */
	public void setZZ_CFOApprovedDate (Timestamp ZZ_CFOApprovedDate);

	/** Get CFO Approved Date	  */
	public Timestamp getZZ_CFOApprovedDate();

    /** Column name ZZ_CFORejectedBy_ID */
    public static final String COLUMNNAME_ZZ_CFORejectedBy_ID = "ZZ_CFORejectedBy_ID";

	/** Set CFO Rejected By	  */
	public void setZZ_CFORejectedBy_ID (int ZZ_CFORejectedBy_ID);

	/** Get CFO Rejected By	  */
	public int getZZ_CFORejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_CFORejectedBy() throws RuntimeException;

    /** Column name ZZ_CFORejectedDate */
    public static final String COLUMNNAME_ZZ_CFORejectedDate = "ZZ_CFORejectedDate";

	/** Set CFO Rejected Date	  */
	public void setZZ_CFORejectedDate (Timestamp ZZ_CFORejectedDate);

	/** Get CFO Rejected Date	  */
	public Timestamp getZZ_CFORejectedDate();

    /** Column name ZZ_COOApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_COOApprovedBy_ID = "ZZ_COOApprovedBy_ID";

	/** Set COO Approved By	  */
	public void setZZ_COOApprovedBy_ID (int ZZ_COOApprovedBy_ID);

	/** Get COO Approved By	  */
	public int getZZ_COOApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_COOApprovedBy() throws RuntimeException;

    /** Column name ZZ_COOApprovedDate */
    public static final String COLUMNNAME_ZZ_COOApprovedDate = "ZZ_COOApprovedDate";

	/** Set COO Approved Date	  */
	public void setZZ_COOApprovedDate (Timestamp ZZ_COOApprovedDate);

	/** Get COO Approved Date	  */
	public Timestamp getZZ_COOApprovedDate();

    /** Column name ZZ_COORejectedBy_ID */
    public static final String COLUMNNAME_ZZ_COORejectedBy_ID = "ZZ_COORejectedBy_ID";

	/** Set COO Rejected By	  */
	public void setZZ_COORejectedBy_ID (int ZZ_COORejectedBy_ID);

	/** Get COO Rejected By	  */
	public int getZZ_COORejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_COORejectedBy() throws RuntimeException;

    /** Column name ZZ_COORejectedDate */
    public static final String COLUMNNAME_ZZ_COORejectedDate = "ZZ_COORejectedDate";

	/** Set COO Rejected Date	  */
	public void setZZ_COORejectedDate (Timestamp ZZ_COORejectedDate);

	/** Get COO Rejected Date	  */
	public Timestamp getZZ_COORejectedDate();

    /** Column name ZZ_DateRequested */
    public static final String COLUMNNAME_ZZ_DateRequested = "ZZ_DateRequested";

	/** Set Date Requested	  */
	public void setZZ_DateRequested (Timestamp ZZ_DateRequested);

	/** Get Date Requested	  */
	public Timestamp getZZ_DateRequested();

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

    /** Column name ZZ_FromSETA */
    public static final String COLUMNNAME_ZZ_FromSETA = "ZZ_FromSETA";

	/** Set From SETA	  */
	public void setZZ_FromSETA (String ZZ_FromSETA);

	/** Get From SETA	  */
	public String getZZ_FromSETA();

    /** Column name ZZ_IsApproved */
    public static final String COLUMNNAME_ZZ_IsApproved = "ZZ_IsApproved";

	/** Set Is Approved	  */
	public void setZZ_IsApproved (boolean ZZ_IsApproved);

	/** Get Is Approved	  */
	public boolean isZZ_IsApproved();

    /** Column name ZZ_Org_Transfer_ID */
    public static final String COLUMNNAME_ZZ_Org_Transfer_ID = "ZZ_Org_Transfer_ID";

	/** Set Org Transfer	  */
	public void setZZ_Org_Transfer_ID (int ZZ_Org_Transfer_ID);

	/** Get Org Transfer	  */
	public int getZZ_Org_Transfer_ID();

    /** Column name ZZ_Org_Transfer_UU */
    public static final String COLUMNNAME_ZZ_Org_Transfer_UU = "ZZ_Org_Transfer_UU";

	/** Set ZZ_Org_Transfer_UU	  */
	public void setZZ_Org_Transfer_UU (String ZZ_Org_Transfer_UU);

	/** Get ZZ_Org_Transfer_UU	  */
	public String getZZ_Org_Transfer_UU();

    /** Column name ZZ_SnrMgrFinApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_SnrMgrFinApprovedBy_ID = "ZZ_SnrMgrFinApprovedBy_ID";

	/** Set Snr Mgr Fin Approved By	  */
	public void setZZ_SnrMgrFinApprovedBy_ID (int ZZ_SnrMgrFinApprovedBy_ID);

	/** Get Snr Mgr Fin Approved By	  */
	public int getZZ_SnrMgrFinApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SnrMgrFinApprovedBy() throws RuntimeException;

    /** Column name ZZ_SnrMgrFinApprovedDate */
    public static final String COLUMNNAME_ZZ_SnrMgrFinApprovedDate = "ZZ_SnrMgrFinApprovedDate";

	/** Set Snr Mgr Fin Approved Date	  */
	public void setZZ_SnrMgrFinApprovedDate (Timestamp ZZ_SnrMgrFinApprovedDate);

	/** Get Snr Mgr Fin Approved Date	  */
	public Timestamp getZZ_SnrMgrFinApprovedDate();

    /** Column name ZZ_SnrMgrFinRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_SnrMgrFinRejectedBy_ID = "ZZ_SnrMgrFinRejectedBy_ID";

	/** Set Snr Mgr Fin Rejected By	  */
	public void setZZ_SnrMgrFinRejectedBy_ID (int ZZ_SnrMgrFinRejectedBy_ID);

	/** Get Snr Mgr Fin Rejected By	  */
	public int getZZ_SnrMgrFinRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SnrMgrFinRejectedBy() throws RuntimeException;

    /** Column name ZZ_SnrMgrFinRejectedDate */
    public static final String COLUMNNAME_ZZ_SnrMgrFinRejectedDate = "ZZ_SnrMgrFinRejectedDate";

	/** Set Snr Mgr Fin Rejected Date	  */
	public void setZZ_SnrMgrFinRejectedDate (Timestamp ZZ_SnrMgrFinRejectedDate);

	/** Get Snr Mgr Fin Rejected Date	  */
	public Timestamp getZZ_SnrMgrFinRejectedDate();

    /** Column name ZZ_SnrMgrSDRApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_SnrMgrSDRApprovedBy_ID = "ZZ_SnrMgrSDRApprovedBy_ID";

	/** Set Snr Mgr SDR Approved By	  */
	public void setZZ_SnrMgrSDRApprovedBy_ID (int ZZ_SnrMgrSDRApprovedBy_ID);

	/** Get Snr Mgr SDR Approved By	  */
	public int getZZ_SnrMgrSDRApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SnrMgrSDRApprovedBy() throws RuntimeException;

    /** Column name ZZ_SnrMgrSDRApprovedDate */
    public static final String COLUMNNAME_ZZ_SnrMgrSDRApprovedDate = "ZZ_SnrMgrSDRApprovedDate";

	/** Set Snr Mgr SDR Approved Date	  */
	public void setZZ_SnrMgrSDRApprovedDate (Timestamp ZZ_SnrMgrSDRApprovedDate);

	/** Get Snr Mgr SDR Approved Date	  */
	public Timestamp getZZ_SnrMgrSDRApprovedDate();

    /** Column name ZZ_SnrMgrSDRRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_SnrMgrSDRRejectedBy_ID = "ZZ_SnrMgrSDRRejectedBy_ID";

	/** Set Snr Mgr SDR Rejected By	  */
	public void setZZ_SnrMgrSDRRejectedBy_ID (int ZZ_SnrMgrSDRRejectedBy_ID);

	/** Get Snr Mgr SDR Rejected By	  */
	public int getZZ_SnrMgrSDRRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SnrMgrSDRRejectedBy() throws RuntimeException;

    /** Column name ZZ_SnrMgrSDRRejectedDate */
    public static final String COLUMNNAME_ZZ_SnrMgrSDRRejectedDate = "ZZ_SnrMgrSDRRejectedDate";

	/** Set Snr Mgr SDR Rejected Date	  */
	public void setZZ_SnrMgrSDRRejectedDate (Timestamp ZZ_SnrMgrSDRRejectedDate);

	/** Get Snr Mgr SDR Rejected Date	  */
	public Timestamp getZZ_SnrMgrSDRRejectedDate();

    /** Column name ZZ_SubmitRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_SubmitRejectedBy_ID = "ZZ_SubmitRejectedBy_ID";

	/** Set Submit Rejected By	  */
	public void setZZ_SubmitRejectedBy_ID (int ZZ_SubmitRejectedBy_ID);

	/** Get Submit Rejected By	  */
	public int getZZ_SubmitRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SubmitRejectedBy() throws RuntimeException;

    /** Column name ZZ_SubmitRejectedDate */
    public static final String COLUMNNAME_ZZ_SubmitRejectedDate = "ZZ_SubmitRejectedDate";

	/** Set Submit Rejected Date	  */
	public void setZZ_SubmitRejectedDate (Timestamp ZZ_SubmitRejectedDate);

	/** Get Submit Rejected Date	  */
	public Timestamp getZZ_SubmitRejectedDate();

    /** Column name ZZ_SubmittedBy_ID */
    public static final String COLUMNNAME_ZZ_SubmittedBy_ID = "ZZ_SubmittedBy_ID";

	/** Set Submitted By	  */
	public void setZZ_SubmittedBy_ID (int ZZ_SubmittedBy_ID);

	/** Get Submitted By	  */
	public int getZZ_SubmittedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SubmittedBy() throws RuntimeException;

    /** Column name ZZ_SubmittedDate */
    public static final String COLUMNNAME_ZZ_SubmittedDate = "ZZ_SubmittedDate";

	/** Set Submitted Date	  */
	public void setZZ_SubmittedDate (Timestamp ZZ_SubmittedDate);

	/** Get Submitted Date	  */
	public Timestamp getZZ_SubmittedDate();

    /** Column name ZZ_ToSETA */
    public static final String COLUMNNAME_ZZ_ToSETA = "ZZ_ToSETA";

	/** Set To SETA	  */
	public void setZZ_ToSETA (String ZZ_ToSETA);

	/** Get To SETA	  */
	public String getZZ_ToSETA();

    /** Column name ZZ_TransferType */
    public static final String COLUMNNAME_ZZ_TransferType = "ZZ_TransferType";

	/** Set Transfer Type	  */
	public void setZZ_TransferType (String ZZ_TransferType);

	/** Get Transfer Type	  */
	public String getZZ_TransferType();
}
