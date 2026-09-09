SET SQLBLANKLINES ON
SET DEFINE OFF

-- MQA customization: Reverse Prepare Payment (Selected)
-- Adds a toolbar button on the "Prepared Payment" tab (C_PaySelectionCheck) that reverses only the
-- row(s) selected in the grid, instead of the whole Payment Selection header
-- (the existing core button, AD_Process 200136 / IDEMPIERE-5170, always reverses every prepared
-- payment under the header and aborts entirely if any one of them already has a Payment).
--
-- Requires za.ntier.mqaplugin class za.ntier.process.PaySelectionCheckReverseSelection to be
-- deployed before this button is used.
--
-- IDs for AD_Element / AD_Process / AD_Column / AD_Field come from NEXTVAL on this system's native
-- id sequences (AD_ELEMENT_SQ / AD_PROCESS_SQ / AD_COLUMN_SQ / AD_FIELD_SQ) — the same mechanism
-- MSequence.getNextID()/DB_Oracle.getNextID() use when SYSTEM_NATIVE_SEQUENCE=Y, so these IDs are
-- generated exactly as if the records had been created through the client. AD_Table_ID / AD_Tab_ID are
-- resolved by name. Check the DBMS_OUTPUT after running to confirm what was created
-- (run SET SERVEROUTPUT ON before executing this script if using SQL*Plus/SQLcl).
--
-- EntityType is set to 'U' (User maintained) below as a safe default — change it to your project's
-- own EntityType value first if MQA customizations are tagged with one elsewhere in this system.
--
-- The field has no DisplayLogic — the button is always shown. IsToolbarButton is 'Y' (Toolbar only,
-- not 'B'/Both), so this field never appears in the single-record Detail/Form view anyway; it only
-- ever renders as the grid toolbar button, whose DisplayLogic (if set) would evaluate against a single
-- current/focused row, not every row in a multi-selection — an unreliable signal for a multi-select
-- action, since a mix of already-confirmed (Processed=Y, C_Payment_ID set — see
-- MPaySelectionCheck.confirmPrint()) and still-reversible (Processed=N) rows under one header is
-- normal, not an edge case. The real guard is server-side: za.ntier.process.PaySelectionCheckReverseSelection
-- independently skips (and logs) any selected row with C_Payment_ID<>0 rather than reversing it.
--
-- Also inserts AD_Element_Trl / AD_Column_Trl / AD_Process_Trl / AD_Field_Trl rows for every
-- AD_Language flagged IsSystemLanguage='Y'. Without these, the Column/Process/Element/Field name
-- lookups render BLANK in the client for any such language — inserting AD_* dictionary rows directly
-- via SQL bypasses the model-layer hook that normally creates these translation rows automatically
-- when a record is created through the client UI. UUID-shaped strings for these rows are generated
-- inline from SYS_GUID() rather than relying on any extension.

