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

/** Generated Model for ZZ_WSP_ATR_Contractors_Training_Rep
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Contractors_Training_Rep")
public class X_ZZ_WSP_ATR_Contractors_Training_Rep extends PO implements I_ZZ_WSP_ATR_Contractors_Training_Rep, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260214L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Contractors_Training_Rep (Properties ctx, int ZZ_WSP_ATR_Contractors_Training_Rep_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Contractors_Training_Rep_ID, trxName);
      /** if (ZZ_WSP_ATR_Contractors_Training_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Contractors_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Contractors_Training_Rep (Properties ctx, int ZZ_WSP_ATR_Contractors_Training_Rep_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Contractors_Training_Rep_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Contractors_Training_Rep_ID == 0)
        {
			setZZ_WSP_ATR_Contractors_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Contractors_Training_Rep (Properties ctx, String ZZ_WSP_ATR_Contractors_Training_Rep_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Contractors_Training_Rep_UU, trxName);
      /** if (ZZ_WSP_ATR_Contractors_Training_Rep_UU == null)
        {
			setZZ_WSP_ATR_Contractors_Training_Rep_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Contractors_Training_Rep (Properties ctx, String ZZ_WSP_ATR_Contractors_Training_Rep_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Contractors_Training_Rep_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Contractors_Training_Rep_UU == null)
        {
			setZZ_WSP_ATR_Contractors_Training_Rep_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Contractors_Training_Rep (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Contractors_Training_Rep[")
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

	/** Set Clerical Planned.
		@param ZZ_Clerical_Planned Clerical Planned
	*/
	public void setZZ_Clerical_Planned (int ZZ_Clerical_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Clerical_Planned, Integer.valueOf(ZZ_Clerical_Planned));
	}

	/** Get Clerical Planned.
		@return Clerical Planned	  */
	public int getZZ_Clerical_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Clerical_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Clerical Trained.
		@param ZZ_Clerical_Trained Clerical Trained
	*/
	public void setZZ_Clerical_Trained (int ZZ_Clerical_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Clerical_Trained, Integer.valueOf(ZZ_Clerical_Trained));
	}

	/** Get Clerical Trained.
		@return Clerical Trained	  */
	public int getZZ_Clerical_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Clerical_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Elementary Planned.
		@param ZZ_Elementary_Planned Elementary Planned
	*/
	public void setZZ_Elementary_Planned (int ZZ_Elementary_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Elementary_Planned, Integer.valueOf(ZZ_Elementary_Planned));
	}

	/** Get Elementary Planned.
		@return Elementary Planned	  */
	public int getZZ_Elementary_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Elementary_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Elementary Trained.
		@param ZZ_Elementary_Trained Elementary Trained
	*/
	public void setZZ_Elementary_Trained (int ZZ_Elementary_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Elementary_Trained, Integer.valueOf(ZZ_Elementary_Trained));
	}

	/** Get Elementary Trained.
		@return Elementary Trained	  */
	public int getZZ_Elementary_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Elementary_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Learners Planned.
		@param ZZ_Learners_Planned Learners Planned
	*/
	public void setZZ_Learners_Planned (int ZZ_Learners_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Learners_Planned, Integer.valueOf(ZZ_Learners_Planned));
	}

	/** Get Learners Planned.
		@return Learners Planned	  */
	public int getZZ_Learners_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learners_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Learners Trained.
		@param ZZ_Learners_Trained Learners Trained
	*/
	public void setZZ_Learners_Trained (int ZZ_Learners_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Learners_Trained, Integer.valueOf(ZZ_Learners_Trained));
	}

	/** Get Learners Trained.
		@return Learners Trained	  */
	public int getZZ_Learners_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learners_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_ZZ_Contractors_Learning_Programme_Ref getZZ_Learning_Programme_Type() throws RuntimeException
	{
		return (I_ZZ_Contractors_Learning_Programme_Ref)MTable.get(getCtx(), I_ZZ_Contractors_Learning_Programme_Ref.Table_ID)
			.getPO(getZZ_Learning_Programme_Type_ID(), get_TrxName());
	}

	/** Set Learning Programme Type.
		@param ZZ_Learning_Programme_Type_ID Learning Programme Type
	*/
	public void setZZ_Learning_Programme_Type_ID (int ZZ_Learning_Programme_Type_ID)
	{
		if (ZZ_Learning_Programme_Type_ID < 1)
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_Learning_Programme_Type_ID, Integer.valueOf(ZZ_Learning_Programme_Type_ID));
	}

	/** Get Learning Programme Type.
		@return Learning Programme Type	  */
	public int getZZ_Learning_Programme_Type_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Learning_Programme_Type_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Managers Planned.
		@param ZZ_Managers_Planned Managers Planned
	*/
	public void setZZ_Managers_Planned (int ZZ_Managers_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Managers_Planned, Integer.valueOf(ZZ_Managers_Planned));
	}

	/** Get Managers Planned.
		@return Managers Planned	  */
	public int getZZ_Managers_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Managers_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Managers Trained.
		@param ZZ_Managers_Trained Managers Trained
	*/
	public void setZZ_Managers_Trained (int ZZ_Managers_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Managers_Trained, Integer.valueOf(ZZ_Managers_Trained));
	}

	/** Get Managers Trained.
		@return Managers Trained	  */
	public int getZZ_Managers_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Managers_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Plant Planned.
		@param ZZ_Plant_Planned Plant Planned
	*/
	public void setZZ_Plant_Planned (int ZZ_Plant_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Plant_Planned, Integer.valueOf(ZZ_Plant_Planned));
	}

	/** Get Plant Planned.
		@return Plant Planned	  */
	public int getZZ_Plant_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Plant_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Plant Trained.
		@param ZZ_Plant_Trained Plant Trained
	*/
	public void setZZ_Plant_Trained (int ZZ_Plant_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Plant_Trained, Integer.valueOf(ZZ_Plant_Trained));
	}

	/** Get Plant Trained.
		@return Plant Trained	  */
	public int getZZ_Plant_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Plant_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Professionals Planned.
		@param ZZ_Professionals_Planned Professionals Planned
	*/
	public void setZZ_Professionals_Planned (int ZZ_Professionals_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Professionals_Planned, Integer.valueOf(ZZ_Professionals_Planned));
	}

	/** Get Professionals Planned.
		@return Professionals Planned	  */
	public int getZZ_Professionals_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Professionals_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Professionals Trained.
		@param ZZ_Professionals_Trained Professionals Trained
	*/
	public void setZZ_Professionals_Trained (int ZZ_Professionals_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Professionals_Trained, Integer.valueOf(ZZ_Professionals_Trained));
	}

	/** Get Professionals Trained.
		@return Professionals Trained	  */
	public int getZZ_Professionals_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Professionals_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Service Planned.
		@param ZZ_Service_Planned Service Planned
	*/
	public void setZZ_Service_Planned (int ZZ_Service_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Service_Planned, Integer.valueOf(ZZ_Service_Planned));
	}

	/** Get Service Planned.
		@return Service Planned	  */
	public int getZZ_Service_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Service_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Service Trained.
		@param ZZ_Service_Trained Service Trained
	*/
	public void setZZ_Service_Trained (int ZZ_Service_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Service_Trained, Integer.valueOf(ZZ_Service_Trained));
	}

	/** Get Service Trained.
		@return Service Trained	  */
	public int getZZ_Service_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Service_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Skilled Workers Planned.
		@param ZZ_Skilled_Workers_Planned Skilled Workers Planned
	*/
	public void setZZ_Skilled_Workers_Planned (int ZZ_Skilled_Workers_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Skilled_Workers_Planned, Integer.valueOf(ZZ_Skilled_Workers_Planned));
	}

	/** Get Skilled Workers Planned.
		@return Skilled Workers Planned	  */
	public int getZZ_Skilled_Workers_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Skilled_Workers_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Skilled Workers Trained.
		@param ZZ_Skilled_Workers_Trained Skilled Workers Trained
	*/
	public void setZZ_Skilled_Workers_Trained (int ZZ_Skilled_Workers_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Skilled_Workers_Trained, Integer.valueOf(ZZ_Skilled_Workers_Trained));
	}

	/** Get Skilled Workers Trained.
		@return Skilled Workers Trained	  */
	public int getZZ_Skilled_Workers_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Skilled_Workers_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Technicians Planned.
		@param ZZ_Technicians_Planned Technicians Planned
	*/
	public void setZZ_Technicians_Planned (int ZZ_Technicians_Planned)
	{
		set_Value (COLUMNNAME_ZZ_Technicians_Planned, Integer.valueOf(ZZ_Technicians_Planned));
	}

	/** Get Technicians Planned.
		@return Technicians Planned	  */
	public int getZZ_Technicians_Planned()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Technicians_Planned);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Technicians Trained.
		@param ZZ_Technicians_Trained Technicians Trained
	*/
	public void setZZ_Technicians_Trained (int ZZ_Technicians_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Technicians_Trained, Integer.valueOf(ZZ_Technicians_Trained));
	}

	/** Get Technicians Trained.
		@return Technicians Trained	  */
	public int getZZ_Technicians_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Technicians_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Total Trained.
		@param ZZ_Total_Trained Total Trained
	*/
	public void setZZ_Total_Trained (int ZZ_Total_Trained)
	{
		set_Value (COLUMNNAME_ZZ_Total_Trained, Integer.valueOf(ZZ_Total_Trained));
	}

	/** Get Total Trained.
		@return Total Trained	  */
	public int getZZ_Total_Trained()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_Total_Trained);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Contractors: Received Training and Planned Training.
		@param ZZ_WSP_ATR_Contractors_Training_Rep_ID Contractors: Received Training and Planned Training
	*/
	public void setZZ_WSP_ATR_Contractors_Training_Rep_ID (int ZZ_WSP_ATR_Contractors_Training_Rep_ID)
	{
		if (ZZ_WSP_ATR_Contractors_Training_Rep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_ID, Integer.valueOf(ZZ_WSP_ATR_Contractors_Training_Rep_ID));
	}

	/** Get Contractors: Received Training and Planned Training.
		@return Contractors: Received Training and Planned Training	  */
	public int getZZ_WSP_ATR_Contractors_Training_Rep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Contractors_Training_Rep_UU.
		@param ZZ_WSP_ATR_Contractors_Training_Rep_UU ZZ_WSP_ATR_Contractors_Training_Rep_UU
	*/
	public void setZZ_WSP_ATR_Contractors_Training_Rep_UU (String ZZ_WSP_ATR_Contractors_Training_Rep_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_UU, ZZ_WSP_ATR_Contractors_Training_Rep_UU);
	}

	/** Get ZZ_WSP_ATR_Contractors_Training_Rep_UU.
		@return ZZ_WSP_ATR_Contractors_Training_Rep_UU	  */
	public String getZZ_WSP_ATR_Contractors_Training_Rep_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Contractors_Training_Rep_UU);
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