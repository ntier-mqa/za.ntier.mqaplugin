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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ZZ_Monthly_Levy_Files
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_Monthly_Levy_Files")
public class X_ZZ_Monthly_Levy_Files extends PO implements I_ZZ_Monthly_Levy_Files, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260522L;

    /** Standard Constructor */
    public X_ZZ_Monthly_Levy_Files (Properties ctx, int ZZ_Monthly_Levy_Files_ID, String trxName)
    {
      super (ctx, ZZ_Monthly_Levy_Files_ID, trxName);
      /** if (ZZ_Monthly_Levy_Files_ID == 0)
        {
			setZZ_Monthly_Levy_Files_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Monthly_Levy_Files (Properties ctx, int ZZ_Monthly_Levy_Files_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Monthly_Levy_Files_ID, trxName, virtualColumns);
      /** if (ZZ_Monthly_Levy_Files_ID == 0)
        {
			setZZ_Monthly_Levy_Files_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Monthly_Levy_Files (Properties ctx, String ZZ_Monthly_Levy_Files_UU, String trxName)
    {
      super (ctx, ZZ_Monthly_Levy_Files_UU, trxName);
      /** if (ZZ_Monthly_Levy_Files_UU == null)
        {
			setZZ_Monthly_Levy_Files_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_Monthly_Levy_Files (Properties ctx, String ZZ_Monthly_Levy_Files_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_Monthly_Levy_Files_UU, trxName, virtualColumns);
      /** if (ZZ_Monthly_Levy_Files_UU == null)
        {
			setZZ_Monthly_Levy_Files_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_Monthly_Levy_Files (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_Monthly_Levy_Files[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_InvoiceBatchLine getC_InvoiceBatchLine() throws RuntimeException
	{
		return (org.compiere.model.I_C_InvoiceBatchLine)MTable.get(getCtx(), org.compiere.model.I_C_InvoiceBatchLine.Table_ID)
			.getPO(getC_InvoiceBatchLine_ID(), get_TrxName());
	}

	/** Set Invoice Batch Line.
		@param C_InvoiceBatchLine_ID Expense Invoice Batch Line
	*/
	public void setC_InvoiceBatchLine_ID (int C_InvoiceBatchLine_ID)
	{
		if (C_InvoiceBatchLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_InvoiceBatchLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_InvoiceBatchLine_ID, Integer.valueOf(C_InvoiceBatchLine_ID));
	}

	/** Get Invoice Batch Line.
		@return Expense Invoice Batch Line
	  */
	public int getC_InvoiceBatchLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_InvoiceBatchLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_Year getC_Year() throws RuntimeException
	{
		return (org.compiere.model.I_C_Year)MTable.get(getCtx(), org.compiere.model.I_C_Year.Table_ID)
			.getPO(getC_Year_ID(), get_TrxName());
	}

	/** Set Year.
		@param C_Year_ID Calendar Year
	*/
	public void setC_Year_ID (int C_Year_ID)
	{
		if (C_Year_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Year_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Year_ID, Integer.valueOf(C_Year_ID));
	}

	/** Get Year.
		@return Calendar Year
	  */
	public int getC_Year_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Year_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Admin.
		@param ZZ_Admin Admin
	*/
	public void setZZ_Admin (BigDecimal ZZ_Admin)
	{
		set_Value (COLUMNNAME_ZZ_Admin, ZZ_Admin);
	}

	/** Get Admin.
		@return Admin	  */
	public BigDecimal getZZ_Admin()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Admin);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Current Date.
		@param ZZ_Current_Date Current Date
	*/
	public void setZZ_Current_Date (Timestamp ZZ_Current_Date)
	{
		set_Value (COLUMNNAME_ZZ_Current_Date, ZZ_Current_Date);
	}

	/** Get Current Date.
		@return Current Date	  */
	public Timestamp getZZ_Current_Date()
	{
		return (Timestamp)get_Value(COLUMNNAME_ZZ_Current_Date);
	}

	/** Set DG.
		@param ZZ_DG DG
	*/
	public void setZZ_DG (BigDecimal ZZ_DG)
	{
		set_Value (COLUMNNAME_ZZ_DG, ZZ_DG);
	}

	/** Get DG.
		@return DG	  */
	public BigDecimal getZZ_DG()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_DG);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set File Name.
		@param ZZ_File_Name File Name
	*/
	public void setZZ_File_Name (String ZZ_File_Name)
	{
		set_Value (COLUMNNAME_ZZ_File_Name, ZZ_File_Name);
	}

	/** Get File Name.
		@return File Name	  */
	public String getZZ_File_Name()
	{
		return (String)get_Value(COLUMNNAME_ZZ_File_Name);
	}

	/** Approved = A */
	public static final String ZZ_GRANT_STATUS_Approved = "A";
	/** Rejected = R */
	public static final String ZZ_GRANT_STATUS_Rejected = "R";
	/** Set Grant Status.
		@param ZZ_Grant_Status Grant Status
	*/
	public void setZZ_Grant_Status (String ZZ_Grant_Status)
	{

		set_Value (COLUMNNAME_ZZ_Grant_Status, ZZ_Grant_Status);
	}

	/** Get Grant Status.
		@return Grant Status	  */
	public String getZZ_Grant_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Grant_Status);
	}

	/** Set MG.
		@param ZZ_MG MG
	*/
	public void setZZ_MG (BigDecimal ZZ_MG)
	{
		set_Value (COLUMNNAME_ZZ_MG, ZZ_MG);
	}

	/** Get MG.
		@return MG	  */
	public BigDecimal getZZ_MG()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_MG);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** January = 01 */
	public static final String ZZ_MONTH_January = "01";
	/** February = 02 */
	public static final String ZZ_MONTH_February = "02";
	/** March = 03 */
	public static final String ZZ_MONTH_March = "03";
	/** April = 04 */
	public static final String ZZ_MONTH_April = "04";
	/** May = 05 */
	public static final String ZZ_MONTH_May = "05";
	/** June = 06 */
	public static final String ZZ_MONTH_June = "06";
	/** July = 07 */
	public static final String ZZ_MONTH_July = "07";
	/** August = 08 */
	public static final String ZZ_MONTH_August = "08";
	/** September = 09 */
	public static final String ZZ_MONTH_September = "09";
	/** October = 10 */
	public static final String ZZ_MONTH_October = "10";
	/** November = 11 */
	public static final String ZZ_MONTH_November = "11";
	/** December = 12 */
	public static final String ZZ_MONTH_December = "12";
	/** Set Month.
		@param ZZ_Month Month
	*/
	public void setZZ_Month (String ZZ_Month)
	{

		set_Value (COLUMNNAME_ZZ_Month, ZZ_Month);
	}

	/** Get Month.
		@return Month	  */
	public String getZZ_Month()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Month);
	}

	public I_ZZ_Monthly_Levy_Files_Hdr getZZ_Monthly_Levy_Files_Hdr() throws RuntimeException
	{
		return (I_ZZ_Monthly_Levy_Files_Hdr)MTable.get(getCtx(), I_ZZ_Monthly_Levy_Files_Hdr.Table_ID)
			.getPO(getZZ_Monthly_Levy_Files_Hdr_ID(), get_TrxName());
	}

	/** Set Monthly Levy Files Hdr.
		@param ZZ_Monthly_Levy_Files_Hdr_ID Monthly Levy Files Hdr
	*/
	public void setZZ_Monthly_Levy_Files_Hdr_ID (int ZZ_Monthly_Levy_Files_Hdr_ID)
	{
		if (ZZ_Monthly_Levy_Files_Hdr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID, Integer.valueOf(ZZ_Monthly_Levy_Files_Hdr_ID));
	}

	/** Get Monthly Levy Files Hdr.
		@return Monthly Levy Files Hdr	  */
	public int getZZ_Monthly_Levy_Files_Hdr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Monthly Levy Files.
		@param ZZ_Monthly_Levy_Files_ID Monthly Levy Files
	*/
	public void setZZ_Monthly_Levy_Files_ID (int ZZ_Monthly_Levy_Files_ID)
	{
		if (ZZ_Monthly_Levy_Files_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_Monthly_Levy_Files_ID, Integer.valueOf(ZZ_Monthly_Levy_Files_ID));
	}

	/** Get Monthly Levy Files.
		@return Monthly Levy Files	  */
	public int getZZ_Monthly_Levy_Files_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Monthly_Levy_Files_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_Monthly_Levy_Files_UU.
		@param ZZ_Monthly_Levy_Files_UU ZZ_Monthly_Levy_Files_UU
	*/
	public void setZZ_Monthly_Levy_Files_UU (String ZZ_Monthly_Levy_Files_UU)
	{
		set_Value (COLUMNNAME_ZZ_Monthly_Levy_Files_UU, ZZ_Monthly_Levy_Files_UU);
	}

	/** Get ZZ_Monthly_Levy_Files_UU.
		@return ZZ_Monthly_Levy_Files_UU	  */
	public String getZZ_Monthly_Levy_Files_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Monthly_Levy_Files_UU);
	}

	/** Set Penalties.
		@param ZZ_Penalties Penalties
	*/
	public void setZZ_Penalties (BigDecimal ZZ_Penalties)
	{
		set_Value (COLUMNNAME_ZZ_Penalties, ZZ_Penalties);
	}

	/** Get Penalties.
		@return Penalties	  */
	public BigDecimal getZZ_Penalties()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Penalties);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set SDL Number.
		@param ZZ_SDL_No SDL Number
	*/
	public void setZZ_SDL_No (String ZZ_SDL_No)
	{
		set_Value (COLUMNNAME_ZZ_SDL_No, ZZ_SDL_No);
	}

	/** Get SDL Number.
		@return SDL Number	  */
	public String getZZ_SDL_No()
	{
		return (String)get_Value(COLUMNNAME_ZZ_SDL_No);
	}

	/** Set Scheme Year Adjustment.
		@param ZZ_Scheme_Year_Adjust Scheme Year Adjustment
	*/
	public void setZZ_Scheme_Year_Adjust (String ZZ_Scheme_Year_Adjust)
	{
		set_Value (COLUMNNAME_ZZ_Scheme_Year_Adjust, ZZ_Scheme_Year_Adjust);
	}

	/** Get Scheme Year Adjustment.
		@return Scheme Year Adjustment	  */
	public String getZZ_Scheme_Year_Adjust()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Scheme_Year_Adjust);
	}

	/** Set Seta Code.
		@param ZZ_Seta_Code Seta Code
	*/
	public void setZZ_Seta_Code (String ZZ_Seta_Code)
	{
		set_Value (COLUMNNAME_ZZ_Seta_Code, ZZ_Seta_Code);
	}

	/** Get Seta Code.
		@return Seta Code	  */
	public String getZZ_Seta_Code()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Seta_Code);
	}

	/** Set Year.
		@param ZZ_Year Year
	*/
	public void setZZ_Year (String ZZ_Year)
	{
		set_Value (COLUMNNAME_ZZ_Year, ZZ_Year);
	}

	/** Get Year.
		@return Year	  */
	public String getZZ_Year()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Year);
	}

	/** Set Interest.
		@param zz_Interest Interest
	*/
	public void setzz_Interest (BigDecimal zz_Interest)
	{
		set_Value (COLUMNNAME_zz_Interest, zz_Interest);
	}

	/** Get Interest.
		@return Interest	  */
	public BigDecimal getzz_Interest()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_zz_Interest);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Total.
		@param zz_Total Total
	*/
	public void setzz_Total (BigDecimal zz_Total)
	{
		set_Value (COLUMNNAME_zz_Total, zz_Total);
	}

	/** Get Total.
		@return Total	  */
	public BigDecimal getzz_Total()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_zz_Total);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}