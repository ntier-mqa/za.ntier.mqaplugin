package za.co.ntier.wsp_atr.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
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
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

public abstract class AbstractMappingSheetImporter implements IWspAtrSheetImporter {

	protected final ReferenceLookupService refService;
	protected final SvrProcess svrProcess;
	
	CLogger			log = null;


	public CLogger getLog() {
		return log;
	}

	public void setLog(CLogger log) {
		this.log = log;
	}

	protected AbstractMappingSheetImporter(ReferenceLookupService refService,SvrProcess svrProcess) {
		this.refService = refService;
		this.svrProcess = svrProcess;
	}

	// ---------- common helpers ----------

	protected Sheet getSheetOrThrow(Workbook wb, X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader) {
		String sheetName = mappingHeader.getZZ_Tab_Name();
		Sheet sheet = wb.getSheet(sheetName);
		if (sheet == null) {
			throw new org.adempiere.exceptions.AdempiereException(
					"Sheet '" + sheetName + "' not found in workbook");

		}
		return sheet;
	}

	protected List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> loadDetails(X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
			String trxName) {
		return new Query(Env.getCtx(),
				X_ZZ_WSP_ATR_Lookup_Mapping_Detail.Table_Name,
				"ZZ_WSP_ATR_Lookup_Mapping_ID=?",
				trxName)
				.setParameters(mappingHeader.getZZ_WSP_ATR_Lookup_Mapping_ID())
				.setOnlyActiveRecords(true)
				.setOrderBy(X_ZZ_WSP_ATR_Lookup_Mapping_Detail.COLUMNNAME_ZZ_Column_Letter)
				.list();
	}

	protected PO newTargetPO(Properties ctx,
			X_ZZ_WSP_ATR_Submitted submitted,
			X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
			String trxName) {

		int adTableId = mappingHeader.getAD_Table_ID();
		if (adTableId <= 0) {
			return null;
			//// throw new org.adempiere.exceptions.AdempiereException(
			//        "Mapping header " + mappingHeader.get_ID()
			//               + " has no AD_Table_ID");
		}

		MTable table = MTable.get(ctx, adTableId);
		PO po = table.getPO(0, trxName);

		// attach header if column exists
		int idx = po.get_ColumnIndex("ZZ_WSP_ATR_Submitted_ID");
		if (idx >= 0) {
			po.set_ValueOfColumn("ZZ_WSP_ATR_Submitted_ID",
					submitted.getZZ_WSP_ATR_Submitted_ID());
		}

		return po;
	}

	
	
	protected String getCellText(Row row, int columnIndex, DataFormatter formatter) {

	    Cell cell = row.getCell(columnIndex);
	    if (cell == null)
	        return "";

	    if (cell.getCellType() == CellType.FORMULA) {
	        switch (cell.getCachedFormulaResultType()) {

	            case STRING:
	                return cell.getStringCellValue();

	            case NUMERIC:
	                return formatter.formatRawCellContents(
	                        cell.getNumericCellValue(),
	                        -1,
	                        cell.getCellStyle().getDataFormatString());

	            case BOOLEAN:
	                return String.valueOf(cell.getBooleanCellValue());

	            default:
	                return "";
	        }
	    }

	    return formatter.formatCellValue(cell);
	}



	protected BigDecimal parseBigDecimal(String txt) {
		if (Util.isEmpty(txt, true))
			return null;
		try {
			return new BigDecimal(txt.trim());
		} catch (Exception e) {
			return null;
		}
	}

	protected String truncate(String s, int max) {
		if (s == null)
			return null;
		s = s.trim();
		if (max > 0 && s.length() > max)
			return s.substring(0, max);
		return s;
	}

	protected int columnLetterToIndex(String letter) {
		if (Util.isEmpty(letter, true))
			return -1;

		letter = letter.trim().toUpperCase();
		int result = 0;
		for (int i = 0; i < letter.length(); i++) {
			char c = letter.charAt(i);
			if (c < 'A' || c > 'Z')
				throw new IllegalArgumentException("Invalid column letter: " + letter);
			result = result * 26 + (c - 'A' + 1);
		}
		return result - 1; // zero-based
	}

	protected String columnIndexToLetter(int index) {
		StringBuilder sb = new StringBuilder();
		int n = index + 1; // convert to 1-based
		while (n > 0) {
			int rem = (n - 1) % 26;
			sb.insert(0, (char) ('A' + rem));
			n = (n - 1) / 26;
		}
		return sb.toString();
	}

