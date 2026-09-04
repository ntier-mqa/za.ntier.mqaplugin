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
 * project's RegisterZZPhase1Processes, adapted for a brand new domain that has no existing
 * menu container yet (the Learner project's equivalent, "Learnerships Migration"
 * AD_Menu_ID=1000280, already existed under "Learnerships" AD_Menu_ID=1000279 before that
 * class was written - confirmed via the live AD_Menu/AD_TreeNodeMM tables 2026-09-04, there
 * is no "SDR"-domain equivalent).
 *
 * <p>Per user decision 2026-09-04: creates a brand new TOP-LEVEL "SDR Migration" summary menu
 * (Parent_ID=0, same root level as "Learnerships" itself - confirmed via
 * AD_TreeNodeMM.Parent_ID=0 for node_id=1000279) rather than nesting under any existing menu,
 * since none of the existing "SDR"-named menus (SDR Configuration/Temporary Branch/Temporary
 * Organisation) are related to this migration. All current and future SDR migration
 * processes get their menu item created as a child of this one new container, the same way
 * every Learner migration process is a child of "Learnerships Migration".
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
 * Classname, skips menu creation for any process that already has an AD_Menu row pointing at
 * it, and skips creating "SDR Migration" itself if a menu with that exact name already
 * exists - safe to run more than once, and safe to extend {@link #PROCESS_CLASSES} with
 * future SDR process classes and re-run.
 *
 * <p>This process itself must still be registered manually as an AD_Process, the usual way,
 * since it cannot register itself.
 */
@Process(name = "za.co.ntier.sdr.process.RegisterSDRProcesses")
public class RegisterSDRProcesses extends SvrProcess {

    private static final String PACKAGE = "za.co.ntier.sdr.process.";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "6";
    private static final String PARENT_MENU_NAME = "SDR Migration";

    private static final String[] PROCESS_CLASSES = {
            "AddSDRReferenceTables",
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int parentMenuId = findOrCreateParentMenu();

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
     * Find-or-create the top-level "SDR Migration" summary menu (Parent_ID=0) that every SDR
     * process's own menu item gets created under - see class Javadoc for why this is a new
     * top-level container rather than nesting under an existing menu.
     */
    private int findOrCreateParentMenu() throws Exception {
        int existingId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_Menu_ID FROM AD_Menu WHERE Name=?", PARENT_MENU_NAME);
        if (existingId > 0) {
            addLog("'" + PARENT_MENU_NAME + "' menu already exists (AD_Menu_ID=" + existingId + ") - reused.");
            return existingId;
        }

        MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
        menu.set_ValueOfColumn("AD_Client_ID", 0);
        menu.setAD_Org_ID(0);
        menu.setName(PARENT_MENU_NAME);
        menu.setDescription("SDR (MQA_0626) migration processes - staged mssdr_* tables to SDR_ Application Dictionary tables");
        menu.setIsSummary(true);
        menu.setEntityType(ENTITY_TYPE);
        menu.setIsSOTrx(true);
        menu.setIsCentrallyMaintained(true);
        menu.saveEx();

        MTree_Base menuTree = new MTree_Base(getCtx(), SystemIDs.TREE_MENUPRIMARY, get_TrxName());
        MTree_NodeMM node = MTree_NodeMM.get(menuTree, menu.getAD_Menu_ID());
        if (node == null) {
            throw new AdempiereException(
                    "AD_TreeNodeMM not auto-created by MMenu.afterSave() for AD_Menu_ID=" + menu.getAD_Menu_ID());
        }
        int rootSeqNo = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=0",
                SystemIDs.TREE_MENUPRIMARY);
        node.setParent_ID(0);
        node.setSeqNo(rootSeqNo);
        node.saveEx();

        addLog("Created top-level menu '" + PARENT_MENU_NAME + "' (AD_Menu_ID=" + menu.getAD_Menu_ID() + ")");
        return menu.getAD_Menu_ID();
    }
}
