package za.co.ntier.wsp_atr.report.process;

public class ReportBuildResult {
    private final int insertedCount;

    public ReportBuildResult(int insertedCount) {
        this.insertedCount = insertedCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }
}