package za.co.ntier.wsp_atr.report.process;

import org.compiere.util.DB;

public abstract class AbstractReportSectionBuilder implements IReportSectionBuilder {

    /**
     * Optional: if you want reruns to be idempotent, delete prior rows for this report+section.
     */
    protected int deleteExistingByReportAndSection(String targetTable, int reportId, String section, String trxName) {
        String sql = "DELETE FROM " + targetTable + " WHERE ZZ_WSP_ATR_Report_ID=? AND ZZ_Section=?";
        return DB.executeUpdateEx(sql, new Object[] { reportId, section }, trxName);
    }
}
