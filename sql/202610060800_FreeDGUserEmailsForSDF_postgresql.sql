-- =====================================================================
-- Roadshow workaround (2026-10-06): free up emails for existing
-- DG Application Users so they can self-register as SDF tomorrow
-- without GeneralRegistrationWindow's "email already registered" block.
--
-- Approach: append '99' to the EMail of every active user holding the
-- 'Agent - DG Application' role (AD_Role_ID=1000023), EXCEPT:
--   - BP is 'MQA' (MQA Staff, C_BPartner_ID=1054359)         -> skip
--   - EMail ends in '@ntier.co.za' (nTier staff)              -> skip
--   - EMail is one of the known test/personal addresses below -> skip
--   - User already holds the SDF role (AD_Role_ID=1000042)    -> skip
-- Users then register fresh under their real email as SDF, ending up
-- with two AD_User rows (old DG login w/ '99' email, new SDF login).
-- These get merged later. NO application code changes involved.
--
-- Run the STEP 0 queries first and eyeball the results before running
-- the UPDATE in STEP 2. Everything is wrapped so you can inspect/rollback.
-- =====================================================================

-- ---------------------------------------------------------------------
-- STEP 0a: Confirm the role name/ID before trusting AD_Role_ID=1000023
-- Confirmed 2026-10-06: 1000023 = 'Agent - DG Application' (Y).
-- (1000046 'DG Application - Read-Only' exists too but is NOT the
-- self-registration role, so it's left out of scope.)
-- ---------------------------------------------------------------------
SELECT AD_Role_ID, Name, IsActive
FROM AD_Role
WHERE Name ILIKE '%DG Application%';

-- ---------------------------------------------------------------------
-- STEP 0b: Confirm the 'MQA' / 'MQA Staff' Business Partner you mean
-- Confirmed 2026-10-06: C_BPartner_ID=1054359, Value='MQA', Name='MQA'.
-- (ILIKE '%MQA%' also catches unrelated BPs like 'Vuke Mqabaqaba' and
-- 'MQA020 / MQA Test TVET College' - the exact match below avoids them.)
-- ---------------------------------------------------------------------
SELECT C_BPartner_ID, Value, Name
FROM C_BPartner
WHERE Name ILIKE '%MQA%' OR Value ILIKE '%MQA%';

-- ---------------------------------------------------------------------
-- STEP 0c: Preview exactly who this will touch (and who/why it'll skip)
-- ---------------------------------------------------------------------
-- NOTE: deliberately NOT filtering IsActive in the FROM/WHERE here so the
-- preview also shows inactive users/role-assignments, with 'SKIP (inactive)'
-- called out explicitly as its own reason rather than silently vanishing.
SELECT
    u.AD_User_ID,
    u.Name,
    u.EMail,
    u.IsActive AS User_IsActive,
    ur.IsActive AS RoleAssignment_IsActive,
    bp.C_BPartner_ID,
    bp.Name AS BP_Name,
    CASE
        WHEN u.IsActive <> 'Y' OR ur.IsActive <> 'Y' THEN 'SKIP (inactive)'
        WHEN bp.Name = 'MQA' THEN 'SKIP (MQA Staff)'
        WHEN LOWER(u.EMail) LIKE '%@ntier.co.za' THEN 'SKIP (nTier staff email)'
        WHEN LOWER(u.EMail) IN (
            'deirdreaugustine776@gmail.com',
            'deirdreaugustine7777@gmail.com',
            'deirdreaugustine77@gmail.com',
            'deirdreaugustine7a7@gmail.com',
            'hieplq@hasuvimex.vn'
        ) THEN 'SKIP (test/personal email)'
        WHEN EXISTS (
            SELECT 1 FROM AD_User_Roles sdfur
            WHERE sdfur.AD_User_ID = u.AD_User_ID
              AND sdfur.AD_Role_ID = 1000042   -- SDF role
              AND sdfur.IsActive = 'Y'
        ) THEN 'SKIP (already has SDF role)'
        ELSE 'WILL APPEND 99'
    END AS Action
FROM AD_User u
JOIN AD_User_Roles ur ON ur.AD_User_ID = u.AD_User_ID
JOIN AD_Role r         ON r.AD_Role_ID = ur.AD_Role_ID
LEFT JOIN C_BPartner bp ON bp.C_BPartner_ID = u.C_BPartner_ID
WHERE r.AD_Role_ID = 1000023   -- 'Agent - DG Application'
  AND u.AD_Client_ID = 1000000
ORDER BY Action, u.EMail;

-- ---------------------------------------------------------------------
-- STEP 1: Save the affected records BEFORE changing anything.
--
-- 1a) Full raw AD_User table snapshot - ALWAYS do this before any update
--     run. Never DROP a previous snapshot; bump the trailing _0 suffix
--     to _1, _2, ... for each subsequent run on a given day/date.
-- ---------------------------------------------------------------------
create table t_ad_user_06102026_0 as
select * from Ad_user;

-- ---------------------------------------------------------------------
-- 1b) Narrower, annotated snapshot of just the DG-role candidates, with
--     the skip reasons baked in, so STEP 2 and the later merge both
--     have a clear record of what was done and why. Same no-drop
--     convention: bump the _0 suffix on re-runs, do not DROP/overwrite.
-- ---------------------------------------------------------------------
-- NOTE: deliberately NOT filtering IsActive in the FROM/WHERE here, same
-- reasoning as STEP 0c - inactive rows are captured and flagged rather
-- than silently dropped, so the counts below can show them explicitly.

