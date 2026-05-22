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

/** Generated Interface for ZZ_Monthly_Levy_Files
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_Monthly_Levy_Files 
{

    /** TableName=ZZ_Monthly_Levy_Files */
    public static final String Table_Name = "ZZ_Monthly_Levy_Files";

    /** AD_Table_ID=1000066 */
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

    /** Column name C_InvoiceBatchLine_ID */
    public static final String COLUMNNAME_C_InvoiceBatchLine_ID = "C_InvoiceBatchLine_ID";

	/** Set Invoice Batch Line.
	  * Expense Invoice Batch Line
	  */
	public void setC_InvoiceBatchLine_ID (int C_InvoiceBatchLine_ID);

	/** Get Invoice Batch Line.
	  * Expense Invoice Batch Line
	  */
	public int getC_InvoiceBatchLine_ID();

	public org.compiere.model.I_C_InvoiceBatchLine getC_InvoiceBatchLine() throws RuntimeException;

    /** Column name C_Year_ID */
    public static final String COLUMNNAME_C_Year_ID = "C_Year_ID";

	/** Set Year.
	  * Calendar Year
	  */
	public void setC_Year_ID (int C_Year_ID);

	/** Get Year.
	  * Calendar Year
	  */
	public int getC_Year_ID();

	public org.compiere.model.I_C_Year getC_Year() throws RuntimeException;

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

    /** Column name ZZ_Admin */
    public static final String COLUMNNAME_ZZ_Admin = "ZZ_Admin";

	/** Set Admin	  */
	public void setZZ_Admin (BigDecimal ZZ_Admin);

	/** Get Admin	  */
	public BigDecimal getZZ_Admin();

    /** Column name ZZ_Current_Date */
    public static final String COLUMNNAME_ZZ_Current_Date = "ZZ_Current_Date";

	/** Set Current Date	  */
	public void setZZ_Current_Date (Timestamp ZZ_Current_Date);

	/** Get Current Date	  */
	public Timestamp getZZ_Current_Date();

    /** Column name ZZ_DG */
    public static final String COLUMNNAME_ZZ_DG = "ZZ_DG";

	/** Set DG	  */
	public void setZZ_DG (BigDecimal ZZ_DG);

	/** Get DG	  */
	public BigDecimal getZZ_DG();

    /** Column name ZZ_File_Name */
    public static final String COLUMNNAME_ZZ_File_Name = "ZZ_File_Name";

	/** Set File Name	  */
	public void setZZ_File_Name (String ZZ_File_Name);

	/** Get File Name	  */
	public String getZZ_File_Name();

    /** Column name ZZ_Grant_Status */
    public static final String COLUMNNAME_ZZ_Grant_Status = "ZZ_Grant_Status";

	/** Set Grant Status	  */
	public void setZZ_Grant_Status (String ZZ_Grant_Status);

	/** Get Grant Status	  */
	public String getZZ_Grant_Status();

    /** Column name ZZ_MG */
    public static final String COLUMNNAME_ZZ_MG = "ZZ_MG";

	/** Set MG	  */
	public void setZZ_MG (BigDecimal ZZ_MG);

	/** Get MG	  */
	public BigDecimal getZZ_MG();

    /** Column name ZZ_Month */
    public static final String COLUMNNAME_ZZ_Month = "ZZ_Month";

	/** Set Month	  */
	public void setZZ_Month (String ZZ_Month);

	/** Get Month	  */
	public String getZZ_Month();

    /** Column name ZZ_Monthly_Levy_Files_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Monthly_Levy_Files_Hdr_ID = "ZZ_Monthly_Levy_Files_Hdr_ID";

	/** Set Monthly Levy Files Hdr	  */
	public void setZZ_Monthly_Levy_Files_Hdr_ID (int ZZ_Monthly_Levy_Files_Hdr_ID);

	/** Get Monthly Levy Files Hdr	  */
	public int getZZ_Monthly_Levy_Files_Hdr_ID();

	public I_ZZ_Monthly_Levy_Files_Hdr getZZ_Monthly_Levy_Files_Hdr() throws RuntimeException;

    /** Column name ZZ_Monthly_Levy_Files_ID */
    public static final String COLUMNNAME_ZZ_Monthly_Levy_Files_ID = "ZZ_Monthly_Levy_Files_ID";

	/** Set Monthly Levy Files	  */
	public void setZZ_Monthly_Levy_Files_ID (int ZZ_Monthly_Levy_Files_ID);

	/** Get Monthly Levy Files	  */
	public int getZZ_Monthly_Levy_Files_ID();

    /** Column name ZZ_Monthly_Levy_Files_UU */
    public static final String COLUMNNAME_ZZ_Monthly_Levy_Files_UU = "ZZ_Monthly_Levy_Files_UU";

	/** Set ZZ_Monthly_Levy_Files_UU	  */
	public void setZZ_Monthly_Levy_Files_UU (String ZZ_Monthly_Levy_Files_UU);

	/** Get ZZ_Monthly_Levy_Files_UU	  */
	public String getZZ_Monthly_Levy_Files_UU();

    /** Column name ZZ_Penalties */
    public static final String COLUMNNAME_ZZ_Penalties = "ZZ_Penalties";

	/** Set Penalties	  */
	public void setZZ_Penalties (BigDecimal ZZ_Penalties);

	/** Get Penalties	  */
	public BigDecimal getZZ_Penalties();

    /** Column name ZZ_SDL_No */
    public static final String COLUMNNAME_ZZ_SDL_No = "ZZ_SDL_No";

	/** Set SDL Number	  */
	public void setZZ_SDL_No (String ZZ_SDL_No);

	/** Get SDL Number	  */
	public String getZZ_SDL_No();

    /** Column name ZZ_Scheme_Year_Adjust */
    public static final String COLUMNNAME_ZZ_Scheme_Year_Adjust = "ZZ_Scheme_Year_Adjust";

	/** Set Scheme Year Adjustment	  */
	public void setZZ_Scheme_Year_Adjust (String ZZ_Scheme_Year_Adjust);

	/** Get Scheme Year Adjustment	  */
	public String getZZ_Scheme_Year_Adjust();

    /** Column name ZZ_Seta_Code */
    public static final String COLUMNNAME_ZZ_Seta_Code = "ZZ_Seta_Code";

	/** Set Seta Code	  */
	public void setZZ_Seta_Code (String ZZ_Seta_Code);

	/** Get Seta Code	  */
	public String getZZ_Seta_Code();

    /** Column name ZZ_Year */
    public static final String COLUMNNAME_ZZ_Year = "ZZ_Year";

	/** Set Year	  */
	public void setZZ_Year (String ZZ_Year);

	/** Get Year	  */
	public String getZZ_Year();

    /** Column name zz_Interest */
    public static final String COLUMNNAME_zz_Interest = "zz_Interest";

	/** Set Interest	  */
	public void setzz_Interest (BigDecimal zz_Interest);

	/** Get Interest	  */
	public BigDecimal getzz_Interest();

    /** Column name zz_Total */
    public static final String COLUMNNAME_zz_Total = "zz_Total";

	/** Set Total	  */
	public void setzz_Total (BigDecimal zz_Total);

	/** Get Total	  */
	public BigDecimal getzz_Total();
}
