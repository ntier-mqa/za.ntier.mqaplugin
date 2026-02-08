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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for ZZ_WSP_ATR_Submitted
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Submitted")
public class X_ZZ_WSP_ATR_Submitted extends PO implements I_ZZ_WSP_ATR_Submitted, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260130L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_ID, trxName);
      /** if (ZZ_WSP_ATR_Submitted_ID == 0)
        {
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Submitted_ID == 0)
        {
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_UU, trxName);
      /** if (ZZ_WSP_ATR_Submitted_UU == null)
        {
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Submitted_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Submitted_UU == null)
        {
			setZZ_WSP_ATR_Submitted_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Submitted (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Submitted[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set File Name.
		@param FileName Name of the local file or URL
	*/
	public void setFileName (String FileName)
	{
		set_Value (COLUMNNAME_FileName, FileName);
	}

	/** Get File Name.
		@return Name of the local file or URL
	  */
	public String getFileName()
	{
		return (String)get_Value(COLUMNNAME_FileName);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Submitted Date.
		@param SubmittedDate Submitted Date
	*/
	public void setSubmittedDate (Timestamp SubmittedDate)
	{
		set_Value (COLUMNNAME_SubmittedDate, SubmittedDate);
	}

	/** Get Submitted Date.
		@return Submitted Date	  */
	public Timestamp getSubmittedDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_SubmittedDate);
	}

	/** Set SDF Organisation.
		@param ZZSdfOrganisation_ID Link Organisation And SDF
	*/
	public void setZZSdfOrganisation_ID (int ZZSdfOrganisation_ID)
	{
		if (ZZSdfOrganisation_ID < 1)
			set_Value (COLUMNNAME_ZZSdfOrganisation_ID, null);
		else
			set_Value (COLUMNNAME_ZZSdfOrganisation_ID, Integer.valueOf(ZZSdfOrganisation_ID));
	}

	/** Get SDF Organisation.
		@return Link Organisation And SDF
	  */
	public int getZZSdfOrganisation_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZSdfOrganisation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Import Submitted Data.
		@param ZZ_Import_Submitted_Data Import Submitted Data
	*/
	public void setZZ_Import_Submitted_Data (String ZZ_Import_Submitted_Data)
	{
		set_Value (COLUMNNAME_ZZ_Import_Submitted_Data, ZZ_Import_Submitted_Data);
	}

	/** Get Import Submitted Data.
		@return Import Submitted Data	  */
	public String getZZ_Import_Submitted_Data()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Import_Submitted_Data);
	}

	/** Draft = DR */
	public static final String ZZ_WSP_ATR_STATUS_Draft = "DR";
	/** Error Importing = EE */
	public static final String ZZ_WSP_ATR_STATUS_ErrorImporting = "EE";
	/** Validation Error = ER */
	public static final String ZZ_WSP_ATR_STATUS_ValidationError = "ER";
	/** Imported = IM */
	public static final String ZZ_WSP_ATR_STATUS_Imported = "IM";
	/** Importing = IP */
	public static final String ZZ_WSP_ATR_STATUS_Importing = "IP";
	/** Validating = VA */
	public static final String ZZ_WSP_ATR_STATUS_Validating = "VA";
	/** Set Status.
		@param ZZ_WSP_ATR_Status Status
	*/
	public void setZZ_WSP_ATR_Status (String ZZ_WSP_ATR_Status)
	{

		set_Value (COLUMNNAME_ZZ_WSP_ATR_Status, ZZ_WSP_ATR_Status);
	}

	/** Get Status.
		@return Status	  */
	public String getZZ_WSP_ATR_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Status);
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

	/** Set ZZ_WSP_ATR_Submitted_UU.
		@param ZZ_WSP_ATR_Submitted_UU ZZ_WSP_ATR_Submitted_UU
	*/
	public void setZZ_WSP_ATR_Submitted_UU (String ZZ_WSP_ATR_Submitted_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Submitted_UU, ZZ_WSP_ATR_Submitted_UU);
	}

	/** Get ZZ_WSP_ATR_Submitted_UU.
		@return ZZ_WSP_ATR_Submitted_UU	  */
	public String getZZ_WSP_ATR_Submitted_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Submitted_UU);
	}
}