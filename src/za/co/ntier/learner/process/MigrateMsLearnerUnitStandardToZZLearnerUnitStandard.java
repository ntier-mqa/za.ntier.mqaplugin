package za.co.ntier.learner.process;

import java.math.BigDecimal;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Phase 4 (see "Phase 4 - LearnerUnitStandard Family - Mapping.txt"): migrates the staged
 * ms_learnerunitstandard table (1,234,103 rows) into ZZLearnerUnitStandard.
 *
 * <p>Set-based SQL INSERT...SELECT (user decision 2026-07-21, matching Phase 2's
 * ZZLearnerLearnershipAssessments architecture) - both this table and its Assessments child are
 * far larger than any per-row Migrate* process built so far in this project. See
 * {@link MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments}'s Javadoc for
 * the full write-up of the batch-primary-key-reservation technique used here (unchanged) - not
 * repeated in full.
 *
 * <p>Crosswalks:
 * <ul>
 *   <li>ZZLearner_ID - direct join on the existing ms_learner_xref table.</li>
 *   <li>ZZProvider_ID - direct join on zzprovider's own "id" recon column (providerid is 100%
 *       populated on this source, unlike LearnerLearnership which needed a separate child
 *       table).</li>
 *   <li>Employer_ID - ms_organisation.sdlnumber = c_bpartner.zz_sdl_no (trimmed, case-sensitive
 *       exact match), replicated as a joinable temp table - same rule as
 *       MigrationSupport.buildOrganisationToBPartnerCrosswalk. Only ~7 c_bpartner rows currently
 *       have zz_sdl_no populated, so most rows are expected to resolve empty for now - not a
 *       bug.</li>
 *   <li>ZZProgrammeStatus/ZZSocioEconomicStatus/ZZCertificateReasonForReprint/
 *       ZZTerminationReason/ZZEnrolmentStatusReason - each built as a temp table joining the
 *       relevant ms_lkp* table's description against ad_ref_list.name (case-insensitive,
 *       trimmed) for the column's AD_Reference_ID, replicating
 *       MigrationSupport.buildListValueCrosswalk's matching logic in SQL.</li>
 *   <li>All *_By actor columns - ms_user.email -&gt; ad_user.email match (unique active email
 *       only), replicated as one shared joinable temp table, same technique already built for
 *       the Assessments migration in Phase 2.</li>
 *   <li>ZZUnitStandard_ID - resolves for ~52% of rows only (see mapping doc Section 1 - no
 *       staged MSSQL catalog source covers the rest). Accepted gap per user decision
 *       2026-07-21, not fixed here. Same indirect-through-zzlearnershipunitstandard ordinal
 *       technique as Phase 2.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerUnitStandardToZZLearnerUnitStandard")
