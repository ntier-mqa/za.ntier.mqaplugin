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

/** Generated Interface for ZZ_Petty_Cash_Recon_Hdr
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_Petty_Cash_Recon_Hdr 
{

    /** TableName=ZZ_Petty_Cash_Recon_Hdr */
    public static final String Table_Name = "ZZ_Petty_Cash_Recon_Hdr";

    /** AD_Table_ID=1000011 */
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

    /** Column name DocumentNo */
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";

	/** Set Document No.
	  * Document sequence number of the document
	  */
	public void setDocumentNo (String DocumentNo);

	/** Get Document No.
	  * Document sequence number of the document
	  */
	public String getDocumentNo();

    /** Column name EndDate */
    public static final String COLUMNNAME_EndDate = "EndDate";

	/** Set End Date.
	  * Last effective date (inclusive)
	  */
	public void setEndDate (Timestamp EndDate);

	/** Get End Date.
	  * Last effective date (inclusive)
	  */
	public Timestamp getEndDate();

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

    /** Column name StartDate */
    public static final String COLUMNNAME_StartDate = "StartDate";

	/** Set Start Date.
	  * First effective day (inclusive)
	  */
	public void setStartDate (Timestamp StartDate);

	/** Get Start Date.
	  * First effective day (inclusive)
	  */
	public Timestamp getStartDate();

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

    /** Column name ZZ_Actual_COH */
    public static final String COLUMNNAME_ZZ_Actual_COH = "ZZ_Actual_COH";

	/** Set Actual Cash On Hand	  */
	public void setZZ_Actual_COH (BigDecimal ZZ_Actual_COH);

	/** Get Actual Cash On Hand	  */
	public BigDecimal getZZ_Actual_COH();

    /** Column name ZZ_Advance_Total */
    public static final String COLUMNNAME_ZZ_Advance_Total = "ZZ_Advance_Total";

	/** Set Advance Total	  */
	public void setZZ_Advance_Total (BigDecimal ZZ_Advance_Total);

	/** Get Advance Total	  */
	public BigDecimal getZZ_Advance_Total();

    /** Column name ZZ_Calculated_COH */
    public static final String COLUMNNAME_ZZ_Calculated_COH = "ZZ_Calculated_COH";

	/** Set Calculated Cash On Hand	  */
	public void setZZ_Calculated_COH (BigDecimal ZZ_Calculated_COH);

	/** Get Calculated Cash On Hand	  */
	public BigDecimal getZZ_Calculated_COH();

    /** Column name ZZ_Claim_Total */
    public static final String COLUMNNAME_ZZ_Claim_Total = "ZZ_Claim_Total";

	/** Set Claim Total	  */
	public void setZZ_Claim_Total (BigDecimal ZZ_Claim_Total);

	/** Get Claim Total	  */
	public BigDecimal getZZ_Claim_Total();

    /** Column name ZZ_Create_Journal_Lines */
    public static final String COLUMNNAME_ZZ_Create_Journal_Lines = "ZZ_Create_Journal_Lines";

	/** Set Create Journal Lines	  */
	public void setZZ_Create_Journal_Lines (String ZZ_Create_Journal_Lines);

	/** Get Create Journal Lines	  */
	public String getZZ_Create_Journal_Lines();

    /** Column name ZZ_Create_Lines */
    public static final String COLUMNNAME_ZZ_Create_Lines = "ZZ_Create_Lines";

	/** Set Create Lines	  */
	public void setZZ_Create_Lines (String ZZ_Create_Lines);

	/** Get Create Lines	  */
	public String getZZ_Create_Lines();

    /** Column name ZZ_Float_Amt */
    public static final String COLUMNNAME_ZZ_Float_Amt = "ZZ_Float_Amt";

	/** Set Float	  */
	public void setZZ_Float_Amt (BigDecimal ZZ_Float_Amt);

	/** Get Float	  */
	public BigDecimal getZZ_Float_Amt();

    /** Column name ZZ_Petty_Cash_Recon_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_ID = "ZZ_Petty_Cash_Recon_Hdr_ID";

	/** Set Petty Cash Recon	  */
	public void setZZ_Petty_Cash_Recon_Hdr_ID (int ZZ_Petty_Cash_Recon_Hdr_ID);

	/** Get Petty Cash Recon	  */
	public int getZZ_Petty_Cash_Recon_Hdr_ID();

    /** Column name ZZ_Petty_Cash_Recon_Hdr_UU */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_UU = "ZZ_Petty_Cash_Recon_Hdr_UU";

	/** Set ZZ_Petty_Cash_Recon_Hdr_UU	  */
	public void setZZ_Petty_Cash_Recon_Hdr_UU (String ZZ_Petty_Cash_Recon_Hdr_UU);

	/** Get ZZ_Petty_Cash_Recon_Hdr_UU	  */
	public String getZZ_Petty_Cash_Recon_Hdr_UU();
}
