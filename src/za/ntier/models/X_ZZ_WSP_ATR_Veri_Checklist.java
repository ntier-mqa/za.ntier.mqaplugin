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

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_WSP_ATR_Veri_Checklist
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Veri_Checklist")
public class X_ZZ_WSP_ATR_Veri_Checklist extends PO implements I_ZZ_WSP_ATR_Veri_Checklist, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260204L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Veri_Checklist (Properties ctx, int ZZ_WSP_ATR_Veri_Checklist_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Veri_Checklist_ID, trxName);
      /** if (ZZ_WSP_ATR_Veri_Checklist_ID == 0)
        {
			setName (null);
			setZZ_Information_Completed (false);
// N
			setZZ_WSP_ATR_Veri_Checklist_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Veri_Checklist (Properties ctx, int ZZ_WSP_ATR_Veri_Checklist_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Veri_Checklist_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Veri_Checklist_ID == 0)
        {
			setName (null);
			setZZ_Information_Completed (false);
// N
			setZZ_WSP_ATR_Veri_Checklist_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Veri_Checklist (Properties ctx, String ZZ_WSP_ATR_Veri_Checklist_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Veri_Checklist_UU, trxName);
      /** if (ZZ_WSP_ATR_Veri_Checklist_UU == null)
        {
			setName (null);
			setZZ_Information_Completed (false);
// N
			setZZ_WSP_ATR_Veri_Checklist_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Veri_Checklist (Properties ctx, String ZZ_WSP_ATR_Veri_Checklist_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Veri_Checklist_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Veri_Checklist_UU == null)
        {
			setName (null);
			setZZ_Information_Completed (false);
// N
			setZZ_WSP_ATR_Veri_Checklist_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Veri_Checklist (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Veri_Checklist[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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

	/** Set Comment/Help.
		@param Help Comment or Hint
	*/
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment/Help.
		@return Comment or Hint
	  */
	public String getHelp()
	{
		return (String)get_Value(COLUMNNAME_Help);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}

	/** Set Information Completed.
		@param ZZ_Information_Completed Information Completed
	*/
	public void setZZ_Information_Completed (boolean ZZ_Information_Completed)
	{
		set_Value (COLUMNNAME_ZZ_Information_Completed, Boolean.valueOf(ZZ_Information_Completed));
	}

	/** Get Information Completed.
		@return Information Completed	  */
	public boolean isZZ_Information_Completed()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Information_Completed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set WSP ATR Veri Checklist.
		@param ZZ_WSP_ATR_Veri_Checklist_ID WSP ATR Veri Checklist
	*/
	public void setZZ_WSP_ATR_Veri_Checklist_ID (int ZZ_WSP_ATR_Veri_Checklist_ID)
	{
		if (ZZ_WSP_ATR_Veri_Checklist_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Veri_Checklist_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Veri_Checklist_ID, Integer.valueOf(ZZ_WSP_ATR_Veri_Checklist_ID));
	}

	/** Get WSP ATR Veri Checklist.
		@return WSP ATR Veri Checklist	  */
	public int getZZ_WSP_ATR_Veri_Checklist_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Veri_Checklist_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Veri_Checklist_UU.
		@param ZZ_WSP_ATR_Veri_Checklist_UU ZZ_WSP_ATR_Veri_Checklist_UU
	*/
	public void setZZ_WSP_ATR_Veri_Checklist_UU (String ZZ_WSP_ATR_Veri_Checklist_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Veri_Checklist_UU, ZZ_WSP_ATR_Veri_Checklist_UU);
	}

	/** Get ZZ_WSP_ATR_Veri_Checklist_UU.
		@return ZZ_WSP_ATR_Veri_Checklist_UU	  */
	public String getZZ_WSP_ATR_Veri_Checklist_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Veri_Checklist_UU);
	}

	public I_ZZ_WSP_ATR_Checklist_Ref getzz_wsp_atr_checklist_ref() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Checklist_Ref)MTable.get(getCtx(), I_ZZ_WSP_ATR_Checklist_Ref.Table_ID)
			.getPO(getzz_wsp_atr_checklist_ref_ID(), get_TrxName());
	}

	/** Set WSP ATR Checklist.
		@param zz_wsp_atr_checklist_ref_ID WSP ATR Checklist
	*/
	public void setzz_wsp_atr_checklist_ref_ID (int zz_wsp_atr_checklist_ref_ID)
	{
		if (zz_wsp_atr_checklist_ref_ID < 1)
			set_ValueNoCheck (COLUMNNAME_zz_wsp_atr_checklist_ref_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_zz_wsp_atr_checklist_ref_ID, Integer.valueOf(zz_wsp_atr_checklist_ref_ID));
	}

	/** Get WSP ATR Checklist.
		@return WSP ATR Checklist	  */
	public int getzz_wsp_atr_checklist_ref_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_zz_wsp_atr_checklist_ref_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}