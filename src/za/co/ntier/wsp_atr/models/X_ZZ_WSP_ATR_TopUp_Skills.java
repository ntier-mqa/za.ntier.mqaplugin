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

/** Generated Model for ZZ_WSP_ATR_TopUp_Skills
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_TopUp_Skills")
public class X_ZZ_WSP_ATR_TopUp_Skills extends PO implements I_ZZ_WSP_ATR_TopUp_Skills, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260617L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_TopUp_Skills (Properties ctx, int ZZ_WSP_ATR_TopUp_Skills_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_TopUp_Skills_ID, trxName);
      /** if (ZZ_WSP_ATR_TopUp_Skills_ID == 0)
        {
			setZZ_WSP_ATR_TopUp_Skills_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_TopUp_Skills (Properties ctx, int ZZ_WSP_ATR_TopUp_Skills_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_TopUp_Skills_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_TopUp_Skills_ID == 0)
        {
			setZZ_WSP_ATR_TopUp_Skills_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_TopUp_Skills (Properties ctx, String ZZ_WSP_ATR_TopUp_Skills_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_TopUp_Skills_UU, trxName);
      /** if (ZZ_WSP_ATR_TopUp_Skills_UU == null)
        {
			setZZ_WSP_ATR_TopUp_Skills_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_TopUp_Skills (Properties ctx, String ZZ_WSP_ATR_TopUp_Skills_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_TopUp_Skills_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_TopUp_Skills_UU == null)
        {
			setZZ_WSP_ATR_TopUp_Skills_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_TopUp_Skills (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_TopUp_Skills[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Comments.
		@param Comments Comments or additional information
	*/
	public void setComments (String Comments)
	{
		set_Value (COLUMNNAME_Comments, Comments);
	}

	/** Get Comments.
		@return Comments or additional information
	  */
	public String getComments()
	{
		return (String)get_Value(COLUMNNAME_Comments);
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

	public I_ZZ_Specializations_Ref getZZ_Specializations() throws RuntimeException
	{
		return (I_ZZ_Specializations_Ref)MTable.get(getCtx(), I_ZZ_Specializations_Ref.Table_ID)
			.getPO(getZZ_Specializations_ID(), get_TrxName());
	}

	/** Set Specialization.
		@param ZZ_Specializations_ID Specialization
	*/
	public void setZZ_Specializations_ID (int ZZ_Specializations_ID)
	{
		if (ZZ_Specializations_ID < 1)
			set_Value (COLUMNNAME_ZZ_Specializations_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Specializations_ID, Integer.valueOf(ZZ_Specializations_ID));
	}

	/** Get Specialization.
		@return Specialization	  */
	public int getZZ_Specializations_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Specializations_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Topup_Skills_Ref getZZ_TopUpSkill() throws RuntimeException
	{
		return (I_ZZ_Topup_Skills_Ref)MTable.get(getCtx(), I_ZZ_Topup_Skills_Ref.Table_ID)
			.getPO(getZZ_TopUpSkill_ID(), get_TrxName());
	}

	/** Set Top-up Skills.
		@param ZZ_TopUpSkill_ID Top-up Skills
	*/
	public void setZZ_TopUpSkill_ID (int ZZ_TopUpSkill_ID)
	{
		if (ZZ_TopUpSkill_ID < 1)
			set_Value (COLUMNNAME_ZZ_TopUpSkill_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_TopUpSkill_ID, Integer.valueOf(ZZ_TopUpSkill_ID));
	}

	/** Get Top-up Skills.
		@return Top-up Skills	  */
	public int getZZ_TopUpSkill_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_TopUpSkill_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set WSP ATR  TopUp Skills.
		@param ZZ_WSP_ATR_TopUp_Skills_ID WSP ATR  TopUp Skills
	*/
	public void setZZ_WSP_ATR_TopUp_Skills_ID (int ZZ_WSP_ATR_TopUp_Skills_ID)
	{
		if (ZZ_WSP_ATR_TopUp_Skills_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_TopUp_Skills_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_TopUp_Skills_ID, Integer.valueOf(ZZ_WSP_ATR_TopUp_Skills_ID));
	}

	/** Get WSP ATR  TopUp Skills.
		@return WSP ATR  TopUp Skills	  */
	public int getZZ_WSP_ATR_TopUp_Skills_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_TopUp_Skills_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_TopUp_Skills_UU.
		@param ZZ_WSP_ATR_TopUp_Skills_UU ZZ_WSP_ATR_TopUp_Skills_UU
	*/
	public void setZZ_WSP_ATR_TopUp_Skills_UU (String ZZ_WSP_ATR_TopUp_Skills_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_TopUp_Skills_UU, ZZ_WSP_ATR_TopUp_Skills_UU);
	}

	/** Get ZZ_WSP_ATR_TopUp_Skills_UU.
		@return ZZ_WSP_ATR_TopUp_Skills_UU	  */
	public String getZZ_WSP_ATR_TopUp_Skills_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_TopUp_Skills_UU);
	}
}