DECLARE
  v_element_id  NUMBER;
  v_process_id  NUMBER;
  v_column_id   NUMBER;
  v_field_id    NUMBER;
  v_table_id    NUMBER;
  v_tab_id      NUMBER;
  v_seqno       NUMBER;

  FUNCTION new_uuid RETURN VARCHAR2 IS
  BEGIN
    RETURN LOWER(REGEXP_REPLACE(RAWTOHEX(SYS_GUID()), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'));
  END;
BEGIN
  SELECT AD_Table_ID INTO v_table_id
  FROM AD_Table
  WHERE TableName = 'C_PaySelectionCheck';

  SELECT t.AD_Tab_ID INTO v_tab_id
  FROM AD_Tab t
  JOIN AD_Window w ON w.AD_Window_ID = t.AD_Window_ID
  WHERE w.Name = 'Payment Selection'
    AND t.Name = 'Prepared Payment';

  SELECT AD_ELEMENT_SQ.NEXTVAL INTO v_element_id FROM DUAL;
  SELECT AD_PROCESS_SQ.NEXTVAL INTO v_process_id FROM DUAL;
  SELECT AD_COLUMN_SQ.NEXTVAL INTO v_column_id FROM DUAL;
  SELECT AD_FIELD_SQ.NEXTVAL INTO v_field_id FROM DUAL;

  IF v_element_id < 1000000 OR v_process_id < 1000000 OR v_column_id < 1000000 OR v_field_id < 1000000 THEN
    DBMS_OUTPUT.PUT_LINE('WARNING: one or more generated IDs are below 1,000,000 (element=' || v_element_id ||
      ', process=' || v_process_id || ', column=' || v_column_id || ', field=' || v_field_id ||
      ') - this system''s native sequences may not be positioned past the core dictionary range.');
  END IF;

  SELECT NVL(MAX(SeqNo), 0) + 10 INTO v_seqno FROM AD_Field WHERE AD_Tab_ID = v_tab_id;

  INSERT INTO AD_Element (AD_Element_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
      ColumnName, Name, PrintName, Description, EntityType, AD_Element_UU)
  VALUES (v_element_id, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
      'ReversePrepPaymentSel', 'Reverse Selected Payment(s)', 'Reverse Selected Payment(s)',
      'Reverse only the selected Prepared Payment row(s)', 'U', 'b2ad6942-4183-4cdd-823e-64a70d5965a8');

  INSERT INTO AD_Process (AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
      Name, Description, Help, IsReport, Value, IsDirectPrint, Classname, AccessLevel, EntityType,
      Statistic_Count, Statistic_Seconds, IsBetaFunctionality, ShowHelp, CopyFromProcess, AD_Process_UU,
      AllowMultipleExecution)
  VALUES (v_process_id, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
      'Reverse Prepare Payment (Selected)', 'Reverse Prepare Payment for the selected row(s) only',
      'Deletes the selected Prepared Payment row(s) and resets the linked Payment Selection Line(s), without touching the rest of the batch',
      'N', 'ReversePrepPaymentSel', 'N', 'za.ntier.process.PaySelectionCheckReverseSelection', '3', 'U',
      0, 0, 'N', 'Y', 'N', '799caad3-19af-4ee9-aa5a-c2a9f4cb078f', 'N');

  INSERT INTO AD_Column (AD_Column_ID, Version, Name, AD_Table_ID, ColumnName, FieldLength, IsKey, IsParent,
      IsMandatory, IsTranslated, IsIdentifier, SeqNo, IsEncrypted, AD_Reference_ID, AD_Client_ID, AD_Org_ID,
      IsActive, Created, CreatedBy, Updated, UpdatedBy, AD_Element_ID, IsUpdateable, AD_Process_ID,
      IsSelectionColumn, EntityType, IsSyncDatabase, IsAlwaysUpdateable, IsAutocomplete, IsAllowLogging,
      AD_Column_UU, IsAllowCopy, SeqNoSelection, IsToolbarButton, IsSecure, FKConstraintType, IsHtml)
  VALUES (v_column_id, 0, 'Reverse Selected Payment(s)', v_table_id, 'ReversePrepPaymentSel', 1, 'N', 'N',
      'N', 'N', 'N', 0, 'N', 28, 0, 0,
      'Y', SYSDATE, 0, SYSDATE, 0, v_element_id, 'Y', v_process_id,
      'N', 'U', 'N', 'Y', 'N', 'Y',
      '585986ba-9ad1-4f22-96a2-77cf3433aa4c', 'Y', 0, 'Y', 'N', 'N', 'N');

  EXECUTE IMMEDIATE 'ALTER TABLE C_PaySelectionCheck ADD ReversePrepPaymentSel CHAR(1) DEFAULT NULL';

  INSERT INTO AD_Field (AD_Field_ID, Name, AD_Tab_ID, AD_Column_ID, IsDisplayed, DisplayLogic, DisplayLength,
      SeqNo, IsSameLine, IsHeading, IsFieldOnly, IsEncrypted, AD_Client_ID, AD_Org_ID, IsActive, Created,
      CreatedBy, Updated, UpdatedBy, IsReadOnly, IsCentrallyMaintained, EntityType, AD_Field_UU,
      IsDisplayedGrid, SeqNoGrid, XPosition, ColumnSpan)
  VALUES (v_field_id, 'Reverse Selected Payment(s)', v_tab_id, v_column_id, 'Y',
      NULL, 1,
      v_seqno, 'N', 'N', 'N', 'N', 0, 0, 'Y', SYSDATE,
      0, SYSDATE, 0, 'N', 'Y', 'U', 'a5ae2c7d-56ac-4033-963b-5470534aabc2',
      'Y', v_seqno, 1, 1);

  FOR v_lang IN (SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage='Y') LOOP
    INSERT INTO AD_Element_Trl (AD_Element_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, PrintName, IsTranslated, AD_Element_Trl_UU)
    VALUES (v_element_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Reverse Selected Payment(s)', 'Reverse Selected Payment(s)', 'Y', new_uuid());

    INSERT INTO AD_Column_Trl (AD_Column_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Column_Trl_UU)
    VALUES (v_column_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Reverse Selected Payment(s)', 'Y', new_uuid());

    INSERT INTO AD_Process_Trl (AD_Process_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Process_Trl_UU)
    VALUES (v_process_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Reverse Prepare Payment (Selected)', 'Y', new_uuid());

    INSERT INTO AD_Field_Trl (AD_Field_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Field_Trl_UU)
    VALUES (v_field_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Reverse Selected Payment(s)', 'Y', new_uuid());
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('Created AD_Element ' || v_element_id || ', AD_Process ' || v_process_id ||
      ', AD_Column ' || v_column_id || ' (table ' || v_table_id || '), AD_Field ' || v_field_id ||
      ' (tab ' || v_tab_id || ')');

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20001,
      'Table C_PaySelectionCheck, or tab "Prepared Payment" on window "Payment Selection", not found');
END;
/

SELECT register_migration_script('202609071500_ReversePreparePaymentSelected_oracle.sql') FROM dual
;
