package za.ntier.report.jasper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClientInfo;
import org.compiere.model.MImage;
import org.compiere.model.MOrgInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;



public class HeaderLogoImageLoader {

	private File imageFile=null;
	/**	Logger			*/
	protected CLogger	log = CLogger.getCLogger(getClass());

	public void getLogoFile( Properties ctx, String destinationFolder){

		MImage image=null;

		//imageFile =  File.createTempFile("logo", "jpg");
		//First check org level for logo
		MOrgInfo orgInfo = MOrgInfo.get( Env.getCtx(), Env.getAD_Org_ID(Env.getCtx()), null );
		if ( orgInfo != null && orgInfo.getLogo_ID()!= 0){
			image = new MImage(Env.getCtx(), orgInfo.getLogo_ID(), null);
		}
		else {
			MClientInfo clientInfo = MClientInfo.get(Env.getCtx(),  Integer.valueOf(Env.getAD_Client_ID(Env.getCtx())), null);

			if ( clientInfo != null && clientInfo.getLogoReport_ID() != 0 ) {
				image = new MImage(Env.getCtx(), clientInfo.getLogoReport_ID(), null);
			}
		}

		if (image != null ) {
			byte [] data = image.getBinaryData();
			if (data != null) {
				writeLogoFilesToTmpReportDir(data,destinationFolder);
			}
		}
	}

	private File writeLogoFilesToTmpReportDir(byte [] data,String destinationFolder) {
		String localFile = destinationFolder + "logo.jpg";
		String downloadedLocalFile = destinationFolder + "TMP_" + "logo.jpg";
		File reportFile = new File(localFile);
		if (reportFile.exists()) {			
			File downloadedFile = new File(downloadedLocalFile);
			getFile(downloadedFile,data);
			if (!reportFile.delete()) {
				throw new AdempiereException("Cannot delete temporary file " + reportFile.toString());
			}
			if (!downloadedFile.renameTo(reportFile)) {
				throw new AdempiereException("Cannot rename temporary file " + downloadedFile.toString() + " to "
						+ reportFile.toString());
			}

		} else {
			getFile(reportFile,data);
		}
		return reportFile;
	}


	public File getFile (File file,byte [] data)
	{

		try
		{
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.close();
		}
		catch (IOException ioe)
		{
			log.log(Level.SEVERE, "getFile", ioe);
			throw new RuntimeException(ioe);
		}
		return file;
	}	//	getFile


}
