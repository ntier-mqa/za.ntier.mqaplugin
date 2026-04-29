package za.ntier.repo;

import java.util.*;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.DB;

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
     * Resolves the 4-digit fiscal year number for a given C_Year_ID.
     * e.g. C_Year_ID → "2025" → 2025
     */
    public int resolveHeaderYearNumber(int cYearId) {
        String fy = DB.getSQLValueString(trx,
                "SELECT FiscalYear FROM C_Year WHERE C_Year_ID=?", cYearId);
        if (fy == null) return 0;
        try { return Integer.parseInt(fy.trim()); } catch (Exception e) { return 0; }
    }

    /**
     * Returns all unlinked levy lines for the given SDL number whose header's
     * year+month (C_Year_ID → FiscalYear, ZZ_Month) is strictly before
     * the current header's year+month boundary.
     *
     * @param sdlNo             SDL number of the BP
     * @param currentHdrYearNum fiscal year number of the current header (e.g. 2025)
     * @param currentHdrMonthOrd month order of the current header (1=Jan … 12=Dec)
     */
    public List<X_ZZ_Monthly_Levy_Files> getPriorUnlinkedLinesForBP(
            String sdlNo, int currentHdrYearNum, int currentHdrMonthOrd) {

        // Step 1: collect all header IDs whose year+month is strictly before the boundary
        Set<Integer> priorHdrIds = resolvePriorHeaderIds(currentHdrYearNum, currentHdrMonthOrd);
        if (priorHdrIds.isEmpty()) return Collections.emptyList();

        // Step 2: load all unlinked lines for this BP across all headers
        List<X_ZZ_Monthly_Levy_Files> all = new Query(ctx, X_ZZ_Monthly_Levy_Files.Table_Name,
                "ZZ_SDL_No=? AND IsActive='Y' AND C_InvoiceBatchLine_ID IS NULL", trx)
                .setParameters(sdlNo).setOnlyActiveRecords(true).list();

        // Step 3: keep only those belonging to a prior header
        List<X_ZZ_Monthly_Levy_Files> result = new ArrayList<>();
        for (X_ZZ_Monthly_Levy_Files r : all) {
            if (priorHdrIds.contains(r.getZZ_Monthly_Levy_Files_Hdr_ID())) {
                result.add(r);
            }
        }
        return result;
    }

    /** Links all levy lines in the list to the given batch line ID. */
    public void linkLinesToBatchLine(List<X_ZZ_Monthly_Levy_Files> lines, int batchLineId) {
        for (X_ZZ_Monthly_Levy_Files r : lines) {
            if (r.getC_InvoiceBatchLine_ID() == 0) {
                DB.executeUpdateEx(
                    "UPDATE " + X_ZZ_Monthly_Levy_Files.Table_Name +
                    " SET C_InvoiceBatchLine_ID=?, Updated=NOW(), UpdatedBy=0" +
                    " WHERE " + X_ZZ_Monthly_Levy_Files.Table_Name + "_ID=?",
                    new Object[]{batchLineId, r.get_ID()},
                    trx);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Queries all active levy file headers, resolves each one's fiscal year number
     * and month order, and returns the set of header IDs that are strictly before
     * the given year+month boundary.
     */
    private Set<Integer> resolvePriorHeaderIds(int currentHdrYearNum, int currentHdrMonthOrd) {
        String sql = "SELECT h.ZZ_Monthly_Levy_Files_Hdr_ID, cy.FiscalYear, h.ZZ_Month " +
                     "FROM ZZ_Monthly_Levy_Files_Hdr h " +
                     "JOIN C_Year cy ON h.C_Year_ID = cy.C_Year_ID " +
                     "WHERE h.IsActive='Y'";

        List<List<Object>> rows = DB.getSQLArrayObjectsEx(trx, sql, new Object[]{});
        Set<Integer> result = new HashSet<>();
        if (rows == null) return result;

        for (List<Object> row : rows) {
            int    hdrId   = ((Number) row.get(0)).intValue();
            String fy      = row.get(1) == null ? "" : row.get(1).toString().trim();
            String month   = row.get(2) == null ? "" : row.get(2).toString().trim();
            int    yearNum = 0;
            try { yearNum = Integer.parseInt(fy); } catch (Exception ignored) {}
            int monthOrd = MonthUtil.order(month);

            if (yearNum < currentHdrYearNum
                    || (yearNum == currentHdrYearNum && monthOrd < currentHdrMonthOrd)) {
                result.add(hdrId);
            }
        }
        return result;
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
