package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Summ_Ed_Level_Rep;
import za.ntier.models.MZZWSPATRSubmitted;

public class WorkforceEducationLevelSection24Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "2.4";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Summ_Ed_Level_Rep";

    @Override
    public String getName() {
        return "Summary of Education Level of the Workforce";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        // rerun-safe: delete by report only (as you confirmed)
        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        final String sql =
                "WITH bd2 AS ( \n"
              + "  SELECT \n"
              + "    bd.Highest_Qualification_Type_ID, \n"
              + "    COALESCE(eq.Name,  '') AS race_txt, \n"
              + "    COALESCE(g.Name,   '') AS gender_txt, \n"
              + "    COALESCE(dis.Name, '') AS disabled_txt, \n"
              + "    COALESCE(sa.Name,  '') AS sa_txt, \n"
              + "    CASE \n"
              + "      WHEN NULLIF(regexp_replace(COALESCE(bd.Birth_Year_TRUE,''), '[^0-9]', '', 'g'), '') IS NULL THEN NULL \n"
              + "      ELSE (extract(year from current_date)::int \n"
              + "            - NULLIF(regexp_replace(bd.Birth_Year_TRUE, '[^0-9]', '', 'g'), '')::int) \n"
              + "    END AS age_years \n"
              + "  FROM ZZ_WSP_ATR_Biodata_Detail bd \n"
              + "  LEFT JOIN ZZ_Equity_Ref  eq   ON eq.ZZ_Equity_Ref_ID  = bd.Race_ID \n"
              + "  LEFT JOIN ZZ_Gender_Ref  g    ON g.ZZ_Gender_Ref_ID   = bd.Gender_ID \n"
              + "  LEFT JOIN ZZ_No_Yes_Ref  dis  ON dis.ZZ_No_Yes_Ref_ID = bd.Disabled_ID \n"
              + "  LEFT JOIN ZZ_No_Yes_Ref  sa   ON sa.ZZ_No_Yes_Ref_ID  = bd.SA_Citizen_ID \n"
              + "  WHERE bd.ZZ_WSP_ATR_Submitted_ID = ? \n"
              + ") \n"
              + "SELECT \n"
              + "  bd2.Highest_Qualification_Type_ID AS highest_qualification_type_id, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%afric%' AND bd2.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS african_male_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%afric%' AND bd2.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS african_female_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%colour%' AND bd2.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS coloured_male_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%colour%' AND bd2.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS coloured_female_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%indian%' AND bd2.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS indian_male_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%indian%' AND bd2.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS indian_female_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%white%'  AND bd2.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS white_male_cnt, \n"
              + "  SUM(CASE WHEN bd2.race_txt ILIKE '%white%'  AND bd2.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS white_female_cnt, \n"
              + "  SUM(CASE WHEN bd2.gender_txt ILIKE '%male%'   THEN 1 ELSE 0 END) AS total_male_cnt, \n"
              + "  SUM(CASE WHEN bd2.gender_txt ILIKE '%female%' THEN 1 ELSE 0 END) AS total_female_cnt, \n"
              + "  SUM(CASE WHEN bd2.disabled_txt ILIKE '%yes%' THEN 1 ELSE 0 END) AS disabled_cnt, \n"
              + "  SUM(CASE WHEN bd2.sa_txt ILIKE '%no%' THEN 1 ELSE 0 END) AS nonsa_cnt, \n"
              + "  SUM(CASE WHEN bd2.age_years IS NOT NULL AND bd2.age_years < 35 THEN 1 ELSE 0 END) AS age_u35_cnt, \n"
              + "  SUM(CASE WHEN bd2.age_years IS NOT NULL AND bd2.age_years BETWEEN 35 AND 55 THEN 1 ELSE 0 END) AS age_35_55_cnt, \n"
              + "  SUM(CASE WHEN bd2.age_years IS NOT NULL AND bd2.age_years > 55 THEN 1 ELSE 0 END) AS age_o55_cnt \n"
              + "FROM bd2 \n"
              + "WHERE bd2.Highest_Qualification_Type_ID IS NOT NULL \n"
              + "GROUP BY bd2.Highest_Qualification_Type_ID \n"
              + "ORDER BY bd2.Highest_Qualification_Type_ID \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Summ_Ed_Level_Rep row =
                            new X_ZZ_WSP_ATR_Summ_Ed_Level_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    // column name is ZZ_Report_Section (your fix)
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setHighest_Qualification_Type_ID(rs.getInt("highest_qualification_type_id"));

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
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE);
                    }

                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }
}