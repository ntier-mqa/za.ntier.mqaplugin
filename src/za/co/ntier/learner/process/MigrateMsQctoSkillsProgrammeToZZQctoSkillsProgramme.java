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

import za.co.ntier.api.model.X_ZZQctoSkillsProgramme;

/**
 * Migrates the staged ms_qctoskillsprogramme table (the QCTO skills programme CATALOG -
 * distinct from ms_learnerqctoskillsprogramme, the per-learner enrolment table) into
 * ZZQctoSkillsProgramme. See "Column Mapping - QCTO Learner Programmes" doc, tab
 * "QCTOSkillsProgramme".
 *
 * <p><b>UPDATE-in-place, same reasoning/caveat as {@link MigrateMsQualificationToZZQualification}</b>:
 * zzqctoskillsprogramme already has 12 placeholder rows (matches ms_qctoskillsprogramme's
 * row count exactly), matched ordinally by zzmigrationcode - an ASSUMPTION, not confirmed.
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzqctoskillsprogramme ADD COLUMN id bigint;}
 * (already run 2026-07-10).
 *
 * <p>Must run AFTER MigrateMsQualificationToZZQualification (resolves qualificationid via
 * its id crosswalk).
 *
 * <p>NOT handled:
 * <ul>
 *   <li>ofooccupationid -&gt; ZZLkpOfoOccupation_ID: same open item as Qualification/
 *       QCTOLearnership - no crosswalk method designed yet. Left unset.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsQctoSkillsProgrammeToZZQctoSkillsProgramme")
public class MigrateMsQctoSkillsProgrammeToZZQctoSkillsProgramme extends SvrProcess {

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzqctoskillsprogramme WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: resetting " + count + " previously-migrated ZZQctoSkillsProgramme "
                    + "placeholder row(s) back to unmigrated (id = NULL).");
            DB.executeUpdateEx("UPDATE zzqctoskillsprogramme SET id = NULL WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, String> nqfLevelMap = MigrationSupport.buildDescriptionMap("ms_lkpnqflevel", get_TrxName());
        Map<Integer, String> qaBodyMap = MigrationSupport.buildDescriptionMap("ms_lkpqualityassurancebody", get_TrxName());
        Map<Integer, String> skillsProgrammeTypeMap = MigrationSupport.buildDescriptionMap("ms_lkpskillsprogrammetype", get_TrxName());
        Map<Integer, String> aetLevelMap = MigrationSupport.buildDescriptionMap("ms_lkpaetlevel", get_TrxName());
        Map<Integer, String> skillsProgrammeGrantTypeMap = MigrationSupport.buildDescriptionMap("ms_lkpskillsprogrammegranttype", get_TrxName());
        Map<Integer, Integer> qualificationCrosswalk = MigrationSupport.buildIdCrosswalk(
                "zzqualification", "zzqualification_id", get_TrxName());

        String sql =
                "SELECT s.id, s.skillsprogrammecode, s.skillsprogrammetitle, s.nqflevelid, s.credits, "
                + "       s.registrationstartdate, s.registrationenddate, s.qualityassurancebodyid, "
                + "       s.skillsprogrammetypeid, s.qualificationid, s.aetlevelid, s.minimumelectivecredits, "
                + "       s.skillsprogrammegranttypeid, s.isohs, s.lastenrolmentdate, s.created, s.updated, "
                + "       s.isdeleted, t.target_id "
                + "FROM (SELECT *, row_number() OVER (ORDER BY id) AS rn FROM ms_qctoskillsprogramme) s "
                + "JOIN (SELECT zzqctoskillsprogramme_id AS target_id, row_number() OVER (ORDER BY zzmigrationcode) AS rn "
                + "      FROM zzqctoskillsprogramme WHERE id IS NULL) t ON t.rn = s.rn "
                + "ORDER BY s.id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsQctoSkillsProgrammeRead");
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
                    processOneRow(rs, nqfLevelMap, qaBodyMap, skillsProgrammeTypeMap, aetLevelMap,
                            skillsProgrammeGrantTypeMap, qualificationCrosswalk);
                    updated++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_qctoskillsprogramme rows (" + updated
                            + " ZZQctoSkillsProgramme updated, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " ms_qctoskillsprogramme row(s): " + updated
                + " ZZQctoSkillsProgramme updated, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs, Map<Integer, String> nqfLevelMap, Map<Integer, String> qaBodyMap,
            Map<Integer, String> skillsProgrammeTypeMap, Map<Integer, String> aetLevelMap,
            Map<Integer, String> skillsProgrammeGrantTypeMap, Map<Integer, Integer> qualificationCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        int targetId = rs.getInt("target_id");
        String skillsProgrammeCode = rs.getString("skillsprogrammecode");
        String skillsProgrammeTitle = rs.getString("skillsprogrammetitle");
        Integer nqfLevelId = (Integer) rs.getObject("nqflevelid");
        int credits = rs.getInt("credits");
        Timestamp regStart = rs.getTimestamp("registrationstartdate");
        Timestamp regEnd = rs.getTimestamp("registrationenddate");
        Integer qaBodyId = (Integer) rs.getObject("qualityassurancebodyid");
        Integer skillsProgrammeTypeId = (Integer) rs.getObject("skillsprogrammetypeid");
        Integer qualificationId = (Integer) rs.getObject("qualificationid");
        Integer aetLevelId = (Integer) rs.getObject("aetlevelid");
        int minElectiveCredits = rs.getInt("minimumelectivecredits");
        Integer skillsProgrammeGrantTypeId = (Integer) rs.getObject("skillsprogrammegranttypeid");
        Integer isOhs = (Integer) rs.getObject("isohs");
        Timestamp lastEnrolment = rs.getTimestamp("lastenrolmentdate");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");

        String trxName = Trx.createTrxName("MsQctoSkillsProgrammeMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZQctoSkillsProgramme skillsProgramme = new X_ZZQctoSkillsProgramme(getCtx(), targetId, trxName);
            skillsProgramme.setZZSkillsProgrammeCode(skillsProgrammeCode);
            skillsProgramme.setZZSkillsProgrammeTitle(skillsProgrammeTitle);
            skillsProgramme.setZZNqfLevel(nqfLevelId == null ? null : nqfLevelMap.get(nqfLevelId));
            skillsProgramme.setZZCredits(credits);
            if (regStart != null) {
                skillsProgramme.setRegistrationstartdate(regStart);
            }
            if (regEnd != null) {
                skillsProgramme.setRegistrationenddate(regEnd);
            }
            skillsProgramme.setZZQualityAssuranceBody(qaBodyId == null ? null : qaBodyMap.get(qaBodyId));
            skillsProgramme.setZZSkillsProgrammeType(
                    skillsProgrammeTypeId == null ? null : skillsProgrammeTypeMap.get(skillsProgrammeTypeId));
            Integer zzQualificationId = qualificationId == null ? null : qualificationCrosswalk.get(qualificationId);
            if (zzQualificationId != null) {
                skillsProgramme.setZZQualification_ID(zzQualificationId);
            }
            skillsProgramme.setZZAetLevel(aetLevelId == null ? null : aetLevelMap.get(aetLevelId));
            skillsProgramme.setZZMinimumElectiveCredits(minElectiveCredits);
            skillsProgramme.setZZSkillsProgrammeGrantType(
                    skillsProgrammeGrantTypeId == null ? null : skillsProgrammeGrantTypeMap.get(skillsProgrammeGrantTypeId));
            skillsProgramme.setZZIsOhs(MigrationSupport.flagToYN(isOhs));
            if (lastEnrolment != null) {
                skillsProgramme.setZZLastEnrolmentDate(lastEnrolment);
            }
            // ZZLkpOfoOccupation_ID (ofooccupationid): NOT SET - see class Javadoc.

            skillsProgramme.saveEx();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzqctoskillsprogramme", "zzqctoskillsprogramme_id", targetId,
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
            errors.add("ms_qctoskillsprogramme.id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-qctoskillsprogramme-errors-" + ts + ".txt");
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
