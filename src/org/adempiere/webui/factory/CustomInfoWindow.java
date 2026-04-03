package org.adempiere.webui.factory;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.ntier.utils.MQAConstants;
import za.ntier.utils.StringUtil;

/**
 * 
 */
public class CustomInfoWindow extends InfoWindow
{
	private static final long serialVersionUID = 5825282805317055525L;

	public CustomInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup)
	{
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	public CustomInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID)
	{
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	public CustomInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field)
	{
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	public CustomInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables)
	{
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}

	/**
	 * zoom to record
	 */
	@Override
	public void zoom()
	{
		Object recordId = contentPanel.getSelectedRowKey();
		if (recordId == null)
			return;

		int AD_Table_ID = infoWindow != null ? infoWindow.getAD_Table_ID() : -1;

		String tableName = AD_Table_ID > 0 ? MTable.getTableName(Env.getCtx(), AD_Table_ID) : null;

		boolean isCustomZoom = false;

		if (StringUtil.equalsAnyIgnoreCase(tableName, MQAConstants.CUSTOM_ZOOM_TABLES))
		{
			String sql = "SELECT AD_Table_ID FROM " + tableName + " WHERE " + p_keyColumn + "=?";
			int dbTableId = 0;

			if (recordId instanceof String uu)
				dbTableId = DB.getSQLValue(null, sql, uu);
			else if (recordId instanceof Integer id)
				dbTableId = DB.getSQLValue(null, sql, id);

			if (dbTableId > 0)
			{
				AD_Table_ID = dbTableId;
			}
			isCustomZoom = true;
		}
		else if ("ZZSdfOrganisation".equalsIgnoreCase(tableName))
		{
			int AD_Window_ID = recordId instanceof Integer id ? Env.getZoomWindowID(AD_Table_ID, id) : 0;
			if (AD_Window_ID > 0)
			{
				int newTableID = getTableIDForWindow(AD_Window_ID);
				if (newTableID > 0 && newTableID != AD_Table_ID)
				{
					AD_Table_ID = newTableID;
				}
			}
			isCustomZoom = true;
		}

		// Execute custom zoom if handled, otherwise fallback to standard
		// InfoPanel zoom
		if (isCustomZoom)
		{
			if (AD_Table_ID > 0)
			{
				if (recordId instanceof String uu)
					AEnv.zoomUU(AD_Table_ID, uu);
				else if (recordId instanceof Integer id)
					AEnv.zoom(AD_Table_ID, id);
			}
		}
		else
		{
			super.zoom();
		}
	}

	// This is for the case where we have a zoom condition to a window with a
	// different table.
	private static int getTableIDForWindow(int adWindowId)
	{
		String sql = "SELECT AD_Table_ID FROM AD_Tab WHERE AD_Window_ID=? AND TabLevel=0 AND IsActive='Y'";
		return DB.getSQLValue(null, sql, adWindowId);
	}
	
	@Override
	protected boolean hasZoom()
	{
		return !isLookup() && infoWindow != null;
	}

}
