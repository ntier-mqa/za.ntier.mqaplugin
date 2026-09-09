package za.ntier.process;

import java.util.ArrayList;
import java.util.List;

import org.compiere.model.MPaySelection;
import org.compiere.model.MPaySelectionCheck;
import org.compiere.model.MPaySelectionLine;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process(name="za.ntier.process.PaySelectionCheckReverseSelection")
public class PaySelectionCheckReverseSelection extends SvrProcess {

	private List<Integer> p_C_PaySelectionCheck_IDs = null;

	@Override
	protected void prepare() {
		p_C_PaySelectionCheck_IDs = getRecord_IDs();
		if (p_C_PaySelectionCheck_IDs == null || p_C_PaySelectionCheck_IDs.isEmpty()) {
			int[] ids = DB.getIDsEx(get_TrxName(),
					"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
					getAD_PInstance_ID());
			p_C_PaySelectionCheck_IDs = new ArrayList<>();
			for (int id : ids)
				p_C_PaySelectionCheck_IDs.add(id);
		}
		if (p_C_PaySelectionCheck_IDs.isEmpty())
			throw new AdempiereUserError("@NoSelection@");
	}

	@Override
	protected String doIt() throws Exception {
		int reversed = 0;
		int skipped = 0;
		int C_PaySelection_ID = 0;

		for (int id : p_C_PaySelectionCheck_IDs) {
			MPaySelectionCheck psc = new MPaySelectionCheck(getCtx(), id, get_TrxName());
			if (psc.get_ID() != id)
				continue;

			C_PaySelection_ID = psc.getC_PaySelection_ID();

			if (psc.getC_Payment_ID() > 0) {
				addLog(id, null, null, "@Error@ @C_PaySelectionCheck_ID@ @Processed@");
				skipped++;
				continue;
			}

			for (MPaySelectionLine psl : psc.getPaySelectionLines(false)) {
				psl.setC_PaySelectionCheck_ID(0);
				psl.setProcessed(false);
				psl.saveEx();
			}
			psc.deleteEx(true);
			reversed++;
		}

		if (reversed > 0 && C_PaySelection_ID > 0) {
			MPaySelection ps = new MPaySelection(getCtx(), C_PaySelection_ID, get_TrxName());
			int remaining = DB.getSQLValueEx(get_TrxName(),
					"SELECT COUNT(*) FROM C_PaySelectionCheck WHERE C_PaySelection_ID=?", C_PaySelection_ID);
			if (remaining == 0) {
				ps.setProcessed(false);
				ps.saveEx();
			}
		}

		StringBuilder msg = new StringBuilder("@Deleted@ #").append(reversed);
		if (skipped > 0)
			msg.append(", @Error@ #").append(skipped);
		return msg.toString();
	}
}
