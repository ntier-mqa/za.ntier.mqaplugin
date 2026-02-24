package za.co.ntier.wsp_atr.process;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;

@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.ImportWspAtrDataFromTemplate")
public class ImportWspAtrDataFromTemplate extends SvrProcess {

	private int p_ZZ_WSP_ATR_Submitted_ID;

	private final ReferenceLookupService refService = new ReferenceLookupService();
	
	private FormulaEvaluator evaluator = null;

	@Override
	protected void prepare() {
		p_ZZ_WSP_ATR_Submitted_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		if (p_ZZ_WSP_ATR_Submitted_ID <= 0) {
			throw new org.adempiere.exceptions.AdempiereException(
					"No WSP/ATR Submitted record selected");
		}

		Properties ctx = Env.getCtx();
		String trxName = get_TrxName();

		X_ZZ_WSP_ATR_Submitted submitted =
				new X_ZZ_WSP_ATR_Submitted(ctx, p_ZZ_WSP_ATR_Submitted_ID, trxName);

		Workbook wb = loadWorkbook(submitted);
		DataFormatter formatter = new DataFormatter();
		evaluator = wb.getCreationHelper().createFormulaEvaluator();

		// Load all mapping headers for this process (you can filter by TabName if needed)
		List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
				ctx,
				X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
				null,
				trxName)
				.setOnlyActiveRecords(true)
				.list();

		if (headers == null || headers.isEmpty()) {
			throw new org.adempiere.exceptions.AdempiereException(
					"No WSP/ATR mapping header records defined");
		}

		int totalImported = 0;
		for (X_ZZ_WSP_ATR_Lookup_Mapping mapHeader : headers) {
			if (mapHeader.getAD_Table_ID() <= 0) {
				continue;
			}
			//if (mapHeader.getZZ_WSP_ATR_Lookup_Mapping_ID() != 1000007) {
			//	continue;
			//}
			boolean isColumns = mapHeader.get_ValueAsBoolean("ZZ_Is_Columns");  // Y=column mode, N=row mode


			IWspAtrSheetImporter importer = isColumns
					? new ColumnModeSheetImporter(refService,this)
							: new RowModeSheetImporter(refService,this);

			int count = importer.importData(ctx, wb, submitted, mapHeader, trxName, this, formatter);
			totalImported += count;

		}
		MZZWSPATRSubmitted mZZWSPATRSubmitted = new MZZWSPATRSubmitted(ctx, p_ZZ_WSP_ATR_Submitted_ID, trxName);

		addLog("The WSP-ATR import for " + mZZWSPATRSubmitted.getOrganisationName() + " with SDL Number " + mZZWSPATRSubmitted.getSdlNumber() + " was successful.");  // will be sent via emails
		return "Imported " + totalImported + " records from all mapped tabs";
	}

	private Workbook loadWorkbook(X_ZZ_WSP_ATR_Submitted submitted) throws Exception {
		// String fileName = submitted.getFileName();
		//  if (Util.isEmpty(fileName, true)) {
		//      throw new AdempiereException("No file name specified on WSP/ATR Submitted record.");
		//  }

		// Get attachment for this record
		MAttachment attachment = MAttachment.get(
				Env.getCtx(),
				X_ZZ_WSP_ATR_Submitted.Table_ID,
				submitted.getZZ_WSP_ATR_Submitted_ID());

		if (attachment == null || attachment.getEntryCount() <= 0) {
			throw new AdempiereException("No attachment found for WSP/ATR Submitted record.");
		}

		// Try to find an entry whose name matches FileName (case-insensitive)
		MAttachmentEntry[] entries = attachment.getEntries();
		MAttachmentEntry selectedEntry = null;

		/*
        for (MAttachmentEntry entry : entries) {
            if (entry != null && fileName.equalsIgnoreCase(entry.getName())) {
                selectedEntry = entry;
                break;
            }
        }
		 */

		// If not found by name, fall back to the first entry
		if (selectedEntry == null) {
			selectedEntry = entries[0];
		}

		if (selectedEntry == null) {
			throw new AdempiereException("Attachment has no valid entries.");
		}

		try (InputStream is = selectedEntry.getInputStream()) {
			if (is == null) {
				throw new AdempiereException(
						"Could not open attachment stream for file " + selectedEntry.getName());
			}
			IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);
			return WorkbookFactory.create(is);
		}
	}

	public FormulaEvaluator getEvaluator() {
		return evaluator;
	}

	public void setEvaluator(FormulaEvaluator evaluator) {
		this.evaluator = evaluator;
	}
}
