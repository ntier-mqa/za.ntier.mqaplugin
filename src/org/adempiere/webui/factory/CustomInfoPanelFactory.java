package org.adempiere.webui.factory;

import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;

public class CustomInfoPanelFactory implements IInfoFactory
{

	private InfoWindow createCustomWindow(int windowNo, String tableName, String keyColumn, String queryValue,
			boolean multiSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables)
	{
		if ("ZZ_QCTO_APPLICATION_INFO_V".equalsIgnoreCase(tableName))
		{
			keyColumn = tableName + "_UU";
			multiSelection = true;
		}
		else if (!"ZZSdfOrganisation".equalsIgnoreCase(tableName))
		{
			return null; // Fallback to generic iDempiere factory
		}

		InfoWindow info = new CustomInfoWindow(windowNo, tableName, keyColumn, queryValue, multiSelection, whereClause,
				AD_InfoWindow_ID, lookup, field, predefinedContextVariables);
		if (info.loadedOK())
			return info;

		info.dispose(false);
		return null;
	}

	@Override
	public InfoWindow create(int windowNo, int AD_InfoWindow_ID, String predefinedContextVariables)
	{
		MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
		if (infoWindow == null || infoWindow.getAD_Table() == null)
			return null;

		String tableName = infoWindow.getAD_Table().getTableName();
		return createCustomWindow(windowNo, tableName, tableName + "_ID", null, false, null, AD_InfoWindow_ID, false,
				null, predefinedContextVariables);
	}

	@Override
	public InfoPanel create(int windowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, boolean lookup)
	{
		return createCustomWindow(windowNo, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID,
				lookup, null, null);
	}

	@Override
	public InfoPanel create(Lookup lookup, GridField field, String tableName, String keyColumn, String value,
			boolean multiSelection, String whereClause, int AD_InfoWindow_ID)
	{
		return createCustomWindow(-1, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID, true,
				field, null);
	}

	@Override
	public InfoWindow create(int AD_InfoWindow_ID)
	{
		MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
		if (infoWindow == null || infoWindow.getAD_Table() == null)
			return null;

		String tableName = infoWindow.getAD_Table().getTableName();
		return createCustomWindow(-1, tableName, tableName + "_ID", null, false, null, AD_InfoWindow_ID, false, null,
				null);
	}

	@Override
	public InfoPanel create(int windowNo, String tableName, String keyColumn, String value, boolean multiSelection,
			String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field)
	{
		return createCustomWindow(windowNo, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID,
				lookup, field, null);
	}

}
