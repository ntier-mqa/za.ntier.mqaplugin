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
package za.ntier.models;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for ZZ_SDR_Temp_Branch
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_SDR_Temp_Branch 
{

    /** TableName=ZZ_SDR_Temp_Branch */
    public static final String Table_Name = "ZZ_SDR_Temp_Branch";

    /** AD_Table_ID=1000211 */
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

    /** Column name C_BPartner_ID */
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";

	/** Set Business Partner.
	  * Identifies a Business Partner
	  */
	public void setC_BPartner_ID (int C_BPartner_ID);

	/** Get Business Partner.
	  * Identifies a Business Partner
	  */
	public int getC_BPartner_ID();

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException;

    /** Column name Cellphonenumber */
    public static final String COLUMNNAME_Cellphonenumber = "Cellphonenumber";

	/** Set Cellphonenumber	  */
	public void setCellphonenumber (String Cellphonenumber);

	/** Get Cellphonenumber	  */
	public String getCellphonenumber();

    /** Column name ContactName */
    public static final String COLUMNNAME_ContactName = "ContactName";

	/** Set Contact Name.
	  * Business Partner Contact Name
	  */
	public void setContactName (String ContactName);

	/** Get Contact Name.
	  * Business Partner Contact Name
	  */
	public String getContactName();

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

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name EMail */
    public static final String COLUMNNAME_EMail = "EMail";

	/** Set EMail Address.
	  * Electronic Mail Address
	  */
	public void setEMail (String EMail);

	/** Get EMail Address.
	  * Electronic Mail Address
	  */
	public String getEMail();

    /** Column name Help */
    public static final String COLUMNNAME_Help = "Help";

	/** Set Comment/Help.
	  * Comment or Hint
	  */
	public void setHelp (String Help);

	/** Get Comment/Help.
	  * Comment or Hint
	  */
	public String getHelp();

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

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

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

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();

    /** Column name ZZ_BranchName */
    public static final String COLUMNNAME_ZZ_BranchName = "ZZ_BranchName";

	/** Set Branch Name	  */
	public void setZZ_BranchName (String ZZ_BranchName);

	/** Get Branch Name	  */
	public String getZZ_BranchName();

    /** Column name ZZ_DocAction */
    public static final String COLUMNNAME_ZZ_DocAction = "ZZ_DocAction";

	/** Set Document Action	  */
	public void setZZ_DocAction (String ZZ_DocAction);

	/** Get Document Action	  */
	public String getZZ_DocAction();

    /** Column name ZZ_DocStatus */
    public static final String COLUMNNAME_ZZ_DocStatus = "ZZ_DocStatus";

	/** Set Document Status	  */
	public void setZZ_DocStatus (String ZZ_DocStatus);

	/** Get Document Status	  */
	public String getZZ_DocStatus();

    /** Column name ZZ_Landline_No */
    public static final String COLUMNNAME_ZZ_Landline_No = "ZZ_Landline_No";

	/** Set Landline No	  */
	public void setZZ_Landline_No (String ZZ_Landline_No);

	/** Get Landline No	  */
	public String getZZ_Landline_No();

    /** Column name ZZ_SDR_Temp_Branch_ID */
    public static final String COLUMNNAME_ZZ_SDR_Temp_Branch_ID = "ZZ_SDR_Temp_Branch_ID";

	/** Set SDR Temporary Branch	  */
	public void setZZ_SDR_Temp_Branch_ID (int ZZ_SDR_Temp_Branch_ID);

	/** Get SDR Temporary Branch	  */
	public int getZZ_SDR_Temp_Branch_ID();

    /** Column name ZZ_SDR_Temp_Branch_UU */
    public static final String COLUMNNAME_ZZ_SDR_Temp_Branch_UU = "ZZ_SDR_Temp_Branch_UU";

	/** Set ZZ_SDR_Temp_Branch_UU	  */
	public void setZZ_SDR_Temp_Branch_UU (String ZZ_SDR_Temp_Branch_UU);

	/** Get ZZ_SDR_Temp_Branch_UU	  */
	public String getZZ_SDR_Temp_Branch_UU();

    /** Column name ZZ_TempLevyNo */
    public static final String COLUMNNAME_ZZ_TempLevyNo = "ZZ_TempLevyNo";

	/** Set Temp Levy No	  */
	public void setZZ_TempLevyNo (String ZZ_TempLevyNo);

	/** Get Temp Levy No	  */
	public String getZZ_TempLevyNo();

    /** Column name ZZ_TradingAs */
    public static final String COLUMNNAME_ZZ_TradingAs = "ZZ_TradingAs";

	/** Set Trading As	  */
	public void setZZ_TradingAs (String ZZ_TradingAs);

	/** Get Trading As	  */
	public String getZZ_TradingAs();
}
