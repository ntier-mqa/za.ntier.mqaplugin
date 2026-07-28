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
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #3): migrates the
 * staged ms_learnerlearnershipemployer table (74,989 rows) into the brand new
 * ZZLearnerLearnershipEmployer table. Generic PO API, same reasoning as
 * {@link MigrateMsLearnerLearnershipHistoryToZZLearnerLearnershipHistory}.
 *
 * <p>Employer_ID via MigrationSupport.buildOrganisationToBPartnerCrosswalk (same as every other
 * "employerid"/"organisationid" column in this project). ZZLevy via
 * MigrationSupport.buildListValueCrosswalk against the shared "Yes_No_Not_Applicable" List
 * reference AddZZLearnerLearnershipEmployerTable/AddZZLearnerLearnershipColumns find-or-create
 * (looked up here by name via AddColumnsSupport.findListReference, since this process runs
 * separately from whichever Add*Table process happened to create it first).
 * ZZLearnershipEmployerType carried across as its raw source id (held back as unresolved, per
 * the mapping doc's decision on the "Lead Employer" hypothesis). Employer_Contact_ID not
 * populated - same unresolved zz_formcontact situation noted throughout Phase 2.
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerLearnershipEmployerToZZLearnerLearnershipEmployer")
public class MigrateMsLearnerLearnershipEmployerToZZLearnerLearnershipEmployer extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TABLE_NAME = "ZZLearnerLearnershipEmployer";
    private static final String SOURCE_TABLE = "ms_learnerlearnershipemployer";
    private static final String YES_NO_NA_REFERENCE_NAME = "Yes_No_Not_Applicable";
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
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(
                get_TrxName());

        int yesNoNaReferenceId = AddColumnsSupport.findListReference(getCtx(), YES_NO_NA_REFERENCE_NAME, get_TrxName());
        if (yesNoNaReferenceId == 0) {
            throw new AdempiereException("List reference '" + YES_NO_NA_REFERENCE_NAME
                    + "' not found - run AddZZLearnerLearnershipEmployerTable or AddZZLearnerLearnershipColumns first.");
        }
        Map<Integer, String> levyCrosswalk = MigrationSupport.buildListValueCrosswalk(
                "ms_lkpyesnonotapplicable", yesNoNaReferenceId, get_TrxName());

        String sql = "SELECT id, learnerlearnershipid, employerid, learnershipemployertypeid, levyyesnoid, "
                + "       created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM " + physicalTable + " z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerLearnershipEmployerRead");
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
                    processOneRow(table, rs, learnerLearnershipCrosswalk, organisationToBPartnerCrosswalk,
                            levyCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 5000 == 0) {
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
            Map<Integer, Integer> organisationToBPartnerCrosswalk, Map<Integer, String> levyCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerLearnershipId = (Integer) rs.getObject("learnerlearnershipid");
        Integer employerId = (Integer) rs.getObject("employerid");
        Integer employerType = (Integer) rs.getObject("learnershipemployertypeid");
        Integer levyYesNoId = (Integer) rs.getObject("levyyesnoid");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsLearnerLearnershipEmployerMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", Env.getAD_Org_ID(getCtx()));
            po.set_ValueOfColumn("IsActive", isDeleted == 0 ? "Y" : "N");
            po.set_ValueOfColumn("id", sourceId);
            if (employerType != null) {
                po.set_ValueOfColumn("ZZLearnershipEmployerType", employerType);
            }

            Integer targetLearnerLearnershipId = learnerLearnershipId == null ? null
                    : learnerLearnershipCrosswalk.get(learnerLearnershipId);
            if (targetLearnerLearnershipId != null) {
                po.set_ValueOfColumn("ZZLearnerLearnership_ID", targetLearnerLearnershipId);
            }

            Integer targetBPartnerId = employerId == null ? null : organisationToBPartnerCrosswalk.get(employerId);
            if (targetBPartnerId != null) {
                po.set_ValueOfColumn("Employer_ID", targetBPartnerId);
            }

            String levyValue = levyYesNoId == null ? null : levyCrosswalk.get(levyYesNoId);
            if (levyValue != null) {
                po.set_ValueOfColumn("ZZLevy", levyValue);
            }

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
        File logFile = new File("/tmp/migrate-ms-learnerlearnershipemployer-errors-" + ts + ".txt");
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
