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
 * SDR_WSPATREvaluationVerificationChecklist child table (54,281 source rows).
 *
 * <p>SDR_WSPATRForm_ID does not match its target table by name ("SDR_WSPATRForm" != the plural
 * "SDR_WSPATRForms") - needs an explicit override; currently always 0 across all rows, genuinely
 * unpopulated in this data (not a mismatch), built as a LOOKUP ready for when the source populates it.
 * Requires {@link AddSDRWSPATRFormsTable} to have already run. SDR_WSPATRForms has no generic "Name"
 * column (it uses SDR_FormName instead, same shape as SDR_Person/SDR_Organisation/SDR_SDF) - CONFIRMED
 * 2026-09-05 the hard way (same "no 'Name' column" error hit before) - SDR_FormName (100% populated) is
 * used as the display column instead.
 *
 * <p>SDR_Grant_ID is CONFIRMED always NULL across all rows and its exact target table (GrantType vs
 * GrantAccount, both in the not-yet-built Grant family) is still unconfirmed - CONFIRMED 2026-09-04
 * (user decision): carried as a plain integer for now rather than guessing a target; can be upgraded
 * once the Grant family (Phase 7) is built and the correct target is confirmed against real data, the
 * same pattern as {@link AddColumnsSupport#upgradeColumnToTableReference}.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATREvaluationVerificationChecklistTable")
public class AddSDRWSPATREvaluationVerificationChecklistTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATREvaluationVerificationChecklist";
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
                "A checklist item on a WSPATR's evaluation/verification "
                + "(mssdr_wspatrevaluationverificationchecklist)", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATR_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverificationchecklist.wspatrid -> SDR_WSPATR", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATREvaluationVerificationChecklistType_ID",
                DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverificationchecklist.wspatrevaluationverificationchecklisttypeid -> "
                + "SDR_WSPATREvaluationVerificationChecklistType (68 rows). CONFIRMED 100%", ENTITY_TYPE,
                get_TrxName());

        int wspatrFormRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_WSPATRForms",
                "SDR_FormName", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_WSPATRForm_ID", DisplayType.Table,
                wspatrFormRefId, 10,
                "mssdr_wspatrevaluationverificationchecklist.wspatrformid -> SDR_WSPATRForms. CHECKED: "
                + "currently always 0 across all 54,281 rows - genuinely unpopulated in this data (not a "
                + "mismatch), kept as a LOOKUP ready for when the source starts populating it", ENTITY_TYPE,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Grant_ID", DisplayType.Integer, 10,
                "mssdr_wspatrevaluationverificationchecklist.grantid - CHECKED: always NULL across all "
                + "54,281 rows, exact target (SDR_GrantType vs SDR_GrantAccount, both in the not-yet-built "
                + "Grant family) still unconfirmed. CONFIRMED 2026-09-04 (user decision): carried as a plain "
                + "integer for now, ready to be upgraded once Phase 7 (Grant) confirms the right target",
                ENTITY_TYPE, get_TrxName());

        int yesNoRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_YesNo", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_ConfirmInformationYesNo_ID",
                DisplayType.Table, yesNoRefId, 10,
                "mssdr_wspatrevaluationverificationchecklist.confirminformationyesnoid -> SDR_YesNo. "
                + "CONFIRMED 88.3% (47,926/54,281)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_InformationOutstandingYesNo_ID",
                DisplayType.Table, yesNoRefId, 10,
                "mssdr_wspatrevaluationverificationchecklist.informationoutstandingyesnoid -> SDR_YesNo. "
                + "CONFIRMED 88.1% (47,837/54,281)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Comment", DisplayType.String, 2000,
                "mssdr_wspatrevaluationverificationchecklist.comment (free text)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_WSPATREvaluationVerificationDeviation_ID",
                DisplayType.TableDir, 10,
                "mssdr_wspatrevaluationverificationchecklist.wspatrevaluationverificationdeviationid -> "
                + "SDR_WSPATREvaluationVerificationDeviation (5 rows). CHECKED: currently always 0 across "
                + "all 54,281 rows, same as WSPATRForm_ID - genuinely unpopulated, not a mismatch", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_OriginalFileName", DisplayType.String, 250,
                "mssdr_wspatrevaluationverificationchecklist.originalfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SavedFileName", DisplayType.String, 250,
                "mssdr_wspatrevaluationverificationchecklist.savedfilename", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FilePath", DisplayType.String, 250,
                "mssdr_wspatrevaluationverificationchecklist.filepath (metadata only, not a working file "
                + "path from here)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 11 business columns.";
    }
}
