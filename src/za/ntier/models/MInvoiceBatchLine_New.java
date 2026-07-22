package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MInvoiceBatchLine;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Util;

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

	/**
	 * Fin UAT - Unique vendor invoice numbers: the same Document No. must not be used
	 * more than once for the same Business Partner across different invoice batches or
	 * directly-entered invoices. The same Document No. is allowed to repeat within the
	 * same batch, since that is how a multi-line vendor invoice is created.
	 */
	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		String documentNo = getDocumentNo();
		int bpartnerId = getC_BPartner_ID();
		int batchId = getC_InvoiceBatch_ID();

		boolean relevantChange = newRecord
				|| is_ValueChanged(MInvoiceBatchLine.COLUMNNAME_DocumentNo)
				|| is_ValueChanged(MInvoiceBatchLine.COLUMNNAME_C_BPartner_ID)
				|| is_ValueChanged(MInvoiceBatchLine.COLUMNNAME_C_InvoiceBatch_ID);

		if (!Util.isEmpty(documentNo) && bpartnerId > 0 && batchId > 0 && relevantChange)
		{
			String trimmedDocNo = documentNo.trim();
			int lineId = get_ID();

			// Duplicate line for the same vendor sitting in a *different* batch
			String dupLineSql = "SELECT C_InvoiceBatchLine_ID FROM C_InvoiceBatchLine "
					+ "WHERE C_BPartner_ID=? AND UPPER(TRIM(DocumentNo))=UPPER(?) "
					+ "AND C_InvoiceBatch_ID<>? AND C_InvoiceBatchLine_ID<>? AND IsActive='Y'";
			int dupLineId = DB.getSQLValueEx(get_TrxName(), dupLineSql, bpartnerId, trimmedDocNo, batchId, lineId);

			// Duplicate on a directly-entered invoice (not created from a line of this same batch)
			int dupInvoiceId = -1;
			if (dupLineId <= 0)
			{
				String dupInvoiceSql = "SELECT i.C_Invoice_ID FROM C_Invoice i "
						+ "WHERE i.C_BPartner_ID=? AND UPPER(TRIM(i.DocumentNo))=UPPER(?) "
						+ "AND i.IsSOTrx='N' AND i.IsActive='Y' "
						+ "AND NOT EXISTS (SELECT 1 FROM C_InvoiceBatchLine bl "
						+ "WHERE bl.C_Invoice_ID=i.C_Invoice_ID AND bl.C_InvoiceBatch_ID=?)";
				dupInvoiceId = DB.getSQLValueEx(get_TrxName(), dupInvoiceSql, bpartnerId, trimmedDocNo, batchId);
			}

			if (dupLineId > 0 || dupInvoiceId > 0)
			{
				log.saveError("Error", "Document No. '" + trimmedDocNo + "' already exists for this vendor on another invoice. "
						+ "The invoice document number must be unique per supplier. "
						+ "(The same document no. is allowed within this batch to create a multi-line invoice.)");
				return false;
			}
		}

		return super.beforeSave(newRecord);
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
	
	/** Set Policy Procedure Checklist.
	@param ZZ_Policy_Procedure_Ck Policy Procedure Checklist
	 */
	public void setZZ_Policy_Procedure_Ck (Object ZZ_Policy_Procedure_Ck)
	{
		set_Value (COLUMNNAME_ZZ_Policy_Procedure_Ck, ZZ_Policy_Procedure_Ck);
	}

	/** Get Policy Procedure Checklist.
	@return Policy Procedure Checklist	  */
	public String getZZ_Policy_Procedure_Ck()
	{
		return (String)get_Value(COLUMNNAME_ZZ_Policy_Procedure_Ck);
	}


	/** Set Account Reconciled / O/S Invoices Verified.
	@param ZZ_Account_Reconned Account Reconciled / O/S Invoices Verified
	 */
	public void setZZ_Account_Reconned (boolean ZZ_Account_Reconned)
	{
		set_Value (COLUMNNAME_ZZ_Account_Reconned, Boolean.valueOf(ZZ_Account_Reconned));
	}

	/** Get Account Reconciled / O/S Invoices Verified.
	@return Account Reconciled / O/S Invoices Verified	  */
	public boolean isZZ_Account_Reconned()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Account_Reconned);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Authorized Purchase Order/SLA Attached.
	@param ZZ_Auth_PO_Order Authorized Purchase Order/SLA Attached
	 */
	public void setZZ_Auth_PO_Order (boolean ZZ_Auth_PO_Order)
	{
		set_Value (COLUMNNAME_ZZ_Auth_PO_Order, Boolean.valueOf(ZZ_Auth_PO_Order));
	}

	/** Get Authorized Purchase Order/SLA Attached.
	@return Authorized Purchase Order/SLA Attached	  */
	public boolean isZZ_Auth_PO_Order()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Auth_PO_Order);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Calculation Checked.
	@param ZZ_Calcs_Checked Calculation Checked
	 */
	public void setZZ_Calcs_Checked (boolean ZZ_Calcs_Checked)
	{
		set_Value (COLUMNNAME_ZZ_Calcs_Checked, Boolean.valueOf(ZZ_Calcs_Checked));
	}

	/** Get Calculation Checked.
	@return Calculation Checked	  */
	public boolean isZZ_Calcs_Checked()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Calcs_Checked);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Creditor ID &amp; Banking Details Verified.
	@param ZZ_Cred_Bank_Dets_Verified Creditor ID &amp; Banking Details Verified
	 */
	public void setZZ_Cred_Bank_Dets_Verified (boolean ZZ_Cred_Bank_Dets_Verified)
	{
		set_Value (COLUMNNAME_ZZ_Cred_Bank_Dets_Verified, Boolean.valueOf(ZZ_Cred_Bank_Dets_Verified));
	}

	/** Get Creditor ID &amp; Banking Details Verified.
	@return Creditor ID &amp; Banking Details Verified	  */
	public boolean isZZ_Cred_Bank_Dets_Verified()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_Cred_Bank_Dets_Verified);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set GL Allocation Checked.
	@param ZZ_GL_Allocation_Checked GL Allocation Checked
	 */
	public void setZZ_GL_Allocation_Checked (boolean ZZ_GL_Allocation_Checked)
	{
		set_Value (COLUMNNAME_ZZ_GL_Allocation_Checked, Boolean.valueOf(ZZ_GL_Allocation_Checked));
	}

	/** Get GL Allocation Checked.
	@return GL Allocation Checked	  */
	public boolean isZZ_GL_Allocation_Checked()
	{
		Object oo = get_Value(COLUMNNAME_ZZ_GL_Allocation_Checked);
		if (oo != null)
		{
			if (oo instanceof Boolean)
				return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}



	@Override
	public void setZZ_Policy_Procedure_Ck(String ZZ_Policy_Procedure_Ck) {
		// TODO Auto-generated method stub

	}


}
