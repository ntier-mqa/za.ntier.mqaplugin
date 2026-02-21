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

/** Generated Model for ZZ_WSP_ATR_Uploads
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WSP_ATR_Uploads")
public class X_ZZ_WSP_ATR_Uploads extends PO implements I_ZZ_WSP_ATR_Uploads, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260221L;

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Uploads (Properties ctx, int ZZ_WSP_ATR_Uploads_ID, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Uploads_ID, trxName);
      /** if (ZZ_WSP_ATR_Uploads_ID == 0)
        {
			setName (null);
			setZZ_WSP_ATR_Uploads_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Uploads (Properties ctx, int ZZ_WSP_ATR_Uploads_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Uploads_ID, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Uploads_ID == 0)
        {
			setName (null);
			setZZ_WSP_ATR_Uploads_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Uploads (Properties ctx, String ZZ_WSP_ATR_Uploads_UU, String trxName)
    {
      super (ctx, ZZ_WSP_ATR_Uploads_UU, trxName);
      /** if (ZZ_WSP_ATR_Uploads_UU == null)
        {
			setName (null);
			setZZ_WSP_ATR_Uploads_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WSP_ATR_Uploads (Properties ctx, String ZZ_WSP_ATR_Uploads_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WSP_ATR_Uploads_UU, trxName, virtualColumns);
      /** if (ZZ_WSP_ATR_Uploads_UU == null)
        {
			setName (null);
			setZZ_WSP_ATR_Uploads_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WSP_ATR_Uploads (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WSP_ATR_Uploads[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
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

	/** Upload Attendance Register  = A */
	public static final String ZZ_WSP_ATR_UPLOAD_TYPE_UploadAttendanceRegister = "A";
	/** Upload WSP-ATR Report = R */
	public static final String ZZ_WSP_ATR_UPLOAD_TYPE_UploadWSP_ATRReport = "R";
	/** Upload Signed Minutes = S */
	public static final String ZZ_WSP_ATR_UPLOAD_TYPE_UploadSignedMinutes = "S";
	/** Set Upload Type.
		@param ZZ_WSP_ATR_Upload_Type Upload Type
	*/
	public void setZZ_WSP_ATR_Upload_Type (String ZZ_WSP_ATR_Upload_Type)
	{

		set_Value (COLUMNNAME_ZZ_WSP_ATR_Upload_Type, ZZ_WSP_ATR_Upload_Type);
	}

	/** Get Upload Type.
		@return Upload Type	  */
	public String getZZ_WSP_ATR_Upload_Type()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Upload_Type);
	}

	/** Set WSP ATR Uploads.
		@param ZZ_WSP_ATR_Uploads_ID WSP ATR Uploads
	*/
	public void setZZ_WSP_ATR_Uploads_ID (int ZZ_WSP_ATR_Uploads_ID)
	{
		if (ZZ_WSP_ATR_Uploads_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Uploads_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WSP_ATR_Uploads_ID, Integer.valueOf(ZZ_WSP_ATR_Uploads_ID));
	}

	/** Get WSP ATR Uploads.
		@return WSP ATR Uploads	  */
	public int getZZ_WSP_ATR_Uploads_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WSP_ATR_Uploads_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WSP_ATR_Uploads_UU.
		@param ZZ_WSP_ATR_Uploads_UU ZZ_WSP_ATR_Uploads_UU
	*/
	public void setZZ_WSP_ATR_Uploads_UU (String ZZ_WSP_ATR_Uploads_UU)
	{
		set_Value (COLUMNNAME_ZZ_WSP_ATR_Uploads_UU, ZZ_WSP_ATR_Uploads_UU);
	}

	/** Get ZZ_WSP_ATR_Uploads_UU.
		@return ZZ_WSP_ATR_Uploads_UU	  */
	public String getZZ_WSP_ATR_Uploads_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WSP_ATR_Uploads_UU);
	}
}