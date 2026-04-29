package za.ntier.repo;

import java.util.Properties;

import org.compiere.util.DB;

public class ApprovalsRepository {
    private final Properties ctx; private final String trx;

    public ApprovalsRepository(Properties ctx, String trx) { this.ctx = ctx; this.trx = trx; }

    /**
     * Returns true if the BP has an active Approved (ZZ_Grant_Status='A')
     * WSP-ATR record for the given financial year.
     */
    public boolean hasApprovedForYear(int bpId, String year) {
        String sql = "SELECT 1 FROM ZZ_WSP_ATR_Approvals " +
                     "WHERE C_BPartner_ID=? AND ZZ_Grant_Status='A' AND IsActive='Y' " +
                     "AND COALESCE(TRIM(ZZ_Financial_Year),'')=TRIM(?) " +
                     "FETCH FIRST 1 ROWS ONLY";
        Integer one = DB.getSQLValue(trx, sql, bpId, year);
        return one != null && one == 1;
    }
}
