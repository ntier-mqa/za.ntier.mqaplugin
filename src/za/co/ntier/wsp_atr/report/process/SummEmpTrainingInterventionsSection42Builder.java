package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_ATR_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Biodata_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 4.2 - Summary of employee training interventions for the period 2024
 *
 * In this section individual training interventions are counted against the number
 * of beneficiaries in each of them.
 *
 * Input:  ZZ_WSP_ATR_ATR_Detail (filtered by ZZ_WSP_ATR_Submitted_ID)
 * Link :  ZZ_WSP_ATR_Biodata_Detail via ZZ_WSP_Employees_ID (same submitted id)
 * Output: ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep (linked to ZZ_WSP_ATR_Report_ID)
 *
 * Notes:
 * - No zz_wsp_employees join (per instruction).
 * - Counts are DISTINCT employees per (Qualification_Type_ID, Learning_Programme_Detail_ID).
 * - Race/Gender/Disabled/SA Citizen derived via reference table Name (ILIKE checks),
 *   same approach as section 2.2.
 * - Age groups computed from Birth_Year_TRUE; report year fixed to 2024 (per section heading).
 * - Totals/Grand Totals are handled in JRXML (this builder inserts detail rows only).
 */
public class SummEmpTrainingInterventionsSection42Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "4.2";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep";
    private static final int REPORT_YEAR = 2024;

    @Override
    public String getName() {
        return "Summary of employee training interventions";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        // rerun behaviour: delete all rows for this report+section
        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        /*
         * Build one row per (qualification_type_id, learning_programme_detail_id).
         * Count DISTINCT employees in each intervention (dedup by zz_wsp_employees_id).
         *
         * Demographics come from Biodata:
         * - Race_ID -> ZZ_Equity_Ref.Name
         * - Gender_ID -> ZZ_Gender_Ref.Name
         * - Disabled_ID / SA_Citizen_ID -> ZZ_No_Yes_Ref.Name
         * - Birth_Year_TRUE -> age bands based on REPORT_YEAR (2024)
         */
        final String sql =
                "WITH base AS ( \n"
              + "  SELECT \n"
              + "    d.Qualification_Type_ID        AS qualification_type_id, \n"
              + "    d.Learning_Programme_Detail_ID  AS learning_programme_detail_id, \n"
              + "    d.ZZ_WSP_Employees_ID           AS zz_wsp_employees_id \n"
              + "  FROM " + X_ZZ_WSP_ATR_ATR_Detail.Table_Name + " d \n"
              + "  WHERE d.IsActive='Y' \n"
              + "    AND d.ZZ_WSP_ATR_Submitted_ID = ? \n"
              + "    AND d.ZZ_WSP_Employees_ID IS NOT NULL \n"
              + "    AND d.Qualification_Type_ID IS NOT NULL \n"
              + "    AND d.Learning_Programme_Detail_ID IS NOT NULL \n"
              + "), dedup AS ( \n"
              + "  SELECT DISTINCT qualification_type_id, learning_programme_detail_id, zz_wsp_employees_id \n"
              + "  FROM base \n"
              + "), demo AS ( \n"
              + "  SELECT \n"
              + "    dd.qualification_type_id, \n"
              + "    dd.learning_programme_detail_id, \n"
              + "    dd.zz_wsp_employees_id, \n"
              + "    COALESCE(eq.Name,  '') AS race_txt, \n"
              + "    COALESCE(g.Name,   '') AS gender_txt, \n"
              + "    COALESCE(dis.Name, '') AS disabled_txt, \n"
              + "    COALESCE(sa.Name,  '') AS sa_txt, \n"
              + "    CASE \n"
              + "      WHEN NULLIF(regexp_replace(COALESCE(bd.Birth_Year_TRUE,''), '[^0-9]', '', 'g'), '') IS NULL THEN NULL \n"
              + "      ELSE (" + REPORT_YEAR + " - NULLIF(regexp_replace(bd.Birth_Year_TRUE, '[^0-9]', '', 'g'), '')::int) \n"
              + "    END AS age_years \n"
              + "  FROM dedup dd \n"
              + "  LEFT JOIN " + X_ZZ_WSP_ATR_Biodata_Detail.Table_Name + " bd \n"
              + "         ON bd.ZZ_WSP_Employees_ID = dd.zz_wsp_employees_id \n"
              + "        AND bd.ZZ_WSP_ATR_Submitted_ID = ? \n"
              + "        AND bd.IsActive='Y' \n"
              + "  LEFT JOIN ZZ_Equity_Ref  eq   ON eq.ZZ_Equity_Ref_ID  = bd.Race_ID \n"
              + "  LEFT JOIN ZZ_Gender_Ref  g    ON g.ZZ_Gender_Ref_ID   = bd.Gender_ID \n"
              + "  LEFT JOIN ZZ_No_Yes_Ref  dis  ON dis.ZZ_No_Yes_Ref_ID = bd.Disabled_ID \n"
              + "  LEFT JOIN ZZ_No_Yes_Ref  sa   ON sa.ZZ_No_Yes_Ref_ID  = bd.SA_Citizen_ID \n"
              + ") \n"
              + "SELECT \n"
              + "  d.qualification_type_id, \n"
              + "  d.learning_programme_detail_id, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%afric%'  AND d.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS african_male_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%afric%'  AND d.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS african_female_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%colour%' AND d.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS coloured_male_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%colour%' AND d.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS coloured_female_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%indian%' AND d.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS indian_male_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%indian%' AND d.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS indian_female_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%white%'  AND d.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS white_male_cnt, \n"
              + "  SUM(CASE WHEN d.race_txt ILIKE '%white%'  AND d.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS white_female_cnt, \n"
              + "  SUM(CASE WHEN d.sa_txt ILIKE '%no%' THEN 1 ELSE 0 END) AS nonsa_cnt, \n"
              + "  SUM(CASE WHEN d.disabled_txt ILIKE '%yes%' THEN 1 ELSE 0 END) AS disabled_cnt, \n"
              + "  SUM(CASE WHEN d.age_years IS NOT NULL AND d.age_years < 35 THEN 1 ELSE 0 END) AS age_u35_cnt, \n"
              + "  SUM(CASE WHEN d.age_years IS NOT NULL AND d.age_years BETWEEN 35 AND 55 THEN 1 ELSE 0 END) AS age_35_55_cnt, \n"
              + "  SUM(CASE WHEN d.age_years IS NOT NULL AND d.age_years > 55 THEN 1 ELSE 0 END) AS age_o55_cnt, \n"
              + "  SUM(CASE WHEN d.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS total_male_cnt, \n"
              + "  SUM(CASE WHEN d.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS total_female_cnt \n"
              + "FROM demo d \n"
              + "GROUP BY d.qualification_type_id, d.learning_programme_detail_id \n"
              + "ORDER BY d.qualification_type_id, d.learning_programme_detail_id \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());
            pstmt.setInt(2, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep row =
                            new X_ZZ_WSP_ATR_Summ_Of_Emp_Training_Inter_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setQualification_Type_ID(rs.getInt("qualification_type_id"));
                    row.setLearning_Programme_Detail_ID(rs.getInt("learning_programme_detail_id"));

                    row.setZZ_African_Male_Cnt(rs.getInt("african_male_cnt"));
                    row.setZZ_African_Female_Cnt(rs.getInt("african_female_cnt"));
                    row.setZZ_Coloured_Male_Cnt(rs.getInt("coloured_male_cnt"));
                    row.setZZ_Coloured_Female_Cnt(rs.getInt("coloured_female_cnt"));
                    row.setZZ_Indian_Male_Cnt(rs.getInt("indian_male_cnt"));
                    row.setZZ_Indian_Female_Cnt(rs.getInt("indian_female_cnt"));
                    row.setZZ_White_Male_Cnt(rs.getInt("white_male_cnt"));
                    row.setZZ_White_Female_Cnt(rs.getInt("white_female_cnt"));

                    row.setZZ_Total_Male_Cnt(rs.getInt("total_male_cnt"));
                    row.setZZ_Total_Female_Cnt(rs.getInt("total_female_cnt"));

                    row.setZZ_Disabled_Cnt(rs.getInt("disabled_cnt"));
                    row.setZZ_NonSA_Cnt(rs.getInt("nonsa_cnt"));

                    row.setZZ_Age_U35_Cnt(rs.getInt("age_u35_cnt"));
                    row.setZZ_Age_35_55_Cnt(rs.getInt("age_35_55_cnt"));
                    row.setZZ_Age_O55_Cnt(rs.getInt("age_o55_cnt"));

                    if (!row.save()) {
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE + ": "); // + row.getProcessMsg());
                    }

                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }
}