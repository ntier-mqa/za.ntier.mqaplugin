package za.co.ntier.wsp_atr.process;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
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

	private static final String EXCEL_PASSWORD = "Learning2026";
	private int p_ZZ_WSP_ATR_Submitted_ID;
	private final ReferenceLookupService refService = new ReferenceLookupService();

	@Override
	protected void prepare() {
		p_ZZ_WSP_ATR_Submitted_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		logHeap("PROCESS START");
		int totalErrors = 0;
		boolean validationCompletedWithErrors = false;
		Workbook wb = null;
		String stage = "start";

		try {
			if (p_ZZ_WSP_ATR_Submitted_ID <= 0) {
				throw new AdempiereException("No WSP/ATR Submitted record selected");
			}

			updateSubmittedStatusCommitted(
					p_ZZ_WSP_ATR_Submitted_ID,
					X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating
					);
			stage = "delete Records commited record";
			deleteRecordsCommitted(p_ZZ_WSP_ATR_Submitted_ID);

			Properties ctx = Env.getCtx();
			String trxName = get_TrxName();

			stage = "load submitted record";
			X_ZZ_WSP_ATR_Submitted submitted =
					new X_ZZ_WSP_ATR_Submitted(ctx, p_ZZ_WSP_ATR_Submitted_ID, trxName);

			logHeap("BEFORE load workbook");
			WorkbookLoadResult loadResult = loadWorkbook(submitted);
			wb = loadResult.workbook;
			logHeap("AFTER load workbook");

			stage = "create formatter/evaluator";
			logHeap("BEFORE create evaluator");
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			logHeap("AFTER create evaluator");

			List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
					ctx,
					X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
					null,
					trxName)
					.setOnlyActiveRecords(true)
					.list();

			if (headers == null || headers.isEmpty()) {
				throw new AdempiereException("No WSP/ATR mapping header records defined");
			}

			stage = "reset workbook before validation";
			ExcelValidationCleaner cleaner = new ExcelValidationCleaner();
			cleaner.resetWorkbookBeforeValidation(wb);

			for (X_ZZ_WSP_ATR_Lookup_Mapping mapHeader : headers) {
				stage = "validate sheet: " + mapHeader.getZZ_Tab_Name()
				+ " / tableId=" + mapHeader.getAD_Table_ID();
				logHeap("BEFORE " + stage);
				if (mapHeader.getAD_Table_ID() <= 0) {
					continue;
				}

				boolean isColumns = mapHeader.get_ValueAsBoolean("ZZ_Is_Columns");
				if (isColumns) {
					ColumnModeSheetValidator v = new ColumnModeSheetValidator(refService, this);
					v.setLog(log);
					totalErrors += v.validate(ctx, wb, submitted, mapHeader, trxName, formatter, evaluator);
				} else {
					// Row-mode validator here later
				}
				logHeap("AFTER " + stage);
			}

			if (totalErrors > 0) {
				stage = "attach error workbook";
				String errName = buildErrorFileName(submitted, loadResult.sourceFileName);
				attachErrorWorkbook(submitted, wb, errName);

				updateSubmittedStatusCommitted(
						p_ZZ_WSP_ATR_Submitted_ID,
						X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ValidationError
						);

				validationCompletedWithErrors = true;

				throw new AdempiereException(
						"Template has " + totalErrors
						+ " validation errors. Download the attached error file from the Upload Dashboard, "
						+ "fix highlighted cells, and try again."
						);
			}

			updateSubmittedStatusCommitted(
					p_ZZ_WSP_ATR_Submitted_ID,
					X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing
					);
			// So garbage collector runs
			formatter = null;
			evaluator = null;
			headers = null;
			cleaner = null;
			loadResult = null;
			submitted = null;
		} catch (OutOfMemoryError oom) {
			log.severe("OutOfMemoryError at stage: " + stage);
			throw oom;
		}  catch (Exception ex) {
			if (!validationCompletedWithErrors) {
				updateSubmittedStatusCommitted(
						p_ZZ_WSP_ATR_Submitted_ID,
						X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting
						);
			}
			throw ex;
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (Exception ignore) {
				}
				wb = null;
			}
		}
		logHeap("BEFORE IMPORT PROCESS and GC");
		
		//System.gc();
		//Thread.sleep(1000);
		//logHeap("AFTER VALIDATION GC");

		try {
			//  ImportWspAtrDataFromTemplate importProc = new ImportWspAtrDataFromTemplate();
			//  importProc.startProcess(getCtx(), getProcessInfo(), Trx.get(get_TrxName(), false));
			WspAtrImportService importService = new WspAtrImportService();
			int totalImported = importService.importSubmitted(
					getCtx(),
					p_ZZ_WSP_ATR_Submitted_ID,
					get_TrxName(),
					this
					);
			addLog("Imported " + totalImported + " records from all mapped tabs");

			updateSubmittedStatusCommitted(
					p_ZZ_WSP_ATR_Submitted_ID,
					X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported
					);
			
			return "";

		} catch (Exception ex) {
			updateSubmittedStatusCommitted(
					p_ZZ_WSP_ATR_Submitted_ID,
					X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting
					);
			throw ex;
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (Exception ignore) {}
			}
			logHeap("PROCESS END BEFORE CLOSE");
		}
	}


	private void attachErrorWorkbook(X_ZZ_WSP_ATR_Submitted submitted, Workbook wb, String fileName) throws Exception {
		logHeap("attachErrorWorkbook - start");

		byte[] data;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			wb.write(bos);
			bos.flush();
			logHeap("attachErrorWorkbook - after wb.write");
			data = bos.toByteArray();
			//log.warning("Generated workbook bytes=" + (data.length / 1024 / 1024) + "MB");
			logHeap("attachErrorWorkbook - after toByteArray");
		}

		if (data == null || data.length == 0) {
			throw new AdempiereException("Generated Excel file is empty.");
		}

		// Strong validation: ensure POI can reopen the generated workbook
		try (Workbook testWb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
			if (testWb.getNumberOfSheets() <= 0) {
				throw new AdempiereException("Generated Excel file is invalid: no sheets found.");
			}
		}

		String trxName = Trx.createTrxName("WSPATR_ERRFILE");
		Trx trx = Trx.get(trxName, true);

		try {
			MAttachment att = MAttachment.get(
					Env.getCtx(),
					X_ZZ_WSP_ATR_Submitted.Table_ID,
					submitted.get_ID()
					);

			if (att == null) {
				att = new MAttachment(
						Env.getCtx(),
						X_ZZ_WSP_ATR_Submitted.Table_ID,
						submitted.get_ID(),
						null,
						trxName
						);
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

	private WorkbookLoadResult loadWorkbook(X_ZZ_WSP_ATR_Submitted submitted) throws Exception {
		MAttachment attachment = MAttachment.get(
				Env.getCtx(),
				X_ZZ_WSP_ATR_Submitted.Table_ID,
				submitted.getZZ_WSP_ATR_Submitted_ID()
				);

		if (attachment == null || attachment.getEntryCount() <= 0) {
			throw new AdempiereException("No attachment found for WSP/ATR Submitted record.");
		}

		MAttachmentEntry[] entries = attachment.getEntries();
		MAttachmentEntry selectedEntry = null;

		for (MAttachmentEntry entry : entries) {
			if (entry == null) {
				continue;
			}

			String name = entry.getName();
			if (Util.isEmpty(name, true)) {
				continue;
			}

			if (name.toUpperCase().startsWith("ERROR")) {
				continue;
			}

			if (selectedEntry == null) {
				selectedEntry = entry;
			}
		}

		if (selectedEntry == null && entries.length > 0) {
			selectedEntry = entries[0];
		}

		if (selectedEntry == null) {
			throw new AdempiereException("Attachment has no valid entries.");
		}

		try (InputStream is = selectedEntry.getInputStream()) {
			if (is == null) {
				throw new AdempiereException(
						"Could not open attachment stream for file " + selectedEntry.getName()
				);
			}

			byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);

			// Only do ZIP-based precheck if the file is actually a plain ZIP xlsm/xlsx
			if (looksLikeZip(bytes)) {
				preCheckXlsmStyleBloat(
						new ByteArrayInputStream(bytes),
						selectedEntry.getName()
				);
			}

			ZipSecureFile.setMinInflateRatio(0.001);
			ZipSecureFile.setMaxEntrySize(200L * 1024L * 1024L);
			ZipSecureFile.setMaxTextSize(50L * 1024L * 1024L);
			IOUtils.setByteArrayMaxOverride(200 * 1024 * 1024);

			WorkbookLoadResult result = new WorkbookLoadResult();
			result.sourceFileName = selectedEntry.getName();
			result.workbook = openWorkbookAuto(bytes, EXCEL_PASSWORD);
			return result;
		}
	}
	private Workbook openWorkbookAuto(byte[] data, String password) throws Exception {
		try {
			return WorkbookFactory.create(new ByteArrayInputStream(data));
		} catch (EncryptedDocumentException e) {
			try (POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(data))) {
				EncryptionInfo info = new EncryptionInfo(fs);
				Decryptor decryptor = Decryptor.getInstance(info);

				if (!decryptor.verifyPassword(password)) {
					throw new AdempiereException("Invalid Excel password for workbook");
				}

				try (InputStream decryptedStream = decryptor.getDataStream(fs)) {
					return WorkbookFactory.create(decryptedStream);
				}
			} catch (AdempiereException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new AdempiereException("Failed to decrypt/open Excel workbook: " + ex.getMessage(), ex);
			}
		} catch (Exception e) {
			throw new AdempiereException("Failed to open Excel workbook: " + e.getMessage(), e);
		}
	}
	
	private boolean looksLikeZip(byte[] data) {
		return data != null
				&& data.length >= 4
				&& data[0] == 0x50
				&& data[1] == 0x4B
				&& data[2] == 0x03
				&& data[3] == 0x04;
	}

	private void preCheckXlsmStyleBloat(InputStream rawIs, String fileName) {
		final long MAX_STYLES_UNCOMPRESSED = 10L * 1024L * 1024L;
		final double MAX_STYLES_RATIO = 120.0;

		try (ZipInputStream zis = new ZipInputStream(rawIs)) {
			ZipEntry e;
			while ((e = zis.getNextEntry()) != null) {
				if (!"xl/styles.xml".equalsIgnoreCase(e.getName())) {
					continue;
				}

				long comp = e.getCompressedSize();
				long uncomp = e.getSize();

				if (uncomp > 0 && uncomp >= MAX_STYLES_UNCOMPRESSED) {
					throw new AdempiereException(
							"Your template appears to contain excessive formatting (styles). "
									+ "Please open it in Excel and do a 'Save As' to a new file, then upload again. "
									+ "Problem area: xl/styles.xml size=" + (uncomp / 1024) + " KB. File: " + fileName
							);
				}

				if (uncomp > 0 && comp > 0) {
					double ratio = (double) uncomp / (double) comp;
					if (ratio >= MAX_STYLES_RATIO) {
						throw new AdempiereException(
								"Your template appears to be format/style bloated (high compression ratio). "
										+ "Please open it in Excel and do a 'Save As' to a new file, then upload again. "
										+ "Problem area: xl/styles.xml ratio=" + String.format("%.1f", ratio)
										+ ". File: " + fileName
								);
					}
				}

				break;
			}
		} catch (AdempiereException ex) {
			throw ex;
		} catch (Exception ex) {
			// ignore pre-check failures
		}
	}

	private String safeFileName(String originalName, X_ZZ_WSP_ATR_Submitted submitted) {
		String name = originalName;

		if (Util.isEmpty(name, true)) {
			name = "WSP_ATR_" + submitted.get_ID() + ".xlsm";
		}

		name = name.trim();
		name = name.replaceAll("[\\\\/:*?\"<>|]", "_");

		if (name.length() > 120) {
			name = name.substring(0, 120);
		}

		return name;
	}

	private String buildErrorFileName(X_ZZ_WSP_ATR_Submitted submitted, String originalFileName) {
		String safeName = safeFileName(originalFileName, submitted);

		String ext = ".xlsx";
		String lower = safeName.toLowerCase();

		if (lower.endsWith(".xlsm")) {
			ext = ".xlsm";
			safeName = safeName.substring(0, safeName.length() - 5);
		} else if (lower.endsWith(".xlsx")) {
			ext = ".xlsx";
			safeName = safeName.substring(0, safeName.length() - 5);
		}

		String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return "ERROR_" + safeName + "_" + ts + ext;
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

	private void deleteRecordsCommitted(int submittedId) throws Exception {
		String trxName = Trx.createTrxName("WSPATR_STATUS2");
		Trx trx = Trx.get(trxName, true);
		try {
			deleteRelatedRecordsBeforeProcessing(submittedId, trxName);
			trx.commit(true);
		} catch (Exception e) {
			trx.rollback();
			throw e;
		} finally {
			trx.close();
		}
	}

	private void deleteRelatedRecordsBeforeProcessing(int submittedId, String trxName) {
		List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
				Env.getCtx(),
				X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
				null,
				trxName)
				.setOnlyActiveRecords(true)
				.list();

		if (headers == null || headers.isEmpty()) {
			return;
		}

		Set<Integer> tableIds = new HashSet<>();
		for (X_ZZ_WSP_ATR_Lookup_Mapping h : headers) {
			if (h.getAD_Table_ID() > 0) {
				tableIds.add(h.getAD_Table_ID());
			}
		}

		class DelJob {
			final String tableName;
			DelJob(String t) {
				tableName = t;
			}
		}

		List<DelJob> jobs = new ArrayList<>();

		for (Integer adTableId : tableIds) {
			MTable t = MTable.get(Env.getCtx(), adTableId);
			if (t == null) {
				continue;
			}

			String tableName = t.getTableName();
			if (Util.isEmpty(tableName, true)) {
				continue;
			}

			if (X_ZZ_WSP_ATR_Submitted.Table_Name.equalsIgnoreCase(tableName)
					|| X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name.equalsIgnoreCase(tableName)) {
				continue;
			}

			if (columnExists(tableName, "ZZ_WSP_ATR_Submitted_ID", trxName)) {
				jobs.add(new DelJob(tableName));
			}
		}

		if (jobs.isEmpty()) {
			return;
		}

		List<DelJob> remaining = new ArrayList<>(jobs);

		for (int pass = 1; pass <= 4 && !remaining.isEmpty(); pass++) {
			List<DelJob> failed = new ArrayList<>();

			for (DelJob job : remaining) {
				try {
					DB.executeUpdateEx(
							"DELETE FROM " + job.tableName + " WHERE ZZ_WSP_ATR_Submitted_ID=?",
							new Object[] { submittedId },
							trxName,
							0
							);
				} catch (Exception ex) {
					failed.add(job);
				}
			}

			remaining = failed;
		}

		if (!remaining.isEmpty()) {
			for (DelJob job : remaining) {
				log.log(
						Level.WARNING,
						"Could not delete from mapped table " + job.tableName
						+ " for ZZ_WSP_ATR_Submitted_ID=" + submittedId
						+ " (likely FK order or different link column)."
						);
			}
		}
	}

	private boolean columnExists(String tableName, String columnName, String trxName) {
		int cnt = DB.getSQLValueEx(
				trxName,
				"SELECT COUNT(1) "
						+ "FROM AD_Column c "
						+ "JOIN AD_Table t ON t.AD_Table_ID=c.AD_Table_ID "
						+ "WHERE t.TableName=? AND c.ColumnName=? AND c.IsActive='Y'",
						tableName,
						columnName
				);
		return cnt > 0;
	}

	private static class WorkbookLoadResult {
		private Workbook workbook;
		private String sourceFileName;
	}
	
	private void logHeap(String label) {
		Runtime rt = Runtime.getRuntime();
		long max = rt.maxMemory();
		long total = rt.totalMemory();
		long free = rt.freeMemory();
		long used = total - free;

		log.warning(label
				+ " | usedMB=" + (used / 1024 / 1024)
				+ " totalMB=" + (total / 1024 / 1024)
				+ " maxMB=" + (max / 1024 / 1024)
				+ " freeMB=" + (free / 1024 / 1024));
				
	}
}