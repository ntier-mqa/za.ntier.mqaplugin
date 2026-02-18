package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Non_Emp_Burs_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 6.2 - Bursaries (Including HET)
 *
 * Output: ZZ_WSP_ATR_Non_Emp_Burs_Rep
 * Input : ZZ_WSP_ATR_Non_Employees_Training (filtered by ZZ_WSP_ATR_Submitted_ID)
 *
 * Columns:
 * - Name of Programme           => ZZ_Learning_Programme_Done_ID (ref: ZZ_Qualification_Type_Details_Ref)
 * - Non-Employee Status         => ZZ_Non_Emp_Status_Done_ID     (ref: ZZ_WSP_Non_Employee_Status_Ref)
 * - Total Trained 2024          => ZZ_Total_Done   (male + female)
 * - Total to be Trained/Planned 2025 => ZZ_Total_Planned (zz_total_planned)
 *
 * Notes:
 * - FULL JOIN done vs planned aggregates to keep planned-only rows.
 * - ZZ_Report_Section set to "6.2".
 * - Re-run behavior: delete existing rows for report+section.
 */
public class NonEmployeesBursariesSection62Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "6.2";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Non_Emp_Burs_Rep";
    private static final String INPUT_TABLE  = "ZZ_WSP_ATR_Non_Employees_Training";

    @Override
    public String getName() {
        return "Bursaries (Including HET)";
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
              "WITH done_agg AS ( \n"
            + "  SELECT \n"
            + "    t.zz_learning_programme_done_id   AS programme_id, \n"
            + "    t.zz_non_emp_status_done_id       AS status_id, \n"
            + "    SUM(COALESCE(t.zz_male,0) + COALESCE(t.zz_female,0))::numeric(10) AS total_done \n"
            + "  FROM " + INPUT_TABLE + " t \n"
            + "  WHERE t.zz_wsp_atr_submitted_id = ? \n"
            + "    AND t.isactive = 'Y' \n"
            + "    AND t.zz_learning_programme_done_id IS NOT NULL \n"
            + "    AND t.zz_non_emp_status_done_id IS NOT NULL \n"
            + "  GROUP BY t.zz_learning_programme_done_id, t.zz_non_emp_status_done_id \n"
            + "), \n"
            + "planned_agg AS ( \n"
            + "  SELECT \n"
            + "    t.zz_learning_programme_planned_id AS programme_id, \n"
            + "    t.zz_non_emp_status_planned_id     AS status_id, \n"
            + "    SUM(COALESCE(t.zz_total_planned,0))::numeric(10) AS total_planned \n"
            + "  FROM " + INPUT_TABLE + " t \n"
            + "  WHERE t.zz_wsp_atr_submitted_id = ? \n"
            + "    AND t.isactive = 'Y' \n"
            + "    AND t.zz_learning_programme_planned_id IS NOT NULL \n"
            + "    AND t.zz_non_emp_status_planned_id IS NOT NULL \n"
            + "  GROUP BY t.zz_learning_programme_planned_id, t.zz_non_emp_status_planned_id \n"
            + "), \n"
            + "merged AS ( \n"
            + "  SELECT \n"
            + "    COALESCE(d.programme_id, p.programme_id) AS programme_id, \n"
            + "    COALESCE(d.status_id,    p.status_id)    AS status_id, \n"
            + "    COALESCE(d.total_done, 0)                AS total_done, \n"
            + "    COALESCE(p.total_planned, 0)             AS total_planned \n"
            + "  FROM done_agg d \n"
            + "  FULL JOIN planned_agg p \n"
            + "    ON p.programme_id = d.programme_id \n"
            + "   AND p.status_id    = d.status_id \n"
            + ") \n"
            + "SELECT \n"
            + "  row_number() OVER (ORDER BY programme_id, status_id) AS row_no, \n"
            + "  programme_id, \n"
            + "  status_id, \n"
            + "  total_done, \n"
            + "  total_planned \n"
            + "FROM merged \n"
            + "WHERE programme_id IS NOT NULL \n"
            + "  AND status_id IS NOT NULL \n"
            + "ORDER BY row_no \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());
            pstmt.setInt(2, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Non_Emp_Burs_Rep row =
                            new X_ZZ_WSP_ATR_Non_Emp_Burs_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("row_no"));

                    row.setZZ_Learning_Programme_Done_ID(rs.getInt("programme_id"));
                    row.setZZ_Non_Emp_Status_Done_ID(rs.getInt("status_id"));

                    row.setZZ_Total_Done(rs.getInt("total_done"));
                    row.setZZ_Total_Planned(rs.getInt("total_planned"));

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