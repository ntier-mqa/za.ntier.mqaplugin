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

    /** Column name ZZ_AppRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_AppRejectedBy_ID = "ZZ_AppRejectedBy_ID";

	/** Set App Rejected By	  */
	public void setZZ_AppRejectedBy_ID (int ZZ_AppRejectedBy_ID);

	/** Get App Rejected By	  */
	public int getZZ_AppRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_AppRejectedBy() throws RuntimeException;

    /** Column name ZZ_AppRejectedDate */
    public static final String COLUMNNAME_ZZ_AppRejectedDate = "ZZ_AppRejectedDate";

	/** Set App Rejected Date	  */
	public void setZZ_AppRejectedDate (Timestamp ZZ_AppRejectedDate);

	/** Get App Rejected Date	  */
	public Timestamp getZZ_AppRejectedDate();

    /** Column name ZZ_ApprovedBy_ID */
    public static final String COLUMNNAME_ZZ_ApprovedBy_ID = "ZZ_ApprovedBy_ID";

	/** Set Approved By	  */
	public void setZZ_ApprovedBy_ID (int ZZ_ApprovedBy_ID);

	/** Get Approved By	  */
	public int getZZ_ApprovedBy_ID();

	public org.compiere.model.I_AD_User getZZ_ApprovedBy() throws RuntimeException;

    /** Column name ZZ_ApprovedDate */
    public static final String COLUMNNAME_ZZ_ApprovedDate = "ZZ_ApprovedDate";

	/** Set Approved Date	  */
	public void setZZ_ApprovedDate (Timestamp ZZ_ApprovedDate);

	/** Get Approved Date	  */
	public Timestamp getZZ_ApprovedDate();

    /** Column name ZZ_Approved_ID */
    public static final String COLUMNNAME_ZZ_Approved_ID = "ZZ_Approved_ID";

	/** Set Approved By	  */
	public void setZZ_Approved_ID (int ZZ_Approved_ID);

	/** Get Approved By	  */
	public int getZZ_Approved_ID();

	public org.compiere.model.I_AD_User getZZ_Approved() throws RuntimeException;

    /** Column name ZZ_DocAction */
    public static final String COLUMNNAME_ZZ_DocAction = "ZZ_DocAction";

	/** Set Document Action	  */
	public void setZZ_DocAction (String ZZ_DocAction);

	/** Get Document Action	  */
	public String getZZ_DocAction();

    /** Column name ZZ_DocStatus */
    public static final String COLUMNNAME_ZZ_DocStatus = "ZZ_DocStatus";

	/** Set Document Status	  */
	public void setZZ_DocStatus (String ZZ_DocStatus);

	/** Get Document Status	  */
	public String getZZ_DocStatus();

    /** Column name ZZ_EvalRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_EvalRejectedBy_ID = "ZZ_EvalRejectedBy_ID";

	/** Set Eval Rejected By	  */
	public void setZZ_EvalRejectedBy_ID (int ZZ_EvalRejectedBy_ID);

	/** Get Eval Rejected By	  */
	public int getZZ_EvalRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_EvalRejectedBy() throws RuntimeException;

    /** Column name ZZ_EvalRejectedDate */
    public static final String COLUMNNAME_ZZ_EvalRejectedDate = "ZZ_EvalRejectedDate";

	/** Set Eval Rejected Date	  */
	public void setZZ_EvalRejectedDate (Timestamp ZZ_EvalRejectedDate);

	/** Get Eval Rejected Date	  */
	public Timestamp getZZ_EvalRejectedDate();

    /** Column name ZZ_EvaluatedBy_ID */
    public static final String COLUMNNAME_ZZ_EvaluatedBy_ID = "ZZ_EvaluatedBy_ID";

	/** Set Evaluated By	  */
	public void setZZ_EvaluatedBy_ID (int ZZ_EvaluatedBy_ID);

	/** Get Evaluated By	  */
	public int getZZ_EvaluatedBy_ID();

	public org.compiere.model.I_AD_User getZZ_EvaluatedBy() throws RuntimeException;

    /** Column name ZZ_EvaluatedDate */
    public static final String COLUMNNAME_ZZ_EvaluatedDate = "ZZ_EvaluatedDate";

	/** Set Evaluated Date	  */
	public void setZZ_EvaluatedDate (Timestamp ZZ_EvaluatedDate);

	/** Get Evaluated Date	  */
	public Timestamp getZZ_EvaluatedDate();

    /** Column name ZZ_Generate_WSP_ATR_Report */
    public static final String COLUMNNAME_ZZ_Generate_WSP_ATR_Report = "ZZ_Generate_WSP_ATR_Report";

	/** Set Generate WSP ATR Report	  */
	public void setZZ_Generate_WSP_ATR_Report (String ZZ_Generate_WSP_ATR_Report);

	/** Get Generate WSP ATR Report	  */
	public String getZZ_Generate_WSP_ATR_Report();

    /** Column name ZZ_Import_Submitted_Data */
    public static final String COLUMNNAME_ZZ_Import_Submitted_Data = "ZZ_Import_Submitted_Data";

	/** Set Import Submitted Data	  */
	public void setZZ_Import_Submitted_Data (String ZZ_Import_Submitted_Data);

	/** Get Import Submitted Data	  */
	public String getZZ_Import_Submitted_Data();

    /** Column name ZZ_IsQuery */
    public static final String COLUMNNAME_ZZ_IsQuery = "ZZ_IsQuery";

	/** Set Is Query	  */
	public void setZZ_IsQuery (boolean ZZ_IsQuery);

	/** Get Is Query	  */
	public boolean isZZ_IsQuery();

    /** Column name ZZ_QueryComment */
    public static final String COLUMNNAME_ZZ_QueryComment = "ZZ_QueryComment";

	/** Set Query Comment	  */
	public void setZZ_QueryComment (String ZZ_QueryComment);

	/** Get Query Comment	  */
	public String getZZ_QueryComment();

    /** Column name ZZ_RecRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_RecRejectedBy_ID = "ZZ_RecRejectedBy_ID";

	/** Set Rec Rejected By	  */
	public void setZZ_RecRejectedBy_ID (int ZZ_RecRejectedBy_ID);

	/** Get Rec Rejected By	  */
	public int getZZ_RecRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_RecRejectedBy() throws RuntimeException;

    /** Column name ZZ_RecRejectedDate */
    public static final String COLUMNNAME_ZZ_RecRejectedDate = "ZZ_RecRejectedDate";

	/** Set Rec Rejected Date	  */
	public void setZZ_RecRejectedDate (Timestamp ZZ_RecRejectedDate);

	/** Get Rec Rejected Date	  */
	public Timestamp getZZ_RecRejectedDate();

    /** Column name ZZ_RecommendedBy_ID */
    public static final String COLUMNNAME_ZZ_RecommendedBy_ID = "ZZ_RecommendedBy_ID";

	/** Set Recommended By	  */
	public void setZZ_RecommendedBy_ID (int ZZ_RecommendedBy_ID);

	/** Get Recommended By	  */
	public int getZZ_RecommendedBy_ID();

	public org.compiere.model.I_AD_User getZZ_RecommendedBy() throws RuntimeException;

    /** Column name ZZ_RecommendedDate */
    public static final String COLUMNNAME_ZZ_RecommendedDate = "ZZ_RecommendedDate";

	/** Set Recommended Date	  */
	public void setZZ_RecommendedDate (Timestamp ZZ_RecommendedDate);

	/** Get Recommended Date	  */
	public Timestamp getZZ_RecommendedDate();

    /** Column name ZZ_ResubmitRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_ResubmitRejectedBy_ID = "ZZ_ResubmitRejectedBy_ID";

	/** Set Resubmit Rejected By	  */
	public void setZZ_ResubmitRejectedBy_ID (int ZZ_ResubmitRejectedBy_ID);

	/** Get Resubmit Rejected By	  */
	public int getZZ_ResubmitRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_ResubmitRejectedBy() throws RuntimeException;

    /** Column name ZZ_ResubmitRejectedDate */
    public static final String COLUMNNAME_ZZ_ResubmitRejectedDate = "ZZ_ResubmitRejectedDate";

	/** Set Resubmit Rejected Date	  */
	public void setZZ_ResubmitRejectedDate (Timestamp ZZ_ResubmitRejectedDate);

	/** Get Resubmit Rejected Date	  */
	public Timestamp getZZ_ResubmitRejectedDate();

    /** Column name ZZ_ResubmittedBy_ID */
    public static final String COLUMNNAME_ZZ_ResubmittedBy_ID = "ZZ_ResubmittedBy_ID";

	/** Set Resubmitted By	  */
	public void setZZ_ResubmittedBy_ID (int ZZ_ResubmittedBy_ID);

	/** Get Resubmitted By	  */
	public int getZZ_ResubmittedBy_ID();

	public org.compiere.model.I_AD_User getZZ_ResubmittedBy() throws RuntimeException;

    /** Column name ZZ_ResubmittedDate */
    public static final String COLUMNNAME_ZZ_ResubmittedDate = "ZZ_ResubmittedDate";

	/** Set Resubmitted Date	  */
	public void setZZ_ResubmittedDate (Timestamp ZZ_ResubmittedDate);

	/** Get Resubmitted Date	  */
	public Timestamp getZZ_ResubmittedDate();

    /** Column name ZZ_SubmitRejectedBy_ID */
    public static final String COLUMNNAME_ZZ_SubmitRejectedBy_ID = "ZZ_SubmitRejectedBy_ID";

	/** Set Submit Rejected By	  */
	public void setZZ_SubmitRejectedBy_ID (int ZZ_SubmitRejectedBy_ID);

	/** Get Submit Rejected By	  */
	public int getZZ_SubmitRejectedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SubmitRejectedBy() throws RuntimeException;

    /** Column name ZZ_SubmitRejectedDate */
    public static final String COLUMNNAME_ZZ_SubmitRejectedDate = "ZZ_SubmitRejectedDate";

	/** Set Submit Rejected Date	  */
	public void setZZ_SubmitRejectedDate (Timestamp ZZ_SubmitRejectedDate);

	/** Get Submit Rejected Date	  */
	public Timestamp getZZ_SubmitRejectedDate();

    /** Column name ZZ_SubmittedBy_ID */
    public static final String COLUMNNAME_ZZ_SubmittedBy_ID = "ZZ_SubmittedBy_ID";

	/** Set Submitted By	  */
	public void setZZ_SubmittedBy_ID (int ZZ_SubmittedBy_ID);

	/** Get Submitted By	  */
	public int getZZ_SubmittedBy_ID();

	public org.compiere.model.I_AD_User getZZ_SubmittedBy() throws RuntimeException;

    /** Column name ZZ_SubmittedDate */
    public static final String COLUMNNAME_ZZ_SubmittedDate = "ZZ_SubmittedDate";

	/** Set Submitted Date	  */
	public void setZZ_SubmittedDate (Timestamp ZZ_SubmittedDate);

	/** Get Submitted Date	  */
	public Timestamp getZZ_SubmittedDate();

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
