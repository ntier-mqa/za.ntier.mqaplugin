package za.ntier.callouts;

import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.X_M_InventoryLine;
import org.compiere.util.DB;
import org.compiere.util.Msg;

import za.co.ntier.api.model.I_ZZ_WPA_Application;
import za.co.ntier.api.model.X_C_BP_AC;
import za.co.ntier.api.model.X_C_BP_OC;
import za.co.ntier.api.model.X_C_BP_SkillsProgramme;
import za.co.ntier.api.model.X_C_BP_TTC;
import za.co.ntier.api.model.X_C_BP_Trades;
import za.co.ntier.api.model.X_ZZQctoQualification;
import za.co.ntier.api.model.X_ZZQctoSkillsProgramme;
import za.co.ntier.api.model.X_ZZQualification;
import za.co.ntier.api.model.X_ZZSkillsProgramme;
import za.co.ntier.api.model.X_ZZ_Occupational_Certificates;
import za.co.ntier.api.model.X_ZZ_WPA_Application;
import za.co.ntier.api.model.X_ZZ_WPA_App_Qualifications;
import za.co.ntier.api.model.X_ZZ_WPA_App_QCTOQualifications;
import za.co.ntier.api.model.X_ZZ_WPA_App_SkillsProgramme;
import za.co.ntier.api.model.X_ZZ_WPA_App_QCTOSkillsProg;
import za.co.ntier.api.model.X_ZZUnitStandard;
import za.co.ntier.api.model.X_ZZSkillsProgrammeUnitStandard;
import za.ntier.models.MZZOpenApplication;
import za.ntier.models.OpenAppOverlapInput;
import za.ntier.models.X_ZZ_Open_Application;
import za.ntier.models.X_ZZ_System_Access_Application;

