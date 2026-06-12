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

/** Generated Interface for C_InvoiceBatch
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_C_InvoiceBatch 
{

    /** TableName=C_InvoiceBatch */
    public static final String Table_Name = "C_InvoiceBatch";

    /** AD_Table_ID=767 */
    public static final int Table_ID = 767;

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

    /** Column name C_InvoiceBatch_ID */
    public static final String COLUMNNAME_C_InvoiceBatch_ID = "C_InvoiceBatch_ID";

	/** Set Invoice Batch.
	  * Expense Invoice Batch Header
	  */
	public void setC_InvoiceBatch_ID (int C_InvoiceBatch_ID);

	/** Get Invoice Batch.
	  * Expense Invoice Batch Header
	  */
	public int getC_InvoiceBatch_ID();

    /** Column name C_InvoiceBatch_UU */
    public static final String COLUMNNAME_C_InvoiceBatch_UU = "C_InvoiceBatch_UU";

	/** Set C_InvoiceBatch_UU	  */
	public void setC_InvoiceBatch_UU (String C_InvoiceBatch_UU);

	/** Get C_InvoiceBatch_UU	  */
	public String getC_InvoiceBatch_UU();

    /** Column name ControlAmt */
    public static final String COLUMNNAME_ControlAmt = "ControlAmt";

	/** Set Control Amount.
	  * If not zero, the Debit amount of the document must be equal this amount
	  */
	public void setControlAmt (BigDecimal ControlAmt);

	/** Get Control Amount.
	  * If not zero, the Debit amount of the document must be equal this amount
	  */
	public BigDecimal getControlAmt();

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

    /** Column name DateDoc */
    public static final String COLUMNNAME_DateDoc = "DateDoc";

	/** Set Document Date.
	  * Date of the Document
	  */
	public void setDateDoc (Timestamp DateDoc);

	/** Get Document Date.
	  * Date of the Document
	  */
	public Timestamp getDateDoc();

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

    /** Column name DocumentAmt */
    public static final String COLUMNNAME_DocumentAmt = "DocumentAmt";

	/** Set Document Amt.
	  * Document Amount
	  */
	public void setDocumentAmt (BigDecimal DocumentAmt);

	/** Get Document Amt.
	  * Document Amount
	  */
	public BigDecimal getDocumentAmt();

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

    /** Column name IsSOTrx */
    public static final String COLUMNNAME_IsSOTrx = "IsSOTrx";

	/** Set Sales Transaction.
	  * This is a Sales Transaction
	  */
	public void setIsSOTrx (boolean IsSOTrx);

	/** Get Sales Transaction.
	  * This is a Sales Transaction
	  */
	public boolean isSOTrx();

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

    /** Column name Processing */
    public static final String COLUMNNAME_Processing = "Processing";

	/** Set Process Now	  */
	public void setProcessing (boolean Processing);

	/** Get Process Now	  */
	public boolean isProcessing();

    /** Column name SalesRep_ID */
    public static final String COLUMNNAME_SalesRep_ID = "SalesRep_ID";

	/** Set Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public void setSalesRep_ID (int SalesRep_ID);

	/** Get Sales Representative.
	  * Sales Representative or Company Agent
	  */
	public int getSalesRep_ID();

	public org.compiere.model.I_AD_User getSalesRep() throws RuntimeException;

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

    /** Column name ZZ_Account_Reconned */
    public static final String COLUMNNAME_ZZ_Account_Reconned = "ZZ_Account_Reconned";

	/** Set Account reconciled / O/S invoices verified	  */
	public void setZZ_Account_Reconned (boolean ZZ_Account_Reconned);

	/** Get Account reconciled / O/S invoices verified	  */
	public boolean isZZ_Account_Reconned();

    /** Column name ZZ_Auth_PO_Order */
    public static final String COLUMNNAME_ZZ_Auth_PO_Order = "ZZ_Auth_PO_Order";

	/** Set Authorized Purchase Order/SLA Attached	  */
	public void setZZ_Auth_PO_Order (boolean ZZ_Auth_PO_Order);

	/** Get Authorized Purchase Order/SLA Attached	  */
	public boolean isZZ_Auth_PO_Order();

    /** Column name ZZ_CEO_ID */
    public static final String COLUMNNAME_ZZ_CEO_ID = "ZZ_CEO_ID";

	/** Set CEO	  */
	public void setZZ_CEO_ID (int ZZ_CEO_ID);

	/** Get CEO	  */
	public int getZZ_CEO_ID();

	public org.compiere.model.I_AD_User getZZ_CEO() throws RuntimeException;

    /** Column name ZZ_CFO_ID */
    public static final String COLUMNNAME_ZZ_CFO_ID = "ZZ_CFO_ID";

	/** Set CFO	  */
	public void setZZ_CFO_ID (int ZZ_CFO_ID);

	/** Get CFO	  */
	public int getZZ_CFO_ID();

	public org.compiere.model.I_AD_User getZZ_CFO() throws RuntimeException;

    /** Column name ZZ_COO_ID */
    public static final String COLUMNNAME_ZZ_COO_ID = "ZZ_COO_ID";

	/** Set COO	  */
	public void setZZ_COO_ID (int ZZ_COO_ID);

	/** Get COO	  */
	public int getZZ_COO_ID();

	public org.compiere.model.I_AD_User getZZ_COO() throws RuntimeException;

    /** Column name ZZ_Calcs_Checked */
    public static final String COLUMNNAME_ZZ_Calcs_Checked = "ZZ_Calcs_Checked";

	/** Set Calculation Checked	  */
	public void setZZ_Calcs_Checked (boolean ZZ_Calcs_Checked);

	/** Get Calculation Checked	  */
	public boolean isZZ_Calcs_Checked();

    /** Column name ZZ_Cred_Bank_Dets_Verified */
    public static final String COLUMNNAME_ZZ_Cred_Bank_Dets_Verified = "ZZ_Cred_Bank_Dets_Verified";

	/** Set Creditor ID &amp;
 Banking Details Verified	  */
	public void setZZ_Cred_Bank_Dets_Verified (boolean ZZ_Cred_Bank_Dets_Verified);

	/** Get Creditor ID &amp;
 Banking Details Verified	  */
	public boolean isZZ_Cred_Bank_Dets_Verified();

    /** Column name ZZ_Date_Not_Recom_CEO */
    public static final String COLUMNNAME_ZZ_Date_Not_Recom_CEO = "ZZ_Date_Not_Recom_CEO";

	/** Set Date Not Approved By CEO	  */
	public void setZZ_Date_Not_Recom_CEO (Timestamp ZZ_Date_Not_Recom_CEO);

	/** Get Date Not Approved By CEO	  */
	public Timestamp getZZ_Date_Not_Recom_CEO();

    /** Column name ZZ_Date_Not_Recom_CFO */
    public static final String COLUMNNAME_ZZ_Date_Not_Recom_CFO = "ZZ_Date_Not_Recom_CFO";

	/** Set Date Not Recommended By CFO	  */
	public void setZZ_Date_Not_Recom_CFO (Timestamp ZZ_Date_Not_Recom_CFO);

	/** Get Date Not Recommended By CFO	  */
	public Timestamp getZZ_Date_Not_Recom_CFO();

    /** Column name ZZ_Date_Not_Recom_COO */
    public static final String COLUMNNAME_ZZ_Date_Not_Recom_COO = "ZZ_Date_Not_Recom_COO";

	/** Set Date Not Recommended By COO	  */
	public void setZZ_Date_Not_Recom_COO (Timestamp ZZ_Date_Not_Recom_COO);

	/** Get Date Not Recommended By COO	  */
	public Timestamp getZZ_Date_Not_Recom_COO();

    /** Column name ZZ_Date_Not_Recom_Snr_Mgr_SDR */
    public static final String COLUMNNAME_ZZ_Date_Not_Recom_Snr_Mgr_SDR = "ZZ_Date_Not_Recom_Snr_Mgr_SDR";

	/** Set Date Not Recommended By Snr Mgr SDR	  */
	public void setZZ_Date_Not_Recom_Snr_Mgr_SDR (Timestamp ZZ_Date_Not_Recom_Snr_Mgr_SDR);

	/** Get Date Not Recommended By Snr Mgr SDR	  */
	public Timestamp getZZ_Date_Not_Recom_Snr_Mgr_SDR();

    /** Column name ZZ_Date_Not_Recomm_Snr_Mgr_Fin */
    public static final String COLUMNNAME_ZZ_Date_Not_Recomm_Snr_Mgr_Fin = "ZZ_Date_Not_Recomm_Snr_Mgr_Fin";

	/** Set Date Not Recommended By Snr Mgr Finance	  */
	public void setZZ_Date_Not_Recomm_Snr_Mgr_Fin (Timestamp ZZ_Date_Not_Recomm_Snr_Mgr_Fin);

	/** Get Date Not Recommended By Snr Mgr Finance	  */
	public Timestamp getZZ_Date_Not_Recomm_Snr_Mgr_Fin();

    /** Column name ZZ_Date_Recom_Snr_Mgr_SDR */
    public static final String COLUMNNAME_ZZ_Date_Recom_Snr_Mgr_SDR = "ZZ_Date_Recom_Snr_Mgr_SDR";

	/** Set Date Recommended By Snr Mgr SDR	  */
	public void setZZ_Date_Recom_Snr_Mgr_SDR (Timestamp ZZ_Date_Recom_Snr_Mgr_SDR);

	/** Get Date Recommended By Snr Mgr SDR	  */
	public Timestamp getZZ_Date_Recom_Snr_Mgr_SDR();

    /** Column name ZZ_Date_Recomm_CEO */
    public static final String COLUMNNAME_ZZ_Date_Recomm_CEO = "ZZ_Date_Recomm_CEO";

	/** Set Date Recommended By CEO	  */
	public void setZZ_Date_Recomm_CEO (Timestamp ZZ_Date_Recomm_CEO);

	/** Get Date Recommended By CEO	  */
	public Timestamp getZZ_Date_Recomm_CEO();

    /** Column name ZZ_Date_Recomm_CFO */
    public static final String COLUMNNAME_ZZ_Date_Recomm_CFO = "ZZ_Date_Recomm_CFO";

	/** Set Date Recommended By CFO	  */
	public void setZZ_Date_Recomm_CFO (Timestamp ZZ_Date_Recomm_CFO);

	/** Get Date Recommended By CFO	  */
	public Timestamp getZZ_Date_Recomm_CFO();

    /** Column name ZZ_Date_Recomm_COO */
    public static final String COLUMNNAME_ZZ_Date_Recomm_COO = "ZZ_Date_Recomm_COO";

	/** Set Date Recommended By COO	  */
	public void setZZ_Date_Recomm_COO (Timestamp ZZ_Date_Recomm_COO);

	/** Get Date Recommended By COO	  */
	public Timestamp getZZ_Date_Recomm_COO();

    /** Column name ZZ_Date_Recomm_Snr_Mgr_Fin */
    public static final String COLUMNNAME_ZZ_Date_Recomm_Snr_Mgr_Fin = "ZZ_Date_Recomm_Snr_Mgr_Fin";

	/** Set Date Recommended By Snr Mgr Finance	  */
	public void setZZ_Date_Recomm_Snr_Mgr_Fin (Timestamp ZZ_Date_Recomm_Snr_Mgr_Fin);

	/** Get Date Recommended By Snr Mgr Finance	  */
	public Timestamp getZZ_Date_Recomm_Snr_Mgr_Fin();

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

    /** Column name ZZ_ExportInvoiceBatchToCsv */
    public static final String COLUMNNAME_ZZ_ExportInvoiceBatchToCsv = "ZZ_ExportInvoiceBatchToCsv";

	/** Set Export Invoice BatchTo Csv	  */
	public void setZZ_ExportInvoiceBatchToCsv (String ZZ_ExportInvoiceBatchToCsv);

	/** Get Export Invoice BatchTo Csv	  */
	public String getZZ_ExportInvoiceBatchToCsv();

    /** Column name ZZ_GL_Allocation_Checked */
    public static final String COLUMNNAME_ZZ_GL_Allocation_Checked = "ZZ_GL_Allocation_Checked";

	/** Set GL Allocation Checked	  */
	public void setZZ_GL_Allocation_Checked (boolean ZZ_GL_Allocation_Checked);

	/** Get GL Allocation Checked	  */
	public boolean isZZ_GL_Allocation_Checked();

    /** Column name ZZ_IS_WSP_ATR */
    public static final String COLUMNNAME_ZZ_IS_WSP_ATR = "ZZ_IS_WSP_ATR";

	/** Set Is WSP ATR Batch	  */
	public void setZZ_IS_WSP_ATR (boolean ZZ_IS_WSP_ATR);

	/** Get Is WSP ATR Batch	  */
	public boolean isZZ_IS_WSP_ATR();

    /** Column name ZZ_Month */
    public static final String COLUMNNAME_ZZ_Month = "ZZ_Month";

	/** Set Month	  */
	public void setZZ_Month (String ZZ_Month);

	/** Get Month	  */
	public String getZZ_Month();

    /** Column name ZZ_Monthly_Levy_Files_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID = "ZZ_Monthly_Levy_Files_Hdr_ID";

	/** Set Monthly Levy Files Hdr	  */
	public void setZZ_Monthly_Levy_Files_Hdr_ID (int ZZ_Monthly_Levy_Files_Hdr_ID);

	/** Get Monthly Levy Files Hdr	  */
	public int getZZ_Monthly_Levy_Files_Hdr_ID();

	public I_ZZ_Monthly_Levy_Files_Hdr getZZ_Monthly_Levy_Files_Hdr() throws RuntimeException;

    /** Column name ZZ_Policy_Procedure_Ck */
    public static final String COLUMNNAME_ZZ_Policy_Procedure_Ck = "ZZ_Policy_Procedure_Ck";

	/** Set Policy Procedure Checklist	  */
	public void setZZ_Policy_Procedure_Ck (String ZZ_Policy_Procedure_Ck);

	/** Get Policy Procedure Checklist	  */
	public String getZZ_Policy_Procedure_Ck();

    /** Column name ZZ_Recommender_ID */
    public static final String COLUMNNAME_ZZ_Recommender_ID = "ZZ_Recommender_ID";

	/** Set Recommender	  */
	public void setZZ_Recommender_ID (int ZZ_Recommender_ID);

	/** Get Recommender	  */
	public int getZZ_Recommender_ID();

	public org.compiere.model.I_AD_User getZZ_Recommender() throws RuntimeException;

    /** Column name ZZ_Snr_Admin_Fin_ID */
    public static final String COLUMNNAME_ZZ_Snr_Admin_Fin_ID = "ZZ_Snr_Admin_Fin_ID";

	/** Set Snr Admin Finance User	  */
	public void setZZ_Snr_Admin_Fin_ID (int ZZ_Snr_Admin_Fin_ID);

	/** Get Snr Admin Finance User	  */
	public int getZZ_Snr_Admin_Fin_ID();

	public org.compiere.model.I_AD_User getZZ_Snr_Admin_Fin() throws RuntimeException;

    /** Column name ZZ_Status */
    public static final String COLUMNNAME_ZZ_Status = "ZZ_Status";

	/** Set Status	  */
	public void setZZ_Status (String ZZ_Status);

	/** Get Status	  */
	public String getZZ_Status();

    /** Column name ZZ_Submitter_ID */
    public static final String COLUMNNAME_ZZ_Submitter_ID = "ZZ_Submitter_ID";

	/** Set Submitted By	  */
	public void setZZ_Submitter_ID (int ZZ_Submitter_ID);

	/** Get Submitted By	  */
	public int getZZ_Submitter_ID();

	public org.compiere.model.I_AD_User getZZ_Submitter() throws RuntimeException;

    /** Column name ZZ_Year */
    public static final String COLUMNNAME_ZZ_Year = "ZZ_Year";

	/** Set Year	  */
	public void setZZ_Year (String ZZ_Year);

	/** Get Year	  */
	public String getZZ_Year();
}
