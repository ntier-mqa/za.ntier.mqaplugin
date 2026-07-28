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
 * Phase 2 (see "Phase 2 - LearnerLearnership Family - Mapping.txt" table #2): migrates the
 * staged ms_learnerlearnershipdocument table (2,447 rows) into the brand new
 * ZZLearnerLearnershipDocument table. Generic PO API, same reasoning as
 * {@link MigrateMsLearnerLearnershipHistoryToZZLearnerLearnershipHistory}.
 *
 * <p>Document_Type is carried across as its raw source id (unresolved - no MSSQL lookup table
 * found for learnerlearnershipdocumenttypeid, see AddZZLearnerLearnershipDocumentTable's Javadoc).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsLearnerLearnershipDocumentToZZLearnerLearnershipDocument")
public class MigrateMsLearnerLearnershipDocumentToZZLearnerLearnershipDocument extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TABLE_NAME = "ZZLearnerLearnershipDocument";
    private static final String SOURCE_TABLE = "ms_learnerlearnershipdocument";
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

        String sql = "SELECT id, learnerlearnershipid, learnerlearnershipdocumenttypeid, originalfilename, "
                + "       savedfilename, filepath, created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM " + physicalTable + " z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsLearnerLearnershipDocumentRead");
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
                    processOneRow(table, rs, learnerLearnershipCrosswalk);
                    created++;
                } catch (Exception e) {
                    logError(rs.getInt("id"), e);
                }
                if (processed % 500 == 0) {
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

    private void processOneRow(MTable table, ResultSet rs, Map<Integer, Integer> learnerLearnershipCrosswalk)
            throws Exception {
        int sourceId = rs.getInt("id");
        Integer learnerLearnershipId = (Integer) rs.getObject("learnerlearnershipid");
        Integer documentType = (Integer) rs.getObject("learnerlearnershipdocumenttypeid");
        String originalFileName = rs.getString("originalfilename");
        String savedFileName = rs.getString("savedfilename");
        String filePath = rs.getString("filepath");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsLearnerLearnershipDocumentMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            PO po = table.getPO(0, trxName);
            po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            po.set_ValueOfColumn("AD_Org_ID", Env.getAD_Org_ID(getCtx()));
            po.set_ValueOfColumn("IsActive", isDeleted == 0 ? "Y" : "N");
            po.set_ValueOfColumn("id", sourceId);
            po.set_ValueOfColumn("Original_File_Name", originalFileName);
            po.set_ValueOfColumn("Saved_File_Name", savedFileName);
            po.set_ValueOfColumn("File_Path", filePath);
            if (documentType != null) {
                po.set_ValueOfColumn("Document_Type", documentType);
            }

            Integer targetLearnerLearnershipId = learnerLearnershipId == null ? null
                    : learnerLearnershipCrosswalk.get(learnerLearnershipId);
            if (targetLearnerLearnershipId != null) {
                po.set_ValueOfColumn("ZZLearnerLearnership_ID", targetLearnerLearnershipId);
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
        File logFile = new File("/tmp/migrate-ms-learnerlearnershipdocument-errors-" + ts + ".txt");
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
