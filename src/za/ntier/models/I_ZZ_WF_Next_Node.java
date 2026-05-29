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

/** Generated Interface for ZZ_WF_Next_Node
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WF_Next_Node 
{

    /** TableName=ZZ_WF_Next_Node */
    public static final String Table_Name = "ZZ_WF_Next_Node";

    /** AD_Table_ID=1000271 */
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

    /** Column name MMailText_ID */
    public static final String COLUMNNAME_MMailText_ID = "MMailText_ID";

	/** Set Mail Text	  */
	public void setMMailText_ID (int MMailText_ID);

	/** Get Mail Text	  */
	public int getMMailText_ID();

	public org.compiere.model.I_R_MailText getMMailText() throws RuntimeException;

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

    /** Column name ZZ_Option_Value */
    public static final String COLUMNNAME_ZZ_Option_Value = "ZZ_Option_Value";

	/** Set Option Value Chosen	  */
	public void setZZ_Option_Value (String ZZ_Option_Value);

	/** Get Option Value Chosen	  */
	public String getZZ_Option_Value();

    /** Column name ZZ_WF_Lines_ID */
    public static final String COLUMNNAME_ZZ_WF_Lines_ID = "ZZ_WF_Lines_ID";

	/** Set ZZ_WF_Lines	  */
	public void setZZ_WF_Lines_ID (int ZZ_WF_Lines_ID);

	/** Get ZZ_WF_Lines	  */
	public int getZZ_WF_Lines_ID();

    /** Column name ZZ_WF_Next_Lines_ID */
    public static final String COLUMNNAME_ZZ_WF_Next_Lines_ID = "ZZ_WF_Next_Lines_ID";

	/** Set Next Node	  */
	public void setZZ_WF_Next_Lines_ID (int ZZ_WF_Next_Lines_ID);

	/** Get Next Node	  */
	public int getZZ_WF_Next_Lines_ID();

	public I_ZZ_WF_Lines getZZ_WF_Next_Lines() throws RuntimeException;

    /** Column name ZZ_WF_Next_Node_ID */
    public static final String COLUMNNAME_ZZ_WF_Next_Node_ID = "ZZ_WF_Next_Node_ID";

	/** Set Next Nodes	  */
	public void setZZ_WF_Next_Node_ID (int ZZ_WF_Next_Node_ID);

	/** Get Next Nodes	  */
	public int getZZ_WF_Next_Node_ID();

    /** Column name ZZ_WF_Next_Node_UU */
    public static final String COLUMNNAME_ZZ_WF_Next_Node_UU = "ZZ_WF_Next_Node_UU";

	/** Set ZZ_WF_Next_Node_UU	  */
	public void setZZ_WF_Next_Node_UU (String ZZ_WF_Next_Node_UU);

	/** Get ZZ_WF_Next_Node_UU	  */
	public String getZZ_WF_Next_Node_UU();
}
