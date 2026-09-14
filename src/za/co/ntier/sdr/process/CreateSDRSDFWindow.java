package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MColumn;
import org.compiere.model.MMenu;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Creates the "SDR SDF" window (4 tabs: main + 2 direct children + 1 grandchild), filed under the
 * "Data" summary menu beneath "Legacy SDR data". See {@link WindowCreationSupport}'s Javadoc for the
 * AD_Client_ID=0/System scoping rationale.
 *
 * <p>SDR_SDF's own real parent is SDR_Person (cross-family) - a lookup/zoom field on the SDF tab
 * itself, not a nested tab (mirrors how SDR_WSPATR's real parent is Organisation without a WSPATR tab
 * nesting under the Organisation window).
 *
 * <p>SDR_SDFOrganisation also carries SDR_Organisation_ID (a secondary/redundant field, not the tab
 * link) alongside its primary SDR_SDF_ID parent link.
 *
 * <p>Idempotent: skips if a window named "SDR SDF" already exists.
 */
@Process(name = "za.co.ntier.sdr.process.CreateSDRSDFWindow")
public class CreateSDRSDFWindow extends SvrProcess {

    private static final String ENTITY_TYPE = "U";
    private static final String PARENT_MENU_NAME = "Legacy SDR data";
    private static final String DATA_MENU_NAME = "Data";
    private static final String WINDOW_NAME = "SDR SDF";

    /** {tableName, tabLevel, linkColumnName (the child's own FK column back to its parent tab, null at level 0)}. */
    private static final String[][] TAB_SPECS = {
            {"SDR_SDF", "0", null},
            {"SDR_SDFCertificateDocument", "1", "SDR_SDF_ID"},
            {"SDR_SDFOrganisation", "1", "SDR_SDF_ID"},
            {"SDR_SDFOrganisationDocumentUpload", "2", "SDR_SDFOrganisation_ID"},
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int existingWindow = DB.getSQLValue(get_TrxName(), "SELECT 1 FROM AD_Window WHERE Name = ?", WINDOW_NAME);
        if (existingWindow == 1) {
            return WINDOW_NAME + " already exists - no action taken.";
        }

        int dataMenuId = WindowCreationSupport.findOrCreateSummaryMenu(getCtx(), PARENT_MENU_NAME, DATA_MENU_NAME,
                ENTITY_TYPE, get_TrxName(), this::addLog);

        MWindow window = new MWindow(getCtx(), 0, get_TrxName());
        window.set_ValueOfColumn("AD_Client_ID", 0);
        window.setAD_Org_ID(0);
        window.setName(WINDOW_NAME);
        window.setIsSOTrx(false);
        window.setWindowType(MWindow.WINDOWTYPE_QueryOnly);
        window.setEntityType(ENTITY_TYPE);
        window.saveEx();

        int seqNo = 10;
        for (String[] spec : TAB_SPECS) {
            String tableName = spec[0];
            int tabLevel = Integer.parseInt(spec[1]);
            String linkColumnName = spec[2];

            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            if (table == null) {
                throw new IllegalStateException(tableName + " does not exist.");
            }

            MTab tab = new MTab(window);
            tab.set_ValueOfColumn("AD_Client_ID", 0);
            tab.setAD_Org_ID(0);
            tab.setEntityType(ENTITY_TYPE);
            tab.setSeqNo(seqNo);
            tab.setName(table.getName());
            tab.setAD_Table_ID(table.getAD_Table_ID());
            tab.setTabLevel(tabLevel);
            tab.setIsSingleRow(false);

            if (tabLevel > 0) {
                MColumn linkColumn = table.getColumn(linkColumnName);
                if (linkColumn == null) {
                    throw new IllegalStateException(tableName + " has no '" + linkColumnName + "' column.");
                }
                tab.setAD_Column_ID(linkColumn.getAD_Column_ID());
            }

            tab.setOrderByClause(table.getTableName() + ".Created");
            tab.saveEx();

            WindowCreationSupport.createFields(getCtx(), tab, get_TrxName());
            WindowCreationSupport.hideIdField(tab, table, get_TrxName());
            WindowCreationSupport.renameSdrFields(tab, get_TrxName());

            addLog(tableName + ": created AD_Tab_ID=" + tab.getAD_Tab_ID() + " (TabLevel=" + tabLevel + ")");
            seqNo += 10;
        }

        MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
        menu.set_ValueOfColumn("AD_Client_ID", 0);
        menu.setAD_Org_ID(0);
        menu.setName(WINDOW_NAME);
        menu.setEntityType(ENTITY_TYPE);
        menu.setIsSOTrx(false);
        menu.setAction(MMenu.ACTION_Window);
        menu.setAD_Window_ID(window.getAD_Window_ID());
        menu.saveEx();

        WindowCreationSupport.attachMenuToTree(getCtx(), menu.getAD_Menu_ID(), dataMenuId, get_TrxName());

        return WINDOW_NAME + ": created AD_Window_ID=" + window.getAD_Window_ID() + " with " + TAB_SPECS.length
                + " tabs, AD_Menu_ID=" + menu.getAD_Menu_ID() + ".";
    }
}
