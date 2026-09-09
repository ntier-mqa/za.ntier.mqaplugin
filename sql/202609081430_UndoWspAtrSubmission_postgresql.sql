-- MQA customization: Undo WSP-ATR Submission
-- Adds a toolbar button on the WSP-ATR Submitted tab (ZZ_WSP_ATR_Submitted) that resets a
-- Submitted (SU) record back to Draft (DR) and clears ZZ_DocAction, so the SDF can resubmit.
-- The button only displays when the record's current status is Submitted (SU) — see DisplayLogic
-- on the AD_Field insert below.
--
-- Requires za.ntier.mqaplugin class za.co.ntier.wsp_atr.process.WSPATRUndoSubmissionProcess
-- to be deployed before this button is used. This toolbar/window button (via this AD_Column +
-- AD_Field) is the only entry point for this action — it is not also wired into the custom
-- WspAtrSubmittedADForm ZK form.
--
-- AD_Window_UU / AD_Tab_UU below identify the existing window/tab for ZZ_WSP_ATR_Submitted
-- (confirmed via the Application Dictionary UI — this table has no scripted window/tab
-- definition elsewhere in this repo). AD_Table_ID is resolved by name and cross-checked
-- against the tab's table to guard against a mismatched window/tab being supplied.
--
-- IDs for AD_Element / AD_Process / AD_Column / AD_Field come from nextval() on this system's
-- native id sequences (ad_element_sq / ad_process_sq / ad_column_sq / ad_field_sq) — the same
-- mechanism MSequence.getNextID()/DB_PostgreSQL.getNextID() use when SYSTEM_NATIVE_SEQUENCE=Y,
-- so these IDs are generated exactly as if the records had been created through the client.
-- Review the RAISE NOTICE output after running to confirm what was created.
-- If a nextval() call fails with "relation ... does not exist", that sequence hasn't been
-- created yet for this table — create it once (e.g. via the Application Dictionary UI) and
-- re-run.
--
-- EntityType is set to 'U' (User maintained) below as a safe default — change it to your
-- project's own EntityType value first if MQA customizations are tagged with one elsewhere in
-- this system.
--
-- IsToolbarButton is set to 'B' (Both — Toolbar and Window), not 'Y' (Toolbar only), so the
-- button shows both in the tab's Grid/List toolbar and inside the single-record Detail/Form
-- view's process menu. This is a 3-value list reference (Y=Toolbar, N=Window, B=Both), not a
-- plain Y/N flag — easy to miss, since the Application Dictionary UI just labels it "Toolbar
-- Button".
--
-- Also inserts AD_Element_Trl / AD_Column_Trl / AD_Process_Trl / AD_Field_Trl rows for every
-- AD_Language flagged IsSystemLanguage='Y'. Without these, the Column/Process/Element/Field
-- name lookups render BLANK in the client for any such language — inserting AD_* dictionary
-- rows directly via SQL bypasses the model-layer hook that normally creates these translation
-- rows automatically when a record is created through the client UI. UUIDs for these rows are
-- generated inline via md5(random())::uuid rather than uuid_generate_v4(), since on at least
-- one environment this script has run against, the application DB role's search_path excludes
-- the public schema where uuid-ossp lives, causing uuid_generate_v4() to fail for that role.

DO $$
DECLARE
  v_element_id  INTEGER;
  v_process_id  INTEGER;
  v_column_id   INTEGER;
  v_field_id    INTEGER;
  v_table_id    INTEGER;
  v_tab_id      INTEGER;
  v_tab_table_id INTEGER;
  v_seqno       INTEGER;
  v_lang        RECORD;
