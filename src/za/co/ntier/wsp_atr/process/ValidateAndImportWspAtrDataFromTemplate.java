package za.co.ntier.wsp_atr.process;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.ValidateAndImportWspAtrDataFromTemplate")
public class ValidateAndImportWspAtrDataFromTemplate extends SvrProcess {

	private int p_ZZ_WSP_ATR_Submitted_ID;
	private final ReferenceLookupService refService = new ReferenceLookupService();

	@Override
	protected void prepare() {
		p_ZZ_WSP_ATR_Submitted_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		int totalErrors = 0;
		boolean validationCompletedWithErrors = false;
		try {
			if (p_ZZ_WSP_ATR_Submitted_ID <= 0)
				throw new AdempiereException("No WSP/ATR Submitted record selected");

			updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID, X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating);


			Properties ctx = Env.getCtx();
			String trxName = get_TrxName();

			X_ZZ_WSP_ATR_Submitted submitted =
					new X_ZZ_WSP_ATR_Submitted(ctx, p_ZZ_WSP_ATR_Submitted_ID, trxName);

			// Load workbook (same as your existing)
			Workbook wb = loadWorkbook(submitted);
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

			// Load mapping headers
			List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
					ctx,
					X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
					null,
					trxName)
					.setOnlyActiveRecords(true)
					.list();

			if (headers == null || headers.isEmpty())
				throw new AdempiereException("No WSP/ATR mapping header records defined");

			// before looping headers / validating
			ExcelValidationCleaner cleaner = new ExcelValidationCleaner();
			cleaner.resetWorkbookBeforeValidation(wb);

			for (X_ZZ_WSP_ATR_Lookup_Mapping mapHeader : headers) {
				if (mapHeader.getAD_Table_ID() <= 0) continue;

				boolean isColumns = mapHeader.get_ValueAsBoolean("ZZ_Is_Columns");
				if (isColumns) {
					ColumnModeSheetValidator v = new ColumnModeSheetValidator(refService, this);
					totalErrors += v.validate(ctx, wb, submitted, mapHeader, trxName, formatter, evaluator);
				} else {
					// build RowModeSheetValidator similarly
					// totalErrors += rowValidator.validate(...)
				}
			}

			if (totalErrors > 0) {
				// write workbook to bytes + attach as error file
				String errName = buildErrorFileName(submitted);
				attachErrorWorkbook(submitted, wb, errName);
				updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID,X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ValidationError );


