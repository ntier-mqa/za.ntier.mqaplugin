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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ZZ_WSP_ATR_ATR_Detail
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_ATR_Detail")
public class X_ZZ_WSP_ATR_ATR_Detail extends PO implements I_ZZ_WSP_ATR_ATR_Detail, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260222L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_ATR_Detail (Properties ctx, int ZZ_WSP_ATR_ATR_Detail_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_ATR_Detail_ID, trxName);
      /** if (ZZ_WSP_ATR_ATR_Detail_ID == 0)
        {
			setZZ_WSP_ATR_ATR_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_ATR_Detail (Properties ctx, int ZZ_WSP_ATR_ATR_Detail_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_ATR_Detail_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_ATR_Detail_ID == 0)
        {
			setZZ_WSP_ATR_ATR_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_ATR_Detail (Properties ctx, String ZZ_WSP_ATR_ATR_Detail_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_ATR_Detail_UU, trxName);
      /** if (ZZ_WSP_ATR_ATR_Detail_UU == null)
        {
			setZZ_WSP_ATR_ATR_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_ATR_Detail (Properties ctx, String ZZ_WSP_ATR_ATR_Detail_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_ATR_Detail_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_ATR_Detail_UU == null)
        {
			setZZ_WSP_ATR_ATR_Detail_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_ATR_Detail (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_ATR_Detail[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_ZZ_Not_Achieved_Ref getDropout_Reason() throws RuntimeException
	{
		return (I_ZZ_Not_Achieved_Ref)MTable.get(getCtx(), I_ZZ_Not_Achieved_Ref.Table_ID)
			.getPO(getDropout_Reason_ID(), get_TrxName());
	}

	/** Set Dropout_Reason_ID.
		@param Dropout_Reason_ID Dropout_Reason_ID
	*/
	public void setDropout_Reason_ID (int Dropout_Reason_ID)
	{
		if (Dropout_Reason_ID < 1)
			set_Value (COLUMNNAME_Dropout_Reason_ID, null);
		else
			set_Value (COLUMNNAME_Dropout_Reason_ID, Integer.valueOf(Dropout_Reason_ID));
	}

	/** Get Dropout_Reason_ID.
		@return Dropout_Reason_ID	  */
	public int getDropout_Reason_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Dropout_Reason_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Employee_Name.
		@param Employee_Name Employee_Name
	*/
	public void setEmployee_Name (String Employee_Name)
	{
		set_Value (COLUMNNAME_Employee_Name, Employee_Name);
	}

	/** Get Employee_Name.
		@return Employee_Name	  */
	public String getEmployee_Name()
	{
		return (String)get_Value(COLUMNNAME_Employee_Name);
	}

	public I_ZZ_WSP_Employees getEmployee_Number() throws RuntimeException
	{
		return (I_ZZ_WSP_Employees)MTable.get(getCtx(), I_ZZ_WSP_Employees.Table_ID)
			.getPO(getEmployee_Number_ID(), get_TrxName());
	}

	/** Set Employee_Number_ID.
		@param Employee_Number_ID Employee_Number_ID
	*/
	public void setEmployee_Number_ID (int Employee_Number_ID)
	{
		if (Employee_Number_ID < 1)
			set_Value (COLUMNNAME_Employee_Number_ID, null);
		else
			set_Value (COLUMNNAME_Employee_Number_ID, Integer.valueOf(Employee_Number_ID));
	}

	/** Get Employee_Number_ID.
		@return Employee_Number_ID	  */
	public int getEmployee_Number_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Employee_Number_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Field_of_Study_Specify_Ref getField_of_Study_Specify() throws RuntimeException
	{
		return (I_ZZ_Field_of_Study_Specify_Ref)MTable.get(getCtx(), I_ZZ_Field_of_Study_Specify_Ref.Table_ID)
			.getPO(getField_of_Study_Specify_ID(), get_TrxName());
	}

	/** Set Field_of_Study_Specify_ID.
		@param Field_of_Study_Specify_ID Field_of_Study_Specify_ID
	*/
	public void setField_of_Study_Specify_ID (int Field_of_Study_Specify_ID)
	{
		if (Field_of_Study_Specify_ID < 1)
			set_Value (COLUMNNAME_Field_of_Study_Specify_ID, null);
		else
			set_Value (COLUMNNAME_Field_of_Study_Specify_ID, Integer.valueOf(Field_of_Study_Specify_ID));
	}

	/** Get Field_of_Study_Specify_ID.
		@return Field_of_Study_Specify_ID	  */
	public int getField_of_Study_Specify_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Field_of_Study_Specify_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Qualification_Type_Details_Ref getLearning_Programme_Detail() throws RuntimeException
	{
		return (I_ZZ_Qualification_Type_Details_Ref)MTable.get(getCtx(), I_ZZ_Qualification_Type_Details_Ref.Table_ID)
			.getPO(getLearning_Programme_Detail_ID(), get_TrxName());
	}

	/** Set Learning_Programme_Detail_ID.
		@param Learning_Programme_Detail_ID Learning_Programme_Detail_ID
	*/
	public void setLearning_Programme_Detail_ID (int Learning_Programme_Detail_ID)
	{
		if (Learning_Programme_Detail_ID < 1)
			set_Value (COLUMNNAME_Learning_Programme_Detail_ID, null);
		else
			set_Value (COLUMNNAME_Learning_Programme_Detail_ID, Integer.valueOf(Learning_Programme_Detail_ID));
	}

	/** Get Learning_Programme_Detail_ID.
		@return Learning_Programme_Detail_ID	  */
	public int getLearning_Programme_Detail_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Learning_Programme_Detail_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_ZZ_Learning_Programme_Ref getQualification_Type() throws RuntimeException
	{
		return (I_ZZ_Learning_Programme_Ref)MTable.get(getCtx(), I_ZZ_Learning_Programme_Ref.Table_ID)
			.getPO(getQualification_Type_ID(), get_TrxName());
	}

	/** Set Qualification_Type_ID.
		@param Qualification_Type_ID Qualification_Type_ID
	*/
	public void setQualification_Type_ID (int Qualification_Type_ID)
	{
		if (Qualification_Type_ID < 1)
			set_Value (COLUMNNAME_Qualification_Type_ID, null);
		else
			set_Value (COLUMNNAME_Qualification_Type_ID, Integer.valueOf(Qualification_Type_ID));
	}

	/** Get Qualification_Type_ID.
		@return Qualification_Type_ID	  */
	public int getQualification_Type_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Qualification_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Total_Training_Cost.
		@param Total_Training_Cost Total_Training_Cost
	*/
	public void setTotal_Training_Cost (BigDecimal Total_Training_Cost)
	{
		set_Value (COLUMNNAME_Total_Training_Cost, Total_Training_Cost);
	}

	/** Get Total_Training_Cost.
		@return Total_Training_Cost	  */
	public BigDecimal getTotal_Training_Cost()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Total_Training_Cost);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	public I_ZZ_Achievement_Status_Ref getTraining_Status() throws RuntimeException
	{
		return (I_ZZ_Achievement_Status_Ref)MTable.get(getCtx(), I_ZZ_Achievement_Status_Ref.Table_ID)
			.getPO(getTraining_Status_ID(), get_TrxName());
	}

	/** Set Training_Status_ID.
		@param Training_Status_ID Training_Status_ID
	*/
	public void setTraining_Status_ID (int Training_Status_ID)
	{
		if (Training_Status_ID < 1)
			set_Value (COLUMNNAME_Training_Status_ID, null);
		else
			set_Value (COLUMNNAME_Training_Status_ID, Integer.valueOf(Training_Status_ID));
	}

	/** Get Training_Status_ID.
		@return Training_Status_ID	  */
	public int getTraining_Status_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Training_Status_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Year_Completed.
		@param Year_Completed Year_Completed
	*/
	public void setYear_Completed (String Year_Completed)
	{
		set_Value (COLUMNNAME_Year_Completed, Year_Completed);
	}

	/** Get Year_Completed.
		@return Year_Completed	  */
	public String getYear_Completed()
	{
		return (String)get_Value(COLUMNNAME_Year_Completed);
	}

	/** Set Year_Enrolled.
		@param Year_Enrolled Year_Enrolled
	*/
	public void setYear_Enrolled (String Year_Enrolled)
	{
		set_Value (COLUMNNAME_Year_Enrolled, Year_Enrolled);
	}

	/** Get Year_Enrolled.
		@return Year_Enrolled	  */
	public String getYear_Enrolled()
	{
		return (String)get_Value(COLUMNNAME_Year_Enrolled);
	}

	/** Set WSP/ATR ATR Detail.
		@param ZZ_WSP_ATR_ATR_Detail_ID WSP/ATR ATR Detail
	*/
	public void setZZ_WSP_ATR_ATR_Detail_ID (int ZZ_WSP_ATR_ATR_Detail_ID)
	{
		if (ZZ_WSP_ATR_ATR_Detail_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_ID, Integer.valueOf(ZZ_WSP_ATR_ATR_Detail_ID));
	}

	/** Get WSP/ATR ATR Detail.
		@return WSP/ATR ATR Detail
	  */
	public int getZZ_WSP_ATR_ATR_Detail_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_ATR_Detail_UU.
		@param ZZ_WSP_ATR_ATR_Detail_UU ZZ_WSP_ATR_ATR_Detail_UU
	*/
	public void setZZ_WSP_ATR_ATR_Detail_UU (String ZZ_WSP_ATR_ATR_Detail_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_UU, ZZ_WSP_ATR_ATR_Detail_UU);
	}

	/** Get ZZ_WSP_ATR_ATR_Detail_UU.
		@return ZZ_WSP_ATR_ATR_Detail_UU	  */
	public String getZZ_WSP_ATR_ATR_Detail_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_UU);
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

	public I_ZZ_WSP_Employees getZZ_WSP_Employees() throws RuntimeException
	{
		return (I_ZZ_WSP_Employees)MTable.get(getCtx(), I_ZZ_WSP_Employees.Table_ID)
			.getPO(getZZ_WSP_Employees_ID(), get_TrxName());
	}

	/** Set ZZ_WSP_Employees.
		@param ZZ_WSP_Employees_ID ZZ_WSP_Employees reference table
	*/
	public void setZZ_WSP_Employees_ID (int ZZ_WSP_Employees_ID)
	{
		if (ZZ_WSP_Employees_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_Employees_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_Employees_ID, Integer.valueOf(ZZ_WSP_Employees_ID));
	}

	/** Get ZZ_WSP_Employees.
		@return ZZ_WSP_Employees reference table
	  */
	public int getZZ_WSP_Employees_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_Employees_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}