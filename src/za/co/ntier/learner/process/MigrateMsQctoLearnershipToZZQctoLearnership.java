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
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZQctoLearnership;

/**
 * Migrates the staged ms_qctolearnership table (the QCTO learnership CATALOG - distinct
 * from ms_learnerqctolearnership, the per-learner enrolment table) into ZZQctoLearnership.
 * See "Column Mapping - QCTO Learner Programmes" doc, tab "QCTOLearnership".
 *
 * <p><b>UPDATE-in-place, same reasoning/caveat as {@link MigrateMsQualificationToZZQualification}</b>:
 * zzqctolearnership already has 28 placeholder rows (matches ms_qctolearnership's row count
 * exactly), matched ordinally by zzmigrationcode - an ASSUMPTION, not confirmed.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzqctolearnership ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>Must run AFTER MigrateMsQualificationToZZQualification (resolves qualificationid via
 * its id crosswalk).
 *
 * <p>NOT handled:
 * <ul>
 *   <li>ofooccupationid -&gt; ZZLkpOfoOccupation_ID: same open item as Qualification - no
 *       crosswalk method designed yet. Left unset. (Also note: the generated
 *       X_ZZQctoLearnership model class doesn't even expose a typed setter for this column,
 *       only for ZZLkpOfoOccupationTree_ID - possibly stale codegen relative to the DB
 *       schema; would need the generic PO set_ValueOfColumn() path if resolved later.)</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsQctoLearnershipToZZQctoLearnership")
public class MigrateMsQctoLearnershipToZZQctoLearnership extends SvrProcess {

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

        int qualificationMigrated = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzqualification WHERE id IS NOT NULL");
        if (qualificationMigrated <= 0) {
            addLog("WARNING: zzqualification has no migrated rows yet - run "
                    + "MigrateMsQualificationToZZQualification first, or every qualificationid "
                    + "on this table will resolve to nothing.");
        }

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzqctolearnership WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: resetting " + count + " previously-migrated ZZQctoLearnership "
                    + "placeholder row(s) back to unmigrated (id = NULL).");
            DB.executeUpdateEx("UPDATE zzqctolearnership SET id = NULL WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, String> nqfLevelMap = MigrationSupport.buildDescriptionMap("ms_lkpnqflevel", get_TrxName());
        Map<Integer, String> qctoLearnershipTypeMap = MigrationSupport.buildDescriptionMap("ms_lkpqctolearnershiptype", get_TrxName());
        Map<Integer, Integer> qualificationCrosswalk = MigrationSupport.buildIdCrosswalk(
                "zzqualification", "zzqualification_id", get_TrxName());

        String sql =
                "SELECT s.id, s.learnershipcode, s.learnershiptitle, s.qctolearnershiptypeid, s.qualificationid, "
                + "       s.nqflevelid, s.credits, s.registrationstartdate, s.registrationenddate, "
                + "       s.minimumelectivecredits, s.artisanlearnershipyesnoid, s.lastenrolmentdate, "
                + "       s.lastachievementdate, s.created, s.updated, s.isdeleted, t.target_id "
                + "FROM (SELECT *, row_number() OVER (ORDER BY id) AS rn FROM ms_qctolearnership) s "
                + "JOIN (SELECT zzqctolearnership_id AS target_id, row_number() OVER (ORDER BY zzmigrationcode) AS rn "
                + "      FROM zzqctolearnership WHERE id IS NULL) t ON t.rn = s.rn "
                + "ORDER BY s.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsQctoLearnershipRead");
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
                    processOneRow(rs, nqfLevelMap, qctoLearnershipTypeMap, qualificationCrosswalk);
                    updated++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_qctolearnership rows (" + updated
                            + " ZZQctoLearnership updated, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_qctolearnership row(s): " + updated
                + " ZZQctoLearnership updated, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs, Map<Integer, String> nqfLevelMap,
            Map<Integer, String> qctoLearnershipTypeMap, Map<Integer, Integer> qualificationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        int targetId = rs.getInt("target_id");
        String learnershipCode = rs.getString("learnershipcode");
        String learnershipTitle = rs.getString("learnershiptitle");
        Integer qctoLearnershipTypeId = (Integer) rs.getObject("qctolearnershiptypeid");
        Integer qualificationId = (Integer) rs.getObject("qualificationid");
        Integer nqfLevelId = (Integer) rs.getObject("nqflevelid");
        int credits = rs.getInt("credits");
        Timestamp regStart = rs.getTimestamp("registrationstartdate");
        Timestamp regEnd = rs.getTimestamp("registrationenddate");
        int minElectiveCredits = rs.getInt("minimumelectivecredits");
        Integer artisanLearnershipYesNoId = (Integer) rs.getObject("artisanlearnershipyesnoid");
        Timestamp lastEnrolment = rs.getTimestamp("lastenrolmentdate");
        Timestamp lastAchievement = rs.getTimestamp("lastachievementdate");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");

        String trxName = Trx.createTrxName("MsQctoLearnershipMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZQctoLearnership qctoLearnership = new X_ZZQctoLearnership(getCtx(), targetId, trxName);
            qctoLearnership.setZZLearnershipCode(learnershipCode);
            qctoLearnership.setZZLearnershipTitle(learnershipTitle);
            qctoLearnership.setZZQctoLearnershipType(
                    qctoLearnershipTypeId == null ? null : qctoLearnershipTypeMap.get(qctoLearnershipTypeId));
            Integer zzQualificationId = qualificationId == null ? null : qualificationCrosswalk.get(qualificationId);
            if (zzQualificationId != null) {
                qctoLearnership.setZZQualification_ID(zzQualificationId);
            }
            qctoLearnership.setZZNqfLevel(nqfLevelId == null ? null : nqfLevelMap.get(nqfLevelId));
            qctoLearnership.setZZCredits(credits);
            if (regStart != null) {
                qctoLearnership.setRegistrationstartdate(regStart);
            }
            if (regEnd != null) {
                qctoLearnership.setRegistrationenddate(regEnd);
            }
            qctoLearnership.setZZMinimumElectiveCredits(minElectiveCredits);
            qctoLearnership.setZZArtisanLearnership(MigrationSupport.yesNoIdToFlag(artisanLearnershipYesNoId));
            if (lastEnrolment != null) {
                qctoLearnership.setZZLastEnrolmentDate(lastEnrolment);
            }
            if (lastAchievement != null) {
                qctoLearnership.setZZLastAchievementDate(lastAchievement);
            }
            // ZZLkpOfoOccupation_ID (ofooccupationid): NOT SET - see class Javadoc.

            qctoLearnership.saveEx();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzqctolearnership", "zzqctolearnership_id", targetId,
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

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_qctolearnership.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-qctolearnership-errors-" + ts + ".txt");
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
