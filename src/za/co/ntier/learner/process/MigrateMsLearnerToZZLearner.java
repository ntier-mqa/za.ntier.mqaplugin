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
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.api.model.X_ZZLearner;

/**
 * Migrates the staged ms_learner table (loaded earlier via pgloader batch files, see
 * Step 2/3 of the Learners Data Migration runbook) into real ZZLearner records, linking
 * each one to the ZZPerson created by {@link MigrateMsPersonToZZPerson} (via ms_person_xref)
 * and building a C_Location for the matriculated address.
 *
 * <p>Must be run AFTER MigrateMsPersonToZZPerson - it relies entirely on ms_person_xref to
 * resolve ZZPerson_ID and will refuse to run if that table is empty.
 *
 * <p>REQUIRES a recon column: {@code ALTER TABLE adempiere.zzlearner ADD COLUMN id bigint;}
 * must be run before this process - it stamps ms_learner.id into that column (same name as
 * the source table's PK) alongside created/updated, purely so a migrated row can be traced
 * back to its source row without going through ms_learner_xref.
 *
 * <p>NOT handled by this process (still open items per the mapping doc):
 * <ul>
 *   <li>matriculatedmunicipalityid - C_Location has no municipality column, and it's unclear
 *       whether municipality should be captured for ZZLearner at all; left unused.</li>
 *   <li>ZZ_ApprovedBy_ID / ZZ_ApprovedDate / ZZ_Date_Not_Approved / ZZ_Date_Not_Recommended /
 *       ZZ_Date_Recommended / ZZ_Recommender_ID - no equivalent columns in ms_learner.</li>
 *   <li>ZZ_DocAction / ZZ_DocStatus - left at their column defaults ('SU' / 'DR').</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerToZZLearner")
public class MigrateMsLearnerToZZLearner extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst; // Y/N - deletes previously-migrated data before reloading

    private static final int DEFAULT_CREATED_BY = 1000003; // matches the Learner/Learnership loader convention
    private static final int MAX_LOGGED_ERRORS = 1000;

    private int countryId;
    private Map<Integer, Integer> provinceCrosswalk;
    private Map<Integer, Integer> cityCrosswalk;

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

        int xrefCount = DB.getSQLValueEx(get_TrxName(),
                "SELECT count(*) FROM ms_person_xref");
        if (xrefCount <= 0) {
            throw new AdempiereException(
                    "ms_person_xref is empty - run MigrateMsPersonToZZPerson first, "
                    + "ZZLearner rows need it to resolve ZZPerson_ID.");
        }

        DB.executeUpdateEx(
                "CREATE TABLE IF NOT EXISTS ms_learner_xref ("
                + "ms_learner_id integer PRIMARY KEY, zzlearner_id numeric(10) NOT NULL)",
                null, get_TrxName());

        if ("Y".equals(p_ClearDataFirst)) {
            clearPreviouslyMigratedData();
        }

        countryId = MigrationSupport.getSouthAfricaCountryId(get_TrxName());
        provinceCrosswalk = MigrationSupport.buildProvinceCrosswalk(get_TrxName());
        cityCrosswalk = MigrationSupport.buildCityCrosswalk(get_TrxName());

        int missingPerson = DB.getSQLValueEx(get_TrxName(),
                "SELECT count(*) FROM ms_learner l "
                + "WHERE NOT EXISTS (SELECT 1 FROM ms_person_xref x WHERE x.ms_person_id = l.personid)");
        if (missingPerson > 0) {
            addLog("WARNING: " + missingPerson + " ms_learner row(s) have no matching "
                    + "ms_person_xref entry and will be skipped (their person was never migrated).");
        }

        String sql =
                "SELECT l.id, l.personid, x.zzperson_id, l.matriculatedtowncity, "
                + "       l.matriculatedprovinceid, l.matriculatedpostalcode, "
                + "       l.created, l.updated, l.isdeleted "
                + "FROM ms_learner l "
                + "JOIN ms_person_xref x ON x.ms_person_id = l.personid "
                + "WHERE NOT EXISTS (SELECT 1 FROM ms_learner_xref lx WHERE lx.ms_learner_id = l.id) "
                + "ORDER BY l.id"
                + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerRead");
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
                    processOneRow(rs);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }

                if (processed % 1000 == 0) {
                    addLog("Processed " + processed + " ms_learner rows (" + created
                            + " ZZLearner created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();

        return "Processed " + processed + " ms_learner row(s): " + created + " ZZLearner created, "
                + missingPerson + " skipped (no matching person), " + errors.size() + " error(s).";
    }

    /**
     * ClearDataFirst=Y support: deletes every ZZLearner row this migration previously created
     * (identified via the "id" recon column - see class Javadoc) and their matriculated
     * C_Location rows, then truncates ms_learner_xref so every ms_learner row is treated as
     * unmigrated again. Does NOT touch ZZPerson/ms_person_xref - that's
     * MigrateMsPersonToZZPerson's own ClearDataFirst option. Runs as one committed step before
     * the main migration loop starts.
     */
    private void clearPreviouslyMigratedData() throws Exception {
        int learnerCount = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzlearner WHERE id IS NOT NULL");
        addLog("ClearDataFirst=Y: deleting " + learnerCount + " previously-migrated ZZLearner row(s) "
                + "(plus their matriculated C_Location rows) before reloading...");

        DB.executeUpdateEx(
                "CREATE TEMP TABLE tmp_learner_clear_locs AS "
                + "SELECT zzmatriculatedlocation_id AS loc_id FROM zzlearner "
                + "WHERE id IS NOT NULL AND zzmatriculatedlocation_id IS NOT NULL",
                null, get_TrxName());

        DB.executeUpdateEx("DELETE FROM zzlearner WHERE id IS NOT NULL", null, get_TrxName());

        DB.executeUpdateEx(
                "DELETE FROM c_location WHERE c_location_id IN (SELECT loc_id FROM tmp_learner_clear_locs)",
                null, get_TrxName());

        DB.executeUpdateEx("DROP TABLE tmp_learner_clear_locs", null, get_TrxName());

        DB.executeUpdateEx("TRUNCATE TABLE ms_learner_xref", null, get_TrxName());

        DB.commit(true, get_TrxName());
        addLog("ClearDataFirst: done - ms_learner_xref reset, reload starting fresh.");
    }

    private void processOneRow(ResultSet rs) throws Exception {
        int msLearnerId = rs.getInt("id");
        int zzPersonId = rs.getInt("zzperson_id");
        String townCity = rs.getString("matriculatedtowncity");
        int provinceId = rs.getInt("matriculatedprovinceid");
        String postalCode = rs.getString("matriculatedpostalcode");
        Timestamp created = rs.getTimestamp("created");
        Timestamp updated = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsLearnerMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            int matriculatedLocationId = MigrationSupport.createLocation(getCtx(), trxName, countryId,
                    provinceCrosswalk.get(provinceId), null, townCity, null, null, null, postalCode);

            X_ZZLearner learner = new X_ZZLearner(getCtx(), 0, trxName);
            learner.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            learner.setIsActive(isDeleted == 0);
            learner.setZZPerson_ID(zzPersonId);
            if (matriculatedLocationId > 0) {
                learner.setZZMatriculatedLocation_ID(matriculatedLocationId);
            }
            // ZZ_ApprovedBy_ID/ZZ_ApprovedDate/ZZ_Date_Not_Approved/ZZ_Date_Not_Recommended/
            // ZZ_Date_Recommended/ZZ_Recommender_ID - no source data, left unset.
            // ZZ_DocAction/ZZ_DocStatus - left at column defaults ('SU'/'DR').

            learner.saveEx();
            int zzLearnerId = learner.get_ID();

            if (created != null) {
                MigrationSupport.stampCreatedUpdated("zzlearner", "zzlearner_id", zzLearnerId,
                        created, DEFAULT_CREATED_BY, updated, DEFAULT_CREATED_BY, msLearnerId, trxName);
            }

            DB.executeUpdateEx("INSERT INTO ms_learner_xref (ms_learner_id, zzlearner_id) VALUES (?, ?)",
                    new Object[] { msLearnerId, zzLearnerId }, trxName);

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private void logError(int msLearnerId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add("ms_learner.id=" + msLearnerId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learner-errors-" + ts + ".txt");
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
