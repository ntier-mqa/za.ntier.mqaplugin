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
 * Phase 4 (see "Phase 4 - LearnerWorkExperience Family - Mapping.txt"): migrates the staged
 * ms_learnerworkexperience table (14,370 rows) into ZZLearnerWorkExperience.
 *
 * <p>Set-based SQL, same architecture as {@link MigrateLearnerAETToZZLearnerAET} - used here for
 * consistency with every family since LearnerDocuments, even though this one is small enough for
 * the per-row pattern by the established size threshold.
 *
 * <p>ZZWorkExperienceStatus_ID resolves via a direct join on the new Work_Experience_Status
 * reference table's own "id" recon column. ZZLevy resolves via the shared
 * "Yes_No_Not_Applicable" List reference (found by name, same as every other family that uses
 * it). Institution/Work_Experience_Type/Qualification are carried across as their raw source
 * values (unresolved - see AddZZLearnerWorkExperienceTable's Javadoc for why Qualification in
 * particular is NOT joined to zzqualification).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerWorkExperienceToZZLearnerWorkExperience")
public class MigrateMsLearnerWorkExperienceToZZLearnerWorkExperience extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String SOURCE_TABLE = "ms_learnerworkexperience";
    private static final String TARGET_TABLE = "zzlearnerworkexperience";
    private static final String SEQUENCE_NAME = "ZZLearnerWorkExperience";
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

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

        for (String tmp : new String[] { "tmp_lwe_source", "tmp_lwe_bpartner_xwalk", "tmp_lwe_levy_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE IF EXISTS " + tmp, null, trxName);
        }

        String sourceSql = "CREATE TEMP TABLE tmp_lwe_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM " + SOURCE_TABLE + " src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_lwe_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_lwe_source", null, trxName);
            return "No new " + SOURCE_TABLE + " rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lwe_bpartner_xwalk AS "
                + "SELECT o.id AS source_id, MIN(bp.c_bpartner_id) AS target_id "
                + "FROM ms_organisation o "
                + "JOIN c_bpartner bp ON trim(bp.zz_sdl_no) = trim(o.sdlnumber) "
                + "WHERE o.sdlnumber IS NOT NULL AND trim(o.sdlnumber) <> '' "
                + "  AND bp.zz_sdl_no IS NOT NULL AND trim(bp.zz_sdl_no) <> '' "
                + "GROUP BY o.id", null, trxName);

        int yesNoNaReferenceId = AddColumnsSupport.findListReference(getCtx(), YES_NO_NA_REFERENCE_NAME, trxName);
        if (yesNoNaReferenceId == 0) {
            throw new AdempiereException("List reference '" + YES_NO_NA_REFERENCE_NAME
                    + "' not found - run AddZZLearnerWorkExperienceTable first.");
        }
        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_lwe_levy_xwalk AS "
                + "SELECT lkp.id AS source_id, MIN(arl.value) AS target_value "
                + "FROM ms_lkpyesnonotapplicable lkp "
                + "JOIN ad_ref_list arl ON arl.ad_reference_id = " + yesNoNaReferenceId
                + "  AND lower(trim(arl.name)) = lower(trim(lkp.description)) "
                + "GROUP BY lkp.id", null, trxName);

        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearnerworkexperience_id, ad_client_id, ad_org_id, created, createdby, updated, "
                + "  updatedby, isactive, zzlearnerworkexperience_uu, zzlearner_id, zzreferencenumber, "
                + "  zzstartdate, zzenddate, employer_id, alternate_employer_id, zzlevy, "
                + "  zzworkexperiencestatus_id, zzempcontract, zzempcontractcopy, zzresponsibleseta, "
                + "  zzregsaqa, zzqcto, zzcurregnumber, zzasspartner, zzdocumentsreceived, "
                + "  zzpreviouslyemployed, zzwpagreement, zzoccupation, institution, "
                + "  work_experience_type, qualification, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.created, ?, src.updated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  lx.zzlearner_id, src.referencenumber, src.startdate, src.enddate, "
                + "  bpx.target_id, altbpx.target_id, levyx.target_value, "
                + "  wes.id, "
                + "  CASE WHEN src.empcontract IS NULL THEN NULL WHEN src.empcontract=1 THEN 'N' "
                + "       WHEN src.empcontract=2 THEN 'Y' ELSE NULL END, "
                + "  CASE WHEN src.empcontractcopy IS NULL THEN NULL WHEN src.empcontractcopy=1 THEN 'N' "
                + "       WHEN src.empcontractcopy=2 THEN 'Y' ELSE NULL END, "
                + "  src.responsibleseta, src.regsaqa, src.qcto, src.curregnumber, src.asspartner, "
                + "  CASE WHEN src.documentsreceived IS NULL THEN NULL WHEN src.documentsreceived <> 0 "
                + "       THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.prevemployed IS NULL THEN NULL WHEN src.prevemployed=1 THEN 'N' "
                + "       WHEN src.prevemployed=2 THEN 'Y' ELSE NULL END, "
                + "  CASE WHEN src.wpagreement IS NULL THEN NULL WHEN src.wpagreement=1 THEN 'N' "
                + "       WHEN src.wpagreement=2 THEN 'Y' ELSE NULL END, "
                + "  src.occupation, src.institutionid, src.workexperienceid, src.qualificationid, src.id "
                + "FROM tmp_lwe_source src "
                + "LEFT JOIN ms_learner_xref lx ON lx.ms_learner_id = src.learnerid "
                + "LEFT JOIN tmp_lwe_bpartner_xwalk bpx ON bpx.source_id = src.employerid "
                + "LEFT JOIN tmp_lwe_bpartner_xwalk altbpx ON altbpx.source_id = src.alternateemployerid "
                + "LEFT JOIN tmp_lwe_levy_xwalk levyx ON levyx.source_id = src.levyyesnoid "
                + "LEFT JOIN work_experience_status wes ON wes.id = src.workexperiencestatusid";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        for (String tmp : new String[] { "tmp_lwe_source", "tmp_lwe_bpartner_xwalk", "tmp_lwe_levy_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE " + tmp, null, trxName);
        }

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
                        + "' - has AddZZLearnerWorkExperienceTable been run yet?");
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
