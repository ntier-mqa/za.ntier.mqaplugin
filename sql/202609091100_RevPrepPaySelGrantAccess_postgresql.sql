-- MQA customization: Reverse Prepare Payment (Selected) — grant process access
-- The button rendered but read-only because no role has an AD_Process_Access row for the new
-- AD_Process. iDempiere grants this automatically for roles created/updated through the client
-- (MRole.updateAccessRecords()) — creating the AD_Process directly via SQL bypasses that hook, so
-- no role has execute access until granted explicitly.
--
-- Rather than guessing which role(s) need it, this copies the grant from whichever role(s) already
-- have active access to the original header-level button (AD_Process_ID=200136,
-- "Reverse Prepare Payment" / IDEMPIERE-5170) — those are exactly the roles that should also be able
-- to run the row-level version. Safe to re-run: guarded with NOT EXISTS.

DO $$
DECLARE
  v_process_id INTEGER;
  v_grant      RECORD;
  v_count      INTEGER := 0;
BEGIN
  SELECT AD_Process_ID INTO v_process_id FROM AD_Process WHERE Value = 'ReversePrepPaymentSel';
  IF v_process_id IS NULL THEN
    RAISE EXCEPTION 'Process with Value=ReversePrepPaymentSel not found — has the creation script run on this database?';
  END IF;

  FOR v_grant IN
    SELECT AD_Role_ID, IsReadWrite FROM AD_Process_Access
    WHERE AD_Process_ID = 200136 AND IsActive = 'Y'
  LOOP
    IF NOT EXISTS (
      SELECT 1 FROM AD_Process_Access
      WHERE AD_Process_ID = v_process_id AND AD_Role_ID = v_grant.AD_Role_ID
    ) THEN
      INSERT INTO AD_Process_Access (AD_Process_ID, AD_Role_ID, AD_Client_ID, AD_Org_ID,
          IsActive, Created, CreatedBy, Updated, UpdatedBy, IsReadWrite, AD_Process_Access_UU)
      VALUES (v_process_id, v_grant.AD_Role_ID, 0, 0,
          'Y', now(), 0, now(), 0, v_grant.IsReadWrite, md5(random()::text || clock_timestamp()::text)::uuid);
      v_count := v_count + 1;
    END IF;
  END LOOP;

  RAISE NOTICE 'Granted AD_Process % access to % role(s) (copied from AD_Process_ID=200136)', v_process_id, v_count;
END $$;

SELECT register_migration_script('202609091100_RevPrepPaySelGrantAccess_postgresql.sql') FROM dual
;
