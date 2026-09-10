package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyFile table (a
 * single source row). CONFIRMED 2026-09-04 (user decision): built despite being a single-row table -
 * looks like a one-off reconciliation manifest rather than an ongoing operational table.
 *
 * <p>The source has no "id" column either (same shape as SDR_LevyGPProcessData) - the standard recon
 * "id" column stays unpopulated. SDR_ReceiptDate/SDR_PostedDate are stored as text ("20260301"-style),
 * not real datetime types - kept as String rather than cast. SDR_Levy/Discretionary/Administration/
 * Interest/Penalties/Total/SETATransfer/Unknown all look like currency amounts but are stored as text
 * in the source - carried as-is, not cast to Amount.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyFileTable")
public class AddSDRLevyFileTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyFile";
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
                "A one-off levy reconciliation manifest (mssdr_levyfile) - single-row table, built anyway "
                + "per user decision", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReceiptDate", DisplayType.String, 50,
                "mssdr_levyfile.receiptdate (stored as text, '20260301'-style, not a real datetime)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SETA_ID", DisplayType.TableDir, 10,
                "mssdr_levyfile.setaid -> SDR_SETA. CONFIRMED match against lkpSETA", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LNumber", DisplayType.String, 50,
                "mssdr_levyfile.lnumber - a real Organisation SDL number (e.g. 'L880776283'), same "
                + "crosswalk pattern as elsewhere in this family, not resolved to a FK on this table",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostedDate", DisplayType.String, 50,
                "mssdr_levyfile.posteddate (stored as text, same shape as SDR_ReceiptDate)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Levy", DisplayType.String, 50,
                "mssdr_levyfile.levy (looks like a currency amount, stored as text)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Discretionary", DisplayType.String, 50,
                "mssdr_levyfile.discretionary (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Administration", DisplayType.String, 50,
                "mssdr_levyfile.administration (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Interest", DisplayType.String, 50,
                "mssdr_levyfile.interest (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Penalties", DisplayType.String, 50,
                "mssdr_levyfile.penalties (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Total", DisplayType.String, 50,
                "mssdr_levyfile.total (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SETATransfer", DisplayType.String, 50,
                "mssdr_levyfile.setatransfer (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Unknown", DisplayType.String, 50,
                "mssdr_levyfile.unknown (text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SchemeYear", DisplayType.Integer, 10,
                "mssdr_levyfile.schemeyear", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 13 business columns.";
    }
}
