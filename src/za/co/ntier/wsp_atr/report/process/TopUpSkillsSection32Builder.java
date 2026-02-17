package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_TopUp_Skills_Rep;
import za.ntier.models.MZZWSPATRSubmitted;

public class TopUpSkillsSection32Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "3.2";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_TopUp_Skills_Rep";

    @Override
    public String getName() {
        return "Generic or Top-up Skills Survey";
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
                "SELECT \n"
              + "  t.row_no, \n"
              + "  t.zz_ofo_specialisation_id, \n"
              + "  t.zz_topupskill_id, \n"
              + "  t.comments \n"
              + "FROM ZZ_WSP_ATR_TopUp_Skills t \n"
              + "WHERE t.zz_wsp_atr_submitted_id = ? \n"
              + "ORDER BY t.row_no, t.zz_wsp_atr_topup_skills_id \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_TopUp_Skills_Rep row =
                            new X_ZZ_WSP_ATR_TopUp_Skills_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    // keep row ordering if the column exists in Rep
                    if (row.get_ColumnIndex("Row_No") >= 0) {
                        row.set_ValueOfColumn("Row_No", rs.getInt("row_no"));
                    }

                    // FK columns (no hard-coded IDs)
                    if (row.get_ColumnIndex("ZZ_OFO_Specialisation_ID") >= 0) {
                        row.set_ValueOfColumn("ZZ_OFO_Specialisation_ID", rs.getInt("zz_ofo_specialisation_id"));
                    } else if (row.get_ColumnIndex("ZZ_OFO_Specialisation") >= 0) {
                        row.set_ValueOfColumn("ZZ_OFO_Specialisation", rs.getInt("zz_ofo_specialisation_id"));
                    }

                    if (row.get_ColumnIndex("ZZ_TopUpSkill_ID") >= 0) {
                        row.set_ValueOfColumn("ZZ_TopUpSkill_ID", rs.getInt("zz_topupskill_id"));
                    } else if (row.get_ColumnIndex("ZZ_TopUp_Skills_ID") >= 0) {
                        row.set_ValueOfColumn("ZZ_TopUp_Skills_ID", rs.getInt("zz_topupskill_id"));
                    }

                    if (row.get_ColumnIndex("Comments") >= 0) {
                        row.set_ValueOfColumn("Comments", rs.getString("comments"));
                    } else if (row.get_ColumnIndex("ZZ_Comments") >= 0) {
                        row.set_ValueOfColumn("ZZ_Comments", rs.getString("comments"));
                    }

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