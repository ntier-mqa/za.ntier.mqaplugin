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
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;

/**
 * Migrates the staged ms_qualification table into ZZQualification. See "Column Mapping -
 * QCTO Learner Programmes" doc, tab "Qualification".
 *
 * <p><b>This is an UPDATE-in-place migration, not an insert.</b> zzqualification already
 * has 222 placeholder rows - the exact same count as ms_qualification - seeded by a
 * different, earlier import mechanism (evidenced by the zzmigrationcode/zzmigratevalues
 * columns, a "ref:Table:Column:Value" format unrelated to the ms_/pgloader pipeline). Those
 * placeholder rows' `value`/`name` are blank or just the row's own numeric id - not real
 * data. There is no direct id crosswalk between ms_qualification.id and the placeholder
 * zzqualification_id, so this process matches them ordinally: the Nth ms_qualification row
 * (ordered by id) is assumed to correspond to the Nth zzqualification placeholder row
 * (ordered by zzmigrationcode). This is corroborated by inspecting zzqctolearnership's own
 * zzmigratevalues text (e.g. "...ZZQualification:ZZQualification_ID:1..." on the row with
 * zzmigrationcode=1) which itself refers to qualifications by ordinal position, not by a
 * real id - but it is still an ASSUMPTION, not a confirmed mapping. If this turns out wrong,
 * the fix is isolated to the JOIN in the SQL below.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzqualification ADD COLUMN id bigint;}
 * (already run 2026-07-10) - once stamped, "WHERE id IS NOT NULL" is what "already migrated"
 * means for this table (ClearDataFirst resets it back to NULL rather than deleting the
 * placeholder row, since deleting would break the ordinal matching for everything else).
 *
 * <p>Must run BEFORE MigrateMsQctoLearnershipToZZQctoLearnership, MigrateMsQctoSkillsProgrammeToZZQctoSkillsProgramme,
 * and MigrateMsLearnershipToZZLearnership - they all resolve their own qualificationid FK
 * via this table's new id crosswalk.
 *
 * <p>NOT handled:
 * <ul>
 *   <li>ofooccupationid -&gt; ZZLkpOfoOccupation_ID: zzlkpofooccupation already has 6047 rows
 *       in iDempiere but no crosswalk method from MSSQL's OFOOccupation id to it has been
 *       worked out yet (likely needs matching by occupation code, not id). Left unset.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsQualificationToZZQualification")
public class MigrateMsQualificationToZZQualification extends SvrProcess {

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

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzqualification WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: resetting " + count + " previously-migrated ZZQualification "
                    + "placeholder row(s) back to unmigrated (id = NULL) - NOT deleting rows, "
                    + "that would break the ordinal match for every other catalog table.");
            DB.executeUpdateEx("UPDATE zzqualification SET id = NULL WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, String> nqfLevelMap = MigrationSupport.buildDescriptionMap("ms_lkpnqflevel", get_TrxName());
        Map<Integer, String> qaBodyMap = MigrationSupport.buildDescriptionMap("ms_lkpqualityassurancebody", get_TrxName());
        Map<Integer, String> qualTypeMap = MigrationSupport.buildDescriptionMap("ms_lkpqualificationtype", get_TrxName());

        // Ordinal match: Nth ms_qualification row (by id) <-> Nth placeholder zzqualification
        // row (by zzmigrationcode) - see class Javadoc for the reasoning/caveat.
        String sql =
                "SELECT s.id, s.saqaqualificationid, s.saqaqualificationtitle, s.nqflevelid, s.credits, "
                + "       s.registrationstartdate, s.registrationenddate, s.lastenrolmentdate, "
                + "       s.lastachievementdate, s.qualityassurancebodyid, s.qualificationtypeid, "
                + "       s.replacementqualificationid, s.newregistrationstartdate, s.newregistrationenddate, "
                + "       s.newlastenrolmentdate, s.newlastachievementdate, s.isreplacement, s.isreregistered, "
                + "       s.minimumelectivecredits, s.created, s.updated, s.isdeleted, t.target_id "
                + "FROM (SELECT *, row_number() OVER (ORDER BY id) AS rn FROM ms_qualification) s "
                + "JOIN (SELECT zzqualification_id AS target_id, row_number() OVER (ORDER BY zzmigrationcode) AS rn "
                + "      FROM zzqualification WHERE id IS NULL) t ON t.rn = s.rn "
                + "ORDER BY s.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsQualificationRead");
        Trx readTrx = Trx.get(readTrxName, true);
        int processed = 0;
        int updated = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, readTrxName);
            pstmt.setFetchSize(1000);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                processed++;
                try {
                    processOneRow(rs, nqfLevelMap, qaBodyMap, qualTypeMap);
                    updated++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_qualification rows (" + updated
                            + " ZZQualification updated, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        int selfRefUpdated = resolveReplacementQualificationSelfReference();

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_qualification row(s): " + updated
                + " ZZQualification updated, " + selfRefUpdated + " replacement-qualification link(s) resolved, "
                + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs, Map<Integer, String> nqfLevelMap,
            Map<Integer, String> qaBodyMap, Map<Integer, String> qualTypeMap) throws Exception {
        int sourceId = rs.getInt("id");
        int targetId = rs.getInt("target_id");
        String saqaCode = rs.getString("saqaqualificationid");
        String saqaTitle = rs.getString("saqaqualificationtitle");
        Integer nqfLevelId = (Integer) rs.getObject("nqflevelid");
        int credits = rs.getInt("credits");
        Timestamp regStart = rs.getTimestamp("registrationstartdate");
        Timestamp regEnd = rs.getTimestamp("registrationenddate");
        Timestamp lastEnrolment = rs.getTimestamp("lastenrolmentdate");
        Timestamp lastAchievement = rs.getTimestamp("lastachievementdate");
        Integer qaBodyId = (Integer) rs.getObject("qualityassurancebodyid");
        Integer qualTypeId = (Integer) rs.getObject("qualificationtypeid");
        Timestamp newRegStart = rs.getTimestamp("newregistrationstartdate");
        Timestamp newRegEnd = rs.getTimestamp("newregistrationenddate");
        Timestamp newLastEnrolment = rs.getTimestamp("newlastenrolmentdate");
        Timestamp newLastAchievement = rs.getTimestamp("newlastachievementdate");
        Integer isReplacement = (Integer) rs.getObject("isreplacement");
        Integer isReregistered = (Integer) rs.getObject("isreregistered");
        int minElectiveCredits = rs.getInt("minimumelectivecredits");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");

        String trxName = Trx.createTrxName("MsQualificationMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            // Loaded by the target placeholder's own PK (UPDATE-in-place, not a new PO) -
            // za.co.ntier.api.model.X_ZZQualification.
            za.co.ntier.api.model.X_ZZQualification qualification =
                    new za.co.ntier.api.model.X_ZZQualification(getCtx(), targetId, trxName);
            qualification.setValue(saqaCode);
            qualification.setName(saqaTitle);
            qualification.setDescription(saqaTitle);
            qualification.setZZSaqaQualificationCode(saqaCode);
            qualification.setZZSaqaQualificationTitle(saqaTitle);
            qualification.setZZNqfLevel(nqfLevelId == null ? null : nqfLevelMap.get(nqfLevelId));
            qualification.setZZCredits(credits);
            if (regStart != null) {
                qualification.setRegistrationstartdate(regStart);
            }
            if (regEnd != null) {
                qualification.setRegistrationenddate(regEnd);
            }
            if (lastEnrolment != null) {
                qualification.setZZLastEnrolmentDate(lastEnrolment);
            }
            if (lastAchievement != null) {
                qualification.setZZLastAchievementDate(lastAchievement);
            }
            qualification.setZZQualityAssuranceBody(qaBodyId == null ? null : qaBodyMap.get(qaBodyId));
            qualification.setZZQualificationType(qualTypeId == null ? null : qualTypeMap.get(qualTypeId));
            if (newRegStart != null) {
                qualification.setZZNewRegistrationStartDate(newRegStart);
            }
            if (newRegEnd != null) {
                qualification.setZZNewRegistrationEndDate(newRegEnd);
            }
            if (newLastEnrolment != null) {
                qualification.setZZNewLastEnrolmentDate(newLastEnrolment);
            }
            if (newLastAchievement != null) {
                qualification.setZZNewLastAchievementDate(newLastAchievement);
            }
            qualification.setZZIsReplacement(MigrationSupport.flagToYN(isReplacement));
            qualification.setZZIsReregistered(MigrationSupport.flagToYN(isReregistered));
            qualification.setZZMinimumElectiveCredits(minElectiveCredits);
            // ZZLkpOfoOccupation_ID (ofooccupationid) and ZZReplacementQualification_ID
            // (replacementqualificationid, a same-table self-reference): NOT SET here -
            // see class Javadoc. The replacement-qualification link is resolved in a second
            // pass after every row has its id crosswalk populated - see
            // resolveReplacementQualificationSelfReference() below.

            qualification.saveEx();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzqualification", "zzqualification_id", targetId,
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

    /**
     * Second pass: now that every migrated row carries its source id (via stampCreatedUpdated
     * above), resolve ms_qualification.replacementqualificationid (another ms_qualification.id)
     * to the corresponding zzqualification_id using the id crosswalk built from this same
     * table's now-populated recon column.
     */
    private int resolveReplacementQualificationSelfReference() {
        Map<Integer, Integer> idCrosswalk = MigrationSupport.buildIdCrosswalk(
                "zzqualification", "zzqualification_id", get_TrxName());
        int updated = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(
                    "SELECT id, replacementqualificationid FROM ms_qualification "
                    + "WHERE replacementqualificationid IS NOT NULL", get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Integer sourceId = rs.getInt("id");
                Integer replacementSourceId = (Integer) rs.getObject("replacementqualificationid");
                Integer thisTargetId = idCrosswalk.get(sourceId);
                Integer replacementTargetId = replacementSourceId == null ? null : idCrosswalk.get(replacementSourceId);
                if (thisTargetId != null && replacementTargetId != null) {
                    DB.executeUpdateEx(
                            "UPDATE zzqualification SET zzreplacementqualification_id = ? WHERE zzqualification_id = ?",
                            new Object[] { replacementTargetId, thisTargetId }, get_TrxName());
                    updated++;
                }
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed resolving ZZReplacementQualification_ID links", e);
        } finally {
            DB.close(rs, pstmt);
        }
        return updated;
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_qualification.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-qualification-errors-" + ts + ".txt");
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
