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

/** Generated Interface for ZZ_WSP_ATR_Finance_Train_Compare_Rep
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_Finance_Train_Compare_Rep 
{

    /** TableName=ZZ_WSP_ATR_Finance_Train_Compare_Rep */
    public static final String Table_Name = "ZZ_WSP_ATR_Finance_Train_Compare_Rep";

    /** AD_Table_ID=1000208 */
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

    /** Column name ZZ_Finance_Type */
    public static final String COLUMNNAME_ZZ_Finance_Type = "ZZ_Finance_Type";

	/** Set Finance Type	  */
	public void setZZ_Finance_Type (String ZZ_Finance_Type);

	/** Get Finance Type	  */
	public String getZZ_Finance_Type();

    /** Column name ZZ_Finance_Value */
    public static final String COLUMNNAME_ZZ_Finance_Value = "ZZ_Finance_Value";

	/** Set Finance Value	  */
	public void setZZ_Finance_Value (String ZZ_Finance_Value);

	/** Get Finance Value	  */
	public String getZZ_Finance_Value();

    /** Column name ZZ_Section */
    public static final String COLUMNNAME_ZZ_Section = "ZZ_Section";

	/** Set Section	  */
	public void setZZ_Section (String ZZ_Section);

	/** Get Section	  */
	public String getZZ_Section();

    /** Column name ZZ_WSP_ATR_Finance_Train_Compare_Rep_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Finance_Train_Compare_Rep_ID = "ZZ_WSP_ATR_Finance_Train_Compare_Rep_ID";

	/** Set Finance and Training Comparison	  */
	public void setZZ_WSP_ATR_Finance_Train_Compare_Rep_ID (int ZZ_WSP_ATR_Finance_Train_Compare_Rep_ID);

	/** Get Finance and Training Comparison	  */
	public int getZZ_WSP_ATR_Finance_Train_Compare_Rep_ID();

    /** Column name ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU = "ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU";

	/** Set ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU	  */
	public void setZZ_WSP_ATR_Finance_Train_Compare_Rep_UU (String ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU);

	/** Get ZZ_WSP_ATR_Finance_Train_Compare_Rep_UU	  */
	public String getZZ_WSP_ATR_Finance_Train_Compare_Rep_UU();

    /** Column name ZZ_WSP_ATR_Report_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_Report_ID = "ZZ_WSP_ATR_Report_ID";

	/** Set WSP ATR Report 	  */
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID);

	/** Get WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID();

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException;
}
