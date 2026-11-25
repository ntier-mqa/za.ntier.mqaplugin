package za.co.ntier.sdr.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
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
 * Added 2026-11-25: creates a read-only Window + Tab + Fields + Menu for EVERY SDR_ table that does
 * not yet have a tab anywhere, and files each menu entry under "Legacy SDR data" &gt; "Reference".
 * As of this date that is 39 tables: the 34 lookup tables and SDR_SICCodeChamber added after the
 * original window build (see {@link AddSDRReferenceTables}, {@link AddSDRSICCodeChamberTable}), plus
 * SDR_GrantProcess, SDR_GrantGPProcessData, SDR_LevyProcess and SDR_LevyGPProcessData (originally
 * "stage only, no window" - now wanted after all).
 *
 * <p>Tables are discovered DYNAMICALLY (SDR_% in AD_Table, not a view, with no AD_Tab row pointing at
 * them, and a physical table present), not from a hardcoded list - so any SDR_ table added later is
 * picked up on the next run, and every table already covered by a family or reference window is left
 * alone. The full list is logged before anything is created.
 *
 * <p>Same recipe as {@link CreateSDRReferenceWindows} (one window per table, {@code WindowType=QueryOnly},
 * single-row=false, field generation via the platform's own TabCreateFields) plus what the later
 * retroactive fix processes applied by hand: the "id" recon field is hidden and the SDR_ field labels
 * are word-separated ({@link WindowCreationSupport#hideIdField}, {@link WindowCreationSupport#renameSdrFields}).
 * Tab sort order is Value, else Name, else the table's creation timestamp, depending on which columns
 * the table actually has (the 4 process-data tables have no Value column).
 *
 * <p>Run under the SYSTEM client: window/tab/menu records are AD metadata hardcoded to AD_Client_ID=0
 * (see {@link CreateSDRReferenceWindows}'s Javadoc for why). Idempotent - a table that already has a
 * tab is never selected, and a window name that already exists is skipped with a warning.
 */
@Process(name = "za.co.ntier.sdr.process.CreateSDRMissingTableWindows")
public class CreateSDRMissingTableWindows extends SvrProcess {

    private static final String ENTITY_TYPE = "U";
    private static final String PARENT_MENU_NAME = "Legacy SDR data";
    private static final String REFERENCE_MENU_NAME = "Reference";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        List<String> tableNames = findTablesWithoutWindow();
        addLog(tableNames.size() + " SDR_ table(s) without a tab: " + tableNames);
        if (tableNames.isEmpty()) {
            return "No SDR_ tables without a window - nothing to do.";
        }

        int referenceMenuId = WindowCreationSupport.findOrCreateSummaryMenu(getCtx(), PARENT_MENU_NAME,
                REFERENCE_MENU_NAME, ENTITY_TYPE, get_TrxName(), this::addLog);

        int created = 0;
        int skipped = 0;
        for (String tableName : tableNames) {
            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            if (table == null) {
                addLog(tableName + ": AD_Table not found - skipped.");
                skipped++;
                continue;
            }
            if (DB.getSQLValue(get_TrxName(), "SELECT 1 FROM AD_Window WHERE Name = ?", table.getName()) == 1) {
                addLog("WARN " + table.getName() + ": a window with this name already exists but has no tab on "
                        + tableName + " - skipped, please check.");
                skipped++;
                continue;
            }

            MWindow window = new MWindow(getCtx(), 0, get_TrxName());
            window.set_ValueOfColumn("AD_Client_ID", 0);
            window.setAD_Org_ID(0);
            window.setName(table.getName());
            window.setIsSOTrx(false);
            window.setWindowType(MWindow.WINDOWTYPE_QueryOnly);
            window.setEntityType(table.getEntityType());
            window.saveEx();

            MTab tab = new MTab(window);
            tab.set_ValueOfColumn("AD_Client_ID", 0);
            tab.setAD_Org_ID(0);
            tab.setEntityType(table.getEntityType());
            tab.setSeqNo(10);
            tab.setName(table.getName());
            tab.setAD_Table_ID(table.getAD_Table_ID());
            tab.setTabLevel(0);
            tab.setIsSingleRow(false);
            tab.setOrderByClause(table.getTableName() + "." + sortColumn(table));
            tab.saveEx();

            WindowCreationSupport.createFields(getCtx(), tab, get_TrxName());
            if (table.getColumn("id") != null) {
                WindowCreationSupport.hideIdField(tab, table, get_TrxName());
            }
            WindowCreationSupport.renameSdrFields(tab, get_TrxName());

            MMenu menu = new MMenu(getCtx(), 0, get_TrxName());
            menu.set_ValueOfColumn("AD_Client_ID", 0);
            menu.setAD_Org_ID(0);
            menu.setName(window.getName());
            menu.setEntityType(table.getEntityType());
            menu.setIsSOTrx(false);
            menu.setAction(MMenu.ACTION_Window);
            menu.setAD_Window_ID(window.getAD_Window_ID());
            menu.saveEx();
            WindowCreationSupport.attachMenuToTree(getCtx(), menu.getAD_Menu_ID(), referenceMenuId, get_TrxName());

            if (table.getAD_Window_ID() <= 0) {
                table.setAD_Window_ID(window.getAD_Window_ID());
                table.saveEx();
            }

            addLog(tableName + ": created AD_Window_ID=" + window.getAD_Window_ID() + ", AD_Menu_ID="
                    + menu.getAD_Menu_ID());
            created++;
        }

        return "SDR windows for previously window-less tables: created " + created + ", skipped " + skipped
                + ". Menu: " + PARENT_MENU_NAME + " > " + REFERENCE_MENU_NAME + ".";
    }

    /** Value if the table has it (lookup tables), else Name, else Created (always present). */
    private String sortColumn(MTable table) {
        if (table.getColumn("Value") != null) {
            return "Value";
        }
        if (table.getColumn("Name") != null) {
            return "Name";
        }
        return "Created";
    }

    private List<String> findTablesWithoutWindow() {
        List<String> names = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement("SELECT t.TableName FROM AD_Table t WHERE t.TableName LIKE 'SDR\\_%' "
                    + "ESCAPE '\\' AND t.IsView = 'N' AND t.IsActive = 'Y' "
                    + "AND NOT EXISTS (SELECT 1 FROM AD_Tab tb WHERE tb.AD_Table_ID = t.AD_Table_ID) "
                    + "AND EXISTS (SELECT 1 FROM information_schema.tables it "
                    + "WHERE it.table_name = LOWER(t.TableName)) ORDER BY t.TableName", get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new org.adempiere.exceptions.AdempiereException("Failed listing SDR_ tables without a window", e);
        } finally {
            DB.close(rs, pstmt);
        }
        return names;
    }
}
