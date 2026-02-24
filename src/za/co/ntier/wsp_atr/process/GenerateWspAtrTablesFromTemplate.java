package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRefTable;
import org.compiere.model.MReference;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.model.X_AD_Reference;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.I_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;

/**
 * GenerateWspAtrTablesFromTemplate
 *
 * - Reads the WSP/ATR XLSM template (parameter FileName).
 * - For each selected sheet (8 logical tabs):
 *      * Reads column headers / structures.
 *      * Uses ZZ_WSP_ATR_Lookup_Mapping + Detail to find reference tables and
 *        whether the sheet stores Name or Value.
 *      * Infers data type (String vs Number) where no mapping.
 * - Creates:
 *      * Header table: ZZ_WSP_ATR_Submitted
 *      * One detail table per selected tab:
 *          ZZ_WSP_ATR_<NormalizedTabName>_Detail
 *   OR, in PreviewOnly mode, just logs the table + column definitions.
 *
 * Special sheet: "Finance and Training Comparison"
 *   -> each row (from row 8) has header in column B and value in column C.
 *      We create generic columns: Header (String) and Value (Number/String).
 */
@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.GenerateWspAtrTablesFromTemplate")
public class GenerateWspAtrTablesFromTemplate extends SvrProcess {

	private static final CLogger log = CLogger.getCLogger(GenerateWspAtrTablesFromTemplate.class);

	// ----------------------------------------------------------------------
	// Parameters
	// ----------------------------------------------------------------------

	@Parameter(name = "FileName")
	private String filePath;

	@Parameter(name = "PreviewOnly")
	private boolean previewOnly;

	@Parameter(name = "Tab_Biodata")
	private boolean pBiodata;

	@Parameter(name = "Tab_ATR")
	private boolean pATR;

	@Parameter(name = "Tab_WSP")
	private boolean pWSP;

	@Parameter(name = "Tab_HTFV")
	private boolean pHTFV;

	@Parameter(name = "Tab_Topup_Skills_Survey")
	private boolean pTopupSkills;

	@Parameter(name = "Tab_NonEmployees_CommunityTraining")
	private boolean pNonEmployees;

	@Parameter(name = "Tab_Contractors")
	private boolean pContractors;

	@Parameter(name = "Tab_Finance_Training_Comparison")
	private boolean pFinanceTraining;

	// ----------------------------------------------------------------------
	// Internal helper types
	// ----------------------------------------------------------------------

	private static class LookupDef {
		int adTableId;           // Reference table AD_Table_ID (0 = none)
		String refTableName;     // Reference table name (if any)
		boolean useValue;        // true = sheet stores Value, false = Name

		@Override
		public String toString() {
			return "LookupDef{table=" + refTableName + ", useValue=" + useValue + "}";
		}
	}

	private enum ColumnKind {
		PLAIN_STRING,
		PLAIN_NUMBER,
		REF_ID
	}

	private static class ColumnDef {
		String headerText;       // Raw header from sheet
		String columnName;       // Normalized AD_Column.ColumnName
		ColumnKind kind;
		LookupDef lookup;        // non-null for REF_ID
		int displayType;         // DisplayType.*
		int fieldLength;         // for String types

		// --- new fields for reference preview ---
		Integer ref_AD_Reference_ID;  // ID of AD_Reference if exists
		String refName;               // desired reference name (e.g. ZZ_Gender_Ref_ID)
		boolean refExists;            // true if reference already exists

		@Override
		public String toString() {
			return columnName + " (" + kind + ", " + displayType +
					(lookup != null ? ", ref=" + lookup.refTableName : "") + ")";
		}
	}


	private static class TabDef {
		String sheetName;              // e.g. "Biodata"
		String adTableName;            // e.g. "ZZ_WSP_ATR_Biodata_Detail"
		String adTableDesc;            // description
		List<ColumnDef> columns = new ArrayList<>();
	}

	// Expected logical sheet names (exact, case-sensitive)
	private static final String SHEET_BIODATA  = "Biodata";
	private static final String SHEET_ATR      = "ATR";
	private static final String SHEET_WSP      = "WSP";
	private static final String SHEET_HTFV     = "HTFV";
	private static final String SHEET_TOPUP    = "Top-up Skills Survey";
	private static final String SHEET_NONEMP   = "Non-Employees-Community Training";
	private static final String SHEET_CONTR    = "Contractors";
	private static final String SHEET_FINANCE  = "Finance and Training Comparison";

	private Workbook workbook;
	private DataFormatter formatter;
	private int adClientId;
	private String trxName;
	private org.apache.poi.ss.usermodel.FormulaEvaluator evaluator;


	@Override
	protected void prepare() {
		trxName = get_TrxName();
		for (ProcessInfoParameter p : getParameter()) {
			// All parameters are injected by @Parameter; this just validates unknowns
			MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
		}
	}

	@Override
	protected String doIt() throws Exception {

		if (Util.isEmpty(filePath, true)) {
			throw new AdempiereException("FileName parameter is empty. Please select the XLSM template file.");
		}

		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			throw new AdempiereException("File not found or not a regular file: " + filePath);
		}

		adClientId = Env.getAD_Client_ID(getCtx());
		formatter = new DataFormatter();

