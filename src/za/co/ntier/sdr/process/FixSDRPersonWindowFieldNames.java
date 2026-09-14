package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MWindow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * One-off fix: retroactively improves every SDR_-prefixed field's display name on the already-built
 * "SDR Person" window (see {@link WindowCreationSupport#renameSdrFields} for the naming algorithm and
 * why this overrides the field-level name rather than the shared AD_Element). Per user feedback
 * 2026-09-14. {@link CreateSDRPersonWindow} itself was updated to call {@code renameSdrFields}
 * directly, so this one-off class is only needed for the window that was already built before that
 * change; every family window created from here on gets correct names from the start.
 */
@Process(name = "za.co.ntier.sdr.process.FixSDRPersonWindowFieldNames")
public class FixSDRPersonWindowFieldNames extends SvrProcess {

    private static final String WINDOW_NAME = "SDR Person";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int windowId = DB.getSQLValue(get_TrxName(), "SELECT AD_Window_ID FROM AD_Window WHERE Name = ?",
                WINDOW_NAME);
        if (windowId <= 0) {
            throw new IllegalStateException(WINDOW_NAME + " window does not exist - run CreateSDRPersonWindow first.");
        }

        MWindow window = new MWindow(getCtx(), windowId, get_TrxName());
        int tabCount = 0;
        for (MTab tab : window.getTabs(false, get_TrxName())) {
            WindowCreationSupport.renameSdrFields(tab, get_TrxName());
            addLog(tab.getName() + ": field names updated.");
            tabCount++;
        }

        return WINDOW_NAME + ": updated field names across " + tabCount + " tab(s).";
    }
}
