-- MQA customization: Reverse Prepare Payment (Selected) — narrow DisplayLogic
-- The field's DisplayLogic evaluates against the single current/focused row in the "Prepared
-- Payment" grid, not against every row in a multi-selection. With the original
-- '@Processed@=Y & @C_Payment_ID@=0', if the focused row already has a Payment (C_Payment_ID<>0)
-- but other selected rows don't, the button wouldn't show at all — even though part of the
-- selection is reversible. Narrowing this to '@Processed@=Y' keeps the button available whenever
-- any row in the tab could plausibly be reversible; the actual per-row enforcement already happens
-- in za.ntier.process.PaySelectionCheckReverseSelection, which independently skips (and logs) any
-- selected row with C_Payment_ID<>0 rather than reversing it.
--
-- Resolves the field by ColumnName ('ReversePrepPaymentSel' on C_PaySelectionCheck), same as
-- 202609091000_RevPrepPaySelAddTrl_postgresql.sql — does not need the numeric AD_Field_ID.

DO $$
DECLARE
  v_table_id  INTEGER;
  v_column_id INTEGER;
  v_field_id  INTEGER;
BEGIN
  SELECT AD_Table_ID INTO v_table_id FROM AD_Table WHERE TableName = 'C_PaySelectionCheck';
  IF v_table_id IS NULL THEN
    RAISE EXCEPTION 'Table C_PaySelectionCheck not found';
  END IF;

  SELECT AD_Column_ID INTO v_column_id FROM AD_Column WHERE ColumnName = 'ReversePrepPaymentSel' AND AD_Table_ID = v_table_id;
  IF v_column_id IS NULL THEN
    RAISE EXCEPTION 'Column ReversePrepPaymentSel on C_PaySelectionCheck not found';
  END IF;

  SELECT AD_Field_ID INTO v_field_id FROM AD_Field WHERE AD_Column_ID = v_column_id;
  IF v_field_id IS NULL THEN
    RAISE EXCEPTION 'Field for column ReversePrepPaymentSel not found';
  END IF;

  UPDATE AD_Field
  SET DisplayLogic = '@Processed@=Y', Updated = now(), UpdatedBy = 0
  WHERE AD_Field_ID = v_field_id;

  RAISE NOTICE 'Updated DisplayLogic on AD_Field % to @Processed@=Y', v_field_id;
END $$;

SELECT register_migration_script('202609091030_RevPrepPaySelDispLogic_postgresql.sql') FROM dual
;
