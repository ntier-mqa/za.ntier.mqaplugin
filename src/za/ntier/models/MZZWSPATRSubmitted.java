package za.ntier.models;

import java.sql.ResultSet;
import org.compiere.model.PO;  // Required for getAllIDs
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
		 if (!newRecord || !success)
	            return success;

	        createVerificationChecklist();
	        return success;
	}
	
	private void createVerificationChecklist()
	{
	    String whereClause =
	        X_ZZ_WSP_ATR_Checklist_Ref.COLUMNNAME_IsActive + "='Y'";

	    int[] checklistRefIDs = PO.getAllIDs(
	        X_ZZ_WSP_ATR_Checklist_Ref.Table_Name,
	        whereClause,
	        get_TrxName());

	    for (int refID : checklistRefIDs)
	    {
	        // Child line
	        MZZWSPATRVeriChecklist line =
	            new MZZWSPATRVeriChecklist(getCtx(), 0, get_TrxName());

	        // Link to header
	        line.setZZ_WSP_ATR_Submitted_ID(getZZ_WSP_ATR_Submitted_ID());

	        // Link to checklist template
	        line.setzz_wsp_atr_checklist_ref_ID(refID);
	        
	        // Copy name from template
	        X_ZZ_WSP_ATR_Checklist_Ref ref =
	            new X_ZZ_WSP_ATR_Checklist_Ref(getCtx(), refID, get_TrxName());
	        line.setZZ_Checklist_No(ref.getValue());  // "1", "2", "3"
	        line.setName(ref.getName());

	        // Default completed flag
	        line.setZZ_Information_Completed(false);

	        // Save child line
	        line.saveEx();
	    }
	}

}
