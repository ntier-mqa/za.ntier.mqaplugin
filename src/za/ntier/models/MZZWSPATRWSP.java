package za.ntier.models;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

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
			return false;
		
		updateWSPTotal();
		
		return super.afterSave(newRecord, success);
	}
	private void updateWSPTotal()
	{
	    int submittedId = getZZ_WSP_ATR_Submitted_ID();

	    String sql =
	        "SELECT COALESCE(SUM(zz_male + zz_female),0) " +
	        "FROM ZZ_WSP_ATR_WSP " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    BigDecimal total =
	        DB.getSQLValueBD(get_TrxName(), sql, submittedId);

	    MZZWSPATRSubmitted submitted =
	        MZZWSPATRSubmitted.getSubmitted(getCtx(), submittedId, get_TrxName());

	    submitted.updateChecklistTotal(
	        MZZWSPATRSubmitted.CL_WSP_TOTAL,
	        total);
	}

}
