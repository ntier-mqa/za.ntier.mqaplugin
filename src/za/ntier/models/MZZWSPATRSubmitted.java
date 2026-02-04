package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

public class MZZWSPATRSubmitted extends X_ZZ_WSP_ATR_Submitted {

	public MZZWSPATRSubmitted(Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_Submitted_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, int ZZ_WSP_ATR_Submitted_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Submitted_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_Submitted_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, String ZZ_WSP_ATR_Submitted_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Submitted_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRSubmitted(Properties ctx, ResultSet rs, String trxName) {
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
		return super.afterSave(newRecord, success);
	}

}
