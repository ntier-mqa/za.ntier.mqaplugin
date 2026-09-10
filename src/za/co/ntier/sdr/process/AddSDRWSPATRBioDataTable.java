package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRBioData child
 * table - the SECOND-LARGEST table in this migration (1,949,395 source rows).
 *
 * <p>Several notable per-column resolutions, all confirmed in the mapping doc:
 * <ul>
 *   <li>SDR_Race_ID -> SDR_Equity (reuses the same 4-value Equity list built for Person, not a
 *       separate "Race" catalog)</li>
 *   <li>SDR_Province_ID -> the SHARED SDR_Province (not the WSP-specific SDR_WSPProvince, even though
 *       both match 100%) - CONFIRMED 2026-09-04 (user decision)</li>
 *   <li>SDR_Municipality_ID -> the WSP-SPECIFIC SDR_WSPMunicipality (NOT the shared SDR_Municipality -
 *       CONFIRMED more complete here: 100% vs 98.3%) - deliberately asymmetric with Province above,
 *       optimized per-column for completeness rather than forced consistency</li>
 *   <li>SDR_EmpStatus_ID -> SDR_WSPAppointment, a user-supplied candidate the automated search missed
 *       (its name doesn't suggest "employment status") - CONFIRMED 99.995% match</li>
 * </ul>
 * None of the FK columns except SDR_Gender_ID, SDR_Province_ID and SDR_LearningProgramme_ID match their
 * target table by name - every other one needs an explicit AD_Reference/AD_Ref_Table override.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRBioDataTable")
public class AddSDRWSPATRBioDataTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRBioData";
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
                "A biographical/demographic record tied to a WSPATR (mssdr_wspatrbiodata) - the "
                + "second-largest table in this migration", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrbiodata.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATRImport_ID", DisplayType.Integer, 10,
                "mssdr_wspatrbiodata.wspatrimportid - UNMAPPED, opaque", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EmployeeNo", DisplayType.String, 50,
                "mssdr_wspatrbiodata.employeeno", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EmployeeName", DisplayType.String, 250,
                "mssdr_wspatrbiodata.employeename", ENTITY_TYPE, get_TrxName());

        int yearRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Year", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_BirthYear_ID", DisplayType.Table, yearRefId,
                10, "mssdr_wspatrbiodata.birthyearid -> SDR_Year (shared). CONFIRMED 100%", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Gender_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrbiodata.genderid -> SDR_Gender (shared with Person)", ENTITY_TYPE, get_TrxName());

        int equityRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_Equity", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_Race_ID", DisplayType.Table, equityRefId,
                10, "mssdr_wspatrbiodata.raceid -> SDR_Equity. CONFIRMED 100% match (1,949,395/1,949,395) - "
                + "reuses the same 4-value Equity list already built for Person, not a separate 'Race' "
                + "catalog", ENTITY_TYPE, get_TrxName());

        int yesNoRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_YesNo", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_Disabled_ID", DisplayType.Table, yesNoRefId,
                10, "mssdr_wspatrbiodata.disabledid -> SDR_YesNo (shared '*YesNoID' convention, even though "
                + "named 'DisabledID'). CONFIRMED 100% match", ENTITY_TYPE, get_TrxName());

        int hasSouthAfricanRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_HasSouthAfrican",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_SACitizen_ID", DisplayType.Table,
                hasSouthAfricanRefId, 10,
                "mssdr_wspatrbiodata.sacitizenid -> SDR_HasSouthAfrican (shared with Person). CONFIRMED 100%",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Province_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrbiodata.provinceid -> SDR_Province (shared). CONFIRMED 100% - a WSP-specific "
                + "9-row alternative (SDR_WSPProvince) ALSO matches 100%; CONFIRMED 2026-09-04 (user "
                + "decision): use the shared catalog, consistent with the rest of the platform", ENTITY_TYPE,
                get_TrxName());

        int wspMunicipalityRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPMunicipality",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_Municipality_ID", DisplayType.Table,
                wspMunicipalityRefId, 10,
                "mssdr_wspatrbiodata.municipalityid -> SDR_WSPMunicipality (WSP-specific 287-row catalog, "
                + "NOT the shared 252-row SDR_Municipality). CONFIRMED 2026-09-04: SDR_WSPMunicipality "
                + "matches 100% while the shared catalog only matches 98.3% - use the more complete one, "
                + "deliberately asymmetric with Province above", ENTITY_TYPE, get_TrxName());

        int wspQualTypeRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPQualificationType",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_HighestQualType_ID", DisplayType.Table,
                wspQualTypeRefId, 10,
                "mssdr_wspatrbiodata.highestqualtypeid -> SDR_WSPQualificationType (27 rows). CONFIRMED 100%",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_LearningProgramme_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrbiodata.learningprogrammeid -> SDR_LearningProgramme (nullable, not "
                + "independently re-tested on this table)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Qualification", DisplayType.String, 250,
                "mssdr_wspatrbiodata.qualification (free text, separate from the lookup-backed field above)",
                ENTITY_TYPE, get_TrxName());

        int wspAppointmentRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPAppointment",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_EmpStatus_ID", DisplayType.Table,
                wspAppointmentRefId, 10,
                "mssdr_wspatrbiodata.empstatusid -> SDR_WSPAppointment (8 rows). RESOLVED 2026-09-04 "
                + "(user-supplied candidate, verified before accepting): CONFIRMED 99.995% match "
                + "(1,949,295/1,949,395) - the automated search missed this table since its name doesn't "
                + "obviously suggest 'employment status'", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_EmpStartYear_ID", DisplayType.Table,
                yearRefId, 10, "mssdr_wspatrbiodata.empstartyearid -> SDR_Year (shared). CONFIRMED 100%",
                ENTITY_TYPE, get_TrxName());

        int managementEquityRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPManagementEquity",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ManagementEquity_ID", DisplayType.Table,
                managementEquityRefId, 10,
                "mssdr_wspatrbiodata.managementequityid -> SDR_WSPManagementEquity (6 rows). CONFIRMED 100%",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OrgStructure", DisplayType.String, 250,
                "mssdr_wspatrbiodata.orgstructure", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PostRef", DisplayType.String, 250,
                "mssdr_wspatrbiodata.postref", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_JobTitle", DisplayType.String, 250,
                "mssdr_wspatrbiodata.jobtitle", ENTITY_TYPE, get_TrxName());

        int ofoSpecialisationRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_OFOSpecialization",
                ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_OFOSpecialisation_ID", DisplayType.Table,
                ofoSpecialisationRefId, 10,
                "mssdr_wspatrbiodata.ofospecialisationid -> SDR_OFOSpecialization (shared - note the target "
                + "table uses the US spelling 'Specialization' while this column keeps the source's British "
                + "'Specialisation'). CONFIRMED 100% on this table", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 21 business columns.";
    }
}
