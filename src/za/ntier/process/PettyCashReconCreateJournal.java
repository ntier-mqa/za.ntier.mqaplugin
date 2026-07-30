package za.ntier.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCharge;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalBatch;
import org.compiere.model.MJournalLine;
import org.compiere.model.X_C_DocType;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.ntier.models.X_ZZ_Petty_Cash_Recon_Claim;
import za.ntier.models.X_ZZ_Petty_Cash_Recon_Hdr;


@Process(name = "za.ntier.process.PettyCashReconCreateJournal")
public class PettyCashReconCreateJournal extends SvrProcess {

	private static final String OTHER_CASH_CONTROL_CHARGE_NAME = "Other Cash Control";

	@Override
	protected void prepare() {
		// No parameters - everything is derived from the header record and system config.
	}

	@Override
	protected String doIt() throws Exception {
		int zz_Petty_Cash_Recon_Hdr_ID = getRecord_ID();
		X_ZZ_Petty_Cash_Recon_Hdr hdr = new X_ZZ_Petty_Cash_Recon_Hdr(getCtx(), zz_Petty_Cash_Recon_Hdr_ID, get_TrxName());
		if (hdr.get_ID() <= 0)
			throw new AdempiereException("Petty Cash Recon header not found: ID=" + zz_Petty_Cash_Recon_Hdr_ID);

		int adClientID = hdr.getAD_Client_ID();
		int adOrgID = hdr.getAD_Org_ID();
		Timestamp dateAcct = hdr.getEndDate() != null ? hdr.getEndDate() : new Timestamp(System.currentTimeMillis());

		MAcctSchema[] schemas = MAcctSchema.getClientAcctSchema(getCtx(), adClientID, get_TrxName());
		if (schemas == null || schemas.length == 0)
			throw new AdempiereException("No Accounting Schema found for AD_Client_ID=" + adClientID);
		MAcctSchema as = schemas[0];

		int docTypeID = DB.getSQLValue(get_TrxName(),
				"SELECT C_DocType_ID FROM C_DocType WHERE DocBaseType=? AND IsActive='Y' AND AD_Client_ID=? ORDER BY C_DocType_ID",
				X_C_DocType.DOCBASETYPE_GLJournal, adClientID);
		if (docTypeID <= 0)
			throw new AdempiereException("Could not find an active GL Journal document type (DocBaseType=GLJ) for AD_Client_ID=" + adClientID);

		int glCategoryID = DB.getSQLValue(get_TrxName(),
				"SELECT GL_Category_ID FROM GL_Category WHERE IsDefault='Y' AND AD_Client_ID=? AND IsActive='Y' ORDER BY GL_Category_ID",
				adClientID);
		if (glCategoryID <= 0)
			throw new AdempiereException("Could not find a default GL Category for AD_Client_ID=" + adClientID);

		int otherCashControlChargeID = DB.getSQLValue(get_TrxName(),
				"SELECT C_Charge_ID FROM C_Charge WHERE UPPER(Name)=UPPER(?) AND IsActive='Y' AND AD_Client_ID=? ORDER BY C_Charge_ID",
				OTHER_CASH_CONTROL_CHARGE_NAME, adClientID);
		if (otherCashControlChargeID <= 0)
			throw new AdempiereException("Could not find Charge named '" + OTHER_CASH_CONTROL_CHARGE_NAME + "' for AD_Client_ID=" + adClientID);
		MAccount crAccount = MCharge.getAccount(otherCashControlChargeID, as);
		if (crAccount == null)
			throw new AdempiereException("Charge '" + OTHER_CASH_CONTROL_CHARGE_NAME + "' has no account configured for Accounting Schema " + as.getName());

		MJournalBatch batch = new MJournalBatch(getCtx(), 0, get_TrxName());
		batch.setClientOrg(adClientID, adOrgID);
		batch.setDescription("Petty Cash Recon " + hdr.getDocumentNo());
		batch.setC_DocType_ID(docTypeID);
		batch.setPostingType(MJournalBatch.POSTINGTYPE_Actual);
		batch.setGL_Category_ID(glCategoryID);
		batch.setC_Currency_ID(as.getC_Currency_ID());
		batch.setDateAcct(dateAcct);
		batch.setDateDoc(dateAcct);
		batch.saveEx();

		MJournal journal = new MJournal(getCtx(), 0, get_TrxName());
		journal.setClientOrg(adClientID, adOrgID);
		journal.setGL_JournalBatch_ID(batch.getGL_JournalBatch_ID());
		journal.setDescription("Petty Cash Recon " + hdr.getDocumentNo());
		journal.setC_DocType_ID(docTypeID);
		journal.setGL_Category_ID(glCategoryID);
		journal.setPostingType(MJournal.POSTINGTYPE_Actual);
		journal.setC_Currency_ID(as.getC_Currency_ID());
		journal.setC_AcctSchema_ID(as.getC_AcctSchema_ID());
		journal.setDateAcct(dateAcct);
		journal.setDateDoc(dateAcct);
		journal.saveEx();

		BigDecimal total = BigDecimal.ZERO;
		int lineNo = 0;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "SELECT ZZ_Petty_Cash_Recon_Claim_ID, C_Charge_ID, Amount, AD_Org_ID, ZZ_Petty_Cash_Motivation "
				+ "FROM ZZ_Petty_Cash_Recon_Claim WHERE ZZ_Petty_Cash_Recon_Hdr_ID = ? ORDER BY Line";
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, zz_Petty_Cash_Recon_Hdr_ID);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int claimID = rs.getInt(1);
				int chargeID = rs.getInt(2);
				BigDecimal amount = rs.getBigDecimal(3);
				int lineOrgID = rs.getInt(4);
				String motivation = rs.getString(5);
				if (chargeID <= 0 || amount == null || amount.signum() == 0)
					continue;

