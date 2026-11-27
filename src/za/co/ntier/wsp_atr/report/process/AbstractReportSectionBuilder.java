package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;

public abstract class AbstractReportSectionBuilder implements IReportSectionBuilder {
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

	/**
	 * Optional: if you want reruns to be idempotent, delete prior rows for this report+section.
	 */
	protected int deleteExistingByReportAndSection(String targetTable, int reportId, String section, String trxName) {
		String sql = "DELETE FROM " + targetTable + " WHERE ZZ_WSP_ATR_Report_ID=?";
		return DB.executeUpdateEx(sql, new Object[] { reportId}, trxName);
	}

	/**
	 * Learning programme types that are NOT pivotal (MQA "List of Non-Pivotal courses").
	 * Compared against ZZ_Learning_Programme_Ref.Name with everything except letters stripped
	 * and upper-cased, so "Ex-Leave Training", "Ex_Leave" and "EXLEAVE" all match.
	 */
	private static final String NON_PIVOTAL_PROGRAMME_KEYS =
			"'INDUCTION','INDUCTIONTRAINING',"
			+ "'EXLEAVE','EXLEAVETRAINING',"
			+ "'CERTIFICATE',"
			+ "'SHORTCOURSE','SHORTCOURSES',"
			+ "'OPERATORLICENCERENEWAL','OPERATORLICENSERENEWAL',"
			+ "'REFRESHER','REFRESHERTRAINING'";

	/**
	 * SQL predicate that is true when the given learning-programme-type id column does not point
	 * to a non-pivotal programme. Rows with no programme type are kept.
	 *
	 * @param learningProgrammeTypeIdColumn qualified column holding a ZZ_Learning_Programme_Ref_ID
	 */
	protected String pivotalProgrammesOnlyPredicate(String learningProgrammeTypeIdColumn) {
		return "NOT EXISTS (SELECT 1 FROM zz_learning_programme_ref np \n"
				+ "    WHERE np.zz_learning_programme_ref_id = " + learningProgrammeTypeIdColumn + " \n"
				+ "      AND UPPER(regexp_replace(COALESCE(np.name,''), '[^A-Za-z]', '', 'g')) IN ("
				+ NON_PIVOTAL_PROGRAMME_KEYS + ")) \n";
	}

		
	protected String getParentAndChildSubmittedIdsInClause(
	        Properties ctx,
	        int parentSubmittedId,
	        boolean consolidatedSubmission,
	        boolean onlySubLevyOrgs,
	        String trxName) {

	    if (!consolidatedSubmission) {
	        return "(" + parentSubmittedId + ")";
	    }

	    final String sql =
	          "SELECT sub.zz_wsp_atr_submitted_id "
	        + "FROM ( "
	        + "    SELECT DISTINCT ON (s.zzsdforganisation_id) "
	        + "           s.zz_wsp_atr_submitted_id, "
	        + "           s.zzsdforganisation_id "
	        + "    FROM adempiere.zz_wsp_atr_sub_levy_orgs slo "
	        + "    JOIN adempiere.zz_wsp_atr_submitted s "
	        + "      ON s.zzsdforganisation_id = slo.zzsdforganisation_id "
	        + "    WHERE slo.zz_wsp_atr_submitted_id = ? "
	        + "      AND s.isactive = 'Y' "
	        + "      AND s.zz_docstatus IN ('"
	        + X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Submitted + "','"
	        + X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported + "','"
	        + X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Uploaded + "','"
	        + X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Query + "') "
	        + "    ORDER BY s.zzsdforganisation_id, "
	        + "             s.updated DESC, "
	        + "             s.zz_wsp_atr_submitted_id DESC "
	        + ") sub "
	        + "ORDER BY sub.zz_wsp_atr_submitted_id";

	    List<Integer> ids = new ArrayList<>();

	    try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
	        pstmt.setInt(1, parentSubmittedId);

	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                ids.add(rs.getInt(1));
	            }
	        }
	    } catch (Exception e) {
	        throw new RuntimeException(
	            "Failed to load parent/child submitted ids for " + parentSubmittedId + ": " + e.getMessage(), e);
	    }

	    if (onlySubLevyOrgs) {
	        if (ids.isEmpty()) {
	            return "(-1)";
	        }

	        return "(" + ids.stream()
	                .map(String::valueOf)
	                .collect(Collectors.joining(",")) + ")";
	    }

	    // consolidated, including parent
	    ids.add(0, parentSubmittedId);

	    return "(" + ids.stream()
	            .distinct()
	            .map(String::valueOf)
	            .collect(Collectors.joining(",")) + ")";
	}

    protected Integer getFiscalYearNumber(MZZWSPATRSubmitted submitted, String trxName) {
        String fiscalYear = DB.getSQLValueString(
                trxName,
                "SELECT FiscalYear FROM C_Year WHERE C_Year_ID=?",
                submitted.getZZ_FinYear_ID()
        );
        if (fiscalYear == null) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(fiscalYear.trim());
        if (!matcher.find()) {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
    }

    protected int resolveFiscalYearOrCurrent(MZZWSPATRSubmitted submitted, String trxName) {
        Integer fiscalYear = getFiscalYearNumber(submitted, trxName);
        return fiscalYear != null ? fiscalYear : Year.now().getValue();
    }

    protected int resolveAtrYearOrPrevious(MZZWSPATRSubmitted submitted, String trxName) {
        return resolveFiscalYearOrCurrent(submitted, trxName) - 1;
    }

}
