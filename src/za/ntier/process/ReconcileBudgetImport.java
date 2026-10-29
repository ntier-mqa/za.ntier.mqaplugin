package za.ntier.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Util;

/**
 * Compares the original Admin + Project budget workbooks (as given to Finance)
 * against the GL_Journal / GL_JournalLine records that the budget import
 * created in iDempiere, for every fiscal year folder found under RootPath, and
 * writes the result as a multi-tab Excel workbook: a "Summary" tab (one row per
 * year) plus one tab per fiscal year with the calendar-month breakdown (record
 * count and total amount, source vs GL).
 * <p>
 * RootPath is the server folder that contains one subfolder per fiscal year,
 * named "YYYY-YY" (e.g. .../fwdbudgetexport/2022-23), each holding the Admin +
 * Project source .xlsx files. All matching year folders under RootPath are
 * processed in one run.
 * <p>
 * Matches GL_Journal where PostingType = Budget ('B'), Description =
 * ZZ_GLImportDesc (or the Description parameter, if supplied), DateAcct in the
 * fiscal year, for the current AD_Client.
 */
@org.adempiere.base.annotation.Process(name = "za.ntier.process.ReconcileBudgetImport")
public class ReconcileBudgetImport extends SvrProcess {

	@Parameter(name = "RootPath")
	private String p_RootPath;

	@Parameter(name = "Description")
	private String p_Description;

	@Parameter(name = "Tolerance")
	private BigDecimal p_Tolerance;

