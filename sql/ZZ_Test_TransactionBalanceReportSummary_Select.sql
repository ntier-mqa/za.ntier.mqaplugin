-- Standalone test query for za.ntier.process.TransactionBalanceReportSummary.
-- Placeholders filled with sample values: AD_Client_ID=1000000, all products, period 2026-01-01..2026-08-31.
-- Total Cost = closing balance qty * current FIFO cost price (M_Cost, M_CostElement.CostingMethod = 'F').
-- If no matching M_Cost row exists for the product, total_cost is NULL (blank).
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
      AND (0 = 0 OR t.m_product_id = 0)
    GROUP BY t.m_product_id, t.ad_client_id
),
cost_lookup AS (
    SELECT DISTINCT ON (mc.ad_client_id, mc.m_product_id)
        mc.ad_client_id,
        mc.m_product_id,
        mc.currentcostprice
    FROM adempiere.m_cost mc
    JOIN adempiere.m_costelement ce ON ce.m_costelement_id = mc.m_costelement_id
    WHERE ce.costingmethod = 'F'
      AND mc.isactive = 'Y'
      AND mc.ad_client_id = 1000000
      AND (0 = 0 OR mc.m_product_id = 0)
    ORDER BY mc.ad_client_id, mc.m_product_id, mc.updated DESC
)
SELECT
    ts.m_product_id,
    ts.ad_client_id,
    ts.opening_balance,
    ts.receipts,
    ts.issues,
    ts.closing_balance,
    ts.closing_balance * cl.currentcostprice AS total_cost
FROM txn_summary ts
LEFT JOIN cost_lookup cl
       ON cl.m_product_id = ts.m_product_id AND cl.ad_client_id = ts.ad_client_id
ORDER BY ts.m_product_id;
