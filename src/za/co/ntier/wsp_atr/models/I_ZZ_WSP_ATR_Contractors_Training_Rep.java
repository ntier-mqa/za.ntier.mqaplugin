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

/** Generated Interface for ZZ_WSP_ATR_Contractors_Training_Rep
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Contractors_Training_Rep 
{

    /** TableName=ZZ_WSP_ATR_Contractors_Training_Rep */
    public static final String Table_Name = "ZZ_WSP_ATR_Contractors_Training_Rep";

    /** AD_Table_ID=1000207 */
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

    /** Column name ZZ_Clerical_Planned */
    public static final String COLUMNNAME_ZZ_Clerical_Planned = "ZZ_Clerical_Planned";

	/** Set Clerical Planned	  */
	public void setZZ_Clerical_Planned (int ZZ_Clerical_Planned);

	/** Get Clerical Planned	  */
	public int getZZ_Clerical_Planned();

    /** Column name ZZ_Clerical_Trained */
    public static final String COLUMNNAME_ZZ_Clerical_Trained = "ZZ_Clerical_Trained";

	/** Set Clerical Trained	  */
	public void setZZ_Clerical_Trained (int ZZ_Clerical_Trained);

	/** Get Clerical Trained	  */
	public int getZZ_Clerical_Trained();

    /** Column name ZZ_Elementary_Planned */
    public static final String COLUMNNAME_ZZ_Elementary_Planned = "ZZ_Elementary_Planned";

	/** Set Elementary Planned	  */
	public void setZZ_Elementary_Planned (int ZZ_Elementary_Planned);

	/** Get Elementary Planned	  */
	public int getZZ_Elementary_Planned();

    /** Column name ZZ_Elementary_Trained */
    public static final String COLUMNNAME_ZZ_Elementary_Trained = "ZZ_Elementary_Trained";

	/** Set Elementary Trained	  */
	public void setZZ_Elementary_Trained (int ZZ_Elementary_Trained);

	/** Get Elementary Trained	  */
	public int getZZ_Elementary_Trained();

    /** Column name ZZ_Learners_Planned */
    public static final String COLUMNNAME_ZZ_Learners_Planned = "ZZ_Learners_Planned";

	/** Set Learners Planned	  */
	public void setZZ_Learners_Planned (int ZZ_Learners_Planned);

	/** Get Learners Planned	  */
	public int getZZ_Learners_Planned();

    /** Column name ZZ_Learners_Trained */
    public static final String COLUMNNAME_ZZ_Learners_Trained = "ZZ_Learners_Trained";

	/** Set Learners Trained	  */
	public void setZZ_Learners_Trained (int ZZ_Learners_Trained);

	/** Get Learners Trained	  */
	public int getZZ_Learners_Trained();

    /** Column name ZZ_Learning_Programme_Type_ID */
    public static final String COLUMNNAME_ZZ_Learning_Programme_Type_ID = "ZZ_Learning_Programme_Type_ID";

	/** Set Learning Programme Type	  */
	public void setZZ_Learning_Programme_Type_ID (int ZZ_Learning_Programme_Type_ID);

	/** Get Learning Programme Type	  */
	public int getZZ_Learning_Programme_Type_ID();

	public I_ZZ_Contractors_Learning_Programme_Ref getZZ_Learning_Programme_Type() throws RuntimeException;

    /** Column name ZZ_Managers_Planned */
    public static final String COLUMNNAME_ZZ_Managers_Planned = "ZZ_Managers_Planned";

	/** Set Managers Planned	  */
	public void setZZ_Managers_Planned (int ZZ_Managers_Planned);

	/** Get Managers Planned	  */
	public int getZZ_Managers_Planned();

    /** Column name ZZ_Managers_Trained */
    public static final String COLUMNNAME_ZZ_Managers_Trained = "ZZ_Managers_Trained";

	/** Set Managers Trained	  */
	public void setZZ_Managers_Trained (int ZZ_Managers_Trained);

	/** Get Managers Trained	  */
	public int getZZ_Managers_Trained();

    /** Column name ZZ_Plant_Planned */
    public static final String COLUMNNAME_ZZ_Plant_Planned = "ZZ_Plant_Planned";

	/** Set Plant Planned	  */
	public void setZZ_Plant_Planned (int ZZ_Plant_Planned);

	/** Get Plant Planned	  */
	public int getZZ_Plant_Planned();

    /** Column name ZZ_Plant_Trained */
    public static final String COLUMNNAME_ZZ_Plant_Trained = "ZZ_Plant_Trained";

	/** Set Plant Trained	  */
	public void setZZ_Plant_Trained (int ZZ_Plant_Trained);

	/** Get Plant Trained	  */
	public int getZZ_Plant_Trained();

    /** Column name ZZ_Professionals_Planned */
    public static final String COLUMNNAME_ZZ_Professionals_Planned = "ZZ_Professionals_Planned";

	/** Set Professionals Planned	  */
	public void setZZ_Professionals_Planned (int ZZ_Professionals_Planned);

	/** Get Professionals Planned	  */
	public int getZZ_Professionals_Planned();

    /** Column name ZZ_Professionals_Trained */
    public static final String COLUMNNAME_ZZ_Professionals_Trained = "ZZ_Professionals_Trained";

	/** Set Professionals Trained	  */
	public void setZZ_Professionals_Trained (int ZZ_Professionals_Trained);

	/** Get Professionals Trained	  */
	public int getZZ_Professionals_Trained();

    /** Column name ZZ_Service_Planned */
    public static final String COLUMNNAME_ZZ_Service_Planned = "ZZ_Service_Planned";

	/** Set Service Planned	  */
	public void setZZ_Service_Planned (int ZZ_Service_Planned);

	/** Get Service Planned	  */
	public int getZZ_Service_Planned();

    /** Column name ZZ_Service_Trained */
    public static final String COLUMNNAME_ZZ_Service_Trained = "ZZ_Service_Trained";

	/** Set Service Trained	  */
	public void setZZ_Service_Trained (int ZZ_Service_Trained);

	/** Get Service Trained	  */
	public int getZZ_Service_Trained();

    /** Column name ZZ_Skilled_Workers_Planned */
    public static final String COLUMNNAME_ZZ_Skilled_Workers_Planned = "ZZ_Skilled_Workers_Planned";

	/** Set Skilled Workers Planned	  */
	public void setZZ_Skilled_Workers_Planned (int ZZ_Skilled_Workers_Planned);

	/** Get Skilled Workers Planned	  */
	public int getZZ_Skilled_Workers_Planned();

    /** Column name ZZ_Skilled_Workers_Trained */
    public static final String COLUMNNAME_ZZ_Skilled_Workers_Trained = "ZZ_Skilled_Workers_Trained";

	/** Set Skilled Workers Trained	  */
	public void setZZ_Skilled_Workers_Trained (int ZZ_Skilled_Workers_Trained);

	/** Get Skilled Workers Trained	  */
	public int getZZ_Skilled_Workers_Trained();

    /** Column name ZZ_Technicians_Planned */
    public static final String COLUMNNAME_ZZ_Technicians_Planned = "ZZ_Technicians_Planned";

	/** Set Technicians Planned	  */
	public void setZZ_Technicians_Planned (int ZZ_Technicians_Planned);

	/** Get Technicians Planned	  */
	public int getZZ_Technicians_Planned();

    /** Column name ZZ_Technicians_Trained */
    public static final String COLUMNNAME_ZZ_Technicians_Trained = "ZZ_Technicians_Trained";

	/** Set Technicians Trained	  */
	public void setZZ_Technicians_Trained (int ZZ_Technicians_Trained);

	/** Get Technicians Trained	  */
	public int getZZ_Technicians_Trained();

    /** Column name ZZ_Total_Trained */
    public static final String COLUMNNAME_ZZ_Total_Trained = "ZZ_Total_Trained";

	/** Set Total Trained	  */
	public void setZZ_Total_Trained (int ZZ_Total_Trained);

	/** Get Total Trained	  */
	public int getZZ_Total_Trained();

    /** Column name ZZ_WSP_ATR_Contractors_Training_Rep_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_ID = "ZZ_WSP_ATR_Contractors_Training_Rep_ID";

	/** Set Contractors: Received Training and Planned Training	  */
	public void setZZ_WSP_ATR_Contractors_Training_Rep_ID (int ZZ_WSP_ATR_Contractors_Training_Rep_ID);

	/** Get Contractors: Received Training and Planned Training	  */
	public int getZZ_WSP_ATR_Contractors_Training_Rep_ID();

    /** Column name ZZ_WSP_ATR_Contractors_Training_Rep_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_UU = "ZZ_WSP_ATR_Contractors_Training_Rep_UU";

	/** Set ZZ_WSP_ATR_Contractors_Training_Rep_UU	  */
	public void setZZ_WSP_ATR_Contractors_Training_Rep_UU (String ZZ_WSP_ATR_Contractors_Training_Rep_UU);

	/** Get ZZ_WSP_ATR_Contractors_Training_Rep_UU	  */
	public String getZZ_WSP_ATR_Contractors_Training_Rep_UU();

    /** Column name ZZ_WSP_ATR_Report_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Report_ID = "ZZ_WSP_ATR_Report_ID";

	/** Set WSP ATR Report 	  */
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID);

	/** Get WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID();

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException;
}
