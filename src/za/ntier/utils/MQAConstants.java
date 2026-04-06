package za.ntier.utils;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;

public final class MQAConstants {

    private static final String WF_RUN_PROCESS_CLASSNAME = "za.co.ntier.wf.process.ZZ_WF_RunProcess";

    /**
     * List of table/view names that use UUIDs for keys and require 
     * custom zoom or multi-selection handling in Info Windows.
     * Add new tables to this array as needed.
     */
    public static final String[] CUSTOM_ZOOM_TABLES = {
        "ZZ_QCTO_APPLICATION_INFO_V",
        "ZZ_AuditSchedule_v"
    };

    /**
     * Resolves the AD_Process_ID for the standard ZZ_WF_RunProcess workflow engine.
     *
     * @param trxName the transaction name (may be null)
     * @return the AD_Process_ID
     * @throws AdempiereException if the process cannot be found
     */
    public static int getWFRunProcessId(String trxName)
    {
        int processId = DB.getSQLValue(trxName,
                "SELECT AD_Process_ID FROM AD_Process WHERE Classname=? AND IsActive='Y'",
                WF_RUN_PROCESS_CLASSNAME);
        if (processId <= 0)
            throw new AdempiereException("Could not find active AD_Process for " + WF_RUN_PROCESS_CLASSNAME);
        return processId;
    }

    private MQAConstants() {
    }
}
