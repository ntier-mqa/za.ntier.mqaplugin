package za.co.ntier.wsp_atr.process;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;

final class WspAtrExportPlanBuilder {

    private static final String SUBMITTED_WINDOW_UU = "406eaf0a-7d74-4942-9429-07f09ffeed85";
    private static final String SUBMITTED_TAB_UU = "b3369b7f-fd0c-4e13-bdcc-b1b042bc2c65";
    private static final int LEVEL_ZERO = 0;
    private static final int LEVEL_ONE = 1;
    private static final String SUBMITTED_LINK_COLUMN = I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_WSP_ATR_Submitted_ID;

    private final ExportSubmittedWspAtrToXlsm process;

    WspAtrExportPlanBuilder(ExportSubmittedWspAtrToXlsm process) {
        this.process = process;
    }

    List<WspAtrExportTab> build() {
        WindowRuntime window = resolveWindow();
        List<WspAtrExportTab> exportTabs = new ArrayList<>();

        WspAtrExportTab submittedTab = buildSubmittedTab(window);
        exportTabs.add(submittedTab);

        exportTabs.addAll(buildLevelOneTabs(window));
        exportTabs.sort(Comparator
                .comparingInt((WspAtrExportTab tab) -> tab.getTabContext().getTabLevel())
                .thenComparingInt(tab -> tab.getTabContext().getTabSeqNo()));

        return ensureUniqueSheetNames(exportTabs);
    }

    private WindowRuntime resolveWindow() {
        PO adWindow = new Query(process.getCtx(), "AD_Window", "AD_Window_UU=? AND IsActive='Y'", process.get_TrxName())
                .setParameters(SUBMITTED_WINDOW_UU)
                .firstOnly();
        if (adWindow == null) {
            throw new IllegalStateException("Unable to resolve AD_Window_UU=" + SUBMITTED_WINDOW_UU);
        }

        return new WindowRuntime(adWindow.get_ID(), SUBMITTED_WINDOW_UU);
    }

    private WspAtrExportTab buildSubmittedTab(WindowRuntime window) {
        TabContext tabContext = resolveSpecificTab(window, SUBMITTED_TAB_UU, LEVEL_ZERO);
        String whereClause = process.hasFiscalYearFilter()
                ? I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_FinYear_ID + "=?"
                : null;
        QueryRowProvider rowProvider = process.hasFiscalYearFilter()
                ? new QueryRowProvider(whereClause, tabContext.getTable().getTableName() + "_ID", process.getFiscalYearId())
                : new QueryRowProvider(whereClause, tabContext.getTable().getTableName() + "_ID");
        return new WspAtrExportTab(
                tabContext,
                rowProvider,
                new CurrentRecordDocumentNoProvider(),
                true,
                tabContext.getTabName());
    }

