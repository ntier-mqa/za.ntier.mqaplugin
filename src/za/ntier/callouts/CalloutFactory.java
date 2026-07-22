package za.ntier.callouts;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.IColumnCalloutFactory;
import org.compiere.model.X_M_InventoryLine;
import org.osgi.service.component.annotations.Component;

import za.ntier.models.X_ZZ_Open_Application;
import za.ntier.models.X_ZZ_System_Access_Application;
import za.co.ntier.api.model.X_C_BP_SkillsProgramme;
import za.co.ntier.api.model.X_C_BP_TTC;
import za.co.ntier.api.model.X_C_BP_OC;
import za.co.ntier.api.model.X_C_BP_Trades;
import za.co.ntier.api.model.X_C_BP_AC;
import za.co.ntier.api.model.X_ZZ_WPA_Application;
import za.co.ntier.api.model.X_ZZ_WPA_App_Qualifications;
import za.co.ntier.api.model.X_ZZ_WPA_App_QCTOQualifications;
import za.co.ntier.api.model.X_ZZ_WPA_App_SkillsProgramme;
import za.co.ntier.api.model.X_ZZ_WPA_App_QCTOSkillsProg;
import za.co.ntier.api.model.X_ZZSkillsProgrammeUnitStandard;

import za.co.ntier.api.model.X_ZZQctoSkillsProgrammeModule;

@Component(

		 property= {"service.ranking:Integer=2"},
		 service = org.adempiere.base.IColumnCalloutFactory.class
		 )

public class CalloutFactory implements IColumnCalloutFactory {

	@Override
	public IColumnCallout[] getColumnCallouts(String tableName, String columnName) {
		List<IColumnCallout> list = new ArrayList<IColumnCallout>();
		if (tableName.equals(X_M_InventoryLine.Table_Name) || 
			tableName.equals(X_ZZ_System_Access_Application.Table_Name) ||
			tableName.equals(X_ZZ_Open_Application.Table_Name) ||
			tableName.equals(X_C_BP_SkillsProgramme.Table_Name) ||
			tableName.equals(X_C_BP_OC.Table_Name) ||
			tableName.equals(X_C_BP_Trades.Table_Name) ||
			tableName.equals(X_C_BP_AC.Table_Name) ||
			tableName.equals(X_C_BP_TTC.Table_Name) ||
			tableName.equals(X_ZZ_WPA_Application.Table_Name) ||
			tableName.equals(X_ZZ_WPA_App_Qualifications.Table_Name) ||
			tableName.equals(X_ZZ_WPA_App_QCTOQualifications.Table_Name) ||
			tableName.equals(X_ZZ_WPA_App_SkillsProgramme.Table_Name) ||
			tableName.equals(X_ZZ_WPA_App_QCTOSkillsProg.Table_Name) ||
			tableName.equals(X_ZZSkillsProgrammeUnitStandard.Table_Name) ||
			tableName.equals(X_ZZQctoSkillsProgrammeModule.Table_Name)
			)
		{
			list.add(new CalloutFromFactory());
		}
		return list != null ?  list.toArray(new IColumnCallout[list.size()]) : new IColumnCallout[0];
	}

}
