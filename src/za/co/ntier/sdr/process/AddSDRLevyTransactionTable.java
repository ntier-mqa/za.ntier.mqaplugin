package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyTransaction MAIN
 * table (351,578 source rows).
 *
 * <p>SDR_Organisation_ID resolves via a DERIVED text-parse of ReferenceNumber (format
 * "&lt;Year&gt;||&lt;LNumber&gt;", extract the L-number and match against Organisation.SDLNumber -
 * CONFIRMED 100% match, 351,578/351,578) rather than a direct FK column - that parsing happens in the
 * eventual Migrate*-style data-population process, not here; the schema simply registers it as a plain
 * TableDir lookup column (name matches SDR_Organisation).
 *
 * <p>IMPORTANT CORRECTION carried from the mapping doc: an earlier pass wrongly reported
 * SDR_ImportID -&gt; SDR_LevyImport as a clean 2-hop crosswalk. Re-verified and found to be a FALSE
 * POSITIVE (same ID-range-coincidence pitfall as GrantTransaction.ProcessID) - ImportID's real value
 * range (6,394-7,021) simply sits inside LevyImport's much larger ID range (1-351,578); rows sharing
 * one ImportID have completely different organisations. SDR_ImportID, SDR_ProcessID and
 * SDR_TransactionID are all carried as plain integers, NOT resolved FKs.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyTransactionTable")
public class AddSDRLevyTransactionTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyTransaction";
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
                "A levy transaction (mssdr_levytransaction)", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionType", DisplayType.String, 250,
                "mssdr_levytransaction.transactiontype", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionDate", DisplayType.DateTime, 7,
                "mssdr_levytransaction.transactiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReferenceNumber", DisplayType.String, 250,
                "mssdr_levytransaction.referencenumber - kept verbatim, also the source of the "
                + "Organisation crosswalk below, worth keeping visible for audit/traceability", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReferenceText", DisplayType.String, 250,
                "mssdr_levytransaction.referencetext", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_levytransaction.referencenumber (DERIVED: format '<Year>||<LNumber>', extract the "
                + "L-number and match against Organisation.SDLNumber) -> SDR_Organisation. CONFIRMED 100% "
                + "match (351,578/351,578) - a single-hop text-parse crosswalk, NOT a join through "
                + "LevyImport (see class Javadoc correction)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportID", DisplayType.Integer, 10,
                "mssdr_levytransaction.importid - FALSE POSITIVE crosswalk (see class Javadoc), does NOT "
                + "reliably reference LevyImport despite a superficial 100% existence-match. Carried as a "
                + "plain integer for reference/audit only", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_levytransaction.processid - CHECKED: 0% match against LevyProcess.ID (ranges don't "
                + "overlap), carried as a plain integer, not resolved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionID", DisplayType.Integer, 10,
                "mssdr_levytransaction.transactionid - not independently re-tested, expected to be a "
                + "small-range per-batch sequence number (same shape as Grant's equivalent column), not a "
                + "global FK - carried as a plain integer", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 8 business columns.";
    }
}
