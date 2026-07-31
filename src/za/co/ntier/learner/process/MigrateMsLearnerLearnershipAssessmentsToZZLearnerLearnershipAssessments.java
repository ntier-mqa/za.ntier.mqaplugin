package za.co.ntier.learner.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #1): migrates the
 * staged ms_learnerlearnershipassessments table (4,091,083 rows) into ZZLearnerLearnershipAssessments.
 *
 * <p><b>ARCHITECTURALLY DIFFERENT from every other Migrate* process in this project</b> (user
 * decision 2026-07-21): every other one uses a per-row Java loop (Query then PO.saveEx() per
 * row), which at 4M rows would take hours and generate a huge audit/transaction overhead. This
 * process instead does a single set-based SQL {@code INSERT INTO ... SELECT} with all crosswalks
 * resolved via temp-table joins, built entirely in raw SQL/DB.executeUpdateEx - no per-row PO
 * objects at all.
 *
 * <p><b>Primary key allocation</b> - the one genuinely new, higher-risk piece: since PO.saveEx()
 * (which normally calls MSequence.getNextID() per row) is bypassed entirely, this process
 * reserves a whole CONTIGUOUS BLOCK of N primary keys with a single atomic
 * {@code UPDATE AD_Sequence SET CurrentNext = CurrentNext + N WHERE Name=... RETURNING
 * CurrentNext - N} (read the pre-update value as the block's starting id) - mirroring exactly
 * what {@link org.compiere.model.MSequence#getNextID} does per-call (confirmed by reading its
 * source: {@code SELECT CurrentNext ... WHERE Name=? AND IsTableID='Y' AND IsAutoSequence='Y'
 * FOR UPDATE}, then {@code UPDATE ... SET CurrentNext=CurrentNext+IncrementNo}), just batched
 * into one round-trip instead of 4 million. Defensively verifies IncrementNo=1 first (the
 * standard convention for a table-id sequence) and throws rather than silently miscalculating if
 * it's ever anything else. This technique has NO precedent elsewhere in this codebase - test with
 * a small MaxRows value first and spot-check a few rows before trusting at full scale, same
 * "verify incrementally" discipline this project has used throughout.
 *
 * <p>Crosswalks (all built as SQL temp tables, not Java Maps, so they can be joined directly in
 * the final INSERT):
 * <ul>
 *   <li>ZZLearnerLearnership_ID - direct join on zzlearnerlearnership's "id" recon column
 *       (requires MigrateMsLearnerLearnershipToZZLearnerLearnership to have already run).</li>
 *   <li>ZZUnitStandard_ID - unitstandardid has no direct "id" recon column to join against
 *       (zzunitstandard was never given one). Resolved INDIRECTLY, through the
 *       already-fully-migrated zzlearnershipunitstandard join table: an ordinal join
 *       (ms_learnershipunitstandard.id &lt;-&gt; zzlearnershipunitstandard.zzmigrationcode, same
 *       row_number()-based technique MigrationSupport.buildOrdinalCrosswalk already uses)
 *       recovers unitstandardid -&gt; zzunitstandard_id via zzlearnershipunitstandard's own
 *       already-correct zzunitstandard_id column - verified 0 NULL FKs on that table beforehand
 *       (see the mapping doc's "verify/close out" section), so this crosswalk should be
 *       complete.</li>
 *   <li>Assessment_Status_ID - direct join on the Assessment_Status reference table's own "id"
 *       recon column (populated when {@link AddColumnsSupport#populateReferenceTable} created
 *       it from ms_lkpassessmentstatus - shared with the 3 Phase 1 Assessments tables).</li>
 *   <li>ZZAssessorPerson_ID / ZZModerator_ID / ZZPartialApprovedBy - ms_user.email -&gt;
 *       ad_user.email match, same rule as MigrationSupport.buildMsUserToAdUserCrosswalk (unique
 *       active ad_user email only) - replicated as a join here since a Java Map can't be joined
 *       in SQL directly.</li>
 * </ul>
 *
 * <p>ZZRPL/ZZIsPartialApproved/ZZIsPreviouslyAchieved use the same 0=No/non-zero=Yes flag logic
 * as MigrationSupport.flagToYN(), replicated as a SQL CASE expression.
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments")
public class MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TARGET_TABLE = "zzlearnerlearnershipassessments";
    private static final String SEQUENCE_NAME = "ZZLearnerLearnershipAssessments";

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

        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lla_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lla_unitstandard_xwalk", null, trxName);
        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lla_ms_user_xwalk", null, trxName);

        // 1. The exact set of source rows to migrate this run, with a stable row_number()
        // already computed (ordered by id) - this fixes the row set BEFORE reserving primary
        // keys, so the reserved block size exactly matches what gets inserted below.
        String sourceSql = "CREATE TEMP TABLE tmp_lla_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM ms_learnerlearnershipassessments src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_lla_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_lla_source", null, trxName);
            return "No new ms_learnerlearnershipassessments rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        // 2. unitstandardid -> zzunitstandard_id, via the already-migrated
        // zzlearnershipunitstandard join table's own zzunitstandard_id column - see class
        // Javadoc for the full reasoning.
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lla_unitstandard_xwalk AS "
                + "SELECT ms.unitstandardid AS source_id, MIN(zls.zzunitstandard_id) AS target_id "
                + "FROM (SELECT id, unitstandardid, row_number() OVER (ORDER BY id) AS rn FROM ms_learnershipunitstandard) ms "
                + "JOIN (SELECT zzunitstandard_id, row_number() OVER (ORDER BY zzmigrationcode) AS rn FROM zzlearnershipunitstandard) zls "
                + "  ON zls.rn = ms.rn "
                + "GROUP BY ms.unitstandardid", null, trxName);

        // 3. ms_user.id -> ad_user_id, matched by unique active email - replicates
        // MigrationSupport.buildMsUserToAdUserCrosswalk as a joinable temp table.
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lla_ms_user_xwalk AS "
                + "SELECT mu.id AS ms_user_id, au.ad_user_id AS ad_user_id "
                + "FROM ms_user mu "
                + "JOIN (SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                + "      FROM ad_user WHERE isactive='Y' AND email IS NOT NULL AND trim(email)<>'' "
                + "      GROUP BY lower(trim(email)) HAVING count(*)=1) au "
                + "  ON au.email_key = lower(trim(mu.email)) "
                + "WHERE mu.email IS NOT NULL AND trim(mu.email) <> ''", null, trxName);

        // 4. Reserve a contiguous block of primary keys - see class Javadoc for why this is
        // safe and what it mirrors.
        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        // adempiere.generate_uuid() - the exact function DB.isGenerateUUIDSupported() itself
        // tests for ("SELECT Generate_UUID() FROM Dual") and MTable/insert_Tree's own DDL uses -
        // confirmed present in this DB (pg_proc lookup), used directly rather than the
        // uuid-ossp uuid_generate_v4() some ms_ source tables happen to use for their own
        // internal columns.
        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearnerlearnershipassessments_id, ad_client_id, ad_org_id, created, createdby, "
                + "  updated, updatedby, isactive, zzlearnerlearnershipassessments_uu, "
                + "  zzlearnerlearnership_id, zzunitstandard_id, zzrpl, zzassessorperson_id, "
                + "  zzassessmentdate, zzmoderator_id, zzmoderationdate, assessment_status_id, "
                + "  zzispartialapproved, zzpartialapprovedby, zzdatepartialapproved, "
                + "  zzispreviouslyachieved, zzdateassessmentcaptured, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.created, ?, src.updated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  ll.zzlearnerlearnership_id, usx.target_id, "
                + "  CASE WHEN src.rpl IS NULL THEN NULL WHEN src.rpl <> 0 THEN 'Y' ELSE 'N' END, "
                + "  assessor.ad_user_id, src.assessmentdate, moderator.ad_user_id, src.moderationdate, "
                + "  ast.id, "
                + "  CASE WHEN src.ispartialapproved IS NULL THEN NULL WHEN src.ispartialapproved <> 0 THEN 'Y' ELSE 'N' END, "
                + "  partialapprover.ad_user_id, src.datepartialapproved, "
                + "  CASE WHEN src.ispreviouslyachieved IS NULL THEN NULL WHEN src.ispreviouslyachieved <> 0 THEN 'Y' ELSE 'N' END, "
                + "  src.dateassessmentcaptured, src.id "
                + "FROM tmp_lla_source src "
                + "LEFT JOIN zzlearnerlearnership ll ON ll.id = src.learnerlearnershipid "
                + "LEFT JOIN tmp_lla_unitstandard_xwalk usx ON usx.source_id = src.unitstandardid "
                + "LEFT JOIN assessment_status ast ON ast.id = src.assessmentstatusid "
                + "LEFT JOIN tmp_lla_ms_user_xwalk assessor ON assessor.ms_user_id = src.assessorid "
                + "LEFT JOIN tmp_lla_ms_user_xwalk moderator ON moderator.ms_user_id = src.moderatorid "
                + "LEFT JOIN tmp_lla_ms_user_xwalk partialapprover ON partialapprover.ms_user_id = src.partialapprovedby";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        DB.executeUpdateEx("DROP TABLE tmp_lla_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE tmp_lla_unitstandard_xwalk", null, trxName);
        DB.executeUpdateEx("DROP TABLE tmp_lla_ms_user_xwalk", null, trxName);

        return "Inserted " + inserted + " " + TARGET_TABLE + " row(s) (primary keys " + startId + ".."
                + (startId + rowCount - 1) + ").";
    }

    /**
     * Reserves a contiguous block of {@code count} primary keys from this table's AD_Sequence
     * row, the same way MSequence.getNextID() would per-row, just batched into one round-trip.
     * See class Javadoc for the full reasoning and why this is safe here.
     *
     * @return the first id in the reserved block (block is start..start+count-1)
     */
    private long reserveIdBlock(String trxName, long count) {
        PreparedStatement pst = null;
        ResultSet rs = null;
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
                        + "' - has AddZZLearnerLearnershipAssessmentsTable been run yet?");
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

        // CORRECTED 2026-07-31: DB.setParameter() has no branch for a boxed Long - only
        // String/Integer/BigDecimal/Timestamp/Boolean/byte[]/Clob (same pitfall
        // MigrationSupport.stampCreatedUpdated's own comment already documents) - passing
        // currentNext+count directly throws "Unknown parameter type" (caught 2026-07-31 while
        // fixing the same bug in the LearnerUnitStandard migrations - this one hadn't been run
        // at full scale yet to surface it). Fixed by binding as BigDecimal instead.
        DB.executeUpdateEx("UPDATE AD_Sequence SET CurrentNext = ? WHERE AD_Sequence_ID = ?",
                new Object[] { java.math.BigDecimal.valueOf(currentNext + count), adSequenceId }, trxName);

        return currentNext;
    }
}
