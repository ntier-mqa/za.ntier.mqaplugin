package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 4 (see "Phase 4 - SDF Family - Mapping.txt"): creates the brand new SDR_SDF MAIN table (2,452
 * source rows). SDR_Person_ID and SDR_SDFHighestEducation_ID both match their target tables by name -
 * plain DisplayType.TableDir.
 *
 * <p>SDR_OccupationalGroup_ID is UNMAPPED - CHECKED: every row is either NULL or the "not set" sentinel
 * 0, zero real distinct values anywhere in the source column. Carried as a plain integer for schema
 * completeness only, not wired as a lookup.
 *
 * <p>SDR_HighestEducationDescription is spelled correctly here (the mapping doc's own prose has a typo,
 * "higheseducationdescription" - corrected to match the source column's actual spelling,
 * highesteducationdescription).
 *
 * <p>Schema only - this class does NOT populate any rows. Data migration (mssdr_sdf -&gt; SDR_SDF, with
 * FK resolution) is a separate Migrate*-style process, not yet written.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRSDFTable")
public class AddSDRSDFTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_SDF";
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
                "A Skills Development Facilitator record (mssdr_sdf), migrated independently of "
                + "SDR_Person despite the 1:1 SDF->Person link", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Person_ID", DisplayType.TableDir, 10,
                "mssdr_sdf.personid -> SDR_Person. CONFIRMED 100% match (2,452/2,452)", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDFHighestEducation_ID", DisplayType.TableDir, 10,
                "mssdr_sdf.sdfhighesteducationid -> SDR_SDFHighestEducation. CONFIRMED 87.3% match "
                + "(2,140/2,452) - nullable in source, gap plausibly just unanswered records", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_HighestEducationDescription", DisplayType.String,
                250, "mssdr_sdf.highesteducationdescription (free text, distinct from the lookup-backed "
                + "SDR_SDFHighestEducation_ID above)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CurrentOccupation", DisplayType.String, 250,
                "mssdr_sdf.currentoccupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_YearsInOccupation", DisplayType.Integer, 10,
                "mssdr_sdf.yearsinoccupation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OccupationalGroup_ID", DisplayType.Integer, 10,
                "mssdr_sdf.occupationalgroupid - UNMAPPED, CHECKED: every row is NULL or the 0 sentinel, "
                + "zero real distinct values in this data - carried as a plain integer for schema "
                + "completeness only", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Experience", DisplayType.String, 2000,
                "mssdr_sdf.experience (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_InterestedInCommunication", DisplayType.Integer, 10,
                "mssdr_sdf.interestedincommunication (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CompletedSDFTraining", DisplayType.Integer, 10,
                "mssdr_sdf.completedsdftraining (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccreditedTrainingProviderName", DisplayType.String,
                250, "mssdr_sdf.accreditedtrainingprovidername", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_GeneralComments", DisplayType.String, 2000,
                "mssdr_sdf.generalcomments (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_CertificateNumber", DisplayType.String, 50,
                "mssdr_sdf.certificatenumber", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 12 business columns.";
    }
}
