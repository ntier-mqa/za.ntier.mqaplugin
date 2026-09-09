-- MQA customization: "Prepared Payment" tab (AD_Tab, C_PaySelectionCheck) — conditional read-only,
-- scoped to only the new button
-- The tab has IsReadOnly='Y' (flat, unconditional) with ReadOnlyLogic=NULL, which per
-- GridTab.isReadOnly() locks every field on the tab always, regardless of row state — including the
-- Reverse Selected Payment(s) button added in 202609071500_ReversePreparePaymentSelected_postgresql.sql,
-- since GridField.isEditable() checks tab-level read-only (line ~449 of GridField.java) before any
-- field-level IsAlwaysUpdateable/ReadOnlyLogic is ever consulted.
--
-- This script:
-- 1) Sets the TAB to IsReadOnly='N', ReadOnlyLogic='@Processed@=Y' — editable exactly while
--    C_PaySelectionCheck.Processed=N (still just prepared, not yet a confirmed Payment — see
--    MPaySelectionCheck.confirmPrint()), locked again once Processed=Y.
-- 2) To avoid that also unlocking every OTHER pre-existing field on the tab (Payment Rule, Amount,
--    Discount, Write-off, Business Partner, etc. — none of which were ever meant to become hand-
--    editable by this change), it explicitly pins each of those fields' own AD_Field.IsReadOnly='Y'.
--    GridField.isEditable() checks the field's own IsReadOnly flag alongside the tab's
--    (`m_vo.tabReadOnly || m_vo.IsReadOnly`), so a field-level 'Y' keeps that field locked regardless
--    of what the tab-level logic now allows. Net effect: only the Reverse Selected Payment(s) button
--    is actually affected by the loosened tab lock; everything else behaves exactly as before.
--
-- Safe to re-run: the field lock-down step only touches rows currently IsReadOnly='N', and reports
-- how many it changed.

DO $$
DECLARE
  v_tab_id           INTEGER;
  v_our_table_id     INTEGER;
  v_our_column_id    INTEGER;
  v_locked_count     INTEGER;
BEGIN
  SELECT t.AD_Tab_ID INTO v_tab_id
  FROM AD_Tab t
  JOIN AD_Window w ON w.AD_Window_ID = t.AD_Window_ID
  WHERE w.Name = 'Payment Selection' AND t.Name = 'Prepared Payment';

  IF v_tab_id IS NULL THEN
    RAISE EXCEPTION 'Tab "Prepared Payment" on window "Payment Selection" not found';
  END IF;

  SELECT AD_Table_ID INTO v_our_table_id FROM AD_Table WHERE TableName = 'C_PaySelectionCheck';
  SELECT AD_Column_ID INTO v_our_column_id FROM AD_Column WHERE ColumnName = 'ReversePrepPaymentSel' AND AD_Table_ID = v_our_table_id;

  IF v_our_column_id IS NULL THEN
    RAISE EXCEPTION 'Column ReversePrepPaymentSel on C_PaySelectionCheck not found — has 202609071500_ReversePreparePaymentSelected_postgresql.sql run on this database?';
  END IF;

  UPDATE AD_Tab
  SET IsReadOnly = 'N', ReadOnlyLogic = '@Processed@=Y', Updated = now(), UpdatedBy = 0
  WHERE AD_Tab_ID = v_tab_id;

  UPDATE AD_Field
  SET IsReadOnly = 'Y', Updated = now(), UpdatedBy = 0
  WHERE AD_Tab_ID = v_tab_id
    AND AD_Column_ID <> v_our_column_id
    AND (IsReadOnly IS DISTINCT FROM 'Y');
  GET DIAGNOSTICS v_locked_count = ROW_COUNT;

  RAISE NOTICE 'AD_Tab % (Prepared Payment): IsReadOnly=N, ReadOnlyLogic=@Processed@=Y. Pinned % other field(s) to IsReadOnly=Y (all except AD_Column_ID %).',
      v_tab_id, v_locked_count, v_our_column_id;
END $$;

SELECT register_migration_script('202609091200_PrepPaymentTabReadOnlyLogic_postgresql.sql') FROM dual
;
