package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 6.3 - Skills Development Related Community Social Programmes Done in prior fiscal year
 * (Excluding Bursary, AET & HET)
 *
 * Output: ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep
 * Input : ZZ_WSP_ATR_Non_Employees_Training (filtered by ZZ_WSP_ATR_Submitted_ID)
 *
 * Report columns (as per screenshot):
 * - Qualification / Learning Programme Type   => ZZ_Learning_Programme_Type_Done_ID
 * - Specify (skills programme/learnership/NQF)=> ZZ_LP_Other_Done (text)
 * - Targeted Beneficiaries                    => ZZ_Target_Ben_Done_ID
 * - Gender Male/Female/Total                  => ZZ_Male, ZZ_Female, ZZ_Total_Done
 * - Race African/Coloured/Indian/White/Total  => ZZ_African, ZZ_Coloured, ZZ_Indian, ZZ_White, (race total derived in JRXML)
 * - Disabled Total                            => ZZ_Disabled_Done
 *
 * Notes:
 * - Totals use male+female (per your instruction).
 * - “Excluding Bursary, AET & HET” can be enforced by excluding programme type names (optional).
 */
public class NonEmployeesSkillsDevSection63Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "6.3";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep";
    private static final String INPUT_TABLE  = "ZZ_WSP_ATR_Non_Employees_Training";
    private static final String BASE_NAME =
            "Skills Development Related Community Social Programmes Done in %s (Excluding Bursary, AET & HET)";
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");
    private String sectionName = String.format(BASE_NAME, "previous financial year");

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

        final String sql =
              "WITH src AS ( \n"
            + "  SELECT \n"
            + "    t.zz_learning_programme_type_done_id AS programme_type_id, \n"
            + "    NULLIF(trim(COALESCE(t.zz_lp_other_done, '')), '') AS lp_other_done, \n"
            + "    t.zz_target_ben_done_id AS target_ben_id, \n"
            + "    SUM(COALESCE(t.zz_male,0))::numeric(10)      AS male_cnt, \n"
            + "    SUM(COALESCE(t.zz_female,0))::numeric(10)    AS female_cnt, \n"
            + "    SUM(COALESCE(t.zz_african,0))::numeric(10)   AS african_cnt, \n"
            + "    SUM(COALESCE(t.zz_coloured,0))::numeric(10)  AS coloured_cnt, \n"
            + "    SUM(COALESCE(t.zz_indian,0))::numeric(10)    AS indian_cnt, \n"
            + "    SUM(COALESCE(t.zz_white,0))::numeric(10)     AS white_cnt, \n"
            + "    SUM(COALESCE(t.zz_disabled_done,0))::numeric(10) AS disabled_cnt \n"
            + "  FROM " + INPUT_TABLE + " t \n"
            + "  INNER JOIN ZZ_Learning_Programme_Ref lpt \n"
            + "    ON lpt.ZZ_Learning_Programme_Ref_ID = t.zz_learning_programme_type_done_id \n"
            + "  WHERE t.zz_wsp_atr_submitted_id in " 
            + getParentAndChildSubmittedIdsInClause(report.getCtx(),submitted.getZZ_WSP_ATR_Submitted_ID(),consolidatedSubmission,onlySubLevyOrgs,trxName)
            + "    AND t.isactive = 'Y' \n"
            + "    AND t.zz_learning_programme_type_done_id IS NOT NULL \n"
            + "    AND TRIM(COALESCE(lpt.name, '')) NOT IN ('Bursary', 'Adult_Education_and_Training') \n"
            + "  GROUP BY t.zz_learning_programme_type_done_id, \n"
            + "           NULLIF(trim(COALESCE(t.zz_lp_other_done, '')), ''), \n"
            + "           t.zz_target_ben_done_id \n"
            + "), \n"
            + "numbered AS ( \n"
            + "  SELECT \n"
            + "    row_number() OVER (ORDER BY programme_type_id, lp_other_done, target_ben_id) AS row_no, \n"
            + "    * \n"
            + "  FROM src \n"
            + ") \n"
            + "SELECT \n"
            + "  row_no, programme_type_id, lp_other_done, target_ben_id, \n"
            + "  male_cnt, female_cnt, (male_cnt + female_cnt) AS total_done, \n"
            + "  african_cnt, coloured_cnt, indian_cnt, white_cnt, disabled_cnt \n"
            + "FROM numbered \n"
            + "ORDER BY row_no \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            //pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep row =
                            new X_ZZ_WSP_ATR_Non_Emp_Skills_Dev_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.setZZ_WSP_ATR_Submitted_ID(submitted.getZZ_WSP_ATR_Submitted_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    // If you want Row_No in output table later, add it; for now output table does not have Row_No.
                    row.setZZ_Learning_Programme_Type_Done_ID(rs.getInt("programme_type_id"));
                    row.setZZ_LP_Other_Done(rs.getString("lp_other_done"));
                    row.setZZ_Target_Ben_Done_ID(rs.getInt("target_ben_id"));

                    row.setZZ_Male(rs.getInt("male_cnt"));
                    row.setZZ_Female(rs.getInt("female_cnt"));
                    row.setZZ_Total_Done(rs.getInt("total_done"));

                    row.setZZ_African(rs.getInt("african_cnt"));
                    row.setZZ_Coloured(rs.getInt("coloured_cnt"));
                    row.setZZ_Indian(rs.getInt("indian_cnt"));
                    row.setZZ_White(rs.getInt("white_cnt"));

                    row.setZZ_Disabled_Done(rs.getInt("disabled_cnt"));

                    // Optional: if you want to carry Non-Employee status too (table has ZZ_Non_Emp_Status_Done_ID)
                    // row.setZZ_Non_Emp_Status_Done_ID(...)

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
            return String.format(BASE_NAME, "previous financial year");
        }

        Matcher matcher = YEAR_PATTERN.matcher(fiscalYear.trim());
        if (!matcher.find()) {
            return String.format(BASE_NAME, "previous financial year");
        }

        int atrYear = Integer.parseInt(matcher.group(1)) - 1;
        return String.format(BASE_NAME, atrYear);
    }
}
