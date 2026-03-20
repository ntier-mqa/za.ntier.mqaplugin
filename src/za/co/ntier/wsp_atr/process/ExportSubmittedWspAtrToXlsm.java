package za.co.ntier.wsp_atr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.process.SvrProcess;

@Process(name = "za.co.ntier.wsp_atr.process.ExportSubmittedWspAtrToXlsm")
public class ExportSubmittedWspAtrToXlsm extends SvrProcess {

    @Override
    protected void prepare() {
    }

    @Override
    protected String doIt() throws Exception {
        WspAtrExportResult result = new WspAtrExportService(this).export();

        if (processUI != null) {
            processUI.download(result.getExportPath().toFile());
        }

        return "Exported " + result.getTotalRowsExported() + " record(s) across "
                + result.getTabCount() + " tab(s) to " + result.getExportPath().getFileName();
    }

    private interface SheetColumn {
        String getHeader();

        void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record);
    }

    private interface RecordProvider {
        List<PO> fetch(ExportSubmittedWspAtrToXlsm process, TabRuntimeContext runtimeContext);
    }

    private interface SyntheticValueProvider {
        Object getValue(PO record);
    }

    private static final class ExportTabDefinition {
        private final String windowUu;
        private final String tabUu;
        private final String sheetName;
        private final RecordProvider recordProvider;
        private final List<SyntheticColumn> syntheticColumns;
        private final Set<String> mandatoryTableColumns;

        private ExportTabDefinition(String windowUu, String tabUu, String sheetName, RecordProvider recordProvider,
                List<SyntheticColumn> syntheticColumns, Set<String> mandatoryTableColumns) {
            this.windowUu = windowUu;
            this.tabUu = tabUu;
            this.sheetName = sheetName;
            this.recordProvider = recordProvider;
            this.syntheticColumns = syntheticColumns;
            this.mandatoryTableColumns = mandatoryTableColumns;
        }

        private static ExportTabDefinition levelZeroSubmittedTab() {
            return new ExportTabDefinition(
                    SUBMITTED_WINDOW_UU,
                    SUBMITTED_TAB_UU,
                    I_ZZ_WSP_ATR_Submitted.Table_Name,
                    (process, runtimeContext) -> new Query(process.getCtx(), runtimeContext.table.getTableName(), null,
                            process.get_TrxName())
                                    .setOrderBy(runtimeContext.table.getTableName() + "_ID")
                                    .list(),
                    List.of(new SyntheticColumn(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo,
                            record -> record.get_Value(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo))),
                    new LinkedHashSet<>(List.of(I_ZZ_WSP_ATR_Submitted.COLUMNNAME_DocumentNo)));
        }
    }

    private static final class TabRuntimeContext {
        private final int adTabId;
        private final MTable table;

        private TabRuntimeContext(int adTabId, MTable table) {
            this.adTabId = adTabId;
            this.table = table;
        }
    }

    private static final class TableColumn implements SheetColumn {
        private final MColumn column;

        private TableColumn(MColumn column) {
            this.column = column;
        }

        @Override
        public String getHeader() {
            return column.getColumnName();
        }

        @Override
        public void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record) {
            process.writeResolvedValue(cell, column, record.get_Value(column.getColumnName()));
        }
    }

    private static final class SyntheticColumn implements SheetColumn {
        private final String header;
        private final SyntheticValueProvider valueProvider;

        private SyntheticColumn(String header, SyntheticValueProvider valueProvider) {
            this.header = header;
            this.valueProvider = valueProvider;
        }

        @Override
        public String getHeader() {
            return header;
        }

        @Override
        public void writeCell(ExportSubmittedWspAtrToXlsm process, Cell cell, PO record) {
            process.writeResolvedValue(cell, null, valueProvider.getValue(record));
        }
    }
}