public class MigrateMsLearnerUnitStandardToZZLearnerUnitStandard extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TARGET_TABLE = "zzlearnerunitstandard";
    private static final String SEQUENCE_NAME = "ZZLearnerUnitStandard";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        String trxName = get_TrxName();
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;
        int adClientId = Env.getAD_Client_ID(getCtx());
        int adOrgId = Env.getAD_Org_ID(getCtx());
        int createdBy = Env.getAD_User_ID(getCtx());

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(trxName, "SELECT count(*) FROM " + TARGET_TABLE + " WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated " + TARGET_TABLE + " row(s)...");
            DB.executeUpdateEx("DELETE FROM " + TARGET_TABLE + " WHERE id IS NOT NULL", null, trxName);
            DB.commit(true, trxName);
        }

        for (String tmp : new String[] { "tmp_lus_source", "tmp_lus_unitstandard_xwalk", "tmp_lus_ms_user_xwalk",
                "tmp_lus_bpartner_xwalk", "tmp_lus_programmestatus_xwalk", "tmp_lus_socioeconomicstatus_xwalk",
                "tmp_lus_reasonforreprint_xwalk", "tmp_lus_terminationreason_xwalk",
                "tmp_lus_enrolmentstatusreason_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE IF EXISTS " + tmp, null, trxName);
        }

        String sourceSql = "CREATE TEMP TABLE tmp_lus_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM ms_learnerunitstandard src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_lus_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_lus_source", null, trxName);
            return "No new ms_learnerunitstandard rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        // unitstandardid -> zzunitstandard_id, INDIRECT via the already-migrated
        // zzlearnershipunitstandard join table (~52% coverage - see class Javadoc).
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lus_unitstandard_xwalk AS "
                + "SELECT ms.unitstandardid AS source_id, MIN(zls.zzunitstandard_id) AS target_id "
                + "FROM (SELECT id, unitstandardid, row_number() OVER (ORDER BY id) AS rn FROM ms_learnershipunitstandard) ms "
                + "JOIN (SELECT zzunitstandard_id, row_number() OVER (ORDER BY zzmigrationcode) AS rn FROM zzlearnershipunitstandard) zls "
                + "  ON zls.rn = ms.rn "
                + "GROUP BY ms.unitstandardid", null, trxName);

        // ms_user.id -> ad_user_id, matched by unique active email.
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lus_ms_user_xwalk AS "
                + "SELECT mu.id AS ms_user_id, au.ad_user_id AS ad_user_id "
                + "FROM ms_user mu "
                + "JOIN (SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                + "      FROM ad_user WHERE isactive='Y' AND email IS NOT NULL AND trim(email)<>'' "
                + "      GROUP BY lower(trim(email)) HAVING count(*)=1) au "
                + "  ON au.email_key = lower(trim(mu.email)) "
                + "WHERE mu.email IS NOT NULL AND trim(mu.email) <> ''", null, trxName);

        // ms_organisation.id -> c_bpartner_id, via sdlnumber = zz_sdl_no (trimmed, exact match).
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lus_bpartner_xwalk AS "
                + "SELECT o.id AS source_id, MIN(bp.c_bpartner_id) AS target_id "
                + "FROM ms_organisation o "
                + "JOIN c_bpartner bp ON trim(bp.zz_sdl_no) = trim(o.sdlnumber) "
                + "WHERE o.sdlnumber IS NOT NULL AND trim(o.sdlnumber) <> '' "
                + "  AND bp.zz_sdl_no IS NOT NULL AND trim(bp.zz_sdl_no) <> '' "
                + "GROUP BY o.id", null, trxName);

        // CORRECTED 2026-07-31: "ms_lkpprogrammestatus" never existed - confirmed the real,
        // shared source is ms_lkpqctoprogrammestatus (same fix as
        // MigrateMsLearnerLearnershipToZZLearnerLearnership - see that class's comment for the
        // verification detail).
        createListValueCrosswalk(trxName, "tmp_lus_programmestatus_xwalk", "ms_lkpqctoprogrammestatus", 1000249);
        createListValueCrosswalk(trxName, "tmp_lus_socioeconomicstatus_xwalk", "ms_lkpsocioeconomicstatus", 1000250);
        createListValueCrosswalk(trxName, "tmp_lus_reasonforreprint_xwalk", "ms_lkpreasonforreprint", 1000253);
        createListValueCrosswalk(trxName, "tmp_lus_terminationreason_xwalk", "ms_lkpterminationreason", 1000254);
        createListValueCrosswalk(trxName, "tmp_lus_enrolmentstatusreason_xwalk", "ms_lkpenrolmentstatusreason", 1000255);

        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearnerunitstandard_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby, "
                + "  isactive, zzlearnerunitstandard_uu, zzlearner_id, zzunitstandard_id, zzcommencementdate, "
                + "  zzcompletiondate, zzcontractnumber, zzprogrammestatus, zzsocioeconomicstatus, "
                + "  zzprovider_id, employer_id, zzisapproved, zzapprovedby, zzdateapproved, "
                + "  zzcertificatenumber, zzcertificatecreatedby, zzdatecertificatecreated, "
                + "  zzcertificatereasonforreprint, zzcertificateprintingerrorreason, zzstatuseffectivedate, "
                + "  zzstudentnumber, zzextensiondate, zzextensionreason, zzterminationdate, "
                + "  zzterminationreason, zzterminatedcapturedby, zzdateterminationcaptured, "
                + "  zzextensioncapturedby, zzdateextensioncaptured, zzregistrationdate, zzregisteredby, "
                + "  zzenrolmentstatusreason, zzmostrecentregistrationdate, zzisendorsed, zzendorsedby, "
                + "  zzdateendorsed, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.created, ?, src.updated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  lx.zzlearner_id, usx.target_id, src.commencementdate, "
                + "  src.completiondate, src.contractnumber, psx.target_value, sesx.target_value, "
                + "  zzp.zzprovider_id, bpx.target_id, "
                + "  CASE WHEN src.isapproved IS NULL THEN NULL WHEN src.isapproved <> 0 THEN 'Y' ELSE 'N' END, "
                + "  approvedbyx.ad_user_id, src.dateapproved, "
                + "  src.certificatenumber, certcreatedbyx.ad_user_id, src.datecertificatecreated, "
                + "  rfrx.target_value, src.certificateprintingerrorreason, src.statuseffectivedate, "
                + "  src.studentnumber, src.extensiondate, src.extensionreason, src.terminationdate, "
                + "  trx_.target_value, termcapturedbyx.ad_user_id, src.dateterminationcaptured, "
                + "  extcapturedbyx.ad_user_id, src.dateextensioncaptured, src.registrationdate, registeredbyx.ad_user_id, "
                + "  esrx.target_value, src.mostrecentregistrationdate, "
                + "  CASE WHEN src.isendorsed IS NULL THEN NULL WHEN src.isendorsed <> 0 THEN 'Y' ELSE 'N' END, "
                + "  endorsedbyx.ad_user_id, src.dateendorsed, src.id "
                + "FROM tmp_lus_source src "
                + "LEFT JOIN ms_learner_xref lx ON lx.ms_learner_id = src.learnerid "
                + "LEFT JOIN tmp_lus_unitstandard_xwalk usx ON usx.source_id = src.unitstandardid "
                + "LEFT JOIN zzprovider zzp ON zzp.id = src.providerid "
                + "LEFT JOIN tmp_lus_bpartner_xwalk bpx ON bpx.source_id = src.employerid "
                + "LEFT JOIN tmp_lus_programmestatus_xwalk psx ON psx.source_id = src.programmestatusid "
                + "LEFT JOIN tmp_lus_socioeconomicstatus_xwalk sesx ON sesx.source_id = src.socioeconomicstatusid "
                + "LEFT JOIN tmp_lus_reasonforreprint_xwalk rfrx ON rfrx.source_id = src.certificatereasonforreprintid "
                + "LEFT JOIN tmp_lus_terminationreason_xwalk trx_ ON trx_.source_id = src.terminationreasonid "
                + "LEFT JOIN tmp_lus_enrolmentstatusreason_xwalk esrx ON esrx.source_id = src.enrolmentstatusreasonid "
                + "LEFT JOIN tmp_lus_ms_user_xwalk approvedbyx ON approvedbyx.ms_user_id = src.approvedby "
                + "LEFT JOIN tmp_lus_ms_user_xwalk certcreatedbyx ON certcreatedbyx.ms_user_id = src.certificatecreatedby "
                + "LEFT JOIN tmp_lus_ms_user_xwalk termcapturedbyx ON termcapturedbyx.ms_user_id = src.terminatedcapturedby "
                + "LEFT JOIN tmp_lus_ms_user_xwalk extcapturedbyx ON extcapturedbyx.ms_user_id = src.extensioncapturedby "
                + "LEFT JOIN tmp_lus_ms_user_xwalk registeredbyx ON registeredbyx.ms_user_id = src.registeredby "
                + "LEFT JOIN tmp_lus_ms_user_xwalk endorsedbyx ON endorsedbyx.ms_user_id = src.endorsedby";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        for (String tmp : new String[] { "tmp_lus_source", "tmp_lus_unitstandard_xwalk", "tmp_lus_ms_user_xwalk",
                "tmp_lus_bpartner_xwalk", "tmp_lus_programmestatus_xwalk", "tmp_lus_socioeconomicstatus_xwalk",
                "tmp_lus_reasonforreprint_xwalk", "tmp_lus_terminationreason_xwalk",
                "tmp_lus_enrolmentstatusreason_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE " + tmp, null, trxName);
        }

        return "Inserted " + inserted + " " + TARGET_TABLE + " row(s) (primary keys " + startId + ".."
                + (startId + rowCount - 1) + ").";
    }

    /**
     * Builds a temp table replicating MigrationSupport.buildListValueCrosswalk's matching logic
     * in SQL: lkpTable.id -&gt; the AD_Ref_List.Value whose Name matches lkpTable.description
     * (case-insensitive, trimmed) for the given AD_Reference_ID.
     */
    private void createListValueCrosswalk(String trxName, String tempTableName, String lkpTable, int adReferenceId) {
        DB.executeUpdateEx(
                "CREATE TEMP TABLE " + tempTableName + " AS "
                + "SELECT lkp.id AS source_id, MIN(arl.value) AS target_value "
                + "FROM " + lkpTable + " lkp "
                + "JOIN ad_ref_list arl ON arl.ad_reference_id = " + adReferenceId
                + "  AND lower(trim(arl.name)) = lower(trim(lkp.description)) "
                + "GROUP BY lkp.id", null, trxName);
    }

    /**
     * Reserves a contiguous block of {@code count} primary keys from this table's AD_Sequence
     * row - see MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments's
     * Javadoc for the full reasoning (unchanged here).
     *
     * @return the first id in the reserved block (block is start..start+count-1)
     */
    private long reserveIdBlock(String trxName, long count) {
        java.sql.PreparedStatement pst = null;
        java.sql.ResultSet rs = null;
        int adSequenceId;
        long currentNext;
        int incrementNo;
        try {
            pst = DB.prepareStatement(
                    "SELECT AD_Sequence_ID, CurrentNext, IncrementNo FROM AD_Sequence "
                    + "WHERE Name=? AND IsActive='Y' AND IsTableID='Y' AND IsAutoSequence='Y' FOR UPDATE",
                    trxName);
            pst.setString(1, SEQUENCE_NAME);
            rs = pst.executeQuery();
            if (!rs.next()) {
                throw new AdempiereException("No AD_Sequence row found for '" + SEQUENCE_NAME
                        + "' - has AddZZLearnerUnitStandardTable been run yet?");
            }
            adSequenceId = rs.getInt("AD_Sequence_ID");
            currentNext = rs.getLong("CurrentNext");
            incrementNo = rs.getInt("IncrementNo");
        } catch (java.sql.SQLException e) {
            throw new AdempiereException("Failed reading AD_Sequence for '" + SEQUENCE_NAME + "'", e);
        } finally {
            DB.close(rs, pst);
        }

        if (incrementNo != 1) {
            throw new AdempiereException("AD_Sequence '" + SEQUENCE_NAME + "' has IncrementNo=" + incrementNo
                    + " (expected 1) - refusing to batch-reserve IDs, this would miscalculate the block.");
        }

        DB.executeUpdateEx("UPDATE AD_Sequence SET CurrentNext = ? WHERE AD_Sequence_ID = ?",
                new Object[] { currentNext + count, adSequenceId }, trxName);

        return currentNext;
    }
}
