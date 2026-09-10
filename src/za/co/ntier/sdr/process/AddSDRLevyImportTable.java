package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 6 (see "Phase 6 - Levy Family - Mapping.txt"): creates the brand new SDR_LevyImport table
 * (351,578 source rows) - a STANDALONE catalog of levy declarations/import events. CORRECTED ROLE
 * (mapping doc top-of-doc correction): this is NOT the parent of SDR_LevyTransaction, despite an
 * earlier investigation pass wrongly reporting that relationship - it's its own independent log,
 * unrelated by FK to LevyTransaction, with its own working Organisation crosswalk. Requires
 * {@link AddSDRLevyAccountTable} to have already run.
 *
 * <p>SDR_Organisation_ID resolves via LNumber directly (no text-parsing needed, unlike
 * LevyTransaction.ReferenceNumber) - CONFIRMED 100% of 3,443 distinct LNumbers match
 * Organisation.SDLNumber. That resolution happens in the eventual Migrate*-style process; the schema
 * here just registers a plain TableDir lookup column (name matches SDR_Organisation).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRLevyImportTable")
public class AddSDRLevyImportTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_LevyImport";
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
                "A levy declaration/import event (mssdr_levyimport) - standalone, NOT a child of "
                + "SDR_LevyTransaction despite an earlier investigation pass's incorrect assumption",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyAccount_ID", DisplayType.TableDir, 10,
                "mssdr_levyimport.levyaccountid -> SDR_LevyAccount. CONFIRMED 100% match (351,578/351,578)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Amount", DisplayType.Amount, 22,
                "mssdr_levyimport.amount", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_levyimport.lnumber -> SDR_Organisation.SDLNumber (direct match, no parsing needed). "
                + "CONFIRMED 100% of 3,443 distinct LNumbers resolve", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LNumber", DisplayType.String, 50,
                "mssdr_levyimport.lnumber - kept verbatim alongside the resolved FK, same "
                + "audit/traceability reasoning as SDR_LevyTransaction.SDR_ReferenceNumber", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessDate", DisplayType.DateTime, 7,
                "mssdr_levyimport.processdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportDate", DisplayType.DateTime, 7,
                "mssdr_levyimport.importdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SchemeYear", DisplayType.Integer, 10,
                "mssdr_levyimport.schemeyear", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyImportsID", DisplayType.Integer, 10,
                "mssdr_levyimport.levyimportsid - UNMAPPED, CHECKED: mostly NULL (315,768/351,578), "
                + "populated values cluster into opaque batches with no matching table found - carried as "
                + "a plain integer", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 8 business columns.";
    }
}