BEGIN
  SELECT AD_Table_ID INTO v_table_id
  FROM AD_Table
  WHERE TableName = 'ZZ_WSP_ATR_Submitted';

  IF v_table_id IS NULL THEN
    RAISE EXCEPTION 'Table ZZ_WSP_ATR_Submitted not found';
  END IF;

  SELECT t.AD_Tab_ID, t.AD_Table_ID INTO v_tab_id, v_tab_table_id
  FROM AD_Tab t
  JOIN AD_Window w ON w.AD_Window_ID = t.AD_Window_ID
  WHERE t.AD_Tab_UU = 'b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65'
    AND w.AD_Window_UU = '406eaf0a-7d74-4942-9429-07f09ffeed85';

  IF v_tab_id IS NULL THEN
    RAISE EXCEPTION 'Tab AD_Tab_UU=b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65 on window AD_Window_UU=406eaf0a-7d74-4942-9429-07f09ffeed85 not found';
  END IF;

  IF v_tab_table_id <> v_table_id THEN
    RAISE EXCEPTION 'Tab AD_Tab_ID=% is based on AD_Table_ID=%, but ZZ_WSP_ATR_Submitted is AD_Table_ID=% — wrong window/tab supplied',
        v_tab_id, v_tab_table_id, v_table_id;
  END IF;

  v_element_id := nextval('ad_element_sq');
  v_process_id := nextval('ad_process_sq');
  v_column_id  := nextval('ad_column_sq');
  v_field_id   := nextval('ad_field_sq');

  IF v_element_id < 1000000 OR v_process_id < 1000000 OR v_column_id < 1000000 OR v_field_id < 1000000 THEN
    RAISE WARNING 'One or more generated IDs are below 1,000,000 (element=%, process=%, column=%, field=%) — this system''s native sequences may not be positioned past the core dictionary range.',
        v_element_id, v_process_id, v_column_id, v_field_id;
  END IF;

  SELECT COALESCE(MAX(SeqNo), 0) + 10 INTO v_seqno FROM AD_Field WHERE AD_Tab_ID = v_tab_id;

  INSERT INTO AD_Element (AD_Element_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
      ColumnName, Name, PrintName, Description, EntityType, AD_Element_UU)
  VALUES (v_element_id, 0, 0, 'Y', now(), 0, now(), 0,
      'ZZ_UndoSubmission', 'Undo Submission', 'Undo Submission',
      'Reset this WSP-ATR submission back to Draft', 'U', '00a9331d-be2f-4a40-bc45-d480ca4bf961');

  INSERT INTO AD_Process (AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
      Name, Description, Help, IsReport, Value, IsDirectPrint, Classname, AccessLevel, EntityType,
      Statistic_Count, Statistic_Seconds, IsBetaFunctionality, ShowHelp, CopyFromProcess, AD_Process_UU,
      AllowMultipleExecution)
  VALUES (v_process_id, 0, 0, 'Y', now(), 0, now(), 0,
      'Undo WSP-ATR Submission', 'Reset a Submitted WSP-ATR record back to Draft',
      'Resets ZZ_DocStatus from Submitted (SU) back to Draft (DR) and clears ZZ_DocAction, so the SDF can resubmit. Only records currently in status Submitted are affected.',
      'N', 'ZZ_UndoWspAtrSubmission', 'N', 'za.co.ntier.wsp_atr.process.WSPATRUndoSubmissionProcess', '3', 'U',
      0, 0, 'N', 'Y', 'N', '39152c75-8af7-418a-be3f-522044a74585', 'N');

  INSERT INTO AD_Column (AD_Column_ID, Version, Name, AD_Table_ID, ColumnName, FieldLength, IsKey, IsParent,
      IsMandatory, IsTranslated, IsIdentifier, SeqNo, IsEncrypted, AD_Reference_ID, AD_Client_ID, AD_Org_ID,
      IsActive, Created, CreatedBy, Updated, UpdatedBy, AD_Element_ID, IsUpdateable, AD_Process_ID,
      IsSelectionColumn, EntityType, IsSyncDatabase, IsAlwaysUpdateable, IsAutocomplete, IsAllowLogging,
      AD_Column_UU, IsAllowCopy, SeqNoSelection, IsToolbarButton, IsSecure, FKConstraintType, IsHtml)
  VALUES (v_column_id, 0, 'Undo Submission', v_table_id, 'ZZ_UndoSubmission', 1, 'N', 'N',
      'N', 'N', 'N', 0, 'N', 28, 0, 0,
      'Y', now(), 0, now(), 0, v_element_id, 'Y', v_process_id,
      'N', 'U', 'N', 'Y', 'N', 'Y',
      '33f384f9-aa2f-49ba-b91a-2fc9fadf75e1', 'Y', 0, 'B', 'N', 'N', 'N');

  EXECUTE 'ALTER TABLE ZZ_WSP_ATR_Submitted ADD COLUMN ZZ_UndoSubmission CHAR(1) DEFAULT NULL';

  INSERT INTO AD_Field (AD_Field_ID, Name, AD_Tab_ID, AD_Column_ID, IsDisplayed, DisplayLogic, DisplayLength,
      SeqNo, IsSameLine, IsHeading, IsFieldOnly, IsEncrypted, AD_Client_ID, AD_Org_ID, IsActive, Created,
      CreatedBy, Updated, UpdatedBy, IsReadOnly, IsCentrallyMaintained, EntityType, AD_Field_UU,
      IsDisplayedGrid, SeqNoGrid, XPosition, ColumnSpan)
  VALUES (v_field_id, 'Undo Submission', v_tab_id, v_column_id, 'Y',
      '@ZZ_DocStatus@=SU', 1,
      v_seqno, 'N', 'N', 'N', 'N', 0, 0, 'Y', now(),
      0, now(), 0, 'N', 'Y', 'U', 'b9cbcf64-b83f-4cb5-b7c5-7c7deedc3fcc',
      'Y', v_seqno, 1, 1);

  FOR v_lang IN SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage='Y' LOOP
    INSERT INTO AD_Element_Trl (AD_Element_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, PrintName, IsTranslated, AD_Element_Trl_UU)
    VALUES (v_element_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
        'Undo Submission', 'Undo Submission', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);

    INSERT INTO AD_Column_Trl (AD_Column_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Column_Trl_UU)
    VALUES (v_column_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
        'Undo Submission', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);

    INSERT INTO AD_Process_Trl (AD_Process_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Process_Trl_UU)
    VALUES (v_process_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
        'Undo WSP-ATR Submission', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);

    INSERT INTO AD_Field_Trl (AD_Field_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Field_Trl_UU)
    VALUES (v_field_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
        'Undo Submission', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
  END LOOP;

  RAISE NOTICE 'Created AD_Element %, AD_Process %, AD_Column % (table %), AD_Field % (tab %)',
      v_element_id, v_process_id, v_column_id, v_table_id, v_field_id, v_tab_id;
END $$;

SELECT register_migration_script('202609081430_UndoWspAtrSubmission_postgresql.sql') FROM dual
;
