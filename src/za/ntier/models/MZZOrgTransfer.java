package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.Query;
import org.compiere.util.DB;
import za.ntier.models.MZZSDR_Temp_Org;

public class MZZOrgTransfer extends X_ZZ_Org_Transfer {

	public MZZOrgTransfer(Properties ctx, int ZZ_Org_Transfer_ID, String trxName) {
		super(ctx, ZZ_Org_Transfer_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, int ZZ_Org_Transfer_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_Org_Transfer_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, String ZZ_Org_Transfer_UU, String trxName) {
		super(ctx, ZZ_Org_Transfer_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, String ZZ_Org_Transfer_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_Org_Transfer_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZOrgTransfer(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean beforeSave(boolean newRecord)
	{
	    if (newRecord || is_ValueChanged("ZZ_SDL_No"))
	    {
	        String sdl = get_ValueAsString("ZZ_SDL_No");

	        if (sdl == null || sdl.trim().isEmpty())
	        {
	            log.saveError("Error", "SDL Number is mandatory");
	            return false;
	        }

	        setC_BPartner_ID(0);
	        set_Value("ZZ_Organisation_Reg_No", null);

	        MZZSDR_Temp_Org tempOrg = new Query(getCtx(),
	        		MZZSDR_Temp_Org.Table_Name,
	                "ZZ_SDL_No=? AND IsActive='Y'",
	                get_TrxName())
	                .setParameters(sdl)
	                .first();

	        if (tempOrg == null)
	        {
	            log.saveError("Error", "No Organisation found for SDL Number");
	            return false;
	        }

	        if (tempOrg.getC_BPartner_ID() <= 0)
	        {
	            log.saveError("Error", "Organisation not linked to Business Partner");
	            return false;
	        }

	        setC_BPartner_ID(tempOrg.getC_BPartner_ID());
	        set_Value("ZZ_Organisation_Reg_No",
	                tempOrg.getZZ_Organisation_Reg_No());
	    }

	    return true;
	}
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// TODO Auto-generated method stub
		return super.afterSave(newRecord, success);
	}

}
