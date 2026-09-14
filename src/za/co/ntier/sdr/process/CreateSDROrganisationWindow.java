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
 * Creates the "SDR Organisation" window (12 tabs: main + 9 direct children + 1 grandchild), filed
 * under the "Data" summary menu beneath "Legacy SDR data". See {@link WindowCreationSupport}'s Javadoc
 * for the AD_Client_ID=0/System scoping rationale.
 *
 * <p>SDR_OrganisationLinkages self-references SDR_Organisation via SDR_ParentOrganisation_ID/
 * SDR_ChildOrganisation_ID (not a plain SDR_Organisation_ID, since it's a many-to-many linkage between
 * two organisation records, not a simple one-parent child row) - nested here using
 * SDR_ParentOrganisation_ID as the tab link, showing "organisations linked as a child of this one";
 * SDR_ChildOrganisation_ID remains a regular field on that tab.
 *
 * <p>SDR_OrganisationBankingDetailsDocumentUpload's PRIMARY parent (per its own schema Javadoc) is
 * SDR_OrganisationBankingDetails_ID, not SDR_Organisation_ID (which it also carries as a redundant
 * convenience column, matching source design) - nested two levels deep, under BankingDetails.
 *
 * <p>SDR_OrganisationIST has schema but no Migrate* process yet (a known gap flagged during window
 * planning) - its tab will show 0 rows until that migration class is written.
 *
 * <p>Idempotent: skips if a window named "SDR Organisation" already exists.
 */
@Process(name = "za.co.ntier.sdr.process.CreateSDROrganisationWindow")
public class CreateSDROrganisationWindow extends SvrProcess {

    private static final String ENTITY_TYPE = "U";
    private static final String PARENT_MENU_NAME = "Legacy SDR data";
    private static final String DATA_MENU_NAME = "Data";
    private static final String WINDOW_NAME = "SDR Organisation";

    /** {tableName, tabLevel, linkColumnName (the child's own FK column back to its parent tab, null at level 0)}. */
    private static final String[][] TAB_SPECS = {
            {"SDR_Organisation", "0", null},
            {"SDR_OrganisationAddress", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationCFODetails", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationComments", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationContacts", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationTrainingCommittee", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationLinkages", "1", "SDR_ParentOrganisation_ID"},
            {"SDR_OrganisationIST", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationBankingDetails", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationBankingDetailsDocumentUpload", "2", "SDR_OrganisationBankingDetails_ID"},
            {"SDR_OrganisationDocuments", "1", "SDR_Organisation_ID"},
            {"SDR_OrganisationEmails", "1", "SDR_Organisation_ID"},
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
