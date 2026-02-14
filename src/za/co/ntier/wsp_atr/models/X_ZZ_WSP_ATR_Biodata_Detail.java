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

/** Generated Model for ZZ_WSP_ATR_Biodata_Detail
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Biodata_Detail")
public class X_ZZ_WSP_ATR_Biodata_Detail extends PO implements I_ZZ_WSP_ATR_Biodata_Detail, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Biodata_Detail (Properties ctx, int ZZ_WSP_ATR_Biodata_Detail_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Biodata_Detail_ID, trxName);
      /** if (ZZ_WSP_ATR_Biodata_Detail_ID == 0)
        {
			setZZ_WSP_ATR_Biodata_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Biodata_Detail (Properties ctx, int ZZ_WSP_ATR_Biodata_Detail_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Biodata_Detail_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Biodata_Detail_ID == 0)
        {
			setZZ_WSP_ATR_Biodata_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Biodata_Detail (Properties ctx, String ZZ_WSP_ATR_Biodata_Detail_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Biodata_Detail_UU, trxName);
      /** if (ZZ_WSP_ATR_Biodata_Detail_UU == null)
        {
			setZZ_WSP_ATR_Biodata_Detail_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Biodata_Detail (Properties ctx, String ZZ_WSP_ATR_Biodata_Detail_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Biodata_Detail_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Biodata_Detail_UU == null)
        {
			setZZ_WSP_ATR_Biodata_Detail_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Biodata_Detail (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Biodata_Detail[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Birth Year - TRUE.
		@param Birth_Year_TRUE Birth Year - TRUE
	*/
	public void setBirth_Year_TRUE (String Birth_Year_TRUE)
	{
		set_Value (COLUMNNAME_Birth_Year_TRUE, Birth_Year_TRUE);
	}

	/** Get Birth Year - TRUE.
		@return Birth Year - TRUE	  */
	public String getBirth_Year_TRUE()
	{
		return (String)get_Value(COLUMNNAME_Birth_Year_TRUE);
	}

	public I_ZZ_No_Yes_Ref getDisabled() throws RuntimeException
	{
		return (I_ZZ_No_Yes_Ref)MTable.get(getCtx(), I_ZZ_No_Yes_Ref.Table_ID)
			.getPO(getDisabled_ID(), get_TrxName());
	}

	/** Set Disabled?.
		@param Disabled_ID Disabled?
	*/
	public void setDisabled_ID (int Disabled_ID)
	{
		if (Disabled_ID < 1)
			set_Value (COLUMNNAME_Disabled_ID, null);
		else
			set_Value (COLUMNNAME_Disabled_ID, Integer.valueOf(Disabled_ID));
	}

	/** Get Disabled?.
		@return Disabled?	  */
	public int getDisabled_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Disabled_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Year_Ref getEmployment_Start_Year_year_of_engagement() throws RuntimeException
	{
		return (I_ZZ_Year_Ref)MTable.get(getCtx(), I_ZZ_Year_Ref.Table_ID)
			.getPO(getEmployment_Start_Year_year_of_engagement_ID(), get_TrxName());
	}

	/** Set Employment Start Year (year of engagement).
		@param Employment_Start_Year_year_of_engagement_ID Employment Start Year (year of engagement)
	*/
	public void setEmployment_Start_Year_year_of_engagement_ID (int Employment_Start_Year_year_of_engagement_ID)
	{
		if (Employment_Start_Year_year_of_engagement_ID < 1)
			set_Value (COLUMNNAME_Employment_Start_Year_year_of_engagement_ID, null);
		else
			set_Value (COLUMNNAME_Employment_Start_Year_year_of_engagement_ID, Integer.valueOf(Employment_Start_Year_year_of_engagement_ID));
	}

	/** Get Employment Start Year (year of engagement).
		@return Employment Start Year (year of engagement)	  */
	public int getEmployment_Start_Year_year_of_engagement_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Employment_Start_Year_year_of_engagement_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public I_ZZ_Gender_Ref getGender() throws RuntimeException
	{
		return (I_ZZ_Gender_Ref)MTable.get(getCtx(), I_ZZ_Gender_Ref.Table_ID)
			.getPO(getGender_ID(), get_TrxName());
	}

	/** Set Gender_ID.
		@param Gender_ID Gender_ID
	*/
	public void setGender_ID (int Gender_ID)
	{
		if (Gender_ID < 1)
			set_Value (COLUMNNAME_Gender_ID, null);
		else
			set_Value (COLUMNNAME_Gender_ID, Integer.valueOf(Gender_ID));
	}

	/** Get Gender_ID.
		@return Gender_ID	  */
	public int getGender_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Gender_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Qualification_Type_Ref getHighest_Qualification_Type() throws RuntimeException
	{
		return (I_ZZ_Qualification_Type_Ref)MTable.get(getCtx(), I_ZZ_Qualification_Type_Ref.Table_ID)
			.getPO(getHighest_Qualification_Type_ID(), get_TrxName());
	}

	/** Set Highest Qualification Type.
		@param Highest_Qualification_Type_ID Highest Qualification Type
	*/
	public void setHighest_Qualification_Type_ID (int Highest_Qualification_Type_ID)
	{
		if (Highest_Qualification_Type_ID < 1)
			set_Value (COLUMNNAME_Highest_Qualification_Type_ID, null);
		else
			set_Value (COLUMNNAME_Highest_Qualification_Type_ID, Integer.valueOf(Highest_Qualification_Type_ID));
	}

	/** Get Highest Qualification Type.
		@return Highest Qualification Type	  */
	public int getHighest_Qualification_Type_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Highest_Qualification_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ID / Passport No / Employee Number.
		@param ID_Passport_No_Employee_Number ID / Passport No / Employee Number
	*/
	public void setID_Passport_No_Employee_Number (String ID_Passport_No_Employee_Number)
	{
		set_Value (COLUMNNAME_ID_Passport_No_Employee_Number, ID_Passport_No_Employee_Number);
	}

	/** Get ID / Passport No / Employee Number.
		@return ID / Passport No / Employee Number	  */
	public String getID_Passport_No_Employee_Number()
	{
		return (String)get_Value(COLUMNNAME_ID_Passport_No_Employee_Number);
	}

	/** Set Job Title.
		@param Job_Title Job Title
	*/
	public void setJob_Title (String Job_Title)
	{
		set_Value (COLUMNNAME_Job_Title, Job_Title);
	}

	/** Get Job Title.
		@return Job Title	  */
	public String getJob_Title()
	{
		return (String)get_Value(COLUMNNAME_Job_Title);
	}

	public I_ZZ_Municipality_Ref getMunicipality() throws RuntimeException
	{
		return (I_ZZ_Municipality_Ref)MTable.get(getCtx(), I_ZZ_Municipality_Ref.Table_ID)
			.getPO(getMunicipality_ID(), get_TrxName());
	}

	/** Set Municipality.
		@param Municipality_ID Municipality
	*/
	public void setMunicipality_ID (int Municipality_ID)
	{
		if (Municipality_ID < 1)
			set_Value (COLUMNNAME_Municipality_ID, null);
		else
			set_Value (COLUMNNAME_Municipality_ID, Integer.valueOf(Municipality_ID));
	}

	/** Get Municipality.
		@return Municipality	  */
	public int getMunicipality_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Municipality_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name &amp; Surname.
		@param Name_Surname Name &amp; Surname
	*/
	public void setName_Surname (String Name_Surname)
	{
		set_Value (COLUMNNAME_Name_Surname, Name_Surname);
	}

	/** Get Name &amp; Surname.
		@return Name &amp; Surname	  */
	public String getName_Surname()
	{
		return (String)get_Value(COLUMNNAME_Name_Surname);
	}

	public I_ZZ_Occupations_Ref getOFO_Occupation_Code() throws RuntimeException
	{
		return (I_ZZ_Occupations_Ref)MTable.get(getCtx(), I_ZZ_Occupations_Ref.Table_ID)
			.getPO(getOFO_Occupation_Code_ID(), get_TrxName());
	}

	/** Set OFO Occupation Code.
		@param OFO_Occupation_Code_ID OFO Occupation Code
	*/
	public void setOFO_Occupation_Code_ID (int OFO_Occupation_Code_ID)
	{
		if (OFO_Occupation_Code_ID < 1)
			set_Value (COLUMNNAME_OFO_Occupation_Code_ID, null);
		else
			set_Value (COLUMNNAME_OFO_Occupation_Code_ID, Integer.valueOf(OFO_Occupation_Code_ID));
	}

	/** Get OFO Occupation Code.
		@return OFO Occupation Code	  */
	public int getOFO_Occupation_Code_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OFO_Occupation_Code_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Occupations_Ref getOFO_Occupation() throws RuntimeException
	{
		return (I_ZZ_Occupations_Ref)MTable.get(getCtx(), I_ZZ_Occupations_Ref.Table_ID)
			.getPO(getOFO_Occupation_ID(), get_TrxName());
	}

	/** Set OFO Occupation.
		@param OFO_Occupation_ID OFO Occupation
	*/
	public void setOFO_Occupation_ID (int OFO_Occupation_ID)
	{
		if (OFO_Occupation_ID < 1)
			set_Value (COLUMNNAME_OFO_Occupation_ID, null);
		else
			set_Value (COLUMNNAME_OFO_Occupation_ID, Integer.valueOf(OFO_Occupation_ID));
	}

	/** Get OFO Occupation.
		@return OFO Occupation	  */
	public int getOFO_Occupation_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OFO_Occupation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Specializations_Ref getOFO_Specialisation() throws RuntimeException
	{
		return (I_ZZ_Specializations_Ref)MTable.get(getCtx(), I_ZZ_Specializations_Ref.Table_ID)
			.getPO(getOFO_Specialisation_ID(), get_TrxName());
	}

	/** Set OFO Specialisation.
		@param OFO_Specialisation_ID OFO Specialisation
	*/
	public void setOFO_Specialisation_ID (int OFO_Specialisation_ID)
	{
		if (OFO_Specialisation_ID < 1)
			set_Value (COLUMNNAME_OFO_Specialisation_ID, null);
		else
			set_Value (COLUMNNAME_OFO_Specialisation_ID, Integer.valueOf(OFO_Specialisation_ID));
	}

	/** Get OFO Specialisation.
		@return OFO Specialisation	  */
	public int getOFO_Specialisation_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_OFO_Specialisation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Occupational Levels For Equity Reporting Purposes.
		@param Occupational_Levels_For_Equity_Reporting_Purposes Occupational Levels For Equity Reporting Purposes
	*/
	public void setOccupational_Levels_For_Equity_Reporting_Purposes (String Occupational_Levels_For_Equity_Reporting_Purposes)
	{
		set_Value (COLUMNNAME_Occupational_Levels_For_Equity_Reporting_Purposes, Occupational_Levels_For_Equity_Reporting_Purposes);
	}

	/** Get Occupational Levels For Equity Reporting Purposes.
		@return Occupational Levels For Equity Reporting Purposes	  */
	public String getOccupational_Levels_For_Equity_Reporting_Purposes()
	{
		return (String)get_Value(COLUMNNAME_Occupational_Levels_For_Equity_Reporting_Purposes);
	}

	/** Set Organisation Structure Filter (Optional).
		@param Organisation_Structure_Filter_Optional Organisation Structure Filter (Optional)
	*/
	public void setOrganisation_Structure_Filter_Optional (String Organisation_Structure_Filter_Optional)
	{
		set_Value (COLUMNNAME_Organisation_Structure_Filter_Optional, Organisation_Structure_Filter_Optional);
	}

	/** Get Organisation Structure Filter (Optional).
		@return Organisation Structure Filter (Optional)	  */
	public String getOrganisation_Structure_Filter_Optional()
	{
		return (String)get_Value(COLUMNNAME_Organisation_Structure_Filter_Optional);
	}

	/** Set Post Reference (Optional).
		@param Post_Reference_Optional Post Reference (Optional)
	*/
	public void setPost_Reference_Optional (String Post_Reference_Optional)
	{
		set_Value (COLUMNNAME_Post_Reference_Optional, Post_Reference_Optional);
	}

	/** Get Post Reference (Optional).
		@return Post Reference (Optional)	  */
	public String getPost_Reference_Optional()
	{
		return (String)get_Value(COLUMNNAME_Post_Reference_Optional);
	}

	public I_ZZ_Province_Ref getProvince() throws RuntimeException
	{
		return (I_ZZ_Province_Ref)MTable.get(getCtx(), I_ZZ_Province_Ref.Table_ID)
			.getPO(getProvince_ID(), get_TrxName());
	}

	/** Set Province.
		@param Province_ID Province
	*/
	public void setProvince_ID (int Province_ID)
	{
		if (Province_ID < 1)
			set_Value (COLUMNNAME_Province_ID, null);
		else
			set_Value (COLUMNNAME_Province_ID, Integer.valueOf(Province_ID));
	}

	/** Get Province.
		@return Province	  */
	public int getProvince_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Province_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Equity_Ref getRace() throws RuntimeException
	{
		return (I_ZZ_Equity_Ref)MTable.get(getCtx(), I_ZZ_Equity_Ref.Table_ID)
			.getPO(getRace_ID(), get_TrxName());
	}

	/** Set Race.
		@param Race_ID Race
	*/
	public void setRace_ID (int Race_ID)
	{
		if (Race_ID < 1)
			set_Value (COLUMNNAME_Race_ID, null);
		else
			set_Value (COLUMNNAME_Race_ID, Integer.valueOf(Race_ID));
	}

	/** Get Race.
		@return Race	  */
	public int getRace_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Race_ID);
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

	public I_ZZ_No_Yes_Ref getSA_Citizen() throws RuntimeException
	{
		return (I_ZZ_No_Yes_Ref)MTable.get(getCtx(), I_ZZ_No_Yes_Ref.Table_ID)
			.getPO(getSA_Citizen_ID(), get_TrxName());
	}

	/** Set SA Citizen?.
		@param SA_Citizen_ID SA Citizen?
	*/
	public void setSA_Citizen_ID (int SA_Citizen_ID)
	{
		if (SA_Citizen_ID < 1)
			set_Value (COLUMNNAME_SA_Citizen_ID, null);
		else
			set_Value (COLUMNNAME_SA_Citizen_ID, Integer.valueOf(SA_Citizen_ID));
	}

	/** Get SA Citizen?.
		@return SA Citizen?	  */
	public int getSA_Citizen_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SA_Citizen_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Specify field of Study for Post-School qualifications or rea.
		@param Specify_field_of_Study_for_Post_School_qualifications_or_reason Specify field of Study for Post-School qualifications or rea
	*/
	public void setSpecify_field_of_Study_for_Post_School_qualifications_or_reason (String Specify_field_of_Study_for_Post_School_qualifications_or_reason)
	{
		set_Value (COLUMNNAME_Specify_field_of_Study_for_Post_School_qualifications_or_reason, Specify_field_of_Study_for_Post_School_qualifications_or_reason);
	}

	/** Get Specify field of Study for Post-School qualifications or rea.
		@return Specify field of Study for Post-School qualifications or rea	  */
	public String getSpecify_field_of_Study_for_Post_School_qualifications_or_reason()
	{
		return (String)get_Value(COLUMNNAME_Specify_field_of_Study_for_Post_School_qualifications_or_reason);
	}

	/** Set WSP/ATR Biodata Detail.
		@param ZZ_WSP_ATR_Biodata_Detail_ID WSP/ATR Biodata Detail
	*/
	public void setZZ_WSP_ATR_Biodata_Detail_ID (int ZZ_WSP_ATR_Biodata_Detail_ID)
	{
		if (ZZ_WSP_ATR_Biodata_Detail_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_ID, Integer.valueOf(ZZ_WSP_ATR_Biodata_Detail_ID));
	}

	/** Get WSP/ATR Biodata Detail.
		@return WSP/ATR Biodata Detail
	  */
	public int getZZ_WSP_ATR_Biodata_Detail_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Biodata_Detail_UU.
		@param ZZ_WSP_ATR_Biodata_Detail_UU ZZ_WSP_ATR_Biodata_Detail_UU
	*/
	public void setZZ_WSP_ATR_Biodata_Detail_UU (String ZZ_WSP_ATR_Biodata_Detail_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_UU, ZZ_WSP_ATR_Biodata_Detail_UU);
	}

	/** Get ZZ_WSP_ATR_Biodata_Detail_UU.
		@return ZZ_WSP_ATR_Biodata_Detail_UU	  */
	public String getZZ_WSP_ATR_Biodata_Detail_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_UU);
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