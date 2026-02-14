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

/** Generated Model for ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep")
public class X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep extends PO implements I_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Disabled Done.
		@param ZZ_Disabled_Done Disabled Done
	*/
	public void setZZ_Disabled_Done (int ZZ_Disabled_Done)
	{
		set_Value (COLUMNNAME_ZZ_Disabled_Done, Integer.valueOf(ZZ_Disabled_Done));
	}

	/** Get Disabled Done.
		@return Disabled Done	  */
	public int getZZ_Disabled_Done()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Disabled_Done);
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

	/** Set LP Other Done.
		@param ZZ_LP_Other_Done LP Other Done
	*/
	public void setZZ_LP_Other_Done (String ZZ_LP_Other_Done)
	{
		set_Value (COLUMNNAME_ZZ_LP_Other_Done, ZZ_LP_Other_Done);
	}

	/** Get LP Other Done.
		@return LP Other Done	  */
	public String getZZ_LP_Other_Done()
	{
		return (String)get_Value(COLUMNNAME_ZZ_LP_Other_Done);
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

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type_Done() throws RuntimeException
	{
		return (I_ZZ_Learning_Programme_Ref)MTable.get(getCtx(), I_ZZ_Learning_Programme_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Type_Done_ID(), get_TrxName());
	}

	/** Set Learning Programme Type Done.
		@param ZZ_Learning_Programme_Type_Done_ID Learning Programme Type Done
	*/
	public void setZZ_Learning_Programme_Type_Done_ID (int ZZ_Learning_Programme_Type_Done_ID)
	{
		if (ZZ_Learning_Programme_Type_Done_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_Done_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_Done_ID, Integer.valueOf(ZZ_Learning_Programme_Type_Done_ID));
	}

	/** Get Learning Programme Type Done.
		@return Learning Programme Type Done	  */
	public int getZZ_Learning_Programme_Type_Done_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Type_Done_ID);
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

	public I_ZZ_Target_Beneficiary_Ref getZZ_Target_Ben_Done() throws RuntimeException
	{
		return (I_ZZ_Target_Beneficiary_Ref)MTable.get(getCtx(), I_ZZ_Target_Beneficiary_Ref.Table_ID)
			.getPO(getZZ_Target_Ben_Done_ID(), get_TrxName());
	}

	/** Set Target Ben Done.
		@param ZZ_Target_Ben_Done_ID Target Ben Done
	*/
	public void setZZ_Target_Ben_Done_ID (int ZZ_Target_Ben_Done_ID)
	{
		if (ZZ_Target_Ben_Done_ID < 1)
			set_Value (COLUMNNAME_ZZ_Target_Ben_Done_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Target_Ben_Done_ID, Integer.valueOf(ZZ_Target_Ben_Done_ID));
	}

	/** Get Target Ben Done.
		@return Target Ben Done	  */
	public int getZZ_Target_Ben_Done_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Target_Ben_Done_ID);
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

	/** Set Non-Employees Skills Development Related Done.
		@param ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID Non-Employees Skills Development Related Done
	*/
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID (int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID)
	{
		if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID));
	}

	/** Get Non-Employees Skills Development Related Done.
		@return Non-Employees Skills Development Related Done	  */
	public int getZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU.
		@param ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU
	*/
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU (String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU.
		@return ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU	  */
	public String getZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep_UU);
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