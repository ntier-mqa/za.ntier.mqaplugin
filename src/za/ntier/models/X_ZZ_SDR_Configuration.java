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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_SDR_Configuration
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_SDR_Configuration")
public class X_ZZ_SDR_Configuration extends PO implements I_ZZ_SDR_Configuration, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260302L;

    /** Standard Constructor */
    public X_ZZ_SDR_Configuration (Properties ctx, int ZZ_SDR_Configuration_ID, String trxName)
    {
      super (ctx, ZZ_SDR_Configuration_ID, trxName);
      /** if (ZZ_SDR_Configuration_ID == 0)
        {
			setZZ_SDR_Configuration_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Configuration (Properties ctx, int ZZ_SDR_Configuration_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_SDR_Configuration_ID, trxName, virtualColumns);
      /** if (ZZ_SDR_Configuration_ID == 0)
        {
			setZZ_SDR_Configuration_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Configuration (Properties ctx, String ZZ_SDR_Configuration_UU, String trxName)
    {
      super (ctx, ZZ_SDR_Configuration_UU, trxName);
      /** if (ZZ_SDR_Configuration_UU == null)
        {
			setZZ_SDR_Configuration_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_SDR_Configuration (Properties ctx, String ZZ_SDR_Configuration_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_SDR_Configuration_UU, trxName, virtualColumns);
      /** if (ZZ_SDR_Configuration_UU == null)
        {
			setZZ_SDR_Configuration_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_SDR_Configuration (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_SDR_Configuration[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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

	public org.compiere.model.I_C_Year getZZ_FinYear() throws RuntimeException
	{
		return (org.compiere.model.I_C_Year)MTable.get(getCtx(), org.compiere.model.I_C_Year.Table_ID)
			.getPO(getZZ_FinYear_ID(), get_TrxName());
	}

	/** Set Fin Year.
		@param ZZ_FinYear_ID Fin Year
	*/
	public void setZZ_FinYear_ID (int ZZ_FinYear_ID)
	{
		if (ZZ_FinYear_ID < 1)
			set_Value (COLUMNNAME_ZZ_FinYear_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_FinYear_ID, Integer.valueOf(ZZ_FinYear_ID));
	}

	/** Get Fin Year.
		@return Fin Year	  */
	public int getZZ_FinYear_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_FinYear_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set SDR Configuration.
		@param ZZ_SDR_Configuration_ID SDR Configuration
	*/
	public void setZZ_SDR_Configuration_ID (int ZZ_SDR_Configuration_ID)
	{
		if (ZZ_SDR_Configuration_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_SDR_Configuration_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_SDR_Configuration_ID, Integer.valueOf(ZZ_SDR_Configuration_ID));
	}

	/** Get SDR Configuration.
		@return SDR Configuration	  */
	public int getZZ_SDR_Configuration_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_SDR_Configuration_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_SDR_Configuration_UU.
		@param ZZ_SDR_Configuration_UU ZZ_SDR_Configuration_UU
	*/
	public void setZZ_SDR_Configuration_UU (String ZZ_SDR_Configuration_UU)
	{
		set_Value (COLUMNNAME_ZZ_SDR_Configuration_UU, ZZ_SDR_Configuration_UU);
	}

	/** Get ZZ_SDR_Configuration_UU.
		@return ZZ_SDR_Configuration_UU	  */
	public String getZZ_SDR_Configuration_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_SDR_Configuration_UU);
	}

	/** Set WSP-ATR Extension End Date.
		@param ZZ_WSP_ATR_Ext_End_Date WSP-ATR Extension End Date
	*/
	public void setZZ_WSP_ATR_Ext_End_Date (Timestamp ZZ_WSP_ATR_Ext_End_Date)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Ext_End_Date, ZZ_WSP_ATR_Ext_End_Date);
	}

	/** Get WSP-ATR Extension End Date.
		@return WSP-ATR Extension End Date	  */
	public Timestamp getZZ_WSP_ATR_Ext_End_Date()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_WSP_ATR_Ext_End_Date);
	}

	/** Set WSP-ATR Extension Start Date.
		@param ZZ_WSP_ATR_Ext_Start_Date WSP-ATR Extension Start Date
	*/
	public void setZZ_WSP_ATR_Ext_Start_Date (Timestamp ZZ_WSP_ATR_Ext_Start_Date)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Ext_Start_Date, ZZ_WSP_ATR_Ext_Start_Date);
	}

	/** Get WSP-ATR Extension Start Date.
		@return WSP-ATR Extension Start Date	  */
	public Timestamp getZZ_WSP_ATR_Ext_Start_Date()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_WSP_ATR_Ext_Start_Date);
	}

	/** Set WSP-ATR Submission End Date.
		@param ZZ_WSP_ATR_Sub_End_Date WSP-ATR Submission End Date
	*/
	public void setZZ_WSP_ATR_Sub_End_Date (Timestamp ZZ_WSP_ATR_Sub_End_Date)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Sub_End_Date, ZZ_WSP_ATR_Sub_End_Date);
	}

	/** Get WSP-ATR Submission End Date.
		@return WSP-ATR Submission End Date	  */
	public Timestamp getZZ_WSP_ATR_Sub_End_Date()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_WSP_ATR_Sub_End_Date);
	}

	/** Set WSP-ATR Submission Start Date.
		@param ZZ_WSP_ATR_Sub_Start_Date WSP-ATR Submission Start Date
	*/
	public void setZZ_WSP_ATR_Sub_Start_Date (Timestamp ZZ_WSP_ATR_Sub_Start_Date)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Sub_Start_Date, ZZ_WSP_ATR_Sub_Start_Date);
	}

	/** Get WSP-ATR Submission Start Date.
		@return WSP-ATR Submission Start Date	  */
	public Timestamp getZZ_WSP_ATR_Sub_Start_Date()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_WSP_ATR_Sub_Start_Date);
	}
}