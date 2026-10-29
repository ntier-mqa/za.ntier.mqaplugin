package za.co.ntier.wf.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Query;
import org.compiere.util.Env;

import za.ntier.models.X_ZZ_WF_Header;

public class MZZWFHeader extends X_ZZ_WF_Header {
    private static final long serialVersionUID = 1L;

	public MZZWFHeader(Properties ctx, int id, String trxName) { 
    	super(ctx, id, trxName); }
    
    public MZZWFHeader (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
    
	/**
	 * Legacy table-only lookup retained for callers outside this plugin.
	 * Workflow execution should use the overload with document client and Unit.
	 */
	@Deprecated
	public static MZZWFHeader forTable(Properties ctx, int adTableId, String trxName)
	{
		return new Query(ctx, Table_Name, "AD_Table_ID=?", trxName)
																	.setParameters(adTableId).setOnlyActiveRecords(true).first();
	}

	/** Compatibility overload; document-aware callers should supply the client explicitly. */
	@Deprecated
	public static MZZWFHeader forTable(	Properties ctx, int adTableId,
										int adOrgId, String trxName)
	{
		return forTable(ctx, adTableId, Env.getAD_Client_ID(ctx), adOrgId, trxName);
	}

	/**
	 * Resolve an active workflow using the document's ownership, not the login context.
	 * Checks the client's Unit override, the client's * Unit, then the System * Unit.
	 * Returns null if none exists; duplicate headers at a matching level are an error.
	 */
	public static MZZWFHeader forTable(	Properties ctx, int adTableId, int adClientId,
										int adOrgId, String trxName)
	{
		MZZWFHeader header = findActiveHeader(ctx, adTableId, adClientId, adOrgId, trxName);
		if (header == null && adOrgId != 0)
		{
			header = findActiveHeader(ctx, adTableId, adClientId, 0, trxName);
		}
		if (header == null && adClientId != 0)
		{
			header = findActiveHeader(ctx, adTableId, 0, 0, trxName);
		}
		return header;
	}

	private static MZZWFHeader findActiveHeader(Properties ctx, int adTableId,
												int adClientId, int adOrgId, String trxName)
	{
		List<MZZWFHeader> headers = new Query(	ctx, Table_Name,
												"AD_Table_ID=? AND AD_Client_ID=? AND AD_Org_ID=?", trxName)
																											.setParameters(adTableId, adClientId, adOrgId)
																											.setOnlyActiveRecords(true).list();
		if (headers.size() > 1)
		{
			throw new AdempiereException(	"Multiple active workflows configured for table "
											+ adTableId + ", client " + adClientId + " and Unit " + adOrgId
											+ ". Please keep only one active workflow for this combination.");
		}
		return headers.isEmpty() ? null : headers.get(0);
	}
    
    public static MZZWFHeader getById(Properties ctx, int headerId, String trxName) {
        return new MZZWFHeader(ctx, headerId, trxName);
    }
    
    public static int getMinSeqNo(Properties ctx, int wfHeaderId, String trxName) {
        Integer min = new org.compiere.model.Query(ctx, MZZWFLines.Table_Name,
                "ZZ_WF_Header_ID=? AND IsActive='Y' AND SeqNo IS NOT NULL", trxName)
                .setParameters(wfHeaderId)
                .aggregate("SeqNo", org.compiere.model.Query.AGGREGATE_MIN, Integer.class);

        return (min != null) ? min.intValue() : Integer.MAX_VALUE;
    }
    
    public static MZZWFLines getFirstLine(Properties ctx, int wfHeaderId, String trxName) {
        return new org.compiere.model.Query(ctx, MZZWFLines.Table_Name,
                "ZZ_WF_Header_ID=? AND IsActive='Y' AND SeqNo IS NOT NULL", trxName)
                .setParameters(wfHeaderId)
                .setOrderBy("SeqNo ASC, ZZ_WF_Lines_ID ASC")
                .first();
    }



}
