package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Plan_Train_Per_Occ_Grp_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 5.2.1 - Planned number to be trained per Occupational Group
 *
 * Input:  ZZ_WSP_ATR_WSP (per submitted)
 * Output: ZZ_WSP_ATR_Plan_Train_Per_Occ_Grp_Rep
 *
 * Layout is row-based (NOT grouped).
 * One row per ZZ_WSP_ATR_WSP record.
 */
public class PlanTrainPerOccGroupSection521Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "5.2.1";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Plan_Train_Per_Occ_Grp_Rep";

    @Override
    public String getName() {
        return "Planned number to be trained per Occupational Group";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report,
                                   MZZWSPATRSubmitted submitted,
                                   String trxName) throws Exception {

        deleteExistingByReportAndSection(
                TARGET_TABLE,
                report.getZZ_WSP_ATR_Report_ID(),
                SECTION,
                trxName
        );

        int inserted = 0;

        final String sql =
              "SELECT \n"
            + "  wsp.ZZ_WSP_ATR_WSP_ID, \n"
            + "  wsp.Row_No, \n"
            + "  wsp.ZZ_OFO_Specialisation_ID, \n"
            + "  wsp.ZZ_Learning_Programme_Type_ID, \n"
            + "  wsp.ZZ_Learning_Programme_ID, \n"
            + "  wsp.ZZ_Qualification, \n"
            + "  COALESCE(wsp.ZZ_Male,0)     AS male_cnt, \n"
            + "  COALESCE(wsp.ZZ_Female,0)   AS female_cnt, \n"
            + "  COALESCE(wsp.ZZ_African,0)  AS african_cnt, \n"
            + "  COALESCE(wsp.ZZ_Coloured,0) AS coloured_cnt, \n"
            + "  COALESCE(wsp.ZZ_Indian,0)   AS indian_cnt, \n"
            + "  COALESCE(wsp.ZZ_White,0)    AS white_cnt, \n"
            + "  COALESCE(wsp.ZZ_Disabled,0) AS disabled_cnt \n"
            + "FROM ZZ_WSP_ATR_WSP wsp \n"
            + "WHERE wsp.ZZ_WSP_ATR_Submitted_ID = ? \n"
            + "ORDER BY wsp.Row_No";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {

            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    X_ZZ_WSP_ATR_Plan_Train_Per_Occ_Grp_Rep row =
                            new X_ZZ_WSP_ATR_Plan_Train_Per_Occ_Grp_Rep(
                                    report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("row_no"));

                    row.setZZ_OFO_Specialisation_ID(
                            rs.getInt("zz_ofo_specialisation_id"));

                    row.setZZ_Learning_Programme_Type_ID(
                            rs.getInt("zz_learning_programme_type_id"));

                    row.setZZ_Learning_Programme_ID(
                            rs.getInt("zz_learning_programme_id"));

                    row.setZZ_Qualification(
                            rs.getString("zz_qualification"));

                    int male   = rs.getInt("male_cnt");
                    int female = rs.getInt("female_cnt");

                    row.setZZ_Male(male);
                    row.setZZ_Female(female);

                    row.setZZ_African(rs.getInt("african_cnt"));
                    row.setZZ_Coloured(rs.getInt("coloured_cnt"));
                    row.setZZ_Indian(rs.getInt("indian_cnt"));
                    row.setZZ_White(rs.getInt("white_cnt"));
                    row.setZZ_Disabled(rs.getInt("disabled_cnt"));

                    // Optional: store row total if column exists
                    row.set_ValueOfColumn("ZZ_Total_Planned",
                            male + female);

                    if (!row.save()) {
                        throw new IllegalStateException(
                                "Failed inserting " + TARGET_TABLE);
                    }

                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }
}