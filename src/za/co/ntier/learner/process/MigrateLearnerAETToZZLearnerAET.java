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
 * Phase 4 (see "Phase 4 - LearnerAET Family - Mapping.txt"): migrates the staged learneraet
 * table (127,422 rows) into ZZLearnerAET.
 *
 * <p>Set-based SQL, same architecture as {@link MigrateMsLearnerUnitStandardToZZLearnerUnitStandard}
 * - 127,422 rows exceeds the largest per-row Migrate* built so far, applying the now-established
 * pattern without re-confirming.
 *
 * <p>Direct joins (no temp table, just the target's own "id" recon column): ZZLearner_ID (via
 * ms_learner_xref), ZZAET_ID (zzaetprogramme), ZZProvider_ID (zzprovider), ZZStatus_ID
 * (aet_status), ZZAETTimePeriod_ID (aet_time_period), ZZGrantType_ID (zzgranttype - currently
 * empty, resolves once MigrateMsGrantTypeToZZGrantType runs). List value crosswalks
 * (ZZSponsorship/ZZSocioEconomicStatus/ZZTerminationReason/ZZEnrolmentStatusReason) and the
 * ms_user email-match / organisation-bpartner crosswalks use the same temp-table technique as
 * every other set-based Migrate* process in this project.
 */
