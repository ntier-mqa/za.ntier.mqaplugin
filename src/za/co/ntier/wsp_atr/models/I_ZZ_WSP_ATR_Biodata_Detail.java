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
package za.co.ntier.wsp_atr.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for ZZ_WSP_ATR_Biodata_Detail
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Biodata_Detail 
{

    /** TableName=ZZ_WSP_ATR_Biodata_Detail */
    public static final String Table_Name = "ZZ_WSP_ATR_Biodata_Detail";

    /** AD_Table_ID=1000164 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Unit.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Unit.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name Birth_Year_TRUE */
    public static final String COLUMNNAME_Birth_Year_TRUE = "Birth_Year_TRUE";

	/** Set Birth Year - TRUE	  */
	public void setBirth_Year_TRUE (String Birth_Year_TRUE);

	/** Get Birth Year - TRUE	  */
	public String getBirth_Year_TRUE();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name Disabled_ID */
    public static final String COLUMNNAME_Disabled_ID = "Disabled_ID";

	/** Set Disabled?	  */
	public void setDisabled_ID (int Disabled_ID);

	/** Get Disabled?	  */
	public int getDisabled_ID();

	public I_ZZ_No_Yes_Ref getDisabled() throws RuntimeException;

    /** Column name Employment_Start_Year_year_of_engagement_ID */
    public static final String COLUMNNAME_Employment_Start_Year_year_of_engagement_ID = "Employment_Start_Year_year_of_engagement_ID";

	/** Set Employment Start Year (year of engagement)	  */
	public void setEmployment_Start_Year_year_of_engagement_ID (int Employment_Start_Year_year_of_engagement_ID);

	/** Get Employment Start Year (year of engagement)	  */
	public int getEmployment_Start_Year_year_of_engagement_ID();

	public I_ZZ_Year_Ref getEmployment_Start_Year_year_of_engagement() throws RuntimeException;

    /** Column name Employment_Status_ID */
    public static final String COLUMNNAME_Employment_Status_ID = "Employment_Status_ID";

	/** Set Employment Status	  */
	public void setEmployment_Status_ID (int Employment_Status_ID);

	/** Get Employment Status	  */
	public int getEmployment_Status_ID();

	public I_ZZ_Appointment_Ref getEmployment_Status() throws RuntimeException;

    /** Column name Gender_ID */
    public static final String COLUMNNAME_Gender_ID = "Gender_ID";

	/** Set Gender_ID	  */
	public void setGender_ID (int Gender_ID);

	/** Get Gender_ID	  */
	public int getGender_ID();

	public I_ZZ_Gender_Ref getGender() throws RuntimeException;

    /** Column name Highest_Qualification_Type_ID */
    public static final String COLUMNNAME_Highest_Qualification_Type_ID = "Highest_Qualification_Type_ID";

	/** Set Highest Qualification Type	  */
	public void setHighest_Qualification_Type_ID (int Highest_Qualification_Type_ID);

	/** Get Highest Qualification Type	  */
	public int getHighest_Qualification_Type_ID();

	public I_ZZ_Qualification_Type_Ref getHighest_Qualification_Type() throws RuntimeException;

    /** Column name ID_Passport_No_Employee_Number */
    public static final String COLUMNNAME_ID_Passport_No_Employee_Number = "ID_Passport_No_Employee_Number";

	/** Set ID / Passport No / Employee Number	  */
	public void setID_Passport_No_Employee_Number (String ID_Passport_No_Employee_Number);

	/** Get ID / Passport No / Employee Number	  */
	public String getID_Passport_No_Employee_Number();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Job_Title */
    public static final String COLUMNNAME_Job_Title = "Job_Title";

	/** Set Job Title	  */
	public void setJob_Title (String Job_Title);

	/** Get Job Title	  */
	public String getJob_Title();

    /** Column name Municipality_ID */
    public static final String COLUMNNAME_Municipality_ID = "Municipality_ID";

	/** Set Municipality	  */
	public void setMunicipality_ID (int Municipality_ID);

	/** Get Municipality	  */
	public int getMunicipality_ID();

	public I_ZZ_Municipality_Ref getMunicipality() throws RuntimeException;

    /** Column name Name_Surname */
    public static final String COLUMNNAME_Name_Surname = "Name_Surname";

	/** Set Name &amp;
 Surname	  */
	public void setName_Surname (String Name_Surname);

	/** Get Name &amp;
 Surname	  */
	public String getName_Surname();

    /** Column name OFO_Occupation_Code_ID */
    public static final String COLUMNNAME_OFO_Occupation_Code_ID = "OFO_Occupation_Code_ID";

	/** Set OFO Occupation Code	  */
	public void setOFO_Occupation_Code_ID (int OFO_Occupation_Code_ID);

	/** Get OFO Occupation Code	  */
	public int getOFO_Occupation_Code_ID();

	public I_ZZ_Occupations_Ref getOFO_Occupation_Code() throws RuntimeException;

    /** Column name OFO_Occupation_ID */
    public static final String COLUMNNAME_OFO_Occupation_ID = "OFO_Occupation_ID";

	/** Set OFO Occupation	  */
	public void setOFO_Occupation_ID (int OFO_Occupation_ID);

	/** Get OFO Occupation	  */
	public int getOFO_Occupation_ID();

	public I_ZZ_Occupations_Ref getOFO_Occupation() throws RuntimeException;

    /** Column name OFO_Specialisation_ID */
    public static final String COLUMNNAME_OFO_Specialisation_ID = "OFO_Specialisation_ID";

	/** Set OFO Specialisation	  */
	public void setOFO_Specialisation_ID (int OFO_Specialisation_ID);

	/** Get OFO Specialisation	  */
	public int getOFO_Specialisation_ID();

	public I_ZZ_Specializations_Ref getOFO_Specialisation() throws RuntimeException;

    /** Column name Occupational_Levels_For_Equity_Reporting_Purposes */
    public static final String COLUMNNAME_Occupational_Levels_For_Equity_Reporting_Purposes = "Occupational_Levels_For_Equity_Reporting_Purposes";

	/** Set Occupational Levels For Equity Reporting Purposes	  */
	public void setOccupational_Levels_For_Equity_Reporting_Purposes (String Occupational_Levels_For_Equity_Reporting_Purposes);

	/** Get Occupational Levels For Equity Reporting Purposes	  */
	public String getOccupational_Levels_For_Equity_Reporting_Purposes();

    /** Column name Organisation_Structure_Filter_Optional */
    public static final String COLUMNNAME_Organisation_Structure_Filter_Optional = "Organisation_Structure_Filter_Optional";

	/** Set Organisation Structure Filter (Optional)	  */
	public void setOrganisation_Structure_Filter_Optional (String Organisation_Structure_Filter_Optional);

	/** Get Organisation Structure Filter (Optional)	  */
	public String getOrganisation_Structure_Filter_Optional();

    /** Column name Post_Reference_Optional */
    public static final String COLUMNNAME_Post_Reference_Optional = "Post_Reference_Optional";

	/** Set Post Reference (Optional)	  */
	public void setPost_Reference_Optional (String Post_Reference_Optional);

	/** Get Post Reference (Optional)	  */
	public String getPost_Reference_Optional();

    /** Column name Province_ID */
    public static final String COLUMNNAME_Province_ID = "Province_ID";

	/** Set Province	  */
	public void setProvince_ID (int Province_ID);

	/** Get Province	  */
	public int getProvince_ID();

	public I_ZZ_Province_Ref getProvince() throws RuntimeException;

    /** Column name Race_ID */
    public static final String COLUMNNAME_Race_ID = "Race_ID";

	/** Set Race	  */
	public void setRace_ID (int Race_ID);

	/** Get Race	  */
	public int getRace_ID();

	public I_ZZ_Equity_Ref getRace() throws RuntimeException;

    /** Column name Row_No */
    public static final String COLUMNNAME_Row_No = "Row_No";

	/** Set Row No	  */
	public void setRow_No (int Row_No);

	/** Get Row No	  */
	public int getRow_No();

    /** Column name SA_Citizen_ID */
    public static final String COLUMNNAME_SA_Citizen_ID = "SA_Citizen_ID";

	/** Set SA Citizen?	  */
	public void setSA_Citizen_ID (int SA_Citizen_ID);

	/** Get SA Citizen?	  */
	public int getSA_Citizen_ID();

	public I_ZZ_No_Yes_Ref getSA_Citizen() throws RuntimeException;

    /** Column name Specify_field_of_Study_for_Post_School_qualifications_or_reason */
    public static final String COLUMNNAME_Specify_field_of_Study_for_Post_School_qualifications_or_reason = "Specify_field_of_Study_for_Post_School_qualifications_or_reason";

	/** Set Specify field of Study for Post-School qualifications or rea	  */
	public void setSpecify_field_of_Study_for_Post_School_qualifications_or_reason (String Specify_field_of_Study_for_Post_School_qualifications_or_reason);

	/** Get Specify field of Study for Post-School qualifications or rea	  */
	public String getSpecify_field_of_Study_for_Post_School_qualifications_or_reason();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name ZZ_WSP_ATR_Biodata_Detail_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_ID = "ZZ_WSP_ATR_Biodata_Detail_ID";

	/** Set WSP/ATR Biodata Detail.
	  * WSP/ATR Biodata Detail
	  */
	public void setZZ_WSP_ATR_Biodata_Detail_ID (int ZZ_WSP_ATR_Biodata_Detail_ID);

	/** Get WSP/ATR Biodata Detail.
	  * WSP/ATR Biodata Detail
	  */
	public int getZZ_WSP_ATR_Biodata_Detail_ID();

    /** Column name ZZ_WSP_ATR_Biodata_Detail_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Biodata_Detail_UU = "ZZ_WSP_ATR_Biodata_Detail_UU";

	/** Set ZZ_WSP_ATR_Biodata_Detail_UU	  */
	public void setZZ_WSP_ATR_Biodata_Detail_UU (String ZZ_WSP_ATR_Biodata_Detail_UU);

	/** Get ZZ_WSP_ATR_Biodata_Detail_UU	  */
	public String getZZ_WSP_ATR_Biodata_Detail_UU();

    /** Column name ZZ_WSP_ATR_Submitted_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Submitted_ID = "ZZ_WSP_ATR_Submitted_ID";

	/** Set WSP/ATR Submitted File.
	  * WSP/ATR Submitted File
	  */
	public void setZZ_WSP_ATR_Submitted_ID (int ZZ_WSP_ATR_Submitted_ID);

	/** Get WSP/ATR Submitted File.
	  * WSP/ATR Submitted File
	  */
	public int getZZ_WSP_ATR_Submitted_ID();

	public I_ZZ_WSP_ATR_Submitted getZZ_WSP_ATR_Submitted() throws RuntimeException;

    /** Column name ZZ_WSP_Employees_ID */
    public static final String COLUMNNAME_ZZ_WSP_Employees_ID = "ZZ_WSP_Employees_ID";

	/** Set ZZ_WSP_Employees.
	  * ZZ_WSP_Employees reference table
	  */
	public void setZZ_WSP_Employees_ID (int ZZ_WSP_Employees_ID);

	/** Get ZZ_WSP_Employees.
	  * ZZ_WSP_Employees reference table
	  */
	public int getZZ_WSP_Employees_ID();

	public I_ZZ_WSP_Employees getZZ_WSP_Employees() throws RuntimeException;
}
