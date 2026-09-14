package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MWindow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * One-off/repeatable fix: re-syncs every SDR_-prefixed field's display name (and its AD_Field_Trl
 * rows) across all 7 already-built family windows, using {@link WindowCreationSupport#renameSdrFields}'s
 * CURRENT naming algorithm. Written for the 2026-09-14 correction that strips a trailing " ID" from
 * every field's on-screen label (see {@link WindowCreationSupport#improveFieldName}'s Javadoc) - that
 * change alone wouldn't affect the 7 windows already built before it, since their fields already had a
 * (now-stale) name stored. Safe to re-run any time {@code improveFieldName}'s algorithm changes again:
 * {@code renameSdrFields} only writes when the computed name actually differs from what's stored.
 *
 * <p>Supersedes the narrower {@link FixSDRPersonWindowFieldNames} (Person only, written before this
 * general version existed) - that class is left in place but this one covers the same window too.
 */
@Process(name = "za.co.ntier.sdr.process.FixSDRWindowFieldNames")
public class FixSDRWindowFieldNames extends SvrProcess {

    private static final String[] WINDOW_NAMES = {
            "SDR Person", "SDR Organisation", "SDR SDF", "SDR WSPATR", "SDR Levy", "SDR Grant",
            "SDR User Security",
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int windowsFixed = 0;
        int windowsSkipped = 0;
        int tabsFixed = 0;

        for (String windowName : WINDOW_NAMES) {
            int windowId = DB.getSQLValue(get_TrxName(), "SELECT AD_Window_ID FROM AD_Window WHERE Name = ?",
                    windowName);
            if (windowId <= 0) {
                addLog(windowName + ": window not found - skipped.");
                windowsSkipped++;
                continue;
            }

            MWindow window = new MWindow(getCtx(), windowId, get_TrxName());
            int tabCount = 0;
            for (MTab tab : window.getTabs(false, get_TrxName())) {
                WindowCreationSupport.renameSdrFields(tab, get_TrxName());
                tabCount++;
            }

            addLog(windowName + ": updated field names across " + tabCount + " tab(s).");
            tabsFixed += tabCount;
            windowsFixed++;
        }

        return "SDR window field names: fixed " + windowsFixed + " window(s), " + tabsFixed + " tab(s) total, "
                + windowsSkipped + " window(s) not found.";
    }
}
