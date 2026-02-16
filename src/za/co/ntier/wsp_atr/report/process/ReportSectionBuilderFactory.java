package za.co.ntier.wsp_atr.report.process;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import za.ntier.models.MZZWSPATRSubmitted;

public class ReportSectionBuilderFactory {

    private ReportSectionBuilderFactory() {}

    public static List<IReportSectionBuilder> getBuilders(Properties ctx, MZZWSPATRSubmitted submitted) {
        // Later you can filter based on submitted columns (template type, year, etc.)
        List<IReportSectionBuilder> list = new ArrayList<>();

        list.add(new WorkforceEmpSummarySection21Builder());
        list.add(new GeoDistributionSection22Builder());
        // list.add(new AnotherSectionBuilder());
        // list.add(new YetAnotherSectionBuilder());

        return list;
    }
}