	/**
	 * Set a value into the target PO based on the column definition and text from Excel.
	 * Supports numeric, String and Table references.
	 * Returns null on success, or an error message if a reference could not be resolved.
	 */
	protected String setValueFromText(Properties ctx,
			PO po,
			MColumn column,
			String text,
			boolean useValueForRef,
			String trxName) {

		String colName = column.getColumnName();
		int displayType = column.getAD_Reference_ID();

		if (DisplayType.isNumeric(displayType)) {
			BigDecimal bd = parseBigDecimal(text);
			po.set_ValueOfColumn(colName, bd);
		} else if (displayType == DisplayType.Table || displayType == DisplayType.TableDir
				|| displayType == DisplayType.Search) {
			Integer id = refService.lookupReferenceId(ctx, column, text, useValueForRef, trxName);
			if (id == null) {
				for (String candidate : buildCandidates(text)) {
					id = refService.lookupReferenceId(ctx, column, candidate, useValueForRef, trxName);
					if (id != null)
						break;
				}
			}
			if (id == null) {
				return "No reference record found for value '" + text + "' in column " + colName;
			}
			po.set_ValueOfColumn(colName, id);
		} else {
			int len = column.getFieldLength();
			po.set_ValueOfColumn(colName, truncate(text, len > 0 ? len : 200));
		}
		return null;
	}
	
	protected Integer tryResolveRefId(Properties ctx, MColumn column, String text, boolean useValueForRef, String trxName) {
		if (Util.isEmpty(text, true))
			return null;

		String refTableName = resolveRefTableName(ctx, column, trxName);
		if (refTableName == null)
			return null;

		// Build candidate list: original text first, then variants (plural/singular, underscore-normalised)
		List<String> allCandidates = new ArrayList<>();
		allCandidates.add(text.trim());
		allCandidates.addAll(buildCandidates(text.trim()));

		for (String candidate : allCandidates) {
			Integer foundId;
			if (useValueForRef) {
				foundId = findIdByColumn(ctx, refTableName, "Value", candidate, trxName);
				if (foundId == null || foundId <= 0)
					foundId = findIdByColumn(ctx, refTableName, "Name", candidate, trxName);
			} else {
				foundId = findIdByColumn(ctx, refTableName, "Name", candidate, trxName);
				if (foundId == null || foundId <= 0)
					foundId = findIdByColumn(ctx, refTableName, "Value", candidate, trxName);
			}
			if (foundId != null && foundId > 0)
				return foundId;
		}

		return null;
	}

