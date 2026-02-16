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

/** Generated Model for ZZ_WSP_ATR_Term_Emp_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Term_Emp_Rep")
public class X_ZZ_WSP_ATR_Term_Emp_Rep extends PO implements I_ZZ_WSP_ATR_Term_Emp_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260216L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Term_Emp_Rep (Properties ctx, int ZZ_WSP_ATR_Term_Emp_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Term_Emp_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Term_Emp_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Term_Emp_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Term_Emp_Rep (Properties ctx, int ZZ_WSP_ATR_Term_Emp_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Term_Emp_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Term_Emp_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Term_Emp_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Term_Emp_Rep (Properties ctx, String ZZ_WSP_ATR_Term_Emp_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Term_Emp_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Term_Emp_Rep_UU == null)
        {
			setZZ_WSP_ATR_Term_Emp_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Term_Emp_Rep (Properties ctx, String ZZ_WSP_ATR_Term_Emp_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Term_Emp_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Term_Emp_Rep_UU == null)
        {
			setZZ_WSP_ATR_Term_Emp_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Term_Emp_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Term_Emp_Rep[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_ZZ_Appointment_Ref getEmployment_Status() throws RuntimeException
	{
		return (I_ZZ_Appointment_Ref)MTable.get(getCtx(), I_ZZ_Appointment_Ref.Table_ID)
			.getPO(getEmployment_Status_ID(), get_TrxName());
	}

	/** Set Employment Status.
		@param Employment_Status_ID Employment Status
	*/
	public void setEmployment_Status_ID (int Employment_Status_ID)
	{
		if (Employment_Status_ID < 1)
			set_Value (COLUMNNAME_Employment_Status_ID, null);
		else
			set_Value (COLUMNNAME_Employment_Status_ID, Integer.valueOf(Employment_Status_ID));
	}

	/** Get Employment Status.
		@return Employment Status	  */
	public int getEmployment_Status_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Employment_Status_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set African Female Count.
		@param ZZ_African_Female_Cnt African Female Count
	*/
	public void setZZ_African_Female_Cnt (int ZZ_African_Female_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_African_Female_Cnt, Integer.valueOf(ZZ_African_Female_Cnt));
	}

	/** Get African Female Count.
		@return African Female Count
	  */
	public int getZZ_African_Female_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_African_Female_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set African Male Count.
		@param ZZ_African_Male_Cnt African Male Count
	*/
	public void setZZ_African_Male_Cnt (int ZZ_African_Male_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_African_Male_Cnt, Integer.valueOf(ZZ_African_Male_Cnt));
	}

	/** Get African Male Count.
		@return African Male Count
	  */
	public int getZZ_African_Male_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_African_Male_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Age 35-55 Count.
		@param ZZ_Age_35_55_Cnt Age 35-55 Count
	*/
	public void setZZ_Age_35_55_Cnt (int ZZ_Age_35_55_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Age_35_55_Cnt, Integer.valueOf(ZZ_Age_35_55_Cnt));
	}

	/** Get Age 35-55 Count.
		@return Age 35-55 Count
	  */
	public int getZZ_Age_35_55_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Age_35_55_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Age &gt;55 Count.
		@param ZZ_Age_O55_Cnt Age &gt;55 Count
	*/
	public void setZZ_Age_O55_Cnt (int ZZ_Age_O55_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Age_O55_Cnt, Integer.valueOf(ZZ_Age_O55_Cnt));
	}

	/** Get Age &gt;55 Count.
		@return Age &gt;55 Count
	  */
	public int getZZ_Age_O55_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Age_O55_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Age &lt;35 Count.
		@param ZZ_Age_U35_Cnt Age &lt;35 Count
	*/
	public void setZZ_Age_U35_Cnt (int ZZ_Age_U35_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Age_U35_Cnt, Integer.valueOf(ZZ_Age_U35_Cnt));
	}

	/** Get Age &lt;35 Count.
		@return Age &lt;35 Count
	  */
	public int getZZ_Age_U35_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Age_U35_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Coloured Female Count.
		@param ZZ_Coloured_Female_Cnt Coloured Female Count
	*/
	public void setZZ_Coloured_Female_Cnt (int ZZ_Coloured_Female_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Coloured_Female_Cnt, Integer.valueOf(ZZ_Coloured_Female_Cnt));
	}

	/** Get Coloured Female Count.
		@return Coloured Female Count
	  */
	public int getZZ_Coloured_Female_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Coloured_Female_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Coloured Male Count.
		@param ZZ_Coloured_Male_Cnt Coloured Male Count
	*/
	public void setZZ_Coloured_Male_Cnt (int ZZ_Coloured_Male_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Coloured_Male_Cnt, Integer.valueOf(ZZ_Coloured_Male_Cnt));
	}

	/** Get Coloured Male Count.
		@return Coloured Male Count
	  */
	public int getZZ_Coloured_Male_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Coloured_Male_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Disabled Count.
		@param ZZ_Disabled_Cnt Disabled Count
	*/
	public void setZZ_Disabled_Cnt (int ZZ_Disabled_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Disabled_Cnt, Integer.valueOf(ZZ_Disabled_Cnt));
	}

	/** Get Disabled Count.
		@return Disabled Count
	  */
	public int getZZ_Disabled_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Disabled_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Indian Female Count.
		@param ZZ_Indian_Female_Cnt Indian Female Count
	*/
	public void setZZ_Indian_Female_Cnt (int ZZ_Indian_Female_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Indian_Female_Cnt, Integer.valueOf(ZZ_Indian_Female_Cnt));
	}

	/** Get Indian Female Count.
		@return Indian Female Count
	  */
	public int getZZ_Indian_Female_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Indian_Female_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Indian Male Count.
		@param ZZ_Indian_Male_Cnt Indian Male Count
	*/
	public void setZZ_Indian_Male_Cnt (int ZZ_Indian_Male_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Indian_Male_Cnt, Integer.valueOf(ZZ_Indian_Male_Cnt));
	}

	/** Get Indian Male Count.
		@return Indian Male Count
	  */
	public int getZZ_Indian_Male_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Indian_Male_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Non-SA Count.
		@param ZZ_NonSA_Cnt Non-SA Count
	*/
	public void setZZ_NonSA_Cnt (int ZZ_NonSA_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_NonSA_Cnt, Integer.valueOf(ZZ_NonSA_Cnt));
	}

	/** Get Non-SA Count.
		@return Non-SA Count
	  */
	public int getZZ_NonSA_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_NonSA_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Section.
		@param ZZ_Report_Section Section
	*/
	public void setZZ_Report_Section (String ZZ_Report_Section)
	{
		set_Value (COLUMNNAME_ZZ_Report_Section, ZZ_Report_Section);
	}

	/** Get Section.
		@return Section	  */
	public String getZZ_Report_Section()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Report_Section);
	}

	/** Set Total Female Count.
		@param ZZ_Total_Female_Cnt Total Female Count
	*/
	public void setZZ_Total_Female_Cnt (int ZZ_Total_Female_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Total_Female_Cnt, Integer.valueOf(ZZ_Total_Female_Cnt));
	}

	/** Get Total Female Count.
		@return Total Female Count
	  */
	public int getZZ_Total_Female_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Total_Female_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Total Male Count.
		@param ZZ_Total_Male_Cnt Total Male Count
	*/
	public void setZZ_Total_Male_Cnt (int ZZ_Total_Male_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Total_Male_Cnt, Integer.valueOf(ZZ_Total_Male_Cnt));
	}

	/** Get Total Male Count.
		@return Total Male Count
	  */
	public int getZZ_Total_Male_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Total_Male_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Summary of Terminated Employees.
		@param ZZ_WSP_ATR_Term_Emp_Rep_ID Summary of Terminated Employees
	*/
	public void setZZ_WSP_ATR_Term_Emp_Rep_ID (int ZZ_WSP_ATR_Term_Emp_Rep_ID)
	{
		if (ZZ_WSP_ATR_Term_Emp_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Term_Emp_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Term_Emp_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Term_Emp_Rep_ID));
	}

	/** Get Summary of Terminated Employees.
		@return Summary of Terminated Employees	  */
	public int getZZ_WSP_ATR_Term_Emp_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Term_Emp_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Term_Emp_Rep_UU.
		@param ZZ_WSP_ATR_Term_Emp_Rep_UU ZZ_WSP_ATR_Term_Emp_Rep_UU
	*/
	public void setZZ_WSP_ATR_Term_Emp_Rep_UU (String ZZ_WSP_ATR_Term_Emp_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Term_Emp_Rep_UU, ZZ_WSP_ATR_Term_Emp_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Term_Emp_Rep_UU.
		@return ZZ_WSP_ATR_Term_Emp_Rep_UU	  */
	public String getZZ_WSP_ATR_Term_Emp_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Term_Emp_Rep_UU);
	}

	/** Set White Female Count.
		@param ZZ_White_Female_Cnt White Female Count
	*/
	public void setZZ_White_Female_Cnt (int ZZ_White_Female_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_White_Female_Cnt, Integer.valueOf(ZZ_White_Female_Cnt));
	}

	/** Get White Female Count.
		@return White Female Count
	  */
	public int getZZ_White_Female_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_White_Female_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set White Male Count.
		@param ZZ_White_Male_Cnt White Male Count
	*/
	public void setZZ_White_Male_Cnt (int ZZ_White_Male_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_White_Male_Cnt, Integer.valueOf(ZZ_White_Male_Cnt));
	}

	/** Get White Male Count.
		@return White Male Count
	  */
	public int getZZ_White_Male_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_White_Male_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}