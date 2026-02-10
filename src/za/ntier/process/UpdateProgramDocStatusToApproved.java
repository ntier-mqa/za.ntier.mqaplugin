package za.ntier.process;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.X_ZZ_Program_Master_Data;

@org.adempiere.base.annotation.Process(name = "za.co.ntier.process.ApproveProgramMasterData")
public class UpdateProgramDocStatusToApproved extends SvrProcess {

    @Parameter(name = "ZZ_Program_Master_Data_ID")
    private int programMasterDataId = 0;

    @Override
    protected void prepare() {
        // @Parameter handles it
    }

    @Override
    protected String doIt() throws Exception {
        if (programMasterDataId <= 0) {
            throw new AdempiereException("ZZ_Program_Master_Data_ID is mandatory.");
        }

        X_ZZ_Program_Master_Data program =
                new X_ZZ_Program_Master_Data(getCtx(), programMasterDataId, get_TrxName());

        if (program.get_ID() <= 0) {
            throw new AdempiereException("Program Master Data not found: " + programMasterDataId);
        }

        // If already approved, do nothing (safe)
        if (X_ZZ_Program_Master_Data.ZZ_DOCSTATUS_Approved.equals(program.getZZ_DocStatus())) {
            return "No change: already AP. (ZZ_Program_Master_Data_ID=" + programMasterDataId + ")";
        }

        program.setZZ_DocStatus(X_ZZ_Program_Master_Data.ZZ_DOCSTATUS_Approved);

        // Optional: clear doc action or set it to something consistent with your workflow
        // program.setZZ_DocAction(null);

        if (!program.save()) {
            throw new AdempiereException("Failed to update ZZ_DocStatus to AP for ZZ_Program_Master_Data_ID=" + programMasterDataId);
        }

        addLog("Updated ZZ_Program_Master_Data_ID=" + programMasterDataId + " to ZZ_DocStatus=AP");
        return "Updated Program DocNo =" + program.getDocumentNo() + " to ZZ_DocStatus=AP";
    }
}
