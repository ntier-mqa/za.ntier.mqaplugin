package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.adempiere.base.annotation.Parameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process(name = "za.ntier.process.TransactionBalanceReportSummary")
public class TransactionBalanceReportSummary extends SvrProcess {

    @Parameter(name = "StartDate")
    protected Timestamp startDate;

    @Parameter(name = "EndDate")
    protected Timestamp endDate;

    @Parameter(name = "M_Product_ID")
    protected int mProductId;

    private int pInstanceId;

    @Override
    protected void prepare() {
        pInstanceId = getAD_PInstance_ID();
    }

    @Override
    protected String doIt() throws Exception {
        int lineNo = 0;

        String sql = """
            WITH txn_summary AS (
                SELECT
                    t.m_product_id,
                    t.ad_client_id,
                    SUM(CASE WHEN t.movementdate < ? THEN t.movementqty ELSE 0 END) AS opening_balance,
                    SUM(CASE WHEN t.movementdate BETWEEN ? AND ? AND t.movementtype IN ('V+', 'V-') THEN t.movementqty ELSE 0 END) AS receipts,
                    SUM(CASE WHEN t.movementdate BETWEEN ? AND ? AND t.movementtype IN ('I+', 'I-') THEN (t.movementqty * -1) ELSE 0 END) AS issues,
                    SUM(CASE WHEN t.movementdate < ? THEN t.movementqty ELSE 0 END) +
                    SUM(CASE WHEN t.movementdate BETWEEN ? AND ? AND t.movementtype IN ('V+', 'V-') THEN t.movementqty ELSE 0 END) +
                    SUM(CASE WHEN t.movementdate BETWEEN ? AND ? AND t.movementtype IN ('I+', 'I-') THEN t.movementqty ELSE 0 END) AS closing_balance
                FROM adempiere.m_transaction t
                WHERE t.ad_client_id = ?
                  AND (? = 0 OR t.m_product_id = ?)
                GROUP BY t.m_product_id, t.ad_client_id
            ),
            cost_summary AS (
                SELECT
                    t.m_product_id,
                    t.ad_client_id,
                    SUM(COALESCE(fa.amtacctdr, 0)) AS total_cost
                FROM adempiere.m_transaction t
                JOIN adempiere.fact_acct fa
                  ON fa.ad_client_id = t.ad_client_id
                 AND (
                        (t.m_inoutline_id IS NOT NULL AND fa.ad_table_id = 319 AND fa.line_id = t.m_inoutline_id)
                     OR (t.m_movementline_id IS NOT NULL AND fa.ad_table_id = 323 AND fa.line_id = t.m_movementline_id)
                     OR (t.m_inventoryline_id IS NOT NULL AND fa.ad_table_id = 321 AND fa.line_id = t.m_inventoryline_id)
                     )
                WHERE t.ad_client_id = ?
                  AND (? = 0 OR t.m_product_id = ?)
                  AND t.movementdate BETWEEN ? AND ?
                  AND t.movementtype IN ('V+', 'V-', 'I+', 'I-')
                GROUP BY t.m_product_id, t.ad_client_id
            )
            SELECT
                ts.m_product_id,
                ts.ad_client_id,
                ts.opening_balance,
                ts.receipts,
                ts.issues,
                ts.closing_balance,
                COALESCE(cs.total_cost, 0) AS total_cost
            FROM txn_summary ts
            LEFT JOIN cost_summary cs
                   ON cs.m_product_id = ts.m_product_id AND cs.ad_client_id = ts.ad_client_id
            ORDER BY ts.m_product_id
        """;

        try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
            int i = 1;
            pstmt.setTimestamp(i++, startDate);   // opening
            pstmt.setTimestamp(i++, startDate);   // receipts start
            pstmt.setTimestamp(i++, endDate);     // receipts end
            pstmt.setTimestamp(i++, startDate);   // issues start
            pstmt.setTimestamp(i++, endDate);     // issues end
            pstmt.setTimestamp(i++, startDate);   // closing part 1
            pstmt.setTimestamp(i++, startDate);   // closing part 2 start
            pstmt.setTimestamp(i++, endDate);     // closing part 2 end
            pstmt.setTimestamp(i++, startDate);   // closing part 3 start
            pstmt.setTimestamp(i++, endDate);     // closing part 3 end
            pstmt.setInt(i++, getAD_Client_ID()); // client
            pstmt.setInt(i++, mProductId);        // product param check
            pstmt.setInt(i++, mProductId);        // actual product value
            pstmt.setInt(i++, getAD_Client_ID()); // cost_summary client
            pstmt.setInt(i++, mProductId);        // cost_summary product param check
            pstmt.setInt(i++, mProductId);        // cost_summary actual product value
            pstmt.setTimestamp(i++, startDate);   // cost_summary period start
            pstmt.setTimestamp(i++, endDate);     // cost_summary period end

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String insertSql = """
                        INSERT INTO adempiere.t_transactions_report_summary (
                          ad_pinstance_id, row_type, m_transaction_id, ad_client_id, ad_org_id, isactive,
                          created, createdby, updated, updatedby, movementtype, m_locator_id, m_product_id,
                          movementdate, movementqty, m_inventoryline_id, m_movementline_id, m_inoutline_id,
                          opening_balance, receipts, issues, closing_balance, total_cost,
                          t_transactions_report_summary_uu, lineno
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                    try (PreparedStatement insertStmt = DB.prepareStatement(insertSql, get_TrxName())) {
                        insertStmt.setInt(1, pInstanceId);
                        insertStmt.setString(2, "S"); // summary row type
                        insertStmt.setObject(3, null); // m_transaction_id
                        insertStmt.setInt(4, rs.getInt("ad_client_id"));
                        insertStmt.setInt(5, 0); // ad_org_id, or use actual if known
                        insertStmt.setString(6, "Y");

                        Timestamp now = new Timestamp(System.currentTimeMillis());
                        insertStmt.setTimestamp(7, now); // created
                        insertStmt.setInt(8, getAD_User_ID()); // createdby
                        insertStmt.setTimestamp(9, now); // updated
                        insertStmt.setInt(10, getAD_User_ID()); // updatedby

                        insertStmt.setObject(11, null); // movementtype
                        insertStmt.setObject(12, null); // m_locator_id
                        insertStmt.setInt(13, rs.getInt("m_product_id"));

                        insertStmt.setObject(14, null); // movementdate
                        insertStmt.setObject(15, null); // movementqty
                        insertStmt.setObject(16, null); // m_inventoryline_id
                        insertStmt.setObject(17, null); // m_movementline_id
                        insertStmt.setObject(18, null); // m_inoutline_id

                        insertStmt.setBigDecimal(19, rs.getBigDecimal("opening_balance"));
                        insertStmt.setBigDecimal(20, rs.getBigDecimal("receipts"));
                        insertStmt.setBigDecimal(21, rs.getBigDecimal("issues"));
                        insertStmt.setBigDecimal(22, rs.getBigDecimal("closing_balance"));
                        insertStmt.setBigDecimal(23, rs.getBigDecimal("total_cost"));

                        insertStmt.setString(24, DB.getSQLValueStringEx(null, "SELECT Generate_UUID() FROM Dual"));
                        insertStmt.setInt(25, ++lineNo);

                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            log.severe("SQL Error: " + e.getMessage());
            throw e;
        }

        return "Summary report created: one row per product.";
    }
}