--drop table ZZ_DGUser_Email_Backup_06102026_0

CREATE TABLE ZZ_DGUser_Email_Backup_06102026_0 AS
SELECT
    u.AD_User_ID,
    u.AD_Client_ID,
    u.AD_Org_ID,
    u.Name,
    u.EMail AS Original_EMail,
    u.C_BPartner_ID,
    bp.Name AS BP_Name,
    (u.IsActive <> 'Y' OR ur.IsActive <> 'Y')      AS Is_Inactive,
    -- COALESCE guards: bp.Name is NULL when the user has no linked BP
    -- (true for most SDF candidates), and a bare "bp.Name = 'MQA'" then
    -- evaluates to NULL (not FALSE) under 3-valued logic, which poisons
    -- the NOT(... OR ... OR ...) below and makes every such row NULL
    -- instead of TRUE - that's what was making to_be_changed show 0.
    (COALESCE(bp.Name, '') = 'MQA')                AS Is_MQA_Staff,
    (COALESCE(LOWER(u.EMail), '') LIKE '%@ntier.co.za') AS Is_Ntier_Email,
    (COALESCE(LOWER(u.EMail), '') IN (
        'deirdreaugustine776@gmail.com',
        'deirdreaugustine7777@gmail.com',
        'deirdreaugustine77@gmail.com',
        'deirdreaugustine7a7@gmail.com',
        'hieplq@hasuvimex.vn'
    ))                                               AS Is_Excluded_Test_Email,
    EXISTS (
        SELECT 1 FROM AD_User_Roles sdfur
        WHERE sdfur.AD_User_ID = u.AD_User_ID
          AND sdfur.AD_Role_ID = 1000042
          AND sdfur.IsActive = 'Y'
    )                                                AS Has_SDF_Role,
    now() AS Backup_Taken
FROM AD_User u
JOIN AD_User_Roles ur ON ur.AD_User_ID = u.AD_User_ID
JOIN AD_Role r         ON r.AD_Role_ID = ur.AD_Role_ID
LEFT JOIN C_BPartner bp ON bp.C_BPartner_ID = u.C_BPartner_ID
WHERE r.AD_Role_ID = 1000023
  AND u.AD_Client_ID = 1000000;

-- Final flag used by STEP 2: update only if none of the skip reasons apply
ALTER TABLE ZZ_DGUser_Email_Backup_06102026_0 ADD COLUMN To_Update boolean;
UPDATE ZZ_DGUser_Email_Backup_06102026_0
SET To_Update = NOT (Is_Inactive OR Is_MQA_Staff OR Is_Ntier_Email OR Is_Excluded_Test_Email OR Has_SDF_Role);

