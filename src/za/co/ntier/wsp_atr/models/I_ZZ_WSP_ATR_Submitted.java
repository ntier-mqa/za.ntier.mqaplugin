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

/** Generated Interface for ZZ_WSP_ATR_Submitted
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Submitted 
{

    /** TableName=ZZ_WSP_ATR_Submitted */
    public static final String Table_Name = "ZZ_WSP_ATR_Submitted";

    /** AD_Table_ID=1000163 */
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

    /** Column name FileName */
    public static final String COLUMNNAME_FileName = "FileName";

	/** Set File Name.
	  * Name of the local file or URL
	  */
	public void setFileName (String FileName);

	/** Get File Name.
	  * Name of the local file or URL
	  */
	public String getFileName();

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

    /** Column name SubmittedDate */
    public static final String COLUMNNAME_SubmittedDate = "SubmittedDate";

	/** Set Submitted Date	  */
	public void setSubmittedDate (Timestamp SubmittedDate);

	/** Get Submitted Date	  */
	public Timestamp getSubmittedDate();

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

    /** Column name ZZSdfOrganisation_ID */
    public static final String COLUMNNAME_ZZSdfOrganisation_ID = "ZZSdfOrganisation_ID";

	/** Set SDF Organisation.
	  * Link Organisation And SDF
	  */
	public void setZZSdfOrganisation_ID (int ZZSdfOrganisation_ID);

	/** Get SDF Organisation.
	  * Link Organisation And SDF
	  */
	public int getZZSdfOrganisation_ID();

    /** Column name ZZ_Import_Submitted_Data */
    public static final String COLUMNNAME_ZZ_Import_Submitted_Data = "ZZ_Import_Submitted_Data";

	/** Set Import Submitted Data	  */
	public void setZZ_Import_Submitted_Data (String ZZ_Import_Submitted_Data);

	/** Get Import Submitted Data	  */
	public String getZZ_Import_Submitted_Data();

    /** Column name ZZ_WSP_ATR_Status */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Status = "ZZ_WSP_ATR_Status";

	/** Set Status	  */
	public void setZZ_WSP_ATR_Status (String ZZ_WSP_ATR_Status);

	/** Get Status	  */
	public String getZZ_WSP_ATR_Status();

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

    /** Column name ZZ_WSP_ATR_Submitted_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Submitted_UU = "ZZ_WSP_ATR_Submitted_UU";

	/** Set ZZ_WSP_ATR_Submitted_UU	  */
	public void setZZ_WSP_ATR_Submitted_UU (String ZZ_WSP_ATR_Submitted_UU);

	/** Get ZZ_WSP_ATR_Submitted_UU	  */
	public String getZZ_WSP_ATR_Submitted_UU();
}
