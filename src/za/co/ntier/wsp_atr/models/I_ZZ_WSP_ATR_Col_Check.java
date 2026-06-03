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

/** Generated Interface for ZZ_WSP_ATR_Col_Check
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Col_Check 
{

    /** TableName=ZZ_WSP_ATR_Col_Check */
    public static final String Table_Name = "ZZ_WSP_ATR_Col_Check";

    /** AD_Table_ID=1000280 */
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

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

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

    /** Column name ZZ_Check_Name */
    public static final String COLUMNNAME_ZZ_Check_Name = "ZZ_Check_Name";

	/** Set Check Name	  */
	public void setZZ_Check_Name (String ZZ_Check_Name);

	/** Get Check Name	  */
	public String getZZ_Check_Name();

    /** Column name ZZ_Check_Type */
    public static final String COLUMNNAME_ZZ_Check_Type = "ZZ_Check_Type";

	/** Set Check Type	  */
	public void setZZ_Check_Type (String ZZ_Check_Type);

	/** Get Check Type	  */
	public String getZZ_Check_Type();

    /** Column name ZZ_Col_Letters_A */
    public static final String COLUMNNAME_ZZ_Col_Letters_A = "ZZ_Col_Letters_A";

	/** Set Col Letters A.
	  * Comma delimited Column Letters
	  */
	public void setZZ_Col_Letters_A (String ZZ_Col_Letters_A);

	/** Get Col Letters A.
	  * Comma delimited Column Letters
	  */
	public String getZZ_Col_Letters_A();

    /** Column name ZZ_Col_Letters_B */
    public static final String COLUMNNAME_ZZ_Col_Letters_B = "ZZ_Col_Letters_B";

	/** Set Col Letters B	  */
	public void setZZ_Col_Letters_B (String ZZ_Col_Letters_B);

	/** Get Col Letters B	  */
	public String getZZ_Col_Letters_B();

    /** Column name ZZ_Error_Message */
    public static final String COLUMNNAME_ZZ_Error_Message = "ZZ_Error_Message";

	/** Set Error Message	  */
	public void setZZ_Error_Message (String ZZ_Error_Message);

	/** Get Error Message	  */
	public String getZZ_Error_Message();

    /** Column name ZZ_WSP_ATR_Col_Check_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Col_Check_ID = "ZZ_WSP_ATR_Col_Check_ID";

	/** Set WSP ATR Import Column Check.
	  * WSP ATR Import check column totals (gender total = race totals)
	  */
	public void setZZ_WSP_ATR_Col_Check_ID (int ZZ_WSP_ATR_Col_Check_ID);

	/** Get WSP ATR Import Column Check.
	  * WSP ATR Import check column totals (gender total = race totals)
	  */
	public int getZZ_WSP_ATR_Col_Check_ID();

    /** Column name ZZ_WSP_ATR_Col_Check_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Col_Check_UU = "ZZ_WSP_ATR_Col_Check_UU";

	/** Set ZZ_WSP_ATR_Col_Check_UU	  */
	public void setZZ_WSP_ATR_Col_Check_UU (String ZZ_WSP_ATR_Col_Check_UU);

	/** Get ZZ_WSP_ATR_Col_Check_UU	  */
	public String getZZ_WSP_ATR_Col_Check_UU();

    /** Column name ZZ_WSP_ATR_Lookup_Mapping_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID = "ZZ_WSP_ATR_Lookup_Mapping_ID";

	/** Set WSP ATR Lookup Mapping	  */
	public void setZZ_WSP_ATR_Lookup_Mapping_ID (int ZZ_WSP_ATR_Lookup_Mapping_ID);

	/** Get WSP ATR Lookup Mapping	  */
	public int getZZ_WSP_ATR_Lookup_Mapping_ID();

	public I_ZZ_WSP_ATR_Lookup_Mapping getZZ_WSP_ATR_Lookup_Mapping() throws RuntimeException;
}
