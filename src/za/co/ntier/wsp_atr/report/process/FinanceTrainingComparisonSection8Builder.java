package za.co.ntier.wsp_atr.report.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Finance_Train_Compare_Rep;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.ntier.models.MZZWSPATRSubmitted;

/**
 * Section 8 - Finance and Training Comparison
 *
 * Input : ZZ_WSP_ATR_Finance (filtered by ZZ_WSP_ATR_Submitted_ID)
 * Output: ZZ_WSP_ATR_Finance_Train_Compare_Rep (linked to ZZ_WSP_ATR_Report_ID)
 *
 * Layout:
 * - Records are grouped by input ZZ_Section (e.g. Skills Development Spend, Training Offered..., Comments...)
 * - Each row is a label (ZZ_Finance_Type) and a value (ZZ_Finance_Value)
 */
public class FinanceTrainingComparisonSection8Builder extends AbstractReportSectionBuilder {

    private static final String SECTION = "8";
    private static final String TARGET_TABLE = "ZZ_WSP_ATR_Finance_Train_Compare_Rep";
    private static final String INPUT_TABLE  = "ZZ_WSP_ATR_Finance";

    @Override
    public String getName() {
        return "Finance and Training Comparison";
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
            + "  COALESCE(f.Row_No, 0)::int     AS row_no, \n"
            + "  COALESCE(f.ZZ_Section, '')     AS zz_section, \n"
            + "  COALESCE(f.ZZ_Finance_Type,'') AS finance_type, \n"
            + "  COALESCE(f.ZZ_Finance_Value,'')AS finance_value \n"
            + "FROM " + INPUT_TABLE + " f \n"
            + "WHERE f.ZZ_WSP_ATR_Submitted_ID = ? \n"
            + "  AND f.IsActive = 'Y' \n"
            + "ORDER BY COALESCE(f.Row_No,0), f.ZZ_WSP_ATR_Finance_ID \n";

        try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
            pstmt.setInt(1, submitted.getZZ_WSP_ATR_Submitted_ID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    X_ZZ_WSP_ATR_Finance_Train_Compare_Rep row =
                            new X_ZZ_WSP_ATR_Finance_Train_Compare_Rep(report.getCtx(), 0, trxName);

                    row.setZZ_WSP_ATR_Report_ID(report.getZZ_WSP_ATR_Report_ID());
                    row.set_ValueOfColumn("ZZ_Report_Section", SECTION);

                    row.setRow_No(rs.getInt("row_no"));
                    row.setZZ_Section(rs.getString("zz_section"));
                    row.setZZ_Finance_Type(rs.getString("finance_type"));
                    row.setZZ_Finance_Value(rs.getString("finance_value"));

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