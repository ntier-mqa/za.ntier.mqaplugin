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

        // Section 2
        list.add(new WorkforceEmpSummarySection21Builder());
        list.add(new GeoDistributionSection22Builder());
        list.add(new TerminatedEmployeesSection23Builder());
        list.add(new WorkforceEducationLevelSection24Builder());

        // Section 3
        list.add(new HardToFillVacancySection31Builder());
        list.add(new TopUpSkillsSection32Builder());

        // Section 4
        list.add(new SummEmpTrainingInterventionsSection42Builder());
        list.add(new InductExLeaveRefresherSection44Builder());
        list.add(new SummPlannedEmpTrainingInterventionsSection431Builder());

        // Section 5
        list.add(new EmployeesTrainedSummarySection41Builder());
        list.add(new SummEmpTrainingInterventionsSection51Builder());

        // Section 6
        list.add(new NonEmployeesAetBursariesSection61Builder());
        list.add(new NonEmployeesBursariesSection62Builder());
        list.add(new NonEmployeesSkillsDevSection63Builder());
        list.add(new NonEmployeesSkillsDevPlanSection64Builder());

        // Section 7
        list.add(new ContractorsTrainingSection7Builder());

        // Section 8
        list.add(new FinanceTrainingComparisonSection8Builder());

        // Section 5.2.1 (planned per occ group)
        list.add(new PlanTrainPerOccGroupSection521Builder());

        return list;
    }
}
