package za.co.ntier.wsp_atr.process;

import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.apps.BackgroundJob;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;

public class RunJasperReport {

	public RunJasperReport() {
		// TODO Auto-generated constructor stub
	}
	
	public static void runProcessInBackgroundWithIntParam(String adProcessUU, String paramName, int paramValue, int recordIdForInstance) {
        final Properties ctx = Env.getCtx();

        MProcess proc = MProcess.get(ctx, adProcessUU);
        if (proc == null || proc.getAD_Process_ID() <= 0) {
            throw new AdempiereException("Process not found (UU=" + adProcessUU + ")");
        }

        ProcessInfo pi = new ProcessInfo(proc.getName(), proc.getAD_Process_ID());
        pi.setAD_User_ID(Env.getAD_User_ID(ctx));
        pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
        pi.setTable_ID(MTable.getTable_ID(X_ZZ_WSP_ATR_Report.Table_Name));
        pi.setRecord_ID(recordIdForInstance);
        pi.setAD_Process_UU(proc.getAD_Process_UU());
        

        MPInstance instance = new MPInstance(ctx, proc.getAD_Process_ID(), 0, recordIdForInstance, null);
        instance.setIsRunAsJob(true);
        instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
        instance.saveEx();

        /*
        // Save parameter (int)
        MPInstancePara para = new MPInstancePara(instance, 10);
        para.setParameterName(paramName);
        para.setP_Number(paramValue);
        para.saveEx();
        */

        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

       // Callback<Integer> cb = id -> { /* params already saved */ };
        // This callback will be called in Background job
        Callback<Integer> createInstanceParaCallback = id -> {
			if (id > 0) {
				MPInstance instanceLater = new MPInstance(Env.getCtx(),id,null);
				MPInstancePara para = new MPInstancePara(instanceLater, 10);
		        para.setParameterName(paramName);
		        para.setP_Number(paramValue);
		        para.saveEx();
			}
		};

        BackgroundJob.create(pi)
            .withContext(ctx)
            .withNotificationType(instance.getNotificationType())
            .withInitialDelay(250)
            .run(createInstanceParaCallback);
    }

}