-- Sanity check the backup landed and counts make sense before updating.
-- total_dg_users includes inactive rows; inactive_excluded is broken out
-- explicitly; the remaining skip categories are only evaluated among the
-- active rows (an inactive row is counted once, under inactive_excluded,
-- not double-counted into the other skip buckets too).
--
-- NOTE: mqa_staff_skipped/ntier_skipped/test_email_skipped/already_sdf_skipped
-- are NOT mutually exclusive (e.g. an nTier email that also already holds the
-- SDF role counts in both), so
--   inactive_excluded + mqa_staff_skipped + ntier_skipped + test_email_skipped
--     + already_sdf_skipped + to_be_changed
-- will OVER-count total_dg_users by however many active users matched more
-- than one skip reason. Use the overlap query right below this to see them.
SELECT
    COUNT(*)                                                           AS total_dg_users,
    COUNT(*) FILTER (WHERE Is_Inactive)                                AS inactive_excluded,
    COUNT(*) FILTER (WHERE NOT Is_Inactive AND Is_MQA_Staff)           AS mqa_staff_skipped,
    COUNT(*) FILTER (WHERE NOT Is_Inactive AND Is_Ntier_Email)         AS ntier_skipped,
    COUNT(*) FILTER (WHERE NOT Is_Inactive AND Is_Excluded_Test_Email) AS test_email_skipped,
    COUNT(*) FILTER (WHERE NOT Is_Inactive AND Has_SDF_Role)           AS already_sdf_skipped,
    COUNT(*) FILTER (WHERE To_Update)                                  AS to_be_changed
FROM ZZ_DGUser_Email_Backup_06102026_0;

-- Reconciliation: this always balances exactly to total_dg_users, because
-- active_skipped_distinct counts each active-but-skipped user once no
-- matter how many skip reasons they matched.
SELECT
    COUNT(*)                                                      AS total_dg_users,
    COUNT(*) FILTER (WHERE Is_Inactive)                           AS inactive_excluded,
    COUNT(*) FILTER (WHERE NOT Is_Inactive AND NOT To_Update)      AS active_skipped_distinct,
    COUNT(*) FILTER (WHERE To_Update)                              AS to_be_changed
FROM ZZ_DGUser_Email_Backup_06102026_0;

-- Who are the overlap cases (matched more than one active skip reason) -
-- this is why the per-reason buckets above summed to more than total_dg_users
SELECT
    AD_User_ID, Name, Original_EMail, BP_Name,
    Is_MQA_Staff, Is_Ntier_Email, Is_Excluded_Test_Email, Has_SDF_Role
FROM ZZ_DGUser_Email_Backup_06102026_0
WHERE NOT Is_Inactive
  AND (
        (CASE WHEN Is_MQA_Staff THEN 1 ELSE 0 END) +
        (CASE WHEN Is_Ntier_Email THEN 1 ELSE 0 END) +
        (CASE WHEN Is_Excluded_Test_Email THEN 1 ELSE 0 END) +
        (CASE WHEN Has_SDF_Role THEN 1 ELSE 0 END)
      ) > 1;

-- ---------------------------------------------------------------------
-- STEP 2: The actual update. Only touches users flagged To_Update in
-- the backup. Idempotent-ish guard (WHERE EMail = Original) so
-- re-running this after a partial failure won't double-append '99'.
-- ---------------------------------------------------------------------
BEGIN;

UPDATE AD_User u
SET EMail     = b.Original_EMail || '99',
    Updated   = now(),
    UpdatedBy = 0   -- SuperUser; change to your AD_User_ID if preferred
FROM ZZ_DGUser_Email_Backup_06102026_0 b
WHERE u.AD_User_ID = b.AD_User_ID
  AND b.To_Update = TRUE
  AND u.EMail = b.Original_EMail;   -- skip if email already changed

-- Review the row count / results before committing
SELECT u.AD_User_ID, b.Original_EMail, u.EMail AS New_EMail
FROM AD_User u
JOIN ZZ_DGUser_Email_Backup_06102026_0 b ON b.AD_User_ID = u.AD_User_ID
WHERE b.To_Update = TRUE
ORDER BY b.Original_EMail;

-- If it looks right:
COMMIT;
-- If anything looks wrong instead, run ROLLBACK; and nothing is changed.

-- ---------------------------------------------------------------------
-- STEP 3 (reference only, for the later merge / rollback):
-- Revert a single user's email back from the backup if needed:
--   UPDATE AD_User SET EMail = b.Original_EMail
--   FROM ZZ_DGUser_Email_Backup_06102026_0 b
--   WHERE AD_User.AD_User_ID = b.AD_User_ID AND AD_User.AD_User_ID = <id>;
--
-- Or restore from the full raw snapshot taken in STEP 1a:
--   UPDATE AD_User u SET EMail = t.EMail
--   FROM t_ad_user_06102026_0 t
--   WHERE u.AD_User_ID = t.AD_User_ID AND u.AD_User_ID = <id>;
-- ---------------------------------------------------------------------
