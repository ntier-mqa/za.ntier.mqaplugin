package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Veri_Checklist;

public class MZZWSPATRVeriChecklist extends X_ZZ_WSP_ATR_Veri_Checklist {

	public MZZWSPATRVeriChecklist(Properties ctx, int ZZ_WSP_ATR_Veri_Checklist_ID, String trxName) {
		super(ctx, ZZ_WSP_ATR_Veri_Checklist_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRVeriChecklist(Properties ctx, int ZZ_WSP_ATR_Veri_Checklist_ID, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Veri_Checklist_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRVeriChecklist(Properties ctx, String ZZ_WSP_ATR_Veri_Checklist_UU, String trxName) {
		super(ctx, ZZ_WSP_ATR_Veri_Checklist_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRVeriChecklist(Properties ctx, String ZZ_WSP_ATR_Veri_Checklist_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_WSP_ATR_Veri_Checklist_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZWSPATRVeriChecklist(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

}
