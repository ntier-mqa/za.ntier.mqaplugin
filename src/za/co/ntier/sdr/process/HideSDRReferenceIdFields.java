package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * One-off fix: {@link CreateSDRReferenceWindows} used {@code TabCreateFields} to auto-generate every
 * field on each of the 72 reference-table tabs, which includes the internal "id" recon column (the
 * original mssdr_* row id, meant for migration bookkeeping only, not for a business user to see) - per
 * user feedback 2026-09-14, hides that field on every tab already created, by setting
 * IsDisplayed/IsDisplayedGrid to 'N'.
 *
 * <p>Reuses {@link CreateSDRReferenceWindows#TABLE_NAMES} rather than duplicating the 72-table list.
 * Idempotent: skips any field already hidden.
 */
@Process(name = "za.co.ntier.sdr.process.HideSDRReferenceIdFields")
public class HideSDRReferenceIdFields extends SvrProcess {

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int hidden = 0;
        int alreadyHidden = 0;
        int skippedNoTab = 0;

        for (String tableName : CreateSDRReferenceWindows.TABLE_NAMES) {
            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            if (table == null) {
                throw new IllegalStateException(tableName + " does not exist.");
            }

            int tabId = DB.getSQLValue(get_TrxName(), "SELECT AD_Tab_ID FROM AD_Tab WHERE AD_Table_ID = ?",
                    table.getAD_Table_ID());
            if (tabId <= 0) {
                addLog(tableName + ": no tab found - skipped (run CreateSDRReferenceWindows first).");
                skippedNoTab++;
                continue;
            }

            MColumn idColumn = table.getColumn("id");
            if (idColumn == null) {
                throw new IllegalStateException(tableName + " has no 'id' column.");
            }

            MTab tab = new MTab(getCtx(), tabId, get_TrxName());
            MField field = null;
            for (MField f : tab.getFields(false, null)) {
                if (f.getAD_Column_ID() == idColumn.getAD_Column_ID()) {
                    field = f;
                    break;
                }
            }
            if (field == null) {
                throw new IllegalStateException(tableName + ": no field found for 'id' column on AD_Tab_ID="
                        + tabId);
            }

            if (!field.isDisplayed() && !field.isDisplayedGrid()) {
                alreadyHidden++;
                continue;
            }

            field.setIsDisplayed(false);
            field.setIsDisplayedGrid(false);
            field.saveEx();
            hidden++;
        }

        return "SDR reference 'id' fields: hidden " + hidden + ", already hidden " + alreadyHidden
                + ", skipped (no tab) " + skippedNoTab + ".";
    }
}
