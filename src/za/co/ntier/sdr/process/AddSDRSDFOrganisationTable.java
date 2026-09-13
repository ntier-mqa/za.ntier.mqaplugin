package za.co.ntier.sdr.process;

import static org.compiere.model.SystemIDs.REFERENCE_AD_USER;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 4 (see "Phase 4 - SDF Family - Mapping.txt"): creates the brand new SDR_SDFOrganisation child
 * table (4,808 source rows) - the link between an SDF and the organisation(s) they're appointed at.
 *
 * <p>SDR_ApprovedBy_ID/SDR_RejectedBy_ID follow the same audit-trail pattern as CreatedBy/UpdatedBy elsewhere
 * in this migration (DisplayType.Search + AD_Reference_Value_ID=REFERENCE_AD_USER, resolving against
 * iDempiere's own AD_User table - there is no separate "SDR_User" table). RejectedBy is CHECKED 0%
 * resolved to a real user in the mapping doc's sample, but kept as the same lookup shape regardless -
 * per the doc, this is almost certainly just a rarely-populated column (approvals vastly outnumber
 * rejections), not a broken crosswalk, so it will simply resolve to null for nearly every row.
 *
 * <p>SDR_AppointmentProcedure_ID does not match its target table by name ("SDR_AppointmentProcedure" !=
 * "SDR_SDFAppointmentProcedure") - needs an explicit AD_Reference/AD_Ref_Table override.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRSDFOrganisationTable")
public class AddSDRSDFOrganisationTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_SDFOrganisation";
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
                "An SDF's appointment at an organisation (mssdr_sdforganisation)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDF_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisation.sdfid -> SDR_SDF. CONFIRMED 99.75% match (4,796/4,808) - small, "
                + "real gap not investigated further given the size", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisation.organisationid -> SDR_Organisation. CONFIRMED 100% match (4,808/4,808)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_StartDate", DisplayType.DateTime, 7,
                "mssdr_sdforganisation.startdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EndDate", DisplayType.DateTime, 7,
                "mssdr_sdforganisation.enddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDFStatus_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisation.sdfstatusid -> SDR_SDFStatus. CONFIRMED 100% match (4,808/4,808)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDFRole_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisation.sdfroleid -> SDR_SDFRole. CONFIRMED 100% match (4,808/4,808)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateApproved", DisplayType.DateTime, 7,
                "mssdr_sdforganisation.dateapproved", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ApprovedBy_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_sdforganisation.approvedby -> AD_User (audit-trail pattern, same as CreatedBy/"
                + "UpdatedBy). CONFIRMED 77.0% of rows with either ApprovedBy or RejectedBy populated "
                + "resolve to a real user", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DateRejected", DisplayType.DateTime, 7,
                "mssdr_sdforganisation.daterejected", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_RejectedBy_ID", DisplayType.Search,
                REFERENCE_AD_USER, 10,
                "mssdr_sdforganisation.rejectedby -> AD_User. CHECKED 0% resolved in the sample - almost "
                + "certainly just rarely populated (rejections are a small minority of this workflow), "
                + "not a broken crosswalk", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ActingForEmployer", DisplayType.Integer, 10,
                "mssdr_sdforganisation.actingforemployer (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDFFunction_ID", DisplayType.TableDir, 10,
                "mssdr_sdforganisation.sdffunctionid -> SDR_SDFFunction. CONFIRMED 74.8% match "
                + "(3,598/4,808) - nullable in source, gap plausibly just unanswered records", ENTITY_TYPE,
                get_TrxName());

        int appointmentProcedureRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(),
                "SDR_SDFAppointmentProcedure", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_AppointmentProcedure_ID", DisplayType.Table,
                appointmentProcedureRefId, 10,
                "mssdr_sdforganisation.appointmentprocedureid -> SDR_SDFAppointmentProcedure. CONFIRMED all "
                + "populated non-zero values (1-4, 3,598 rows) fall within the lookup's 4-row range; the "
                + "remaining 1,210 rows are the 0 sentinel", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AppointmentProcedureOther", DisplayType.String, 250,
                "mssdr_sdforganisation.appointmentprocedureother (free text, populated when "
                + "AppointmentProcedure = Other)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ReplacingPrimarySDF", DisplayType.Integer, 10,
                "mssdr_sdforganisation.replacingprimarysdf (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SecondarySDF", DisplayType.Integer, 10,
                "mssdr_sdforganisation.secondarysdf (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_PreviousSDF", DisplayType.String, 250,
                "mssdr_sdforganisation.previoussdf (free text)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 17 business columns.";
    }
}
