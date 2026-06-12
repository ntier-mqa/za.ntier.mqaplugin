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

/** Generated Model for C_InvoiceBatch
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_InvoiceBatch")
public class X_C_InvoiceBatch extends PO implements I_C_InvoiceBatch, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260612L;

    /** Standard Constructor */
    public X_C_InvoiceBatch (Properties ctx, int C_InvoiceBatch_ID, String trxName)
    {
      super (ctx, C_InvoiceBatch_ID, trxName);
      /** if (C_InvoiceBatch_ID == 0)
        {
			setC_Currency_ID (0);
// @$C_Currency_ID@
			setC_InvoiceBatch_ID (0);
			setControlAmt (Env.ZERO);
// 0
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocumentAmt (Env.ZERO);
			setDocumentNo (null);
			setIsSOTrx (false);
// N
			setProcessed (false);
			setSalesRep_ID (0);
			setZZ_Account_Reconned (false);
// N
			setZZ_Auth_PO_Order (false);
// N
			setZZ_Calcs_Checked (false);
// N
			setZZ_Cred_Bank_Dets_Verified (false);
// N
			setZZ_GL_Allocation_Checked (false);
// N
			setZZ_IS_WSP_ATR (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_InvoiceBatch (Properties ctx, int C_InvoiceBatch_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_InvoiceBatch_ID, trxName, virtualColumns);
      /** if (C_InvoiceBatch_ID == 0)
        {
			setC_Currency_ID (0);
// @$C_Currency_ID@
			setC_InvoiceBatch_ID (0);
			setControlAmt (Env.ZERO);
// 0
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocumentAmt (Env.ZERO);
			setDocumentNo (null);
			setIsSOTrx (false);
// N
			setProcessed (false);
			setSalesRep_ID (0);
			setZZ_Account_Reconned (false);
// N
			setZZ_Auth_PO_Order (false);
// N
			setZZ_Calcs_Checked (false);
// N
			setZZ_Cred_Bank_Dets_Verified (false);
// N
			setZZ_GL_Allocation_Checked (false);
// N
			setZZ_IS_WSP_ATR (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_InvoiceBatch (Properties ctx, String C_InvoiceBatch_UU, String trxName)
    {
      super (ctx, C_InvoiceBatch_UU, trxName);
      /** if (C_InvoiceBatch_UU == null)
        {
			setC_Currency_ID (0);
// @$C_Currency_ID@
			setC_InvoiceBatch_ID (0);
			setControlAmt (Env.ZERO);
// 0
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocumentAmt (Env.ZERO);
			setDocumentNo (null);
			setIsSOTrx (false);
// N
			setProcessed (false);
			setSalesRep_ID (0);
			setZZ_Account_Reconned (false);
// N
			setZZ_Auth_PO_Order (false);
// N
			setZZ_Calcs_Checked (false);
// N
			setZZ_Cred_Bank_Dets_Verified (false);
// N
			setZZ_GL_Allocation_Checked (false);
// N
			setZZ_IS_WSP_ATR (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_InvoiceBatch (Properties ctx, String C_InvoiceBatch_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_InvoiceBatch_UU, trxName, virtualColumns);
      /** if (C_InvoiceBatch_UU == null)
        {
			setC_Currency_ID (0);
// @$C_Currency_ID@
			setC_InvoiceBatch_ID (0);
			setControlAmt (Env.ZERO);
// 0
			setDateDoc (new Timestamp( System.currentTimeMillis() ));
// @#Date@
			setDocumentAmt (Env.ZERO);
			setDocumentNo (null);
			setIsSOTrx (false);
// N
			setProcessed (false);
			setSalesRep_ID (0);
			setZZ_Account_Reconned (false);
// N
			setZZ_Auth_PO_Order (false);
// N
			setZZ_Calcs_Checked (false);
// N
			setZZ_Cred_Bank_Dets_Verified (false);
// N
			setZZ_GL_Allocation_Checked (false);
// N
			setZZ_IS_WSP_ATR (false);
// N
        } */
    }

    /** Load Constructor */
    public X_C_InvoiceBatch (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_InvoiceBatch[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Invoice Batch.
		@param C_InvoiceBatch_ID Expense Invoice Batch Header
	*/
	public void setC_InvoiceBatch_ID (int C_InvoiceBatch_ID)
	{
		if (C_InvoiceBatch_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_InvoiceBatch_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_InvoiceBatch_ID, Integer.valueOf(C_InvoiceBatch_ID));
	}

	/** Get Invoice Batch.
		@return Expense Invoice Batch Header
	  */
	public int getC_InvoiceBatch_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_InvoiceBatch_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_InvoiceBatch_UU.
		@param C_InvoiceBatch_UU C_InvoiceBatch_UU
	*/
	public void setC_InvoiceBatch_UU (String C_InvoiceBatch_UU)
	{
		set_Value (COLUMNNAME_C_InvoiceBatch_UU, C_InvoiceBatch_UU);
	}

	/** Get C_InvoiceBatch_UU.
		@return C_InvoiceBatch_UU	  */
	public String getC_InvoiceBatch_UU()
	{
		return (String)get_Value(COLUMNNAME_C_InvoiceBatch_UU);
	}

	/** Set Control Amount.
		@param ControlAmt If not zero, the Debit amount of the document must be equal this amount
	*/
	public void setControlAmt (BigDecimal ControlAmt)
	{
		set_Value (COLUMNNAME_ControlAmt, ControlAmt);
	}

	/** Get Control Amount.
		@return If not zero, the Debit amount of the document must be equal this amount
	  */
	public BigDecimal getControlAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ControlAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Document Date.
		@param DateDoc Date of the Document
	*/
	public void setDateDoc (Timestamp DateDoc)
	{
		set_Value (COLUMNNAME_DateDoc, DateDoc);
	}

	/** Get Document Date.
		@return Date of the Document
	  */
	public Timestamp getDateDoc()
	{
		return (Timestamp)get_Value(COLUMNNAME_DateDoc);
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

	/** Set Document Amt.
		@param DocumentAmt Document Amount
	*/
	public void setDocumentAmt (BigDecimal DocumentAmt)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentAmt, DocumentAmt);
	}

	/** Get Document Amt.
		@return Document Amount
	  */
	public BigDecimal getDocumentAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_DocumentAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Sales Transaction.
		@param IsSOTrx This is a Sales Transaction
	*/
	public void setIsSOTrx (boolean IsSOTrx)
	{
		set_Value (COLUMNNAME_IsSOTrx, Boolean.valueOf(IsSOTrx));
	}

	/** Get Sales Transaction.
		@return This is a Sales Transaction
	  */
	public boolean isSOTrx()
	{
		Object oo = get_Value(COLUMNNAME_IsSOTrx);
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

	public org.compiere.model.I_AD_User getSalesRep() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getSalesRep_ID(), get_TrxName());
	}

	/** Set Sales Representative.
		@param SalesRep_ID Sales Representative or Company Agent
	*/
	public void setSalesRep_ID (int SalesRep_ID)
	{
		if (SalesRep_ID < 1)
			set_Value (COLUMNNAME_SalesRep_ID, null);
		else
			set_Value (COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID));
	}

	/** Get Sales Representative.
		@return Sales Representative or Company Agent
	  */
	public int getSalesRep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesRep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Account reconciled / O/S invoices verified.
		@param ZZ_Account_Reconned Account reconciled / O/S invoices verified
	*/
	public void setZZ_Account_Reconned (boolean ZZ_Account_Reconned)
	{
		set_Value (COLUMNNAME_ZZ_Account_Reconned, Boolean.valueOf(ZZ_Account_Reconned));
	}

	/** Get Account reconciled / O/S invoices verified.
		@return Account reconciled / O/S invoices verified	  */
	public boolean isZZ_Account_Reconned()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Account_Reconned);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Authorized Purchase Order/SLA Attached.
		@param ZZ_Auth_PO_Order Authorized Purchase Order/SLA Attached
	*/
	public void setZZ_Auth_PO_Order (boolean ZZ_Auth_PO_Order)
	{
		set_Value (COLUMNNAME_ZZ_Auth_PO_Order, Boolean.valueOf(ZZ_Auth_PO_Order));
	}

	/** Get Authorized Purchase Order/SLA Attached.
		@return Authorized Purchase Order/SLA Attached	  */
	public boolean isZZ_Auth_PO_Order()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Auth_PO_Order);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_AD_User getZZ_CEO() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CEO_ID(), get_TrxName());
	}

	/** Set CEO.
		@param ZZ_CEO_ID CEO
	*/
	public void setZZ_CEO_ID (int ZZ_CEO_ID)
	{
		if (ZZ_CEO_ID < 1)
			set_Value (COLUMNNAME_ZZ_CEO_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CEO_ID, Integer.valueOf(ZZ_CEO_ID));
	}

	/** Get CEO.
		@return CEO	  */
	public int getZZ_CEO_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CEO_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_CFO() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_CFO_ID(), get_TrxName());
	}

	/** Set CFO.
		@param ZZ_CFO_ID CFO
	*/
	public void setZZ_CFO_ID (int ZZ_CFO_ID)
	{
		if (ZZ_CFO_ID < 1)
			set_Value (COLUMNNAME_ZZ_CFO_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_CFO_ID, Integer.valueOf(ZZ_CFO_ID));
	}

	/** Get CFO.
		@return CFO	  */
	public int getZZ_CFO_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_CFO_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getZZ_COO() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_COO_ID(), get_TrxName());
	}

	/** Set COO.
		@param ZZ_COO_ID COO
	*/
	public void setZZ_COO_ID (int ZZ_COO_ID)
	{
		if (ZZ_COO_ID < 1)
			set_Value (COLUMNNAME_ZZ_COO_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_COO_ID, Integer.valueOf(ZZ_COO_ID));
	}

	/** Get COO.
		@return COO	  */
	public int getZZ_COO_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_COO_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Calculation Checked.
		@param ZZ_Calcs_Checked Calculation Checked
	*/
	public void setZZ_Calcs_Checked (boolean ZZ_Calcs_Checked)
	{
		set_Value (COLUMNNAME_ZZ_Calcs_Checked, Boolean.valueOf(ZZ_Calcs_Checked));
	}

	/** Get Calculation Checked.
		@return Calculation Checked	  */
	public boolean isZZ_Calcs_Checked()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Calcs_Checked);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Creditor ID &amp; Banking Details Verified.
		@param ZZ_Cred_Bank_Dets_Verified Creditor ID &amp; Banking Details Verified
	*/
	public void setZZ_Cred_Bank_Dets_Verified (boolean ZZ_Cred_Bank_Dets_Verified)
	{
		set_Value (COLUMNNAME_ZZ_Cred_Bank_Dets_Verified, Boolean.valueOf(ZZ_Cred_Bank_Dets_Verified));
	}

	/** Get Creditor ID &amp; Banking Details Verified.
		@return Creditor ID &amp; Banking Details Verified	  */
	public boolean isZZ_Cred_Bank_Dets_Verified()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Cred_Bank_Dets_Verified);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Date Not Approved By CEO.
		@param ZZ_Date_Not_Recom_CEO Date Not Approved By CEO
	*/
	public void setZZ_Date_Not_Recom_CEO (Timestamp ZZ_Date_Not_Recom_CEO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Recom_CEO, ZZ_Date_Not_Recom_CEO);
	}

	/** Get Date Not Approved By CEO.
		@return Date Not Approved By CEO	  */
	public Timestamp getZZ_Date_Not_Recom_CEO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Recom_CEO);
	}

	/** Set Date Not Recommended By CFO.
		@param ZZ_Date_Not_Recom_CFO Date Not Recommended By CFO
	*/
	public void setZZ_Date_Not_Recom_CFO (Timestamp ZZ_Date_Not_Recom_CFO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Recom_CFO, ZZ_Date_Not_Recom_CFO);
	}

	/** Get Date Not Recommended By CFO.
		@return Date Not Recommended By CFO	  */
	public Timestamp getZZ_Date_Not_Recom_CFO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Recom_CFO);
	}

	/** Set Date Not Recommended By COO.
		@param ZZ_Date_Not_Recom_COO Date Not Recommended By COO
	*/
	public void setZZ_Date_Not_Recom_COO (Timestamp ZZ_Date_Not_Recom_COO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Recom_COO, ZZ_Date_Not_Recom_COO);
	}

	/** Get Date Not Recommended By COO.
		@return Date Not Recommended By COO	  */
	public Timestamp getZZ_Date_Not_Recom_COO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Recom_COO);
	}

	/** Set Date Not Recommended By Snr Mgr SDR.
		@param ZZ_Date_Not_Recom_Snr_Mgr_SDR Date Not Recommended By Snr Mgr SDR
	*/
	public void setZZ_Date_Not_Recom_Snr_Mgr_SDR (Timestamp ZZ_Date_Not_Recom_Snr_Mgr_SDR)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Recom_Snr_Mgr_SDR, ZZ_Date_Not_Recom_Snr_Mgr_SDR);
	}

	/** Get Date Not Recommended By Snr Mgr SDR.
		@return Date Not Recommended By Snr Mgr SDR	  */
	public Timestamp getZZ_Date_Not_Recom_Snr_Mgr_SDR()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Recom_Snr_Mgr_SDR);
	}

	/** Set Date Not Recommended By Snr Mgr Finance.
		@param ZZ_Date_Not_Recomm_Snr_Mgr_Fin Date Not Recommended By Snr Mgr Finance
	*/
	public void setZZ_Date_Not_Recomm_Snr_Mgr_Fin (Timestamp ZZ_Date_Not_Recomm_Snr_Mgr_Fin)
	{
		set_Value (COLUMNNAME_ZZ_Date_Not_Recomm_Snr_Mgr_Fin, ZZ_Date_Not_Recomm_Snr_Mgr_Fin);
	}

	/** Get Date Not Recommended By Snr Mgr Finance.
		@return Date Not Recommended By Snr Mgr Finance	  */
	public Timestamp getZZ_Date_Not_Recomm_Snr_Mgr_Fin()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Not_Recomm_Snr_Mgr_Fin);
	}

	/** Set Date Recommended By Snr Mgr SDR.
		@param ZZ_Date_Recom_Snr_Mgr_SDR Date Recommended By Snr Mgr SDR
	*/
	public void setZZ_Date_Recom_Snr_Mgr_SDR (Timestamp ZZ_Date_Recom_Snr_Mgr_SDR)
	{
		set_Value (COLUMNNAME_ZZ_Date_Recom_Snr_Mgr_SDR, ZZ_Date_Recom_Snr_Mgr_SDR);
	}

	/** Get Date Recommended By Snr Mgr SDR.
		@return Date Recommended By Snr Mgr SDR	  */
	public Timestamp getZZ_Date_Recom_Snr_Mgr_SDR()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Recom_Snr_Mgr_SDR);
	}

	/** Set Date Recommended By CEO.
		@param ZZ_Date_Recomm_CEO Date Recommended By CEO
	*/
	public void setZZ_Date_Recomm_CEO (Timestamp ZZ_Date_Recomm_CEO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Recomm_CEO, ZZ_Date_Recomm_CEO);
	}

	/** Get Date Recommended By CEO.
		@return Date Recommended By CEO	  */
	public Timestamp getZZ_Date_Recomm_CEO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Recomm_CEO);
	}

	/** Set Date Recommended By CFO.
		@param ZZ_Date_Recomm_CFO Date Recommended By CFO
	*/
	public void setZZ_Date_Recomm_CFO (Timestamp ZZ_Date_Recomm_CFO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Recomm_CFO, ZZ_Date_Recomm_CFO);
	}

	/** Get Date Recommended By CFO.
		@return Date Recommended By CFO	  */
	public Timestamp getZZ_Date_Recomm_CFO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Recomm_CFO);
	}

	/** Set Date Recommended By COO.
		@param ZZ_Date_Recomm_COO Date Recommended By COO
	*/
	public void setZZ_Date_Recomm_COO (Timestamp ZZ_Date_Recomm_COO)
	{
		set_Value (COLUMNNAME_ZZ_Date_Recomm_COO, ZZ_Date_Recomm_COO);
	}

	/** Get Date Recommended By COO.
		@return Date Recommended By COO	  */
	public Timestamp getZZ_Date_Recomm_COO()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Recomm_COO);
	}

	/** Set Date Recommended By Snr Mgr Finance.
		@param ZZ_Date_Recomm_Snr_Mgr_Fin Date Recommended By Snr Mgr Finance
	*/
	public void setZZ_Date_Recomm_Snr_Mgr_Fin (Timestamp ZZ_Date_Recomm_Snr_Mgr_Fin)
	{
		set_Value (COLUMNNAME_ZZ_Date_Recomm_Snr_Mgr_Fin, ZZ_Date_Recomm_Snr_Mgr_Fin);
	}

	/** Get Date Recommended By Snr Mgr Finance.
		@return Date Recommended By Snr Mgr Finance	  */
	public Timestamp getZZ_Date_Recomm_Snr_Mgr_Fin()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Date_Recomm_Snr_Mgr_Fin);
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

	/** Set Export Invoice BatchTo Csv.
		@param ZZ_ExportInvoiceBatchToCsv Export Invoice BatchTo Csv
	*/
	public void setZZ_ExportInvoiceBatchToCsv (String ZZ_ExportInvoiceBatchToCsv)
	{
		set_Value (COLUMNNAME_ZZ_ExportInvoiceBatchToCsv, ZZ_ExportInvoiceBatchToCsv);
	}

	/** Get Export Invoice BatchTo Csv.
		@return Export Invoice BatchTo Csv	  */
	public String getZZ_ExportInvoiceBatchToCsv()
	{
		return (String)get_Value(COLUMNNAME_ZZ_ExportInvoiceBatchToCsv);
	}

	/** Set GL Allocation Checked.
		@param ZZ_GL_Allocation_Checked GL Allocation Checked
	*/
	public void setZZ_GL_Allocation_Checked (boolean ZZ_GL_Allocation_Checked)
	{
		set_Value (COLUMNNAME_ZZ_GL_Allocation_Checked, Boolean.valueOf(ZZ_GL_Allocation_Checked));
	}

	/** Get GL Allocation Checked.
		@return GL Allocation Checked	  */
	public boolean isZZ_GL_Allocation_Checked()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_GL_Allocation_Checked);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Is WSP ATR Batch.
		@param ZZ_IS_WSP_ATR Is WSP ATR Batch
	*/
	public void setZZ_IS_WSP_ATR (boolean ZZ_IS_WSP_ATR)
	{
		set_Value (COLUMNNAME_ZZ_IS_WSP_ATR, Boolean.valueOf(ZZ_IS_WSP_ATR));
	}

	/** Get Is WSP ATR Batch.
		@return Is WSP ATR Batch	  */
	public boolean isZZ_IS_WSP_ATR()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_IS_WSP_ATR);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** January = 01 */
	public static final String ZZ_MONTH_January = "01";
	/** February = 02 */
	public static final String ZZ_MONTH_February = "02";
	/** March = 03 */
	public static final String ZZ_MONTH_March = "03";
	/** April = 04 */
	public static final String ZZ_MONTH_April = "04";
	/** May = 05 */
	public static final String ZZ_MONTH_May = "05";
	/** June = 06 */
	public static final String ZZ_MONTH_June = "06";
	/** July = 07 */
	public static final String ZZ_MONTH_July = "07";
	/** August = 08 */
	public static final String ZZ_MONTH_August = "08";
	/** September = 09 */
	public static final String ZZ_MONTH_September = "09";
	/** October = 10 */
	public static final String ZZ_MONTH_October = "10";
	/** November = 11 */
	public static final String ZZ_MONTH_November = "11";
	/** December = 12 */
	public static final String ZZ_MONTH_December = "12";
	/** Set Month.
		@param ZZ_Month Month
	*/
	public void setZZ_Month (String ZZ_Month)
	{

		set_Value (COLUMNNAME_ZZ_Month, ZZ_Month);
	}

	/** Get Month.
		@return Month	  */
	public String getZZ_Month()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Month);
	}

	public I_ZZ_Monthly_Levy_Files_Hdr getZZ_Monthly_Levy_Files_Hdr() throws RuntimeException
	{
		return (I_ZZ_Monthly_Levy_Files_Hdr)MTable.get(getCtx(), I_ZZ_Monthly_Levy_Files_Hdr.Table_ID)
			.getPO(getZZ_Monthly_Levy_Files_Hdr_ID(), get_TrxName());
	}

	/** Set Monthly Levy Files Hdr.
		@param ZZ_Monthly_Levy_Files_Hdr_ID Monthly Levy Files Hdr
	*/
	public void setZZ_Monthly_Levy_Files_Hdr_ID (int ZZ_Monthly_Levy_Files_Hdr_ID)
	{
		if (ZZ_Monthly_Levy_Files_Hdr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID, Integer.valueOf(ZZ_Monthly_Levy_Files_Hdr_ID));
	}

	/** Get Monthly Levy Files Hdr.
		@return Monthly Levy Files Hdr	  */
	public int getZZ_Monthly_Levy_Files_Hdr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Payments R2 000 - R10 000: two quotes obtained = 1 */
	public static final String ZZ_POLICY_PROCEDURE_CK_PaymentsR2000_R10000TwoQuotesObtained = "1";
	/** Payments R10 000 - R200 000: 3 or more quotes applicable = 2 */
	public static final String ZZ_POLICY_PROCEDURE_CK_PaymentsR10000_R2000003OrMoreQuotesApplicable = "2";
	/** Payments above R200 000:competitive bids obtained (attached) = 3 */
	public static final String ZZ_POLICY_PROCEDURE_CK_PaymentsAboveR200000CompetitiveBidsObtainedAttached = "3";
	/** Accepted tender approved by procurement committee (Attached) = 4 */
	public static final String ZZ_POLICY_PROCEDURE_CK_AcceptedTenderApprovedByProcurementCommitteeAttached = "4";
	/** Capital expenditure budgeted - approved by CFO = 5 */
	public static final String ZZ_POLICY_PROCEDURE_CK_CapitalExpenditureBudgeted_ApprovedByCFO = "5";
	/** Set Policy Procedure Checklist.
		@param ZZ_Policy_Procedure_Ck Policy Procedure Checklist
	*/
	public void setZZ_Policy_Procedure_Ck (String ZZ_Policy_Procedure_Ck)
	{

		set_Value (COLUMNNAME_ZZ_Policy_Procedure_Ck, ZZ_Policy_Procedure_Ck);
	}

	/** Get Policy Procedure Checklist.
		@return Policy Procedure Checklist	  */
	public String getZZ_Policy_Procedure_Ck()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Policy_Procedure_Ck);
	}

	public org.compiere.model.I_AD_User getZZ_Recommender() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Recommender_ID(), get_TrxName());
	}

	/** Set Recommender.
		@param ZZ_Recommender_ID Recommender
	*/
	public void setZZ_Recommender_ID (int ZZ_Recommender_ID)
	{
		if (ZZ_Recommender_ID < 1)
			set_Value (COLUMNNAME_ZZ_Recommender_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Recommender_ID, Integer.valueOf(ZZ_Recommender_ID));
	}

	/** Get Recommender.
		@return Recommender	  */
	public int getZZ_Recommender_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Recommender_ID);
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

	/** Completed = C */
	public static final String ZZ_STATUS_Completed = "C";
	/** Drafted = D */
	public static final String ZZ_STATUS_Drafted = "D";
	/** In Progress = I */
	public static final String ZZ_STATUS_InProgress = "I";
	/** Set Status.
		@param ZZ_Status Status
	*/
	public void setZZ_Status (String ZZ_Status)
	{

		set_Value (COLUMNNAME_ZZ_Status, ZZ_Status);
	}

	/** Get Status.
		@return Status	  */
	public String getZZ_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Status);
	}

	public org.compiere.model.I_AD_User getZZ_Submitter() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getZZ_Submitter_ID(), get_TrxName());
	}

	/** Set Submitted By.
		@param ZZ_Submitter_ID Submitted By
	*/
	public void setZZ_Submitter_ID (int ZZ_Submitter_ID)
	{
		if (ZZ_Submitter_ID < 1)
			set_Value (COLUMNNAME_ZZ_Submitter_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Submitter_ID, Integer.valueOf(ZZ_Submitter_ID));
	}

	/** Get Submitted By.
		@return Submitted By	  */
	public int getZZ_Submitter_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Submitter_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Year.
		@param ZZ_Year Year
	*/
	public void setZZ_Year (String ZZ_Year)
	{
		set_Value (COLUMNNAME_ZZ_Year, ZZ_Year);
	}

	/** Get Year.
		@return Year	  */
	public String getZZ_Year()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Year);
	}
}