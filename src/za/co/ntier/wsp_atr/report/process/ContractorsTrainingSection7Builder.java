package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Contractors_Training_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 7 - Contractors: Received Training in 2024 and Planned Training for 2025
 *
 * Input : ZZ_WSP_ATR_Contractors (filtered by ZZ_WSP_ATR_Submitted_ID)
 * Output: ZZ_WSP_ATR_Contractors_Training_Rep (linked to ZZ_WSP_ATR_Report_ID)
 *
 * Notes:
 * - Groups by contractors learning programme (input ZZ_Learning_Programme_ID).
 * - Uses integer reads (rs.getInt()).
 * - Planned total is NOT stored in output table (no column) -> JRXML computes it.
 */
public class ContractorsTrainingSection7Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "7";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Contractors_Training_Rep";
    private static final String INPUT_TABLE  = "ZZ_WSP_ATR_Contractors";

    @Override
    public String getName() {
        return "Contractors: Received Training in  and Planned Training for ";
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
              "WITH grp AS ( \n"
            + "  SELECT \n"
            + "    c.zz_learning_programme_id AS programme_id, \n"
            + "    SUM(COALESCE(c.zz_managers_trained,0))::int          AS managers_trained, \n"
            + "    SUM(COALESCE(c.zz_professionals_trained,0))::int     AS professionals_trained, \n"
            + "    SUM(COALESCE(c.zz_technicians_trained,0))::int       AS technicians_trained, \n"
            + "    SUM(COALESCE(c.zz_clerical_trained,0))::int          AS clerical_trained, \n"
            + "    SUM(COALESCE(c.zz_service_trained,0))::int           AS service_trained, \n"
            + "    SUM(COALESCE(c.zz_skilled_workers_trained,0))::int   AS skilled_trained, \n"
            + "    SUM(COALESCE(c.zz_plant_trained,0))::int             AS plant_trained, \n"
            + "    SUM(COALESCE(c.zz_elementary_trained,0))::int        AS elementary_trained, \n"
            + "    SUM(COALESCE(c.zz_learners_trained,0))::int          AS learners_trained, \n"
            + "    SUM(COALESCE(c.zz_total_trained,0))::int             AS total_trained, \n"
            + "    SUM(COALESCE(c.zz_managers_planned,0))::int          AS managers_planned, \n"
            + "    SUM(COALESCE(c.zz_professionals_planned,0))::int     AS professionals_planned, \n"
            + "    SUM(COALESCE(c.zz_technicians_planned,0))::int       AS technicians_planned, \n"
            + "    SUM(COALESCE(c.zz_clerical_planned,0))::int          AS clerical_planned, \n"
            + "    SUM(COALESCE(c.zz_service_planned,0))::int           AS service_planned, \n"
            + "    SUM(COALESCE(c.zz_skilled_workers_planned,0))::int   AS skilled_planned, \n"
            + "    SUM(COALESCE(c.zz_plant_planned,0))::int             AS plant_planned, \n"
            + "    SUM(COALESCE(c.zz_elementary_planned,0))::int        AS elementary_planned, \n"
            + "    SUM(COALESCE(c.zz_learners_planned,0))::int          AS learners_planned \n"
            + "  FROM " + INPUT_TABLE + " c \n"
            + "  WHERE c.zz_wsp_atr_submitted_id = ? \n"
            + "    AND c.isactive = 'Y' \n"
            + "    AND c.zz_learning_programme_id IS NOT NULL \n"
            + "  GROUP BY c.zz_learning_programme_id \n"
            + "), numbered AS ( \n"
            + "  SELECT row_number() OVER (ORDER BY programme_id) AS row_no, * \n"
            + "  FROM grp \n"
            + ") \n"
            + "SELECT * FROM numbered ORDER BY row_no \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Contractors_Training_Rep row =
                            new X_ZZ_WSP_ATR_Contractors_Training_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("row_no"));

                    // IMPORTANT: output column ZZ_Learning_Programme_Type_ID references zz_contractors_learning_programme_ref
                    // input column zz_learning_programme_id also references that ref
                    row.setZZ_Learning_Programme_Type_ID(rs.getInt("programme_id"));

                    // Trained 2024
                    row.setZZ_Managers_Trained(rs.getInt("managers_trained"));
                    row.setZZ_Professionals_Trained(rs.getInt("professionals_trained"));
                    row.setZZ_Technicians_Trained(rs.getInt("technicians_trained"));
                    row.setZZ_Clerical_Trained(rs.getInt("clerical_trained"));
                    row.setZZ_Service_Trained(rs.getInt("service_trained"));
                    row.setZZ_Skilled_Workers_Trained(rs.getInt("skilled_trained"));
                    row.setZZ_Plant_Trained(rs.getInt("plant_trained"));
                    row.setZZ_Elementary_Trained(rs.getInt("elementary_trained"));
                    row.setZZ_Learners_Trained(rs.getInt("learners_trained"));
                    row.setZZ_Total_Trained(rs.getInt("total_trained"));

                    // Planned 2025
                    row.setZZ_Managers_Planned(rs.getInt("managers_planned"));
                    row.setZZ_Professionals_Planned(rs.getInt("professionals_planned"));
                    row.setZZ_Technicians_Planned(rs.getInt("technicians_planned"));
                    row.setZZ_Clerical_Planned(rs.getInt("clerical_planned"));
                    row.setZZ_Service_Planned(rs.getInt("service_planned"));
                    row.setZZ_Skilled_Workers_Planned(rs.getInt("skilled_planned"));
                    row.setZZ_Plant_Planned(rs.getInt("plant_planned"));
                    row.setZZ_Elementary_Planned(rs.getInt("elementary_planned"));
                    row.setZZ_Learners_Planned(rs.getInt("learners_planned"));

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