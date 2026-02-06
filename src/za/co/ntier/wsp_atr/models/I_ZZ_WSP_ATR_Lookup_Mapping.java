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

/** Generated Interface for ZZ_WSP_ATR_Lookup_Mapping
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Lookup_Mapping 
{

    /** TableName=ZZ_WSP_ATR_Lookup_Mapping */
    public static final String Table_Name = "ZZ_WSP_ATR_Lookup_Mapping";

    /** AD_Table_ID=1000148 */
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

    /** Column name Start_Row */
    public static final String COLUMNNAME_Start_Row = "Start_Row";

	/** Set Start Row	  */
	public void setStart_Row (BigDecimal Start_Row);

	/** Get Start Row	  */
	public BigDecimal getStart_Row();

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

    /** Column name ZZ_Is_Columns */
    public static final String COLUMNNAME_ZZ_Is_Columns = "ZZ_Is_Columns";

	/** Set Is Columns	  */
	public void setZZ_Is_Columns (boolean ZZ_Is_Columns);

	/** Get Is Columns	  */
	public boolean isZZ_Is_Columns();

    /** Column name ZZ_Tab_Name */
    public static final String COLUMNNAME_ZZ_Tab_Name = "ZZ_Tab_Name";

	/** Set Tab Name	  */
	public void setZZ_Tab_Name (String ZZ_Tab_Name);

	/** Get Tab Name	  */
	public String getZZ_Tab_Name();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID = "ZZ_WSP_ATR_Lookup_Mapping_ID";

	/** Set WSP ATR Lookup Mapping	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_ID (int ZZ_WSP_ATR_Lookup_Mapping_ID);

	/** Get WSP ATR Lookup Mapping	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_ID();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_UU = "ZZ_WSP_ATR_Lookup_Mapping_UU";

	/** Set ZZ_WSP_ATR_Lookup_Mapping_UU	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_UU (String ZZ_WSP_ATR_Lookup_Mapping_UU);

	/** Get ZZ_WSP_ATR_Lookup_Mapping_UU	  */
	public String getZZ_WSP_ATR_Lookup_Mapping_UU();
}
