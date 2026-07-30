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

/** Generated Model for ZZ_Petty_Cash_Recon_Hdr
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_Petty_Cash_Recon_Hdr")
public class X_ZZ_Petty_Cash_Recon_Hdr extends PO implements I_ZZ_Petty_Cash_Recon_Hdr, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260730L;

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Hdr (Properties ctx, int ZZ_Petty_Cash_Recon_Hdr_ID, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Hdr_ID, trxName);
      /** if (ZZ_Petty_Cash_Recon_Hdr_ID == 0)
        {
			setZZ_Petty_Cash_Recon_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Hdr (Properties ctx, int ZZ_Petty_Cash_Recon_Hdr_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Hdr_ID, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Recon_Hdr_ID == 0)
        {
			setZZ_Petty_Cash_Recon_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Hdr (Properties ctx, String ZZ_Petty_Cash_Recon_Hdr_UU, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Hdr_UU, trxName);
      /** if (ZZ_Petty_Cash_Recon_Hdr_UU == null)
        {
			setZZ_Petty_Cash_Recon_Hdr_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Hdr (Properties ctx, String ZZ_Petty_Cash_Recon_Hdr_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Hdr_UU, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Recon_Hdr_UU == null)
        {
			setZZ_Petty_Cash_Recon_Hdr_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_Petty_Cash_Recon_Hdr (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_Petty_Cash_Recon_Hdr[")
        .append(get_ID()).append("]");
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

	/** Set End Date.
		@param EndDate Last effective date (inclusive)
	*/
	public void setEndDate (Timestamp EndDate)
	{
		set_Value (COLUMNNAME_EndDate, EndDate);
	}

	/** Get End Date.
		@return Last effective date (inclusive)
	  */
	public Timestamp getEndDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_EndDate);
	}

	/** Set Start Date.
		@param StartDate First effective day (inclusive)
	*/
	public void setStartDate (Timestamp StartDate)
	{
		set_Value (COLUMNNAME_StartDate, StartDate);
	}

	/** Get Start Date.
		@return First effective day (inclusive)
	  */
	public Timestamp getStartDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_StartDate);
	}

	/** Set Actual Cash On Hand.
		@param ZZ_Actual_COH Actual Cash On Hand
	*/
	public void setZZ_Actual_COH (BigDecimal ZZ_Actual_COH)
	{
		set_Value (COLUMNNAME_ZZ_Actual_COH, ZZ_Actual_COH);
	}

	/** Get Actual Cash On Hand.
		@return Actual Cash On Hand	  */
	public BigDecimal getZZ_Actual_COH()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Actual_COH);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Advance Total.
		@param ZZ_Advance_Total Advance Total
	*/
	public void setZZ_Advance_Total (BigDecimal ZZ_Advance_Total)
	{
		set_Value (COLUMNNAME_ZZ_Advance_Total, ZZ_Advance_Total);
	}

	/** Get Advance Total.
		@return Advance Total	  */
	public BigDecimal getZZ_Advance_Total()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Advance_Total);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Calculated Cash On Hand.
		@param ZZ_Calculated_COH Calculated Cash On Hand
	*/
	public void setZZ_Calculated_COH (BigDecimal ZZ_Calculated_COH)
	{
		set_Value (COLUMNNAME_ZZ_Calculated_COH, ZZ_Calculated_COH);
	}

	/** Get Calculated Cash On Hand.
		@return Calculated Cash On Hand	  */
	public BigDecimal getZZ_Calculated_COH()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Calculated_COH);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Claim Total.
		@param ZZ_Claim_Total Claim Total
	*/
	public void setZZ_Claim_Total (BigDecimal ZZ_Claim_Total)
	{
		set_Value (COLUMNNAME_ZZ_Claim_Total, ZZ_Claim_Total);
	}

	/** Get Claim Total.
		@return Claim Total	  */
	public BigDecimal getZZ_Claim_Total()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Claim_Total);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Create Journal Lines.
		@param ZZ_Create_Journal_Lines Create Journal Lines
	*/
	public void setZZ_Create_Journal_Lines (String ZZ_Create_Journal_Lines)
	{
		set_Value (COLUMNNAME_ZZ_Create_Journal_Lines, ZZ_Create_Journal_Lines);
	}

	/** Get Create Journal Lines.
		@return Create Journal Lines	  */
	public String getZZ_Create_Journal_Lines()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Create_Journal_Lines);
	}

	/** Set Create Lines.
		@param ZZ_Create_Lines Create Lines
	*/
	public void setZZ_Create_Lines (String ZZ_Create_Lines)
	{
		set_Value (COLUMNNAME_ZZ_Create_Lines, ZZ_Create_Lines);
	}

	/** Get Create Lines.
		@return Create Lines	  */
	public String getZZ_Create_Lines()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Create_Lines);
	}

	/** Set Float.
		@param ZZ_Float_Amt Float
	*/
	public void setZZ_Float_Amt (BigDecimal ZZ_Float_Amt)
	{
		set_Value (COLUMNNAME_ZZ_Float_Amt, ZZ_Float_Amt);
	}

	/** Get Float.
		@return Float	  */
	public BigDecimal getZZ_Float_Amt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Float_Amt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Petty Cash Recon.
		@param ZZ_Petty_Cash_Recon_Hdr_ID Petty Cash Recon
	*/
	public void setZZ_Petty_Cash_Recon_Hdr_ID (int ZZ_Petty_Cash_Recon_Hdr_ID)
	{
		if (ZZ_Petty_Cash_Recon_Hdr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_ID, Integer.valueOf(ZZ_Petty_Cash_Recon_Hdr_ID));
	}

	/** Get Petty Cash Recon.
		@return Petty Cash Recon	  */
	public int getZZ_Petty_Cash_Recon_Hdr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_Petty_Cash_Recon_Hdr_UU.
		@param ZZ_Petty_Cash_Recon_Hdr_UU ZZ_Petty_Cash_Recon_Hdr_UU
	*/
	public void setZZ_Petty_Cash_Recon_Hdr_UU (String ZZ_Petty_Cash_Recon_Hdr_UU)
	{
		set_Value (COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_UU, ZZ_Petty_Cash_Recon_Hdr_UU);
	}

	/** Get ZZ_Petty_Cash_Recon_Hdr_UU.
		@return ZZ_Petty_Cash_Recon_Hdr_UU	  */
	public String getZZ_Petty_Cash_Recon_Hdr_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_UU);
	}
}