		if (log.isLoggable(Level.INFO)) {
			log.info("GenerateWspAtrTablesFromTemplate - file=" + file.getAbsolutePath()
			+ ", previewOnly=" + previewOnly);
		}

		try (InputStream is = new FileInputStream(file)) {
			workbook = WorkbookFactory.create(is);
		}

		evaluator = workbook.getCreationHelper().createFormulaEvaluator();
		formatter = new DataFormatter();

		// First validate that needed sheets exist
		validateSelectedSheetsExist();

		// Build header table definition (ZZ_WSP_ATR_Submitted)
		TabDef headerTab = buildSubmittedHeaderTableDef();

		// Build detail table definitions per selected sheet
		List<TabDef> tabDefs = new ArrayList<>();
		if (pBiodata)      tabDefs.add(buildTabFromSheet(SHEET_BIODATA));
		if (pATR)          tabDefs.add(buildTabFromSheet(SHEET_ATR));
		if (pWSP)          tabDefs.add(buildTabFromSheet(SHEET_WSP));
		if (pHTFV)         tabDefs.add(buildTabFromSheet(SHEET_HTFV));
		if (pTopupSkills)  tabDefs.add(buildTabFromSheet(SHEET_TOPUP));
		if (pNonEmployees) tabDefs.add(buildTabFromSheet(SHEET_NONEMP));
		if (pContractors)  tabDefs.add(buildTabFromSheet(SHEET_CONTR));
		if (pFinanceTraining) tabDefs.add(buildFinanceTabDef());

		if (previewOnly) {
			previewTables(headerTab, tabDefs);
		} else {
			createHeaderTableIfNeeded(headerTab);
			for (TabDef def : tabDefs) {
				createDetailTableIfNeeded(def);   // FIXED: call matches method signature
			}
		}