public class CalloutFromFactory implements IColumnCallout {

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (mTab.getTableName().equals(X_ZZ_WPA_Application.Table_Name))
		{
			if (mField.getColumnName().equals(I_ZZ_WPA_Application.COLUMNNAME_C_BPartner_ID))
			{
				int bpartnerId = tabInt(mTab, I_ZZ_WPA_Application.COLUMNNAME_C_BPartner_ID);
				if (bpartnerId > 0)
				{
					MBPartner bp = new MBPartner(ctx, bpartnerId, null);
					if (bp.get_ID() > 0)
					{
						mTab.setValue(I_ZZ_WPA_Application.COLUMNNAME_Value, bp.getValue());
					}
				}
				else
				{
					mTab.setValue(I_ZZ_WPA_Application.COLUMNNAME_Value, null);
				}
			}
			return "";
		}
		if (mTab.getTableName().equals(X_ZZ_Open_Application.Table_Name) && 
				mField.getColumnName().equals(X_ZZ_Open_Application.COLUMNNAME_ZZ_Programs)) {
			String programs = (String) value;
			if (programs == null || programs.trim().isEmpty())
				return "";
			java.util.LinkedHashSet<Integer> programIds = new java.util.LinkedHashSet<>();
			for (String s : programs.split("\\s*,\\s*")) {
				if (s == null || s.trim().isEmpty())
					continue;
				try {
					programIds.add(Integer.parseInt(s.trim()));
				} catch (NumberFormatException nfe) {
					Object[] args = new Object[] { s.trim() };
					//throw new AdempiereException(Msg.getMsg(ctx, "INVALIDPROGRAMID", args));
					return Msg.getMsg(ctx, "INVALIDPROGRAMID", args);
				}
			}
			if (programIds.isEmpty())
		        return "";
			OpenAppOverlapInput openAppOverlapInput = new OpenAppOverlapInput(
			        ctx,
			        null,
			        tabInt(mTab, X_ZZ_Open_Application.COLUMNNAME_C_Year_ID),
			        tabInt(mTab, X_ZZ_Open_Application.COLUMNNAME_ZZ_Open_Application_ID), // null -> 0
			        tabTs(mTab, X_ZZ_Open_Application.COLUMNNAME_StartDate),
			        tabTs(mTab, X_ZZ_Open_Application.COLUMNNAME_EndDate)
			);

			 java.util.List<Integer> overlappingIds = MZZOpenApplication.overlapping(programIds,openAppOverlapInput);
			 if (!overlappingIds.isEmpty()) {
			        String programNamesCsv = MZZOpenApplication.getProgramNamesCsv(overlappingIds,null);
			        Object[] args = new Object[] { programNamesCsv };
			        return(Msg.getMsg(ctx, "OVERLAPPINGOPENAPP", args));
			    }
		   
		}
		if (mTab.getTableName().equals(X_M_InventoryLine.Table_Name) && 
				mField.getColumnName().equals(X_M_InventoryLine.COLUMNNAME_M_Product_ID)) {
			if (value != null) {
				String SQL = "SELECT ca.c_charge_id "
						+ "FROM M_Product_Acct pa "
						+ "join C_ValidCombination vc on pa.p_expense_acct = c_validcombination_id "
						+ "join c_charge_acct ca on vc.c_validcombination_id = ca.ch_expense_acct "
						+ "WHERE pa.C_AcctSchema_ID=1000000 AND pa.M_Product_ID=?";
				int chargeID = DB.getSQLValue(null, SQL, value);
				if (chargeID > 0) {
					mTab.setValue(X_M_InventoryLine.COLUMNNAME_C_Charge_ID, chargeID);
				}

			} else {
				mTab.setValue(X_M_InventoryLine.COLUMNNAME_C_Charge_ID, null);
			}
		}
		if (mTab.getTableName().equals(X_ZZ_System_Access_Application.Table_Name) && 
				(mField.getColumnName().equals(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_User_ID) || 
						mField.getColumnName().equals(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Application_Type))) {
			if (mTab.get_ValueAsString(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Application_Type) != null &&
					!mTab.get_ValueAsString(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Application_Type).equals("U")) {
				mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles, null);
				mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles_Updated, null);
			} else {
				int user_ID = 0;
				Object user = (mTab.getValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_User_ID));
				if (user != null) {
					user_ID = (int)user;
				}
				if (user_ID > 0) {
					String SQL = "SELECT string_agg(CAST(r.ad_role_id AS TEXT), ',' ORDER BY r.ad_role_id) AS DefaultValue"
							+ " FROM ad_user_roles r "
							+ " join ad_role rol on r.ad_role_id = rol.ad_role_id"
							+ " WHERE r.ad_user_id = ? and rol.isactive = 'Y'";
					String roles = DB.getSQLValueString(null, SQL, user_ID);
					mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles, roles);
					mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles_Updated, roles);
				} else {
					mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles, null);
					mTab.setValue(X_ZZ_System_Access_Application.COLUMNNAME_ZZ_Roles_Updated, null);
				}

			}
		}
		
		if (mTab.getTableName().equals(X_C_BP_SkillsProgramme.Table_Name)	&&
			mField.getColumnName().equals(X_C_BP_SkillsProgramme.COLUMNNAME_ZZQctoSkillsProgramme_ID))
		{

			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZQctoSkillsProgramme srcSkill = new X_ZZQctoSkillsProgramme(ctx, srcID, null);

					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZLkpOfoOccupation_ID, srcSkill.getZZLkpOfoOccupation_ID());
					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZNqfLevel, srcSkill.getZZNqfLevel());
					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZCredits, srcSkill.getZZCredits());
				}
			}
			else
			{
				// Clear values if ZZQctoSkillsProgramme_ID is cleared
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZLkpOfoOccupation_ID, null);
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_C_BP_SkillsProgramme.Table_Name)	&&
			mField.getColumnName().equals(X_C_BP_SkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID))
		{

			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZSkillsProgramme srcSkill = new X_ZZSkillsProgramme(ctx, srcID, null);

					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZLkpOfoOccupation_ID, srcSkill.getZZLkpOfoOccupationTree_ID());
					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZNqfLevel, srcSkill.getZZNqfLevel());
					mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZCredits, srcSkill.getZZCredits());
				}
			}
			else
			{
				// Clear values if ZZSkillsProgramme_ID is cleared
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZLkpOfoOccupation_ID, null);
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_C_BP_SkillsProgramme.COLUMNNAME_ZZCredits, null);
			}
		}

		if ((mTab.getTableName().equals(X_C_BP_AC.Table_Name)	||
				mTab.getTableName().equals(X_C_BP_TTC.Table_Name))	&&
			mField.getColumnName().equals(X_ZZ_Occupational_Certificates.COLUMNNAME_ZZ_Occupational_Certificates_ID))
		{

			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZ_Occupational_Certificates srcCert = new X_ZZ_Occupational_Certificates(ctx, srcID, null);

					mTab.setValue(X_C_BP_AC.COLUMNNAME_ZZLkpOfoOccupation_ID, srcCert.getZZLkpOfoOccupation_ID());
					mTab.setValue(X_C_BP_AC.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_C_BP_AC.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				// Clear values if ZZ_Occupational_Certificates_ID is cleared
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZLkpOfoOccupation_ID, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZCredits, null);
			}
		}
		if ((mTab.getTableName().equals(X_C_BP_OC.Table_Name)	||
			mTab.getTableName().equals(X_C_BP_Trades.Table_Name))	&&
																	mField.getColumnName().equals(X_ZZQctoQualification.COLUMNNAME_ZZQctoQualification_ID))
		{

			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZQctoQualification srcCert = new X_ZZQctoQualification(ctx, srcID, null);

					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZLkpOfoOccupation_ID, srcCert.getZZLkpOfoOccupation_ID());
					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				// Clear values if ZZQctoQualification_ID is cleared
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZLkpOfoOccupation_ID, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZCredits, null);
			}
		}

		if ((mTab.getTableName().equals(X_C_BP_OC.Table_Name)	||
			mTab.getTableName().equals(X_C_BP_Trades.Table_Name))	&&
																	mField.getColumnName().equals(X_ZZQualification.COLUMNNAME_ZZQualification_ID))
		{

			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZQualification srcCert = new X_ZZQualification(ctx, srcID, null);

					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZLkpOfoOccupation_ID, srcCert.getZZLkpOfoOccupationTree_ID());
					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				// Clear values if ZZQualification_ID is cleared
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZLkpOfoOccupation_ID, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_C_BP_OC.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_ZZ_WPA_App_Qualifications.Table_Name)	&&
			mField.getColumnName().equals(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZQualification_ID))
		{
			if (value != null)
			{
				int srcID = (Integer) value;
				if (srcID > 0)
				{
					X_ZZQualification srcCert = new X_ZZQualification(ctx, srcID, null);
					mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZProgrammeName, srcCert.getZZSaqaQualificationTitle());
					mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZProgrammeName, null);
				mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_ZZ_WPA_App_Qualifications.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_ZZ_WPA_App_QCTOQualifications.Table_Name)	&&
			mField.getColumnName().equals(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZQctoQualification_ID))
		{
			if (value != null)
			{
				int srcID = (Integer) value;
				if (srcID > 0)
				{
					X_ZZQctoQualification srcCert = new X_ZZQctoQualification(ctx, srcID, null);
					mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZProgrammeName, srcCert.getZZSaqaQualificationTitle());
					mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZProgrammeName, null);
				mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_ZZ_WPA_App_QCTOQualifications.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_ZZ_WPA_App_SkillsProgramme.Table_Name) &&
			mField.getColumnName().equals(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZSkillsProgramme_ID))
		{
			if (value != null)
			{
				int srcID = (Integer) value;
				if (srcID > 0)
				{
					X_ZZSkillsProgramme srcCert = new X_ZZSkillsProgramme(ctx, srcID, null);
					mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZProgrammeName, srcCert.getZZSkillsProgrammeTitle());
					mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZProgrammeName, null);
				mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_ZZ_WPA_App_SkillsProgramme.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_ZZ_WPA_App_QCTOSkillsProg.Table_Name)	&&
			mField.getColumnName().equals(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZQctoSkillsProgramme_ID))
		{
			if (value != null)
			{
				int srcID = (Integer) value;
				if (srcID > 0)
				{
					X_ZZQctoSkillsProgramme srcCert = new X_ZZQctoSkillsProgramme(ctx, srcID, null);
					mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZProgrammeName, srcCert.getZZSkillsProgrammeTitle());
					mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZNqfLevel, srcCert.getZZNqfLevel());
					mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZCredits, srcCert.getZZCredits());
				}
			}
			else
			{
				mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZProgrammeName, null);
				mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_ZZ_WPA_App_QCTOSkillsProg.COLUMNNAME_ZZCredits, null);
			}
		}

		if (mTab.getTableName().equals(X_ZZSkillsProgrammeUnitStandard.Table_Name) &&
			mField.getColumnName().equals(X_ZZSkillsProgrammeUnitStandard.COLUMNNAME_ZZUnitStandard_ID))
		{
			if (value != null)
			{
				int srcID = 0;
				if (value instanceof Number)
				{
					srcID = ((Number) value).intValue();
				}
				else
				{
					srcID = Integer.parseInt(value.toString());
				}

				if (srcID > 0)
				{
					X_ZZUnitStandard srcUnit = new X_ZZUnitStandard(ctx, srcID, null);
					mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZSaqaUnitStandardCode, srcUnit.getZZSaqaUnitStandardCode());
					mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZNqfLevel, srcUnit.getZZNqfLevel());
					mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZCredits, srcUnit.getZZCredits());
				}
			}
			else
			{
				mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZSaqaUnitStandardCode, null);
				mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZNqfLevel, null);
				mTab.setValue(X_ZZUnitStandard.COLUMNNAME_ZZCredits, null);
			}
		}

		return null;
	}

	@Override
	public String toString() {
		return "CalloutFromFactory [toString()=" + super.toString() + "]";
	}
	
	private static int tabInt(org.compiere.model.GridTab tab, String columnName) {
	    Object v = tab.getValue(columnName);
	    if (v == null) return 0;
	    if (v instanceof Number) return ((Number) v).intValue();
	    return Integer.parseInt(v.toString().trim());
	}

	private static Timestamp tabTs(org.compiere.model.GridTab tab, String columnName) {
	    Object v = tab.getValue(columnName);
	    return (v instanceof Timestamp) ? (Timestamp) v : null;
	}


}
