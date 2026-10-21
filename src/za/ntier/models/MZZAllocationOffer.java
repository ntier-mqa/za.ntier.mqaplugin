package za.ntier.models;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

import za.co.ntier.api.model.I_ZZ_Allocation_Intake_Plan;
import za.co.ntier.api.model.X_ZZ_Allocation_Intake_Plan;
import za.co.ntier.api.model.X_ZZ_Allocation_Offer;

/**
 * Custom Model for ZZ_Allocation_Offer
 */
public class MZZAllocationOffer extends X_ZZ_Allocation_Offer
{
	private static final long serialVersionUID = 20261016L;

	public MZZAllocationOffer(Properties ctx, int ZZ_Allocation_Offer_ID, String trxName)
	{
		super(ctx, ZZ_Allocation_Offer_ID, trxName);
	}

	public MZZAllocationOffer(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Get intake plan lines for this allocation offer
	 * @return List of intake plan lines
	 */
	public List<X_ZZ_Allocation_Intake_Plan> getIntakeLines()
	{
		return new Query(getCtx(), I_ZZ_Allocation_Intake_Plan.Table_Name,
				I_ZZ_Allocation_Intake_Plan.COLUMNNAME_ZZ_Allocation_Offer_ID + " = ?", get_TrxName())
				.setParameters(getZZ_Allocation_Offer_ID())
				.setOnlyActiveRecords(true)
				.setOrderBy(I_ZZ_Allocation_Intake_Plan.COLUMNNAME_LineNo)
				.list();
	}

	/**
	 * Calculate total number of learners across all intake lines
	 * @return Total intake learners
	 */
	public int calculateTotalIntakeLearners()
	{
		int total = 0;
		for (X_ZZ_Allocation_Intake_Plan line : getIntakeLines())
		{
			total += line.getZZNoLearners();
		}
		return total;
	}

	/**
	 * Approve / Accept offer action by MQA LP Admin
	 */
	public void approveOffer()
	{
//		setZZ_DocStatus(ZZ_DOCSTATUS_OfferAccepted);
		setZZAcceptedLearners(calculateTotalIntakeLearners());
		saveEx();
	}

	/**
	 * Return with Query action by MQA LP Admin
	 * @param queryComments Reason / failure comments
	 */
	public void queryOffer(String queryComments)
	{
		setZZ_DocStatus(ZZ_DOCSTATUS_Query);
		setZZ_Reason(queryComments);
		saveEx();
	}
}
