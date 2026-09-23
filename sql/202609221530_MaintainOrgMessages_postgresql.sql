-- MQA customization: create the missing AD_Message rows used by the SDR Maintain Organisation form.
--
-- MaintainOrganisationVM resolves its dialog and validation text through Msg.getMsg(). When no
-- AD_Message row matches, iDempiere logs "NOT found: <key>" and returns THE KEY ITSELF rather than
-- failing - which is why saving the form showed a dialog reading "ZZOrgSavedSuccess".
--
-- Only ZZValidateNotNull and ZZMaintainOrgSelfReference currently exist; every insert below is
-- guarded with NOT EXISTS on Value, so those two (and anything already added by hand) are left
-- exactly as they are. Safe to re-run.
--
-- MsgTip is deliberately NULL for the validation messages. Msg caches each message as
-- MsgText + SEPARATOR + MsgTip whenever MsgTip IS NOT NULL (Msg.addMessagesInCache), and the
-- validation call sites use the two-argument Msg.getMsg(ctx, key), which returns that whole
-- concatenated string - so a tip on those rows would be appended straight into the validation text
-- the user sees. Only ZZOrgSavedSuccess carries a tip, because MasterUtil.showInfoDialog() passes
-- Msg.getMsg(key, false) (the tip) as the dialog TITLE and Msg.getMsg(key, true) (the text) as the
-- dialog BODY. Without a tip the dialog falls back to a generic "Dialog" heading.
--
-- Wording follows the house style of the existing row ("This Organisation is not allowed as a Child
-- Organisation"): sentence case, key nouns capitalised, no trailing full stop. Review and adjust in
-- the Application Dictionary if you want different phrasing - the keys are what matter to the code.
--
-- IDs come from nextval('ad_message_sq'), the same native sequence MSequence.getNextID() uses.
-- AD_Message_Trl rows are inserted for every IsSystemLanguage='Y' language; without them the text
-- renders blank for those languages, because inserting AD_* rows directly via SQL bypasses the
-- model-layer hook that normally creates translations.
--
-- Msg caches messages per language, so reset the cache or restart the application server after
-- running this before the new text appears.

DO $$
DECLARE
  r       RECORD;
  v_id    INTEGER;
  v_lang  RECORD;
  v_made  INTEGER := 0;
BEGIN
  FOR r IN
    SELECT * FROM (VALUES
      -- key                                  MsgText                                                                          MsgTip        Type  UUID
      ('ZZOrgSavedSuccess',                   'Organisation details saved successfully',                                       'Saved',      'I',  'ae5b017c-516a-420a-9938-88e554b15be0'),
      ('ZZMaintainOrgChildOrgNotFound',       'No Organisation was found for this SDL Number',                                  NULL::varchar,'E',  'c4812059-c96c-4822-9959-fb30cf32e23b'),
      ('ZZMaintainOrgMissingOrg',             'The Organisation could not be found. Please save the Organisation first',        NULL::varchar,'E',  '9acfbf07-f3b7-46eb-b0f8-dbb967506180'),
      ('ZZMaintainOrgChildLinked',            'This Organisation is already linked to another Parent Organisation',             NULL::varchar,'E',  '9c6a2478-e3dc-43a0-8e82-5f7c3833dfc9'),
      ('ZZMaintainOrgDuplicateSdlNoOtherRow', 'This SDL Number has already been captured on another row',                       NULL::varchar,'E',  'eee7e2d9-ee11-4556-bc37-1a0c1bee4110'),
      ('ZZMaintainOrgAcceptChildOrgOnly',     'A Parent Organisation cannot be added as a Child Organisation',                  NULL::varchar,'E',  '926c68cd-d25e-404c-ab4d-d0ed76aa0618'),
      -- Currently referenced only from commented-out code in MaintainOrganisationVM (~line 772).
      -- Created anyway so the dialog works immediately if that block is ever re-enabled.
      ('ZZOrgMaintainNotFoundOrg',            'No Organisation was found for this SDL Number',                                  NULL::varchar,'E',  'b8f53763-31e5-46a3-bd92-1a32ccad135c')
    ) AS t(value, msgtext, msgtip, msgtype, uu)
  LOOP
    IF EXISTS (SELECT 1 FROM AD_Message WHERE Value = r.value) THEN
      RAISE NOTICE 'AD_Message % already exists - left untouched', r.value;
      CONTINUE;
    END IF;

    v_id := nextval('ad_message_sq');

    IF v_id < 1000000 THEN
      RAISE WARNING 'Generated AD_Message_ID % for % is below 1,000,000 - this system''s native sequence may not be positioned past the core dictionary range.', v_id, r.value;
    END IF;

    INSERT INTO AD_Message (AD_Message_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
        Value, MsgText, MsgTip, MsgType, EntityType, AD_Message_UU)
    VALUES (v_id, 0, 0, 'Y', now(), 0, now(), 0,
        r.value, r.msgtext, r.msgtip, r.msgtype, 'U', r.uu::uuid);

    FOR v_lang IN SELECT AD_Language FROM AD_Language WHERE IsSystemLanguage = 'Y' LOOP
      INSERT INTO AD_Message_Trl (AD_Message_ID, AD_Language, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,
          MsgText, MsgTip, IsTranslated, AD_Message_Trl_UU)
      VALUES (v_id, v_lang.AD_Language, 0, 0, 'Y', now(), 0, now(), 0,
          r.msgtext, r.msgtip, 'Y', md5(random()::text || clock_timestamp()::text)::uuid);
    END LOOP;

    v_made := v_made + 1;
    RAISE NOTICE 'Created AD_Message % (ID %)', r.value, v_id;
  END LOOP;

  RAISE NOTICE 'Created % new AD_Message row(s)', v_made;
END $$;

-- Sanity check - every key the Maintain Organisation form uses should now return a row.
SELECT Value, MsgText, MsgTip, MsgType
FROM   AD_Message
WHERE  Value IN ('ZZOrgSavedSuccess', 'ZZOrgMaintainNotFoundOrg', 'ZZValidateNotNull',
                 'ZZMaintainOrgChildOrgNotFound', 'ZZMaintainOrgMissingOrg',
                 'ZZMaintainOrgChildLinked', 'ZZMaintainOrgDuplicateSdlNoOtherRow',
                 'ZZMaintainOrgAcceptChildOrgOnly', 'ZZMaintainOrgSelfReference')
ORDER  BY Value
;

SELECT register_migration_script('202609221530_MaintainOrgMessages_postgresql.sql') FROM dual
;
