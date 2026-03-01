package za.co.ntier.wsp_atr.process;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.model.MRefTable;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

/**
 * Column-mode importer.
 *
 * Uses ZZ_WSP_ATR_Lookup_Mapping_Detail records where:
 *   - ZZ_Column_Letter         = main column letter on the sheet (A,B,C,...)
 *   - AD_Column_ID             = target AD_Column to populate
 *   - ZZ_Use_Value             = if Y, use Value when resolving references, else Name
 *   - ZZ_Create_If_Not_Exists  = if Y and column is Table/TableDir, create ref record if missing
 *   - ZZ_Value_Column_Letter   = optional letter for Value column (for create-if-missing)
 *   - ZZ_Name_Column_Letter    = optional letter for Name column (for create-if-missing)
 */
public class ColumnModeSheetImporter extends AbstractMappingSheetImporter {

	// Data usually starts at row 7 (Excel 1-based) => index 6 (0-based)
	private static final int DEFAULT_COMMIT_EVERY = 1000; // change as you like
	

	public ColumnModeSheetImporter(ReferenceLookupService refService,SvrProcess svrProcess) {
		super(refService,svrProcess);
	}

	@Override
	public int importData(Properties ctx,
			Workbook wb,
			X_ZZ_WSP_ATR_Submitted submitted,
			X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
			String trxName,
			ImportWspAtrDataFromTemplate process,
			DataFormatter formatter) throws IllegalStateException, SQLException {

		Sheet sheet = getSheetOrThrow(wb, mappingHeader); 
		List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> details =
				loadDetails(mappingHeader, trxName);

		if (details == null || details.isEmpty()) {
			return 0;
		}

		// Commit strategy
		final int commitEvery = DEFAULT_COMMIT_EVERY;

		// Precompute: columnIndex -> ColumnMeta
		Map<Integer, ColumnMeta> colIndexToMeta = new HashMap<>();


		for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
			String letter = det.getZZ_Column_Letter();
			if (Util.isEmpty(letter, true)) {
				continue;
			}
			int colIndex = columnLetterToIndex(letter);

			ColumnMeta meta = new ColumnMeta();
			meta.columnIndex = colIndex;
			meta.detail = det;

			int adColumnId = det.getAD_Column_ID();
			if (adColumnId <= 0) {
				throw new AdempiereException("Detail " + det.get_ID() + " has no AD_Column_ID set");
			}

			MColumn column = new MColumn(ctx, adColumnId, trxName);
			if (column.get_ID() <= 0) {
				throw new AdempiereException("AD_Column_ID " + adColumnId + " not found");
			}
			meta.column = column;

			meta.useValueForRef = det.isZZ_Use_Value();
			meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();

			String valueColLetter = det.getZZ_Value_Column_Letter();
			String nameColLetter  = det.getZZ_Name_Column_Letter();

			if (!Util.isEmpty(valueColLetter, true)) {
				meta.valueColumnIndex = columnLetterToIndex(valueColLetter);
			}
			if (!Util.isEmpty(nameColLetter, true)) {
				meta.nameColumnIndex = columnLetterToIndex(nameColLetter);
			}
			meta.mandatory = det.isMandatory(); 
			meta.ignoreIfBlank = det.isIgnore_If_Blank();


			colIndexToMeta.put(colIndex, meta);
		}


		int lastRow = sheet.getLastRowNum();
		int imported = 0;

		// Determine target table name once (needed for restartable check)
		String targetTableName = getTargetTableNameOrThrow(ctx, mappingHeader);
		int startRow = (mappingHeader.getStart_Row() == null) ? 0 : mappingHeader.getStart_Row().intValue();
		if (startRow <= 0) startRow = 4; // keep current default behavior

