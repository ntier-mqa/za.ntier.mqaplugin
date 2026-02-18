package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Empl_Trained_Planned_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

public class EmployeesTrainedSummarySection41Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "4.1";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Empl_Trained_Planned_Rep";

    @Override
    public String getName() {
        return "Summary of employee beneficiaries of training (unique employees)";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        /*
         * Notes:
         * - DISTINCT employees comes from ZZ_WSP_ATR_ATR_Detail for this submitted file.
         * - We use ZZ_WSP_Employees_ID first, and fall back to Employee_Number_ID if needed.
         * - Major group derived exactly as you specified:
         *   biodata.OFO_Occupation_Code_ID -> ZZ_Occupations_Ref.Value like '2021-111101'
         *   -> take substring(1,6) = '2021-1'
         *   -> lookup ZZ_WSP_ATR_OFO_Major_Group.Value = '2021-1'
         * - Age buckets from Birth_Year_TRUE: 2024 - birthYear
         */

        final String sql =
            "WITH emp AS ( \n" +
            "  SELECT DISTINCT ON (COALESCE(a.zz_wsp_employees_id, a.employee_number_id)) \n" +
            "    COALESCE(a.zz_wsp_employees_id, a.employee_number_id) AS emp_id \n" +
            "  FROM zz_wsp_atr_atr_detail a \n" +
            "  WHERE a.zz_wsp_atr_submitted_id = ? \n" +
            "    AND a.isactive = 'Y' \n" +
            "    AND COALESCE(a.zz_wsp_employees_id, a.employee_number_id) IS NOT NULL \n" +
            "  ORDER BY COALESCE(a.zz_wsp_employees_id, a.employee_number_id), a.row_no NULLS LAST, a.zz_wsp_atr_atr_detail_id \n" +
            "), bio AS ( \n" +
            "  SELECT \n" +
            "    e.emp_id, \n" +
            "    b.gender_id, \n" +
            "    b.race_id, \n" +
            "    b.disabled_id, \n" +
            "    b.sa_citizen_id, \n" +
            "    b.birth_year_true, \n" +
            "    b.ofo_occupation_code_id \n" +
            "  FROM emp e \n" +
            "  JOIN zz_wsp_atr_biodata_detail b \n" +
            "    ON b.zz_wsp_employees_id = e.emp_id \n" +
            "   AND b.zz_wsp_atr_submitted_id = ? \n" +
            "), occ AS ( \n" +
            "  SELECT \n" +
            "    bio.*, \n" +
            "    o.value AS ofo_value \n" +
            "  FROM bio \n" +
            "  LEFT JOIN zz_occupations_ref o \n" +
            "    ON o.zz_occupations_ref_id = bio.ofo_occupation_code_id \n" +
            "), mg AS ( \n" +
            "  SELECT \n" +
            "    occ.*, \n" +
            "    mgref.ZZ_WSP_ATR_OFO_Major_Group_Ref_ID AS major_group_id \n" +
            "  FROM occ \n" +
            "  LEFT JOIN ZZ_WSP_ATR_OFO_Major_Group_Ref mgref \n" +
            "    ON mgref.value = substring(occ.ofo_value from 1 for 6) \n" +
            ") \n" +
            "SELECT \n" +
            "  mg.major_group_id, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'African%'  AND ge.name ILIKE 'Male%'   THEN 1 ELSE 0 END) AS african_male, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'African%'  AND ge.name ILIKE 'Female%' THEN 1 ELSE 0 END) AS african_female, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'Coloured%' AND ge.name ILIKE 'Male%'   THEN 1 ELSE 0 END) AS coloured_male, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'Coloured%' AND ge.name ILIKE 'Female%' THEN 1 ELSE 0 END) AS coloured_female, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'Indian%'   AND ge.name ILIKE 'Male%'   THEN 1 ELSE 0 END) AS indian_male, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'Indian%'   AND ge.name ILIKE 'Female%' THEN 1 ELSE 0 END) AS indian_female, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'White%'    AND ge.name ILIKE 'Male%'   THEN 1 ELSE 0 END) AS white_male, \n" +
            "  SUM(CASE WHEN eq.name ILIKE 'White%'    AND ge.name ILIKE 'Female%' THEN 1 ELSE 0 END) AS white_female, \n" +
            "  SUM(CASE WHEN ge.name ILIKE 'Male%'   THEN 1 ELSE 0 END) AS total_male, \n" +
            "  SUM(CASE WHEN ge.name ILIKE 'Female%' THEN 1 ELSE 0 END) AS total_female, \n" +
            "  SUM(CASE WHEN dis.value = 'Y' THEN 1 ELSE 0 END) AS disabled_cnt, \n" +
            "  SUM(CASE WHEN sa.value = 'N' THEN 1 ELSE 0 END) AS nonsa_cnt, \n" +
            "  SUM(CASE \n" +
            "        WHEN mg.birth_year_true ~ '^[0-9]{4}$' \n" +
            "         AND (2024 - mg.birth_year_true::int) < 35 THEN 1 ELSE 0 END) AS age_u35, \n" +
            "  SUM(CASE \n" +
            "        WHEN mg.birth_year_true ~ '^[0-9]{4}$' \n" +
            "         AND (2024 - mg.birth_year_true::int) BETWEEN 35 AND 55 THEN 1 ELSE 0 END) AS age_35_55, \n" +
            "  SUM(CASE \n" +
            "        WHEN mg.birth_year_true ~ '^[0-9]{4}$' \n" +
            "         AND (2024 - mg.birth_year_true::int) > 55 THEN 1 ELSE 0 END) AS age_o55 \n" +
            "FROM mg \n" +
            "LEFT JOIN zz_equity_ref   eq  ON eq.zz_equity_ref_id   = mg.race_id \n" +
            "LEFT JOIN zz_gender_ref   ge  ON ge.zz_gender_ref_id   = mg.gender_id \n" +
            "LEFT JOIN zz_no_yes_ref   dis ON dis.zz_no_yes_ref_id   = mg.disabled_id \n" +
            "LEFT JOIN zz_no_yes_ref   sa  ON sa.zz_no_yes_ref_id    = mg.sa_citizen_id \n" +
            "WHERE mg.major_group_id IS NOT NULL \n" +
            "GROUP BY mg.major_group_id \n" +
            "ORDER BY mg.major_group_id \n";

        int inserted = 0;
        int rowNo = 0;

        try (PreparedStatement ps = DB.prepareStatement(sql, trxName)) {
            ps.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());
            ps.setInt(2, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rowNo++;

                    X_ZZ_WSP_ATR_Empl_Trained_Planned_Rep rec =
                        new X_ZZ_WSP_ATR_Empl_Trained_Planned_Rep(report.getCtx(), 0, trxName);

                    rec.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    rec.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    // if your table has Row_No, keep it ordered
                    if (rec.get_ColumnIndex("Row_No") >= 0) {
                        rec.set_ValueOfColumn("Row_No", rowNo);
                    }

                    rec.setZZ_WSP_ATR_OFO_Major_Group_Ref_ID(rs.getInt("major_group_id"));

                    rec.setZZ_African_Male_Cnt(rs.getInt("african_male"));
                    rec.setZZ_African_Female_Cnt(rs.getInt("african_female"));
                    rec.setZZ_Coloured_Male_Cnt(rs.getInt("coloured_male"));
                    rec.setZZ_Coloured_Female_Cnt(rs.getInt("coloured_female"));
                    rec.setZZ_Indian_Male_Cnt(rs.getInt("indian_male"));
                    rec.setZZ_Indian_Female_Cnt(rs.getInt("indian_female"));
                    rec.setZZ_White_Male_Cnt(rs.getInt("white_male"));
                    rec.setZZ_White_Female_Cnt(rs.getInt("white_female"));

                    rec.setZZ_Total_Male_Cnt(rs.getInt("total_male"));
                    rec.setZZ_Total_Female_Cnt(rs.getInt("total_female"));

                    rec.setZZ_Disabled_Cnt(rs.getInt("disabled_cnt"));
                    rec.setZZ_NonSA_Cnt(rs.getInt("nonsa_cnt"));

                    rec.setZZ_Age_U35_Cnt(rs.getInt("age_u35"));
                    rec.setZZ_Age_35_55_Cnt(rs.getInt("age_35_55"));
                    rec.setZZ_Age_O55_Cnt(rs.getInt("age_o55"));

                    if (!rec.save()) {
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE);
                    }
                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }
}