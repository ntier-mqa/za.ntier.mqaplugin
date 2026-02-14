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

/** Generated Model for ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep")
public class X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep extends PO implements I_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (Properties ctx, int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (Properties ctx, String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU == null)
        {
			setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Disabled Planned.
		@param ZZ_Disabled_Planned Disabled Planned
	*/
	public void setZZ_Disabled_Planned (int ZZ_Disabled_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Disabled_Planned, Integer.valueOf(ZZ_Disabled_Planned));
	}

	/** Get Disabled Planned.
		@return Disabled Planned	  */
	public int getZZ_Disabled_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Disabled_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme_Planned() throws RuntimeException
	{
		return (I_ZZ_Qualification_Type_Details_Ref)MTable.get(getCtx(), I_ZZ_Qualification_Type_Details_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Planned_ID(), get_TrxName());
	}

	/** Set Learning Programme Planned.
		@param ZZ_Learning_Programme_Planned_ID Learning Programme Planned
	*/
	public void setZZ_Learning_Programme_Planned_ID (int ZZ_Learning_Programme_Planned_ID)
	{
		if (ZZ_Learning_Programme_Planned_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Planned_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Planned_ID, Integer.valueOf(ZZ_Learning_Programme_Planned_ID));
	}

	/** Get Learning Programme Planned.
		@return Learning Programme Planned	  */
	public int getZZ_Learning_Programme_Planned_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Planned_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type_Planned() throws RuntimeException
	{
		return (I_ZZ_Learning_Programme_Ref)MTable.get(getCtx(), I_ZZ_Learning_Programme_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Type_Planned_ID(), get_TrxName());
	}

	/** Set Learning Programme Type Planned.
		@param ZZ_Learning_Programme_Type_Planned_ID Learning Programme Type Planned
	*/
	public void setZZ_Learning_Programme_Type_Planned_ID (int ZZ_Learning_Programme_Type_Planned_ID)
	{
		if (ZZ_Learning_Programme_Type_Planned_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_Planned_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_Planned_ID, Integer.valueOf(ZZ_Learning_Programme_Type_Planned_ID));
	}

	/** Get Learning Programme Type Planned.
		@return Learning Programme Type Planned	  */
	public int getZZ_Learning_Programme_Type_Planned_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Type_Planned_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Target_Beneficiary_Ref getZZ_Target_Ben_Planned() throws RuntimeException
	{
		return (I_ZZ_Target_Beneficiary_Ref)MTable.get(getCtx(), I_ZZ_Target_Beneficiary_Ref.Table_ID)
			.getPO(getZZ_Target_Ben_Planned_ID(), get_TrxName());
	}

	/** Set Target Ben Planned.
		@param ZZ_Target_Ben_Planned_ID Target Ben Planned
	*/
	public void setZZ_Target_Ben_Planned_ID (int ZZ_Target_Ben_Planned_ID)
	{
		if (ZZ_Target_Ben_Planned_ID < 1)
			set_Value (COLUMNNAME_ZZ_Target_Ben_Planned_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Target_Ben_Planned_ID, Integer.valueOf(ZZ_Target_Ben_Planned_ID));
	}

	/** Get Target Ben Planned.
		@return Target Ben Planned	  */
	public int getZZ_Target_Ben_Planned_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Target_Ben_Planned_ID);
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

	/** Set Skills Development  Planned.
		@param ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID Skills Development  Planned
	*/
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID)
	{
		if (ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID));
	}

	/** Get Skills Development  Planned.
		@return Skills Development  Planned	  */
	public int getZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU.
		@param ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU
	*/
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU (String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU, ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU.
		@return ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU	  */
	public String getZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU);
	}

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Report)MTable.get(getCtx(), I_ZZ_WSP_ATR_Report.Table_ID)
			.getPO(getZZ_WSP_ATR_Report_ID(), get_TrxName());
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
}