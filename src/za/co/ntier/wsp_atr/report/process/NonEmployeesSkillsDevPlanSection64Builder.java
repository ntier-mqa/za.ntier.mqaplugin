package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 6.4 - Skills Development Related Community Social Programme Planned in fiscal year
 * (Excluding Bursary, AET & HET)
 *
 * Input : ZZ_WSP_ATR_Non_Employees_Training (filtered by ZZ_WSP_ATR_Submitted_ID)
 * Output: ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep (linked to ZZ_WSP_ATR_Report_ID)
 *
 * Exclusions:
 * - Exclude programme types named Bursary and Adult_Education_and_Training.
 *
 * Counts:
 * - Use integer values (rs.getInt).
 */
public class NonEmployeesSkillsDevPlanSection64Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "6.4";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep";
    private static final String INPUT_TABLE  = "ZZ_WSP_ATR_Non_Employees_Training";
    private static final String BASE_NAME =
            "Skills Development Related Community Social Programme Planned in %s (Excluding Bursary, AET & HET)";
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");
    private String sectionName = String.format(BASE_NAME, "financial year");

    @Override
    public String getName() {
        return sectionName;
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted,boolean consolidatedSubmission,
    		boolean onlySubLevyOrgs, String trxName) throws Exception {
        sectionName = resolveSectionName(submitted, trxName);

        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        /*
         * We group by:
         * - planned programme type (ref ZZ_Learning_Programme_Ref)
         * - planned learning programme (details ref) => used for "Specify ..." in report
         * - planned target beneficiary
         *
         * Exclude Bursary/AET using programme type name from reference table.
         */
        final String sql =
              "WITH src AS ( \n"
            + "  SELECT \n"
            + "    t.zz_learning_programme_type_planned_id AS programme_type_id, \n"
            + "    t.zz_learning_programme_planned_id      AS programme_id, \n"
            + "    t.zz_target_ben_planned_id              AS target_ben_id, \n"
            + "    SUM(COALESCE(t.zz_total_planned,0))::int        AS total_planned, \n"
            + "    SUM(COALESCE(t.zz_disabled_planned,0))::int     AS disabled_planned \n"
            + "  FROM " + INPUT_TABLE + " t \n"
            + "  INNER JOIN ZZ_Learning_Programme_Ref lpt \n"
            + "          ON lpt.ZZ_Learning_Programme_Ref_ID = t.zz_learning_programme_type_planned_id \n"
            + "  WHERE t.zz_wsp_atr_submitted_id in " 
            + getParentAndChildSubmittedIdsInClause(report.getCtx(),submitted.getZZ_WSP_ATR_Submitted_ID(),consolidatedSubmission,onlySubLevyOrgs,trxName)
            + "    AND t.isactive = 'Y' \n"
            + "    AND t.zz_learning_programme_type_planned_id IS NOT NULL \n"
            + "    AND TRIM(COALESCE(lpt.name, '')) NOT IN ('Bursary', 'Adult_Education_and_Training') \n"
            + "  GROUP BY t.zz_learning_programme_type_planned_id, \n"
            + "           t.zz_learning_programme_planned_id, \n"
            + "           t.zz_target_ben_planned_id \n"
            + "), \n"
            + "numbered AS ( \n"
            + "  SELECT \n"
            + "    row_number() OVER (ORDER BY programme_type_id, programme_id, target_ben_id) AS row_no, \n"
            + "    * \n"
            + "  FROM src \n"
            + ") \n"
            + "SELECT \n"
            + "  row_no, programme_type_id, programme_id, target_ben_id, total_planned, disabled_planned \n"
            + "FROM numbered \n"
            + "ORDER BY row_no \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            //pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep row =
                            new X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Plan_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    // Planned fields
                    row.setZZ_Learning_Programme_Type_Planned_ID(rs.getInt("programme_type_id"));
                    row.setZZ_Learning_Programme_Planned_ID(rs.getInt("programme_id"));
                    row.setZZ_Target_Ben_Planned_ID(rs.getInt("target_ben_id"));

                    // Counts as ints
                    row.setZZ_Total_Planned(rs.getInt("total_planned"));
                    row.setZZ_Disabled_Planned(rs.getInt("disabled_planned"));

                    if (!row.save()) {
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE + ": ");
                    }

                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }

    private String resolveSectionName(MZZWSPATRSubmitted submitted, String trxName) {
        String fiscalYear = DB.getSQLValueStringEx(
                trxName,
                "SELECT FiscalYear FROM C_Year WHERE C_Year_ID=?",
                submitted.getZZ_FinYear_ID()
        );
        if (fiscalYear == null) {
            return String.format(BASE_NAME, "financial year");
        }

        Matcher matcher = YEAR_PATTERN.matcher(fiscalYear.trim());
        if (!matcher.find()) {
            return String.format(BASE_NAME, "financial year");
        }

        return String.format(BASE_NAME, matcher.group(1));
    }
}
