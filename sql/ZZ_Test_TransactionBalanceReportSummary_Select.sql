-- Standalone test query for za.ntier.process.TransactionBalanceReportSummary.
-- Placeholders filled with sample values: AD_Client_ID=1000000, all products, period 2026-01-01..2026-08-31.
-- Edit the client id, product id and dates as needed, then run directly in psql.

WITH txn_summary AS (
    SELECT
        t.m_product_id,
        t.ad_client_id,
        SUM(CASE WHEN t.movementdate < '2026-01-01' AND t.movementtype IN ('V+', 'V-', 'I+', 'I-') THEN t.movementqty ELSE 0 END) AS opening_balance,
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('V+', 'V-') THEN t.movementqty ELSE 0 END) AS receipts,
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('I+', 'I-') THEN (t.movementqty * -1) ELSE 0 END) AS issues,
        SUM(CASE WHEN t.movementdate < '2026-01-01' AND t.movementtype IN ('V+', 'V-', 'I+', 'I-') THEN t.movementqty ELSE 0 END) +
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('V+', 'V-') THEN t.movementqty ELSE 0 END) +
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('I+', 'I-') THEN t.movementqty ELSE 0 END) AS closing_balance
    FROM adempiere.m_transaction t
    WHERE t.ad_client_id = 1000000
      AND (t.m_product_id = 1000409)
    GROUP BY t.m_product_id, t.ad_client_id
),
cost_summary AS (
    SELECT
        t.m_product_id,
        t.ad_client_id,
        SUM(CASE WHEN t.movementdate < '2026-01-01' AND t.movementtype IN ('V+', 'V-', 'I+', 'I-') THEN fa.amtacctdr ELSE 0 END) AS opening_cost,
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('V+', 'V-') THEN fa.amtacctdr ELSE 0 END) AS receipt_cost,
        SUM(CASE WHEN t.movementdate BETWEEN '2026-01-01' AND '2026-08-31' AND t.movementtype IN ('I+', 'I-') THEN fa.amtacctdr ELSE 0 END) AS issue_cost
    FROM adempiere.m_transaction t
    JOIN adempiere.fact_acct fa
      ON fa.ad_client_id = t.ad_client_id
     AND (
            (t.m_inoutline_id IS NOT NULL AND fa.ad_table_id = 319 AND fa.line_id = t.m_inoutline_id)
         OR (t.m_movementline_id IS NOT NULL AND fa.ad_table_id = 323 AND fa.line_id = t.m_movementline_id)
         OR (t.m_inventoryline_id IS NOT NULL AND fa.ad_table_id = 321 AND fa.line_id = t.m_inventoryline_id)
         )
    WHERE t.ad_client_id = 1000000
      AND ( t.m_product_id = 1000409)
    GROUP BY t.m_product_id, t.ad_client_id
)
SELECT
    ts.m_product_id,
    ts.ad_client_id,
    ts.opening_balance,
    ts.receipts,
    ts.issues,
    ts.closing_balance,
    COALESCE(cs.opening_cost, 0) + COALESCE(cs.receipt_cost, 0) - COALESCE(cs.issue_cost, 0) AS total_cost
FROM txn_summary ts
LEFT JOIN cost_summary cs
       ON cs.m_product_id = ts.m_product_id AND cs.ad_client_id = ts.ad_client_id
ORDER BY ts.m_product_id;
