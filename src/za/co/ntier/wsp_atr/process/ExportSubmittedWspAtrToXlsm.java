package za.co.ntier.wsp_atr.process;

import org.adempiere.base.annotation.Process;
import org.adempiere.base.annotation.Parameter;
import org.apache.poi.ss.usermodel.Cell;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

@Process(name = "za.co.ntier.wsp_atr.process.ExportSubmittedWspAtrToXlsm")
public class ExportSubmittedWspAtrToXlsm extends SvrProcess {

    @Parameter(name = "ZZ_FinYear_ID")
    private int fiscalYearId;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
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

    void writeResolvedValue(Cell cell, MColumn column, Object value) {
        new WspAtrExportValueFormatter(this).writeValue(cell, column, value);
    }

    int getFiscalYearId() {
        return fiscalYearId;
    }

    boolean hasFiscalYearFilter() {
        return fiscalYearId > 0;
    }
}
