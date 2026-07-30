package za.co.ntier.learner.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Phase 4 (see "Phase 4 - LearnerUnitStandard Family - Mapping.txt" Section 2): migrates the
 * staged ms_learnerunitstandardassessments table (1,180,150 rows) into
 * ZZLearnerUnitStandardAssessments.
 *
 * <p>Set-based SQL, same architecture as
 * {@link MigrateMsLearnerUnitStandardToZZLearnerUnitStandard} and Phase 2's
 * ZZLearnerLearnershipAssessments migration (user decision 2026-07-21) - see that class's
 * Javadoc for the batch-primary-key-reservation technique (unchanged here).
 *
 * <p>Simpler than the parent: ZZLearnerUnitStandard_ID resolves via a direct join on the
 * parent's own "id" recon column (requires
 * {@link MigrateMsLearnerUnitStandardToZZLearnerUnitStandard} to have already run).
 * Assessment_Status_ID resolves via a direct join on the shared reference table's own "id" recon
 * column (populated when it was first created back in Phase 1) - no crosswalk needed. Actor
 * columns (assessorid/moderatorid) use the same ms_user email-match temp table technique as
 * every other set-based migration in this project.
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerUnitStandardAssessmentsToZZLearnerUnitStandardAssessments")
public class MigrateMsLearnerUnitStandardAssessmentsToZZLearnerUnitStandardAssessments extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TARGET_TABLE = "zzlearnerunitstandardassessments";
    private static final String SEQUENCE_NAME = "ZZLearnerUnitStandardAssessments";

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

        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lusa_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lusa_ms_user_xwalk", null, trxName);

        String sourceSql = "CREATE TEMP TABLE tmp_lusa_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM ms_learnerunitstandardassessments src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_lusa_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_lusa_source", null, trxName);
            return "No new ms_learnerunitstandardassessments rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lusa_ms_user_xwalk AS "
                + "SELECT mu.id AS ms_user_id, au.ad_user_id AS ad_user_id "
                + "FROM ms_user mu "
                + "JOIN (SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                + "      FROM ad_user WHERE isactive='Y' AND email IS NOT NULL AND trim(email)<>'' "
                + "      GROUP BY lower(trim(email)) HAVING count(*)=1) au "
                + "  ON au.email_key = lower(trim(mu.email)) "
                + "WHERE mu.email IS NOT NULL AND trim(mu.email) <> ''", null, trxName);

        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearnerunitstandardassessments_id, ad_client_id, ad_org_id, created, createdby, "
                + "  updated, updatedby, isactive, zzlearnerunitstandardassessments_uu, "
                + "  zzlearnerunitstandard_id, zzrpl, zzassessorperson_id, zzassessmentdate, "
                + "  zzmoderator_id, zzmoderationdate, assessment_status_id, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.created, ?, src.updated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  lus.zzlearnerunitstandard_id, "
                + "  CASE WHEN src.rpl IS NULL THEN NULL WHEN src.rpl <> 0 THEN 'Y' ELSE 'N' END, "
                + "  assessor.ad_user_id, src.assessmentdate, moderator.ad_user_id, src.moderationdate, "
                + "  ast.id, src.id "
                + "FROM tmp_lusa_source src "
                + "LEFT JOIN zzlearnerunitstandard lus ON lus.id = src.learnerunitstandardid "
                + "LEFT JOIN assessment_status ast ON ast.id = src.assessmentstatusid "
                + "LEFT JOIN tmp_lusa_ms_user_xwalk assessor ON assessor.ms_user_id = src.assessorid "
                + "LEFT JOIN tmp_lusa_ms_user_xwalk moderator ON moderator.ms_user_id = src.moderatorid";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        DB.executeUpdateEx("DROP TABLE tmp_lusa_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE tmp_lusa_ms_user_xwalk", null, trxName);

        return "Inserted " + inserted + " " + TARGET_TABLE + " row(s) (primary keys " + startId + ".."
                + (startId + rowCount - 1) + ").";
    }

    /**
     * Reserves a contiguous block of {@code count} primary keys from this table's AD_Sequence
     * row - see MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments's
     * Javadoc for the full reasoning (unchanged here).
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
                        + "' - has AddZZLearnerUnitStandardAssessmentsTable been run yet?");
            }
            adSequenceId = rs.getInt("AD_Sequence_ID");
            currentNext = rs.getLong("CurrentNext");
            incrementNo = rs.getInt("IncrementNo");
        } catch (SQLException e) {
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
