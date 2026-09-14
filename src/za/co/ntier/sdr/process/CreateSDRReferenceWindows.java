package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MMenu;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MTree_Base;
import org.compiere.model.MTree_NodeMM;
import org.compiere.model.MWindow;
import org.compiere.model.SystemIDs;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.process.TabCreateFields;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Creates one read-only Window+Tab+Fields+Menu per SDR_ reference/catalog table (the 72 tables built
 * by {@link AddSDRReferenceTables}), and files every one of those menu entries under a new "Reference"
 * summary/folder menu node placed directly beneath the existing, manually-created "Legacy SDR data"
 * top-level menu (sibling of "System Utils").
 *
 * <p>Mirrors core iDempiere's own {@code org.compiere.process.CreateWindowFromTable} (the "Create
 * Window, Tab &amp; Field from Table" button on the Table and Column window) - window/tab creation
 * steps and the {@link TabCreateFields} invocation are copied directly from that class rather than
 * re-invented, since that's the exact same field-generation logic the platform itself uses. Two
 * deliberate departures from that class's defaults, since these are multi-row lookup tables, not
 * single-record setup tabs: {@code IsSingleRow=false} (it hardcodes {@code true}), and
 * {@code WindowType=QueryOnly} (it takes WindowType as a raw parameter with no default) - browsing/
 * viewing legacy migrated reference data, not data entry. {@code IsSOTrx=false} throughout (not
 * sales/purchase transaction data).
 *
 * <p>CLIENT SCOPING: unlike this package's Add-, Migrate-, and Register-style processes (which all hardcode
 * AD_Client_ID=0/System for their own AD_Process registration, since those are admin utilities meant
 * to run regardless of client), this process creates its Window/Tab/Menu/Field records using
 * {@link #getCtx()}'s NATURAL running-context client - per user instruction, this process must be run
 * under the MQA client (not System) so every artifact it creates lands on MQA, matching the SDR_ data
 * itself. See {@link RegisterSDRProcesses}'s own client-context Javadoc for why this distinction
 * matters (AD_Client_ID is picked up from context, not hardcoded).
 *
 * <p>Idempotent: skips any table whose window already exists (by Window Name, same duplicate check
 * core uses), and skips creating the "Reference" folder menu if it already exists as a child of
 * "Legacy SDR data" - safe to re-run after adding more reference tables to {@link #TABLE_NAMES} later.
 */
@Process(name = "za.co.ntier.sdr.process.CreateSDRReferenceWindows")
public class CreateSDRReferenceWindows extends SvrProcess {

    private static final String ENTITY_TYPE = "U";
    private static final String PARENT_MENU_NAME = "Legacy SDR data";
    private static final String REFERENCE_MENU_NAME = "Reference";

    /** Every SDR_ reference/catalog table built by {@link AddSDRReferenceTables}, in the same order. */
    private static final String[] TABLE_NAMES = {
            // --- Person family (23) ---
            "SDR_Title", "SDR_Gender", "SDR_Equity", "SDR_Disability", "SDR_HomeLanguage",
            "SDR_Nationality", "SDR_CitizenResidentialStatus", "SDR_SocioEconomicStatus",
            "SDR_AlternateIDType", "SDR_SchoolEMIS", "SDR_LastSchoolYear", "SDR_STATSSAAreaCode",
            "SDR_POPIActStatus", "SDR_HasSouthAfrican", "SDR_Verified", "SDR_ImmigrantStatus",
            "SDR_Suburb", "SDR_City", "SDR_Municipality", "SDR_UrbanRural", "SDR_Province",
            "SDR_HealthFunctioningStatus", "SDR_HealthFunctioningRating",

            // --- Organisation family (17) ---
            "SDR_OrganisationRegistrationNumberType", "SDR_TypeofOrganisation", "SDR_LegalStatus",
            "SDR_Partnership", "SDR_SICCode", "SDR_OrganisationSize", "SDR_BEEStatus",
            "SDR_LevyNumberType", "SDR_ChamberCode", "SDR_SubSector", "SDR_OrganisationType",
            "SDR_Designation", "SDR_BankName", "SDR_AccountType", "SDR_YesNo",
            "SDR_FinancialYearEnd", "SDR_WSPATRDocumentRelates",

            // --- SDF family (5) ---
            "SDR_SDFHighestEducation", "SDR_SDFStatus", "SDR_SDFRole", "SDR_SDFFunction",
            "SDR_SDFAppointmentProcedure",

            // --- WSPATR family (19) ---
            "SDR_WSPStatus", "SDR_LearningProgrammeType", "SDR_LearningProgramme",
            "SDR_WSPAchievementStatus", "SDR_WSPDropOut", "SDR_WSPProvince", "SDR_WSPMunicipality",
            "SDR_WSPQualificationType", "SDR_WSPManagementEquity",
            "SDR_WSPATREvaluationVerificationStatus", "SDR_WSPATREvaluationStatus",
            "SDR_WSPATREvaluationApprovalStatus", "SDR_WSPATREvaluationVerificationChecklistType",
            "SDR_WSPATREvaluationVerificationDeviation", "SDR_WSPScarceReason",
            "SDR_WSPNonEmployeeStatus", "SDR_WSPTargetBeneficiary", "SDR_WSPTopUpSkills",
            "SDR_WSPAppointment",

            // --- Levy family (3) ---
            "SDR_FinancialYear", "SDR_LevyField", "SDR_SETA",

            // --- Grant family (1) ---
            "SDR_GrantCode",

            // --- User/Security family (1) ---
            "SDR_Role",

            // --- Cross-cutting (3) ---
            "SDR_OFOSpecialization", "SDR_Year", "SDR_FormType",
    };

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        int referenceMenuId = findOrCreateReferenceMenu();

        MTree_Base menuTree = new MTree_Base(getCtx(), SystemIDs.TREE_MENUPRIMARY, get_TrxName());
        int nextSeqNo = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=?",
                SystemIDs.TREE_MENUPRIMARY, referenceMenuId);

        int windowsCreated = 0;
        int windowsSkipped = 0;

        for (String tableName : TABLE_NAMES) {
            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            if (table == null) {
                throw new IllegalStateException(tableName + " does not exist - run AddSDRReferenceTables first");
            }

            int existingWindow = DB.getSQLValue(get_TrxName(), "SELECT 1 FROM AD_Window WHERE Name = ?",
                    table.getName());
            if (existingWindow == 1) {
                addLog(table.getName() + ": window already exists - skipped.");
                windowsSkipped++;
                continue;
            }

            MWindow window = new MWindow(getCtx(), 0, get_TrxName());
            window.setName(table.getName());
            window.setIsSOTrx(false);
            window.setWindowType(MWindow.WINDOWTYPE_QueryOnly);
            window.setEntityType(table.getEntityType());
            window.saveEx();

            MTab tab = new MTab(window);
            tab.setEntityType(table.getEntityType());
            tab.setSeqNo(10);
            tab.setName(table.getName());
            tab.setAD_Table_ID(table.getAD_Table_ID());
            tab.setTabLevel(0);
            tab.setIsSingleRow(false);
            tab.setOrderByClause(table.getTableName() + ".Value");
            tab.saveEx();

            createFields(tab);

            MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
            menu.setName(window.getName());
            menu.setEntityType(table.getEntityType());
            menu.setIsSOTrx(false);
            menu.setAction(MMenu.ACTION_Window);
            menu.setAD_Window_ID(window.getAD_Window_ID());
            menu.saveEx();

            MTree_NodeMM node = MTree_NodeMM.get(menuTree, menu.getAD_Menu_ID());
            if (node == null) {
                throw new AdempiereException(
                        "AD_TreeNodeMM not auto-created by MMenu.afterSave() for AD_Menu_ID=" + menu.getAD_Menu_ID());
            }
            node.setParent_ID(referenceMenuId);
            node.setSeqNo(nextSeqNo);
            node.saveEx();
            nextSeqNo++;

            if (table.getAD_Window_ID() <= 0) {
                table.setAD_Window_ID(window.getAD_Window_ID());
                table.saveEx();
            }

            addLog(tableName + ": created AD_Window_ID=" + window.getAD_Window_ID() + ", AD_Menu_ID="
                    + menu.getAD_Menu_ID());
            windowsCreated++;
        }

        return "SDR reference windows: created " + windowsCreated + ", skipped " + windowsSkipped
                + " (already existed).";
    }

    /**
     * Reuses the exact {@link TabCreateFields} invocation core's own CreateWindowFromTable uses -
     * generates one MField per MColumn on the tab's table (including the standard audit columns),
     * matching the platform's normal "Create Fields" behavior rather than reimplementing field
     * generation.
     */
    private void createFields(MTab tab) {
        ProcessInfo processInfo = new ProcessInfo("", SystemIDs.PROCESS_AD_TAB_CREATEFIELDS, MTab.Table_ID,
                tab.getAD_Tab_ID(), tab.getAD_Tab_UU());

        MPInstance instance = new MPInstance(getCtx(), SystemIDs.PROCESS_AD_TAB_CREATEFIELDS, MTab.Table_ID,
                tab.getAD_Tab_ID(), tab.getAD_Tab_UU());
        instance.saveEx();
        processInfo.setAD_PInstance_ID(instance.getAD_PInstance_ID());

        TabCreateFields createFields = new TabCreateFields();
        boolean success = createFields.startProcess(getCtx(), processInfo, Trx.get(get_TrxName(), false));
        if (!success) {
            throw new AdempiereException("TabCreateFields failed for AD_Tab_ID=" + tab.getAD_Tab_ID() + ": "
                    + processInfo.getSummary());
        }
    }

    /**
     * Finds "Legacy SDR data" (must already exist - the same manually-created top-level menu
     * {@link RegisterSDRProcesses} files "System Utils" under) and either finds or creates the
     * "Reference" summary/folder menu as its direct child.
     */
    private int findOrCreateReferenceMenu() {
        int parentMenuId = DB.getSQLValueEx(get_TrxName(), "SELECT AD_Menu_ID FROM AD_Menu WHERE Name=?",
                PARENT_MENU_NAME);
        if (parentMenuId <= 0) {
            throw new AdempiereException(
                    "Expected menu '" + PARENT_MENU_NAME + "' not found - it must already exist.");
        }

        int existing = DB.getSQLValue(get_TrxName(),
                "SELECT n.Node_ID FROM AD_TreeNodeMM n JOIN AD_Menu m ON m.AD_Menu_ID = n.Node_ID "
                        + "WHERE n.AD_Tree_ID = ? AND n.Parent_ID = ? AND m.Name = ?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId, REFERENCE_MENU_NAME);
        if (existing > 0) {
            addLog("Using existing '" + REFERENCE_MENU_NAME + "' menu (AD_Menu_ID=" + existing + ").");
            return existing;
        }

        MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
        menu.setName(REFERENCE_MENU_NAME);
        menu.setEntityType(ENTITY_TYPE);
        menu.setIsSOTrx(false);
        menu.setIsSummary(true);
        menu.saveEx();

        MTree_Base menuTree = new MTree_Base(getCtx(), SystemIDs.TREE_MENUPRIMARY, get_TrxName());
        MTree_NodeMM node = MTree_NodeMM.get(menuTree, menu.getAD_Menu_ID());
        if (node == null) {
            throw new AdempiereException(
                    "AD_TreeNodeMM not auto-created by MMenu.afterSave() for AD_Menu_ID=" + menu.getAD_Menu_ID());
        }
        int seqNo = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(SeqNo),-1)+1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=? AND Parent_ID=?",
                SystemIDs.TREE_MENUPRIMARY, parentMenuId);
        node.setParent_ID(parentMenuId);
        node.setSeqNo(seqNo);
        node.saveEx();

        addLog("Created '" + REFERENCE_MENU_NAME + "' menu (AD_Menu_ID=" + menu.getAD_Menu_ID() + ") under '"
                + PARENT_MENU_NAME + "'.");
        return menu.getAD_Menu_ID();
    }
}
