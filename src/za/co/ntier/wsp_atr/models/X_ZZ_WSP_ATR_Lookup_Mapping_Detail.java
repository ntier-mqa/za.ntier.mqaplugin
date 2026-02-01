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

/** Generated Model for ZZ_WSP_ATR_Lookup_Mapping_Detail
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Lookup_Mapping_Detail")
public class X_ZZ_WSP_ATR_Lookup_Mapping_Detail extends PO implements I_ZZ_WSP_ATR_Lookup_Mapping_Detail, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260201L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Lookup_Mapping_Detail (Properties ctx, int ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, trxName);
      /** if (ZZ_WSP_ATR_Lookup_Mapping_Detail_ID == 0)
        {
			setIgnore_If_Blank (false);
// N
			setIsMandatory (false);
// N
			setZZ_Create_If_Not_Exists (false);
// N
			setZZ_Use_Value (false);
// N
			setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Lookup_Mapping_Detail (Properties ctx, int ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Lookup_Mapping_Detail_ID == 0)
        {
			setIgnore_If_Blank (false);
// N
			setIsMandatory (false);
// N
			setZZ_Create_If_Not_Exists (false);
// N
			setZZ_Use_Value (false);
// N
			setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Lookup_Mapping_Detail (Properties ctx, String ZZ_WSP_ATR_Lookup_Mapping_Detail_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Lookup_Mapping_Detail_UU, trxName);
      /** if (ZZ_WSP_ATR_Lookup_Mapping_Detail_UU == null)
        {
			setIgnore_If_Blank (false);
// N
			setIsMandatory (false);
// N
			setZZ_Create_If_Not_Exists (false);
// N
			setZZ_Use_Value (false);
// N
			setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Lookup_Mapping_Detail (Properties ctx, String ZZ_WSP_ATR_Lookup_Mapping_Detail_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Lookup_Mapping_Detail_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Lookup_Mapping_Detail_UU == null)
        {
			setIgnore_If_Blank (false);
// N
			setIsMandatory (false);
// N
			setZZ_Create_If_Not_Exists (false);
// N
			setZZ_Use_Value (false);
// N
			setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Lookup_Mapping_Detail (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Lookup_Mapping_Detail[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getAD_Column_ID(), get_TrxName());
	}

	/** Set Column.
		@param AD_Column_ID Column in the table
	*/
	public void setAD_Column_ID (int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_Value (COLUMNNAME_AD_Column_ID, null);
		else
			set_Value (COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/** Get Column.
		@return Column in the table
	  */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Column_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table)MTable.get(getCtx(), org.compiere.model.I_AD_Table.Table_ID)
			.getPO(getAD_Table_ID(), get_TrxName());
	}

	/** Set Table.
		@param AD_Table_ID Database Table information
	*/
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Ignore If Blank.
		@param Ignore_If_Blank Ignore If Blank
	*/
	public void setIgnore_If_Blank (boolean Ignore_If_Blank)
	{
		set_Value (COLUMNNAME_Ignore_If_Blank, Boolean.valueOf(Ignore_If_Blank));
	}

	/** Get Ignore If Blank.
		@return Ignore If Blank	  */
	public boolean isIgnore_If_Blank()
	{
		Object oo = get_Value(COLUMNNAME_Ignore_If_Blank);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Mandatory.
		@param IsMandatory Data entry is required in this column
	*/
	public void setIsMandatory (boolean IsMandatory)
	{
		set_Value (COLUMNNAME_IsMandatory, Boolean.valueOf(IsMandatory));
	}

	/** Get Mandatory.
		@return Data entry is required in this column
	  */
	public boolean isMandatory()
	{
		Object oo = get_Value(COLUMNNAME_IsMandatory);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Column Letter.
		@param ZZ_Column_Letter Column Letter
	*/
	public void setZZ_Column_Letter (String ZZ_Column_Letter)
	{
		set_Value (COLUMNNAME_ZZ_Column_Letter, ZZ_Column_Letter);
	}

	/** Get Column Letter.
		@return Column Letter	  */
	public String getZZ_Column_Letter()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Column_Letter);
	}

	/** Set Create If Not Exists.
		@param ZZ_Create_If_Not_Exists Create If Not Exists
	*/
	public void setZZ_Create_If_Not_Exists (boolean ZZ_Create_If_Not_Exists)
	{
		set_Value (COLUMNNAME_ZZ_Create_If_Not_Exists, Boolean.valueOf(ZZ_Create_If_Not_Exists));
	}

	/** Get Create If Not Exists.
		@return Create If Not Exists	  */
	public boolean isZZ_Create_If_Not_Exists()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Create_If_Not_Exists);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Header Name.
		@param ZZ_Header_Name Header Name
	*/
	public void setZZ_Header_Name (String ZZ_Header_Name)
	{
		set_Value (COLUMNNAME_ZZ_Header_Name, ZZ_Header_Name);
	}

	/** Get Header Name.
		@return Header Name	  */
	public String getZZ_Header_Name()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Header_Name);
	}

	/** Set Name Column Letter.
		@param ZZ_Name_Column_Letter Name Column Letter
	*/
	public void setZZ_Name_Column_Letter (String ZZ_Name_Column_Letter)
	{
		set_Value (COLUMNNAME_ZZ_Name_Column_Letter, ZZ_Name_Column_Letter);
	}

	/** Get Name Column Letter.
		@return Name Column Letter	  */
	public String getZZ_Name_Column_Letter()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Name_Column_Letter);
	}

	/** Set Row No.
		@param ZZ_Row_No Row No
	*/
	public void setZZ_Row_No (int ZZ_Row_No)
	{
		set_Value (COLUMNNAME_ZZ_Row_No, Integer.valueOf(ZZ_Row_No));
	}

	/** Get Row No.
		@return Row No	  */
	public int getZZ_Row_No()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Row_No);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Use Value for Validation.
		@param ZZ_Use_Value Use Value for Validation
	*/
	public void setZZ_Use_Value (boolean ZZ_Use_Value)
	{
		set_Value (COLUMNNAME_ZZ_Use_Value, Boolean.valueOf(ZZ_Use_Value));
	}

	/** Get Use Value for Validation.
		@return Use Value for Validation	  */
	public boolean isZZ_Use_Value()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Use_Value);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Value Column Letter.
		@param ZZ_Value_Column_Letter Value Column Letter
	*/
	public void setZZ_Value_Column_Letter (String ZZ_Value_Column_Letter)
	{
		set_Value (COLUMNNAME_ZZ_Value_Column_Letter, ZZ_Value_Column_Letter);
	}

	/** Get Value Column Letter.
		@return Value Column Letter	  */
	public String getZZ_Value_Column_Letter()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Value_Column_Letter);
	}

	/** Set ZZ WSP ATR Lookup Mapping Detail.
		@param ZZ_WSP_ATR_Lookup_Mapping_Detail_ID ZZ WSP ATR Lookup Mapping Detail
	*/
	public void setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (int ZZ_WSP_ATR_Lookup_Mapping_Detail_ID)
	{
		if (ZZ_WSP_ATR_Lookup_Mapping_Detail_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_ID, Integer.valueOf(ZZ_WSP_ATR_Lookup_Mapping_Detail_ID));
	}

	/** Get ZZ WSP ATR Lookup Mapping Detail.
		@return ZZ WSP ATR Lookup Mapping Detail	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_Detail_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Lookup_Mapping_Detail_UU.
		@param ZZ_WSP_ATR_Lookup_Mapping_Detail_UU ZZ_WSP_ATR_Lookup_Mapping_Detail_UU
	*/
	public void setZZ_WSP_ATR_Lookup_Mapping_Detail_UU (String ZZ_WSP_ATR_Lookup_Mapping_Detail_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_UU, ZZ_WSP_ATR_Lookup_Mapping_Detail_UU);
	}

	/** Get ZZ_WSP_ATR_Lookup_Mapping_Detail_UU.
		@return ZZ_WSP_ATR_Lookup_Mapping_Detail_UU	  */
	public String getZZ_WSP_ATR_Lookup_Mapping_Detail_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_UU);
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