				MAccount drAccount = MCharge.getAccount(chargeID, as);
				if (drAccount == null)
					throw new AdempiereException("Charge (C_Charge_ID=" + chargeID + ") has no account configured for Accounting Schema " + as.getName());

				lineNo += 10;
				MJournalLine line = new MJournalLine(journal);
				line.setAD_Org_ID(lineOrgID > 0 ? lineOrgID : adOrgID);
				line.setLine(lineNo);
				line.setDescription(motivation);
				line.setC_ValidCombination_ID(drAccount);
				line.setAmtSourceDr(amount);
				line.setAmtSourceCr(BigDecimal.ZERO);
				line.setAmtAcctDr(amount);
				line.setAmtAcctCr(BigDecimal.ZERO);
				line.saveEx();

				X_ZZ_Petty_Cash_Recon_Claim claim = new X_ZZ_Petty_Cash_Recon_Claim(getCtx(), claimID, get_TrxName());
				claim.setGL_JournalLine_ID(line.getGL_JournalLine_ID());
				claim.saveEx();

				total = total.add(amount);
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, sql, ex);
			throw ex;
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}

		if (total.signum() == 0)
			throw new AdempiereException("No claim lines with an amount found for Petty Cash Recon Header ID=" + zz_Petty_Cash_Recon_Hdr_ID);

		lineNo += 10;
		MJournalLine crLine = new MJournalLine(journal);
		crLine.setAD_Org_ID(adOrgID);
		crLine.setLine(lineNo);
		crLine.setDescription(OTHER_CASH_CONTROL_CHARGE_NAME);
		crLine.setC_ValidCombination_ID(crAccount);
		crLine.setAmtSourceDr(BigDecimal.ZERO);
		crLine.setAmtSourceCr(total);
		crLine.setAmtAcctDr(BigDecimal.ZERO);
		crLine.setAmtAcctCr(total);
		crLine.saveEx();

		return "@GL_Journal_ID@: " + journal.getDocumentNo();
	}

}