@Process(name = "za.co.ntier.learner.process.MigrateLearnerAETToZZLearnerAET")
public class MigrateLearnerAETToZZLearnerAET extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String SOURCE_TABLE = "learneraet";
    private static final String TARGET_TABLE = "zzlearneraet";
    private static final String SEQUENCE_NAME = "ZZLearnerAET";

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

        for (String tmp : new String[] { "tmp_la_source", "tmp_la_ms_user_xwalk", "tmp_la_bpartner_xwalk",
                "tmp_la_sponsorship_xwalk", "tmp_la_socioeconomicstatus_xwalk", "tmp_la_terminationreason_xwalk",
                "tmp_la_enrolmentstatusreason_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE IF EXISTS " + tmp, null, trxName);
        }

        String sourceSql = "CREATE TEMP TABLE tmp_la_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM " + SOURCE_TABLE + " src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_la_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_la_source", null, trxName);
            return "No new " + SOURCE_TABLE + " rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_la_ms_user_xwalk AS "
                + "SELECT mu.id AS ms_user_id, au.ad_user_id AS ad_user_id "
                + "FROM ms_user mu "
                + "JOIN (SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                + "      FROM ad_user WHERE isactive='Y' AND email IS NOT NULL AND trim(email)<>'' "
                + "      GROUP BY lower(trim(email)) HAVING count(*)=1) au "
                + "  ON au.email_key = lower(trim(mu.email)) "
                + "WHERE mu.email IS NOT NULL AND trim(mu.email) <> ''", null, trxName);

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_la_bpartner_xwalk AS "
                + "SELECT o.id AS source_id, MIN(bp.c_bpartner_id) AS target_id "
                + "FROM ms_organisation o "
                + "JOIN c_bpartner bp ON trim(bp.zz_sdl_no) = trim(o.sdlnumber) "
                + "WHERE o.sdlnumber IS NOT NULL AND trim(o.sdlnumber) <> '' "
                + "  AND bp.zz_sdl_no IS NOT NULL AND trim(bp.zz_sdl_no) <> '' "
                + "GROUP BY o.id", null, trxName);

        createListValueCrosswalk(trxName, "tmp_la_sponsorship_xwalk", "ms_lkpsponsorship", 1000251);
        createListValueCrosswalk(trxName, "tmp_la_socioeconomicstatus_xwalk", "ms_lkpsocioeconomicstatus", 1000250);
        createListValueCrosswalk(trxName, "tmp_la_terminationreason_xwalk", "ms_lkpterminationreason", 1000254);
        createListValueCrosswalk(trxName, "tmp_la_enrolmentstatusreason_xwalk", "ms_lkpenrolmentstatusreason", 1000255);

        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearneraet_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby, "
                + "  isactive, zzlearneraet_uu, zzlearner_id, zzaet_id, zzstartdate, zzenddate, "
                + "  zzstatus_id, zzsponsorship, zzstatuseffectivedate, zzcompletiondate, "
                + "  zzprovider_id, employer_id, zzsocioeconomicstatus, zzextensiondate, "
                + "  zzextensionreason, zzterminationdate, zzterminationreason, zzdateextensioncaptured, "
                + "  zzregistrationdate, zzregisteredby, zzenrolmentstatusreason, "
                + "  zzmostrecentregistrationdate, zzactualterminateddate, zzregistrationnumber, "
                + "  zzgranttype_id, zzsetmisreg, zzsetmiscomp, zzterminatedcapturedby, "
                + "  zzelementarycompletiondate, zzdeclaredcompetentby, zzaettimeperiod_id, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.datecreated, ?, src.dateupdated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  lx.zzlearner_id, aetp.id, src.startdate, src.enddate, "
                + "  status.id, spx.target_value, src.statuseffectivedate, src.completiondate, "
                + "  zzp.zzprovider_id, bpx.target_id, sesx.target_value, src.extensiondate, "
                + "  src.extensionreason, src.terminationdate, trx_.target_value, src.dateextensioncaptured, "
                + "  src.registrationdate, registeredbyx.ad_user_id, esrx.target_value, "
                + "  src.mostrecentregistrationdate, src.actualterminateddate, src.registrationnumber, "
                + "  zzgt.zzgranttype_id, "
                + "  CASE WHEN src.setmisreg IS NULL THEN NULL WHEN src.setmisreg <> 0 THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.setmiscomp IS NULL THEN NULL WHEN src.setmiscomp <> 0 THEN 'Y' ELSE 'N' END, "
                + "  termcapturedbyx.ad_user_id, src.elementarycompletiondate, declaredcompetentbyx.ad_user_id, "
                + "  timeperiod.id, src.id "
                + "FROM tmp_la_source src "
                + "LEFT JOIN ms_learner_xref lx ON lx.ms_learner_id = src.learnerid "
                + "LEFT JOIN zzaetprogramme aetp ON aetp.id = src.aetid "
                + "LEFT JOIN zzprovider zzp ON zzp.id = src.providerid "
                + "LEFT JOIN aet_status status ON status.id = src.statusid "
                + "LEFT JOIN aet_time_period timeperiod ON timeperiod.id = src.aettimeperiodid "
                + "LEFT JOIN zzgranttype zzgt ON zzgt.id = src.granttypeid "
                + "LEFT JOIN tmp_la_bpartner_xwalk bpx ON bpx.source_id = src.employerid "
                + "LEFT JOIN tmp_la_sponsorship_xwalk spx ON spx.source_id = src.sponsorshipid "
                + "LEFT JOIN tmp_la_socioeconomicstatus_xwalk sesx ON sesx.source_id = src.socioeconomicstatusid "
                + "LEFT JOIN tmp_la_terminationreason_xwalk trx_ ON trx_.source_id = src.terminationreasonid "
                + "LEFT JOIN tmp_la_enrolmentstatusreason_xwalk esrx ON esrx.source_id = src.enrolmentstatusreasonid "
                + "LEFT JOIN tmp_la_ms_user_xwalk registeredbyx ON registeredbyx.ms_user_id = src.registeredby "
                + "LEFT JOIN tmp_la_ms_user_xwalk termcapturedbyx ON termcapturedbyx.ms_user_id = src.terminatedcapturedby "
                + "LEFT JOIN tmp_la_ms_user_xwalk declaredcompetentbyx ON declaredcompetentbyx.ms_user_id = src.declaredcompetentby";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        for (String tmp : new String[] { "tmp_la_source", "tmp_la_ms_user_xwalk", "tmp_la_bpartner_xwalk",
                "tmp_la_sponsorship_xwalk", "tmp_la_socioeconomicstatus_xwalk", "tmp_la_terminationreason_xwalk",
                "tmp_la_enrolmentstatusreason_xwalk" }) {
            DB.executeUpdateEx("DROP TABLE " + tmp, null, trxName);
        }

        return "Inserted " + inserted + " " + TARGET_TABLE + " row(s) (primary keys " + startId + ".."
                + (startId + rowCount - 1) + ").";
    }

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
                        + "' - has AddZZLearnerAETTable been run yet?");
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
