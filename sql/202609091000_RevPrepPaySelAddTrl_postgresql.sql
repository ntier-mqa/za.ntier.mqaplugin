-- MQA customization: Reverse Prepare Payment (Selected) — backfill translations
-- 202609071500_ReversePreparePaymentSelected_postgresql.sql already ran against this database
-- before it was updated to insert AD_*_Trl rows, so the AD_Element/AD_Process/AD_Column/AD_Field
-- records it created exist without translations. This script finds those existing records (by the
-- unique ColumnName/Value they were created with — it does NOT call nextval(), which would create
-- new duplicate dictionary entries) and inserts the missing AD_Element_Trl / AD_Column_Trl /
-- AD_Process_Trl / AD_Field_Trl rows for every AD_Language flagged IsSystemLanguage='Y'.
--
-- Safe to re-run: each insert is guarded with NOT EXISTS on (id, language).

DO $$
DECLARE
  v_element_id  INTEGER;
  v_process_id  INTEGER;
  v_column_id   INTEGER;
  v_field_id    INTEGER;
  v_table_id    INTEGER;
  v_lang        RECORD;
BEGIN
  SELECT AD_Table_ID INTO v_table_id FROM AD_Table WHERE TableName = 'C_PaySelectionCheck';
  IF v_table_id IS NULL THEN
    RAISE EXCEPTION 'Table C_PaySelectionCheck not found';
  END IF;

  SELECT AD_Element_ID INTO v_element_id FROM AD_Element WHERE ColumnName = 'ReversePrepPaymentSel';
  SELECT AD_Process_ID INTO v_process_id FROM AD_Process WHERE Value = 'ReversePrepPaymentSel';
  SELECT AD_Column_ID INTO v_column_id FROM AD_Column WHERE ColumnName = 'ReversePrepPaymentSel' AND AD_Table_ID = v_table_id;
  SELECT AD_Field_ID INTO v_field_id FROM AD_Field WHERE AD_Column_ID = v_column_id;

  IF v_element_id IS NULL OR v_process_id IS NULL OR v_column_id IS NULL OR v_field_id IS NULL THEN
    RAISE EXCEPTION 'Could not resolve existing records (element=%, process=%, column=%, field=%) — has 202609071500_ReversePreparePaymentSelected_postgresql.sql actually run on this database?',
        v_element_id, v_process_id, v_column_id, v_field_id;
  END IF;

  FOR v_lang IN SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage='Y' LOOP
    IF NOT EXISTS (SELECT 1 FROM AD_Element_Trl WHERE AD_Element_ID = v_element_id AND AD_Language = v_lang.AD_Language) THEN
      INSERT INTO AD_Element_Trl (AD_Element_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          Name, PrintName, IsTranslated, AD_Element_Trl_UU)
      VALUES (v_element_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          'Reverse Selected Payment(s)', 'Reverse Selected Payment(s)', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM AD_Column_Trl WHERE AD_Column_ID = v_column_id AND AD_Language = v_lang.AD_Language) THEN
      INSERT INTO AD_Column_Trl (AD_Column_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          Name, IsTranslated, AD_Column_Trl_UU)
      VALUES (v_column_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          'Reverse Selected Payment(s)', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM AD_Process_Trl WHERE AD_Process_ID = v_process_id AND AD_Language = v_lang.AD_Language) THEN
      INSERT INTO AD_Process_Trl (AD_Process_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          Name, IsTranslated, AD_Process_Trl_UU)
      VALUES (v_process_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          'Reverse Prepare Payment (Selected)', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM AD_Field_Trl WHERE AD_Field_ID = v_field_id AND AD_Language = v_lang.AD_Language) THEN
      INSERT INTO AD_Field_Trl (AD_Field_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          Name, IsTranslated, AD_Field_Trl_UU)
      VALUES (v_field_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          'Reverse Selected Payment(s)', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END IF;
  END LOOP;

  RAISE NOTICE 'Backfilled translations for AD_Element %, AD_Process %, AD_Column %, AD_Field %',
      v_element_id, v_process_id, v_column_id, v_field_id;
END $$;

SELECT register_migration_script('202609091000_RevPrepPaySelAddTrl_postgresql.sql') FROM dual
;
