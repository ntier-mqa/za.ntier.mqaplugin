-- MQA customization: activate the ZZ_Parent_Uploads rule on ZZOrganisationLinkage.
--
-- ZZ_Parent_Uploads says whether a parent organisation uploads and submits the WSP/ATR on a
-- given child's behalf. The WSP/ATR upload code now filters on it in four places
-- (WspAtrUploadsADForm.isParentOrganisation / .anyChildHasImportedStatus,
-- WspAtrSubmittedADForm.rebuildSubLevyOrgLinks, WspAtrUploadsService.isChildWithParentUploadsEnabled)
-- and everywhere reads NULL as 'N'.
--
-- That makes this script a HARD PREREQUISITE for the code release: every existing linkage row
-- has ZZ_Parent_Uploads = NULL, so deploying the code without backfilling would instantly drop
-- every child out of its parent's consolidation. Backfilling to 'Y' preserves exactly the
-- behaviour that is live today (all linked children are parent-managed) and lets SDFs opt
-- individual children OUT from there.
--
-- Run ZZ_Verify_Parent_Uploads_Config.sql first to confirm the physical column exists.
-- Run this BEFORE deploying the plugin.

DO $$
DECLARE
  v_table_id    INTEGER;
  v_column_id   INTEGER;
  v_col_exists  BOOLEAN;
  v_backfilled  INTEGER;
  v_cleaned     INTEGER;
BEGIN
  ----------------------------------------------------------------------------------
  -- 0. Guards
  ----------------------------------------------------------------------------------
  SELECT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'zzorganisationlinkage' AND column_name = 'zz_parent_uploads'
  ) INTO v_col_exists;

  IF NOT v_col_exists THEN
    RAISE EXCEPTION 'Column zzorganisationlinkage.zz_parent_uploads does not exist. '
      'Sync the AD column to the database (Table and Column -> Synchronize Column) before running this script.';
  END IF;

  SELECT AD_Table_ID INTO v_table_id FROM AD_Table WHERE TableName = 'ZZOrganisationLinkage';
  IF v_table_id IS NULL THEN
    RAISE EXCEPTION 'Table ZZOrganisationLinkage not found in AD_Table';
  END IF;

  SELECT AD_Column_ID INTO v_column_id
  FROM   AD_Column
  WHERE  ColumnName = 'ZZ_Parent_Uploads' AND AD_Table_ID = v_table_id;
  IF v_column_id IS NULL THEN
    RAISE EXCEPTION 'AD_Column ZZ_Parent_Uploads on ZZOrganisationLinkage not found';
  END IF;

  ----------------------------------------------------------------------------------
  -- 1. Backfill: NULL (and any stray blank) -> 'Y', preserving current behaviour.
  --    Rows already explicitly set to 'Y' or 'N' are left exactly as they are.
  ----------------------------------------------------------------------------------
  UPDATE adempiere.zzorganisationlinkage
  SET    zz_parent_uploads = 'Y',
         updated           = now(),
         updatedby         = 0
  WHERE  zz_parent_uploads IS NULL
     OR  TRIM(zz_parent_uploads) = '';

  GET DIAGNOSTICS v_backfilled = ROW_COUNT;
  RAISE NOTICE 'Backfilled % linkage row(s) to ZZ_Parent_Uploads = Y', v_backfilled;

  ----------------------------------------------------------------------------------
  -- 2. Make the flag mandatory so new linkages force an explicit Yes/No choice.
  --    No DefaultValue on purpose - the SDF must decide per child rather than inherit
  --    one by omission.
  --
  --    This is AD-level (PO.save) enforcement only. A physical NOT NULL constraint is
  --    deliberately NOT applied here: it would hard-fail any insert path that does not
  --    set the column, and it cannot be undone as cheaply as this flag. See the note at
  --    the bottom of this file if you decide you want it later.
  ----------------------------------------------------------------------------------
  UPDATE AD_Column
  SET    IsMandatory  = 'Y',
         DefaultValue = NULL,
         Updated      = now(),
         UpdatedBy    = 0
  WHERE  AD_Column_ID = v_column_id;

  RAISE NOTICE 'AD_Column ZZ_Parent_Uploads (%) set mandatory with no default', v_column_id;

  ----------------------------------------------------------------------------------
  -- 3. Clean up consolidations that the new rule no longer allows.
  --
  --    After the 'Y' backfill this normally removes nothing - it only bites where someone
  --    had already set ZZ_Parent_Uploads = 'N' by hand. Restricted to submissions that are
  --    still Draft/Imported: once a parent has submitted, its consolidation is part of a
  --    lodged submission and must not be re-scoped underneath it.
  ----------------------------------------------------------------------------------
  DELETE FROM adempiere.zz_wsp_atr_sub_levy_orgs slo
  USING  adempiere.zz_wsp_atr_submitted s,
         adempiere.zzsdforganisation parent_so,
         adempiere.zzsdforganisation child_so,
         adempiere.zzorganisationlinkage l
  WHERE  s.zz_wsp_atr_submitted_id = slo.zz_wsp_atr_submitted_id
  AND    parent_so.zzsdforganisation_id = s.zzsdforganisation_id
  AND    child_so.zzsdforganisation_id  = slo.zzsdforganisation_id
  AND    l.bpartner_parent_id = parent_so.c_bpartner_id
  AND    l.c_bpartner_id      = child_so.c_bpartner_id
  AND    s.zz_docstatus IN ('DR', 'IM')
  AND    COALESCE(l.zz_parent_uploads, 'N') <> 'Y';

  GET DIAGNOSTICS v_cleaned = ROW_COUNT;
  RAISE NOTICE 'Removed % stale sub levy org row(s) from Draft/Imported submissions', v_cleaned;

END $$;

-- Sanity check - expect zero rows.
SELECT COUNT(*) AS linkages_still_unset
FROM   adempiere.zzorganisationlinkage
WHERE  zz_parent_uploads IS NULL OR TRIM(zz_parent_uploads) = ''
;

-- OPTIONAL follow-up, only once you are satisfied nothing inserts linkages without the flag:
--   ALTER TABLE adempiere.zzorganisationlinkage
--     ALTER COLUMN zz_parent_uploads SET NOT NULL;
-- Note that iDempiere's "Synchronize Column" will apply this itself now that the AD column is
-- mandatory, so treat the next sync of this table as the point where it becomes real.

SELECT register_migration_script('202609211400_ParentUploadsBackfill_postgresql.sql') FROM dual
;
