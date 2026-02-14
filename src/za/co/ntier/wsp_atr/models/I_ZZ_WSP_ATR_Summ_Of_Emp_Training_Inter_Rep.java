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

/** Generated Interface for ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep 
{

    /** TableName=ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep */
    public static final String Table_Name = "ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep";

    /** AD_Table_ID=1000200 */
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

    /** Column name Learning_Programme_Detail_ID */
    public static final String COLUMNNAME_Learning_Programme_Detail_ID = "Learning_Programme_Detail_ID";

	/** Set Learning_Programme_Detail_ID	  */
	public void setLearning_Programme_Detail_ID (int Learning_Programme_Detail_ID);

	/** Get Learning_Programme_Detail_ID	  */
	public int getLearning_Programme_Detail_ID();

	public I_ZZ_Qualification_Type_Details_Ref getLearning_Programme_Detail() throws RuntimeException;

    /** Column name Qualification_Type_ID */
    public static final String COLUMNNAME_Qualification_Type_ID = "Qualification_Type_ID";

	/** Set Qualification_Type_ID	  */
	public void setQualification_Type_ID (int Qualification_Type_ID);

	/** Get Qualification_Type_ID	  */
	public int getQualification_Type_ID();

	public I_ZZ_Learning_Programme_Ref getQualification_Type() throws RuntimeException;

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

    /** Column name ZZ_African_Female_Cnt */
    public static final String COLUMNNAME_ZZ_African_Female_Cnt = "ZZ_African_Female_Cnt";

	/** Set African Female Count.
	  * African Female Count
	  */
	public void setZZ_African_Female_Cnt (int ZZ_African_Female_Cnt);

	/** Get African Female Count.
	  * African Female Count
	  */
	public int getZZ_African_Female_Cnt();

    /** Column name ZZ_African_Male_Cnt */
    public static final String COLUMNNAME_ZZ_African_Male_Cnt = "ZZ_African_Male_Cnt";

	/** Set African Male Count.
	  * African Male Count
	  */
	public void setZZ_African_Male_Cnt (int ZZ_African_Male_Cnt);

	/** Get African Male Count.
	  * African Male Count
	  */
	public int getZZ_African_Male_Cnt();

    /** Column name ZZ_Age_35_55_Cnt */
    public static final String COLUMNNAME_ZZ_Age_35_55_Cnt = "ZZ_Age_35_55_Cnt";

	/** Set Age 35-55 Count.
	  * Age 35-55 Count
	  */
	public void setZZ_Age_35_55_Cnt (int ZZ_Age_35_55_Cnt);

	/** Get Age 35-55 Count.
	  * Age 35-55 Count
	  */
	public int getZZ_Age_35_55_Cnt();

    /** Column name ZZ_Age_O55_Cnt */
    public static final String COLUMNNAME_ZZ_Age_O55_Cnt = "ZZ_Age_O55_Cnt";

	/** Set Age &gt;
55 Count.
	  * Age &gt;
55 Count
	  */
	public void setZZ_Age_O55_Cnt (int ZZ_Age_O55_Cnt);

	/** Get Age &gt;
55 Count.
	  * Age &gt;
55 Count
	  */
	public int getZZ_Age_O55_Cnt();

    /** Column name ZZ_Age_U35_Cnt */
    public static final String COLUMNNAME_ZZ_Age_U35_Cnt = "ZZ_Age_U35_Cnt";

	/** Set Age &lt;
35 Count.
	  * Age &lt;
35 Count
	  */
	public void setZZ_Age_U35_Cnt (int ZZ_Age_U35_Cnt);

	/** Get Age &lt;
35 Count.
	  * Age &lt;
35 Count
	  */
	public int getZZ_Age_U35_Cnt();

    /** Column name ZZ_Coloured_Female_Cnt */
    public static final String COLUMNNAME_ZZ_Coloured_Female_Cnt = "ZZ_Coloured_Female_Cnt";

	/** Set Coloured Female Count.
	  * Coloured Female Count
	  */
	public void setZZ_Coloured_Female_Cnt (int ZZ_Coloured_Female_Cnt);

	/** Get Coloured Female Count.
	  * Coloured Female Count
	  */
	public int getZZ_Coloured_Female_Cnt();

    /** Column name ZZ_Coloured_Male_Cnt */
    public static final String COLUMNNAME_ZZ_Coloured_Male_Cnt = "ZZ_Coloured_Male_Cnt";

	/** Set Coloured Male Count.
	  * Coloured Male Count
	  */
	public void setZZ_Coloured_Male_Cnt (int ZZ_Coloured_Male_Cnt);

	/** Get Coloured Male Count.
	  * Coloured Male Count
	  */
	public int getZZ_Coloured_Male_Cnt();

    /** Column name ZZ_Disabled_Cnt */
    public static final String COLUMNNAME_ZZ_Disabled_Cnt = "ZZ_Disabled_Cnt";

	/** Set Disabled Count.
	  * Disabled Count
	  */
	public void setZZ_Disabled_Cnt (int ZZ_Disabled_Cnt);

	/** Get Disabled Count.
	  * Disabled Count
	  */
	public int getZZ_Disabled_Cnt();

    /** Column name ZZ_Indian_Female_Cnt */
    public static final String COLUMNNAME_ZZ_Indian_Female_Cnt = "ZZ_Indian_Female_Cnt";

	/** Set Indian Female Count.
	  * Indian Female Count
	  */
	public void setZZ_Indian_Female_Cnt (int ZZ_Indian_Female_Cnt);

	/** Get Indian Female Count.
	  * Indian Female Count
	  */
	public int getZZ_Indian_Female_Cnt();

    /** Column name ZZ_Indian_Male_Cnt */
    public static final String COLUMNNAME_ZZ_Indian_Male_Cnt = "ZZ_Indian_Male_Cnt";

	/** Set Indian Male Count.
	  * Indian Male Count
	  */
	public void setZZ_Indian_Male_Cnt (int ZZ_Indian_Male_Cnt);

	/** Get Indian Male Count.
	  * Indian Male Count
	  */
	public int getZZ_Indian_Male_Cnt();

    /** Column name ZZ_NonSA_Cnt */
    public static final String COLUMNNAME_ZZ_NonSA_Cnt = "ZZ_NonSA_Cnt";

	/** Set Non-SA Count.
	  * Non-SA Count
	  */
	public void setZZ_NonSA_Cnt (int ZZ_NonSA_Cnt);

	/** Get Non-SA Count.
	  * Non-SA Count
	  */
	public int getZZ_NonSA_Cnt();

    /** Column name ZZ_Total_Female_Cnt */
    public static final String COLUMNNAME_ZZ_Total_Female_Cnt = "ZZ_Total_Female_Cnt";

	/** Set Total Female Count.
	  * Total Female Count
	  */
	public void setZZ_Total_Female_Cnt (int ZZ_Total_Female_Cnt);

	/** Get Total Female Count.
	  * Total Female Count
	  */
	public int getZZ_Total_Female_Cnt();

    /** Column name ZZ_Total_Male_Cnt */
    public static final String COLUMNNAME_ZZ_Total_Male_Cnt = "ZZ_Total_Male_Cnt";

	/** Set Total Male Count.
	  * Total Male Count
	  */
	public void setZZ_Total_Male_Cnt (int ZZ_Total_Male_Cnt);

	/** Get Total Male Count.
	  * Total Male Count
	  */
	public int getZZ_Total_Male_Cnt();

    /** Column name ZZ_WSP_ATR_Report_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Report_ID = "ZZ_WSP_ATR_Report_ID";

	/** Set WSP ATR Report 	  */
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID);

	/** Get WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID();

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException;

    /** Column name ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID = "ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID";

	/** Set Summary of employee training interventions for the period	  */
	public void setZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID (int ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID);

	/** Get Summary of employee training interventions for the period	  */
	public int getZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_ID();

    /** Column name ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU = "ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU";

	/** Set ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU	  */
	public void setZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU (String ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU);

	/** Get ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU	  */
	public String getZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep_UU();

    /** Column name ZZ_White_Female_Cnt */
    public static final String COLUMNNAME_ZZ_White_Female_Cnt = "ZZ_White_Female_Cnt";

	/** Set White Female Count.
	  * White Female Count
	  */
	public void setZZ_White_Female_Cnt (int ZZ_White_Female_Cnt);

	/** Get White Female Count.
	  * White Female Count
	  */
	public int getZZ_White_Female_Cnt();

    /** Column name ZZ_White_Male_Cnt */
    public static final String COLUMNNAME_ZZ_White_Male_Cnt = "ZZ_White_Male_Cnt";

	/** Set White Male Count.
	  * White Male Count
	  */
	public void setZZ_White_Male_Cnt (int ZZ_White_Male_Cnt);

	/** Get White Male Count.
	  * White Male Count
	  */
	public int getZZ_White_Male_Cnt();
}
