package za.co.ntier.sdr.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new
 * SDR_GrantTransactionApproval table (371 source rows). Despite the name, this table does NOT carry a
 * GrantTransaction FK - CONFIRMED against the source DDL, the mapping doc's column list is complete
 * (no GrantTransaction_ID column exists in mssdr_granttransactionapproval).
 *
 * <p>SDR_COORecommended_ID/SDR_CFORecommended_ID/SDR_CEOApproved_ID all resolve to the shared SDR_YesNo
 * table (platform-wide "*YesNoID" convention, all CONFIRMED 99.7-100% match) - none match "SDR_YesNo"
 * by name, so each needs the explicit override, resolved once and reused. The 3 *By columns follow the
 * platform's audit-trail pattern (Search + REFERENCE_AD_USER).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTransactionApprovalTable")
public class AddSDRGrantTransactionApprovalTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTransactionApproval";
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
                "A grant approval workflow record (mssdr_granttransactionapproval) - not directly linked "
                + "to a specific GrantTransaction row (no such column exists in the source)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantCode_ID", DisplayType.TableDir, 10,
                "mssdr_granttransactionapproval.grantcodeid -> SDR_GrantCode (shared, already confirmed "
                + "platform-wide)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportID", DisplayType.Integer, 10,
                "mssdr_granttransactionapproval.importid - UNMAPPED, opaque, same treatment as elsewhere",
                ENTITY_TYPE, get_TrxName());

        int yesNoRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_YesNo", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_COORecommended_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_granttransactionapproval.coorecommendedid -> SDR_YesNo. CONFIRMED 100% match "
                + "(371/371)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_COORecommendedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_granttransactionapproval.coorecommendedby -> AD_User. CONFIRMED 100% match (371/371)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateCOORecommended", DisplayType.DateTime, 7,
                "mssdr_granttransactionapproval.datecoorecommended", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_CFORecommended_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_granttransactionapproval.cforecommendedid -> SDR_YesNo. CONFIRMED 100% match "
                + "(371/371)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_CFORecommendedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_granttransactionapproval.cforecommendedby -> AD_User. CONFIRMED 100% match (371/371)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateCFORecommended", DisplayType.DateTime, 7,
                "mssdr_granttransactionapproval.datecforecommended", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_CEOApproved_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_granttransactionapproval.ceoapprovedid -> SDR_YesNo. CONFIRMED 99.7% match (370/371)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_CEOApprovedBy", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_granttransactionapproval.ceoapprovedby -> AD_User. CONFIRMED 99.7% match (370/371)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateCEOApproved", DisplayType.DateTime, 7,
                "mssdr_granttransactionapproval.dateceoapproved", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 11 business columns.";
    }
}
