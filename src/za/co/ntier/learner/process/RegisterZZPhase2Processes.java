package za.co.ntier.learner.process;

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
 * One-off registration process for Phase 2 (see "Additional Tables - Migration Plan.txt" and
 * "Phase 2 - LearnerLearnership Family - Mapping.txt"): creates the AD_Process record AND a menu
 * item for each of the 6 Phase 2 table/column-creation classes, same mechanism and conventions as
 * {@link RegisterZZPhase1Processes} (see that class's Javadoc for the full write-up of the
 * AD_Process convention and the AD_TreeNodeMM re-parenting mechanism - not repeated here).
 *
 * <p>Idempotent: skips any class name that already has an AD_Process row with a matching
 * Classname, and skips menu creation for any process that already has an AD_Menu row pointing at
 * it - safe to run more than once.
 *
 * <p>This process itself must still be registered manually as an AD_Process, the usual way,
 * since it cannot register itself.
 */
@Process(name = "za.co.ntier.learner.process.RegisterZZPhase2Processes")
public class RegisterZZPhase2Processes extends SvrProcess {

    private static final String PACKAGE = "za.co.ntier.learner.process.";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "6";
    private static final int PARENT_MENU_ID = 1000280;

    private static final String[] PROCESS_CLASSES = {
            "AddZZLearnerLearnershipColumns",
            "AddZZLearnerLearnershipAssessmentsTable",
            "AddZZLearnerLearnershipDocumentTable",
            "AddZZLearnerLearnershipEmployerTable",
            "AddZZLearnerLearnershipProviderTable",
            "AddZZLearnerLearnershipHistoryTable",
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int processesCreated = 0;
        int processesSkipped = 0;
        int menusCreated = 0;
        int menusSkipped = 0;

        int nextSeqNo = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=?",
                SystemIDs.TREE_MENUPRIMARY, PARENT_MENU_ID);
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
            node.setParent_ID(PARENT_MENU_ID);
            node.setSeqNo(nextSeqNo);
            node.saveEx();

            addLog(className + ": created AD_Menu_ID=" + menu.getAD_Menu_ID() + " under parent " + PARENT_MENU_ID
                    + " (seqno=" + nextSeqNo + ")");
            nextSeqNo++;
            menusCreated++;
        }

        return "AD_Process: created " + processesCreated + ", skipped " + processesSkipped + ". "
                + "AD_Menu: created " + menusCreated + ", skipped " + menusSkipped + ".";
    }
}
