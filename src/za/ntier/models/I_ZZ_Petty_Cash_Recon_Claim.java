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

/** Generated Interface for ZZ_Petty_Cash_Recon_Claim
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_Petty_Cash_Recon_Claim 
{

    /** TableName=ZZ_Petty_Cash_Recon_Claim */
    public static final String Table_Name = "ZZ_Petty_Cash_Recon_Claim";

    /** AD_Table_ID=1000016 */
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

    /** Column name Amount */
    public static final String COLUMNNAME_Amount = "Amount";

	/** Set Amount.
	  * Amount in a defined currency
	  */
	public void setAmount (BigDecimal Amount);

	/** Get Amount.
	  * Amount in a defined currency
	  */
	public BigDecimal getAmount();

    /** Column name C_Charge_ID */
    public static final String COLUMNNAME_C_Charge_ID = "C_Charge_ID";

	/** Set Charge.
	  * Additional document charges
	  */
	public void setC_Charge_ID (int C_Charge_ID);

	/** Get Charge.
	  * Additional document charges
	  */
	public int getC_Charge_ID();

	public org.compiere.model.I_C_Charge getC_Charge() throws RuntimeException;

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

    /** Column name GL_JournalLine_ID */
    public static final String COLUMNNAME_GL_JournalLine_ID = "GL_JournalLine_ID";

	/** Set Journal Line.
	  * General Ledger Journal Line
	  */
	public void setGL_JournalLine_ID (int GL_JournalLine_ID);

	/** Get Journal Line.
	  * General Ledger Journal Line
	  */
	public int getGL_JournalLine_ID();

	public org.compiere.model.I_GL_JournalLine getGL_JournalLine() throws RuntimeException;

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

    /** Column name Line */
    public static final String COLUMNNAME_Line = "Line";

	/** Set Line No.
	  * Unique line for this document
	  */
	public void setLine (int Line);

	/** Get Line No.
	  * Unique line for this document
	  */
	public int getLine();

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

    /** Column name ZZ_Advance_Balance */
    public static final String COLUMNNAME_ZZ_Advance_Balance = "ZZ_Advance_Balance";

	/** Set Advance Balance	  */
	public void setZZ_Advance_Balance (BigDecimal ZZ_Advance_Balance);

	/** Get Advance Balance	  */
	public BigDecimal getZZ_Advance_Balance();

    /** Column name ZZ_Petty_Cash_Advance_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Advance_Hdr_ID = "ZZ_Petty_Cash_Advance_Hdr_ID";

	/** Set Petty Cash Advance	  */
	public void setZZ_Petty_Cash_Advance_Hdr_ID (int ZZ_Petty_Cash_Advance_Hdr_ID);

	/** Get Petty Cash Advance	  */
	public int getZZ_Petty_Cash_Advance_Hdr_ID();

	public I_ZZ_Petty_Cash_Advance_Hdr getZZ_Petty_Cash_Advance_Hdr() throws RuntimeException;

    /** Column name ZZ_Petty_Cash_Claim_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Claim_Hdr_ID = "ZZ_Petty_Cash_Claim_Hdr_ID";

	/** Set Petty Cash Claim	  */
	public void setZZ_Petty_Cash_Claim_Hdr_ID (int ZZ_Petty_Cash_Claim_Hdr_ID);

	/** Get Petty Cash Claim	  */
	public int getZZ_Petty_Cash_Claim_Hdr_ID();

	public I_ZZ_Petty_Cash_Claim_Hdr getZZ_Petty_Cash_Claim_Hdr() throws RuntimeException;

    /** Column name ZZ_Petty_Cash_Claim_Line_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Claim_Line_ID = "ZZ_Petty_Cash_Claim_Line_ID";

	/** Set Petty Cash Claim Line	  */
	public void setZZ_Petty_Cash_Claim_Line_ID (int ZZ_Petty_Cash_Claim_Line_ID);

	/** Get Petty Cash Claim Line	  */
	public int getZZ_Petty_Cash_Claim_Line_ID();

	public I_ZZ_Petty_Cash_Claim_Line getZZ_Petty_Cash_Claim_Line() throws RuntimeException;

    /** Column name ZZ_Petty_Cash_Motivation */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Motivation = "ZZ_Petty_Cash_Motivation";

	/** Set Motivation	  */
	public void setZZ_Petty_Cash_Motivation (String ZZ_Petty_Cash_Motivation);

	/** Get Motivation	  */
	public String getZZ_Petty_Cash_Motivation();

    /** Column name ZZ_Petty_Cash_Recon_Claim_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_ID = "ZZ_Petty_Cash_Recon_Claim_ID";

	/** Set Petty Cash Recon Claim	  */
	public void setZZ_Petty_Cash_Recon_Claim_ID (int ZZ_Petty_Cash_Recon_Claim_ID);

	/** Get Petty Cash Recon Claim	  */
	public int getZZ_Petty_Cash_Recon_Claim_ID();

    /** Column name ZZ_Petty_Cash_Recon_Claim_UU */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Recon_Claim_UU = "ZZ_Petty_Cash_Recon_Claim_UU";

	/** Set ZZ_Petty_Cash_Recon_Claim_UU	  */
	public void setZZ_Petty_Cash_Recon_Claim_UU (String ZZ_Petty_Cash_Recon_Claim_UU);

	/** Get ZZ_Petty_Cash_Recon_Claim_UU	  */
	public String getZZ_Petty_Cash_Recon_Claim_UU();

    /** Column name ZZ_Petty_Cash_Recon_Hdr_ID */
    public static final String COLUMNNAME_ZZ_Petty_Cash_Recon_Hdr_ID = "ZZ_Petty_Cash_Recon_Hdr_ID";

	/** Set Petty Cash Recon	  */
	public void setZZ_Petty_Cash_Recon_Hdr_ID (int ZZ_Petty_Cash_Recon_Hdr_ID);

	/** Get Petty Cash Recon	  */
	public int getZZ_Petty_Cash_Recon_Hdr_ID();

	public I_ZZ_Petty_Cash_Recon_Hdr getZZ_Petty_Cash_Recon_Hdr() throws RuntimeException;
}
