package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new
 * SDR_OrganisationDocuments child table (15,927 source rows).
 *
 * <p>SDR_DocumentRelates_ID was CHECKED against SDR_WSPATRDocumentRelates (mssdr_lkpwspatrdocumentrelates)
 * and found NOT to be the right table (only 9.6% match, id range doesn't even cover the data) -
 * CONFIRMED 2026-09-04 (user decision): left unresolved, carried as a plain integer, not investigated
 * further.
 *
 * <p>SDR_WSPATR_ID targets SDR_WSPATR, which does not exist yet (WSPATR is Phase 5, not yet built) -
 * the crosswalk itself is CONFIRMED 100% match (3,690/3,690 among non-null values), but the physical
 * Table reference can't be wired until SDR_WSPATR exists. Carried as a plain integer for now - upgrade
 * once Phase 5 is built, same as the SDF_ID columns on the banking-details tables.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationDocumentsTable")
public class AddSDROrganisationDocumentsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationDocuments";
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
                "A document attached to an organisation (mssdr_organisationdocuments) - metadata only, "
                + "no file retrieval", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationdocuments.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_DocumentRelates_ID", DisplayType.Integer, 10,
                "mssdr_organisationdocuments.documentrelatesid - CHECKED against SDR_WSPATRDocumentRelates: "
                + "only 9.6% match, not the right table. CONFIRMED 2026-09-04 (user decision): left "
                + "unresolved, carried as a plain integer", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Comment", DisplayType.String, 2000,
                "mssdr_organisationdocuments.comment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_organisationdocuments.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_organisationdocuments.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_organisationdocuments.filepath (metadata only, not a working file path from here)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.Integer, 10,
                "mssdr_organisationdocuments.wspatrid -> SDR_WSPATR (not yet built, Phase 5) - crosswalk "
                + "CONFIRMED 100% match (3,690/3,690 of non-null values) but carried as a plain integer "
                + "until SDR_WSPATR exists and this column can be upgraded to a proper Table reference",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 7 business columns.";
    }
}
