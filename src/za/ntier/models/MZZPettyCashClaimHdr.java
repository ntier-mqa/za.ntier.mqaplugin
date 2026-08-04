package za.ntier.models;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.DB;

public class MZZPettyCashClaimHdr extends X_ZZ_Petty_Cash_Claim_Hdr {

	private static final long serialVersionUID = 1L;

	public MZZPettyCashClaimHdr(Properties ctx, int ZZ_Petty_Cash_Claim_Hdr_ID, String trxName) {
		super(ctx, ZZ_Petty_Cash_Claim_Hdr_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimHdr(Properties ctx, int ZZ_Petty_Cash_Claim_Hdr_ID, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_Petty_Cash_Claim_Hdr_ID, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimHdr(Properties ctx, String ZZ_Petty_Cash_Claim_Hdr_UU, String trxName) {
		super(ctx, ZZ_Petty_Cash_Claim_Hdr_UU, trxName);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimHdr(Properties ctx, String ZZ_Petty_Cash_Claim_Hdr_UU, String trxName,
			String... virtualColumns) {
		super(ctx, ZZ_Petty_Cash_Claim_Hdr_UU, trxName, virtualColumns);
		// TODO Auto-generated constructor stub
	}

	public MZZPettyCashClaimHdr(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}
	@Override
	protected boolean beforeDelete() {
		if (!getZZ_DocStatus().equals(ZZ_DOCSTATUS_Draft)) {
			log.saveError("Error", "Only Drafted Cash Claim Requests can be deleted");
			return false;
		}
		return super.beforeDelete();
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord && getZZ_Petty_Cash_Advance_Hdr_ID() <= 0) {
			int advanceID = findAdvance(getZZ_Credit_Card_No(), getTotalAmt());
			if (advanceID > 0) {
				setZZ_Petty_Cash_Advance_Hdr_ID(advanceID);  // Link to advance for recons
			}
		}
		return super.beforeSave(newRecord);
	}

	// mirrors PettyCashClaimRequest.findAdvance() so a matching advance is linked as soon as the claim is created
	private int findAdvance(String zz_Credit_Card_No, BigDecimal claimAmt) {
		if (zz_Credit_Card_No == null || claimAmt == null) {
			return -1;
		}
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String selectQuery = "SELECT ca.ZZ_Petty_Cash_Advance_Hdr_ID"
				+ " from ZZ_Petty_Cash_Advance_Hdr ca "
				+ " where ca.ZZ_DocStatus = 'CO' and ca.ZZ_Credit_Card_No = ? "
				+ " order by ca.ZZ_Date_Completed Desc limit 1";
		try {
			pstmt = DB.prepareStatement(selectQuery, get_TrxName());
			pstmt.setString(1, zz_Credit_Card_No);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				int advanceID = rs.getInt(1);
				if (advanceID > 0) {
					int reconCnt = DB.getSQLValueEx(get_TrxName(),
							"Select count(*) from ZZ_Petty_Cash_Recon_Claim rc"
							+ " join ZZ_Petty_Cash_Claim_Hdr ch on ch.ZZ_Petty_Cash_Claim_Hdr_ID = rc.ZZ_Petty_Cash_Claim_Hdr_ID"
							+ " where ch.ZZ_Petty_Cash_Advance_Hdr_ID = ?", advanceID);
					if (reconCnt <= 0) {
						MZZPettyCashAdvanceHdr mZZPettyCashAdvanceHdr = new MZZPettyCashAdvanceHdr(getCtx(), advanceID, get_TrxName());
						BigDecimal advanceTotal = (mZZPettyCashAdvanceHdr.getTotalAmt() != null) ? mZZPettyCashAdvanceHdr.getTotalAmt() : BigDecimal.ZERO;
						BigDecimal advanceBalance = advanceTotal.subtract(getClaimTotalForAdvance(advanceID, getZZ_Petty_Cash_Claim_Hdr_ID()));
						if (advanceBalance.compareTo(claimAmt) >= 0) {
							return advanceID;
						}
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, selectQuery, ex);
		} finally {
			DB.close(rs, pstmt);
		}
		return -1;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success && newRecord) {
			int advanceID = getZZ_Petty_Cash_Advance_Hdr_ID();
			if (advanceID > 0) {
				MZZPettyCashAdvanceHdr mZZPettyCashAdvanceHdr = new MZZPettyCashAdvanceHdr(getCtx(), advanceID, get_TrxName());
				BigDecimal totalAmtClaim = getClaimTotalForAdvance(advanceID, getZZ_Petty_Cash_Claim_Hdr_ID());
				BigDecimal totalAmtAdvance = (mZZPettyCashAdvanceHdr.getTotalAmt() != null) ? mZZPettyCashAdvanceHdr.getTotalAmt() : BigDecimal.ZERO;
				totalAmtClaim = totalAmtClaim.add((getTotalAmt() != null) ? getTotalAmt() : BigDecimal.ZERO);
				BigDecimal zz_Advance_Balance = totalAmtAdvance.subtract(totalAmtClaim);
				if (zz_Advance_Balance.compareTo(BigDecimal.ZERO) < 0) {
					zz_Advance_Balance = BigDecimal.ZERO;
				}
				setZZ_Advance_Balance(zz_Advance_Balance);
				mZZPettyCashAdvanceHdr.setZZ_Advance_Balance(zz_Advance_Balance);
				mZZPettyCashAdvanceHdr.saveEx();
				if (!save()) {
					log.saveError("Error", "Unable to update Advance Balance on new Petty Cash Claim");
					return false;
				}
			}
		}
		return super.afterSave(newRecord, success);
	}

	// exclude the current claim
	private BigDecimal getClaimTotalForAdvance(int zz_Petty_Cash_Advance_Hdr_ID, int zz_Petty_Cash_Claim_Hdr_ID) {
		BigDecimal claimTotal = DB.getSQLValueBD(get_TrxName(),
				"Select sum(cl.totalamt) from ZZ_Petty_Cash_Claim_Hdr cl where cl.ZZ_Petty_Cash_Advance_Hdr_ID = ? and cl.ZZ_Petty_Cash_Claim_Hdr_ID <> ?",
				zz_Petty_Cash_Advance_Hdr_ID, zz_Petty_Cash_Claim_Hdr_ID);
		return claimTotal != null ? claimTotal : BigDecimal.ZERO;
	}

}
