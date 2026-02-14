package za.co.ntier.wsp_atr.report.process;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;

public interface IReportSectionBuilder {

    /** Human name for logs */
    String getName();

    /** The section code you want stored in ZZ_Section, e.g. "2.1" */
    String getSectionCode();

    /**
     * Build this report section for the submitted record.
     * Must insert rows linked to report.getZZ_WSP_ATR_Report_ID().
     */
    ReportBuildResult build(X_ZZ_WSP_ATR_Report report, I_ZZ_WSP_ATR_Submitted submitted, String trxName) throws Exception;
}