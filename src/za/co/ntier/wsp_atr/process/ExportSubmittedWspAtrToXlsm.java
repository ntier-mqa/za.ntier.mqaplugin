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
}