				// attachErrorWorkbook(submitted, wb, "ERROR_" + safeFileName(submitted) + ".xlsm");
				validationCompletedWithErrors = true;
				throw new AdempiereException("Template has " + totalErrors
						+ " validation errors. Download the attached error file from the Upload Dashboard, fix highlighted cells, and try again.");
			}

			updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID, X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing);
		} catch (Exception ex) {
			if (!validationCompletedWithErrors) {
				// ✅ This is the missing piece: sheet missing / workbook load / any unexpected validation error
				updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID,X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting);

				// Optional: store message somewhere if you have a column (recommended)
				// updateSubmittedErrorCommitted(p_ZZ_WSP_ATR_Submitted_ID, ex.getMessage());
			}

			throw ex;
		}


		// No errors => run import
		try {
			ImportWspAtrDataFromTemplate importProc = new ImportWspAtrDataFromTemplate();
			importProc.startProcess(getCtx(), getProcessInfo(), Trx.get(get_TrxName(), false)); // OR refactor import into a service and call it directly
			updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID, X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported);
			return "";
			//return "Validation passed. Import completed.";
		} catch (Exception ex) {
			updateSubmittedStatusCommitted(p_ZZ_WSP_ATR_Submitted_ID,X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting );
			throw ex;
		}


	}

	// implement: loadWorkbook(submitted) same as you already have
	// implement: attachErrorWorkbook(submitted, wb, filename) -> MAttachment.addEntry(...)

		
	private void attachErrorWorkbook(X_ZZ_WSP_ATR_Submitted submitted, Workbook wb, String fileName) throws Exception {
	    byte[] data;
	    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
	        wb.write(bos);
	        bos.flush();
	        data = bos.toByteArray();
	    }

	    String trxName = Trx.createTrxName("WSPATR_ERRFILE");
	    Trx trx = Trx.get(trxName, true);
	    try {
	        MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submitted.get_ID());
	        if (att == null) {
	            att = new MAttachment(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submitted.get_ID(), null, trxName);
	        } else {
	            att.set_TrxName(trxName);
	        }

	        att.addEntry(fileName, data);
	        att.saveEx(trxName);

	        trx.commit(true);
	    } catch (Exception e) {
	        trx.rollback();
	        throw e;
	    } finally {
	        trx.close();
	    }
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

		// Prefer a non-error file that matches the original filename (if used)
		for (MAttachmentEntry entry : entries) {
			if (entry == null)
				continue;

			String name = entry.getName();
			if (Util.isEmpty(name, true))
				continue;

			// Skip error files
			if (name.toUpperCase().startsWith("ERROR"))
				continue;



			// Otherwise remember the first valid non-error file
			if (selectedEntry == null) {
				selectedEntry = entry;
			}
		}

		// Safety fallback
		if (selectedEntry == null && entries.length > 0) {
			selectedEntry = entries[0];
		}
		

		if (selectedEntry == null) {
			throw new AdempiereException("Attachment has no valid entries.");
		}

		/*
		try (InputStream is = selectedEntry.getInputStream()) {
		    if (is == null) {
		        throw new AdempiereException(
		            "Could not open attachment stream for file " + selectedEntry.getName());
		    }

		    // POI zip-bomb protection (xlsm/xlsx are ZIPs)
		    ZipSecureFile.setMinInflateRatio(0.001); // default 0.01; this is safer but less strict
		    ZipSecureFile.setMaxEntrySize(200L * 1024L * 1024L); // optional
		    ZipSecureFile.setMaxTextSize(50L * 1024L * 1024L);   // optional

		    // Still keep your byte[] limit override (good for large sheets)
		    IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);

		    return WorkbookFactory.create(is);
		}
		*/
		
		try (InputStream is = selectedEntry.getInputStream()) {
		    if (is == null) {
		        throw new AdempiereException("Could not open attachment stream for file " + selectedEntry.getName());
		    }

		    byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);

		    // pre-check using a fresh stream
		    preCheckXlsmStyleBloat(new java.io.ByteArrayInputStream(bytes), selectedEntry.getName());

		    // now load workbook using another fresh stream
		    ZipSecureFile.setMinInflateRatio(0.001); // default 0.01; this is safer but less strict
		    ZipSecureFile.setMaxEntrySize(200L * 1024L * 1024L); // optional
		    ZipSecureFile.setMaxTextSize(50L * 1024L * 1024L);   // optional
		    IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);

		    return WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes));
		} catch (AdempiereException ex) {
	        throw ex;
		}
	}
	
	private void preCheckXlsmStyleBloat(InputStream rawIs, String fileName) {
	    // thresholds - tune for your templates
	    final long MAX_STYLES_UNCOMPRESSED = 10L * 1024L * 1024L; // 10MB
	    final double MAX_STYLES_RATIO = 120.0; // uncompressed/compressed

	    try (ZipInputStream zis = new ZipInputStream(rawIs)) {
	        ZipEntry e;
	        while ((e = zis.getNextEntry()) != null) {
	            if (!"xl/styles.xml".equalsIgnoreCase(e.getName())) {
	                continue;
	            }

	            long comp = e.getCompressedSize(); // may be -1 for streams; often present
	            long uncomp = e.getSize();         // may be -1; often present

	            // If sizes are available, do an early warning
	            if (uncomp > 0 && uncomp >= MAX_STYLES_UNCOMPRESSED) {
	                throw new AdempiereException(
	                    "Your template appears to contain excessive formatting (styles). " +
	                    "Please open it in Excel and do a 'Save As' to a new file, then upload again. " +
	                    "Problem area: xl/styles.xml size=" + (uncomp / 1024) + " KB. File: " + fileName
	                );
	            }

	            if (uncomp > 0 && comp > 0) {
	                double ratio = (double) uncomp / (double) comp;
	                if (ratio >= MAX_STYLES_RATIO) {
	                    throw new AdempiereException(
	                        "Your template appears to be format/style bloated (high compression ratio). " +
	                        "Please open it in Excel and do a 'Save As' to a new file, then upload again. " +
	                        "Problem area: xl/styles.xml ratio=" + String.format("%.1f", ratio) + ". File: " + fileName
	                    );
	                }
	            }

	            // If we found styles.xml and it doesn't trip thresholds, we can stop
	            break;
	        }
	    } catch (AdempiereException ex) {
	        throw ex;
	    } catch (Exception ex) {
	        // If pre-check fails for any reason, don't block import; just continue
	        // (Optional: log at FINE level)
	    }
	}

	private String safeFileName(X_ZZ_WSP_ATR_Submitted submitted) {
		// If you have a real filename column, use it. Otherwise fall back.
		String name = null;

		// Example (only if your model actually has it):
		// name = submitted.getFileName();

		if (Util.isEmpty(name, true)) {
			name = "WSP_ATR_" + submitted.get_ID() + ".xlsm";
		}

		name = name.trim();

		// Ensure extension
		if (!name.toLowerCase().endsWith(".xlsm")) {
			name = name + ".xlsm";
		}

		// Remove path chars and other illegal filename chars
		name = name.replaceAll("[\\\\/:*?\"<>|]", "_");

		// Keep it reasonable length for attachments
		if (name.length() > 120) {
			name = name.substring(0, 120);
		}

		return name;
	}

	private String buildErrorFileName(X_ZZ_WSP_ATR_Submitted submitted) {
		String baseName = safeFileName(submitted);

		// Remove extension
		if (baseName.toLowerCase().endsWith(".xlsm")) {
			baseName = baseName.substring(0, baseName.length() - 5);
		}

		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

		return "ERROR_" + baseName + "_" + ts + ".xlsm";
	}



	private void updateSubmittedStatusCommitted(int submittedId, String status) throws Exception {
		String trxName = Trx.createTrxName("WSPATR_STATUS");
		Trx trx = Trx.get(trxName, true);
		try {
			DB.executeUpdateEx(
					"UPDATE ZZ_WSP_ATR_Submitted SET ZZ_DocStatus=? WHERE ZZ_WSP_ATR_Submitted_ID=?",
					new Object[] { status, submittedId },
					trxName
					);
			trx.commit(true);
		} catch (Exception e) {
			trx.rollback();
			throw e;
		} finally {
			trx.close();
		}
	}



}
