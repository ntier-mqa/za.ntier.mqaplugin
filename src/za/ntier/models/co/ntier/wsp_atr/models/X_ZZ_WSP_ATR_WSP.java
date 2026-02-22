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

/** Generated Model for ZZ_WSP_ATR_WSP
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_WSP")
public class X_ZZ_WSP_ATR_WSP extends PO implements I_ZZ_WSP_ATR_WSP, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260222L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_WSP (Properties ctx, int ZZ_WSP_ATR_WSP_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_WSP_ID, trxName);
      /** if (ZZ_WSP_ATR_WSP_ID == 0)
        {
			setZZ_WSP_ATR_WSP_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_WSP (Properties ctx, int ZZ_WSP_ATR_WSP_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_WSP_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_WSP_ID == 0)
        {
			setZZ_WSP_ATR_WSP_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_WSP (Properties ctx, String ZZ_WSP_ATR_WSP_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_WSP_UU, trxName);
      /** if (ZZ_WSP_ATR_WSP_UU == null)
        {
			setZZ_WSP_ATR_WSP_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_WSP (Properties ctx, String ZZ_WSP_ATR_WSP_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_WSP_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_WSP_UU == null)
        {
			setZZ_WSP_ATR_WSP_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_WSP (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_WSP[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Qualification.
		@param Qualification Qualification
	*/
	public void setQualification (String Qualification)
	{
		set_Value (COLUMNNAME_Qualification, Qualification);
	}

	/** Get Qualification.
		@return Qualification	  */
	public String getQualification()
	{
		return (String)get_Value(COLUMNNAME_Qualification);
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

	/** Set African.
		@param ZZ_African African
	*/
	public void setZZ_African (int ZZ_African)
	{
		set_Value (COLUMNNAME_ZZ_African, Integer.valueOf(ZZ_African));
	}

	/** Get African.
		@return African	  */
	public int getZZ_African()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_African);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Coloured.
		@param ZZ_Coloured Coloured
	*/
	public void setZZ_Coloured (int ZZ_Coloured)
	{
		set_Value (COLUMNNAME_ZZ_Coloured, Integer.valueOf(ZZ_Coloured));
	}

	/** Get Coloured.
		@return Coloured	  */
	public int getZZ_Coloured()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Coloured);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Disabled.
		@param ZZ_Disabled Disabled
	*/
	public void setZZ_Disabled (int ZZ_Disabled)
	{
		set_Value (COLUMNNAME_ZZ_Disabled, Integer.valueOf(ZZ_Disabled));
	}

	/** Get Disabled.
		@return Disabled	  */
	public int getZZ_Disabled()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Disabled);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Female.
		@param ZZ_Female Female
	*/
	public void setZZ_Female (int ZZ_Female)
	{
		set_Value (COLUMNNAME_ZZ_Female, Integer.valueOf(ZZ_Female));
	}

	/** Get Female.
		@return Female	  */
	public int getZZ_Female()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Female);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Indian.
		@param ZZ_Indian Indian
	*/
	public void setZZ_Indian (int ZZ_Indian)
	{
		set_Value (COLUMNNAME_ZZ_Indian, Integer.valueOf(ZZ_Indian));
	}

	/** Get Indian.
		@return Indian	  */
	public int getZZ_Indian()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Indian);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme() throws RuntimeException
	{
		return (I_ZZ_Qualification_Type_Details_Ref)MTable.get(getCtx(), I_ZZ_Qualification_Type_Details_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_ID(), get_TrxName());
	}

	/** Set Learning Programme.
		@param ZZ_Learning_Programme_ID Learning Programme
	*/
	public void setZZ_Learning_Programme_ID (int ZZ_Learning_Programme_ID)
	{
		if (ZZ_Learning_Programme_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_ID, Integer.valueOf(ZZ_Learning_Programme_ID));
	}

	/** Get Learning Programme.
		@return Learning Programme	  */
	public int getZZ_Learning_Programme_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type() throws RuntimeException
	{
		return (I_ZZ_Learning_Programme_Ref)MTable.get(getCtx(), I_ZZ_Learning_Programme_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Type_ID(), get_TrxName());
	}

	/** Set Learning Programme Type.
		@param ZZ_Learning_Programme_Type_ID Learning Programme Type
	*/
	public void setZZ_Learning_Programme_Type_ID (int ZZ_Learning_Programme_Type_ID)
	{
		if (ZZ_Learning_Programme_Type_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_ID, Integer.valueOf(ZZ_Learning_Programme_Type_ID));
	}

	/** Get Learning Programme Type.
		@return Learning Programme Type	  */
	public int getZZ_Learning_Programme_Type_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Male.
		@param ZZ_Male Male
	*/
	public void setZZ_Male (int ZZ_Male)
	{
		set_Value (COLUMNNAME_ZZ_Male, Integer.valueOf(ZZ_Male));
	}

	/** Get Male.
		@return Male	  */
	public int getZZ_Male()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Male);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Occupations_Ref getZZ_OFO_Specialisation() throws RuntimeException
	{
		return (I_ZZ_Occupations_Ref)MTable.get(getCtx(), I_ZZ_Occupations_Ref.Table_ID)
			.getPO(getZZ_OFO_Specialisation_ID(), get_TrxName());
	}

	/** Set Specialisation/Occupation Title.
		@param ZZ_OFO_Specialisation_ID Specialisation/Occupation Title
	*/
	public void setZZ_OFO_Specialisation_ID (int ZZ_OFO_Specialisation_ID)
	{
		if (ZZ_OFO_Specialisation_ID < 1)
			set_Value (COLUMNNAME_ZZ_OFO_Specialisation_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_OFO_Specialisation_ID, Integer.valueOf(ZZ_OFO_Specialisation_ID));
	}

	/** Get Specialisation/Occupation Title.
		@return Specialisation/Occupation Title	  */
	public int getZZ_OFO_Specialisation_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_OFO_Specialisation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Qualification.
		@param ZZ_Qualification Qualification
	*/
	public void setZZ_Qualification (String ZZ_Qualification)
	{
		set_Value (COLUMNNAME_ZZ_Qualification, ZZ_Qualification);
	}

	/** Get Qualification.
		@return Qualification	  */
	public String getZZ_Qualification()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Qualification);
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

	/** Set ZZ WSP ATR WSP.
		@param ZZ_WSP_ATR_WSP_ID ZZ WSP ATR WSP
	*/
	public void setZZ_WSP_ATR_WSP_ID (int ZZ_WSP_ATR_WSP_ID)
	{
		if (ZZ_WSP_ATR_WSP_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_WSP_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_WSP_ID, Integer.valueOf(ZZ_WSP_ATR_WSP_ID));
	}

	/** Get ZZ WSP ATR WSP.
		@return ZZ WSP ATR WSP	  */
	public int getZZ_WSP_ATR_WSP_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_WSP_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_WSP_UU.
		@param ZZ_WSP_ATR_WSP_UU ZZ_WSP_ATR_WSP_UU
	*/
	public void setZZ_WSP_ATR_WSP_UU (String ZZ_WSP_ATR_WSP_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_WSP_UU, ZZ_WSP_ATR_WSP_UU);
	}

	/** Get ZZ_WSP_ATR_WSP_UU.
		@return ZZ_WSP_ATR_WSP_UU	  */
	public String getZZ_WSP_ATR_WSP_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_WSP_UU);
	}

	/** Set White.
		@param ZZ_White White
	*/
	public void setZZ_White (int ZZ_White)
	{
		set_Value (COLUMNNAME_ZZ_White, Integer.valueOf(ZZ_White));
	}

	/** Get White.
		@return White	  */
	public int getZZ_White()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_White);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}