		int emptyRowsInARow = 0;
		for (int r = startRow; r <= lastRow; r++) {
			Row row = sheet.getRow(r);
			if (row == null)
				continue;

			// ✅ SINGLE rule: ignore empty rows
			if (isRowCompletelyEmpty(
					row,
					colIndexToMeta.keySet(),
					formatter,
					process.getEvaluator())) {
				emptyRowsInARow++;
				if (emptyRowsInARow > 10) {
					break;  // to many empty lines.  Assume the rest are empty
				}
				continue;
			}
			
			emptyRowsInARow = 0;

			// 2️⃣ Ignore rows based on Ignore_If_Blank
			if (shouldIgnoreRowBecauseOfIgnoreIfBlank(
					row,
					colIndexToMeta.values(),
					formatter,
					process.getEvaluator())) {
				continue;
			}
			if (isRowEmptyByMappedColumns(row, colIndexToMeta.keySet(), formatter,process))
				continue;

			if (isMissingMandatory(row, colIndexToMeta.values(), formatter,process)) {
				// optional log
			//	process.addLog("Skipping row " + (r + 1) + " - mandatory column missing (tab " + mappingHeader.getZZ_Tab_Name() + ")");
				continue;
			}

			// Excel-style row number (1-based). Change to (r) if you want 0-based.
			int sheetRowNo = r + 1;

			// Restartable: if a line already exists for this Submitted + Row_No, skip
			if (rowAlreadyImported(ctx, targetTableName, submitted.get_ID(), sheetRowNo, trxName)) {
				continue;
			}

			PO line = newTargetPO(ctx, submitted, mappingHeader, trxName);

			// Populate Row_No if the target table has it
			if (line.get_ColumnIndex("Row_No") >= 0) {
				line.set_ValueOfColumn("Row_No", sheetRowNo);
			}

			boolean skipRow = false;

			try {
				for (Map.Entry<Integer, ColumnMeta> entry : colIndexToMeta.entrySet()) {
					ColumnMeta meta = entry.getValue();
					int colIndex = meta.columnIndex;

					String mainText = getCellText(row, colIndex, formatter,process.getEvaluator());
					String valueText = null;
					String nameText  = null;

					boolean isRefColumn =
							meta.column.getAD_Reference_ID() == DisplayType.Table
							|| meta.column.getAD_Reference_ID() == DisplayType.TableDir
							|| meta.column.getAD_Reference_ID() == DisplayType.Search;

					if (meta.createIfNotExist && isRefColumn) {
						if (meta.valueColumnIndex != null) {
							valueText = getCellText(row, meta.valueColumnIndex, formatter,process.getEvaluator());
						}
						if (meta.nameColumnIndex != null) {
							nameText = getCellText(row, meta.nameColumnIndex, formatter,process.getEvaluator());
						}

						if (Util.isEmpty(mainText, true)
								&& Util.isEmpty(valueText, true)
								&& Util.isEmpty(nameText, true)) {
							continue;
						}

						setValueFromText(ctx,
								line,
								meta,
								mainText,
								true,
								valueText,
								nameText,
								trxName);

					} else {
						if (Util.isEmpty(mainText, true))
							continue;

						setValueFromTextMandatoryRefAware(ctx, line, meta, mainText, trxName);
					}
				} 
			}catch (SkipRowException e) {
				skipRow = true;
		//		process.addLog("Skipping row " + (r + 1) + " - " + e.getMessage()
		//		+ " (tab " + mappingHeader.getZZ_Tab_Name() + ")");
			}

			if (skipRow) {
				continue; // ignore entire line
			}

			
			try {
			    line.saveEx();
			    imported++;

			    if (commitEvery > 0 && (imported % commitEvery) == 0) {
			        DB.commit(true, trxName);
			    }

			} catch (Exception ex) {
			    // IMPORTANT: rollback resets aborted transaction state in PostgreSQL
			    DB.rollback(true, trxName);

			    process.addLog("ERROR row " + (r + 1)
			        + " (tab " + mappingHeader.getZZ_Tab_Name() + "): "
			        + ex.getClass().getSimpleName() + " - " + ex.getMessage());

			    // optional: also log stacktrace to server log
			    log.log(Level.SEVERE,
			        "Import failed at row " + (r + 1) + ", tab " + mappingHeader.getZZ_Tab_Name(),
			        ex);

			    // Decide: skip row and continue OR rethrow to stop whole import
			    continue;
			}

			
		}

		// Final commit at end (safe)
		
		try {
		    DB.commit(true, trxName);
		} catch (Exception ex) {
		    DB.rollback(true, trxName);
		    log.log(Level.SEVERE, "Final commit failed for tab " + mappingHeader.getZZ_Tab_Name(), ex);
		    throw ex; // or return imported if you want partial success
		}

