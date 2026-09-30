-- MQA customization: rebuild ZZ_WSP_ATR_Sub_Levy_Orgs after the Separate-WSP-ATR polarity change.
--
-- The consolidation set used to be the children a parent UPLOADS for
-- (ZZ_Parent_Uploads = 'Y'). It is now the children that file their OWN return
-- (ZZ_Parent_Uploads = 'N', shown as "Separate WSP-ATR? = Yes"), because those are the
-- submissions a consolidated report is actually assembled from.
--
-- WspAtrSubmittedADForm.rebuildSubLevyOrgLinks only re-runs when a submission is created or when
-- the model validator sees a linkage change - neither of which a CODE change triggers. So rows
-- written before the flip still hold the old membership, and the consolidated-report guard then
-- names the wrong children (observed: parent 1021581 still listed L020792042, a stored-'Y' child).
-- This script re-derives the set for every submission that can still legitimately change.
--
-- Scope: Draft ('DR') and Imported ('IM') submissions only. Anything further along is part of a
-- lodged submission and must not be silently re-scoped underneath it.
--
-- Mirrors rebuildSubLevyOrgLinks exactly: active linkage, active child, child not 'UnSdfOrg'.
-- UUIDs use md5(random())::uuid rather than generate_uuid() so this runs even where the uuid-ossp
-- extension is not visible to the application role. Safe to re-run - it is a full rebuild.

DO $$
DECLARE
  r          RECORD;
  v_seq      TEXT := 'adempiere.zz_wsp_atr_sub_levy_orgs_sq';
  v_parents  INTEGER := 0;
  v_deleted  INTEGER := 0;
  v_inserted INTEGER := 0;
  v_d        INTEGER;
  v_i        INTEGER;
BEGIN
  IF to_regclass(v_seq) IS NULL THEN
    RAISE EXCEPTION 'Sequence % not found - this table may not use a native id sequence on this system. Check AD_Sequence for ZZ_WSP_ATR_Sub_Levy_Orgs before running.', v_seq;
  END IF;

  FOR r IN
    SELECT s.zz_wsp_atr_submitted_id AS submitted_id,
           s.ad_client_id,
           s.ad_org_id,
           so.c_bpartner_id          AS parent_bp
    FROM   adempiere.zz_wsp_atr_submitted s
    JOIN   adempiere.zzsdforganisation so
           ON so.zzsdforganisation_id = s.zzsdforganisation_id
    WHERE  s.isactive = 'Y'
    AND    s.zz_docstatus IN ('DR', 'IM')
  LOOP
    DELETE FROM adempiere.zz_wsp_atr_sub_levy_orgs
    WHERE  zz_wsp_atr_submitted_id = r.submitted_id;
    GET DIAGNOSTICS v_d = ROW_COUNT;

    INSERT INTO adempiere.zz_wsp_atr_sub_levy_orgs (
        zz_wsp_atr_sub_levy_orgs_id, ad_client_id, ad_org_id, isactive,
        created, createdby, updated, updatedby,
        zz_wsp_atr_submitted_id, zzsdforganisation_id, zz_wsp_atr_sub_levy_orgs_uu)
    SELECT nextval(v_seq), r.ad_client_id, r.ad_org_id, 'Y',
           now(), 0, now(), 0,
           r.submitted_id, child.zzsdforganisation_id,
           md5(random()::text || clock_timestamp()::text)::uuid
    FROM ( SELECT DISTINCT so.zzsdforganisation_id
           FROM   adempiere.zzorganisationlinkage l
           JOIN   adempiere.zzsdforganisation so
                  ON so.c_bpartner_id = l.c_bpartner_id
           WHERE  l.bpartner_parent_id = r.parent_bp
           AND    l.isactive = 'Y'
           -- Separate WSP-ATR? = Yes
           AND    COALESCE(l.zz_parent_uploads, 'N') = 'N'
           AND    so.isactive = 'Y'
           AND    so.zz_docstatus <> 'UnSdfOrg'
         ) child;
    GET DIAGNOSTICS v_i = ROW_COUNT;

    v_parents  := v_parents + 1;
    v_deleted  := v_deleted + v_d;
    v_inserted := v_inserted + v_i;

    IF v_d <> v_i THEN
      RAISE NOTICE 'Submitted % : % row(s) removed, % row(s) added', r.submitted_id, v_d, v_i;
    END IF;
  END LOOP;

  RAISE NOTICE 'Rebuilt % Draft/Imported submission(s): % row(s) deleted, % row(s) inserted',
      v_parents, v_deleted, v_inserted;
END $$;

-- Sanity check - expect ZERO rows. Any row here is a stored-'Y' child still in a consolidation
-- set, which the current code would never create.
SELECT slo.zz_wsp_atr_submitted_id, bp.value AS sdl_no, l.zz_parent_uploads
FROM   adempiere.zz_wsp_atr_sub_levy_orgs slo
JOIN   adempiere.zz_wsp_atr_submitted s   ON s.zz_wsp_atr_submitted_id = slo.zz_wsp_atr_submitted_id
JOIN   adempiere.zzsdforganisation pso    ON pso.zzsdforganisation_id  = s.zzsdforganisation_id
JOIN   adempiere.zzsdforganisation so     ON so.zzsdforganisation_id   = slo.zzsdforganisation_id
JOIN   adempiere.c_bpartner bp            ON bp.c_bpartner_id          = so.c_bpartner_id
JOIN   adempiere.zzorganisationlinkage l  ON l.c_bpartner_id = so.c_bpartner_id
                                         AND l.bpartner_parent_id = pso.c_bpartner_id
WHERE  s.zz_docstatus IN ('DR', 'IM')
AND    COALESCE(l.zz_parent_uploads, 'N') = 'Y'
;

SELECT register_migration_script('202609301200_RebuildSubLevyOrgs_postgresql.sql') FROM dual
;
