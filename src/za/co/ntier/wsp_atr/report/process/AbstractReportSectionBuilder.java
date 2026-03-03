package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

public abstract class AbstractReportSectionBuilder implements IReportSectionBuilder {

	/**
	 * Optional: if you want reruns to be idempotent, delete prior rows for this report+section.
	 */
	protected int deleteExistingByReportAndSection(String targetTable, int reportId, String section, String trxName) {
		String sql = "DELETE FROM " + targetTable + " WHERE ZZ_WSP_ATR_Report_ID=?";
		return DB.executeUpdateEx(sql, new Object[] { reportId}, trxName);
	}
	
	public String getParentAndChildSubmittedIdsInClause(Properties ctx,
			int parentSubmittedId,
			String trxName) {

		final String sql =
				"WITH parent AS ( " +
						"   SELECT s.zz_wsp_atr_submitted_id AS parent_submitted_id, " +
						"          so.c_bpartner_id AS parent_bp " +
						"   FROM adempiere.zz_wsp_atr_submitted s " +
						"   JOIN adempiere.zzsdforganisation so " +
						"     ON so.zzsdforganisation_id = s.zzsdforganisation_id " +
						"   WHERE s.zz_wsp_atr_submitted_id = ? " +
						"), " +
						"children_bp AS ( " +
						"   SELECT l.c_bpartner_id AS child_bp " +
						"   FROM adempiere.zzorganisationlinkage l " +
						"   JOIN parent p ON l.bpartner_parent_id = p.parent_bp " +
						"   WHERE l.isactive = 'Y' and l.zz_parent_uploads = 'Y' " +
						"), " +
						"children_submitted AS ( " +
						"   SELECT s.zz_wsp_atr_submitted_id " +
						"   FROM adempiere.zz_wsp_atr_submitted s " +
						"   JOIN adempiere.zzsdforganisation so " +
						"     ON so.zzsdforganisation_id = s.zzsdforganisation_id " +
						"   JOIN children_bp cb " +
						"     ON cb.child_bp = so.c_bpartner_id " +
						"   WHERE s.isactive = 'Y' " +
						"     AND s.zz_docstatus IN ('" +
						X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported + "','" +
						X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Uploaded + "','" +
						X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Query + "') " +
						") " +
						"SELECT parent_submitted_id FROM parent " +
						"UNION ALL " +
						"SELECT zz_wsp_atr_submitted_id FROM children_submitted " +
						"ORDER BY 1";

		List<Integer> ids = new ArrayList<>();

		try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
			pstmt.setInt(1, parentSubmittedId);

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					ids.add(rs.getInt(1));
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to load parent/child submitted ids for "
					+ parentSubmittedId + ": " + e.getMessage(), e);
		}

		if (ids.isEmpty()) {
			return "(" + parentSubmittedId + ")";
		}

		return "(" + ids.stream()
		.map(String::valueOf)
		.collect(Collectors.joining(",")) + ")";
	}

}
