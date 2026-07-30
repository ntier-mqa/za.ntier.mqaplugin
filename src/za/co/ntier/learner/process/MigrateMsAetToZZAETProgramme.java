package za.co.ntier.learner.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
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

/**
 * Phase 3: migrates the staged "aet" table (21 rows) into ZZAETProgramme. Generic PO API (no
 * generated model class exists yet for this brand new table).
 *
 * <p>SocioEconomicStatus resolved via MigrationSupport.buildListValueCrosswalk against the same
 * ms_lkpsocioeconomicstatus/1000250 reference used throughout this project - expected to resolve
 * empty for every row (source column is always 0 in the current data), which is correct, not a
 * bug. SMS_ID carried across as its raw source value (unresolved).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsAetToZZAETProgramme")
public class MigrateMsAetToZZAETProgramme extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final String TABLE_NAME = "ZZAETProgramme";
    private static final String SOURCE_TABLE = "aet";
    private static final int DEFAULT_CREATED_BY = 1000003;

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

        Map<Integer, String> socioEconomicStatusMap = MigrationSupport.buildListValueCrosswalk(
                "ms_lkpsocioeconomicstatus", 1000250, get_TrxName());

        String sql = "SELECT id, aetprogrammecode, aetprogrammedescription, registrationstartdate, "
                + "       registrationenddate, iscreditbased, credits, socioeconomicstatusid, smsid, "
                + "       created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM " + physicalTable + " z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int created = 0;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                processed++;
                int sourceId = rs.getInt("id");
                Integer isCreditBased = (Integer) rs.getObject("iscreditbased");
                Integer socioEconomicStatusId = (Integer) rs.getObject("socioeconomicstatusid");
                Integer smsId = (Integer) rs.getObject("smsid");
                Timestamp createdTs = rs.getTimestamp("created");
                Timestamp updatedTs = rs.getTimestamp("updated");
                int isDeleted = rs.getInt("isdeleted");

                PO po = table.getPO(0, get_TrxName());
                po.set_ValueOfColumn("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
                po.set_ValueOfColumn("AD_Org_ID", Env.getAD_Org_ID(getCtx()));
                po.set_ValueOfColumn("IsActive", isDeleted == 0 ? "Y" : "N");
                po.set_ValueOfColumn("id", sourceId);
                po.set_ValueOfColumn("Value", rs.getString("aetprogrammecode"));
                po.set_ValueOfColumn("Name", rs.getString("aetprogrammedescription"));
                po.set_ValueOfColumn("Registration_Start_Date", rs.getTimestamp("registrationstartdate"));
                po.set_ValueOfColumn("Registration_End_Date", rs.getTimestamp("registrationenddate"));
                po.set_ValueOfColumn("Is_Credit_Based", MigrationSupport.flagToYN(isCreditBased));
                po.set_ValueOfColumn("Credits", rs.getInt("credits"));
                if (smsId != null) {
                    po.set_ValueOfColumn("SMS_ID", smsId);
                }
                String socioEconomicStatus = socioEconomicStatusId == null ? null
                        : socioEconomicStatusMap.get(socioEconomicStatusId);
                if (socioEconomicStatus != null) {
                    po.set_ValueOfColumn("SocioEconomicStatus", socioEconomicStatus);
                }

                po.saveEx();
                int newId = po.get_ID();
                created++;

                if (createdTs != null) {
                    MigrationSupport.stampCreatedUpdated(physicalTable, physicalTable + "_id", newId,
                            createdTs, DEFAULT_CREATED_BY, updatedTs, DEFAULT_CREATED_BY, sourceId, get_TrxName());
                }
            }
        } finally {
            DB.close(rs, pstmt);
        }

        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created + " " + TABLE_NAME + " created.";
    }
}
