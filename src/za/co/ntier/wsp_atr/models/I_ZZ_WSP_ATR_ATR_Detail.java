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

/** Generated Interface for ZZ_WSP_ATR_ATR_Detail
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_ATR_Detail 
{

    /** TableName=ZZ_WSP_ATR_ATR_Detail */
    public static final String Table_Name = "ZZ_WSP_ATR_ATR_Detail";

    /** AD_Table_ID=1000166 */
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

    /** Column name Dropout_Reason_ID */
    public static final String COLUMNNAME_Dropout_Reason_ID = "Dropout_Reason_ID";

	/** Set Dropout_Reason_ID	  */
	public void setDropout_Reason_ID (int Dropout_Reason_ID);

	/** Get Dropout_Reason_ID	  */
	public int getDropout_Reason_ID();

	public I_ZZ_Not_Achieved_Ref getDropout_Reason() throws RuntimeException;

    /** Column name Employee_Name */
    public static final String COLUMNNAME_Employee_Name = "Employee_Name";

	/** Set Employee_Name	  */
	public void setEmployee_Name (String Employee_Name);

	/** Get Employee_Name	  */
	public String getEmployee_Name();

    /** Column name Employee_Number_ID */
    public static final String COLUMNNAME_Employee_Number_ID = "Employee_Number_ID";

	/** Set Employee_Number_ID	  */
	public void setEmployee_Number_ID (int Employee_Number_ID);

	/** Get Employee_Number_ID	  */
	public int getEmployee_Number_ID();

	public I_ZZ_WSP_Employees getEmployee_Number() throws RuntimeException;

    /** Column name Field_of_Study_Specify_ID */
    public static final String COLUMNNAME_Field_of_Study_Specify_ID = "Field_of_Study_Specify_ID";

	/** Set Field_of_Study_Specify_ID	  */
	public void setField_of_Study_Specify_ID (int Field_of_Study_Specify_ID);

	/** Get Field_of_Study_Specify_ID	  */
	public int getField_of_Study_Specify_ID();

	public I_ZZ_Field_of_Study_Specify_Ref getField_of_Study_Specify() throws RuntimeException;

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

    /** Column name Learning_Programme_Detail_ID */
    public static final String COLUMNNAME_Learning_Programme_Detail_ID = "Learning_Programme_Detail_ID";

	/** Set Learning_Programme_Detail_ID	  */
	public void setLearning_Programme_Detail_ID (int Learning_Programme_Detail_ID);

	/** Get Learning_Programme_Detail_ID	  */
	public int getLearning_Programme_Detail_ID();

	public I_ZZ_Qualification_Type_Details_Ref getLearning_Programme_Detail() throws RuntimeException;

    /** Column name Qualification */
    public static final String COLUMNNAME_Qualification = "Qualification";

	/** Set Qualification	  */
	public void setQualification (String Qualification);

	/** Get Qualification	  */
	public String getQualification();

    /** Column name Qualification_Type_ID */
    public static final String COLUMNNAME_Qualification_Type_ID = "Qualification_Type_ID";

	/** Set Qualification_Type_ID	  */
	public void setQualification_Type_ID (int Qualification_Type_ID);

	/** Get Qualification_Type_ID	  */
	public int getQualification_Type_ID();

	public I_ZZ_Learning_Programme_Ref getQualification_Type() throws RuntimeException;

    /** Column name Row_No */
    public static final String COLUMNNAME_Row_No = "Row_No";

	/** Set Row No	  */
	public void setRow_No (int Row_No);

	/** Get Row No	  */
	public int getRow_No();

    /** Column name Total_Training_Cost */
    public static final String COLUMNNAME_Total_Training_Cost = "Total_Training_Cost";

	/** Set Total_Training_Cost	  */
	public void setTotal_Training_Cost (BigDecimal Total_Training_Cost);

	/** Get Total_Training_Cost	  */
	public BigDecimal getTotal_Training_Cost();

    /** Column name Training_Status_ID */
    public static final String COLUMNNAME_Training_Status_ID = "Training_Status_ID";

	/** Set Training_Status_ID	  */
	public void setTraining_Status_ID (int Training_Status_ID);

	/** Get Training_Status_ID	  */
	public int getTraining_Status_ID();

	public I_ZZ_Achievement_Status_Ref getTraining_Status() throws RuntimeException;

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

    /** Column name Year_Completed */
    public static final String COLUMNNAME_Year_Completed = "Year_Completed";

	/** Set Year_Completed	  */
	public void setYear_Completed (String Year_Completed);

	/** Get Year_Completed	  */
	public String getYear_Completed();

    /** Column name Year_Enrolled */
    public static final String COLUMNNAME_Year_Enrolled = "Year_Enrolled";

	/** Set Year_Enrolled	  */
	public void setYear_Enrolled (String Year_Enrolled);

	/** Get Year_Enrolled	  */
	public String getYear_Enrolled();

    /** Column name ZZ_WSP_ATR_ATR_Detail_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_ID = "ZZ_WSP_ATR_ATR_Detail_ID";

	/** Set WSP/ATR ATR Detail.
	  * WSP/ATR ATR Detail
	  */
	public void setZZ_WSP_ATR_ATR_Detail_ID (int ZZ_WSP_ATR_ATR_Detail_ID);

	/** Get WSP/ATR ATR Detail.
	  * WSP/ATR ATR Detail
	  */
	public int getZZ_WSP_ATR_ATR_Detail_ID();

    /** Column name ZZ_WSP_ATR_ATR_Detail_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_ATR_Detail_UU = "ZZ_WSP_ATR_ATR_Detail_UU";

	/** Set ZZ_WSP_ATR_ATR_Detail_UU	  */
	public void setZZ_WSP_ATR_ATR_Detail_UU (String ZZ_WSP_ATR_ATR_Detail_UU);

	/** Get ZZ_WSP_ATR_ATR_Detail_UU	  */
	public String getZZ_WSP_ATR_ATR_Detail_UU();

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
