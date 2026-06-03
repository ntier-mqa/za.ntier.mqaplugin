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

/** Generated Model for ZZ_WSP_ATR_Col_Check
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Col_Check")
public class X_ZZ_WSP_ATR_Col_Check extends PO implements I_ZZ_WSP_ATR_Col_Check, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260603L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Col_Check (Properties ctx, int ZZ_WSP_ATR_Col_Check_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Col_Check_ID, trxName);
      /** if (ZZ_WSP_ATR_Col_Check_ID == 0)
        {
			setZZ_WSP_ATR_Col_Check_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Col_Check (Properties ctx, int ZZ_WSP_ATR_Col_Check_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Col_Check_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Col_Check_ID == 0)
        {
			setZZ_WSP_ATR_Col_Check_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Col_Check (Properties ctx, String ZZ_WSP_ATR_Col_Check_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Col_Check_UU, trxName);
      /** if (ZZ_WSP_ATR_Col_Check_UU == null)
        {
			setZZ_WSP_ATR_Col_Check_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Col_Check (Properties ctx, String ZZ_WSP_ATR_Col_Check_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Col_Check_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Col_Check_UU == null)
        {
			setZZ_WSP_ATR_Col_Check_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Col_Check (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Col_Check[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Check Name.
		@param ZZ_Check_Name Check Name
	*/
	public void setZZ_Check_Name (String ZZ_Check_Name)
	{
		set_Value (COLUMNNAME_ZZ_Check_Name, ZZ_Check_Name);
	}

	/** Get Check Name.
		@return Check Name	  */
	public String getZZ_Check_Name()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Check_Name);
	}

	/** NOT_ZERO = N */
	public static final String ZZ_CHECK_TYPE_NOT_ZERO = "N";
	/** SUM_EQUALS_SUM = S */
	public static final String ZZ_CHECK_TYPE_SUM_EQUALS_SUM = "S";
	/** Set Check Type.
		@param ZZ_Check_Type Check Type
	*/
	public void setZZ_Check_Type (String ZZ_Check_Type)
	{

		set_Value (COLUMNNAME_ZZ_Check_Type, ZZ_Check_Type);
	}

	/** Get Check Type.
		@return Check Type	  */
	public String getZZ_Check_Type()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Check_Type);
	}

	/** Set Col Letters A.
		@param ZZ_Col_Letters_A Comma delimited Column Letters
	*/
	public void setZZ_Col_Letters_A (String ZZ_Col_Letters_A)
	{
		set_Value (COLUMNNAME_ZZ_Col_Letters_A, ZZ_Col_Letters_A);
	}

	/** Get Col Letters A.
		@return Comma delimited Column Letters
	  */
	public String getZZ_Col_Letters_A()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Col_Letters_A);
	}

	/** Set Col Letters B.
		@param ZZ_Col_Letters_B Col Letters B
	*/
	public void setZZ_Col_Letters_B (String ZZ_Col_Letters_B)
	{
		set_Value (COLUMNNAME_ZZ_Col_Letters_B, ZZ_Col_Letters_B);
	}

	/** Get Col Letters B.
		@return Col Letters B	  */
	public String getZZ_Col_Letters_B()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Col_Letters_B);
	}

	/** Set WSP ATR Import Column Check.
		@param ZZ_WSP_ATR_Col_Check_ID WSP ATR Import check column totals (gender total = race totals)
	*/
	public void setZZ_WSP_ATR_Col_Check_ID (int ZZ_WSP_ATR_Col_Check_ID)
	{
		if (ZZ_WSP_ATR_Col_Check_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Col_Check_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Col_Check_ID, Integer.valueOf(ZZ_WSP_ATR_Col_Check_ID));
	}

	/** Get WSP ATR Import Column Check.
		@return WSP ATR Import check column totals (gender total = race totals)
	  */
	public int getZZ_WSP_ATR_Col_Check_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Col_Check_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Col_Check_UU.
		@param ZZ_WSP_ATR_Col_Check_UU ZZ_WSP_ATR_Col_Check_UU
	*/
	public void setZZ_WSP_ATR_Col_Check_UU (String ZZ_WSP_ATR_Col_Check_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Col_Check_UU, ZZ_WSP_ATR_Col_Check_UU);
	}

	/** Get ZZ_WSP_ATR_Col_Check_UU.
		@return ZZ_WSP_ATR_Col_Check_UU	  */
	public String getZZ_WSP_ATR_Col_Check_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Col_Check_UU);
	}

	public I_ZZ_WSP_ATR_Lookup_Mapping getZZ_WSP_ATR_Lookup_Mapping() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Lookup_Mapping)MTable.get(getCtx(), I_ZZ_WSP_ATR_Lookup_Mapping.Table_ID)
			.getPO(getZZ_WSP_ATR_Lookup_Mapping_ID(), get_TrxName());
	}

	/** Set WSP ATR Lookup Mapping.
		@param ZZ_WSP_ATR_Lookup_Mapping_ID WSP ATR Lookup Mapping
	*/
	public void setZZ_WSP_ATR_Lookup_Mapping_ID (int ZZ_WSP_ATR_Lookup_Mapping_ID)
	{
		if (ZZ_WSP_ATR_Lookup_Mapping_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID, Integer.valueOf(ZZ_WSP_ATR_Lookup_Mapping_ID));
	}

	/** Get WSP ATR Lookup Mapping.
		@return WSP ATR Lookup Mapping	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}