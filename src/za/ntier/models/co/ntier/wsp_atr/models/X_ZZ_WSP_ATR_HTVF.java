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
package za.co.ntier.wsp_atr.models;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for ZZ_WSP_ATR_HTVF
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_HTVF")
public class X_ZZ_WSP_ATR_HTVF extends PO implements I_ZZ_WSP_ATR_HTVF, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260222L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_HTVF (Properties ctx, int ZZ_WSP_ATR_HTVF_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_HTVF_ID, trxName);
      /** if (ZZ_WSP_ATR_HTVF_ID == 0)
        {
			setZZ_WSP_ATR_HTVF_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_HTVF (Properties ctx, int ZZ_WSP_ATR_HTVF_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_HTVF_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_HTVF_ID == 0)
        {
			setZZ_WSP_ATR_HTVF_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_HTVF (Properties ctx, String ZZ_WSP_ATR_HTVF_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_HTVF_UU, trxName);
      /** if (ZZ_WSP_ATR_HTVF_UU == null)
        {
			setZZ_WSP_ATR_HTVF_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_HTVF (Properties ctx, String ZZ_WSP_ATR_HTVF_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_HTVF_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_HTVF_UU == null)
        {
			setZZ_WSP_ATR_HTVF_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_HTVF (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_HTVF[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Row No.
		@param Row_No Row No
	*/
	public void setRow_No (int Row_No)
	{
		set_Value (COLUMNNAME_Row_No, Integer.valueOf(Row_No));
	}

	/** Get Row No.
		@return Row No	  */
	public int getRow_No()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Row_No);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Scarce_Reason_Ref getZZ_Further_Scarce_Reason2() throws RuntimeException
	{
		return (I_ZZ_Scarce_Reason_Ref)MTable.get(getCtx(), I_ZZ_Scarce_Reason_Ref.Table_ID)
			.getPO(getZZ_Further_Scarce_Reason2_ID(), get_TrxName());
	}

	/** Set Further Scarce Reason 2.
		@param ZZ_Further_Scarce_Reason2_ID Further Scarce Reason 2
	*/
	public void setZZ_Further_Scarce_Reason2_ID (int ZZ_Further_Scarce_Reason2_ID)
	{
		if (ZZ_Further_Scarce_Reason2_ID < 1)
			set_Value (COLUMNNAME_ZZ_Further_Scarce_Reason2_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Further_Scarce_Reason2_ID, Integer.valueOf(ZZ_Further_Scarce_Reason2_ID));
	}

	/** Get Further Scarce Reason 2.
		@return Further Scarce Reason 2	  */
	public int getZZ_Further_Scarce_Reason2_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Further_Scarce_Reason2_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Scarce_Reason_Ref getZZ_Further_Scarce_Reason() throws RuntimeException
	{
		return (I_ZZ_Scarce_Reason_Ref)MTable.get(getCtx(), I_ZZ_Scarce_Reason_Ref.Table_ID)
			.getPO(getZZ_Further_Scarce_Reason_ID(), get_TrxName());
	}

	/** Set Further Scarce Reason.
		@param ZZ_Further_Scarce_Reason_ID Further Scarce Reason
	*/
	public void setZZ_Further_Scarce_Reason_ID (int ZZ_Further_Scarce_Reason_ID)
	{
		if (ZZ_Further_Scarce_Reason_ID < 1)
			set_Value (COLUMNNAME_ZZ_Further_Scarce_Reason_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Further_Scarce_Reason_ID, Integer.valueOf(ZZ_Further_Scarce_Reason_ID));
	}

	/** Get Further Scarce Reason.
		@return Further Scarce Reason	  */
	public int getZZ_Further_Scarce_Reason_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Further_Scarce_Reason_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Occupations_Ref getZZ_Occupations() throws RuntimeException
	{
		return (I_ZZ_Occupations_Ref)MTable.get(getCtx(), I_ZZ_Occupations_Ref.Table_ID)
			.getPO(getZZ_Occupations_ID(), get_TrxName());
	}

	/** Set Occupation.
		@param ZZ_Occupations_ID Occupation
	*/
	public void setZZ_Occupations_ID (int ZZ_Occupations_ID)
	{
		if (ZZ_Occupations_ID < 1)
			set_Value (COLUMNNAME_ZZ_Occupations_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Occupations_ID, Integer.valueOf(ZZ_Occupations_ID));
	}

	/** Get Occupation.
		@return Occupation	  */
	public int getZZ_Occupations_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Occupations_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Other Reasons/Comments.
		@param ZZ_Scarce_Other_Reasons_Comments Other Reasons/Comments
	*/
	public void setZZ_Scarce_Other_Reasons_Comments (String ZZ_Scarce_Other_Reasons_Comments)
	{
		set_Value (COLUMNNAME_ZZ_Scarce_Other_Reasons_Comments, ZZ_Scarce_Other_Reasons_Comments);
	}

	/** Get Other Reasons/Comments.
		@return Other Reasons/Comments	  */
	public String getZZ_Scarce_Other_Reasons_Comments()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Scarce_Other_Reasons_Comments);
	}

	public I_ZZ_Scarce_Reason_Ref getZZ_Scarce_Reason() throws RuntimeException
	{
		return (I_ZZ_Scarce_Reason_Ref)MTable.get(getCtx(), I_ZZ_Scarce_Reason_Ref.Table_ID)
			.getPO(getZZ_Scarce_Reason_ID(), get_TrxName());
	}

	/** Set Scarce Reason.
		@param ZZ_Scarce_Reason_ID Scarce Reason
	*/
	public void setZZ_Scarce_Reason_ID (int ZZ_Scarce_Reason_ID)
	{
		if (ZZ_Scarce_Reason_ID < 1)
			set_Value (COLUMNNAME_ZZ_Scarce_Reason_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Scarce_Reason_ID, Integer.valueOf(ZZ_Scarce_Reason_ID));
	}

	/** Get Scarce Reason.
		@return Scarce Reason	  */
	public int getZZ_Scarce_Reason_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Scarce_Reason_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Vacancies EC Count.
		@param ZZ_Vacancies_EC_Cnt Vacancies EC Count
	*/
	public void setZZ_Vacancies_EC_Cnt (BigDecimal ZZ_Vacancies_EC_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_EC_Cnt, ZZ_Vacancies_EC_Cnt);
	}

	/** Get Vacancies EC Count.
		@return Vacancies EC Count	  */
	public BigDecimal getZZ_Vacancies_EC_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_EC_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies FS Count.
		@param ZZ_Vacancies_FS_Cnt Vacancies FS Count
	*/
	public void setZZ_Vacancies_FS_Cnt (BigDecimal ZZ_Vacancies_FS_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_FS_Cnt, ZZ_Vacancies_FS_Cnt);
	}

	/** Get Vacancies FS Count.
		@return Vacancies FS Count	  */
	public BigDecimal getZZ_Vacancies_FS_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_FS_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies GP Cnt.
		@param ZZ_Vacancies_GP_Cnt Vacancies GP Cnt
	*/
	public void setZZ_Vacancies_GP_Cnt (BigDecimal ZZ_Vacancies_GP_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_GP_Cnt, ZZ_Vacancies_GP_Cnt);
	}

	/** Get Vacancies GP Cnt.
		@return Vacancies GP Cnt	  */
	public BigDecimal getZZ_Vacancies_GP_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_GP_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies KZN Count.
		@param ZZ_Vacancies_KZN_Cnt Vacancies KZN Count
	*/
	public void setZZ_Vacancies_KZN_Cnt (BigDecimal ZZ_Vacancies_KZN_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_KZN_Cnt, ZZ_Vacancies_KZN_Cnt);
	}

	/** Get Vacancies KZN Count.
		@return Vacancies KZN Count	  */
	public BigDecimal getZZ_Vacancies_KZN_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_KZN_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies LP Count.
		@param ZZ_Vacancies_LP_Cnt Vacancies LP Count
	*/
	public void setZZ_Vacancies_LP_Cnt (BigDecimal ZZ_Vacancies_LP_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_LP_Cnt, ZZ_Vacancies_LP_Cnt);
	}

	/** Get Vacancies LP Count.
		@return Vacancies LP Count	  */
	public BigDecimal getZZ_Vacancies_LP_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_LP_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies MP Count.
		@param ZZ_Vacancies_MP_Cnt Vacancies MP Count
	*/
	public void setZZ_Vacancies_MP_Cnt (BigDecimal ZZ_Vacancies_MP_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_MP_Cnt, ZZ_Vacancies_MP_Cnt);
	}

	/** Get Vacancies MP Count.
		@return Vacancies MP Count	  */
	public BigDecimal getZZ_Vacancies_MP_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_MP_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies NP Count.
		@param ZZ_Vacancies_NP_Cnt Vacancies NP Count
	*/
	public void setZZ_Vacancies_NP_Cnt (BigDecimal ZZ_Vacancies_NP_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_NP_Cnt, ZZ_Vacancies_NP_Cnt);
	}

	/** Get Vacancies NP Count.
		@return Vacancies NP Count	  */
	public BigDecimal getZZ_Vacancies_NP_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_NP_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies NW Count.
		@param ZZ_Vacancies_NW_Cnt Vacancies NW Count
	*/
	public void setZZ_Vacancies_NW_Cnt (BigDecimal ZZ_Vacancies_NW_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_NW_Cnt, ZZ_Vacancies_NW_Cnt);
	}

	/** Get Vacancies NW Count.
		@return Vacancies NW Count	  */
	public BigDecimal getZZ_Vacancies_NW_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_NW_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Vacancies WC Count.
		@param ZZ_Vacancies_WC_Cnt Vacancies WC Count
	*/
	public void setZZ_Vacancies_WC_Cnt (BigDecimal ZZ_Vacancies_WC_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Vacancies_WC_Cnt, ZZ_Vacancies_WC_Cnt);
	}

	/** Get Vacancies WC Count.
		@return Vacancies WC Count	  */
	public BigDecimal getZZ_Vacancies_WC_Cnt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ZZ_Vacancies_WC_Cnt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set WSP/ATR HTVF Detail.
		@param ZZ_WSP_ATR_HTVF_ID WSP/ATR HTVF Detail
	*/
	public void setZZ_WSP_ATR_HTVF_ID (int ZZ_WSP_ATR_HTVF_ID)
	{
		if (ZZ_WSP_ATR_HTVF_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_HTVF_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_HTVF_ID, Integer.valueOf(ZZ_WSP_ATR_HTVF_ID));
	}

	/** Get WSP/ATR HTVF Detail.
		@return WSP/ATR HTVF Detail	  */
	public int getZZ_WSP_ATR_HTVF_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_HTVF_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_HTVF_UU.
		@param ZZ_WSP_ATR_HTVF_UU ZZ_WSP_ATR_HTVF_UU
	*/
	public void setZZ_WSP_ATR_HTVF_UU (String ZZ_WSP_ATR_HTVF_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_HTVF_UU, ZZ_WSP_ATR_HTVF_UU);
	}

	/** Get ZZ_WSP_ATR_HTVF_UU.
		@return ZZ_WSP_ATR_HTVF_UU	  */
	public String getZZ_WSP_ATR_HTVF_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_HTVF_UU);
	}

	public I_ZZ_WSP_ATR_Submitted getZZ_WSP_ATR_Submitted() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Submitted)MTable.get(getCtx(), I_ZZ_WSP_ATR_Submitted.Table_ID)
			.getPO(getZZ_WSP_ATR_Submitted_ID(), get_TrxName());
	}

	/** Set WSP/ATR Submitted File.
		@param ZZ_WSP_ATR_Submitted_ID WSP/ATR Submitted File
	*/
	public void setZZ_WSP_ATR_Submitted_ID (int ZZ_WSP_ATR_Submitted_ID)
	{
		if (ZZ_WSP_ATR_Submitted_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Submitted_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Submitted_ID, Integer.valueOf(ZZ_WSP_ATR_Submitted_ID));
	}

	/** Get WSP/ATR Submitted File.
		@return WSP/ATR Submitted File
	  */
	public int getZZ_WSP_ATR_Submitted_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Submitted_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}