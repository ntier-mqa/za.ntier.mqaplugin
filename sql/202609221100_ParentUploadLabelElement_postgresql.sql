-- MQA customization: "Upload" column header on the SDR Child Organisation grid.
--
-- MaintainOrganisationVM.initChildOrg() renders the ZZ_Parent_Uploads Yes/No column with
--   Msg.getElement(Env.getCtx(), "ZZParentUpload")
-- which resolves to AD_Element.Name WHERE UPPER(ColumnName)='ZZPARENTUPLOAD'. This script creates
-- that element.
--
-- Why a dedicated element instead of AD_Column.Name: the grid header must read "Upload", but the
-- back office field for the same column is clearer as "Parent Uploads". AD_Column.Name drives both,
-- so a label-only element scopes the shorter wording to this grid. This mirrors ZZLegalName /
-- ZZTradeName, which the same grid already uses as label-only elements.
--
-- IMPORTANT: Msg.getElement() returns "" when the element is missing - it does not raise. Without
-- this script the column header renders BLANK rather than failing visibly. Run it before (or with)
-- the plugin deployment.
--
-- Msg caches element lookups per language (CCache on AD_Element), so after running this either
-- reset the cache or restart the application server before the new header appears.
--
-- ID comes from nextval('ad_element_sq'), the same native sequence MSequence.getNextID() uses, so
-- the record is numbered exactly as if it had been created through the client. AD_Element_Trl rows
-- are inserted for every IsSystemLanguage='Y' language - without them the label renders blank for
-- those languages, because inserting AD_* rows directly via SQL bypasses the model-layer hook that
-- normally creates translations. Trl UUIDs use md5(random())::uuid rather than uuid_generate_v4(),
-- since the application DB role's search_path may exclude the schema where uuid-ossp lives.
--
-- Safe to re-run: guarded with NOT EXISTS on ColumnName.

DO $$
DECLARE
  v_element_id  INTEGER;
  v_lang        RECORD;
BEGIN
  SELECT AD_Element_ID INTO v_element_id FROM AD_Element WHERE UPPER(ColumnName) = 'ZZPARENTUPLOAD';

  IF v_element_id IS NOT NULL THEN
    RAISE NOTICE 'AD_Element ZZParentUpload already exists (ID %) - leaving it untouched', v_element_id;
  ELSE
    v_element_id := nextval('ad_element_sq');

    IF v_element_id < 1000000 THEN
      RAISE WARNING 'Generated AD_Element_ID % is below 1,000,000 - this system''s native sequence may not be positioned past the core dictionary range.', v_element_id;
    END IF;

    INSERT INTO AD_Element (AD_Element_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        ColumnName, Name, PrintName, Description, EntityType, AD_Element_UU)
    VALUES (v_element_id, 0, 0, 'Y', now(), 0, now(), 0,
        'ZZParentUpload', 'Upload', 'Upload',
        'Grid header for the parent-uploads Yes/No choice on the SDR Child Organisation tab',
        'U', 'd929c430-2ad3-4f29-ba78-2bbbd5c59513');

    RAISE NOTICE 'Created AD_Element ZZParentUpload (ID %) with Name = Upload', v_element_id;
  END IF;

  FOR v_lang IN SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage = 'Y' LOOP
    IF NOT EXISTS (SELECT 1 FROM AD_Element_Trl
                   WHERE AD_Element_ID = v_element_id AND AD_Language = v_lang.AD_Language) THEN
      INSERT INTO AD_Element_Trl (AD_Element_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          Name, PrintName, IsTranslated, AD_Element_Trl_UU)
      VALUES (v_element_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          'Upload', 'Upload', 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END IF;
  END LOOP;

END $$;

-- Sanity check - expect exactly one row, Name = 'Upload'.
SELECT AD_Element_ID, ColumnName, Name, PrintName, EntityType
FROM   AD_Element
WHERE  UPPER(ColumnName) = 'ZZPARENTUPLOAD'
;

SELECT register_migration_script('202609221100_ParentUploadLabelElement_postgresql.sql') FROM dual
;
