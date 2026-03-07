package za.co.ntier.wf.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;

import za.ntier.models.X_ZZ_WF_Header;

public class MZZWFHeader extends X_ZZ_WF_Header {
    private static final long serialVersionUID = 1L;

	public MZZWFHeader(Properties ctx, int id, String trxName) { 
    	super(ctx, id, trxName); }
    
    public MZZWFHeader (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }
    
    public static MZZWFHeader forTable(Properties ctx, int adTableId, String trxName) {
        return new Query(ctx, Table_Name, "AD_Table_ID=? AND IsActive='Y'", trxName)
                .setParameters(adTableId).setOnlyActiveRecords(true).first();
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
