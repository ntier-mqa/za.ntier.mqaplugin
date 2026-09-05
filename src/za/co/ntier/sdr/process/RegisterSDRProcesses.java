package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMenu;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTree_Base;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.SystemIDs;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Registration process for this package's SvrProcess classes - same shape as the Learner
 * project's RegisterZZPhase1Processes.
 *
 * <p>CORRECTED 2026-09-05: this class originally created its own top-level "SDR Migration"
 * summary menu (Parent_ID=0). That never actually ran - RegisterSDRProcesses can't register
 * itself (see below), so the user manually created its own AD_Process record AND its own
 * menu placement first, choosing to nest it under a hand-built "Legacy SDR data" (AD_Menu_ID
 * 1000336) &gt; "System Utils" (AD_Menu_ID 1000337) hierarchy instead. Per user instruction
 * 2026-09-05: target that existing "System Utils" menu directly (found by name, verified to
 * be the child of "Legacy SDR data" to avoid latching onto an unrelated same-named menu) -
 * this class no longer creates any menu of its own.
 *
 * <p>AD_Process convention (mirrors RegisterZZPhase1Processes exactly):
 * <ul>
 *   <li>Value = plain class name, truncated to 40 chars</li>
 *   <li>Name = plain class name, in full</li>
 *   <li>Classname = fully-qualified za.co.ntier.sdr.process.&lt;ClassName&gt;</li>
 *   <li>EntityType = "U" (User Maintained), AccessLevel = "6" (System+Client+Org)</li>
 *   <li>AD_Client_ID = 0, AD_Org_ID = 0 (System level)</li>
 * </ul>
 *
 * <p>Idempotent: skips any class name that already has an AD_Process row with a matching
 * Classname, and skips menu creation for any process that already has an AD_Menu row
 * pointing at it - safe to run more than once, and safe to extend {@link #PROCESS_CLASSES}
 * with future SDR process classes and re-run.
 *
 * <p>This process itself must still be registered manually as an AD_Process, the usual way,
 * since it cannot register itself - already done for this class (2026-09-05, under "System
 * Utils"); any brand new SDR process class added later still needs the same manual
 * first-registration if it's meant to run before RegisterSDRProcesses can pick it up.
 */
@Process(name = "za.co.ntier.sdr.process.RegisterSDRProcesses")
public class RegisterSDRProcesses extends SvrProcess {

    private static final String PACKAGE = "za.co.ntier.sdr.process.";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "6";
    private static final String PARENT_MENU_NAME = "System Utils";
    private static final String PARENT_MENU_EXPECTED_PARENT_NAME = "Legacy SDR data";

    private static final String[] PROCESS_CLASSES = {
            "AddSDRReferenceTables",
            "AddSDRPersonTable",
            "AddSDRPersonAddressTable",
            "AddSDRPersonDocumentUploadTable",
            "AddSDRPersonHealthFunctioningStatusRatingTable",
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int parentMenuId = findExistingParentMenu();

        int processesCreated = 0;
        int processesSkipped = 0;
        int menusCreated = 0;
        int menusSkipped = 0;

        int nextSeqNo = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId);
        MTree_Base menuTree = new MTree_Base(getCtx(), SystemIDs.TREE_MENUPRIMARY, get_TrxName());

        for (String className : PROCESS_CLASSES) {
            String fullClassName = PACKAGE + className;

            int processId = DB.getSQLValueEx(get_TrxName(),
                    "SELECT AD_Process_ID FROM AD_Process WHERE Classname=?", fullClassName);
            if (processId > 0) {
                addLog(className + ": AD_Process already exists (AD_Process_ID=" + processId + ") - skipped.");
                processesSkipped++;
            } else {
                MProcess process = new MProcess(getCtx(), 0, get_TrxName());
                process.set_ValueOfColumn("AD_Client_ID", 0);
                process.setAD_Org_ID(0);
                process.setValue(className.length() > 40 ? className.substring(0, 40) : className);
                process.setName(className);
                process.setClassname(fullClassName);
                process.setEntityType(ENTITY_TYPE);
                process.setAccessLevel(ACCESS_LEVEL);
                process.setIsActive(true);
                process.saveEx();
                processId = process.getAD_Process_ID();
                addLog(className + ": created AD_Process_ID=" + processId);
                processesCreated++;
            }

            int menuId = DB.getSQLValueEx(get_TrxName(),
                    "SELECT AD_Menu_ID FROM AD_Menu WHERE AD_Process_ID=?", processId);
            if (menuId > 0) {
                addLog(className + ": AD_Menu already exists (AD_Menu_ID=" + menuId + ") - skipped.");
                menusSkipped++;
                continue;
            }

            MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
            menu.set_ValueOfColumn("AD_Client_ID", 0);
            menu.setAD_Org_ID(0);
            menu.setName(className);
            menu.setAction(MMenu.ACTION_Process);
            menu.setAD_Process_ID(processId);
            menu.setEntityType(ENTITY_TYPE);
            menu.setIsSOTrx(true);
            menu.setIsCentrallyMaintained(true);
            menu.saveEx();

            MTree_NodeMM node = MTree_NodeMM.get(menuTree, menu.getAD_Menu_ID());
            if (node == null) {
                throw new AdempiereException(
                        "AD_TreeNodeMM not auto-created by MMenu.afterSave() for AD_Menu_ID=" + menu.getAD_Menu_ID());
            }
            node.setParent_ID(parentMenuId);
            node.setSeqNo(nextSeqNo);
            node.saveEx();

            addLog(className + ": created AD_Menu_ID=" + menu.getAD_Menu_ID() + " under parent " + parentMenuId
                    + " (seqno=" + nextSeqNo + ")");
            nextSeqNo++;
            menusCreated++;
        }

        return "AD_Process: created " + processesCreated + ", skipped " + processesSkipped + ". "
                + "AD_Menu: created " + menusCreated + ", skipped " + menusSkipped + ".";
    }

    /**
     * Finds the existing "System Utils" menu (user-created 2026-09-05, under "Legacy SDR
     * data") that every SDR process's own menu item gets created under - does NOT create
     * anything, fails fast if it's missing or not where expected, rather than silently
     * building a new menu in the wrong place.
     */
    private int findExistingParentMenu() {
        int parentMenuId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_Menu_ID FROM AD_Menu WHERE Name=?", PARENT_MENU_NAME);
        if (parentMenuId <= 0) {
            throw new AdempiereException("Expected menu '" + PARENT_MENU_NAME + "' not found - "
                    + "it must already exist (create it manually first, the usual way).");
        }

        int actualParentId = DB.getSQLValueEx(get_TrxName(),
                "SELECT Parent_ID FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Node_ID=?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId);
        String actualParentName = actualParentId > 0
                ? DB.getSQLValueStringEx(get_TrxName(), "SELECT Name FROM AD_Menu WHERE AD_Menu_ID=?", actualParentId)
                : null;
        if (!PARENT_MENU_EXPECTED_PARENT_NAME.equals(actualParentName)) {
            throw new AdempiereException("Menu '" + PARENT_MENU_NAME + "' (AD_Menu_ID=" + parentMenuId
                    + ") was expected to be a child of '" + PARENT_MENU_EXPECTED_PARENT_NAME
                    + "' but its parent is '" + actualParentName + "' - not the menu this process expects to use, "
                    + "refusing to guess.");
        }

        addLog("Using existing '" + PARENT_MENU_NAME + "' menu (AD_Menu_ID=" + parentMenuId + ").");
        return parentMenuId;
    }
}
