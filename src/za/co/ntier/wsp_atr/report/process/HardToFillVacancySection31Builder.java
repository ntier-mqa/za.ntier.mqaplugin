package za.co.ntier.wsp_atr.report.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_HTFV_Rep;
import za.ntier.models.MZZWSPATRSubmitted;

public class HardToFillVacancySection31Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "3.1";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_HTFV_Rep";

    @Override
    public String getName() {
        return "Hard-To-Fill Vacancy (HTFV)";
    }

    @Override
    public String getSectionCode() {
        return SECTION;
    }

    @Override
    public ReportBuildResult build(X_ZZ_WSP_ATR_Report report, MZZWSPATRSubmitted submitted, String trxName) throws Exception {

        // rerun-safe: delete by report only (as per your base class)
        deleteExistingByReportAndSection(TARGET_TABLE, report.getZZ_WSP_ATR_Report_ID(), SECTION, trxName);

        int inserted = 0;

        final String sql =
                "SELECT \n"
              + "  h.Row_No, \n"
              + "  h.ZZ_Occupations_ID, \n"
              + "  occ.Name AS occupation_name, \n"
              + "  COALESCE(occ.Value, '') AS occupation_code, \n"
              + "  h.ZZ_Scarce_Reason_ID, \n"
              + "  sr.Name AS scarce_reason_name, \n"
              + "  h.ZZ_Further_Scarce_Reason_ID, \n"
              + "  fsr1.Name AS further_reason1_name, \n"
              + "  h.ZZ_Further_Scarce_Reason2_ID, \n"
              + "  fsr2.Name AS further_reason2_name, \n"
              + "  h.ZZ_Scarce_Other_Reasons_Comments, \n"
              + "  h.ZZ_Vacancies_EC_Cnt, h.ZZ_Vacancies_FS_Cnt, h.ZZ_Vacancies_GP_Cnt, \n"
              + "  h.ZZ_Vacancies_KZN_Cnt, h.ZZ_Vacancies_LP_Cnt, h.ZZ_Vacancies_MP_Cnt, \n"
              + "  h.ZZ_Vacancies_NP_Cnt, h.ZZ_Vacancies_NW_Cnt, h.ZZ_Vacancies_WC_Cnt \n"
              + "FROM ZZ_WSP_ATR_HTVF h \n"
              + "LEFT JOIN ZZ_Occupations_Ref   occ  ON occ.ZZ_Occupations_Ref_ID = h.ZZ_Occupations_ID \n"
              + "LEFT JOIN ZZ_Scarce_Reason_Ref sr   ON sr.ZZ_Scarce_Reason_Ref_ID = h.ZZ_Scarce_Reason_ID \n"
              + "LEFT JOIN ZZ_Scarce_Reason_Ref fsr1 ON fsr1.ZZ_Scarce_Reason_Ref_ID = h.ZZ_Further_Scarce_Reason_ID \n"
              + "LEFT JOIN ZZ_Scarce_Reason_Ref fsr2 ON fsr2.ZZ_Scarce_Reason_Ref_ID = h.ZZ_Further_Scarce_Reason2_ID \n"
              + "WHERE h.ZZ_WSP_ATR_Submitted_ID = ? \n"
              + "ORDER BY h.Row_No, h.ZZ_WSP_ATR_HTVF_ID \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    X_ZZ_WSP_ATR_HTFV_Rep row = new X_ZZ_WSP_ATR_HTFV_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.setZZ_WSP_ATR_Submitted_ID(submitted.getZZ_WSP_ATR_Submitted_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("Row_No"));

                    // keep IDs (FKs), don’t hard-code any reference IDs
                    row.setZZ_Occupations_ID(rs.getInt("ZZ_Occupations_ID"));
                    row.setZZ_Scarce_Reason_ID(rs.getInt("ZZ_Scarce_Reason_ID"));
                    row.setZZ_Further_Scarce_Reason_ID(rs.getInt("ZZ_Further_Scarce_Reason_ID"));
                    row.setZZ_Further_Scarce_Reason2_ID(rs.getInt("ZZ_Further_Scarce_Reason2_ID"));

                    // copy comments
                    row.setZZ_Scarce_Other_Reasons_Comments(rs.getString("ZZ_Scarce_Other_Reasons_Comments"));

                    // numeric counts by province (BigDecimal)
                    row.setZZ_Vacancies_EC_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_EC_Cnt")));
                    row.setZZ_Vacancies_FS_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_FS_Cnt")));
                    row.setZZ_Vacancies_GP_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_GP_Cnt")));
                    row.setZZ_Vacancies_KZN_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_KZN_Cnt")));
                    row.setZZ_Vacancies_LP_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_LP_Cnt")));
                    row.setZZ_Vacancies_MP_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_MP_Cnt")));
                    row.setZZ_Vacancies_NP_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_NP_Cnt")));
                    row.setZZ_Vacancies_NW_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_NW_Cnt")));
                    row.setZZ_Vacancies_WC_Cnt(nz(rs.getBigDecimal("ZZ_Vacancies_WC_Cnt")));

                    if (!row.save()) {
                        throw new IllegalStateException("Failed inserting " + TARGET_TABLE);
                    }
                    inserted++;
                }
            }
        }

        return new ReportBuildResult(inserted);
    }

    private static BigDecimal nz(BigDecimal bd) {
        return bd == null ? BigDecimal.ZERO : bd;
    }
}