package za.co.ntier.wsp_atr.repo;

import java.util.List;
import java.util.Properties;

import org.compiere.model.MAttachment;
import org.compiere.util.DB;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;

public class WspAtrUploadsRepository {

    private final Properties ctx;

    public WspAtrUploadsRepository(Properties ctx) {
        this.ctx = ctx;
    }

    public List<List<Object>> rawSubmittedRowsForUser(int adUserId) {
        String sql =
            "SELECT s.ZZ_WSP_ATR_Submitted_ID, s.SubmittedDate, v.orgname, s.ZZ_WSP_ATR_Status " +
            "FROM ZZ_WSP_ATR_Submitted s " +
            "LEFT JOIN adempiere.zzsdforganisation_v v ON v.zzsdforganisation_v_id = s.ZZSDFOrganisation_ID " +
            "WHERE v.ad_user_id = ? " +
            "ORDER BY s.ZZ_WSP_ATR_Submitted_ID DESC";

        return DB.getSQLArrayObjectsEx(null, sql, adUserId);
    }

    public Integer findExistingUploadId(int submittedId, String uploadType, String trxName) {
        int id = DB.getSQLValueEx(trxName,
            "SELECT ZZ_WSP_ATR_Uploads_ID " +
            "FROM ZZ_WSP_ATR_Uploads " +
            "WHERE ZZ_WSP_ATR_Submitted_ID=? " +
            "AND ZZ_WSP_ATR_Upload_Type=? " +
            "ORDER BY Updated DESC NULLS LAST, Created DESC NULLS LAST, ZZ_WSP_ATR_Uploads_ID DESC " +
            "FETCH FIRST 1 ROWS ONLY",
            submittedId, uploadType
        );
        return id > 0 ? Integer.valueOf(id) : null;
    }

    public String findFirstAttachmentFileName(int tableId, int recordId) {
        if (recordId <= 0) return null;
        MAttachment att = MAttachment.get(ctx, tableId, recordId);
        if (att == null || att.getEntryCount() <= 0) return null;

        for (var e : att.getEntries()) {
            if (e == null) continue;
            String n = e.getName();
            if (!Util.isEmpty(n, true)) return n;
        }
        return null;
    }

    public boolean columnExists(String tableName, String columnName) {
        int cnt = DB.getSQLValueEx(null,
            "SELECT COUNT(1) " +
            "FROM AD_Column c " +
            "JOIN AD_Table t ON t.AD_Table_ID=c.AD_Table_ID " +
            "WHERE t.TableName=? AND c.ColumnName=? AND c.IsActive='Y'",
            tableName, columnName
        );
        return cnt > 0;
    }

    public boolean hasReportLinkColumn() {
        return columnExists(X_ZZ_WSP_ATR_Report.Table_Name, "ZZ_WSP_ATR_Submitted_ID");
    }

    public int findLatestReportIdForSubmitted(int submittedId) {
        if (hasReportLinkColumn()) {
            return DB.getSQLValueEx(null,
                "SELECT ZZ_WSP_ATR_Report_ID " +
                "FROM ZZ_WSP_ATR_Report " +
                "WHERE ZZ_WSP_ATR_Submitted_ID=? " +
                "ORDER BY Created DESC, ZZ_WSP_ATR_Report_ID DESC " +
                "FETCH FIRST 1 ROWS ONLY",
                submittedId
            );
        }

        return DB.getSQLValueEx(null,
            "SELECT ZZ_WSP_ATR_Report_ID " +
            "FROM ZZ_WSP_ATR_Report " +
            "ORDER BY Created DESC, ZZ_WSP_ATR_Report_ID DESC " +
            "FETCH FIRST 1 ROWS ONLY"
        );
    }

    public boolean hasAnyReportForSubmitted(int submittedId) {
        if (hasReportLinkColumn()) {
            int cnt = DB.getSQLValueEx(null,
                "SELECT COUNT(1) FROM ZZ_WSP_ATR_Report WHERE ZZ_WSP_ATR_Submitted_ID=?",
                submittedId
            );
            return cnt > 0;
        }
        int any = DB.getSQLValueEx(null, "SELECT COUNT(1) FROM ZZ_WSP_ATR_Report");
        return any > 0;
    }

    public void deleteAttachment(int tableId, int recordId, String trxName) {
        MAttachment att = MAttachment.get(ctx, tableId, recordId);
        if (att != null) att.delete(true);
    }
}
