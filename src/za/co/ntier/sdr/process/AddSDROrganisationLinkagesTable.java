package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new
 * SDR_OrganisationLinkages child table (596 source rows, self-referencing - same shape as Person's
 * SDR_ParentPerson_ID).
 *
 * <p>SDR_Organisation has no generic "Name" column (it uses SDR_LegalName/SDR_TradeName instead), so
 * the self-reference's AD_Ref_Table.AD_Display must be passed explicitly - "SDR_LegalName" is used,
 * same reasoning as SDR_Person's self-reference using "Surname".
 *
 * <p>SDR_FinancialYear_ID and SDR_FinancialYearEnd_ID both match their target tables by name (plain
 * TableDir); SDR_FinancialYearStart_ID does not ("SDR_FinancialYearStart" != "SDR_FinancialYear") and
 * needs an explicit override, reusing the same target table.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationLinkagesTable")
public class AddSDROrganisationLinkagesTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationLinkages";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "3";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        MTable existing = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (existing != null) {
            addLog(TABLE_NAME + " already exists - not recreated.");
            return TABLE_NAME + " already exists - no action taken.";
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "A parent/child link between two organisations (mssdr_organisationlinkages)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        // Self-referencing FKs: SDR_Organisation has no "Name" column, so the display column must be
        // passed explicitly. Both Parent/Child columns target the same table, so the reference is
        // resolved once and reused.
        int organisationRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Organisation",
                "SDR_LegalName", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ParentOrganisation_ID", DisplayType.Table,
                organisationRefId, 10,
                "mssdr_organisationlinkages.parentorganisationid -> SDR_Organisation (self-referencing). "
                + "CONFIRMED 100% match (596/596)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ChildOrganisation_ID", DisplayType.Table,
                organisationRefId, 10,
                "mssdr_organisationlinkages.childorganisationid -> SDR_Organisation (self-referencing). "
                + "CONFIRMED 99.8% match (595/596) - 1 row's value does not resolve, left null for that row",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LinkStartDate", DisplayType.DateTime, 7,
                "mssdr_organisationlinkages.linkstartdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LinkEndDate", DisplayType.DateTime, 7,
                "mssdr_organisationlinkages.linkenddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_UploadDocument_ID", DisplayType.Integer, 10,
                "mssdr_organisationlinkages.uploaddocumentid - UNMAPPED, no matching document table found - "
                + "carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RemoveDocument_ID", DisplayType.Integer, 10,
                "mssdr_organisationlinkages.removedocumentid - UNMAPPED, no matching document table found - "
                + "carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_organisationlinkages.financialyearid -> SDR_FinancialYear (shared catalog)", ENTITY_TYPE,
                get_TrxName());

        int financialYearRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_FinancialYear",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_FinancialYearStart_ID", DisplayType.Table,
                financialYearRefId, 10,
                "mssdr_organisationlinkages.financialyearstartid -> SDR_FinancialYear. CONFIRMED 100% match "
                + "(596/596)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYearEnd_ID", DisplayType.TableDir, 10,
                "mssdr_organisationlinkages.financialyearendid -> SDR_FinancialYearEnd (lookup itself is "
                + "empty in the source - resolves to NULL until populated)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 8 business columns.";
    }
}
