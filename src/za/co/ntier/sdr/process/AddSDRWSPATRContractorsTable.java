package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRContractors
 * child table (12,384 source rows). All *Trained/*Planned columns are plain integer headcounts, no
 * lookups - the source has a TotalTrained column but no TotalPlanned counterpart (an asymmetry in the
 * original schema, not a migration gap).
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRContractorsTable")
public class AddSDRWSPATRContractorsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRContractors";
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
                "Contractor training headcounts for a WSPATR (mssdr_wspatrcontractors)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrcontractors.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgrammeType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrcontractors.learningprogrammetypeid -> SDR_LearningProgrammeType", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgramme_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrcontractors.learningprogrammeid -> SDR_LearningProgramme", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ManagersTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.managerstrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProfessionalsTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.professionalstrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TechnicianTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.technicianstrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ClericalTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.clericaltrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ServiceTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.servicetrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SkilledWorkersTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.skilledworkerstrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PlantTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.planttrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ElementaryTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.elementarytrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearnersTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.learnerstrained (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TotalTrained", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.totaltrained (headcount) - no TotalPlanned counterpart exists in "
                + "the source (asymmetry in the original schema, not a migration gap)", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ManagersPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.managersplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ProfessionalsPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.professionalsplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_TechniciansPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.techniciansplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ClericalPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.clericalplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ServicePlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.serviceplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SkilledWorkersPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.skilledworkersplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PlantPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.plantplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ElementaryPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.elementaryplanned (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearnersPlanned", DisplayType.Integer, 10,
                "mssdr_wspatrcontractors.learnersplanned (headcount)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 23 business columns.";
    }
}
