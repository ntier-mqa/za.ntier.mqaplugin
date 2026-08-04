package za.ntier.models;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.DB;

public class MZZPettyCashClaimLine extends X_ZZ_Petty_Cash_Claim_Line {

	private static final long serialVersionUID = 1L;

	public MZZPettyCashClaimLine(Properties ctx, int ZZ_Petty_Cash_Claim_Line_ID, String trxName) {
		super(ctx, ZZ_Petty_Cash_Claim_Line_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimLine(Properties ctx, int ZZ_Petty_Cash_Claim_Line_ID, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_Petty_Cash_Claim_Line_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimLine(Properties ctx, String ZZ_Petty_Cash_Claim_Line_UU, String trxName) {
		super(ctx, ZZ_Petty_Cash_Claim_Line_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimLine(Properties ctx, String ZZ_Petty_Cash_Claim_Line_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_Petty_Cash_Claim_Line_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimLine(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success)
		{			
			updateTotals();
		}
		return super.afterSave(newRecord, success);
	}
	

	
	@Override
	protected boolean afterDelete(boolean success) {
		if (success)
		{			
			updateTotals();
		}
		return super.afterDelete(success);
	}
	
	
	private void updateTotals() {
		StringBuilder sql = new StringBuilder("UPDATE ZZ_Petty_Cash_Claim_Hdr h ")
			.append("SET TotalAmt = NVL((SELECT SUM(Amount) FROM ZZ_Petty_Cash_Claim_Line l ")
				.append("WHERE h.ZZ_Petty_Cash_Claim_Hdr_ID=l.ZZ_Petty_Cash_Claim_Hdr_ID AND l.IsActive='Y'),0) ")
			.append("WHERE ZZ_Petty_Cash_Claim_Hdr_ID=").append(getZZ_Petty_Cash_Claim_Hdr_ID());
		DB.executeUpdate(sql.toString(), get_TrxName());
		MZZPettyCashClaimHdr mZZPettyCashClaimHdr = new MZZPettyCashClaimHdr(getCtx(), getZZ_Petty_Cash_Claim_Hdr_ID(), get_TrxName());
		int advanceID = mZZPettyCashClaimHdr.getZZ_Petty_Cash_Advance_Hdr_ID();
		if (advanceID > 0) {
			MZZPettyCashAdvanceHdr mZZPettyCashAdvanceHdr = new MZZPettyCashAdvanceHdr(getCtx(), advanceID, get_TrxName());
			BigDecimal totalAmtAdvance = (mZZPettyCashAdvanceHdr.getTotalAmt() != null) ? mZZPettyCashAdvanceHdr.getTotalAmt() : BigDecimal.ZERO;
			// other claims already linked to the same advance also count against its balance
			BigDecimal otherClaimsTotal = DB.getSQLValueBD(get_TrxName(),
					"Select sum(cl.totalamt) from ZZ_Petty_Cash_Claim_Hdr cl where cl.ZZ_Petty_Cash_Advance_Hdr_ID = ? and cl.ZZ_Petty_Cash_Claim_Hdr_ID <> ?",
					advanceID, mZZPettyCashClaimHdr.getZZ_Petty_Cash_Claim_Hdr_ID());
			if (otherClaimsTotal == null) {
				otherClaimsTotal = BigDecimal.ZERO;
			}
			BigDecimal totalAmtClaim = otherClaimsTotal.add((mZZPettyCashClaimHdr.getTotalAmt() != null) ? mZZPettyCashClaimHdr.getTotalAmt() : BigDecimal.ZERO);
			BigDecimal zz_Advance_Balance = totalAmtAdvance.subtract(totalAmtClaim);
			if (zz_Advance_Balance.compareTo(BigDecimal.ZERO) < 0) {
				zz_Advance_Balance = BigDecimal.ZERO;
			}
			mZZPettyCashClaimHdr.setZZ_Advance_Balance(zz_Advance_Balance);
			mZZPettyCashClaimHdr.saveEx();
			mZZPettyCashAdvanceHdr.setZZ_Advance_Balance(zz_Advance_Balance);
			mZZPettyCashAdvanceHdr.saveEx();
		}
	}

	
	
	
}
