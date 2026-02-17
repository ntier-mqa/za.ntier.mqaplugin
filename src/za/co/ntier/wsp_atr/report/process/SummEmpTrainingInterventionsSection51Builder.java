package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Summ_Emp_Train_Inter_Rep;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 5.1 - Summary of employee training interventions for the period
 *
 * Input:
 *  - ZZ_WSP_ATR_ATR_Detail (per submitted)
 *  - ZZ_WSP_ATR_Biodata_Detail (join via ZZ_WSP_Employees_ID + Submitted_ID)
 *
 * Output:
 *  - ZZ_WSP_ATR_Summ_Emp_Train_Inter_Rep (grouped by Qualification_Type_ID + Learning_Programme_Detail_ID)
 *
 * Counting rule:
 *  - Count each employee once per Qualification_Type_ID + Learning_Programme_Detail_ID
 *    (de-dup via DISTINCT in SQL CTE)
 */
public class SummEmpTrainingInterventionsSection51Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "5.1";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Summ_Emp_Train_Inter_Rep";

    @Override
    public String getName() {
        return "Summary of employee training interventions for the period";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        final String sql =
              "WITH base AS ( \n"
            + "  SELECT DISTINCT \n"
            + "      d.qualification_type_id, \n"
            + "      d.learning_programme_detail_id, \n"
            + "      d.zz_wsp_employees_id, \n"
            + "      bd.race_id, bd.gender_id, bd.disabled_id, bd.sa_citizen_id, bd.birth_year_true \n"
            + "  FROM zz_wsp_atr_atr_detail d \n"
            + "  JOIN zz_wsp_atr_biodata_detail bd \n"
            + "    ON bd.zz_wsp_employees_id = d.zz_wsp_employees_id \n"
            + "   AND bd.zz_wsp_atr_submitted_id = d.zz_wsp_atr_submitted_id \n"
            + "  WHERE d.zz_wsp_atr_submitted_id = ? \n"
            + "    AND d.zz_wsp_employees_id IS NOT NULL \n"
            + "), txt AS ( \n"
            + "  SELECT \n"
            + "      b.qualification_type_id, \n"
            + "      b.learning_programme_detail_id, \n"
            + "      COALESCE(eq.name,'') AS race_txt, \n"
            + "      COALESCE(g.name,'')  AS gender_txt, \n"
            + "      COALESCE(ny_dis.name,'') AS disabled_txt, \n"
            + "      COALESCE(ny_sa.name,'')  AS sa_txt, \n"
            + "      CASE \n"
            + "        WHEN NULLIF(regexp_replace(COALESCE(b.birth_year_true,''), '[^0-9]', '', 'g'), '') IS NULL THEN NULL \n"
            + "        ELSE (extract(year from current_date)::int \n"
            + "             - NULLIF(regexp_replace(b.birth_year_true, '[^0-9]', '', 'g'), '')::int) \n"
            + "      END AS age_years \n"
            + "  FROM base b \n"
            + "  LEFT JOIN zz_equity_ref  eq     ON eq.zz_equity_ref_id  = b.race_id \n"
            + "  LEFT JOIN zz_gender_ref  g      ON g.zz_gender_ref_id   = b.gender_id \n"
            + "  LEFT JOIN zz_no_yes_ref  ny_dis ON ny_dis.zz_no_yes_ref_id = b.disabled_id \n"
            + "  LEFT JOIN zz_no_yes_ref  ny_sa  ON ny_sa.zz_no_yes_ref_id  = b.sa_citizen_id \n"
            + ") \n"
            + "SELECT \n"
            + "  qualification_type_id, \n"
            + "  learning_programme_detail_id, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%afric%'  AND gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS african_male_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%afric%'  AND gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS african_female_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%colour%' AND gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS coloured_male_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%colour%' AND gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS coloured_female_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%indian%' AND gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS indian_male_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%indian%' AND gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS indian_female_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%white%'  AND gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS white_male_cnt, \n"
            + "  SUM(CASE WHEN race_txt ILIKE '%white%'  AND gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS white_female_cnt, \n"
            + "  SUM(CASE WHEN gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS total_male_cnt, \n"
            + "  SUM(CASE WHEN gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS total_female_cnt, \n"
            + "  SUM(CASE WHEN disabled_txt ILIKE '%yes%' THEN 1 ELSE 0 END) AS disabled_cnt, \n"
            + "  SUM(CASE WHEN sa_txt ILIKE '%no%' THEN 1 ELSE 0 END) AS nonsa_cnt, \n"
            + "  SUM(CASE WHEN age_years IS NOT NULL AND age_years < 35 THEN 1 ELSE 0 END) AS age_u35_cnt, \n"
            + "  SUM(CASE WHEN age_years IS NOT NULL AND age_years BETWEEN 35 AND 55 THEN 1 ELSE 0 END) AS age_35_55_cnt, \n"
            + "  SUM(CASE WHEN age_years IS NOT NULL AND age_years > 55 THEN 1 ELSE 0 END) AS age_o55_cnt \n"
            + "FROM txt \n"
            + "GROUP BY qualification_type_id, learning_programme_detail_id \n"
            + "ORDER BY qualification_type_id, learning_programme_detail_id \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Summ_Emp_Train_Inter_Rep row =
                            new X_ZZ_WSP_ATR_Summ_Emp_Train_Inter_Rep(report.getCtx(), 0, trxName);

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
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE + ": ");
                    }

                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }
}