    private List<WspAtrExportTab> buildLevelOneTabs(WindowRuntime window) {
        List<WspAtrExportTab> tabs = new ArrayList<>();
        Map<Integer, X_ZZ_WSP_ATR_Lookup_Mapping> mappingsByTableId = loadMappingsByTableId();
        Map<Integer, String> documentNoCache = new HashMap<>();

        for (Map.Entry<Integer, X_ZZ_WSP_ATR_Lookup_Mapping> entry : mappingsByTableId.entrySet()) {
            int tableId = entry.getKey();
            if (tableId == I_ZZ_WSP_ATR_Submitted.Table_ID) {
                continue;
            }

            PO adTab = new Query(process.getCtx(), "AD_Tab",
                    "AD_Window_ID=? AND AD_Table_ID=? AND IsActive='Y' AND COALESCE(TabLevel,0)=?",
                    process.get_TrxName())
                            .setParameters(window.getWindowId(), tableId, LEVEL_ONE)
                            .setOrderBy("SeqNo, AD_Tab_ID")
                            .firstOnly();
            if (adTab == null) {
                continue;
            }

            TabContext tabContext = createTabContext(adTab, window, LEVEL_ONE);
            ensureSubmittedLinkColumnExists(tabContext);

            X_ZZ_WSP_ATR_Lookup_Mapping mapping = entry.getValue();
            String whereClause = SUBMITTED_LINK_COLUMN + ">0";
            QueryRowProvider rowProvider;
            if (process.hasFiscalYearFilter()) {
                whereClause += " AND " + SUBMITTED_LINK_COLUMN + " IN (SELECT "
                        + I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_WSP_ATR_Submitted_ID
                        + " FROM " + I_ZZ_WSP_ATR_Submitted.Table_Name
                        + " WHERE " + I_ZZ_WSP_ATR_Submitted.COLUMNNAME_ZZ_FinYear_ID + "=?)";
                rowProvider = new QueryRowProvider(whereClause,
                        SUBMITTED_LINK_COLUMN + ", " + tabContext.getTable().getTableName() + "_ID",
                        process.getFiscalYearId());
            } else {
                rowProvider = new QueryRowProvider(whereClause,
                        SUBMITTED_LINK_COLUMN + ", " + tabContext.getTable().getTableName() + "_ID");
            }
            tabs.add(new WspAtrExportTab(
                    tabContext,
                    rowProvider,
                    new ParentDocumentNoProvider(documentNoCache),
                    true,
                    resolveSheetName(mapping, adTab)));
        }

        return tabs;
    }

    private Map<Integer, X_ZZ_WSP_ATR_Lookup_Mapping> loadMappingsByTableId() {
        List<X_ZZ_WSP_ATR_Lookup_Mapping> mappings = new Query(process.getCtx(),
                X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                X_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_AD_Table_ID + ">0 AND IsActive='Y'",
                process.get_TrxName())
                        .setOrderBy(X_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_SeqNo + ", "
                                + X_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_Tab_Name + ", "
                                + X_ZZ_WSP_ATR_Lookup_Mapping.COLUMNNAME_ZZ_WSP_ATR_Lookup_Mapping_ID)
                        .list();

        Map<Integer, X_ZZ_WSP_ATR_Lookup_Mapping> mappingsByTableId = new LinkedHashMap<>();
        for (X_ZZ_WSP_ATR_Lookup_Mapping mapping : mappings) {
            if (mapping.isZZ_Is_For_Bulk()) {
                continue;
            }
            if (mapping.getAD_Table_ID() > 0) {
                mappingsByTableId.putIfAbsent(mapping.getAD_Table_ID(), mapping);
            }
        }
        return mappingsByTableId;
    }

    private TabContext resolveSpecificTab(WindowRuntime window, String tabUu, int expectedLevel) {
        PO adTab = new Query(process.getCtx(), "AD_Tab", "AD_Tab_UU=? AND IsActive='Y'", process.get_TrxName())
                .setParameters(tabUu)
                .firstOnly();
        if (adTab == null) {
            throw new IllegalStateException("Unable to resolve AD_Tab_UU=" + tabUu);
        }
        return createTabContext(adTab, window, expectedLevel);
    }

    private TabContext createTabContext(PO adTab, WindowRuntime window, int expectedLevel) {
        if (adTab.get_ValueAsInt("AD_Window_ID") != window.getWindowId()) {
            throw new IllegalStateException("AD_Tab_ID=" + adTab.get_ID() + " does not belong to AD_Window_UU="
                    + window.getWindowUu());
        }

        int actualLevel = adTab.get_ValueAsInt("TabLevel");
        if (actualLevel != expectedLevel) {
            throw new IllegalStateException("AD_Tab_ID=" + adTab.get_ID() + " expected TabLevel="
                    + expectedLevel + " but was " + actualLevel);
        }

        int tableId = adTab.get_ValueAsInt("AD_Table_ID");
        MTable table = MTable.get(process.getCtx(), tableId);
        if (table == null || table.getAD_Table_ID() <= 0) {
            throw new IllegalStateException("Unable to resolve AD_Table_ID=" + tableId + " for AD_Tab_ID=" + adTab.get_ID());
        }

        return new TabContext(
                adTab.get_ID(),
                adTab.get_ValueAsString("AD_Tab_UU"),
                adTab.get_ValueAsInt("SeqNo"),
                actualLevel,
                Util.isEmpty(adTab.get_ValueAsString("Name"), true)
                        ? table.getTableName()
                        : adTab.get_ValueAsString("Name"),
                table);
    }

