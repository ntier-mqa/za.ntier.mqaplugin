package za.ntier.repo;

import java.util.*;
import java.util.Properties;

import org.compiere.model.Query;

import za.ntier.models.X_ZZ_Monthly_Levy_Files;
import za.ntier.models.X_ZZ_Monthly_Levy_Files_Hdr;
import za.ntier.utils.MonthUtil;

public class LevyFilesRepository {
    private final Properties ctx; private final String trx;

    public LevyFilesRepository(Properties ctx, String trx) { this.ctx = ctx; this.trx = trx; }

    public X_ZZ_Monthly_Levy_Files_Hdr getHeaderById(int id) {
        return new X_ZZ_Monthly_Levy_Files_Hdr(ctx, id, trx);
    }

    /** Returns all unlinked lines for the current header, sorted by ZZ_Year then ZZ_Month. */
    public List<X_ZZ_Monthly_Levy_Files> getUnprocessedRows(int hdrId) {
        List<X_ZZ_Monthly_Levy_Files> rows = new Query(ctx, X_ZZ_Monthly_Levy_Files.Table_Name,
                "ZZ_Monthly_Levy_Files_Hdr_ID=? AND IsActive='Y' AND C_InvoiceBatchLine_ID IS NULL", trx)
                .setParameters(hdrId).setOnlyActiveRecords(true).list();
        rows.sort(Comparator
                .comparing((X_ZZ_Monthly_Levy_Files r) -> safe(r.getZZ_Year()))
                .thenComparingInt(r -> MonthUtil.order(safe(r.getZZ_Month()))));
        return rows;
    }

    /**
     * Returns all unlinked levy lines for the given SDL number from headers whose
     * year+month is strictly before the supplied currentYear/currentMonth.
     */
    public List<X_ZZ_Monthly_Levy_Files> getPriorUnlinkedLinesForBP(
            String sdlNo, String currentYear, String currentMonth) {

        List<X_ZZ_Monthly_Levy_Files> all = new Query(ctx, X_ZZ_Monthly_Levy_Files.Table_Name,
                "ZZ_SDL_No=? AND IsActive='Y' AND C_InvoiceBatchLine_ID IS NULL", trx)
                .setParameters(sdlNo).setOnlyActiveRecords(true).list();

        int currentYearInt  = parseYearInt(currentYear);
        int currentMonthOrd = MonthUtil.order(currentMonth);

        List<X_ZZ_Monthly_Levy_Files> result = new ArrayList<>();
        for (X_ZZ_Monthly_Levy_Files r : all) {
            int ry = parseYearInt(safe(r.getZZ_Year()));
            int rm = MonthUtil.order(safe(r.getZZ_Month()));
            if (ry < currentYearInt || (ry == currentYearInt && rm < currentMonthOrd)) {
                result.add(r);
            }
        }
        return result;
    }

    /** Links all levy lines in the list to the given batch line ID. */
    public void linkLinesToBatchLine(List<X_ZZ_Monthly_Levy_Files> lines, int batchLineId) {
        for (X_ZZ_Monthly_Levy_Files r : lines) {
            if (r.getC_InvoiceBatchLine_ID() == 0) {
                r.setC_InvoiceBatchLine_ID(batchLineId);
                r.saveEx();
            }
        }
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static int parseYearInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
