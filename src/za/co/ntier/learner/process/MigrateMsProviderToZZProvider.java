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

import za.co.ntier.api.model.X_ZZProvider;

/**
 * Migrates the staged ms_skillsdevelopmentprovider table into ZZProvider. See "Column Mapping
 * - QCTO Learner Programmes" doc, tab "Provider", and "ZZProvider - New Columns to Add.txt"
 * for the full column-by-column reasoning behind the columns added below (2026-07-16).
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
 * <p>The new Section A/B/C columns added by AddZZProviderColumns.java (2026-07-14 onward) are
 * NOT yet present on the generated X_ZZProvider model class (no Tycho rebuild/model
 * regeneration has happened since those AD_Column rows were created), so they are set via the
 * generic {@code PO.set_ValueOfColumn(String, Object)} API instead of typed setters - same
 * technique AddColumnsSupport.populateReferenceTable already uses for the same reason. Once a
 * model regeneration happens, these could be switched to typed setters, but there's no
 * functional difference either way.
 *
 * <p>Reference columns (Provider_Type_ID, Provider_Class_ID, etc.) are resolved via
 * MigrationSupport.buildIdCrosswalk() against the small reference tables AddZZProviderColumns
 * created - each has an "id" recon column holding the source ms_lkpXXX row's own id, so the
 * crosswalk is ms_lkpXXX.id -&gt; reference table's own PK, exactly like every other
 * buildIdCrosswalk() use in this project. Saqa_QA_ID is the one exception - matched by
 * ETQAID text (MigrationSupport.buildValueCrosswalk), not by id, per the mapping doc Section D.
 * C_BPartner_ID (from organisationid) uses MigrationSupport.buildOrganisationToBPartnerCrosswalk
 * (Organisation.SDLNumber -&gt; c_bpartner.zz_sdl_no, link-only, never creates a C_BPartner).
 *
 * <p>Is_Saica is converted via {@link MigrationSupport#flagToYN(Integer)} (0/nonzero), NOT
 * {@link MigrationSupport#yesNoIdToFlag(Integer)} - confirmed 2026-07-16 by checking the live
 * data: issaica only ever contains 0 or null across every staged row on Provider/
 * WorkplaceApproval/AssessmentCentre, and 0 is not a valid lkpYesNo id (which only has 1/2) -
 * so this can only be the plain 0/nonzero convention, not the lookup-id one.
 *
 * <p>"levy" is now resolved (2026-07-16, user-confirmed): Levy_ID via lkpLevyField, same
 * buildIdCrosswalk pattern as every other Section B reference column.
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

        Map<Integer, Integer> providerTypeCrosswalk = MigrationSupport.buildIdCrosswalk("provider_type", "provider_type_id", get_TrxName());
        Map<Integer, Integer> providerClassCrosswalk = MigrationSupport.buildIdCrosswalk("provider_class", "provider_class_id", get_TrxName());
        Map<Integer, Integer> providerAccreditationStatusCrosswalk = MigrationSupport.buildIdCrosswalk("provider_accreditation_status", "provider_accreditation_status_id", get_TrxName());
        Map<Integer, Integer> qualityAssuranceBodyCrosswalk = MigrationSupport.buildIdCrosswalk("quality_assurance_body", "quality_assurance_body_id", get_TrxName());
        Map<Integer, Integer> accreditationTypeCrosswalk = MigrationSupport.buildIdCrosswalk("accreditation_type", "accreditation_type_id", get_TrxName());
        Map<Integer, Integer> providerInternalExternalCrosswalk = MigrationSupport.buildIdCrosswalk("provider_internal_external", "provider_internal_external_id", get_TrxName());
        Map<Integer, Integer> accreditingCouncilCrosswalk = MigrationSupport.buildIdCrosswalk("accrediting_council", "accrediting_council_id", get_TrxName());
        Map<Integer, Integer> providerApplicationCrosswalk = MigrationSupport.buildIdCrosswalk("provider_application", "provider_application_id", get_TrxName());
        Map<Integer, Integer> levyCrosswalk = MigrationSupport.buildIdCrosswalk("levy", "levy_id", get_TrxName());
        Map<String, Integer> saqaQaCrosswalk = MigrationSupport.buildValueCrosswalk("saqa_qa", "saqa_qa_id", get_TrxName());
        Map<Integer, Integer> organisationToBPartnerCrosswalk = MigrationSupport.buildOrganisationToBPartnerCrosswalk(get_TrxName());

        String sql = "SELECT id, providercode, organisationid, providertypeid, providerclassid, "
                + "       provideraccreditationstatusid, accreditationstartdate, accreditationenddate, "
                + "       qualityassurancebodyid, saqacode, saqaprovidercode, dhetregistrationstartdate, "
                + "       dhetregistrationenddate, dhetregistrationnumber, issaica, webaddress, saqaqaid, "
                + "       providerapplicationid, qctoprovidernumber, accreditationtypeid, "
                + "       applicationreceiveddate, providerinternalexternalid, provideralertemail, "
                + "       accreditingcouncil, accreditationreviewdate, levy, created, updated, isdeleted "
                + "FROM " + SOURCE_TABLE
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
                    processOneRow(rs, providerTypeCrosswalk, providerClassCrosswalk, providerAccreditationStatusCrosswalk,
                            qualityAssuranceBodyCrosswalk, accreditationTypeCrosswalk, providerInternalExternalCrosswalk,
                            accreditingCouncilCrosswalk, providerApplicationCrosswalk, levyCrosswalk, saqaQaCrosswalk,
                            organisationToBPartnerCrosswalk);
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

    private void processOneRow(ResultSet rs, Map<Integer, Integer> providerTypeCrosswalk,
            Map<Integer, Integer> providerClassCrosswalk, Map<Integer, Integer> providerAccreditationStatusCrosswalk,
            Map<Integer, Integer> qualityAssuranceBodyCrosswalk, Map<Integer, Integer> accreditationTypeCrosswalk,
            Map<Integer, Integer> providerInternalExternalCrosswalk, Map<Integer, Integer> accreditingCouncilCrosswalk,
            Map<Integer, Integer> providerApplicationCrosswalk, Map<Integer, Integer> levyCrosswalk,
            Map<String, Integer> saqaQaCrosswalk, Map<Integer, Integer> organisationToBPartnerCrosswalk) throws Exception {
        int sourceId = rs.getInt("id");
        String providerCode = rs.getString("providercode");
        Integer organisationId = (Integer) rs.getObject("organisationid");
        Integer providerTypeId = (Integer) rs.getObject("providertypeid");
        Integer providerClassId = (Integer) rs.getObject("providerclassid");
        Integer providerAccreditationStatusId = (Integer) rs.getObject("provideraccreditationstatusid");
        Integer qualityAssuranceBodyId = (Integer) rs.getObject("qualityassurancebodyid");
        Integer issaica = (Integer) rs.getObject("issaica");
        Integer saqaQaId = (Integer) rs.getObject("saqaqaid");
        Integer providerApplicationId = (Integer) rs.getObject("providerapplicationid");
        Integer accreditationTypeId = (Integer) rs.getObject("accreditationtypeid");
        Integer providerInternalExternalId = (Integer) rs.getObject("providerinternalexternalid");
        Integer accreditingCouncilId = (Integer) rs.getObject("accreditingcouncil");
        Integer levyId = (Integer) rs.getObject("levy");
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
            // name: no source column - see Section C, organisationid resolves to C_BPartner_ID,
            // not a display name.

            // Section A - plain columns
            setGeneric(provider, "Accreditation_Start_Date", rs.getTimestamp("accreditationstartdate"));
            setGeneric(provider, "Accreditation_End_Date", rs.getTimestamp("accreditationenddate"));
            setGeneric(provider, "Saqa_Code", rs.getString("saqacode"));
            setGeneric(provider, "Saqa_Provider_Code", rs.getString("saqaprovidercode"));
            setGeneric(provider, "Dhet_Registration_Start_Date", rs.getTimestamp("dhetregistrationstartdate"));
            setGeneric(provider, "Dhet_Registration_End_Date", rs.getTimestamp("dhetregistrationenddate"));
            setGeneric(provider, "Dhet_Registration_Number", rs.getString("dhetregistrationnumber"));
            setGeneric(provider, "Is_Saica", MigrationSupport.flagToYN(issaica));
            setGeneric(provider, "Web_Address", rs.getString("webaddress"));
            setGeneric(provider, "Qcto_Provider_Number", rs.getString("qctoprovidernumber"));
            setGeneric(provider, "Application_Received_Date", rs.getTimestamp("applicationreceiveddate"));
            setGeneric(provider, "Provider_Alert_Email", rs.getString("provideralertemail"));
            setGeneric(provider, "Accreditation_Review_Date", rs.getTimestamp("accreditationreviewdate"));

            // Section B - reference columns
            setIfResolved(provider, "Provider_Type_ID", providerTypeCrosswalk, providerTypeId);
            setIfResolved(provider, "Provider_Class_ID", providerClassCrosswalk, providerClassId);
            setIfResolved(provider, "Provider_Accreditation_Status_ID", providerAccreditationStatusCrosswalk, providerAccreditationStatusId);
            setIfResolved(provider, "Quality_Assurance_Body_ID", qualityAssuranceBodyCrosswalk, qualityAssuranceBodyId);
            setIfResolved(provider, "Accreditation_Type_ID", accreditationTypeCrosswalk, accreditationTypeId);
            setIfResolved(provider, "Provider_Internal_External_ID", providerInternalExternalCrosswalk, providerInternalExternalId);
            setIfResolved(provider, "Accrediting_Council_ID", accreditingCouncilCrosswalk, accreditingCouncilId);
            setIfResolved(provider, "Provider_Application_ID", providerApplicationCrosswalk, providerApplicationId);
            setIfResolved(provider, "Levy_ID", levyCrosswalk, levyId);
            setIfResolvedByValue(provider, "Saqa_QA_ID", saqaQaCrosswalk, saqaQaId == null ? null : String.valueOf(saqaQaId));

            // Section C - C_BPartner_ID
            setIfResolved(provider, "C_BPartner_ID", organisationToBPartnerCrosswalk, organisationId);

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

    /** Sets a column via the generic PO API, only if the value is non-null (used for the new
     * Section A/B/C columns, which have no typed setter on the generated model class yet). */
    private static void setGeneric(PO po, String columnName, Object value) {
        if (value != null) {
            po.set_ValueOfColumn(columnName, value);
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

    private static void setIfResolvedByValue(PO po, String columnName, Map<String, Integer> crosswalk, String sourceValue) {
        if (sourceValue == null) {
            return;
        }
        Integer targetId = crosswalk.get(sourceValue.trim());
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
