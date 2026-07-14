package za.co.ntier.learner.process;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZProvider;

/**
 * Migrates the staged ms_skillsdevelopmentprovider table into ZZProvider - a bare
 * code/value/name reference table used by the QCTO join tables (LeadSDProvider,
 * SecondarySDProvider, SDProvider FKs). See "Column Mapping - QCTO Learner Programmes"
 * doc, tab "Provider".
 *
 * <p>OPEN QUESTION (not yet confirmed by the business, see mapping doc "Open Items" #1):
 * MSSQL has both a generic "Provider" table (881 rows) and "SkillsDevelopmentProvider"
 * (54 rows). This process sources from SkillsDevelopmentProvider because the QCTO join
 * tables' FK columns are literally named leadsdproviderid/secondarysdproviderid/
 * sdproviderid ("SD" = Skills Development) - but this has NOT been confirmed. If the
 * business says otherwise, change the FROM table below (and re-point
 * MigrationSupport.buildIdCrosswalk("zzprovider", ...) callers - no other change needed).
 *
 * <p>REQUIRES the recon column: {@code ALTER TABLE adempiere.zzprovider ADD COLUMN id bigint;}
 * (already run 2026-07-10) - stamps ms_skillsdevelopmentprovider.id into it, same convention
 * as MigrateMsPersonToZZPerson.
 *
 * <p>NOT handled - zzprovider has no column to receive these (bare table, only
 * zzprovidercode/value/name exist): organisationid (would give a real provider name - see
 * "Unmapped" note below), providertypeid, providerclassid, provideraccreditationstatusid,
 * accreditationstartdate/enddate, qualityassurancebodyid, saqacode, saqaprovidercode,
 * dhetregistration*, issaica, webaddress, saqaqaid, providerapplicationid,
 * qctoprovidernumber, accreditationtypeid, applicationreceiveddate,
 * providerinternalexternalid, provideralertemail, accreditingcouncil,
 * accreditationreviewdate, levy. If the business wants any of these tracked, zzprovider
 * needs new columns first (AD_Column change - out of scope here, needs sign-off).
 */
@Process(name = "za.co.ntier.learner.process.MigrateMsProviderToZZProvider")
public class MigrateMsProviderToZZProvider extends SvrProcess {

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    @Parameter(name = "ClearDataFirst")
    private String p_ClearDataFirst;

    private static final int DEFAULT_CREATED_BY = 1000003;
    private static final int MAX_LOGGED_ERRORS = 1000;
    private static final String SOURCE_TABLE = "ms_skillsdevelopmentprovider";

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
            int count = DB.getSQLValueEx(get_TrxName(), "SELECT count(*) FROM zzprovider WHERE id IS NOT NULL");
            addLog("ClearDataFirst=Y: deleting " + count + " previously-migrated ZZProvider row(s)...");
            DB.executeUpdateEx("DELETE FROM zzprovider WHERE id IS NOT NULL", null, get_TrxName());
            DB.commit(true, get_TrxName());
        }

        String sql = "SELECT id, providercode, created, updated, isdeleted FROM " + SOURCE_TABLE
                + " WHERE NOT EXISTS (SELECT 1 FROM zzprovider z WHERE z.id = " + SOURCE_TABLE + ".id) "
                + "ORDER BY id" + (maxRows > 0 ? " LIMIT " + maxRows : "");

        String readTrxName = Trx.createTrxName("MsProviderRead");
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
                    addLog("Processed " + processed + " " + SOURCE_TABLE + " rows (" + created
                            + " ZZProvider created, " + errors.size() + " error(s))...");
                }
            }
        } finally {
            DB.close(rs, pstmt);
            readTrx.rollback();
            readTrx.close();
        }

        writeErrorLogIfAny();
        return "Processed " + processed + " " + SOURCE_TABLE + " row(s): " + created
                + " ZZProvider created, " + errors.size() + " error(s).";
    }

    private void processOneRow(ResultSet rs) throws Exception {
        int sourceId = rs.getInt("id");
        String providerCode = rs.getString("providercode");
        Timestamp createdTs = rs.getTimestamp("created");
        Timestamp updatedTs = rs.getTimestamp("updated");
        int isDeleted = rs.getInt("isdeleted");

        String trxName = Trx.createTrxName("MsProviderMigrate");
        Trx trx = Trx.get(trxName, true);
        try {
            X_ZZProvider provider = new X_ZZProvider(getCtx(), 0, trxName);
            provider.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            provider.setIsActive(isDeleted == 0);
            provider.setZZProviderCode(providerCode);
            provider.setValue(providerCode);
            // name: no source column - OrganisationID on ms_skillsdevelopmentprovider would
            // give the real organisation name, but the Organisation-family table hasn't been
            // identified/staged yet. Left null - see class Javadoc "NOT handled".

            provider.saveEx();
            int zzProviderId = provider.get_ID();

            if (createdTs != null) {
                MigrationSupport.stampCreatedUpdated("zzprovider", "zzprovider_id", zzProviderId,
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
            errors.add(SOURCE_TABLE + ".id=" + sourceId + ": " + e.getMessage());
        }
    }

    private void writeErrorLogIfAny() {
        if (errors.isEmpty()) {
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File logFile = new File("/tmp/migrate-ms-provider-errors-" + ts + ".txt");
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
