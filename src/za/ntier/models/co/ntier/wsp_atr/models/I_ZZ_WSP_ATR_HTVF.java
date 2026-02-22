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

/** Generated Interface for ZZ_WSP_ATR_HTVF
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WSP_ATR_HTVF 
{

    /** TableName=ZZ_WSP_ATR_HTVF */
    public static final String Table_Name = "ZZ_WSP_ATR_HTVF";

    /** AD_Table_ID=1000171 */
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

    /** Column name ZZ_Further_Scarce_Reason2_ID */
    public static final String COLUMNNAME_ZZ_Further_Scarce_Reason2_ID = "ZZ_Further_Scarce_Reason2_ID";

	/** Set Further Scarce Reason 2	  */
	public void setZZ_Further_Scarce_Reason2_ID (int ZZ_Further_Scarce_Reason2_ID);

	/** Get Further Scarce Reason 2	  */
	public int getZZ_Further_Scarce_Reason2_ID();

	public I_ZZ_Scarce_Reason_Ref getZZ_Further_Scarce_Reason2() throws RuntimeException;

    /** Column name ZZ_Further_Scarce_Reason_ID */
    public static final String COLUMNNAME_ZZ_Further_Scarce_Reason_ID = "ZZ_Further_Scarce_Reason_ID";

	/** Set Further Scarce Reason	  */
	public void setZZ_Further_Scarce_Reason_ID (int ZZ_Further_Scarce_Reason_ID);

	/** Get Further Scarce Reason	  */
	public int getZZ_Further_Scarce_Reason_ID();

	public I_ZZ_Scarce_Reason_Ref getZZ_Further_Scarce_Reason() throws RuntimeException;

    /** Column name ZZ_Occupations_ID */
    public static final String COLUMNNAME_ZZ_Occupations_ID = "ZZ_Occupations_ID";

	/** Set Occupation	  */
	public void setZZ_Occupations_ID (int ZZ_Occupations_ID);

	/** Get Occupation	  */
	public int getZZ_Occupations_ID();

	public I_ZZ_Occupations_Ref getZZ_Occupations() throws RuntimeException;

    /** Column name ZZ_Scarce_Other_Reasons_Comments */
    public static final String COLUMNNAME_ZZ_Scarce_Other_Reasons_Comments = "ZZ_Scarce_Other_Reasons_Comments";

	/** Set Other Reasons/Comments	  */
	public void setZZ_Scarce_Other_Reasons_Comments (String ZZ_Scarce_Other_Reasons_Comments);

	/** Get Other Reasons/Comments	  */
	public String getZZ_Scarce_Other_Reasons_Comments();

    /** Column name ZZ_Scarce_Reason_ID */
    public static final String COLUMNNAME_ZZ_Scarce_Reason_ID = "ZZ_Scarce_Reason_ID";

	/** Set Scarce Reason	  */
	public void setZZ_Scarce_Reason_ID (int ZZ_Scarce_Reason_ID);

	/** Get Scarce Reason	  */
	public int getZZ_Scarce_Reason_ID();

	public I_ZZ_Scarce_Reason_Ref getZZ_Scarce_Reason() throws RuntimeException;

    /** Column name ZZ_Vacancies_EC_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_EC_Cnt = "ZZ_Vacancies_EC_Cnt";

	/** Set Vacancies EC Count	  */
	public void setZZ_Vacancies_EC_Cnt (BigDecimal ZZ_Vacancies_EC_Cnt);

	/** Get Vacancies EC Count	  */
	public BigDecimal getZZ_Vacancies_EC_Cnt();

    /** Column name ZZ_Vacancies_FS_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_FS_Cnt = "ZZ_Vacancies_FS_Cnt";

	/** Set Vacancies FS Count	  */
	public void setZZ_Vacancies_FS_Cnt (BigDecimal ZZ_Vacancies_FS_Cnt);

	/** Get Vacancies FS Count	  */
	public BigDecimal getZZ_Vacancies_FS_Cnt();

    /** Column name ZZ_Vacancies_GP_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_GP_Cnt = "ZZ_Vacancies_GP_Cnt";

	/** Set Vacancies GP Cnt	  */
	public void setZZ_Vacancies_GP_Cnt (BigDecimal ZZ_Vacancies_GP_Cnt);

	/** Get Vacancies GP Cnt	  */
	public BigDecimal getZZ_Vacancies_GP_Cnt();

    /** Column name ZZ_Vacancies_KZN_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_KZN_Cnt = "ZZ_Vacancies_KZN_Cnt";

	/** Set Vacancies KZN Count	  */
	public void setZZ_Vacancies_KZN_Cnt (BigDecimal ZZ_Vacancies_KZN_Cnt);

	/** Get Vacancies KZN Count	  */
	public BigDecimal getZZ_Vacancies_KZN_Cnt();

    /** Column name ZZ_Vacancies_LP_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_LP_Cnt = "ZZ_Vacancies_LP_Cnt";

	/** Set Vacancies LP Count	  */
	public void setZZ_Vacancies_LP_Cnt (BigDecimal ZZ_Vacancies_LP_Cnt);

	/** Get Vacancies LP Count	  */
	public BigDecimal getZZ_Vacancies_LP_Cnt();

    /** Column name ZZ_Vacancies_MP_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_MP_Cnt = "ZZ_Vacancies_MP_Cnt";

	/** Set Vacancies MP Count	  */
	public void setZZ_Vacancies_MP_Cnt (BigDecimal ZZ_Vacancies_MP_Cnt);

	/** Get Vacancies MP Count	  */
	public BigDecimal getZZ_Vacancies_MP_Cnt();

    /** Column name ZZ_Vacancies_NP_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_NP_Cnt = "ZZ_Vacancies_NP_Cnt";

	/** Set Vacancies NP Count	  */
	public void setZZ_Vacancies_NP_Cnt (BigDecimal ZZ_Vacancies_NP_Cnt);

	/** Get Vacancies NP Count	  */
	public BigDecimal getZZ_Vacancies_NP_Cnt();

    /** Column name ZZ_Vacancies_NW_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_NW_Cnt = "ZZ_Vacancies_NW_Cnt";

	/** Set Vacancies NW Count	  */
	public void setZZ_Vacancies_NW_Cnt (BigDecimal ZZ_Vacancies_NW_Cnt);

	/** Get Vacancies NW Count	  */
	public BigDecimal getZZ_Vacancies_NW_Cnt();

    /** Column name ZZ_Vacancies_WC_Cnt */
    public static final String COLUMNNAME_ZZ_Vacancies_WC_Cnt = "ZZ_Vacancies_WC_Cnt";

	/** Set Vacancies WC Count	  */
	public void setZZ_Vacancies_WC_Cnt (BigDecimal ZZ_Vacancies_WC_Cnt);

	/** Get Vacancies WC Count	  */
	public BigDecimal getZZ_Vacancies_WC_Cnt();

    /** Column name ZZ_WSP_ATR_HTVF_ID */
    public static final String COLUMNNAME_ZZ_WSP_ATR_HTVF_ID = "ZZ_WSP_ATR_HTVF_ID";

	/** Set WSP/ATR HTVF Detail	  */
	public void setZZ_WSP_ATR_HTVF_ID (int ZZ_WSP_ATR_HTVF_ID);

	/** Get WSP/ATR HTVF Detail	  */
	public int getZZ_WSP_ATR_HTVF_ID();

    /** Column name ZZ_WSP_ATR_HTVF_UU */
    public static final String COLUMNNAME_ZZ_WSP_ATR_HTVF_UU = "ZZ_WSP_ATR_HTVF_UU";

	/** Set ZZ_WSP_ATR_HTVF_UU	  */
	public void setZZ_WSP_ATR_HTVF_UU (String ZZ_WSP_ATR_HTVF_UU);

	/** Get ZZ_WSP_ATR_HTVF_UU	  */
	public String getZZ_WSP_ATR_HTVF_UU();

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
}
