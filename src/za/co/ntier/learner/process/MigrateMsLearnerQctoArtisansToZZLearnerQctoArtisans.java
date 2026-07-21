package za.co.ntier.learner.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZLearnerQCTOArtisans;

/**
 * Migrates the staged ms_learnerqctoartisans table (each learner's enrolment in a QCTO
 * artisan trade programme) into ZZLearnerQCTOArtisans. This is one of the 3 original tables
 * requested. See "Column Mapping - QCTO Learner Programmes" doc, tab "LearnerQCTOArtisans"
 * for the full column-by-column reasoning.
 *
 * <p>Must run AFTER: MigrateMsPersonToZZPerson/MigrateMsLearnerToZZLearner (ms_learner_xref),
 * MigrateMsQctoLearnershipToZZQctoLearnership, MigrateMsQctoProgrammeStatusToZZQctoProgrammeStatus,
 * MigrateMsProviderToZZProvider, MigrateMsWorkplaceApprovalToZZWorkplaceApproval,
 * MigrateMsAssessmentCentreToZZAssessmentCentre, MigrateMsQualificationToZZQualification,
 * MigrateMsLearnershipToZZLearnership, MigrateMsGrantTypeToZZGrantType - every FK column
 * below depends on one of those crosswalks.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzlearnerqctoartisans ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>AD_User actor columns (enrolledby, certificatecreatedby, terminatedcapturedby,
 * extensioncapturedby, registeredby, approvedby, nambconfirmationuser) are resolved via
 * ms_user.email -&gt; ad_user.email, link-only - NEVER creates an AD_User, same rule as
 * zzperson.ad_user_id.
 *
 * <p>The Section A/B columns AddZZLearnerQctoArtisansColumns.java added (2026-07-16) are set
 * via the generic {@code PO.set_ValueOfColumn(String, Object)} API (no typed setter exists yet
 * on the generated model class - see MigrateMsProviderToZZProvider's Javadoc for why). See
 * "ZZLearnerQCTOArtisans - New Columns to Add.txt" for the full column-by-column reasoning:
 * <ul>
 *   <li>Is_Approved, Namb_Confirmation converted via {@link MigrationSupport#flagToYN(Integer)}
 *       (confirmed by correlation - e.g. isapproved=1 always coincides with approvedby/
 *       dateapproved being populated - NOT the lkpYesNo id convention).</li>
 *   <li>Previous_Employed, WP_Agreement, Is_Terms_Employment, Emp_Contract, Emp_Contract_Copy
 *       converted via {@link MigrationSupport#yesNoIdToFlag(Integer)} (confirmed only 1/2/null
 *       across all staged rows - the lkpYesNo id convention).</li>
 *   <li>Approved_By, Namb_Confirmation_User - same ms_user email-match crosswalk as the other
 *       actor columns above, link-only.</li>
 *   <li>Employer_ID - CONFIRMED 2026-07-16 (employerid matches ms_organisation.id 1085/1085
 *       times) resolved via the SAME MigrationSupport.buildOrganisationToBPartnerCrosswalk used
 *       for organisationid on Provider/WorkplaceApproval/AssessmentCentre.</li>
 * </ul>
 *
 * <p>FIXED 2026-07-16: ZZTerminationReason/ZZTerminationReasonText were previously swapped -
 * ZZTerminationReason (a List reference matching lkpTerminationReason verbatim) was being set
 * from the raw free-text terminationreason column, and ZZTerminationReasonText (plain String)
 * was being set from terminationreasonid's resolved description - backwards on both counts.
 * Now: ZZTerminationReason = terminationreasonid resolved via lkpTerminationReason.description;
 * ZZTerminationReasonText = raw terminationreason free text.
 *
 * <p>Still NOT handled (all documented as "Unmapped"/"Ignored" in the mapping doc - no target
 * column exists, redundant with an already-resolved column, or an open item blocks it):
 * <ul>
 *   <li>extractrecordid - 0 of 1230 rows populated, dead artifact, same as migrationrecordid.</li>
 *   <li>previouslearnershipcode / previouslearnershiptitle - redundant text mirrors of
 *       previouslearnership id, no separate target column.</li>
 *   <li>accountnumber - the MSSQL row's own UUID, not business data.</li>
 *   <li>financialyearid -&gt; ZZFinancialYear_ID - FinancialYear-to-C_Year crosswalk not
 *       yet designed (Open Item #3 in the mapping doc).</li>
 *   <li>qctoartisantypeid -&gt; ZZQctoArtisanType - DATA DEFECT: target column is
 *       char(1) but the two valid list values are "Artisan"/"ARPL" (multi-char), neither
 *       fits. NOT setting this until the business decides how to resolve it (Open Item #2).
 *       Not touching AD_Column or the physical column without sign-off.</li>
 *   <li>leadsdprovidercontactid / wacontactid / secondarysdprovidercontactid /
 *       secondarywacontactid / accontactid -&gt; zz_formcontact FKs - the existing 1036
 *       zz_formcontact rows are a different contact family (PersonAddress-related) from an
 *       earlier migration, not reusable here. Creating a NEW zz_formcontact row per
 *       enrolment row also looked like the wrong granularity (contacts conceptually belong
 *       to the Provider/WorkplaceApproval/AssessmentCentre entity, not to each learner's
 *       enrolment) and zz_formcontact's ZZ_ContactType reference list (Main/Org/Physical/
 *       Postal/Vacation/etc.) has no value that obviously fits "provider contact person"
 *       either. Left unset pending a decision on how contacts should really be modelled.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerQctoArtisansToZZLearnerQctoArtisans")
public class MigrateMsLearnerQctoArtisansToZZLearnerQctoArtisans extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;

    private final List<String> errors = new ArrayList<>();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        int personMigrated = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM ms_learner_xref");
        if (personMigrated <= 0) {
            addLog("WARNING: ms_learner_xref is empty - run MigrateMsPersonToZZPerson and "
                    + "MigrateMsLearnerToZZLearner first, or every learnerid on this table "
                    + "will resolve to nothing.");
        }

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzlearnerqctoartisans WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZLearnerQCTOArtisans row(s)...");
            DB.executeUpdateEx("DELETE FROM zzlearnerqctoartisans WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        // ms_learner_xref has different column names to the generic recon convention (no
        // "id" column - it's ms_learner_id/zzlearner_id) so it can't go through
        // buildIdCrosswalk() as-is; load it directly instead.
        Map<Integer, Integer> learnerCrosswalk = loadLearnerCrosswalk();
        Map<Integer, Integer> qctoLearnershipCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctolearnership", "zzqctolearnership_id", get_TrxName());
        Map<Integer, Integer> qctoProgrammeStatusCrosswalk = MigrationSupport.buildIdCrosswalk("zzqctoprogrammestatus", "zzqctoprogrammestatus_id", get_TrxName());
        Map<Integer, Integer> providerCrosswalk = MigrationSupport.buildIdCrosswalk("zzprovider", "zzprovider_id", get_TrxName());
        Map<Integer, Integer> waCrosswalk = MigrationSupport.buildIdCrosswalk("zzworkplaceapproval", "zzworkplaceapproval_id", get_TrxName());
        Map<Integer, Integer> acCrosswalk = MigrationSupport.buildIdCrosswalk("zzassessmentcentre", "zzassessmentcentre_id", get_TrxName());
        Map<Integer, Integer> qualificationCrosswalk = MigrationSupport.buildIdCrosswalk("zzqualification", "zzqualification_id", get_TrxName());
        Map<Integer, Integer> learnershipCrosswalk = MigrationSupport.buildIdCrosswalk("zzlearnership", "zzlearnership_id", get_TrxName());
        Map<Integer, Integer> grantTypeCrosswalk = MigrationSupport.buildIdCrosswalk("zzgranttype", "zzgranttype_id", get_TrxName());
        Map<Integer, Integer> msUserToAdUser = MigrationSupport.buildMsUserToAdUserCrosswalk(get_TrxName());
        // ZZSocioEconomicStatus/ZZSponsorship/ZZProject/ZZArtisanProject/ZZEnrolmentStatusReason/
        // ZZQualificationRequirements are List references (AD_Reference_ID=17), NOT plain
        // String columns as originally assumed - confirmed 2026-07-16 after a live run failed
        // PO validation. buildListValueCrosswalk() matches by name against each column's own
        // AD_Ref_List, returning the correct short Value code to store (see its Javadoc). The
        // AD_Reference_ID for each was read directly off zzlearnerqctoartisans' AD_Column rows.
        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsocioeconomicstatus", 1000250, get_TrxName());
        Map<Integer, String> sponsorshipMap = MigrationSupport.buildListValueCrosswalk("ms_lkpsponsorship", 1000251, get_TrxName());
        Map<Integer, String> projectMap = MigrationSupport.buildListValueCrosswalk("ms_lkpproject", 1000252, get_TrxName());
        Map<Integer, String> artisanProjectMap = MigrationSupport.buildListValueCrosswalk("ms_lkpartisanproject", 1000322, get_TrxName());
        Map<Integer, String> enrolmentStatusReasonMap = MigrationSupport.buildListValueCrosswalk("ms_lkpenrolmentstatusreason", 1000255, get_TrxName());
        // terminationReasonMap feeds ZZTerminationReason (also a List reference, 1000254) via
        // plain description text - unlike the columns above, this happens to be correct as-is
        // because that specific list's Value equals its Name for all 8 entries (confirmed
        // directly), not because it's a String column - ZZTerminationReasonText (the real
        // String column) gets the raw source text separately, see processOneRow().
        Map<Integer, String> terminationReasonMap = MigrationSupport.buildDescriptionMap("ms_lkpterminationreason", get_TrxName());
        Map<Integer, String> qualificationRequirementsMap = MigrationSupport.buildListValueCrosswalk("ms_lkpqualificationentryrequirements", 1000332, get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(get_TrxName());

        String sql =
                "SELECT id, learnerid, qctolearnershipid, agreementreferencenumber, commencementdate, "
                + "       completiondate, contractnumber, qctoprogrammestatusid, socioeconomicstatusid, "
                + "       sponsorshipid, projectid, leadsdproviderid, secondarysdproviderid, waid, "
                + "       secondarywaid, acid, enrolledby, enrolmentdate, isapproved, approvedby, "
                + "       dateapproved, certificatenumber, "
                + "       certificatecreatedby, datecertificatecreated, statuseffectivedate, studentnumber, "
                + "       extensiondate, extensionreason, terminationdate, terminationreason, "
                + "       terminationreasonid, artisanprojectid, terminatedcapturedby, "
                + "       dateterminationcaptured, extensioncapturedby, dateextensioncaptured, "
                + "       registrationdate, registeredby, enrolmentstatusreasonid, "
                + "       mostrecentregistrationdate, actualterminateddate, qualificationid, "
                + "       completionprocesseddate, previouslearnership, previousemployed, learneremployed, "
                + "       wpagreement, durationlearneremployed, istermsemployment, termsemployment, "
                + "       empcontract, empcontractcopy, responsibleseta, asspartner, regsaqa, curregnumber, "
                + "       qcto, occupation, "
                + "       granttypeid, leadsdproviderlevyyesnoid, walevyyesnoid, tradetestserialnumber, "
                + "       nambconfirmation, nambconfirmationdate, nambconfirmationuser, "
                + "       secondarysdproviderlevyyesnoid, secondarywalevyyesnoid, qualificationrequirementsid, "
                + "       aclevyyesnoid, employerid, created, updated, isdeleted "
                + "FROM ms_learnerqctoartisans "
                + "WHERE NOT EXISTS (SELECT 1 FROM zzlearnerqctoartisans z WHERE z.id = ms_learnerqctoartisans.id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerQctoArtisansRead");
        Trx readTrx = Trx.get(readTrxName, true);
        int processed = 0;
        int created = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(rs, learnerCrosswalk, qctoLearnershipCrosswalk, qctoProgrammeStatusCrosswalk,
                            providerCrosswalk, waCrosswalk, acCrosswalk, qualificationCrosswalk,
                            learnershipCrosswalk, grantTypeCrosswalk, msUserToAdUser, socioEconomicStatusMap,
                            sponsorshipMap, projectMap, artisanProjectMap, enrolmentStatusReasonMap,
                            terminationReasonMap, qualificationRequirementsMap, organisationToBPartnerCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_learnerqctoartisans rows (" + created
                            + " ZZLearnerQCTOArtisans created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_learnerqctoartisans row(s): " + created
                + " ZZLearnerQCTOArtisans created, " + errors.size() + " error(s).";
    }

    /** ms_learner_xref has bespoke column names (ms_learner_id -> zzlearner_id), so it's
     * read directly rather than through the generic buildIdCrosswalk() helper. */
    private Map<Integer, Integer> loadLearnerCrosswalk() {
        Map<Integer, Integer> result = new java.util.HashMap<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement("SELECT ms_learner_id, zzlearner_id FROM ms_learner_xref", get_TrxName());
            rs = pst.executeQuery();
            while (rs.next()) {
                result.put(rs.getInt("ms_learner_id"), rs.getInt("zzlearner_id"));
            }
        } catch (Exception e) {
            throw new org.adempiere.exceptions.AdempiereException("Failed loading ms_learner_xref", e);
        } finally {
            DB.close(rs, pst);
        }
        return result;
    }

    private void processOneRow(ResultSet rs, Map<Integer, Integer> learnerCrosswalk,
            Map<Integer, Integer> qctoLearnershipCrosswalk, Map<Integer, Integer> qctoProgrammeStatusCrosswalk,
            Map<Integer, Integer> providerCrosswalk, Map<Integer, Integer> waCrosswalk,
            Map<Integer, Integer> acCrosswalk, Map<Integer, Integer> qualificationCrosswalk,
            Map<Integer, Integer> learnershipCrosswalk, Map<Integer, Integer> grantTypeCrosswalk,
            Map<Integer, Integer> msUserToAdUser, Map<Integer, String> socioEconomicStatusMap,
            Map<Integer, String> sponsorshipMap, Map<Integer, String> projectMap,
            Map<Integer, String> artisanProjectMap, Map<Integer, String> enrolmentStatusReasonMap,
            Map<Integer, String> terminationReasonMap, Map<Integer, String> qualificationRequirementsMap,
            Map<Integer, Integer> organisationToBPartnerCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerId = (Integer) rs.getObject("learnerid");
        Integer qctoLearnershipId = (Integer) rs.getObject("qctolearnershipid");
        Integer qctoProgrammeStatusId = (Integer) rs.getObject("qctoprogrammestatusid");
        Integer socioEconomicStatusId = (Integer) rs.getObject("socioeconomicstatusid");
        Integer sponsorshipId = (Integer) rs.getObject("sponsorshipid");
        Integer projectId = (Integer) rs.getObject("projectid");
        Integer leadSdProviderId = (Integer) rs.getObject("leadsdproviderid");
        Integer secondarySdProviderId = (Integer) rs.getObject("secondarysdproviderid");
        Integer waId = (Integer) rs.getObject("waid");
        Integer secondaryWaId = (Integer) rs.getObject("secondarywaid");
        Integer acId = (Integer) rs.getObject("acid");
        Integer enrolledBy = (Integer) rs.getObject("enrolledby");
        Integer certificateCreatedBy = (Integer) rs.getObject("certificatecreatedby");
        Integer terminatedCapturedBy = (Integer) rs.getObject("terminatedcapturedby");
        Integer extensionCapturedBy = (Integer) rs.getObject("extensioncapturedby");
        Integer registeredBy = (Integer) rs.getObject("registeredby");
        Integer qualificationId = (Integer) rs.getObject("qualificationid");
        Integer previousLearnership = (Integer) rs.getObject("previouslearnership");
        Integer grantTypeId = (Integer) rs.getObject("granttypeid");
        Integer leadSdProviderLevyYesNoId = (Integer) rs.getObject("leadsdproviderlevyyesnoid");
        Integer waLevyYesNoId = (Integer) rs.getObject("walevyyesnoid");
        Integer secondarySdProviderLevyYesNoId = (Integer) rs.getObject("secondarysdproviderlevyyesnoid");
        Integer secondaryWaLevyYesNoId = (Integer) rs.getObject("secondarywalevyyesnoid");
        Integer isApproved = (Integer) rs.getObject("isapproved");
        Integer approvedBy = (Integer) rs.getObject("approvedby");
        Integer previousEmployed = (Integer) rs.getObject("previousemployed");
        Integer wpAgreement = (Integer) rs.getObject("wpagreement");
        Integer isTermsEmployment = (Integer) rs.getObject("istermsemployment");
        Integer empContract = (Integer) rs.getObject("empcontract");
        Integer empContractCopy = (Integer) rs.getObject("empcontractcopy");
        Integer nambConfirmation = (Integer) rs.getObject("nambconfirmation");
        Integer nambConfirmationUser = (Integer) rs.getObject("nambconfirmationuser");
        Integer employerId = (Integer) rs.getObject("employerid");
        Integer acLevyYesNoId = (Integer) rs.getObject("aclevyyesnoid");
        Integer artisanProjectId = (Integer) rs.getObject("artisanprojectid");
        Integer enrolmentStatusReasonId = (Integer) rs.getObject("enrolmentstatusreasonid");
        Integer terminationReasonId = (Integer) rs.getObject("terminationreasonid");
        Integer qualificationRequirementsId = (Integer) rs.getObject("qualificationrequirementsid");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        if (learnerId == null || learnerCrosswalk.get(learnerId) == null) {
            throw new org.adempiere.exceptions.AdempiereException(
                    "No matching ZZLearner for learnerid=" + learnerId + " (person/learner not migrated yet?)");
        }

        String trxName = Trx.createTrxName("MsLearnerQctoArtisansMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZLearnerQCTOArtisans artisan = new X_ZZLearnerQCTOArtisans(getCtx(), 0, trxName);
            artisan.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            artisan.setIsActive(isDeleted == 0);
            artisan.setZZLearner_ID(learnerCrosswalk.get(learnerId));
            setIfResolved(qctoLearnershipCrosswalk, qctoLearnershipId, artisan::setZZQctoLearnership_ID);
            artisan.setZZAgreementReferenceNumber(rs.getString("agreementreferencenumber"));
            setTimestamp(rs.getTimestamp("commencementdate"), artisan::setZZCommencementDate);
            setTimestamp(rs.getTimestamp("completiondate"), artisan::setZZCompletionDate);
            artisan.setZZContractNumber(rs.getString("contractnumber"));
            setIfResolved(qctoProgrammeStatusCrosswalk, qctoProgrammeStatusId, artisan::setZZQctoProgrammeStatus_ID);
            artisan.setZZSocioEconomicStatus(socioEconomicStatusId == null ? null : socioEconomicStatusMap.get(socioEconomicStatusId));
            artisan.setZZSponsorship(sponsorshipId == null ? null : sponsorshipMap.get(sponsorshipId));
            artisan.setZZProject(projectId == null ? null : projectMap.get(projectId));
            // ZZFinancialYear_ID (financialyearid): NOT SET - see class Javadoc.
            setIfResolved(providerCrosswalk, leadSdProviderId, artisan::setZZLeadSDProvider_ID);
            setIfResolved(providerCrosswalk, secondarySdProviderId, artisan::setZZSecondarySDProvider_ID);
            setIfResolved(waCrosswalk, waId, artisan::setZZWA_ID);
            setIfResolved(waCrosswalk, secondaryWaId, artisan::setZZSecondaryWA_ID);
            setIfResolved(acCrosswalk, acId, artisan::setZZAC_ID);
            setIfResolved(msUserToAdUser, enrolledBy, artisan::setZZEnrolledBy);
            setTimestamp(rs.getTimestamp("enrolmentdate"), artisan::setZZEnrolmentDate);
            setGeneric(artisan, "Is_Approved", MigrationSupport.flagToYN(isApproved));
            setPoIfResolved(artisan, "Approved_By", msUserToAdUser, approvedBy);
            setGeneric(artisan, "Date_Approved", rs.getTimestamp("dateapproved"));
            artisan.setZZCertificateNumber(rs.getString("certificatenumber"));
            setIfResolved(msUserToAdUser, certificateCreatedBy, artisan::setZZCertificateCreatedBy);
            setTimestamp(rs.getTimestamp("datecertificatecreated"), artisan::setZZDateCertificateCreated);
            setTimestamp(rs.getTimestamp("statuseffectivedate"), artisan::setZZStatusEffectiveDate);
            artisan.setZZStudentNumber(rs.getString("studentnumber"));
            setTimestamp(rs.getTimestamp("extensiondate"), artisan::setZZExtensionDate);
            artisan.setZZExtensionReason(rs.getString("extensionreason"));
            setTimestamp(rs.getTimestamp("terminationdate"), artisan::setZZTerminationDate);
            // FIXED 2026-07-16 - these were swapped (see class Javadoc): ZZTerminationReason is
            // a List reference matching lkpTerminationReason verbatim (terminationreasonid's
            // resolved description), ZZTerminationReasonText is the raw free text.
            artisan.setZZTerminationReason(terminationReasonId == null ? null : terminationReasonMap.get(terminationReasonId));
            artisan.setZZTerminationReasonText(rs.getString("terminationreason"));
            // extractrecordid: NOT SET - dead artifact (0 of 1230 rows populated), see class Javadoc.
            artisan.setZZArtisanProject(artisanProjectId == null ? null : artisanProjectMap.get(artisanProjectId));
            setIfResolved(msUserToAdUser, terminatedCapturedBy, artisan::setZZTerminatedCapturedBy);
            setTimestamp(rs.getTimestamp("dateterminationcaptured"), artisan::setZZDateTerminationCaptured);
            setIfResolved(msUserToAdUser, extensionCapturedBy, artisan::setZZExtensionCapturedBy);
            setTimestamp(rs.getTimestamp("dateextensioncaptured"), artisan::setZZDateExtensionCaptured);
            setTimestamp(rs.getTimestamp("registrationdate"), artisan::setZZRegistrationDate);
            setIfResolved(msUserToAdUser, registeredBy, artisan::setZZRegisteredBy);
            artisan.setZZEnrolmentStatusReason(enrolmentStatusReasonId == null ? null : enrolmentStatusReasonMap.get(enrolmentStatusReasonId));
            setTimestamp(rs.getTimestamp("mostrecentregistrationdate"), artisan::setZZMostRecentRegistrationDate);
            setTimestamp(rs.getTimestamp("actualterminateddate"), artisan::setZZActualTerminatedDate);
            setIfResolved(qualificationCrosswalk, qualificationId, artisan::setZZQualification_ID);
            setTimestamp(rs.getTimestamp("completionprocesseddate"), artisan::setZZCompletionProcessedDate);
            setIfResolved(learnershipCrosswalk, previousLearnership, artisan::setZZPreviousLearnership_ID);
            // previouslearnershipcode / previouslearnershiptitle: NOT SET - redundant text
            // mirrors of previouslearnership, no separate target column, see class Javadoc.
            setGeneric(artisan, "Previous_Employed", MigrationSupport.yesNoIdToFlag(previousEmployed));
            setGeneric(artisan, "Learner_Employed", rs.getString("learneremployed"));
            setGeneric(artisan, "WP_Agreement", MigrationSupport.yesNoIdToFlag(wpAgreement));
            artisan.setZZDurationLearnerEmployed(rs.getString("durationlearneremployed"));
            setGeneric(artisan, "Is_Terms_Employment", MigrationSupport.yesNoIdToFlag(isTermsEmployment));
            setGeneric(artisan, "Terms_Employment", rs.getString("termsemployment"));
            setGeneric(artisan, "Emp_Contract", MigrationSupport.yesNoIdToFlag(empContract));
            setGeneric(artisan, "Emp_Contract_Copy", MigrationSupport.yesNoIdToFlag(empContractCopy));
            setGeneric(artisan, "Responsible_Seta", rs.getString("responsibleseta"));
            setGeneric(artisan, "Ass_Partner", rs.getString("asspartner"));
            setGeneric(artisan, "Reg_Saqa", rs.getString("regsaqa"));
            setGeneric(artisan, "Cur_Reg_Number", rs.getString("curregnumber"));
            setGeneric(artisan, "Qcto", rs.getString("qcto"));
            setGeneric(artisan, "Occupation", rs.getString("occupation"));
            setIfResolved(grantTypeCrosswalk, grantTypeId, artisan::setZZGrantType_ID);
            artisan.setZZLeadSDProviderLevy(MigrationSupport.yesNoIdToFlag(leadSdProviderLevyYesNoId));
            // ZZLeadSDProviderContact_ID (leadsdprovidercontactid): NOT SET, see class Javadoc.
            artisan.setZZWALevy(MigrationSupport.yesNoIdToFlag(waLevyYesNoId));
            // ZZWAContact_ID (wacontactid): NOT SET, see class Javadoc.
            artisan.setZZTradeTestSerialNumber(rs.getString("tradetestserialnumber"));
            setGeneric(artisan, "Namb_Confirmation", MigrationSupport.flagToYN(nambConfirmation));
            setGeneric(artisan, "Namb_Confirmation_Date", rs.getTimestamp("nambconfirmationdate"));
            setPoIfResolved(artisan, "Namb_Confirmation_User", msUserToAdUser, nambConfirmationUser);
            artisan.setZZSecondarySDProviderLevy(MigrationSupport.yesNoIdToFlag(secondarySdProviderLevyYesNoId));
            // ZZSecondarySDProviderContact_ID (secondarysdprovidercontactid): NOT SET, see class Javadoc.
            artisan.setZZSecondaryWALevy(MigrationSupport.yesNoIdToFlag(secondaryWaLevyYesNoId));
            // ZZSecondaryWAContact_ID (secondarywacontactid): NOT SET, see class Javadoc.
            artisan.setZZQualificationRequirements(qualificationRequirementsId == null ? null : qualificationRequirementsMap.get(qualificationRequirementsId));
            artisan.setZZACLevy(MigrationSupport.yesNoIdToFlag(acLevyYesNoId));
            // ZZACContact_ID (accontactid): NOT SET, see class Javadoc.
            // ZZQctoArtisanType (qctoartisantypeid): NOT SET - DATA DEFECT, see class Javadoc.
            setPoIfResolved(artisan, "Employer_ID", organisationToBPartnerCrosswalk, employerId);
            // accountnumber: NOT SET - the MSSQL row's own UUID, not business data.

            artisan.saveEx();
            int zzId = artisan.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzlearnerqctoartisans", "zzlearnerqctoartisans_id", zzId,
                        createdTs, DEFAULT_CREATED_BY, updatedTs, DEFAULT_CREATED_BY, sourceId, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private static void setIfResolved(Map<Integer, Integer> crosswalk, Integer sourceId, java.util.function.IntConsumer setter) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            setter.accept(targetId);
        }
    }

    /** Sets a column via the generic PO API, only if the value is non-null (used for the new
     * Section A/B columns, which have no typed setter on the generated model class yet). */
    private static void setGeneric(PO po, String columnName, Object value) {
        if (value != null) {
            po.set_ValueOfColumn(columnName, value);
        }
    }

    /** Same as {@link #setIfResolved(Map, Integer, java.util.function.IntConsumer)} but for a
     * new column with no typed setter, set via the generic PO API. */
    private static void setPoIfResolved(PO po, String columnName, Map<Integer, Integer> crosswalk, Integer sourceId) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            po.set_ValueOfColumn(columnName, targetId);
        }
    }

    private static void setTimestamp(Timestamp value, java.util.function.Consumer<Timestamp> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_learnerqctoartisans.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learnerqctoartisans-errors-" + ts + ".txt");
        try (PrintWriter out = new PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(logFile)))) {
            for (String err : errors) {
                out.println(err);
            }
            addLog("Error log written to: " + logFile.getAbsolutePath()
                    + (errors.size() >= MAX_LOGGED_ERRORS ? " (truncated at " + MAX_LOGGED_ERRORS + ")" : ""));
        } catch (Exception e) {
            addLog("WARN: could not write error log: " + e.getMessage());
        }
    }
}
