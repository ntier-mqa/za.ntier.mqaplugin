package za.co.ntier.wsp_atr.process;

import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;

final class WspAtrExportResult {
    private final Path exportPath;
    private final int totalRowsExported;
    private final int tabCount;

    WspAtrExportResult(Path exportPath, int totalRowsExported, int tabCount) {
        this.exportPath = exportPath;
        this.totalRowsExported = totalRowsExported;
        this.tabCount = tabCount;
    }

    Path getExportPath() {
        return exportPath;
    }

    int getTotalRowsExported() {
        return totalRowsExported;
    }

    int getTabCount() {
        return tabCount;
    }
}

final class WspAtrExportTab {
    private final TabContext tabContext;
    private final WspAtrRowProvider rowProvider;
    private final WspAtrDocumentNoProvider documentNoProvider;
    private final boolean includeDocumentNo;
    private final String sheetName;

    WspAtrExportTab(TabContext tabContext, WspAtrRowProvider rowProvider,
            WspAtrDocumentNoProvider documentNoProvider, boolean includeDocumentNo, String sheetName) {
        this.tabContext = tabContext;
        this.rowProvider = rowProvider;
        this.documentNoProvider = documentNoProvider;
        this.includeDocumentNo = includeDocumentNo;
        this.sheetName = sheetName;
    }

    TabContext getTabContext() {
        return tabContext;
    }

    WspAtrRowProvider getRowProvider() {
        return rowProvider;
    }

    WspAtrDocumentNoProvider getDocumentNoProvider() {
        return documentNoProvider;
    }

    boolean isIncludeDocumentNo() {
        return includeDocumentNo;
    }

    String getSheetName() {
        return sheetName;
    }

    WspAtrExportTab withSheetName(String newSheetName) {
        return new WspAtrExportTab(tabContext, rowProvider, documentNoProvider, includeDocumentNo, newSheetName);
    }
}

final class TabContext {
    private final int adTabId;
    private final String tabUu;
    private final int tabSeqNo;
    private final int tabLevel;
    private final String tabName;
    private final MTable table;

    TabContext(int adTabId, String tabUu, int tabSeqNo, int tabLevel, String tabName, MTable table) {
        this.adTabId = adTabId;
        this.tabUu = tabUu;
        this.tabSeqNo = tabSeqNo;
        this.tabLevel = tabLevel;
        this.tabName = tabName;
        this.table = table;
    }

    int getAdTabId() {
        return adTabId;
    }

    String getTabUu() {
        return tabUu;
    }

    int getTabSeqNo() {
        return tabSeqNo;
    }

    int getTabLevel() {
        return tabLevel;
    }

    String getTabName() {
        return tabName;
    }

    MTable getTable() {
        return table;
    }
}

interface WspAtrRowProvider {
    List<PO> fetch(ExportSubmittedWspAtrToXlsm process, TabContext tabContext);
}

final class QueryRowProvider implements WspAtrRowProvider {
    private final String whereClause;
    private final String orderBy;
    private final Object[] parameters;

    QueryRowProvider(String whereClause, String orderBy) {
        this(whereClause, orderBy, new Object[0]);
    }

    QueryRowProvider(String whereClause, String orderBy, Object... parameters) {
        this.whereClause = whereClause;
        this.orderBy = orderBy;
        this.parameters = parameters == null ? new Object[0] : parameters;
    }

    @Override
    public List<PO> fetch(ExportSubmittedWspAtrToXlsm process, TabContext tabContext) {
        Query query = new Query(process.getCtx(), tabContext.getTable().getTableName(), whereClause, process.get_TrxName());
        if (parameters.length > 0) {
            query.setParameters(parameters);
        }
        if (orderBy != null && !orderBy.isBlank()) {
            query.setOrderBy(orderBy);
        }
        return query.list();
    }
}

interface WspAtrDocumentNoProvider {
    Object getDocumentNo(PO record);
}

interface WspAtrSheetColumn {
    String getHeader();

    void writeCell(WspAtrExportValueFormatter formatter, Cell cell, PO record);
}

final class WspAtrTableColumn implements WspAtrSheetColumn {
    private final MColumn column;
    private final String header;

    WspAtrTableColumn(MColumn column, String header) {
        this.column = column;
        this.header = header;
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public void writeCell(WspAtrExportValueFormatter formatter, Cell cell, PO record) {
        formatter.writeValue(cell, column, record.get_Value(column.getColumnName()));
    }
}

final class WspAtrSyntheticColumn implements WspAtrSheetColumn {
    private final String header;
    private final WspAtrDocumentNoProvider valueProvider;

    WspAtrSyntheticColumn(String header, WspAtrDocumentNoProvider valueProvider) {
        this.header = header;
        this.valueProvider = valueProvider;
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public void writeCell(WspAtrExportValueFormatter formatter, Cell cell, PO record) {
        formatter.writeValue(cell, null, valueProvider.getDocumentNo(record));
    }
}
