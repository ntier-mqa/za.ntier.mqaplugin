package za.ntier.models;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_HTVF;

public class MZZWSPATRHTVF extends X_ZZ_WSP_ATR_HTVF {

	public MZZWSPATRHTVF(Properties ctx, int ZZ_WSP_ATR_HTVF_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_HTVF_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRHTVF(Properties ctx, int ZZ_WSP_ATR_HTVF_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_HTVF_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRHTVF(Properties ctx, String ZZ_WSP_ATR_HTVF_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_HTVF_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRHTVF(Properties ctx, String ZZ_WSP_ATR_HTVF_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_HTVF_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRHTVF(Properties ctx, ResultSet rs, String trxName) {
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
		
		updateHTVFTotal();
		
		return super.afterSave(newRecord, success);
	}

	private void updateHTVFTotal()
	{
	    int submittedId = getZZ_WSP_ATR_Submitted_ID();

	    String sql =
	        "SELECT COUNT(*) " +
	        "FROM ZZ_WSP_ATR_HTVF " +
	        "WHERE ZZ_WSP_ATR_Submitted_ID=?";

	    int total = DB.getSQLValue(get_TrxName(), sql, submittedId);

	    MZZWSPATRSubmitted submitted =
	        MZZWSPATRSubmitted.getSubmitted(getCtx(), submittedId, get_TrxName());

	    submitted.updateChecklistTotal(
	        MZZWSPATRSubmitted.CL_HTVF,
	        BigDecimal.valueOf(total));
	}
}
