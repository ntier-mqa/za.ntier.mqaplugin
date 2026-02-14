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

/** Generated Model for ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep")
public class X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep extends PO implements I_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep[")
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

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme_Done() throws RuntimeException
	{
		return (I_ZZ_Qualification_Type_Details_Ref)MTable.get(getCtx(), I_ZZ_Qualification_Type_Details_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Done_ID(), get_TrxName());
	}

	/** Set Learning Programme Done.
		@param ZZ_Learning_Programme_Done_ID Learning Programme Done
	*/
	public void setZZ_Learning_Programme_Done_ID (int ZZ_Learning_Programme_Done_ID)
	{
		if (ZZ_Learning_Programme_Done_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Done_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Done_ID, Integer.valueOf(ZZ_Learning_Programme_Done_ID));
	}

	/** Get Learning Programme Done.
		@return Learning Programme Done	  */
	public int getZZ_Learning_Programme_Done_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Done_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_WSP_Non_Employee_Status_Ref getZZ_Non_Emp_Status_Done() throws RuntimeException
	{
		return (I_ZZ_WSP_Non_Employee_Status_Ref)MTable.get(getCtx(), I_ZZ_WSP_Non_Employee_Status_Ref.Table_ID)
			.getPO(getZZ_Non_Emp_Status_Done_ID(), get_TrxName());
	}

	/** Set Non Emp Status Done.
		@param ZZ_Non_Emp_Status_Done_ID Non Emp Status Done
	*/
	public void setZZ_Non_Emp_Status_Done_ID (int ZZ_Non_Emp_Status_Done_ID)
	{
		if (ZZ_Non_Emp_Status_Done_ID < 1)
			set_Value (COLUMNNAME_ZZ_Non_Emp_Status_Done_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Non_Emp_Status_Done_ID, Integer.valueOf(ZZ_Non_Emp_Status_Done_ID));
	}

	/** Get Non Emp Status Done.
		@return Non Emp Status Done	  */
	public int getZZ_Non_Emp_Status_Done_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Non_Emp_Status_Done_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Total Done.
		@param ZZ_Total_Done Total Done
	*/
	public void setZZ_Total_Done (int ZZ_Total_Done)
	{
		set_Value (COLUMNNAME_ZZ_Total_Done, Integer.valueOf(ZZ_Total_Done));
	}

	/** Get Total Done.
		@return Total Done	  */
	public int getZZ_Total_Done()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Total_Done);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Total Planned.
		@param ZZ_Total_Planned Total Planned
	*/
	public void setZZ_Total_Planned (int ZZ_Total_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Total_Planned, Integer.valueOf(ZZ_Total_Planned));
	}

	/** Get Total Planned.
		@return Total Planned	  */
	public int getZZ_Total_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Total_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Non-Employees AET and Bursaries.
		@param ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID Non-Employees AET and Bursaries
	*/
	public void setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID (int ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID)
	{
		if (ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID));
	}

	/** Get Non-Employees AET and Bursaries.
		@return Non-Employees AET and Bursaries	  */
	public int getZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU.
		@param ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU
	*/
	public void setZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU (String ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU, ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU.
		@return ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU	  */
	public String getZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_AET_Burs_Rep_UU);
	}
}