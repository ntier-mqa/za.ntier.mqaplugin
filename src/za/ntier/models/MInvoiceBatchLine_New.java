package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MInvoiceBatchLine;

public class MInvoiceBatchLine_New extends MInvoiceBatchLine implements I_C_InvoiceBatchLine {

	private static final long serialVersionUID = 1L;
	public MInvoiceBatchLine_New(Properties ctx, String C_InvoiceBatchLine_UU, String trxName) {
		super(ctx, C_InvoiceBatchLine_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MInvoiceBatchLine_New(Properties ctx, int C_InvoiceBatchLine_ID, String trxName) {
		super(ctx, C_InvoiceBatchLine_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MInvoiceBatchLine_New(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	/** Approved = A */
	public static final String ZZ_GRANT_STATUS_Approved = "A";
	/** Rejected = R */
	public static final String ZZ_GRANT_STATUS_Rejected = "R";
	/** Set Grant Status.
		@param ZZ_Grant_Status Grant Status
	*/
	public void setZZ_Grant_Status (String ZZ_Grant_Status)
	{

		set_ValueNoCheck (COLUMNNAME_ZZ_Grant_Status, ZZ_Grant_Status);
	}

	/** Get Grant Status.
		@return Grant Status	  */
	public String getZZ_Grant_Status()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Grant_Status);
	}

}
