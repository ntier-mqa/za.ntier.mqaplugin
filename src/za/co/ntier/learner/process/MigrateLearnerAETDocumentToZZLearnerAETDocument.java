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
 * Phase 4 (see "Phase 4 - LearnerAET Family - Mapping.txt" Child 2): migrates the staged
 * learneraetdocument table (145,566 rows) into ZZLearnerAETDocument.
 *
 * <p>Set-based SQL, same architecture as {@link MigrateLearnerAETToZZLearnerAET}. Document_Type_ID
 * resolves via a direct join on the shared "Learner_Programme_Document_Type" reference table's
 * own "id" recon column (RESOLVED 2026-07-31 - see mapping doc's "STAGING PROJECT" section; not
 * left unresolved).
 */
@Process(name = "za.co.ntier.learner.process.MigrateLearnerAETDocumentToZZLearnerAETDocument")
public class MigrateLearnerAETDocumentToZZLearnerAETDocument extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String SOURCE_TABLE = "learneraetdocument";
    private static final String TARGET_TABLE = "zzlearneraetdocument";
    private static final String SEQUENCE_NAME = "ZZLearnerAETDocument";

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

        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lad_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE IF EXISTS tmp_lad_ms_user_xwalk", null, trxName);

        String sourceSql = "CREATE TEMP TABLE tmp_lad_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM " + SOURCE_TABLE + " src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_lad_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_lad_source", null, trxName);
            return "No new " + SOURCE_TABLE + " rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lad_ms_user_xwalk AS "
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
                + " (zzlearneraetdocument_id, ad_client_id, ad_org_id, created, createdby, updated, "
                + "  updatedby, isactive, zzlearneraetdocument_uu, zzlearneraet_id, document_type_id, "
                + "  original_file_name, saved_file_name, file_path, uploaded_by, updated_by, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.datecreated, ?, src.dateupdated, ?, "
                + "  CASE WHEN src.isdeleted THEN 'N' ELSE 'Y' END, adempiere.generate_uuid(), "
                + "  laet.zzlearneraet_id, doctype.id, src.originalfilename, src.savedfilename, "
                + "  src.filepath, uploadedbyx.ad_user_id, updatedbyx.ad_user_id, src.id "
                + "FROM tmp_lad_source src "
                + "LEFT JOIN zzlearneraet laet ON laet.id = src.learneraetid "
                + "LEFT JOIN learner_programme_document_type doctype ON doctype.id = src.learnerprogrammedocumenttypeid "
                + "LEFT JOIN tmp_lad_ms_user_xwalk uploadedbyx ON uploadedbyx.ms_user_id = src.createdby "
                + "LEFT JOIN tmp_lad_ms_user_xwalk updatedbyx ON updatedbyx.ms_user_id = src.updatedby";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        DB.executeUpdateEx("DROP TABLE tmp_lad_source", null, trxName);
        DB.executeUpdateEx("DROP TABLE tmp_lad_ms_user_xwalk", null, trxName);

        return "Inserted " + inserted + " " + TARGET_TABLE + " row(s) (primary keys " + startId + ".."
                + (startId + rowCount - 1) + ").";
    }

    /**
     * Reserves a contiguous block of {@code count} primary keys from this table's AD_Sequence
     * row - see MigrateMsLearnerLearnershipAssessmentsToZZLearnerLearnershipAssessments's
     * Javadoc for the full reasoning (unchanged here, including the BigDecimal fix for
     * DB.setParameter()'s lack of a boxed-Long branch).
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
                        + "' - has AddZZLearnerAETDocumentTable been run yet?");
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
                new Object[] { BigDecimal.valueOf(currentNext + count), adSequenceId }, trxName);

        return currentNext;
    }
}
