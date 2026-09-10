package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRTopUpSkills
 * child table (6,858 source rows).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRTopUpSkillsTable")
public class AddSDRWSPATRTopUpSkillsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRTopUpSkills";
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
                "A top-up skills record on a WSPATR (mssdr_wspatrtopupskills)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrtopupskills.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrtopupskills.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());

        int ofoSpecialisationRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_OFOSpecialization",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_OFOSpecialisation_ID", DisplayType.Table,
                ofoSpecialisationRefId, 10,
                "mssdr_wspatrtopupskills.ofospecialisationid -> SDR_OFOSpecialization (shared). CONFIRMED "
                + "100%", ENTITY_TYPE, get_TrxName());

        int topUpSkillRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPTopUpSkills",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_TopUpSkill_ID", DisplayType.Table,
                topUpSkillRefId, 10,
                "mssdr_wspatrtopupskills.topupskillid -> SDR_WSPTopUpSkills (31 rows). CONFIRMED 100% "
                + "(6,858/6,858)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Comments", DisplayType.String, 2000,
                "mssdr_wspatrtopupskills.comments (free text)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 5 business columns.";
    }
}
