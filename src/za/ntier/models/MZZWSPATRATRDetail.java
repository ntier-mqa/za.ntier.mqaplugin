package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

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
		return super.afterSave(newRecord, success);
	}

}
