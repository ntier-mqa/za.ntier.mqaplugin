-- Adds the Total_Cost column used by za.ntier.process.TransactionBalanceReportSummary.
-- Populated from the posted DR amount (Fact_Acct.AmtAcctDr) for the M_InOut, M_Movement or
-- M_Inventory document line that generated each M_Transaction row, for transactions falling
-- within the report's date range.

ALTER TABLE adempiere.t_transactions_report_summary
    ADD COLUMN IF NOT EXISTS total_cost NUMERIC DEFAULT NULL;
