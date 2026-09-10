package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRHTFV
 * ("Hard To Fill Vacancy") child table (4,657 source rows). The 9 province-code columns (EC/FS/GP/
 * KZN/LP/MP/NP/NW/WC) are plain integer headcounts, no lookups - the province identity is encoded in
 * the column name itself, not a value.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRHTFVTable")
public class AddSDRWSPATRHTFVTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRHTFV";
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
                "A Hard-To-Fill-Vacancy record on a WSPATR (mssdr_wspatrhtfv)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrhtfv.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());

        int ofoSpecialisationRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_OFOSpecialization",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_OFOSpecialisation_ID", DisplayType.Table,
                ofoSpecialisationRefId, 10,
                "mssdr_wspatrhtfv.ofospecialisationid -> SDR_OFOSpecialization (shared). CONFIRMED 100%",
                ENTITY_TYPE, get_TrxName());

        int scarceReasonRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPScarceReason",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_PrimaryReason_ID", DisplayType.Table,
                scarceReasonRefId, 10,
                "mssdr_wspatrhtfv.primaryreasonid -> SDR_WSPScarceReason (8 rows). CONFIRMED 100%", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_FirstReason_ID", DisplayType.Table,
                scarceReasonRefId, 10,
                "mssdr_wspatrhtfv.firstreasonid -> SDR_WSPScarceReason. CONFIRMED 78.6% (3,661/4,657, "
                + "nullable)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_SecondReason_ID", DisplayType.Table,
                scarceReasonRefId, 10,
                "mssdr_wspatrhtfv.secondreasonid -> SDR_WSPScarceReason. CONFIRMED 54.0% (2,514/4,657, "
                + "nullable)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Comments", DisplayType.String, 2000,
                "mssdr_wspatrhtfv.comments (free text)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EC", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.ec (Eastern Cape headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FS", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.fs (Free State headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GP", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.gp (Gauteng headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_KZN", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.kzn (KwaZulu-Natal headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LP", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.lp (Limpopo headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_MP", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.mp (Mpumalanga headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NP", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.np (Northern Cape headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NW", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.nw (North West headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WC", DisplayType.Integer, 10,
                "mssdr_wspatrhtfv.wc (Western Cape headcount)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 16 business columns.";
    }
}
