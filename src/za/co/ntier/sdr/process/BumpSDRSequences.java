package za.co.ntier.sdr.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Added 2026-11-25 for the dev -&gt; UAT data move (pg_dump/pg_restore of the sdr_* tables): the
 * restored rows carry the primary keys that were originally assigned by AD_Sequence on dev, but
 * UAT's AD_Sequence rows for those tables are still at their initial value (the tables were created
 * empty by the pack-in). Without this, the first record created through the UI on UAT would be handed
 * an id that already exists.
 *
 * <p>For every SDR_ table in AD_Table (single key column), sets AD_Sequence.CurrentNext to
 * MAX(key)+1 - but never LOWER than its current value (GREATEST), so it is safe to re-run. If the
 * server uses native Postgres sequences for table ids (a sequence named &lt;table&gt;_sq exists), that
 * sequence is advanced too.
 *
 * <p>Run under the System client: AD_Sequence rows for tables created by the pack-in belong to the
 * System client, and the update is raw SQL (no PO), so no cross-tenant guard is involved. Restart the
 * server or reset the cache afterwards so no stale sequence value is held in memory.
 */
@Process(name = "za.co.ntier.sdr.process.BumpSDRSequences")
public class BumpSDRSequences extends SvrProcess {

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        List<String[]> tables = new ArrayList<>(); // {tableName, keyColumn}
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // Key column = the table's single IsKey='Y' AD_Column; tables without exactly one are skipped.
            pstmt = DB.prepareStatement(
                    "SELECT t.TableName, MIN(c.ColumnName) FROM AD_Table t "
                            + "JOIN AD_Column c ON c.AD_Table_ID = t.AD_Table_ID AND c.IsKey = 'Y' "
                            + "WHERE t.TableName LIKE 'SDR\\_%' ESCAPE '\\' AND t.IsView = 'N' "
                            + "GROUP BY t.TableName HAVING COUNT(*) = 1 ORDER BY t.TableName",
                    get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                tables.add(new String[] { rs.getString(1), rs.getString(2) });
            }
        } finally {
            DB.close(rs, pstmt);
        }

        int bumped = 0;
        int unchanged = 0;
        int noSequence = 0;
        int nativeBumped = 0;
        int noPhysicalTable = 0;

        for (String[] t : tables) {
            String tableName = t[0];
            String keyColumn = t[1];

            long physical = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", tableName.toLowerCase());
            if (physical == 0) {
                noPhysicalTable++;
                addLog(tableName + ": no physical table - skipped.");
                continue;
            }

            long max = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COALESCE(MAX(" + keyColumn + "), 0) FROM " + tableName);
            long wantedNext = max + 1;

            long current = DB.getSQLValueEx(get_TrxName(),
                    "SELECT MAX(CurrentNext) FROM AD_Sequence WHERE Name = ? AND IsTableID = 'Y'", tableName);
            if (current < 0) { // getSQLValueEx returns -1 when no row / NULL
                noSequence++;
                addLog(tableName + ": no AD_Sequence (IsTableID='Y') row - skipped. MAX(" + keyColumn + ")=" + max);
                continue;
            }

            if (current >= wantedNext) {
                unchanged++;
            } else {
                DB.executeUpdateEx("UPDATE AD_Sequence SET CurrentNext = ? WHERE Name = ? AND IsTableID = 'Y' "
                        + "AND CurrentNext < ?", new Object[] { wantedNext, tableName, wantedNext }, get_TrxName());
                bumped++;
                addLog(tableName + ": CurrentNext " + current + " -> " + wantedNext);
            }

            String nativeSeq = tableName.toLowerCase() + "_sq";
            long hasNative = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM pg_class WHERE relkind = 'S' AND relname = ?", nativeSeq);
            if (hasNative > 0) {
                // setval(seq, X) with is_called=true => next value handed out is X+1.
                long last = DB.getSQLValueEx(get_TrxName(), "SELECT last_value FROM " + nativeSeq);
                if (last < max) {
                    DB.getSQLValueEx(get_TrxName(), "SELECT setval('" + nativeSeq + "', " + max + ")");
                    nativeBumped++;
                    addLog(tableName + ": native sequence " + nativeSeq + " " + last + " -> " + max);
                }
            }
        }

        return "SDR sequences: " + tables.size() + " table(s) checked, " + bumped + " AD_Sequence bumped, "
                + unchanged + " already ahead of MAX(key), " + noSequence + " without AD_Sequence, " + noPhysicalTable
                + " without physical table, " + nativeBumped + " native sequence(s) advanced.";
    }
}
