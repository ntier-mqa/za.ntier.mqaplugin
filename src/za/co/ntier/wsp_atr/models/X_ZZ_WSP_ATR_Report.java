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

/** Generated Model for ZZ_WSP_ATR_Report
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Report")
public class X_ZZ_WSP_ATR_Report extends PO implements I_ZZ_WSP_ATR_Report, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260222L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Report (Properties ctx, int ZZ_WSP_ATR_Report_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Report_ID, trxName);
      /** if (ZZ_WSP_ATR_Report_ID == 0)
        {
			setName (null);
			setZZ_WSP_ATR_Report_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Report (Properties ctx, int ZZ_WSP_ATR_Report_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Report_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Report_ID == 0)
        {
			setName (null);
			setZZ_WSP_ATR_Report_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Report (Properties ctx, String ZZ_WSP_ATR_Report_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Report_UU, trxName);
      /** if (ZZ_WSP_ATR_Report_UU == null)
        {
			setName (null);
			setZZ_WSP_ATR_Report_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Report (Properties ctx, String ZZ_WSP_ATR_Report_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Report_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Report_UU == null)
        {
			setName (null);
			setZZ_WSP_ATR_Report_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Report (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Report[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Date Printed.
		@param DatePrinted Date the document was printed.
	*/
	public void setDatePrinted (Timestamp DatePrinted)
	{
		set_ValueNoCheck (COLUMNNAME_DatePrinted, DatePrinted);
	}

	/** Get Date Printed.
		@return Date the document was printed.
	  */
	public Timestamp getDatePrinted()
	{
		return (Timestamp)get_Value(COLUMNNAME_DatePrinted);
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

	/** Set Generate WSP ATR Report.
		@param ZZ_Generate_WSP_ATR_Report Generate WSP ATR Report
	*/
	public void setZZ_Generate_WSP_ATR_Report (String ZZ_Generate_WSP_ATR_Report)
	{
		set_Value (COLUMNNAME_ZZ_Generate_WSP_ATR_Report, ZZ_Generate_WSP_ATR_Report);
	}

	/** Get Generate WSP ATR Report.
		@return Generate WSP ATR Report	  */
	public String getZZ_Generate_WSP_ATR_Report()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Generate_WSP_ATR_Report);
	}

	/** Set WSP ATR Report .
		@param ZZ_WSP_ATR_Report_ID WSP ATR Report 
	*/
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID)
	{
		if (ZZ_WSP_ATR_Report_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Report_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Report_ID, Integer.valueOf(ZZ_WSP_ATR_Report_ID));
	}

	/** Get WSP ATR Report .
		@return WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Report_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Report_UU.
		@param ZZ_WSP_ATR_Report_UU ZZ_WSP_ATR_Report_UU
	*/
	public void setZZ_WSP_ATR_Report_UU (String ZZ_WSP_ATR_Report_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Report_UU, ZZ_WSP_ATR_Report_UU);
	}

	/** Get ZZ_WSP_ATR_Report_UU.
		@return ZZ_WSP_ATR_Report_UU	  */
	public String getZZ_WSP_ATR_Report_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Report_UU);
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