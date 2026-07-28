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
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/**
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #5): migrates the
 * staged ms_learnerlearnershiphistory table into the brand new ZZLearnerLearnershipHistory
 * table. Smallest of the 5 Phase 2 children (397 rows) - built/tested first as the simplest case.
 *
 * <p>ZZLearnerLearnershipHistory has NO generated model class yet (brand new AD_Table, no Tycho
 * build/model regeneration has happened) - uses the generic PO API (table.getPO(0, trxName)) for
 * every column, same approach {@link AddColumnsSupport#populateReferenceTable} already uses for
 * the same reason.
 *
 * <p>Crosswalks: ZZLearnerLearnership_ID via MigrationSupport.buildIdCrosswalk against the
 * parent's own "id" recon column (requires MigrateMsLearnerLearnershipToZZLearnerLearnership to
 * have already run). Lead_Provider_Old/New_ID via buildIdCrosswalk against zzprovider's "id"
 * column. Lead_Employer_Old/New_ID via buildOrganisationToBPartnerCrosswalk (same
 * Organisation-to-BPartner crosswalk used throughout this project for "employerid"/
 * "organisationid" columns).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerLearnershipHistoryToZZLearnerLearnershipHistory")
public class MigrateMsLearnerLearnershipHistoryToZZLearnerLearnershipHistory extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TABLE_NAME = "ZZLearnerLearnershipHistory";
    private static final String SOURCE_TABLE = "ms_learnerlearnershiphistory";
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
        MTable table = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (table == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }
        String physicalTable = table.getTableName().toLowerCase();

        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        if ("Y".equals(p_ClearDataFirst)) {
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM " + physicalTable + " WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated " + TABLE_NAME + " row(s)...");
            DB.executeUpdateEx("DELETE FROM " + physicalTable + " WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        Map<Integer, Integer> learnerLearnershipCrosswalk = MigrationSupport.buildIdCrosswalk(
                "zzlearnerlearnership", "zzlearnerlearnership_id", get_TrxName());
        Map<Integer, Integer> providerCrosswalk = MigrationSupport.buildIdCrosswalk(
                "zzprovider", "zzprovider_id", get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(
                get_TrxName());

        String sql = "SELECT id, learnerlearnershipid, leadprovideridold, leadprovideridnew, "
                + "       leademployeridold, leademployeridnew, created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM " + physicalTable + " z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerLearnershipHistoryRead");
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
                    processOneRow(table, rs, learnerLearnershipCrosswalk, providerCrosswalk,
                            organisationToBPartnerCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 100 == 0) {
                    addLog("Processed " + processed + " " + SOURCE_TABLE + " rows (" + created + " " + TABLE_NAME
                            + " created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created + " " + TABLE_NAME
                + " created, " + errors.size() + " error(s).";
    }

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> learnerLearnershipCrosswalk,
            Map<Integer, Integer> providerCrosswalk, Map<Integer, Integer> organisationToBPartnerCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerLearnershipId = (Integer) rs.getObject("learnerlearnershipid");
        Integer leadProviderIdOld = (Integer) rs.getObject("leadprovideridold");
        Integer leadProviderIdNew = (Integer) rs.getObject("leadprovideridnew");
        Integer leadEmployerIdOld = (Integer) rs.getObject("leademployeridold");
        Integer leadEmployerIdNew = (Integer) rs.getObject("leademployeridnew");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsLearnerLearnershipHistoryMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", Env.getAD_Org_ID(getCtx()));
            po.set_ValueOfColumn("IsActive", isDeleted == 0 ? "Y" : "N");
            po.set_ValueOfColumn("id", sourceId);

            setIfResolved(po, "ZZLearnerLearnership_ID", learnerLearnershipCrosswalk, learnerLearnershipId);
            setIfResolved(po, "Lead_Provider_Old_ID", providerCrosswalk, leadProviderIdOld);
            setIfResolved(po, "Lead_Provider_New_ID", providerCrosswalk, leadProviderIdNew);
            setIfResolved(po, "Lead_Employer_Old_ID", organisationToBPartnerCrosswalk, leadEmployerIdOld);
            setIfResolved(po, "Lead_Employer_New_ID", organisationToBPartnerCrosswalk, leadEmployerIdNew);

            po.saveEx();
            int newId = po.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated(table.getTableName().toLowerCase(),
                        table.getTableName().toLowerCase() + "_id", newId, createdTs, Env.getAD_User_ID(getCtx()),
                        updatedTs, Env.getAD_User_ID(getCtx()), sourceId, trxName);
            }

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }

    private static void setIfResolved(PO po, String columnName, Map<Integer, Integer> crosswalk, Integer sourceId) {
        if (sourceId == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceId);
        if (targetId != null) {
            po.set_ValueOfColumn(columnName, targetId);
        }
    }

    private void logError(int sourceId, Exception e) {
        if (errors.size() < MAX_LOGGED_ERRORS) {
            errors.add(SOURCE_TABLE + ".id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-learnerlearnershiphistory-errors-" + ts + ".txt");
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
