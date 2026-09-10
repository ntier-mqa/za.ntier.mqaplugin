package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantTransaction MAIN
 * table (447,404 source rows).
 *
 * <p>SDR_Organisation_ID resolves via a two-tier DERIVED text-parse of ReferenceNumber (format
 * "&lt;Year&gt;|&lt;GrantCode&gt;|&lt;OrgNumber&gt;"): exact full-string match against
 * Organisation.SDLNumber first (clean, unambiguous), falling back to a 9-digit-numeric-suffix-only
 * match (ignoring the leading letter - the letter is a registration-type code, NOT part of the
 * organisation's identity) for everything else. Together these resolve 99.4% of all rows
 * (444,580/447,404) - the headline finding of this family's investigation (the leading letter was
 * previously assumed to matter and only the "L" subset, 7.7%, was thought resolvable). ~8.8% of the
 * fallback matches are ambiguous (map to more than one Organisation row, mostly degenerate placeholder
 * SDL numbers) - needs a documented tie-break in the eventual Migrate*-style data-population process
 * (exact match first, numeric-suffix fallback second, flag residual ambiguity for manual review). That
 * resolution logic belongs to the data-population step, not this schema; the schema here just registers
 * a plain TableDir lookup column (name matches SDR_Organisation).
 *
 * <p>SDR_ImportID/SDR_ProcessID/SDR_TransactionID are all UNMAPPED (opaque, false-positive-risk, and
 * per-batch-sequence respectively - see mapping doc) - carried as plain integers.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantTransactionTable")
public class AddSDRGrantTransactionTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantTransaction";
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
                "A grant transaction (mssdr_granttransaction)", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionType", DisplayType.String, 250,
                "mssdr_granttransaction.transactiontype", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionDate", DisplayType.DateTime, 7,
                "mssdr_granttransaction.transactiondate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReferenceNumber", DisplayType.String, 250,
                "mssdr_granttransaction.referencenumber - kept verbatim, also the source of the "
                + "Organisation crosswalk below, for audit/traceability", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReferenceText", DisplayType.String, 250,
                "mssdr_granttransaction.referencetext", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_granttransaction.referencenumber (DERIVED: two-tier resolution, see class Javadoc) "
                + "-> SDR_Organisation. CONFIRMED 99.4% of all rows (444,580/447,404) resolve", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportID", DisplayType.Integer, 10,
                "mssdr_granttransaction.importid - UNMAPPED, opaque, no GrantImport table exists to test "
                + "against", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_granttransaction.processid - UNMAPPED, only 65.2% match against GrantProcess.ID and "
                + "ID ranges don't fully overlap - false-positive risk, left unresolved", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TransactionID", DisplayType.Integer, 10,
                "mssdr_granttransaction.transactionid - UNMAPPED, only 744 distinct values (range 0-999) "
                + "across 447,404 rows - a per-batch sequence number scoped within one ProcessID's XML "
                + "payload, not a global FK", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IsSRUVendor", DisplayType.Integer, 10,
                "mssdr_granttransaction.issruvendor (int flag, not independently investigated)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 9 business columns.";
    }
}
