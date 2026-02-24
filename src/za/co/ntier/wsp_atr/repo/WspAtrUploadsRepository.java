package za.co.ntier.wsp_atr.repo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MAttachment;
import org.compiere.util.DB;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;

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
            " And s.zz_wsp_atr_status in ('" + X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Imported + "'"
            		+ ",'" + X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Uploaded + "') " +
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
    
    public SdrWindowConfig getSdrWindowConfig(int adClientId) {
        // choose the “active” configuration row; if you have multiple per org, adjust filters
        List<List<Object>> rows = DB.getSQLArrayObjectsEx(null,
            "SELECT zz_wsp_atr_sub_start_date, zz_wsp_atr_sub_end_date, " +
            "       zz_wsp_atr_ext_start_date, zz_wsp_atr_ext_end_date " +
            "FROM zz_sdr_configuration " +
            "WHERE ad_client_id=? AND isactive='Y' " +
            "ORDER BY updated DESC " +
            "FETCH FIRST 1 ROWS ONLY",
            adClientId
        );
        if (rows == null || rows.isEmpty())
            return null;

        List<Object> r = rows.get(0);
        return new SdrWindowConfig(
            (Timestamp) r.get(0),
            (Timestamp) r.get(1),
            (Timestamp) r.get(2),
            (Timestamp) r.get(3)
        );
    }

    public int getSubmittedOrgId(int submittedId) {
    	X_ZZ_WSP_ATR_Submitted x_ZZ_WSP_ATR_Submitted = new X_ZZ_WSP_ATR_Submitted(ctx,submittedId,null);
    	return x_ZZ_WSP_ATR_Submitted.getZZSdfOrganisation_ID();
        // this is AD_Org_ID or your org link; adjust if needed
       // return DB.getSQLValueEx(null,
          //  "SELECT ad_org_id FROM zz_wsp_atr_submitted WHERE zz_wsp_atr_submitted_id=?",
         //   submittedId);
    }

    public boolean hasSubmittedTemplateAttachment(int submittedId) {
        // template is on ZZ_WSP_ATR_Submitted table attachments
        MAttachment att = MAttachment.get(ctx, X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId);
        if (att == null || att.getEntryCount() <= 0) return false;

        // Must be non-error and must be xlsm
        for (var e : att.getEntries()) {
            if (e == null) continue;
            String n = e.getName();
            if (Util.isEmpty(n, true)) continue;
            String upper = n.toUpperCase();
            if (upper.startsWith("ERROR")) continue;
            if (!upper.endsWith(".XLSM")) continue;
            return true;
        }
        return false;
    }

    public boolean hasUploadTypeAttachment(int submittedId, String uploadType) {
        Integer uploadId = findExistingUploadId(submittedId, uploadType, null);
        if (uploadId == null) return false;
        String file = findFirstAttachmentFileName(X_ZZ_WSP_ATR_Uploads.Table_ID, uploadId.intValue());
        return !Util.isEmpty(file, true);
    }

    /**
     * TODO: implement this based on your “Requests for Extensions (Batch) - WSP-ATR” tables
     */
    public boolean isOrgInApprovedWspAtrExtensionBatch(int zzSdfOrganisationId) {

        // NOTE: your parameter is actually ZZSDForgranisation_ID, not AD_Org_ID.
        X_ZZSdfOrganisation org = new X_ZZSdfOrganisation(ctx, zzSdfOrganisationId, null);
        int c_BPartner_ID = org.getC_BPartner_ID();
        if (c_BPartner_ID <= 0) {
            return false;
        }

        String sql =
            "SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END " +
            "FROM adempiere.c_bpartner bp " +
            "JOIN adempiere.zz_wsp_atr_extension e " +
            "  ON e.zz_sdl_no = bp.value " +
            "JOIN adempiere.zz_wsp_atr_extension_batch b " +
            "  ON b.zz_wsp_atr_extension_batch_id = e.zz_wsp_atr_extension_batch_id " +
            "WHERE bp.c_bpartner_id = ? " +
            "  AND bp.isactive = 'Y' " +
            "  AND e.isactive = 'Y' " +
            "  AND b.isactive = 'Y' " +
            "  AND b.zz_docstatus IN ('AP') " +  // <-- adjust if your "approved" code differs
            "  AND b.zz_wsp_atr_ext_start_date IS NOT NULL " +
            "  AND b.zz_wsp_atr_ext_end_date IS NOT NULL " +
            "  AND CURRENT_TIMESTAMP BETWEEN b.zz_wsp_atr_ext_start_date AND b.zz_wsp_atr_ext_end_date ";

        int withinWindow = DB.getSQLValueEx(null, sql, c_BPartner_ID);
        return withinWindow == 1;
    }

    /**
     * “Uploaded” status check — YOU must map this to your real status values/column.
     * If you already have a ZZ_WSP_ATR_Submission_Status column, use that instead.
     */
    public boolean isSubmissionStatusUploaded(int submittedId) {
        String status = DB.getSQLValueStringEx(null,
            "SELECT zz_wsp_atr_status FROM zz_wsp_atr_submitted WHERE zz_wsp_atr_submitted_id=?",
            submittedId);

        // TODO: change to your real value for “Uploaded”
        return X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Uploaded.equalsIgnoreCase(status);
    }

    // small DTO inside repo package
    public static class SdrWindowConfig {
        public final Timestamp subStart, subEnd, extStart, extEnd;
        public SdrWindowConfig(Timestamp subStart, Timestamp subEnd, Timestamp extStart, Timestamp extEnd) {
            this.subStart = subStart;
            this.subEnd = subEnd;
            this.extStart = extStart;
            this.extEnd = extEnd;
        }
    }
}
