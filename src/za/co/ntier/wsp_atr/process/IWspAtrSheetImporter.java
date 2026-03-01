package za.co.ntier.wsp_atr.process;

import java.sql.SQLException;
import java.util.Properties;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

public interface IWspAtrSheetImporter {

    /**
     * Import data from a single sheet as defined by mappingHeader.
     *
     * @return number of records created (lines or header-type rows)
     * @throws SQLException 
     * @throws IllegalStateException 
     */
    int importData(Properties ctx,
                   Workbook wb,
                   X_ZZ_WSP_ATR_Submitted submitted,
                   X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                   String trxName,
                   ImportWspAtrDataFromTemplate process,
                   DataFormatter formatter) throws IllegalStateException, SQLException;
    
    public CLogger getLog();

	public void setLog(CLogger log);
}
