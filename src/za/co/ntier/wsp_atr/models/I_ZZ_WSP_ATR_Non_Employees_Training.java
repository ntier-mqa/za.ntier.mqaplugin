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

/** Generated Interface for ZZ_WSP_ATR_Non_Employees_Training
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Non_Employees_Training 
{

    /** TableName=ZZ_WSP_ATR_Non_Employees_Training */
    public static final String Table_Name = "ZZ_WSP_ATR_Non_Employees_Training";

    /** AD_Table_ID=1000174 */
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

    /** Column name Row_No */
    public static final String COLUMNNAME_Row_No = "Row_No";

	/** Set Row No	  */
	public void setRow_No (int Row_No);

	/** Get Row No	  */
	public int getRow_No();

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

    /** Column name ZZ_African */
    public static final String COLUMNNAME_ZZ_African = "ZZ_African";

	/** Set African	  */
	public void setZZ_African (int ZZ_African);

	/** Get African	  */
	public int getZZ_African();

    /** Column name ZZ_Coloured */
    public static final String COLUMNNAME_ZZ_Coloured = "ZZ_Coloured";

	/** Set Coloured	  */
	public void setZZ_Coloured (int ZZ_Coloured);

	/** Get Coloured	  */
	public int getZZ_Coloured();

    /** Column name ZZ_Disabled_Done */
    public static final String COLUMNNAME_ZZ_Disabled_Done = "ZZ_Disabled_Done";

	/** Set Disabled Done	  */
	public void setZZ_Disabled_Done (int ZZ_Disabled_Done);

	/** Get Disabled Done	  */
	public int getZZ_Disabled_Done();

    /** Column name ZZ_Disabled_Planned */
    public static final String COLUMNNAME_ZZ_Disabled_Planned = "ZZ_Disabled_Planned";

	/** Set Disabled Planned	  */
	public void setZZ_Disabled_Planned (int ZZ_Disabled_Planned);

	/** Get Disabled Planned	  */
	public int getZZ_Disabled_Planned();

    /** Column name ZZ_Female */
    public static final String COLUMNNAME_ZZ_Female = "ZZ_Female";

	/** Set Female	  */
	public void setZZ_Female (int ZZ_Female);

	/** Get Female	  */
	public int getZZ_Female();

    /** Column name ZZ_Indian */
    public static final String COLUMNNAME_ZZ_Indian = "ZZ_Indian";

	/** Set Indian	  */
	public void setZZ_Indian (int ZZ_Indian);

	/** Get Indian	  */
	public int getZZ_Indian();

    /** Column name ZZ_LP_Other_Done */
    public static final String COLUMNNAME_ZZ_LP_Other_Done = "ZZ_LP_Other_Done";

	/** Set LP Other Done	  */
	public void setZZ_LP_Other_Done (String ZZ_LP_Other_Done);

	/** Get LP Other Done	  */
	public String getZZ_LP_Other_Done();

    /** Column name ZZ_LP_Other_Planned */
    public static final String COLUMNNAME_ZZ_LP_Other_Planned = "ZZ_LP_Other_Planned";

	/** Set LP Other Planned	  */
	public void setZZ_LP_Other_Planned (String ZZ_LP_Other_Planned);

	/** Get LP Other Planned	  */
	public String getZZ_LP_Other_Planned();

    /** Column name ZZ_Learning_Programme_Done_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Done_ID = "ZZ_Learning_Programme_Done_ID";

	/** Set Learning Programme Done	  */
	public void setZZ_Learning_Programme_Done_ID (int ZZ_Learning_Programme_Done_ID);

	/** Get Learning Programme Done	  */
	public int getZZ_Learning_Programme_Done_ID();

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme_Done() throws RuntimeException;

    /** Column name ZZ_Learning_Programme_Planned_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Planned_ID = "ZZ_Learning_Programme_Planned_ID";

	/** Set Learning Programme Planned	  */
	public void setZZ_Learning_Programme_Planned_ID (int ZZ_Learning_Programme_Planned_ID);

	/** Get Learning Programme Planned	  */
	public int getZZ_Learning_Programme_Planned_ID();

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme_Planned() throws RuntimeException;

    /** Column name ZZ_Learning_Programme_Type_Done_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Type_Done_ID = "ZZ_Learning_Programme_Type_Done_ID";

	/** Set Learning Programme Type Done	  */
	public void setZZ_Learning_Programme_Type_Done_ID (int ZZ_Learning_Programme_Type_Done_ID);

	/** Get Learning Programme Type Done	  */
	public int getZZ_Learning_Programme_Type_Done_ID();

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type_Done() throws RuntimeException;

    /** Column name ZZ_Learning_Programme_Type_Planned_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Type_Planned_ID = "ZZ_Learning_Programme_Type_Planned_ID";

	/** Set Learning Programme Type Planned	  */
	public void setZZ_Learning_Programme_Type_Planned_ID (int ZZ_Learning_Programme_Type_Planned_ID);

	/** Get Learning Programme Type Planned	  */
	public int getZZ_Learning_Programme_Type_Planned_ID();

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type_Planned() throws RuntimeException;

    /** Column name ZZ_Male */
    public static final String COLUMNNAME_ZZ_Male = "ZZ_Male";

	/** Set Male	  */
	public void setZZ_Male (int ZZ_Male);

	/** Get Male	  */
	public int getZZ_Male();

    /** Column name ZZ_Non_Emp_Status_Done_ID */
    public static final String COLUMNNAME_ZZ_Non_Emp_Status_Done_ID = "ZZ_Non_Emp_Status_Done_ID";

	/** Set Non Emp Status Done	  */
	public void setZZ_Non_Emp_Status_Done_ID (int ZZ_Non_Emp_Status_Done_ID);

	/** Get Non Emp Status Done	  */
	public int getZZ_Non_Emp_Status_Done_ID();

	public I_ZZ_WSP_Non_Employee_Status_Ref getZZ_Non_Emp_Status_Done() throws RuntimeException;

    /** Column name ZZ_Non_Emp_Status_Planned_ID */
    public static final String COLUMNNAME_ZZ_Non_Emp_Status_Planned_ID = "ZZ_Non_Emp_Status_Planned_ID";

	/** Set Non Emp Status Planned	  */
	public void setZZ_Non_Emp_Status_Planned_ID (int ZZ_Non_Emp_Status_Planned_ID);

	/** Get Non Emp Status Planned	  */
	public int getZZ_Non_Emp_Status_Planned_ID();

	public I_ZZ_WSP_Non_Employee_Status_Ref getZZ_Non_Emp_Status_Planned() throws RuntimeException;

    /** Column name ZZ_Target_Ben_Done_ID */
    public static final String COLUMNNAME_ZZ_Target_Ben_Done_ID = "ZZ_Target_Ben_Done_ID";

	/** Set Target Ben Done	  */
	public void setZZ_Target_Ben_Done_ID (int ZZ_Target_Ben_Done_ID);

	/** Get Target Ben Done	  */
	public int getZZ_Target_Ben_Done_ID();

	public I_ZZ_Target_Beneficiary_Ref getZZ_Target_Ben_Done() throws RuntimeException;

    /** Column name ZZ_Target_Ben_Planned_ID */
    public static final String COLUMNNAME_ZZ_Target_Ben_Planned_ID = "ZZ_Target_Ben_Planned_ID";

	/** Set Target Ben Planned	  */
	public void setZZ_Target_Ben_Planned_ID (int ZZ_Target_Ben_Planned_ID);

	/** Get Target Ben Planned	  */
	public int getZZ_Target_Ben_Planned_ID();

	public I_ZZ_Target_Beneficiary_Ref getZZ_Target_Ben_Planned() throws RuntimeException;

    /** Column name ZZ_Total_Done */
    public static final String COLUMNNAME_ZZ_Total_Done = "ZZ_Total_Done";

	/** Set Total Done	  */
	public void setZZ_Total_Done (int ZZ_Total_Done);

	/** Get Total Done	  */
	public int getZZ_Total_Done();

    /** Column name ZZ_Total_Planned */
    public static final String COLUMNNAME_ZZ_Total_Planned = "ZZ_Total_Planned";

	/** Set Total Planned	  */
	public void setZZ_Total_Planned (int ZZ_Total_Planned);

	/** Get Total Planned	  */
	public int getZZ_Total_Planned();

    /** Column name ZZ_WSP_ATR_Non_Employees_Training_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Non_Employees_Training_ID = "ZZ_WSP_ATR_Non_Employees_Training_ID";

	/** Set WSP ATR Non Employees Training	  */
	public void setZZ_WSP_ATR_Non_Employees_Training_ID (int ZZ_WSP_ATR_Non_Employees_Training_ID);

	/** Get WSP ATR Non Employees Training	  */
	public int getZZ_WSP_ATR_Non_Employees_Training_ID();

    /** Column name ZZ_WSP_ATR_Non_Employees_Training_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Non_Employees_Training_UU = "ZZ_WSP_ATR_Non_Employees_Training_UU";

	/** Set ZZ_WSP_ATR_Non_Employees_Training_UU	  */
	public void setZZ_WSP_ATR_Non_Employees_Training_UU (String ZZ_WSP_ATR_Non_Employees_Training_UU);

	/** Get ZZ_WSP_ATR_Non_Employees_Training_UU	  */
	public String getZZ_WSP_ATR_Non_Employees_Training_UU();

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

    /** Column name ZZ_White */
    public static final String COLUMNNAME_ZZ_White = "ZZ_White";

	/** Set White	  */
	public void setZZ_White (int ZZ_White);

	/** Get White	  */
	public int getZZ_White();
}
