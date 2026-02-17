package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 4.4 - Induction, Ex-leave and Refresher training done (2024) and planned (2025)
 *
 * Output: ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep
 *
 * Planned (2025): from ZZ_WSP_ATR_WSP
 * - bucket from ZZ_Learning_Programme_Ref via ZZ_Learning_Programme_Type_ID
 * - planned headcount = ZZ_Male + ZZ_Female
 *
 * Trained (2024): from ZZ_WSP_ATR_ATR_Detail
 * - bucket from ZZ_Learning_Programme_Ref via qualification_type_id
 * - count DISTINCT employee_number_id (each person counted once regardless of interventions)
 * - major group derived via biodata -> ofo occupation -> occupations_ref.value prefix -> major group ref
 */
public class InductExLeaveRefresherSection44Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "4.4";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep";

    @Override
    public String getName() {
        return "Induction, Ex-leave and Refresher training done (2024) and planned (2025)";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        // rerun behaviour: delete rows for this report + section
        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        final String sql =
              "WITH \n"
            + "bucketed_planned AS ( \n"
            + "  SELECT \n"
            + "    mg.zz_wsp_atr_ofo_major_group_ref_id AS major_group_id, \n"
            + "    CASE \n"
            + "      WHEN lpr.name ILIKE '%induct%' OR lpr.value ILIKE '%induct%' THEN 'INDUCTION' \n"
            + "      WHEN lpr.name ILIKE '%refres%' OR lpr.value ILIKE '%refres%' THEN 'REFRESHER' \n"
            + "      WHEN lpr.name ILIKE '%ex%leave%' OR lpr.value ILIKE '%ex%leave%' \n"
            + "        OR lpr.name ILIKE '%ex-leave%' OR lpr.value ILIKE '%ex-leave%' THEN 'EXLEAVE' \n"
            + "      ELSE 'OTHER' \n"
            + "    END AS bucket, \n"
            + "    (COALESCE(w.zz_male,0) + COALESCE(w.zz_female,0))::numeric AS planned_total \n"
            + "  FROM zz_wsp_atr_wsp w \n"
            + "  JOIN zz_occupations_ref occ \n"
            + "    ON occ.zz_occupations_ref_id = w.zz_ofo_specialisation_id \n"
            + "  JOIN zz_wsp_atr_ofo_major_group_ref mg \n"
            + "    ON mg.value = left(occ.value, 6) \n"
            + "  JOIN zz_learning_programme_ref lpr \n"
            + "    ON lpr.zz_learning_programme_ref_id = w.zz_learning_programme_type_id \n"
            + "  WHERE w.zz_wsp_atr_submitted_id = ? \n"
            + "), planned AS ( \n"
            + "  SELECT \n"
            + "    major_group_id, \n"
            + "    SUM(CASE WHEN bucket='INDUCTION' THEN planned_total ELSE 0 END)::numeric AS induction_planned, \n"
            + "    SUM(CASE WHEN bucket='EXLEAVE'   THEN planned_total ELSE 0 END)::numeric AS exleave_planned, \n"
            + "    SUM(CASE WHEN bucket='REFRESHER' THEN planned_total ELSE 0 END)::numeric AS refresher_planned \n"
            + "  FROM bucketed_planned \n"
            + "  GROUP BY major_group_id \n"
            + "), \n"
            + "bucketed_trained AS ( \n"
            + "  SELECT \n"
            + "    mg.zz_wsp_atr_ofo_major_group_ref_id AS major_group_id, \n"
            + "    d.employee_number_id, \n"
            + "    CASE \n"
            + "      WHEN lpr.name ILIKE '%induct%' OR lpr.value ILIKE '%induct%' THEN 'INDUCTION' \n"
            + "      WHEN lpr.name ILIKE '%refres%' OR lpr.value ILIKE '%refres%' THEN 'REFRESHER' \n"
            + "      WHEN lpr.name ILIKE '%ex%leave%' OR lpr.value ILIKE '%ex%leave%' \n"
            + "        OR lpr.name ILIKE '%ex-leave%' OR lpr.value ILIKE '%ex-leave%' THEN 'EXLEAVE' \n"
            + "      ELSE 'OTHER' \n"
            + "    END AS bucket \n"
            + "  FROM zz_wsp_atr_atr_detail d \n"
            + "  JOIN zz_learning_programme_ref lpr \n"
            + "    ON lpr.zz_learning_programme_ref_id = d.qualification_type_id \n"
            + "  JOIN zz_wsp_atr_biodata_detail bd \n"
            + "    ON bd.employee_number_id = d.employee_number_id \n"
            + "   AND bd.zz_wsp_atr_submitted_id = d.zz_wsp_atr_submitted_id \n"
            + "  JOIN zz_occupations_ref occ \n"
            + "    ON occ.zz_occupations_ref_id = bd.ofo_occupation_code_id \n"
            + "  JOIN zz_wsp_atr_ofo_major_group_ref mg \n"
            + "    ON mg.value = left(occ.value, 6) \n"
            + "  WHERE d.zz_wsp_atr_submitted_id = ? \n"
            + "    AND d.employee_number_id IS NOT NULL \n"
            + "), trained AS ( \n"
            + "  SELECT \n"
            + "    major_group_id, \n"
            + "    COUNT(DISTINCT CASE WHEN bucket='INDUCTION' THEN employee_number_id END)::numeric AS induction_trained, \n"
            + "    COUNT(DISTINCT CASE WHEN bucket='EXLEAVE'   THEN employee_number_id END)::numeric AS exleave_trained, \n"
            + "    COUNT(DISTINCT CASE WHEN bucket='REFRESHER' THEN employee_number_id END)::numeric AS refresher_trained \n"
            + "  FROM bucketed_trained \n"
            + "  GROUP BY major_group_id \n"
            + "), merged AS ( \n"
            + "  SELECT \n"
            + "    COALESCE(p.major_group_id, t.major_group_id) AS major_group_id, \n"
            + "    COALESCE(t.induction_trained, 0)::numeric AS induction_trained, \n"
            + "    COALESCE(t.exleave_trained,   0)::numeric AS exleave_trained, \n"
            + "    COALESCE(t.refresher_trained, 0)::numeric AS refresher_trained, \n"
            + "    COALESCE(p.induction_planned, 0)::numeric AS induction_planned, \n"
            + "    COALESCE(p.exleave_planned,   0)::numeric AS exleave_planned, \n"
            + "    COALESCE(p.refresher_planned, 0)::numeric AS refresher_planned \n"
            + "  FROM planned p \n"
            + "  FULL OUTER JOIN trained t \n"
            + "    ON t.major_group_id = p.major_group_id \n"
            + ") \n"
            + "SELECT \n"
            + "  m.major_group_id, \n"
            + "  m.induction_trained, m.exleave_trained, m.refresher_trained, \n"
            + "  m.induction_planned, m.exleave_planned, m.refresher_planned \n"
            + "FROM merged m \n"
            + "ORDER BY m.major_group_id \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            int idx = 1;
            pstmt.setInt(idx++, submitted.getZZ_WSP_ATR_Submitted_ID()); // planned
            pstmt.setInt(idx++, submitted.getZZ_WSP_ATR_Submitted_ID()); // trained

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep row =
                            new X_ZZ_WSP_ATR_Induct_Ex_Leave_Training_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.set_ValueOfColumn("ZZ_WSP_ATR_OFO_Major_Group_Ref_ID", rs.getInt("major_group_id"));

                    // trained (2024)
                    row.set_ValueOfColumn("ZZ_Induction_Trained_Cnt", rs.getBigDecimal("induction_trained"));
                    row.set_ValueOfColumn("ZZ_Ex_Leave_Trained_Cnt",  rs.getBigDecimal("exleave_trained"));
                    row.set_ValueOfColumn("ZZ_Refresher_Trained",     rs.getBigDecimal("refresher_trained"));

                    // planned (2025)
                    row.set_ValueOfColumn("ZZ_Induction_Planned_Cnt",  rs.getBigDecimal("induction_planned"));
                    row.set_ValueOfColumn("ZZ_Ex_Leave_Planned_Cnt",   rs.getBigDecimal("exleave_planned"));
                    row.set_ValueOfColumn("ZZ_Refresher_Planned",      rs.getBigDecimal("refresher_planned"));

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