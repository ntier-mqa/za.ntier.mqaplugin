package za.co.ntier.wf.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;

import za.co.ntier.wf.util.ADColumnUtil;
import za.ntier.models.X_ZZ_WF_Lines;
import org.compiere.util.DB;

public class MZZWFLines extends X_ZZ_WF_Lines {
	private static final long serialVersionUID = 1L;
	public MZZWFLines(Properties ctx, int id, String trxName)    { 
		super(ctx, id, trxName); 
	}
	/** Load Constructor */
	public MZZWFLines (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}


	public static List<MZZWFLines> listByHeaderOrderSeq(Properties ctx, int headerId, String trxName) {
		return new Query(ctx, Table_Name, "ZZ_WF_Header_ID=? AND IsActive='Y'", trxName)
				.setParameters(headerId).setOnlyActiveRecords(true).setOrderBy("SeqNo").list();
	}
	public static MZZWFLines findFirstByAllowedFromStatus(Properties ctx, int headerId, String status, String trxName) {
		return new Query(ctx, Table_Name, "ZZ_WF_Header_ID=? AND IsActive='Y' AND AllowedFromStatus=?", trxName)
				.setParameters(headerId, status).setOnlyActiveRecords(true).setOrderBy("SeqNo").first();
	}
	public static MZZWFLines findByStatusAndAction(Properties ctx, int headerId, String status, String action, String trxName) {
		return new Query(ctx, Table_Name, "ZZ_WF_Header_ID=? AND IsActive='Y' AND AllowedFromStatus=? AND SetDocAction=?", trxName)
				.setParameters(headerId, status, action).setOnlyActiveRecords(true).setOrderBy("SeqNo").first();
	}
	public String getResponsibleColumnName(Properties ctx, String trxName) {
		int colId = getZZ_Specific_Responsible_Col_ID();
		if (colId <= 0) return null;
		return ADColumnUtil.getColumnName(ctx, colId, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (newRecord || getSeqNo() == 0) {
			int nextSeqNo = DB.getSQLValue(get_TrxName(),
					"SELECT COALESCE(MAX(SeqNo), 0) + 10 FROM ZZ_WF_Lines WHERE ZZ_WF_Header_ID=?",
					getZZ_WF_Header_ID());
			setSeqNo(nextSeqNo);
		}
		return true;
	}
}
