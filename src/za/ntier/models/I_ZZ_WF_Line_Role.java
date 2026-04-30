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

/** Generated Interface for ZZ_WF_Line_Role
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_ZZ_WF_Line_Role 
{

    /** TableName=ZZ_WF_Line_Role */
    public static final String Table_Name = "ZZ_WF_Line_Role";

    /** AD_Table_ID=1000079 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

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

    /** Column name AD_Role_ID */
    public static final String COLUMNNAME_AD_Role_ID = "AD_Role_ID";

	/** Set Role.
	  * Responsibility Role
	  */
	public void setAD_Role_ID (int AD_Role_ID);

	/** Get Role.
	  * Responsibility Role
	  */
	public int getAD_Role_ID();

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException;

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

    /** Column name ZZ_Is_Responsible */
    public static final String COLUMNNAME_ZZ_Is_Responsible = "ZZ_Is_Responsible";

	/** Set Is Responsible	  */
	public void setZZ_Is_Responsible (boolean ZZ_Is_Responsible);

	/** Get Is Responsible	  */
	public boolean isZZ_Is_Responsible();

    /** Column name ZZ_NextStatus */
    public static final String COLUMNNAME_ZZ_NextStatus = "ZZ_NextStatus";

	/** Set Next Status	  */
	public void setZZ_NextStatus (String ZZ_NextStatus);

	/** Get Next Status	  */
	public String getZZ_NextStatus();

    /** Column name ZZ_Notify */
    public static final String COLUMNNAME_ZZ_Notify = "ZZ_Notify";

	/** Set Notify	  */
	public void setZZ_Notify (boolean ZZ_Notify);

	/** Get Notify	  */
	public boolean isZZ_Notify();

    /** Column name ZZ_WF_Line_Role_ID */
    public static final String COLUMNNAME_ZZ_WF_Line_Role_ID = "ZZ_WF_Line_Role_ID";

	/** Set ZZ_WF_Line_Role	  */
	public void setZZ_WF_Line_Role_ID (int ZZ_WF_Line_Role_ID);

	/** Get ZZ_WF_Line_Role	  */
	public int getZZ_WF_Line_Role_ID();

    /** Column name ZZ_WF_Line_Role_UU */
    public static final String COLUMNNAME_ZZ_WF_Line_Role_UU = "ZZ_WF_Line_Role_UU";

	/** Set ZZ_WF_Line_Role_UU	  */
	public void setZZ_WF_Line_Role_UU (String ZZ_WF_Line_Role_UU);

	/** Get ZZ_WF_Line_Role_UU	  */
	public String getZZ_WF_Line_Role_UU();

    /** Column name ZZ_WF_Lines_ID */
    public static final String COLUMNNAME_ZZ_WF_Lines_ID = "ZZ_WF_Lines_ID";

	/** Set ZZ_WF_Lines	  */
	public void setZZ_WF_Lines_ID (int ZZ_WF_Lines_ID);

	/** Get ZZ_WF_Lines	  */
	public int getZZ_WF_Lines_ID();

	public I_ZZ_WF_Lines getZZ_WF_Lines() throws RuntimeException;
}
