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

/** Generated Model for ZZ_WSP_ATR_Approvals
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Approvals")
public class X_ZZ_WSP_ATR_Approvals extends PO implements I_ZZ_WSP_ATR_Approvals, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260227L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Approvals (Properties ctx, int ZZ_WSP_ATR_Approvals_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Approvals_ID, trxName);
      /** if (ZZ_WSP_ATR_Approvals_ID == 0)
        {
			setZZ_WSP_ATR_Approvals_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Approvals (Properties ctx, int ZZ_WSP_ATR_Approvals_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Approvals_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Approvals_ID == 0)
        {
			setZZ_WSP_ATR_Approvals_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Approvals (Properties ctx, String ZZ_WSP_ATR_Approvals_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Approvals_UU, trxName);
      /** if (ZZ_WSP_ATR_Approvals_UU == null)
        {
			setZZ_WSP_ATR_Approvals_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Approvals (Properties ctx, String ZZ_WSP_ATR_Approvals_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Approvals_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Approvals_UU == null)
        {
			setZZ_WSP_ATR_Approvals_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Approvals (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Approvals[")
        .append(get_ID()).append("]");
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

	/** Set Processed On.
		@param ProcessedOn The date+time (expressed in decimal format) when the document has been processed
	*/
	public void setProcessedOn (Timestamp ProcessedOn)
	{
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);
	}

	/** Get Processed On.
		@return The date+time (expressed in decimal format) when the document has been processed
	  */
	public Timestamp getProcessedOn()
	{
		return (Timestamp)get_Value(COLUMNNAME_ProcessedOn);
	}

	/** Set Financial Year.
		@param ZZ_Financial_Year Financial Year
	*/
	public void setZZ_Financial_Year (String ZZ_Financial_Year)
	{
		set_Value (COLUMNNAME_ZZ_Financial_Year, ZZ_Financial_Year);
	}

	/** Get Financial Year.
		@return Financial Year	  */
	public String getZZ_Financial_Year()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Financial_Year);
	}

	/** Approved = A */
	public static final String ZZ_GRANT_STATUS_Approved = "A";
	/** Rejected = R */
	public static final String ZZ_GRANT_STATUS_Rejected = "R";
	/** Set Grant Status.
		@param ZZ_Grant_Status Grant Status
	*/
	public void setZZ_Grant_Status (String ZZ_Grant_Status)
	{

		set_Value (COLUMNNAME_ZZ_Grant_Status, ZZ_Grant_Status);
	}

	/** Get Grant Status.
		@return Grant Status	  */
	public String getZZ_Grant_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Grant_Status);
	}

	/** Set WSP ATR Approvals.
		@param ZZ_WSP_ATR_Approvals_ID WSP ATR Approvals
	*/
	public void setZZ_WSP_ATR_Approvals_ID (int ZZ_WSP_ATR_Approvals_ID)
	{
		if (ZZ_WSP_ATR_Approvals_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Approvals_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Approvals_ID, Integer.valueOf(ZZ_WSP_ATR_Approvals_ID));
	}

	/** Get WSP ATR Approvals.
		@return WSP ATR Approvals	  */
	public int getZZ_WSP_ATR_Approvals_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Approvals_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Approvals_UU.
		@param ZZ_WSP_ATR_Approvals_UU ZZ_WSP_ATR_Approvals_UU
	*/
	public void setZZ_WSP_ATR_Approvals_UU (String ZZ_WSP_ATR_Approvals_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Approvals_UU, ZZ_WSP_ATR_Approvals_UU);
	}

	/** Get ZZ_WSP_ATR_Approvals_UU.
		@return ZZ_WSP_ATR_Approvals_UU	  */
	public String getZZ_WSP_ATR_Approvals_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Approvals_UU);
	}
}