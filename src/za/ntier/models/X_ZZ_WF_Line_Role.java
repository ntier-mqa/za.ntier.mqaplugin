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
/** Generated Model - DO NOT CHANGE */
package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_WF_Line_Role
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WF_Line_Role")
public class X_ZZ_WF_Line_Role extends PO implements I_ZZ_WF_Line_Role, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260430L;

    /** Standard Constructor */
    public X_ZZ_WF_Line_Role (Properties ctx, int ZZ_WF_Line_Role_ID, String trxName)
    {
      super (ctx, ZZ_WF_Line_Role_ID, trxName);
      /** if (ZZ_WF_Line_Role_ID == 0)
        {
			setAD_Role_ID (0);
			setZZ_Is_Responsible (false);
// N
			setZZ_Notify (false);
// N
			setZZ_WF_Line_Role_ID (0);
			setZZ_WF_Lines_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Line_Role (Properties ctx, int ZZ_WF_Line_Role_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WF_Line_Role_ID, trxName, virtualColumns);
      /** if (ZZ_WF_Line_Role_ID == 0)
        {
			setAD_Role_ID (0);
			setZZ_Is_Responsible (false);
// N
			setZZ_Notify (false);
// N
			setZZ_WF_Line_Role_ID (0);
			setZZ_WF_Lines_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Line_Role (Properties ctx, String ZZ_WF_Line_Role_UU, String trxName)
    {
      super (ctx, ZZ_WF_Line_Role_UU, trxName);
      /** if (ZZ_WF_Line_Role_UU == null)
        {
			setAD_Role_ID (0);
			setZZ_Is_Responsible (false);
// N
			setZZ_Notify (false);
// N
			setZZ_WF_Line_Role_ID (0);
			setZZ_WF_Lines_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Line_Role (Properties ctx, String ZZ_WF_Line_Role_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WF_Line_Role_UU, trxName, virtualColumns);
      /** if (ZZ_WF_Line_Role_UU == null)
        {
			setAD_Role_ID (0);
			setZZ_Is_Responsible (false);
// N
			setZZ_Notify (false);
// N
			setZZ_WF_Line_Role_ID (0);
			setZZ_WF_Lines_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WF_Line_Role (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_ZZ_WF_Line_Role[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Role)MTable.get(getCtx(), org.compiere.model.I_AD_Role.Table_ID)
			.getPO(getAD_Role_ID(), get_TrxName());
	}

	/** Set Role.
		@param AD_Role_ID Responsibility Role
	*/
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0)
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Is Responsible.
		@param ZZ_Is_Responsible Is Responsible
	*/
	public void setZZ_Is_Responsible (boolean ZZ_Is_Responsible)
	{
		set_Value (COLUMNNAME_ZZ_Is_Responsible, Boolean.valueOf(ZZ_Is_Responsible));
	}

	/** Get Is Responsible.
		@return Is Responsible	  */
	public boolean isZZ_Is_Responsible()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Is_Responsible);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Approved By Manager Finance Consumables = AC */
	public static final String ZZ_NEXTSTATUS_ApprovedByManagerFinanceConsumables = "AC";
	/** Approved = AP */
	public static final String ZZ_NEXTSTATUS_Approved = "AP";
	/** Prepared for CEO = CF */
	public static final String ZZ_NEXTSTATUS_PreparedForCEO = "CF";
	/** Completed = CO */
	public static final String ZZ_NEXTSTATUS_Completed = "CO";
	/** Draft = DR */
	public static final String ZZ_NEXTSTATUS_Draft = "DR";
	/** Error Importing = EE */
	public static final String ZZ_NEXTSTATUS_ErrorImporting = "EE";
	/** Validation Error = ER */
	public static final String ZZ_NEXTSTATUS_ValidationError = "ER";
	/** Evaluated = EV */
	public static final String ZZ_NEXTSTATUS_Evaluated = "EV";
	/** Importing = IG */
	public static final String ZZ_NEXTSTATUS_Importing = "IG";
	/** Imported = IM */
	public static final String ZZ_NEXTSTATUS_Imported = "IM";
	/** In Progress = IP */
	public static final String ZZ_NEXTSTATUS_InProgress = "IP";
	/** Not Recommended By Senior Mgr SDR = N1 */
	public static final String ZZ_NEXTSTATUS_NotRecommendedBySeniorMgrSDR = "N1";
	/** Not Recommended By Senior Mgr Finance = N2 */
	public static final String ZZ_NEXTSTATUS_NotRecommendedBySeniorMgrFinance = "N2";
	/** Not Recommended By COO = N3 */
	public static final String ZZ_NEXTSTATUS_NotRecommendedByCOO = "N3";
	/** Not Recommended By CFO = N4 */
	public static final String ZZ_NEXTSTATUS_NotRecommendedByCFO = "N4";
	/** Not Recommended By CEO = N5 */
	public static final String ZZ_NEXTSTATUS_NotRecommendedByCEO = "N5";
	/** Not Approved by Snr Manager = NA */
	public static final String ZZ_NEXTSTATUS_NotApprovedBySnrManager = "NA";
	/** Not Approved By Manager Finance Consumables = NC */
	public static final String ZZ_NEXTSTATUS_NotApprovedByManagerFinanceConsumables = "NC";
	/** Not Approved By SDL Finance Mgr = ND */
	public static final String ZZ_NEXTSTATUS_NotApprovedBySDLFinanceMgr = "ND";
	/** Not Approved By IT Manager = NI */
	public static final String ZZ_NEXTSTATUS_NotApprovedByITManager = "NI";
	/** Not Approved by LM = NL */
	public static final String ZZ_NEXTSTATUS_NotApprovedByLM = "NL";
	/** Not Approved = NP */
	public static final String ZZ_NEXTSTATUS_NotApproved = "NP";
	/** Not Recommended = NR */
	public static final String ZZ_NEXTSTATUS_NotRecommended = "NR";
	/** Not Approved by Snr Admin Finance = NS */
	public static final String ZZ_NEXTSTATUS_NotApprovedBySnrAdminFinance = "NS";
	/** Pending = PE */
	public static final String ZZ_NEXTSTATUS_Pending = "PE";
	/** Query = QR */
	public static final String ZZ_NEXTSTATUS_Query = "QR";
	/** Recommended By Senior Mgr Finance = R1 */
	public static final String ZZ_NEXTSTATUS_RecommendedBySeniorMgrFinance = "R1";
	/** Recommended By COO = R2 */
	public static final String ZZ_NEXTSTATUS_RecommendedByCOO = "R2";
	/** Recommended By CFO = R3 */
	public static final String ZZ_NEXTSTATUS_RecommendedByCFO = "R3";
	/** Recommended By CEO = R4 */
	public static final String ZZ_NEXTSTATUS_RecommendedByCEO = "R4";
	/** Recommended for Approval = RA */
	public static final String ZZ_NEXTSTATUS_RecommendedForApproval = "RA";
	/** Recommended = RC */
	public static final String ZZ_NEXTSTATUS_Recommended = "RC";
	/** Recommended By Senior Mgr SDR = RD */
	public static final String ZZ_NEXTSTATUS_RecommendedBySeniorMgrSDR = "RD";
	/** Recommended for Evaluation = RE */
	public static final String ZZ_NEXTSTATUS_RecommendedForEvaluation = "RE";
	/** Submitted to Manager Finance Consumables = SC */
	public static final String ZZ_NEXTSTATUS_SubmittedToManagerFinanceConsumables = "SC";
	/** Submitted To SDL Finance Mgr = SD */
	public static final String ZZ_NEXTSTATUS_SubmittedToSDLFinanceMgr = "SD";
	/** Submitted To IT Manager = SI */
	public static final String ZZ_NEXTSTATUS_SubmittedToITManager = "SI";
	/** Submitted To IT Admin = ST */
	public static final String ZZ_NEXTSTATUS_SubmittedToITAdmin = "ST";
	/** Submitted = SU */
	public static final String ZZ_NEXTSTATUS_Submitted = "SU";
	/** Transfer Out = TO */
	public static final String ZZ_NEXTSTATUS_TransferOut = "TO";
	/** Updated by SDR Admin = UA */
	public static final String ZZ_NEXTSTATUS_UpdatedBySDRAdmin = "UA";
	/** Uploaded = UP */
	public static final String ZZ_NEXTSTATUS_Uploaded = "UP";
	/** Delinked = UnSdfOrg */
	public static final String ZZ_NEXTSTATUS_Delinked = "UnSdfOrg";
	/** Validating = VA */
	public static final String ZZ_NEXTSTATUS_Validating = "VA";
	/** Verified = VE */
	public static final String ZZ_NEXTSTATUS_Verified = "VE";
	/** Set Next Status.
		@param ZZ_NextStatus Next Status
	*/
	public void setZZ_NextStatus (String ZZ_NextStatus)
	{

		set_Value (COLUMNNAME_ZZ_NextStatus, ZZ_NextStatus);
	}

	/** Get Next Status.
		@return Next Status	  */
	public String getZZ_NextStatus()
	{
		return (String)get_Value(COLUMNNAME_ZZ_NextStatus);
	}

	/** Set Notify.
		@param ZZ_Notify Notify
	*/
	public void setZZ_Notify (boolean ZZ_Notify)
	{
		set_Value (COLUMNNAME_ZZ_Notify, Boolean.valueOf(ZZ_Notify));
	}

	/** Get Notify.
		@return Notify	  */
	public boolean isZZ_Notify()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Notify);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set ZZ_WF_Line_Role.
		@param ZZ_WF_Line_Role_ID ZZ_WF_Line_Role
	*/
	public void setZZ_WF_Line_Role_ID (int ZZ_WF_Line_Role_ID)
	{
		if (ZZ_WF_Line_Role_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Line_Role_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Line_Role_ID, Integer.valueOf(ZZ_WF_Line_Role_ID));
	}

	/** Get ZZ_WF_Line_Role.
		@return ZZ_WF_Line_Role	  */
	public int getZZ_WF_Line_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WF_Line_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WF_Line_Role_UU.
		@param ZZ_WF_Line_Role_UU ZZ_WF_Line_Role_UU
	*/
	public void setZZ_WF_Line_Role_UU (String ZZ_WF_Line_Role_UU)
	{
		set_Value (COLUMNNAME_ZZ_WF_Line_Role_UU, ZZ_WF_Line_Role_UU);
	}

	/** Get ZZ_WF_Line_Role_UU.
		@return ZZ_WF_Line_Role_UU	  */
	public String getZZ_WF_Line_Role_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WF_Line_Role_UU);
	}

	public I_ZZ_WF_Lines getZZ_WF_Lines() throws RuntimeException
	{
		return (I_ZZ_WF_Lines)MTable.get(getCtx(), I_ZZ_WF_Lines.Table_ID)
			.getPO(getZZ_WF_Lines_ID(), get_TrxName());
	}

	/** Set ZZ_WF_Lines.
		@param ZZ_WF_Lines_ID ZZ_WF_Lines
	*/
	public void setZZ_WF_Lines_ID (int ZZ_WF_Lines_ID)
	{
		if (ZZ_WF_Lines_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Lines_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Lines_ID, Integer.valueOf(ZZ_WF_Lines_ID));
	}

	/** Get ZZ_WF_Lines.
		@return ZZ_WF_Lines	  */
	public int getZZ_WF_Lines_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WF_Lines_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}