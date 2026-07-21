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
 * One-off registration process: creates the AD_Process (Application Dictionary > Process)
 * record AND a menu item for each of the 15 Phase 1 table-creation classes (see
 * "Phase 1 - New Tables - Mapping.txt" and Migration Steps.txt Step 15).
 *
 * <p>AD_Process convention, matching the 12 rows the user created manually for the earlier
 * processes in this project:
 * <ul>
 *   <li>Value = plain class name, truncated to 40 chars (AD_Process.Value is varchar(40))</li>
 *   <li>Name = plain class name, in full (AD_Process.Name is varchar(60), none of these exceed it)</li>
 *   <li>Classname = fully-qualified za.co.ntier.learner.process.&lt;ClassName&gt;</li>
 *   <li>EntityType = "U" (User Maintained)</li>
 *   <li>AccessLevel = "6" (System+Client+Org - the majority convention for these table/column
 *       creation processes; matches 11 of the 12 existing rows)</li>
 *   <li>AD_Client_ID = 0, AD_Org_ID = 0 (System level)</li>
 * </ul>
 *
 * <p>Menu convention, per user instruction: each new AD_Menu item is created as a child of
 * AD_Menu_ID=1000280 ("Learnerships Migration", AD_Menu_UU=916e6d83-3b93-437f-81cd-658f28ebc685)
 * in the primary Menu tree (AD_Tree_ID=10, {@link SystemIDs#TREE_MENUPRIMARY}) - the same tree
 * that already carries the 5 Migrate* process menu items as children of that same parent.
 * MMenu.afterSave() auto-inserts a AD_TreeNodeMM row (Parent_ID=0, SeqNo=999) for every
 * IsAllNodes='Y' Menu-type tree; this process then re-parents that auto-created node under
 * 1000280 with the next available SeqNo.
 *
 * <p>Idempotent: skips any class name that already has an AD_Process row with a matching
 * Classname, and skips menu creation for any process that already has an AD_Menu row pointing
 * at it - so it is safe to run more than once.
 *
 * <p>This process itself must still be registered manually as an AD_Process, the usual way,
 * since it cannot register itself.
 */
@Process(name = "za.co.ntier.learner.process.RegisterZZPhase1Processes")
public class RegisterZZPhase1Processes extends SvrProcess {

    private static final String PACKAGE = "za.co.ntier.learner.process.";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "6";
    private static final int PARENT_MENU_ID = 1000280;

    private static final String[] PROCESS_CLASSES = {
            "AddZZLearnerQCTOArtisanDocumentTable",
            "AddZZLearnerQCTOArtisanEISADetailsTable",
            "AddZZLearnerQCTOArtisansAssessmentsTable",
            "AddZZLearnerQCTOLearnershipACTable",
            "AddZZLearnerQCTOLearnershipAssessmentsColumns",
            "AddZZLearnerQCTOLearnershipDocumentTable",
            "AddZZLearnerQCTOLearnershipEISADetailsTable",
            "AddZZLearnerQCTOLearnershipHistoryTable",
            "AddZZLearnerQCTOLearnershipSDProviderTable",
            "AddZZLearnerQCTOLearnershipWATable",
            "AddZZLearnerQCTOSkillsProgrammeAssessmentsTable",
            "AddZZLearnerQCTOSkillsProgrammeDocumentTable",
            "AddZZLearnershipDocumentTable",
            "AddZZLearnerEISAReadinessTable",
            "AddZZLearnerEISAEnrolmentTable",
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
