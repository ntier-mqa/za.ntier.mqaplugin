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
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_WSP_ATR_Finance
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Finance")
public class X_ZZ_WSP_ATR_Finance extends PO implements I_ZZ_WSP_ATR_Finance, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260218L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Finance (Properties ctx, int ZZ_WSP_ATR_Finance_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Finance_ID, trxName);
      /** if (ZZ_WSP_ATR_Finance_ID == 0)
        {
			setZZ_WSP_ATR_Finance_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Finance (Properties ctx, int ZZ_WSP_ATR_Finance_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Finance_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Finance_ID == 0)
        {
			setZZ_WSP_ATR_Finance_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Finance (Properties ctx, String ZZ_WSP_ATR_Finance_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Finance_UU, trxName);
      /** if (ZZ_WSP_ATR_Finance_UU == null)
        {
			setZZ_WSP_ATR_Finance_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Finance (Properties ctx, String ZZ_WSP_ATR_Finance_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Finance_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Finance_UU == null)
        {
			setZZ_WSP_ATR_Finance_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Finance (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Finance[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Row No.
		@param Row_No Row No
	*/
	public void setRow_No (int Row_No)
	{
		set_Value (COLUMNNAME_Row_No, Integer.valueOf(Row_No));
	}

	/** Get Row No.
		@return Row No	  */
	public int getRow_No()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Row_No);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Finance Type.
		@param ZZ_Finance_Type Finance Type
	*/
	public void setZZ_Finance_Type (String ZZ_Finance_Type)
	{
		set_Value (COLUMNNAME_ZZ_Finance_Type, ZZ_Finance_Type);
	}

	/** Get Finance Type.
		@return Finance Type	  */
	public String getZZ_Finance_Type()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Finance_Type);
	}

	/** Set Finance Value.
		@param ZZ_Finance_Value Finance Value
	*/
	public void setZZ_Finance_Value (String ZZ_Finance_Value)
	{
		set_Value (COLUMNNAME_ZZ_Finance_Value, ZZ_Finance_Value);
	}

	/** Get Finance Value.
		@return Finance Value	  */
	public String getZZ_Finance_Value()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Finance_Value);
	}

	/** Set Section.
		@param ZZ_Section Section
	*/
	public void setZZ_Section (String ZZ_Section)
	{
		set_Value (COLUMNNAME_ZZ_Section, ZZ_Section);
	}

	/** Get Section.
		@return Section	  */
	public String getZZ_Section()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Section);
	}

	/** Set WSP ATR Finance.
		@param ZZ_WSP_ATR_Finance_ID WSP ATR Finance
	*/
	public void setZZ_WSP_ATR_Finance_ID (int ZZ_WSP_ATR_Finance_ID)
	{
		if (ZZ_WSP_ATR_Finance_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Finance_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Finance_ID, Integer.valueOf(ZZ_WSP_ATR_Finance_ID));
	}

	/** Get WSP ATR Finance.
		@return WSP ATR Finance	  */
	public int getZZ_WSP_ATR_Finance_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Finance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Finance_UU.
		@param ZZ_WSP_ATR_Finance_UU ZZ_WSP_ATR_Finance_UU
	*/
	public void setZZ_WSP_ATR_Finance_UU (String ZZ_WSP_ATR_Finance_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Finance_UU, ZZ_WSP_ATR_Finance_UU);
	}

	/** Get ZZ_WSP_ATR_Finance_UU.
		@return ZZ_WSP_ATR_Finance_UU	  */
	public String getZZ_WSP_ATR_Finance_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Finance_UU);
	}

	public I_ZZ_WSP_ATR_Submitted getZZ_WSP_ATR_Submitted() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Submitted)MTable.get(getCtx(), I_ZZ_WSP_ATR_Submitted.Table_ID)
			.getPO(getZZ_WSP_ATR_Submitted_ID(), get_TrxName());
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
}