	private static final Pattern PERIOD_RE = Pattern.compile("Period\\s+(\\d+)\\s*-\\s*(\\d{4})",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern YEAR_DIR_RE = Pattern.compile("^(\\d{4})-\\d{2}$");
	private static final String[] COLUMN_HEADERS = { "Period", "Src Count", "GL Count", "Count Diff", "Src Amount",
			"GL Amount", "Amount Diff", "Status" };

	private static final String DEFAULT_ROOT_PATH = "/home/ntier/fwdbudgetexport";
	private static final String DEFAULT_DESCRIPTION = "ZZ_GLImportDesc";
	private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.01");

	@Override
	protected void prepare() {
		if (Util.isEmpty(p_RootPath, true))
			p_RootPath = DEFAULT_ROOT_PATH;
		else
			p_RootPath = p_RootPath.trim();
		if (Util.isEmpty(p_Description, true))
			p_Description = DEFAULT_DESCRIPTION;
		if (p_Tolerance == null)
			p_Tolerance = DEFAULT_TOLERANCE;
	}

	@Override
	protected String doIt() throws Exception {
		File root = new File(p_RootPath);
		if (!root.isDirectory())
			throw new AdempiereException("Not a directory on the server: [" + p_RootPath + "] (length="
					+ p_RootPath.length() + ", exists=" + root.exists() + ", canRead=" + root.canRead() + ", absolute="
					+ root.getAbsolutePath() + ")");

		List<File> yearDirs = new ArrayList<>();
		File[] children = root.listFiles(File::isDirectory);
		if (children != null) {
			for (File f : children) {
				if (YEAR_DIR_RE.matcher(f.getName()).matches())
					yearDirs.add(f);
			}
		}
		if (yearDirs.isEmpty())
			throw new AdempiereException("No 'YYYY-YY' year folders found under " + root.getAbsolutePath());

		yearDirs.sort(Comparator.comparing(File::getName));

		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Styles styles = new Styles(wb);

			Sheet summarySheet = wb.createSheet("Summary");
			writeRow(summarySheet, 0, styles.header, "Year", "Fiscal Year", "Src Count", "GL Count", "Count Diff",
					"Src Amount", "GL Amount", "Amount Diff", "Status");
			int summaryRowNum = 1;

			boolean grandOk = true;
			long grandSrcCount = 0, grandGlCount = 0;
			BigDecimal grandSrcAmt = BigDecimal.ZERO, grandGlAmt = BigDecimal.ZERO;
			List<String> yearStatus = new ArrayList<>();

			for (File yearDir : yearDirs) {
				Matcher m = YEAR_DIR_RE.matcher(yearDir.getName());
				m.matches();
				int startYear = Integer.parseInt(m.group(1));
				LocalDate fyStart = LocalDate.of(startYear, 4, 1);
				LocalDate fyEnd = fyStart.plusYears(1);
				String fyLabel = fyStart + " to " + fyEnd.minusDays(1);

				Sheet yearSheet = wb.createSheet(WorkbookUtil.createSafeSheetName(yearDir.getName()));

				YearResult yr;
				try {
					yr = reconcileYear(yearDir, fyStart, fyEnd, yearSheet, styles);
				} catch (AdempiereException e) {
					wb.removeSheetAt(wb.getSheetIndex(yearSheet));
					writeRow(summarySheet, summaryRowNum++, styles.normal, yearDir.getName(), fyLabel, "", "", "", "",
							"", "", "SKIPPED: " + e.getMessage());
					addLog("WARN: " + yearDir.getName() + " skipped - " + e.getMessage());
					yearStatus.add(yearDir.getName() + "=SKIPPED");
					continue;
				}

				grandOk = grandOk && yr.ok;
				grandSrcCount += yr.srcCount;
				grandGlCount += yr.glCount;
				grandSrcAmt = grandSrcAmt.add(yr.srcAmount);
				grandGlAmt = grandGlAmt.add(yr.glAmount);
				yearStatus.add(yearDir.getName() + "=" + (yr.ok ? "OK" : "MISMATCH"));

				Row sr = summarySheet.createRow(summaryRowNum++);
				setCell(sr, 0, yearDir.getName(), styles.normal);
				setCell(sr, 1, fyLabel, styles.normal);
				setCell(sr, 2, yr.srcCount, styles.normal);
				setCell(sr, 3, yr.glCount, styles.normal);
				setCell(sr, 4, yr.srcCount - yr.glCount, styles.normal);
				setCell(sr, 5, yr.srcAmount, styles.money);
				setCell(sr, 6, yr.glAmount, styles.money);
				setCell(sr, 7, yr.srcAmount.subtract(yr.glAmount), styles.money);
				setCell(sr, 8, yr.ok ? "OK" : "MISMATCH", yr.ok ? styles.statusOk : styles.statusMismatch);
			}

			Row totalRow = summarySheet.createRow(summaryRowNum);
			setCell(totalRow, 0, "GRAND TOTAL", styles.totalLabel);
			setCell(totalRow, 1, "", styles.totalLabel);
			setCell(totalRow, 2, grandSrcCount, styles.totalLabel);
			setCell(totalRow, 3, grandGlCount, styles.totalLabel);
			setCell(totalRow, 4, grandSrcCount - grandGlCount, styles.totalLabel);
			setCell(totalRow, 5, grandSrcAmt, styles.totalMoney);
			setCell(totalRow, 6, grandGlAmt, styles.totalMoney);
			setCell(totalRow, 7, grandSrcAmt.subtract(grandGlAmt), styles.totalMoney);
			setCell(totalRow, 8, grandOk ? "OK" : "MISMATCH", grandOk ? styles.statusOk : styles.statusMismatch);

			for (int i = 0; i <= 8; i++)
				summarySheet.autoSizeColumn(i);
			wb.setSheetOrder("Summary", 0);
			wb.setActiveSheet(0);

			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			File outFile = new File(System.getProperty("java.io.tmpdir"),
					"BudgetImportReconciliation_" + timestamp + ".xlsx");
			try (FileOutputStream fos = new FileOutputStream(outFile)) {
				wb.write(fos);
			}
			addLog("Report written: " + outFile.getAbsolutePath());

			ProcessInfo pi = getProcessInfo();
			pi.setExport(true);
			pi.setExportFile(outFile);
			pi.setExportFileExtension("xlsx");

			return (grandOk ? "MATCH" : "MISMATCH") + " across " + yearDirs.size() + " year(s): "
					+ String.join(", ", yearStatus) + " - see attached Excel report for full detail";
		}
	}

