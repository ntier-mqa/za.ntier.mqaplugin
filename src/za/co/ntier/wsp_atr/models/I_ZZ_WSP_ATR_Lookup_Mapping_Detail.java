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

/** Generated Interface for ZZ_WSP_ATR_Lookup_Mapping_Detail
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Lookup_Mapping_Detail 
{

    /** TableName=ZZ_WSP_ATR_Lookup_Mapping_Detail */
    public static final String Table_Name = "ZZ_WSP_ATR_Lookup_Mapping_Detail";

    /** AD_Table_ID=1000149 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 4 - System 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(4);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Column_ID */
    public static final String COLUMNNAME_AD_Column_ID = "AD_Column_ID";

	/** Set Column.
	  * Column in the table
	  */
	public void setAD_Column_ID (int AD_Column_ID);

	/** Get Column.
	  * Column in the table
	  */
	public int getAD_Column_ID();

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException;

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

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

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

    /** Column name Ignore_If_Blank */
    public static final String COLUMNNAME_Ignore_If_Blank = "Ignore_If_Blank";

	/** Set Ignore If Blank	  */
	public void setIgnore_If_Blank (boolean Ignore_If_Blank);

	/** Get Ignore If Blank	  */
	public boolean isIgnore_If_Blank();

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

    /** Column name IsMandatory */
    public static final String COLUMNNAME_IsMandatory = "IsMandatory";

	/** Set Mandatory.
	  * Data entry is required in this column
	  */
	public void setIsMandatory (boolean IsMandatory);

	/** Get Mandatory.
	  * Data entry is required in this column
	  */
	public boolean isMandatory();

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

    /** Column name ZZ_Column_Letter */
    public static final String COLUMNNAME_ZZ_Column_Letter = "ZZ_Column_Letter";

	/** Set Column Letter	  */
	public void setZZ_Column_Letter (String ZZ_Column_Letter);

	/** Get Column Letter	  */
	public String getZZ_Column_Letter();

    /** Column name ZZ_Create_If_Not_Exists */
    public static final String COLUMNNAME_ZZ_Create_If_Not_Exists = "ZZ_Create_If_Not_Exists";

	/** Set Create If Not Exists	  */
	public void setZZ_Create_If_Not_Exists (boolean ZZ_Create_If_Not_Exists);

	/** Get Create If Not Exists	  */
	public boolean isZZ_Create_If_Not_Exists();

    /** Column name ZZ_Header_Name */
    public static final String COLUMNNAME_ZZ_Header_Name = "ZZ_Header_Name";

	/** Set Header Name	  */
	public void setZZ_Header_Name (String ZZ_Header_Name);

	/** Get Header Name	  */
	public String getZZ_Header_Name();

    /** Column name ZZ_Name_Column_Letter */
    public static final String COLUMNNAME_ZZ_Name_Column_Letter = "ZZ_Name_Column_Letter";

	/** Set Name Column Letter	  */
	public void setZZ_Name_Column_Letter (String ZZ_Name_Column_Letter);

	/** Get Name Column Letter	  */
	public String getZZ_Name_Column_Letter();

    /** Column name ZZ_Row_No */
    public static final String COLUMNNAME_ZZ_Row_No = "ZZ_Row_No";

	/** Set Row No	  */
	public void setZZ_Row_No (int ZZ_Row_No);

	/** Get Row No	  */
	public int getZZ_Row_No();

    /** Column name ZZ_Use_Value */
    public static final String COLUMNNAME_ZZ_Use_Value = "ZZ_Use_Value";

	/** Set Use Value for Validation	  */
	public void setZZ_Use_Value (boolean ZZ_Use_Value);

	/** Get Use Value for Validation	  */
	public boolean isZZ_Use_Value();

    /** Column name ZZ_Value_Column_Letter */
    public static final String COLUMNNAME_ZZ_Value_Column_Letter = "ZZ_Value_Column_Letter";

	/** Set Value Column Letter	  */
	public void setZZ_Value_Column_Letter (String ZZ_Value_Column_Letter);

	/** Get Value Column Letter	  */
	public String getZZ_Value_Column_Letter();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_Detail_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_ID = "ZZ_WSP_ATR_Lookup_Mapping_Detail_ID";

	/** Set ZZ WSP ATR Lookup Mapping Detail	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_Detail_ID (int ZZ_WSP_ATR_Lookup_Mapping_Detail_ID);

	/** Get ZZ WSP ATR Lookup Mapping Detail	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_Detail_ID();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_Detail_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_Detail_UU = "ZZ_WSP_ATR_Lookup_Mapping_Detail_UU";

	/** Set ZZ_WSP_ATR_Lookup_Mapping_Detail_UU	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_Detail_UU (String ZZ_WSP_ATR_Lookup_Mapping_Detail_UU);

	/** Get ZZ_WSP_ATR_Lookup_Mapping_Detail_UU	  */
	public String getZZ_WSP_ATR_Lookup_Mapping_Detail_UU();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID = "ZZ_WSP_ATR_Lookup_Mapping_ID";

	/** Set WSP ATR Lookup Mapping	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_ID (int ZZ_WSP_ATR_Lookup_Mapping_ID);

	/** Get WSP ATR Lookup Mapping	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_ID();

	public I_ZZ_WSP_ATR_Lookup_Mapping getZZ_WSP_ATR_Lookup_Mapping() throws RuntimeException;
}
