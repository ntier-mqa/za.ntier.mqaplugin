package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new
 * SDR_WSPATRWorkplaceSkillsPlan child table (273,182 source rows). This is the table the mapping doc's
 * headline LearningProgrammeType vs LearningProgramme resolution was originally verified against - both
 * lookups CONFIRMED 100% here.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRWorkplaceSkillsPlanTable")
public class AddSDRWSPATRWorkplaceSkillsPlanTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRWorkplaceSkillsPlan";
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
                "A workplace skills plan line item on a WSPATR (mssdr_wspatrworkplaceskillsplan)", ENTITY_TYPE,
                ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrworkplaceskillsplan.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());

        int ofoSpecialisationRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_OFOSpecialization",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_OFOSpecialisation_ID", DisplayType.Table,
                ofoSpecialisationRefId, 10,
                "mssdr_wspatrworkplaceskillsplan.ofospecialisationid -> SDR_OFOSpecialization (shared). "
                + "CONFIRMED 100%", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgrammeType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrworkplaceskillsplan.learningprogrammetypeid -> SDR_LearningProgrammeType. "
                + "CONFIRMED 100% (273,182/273,182) - the table the headline LearningProgrammeType vs "
                + "LearningProgramme resolution was verified against", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgramme_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrworkplaceskillsplan.learningprogrammeid -> SDR_LearningProgramme. CONFIRMED "
                + "100% of populated non-zero values (116,439/116,439) - the other 156,743 rows are the 0 "
                + "sentinel", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Qualification", DisplayType.String, 250,
                "mssdr_wspatrworkplaceskillsplan.qualification (free text)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Male", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.male (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Female", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.female (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_African", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.african (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Coloured", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.coloured (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Indian", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.indian (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_White", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.white (headcount)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Disabled", DisplayType.Integer, 10,
                "mssdr_wspatrworkplaceskillsplan.disabled (headcount)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 12 business columns.";
    }
}