	/** Holds one year's aggregate, rolled up into the grand total. */
	private static class YearResult {
		boolean ok;
		long srcCount, glCount;
		BigDecimal srcAmount = BigDecimal.ZERO, glAmount = BigDecimal.ZERO;
	}

	private YearResult reconcileYear(File yearDir, LocalDate fyStart, LocalDate fyEnd, Sheet sheet, Styles styles)
			throws Exception {
		File adminFile = findSourceFile(yearDir, "admin");
		File projectFile = findSourceFile(yearDir, "project");

		int r = 0;
		setCell(sheet.createRow(r++), 0, "Fiscal Year", styles.label, fyStart + " to " + fyEnd.minusDays(1),
				styles.normal);
		setCell(sheet.createRow(r++), 0, "Admin File", styles.label, adminFile.getName(), styles.normal);
		setCell(sheet.createRow(r++), 0, "Project File", styles.label, projectFile.getName(), styles.normal);
		r++; // blank row

		Map<YearMonth, Integer> srcCount = new LinkedHashMap<>();
		Map<YearMonth, BigDecimal> srcAmount = new LinkedHashMap<>();
		parseBudgetFile(adminFile, srcCount, srcAmount);
		parseBudgetFile(projectFile, srcCount, srcAmount);

		Map<YearMonth, Long> glCount = new LinkedHashMap<>();
		Map<YearMonth, BigDecimal> glAmount = new LinkedHashMap<>();
		fetchGLData(fyStart, fyEnd, glCount, glAmount);

		writeRow(sheet, r++, styles.header, COLUMN_HEADERS);

		YearResult yr = new YearResult();
		yr.ok = true;

		for (YearMonth ym = YearMonth.from(fyStart); ym.atDay(1).isBefore(fyEnd); ym = ym.plusMonths(1)) {
			long sCount = srcCount.getOrDefault(ym, 0);
			BigDecimal sAmt = srcAmount.getOrDefault(ym, BigDecimal.ZERO);
			long gCount = glCount.getOrDefault(ym, 0L);
			BigDecimal gAmt = glAmount.getOrDefault(ym, BigDecimal.ZERO);

			long cntDiff = sCount - gCount;
			BigDecimal amtDiff = sAmt.subtract(gAmt);
			boolean ok = cntDiff == 0 && amtDiff.abs().compareTo(p_Tolerance) <= 0;
			yr.ok = yr.ok && ok;

			yr.srcCount += sCount;
			yr.glCount += gCount;
			yr.srcAmount = yr.srcAmount.add(sAmt);
			yr.glAmount = yr.glAmount.add(gAmt);

			Row row = sheet.createRow(r++);
			setCell(row, 0, ym.format(DateTimeFormatter.ofPattern("MMM-yy")), styles.normal);
			setCell(row, 1, sCount, styles.normal);
			setCell(row, 2, gCount, styles.normal);
			setCell(row, 3, cntDiff, styles.normal);
			setCell(row, 4, sAmt, styles.money);
			setCell(row, 5, gAmt, styles.money);
			setCell(row, 6, amtDiff, styles.money);
			setCell(row, 7, ok ? "OK" : "MISMATCH", ok ? styles.statusOk : styles.statusMismatch);
		}

		long totalCntDiff = yr.srcCount - yr.glCount;
		BigDecimal totalAmtDiff = yr.srcAmount.subtract(yr.glAmount);
		Row totalRow = sheet.createRow(r++);
		setCell(totalRow, 0, "TOTAL", styles.totalLabel);
		setCell(totalRow, 1, yr.srcCount, styles.totalLabel);
		setCell(totalRow, 2, yr.glCount, styles.totalLabel);
		setCell(totalRow, 3, totalCntDiff, styles.totalLabel);
		setCell(totalRow, 4, yr.srcAmount, styles.totalMoney);
		setCell(totalRow, 5, yr.glAmount, styles.totalMoney);
		setCell(totalRow, 6, totalAmtDiff, styles.totalMoney);
		setCell(totalRow, 7, yr.ok ? "OK" : "MISMATCH", yr.ok ? styles.statusOk : styles.statusMismatch);

		for (int i = 0; i <= 7; i++)
			sheet.autoSizeColumn(i);

		return yr;
	}

