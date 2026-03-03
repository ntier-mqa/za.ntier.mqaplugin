package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

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
