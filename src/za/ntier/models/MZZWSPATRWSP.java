package za.ntier.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_WSP;

public class MZZWSPATRWSP extends X_ZZ_WSP_ATR_WSP {

	public MZZWSPATRWSP(Properties ctx, int ZZ_WSP_ATR_WSP_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_WSP_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRWSP(Properties ctx, int ZZ_WSP_ATR_WSP_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_WSP_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRWSP(Properties ctx, String ZZ_WSP_ATR_WSP_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_WSP_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRWSP(Properties ctx, String ZZ_WSP_ATR_WSP_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_WSP_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRWSP(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// TODO Auto-generated method stub
		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		// TODO Auto-generated method stub
		if (!success)
			return success;
		
		updateVerificationTotals();
		
		return true;
	}

	private void updateVerificationTotals()
	{
	    int submittedId = getZZ_WSP_ATR_Submitted_ID();

	    // --- WSP TOTAL ---
	    String wspSql =
	        "SELECT COALESCE(SUM(MaleCount),0) + " +
	        "       COALESCE(SUM(FemaleCount),0) " +
	        "FROM ZZ_WSP_ATR_ATR_Detail WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    int wspTotal = DB.getSQLValue(get_TrxName(), wspSql, submittedId);

	    // --- ATR TOTAL ---
	    String atrSql =
	        "SELECT COUNT(*) FROM ZZ_WSP_ATR_WSP " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    int atrTotal = DB.getSQLValue(get_TrxName(), atrSql, submittedId);

	    // --- HTVF TOTAL ---
	    String htvfSql =
	        "SELECT COUNT(*) FROM ZZ_WSP_ATR_HTVF " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    int htvfTotal = DB.getSQLValue(get_TrxName(), htvfSql, submittedId);

	    // --- Percentage ---
	    BigDecimal pct = Env.ZERO;
	    if (wspTotal > 0)
	    {
	        pct = new BigDecimal(atrTotal)
	                .divide(new BigDecimal(wspTotal), 4, RoundingMode.HALF_UP)
	                .multiply(new BigDecimal(100));
	    }

	    // --- Update verification row ---
	    MZZWSPATRVeriChecklist ver = new Query(getCtx(),
	    		MZZWSPATRVeriChecklist.Table_Name,
	            "ZZ_WSP_ATR_Submitted_ID=?",
	            get_TrxName())
	            .setParameters(submittedId)
	            .first();

	    if (ver != null)
	    {
	        ver.set_ValueOfColumn("ZZ_TotalNo", htvfTotal);
	        ver.set_ValueOfColumn("ZZ_WSPTotal", wspTotal);
	        ver.set_ValueOfColumn("ZZ_ATRTotal", atrTotal);
	        ver.set_ValueOfColumn("ZZ_ATRvsWSPPct", pct);

	        ver.saveEx();
	    }
	}
}
