package za.ntier.models;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.adempiere.base.IModelFactory;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

import za.co.ntier.api.model.I_ZZ_Program_Master_Data;
import za.co.ntier.wf.model.MZZWFHeader;
import za.co.ntier.wf.model.MZZWFLineRole;
import za.co.ntier.wf.model.MZZWFLines;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Biodata_Detail;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_HTVF;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_Employees;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Biodata_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_Employees;

@Component(

		 property= {"service.ranking:Integer=2"},
		 service = org.adempiere.base.IModelFactory.class
		 )
public class MyModelFactory implements IModelFactory {

    private static class ModelDef {
        final Class<? extends PO> modelClass;
        final Constructor<? extends PO> idCtor;
        final Constructor<? extends PO> rsCtor;

        ModelDef(Class<? extends PO> modelClass,
                 Constructor<? extends PO> idCtor,
                 Constructor<? extends PO> rsCtor) {
            this.modelClass = modelClass;
            this.idCtor = idCtor;
            this.rsCtor = rsCtor;
        }
    }

    private static final Map<String, ModelDef> MODELS = new HashMap<>();

    static {
        // Register all your tables + model classes here
    	//register(I_ZZ_WSP_ATR_HTVF.Table_Name,          MZZWSPATRHTVF.class);
    	//register(I_ZZ_WSP_ATR_WSP.Table_Name,          MZZWSPATRWSP.class);
    	//register(I_ZZ_WSP_ATR_ATR_Detail.Table_Name,          MZZWSPATRATRDetail.class);
    	register(I_ZZ_WSP_ATR_Veri_Checklist.Table_Name,          MZZWSPATRVeriChecklist.class);
    	register(I_ZZ_WSP_ATR_Submitted.Table_Name,          MZZWSPATRSubmitted.class);
    	register(I_ZZ_SDR_Temp_Org.Table_Name,          MZZSDR_Temp_Org.class);
    	register(I_ZZSdfOrganisation.Table_Name,          MZZSdfOrganisation.class);
        register(I_ZZ_ATRVerification.Table_Name,          MZZATRVerification.class);
        register(I_ZZ_WF_Line_Role.Table_Name,          MZZWFLineRole.class);
        register(I_ZZ_WF_Lines.Table_Name,              MZZWFLines.class);
        register(I_ZZ_WF_Header.Table_Name,             MZZWFHeader.class);
        register(I_ZZ_Monthly_Levy_Files.Table_Name,    X_ZZ_Monthly_Levy_Files.class);
        register(I_ZZ_Open_Application.Table_Name,      MZZOpenApplication.class);
        register(I_ZZ_WSP_ATR_Approvals.Table_Name,     X_ZZ_WSP_ATR_Approvals.class);
        register(I_ZZ_Program_Master_Data.Table_Name,   MZZProgramMasterData.class);
        register(I_ZZ_System_Access_Application.Table_Name, MZZSystemAccessApplication.class);
        register(I_M_Inventory.Table_Name,              MInventory_New.class);
        register(I_ZZ_Petty_Cash_Recon_Advance.Table_Name,  MZZPettyCashReconAdvance.class);
        register(I_ZZ_Petty_Cash_Recon_Claim.Table_Name,    MZZPettyCashReconClaim.class);
        register(I_ZZ_Petty_Cash_Recon_Hdr.Table_Name,      MZZPettyCashReconHdr.class);
        register(I_ZZ_Petty_Cash_Claim_Line.Table_Name,     MZZPettyCashClaimLine.class);
        register(I_ZZ_Petty_Cash_Claim_Hdr.Table_Name,      MZZPettyCashClaimHdr.class);
        register(I_ZZ_Petty_Cash_Advance_Line.Table_Name,   MZZPettyCashAdvanceLine.class);
        register(I_ZZ_Petty_Cash_Advance_Hdr.Table_Name,    MZZPettyCashAdvanceHdr.class);
        register(I_ZZ_Petty_Cash_Application.Table_Name,    MZZPettyCashApplication.class);
        register(I_C_InvoiceBatch.Table_Name,           MInvoiceBatch_New.class);
        register(I_C_BP_BankAccount.Table_Name,         MBPBankAccount_New.class);
        register(I_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,     X_ZZ_WSP_ATR_Lookup_Mapping.class);
        register(I_ZZ_WSP_ATR_Lookup_Mapping_Detail.Table_Name,     X_ZZ_WSP_ATR_Lookup_Mapping_Detail.class);
        register(I_ZZ_WSP_ATR_Biodata_Detail.Table_Name,X_ZZ_WSP_ATR_Biodata_Detail.class);
        register(I_ZZ_WSP_Employees.Table_Name,X_ZZ_WSP_Employees.class);
        //register(I_ZZ_WSP_ATR_Submitted.Table_Name,X_ZZ_WSP_ATR_Submitted.class);
    }

    private static void register(String tableName, Class<? extends PO> modelClass) {
        try {
            Constructor<? extends PO> idCtor =
                    modelClass.getConstructor(Properties.class, int.class, String.class);
            Constructor<? extends PO> rsCtor =
                    modelClass.getConstructor(Properties.class, ResultSet.class, String.class);

            MODELS.put(tableName, new ModelDef(modelClass, idCtor, rsCtor));
        } catch (NoSuchMethodException e) {
            // Fail fast at startup if the model doesn't have the standard constructors
            throw new ExceptionInInitializerError(
                    "Invalid PO class " + modelClass.getName() +
                    " for table " + tableName +
                    " (expected (Properties,int,String) and (Properties,ResultSet,String) constructors)"
            );
        }
    }

    @Override
    public Class<?> getClass(String tableName) {
        ModelDef def = MODELS.get(tableName);
        return def != null ? def.modelClass : null;
    }

    @Override
    public PO getPO(String tableName, int Record_ID, String trxName) {
        ModelDef def = MODELS.get(tableName);
        if (def == null) {
            return null;
        }
        try {
            return def.idCtor.newInstance(Env.getCtx(), Record_ID, trxName);
        } catch (Exception e) {
            throw new AdempiereException(
                    "Error creating PO (id) for table " + tableName + " and record " + Record_ID, e);
        }
    }

    @Override
    public PO getPO(String tableName, ResultSet rs, String trxName) {
        ModelDef def = MODELS.get(tableName);
        if (def == null) {
            return null;
        }
        try {
            return def.rsCtor.newInstance(Env.getCtx(), rs, trxName);
        } catch (Exception e) {
            throw new AdempiereException(
                    "Error creating PO (ResultSet) for table " + tableName, e);
        }
    }
}