		String msg = (previewOnly ? "Preview only. " : "Tables created/updated. ")
				+ "Processed tabs: " + tabDefs.size();
		addLog(msg);
		return msg;
	}

	// ----------------------------------------------------------------------
	// 1. Sheet validation
	// ----------------------------------------------------------------------

	private void validateSelectedSheetsExist() {
		checkSheetExistsIfSelected(SHEET_BIODATA, pBiodata);
		checkSheetExistsIfSelected(SHEET_ATR, pATR);
		checkSheetExistsIfSelected(SHEET_WSP, pWSP);
		checkSheetExistsIfSelected(SHEET_HTFV, pHTFV);
		checkSheetExistsIfSelected(SHEET_TOPUP, pTopupSkills);
		checkSheetExistsIfSelected(SHEET_NONEMP, pNonEmployees);
		checkSheetExistsIfSelected(SHEET_CONTR, pContractors);
		checkSheetExistsIfSelected(SHEET_FINANCE, pFinanceTraining);
	}

	private void checkSheetExistsIfSelected(String sheetName, boolean selected) {
		if (!selected)
			return;

		Sheet s = workbook.getSheet(sheetName);
		if (s == null) {
			throw new AdempiereException("Selected sheet '" + sheetName + "' not found in workbook.");
		}
		if (workbook.isSheetHidden(workbook.getSheetIndex(sheetName))
				|| workbook.isSheetVeryHidden(workbook.getSheetIndex(sheetName))) {
			throw new AdempiereException("Selected sheet '" + sheetName + "' is hidden. Please unhide or deselect.");
		}
	}

	// ----------------------------------------------------------------------
	// 2. Header table definition (ZZ_WSP_ATR_Submitted)
	// ----------------------------------------------------------------------

	/**
	 * Build definition for main header table storing file + meta.
	 * TableName: ZZ_WSP_ATR_Submitted
	 */
	private TabDef buildSubmittedHeaderTableDef() {
		TabDef t = new TabDef();
		t.sheetName = null;
		t.adTableName = "ZZ_WSP_ATR_Submitted";
		t.adTableDesc = "WSP/ATR Submitted File";

		// Standard system columns are added automatically by iDempiere, we just
		// define business columns here.

		// Name (string 60)
		ColumnDef name = new ColumnDef();
		name.headerText = "Name";
		name.columnName = "Name";
		name.kind = ColumnKind.PLAIN_STRING;
		name.displayType = DisplayType.String;
		name.fieldLength = 60;
		t.columns.add(name);

		// Description (string 255)
		ColumnDef desc = new ColumnDef();
		desc.headerText = "Description";
		desc.columnName = "Description";
		desc.kind = ColumnKind.PLAIN_STRING;
		desc.displayType = DisplayType.String;
		desc.fieldLength = 255;
		t.columns.add(desc);

		// SubmittedDate (Date)
		ColumnDef dt = new ColumnDef();
		dt.headerText = "Submitted Date";
		dt.columnName = "SubmittedDate";
		dt.kind = ColumnKind.PLAIN_STRING; // we'll store as Date -> DisplayType.Date
		dt.displayType = DisplayType.Date;
		dt.fieldLength = 0;
		t.columns.add(dt);

		// FileName (String 255)
		ColumnDef fn = new ColumnDef();
		fn.headerText = "File Name";
		fn.columnName = "FileName";
		fn.kind = ColumnKind.PLAIN_STRING;
		fn.displayType = DisplayType.String;
		fn.fieldLength = 255;
		t.columns.add(fn);

		// Attachment (binary via standard AD_Attachment / not a direct column)
		// If later you want a "Document" column we can add it.

		return t;
	}

	// ----------------------------------------------------------------------
	// 3. Lookup mappings (ZZ_WSP_ATR_Lookup_Mapping + Detail)
	// ----------------------------------------------------------------------

	/** tabKey -> (headerKey -> LookupDef) */
	private Map<String, Map<String, LookupDef>> lookupCfg;

	/**
	 * Load all lookup mapping definitions into a nested map:
	 *   tabKey (lowercase) -> headerKey (canonical) -> LookupDef
	 */
	private Map<String, Map<String, LookupDef>> loadLookupMappings() {
		Map<String, Map<String, LookupDef>> result = new HashMap<>();

		List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> details =
				new Query(getCtx(), I_ZZ_WSP_ATR_Lookup_Mapping_Detail.Table_Name, null, trxName)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.list();

		for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
			I_ZZ_WSP_ATR_Lookup_Mapping header = det.getZZ_WSP_ATR_Lookup_Mapping(); // FIXED type to interface
			if (header == null)
				continue;

			String tabName = header.getZZ_Tab_Name();
			String headerName = det.getZZ_Header_Name();
			if (Util.isEmpty(tabName, true) || Util.isEmpty(headerName, true))
				continue;

			String tabKey = tabName.trim().toLowerCase(Locale.ROOT);
			String headerKey = canonicalHeader(headerName);

			Map<String, LookupDef> perTab = result.computeIfAbsent(tabKey, k -> new HashMap<>());

			LookupDef def = new LookupDef();
			def.useValue = det.isZZ_Use_Value();
			def.adTableId = det.getAD_Table_ID();
			if (def.adTableId > 0) {
				MTable ref = MTable.get(getCtx(), def.adTableId);
				if (ref != null && ref.getAD_Table_ID() > 0) {
					def.refTableName = ref.getTableName();
				}
			}

			perTab.put(headerKey, def);
		}

		return result;
	}

	/** Canonicalize a header for matching: lowercase, trimmed, whitespace collapsed. */
	private String canonicalHeader(String s) {
		if (s == null)
			return "";
		String cleaned = stripFormulaNoise(stripCellRefs(s))
				.replace('\n', ' ')
				.replace('\r', ' ')
				.trim()
				.replaceAll("\\s+", " ");
		return cleaned.toLowerCase(Locale.ROOT);
	}



	// ----------------------------------------------------------------------
	// 4. Column header extraction from sheets
	// ----------------------------------------------------------------------

	/** Try to decide header row: usually row 4, sometimes row 3 (1-based). */
	private int findHeaderRowIndex(Sheet sheet) {
		// 0-based indices for candidate header rows 3 and 4 (1-based)
		int idx3 = 2;
		int idx4 = 3;
		int lastRow = sheet.getLastRowNum();

		if (lastRow < idx3)
			return 0; // fallback

		int score3 = scoreHeaderRow(sheet.getRow(idx3));
		int score4 = (lastRow >= idx4) ? scoreHeaderRow(sheet.getRow(idx4)) : -1;

		// Prefer row 4 if it has equal or more non-empty cells than row 3
		if (score4 >= score3 && score4 > 0)
			return idx4;
		if (score3 > 0)
			return idx3;

		// fallback: first non-empty row
		for (int r = 0; r <= lastRow; r++) {
			Row row = sheet.getRow(r);
			if (scoreHeaderRow(row) > 0)
				return r;
		}
		return 0;
	}

	private int scoreHeaderRow(Row row) {
		if (row == null)
			return 0;
		int lastCell = row.getLastCellNum();
		if (lastCell <= 0)
			return 0;
		int nonEmpty = 0;
		for (int c = 0; c < lastCell; c++) {
			Cell cell = row.getCell(c);
			if (cell == null)
				continue;
			String v;
			if (evaluator != null)
				v = formatter.formatCellValue(cell, evaluator);
			else
				v = formatter.formatCellValue(cell);
			if (!Util.isEmpty(v, true))
				nonEmpty++;
		}
		return nonEmpty;
	}


	/**
	 * Build the AD_Table name for a sheet:
	 *   ZZ_WSP_ATR_<NormalizedSheetName>_Detail
	 */
	private String buildDetailTableName(String sheetName) {
		String norm = normalizeName(sheetName);
		return "ZZ_WSP_ATR_" + norm + "_Detail";
	}

	/** Normalize arbitrary text into a legal table/column name component. */
	private String normalizeName(String s) {
		if (s == null)
			return "";
		String tmp = s.trim()
				.replace('\n', ' ')
				.replace('\r', ' ');
		// replace non-alnum with underscore
		tmp = tmp.replaceAll("[^A-Za-z0-9]", "_");
		// collapse multiple underscores
		tmp = tmp.replaceAll("_+", "_");
		// trim underscores
		tmp = tmp.replaceAll("^_+", "").replaceAll("_+$", "");
		if (tmp.isEmpty())
			tmp = "X";
		if (Character.isDigit(tmp.charAt(0)))
			tmp = "_" + tmp;
		return tmp;
	}

	// ----------------------------------------------------------------------
	// 5. Build TabDef for regular (column-based) sheets
	// ----------------------------------------------------------------------

	private static final int DATA_START_ROW_INDEX = 6; // row 7 (1-based) – ignore rows 5 & 6

	private TabDef buildTabFromSheet(String sheetName) {
		Sheet sheet = workbook.getSheet(sheetName);
		if (sheet == null)
			throw new AdempiereException("Sheet not found: " + sheetName);

		if (lookupCfg == null)
			lookupCfg = loadLookupMappings();

		String tabKey = sheetName.trim().toLowerCase(Locale.ROOT);
		Map<String, LookupDef> tabLookup = lookupCfg.getOrDefault(tabKey, Collections.emptyMap());

		int headerRowIndex = findHeaderRowIndex(sheet);
		Row headerRow = sheet.getRow(headerRowIndex);
		Row headerRow2 = sheet.getRow(headerRowIndex + 1);
		Row headerRow3 = sheet.getRow(headerRowIndex + 2);

		if (headerRow == null)
			throw new AdempiereException("No header row found in sheet " + sheetName);

		int lastCell = headerRow.getLastCellNum();
		if (lastCell <= 0)
			throw new AdempiereException("No header columns found in sheet " + sheetName);

		TabDef t = new TabDef();
		t.sheetName = sheetName;
		t.adTableName = buildDetailTableName(sheetName);
		t.adTableDesc = "WSP/ATR " + sheetName + " Detail";

		for (int col = 0; col < lastCell; col++) {
			String h1 = getCellText(headerRow, col);
			String h2 = getCellText(headerRow2, col);
			String h3 = getCellText(headerRow3, col);

			String combined = combineHeaderFragments(h1, h2, h3);
			if (Util.isEmpty(combined, true))
				continue; // truly empty column (e.g. spacing)

			// Try to find a mapping: use full combined header first, then sub-rows
			LookupDef lookup = findLookupForHeader(tabLookup, combined, h2, h1);

			ColumnDef cd = new ColumnDef();
			cd.headerText = combined;

			if (lookup != null && lookup.adTableId > 0 && !Util.isEmpty(lookup.refTableName, true)) {
				cd.kind = ColumnKind.REF_ID;
				cd.lookup = lookup;

				// Use the HEADER text as the basis for the column name (e.g. Gender_ID, Race_ID)
				// Prefer the first header line if available, otherwise use combined
				String headerBase = !Util.isEmpty(h1, true) ? h1 : combined;

				String base = normalizeName(headerBase);
				if (Util.isEmpty(base, true)) {
					// Fallback to ref table name if header is somehow empty
					base = normalizeName(lookup.refTableName);
				}

				// Ensure it ends with _ID
				if (!base.toUpperCase(Locale.ROOT).endsWith("_ID")) {
					base = base + "_ID";
				}

				cd.columnName = base;

				// Use TABLE (18) for reference columns and length 10
				cd.displayType = DisplayType.Table;
				cd.fieldLength = 10;
			} else {
				// Infer data type from sample rows
				inferPlainColumnType(sheet, col, cd);
				// (make sure inferPlainColumnType sets fieldLength = 200 for strings)
			}

			// Ensure unique column name within table
			cd.columnName = ensureUniqueColumnName(t, cd.columnName);

			t.columns.add(cd);
		}


		return t;
	}

	private String getCellText(Row row, int col) {
		if (row == null)
			return "";
		Cell cell = row.getCell(col);
		if (cell == null)
			return "";

		String text;
		if (evaluator != null) {
			try {
				text = formatter.formatCellValue(cell, evaluator);
			} catch (RuntimeException ex) {
				// Some formulas (e.g. SUMPRODUCT over error ranges) can blow up.
				// Fall back to non-evaluated text (formula string or cached).
				text = formatter.formatCellValue(cell);
			}
		} else {
			text = formatter.formatCellValue(cell);
		}
		return text != null ? text.trim() : "";
	}



	
	/**
	 * Combine up to 3 header fragments (e.g. from three rows) into a single header,
	 * ignoring generic words and formula-like fragments.
	 */
	private String combineHeaderFragments(String h1, String h2, String h3) {
		List<String> parts = new ArrayList<>();

		String p1 = stripFormulaNoise(stripCellRefs(h1));
		String p2 = stripFormulaNoise(stripCellRefs(h2));
		String p3 = stripFormulaNoise(stripCellRefs(h3));

		if (!Util.isEmpty(p1, true))
			parts.add(p1.trim());
		if (!Util.isEmpty(p2, true))
			parts.add(p2.trim());
		if (!Util.isEmpty(p3, true))
			parts.add(p3.trim());

		// Remove generic junk like "Compulsory"
		parts.removeIf(p -> "Compulsory".equalsIgnoreCase(p));

		if (parts.isEmpty())
			return "";

		return String.join(" - ", parts);
	}




	private LookupDef findLookupForHeader(Map<String, LookupDef> tabLookup,
			String combined, String h2, String h1) {
		String[] candidates = {
				combined,
				h2,
				h1
		};
		for (String c : candidates) {
			if (Util.isEmpty(c, true))
				continue;
			String key = canonicalHeader(c);
			LookupDef def = tabLookup.get(key);
			if (def != null)
				return def;
		}
		return null;
	}

	private void inferPlainColumnType(Sheet sheet, int col, ColumnDef cd) {
		int numericCount = 0;
		int stringCount = 0;
		int maxLen = 0;

		int lastRow = sheet.getLastRowNum();
		int samples = 0;
		for (int r = DATA_START_ROW_INDEX; r <= lastRow && samples < 50; r++) {
			Row row = sheet.getRow(r);
			if (row == null)
				continue;
			Cell cell = row.getCell(col);
			if (cell == null)
				continue;

			CellType type = cell.getCellType();
			if (type == CellType.FORMULA) {
				type = cell.getCachedFormulaResultType();
			}

			if (type == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
				numericCount++;
				samples++;
			} else {
				String txt;
				if (evaluator != null) {
					try {
						txt = formatter.formatCellValue(cell, evaluator);
					} catch (RuntimeException ex) {
						txt = formatter.formatCellValue(cell);
					}
				} else {
					txt = formatter.formatCellValue(cell);
				}

				if (txt == null)
					txt = "";
				txt = txt.trim();
				if (txt.isEmpty())
					continue;

				stringCount++;
				maxLen = Math.max(maxLen, txt.length());
				samples++;
			}

		}

		if (numericCount > 0 && stringCount == 0) {
			cd.kind = ColumnKind.PLAIN_NUMBER;
			cd.displayType = DisplayType.Number;
			cd.fieldLength = 10;
			cd.columnName = normalizeName(cd.headerText);
		} else {
			cd.kind = ColumnKind.PLAIN_STRING;
			cd.displayType = DisplayType.String;
			if (maxLen <= 60)
				cd.fieldLength = 60;
			else if (maxLen <= 120)
				cd.fieldLength = 120;
			else
				cd.fieldLength = 255;
			cd.columnName = normalizeName(cd.headerText);
		}
	}

	/** Remove Excel cell refs like (D5) or standalone D5 tokens from header text */
	private String stripCellRefs(String s) {
		if (s == null)
			return "";
		String r = s;
		// Remove things like "(D5)" or "(AA10)"
		r = r.replaceAll("\\([A-Z]{1,3}\\d+\\)", "");
		// Remove standalone tokens like "D5" or "AA10"
		r = r.replaceAll("\\b[A-Z]{1,3}\\d+\\b", "");
		// Collapse whitespace
		r = r.replaceAll("\\s+", " ").trim();
		return r;
	}

	/** Cut off trailing formula-like garbage (CONCATENATE, COUNTA, COUNTIF, SUMPRODUCT, etc.) */
	private String stripFormulaNoise(String s) {
		if (s == null)
			return "";
		String u = s.toUpperCase(Locale.ROOT);
		int cut = u.length();
		String[] kws = {
				"CONCATENATE", "SUMPRODUCT", "COUNTIF", "COUNTA", "SUM(", "SUM_", "IF(", "IF_"
		};
		for (String kw : kws) {
			int i = u.indexOf(kw);
			if (i >= 0 && i < cut) {
				cut = i;
			}
		}
		String res = s.substring(0, cut);
		res = res.replaceAll("\\s+", " ").trim();
		return res;
	}



	private String ensureUniqueColumnName(TabDef t, String base) {
		if (base == null || base.isEmpty())
			base = "Column";
		String name = base;
		int idx = 1;
		Set<String> existing = new HashSet<>();
		for (ColumnDef c : t.columns)
			existing.add(c.columnName);
		while (existing.contains(name)) {
			name = base + "_" + idx;
			idx++;
		}
		return name;
	}

	// ----------------------------------------------------------------------
	// 6. Special-case definition for "Finance and Training Comparison"
	// ----------------------------------------------------------------------

	private TabDef buildFinanceTabDef() {
		// For this sheet, we treat each row as a record with:
		//   HeaderLabel (B) and Value (C). Headers without data in C will be
		//   ignored at import time. For table definition we just need generic columns.
		TabDef t = new TabDef();
		t.sheetName = SHEET_FINANCE;
		t.adTableName = buildDetailTableName(SHEET_FINANCE);
		t.adTableDesc = "WSP/ATR Finance and Training Comparison Detail";

		ColumnDef lineNo = new ColumnDef();
		lineNo.headerText = "Line No";
		lineNo.columnName = "LineNo";
		lineNo.kind = ColumnKind.PLAIN_NUMBER;
		lineNo.displayType = DisplayType.Integer;
		lineNo.fieldLength = 10;
		t.columns.add(lineNo);

		ColumnDef headerLabel = new ColumnDef();
		headerLabel.headerText = "Header Label";
		headerLabel.columnName = "HeaderLabel";
		headerLabel.kind = ColumnKind.PLAIN_STRING;
		headerLabel.displayType = DisplayType.String;
		headerLabel.fieldLength = 255;
		t.columns.add(headerLabel);

		ColumnDef valueNum = new ColumnDef();
		valueNum.headerText = "Numeric Value";
		valueNum.columnName = "ValueNumber";
		valueNum.kind = ColumnKind.PLAIN_NUMBER;
		valueNum.displayType = DisplayType.Number;
		valueNum.fieldLength = 10;
		t.columns.add(valueNum);

		ColumnDef valueTxt = new ColumnDef();
		valueTxt.headerText = "Text Value";
		valueTxt.columnName = "ValueText";
		valueTxt.kind = ColumnKind.PLAIN_STRING;
		valueTxt.displayType = DisplayType.String;
		valueTxt.fieldLength = 255;
		t.columns.add(valueTxt);

		return t;
	}

	// ----------------------------------------------------------------------
	// 7. Preview logging
	// ----------------------------------------------------------------------

	private void previewTables(TabDef headerTab, List<TabDef> details) {
		//addLog("=== PREVIEW: Header table ===");
		//logTabDefinition(headerTab);

		// NEW: fill reference existence info for REF_ID columns
		fillReferencePreviewInfo(details);

		addLog("=== PREVIEW: Detail tables (" + details.size() + ") ===");
		for (TabDef t : details) {
			logTabDefinition(t);
		}
	}

	/**
	 * For preview: fill reference info for REF_ID columns.
	 * Checks if an AD_Reference (Table Validation) exists for the lookup table.
	 */
	private void fillReferencePreviewInfo(List<TabDef> details) {
		if (details == null)
			return;

		for (TabDef t : details) {
			if (t.columns == null)
				continue;

			for (ColumnDef c : t.columns) {
				if (c.kind != ColumnKind.REF_ID || c.lookup == null)
					continue;

				// Desired reference name: <RefTableName>_ID
				String baseRefName = (c.lookup.refTableName != null)
						? c.lookup.refTableName.trim()
								: "";
				if (Util.isEmpty(baseRefName, true)) {
					continue;
				}

				String refName = baseRefName + "_ID";
				c.refName = refName;

				// Look for existing AD_Reference with this name and Table Validation
				MReference ref = new Query(getCtx(), MReference.Table_Name,
						"Name=? AND ValidationType=?", trxName)
						.setParameters(refName, X_AD_Reference.VALIDATIONTYPE_TableValidation)
						.first();

				if (ref != null && ref.getAD_Reference_ID() > 0) {
					c.refExists = true;
					c.ref_AD_Reference_ID = ref.getAD_Reference_ID();
				} else {
					c.refExists = false;
					c.ref_AD_Reference_ID = null;
				}
			}
		}
	}




	private void logTabDefinition(TabDef t) {
		StringBuilder sb = new StringBuilder();
		sb.append("Table ").append(t.adTableName)
		.append(" [").append(t.adTableDesc).append("]");
		addLog(sb.toString());

		for (ColumnDef c : t.columns) {
			String msg = "  Column " + c.columnName +
					"  type=" + DisplayType.getDescription(c.displayType) +
					" len=" + c.fieldLength +
					" kind=" + c.kind +
					(c.lookup != null ? " lookup=" + c.lookup : "");
			addLog(msg);

			if (previewOnly && c.kind == ColumnKind.REF_ID && c.lookup != null
					&& !Util.isEmpty(c.lookup.refTableName, true)) {

				String refName = c.lookup.refTableName + "_ID";
				int refId = new Query(getCtx(), MReference.Table_Name,
						"Name=? AND ValidationType=?", trxName)
						.setParameters(refName, MReference.VALIDATIONTYPE_TableValidation)
						.firstId();

				if (refId > 0) {
					addLog("    -> AD_Reference exists: " + refName + " (ID=" + refId + ")");
				} else {
					addLog("    -> [PREVIEW] Would create AD_Reference: " + refName
							+ " (TableValidation for table " + c.lookup.refTableName + ")");
				}
			}
		}
	}



	// ----------------------------------------------------------------------
	// 8. Create / update AD_Table + AD_Column (+ AD_Element)
	// ----------------------------------------------------------------------

	private static final String ENTITY_TYPE = "U"; // adjust if you use custom ET

	private void createHeaderTableIfNeeded(TabDef headerTab) {
	//	createTableAndColumns(headerTab, true);
	}

	private void createDetailTableIfNeeded(TabDef detailTab) {
		createTableAndColumns(detailTab, false);
	}

	private void createTableAndColumns(TabDef t, boolean isHeader) {
		// Lookup or create AD_Table
		MTable table = MTable.get(getCtx(), t.adTableName);
		boolean newTable = false;
		if (table == null || table.getAD_Table_ID() <= 0) {
			table = new MTable(getCtx(), 0, trxName);
			table.setTableName(t.adTableName);
			// AD_Table.Name is short; use the sheet or a compact name
			String name = t.adTableDesc;
			if (Util.isEmpty(name, true))
				name = t.adTableName;
			if (name.length() > 60)
				name = name.substring(0, 60);
			table.setName(name);
			table.setDescription(t.adTableDesc);
			table.setAccessLevel(MTable.ACCESSLEVEL_ClientPlusOrganization); // "3"
			table.setEntityType(ENTITY_TYPE);
			table.setIsView(false);
			table.setIsSecurityEnabled(false);
			table.setIsCentrallyMaintained(false);
			table.setIsHighVolume(false);
			table.setIsChangeLog(false);
			table.setIsDeleteable(true);
			//table.setUserMaintained(true);
			table.saveEx();
			newTable = true;
			addLog("Created AD_Table: " + t.adTableName);
		} else {
			addLog("AD_Table already exists: " + t.adTableName);
		}

		// Create / update columns
		int seqNo = 10;
		for (ColumnDef c : t.columns) {
			createOrUpdateColumn(table, c, seqNo, isHeader);
			seqNo += 10;
		}

		if (!newTable) {
			// just for info
			addLog("Updated columns for table: " + t.adTableName);
		}
	}

	private void createOrUpdateColumn(MTable table, ColumnDef c, int seqNo, boolean isHeader) {
	    // Find existing column
	    MColumn column = new Query(getCtx(), MColumn.Table_Name,
	            "AD_Table_ID=? AND ColumnName=?", trxName)
	            .setParameters(table.getAD_Table_ID(), c.columnName)
	            .first();
	    boolean isNew = false;
	    if (column == null) {
	        column = new MColumn(getCtx(), 0, trxName);
	        column.setAD_Table_ID(table.getAD_Table_ID());
	        column.setColumnName(c.columnName);
	        isNew = true;
	    }

	    // Friendly element / name
	    String elementName = c.headerText;
	    if (Util.isEmpty(elementName, true)) {
	        elementName = c.columnName.replace('_', ' ');
	    }
	    elementName = elementName.trim();
	    if (elementName.length() > 60)
	        elementName = elementName.substring(0, 60);

	    // Element: key by ColumnName
	    M_Element element = M_Element.get(getCtx(), c.columnName);
	    if (element == null || element.getAD_Element_ID() <= 0) {
	        element = new M_Element(getCtx(), 0, trxName);
	        element.setColumnName(c.columnName);
	        element.setName(elementName);
	        element.setPrintName(elementName);
	        element.setEntityType(ENTITY_TYPE);
	        element.saveEx();
	    }

	    column.setAD_Element_ID(element.getAD_Element_ID());
	    column.setName(elementName);
	    column.setDescription(c.headerText);
	    column.setHelp(null);

	    // For non-reference columns we just use c.displayType,
	    // for reference columns we'll override to Table below.
	    column.setAD_Reference_ID(c.displayType);
	    if (c.fieldLength > 0)
	        column.setFieldLength(c.fieldLength);

	    column.setSeqNo(seqNo);
	    column.setIsTranslated(false);
	    column.setIsEncrypted(false);
	    column.setIsAlwaysUpdateable(true);
	    column.setIsUpdateable(true);
	    column.setIsIdentifier(false);
	    column.setIsParent(false);
	    column.setIsMandatory(false);

	    // PK column: <tableName>_ID
	    String pkName = table.getTableName() + "_ID";
	    if (c.columnName.equalsIgnoreCase(pkName)) {
	        column.setAD_Reference_ID(DisplayType.ID);
	        column.setIsKey(true);
	        column.setIsMandatory(true);
	        column.setIsUpdateable(false);
	    }

	    // Standard client/org
	    if ("AD_Client_ID".equalsIgnoreCase(c.columnName) ||
	        "AD_Org_ID".equalsIgnoreCase(c.columnName)) {
	        column.setIsMandatory(true);
	        column.setIsUpdateable(false);
	    }

	    // File column (if you added one in header TabDef)
	    if ("FileData".equalsIgnoreCase(c.columnName)) {
	        column.setAD_Reference_ID(DisplayType.Binary);
	    }

	    // ---- IMPORTANT: REF_ID columns need AD_Reference_Value_ID before save ----
	    if (c.kind == ColumnKind.REF_ID && c.lookup != null && c.lookup.adTableId > 0) {
	        // Make sure it is TABLE (18) and max length 10
	        column.setAD_Reference_ID(DisplayType.Table);
	        column.setFieldLength(10);

	        // This will create/find AD_Reference + AD_Ref_Table and
	        // set AD_Reference_Value_ID on the column, then save it.
	        ensureTableReferenceForLookup(column, c, isNew, table.getTableName());
	    } else {
	        // Normal column: we can save directly
	        column.saveEx();
	        addLog((isNew ? "Created" : "Updated") + " column "
	                + table.getTableName() + "." + c.columnName);
	    }
	}
	
	/**
	 * For a REF_ID column:
	 *  - Ensure there is an AD_Reference (ValidationType = 'T') with Name = <refTableName>_ID
	 *  - Ensure there is an AD_Ref_Table row for that reference, pointing to the lookup table
	 *  - Link the column: AD_Reference_ID = Table (18), AD_Reference_Value_ID = that reference
	 *
	 * In previewOnly mode:
	 *  - DO NOT create anything, just log what would happen.
	 */
	private void ensureTableReferenceForLookup(MColumn column, ColumnDef c, boolean isNewColumn, String tableName) {

	    if (c.lookup == null || c.lookup.adTableId <= 0 || Util.isEmpty(c.lookup.refTableName, true)) {
	        return;
	    }

	    // Reference name: use lookup table name + "_ID"
	    final String refName = c.lookup.refTableName + "_ID";

	    // ------------------------------------------------------------------
	    // 1) Find existing AD_Reference (Table Validation) by name
	    // ------------------------------------------------------------------
	    MReference ref = new Query(getCtx(), MReference.Table_Name,
	                    "Name=? AND ValidationType=?", trxName)
	            .setParameters(refName, MReference.VALIDATIONTYPE_TableValidation)
	            .first();

	    int refId = (ref != null) ? ref.getAD_Reference_ID() : 0;

	    if (previewOnly) {
	        if (refId > 0) {
	            addLog("  [REF] Column " + tableName + "." + column.getColumnName()
	                    + " will use existing AD_Reference '" + refName
	                    + "' (ID=" + refId + ") for table " + c.lookup.refTableName);
	        } else {
	            addLog("  [REF] Column " + tableName + "." + column.getColumnName()
	                    + " would create AD_Reference '" + refName
	                    + "' (Table=" + c.lookup.refTableName + ")");
	        }
	        return; // no DB changes in preview
	    }

	    // ------------------------------------------------------------------
	    // 2) Create AD_Reference if missing
	    // ------------------------------------------------------------------
	    if (refId <= 0) {
	        ref = new MReference(getCtx(), 0, trxName);
	        ref.setName(refName);
	        ref.setValidationType(MReference.VALIDATIONTYPE_TableValidation); // 'T' - Table Validation
	        ref.setEntityType(ENTITY_TYPE);
	        ref.saveEx();
	        refId = ref.getAD_Reference_ID();

	        addLog("Created AD_Reference '" + refName
	                + "' (ID=" + refId + ") for table " + c.lookup.refTableName);
	    }

	    // Safety
	    if (refId <= 0) {
	        throw new AdempiereException(
	                "Failed to create or load AD_Reference for " + refName);
	    }

	    // ------------------------------------------------------------------
	    // 3) Ensure AD_Ref_Table row exists and is linked to lookup table
	    //     IMPORTANT: don't modify immutable cache object
	    // ------------------------------------------------------------------
	    MRefTable refTableCfg = MRefTable.get(getCtx(), refId, trxName);

	    if (refTableCfg != null) {
	        // MRefTable.get() returns an immutable instance from cache.
	        // Wrap it in a mutable copy before changing.
	        refTableCfg = new MRefTable(getCtx(), refTableCfg, trxName);
	    } else {
	        // No AD_Ref_Table yet for this reference -> create new
	        refTableCfg = new MRefTable(getCtx(), 0, trxName);
	        refTableCfg.setAD_Reference_ID(refId);
	    }

	    // Link to lookup table
	    refTableCfg.setAD_Table_ID(c.lookup.adTableId);

	    // Determine key & display columns on the lookup table
	    MTable refTable = MTable.get(getCtx(), c.lookup.adTableId);
	    if (refTable != null && refTable.getAD_Table_ID() > 0) {

	        // Key column: IsKey='Y' or <TableName>_ID
	        int keyColId = new Query(getCtx(), MColumn.Table_Name,
	                "AD_Table_ID=? AND IsKey='Y'", trxName)
	                .setParameters(refTable.getAD_Table_ID())
	                .firstId();

	        if (keyColId <= 0) {
	            keyColId = new Query(getCtx(), MColumn.Table_Name,
	                    "AD_Table_ID=? AND ColumnName=?", trxName)
	                    .setParameters(refTable.getAD_Table_ID(),
	                            refTable.getTableName() + "_ID")
	                    .firstId();
	        }

	        // Display column: IsIdentifier='Y' or Name
	        int displayColId = new Query(getCtx(), MColumn.Table_Name,
	                "AD_Table_ID=? AND IsIdentifier='Y'", trxName)
	                .setParameters(refTable.getAD_Table_ID())
	                .firstId();

	        if (displayColId <= 0) {
	            displayColId = new Query(getCtx(), MColumn.Table_Name,
	                    "AD_Table_ID=? AND ColumnName='Name'", trxName)
	                    .setParameters(refTable.getAD_Table_ID())
	                    .firstId();
	        }

	        if (keyColId > 0) {
	            refTableCfg.setAD_Key(keyColId);
	        }
	        if (displayColId > 0) {
	            refTableCfg.setAD_Display(displayColId);
	        }
	    }

	    refTableCfg.setEntityType(ENTITY_TYPE);
	    refTableCfg.saveEx();

	    addLog("Ensured AD_Ref_Table for AD_Reference '" + refName
	            + "' (ID=" + refId + "), lookup table=" + c.lookup.refTableName + ")");

	    // ------------------------------------------------------------------
	    // 4) Link the column to this reference and save the column
	    // ------------------------------------------------------------------
	    column.setAD_Reference_ID(DisplayType.Table);   // 18 - Table
	    column.setAD_Reference_Value_ID(refId);
	    column.saveEx();

	    addLog((isNewColumn ? "Created" : "Updated") + " column "
	            + tableName + "." + column.getColumnName()
	            + " with AD_Reference_ID=" + refId
	            + " (Table=" + c.lookup.refTableName + ")");
	}


	


}