	//	process.addLog("Imported " + imported + " rows from tab " + mappingHeader.getZZ_Tab_Name());
		return imported;
	}

	private String getTargetTableNameOrThrow(Properties ctx, X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader) {
		int adTableId = mappingHeader.getAD_Table_ID(); // assumes your header has AD_Table_ID
		if (adTableId <= 0) {
			throw new AdempiereException("Mapping header " + mappingHeader.get_ID() + " has no AD_Table_ID set");
		}
		MTable t = MTable.get(ctx, adTableId);
		if (t == null || t.getAD_Table_ID() <= 0) {
			throw new AdempiereException("Target AD_Table_ID " + adTableId + " not found");
		}
		return t.getTableName();
	}

	/**
	 * Restartable check: row already imported if a record exists for:
	 *   Submitted_ID + Row_No
	 */
	private boolean rowAlreadyImported(Properties ctx, String tableName, int submittedId, int rowNo, String trxName) {
		// Assumes both columns exist on the target table:
		//   ZZ_WSP_ATR_Submitted_ID
		//   Row_No
		String where = "ZZ_WSP_ATR_Submitted_ID=? AND Row_No=?";
		int existingId = new Query(ctx, tableName, where, trxName)
				.setParameters(submittedId, rowNo)
				.firstId();
		return existingId > 0;
	}

	/**
	 * Extended version:
	 *  - If NOT a Table/TableDir, or createIfNotExist=false, just delegates to the old method.
	 *  - If Table/TableDir AND createIfNotExist=true:
	 *      * tries to find the record in the reference table using Value or Name
	 *      * if not found, creates it (using refValue/refName/mainText)
	 *      * then sets the FK on the PO
	 */

	protected void setValueFromText(Properties ctx,
			PO po,
			ColumnMeta meta,
			String text,
			boolean createIfNotExist,
			String refValue,
			String refName,
			String trxName) {
		MColumn column = meta.column;
		boolean useValueForRef = meta.useValueForRef;

		int displayType = column.getAD_Reference_ID();

		// If not a reference or we don't want to create, just delegate to existing method
		if (displayType != DisplayType.Table
				&& displayType != DisplayType.TableDir
				&& displayType != DisplayType.Search) {
			// normal numeric/string handling, including references handled by old logic
			setValueFromText(ctx, po, column, text, useValueForRef, trxName);
			return;
		}

		if (!createIfNotExist) {
			// Old behaviour for Table/TableDir
			setValueFromText(ctx, po, column, text, useValueForRef, trxName);
			return;
		}

		// --- From here: Table/TableDir + createIfNotExist = true ---

		// If absolutely nothing is supplied, just skip
		boolean noMain = Util.isEmpty(text, true);
		boolean noVal  = Util.isEmpty(refValue, true);
		boolean noName = Util.isEmpty(refName, true);
		if (noMain && noVal && noName) {
			return;
		}

		// Clean up texts
		String mainText = noMain ? null : text.trim();
		String valueText = noVal ? null : refValue.trim();
		String nameText  = noName ? null : refName.trim();

		// Decide what we will use as "Value" and "Name" for lookup/create
		String valueToUse = valueText;
		String nameToUse  = nameText;

		if (valueToUse == null && useValueForRef && mainText != null) {
			valueToUse = mainText;
		}
		if (nameToUse == null && !useValueForRef && mainText != null) {
			nameToUse = mainText;
		}

		// If still nothing for either, fallback: use mainText as Name
		if (valueToUse == null && nameToUse == null && mainText != null) {
			nameToUse = mainText;
		}

		// We need the reference table from AD_Reference_Value_ID
		int adRefTableId = column.getAD_Reference_Value_ID();
		if (adRefTableId <= 0) {
			// No table reference definition, fallback to old behaviour
			setValueFromText(ctx, po, column, text, useValueForRef, trxName);
			return;
		}

		MRefTable refTableCfg = MRefTable.get(ctx, adRefTableId, trxName);
		if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0) {
			setValueFromText(ctx, po, column, text, useValueForRef, trxName);
			return;
		}

		MTable refTable = MTable.get(ctx, refTableCfg.getAD_Table_ID());
		if (refTable == null || refTable.getAD_Table_ID() <= 0) {
			setValueFromText(ctx, po, column, text, useValueForRef, trxName);
			return;
		}

		String refTableName = refTable.getTableName();

		// 1) Try to find an existing record

		Integer foundId = null;

		// Prefer Value if we have it and ZZ_Use_Value is true
		if (useValueForRef && !Util.isEmpty(valueToUse, true)) {
			foundId = findIdByColumn(ctx, refTableName, "Value", valueToUse, trxName);
		}

		// If not found and we have a Name, try Name
		//if ((foundId == null || foundId <= 0) && !Util.isEmpty(nameToUse, true)) {
		//     foundId = findIdByColumn(ctx, refTableName, "Name", nameToUse, trxName);
		//  }

		// If not found and we still have mainText, try Name=mainText
		if ((foundId == null || foundId <= 0) && mainText != null) {
			foundId = findIdByColumn(ctx, refTableName, "Name", mainText, trxName);
		}

		if (foundId != null && foundId > 0) {
			po.set_ValueOfColumn(column.getColumnName(), foundId);
			return;
		}

		// 2) Not found: create new record in the reference table

		if (valueToUse == null && nameToUse == null && mainText == null) {
			throw new AdempiereException("Cannot create reference record; no Value/Name/main text provided");
		}

		// Enforce: if a Name column is configured, it must have a value to create


		if (meta.nameColumnIndex != null && Util.isEmpty(nameText, true)) {
			if (meta.mandatory) {
				throw new SkipRowException("Mandatory ref cannot be created (Name empty) for column "
						+ column.getColumnName());
			}
			if (svrProcess != null) {
				//svrProcess.addLog("Skipping create in " + refTableName
					//	+ " for column " + column.getColumnName()
					//	+ " - Name column is configured but empty. Sheet value='" + (mainText != null ? mainText : "") + "'");
			}
			return;
		}



		PO refPO = refTable.getPO(0, trxName);
		if (refPO == null) {
			throw new AdempiereException("Cannot create reference record for table " + refTableName);
		}

		// Ensure we always have *something* for Name
		if (nameToUse == null && meta.nameColumnIndex == null) {
			if (valueToUse != null) {
				nameToUse = valueToUse;
			} else if (mainText != null) {
				nameToUse = mainText;
			}
		}

		if ((meta.valueColumnIndex == null || meta.valueColumnIndex < 0)
				&& !meta.useValueForRef) {
			valueToUse = getNextAddedValue(refTableName, trxName);
		}
		// For Value, if we don't have anything, we can reuse Name
		if (valueToUse == null && nameToUse != null) {
			valueToUse = nameToUse;
		}
		// Set standard fields if those columns exist
		if (refTable.getColumn("Value") != null && valueToUse != null) {
			refPO.set_ValueOfColumn("Value", valueToUse);
		}
		if (refTable.getColumn("Name") != null && nameToUse != null) {
			refPO.set_ValueOfColumn("Name", nameToUse);
		}

		// If the table has EntityType, default to 'U' (User Maintained)
		if (refTable.getColumn("EntityType") != null) {
			refPO.set_ValueOfColumn("EntityType", "U");
		}

		refPO.saveEx();
		int newId = refPO.get_ID();
		if (newId <= 0) {
			throw new AdempiereException("Failed to create reference record in " + refTableName);
		}

		po.set_ValueOfColumn(column.getColumnName(), newId);
	}

	private String getNextAddedValue(String tableName, String trxName) {
		// Extract numeric part after 'ADDED_'
		final String sql =
				"SELECT MAX(CAST(SUBSTRING(Value, 7) AS INTEGER)) " +
						"FROM " + tableName + " " +
						"WHERE Value LIKE 'ADDED_%'";

		int max = DB.getSQLValueEx(trxName, sql);

		int next = max > 0 ? max + 1 : 1;

		return String.format("ADDED_%05d", next);
	}

	private void setValueFromTextMandatoryRefAware(Properties ctx, PO po, ColumnMeta meta, String text, String trxName) {
		MColumn column = meta.column;

		boolean isRefColumn =
				column.getAD_Reference_ID() == DisplayType.Table
				|| column.getAD_Reference_ID() == DisplayType.TableDir
				|| column.getAD_Reference_ID() == DisplayType.Search;

		if (!isRefColumn) {
			// non-ref -> use existing logic
			setValueFromText(ctx, po, column, text, meta.useValueForRef, trxName);
			return;
		}

		// If not mandatory, keep old behaviour
		if (!meta.mandatory) {
			setValueFromText(ctx, po, column, text, meta.useValueForRef, trxName);
			return;
		}

		// Mandatory ref: try resolve FIRST. If cannot resolve -> skip entire row.
		Integer id = tryResolveRefId(ctx, column, text, meta.useValueForRef, trxName);
		if (id == null || id <= 0) {
			throw new SkipRowException("Mandatory reference not found for column "
					+ column.getColumnName() + " value='" + text + "'");
		}

		po.set_ValueOfColumn(column.getColumnName(), id);
	}




	/**
	 * Check if a row is effectively empty for all mapped main columns.
	 */
	private boolean isRowEmptyByMappedColumns(Row row,
			Iterable<Integer> colIndexes,
			DataFormatter formatter,
			ImportWspAtrDataFromTemplate process) {
		for (Integer colIndex : colIndexes) {
			String txt = getCellText(row, colIndex, formatter,process.getEvaluator());
			if (!Util.isEmpty(txt, true))
				return false;
		}
		return true;
	}

	private boolean isMissingMandatory(Row row,
			Iterable<ColumnMeta> metas,
			DataFormatter formatter,
			ImportWspAtrDataFromTemplate process) {
		for (ColumnMeta meta : metas) {
			if (!meta.mandatory)
				continue;

			String txt = getCellText(row, meta.columnIndex, formatter,process.getEvaluator());
			if (Util.isEmpty(txt, true)) {
				return true;
			}
		}
		return false;
	}




	private static class SkipRowException extends RuntimeException {
		SkipRowException(String msg) { super(msg); }
	}

}

