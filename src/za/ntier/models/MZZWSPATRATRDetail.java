package za.ntier.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_ATR_Detail;

public class MZZWSPATRATRDetail extends X_ZZ_WSP_ATR_ATR_Detail {

	public MZZWSPATRATRDetail(Properties ctx, int ZZ_WSP_ATR_ATR_Detail_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_ATR_Detail_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRATRDetail(Properties ctx, int ZZ_WSP_ATR_ATR_Detail_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_ATR_Detail_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRATRDetail(Properties ctx, String ZZ_WSP_ATR_ATR_Detail_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_ATR_Detail_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRATRDetail(Properties ctx, String ZZ_WSP_ATR_ATR_Detail_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_ATR_Detail_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRATRDetail(Properties ctx, ResultSet rs, String trxName) {
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
			return false;
		
		updateATRAndDeviation();
		
		return super.afterSave(newRecord, success);
	}
	
	
	
	private void updateATRAndDeviation()
	{
	    int submittedId = getZZ_WSP_ATR_Submitted_ID();

	    String atrSql =
	        "SELECT COUNT(*) " +
	        "FROM ZZ_WSP_ATR_ATR_DETAIL " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    int atrTotal = DB.getSQLValue(get_TrxName(), atrSql, submittedId);

	    String wspSql =
	        "SELECT COALESCE(SUM(zz_male + zz_female),0) " +
	        "FROM ZZ_WSP_ATR_WSP " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    BigDecimal wspTotal =
	        DB.getSQLValueBD(get_TrxName(), wspSql, submittedId);

	    BigDecimal atrBD = BigDecimal.valueOf(atrTotal);

	    BigDecimal deviation = BigDecimal.ZERO;

	    if (wspTotal != null && wspTotal.signum() > 0)
	    {
	        deviation = atrBD
	            .divide(wspTotal, 4, RoundingMode.HALF_UP)
	            .multiply(new BigDecimal("100"));
	    }

	    MZZWSPATRSubmitted submitted =
	        MZZWSPATRSubmitted.getSubmitted(getCtx(), submittedId, get_TrxName());

	    submitted.updateChecklistTotal(
	        MZZWSPATRSubmitted.CL_ATR_TOTAL,
	        atrBD);

	    submitted.updateChecklistTotal(
	        MZZWSPATRSubmitted.CL_DEVIATION_PCT,
	        deviation);
	}


}
