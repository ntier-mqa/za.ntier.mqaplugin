package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Summ_Planned_Emp_Training_Inter_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_WSP;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 4.3.1 - Summary of Planned Employee Training Interventions (Occupational Group)
 *
 * Input:  ZZ_WSP_ATR_WSP (filtered by ZZ_WSP_ATR_Submitted_ID)
 * Output: ZZ_WSP_ATR_Summ_Planned_Emp_Training_Inter_Rep (linked to ZZ_WSP_ATR_Report_ID)
 *
 * Notes:
 * - This builder aggregates counts already captured in ZZ_WSP_ATR_WSP (Male/Female + Race + Disabled).
 * - ZZ_Report_Section is set to "4.3.1".
 * - Row_No is generated via row_number() for consistent ordering.
 */
public class SummPlannedEmpTrainingInterventionsSection431Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "4.3.1";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Summ_Planned_Emp_Training_Inter_Rep";

    @Override
    public String getName() {
        return "Summary of Planned Employee Training Interventions (Occupational Group)";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        // rerun behaviour: delete rows for this report + this section
        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        /*
         * Aggregate by occupational group + programme type + programme + qualification text.
         * If later you need to exclude Refresher/Induction/Ex-leave, add predicates in the "src" WHERE clause below
         * once you confirm which input column indicates that category.
         */
        final String sql =
                "WITH src AS ( \n"
              + "  SELECT \n"
              + "    w.ZZ_OFO_Specialisation_ID, \n"
              + "    w.ZZ_Learning_Programme_Type_ID, \n"
              + "    w.ZZ_Learning_Programme_ID, \n"
              + "    w.ZZ_Qualification, \n"
              + "    COALESCE(w.ZZ_Male,0)     AS male_cnt, \n"
              + "    COALESCE(w.ZZ_Female,0)   AS female_cnt, \n"
              + "    COALESCE(w.ZZ_African,0)  AS african_cnt, \n"
              + "    COALESCE(w.ZZ_Coloured,0) AS coloured_cnt, \n"
              + "    COALESCE(w.ZZ_Indian,0)   AS indian_cnt, \n"
              + "    COALESCE(w.ZZ_White,0)    AS white_cnt, \n"
              + "    COALESCE(w.ZZ_Disabled,0) AS disabled_cnt \n"
              + "  FROM " + X_ZZ_WSP_ATR_WSP.Table_Name + " w \n"
              + "  WHERE w.ZZ_WSP_ATR_Submitted_ID = ? \n"
              + "), agg AS ( \n"
              + "  SELECT \n"
              + "    ZZ_OFO_Specialisation_ID, \n"
              + "    ZZ_Learning_Programme_Type_ID, \n"
              + "    ZZ_Learning_Programme_ID, \n"
              + "    ZZ_Qualification, \n"
              + "    SUM(male_cnt)     AS male_cnt, \n"
              + "    SUM(female_cnt)   AS female_cnt, \n"
              + "    SUM(african_cnt)  AS african_cnt, \n"
              + "    SUM(coloured_cnt) AS coloured_cnt, \n"
              + "    SUM(indian_cnt)   AS indian_cnt, \n"
              + "    SUM(white_cnt)    AS white_cnt, \n"
              + "    SUM(disabled_cnt) AS disabled_cnt \n"
              + "  FROM src \n"
              + "  GROUP BY ZZ_OFO_Specialisation_ID, ZZ_Learning_Programme_Type_ID, ZZ_Learning_Programme_ID, ZZ_Qualification \n"
              + "), numbered AS ( \n"
              + "  SELECT \n"
              + "    row_number() OVER ( \n"
              + "      ORDER BY ZZ_OFO_Specialisation_ID NULLS LAST, \n"
              + "               ZZ_Learning_Programme_Type_ID NULLS LAST, \n"
              + "               ZZ_Learning_Programme_ID NULLS LAST, \n"
              + "               ZZ_Qualification NULLS LAST \n"
              + "    ) AS row_no, \n"
              + "    * \n"
              + "  FROM agg \n"
              + ") \n"
              + "SELECT * FROM numbered ORDER BY row_no \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Summ_Planned_Emp_Training_Inter_Rep row =
                            new X_ZZ_WSP_ATR_Summ_Planned_Emp_Training_Inter_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.setZZ_WSP_ATR_Submitted_ID(submitted.getZZ_WSP_ATR_Submitted_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("row_no"));

                    // group keys
                    row.setZZ_OFO_Specialisation_ID(rs.getInt("ZZ_OFO_Specialisation_ID"));
                    row.setZZ_Learning_Programme_Type_ID(rs.getInt("ZZ_Learning_Programme_Type_ID"));
                    row.setZZ_Learning_Programme_ID(rs.getInt("ZZ_Learning_Programme_ID"));

                    // both exist in your output table; keep them aligned
                    String qual = rs.getString("ZZ_Qualification");
                    row.setZZ_Qualification(qual);
                    row.setQualification(qual);

                    // counts
                    row.setZZ_Male(rs.getInt("male_cnt"));
                    row.setZZ_Female(rs.getInt("female_cnt"));

                    row.setZZ_African(rs.getInt("african_cnt"));
                    row.setZZ_Coloured(rs.getInt("coloured_cnt"));
                    row.setZZ_Indian(rs.getInt("indian_cnt"));
                    row.setZZ_White(rs.getInt("white_cnt"));

                    row.setZZ_Disabled(rs.getInt("disabled_cnt"));

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