    private void ensureSubmittedLinkColumnExists(TabContext tabContext) {
        if (tabContext.getTable().getColumnIndex(SUBMITTED_LINK_COLUMN) < 0) {
            throw new IllegalStateException("Table " + tabContext.getTable().getTableName()
                    + " is missing mandatory link column " + SUBMITTED_LINK_COLUMN);
        }
    }

    private String resolveSheetName(X_ZZ_WSP_ATR_Lookup_Mapping mapping, PO adTab) {
        if (!Util.isEmpty(mapping.getZZ_Tab_Name(), true)) {
            return mapping.getZZ_Tab_Name();
        }
        String tabName = adTab.get_ValueAsString("Name");
        return Util.isEmpty(tabName, true) ? String.valueOf(mapping.getAD_Table_ID()) : tabName;
    }

    private List<WspAtrExportTab> ensureUniqueSheetNames(List<WspAtrExportTab> exportTabs) {
        List<WspAtrExportTab> uniqueTabs = new ArrayList<>();
        Set<String> usedNames = new LinkedHashSet<>();

        for (WspAtrExportTab exportTab : exportTabs) {
            String baseName = sanitizeSheetName(exportTab.getSheetName());
            String candidate = baseName;
            int suffix = 1;
            while (!usedNames.add(candidate.toUpperCase())) {
                String suffixText = "_" + suffix++;
                int maxBaseLength = Math.max(1, 31 - suffixText.length());
                candidate = baseName.substring(0, Math.min(baseName.length(), maxBaseLength)) + suffixText;
            }
            uniqueTabs.add(exportTab.withSheetName(candidate));
        }

        return uniqueTabs;
    }

    private String sanitizeSheetName(String sheetName) {
        String candidate = Util.isEmpty(sheetName, true) ? "Sheet" : sheetName.trim();
        candidate = candidate
                .replace('\\', '_')
                .replace('/', '_')
                .replace('*', '_')
                .replace('?', '_')
                .replace(':', '_')
                .replace('[', '_')
                .replace(']', '_');
        if (candidate.length() > 31) {
            candidate = candidate.substring(0, 31);
        }
        return candidate.isBlank() ? "Sheet" : candidate;
    }

    private final class CurrentRecordDocumentNoProvider implements WspAtrDocumentNoProvider {
        @Override
        public Object getDocumentNo(PO record) {
            return record.get_Value(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo);
        }
    }

    private final class ParentDocumentNoProvider implements WspAtrDocumentNoProvider {
        private final Map<Integer, String> documentNoCache;

        private ParentDocumentNoProvider(Map<Integer, String> documentNoCache) {
            this.documentNoCache = documentNoCache;
        }

        @Override
        public Object getDocumentNo(PO record) {
            int submittedId = record.get_ValueAsInt(SUBMITTED_LINK_COLUMN);
            if (submittedId <= 0) {
                return "";
            }
            return documentNoCache.computeIfAbsent(submittedId, this::loadDocumentNo);
        }

        private String loadDocumentNo(int submittedId) {
            PO submittedRecord = MTable.get(process.getCtx(), I_ZZ_WSP_ATR_Submitted.Table_ID)
                    .getPO(submittedId, process.get_TrxName());
            if (submittedRecord == null) {
                return "";
            }
            Object value = submittedRecord.get_Value(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo);
            return value == null ? "" : String.valueOf(value);
        }
    }

    private static final class WindowRuntime {
        private final int windowId;
        private final String windowUu;

        private WindowRuntime(int windowId, String windowUu) {
            this.windowId = windowId;
            this.windowUu = windowUu;
        }

        private int getWindowId() {
            return windowId;
        }

        private String getWindowUu() {
            return windowUu;
        }
    }
}