	private String resolveRefTableName(Properties ctx, MColumn column, String trxName) {
		int adRefValueId = column.getAD_Reference_Value_ID();
		if (adRefValueId > 0) {
			MRefTable refTableCfg = MRefTable.get(ctx, adRefValueId, trxName);
			if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0)
				return null;
			MTable refTable = MTable.get(ctx, refTableCfg.getAD_Table_ID());
			if (refTable == null || refTable.getAD_Table_ID() <= 0)
				return null;
			return refTable.getTableName();
		}
		// TableDir: find the table whose primary key column matches this column name
		if (column.getAD_Reference_ID() == DisplayType.TableDir) {
			String tableName = DB.getSQLValueStringEx(trxName,
					"SELECT t.TableName FROM AD_Table t "
					+ "JOIN AD_Column c ON c.AD_Table_ID = t.AD_Table_ID "
					+ "WHERE c.ColumnName = ? AND c.IsKey = 'Y' AND t.IsActive = 'Y' "
					+ "ORDER BY t.TableName FETCH FIRST 1 ROWS ONLY",
					column.getColumnName());
			return Util.isEmpty(tableName, true) ? null : tableName;
		}
		return null;
	}

	private List<String> buildCandidates(String text) {
		List<String> candidates = new ArrayList<>();
		addWithPluralVariant(candidates, text);
		if (text.contains("_")) {
			String normalized = text.replace('_', ' ');
			addWithPluralVariant(candidates, normalized);
		}
		candidates.remove(text); // first candidate is the original — already tried by caller
		return candidates;
	}

	private void addWithPluralVariant(List<String> candidates, String text) {
		if (!candidates.contains(text))
			candidates.add(text);
		String lower = text.toLowerCase();
		if (lower.endsWith("s")) {
			String singular = text.substring(0, text.length() - 1);
			if (!candidates.contains(singular))
				candidates.add(singular);
		} else {
			String plural = text + "s";
			if (!candidates.contains(plural))
				candidates.add(plural);
		}
	}

	/**
	 * Sets a reference FK column on the PO, creating the reference record in the target
	 * table when it does not already exist.
	 *
	 * <p>valueText / nameText come from optional separate columns on the sheet
	 * (configured via ZZ_Value_Column_Letter / ZZ_Name_Column_Letter on the mapping detail).
	 * When those are absent, mainText is used as both Value and Name.</p>
	 *
	 * <p>Falls back to the plain lookup path when the column is not a reference type or
	 * {@code meta.createIfNotExist} is false.</p>
	 */
	protected void setValueFromTextOrCreate(Properties ctx,
			PO po,
			ColumnMeta meta,
			String mainText,
			String valueText,
			String nameText,
			String trxName) {

		MColumn column = meta.column;
		int displayType = column.getAD_Reference_ID();

		boolean isRef = displayType == DisplayType.Table
				|| displayType == DisplayType.TableDir
				|| displayType == DisplayType.Search;

		if (!isRef || !meta.createIfNotExist) {
			setValueFromText(ctx, po, column, mainText, meta.useValueForRef, trxName);
			return;
		}

		boolean noMain  = Util.isEmpty(mainText,  true);
		boolean noVal   = Util.isEmpty(valueText,  true);
		boolean noName  = Util.isEmpty(nameText,   true);
		if (noMain && noVal && noName) {
			return;
		}

		String cleanMain  = noMain  ? null : mainText.trim();
		String cleanValue = noVal   ? null : valueText.trim();
		String cleanName  = noName  ? null : nameText.trim();

		// Decide which text maps to Value / Name for lookup and creation
		String valueToUse = cleanValue;
		String nameToUse  = cleanName;
		if (valueToUse == null && meta.useValueForRef && cleanMain != null) {
			valueToUse = cleanMain;
		}
		if (nameToUse == null && !meta.useValueForRef && cleanMain != null) {
			nameToUse = cleanMain;
		}
		if (valueToUse == null && nameToUse == null && cleanMain != null) {
			nameToUse = cleanMain;
		}

		// Resolve the reference table from the column definition
		int adRefTableId = column.getAD_Reference_Value_ID();
		if (adRefTableId <= 0) {
			setValueFromText(ctx, po, column, mainText, meta.useValueForRef, trxName);
			return;
		}
		MRefTable refTableCfg = MRefTable.get(ctx, adRefTableId, trxName);
		if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0) {
			setValueFromText(ctx, po, column, mainText, meta.useValueForRef, trxName);
			return;
		}
		MTable refTable = MTable.get(ctx, refTableCfg.getAD_Table_ID());
		if (refTable == null || refTable.getAD_Table_ID() <= 0) {
			setValueFromText(ctx, po, column, mainText, meta.useValueForRef, trxName);
			return;
		}
		String refTableName = refTable.getTableName();

		// 1) Try to find an existing record by Value, then by Name / mainText
		Integer foundId = null;
		if (meta.useValueForRef && !Util.isEmpty(valueToUse, true)) {
			foundId = findIdByColumn(ctx, refTableName, "Value", valueToUse, trxName);
		}
		if ((foundId == null || foundId <= 0) && cleanMain != null) {
			foundId = findIdByColumn(ctx, refTableName, "Name", cleanMain, trxName);
		}
		if (foundId != null && foundId > 0) {
			po.set_ValueOfColumn(column.getColumnName(), foundId);
			return;
		}

		// 2) Not found — create a new record in the reference table
		if (meta.nameColumnIndex != null && Util.isEmpty(nameText, true)) {
			// Name column is configured but blank — silently skip rather than create a bad record
			return;
		}

		PO refPO = refTable.getPO(0, trxName);
		if (refPO == null) {
			throw new org.adempiere.exceptions.AdempiereException(
					"Cannot create reference record for table " + refTableName);
		}

		// Ensure we have something for Name
		if (nameToUse == null && meta.nameColumnIndex == null) {
			nameToUse = valueToUse != null ? valueToUse : cleanMain;
		}
		// Auto-generate a Value when no explicit value column is configured
		if ((meta.valueColumnIndex == null || meta.valueColumnIndex < 0) && !meta.useValueForRef) {
			valueToUse = getNextAddedValue(refTableName, trxName);
		}
		if (valueToUse == null && nameToUse != null) {
			valueToUse = nameToUse;
		}

		if (refTable.getColumn("Value") != null && valueToUse != null) {
			refPO.set_ValueOfColumn("Value", valueToUse);
		}
		if (refTable.getColumn("Name") != null && nameToUse != null) {
			refPO.set_ValueOfColumn("Name", nameToUse);
		}
		if (refTable.getColumn("EntityType") != null) {
			refPO.set_ValueOfColumn("EntityType", "U");
		}

		refPO.saveEx();
		int newId = refPO.get_ID();
		if (newId <= 0) {
			throw new org.adempiere.exceptions.AdempiereException(
					"Failed to create reference record in " + refTableName);
		}
		po.set_ValueOfColumn(column.getColumnName(), newId);
	}

	/**
	 * Returns a sequenced placeholder Value for newly-created reference records
	 * when no explicit Value text is available (e.g. "ADDED_00001").
	 */
	protected String getNextAddedValue(String tableName, String trxName) {
		int max = DB.getSQLValueEx(trxName,
				"SELECT MAX(CAST(SUBSTRING(Value, 7) AS INTEGER)) "
				+ "FROM " + tableName + " WHERE Value LIKE 'ADDED_%'");
		int next = max > 0 ? max + 1 : 1;
		return String.format("ADDED_%05d", next);
	}

	protected Integer findIdByColumn(Properties ctx, String tableName, String columnName, String text, String trxName) {
		if (Util.isEmpty(text, true))
			return null;

		String where = "UPPER(TRIM(" + columnName + "))=UPPER(?)";
		int id = new Query(ctx, tableName, where, trxName)
				.setParameters(text.trim())
				.firstId();

		return (id > 0) ? id : null;
	}
	
	
	protected boolean isRowCompletelyEmpty(
	        Row row,
	        Iterable<ColumnMeta> metas) {

	    for (ColumnMeta meta : metas) {

	        Cell cell = row.getCell(meta.columnIndex);

	        if (cell == null) {
	            continue;
	        }

	        CellType type = cell.getCellType();

	        // Use cached formula result, do not recalculate
	        if (type == CellType.FORMULA) {
	            type = cell.getCachedFormulaResultType();
	        }

	        switch (type) {
	        case STRING:
	            if (!cell.getStringCellValue().trim().isEmpty()) {
	                return false;
	            }
	            break;

	        case NUMERIC:
	            if (cell.getNumericCellValue() != 0) {
	                return false;
	            }
	            break;

	        case BOOLEAN:
	            if (cell.getBooleanCellValue()) {
	                return false;
	            }
	            break;

	        case BLANK:
	        default:
	            break;
	        }
	    }

	    return true;
	}

	protected boolean isBlankOrZero(String txt) {
		if (txt == null)
			return true;

		String s = txt.trim();
		if (s.isEmpty())
			return true;

		// normalize numeric-looking strings
		s = s.replace(" ", "").replace(",", "");

		try {
			return new java.math.BigDecimal(s)
					.compareTo(java.math.BigDecimal.ZERO) == 0;
		} catch (Exception ignore) {
			return false; // non-numeric non-empty text
		}
	}

	protected boolean shouldIgnoreRowBecauseOfIgnoreIfBlank(
			Row row,
			Iterable<ColumnMeta> metas,
			DataFormatter formatter) {

		for (ColumnMeta meta : metas) {
			if (!meta.ignoreIfBlank)
				continue;

			String txt = getCellText(row, meta.columnIndex, formatter);

			if (isBlankOrZero(txt)) {
				return true; // 🔴 ignore entire row
			}
		}
		return false;
	}

	protected static class ColumnMeta {
		int columnIndex;
		//X_ZZ_WSP_ATR_Lookup_Mapping_Detail detail;
		MColumn column;
		boolean useValueForRef;
		boolean isFormular;

		// create-if-missing support
		boolean createIfNotExist;
		Integer valueColumnIndex; // may be null
		Integer nameColumnIndex;  // may be null
		boolean mandatory; // if text is empty then ignore entire row.
		boolean ignoreIfBlank;
		String headerName; // from mapping detail (Header Name)
	}


}

