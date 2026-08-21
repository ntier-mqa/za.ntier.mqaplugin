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
 * Phase 4 (see "Phase 4 - LearnerInternship Family - Mapping.txt"): migrates the staged
 * ms_learnerinternship table (9,145 rows) into ZZLearnerInternship.
 *
 * <p>Set-based SQL, same architecture as every family since LearnerDocuments. Direct joins (no
 * temp table, just each reference table's own "id" recon column) to the 12 reference tables
 * {@link AddZZLearnerInternshipTable} builds: internship_type, internship_status,
 * internship_qualification_type, internship_disciplines, type_of_placement, placement_status,
 * highest_education_level, year_of_study, nqf_level, sic_code, graduate_intern, financial_year.
 * List value crosswalks (ZZSponsorship/ZZSocioEconomicStatus/ZZTerminationReason/
 * ZZEnrolmentStatusReason) and the ms_user email-match / organisation-bpartner crosswalks use the
 * same temp-table technique as every other set-based Migrate* process in this project.
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerInternshipToZZLearnerInternship")
public class MigrateMsLearnerInternshipToZZLearnerInternship extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String SOURCE_TABLE = "ms_learnerinternship";
    private static final String TARGET_TABLE = "zzlearnerinternship";
    private static final String SEQUENCE_NAME = "ZZLearnerInternship";

    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";

    private static final String[] TEMP_TABLES = { "tmp_li_source", "tmp_li_ms_user_xwalk", "tmp_li_bpartner_xwalk",
            "tmp_li_sponsorship_xwalk", "tmp_li_socioeconomicstatus_xwalk", "tmp_li_terminationreason_xwalk",
            "tmp_li_enrolmentstatusreason_xwalk", "tmp_li_levy_xwalk" };

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

        for (String tmp : TEMP_TABLES) {
            DB.executeUpdateEx("DROP TABLE IF EXISTS " + tmp, null, trxName);
        }

        String sourceSql = "CREATE TEMP TABLE tmp_li_source AS "
                + "SELECT src.*, row_number() OVER (ORDER BY src.id) AS rn "
                + "FROM " + SOURCE_TABLE + " src "
                + "WHERE NOT EXISTS (SELECT 1 FROM " + TARGET_TABLE + " z WHERE z.id = src.id) "
                + "ORDER BY src.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");
        DB.executeUpdateEx(sourceSql, null, trxName);

        int rowCount = DB.getSQLValueEx(trxName, "SELECT count(*) FROM tmp_li_source");
        if (rowCount == 0) {
            DB.executeUpdateEx("DROP TABLE tmp_li_source", null, trxName);
            return "No new " + SOURCE_TABLE + " rows to migrate.";
        }
        addLog("Migrating " + rowCount + " row(s)...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_li_ms_user_xwalk AS "
                + "SELECT mu.id AS ms_user_id, au.ad_user_id AS ad_user_id "
                + "FROM ms_user mu "
                + "JOIN (SELECT lower(trim(email)) AS email_key, min(ad_user_id) AS ad_user_id "
                + "      FROM ad_user WHERE isactive='Y' AND email IS NOT NULL AND trim(email)<>'' "
                + "      GROUP BY lower(trim(email)) HAVING count(*)=1) au "
                + "  ON au.email_key = lower(trim(mu.email)) "
                + "WHERE mu.email IS NOT NULL AND trim(mu.email) <> ''", null, trxName);

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_li_bpartner_xwalk AS "
                + "SELECT o.id AS source_id, MIN(bp.c_bpartner_id) AS target_id "
                + "FROM ms_organisation o "
                + "JOIN c_bpartner bp ON trim(bp.zz_sdl_no) = trim(o.sdlnumber) "
                + "WHERE o.sdlnumber IS NOT NULL AND trim(o.sdlnumber) <> '' "
                + "  AND bp.zz_sdl_no IS NOT NULL AND trim(bp.zz_sdl_no) <> '' "
                + "GROUP BY o.id", null, trxName);

        createListValueCrosswalk(trxName, "tmp_li_sponsorship_xwalk", "ms_lkpsponsorship", 1000251);
        createListValueCrosswalk(trxName, "tmp_li_socioeconomicstatus_xwalk", "ms_lkpsocioeconomicstatus", 1000250);
        createListValueCrosswalk(trxName, "tmp_li_terminationreason_xwalk", "ms_lkpterminationreason", 1000254);
        createListValueCrosswalk(trxName, "tmp_li_enrolmentstatusreason_xwalk", "ms_lkpenrolmentstatusreason",
                1000255);

        int yesNoNaReferenceId = AddColumnsSupport.findListReference(getCtx(), YES_NO_NA_REFERENCE_NAME, trxName);
        if (yesNoNaReferenceId == 0) {
            throw new AdempiereException("List reference '" + YES_NO_NA_REFERENCE_NAME
                    + "' not found - run AddZZLearnerInternshipTable first.");
        }
        createListValueCrosswalk(trxName, "tmp_li_levy_xwalk", "ms_lkpyesnonotapplicable", yesNoNaReferenceId);

        long startId = reserveIdBlock(trxName, rowCount);
        addLog("Reserved primary key block " + startId + ".." + (startId + rowCount - 1) + " for " + SEQUENCE_NAME);

        String insertSql =
                "INSERT INTO " + TARGET_TABLE
                + " (zzlearnerinternship_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby, "
                + "  isactive, zzlearnerinternship_uu, zzlearner_id, "
                + "  internship_type_id, internship_status_id, internship_qualification_type_id, "
                + "  internship_disciplines_id, type_of_placement_id, placement_status_id, "
                + "  highest_education_level_id, year_of_study_id, nqf_level_id, sic_code_id, "
                + "  graduate_intern_id, financial_year_id, "
                + "  employer_id, alternate_employer_id, placement_employer_id, "
                + "  zzsponsorship, zzsocioeconomicstatus, zzterminationreason, zzenrolmentstatusreason, zzlevy, "
                + "  registered_by, terminated_captured_by, "
                + "  zzinternshipstartdate, zzinternshipenddate, zzextensiondate, zzterminationdate, "
                + "  zzdateterminationcaptured, zzdateextensioncaptured, zzregistrationdate, zzcompletiondate, "
                + "  zzmostrecentregistrationdate, zzactualterminateddate, zzqualificationachievementdate, "
                + "  zzemploymentstartdate, zzemploymentenddate, zzcontractdate, "
                + "  zzisindustryspecific, zzindustrynonspecific, zzcontract, zzempcontract, zzempcontractcopy, "
                + "  zzdocumentsreceived, zzpreviouslyemployed, zzwpagreement, "
                + "  contractnumber, servicelevelagreementnumber, nonspecificsiccode, zzresponsibleseta, "
                + "  zzasspartner, zzregsaqa, zzcurregnumber, zzqcto, zzoccupation, extensionreason, "
                + "  terminationreasontext, employercontractnumber, employercontactnumber, fet, het, "
                + "  physical_address1, physical_address2, physical_address3, physical_postal_code, "
                + "  institution, ofo_occupation, qualification, training_provider_public_private, id) "
                + "SELECT " + startId + " + src.rn - 1, ?, ?, src.created, ?, src.updated, ?, "
                + "  CASE WHEN src.isdeleted = 0 THEN 'Y' ELSE 'N' END, adempiere.generate_uuid(), "
                + "  lx.zzlearner_id, "
                + "  itype.id, istatus.id, iqtype.id, idisc.id, top.id, pstatus.id, hel.id, yos.id, nqf.id, "
                + "  sic.id, gi.id, finyear.id, "
                + "  bpx.target_id, altbpx.target_id, placebpx.target_id, "
                + "  spx.target_value, sesx.target_value, trx_.target_value, esrx.target_value, "
                + "  levyx.target_value, "
                + "  registeredbyx.ad_user_id, termcapturedbyx.ad_user_id, "
                + "  src.internshipstartdate, src.internshipenddate, src.extensiondate, src.terminationdate, "
                + "  src.dateterminationcaptured, src.dateextensioncaptured, src.registrationdate, "
                + "  src.completiondate, src.mostrecentregistrationdate, src.actualterminateddate, "
                + "  src.qualificationachievementdate, src.employmentstartdate, src.employmentenddate, "
                + "  src.contractdate, "
                + "  CASE WHEN src.isindustryspecific IS NULL THEN NULL WHEN src.isindustryspecific <> 0 "
                + "       THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.industrynonspecific IS NULL THEN NULL WHEN src.industrynonspecific <> 0 "
                + "       THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.contract IS NULL THEN NULL WHEN src.contract <> 0 THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.empcontract IS NULL THEN NULL WHEN src.empcontract=1 THEN 'N' "
                + "       WHEN src.empcontract=2 THEN 'Y' ELSE NULL END, "
                + "  CASE WHEN src.empcontractcopy IS NULL THEN NULL WHEN src.empcontractcopy=1 THEN 'N' "
                + "       WHEN src.empcontractcopy=2 THEN 'Y' ELSE NULL END, "
                + "  CASE WHEN src.documentsreceived IS NULL THEN NULL WHEN src.documentsreceived <> 0 "
                + "       THEN 'Y' ELSE 'N' END, "
                + "  CASE WHEN src.prevemployed IS NULL THEN NULL WHEN src.prevemployed=1 THEN 'N' "
                + "       WHEN src.prevemployed=2 THEN 'Y' ELSE NULL END, "
                + "  CASE WHEN src.wpagreement IS NULL THEN NULL WHEN src.wpagreement=1 THEN 'N' "
                + "       WHEN src.wpagreement=2 THEN 'Y' ELSE NULL END, "
                + "  src.contractnumber, src.servicelevelagreementnumber, src.nonspecificsiccode, "
                + "  src.responsibleseta, src.asspartner, src.regsaqa, src.curregnumber, src.qcto, "
                + "  src.occupation, src.extensionreason, src.terminationreason, src.employercontractnumber, "
                + "  src.employercontactnumber, src.fet, src.het, "
                + "  src.physicaladdress1, src.physicaladdress2, src.physicaladdress3, src.physicalcode, "
                + "  src.institutionid, src.ofooccupationid, src.qualificationid, "
                + "  src.trainingproviderpublicprivateid, src.id "
                + "FROM tmp_li_source src "
                + "LEFT JOIN ms_learner_xref lx ON lx.ms_learner_id = src.learnerid "
                + "LEFT JOIN internship_type itype ON itype.id = src.internshiptypeid "
                + "LEFT JOIN internship_status istatus ON istatus.id = src.internshipstatusid "
                + "LEFT JOIN internship_qualification_type iqtype ON iqtype.id = src.internshipqualificationtypeid "
                + "LEFT JOIN internship_disciplines idisc ON idisc.id = src.internshipdisciplinesid "
                + "LEFT JOIN type_of_placement top ON top.id = src.typeofplacementid "
                + "LEFT JOIN placement_status pstatus ON pstatus.id = src.placementstatusid "
                + "LEFT JOIN highest_education_level hel ON hel.id = src.highesteducationlevelid "
                + "LEFT JOIN year_of_study yos ON yos.id = src.yearofstudyid "
                + "LEFT JOIN nqf_level nqf ON nqf.id = src.nqflevelid "
                + "LEFT JOIN sic_code sic ON sic.id = src.siccodeid "
                + "LEFT JOIN graduate_intern gi ON gi.id = src.graduateinternid "
                + "LEFT JOIN financial_year finyear ON finyear.id = src.financialyearid "
                + "LEFT JOIN tmp_li_bpartner_xwalk bpx ON bpx.source_id = src.employerid "
                + "LEFT JOIN tmp_li_bpartner_xwalk altbpx ON altbpx.source_id = src.alternateemployerid "
                + "LEFT JOIN tmp_li_bpartner_xwalk placebpx ON placebpx.source_id = src.placementemployerid "
                + "LEFT JOIN tmp_li_sponsorship_xwalk spx ON spx.source_id = src.sponsorshipid "
                + "LEFT JOIN tmp_li_socioeconomicstatus_xwalk sesx ON sesx.source_id = src.socioeconomicstatusid "
                + "LEFT JOIN tmp_li_terminationreason_xwalk trx_ ON trx_.source_id = src.terminationreasonid "
                + "LEFT JOIN tmp_li_enrolmentstatusreason_xwalk esrx ON esrx.source_id = src.enrolmentstatusreasonid "
                + "LEFT JOIN tmp_li_levy_xwalk levyx ON levyx.source_id = src.levyyesnoid "
                + "LEFT JOIN tmp_li_ms_user_xwalk registeredbyx ON registeredbyx.ms_user_id = src.registeredby "
                + "LEFT JOIN tmp_li_ms_user_xwalk termcapturedbyx ON termcapturedbyx.ms_user_id = "
                + "src.terminatedcapturedby";

        int inserted = DB.executeUpdateEx(insertSql,
                new Object[] { adClientId, adOrgId, createdBy, createdBy }, trxName);

        for (String tmp : TEMP_TABLES) {
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
                        + "' - has AddZZLearnerInternshipTable been run yet?");
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
