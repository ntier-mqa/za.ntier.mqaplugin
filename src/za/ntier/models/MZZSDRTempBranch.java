package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

public class MZZSDRTempBranch extends X_ZZ_SDR_Temp_Branch {

	public MZZSDRTempBranch(Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName) {
		super(ctx, ZZ_SDR_Temp_Branch_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZSDRTempBranch(Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_SDR_Temp_Branch_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZSDRTempBranch(Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName) {
		super(ctx, ZZ_SDR_Temp_Branch_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZSDRTempBranch(Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_SDR_Temp_Branch_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZSDRTempBranch(Properties ctx, ResultSet rs, String trxName) {
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