	/**
	 * Finds the single .xlsx in dir whose name contains keyword, skipping the
	 * converted import templates.
	 */
	private File findSourceFile(File dir, String keyword) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				String low = f.getName().toLowerCase();
				if (!low.endsWith(".xlsx"))
					continue;
				if (low.contains("idempiere") || low.contains("template"))
					continue;
				if (low.contains(keyword))
					return f;
			}
		}
		throw new AdempiereException("Could not find a '" + keyword + "' budget .xlsx in " + dir.getAbsolutePath());
	}

	/**
	 * Reads one budget workbook: header row has 'Account' in column A and 'Period N
	 * - YYYY' columns; data rows follow until Description = 'Total'. Adds one count
	 * + amount per non-zero Period cell into the running totals.
	 */
	private void parseBudgetFile(File file, Map<YearMonth, Integer> outCount, Map<YearMonth, BigDecimal> outAmount)
			throws Exception {
		try (FileInputStream fis = new FileInputStream(file); Workbook wb = WorkbookFactory.create(fis)) {

			Sheet sheet = wb.getSheetAt(0);

			Row headerRow = null;
			for (int r = sheet.getFirstRowNum(); r <= Math.min(sheet.getLastRowNum(), 10); r++) {
				Row row = sheet.getRow(r);
				if (row != null && "Account".equals(getCellString(row.getCell(0)))) {
					headerRow = row;
					break;
				}
			}
			if (headerRow == null)
				throw new AdempiereException("Could not find header row ('Account' in column A) in " + file.getName());

			Map<Integer, YearMonth> colPeriod = new LinkedHashMap<>();
			for (Cell cell : headerRow) {
				String text = getCellString(cell);
				if (text == null)
					continue;
				Matcher m = PERIOD_RE.matcher(text.trim());
				if (m.matches()) {
					int periodNo = Integer.parseInt(m.group(1));
					int headerYear = Integer.parseInt(m.group(2));
					colPeriod.put(cell.getColumnIndex(), periodMonth(periodNo, headerYear));
				}
			}
			if (colPeriod.isEmpty())
				throw new AdempiereException("Could not find any 'Period N - YYYY' columns in " + file.getName());

			for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (row == null)
					continue;
				String account = getCellString(row.getCell(0));
				String description = getCellString(row.getCell(1));
				if (Util.isEmpty(account, true)) {
					if ("Total".equals(description))
						break;
					continue;
				}
				for (Map.Entry<Integer, YearMonth> e : colPeriod.entrySet()) {
					Cell cell = row.getCell(e.getKey());
					if (cell == null || cell.getCellType() != CellType.NUMERIC)
						continue;
					double val = cell.getNumericCellValue();
					if (val == 0d)
						continue;
					YearMonth ym = e.getValue();
					outCount.merge(ym, 1, Integer::sum);
					outAmount.merge(ym, BigDecimal.valueOf(val), BigDecimal::add);
				}
			}
		}
	}

	private static YearMonth periodMonth(int periodNo, int headerYear) {
		int month = ((periodNo - 1 + 3) % 12) + 1; // Period 1 = Apr
		int year = month <= 3 ? headerYear : headerYear - 1;
		return YearMonth.of(year, month);
	}

	private static String getCellString(Cell cell) {
		if (cell == null)
			return null;
		if (cell.getCellType() == CellType.STRING)
			return cell.getStringCellValue();
		if (cell.getCellType() == CellType.NUMERIC)
			return String.valueOf(cell.getNumericCellValue());
		return null;
	}

	/**
	 * Pulls per-month line count and net amount (Dr - Cr) from
	 * GL_Journal/GL_JournalLine for the current client.
	 */
	private void fetchGLData(LocalDate fyStart, LocalDate fyEnd, Map<YearMonth, Long> outCount,
			Map<YearMonth, BigDecimal> outAmount) throws Exception {
		String sql = """
				SELECT date_trunc('month', j.dateacct)::date AS period_month,
				       COUNT(*) AS line_count,
				       COALESCE(SUM(l.amtsourcedr), 0) AS total_dr,
				       COALESCE(SUM(l.amtsourcecr), 0) AS total_cr
				FROM gl_journal j
				JOIN gl_journalline l ON l.gl_journal_id = j.gl_journal_id
				WHERE j.ad_client_id = ?
				  AND j.postingtype = 'B'
				  AND j.description = ?
				  AND j.dateacct >= ?
				  AND j.dateacct <  ?
				GROUP BY 1
				ORDER BY 1
				""";

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, getAD_Client_ID());
			pstmt.setString(2, p_Description);
			pstmt.setTimestamp(3, Timestamp.valueOf(fyStart.atStartOfDay()));
			pstmt.setTimestamp(4, Timestamp.valueOf(fyEnd.atStartOfDay()));

			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					YearMonth ym = YearMonth.from(rs.getDate("period_month").toLocalDate());
					outCount.put(ym, rs.getLong("line_count"));
					BigDecimal dr = rs.getBigDecimal("total_dr");
					BigDecimal cr = rs.getBigDecimal("total_cr");
					outAmount.put(ym, dr.subtract(cr));
				}
			}
		}
	}

	// ---- Excel writing helpers
	// -------------------------------------------------------------

	private static void writeRow(Sheet sheet, int rowNum, CellStyle style, String... values) {
		Row row = sheet.createRow(rowNum);
		for (int i = 0; i < values.length; i++)
			setCell(row, i, values[i], style);
	}

	private static void setCell(Row row, int col, String value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private static void setCell(Row row, int col, long value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private static void setCell(Row row, int col, BigDecimal value, CellStyle style) {
		Cell cell = row.createCell(col);
		cell.setCellValue(value.doubleValue());
		cell.setCellStyle(style);
	}

	/** Writes a two-column "label: value" row (col 0 = label, col 1 = value). */
	private static void setCell(Row row, int labelCol, String label, CellStyle labelStyle, String value,
			CellStyle valueStyle) {
		setCell(row, labelCol, label, labelStyle);
		setCell(row, labelCol + 1, value, valueStyle);
	}

	/** Cell styles shared across all sheets in the workbook. */
	private static class Styles {
		final CellStyle header;
		final CellStyle label;
		final CellStyle normal;
		final CellStyle money;
		final CellStyle totalLabel;
		final CellStyle totalMoney;
		final CellStyle statusOk;
		final CellStyle statusMismatch;

		Styles(XSSFWorkbook wb) {
			Font boldFont = wb.createFont();
			boldFont.setBold(true);

			header = wb.createCellStyle();
			header.setFont(boldFont);
			header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			label = wb.createCellStyle();
			label.setFont(boldFont);

			normal = wb.createCellStyle();

			short moneyFormat = wb.createDataFormat().getFormat("#,##0.00");
			money = wb.createCellStyle();
			money.setDataFormat(moneyFormat);

			totalLabel = wb.createCellStyle();
			totalLabel.setFont(boldFont);
			totalLabel.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);

			totalMoney = wb.createCellStyle();
			totalMoney.setFont(boldFont);
			totalMoney.setDataFormat(moneyFormat);
			totalMoney.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);

			statusOk = wb.createCellStyle();
			statusOk.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
			statusOk.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			statusMismatch = wb.createCellStyle();
			statusMismatch.setFont(boldFont);
			statusMismatch.setFillForegroundColor(IndexedColors.ROSE.getIndex());
			statusMismatch.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}
	}
}
