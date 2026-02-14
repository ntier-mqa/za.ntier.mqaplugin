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

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep")
public class X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep extends PO implements I_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep (Properties ctx, int ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep (Properties ctx, int ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep (Properties ctx, String ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU == null)
        {
			setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep (Properties ctx, String ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU == null)
        {
			setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Ex Leave Planned.
		@param ZZ_Ex_Leave_Planned_Cnt Ex Leave Planned
	*/
	public void setZZ_Ex_Leave_Planned_Cnt (int ZZ_Ex_Leave_Planned_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Ex_Leave_Planned_Cnt, Integer.valueOf(ZZ_Ex_Leave_Planned_Cnt));
	}

	/** Get Ex Leave Planned.
		@return Ex Leave Planned	  */
	public int getZZ_Ex_Leave_Planned_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Ex_Leave_Planned_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Ex Leave Trained .
		@param ZZ_Ex_Leave_Trained_Cnt Ex Leave Trained 
	*/
	public void setZZ_Ex_Leave_Trained_Cnt (int ZZ_Ex_Leave_Trained_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Ex_Leave_Trained_Cnt, Integer.valueOf(ZZ_Ex_Leave_Trained_Cnt));
	}

	/** Get Ex Leave Trained .
		@return Ex Leave Trained 	  */
	public int getZZ_Ex_Leave_Trained_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Ex_Leave_Trained_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Induction Planned.
		@param ZZ_Induction_Planned_Cnt Induction Planned
	*/
	public void setZZ_Induction_Planned_Cnt (int ZZ_Induction_Planned_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Induction_Planned_Cnt, Integer.valueOf(ZZ_Induction_Planned_Cnt));
	}

	/** Get Induction Planned.
		@return Induction Planned	  */
	public int getZZ_Induction_Planned_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Induction_Planned_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Induction Trained.
		@param ZZ_Induction_Trained_Cnt Induction Trained
	*/
	public void setZZ_Induction_Trained_Cnt (int ZZ_Induction_Trained_Cnt)
	{
		set_Value (COLUMNNAME_ZZ_Induction_Trained_Cnt, Integer.valueOf(ZZ_Induction_Trained_Cnt));
	}

	/** Get Induction Trained.
		@return Induction Trained	  */
	public int getZZ_Induction_Trained_Cnt()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Induction_Trained_Cnt);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Refresher Planned.
		@param ZZ_Refresher_Planned Refresher Planned
	*/
	public void setZZ_Refresher_Planned (int ZZ_Refresher_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Refresher_Planned, Integer.valueOf(ZZ_Refresher_Planned));
	}

	/** Get Refresher Planned.
		@return Refresher Planned	  */
	public int getZZ_Refresher_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Refresher_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Refresher Trained.
		@param ZZ_Refresher_Trained Refresher Trained
	*/
	public void setZZ_Refresher_Trained (int ZZ_Refresher_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Refresher_Trained, Integer.valueOf(ZZ_Refresher_Trained));
	}

	/** Get Refresher Trained.
		@return Refresher Trained	  */
	public int getZZ_Refresher_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Refresher_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Induction, Ex-leave and Refresher training done and planned.
		@param ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID Summary of induction / ex-leave and refresher training done and planned. Please note each person was counted once, regardless of the number of interventions
	*/
	public void setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID (int ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID)
	{
		if (ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID));
	}

	/** Get Induction, Ex-leave and Refresher training done and planned.
		@return Summary of induction / ex-leave and refresher training done and planned. Please note each person was counted once, regardless of the number of interventions
	  */
	public int getZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU.
		@param ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU
	*/
	public void setZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU (String ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU, ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU.
		@return ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU	  */
	public String getZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep_UU);
	}

	public I_ZZ_WSP_ATR_OFO_Major_Group_Ref getZZ_WSP_ATR_OFO_Major_Group_Ref() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_OFO_Major_Group_Ref)MTable.get(getCtx(), I_ZZ_WSP_ATR_OFO_Major_Group_Ref.Table_ID)
			.getPO(getZZ_WSP_ATR_OFO_Major_Group_Ref_ID(), get_TrxName());
	}

	/** Set OFO Major Group Ref.
		@param ZZ_WSP_ATR_OFO_Major_Group_Ref_ID OFO Major Group Ref reference table
	*/
	public void setZZ_WSP_ATR_OFO_Major_Group_Ref_ID (int ZZ_WSP_ATR_OFO_Major_Group_Ref_ID)
	{
		if (ZZ_WSP_ATR_OFO_Major_Group_Ref_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_OFO_Major_Group_Ref_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_OFO_Major_Group_Ref_ID, Integer.valueOf(ZZ_WSP_ATR_OFO_Major_Group_Ref_ID));
	}

	/** Get OFO Major Group Ref.
		@return OFO Major Group Ref reference table
	  */
	public int getZZ_WSP_ATR_OFO_Major_Group_Ref_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_OFO_Major_Group_Ref_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_WSP_ATR_Report getZZ_WSP_ATR_Report() throws RuntimeException
	{
		return (I_ZZ_WSP_ATR_Report)MTable.get(getCtx(), I_ZZ_WSP_ATR_Report.Table_ID)
			.getPO(getZZ_WSP_ATR_Report_ID(), get_TrxName());
	}

	/** Set WSP ATR Report .
		@param ZZ_WSP_ATR_Report_ID WSP ATR Report 
	*/
	public void setZZ_WSP_ATR_Report_ID (int ZZ_WSP_ATR_Report_ID)
	{
		if (ZZ_WSP_ATR_Report_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Report_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Report_ID, Integer.valueOf(ZZ_WSP_ATR_Report_ID));
	}

	/** Get WSP ATR Report .
		@return WSP ATR Report 	  */
	public int getZZ_WSP_ATR_Report_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Report_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}