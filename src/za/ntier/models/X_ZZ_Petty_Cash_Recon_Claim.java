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
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ZZ_Petty_Cash_Recon_Claim
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_Petty_Cash_Recon_Claim")
public class X_ZZ_Petty_Cash_Recon_Claim extends PO implements I_ZZ_Petty_Cash_Recon_Claim, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260730L;

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Claim (Properties ctx, int ZZ_Petty_Cash_Recon_Claim_ID, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Claim_ID, trxName);
      /** if (ZZ_Petty_Cash_Recon_Claim_ID == 0)
        {
			setZZ_Petty_Cash_Recon_Claim_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Claim (Properties ctx, int ZZ_Petty_Cash_Recon_Claim_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Claim_ID, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Recon_Claim_ID == 0)
        {
			setZZ_Petty_Cash_Recon_Claim_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Claim (Properties ctx, String ZZ_Petty_Cash_Recon_Claim_UU, String trxName)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Claim_UU, trxName);
      /** if (ZZ_Petty_Cash_Recon_Claim_UU == null)
        {
			setZZ_Petty_Cash_Recon_Claim_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Petty_Cash_Recon_Claim (Properties ctx, String ZZ_Petty_Cash_Recon_Claim_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Petty_Cash_Recon_Claim_UU, trxName, virtualColumns);
      /** if (ZZ_Petty_Cash_Recon_Claim_UU == null)
        {
			setZZ_Petty_Cash_Recon_Claim_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_Petty_Cash_Recon_Claim (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_Petty_Cash_Recon_Claim[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Amount.
		@param Amount Amount in a defined currency
	*/
	public void setAmount (BigDecimal Amount)
	{
		set_ValueNoCheck (COLUMNNAME_Amount, Amount);
	}

	/** Get Amount.
		@return Amount in a defined currency
	  */
	public BigDecimal getAmount()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Amount);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException
	{
		return (org.compiere.model.I_C_Charge)MTable.get(getCtx(), org.compiere.model.I_C_Charge.Table_ID)
			.getPO(getC_Charge_ID(), get_TrxName());
	}

	/** Set Charge.
		@param C_Charge_ID Additional document charges
	*/
	public void setC_Charge_ID (int C_Charge_ID)
	{
		if (C_Charge_ID < 1)
			set_Value (COLUMNNAME_C_Charge_ID, null);
		else
			set_Value (COLUMNNAME_C_Charge_ID, Integer.valueOf(C_Charge_ID));
	}

	/** Get Charge.
		@return Additional document charges
	  */
	public int getC_Charge_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Charge_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_GL_JournalLine getGL_JournalLine() throws RuntimeException
	{
		return (org.compiere.model.I_GL_JournalLine)MTable.get(getCtx(), org.compiere.model.I_GL_JournalLine.Table_ID)
			.getPO(getGL_JournalLine_ID(), get_TrxName());
	}

	/** Set Journal Line.
		@param GL_JournalLine_ID General Ledger Journal Line
	*/
	public void setGL_JournalLine_ID (int GL_JournalLine_ID)
	{
		if (GL_JournalLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_GL_JournalLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_GL_JournalLine_ID, Integer.valueOf(GL_JournalLine_ID));
	}

	/** Get Journal Line.
		@return General Ledger Journal Line
	  */
	public int getGL_JournalLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_GL_JournalLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Line No.
		@param Line Unique line for this document
	*/
	public void setLine (int Line)
	{
		set_ValueNoCheck (COLUMNNAME_Line, Integer.valueOf(Line));
	}

	/** Get Line No.
		@return Unique line for this document
	  */
	public int getLine()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Line);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_ZZ_Petty_Cash_Advance_Hdr getZZ_Petty_Cash_Advance_Hdr() throws RuntimeException
	{
		return (I_ZZ_Petty_Cash_Advance_Hdr)MTable.get(getCtx(), I_ZZ_Petty_Cash_Advance_Hdr.Table_ID)
			.getPO(getZZ_Petty_Cash_Advance_Hdr_ID(), get_TrxName());
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

	public I_ZZ_Petty_Cash_Claim_Hdr getZZ_Petty_Cash_Claim_Hdr() throws RuntimeException
	{
		return (I_ZZ_Petty_Cash_Claim_Hdr)MTable.get(getCtx(), I_ZZ_Petty_Cash_Claim_Hdr.Table_ID)
			.getPO(getZZ_Petty_Cash_Claim_Hdr_ID(), get_TrxName());
	}

	/** Set Petty Cash Claim.
		@param ZZ_Petty_Cash_Claim_Hdr_ID Petty Cash Claim
	*/
	public void setZZ_Petty_Cash_Claim_Hdr_ID (int ZZ_Petty_Cash_Claim_Hdr_ID)
	{
		if (ZZ_Petty_Cash_Claim_Hdr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Claim_Hdr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Claim_Hdr_ID, Integer.valueOf(ZZ_Petty_Cash_Claim_Hdr_ID));
	}

	/** Get Petty Cash Claim.
		@return Petty Cash Claim	  */
	public int getZZ_Petty_Cash_Claim_Hdr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Petty_Cash_Claim_Hdr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Petty_Cash_Claim_Line getZZ_Petty_Cash_Claim_Line() throws RuntimeException
	{
		return (I_ZZ_Petty_Cash_Claim_Line)MTable.get(getCtx(), I_ZZ_Petty_Cash_Claim_Line.Table_ID)
			.getPO(getZZ_Petty_Cash_Claim_Line_ID(), get_TrxName());
	}

	/** Set Petty Cash Claim Line.
		@param ZZ_Petty_Cash_Claim_Line_ID Petty Cash Claim Line
	*/
	public void setZZ_Petty_Cash_Claim_Line_ID (int ZZ_Petty_Cash_Claim_Line_ID)
	{
		if (ZZ_Petty_Cash_Claim_Line_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Claim_Line_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Claim_Line_ID, Integer.valueOf(ZZ_Petty_Cash_Claim_Line_ID));
	}

	/** Get Petty Cash Claim Line.
		@return Petty Cash Claim Line	  */
	public int getZZ_Petty_Cash_Claim_Line_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Petty_Cash_Claim_Line_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Motivation.
		@param ZZ_Petty_Cash_Motivation Motivation
	*/
	public void setZZ_Petty_Cash_Motivation (String ZZ_Petty_Cash_Motivation)
	{
		set_Value (COLUMNNAME_ZZ_Petty_Cash_Motivation, ZZ_Petty_Cash_Motivation);
	}

	/** Get Motivation.
		@return Motivation	  */
	public String getZZ_Petty_Cash_Motivation()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Petty_Cash_Motivation);
	}

	/** Set Petty Cash Recon Claim.
		@param ZZ_Petty_Cash_Recon_Claim_ID Petty Cash Recon Claim
	*/
	public void setZZ_Petty_Cash_Recon_Claim_ID (int ZZ_Petty_Cash_Recon_Claim_ID)
	{
		if (ZZ_Petty_Cash_Recon_Claim_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_ID, Integer.valueOf(ZZ_Petty_Cash_Recon_Claim_ID));
	}

	/** Get Petty Cash Recon Claim.
		@return Petty Cash Recon Claim	  */
	public int getZZ_Petty_Cash_Recon_Claim_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_Petty_Cash_Recon_Claim_UU.
		@param ZZ_Petty_Cash_Recon_Claim_UU ZZ_Petty_Cash_Recon_Claim_UU
	*/
	public void setZZ_Petty_Cash_Recon_Claim_UU (String ZZ_Petty_Cash_Recon_Claim_UU)
	{
		set_Value (COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_UU, ZZ_Petty_Cash_Recon_Claim_UU);
	}

	/** Get ZZ_Petty_Cash_Recon_Claim_UU.
		@return ZZ_Petty_Cash_Recon_Claim_UU	  */
	public String getZZ_Petty_Cash_Recon_Claim_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_UU);
	}

	public I_ZZ_Petty_Cash_Recon_Hdr getZZ_Petty_Cash_Recon_Hdr() throws RuntimeException
	{
		return (I_ZZ_Petty_Cash_Recon_Hdr)MTable.get(getCtx(), I_ZZ_Petty_Cash_Recon_Hdr.Table_ID)
			.getPO(getZZ_Petty_Cash_Recon_Hdr_ID(), get_TrxName());
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
}