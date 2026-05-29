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

/** Generated Model for ZZ_WF_Next_Node
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="ZZ_WF_Next_Node")
public class X_ZZ_WF_Next_Node extends PO implements I_ZZ_WF_Next_Node, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260529L;

    /** Standard Constructor */
    public X_ZZ_WF_Next_Node (Properties ctx, int ZZ_WF_Next_Node_ID, String trxName)
    {
      super (ctx, ZZ_WF_Next_Node_ID, trxName);
      /** if (ZZ_WF_Next_Node_ID == 0)
        {
			setZZ_WF_Lines_ID (0);
			setZZ_WF_Next_Node_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Next_Node (Properties ctx, int ZZ_WF_Next_Node_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WF_Next_Node_ID, trxName, virtualColumns);
      /** if (ZZ_WF_Next_Node_ID == 0)
        {
			setZZ_WF_Lines_ID (0);
			setZZ_WF_Next_Node_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Next_Node (Properties ctx, String ZZ_WF_Next_Node_UU, String trxName)
    {
      super (ctx, ZZ_WF_Next_Node_UU, trxName);
      /** if (ZZ_WF_Next_Node_UU == null)
        {
			setZZ_WF_Lines_ID (0);
			setZZ_WF_Next_Node_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_ZZ_WF_Next_Node (Properties ctx, String ZZ_WF_Next_Node_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, ZZ_WF_Next_Node_UU, trxName, virtualColumns);
      /** if (ZZ_WF_Next_Node_UU == null)
        {
			setZZ_WF_Lines_ID (0);
			setZZ_WF_Next_Node_ID (0);
        } */
    }

    /** Load Constructor */
    public X_ZZ_WF_Next_Node (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
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
      StringBuilder sb = new StringBuilder ("X_ZZ_WF_Next_Node[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_R_MailText getMMailText() throws RuntimeException
	{
		return (org.compiere.model.I_R_MailText)MTable.get(getCtx(), org.compiere.model.I_R_MailText.Table_ID)
			.getPO(getMMailText_ID(), get_TrxName());
	}

	/** Set Mail Text.
		@param MMailText_ID Mail Text
	*/
	public void setMMailText_ID (int MMailText_ID)
	{
		if (MMailText_ID < 1)
			set_Value (COLUMNNAME_MMailText_ID, null);
		else
			set_Value (COLUMNNAME_MMailText_ID, Integer.valueOf(MMailText_ID));
	}

	/** Get Mail Text.
		@return Mail Text	  */
	public int getMMailText_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_MMailText_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Option Value Chosen.
		@param ZZ_Option_Value Option Value Chosen
	*/
	public void setZZ_Option_Value (String ZZ_Option_Value)
	{
		set_Value (COLUMNNAME_ZZ_Option_Value, ZZ_Option_Value);
	}

	/** Get Option Value Chosen.
		@return Option Value Chosen	  */
	public String getZZ_Option_Value()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Option_Value);
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

	public I_ZZ_WF_Lines getZZ_WF_Next_Lines() throws RuntimeException
	{
		return (I_ZZ_WF_Lines)MTable.get(getCtx(), I_ZZ_WF_Lines.Table_ID)
			.getPO(getZZ_WF_Next_Lines_ID(), get_TrxName());
	}

	/** Set Next Node.
		@param ZZ_WF_Next_Lines_ID Next Node
	*/
	public void setZZ_WF_Next_Lines_ID (int ZZ_WF_Next_Lines_ID)
	{
		if (ZZ_WF_Next_Lines_ID < 1)
			set_Value (COLUMNNAME_ZZ_WF_Next_Lines_ID, null);
		else
			set_Value (COLUMNNAME_ZZ_WF_Next_Lines_ID, Integer.valueOf(ZZ_WF_Next_Lines_ID));
	}

	/** Get Next Node.
		@return Next Node	  */
	public int getZZ_WF_Next_Lines_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WF_Next_Lines_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Next Nodes.
		@param ZZ_WF_Next_Node_ID Next Nodes
	*/
	public void setZZ_WF_Next_Node_ID (int ZZ_WF_Next_Node_ID)
	{
		if (ZZ_WF_Next_Node_ID < 1)
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Next_Node_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_ZZ_WF_Next_Node_ID, Integer.valueOf(ZZ_WF_Next_Node_ID));
	}

	/** Get Next Nodes.
		@return Next Nodes	  */
	public int getZZ_WF_Next_Node_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ZZ_WF_Next_Node_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set ZZ_WF_Next_Node_UU.
		@param ZZ_WF_Next_Node_UU ZZ_WF_Next_Node_UU
	*/
	public void setZZ_WF_Next_Node_UU (String ZZ_WF_Next_Node_UU)
	{
		set_Value (COLUMNNAME_ZZ_WF_Next_Node_UU, ZZ_WF_Next_Node_UU);
	}

	/** Get ZZ_WF_Next_Node_UU.
		@return ZZ_WF_Next_Node_UU	  */
	public String getZZ_WF_Next_Node_UU()
	{
		return (String)get_Value(COLUMNNAME_ZZ_WF_Next_Node_UU);
	}
}