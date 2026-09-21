-- =====================================================================================
-- READ-ONLY verification for the ZZ_Parent_Uploads rollout.
--
-- Run this BEFORE applying the backfill migration. It answers the four questions the
-- code change depends on:
--   1. Does the physical column exist on zzorganisationlinkage?
--   2. Is the AD_Column configured correctly (Yes/No list, reference 319)?
--   3. Is there an AD_Field so the flag can be maintained in the back office?
--   4. What does the existing data look like, and what would the code change do to it?
--
-- Nothing here writes. Safe to run on any environment.
-- =====================================================================================

-- ---- 1. Physical column on zzorganisationlinkage ----
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM   information_schema.columns
WHERE  table_name  = 'zzorganisationlinkage'
AND    column_name = 'zz_parent_uploads';
-- Expect exactly one row. NO ROWS = the AD column was never synced to the database and
-- the code change will fail at runtime with "column l.zz_parent_uploads does not exist".

-- ---- 2. AD_Column configuration ----
SELECT c.AD_Column_ID, c.ColumnName, c.Name, c.AD_Reference_ID, r.Name AS reference_name,
       c.FieldLength, c.IsMandatory, c.DefaultValue, c.EntityType, c.IsActive
FROM   AD_Column c
JOIN   AD_Table  t ON t.AD_Table_ID = c.AD_Table_ID
LEFT   JOIN AD_Reference r ON r.AD_Reference_ID = c.AD_Reference_ID
WHERE  t.TableName  = 'ZZOrganisationLinkage'
AND    c.ColumnName = 'ZZ_Parent_Uploads';
-- Expect AD_Reference_ID = 319 (_YesNo list), FieldLength = 1.

-- ---- 3. AD_Field / window placement (can a user maintain it?) ----
SELECT f.AD_Field_ID, f.Name, f.IsDisplayed, f.IsReadOnly, f.SeqNo,
       tab.Name AS tab_name, w.Name AS window_name
FROM   AD_Field f
JOIN   AD_Column c ON c.AD_Column_ID = f.AD_Column_ID
JOIN   AD_Table  t ON t.AD_Table_ID  = c.AD_Table_ID
JOIN   AD_Tab  tab ON tab.AD_Tab_ID  = f.AD_Tab_ID
JOIN   AD_Window  w ON w.AD_Window_ID = tab.AD_Window_ID
WHERE  t.TableName  = 'ZZOrganisationLinkage'
AND    c.ColumnName = 'ZZ_Parent_Uploads';
-- NO ROWS = the flag cannot be set from the back office at all.

-- ---- 4a. Current value distribution across ALL linkage rows ----
SELECT COALESCE(zz_parent_uploads, '(null)') AS zz_parent_uploads,
       isactive,
       COUNT(*) AS rows
FROM   adempiere.zzorganisationlinkage
GROUP  BY 1, 2
ORDER  BY 1, 2;

-- ---- 4b. Impact: linkages that qualify TODAY vs AFTER the code change ----
-- "Qualifying" mirrors WspAtrUploadsADForm.isParentOrganisation: active+approved on both sides.
SELECT COUNT(*)                                                              AS qualifying_links_today,
       COUNT(*) FILTER (WHERE COALESCE(l.zz_parent_uploads,'N') = 'Y')       AS qualifying_links_after,
       COUNT(DISTINCT parent_so.zzsdforganisation_id)                        AS parent_orgs_today,
       COUNT(DISTINCT parent_so.zzsdforganisation_id)
           FILTER (WHERE COALESCE(l.zz_parent_uploads,'N') = 'Y')            AS parent_orgs_after
FROM   adempiere.zzsdforganisation parent_so
JOIN   adempiere.zzorganisationlinkage l ON l.bpartner_parent_id = parent_so.c_bpartner_id
JOIN   adempiere.zzsdforganisation child_so ON child_so.c_bpartner_id = l.c_bpartner_id
WHERE  parent_so.isactive = 'Y'
AND    child_so.isactive  = 'Y'
AND    l.isactive         = 'Y'
AND    COALESCE(parent_so.zz_docstatus,'') = 'AP'
AND    COALESCE(child_so.zz_docstatus,'')  = 'AP';
-- If *_after is 0 and *_today is not, the backfill has not been run yet. Deploying the
-- code in that state silently removes every parent/child consolidation.

-- ---- 4c. Sub levy org rows already built that the new rule would exclude ----
SELECT COUNT(*) AS stale_sub_levy_rows
FROM   adempiere.zz_wsp_atr_sub_levy_orgs slo
JOIN   adempiere.zz_wsp_atr_submitted s   ON s.zz_wsp_atr_submitted_id = slo.zz_wsp_atr_submitted_id
JOIN   adempiere.zzsdforganisation parent_so ON parent_so.zzsdforganisation_id = s.zzsdforganisation_id
JOIN   adempiere.zzsdforganisation child_so  ON child_so.zzsdforganisation_id  = slo.zzsdforganisation_id
JOIN   adempiere.zzorganisationlinkage l
       ON l.bpartner_parent_id = parent_so.c_bpartner_id
      AND l.c_bpartner_id      = child_so.c_bpartner_id
WHERE  COALESCE(l.zz_parent_uploads,'N') <> 'Y';
-- These rows were written by rebuildSubLevyOrgLinks under the old (unfiltered) rule.
-- They are NOT re-evaluated automatically, so they need a rebuild after the backfill.
