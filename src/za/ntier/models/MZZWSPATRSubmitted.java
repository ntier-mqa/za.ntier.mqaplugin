package za.ntier.models;

import java.sql.ResultSet;
import org.compiere.model.PO;  // Required for getAllIDs
import java.util.Properties;
import java.util.List;
import org.compiere.model.Query;

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
	    // Call parent first
	    boolean ok = super.afterSave(newRecord, success);

	    // Only create checklist on new record
	    if (ok && newRecord)
	        createVerificationChecklist();

	    return ok;
	}
	
	
	private void createVerificationChecklist() {
	    // Load checklist template rows in numeric order of Value
		List<PO> poList = new Query(getCtx(),
		        X_ZZ_WSP_ATR_Checklist_Ref.Table_Name,
		        X_ZZ_WSP_ATR_Checklist_Ref.COLUMNNAME_IsActive + "='Y'",
		        get_TrxName())
		    .setOrderBy("CAST(Value AS integer)")
		    .list();   // <-- returns List<PO>

		int lineNo = 10;
		for (PO po : poList) {
		    X_ZZ_WSP_ATR_Checklist_Ref ref = 
		        new X_ZZ_WSP_ATR_Checklist_Ref(po.getCtx(), po.get_ID(), po.get_TrxName());

		    MZZWSPATRVeriChecklist line = new MZZWSPATRVeriChecklist(getCtx(), 0, get_TrxName());
		    line.setZZ_WSP_ATR_Submitted_ID(getZZ_WSP_ATR_Submitted_ID());
		    line.setzz_wsp_atr_checklist_ref_ID(ref.getzz_wsp_atr_checklist_ref_ID());
		    line.setLineNo(lineNo);
		    lineNo += 10;
		    line.setZZ_Checklist_No(ref.getValue());
		    line.setName(ref.getName());
		    line.setZZ_Information_Completed(false);
		    line.saveEx();
		}
	}
	
	

}
