package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 7 (see "Phase 7 - Grant Family - Mapping.txt"): creates the brand new SDR_GrantProcess table
 * (12,843 source rows) - LOW BUSINESS VALUE, same shape as Levy's SDR_LevyProcess. Must be built before
 * {@link AddSDRGrantGPProcessDataTable}, which FKs to it.
 *
 * <p>SDR_LevyGrantProcessQueueStatus_ID is UNMAPPED - CHECKED: no matching lookup table exists anywhere
 * in the staged lkp* tables (this is a RETROACTIVE correction found during this family's pass - the
 * identical column on Levy's SDR_LevyProcess had not previously been flagged as unresolved there
 * either, same genuine gap in both families). SDR_GrantSentXML/SDR_GrantReturnXML are large free-text
 * XML blobs (where the ReferenceNumber/Creditor crosswalk pattern was originally discovered) -
 * CONFIRMED 2026-09-04 (user decision): left unparsed, since GrantTransaction's own crosswalk already
 * resolves 99.4% on its own, making XML-parsing redundant.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRGrantProcessTable")
public class AddSDRGrantProcessTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_GrantProcess";
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
                "A grant batch process record (mssdr_grantprocess) - low business value", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProcessID", DisplayType.Integer, 10,
                "mssdr_grantprocess.processid - UNMAPPED, does not match anything else in this scope",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportID", DisplayType.Integer, 10,
                "mssdr_grantprocess.importid - UNMAPPED, opaque, same as elsewhere", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantCode", DisplayType.String, 250,
                "mssdr_grantprocess.grantcode (free text, not independently checked against "
                + "lkpGrantCode.description this pass)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LevyGrantProcessQueueStatus_ID", DisplayType.Integer,
                10, "mssdr_grantprocess.levygrantprocessqueuestatusid - UNMAPPED, CHECKED: no matching "
                + "lookup table exists anywhere in the staged lkp* tables - genuinely unresolved, same "
                + "conclusion as the identical column on Levy's SDR_LevyProcess", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusCode", DisplayType.Integer, 10,
                "mssdr_grantprocess.statuscode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StatusDescription", DisplayType.String, 250,
                "mssdr_grantprocess.statusdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantSentXML", DisplayType.TextLong, 0,
                "mssdr_grantprocess.grantsentxml - large free-text XML blob, not parsed, carried unparsed "
                + "for reference only (redundant with GrantTransaction's own 99.4%-resolving crosswalk)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GrantReturnXML", DisplayType.TextLong, 0,
                "mssdr_grantprocess.grantreturnxml - same as SDR_GrantSentXML, unparsed", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 8 business columns.";
    }
}
