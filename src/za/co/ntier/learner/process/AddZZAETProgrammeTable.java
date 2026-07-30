package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

/**
 * Phase 3 (see "Additional Tables - Migration Plan.txt", corrected 2026-07-21 during Phase 2):
 * creates the brand new ZZAETProgramme reference/catalog table (21 rows expected, from the
 * staged "aet" table - a bare-named staged source, no "ms_" prefix, and no corresponding
 * AD_Table at all before this - the plan doc had originally miscategorised this as "already has
 * a real target table, likely DONE").
 *
 * <p>Built via {@link AddColumnsSupport#createReferenceTableSchema} (Value=aetprogrammecode,
 * Name=aetprogrammedescription) then extra columns added via {@link AddColumnsSupport#addColumn}
 * - same 2-step pattern as {@link AddZZLearningAreaTable}.
 *
 * <p>SocioEconomicStatus reuses the SAME List reference (1000250) already used throughout this
 * project - the source column is always 0 in the "aet" staging table's current data (never
 * meaningfully populated), so this will resolve empty for every row for now, which is expected,
 * not a bug. SMS_ID (smsid) is added as plain Integer, unresolved - no lookup table found under
 * any name searched, and no other table in this project references "smsid" either.
 */
@Process(name = "za.co.ntier.learner.process.AddZZAETProgrammeTable")
public class AddZZAETProgrammeTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZAETProgramme";
    private static final String ENTITY_TYPE = "MQA Learner";
    private static final String ACCESS_LEVEL = "3";
    private static final int REFERENCE_SOCIOECONOMICSTATUS = 1000250;

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

        MTable table = AddColumnsSupport.createReferenceTableSchema(getCtx(), TABLE_NAME,
                "AET Programme reference (staged \"aet\" table): Value=aetprogrammecode, "
                + "Name=aetprogrammedescription", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(), this::addLog);

        AddColumnsSupport.addColumn(getCtx(), table, "Registration_Start_Date", DisplayType.DateTime, 7,
                "aet.registrationstartdate", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Registration_End_Date", DisplayType.DateTime, 7,
                "aet.registrationenddate", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Is_Credit_Based", DisplayType.YesNo, 1,
                "aet.iscreditbased", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "Credits", DisplayType.Integer, 10,
                "aet.credits", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "SocioEconomicStatus", DisplayType.List,
                REFERENCE_SOCIOECONOMICSTATUS, 1, "aet.socioeconomicstatusid -> ms_lkpsocioeconomicstatus "
                + "(always 0 in current data - resolves empty, expected)", ENTITY_TYPE, get_TrxName(),
                this::addLog);
        AddColumnsSupport.addColumn(getCtx(), table, "SMS_ID", DisplayType.Integer, 10,
                "aet.smsid (unresolved - no lookup table found under any name searched)",
                ENTITY_TYPE, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with Value/Name/id plus 6 extra columns.";
    }
}
