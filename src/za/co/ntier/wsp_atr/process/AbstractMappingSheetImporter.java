package za.co.ntier.wsp_atr.process;

import java.math.BigDecimal;
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

	protected String getCellText(Row row,
			int colIndex,
			DataFormatter formatter,
			FormulaEvaluator evaluator) {
		if (row == null)
			return "";

		Cell cell = row.getCell(colIndex);
		if (cell == null)
			return "";

		try {
			// IMPORTANT: this evaluates formulas and returns the displayed result
			String value = formatter.formatCellValue(cell, evaluator);
			return value != null ? value.trim() : "";
		} catch (Exception e) {
			// Fallbacks (very defensive)
			try {
				CellType type = cell.getCellType();

				if (type == CellType.FORMULA && evaluator != null) {
					type = evaluator.evaluateFormulaCell(cell);
				}

				switch (type) {
				case STRING:
					return cell.getStringCellValue().trim();
				case NUMERIC:
					return String.valueOf(cell.getNumericCellValue());
				case BOOLEAN:
					return String.valueOf(cell.getBooleanCellValue());
				default:
					return "";
				}
			} catch (Exception ignore) {
				return "";
			}
		}
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

	/**
	 * Set a value into the target PO based on the column definition and text from Excel.
	 * Supports numeric, String and Table references.
	 */
	protected void setValueFromText(Properties ctx,
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
				// throw new org.adempiere.exceptions.AdempiereException(
				//        "No reference record found for text '" + text
				//               + "' in column " + colName);
				svrProcess.addLog(po.get_TableName() + " - No reference record found for text '" + text
						+ "' in column " + colName);
				return;

			}
			po.set_ValueOfColumn(colName, id);
		} else {
			int len = column.getFieldLength();
			po.set_ValueOfColumn(colName, truncate(text, len > 0 ? len : 200));
		}
	}
	
	protected Integer tryResolveRefId(Properties ctx, MColumn column, String text, boolean useValueForRef, String trxName) {
	    if (Util.isEmpty(text, true))
	        return null;

	    int adRefTableId = column.getAD_Reference_Value_ID();
	    if (adRefTableId <= 0)
	        return null;

	    MRefTable refTableCfg = MRefTable.get(ctx, adRefTableId, trxName);
	    if (refTableCfg == null || refTableCfg.getAD_Table_ID() <= 0)
	        return null;

	    MTable refTable = MTable.get(ctx, refTableCfg.getAD_Table_ID());
	    if (refTable == null || refTable.getAD_Table_ID() <= 0)
	        return null;

	    String refTableName = refTable.getTableName();
	    String trimmed = text.trim();

	    Integer foundId;
	    if (useValueForRef) {
	        foundId = findIdByColumn(ctx, refTableName, "Value", trimmed, trxName);
	        if (foundId == null || foundId <= 0)
	            foundId = findIdByColumn(ctx, refTableName, "Name", trimmed, trxName);
	    } else {
	        foundId = findIdByColumn(ctx, refTableName, "Name", trimmed, trxName);
	        if (foundId == null || foundId <= 0)
	            foundId = findIdByColumn(ctx, refTableName, "Value", trimmed, trxName);
	    }

	    return foundId;
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
	        Iterable<Integer> colIndexes,
	        DataFormatter formatter,
	        FormulaEvaluator evaluator) {

	    for (Integer colIndex : colIndexes) {
	        if (colIndex == null || colIndex < 0)
	            continue;

	        String txt = getCellText(row, colIndex, formatter, evaluator);
	        if (!isBlankOrZero(txt)) {
	            return false; // found real data → row is NOT empty
	        }
	    }
	    return true; // all mapped cols empty / zero
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
	        DataFormatter formatter,
	        FormulaEvaluator evaluator) {

	    for (ColumnMeta meta : metas) {
	        if (!meta.ignoreIfBlank)
	            continue;

	        String txt = getCellText(row, meta.columnIndex, formatter, evaluator);

	        if (isBlankOrZero(txt)) {
	            return true; // 🔴 ignore entire row
	        }
	    }
	    return false;
	}
	
	protected static class ColumnMeta {
		int columnIndex;
		X_ZZ_WSP_ATR_Lookup_Mapping_Detail detail;
		MColumn column;
		boolean useValueForRef;

		// create-if-missing support
		boolean createIfNotExist;
		Integer valueColumnIndex; // may be null
		Integer nameColumnIndex;  // may be null
		boolean mandatory; // if text is empty then ignore entire row.
		boolean ignoreIfBlank;
		String headerName; // from mapping detail (Header Name)
	}


}

