SET SQLBLANKLINES ON
SET DEFINE OFF

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
-- IDs for AD_Element / AD_Process / AD_Column / AD_Field come from NEXTVAL on this system's
-- native id sequences (AD_ELEMENT_SQ / AD_PROCESS_SQ / AD_COLUMN_SQ / AD_FIELD_SQ) — the same
-- mechanism MSequence.getNextID()/DB_Oracle.getNextID() use when SYSTEM_NATIVE_SEQUENCE=Y, so
-- these IDs are generated exactly as if the records had been created through the client.
-- Check the DBMS_OUTPUT after running to confirm what was created (run SET SERVEROUTPUT ON
-- before executing this script if using SQL*Plus/SQLcl).
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
-- rows automatically when a record is created through the client UI. UUID-shaped strings for
-- these rows are generated inline from SYS_GUID() rather than relying on any extension.

DECLARE
  v_element_id   NUMBER;
  v_process_id   NUMBER;
  v_column_id    NUMBER;
  v_field_id     NUMBER;
  v_table_id     NUMBER;
  v_tab_id       NUMBER;
  v_tab_table_id NUMBER;
  v_seqno        NUMBER;

  FUNCTION new_uuid RETURN VARCHAR2 IS
  BEGIN
    RETURN LOWER(REGEXP_REPLACE(RAWTOHEX(SYS_GUID()), '(.{8})(.{4})(.{4})(.{4})(.{12})', '\1-\2-\3-\4-\5'));
  END;
BEGIN
  SELECT AD_Table_ID INTO v_table_id
  FROM AD_Table
  WHERE TableName = 'ZZ_WSP_ATR_Submitted';

  SELECT t.AD_Tab_ID, t.AD_Table_ID INTO v_tab_id, v_tab_table_id
  FROM AD_Tab t
  JOIN AD_Window w ON w.AD_Window_ID = t.AD_Window_ID
  WHERE t.AD_Tab_UU = 'b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65'
    AND w.AD_Window_UU = '406eaf0a-7d74-4942-9429-07f09ffeed85';

  IF v_tab_table_id <> v_table_id THEN
    RAISE_APPLICATION_ERROR(-20002,
      'Tab AD_Tab_ID=' || v_tab_id || ' is based on AD_Table_ID=' || v_tab_table_id ||
      ', but ZZ_WSP_ATR_Submitted is AD_Table_ID=' || v_table_id || ' - wrong window/tab supplied');
  END IF;

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
      'ZZ_UndoSubmission', 'Undo Submission', 'Undo Submission',
      'Reset this WSP-ATR submission back to Draft', 'U', '00a9331d-be2f-4a40-bc45-d480ca4bf961');

  INSERT INTO AD_Process (AD_Process_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
      Name, Description, Help, IsReport, Value, IsDirectPrint, Classname, AccessLevel, EntityType,
      Statistic_Count, Statistic_Seconds, IsBetaFunctionality, ShowHelp, CopyFromProcess, AD_Process_UU,
      AllowMultipleExecution)
  VALUES (v_process_id, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
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
      'Y', SYSDATE, 0, SYSDATE, 0, v_element_id, 'Y', v_process_id,
      'N', 'U', 'N', 'Y', 'N', 'Y',
      '33f384f9-aa2f-49ba-b91a-2fc9fadf75e1', 'Y', 0, 'B', 'N', 'N', 'N');

  EXECUTE IMMEDIATE 'ALTER TABLE ZZ_WSP_ATR_Submitted ADD ZZ_UndoSubmission CHAR(1) DEFAULT NULL';

  INSERT INTO AD_Field (AD_Field_ID, Name, AD_Tab_ID, AD_Column_ID, IsDisplayed, DisplayLogic, DisplayLength,
      SeqNo, IsSameLine, IsHeading, IsFieldOnly, IsEncrypted, AD_Client_ID, AD_Org_ID, IsActive, Created,
      CreatedBy, Updated, UpdatedBy, IsReadOnly, IsCentrallyMaintained, EntityType, AD_Field_UU,
      IsDisplayedGrid, SeqNoGrid, XPosition, ColumnSpan)
  VALUES (v_field_id, 'Undo Submission', v_tab_id, v_column_id, 'Y',
      '@ZZ_DocStatus@=SU', 1,
      v_seqno, 'N', 'N', 'N', 'N', 0, 0, 'Y', SYSDATE,
      0, SYSDATE, 0, 'N', 'Y', 'U', 'b9cbcf64-b83f-4cb5-b7c5-7c7deedc3fcc',
      'Y', v_seqno, 1, 1);

  FOR v_lang IN (SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage='Y') LOOP
    INSERT INTO AD_Element_Trl (AD_Element_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, PrintName, IsTranslated, AD_Element_Trl_UU)
    VALUES (v_element_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Undo Submission', 'Undo Submission', 'Y', new_uuid());

    INSERT INTO AD_Column_Trl (AD_Column_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Column_Trl_UU)
    VALUES (v_column_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Undo Submission', 'Y', new_uuid());

    INSERT INTO AD_Process_Trl (AD_Process_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Process_Trl_UU)
    VALUES (v_process_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Undo WSP-ATR Submission', 'Y', new_uuid());

    INSERT INTO AD_Field_Trl (AD_Field_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Name, IsTranslated, AD_Field_Trl_UU)
    VALUES (v_field_id, v_lang.AD_Language, 0, 0, 'Y', SYSDATE, 0, SYSDATE, 0,
        'Undo Submission', 'Y', new_uuid());
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('Created AD_Element ' || v_element_id || ', AD_Process ' || v_process_id ||
      ', AD_Column ' || v_column_id || ' (table ' || v_table_id || '), AD_Field ' || v_field_id ||
      ' (tab ' || v_tab_id || ')');

  COMMIT;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20001,
      'Table ZZ_WSP_ATR_Submitted, or tab AD_Tab_UU=b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65 on window AD_Window_UU=406eaf0a-7d74-4942-9429-07f09ffeed85, not found');
END;
/

SELECT register_migration_script('202609081430_UndoWspAtrSubmission_oracle.sql') FROM dual
;
