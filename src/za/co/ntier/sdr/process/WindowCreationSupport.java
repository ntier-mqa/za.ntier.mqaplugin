package za.co.ntier.sdr.process;

import java.util.Properties;
import java.util.function.Consumer;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MMenu;
import org.compiere.model.MPInstance;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MTree_Base;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.SystemIDs;
import org.compiere.process.ProcessInfo;
import org.compiere.process.TabCreateFields;
import org.compiere.util.DB;
import org.compiere.util.Trx;

/**
 * Shared window/menu-tree plumbing for {@link CreateSDRReferenceWindows} and the per-family
 * "CreateSDR*Window" classes - mirrors {@link za.co.ntier.learner.process.AddColumnsSupport}'s and
 * {@link SDRMigrationSupport}'s shape: the truly generic AD-object mechanics live here once, while
 * each caller supplies its own table/tab list, matching this project's established pattern of thin
 * per-family driver classes over a shared primitives helper. Every method here takes {@code ctx}
 * explicitly (never a global context accessor), matching {@code AddColumnsSupport}'s own convention.
 *
 * <p>CLIENT SCOPING: every AD object this class creates is hardcoded to AD_Client_ID=0/System, for the
 * exact reason documented on {@link CreateSDRReferenceWindows}'s own class Javadoc - a new menu's tree
 * node is auto-inserted (by {@code MMenu.afterSave()}) into whichever AD_Tree row matches THAT
 * RECORD's own client, and "Legacy SDR data" (the pre-existing top-level menu everything here nests
 * under) lives in the System client's own menu tree. Creating these records under any other client
 * would auto-insert their tree nodes into a different tree entirely, making correct nesting under
 * "Legacy SDR data" impossible regardless of how Parent_ID is set afterward.
 */
final class WindowCreationSupport {

    private WindowCreationSupport() {
    }

    /**
     * Finds an existing top-level menu by name (must already exist - e.g. "Legacy SDR data", itself
     * manually created), or a summary/folder menu already filed as its child by name (e.g.
     * "Reference"/"Data"), creating the latter if missing. Returns the folder's own AD_Menu_ID either
     * way, for use as a {@code parentMenuId} by callers doing per-window menu attachment.
     */
    static int findOrCreateSummaryMenu(Properties ctx, String parentMenuName, String folderMenuName,
            String entityType, String trxName, Consumer<String> logger) {
        int parentMenuId = DB.getSQLValueEx(trxName, "SELECT AD_Menu_ID FROM AD_Menu WHERE Name=?", parentMenuName);
        if (parentMenuId <= 0) {
            throw new AdempiereException("Expected menu '" + parentMenuName + "' not found - it must already exist.");
        }

        int existing = DB.getSQLValue(trxName,
                "SELECT n.Node_ID FROM AD_TreeNodeMM n JOIN AD_Menu m ON m.AD_Menu_ID = n.Node_ID "
                        + "WHERE n.AD_Tree_ID = ? AND n.Parent_ID = ? AND m.Name = ?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId, folderMenuName);
        if (existing > 0) {
            logger.accept("Using existing '" + folderMenuName + "' menu (AD_Menu_ID=" + existing + ").");
            return existing;
        }

        MMenu menu = new MMenu(ctx, 0, trxName);
        menu.set_ValueOfColumn("AD_Client_ID", 0);
        menu.setAD_Org_ID(0);
        menu.setName(folderMenuName);
        menu.setEntityType(entityType);
        menu.setIsSOTrx(false);
        menu.setIsSummary(true);
        menu.saveEx();

        attachMenuToTree(ctx, menu.getAD_Menu_ID(), parentMenuId, trxName);

        logger.accept("Created '" + folderMenuName + "' menu (AD_Menu_ID=" + menu.getAD_Menu_ID() + ") under '"
                + parentMenuName + "'.");
        return menu.getAD_Menu_ID();
    }

    /**
     * Reparents an already-saved MMenu's auto-created tree node under {@code parentMenuId}, appending
     * it after any existing children (matching {@link RegisterSDRProcesses}'s own SeqNo convention).
     * The menu must have been created at AD_Client_ID=0 (see class Javadoc) - its tree node is looked
     * up in {@link SystemIDs#TREE_MENUPRIMARY}, the System client's own menu tree, which is where
     * "Legacy SDR data" (and everything filed under it) lives.
     */
    static void attachMenuToTree(Properties ctx, int menuId, int parentMenuId, String trxName) {
        MTree_Base menuTree = new MTree_Base(ctx, SystemIDs.TREE_MENUPRIMARY, trxName);
        MTree_NodeMM node = MTree_NodeMM.get(menuTree, menuId);
        if (node == null) {
            throw new AdempiereException(
                    "AD_TreeNodeMM not auto-created by MMenu.afterSave() for AD_Menu_ID=" + menuId);
        }
        int seqNo = DB.getSQLValueEx(trxName,
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId);
        node.setParent_ID(parentMenuId);
        node.setSeqNo(seqNo);
        node.saveEx();
    }

    /**
     * Reuses the exact {@link TabCreateFields} invocation core's own {@code CreateWindowFromTable}
     * uses - generates one MField per MColumn on the tab's table (including the standard audit
     * columns), matching the platform's normal "Create Fields" behavior rather than reimplementing
     * field generation.
     */
    static void createFields(Properties ctx, MTab tab, String trxName) {
        ProcessInfo processInfo = new ProcessInfo("", SystemIDs.PROCESS_AD_TAB_CREATEFIELDS, MTab.Table_ID,
                tab.getAD_Tab_ID(), tab.getAD_Tab_UU());

        MPInstance instance = new MPInstance(ctx, SystemIDs.PROCESS_AD_TAB_CREATEFIELDS, MTab.Table_ID,
                tab.getAD_Tab_ID(), tab.getAD_Tab_UU());
        instance.saveEx();
        processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());

        TabCreateFields createFields = new TabCreateFields();
        boolean success = createFields.startProcess(ctx, processInfo, Trx.get(trxName, false));
        if (!success) {
            throw new AdempiereException("TabCreateFields failed for AD_Tab_ID=" + tab.getAD_Tab_ID() + ": "
                    + processInfo.getSummary());
        }
    }

    /**
     * Hides the "id" recon column's auto-generated field on the given tab - per user feedback
     * 2026-09-14 (originally applied retroactively to the 72 reference-table tabs via
     * {@code HideSDRReferenceIdFields}), every SDR_ table's "id" column is the original mssdr_* row id,
     * meant for migration bookkeeping only, never for a business user to see. Call this right after
     * {@link #createFields} for every family-window tab going forward, rather than needing a
     * retroactive fix each time.
     */
    static void hideIdField(MTab tab, MTable table, String trxName) {
        MColumn idColumn = table.getColumn("id");
        if (idColumn == null) {
            throw new IllegalStateException(table.getTableName() + " has no 'id' column.");
        }

        MField field = null;
        for (MField f : tab.getFields(true, trxName)) {
            if (f.getAD_Column_ID() == idColumn.getAD_Column_ID()) {
                field = f;
                break;
            }
        }
        if (field == null) {
            throw new IllegalStateException(
                    table.getTableName() + ": no field found for 'id' column on AD_Tab_ID=" + tab.getAD_Tab_ID());
        }

        field.setIsDisplayed(false);
        field.setIsDisplayedGrid(false);
        field.saveEx();
    }
}
