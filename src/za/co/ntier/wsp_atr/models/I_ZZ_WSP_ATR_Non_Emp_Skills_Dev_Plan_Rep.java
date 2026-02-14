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

/** Generated Interface for ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep 
{

    /** TableName=ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep */
    public static final String Table_Name = "ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep";

    /** AD_Table_ID=1000206 */
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

    /** Column name ZZ_Disabled_Planned */
    public static final String COLUMNNAME_ZZ_Disabled_Planned = "ZZ_Disabled_Planned";

	/** Set Disabled Planned	  */
	public void setZZ_Disabled_Planned (int ZZ_Disabled_Planned);

	/** Get Disabled Planned	  */
	public int getZZ_Disabled_Planned();

    /** Column name ZZ_Learning_Programme_Planned_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Planned_ID = "ZZ_Learning_Programme_Planned_ID";

	/** Set Learning Programme Planned	  */
	public void setZZ_Learning_Programme_Planned_ID (int ZZ_Learning_Programme_Planned_ID);

	/** Get Learning Programme Planned	  */
	public int getZZ_Learning_Programme_Planned_ID();

	public I_ZZ_Qualification_Type_Details_Ref getZZ_Learning_Programme_Planned() throws RuntimeException;

    /** Column name ZZ_Learning_Programme_Type_Planned_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Type_Planned_ID = "ZZ_Learning_Programme_Type_Planned_ID";

	/** Set Learning Programme Type Planned	  */
	public void setZZ_Learning_Programme_Type_Planned_ID (int ZZ_Learning_Programme_Type_Planned_ID);

	/** Get Learning Programme Type Planned	  */
	public int getZZ_Learning_Programme_Type_Planned_ID();

	public I_ZZ_Learning_Programme_Ref getZZ_Learning_Programme_Type_Planned() throws RuntimeException;

    /** Column name ZZ_Target_Ben_Planned_ID */
    public static final String COLUMNNAME_ZZ_Target_Ben_Planned_ID = "ZZ_Target_Ben_Planned_ID";

	/** Set Target Ben Planned	  */
	public void setZZ_Target_Ben_Planned_ID (int ZZ_Target_Ben_Planned_ID);

	/** Get Target Ben Planned	  */
	public int getZZ_Target_Ben_Planned_ID();

	public I_ZZ_Target_Beneficiary_Ref getZZ_Target_Ben_Planned() throws RuntimeException;

    /** Column name ZZ_Total_Planned */
    public static final String COLUMNNAME_ZZ_Total_Planned = "ZZ_Total_Planned";

	/** Set Total Planned	  */
	public void setZZ_Total_Planned (int ZZ_Total_Planned);

	/** Get Total Planned	  */
	public int getZZ_Total_Planned();

    /** Column name ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID = "ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID";

	/** Set Skills Development  Planned	  */
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID (int ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID);

	/** Get Skills Development  Planned	  */
	public int getZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_ID();

    /** Column name ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU = "ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU";

	/** Set ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU	  */
	public void setZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU (String ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU);

	/** Get ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU	  */
	public String getZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep_UU();

    /** Column name ZZ_WSP_ATR_Report_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Report_ID = "ZZ_WSP_ATR_Report_ID";

	/** Set WSP ATR Report 	  */
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID);

	/** Get WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID();

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException;
}
