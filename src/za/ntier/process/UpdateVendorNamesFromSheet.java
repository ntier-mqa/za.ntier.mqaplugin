package za.ntier.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;

/**
 * Reads an Excel sheet containing Vendor IDs (col A) and Vendor Names (col E).
 * For Vendor IDs starting with "L", finds the matching C_BPartner by bp.value.
 * If bp.Name differs from the sheet Vendor Name, updates it.
 * Writes a change log Excel file to the same folder as the input file.
 */
@org.adempiere.base.annotation.Process(name = "za.ntier.process.UpdateVendorNamesFromSheet")
public class UpdateVendorNamesFromSheet extends SvrProcess {

	private String p_FilePath;

	private static final DataFormatter DF = new DataFormatter();

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			if ("FileName".equals(para.getParameterName())) {
				p_FilePath = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception {

		if (p_FilePath == null || p_FilePath.isBlank())
			throw new IllegalArgumentException("FileName parameter is required");

		File inFile = new File(p_FilePath);
		if (!inFile.exists())
			throw new IllegalArgumentException("File not found: " + p_FilePath);

		// --- read input sheet ---
		List<String[]> lVendors = new ArrayList<>();
		try (FileInputStream fis = new FileInputStream(inFile);
		     Workbook wb = WorkbookFactory.create(fis)) {

			Sheet sheet = wb.getSheetAt(0);
			boolean firstRow = true;

			for (Row row : sheet) {
				if (firstRow) { firstRow = false; continue; } // skip header

				Cell vendorIdCell   = row.getCell(0); // col A – Vendor ID
				Cell vendorNameCell = row.getCell(4); // col E – Vendor Name

				if (vendorIdCell == null) continue;

				String vendorId   = DF.formatCellValue(vendorIdCell).trim();
				String vendorName = vendorNameCell != null ? DF.formatCellValue(vendorNameCell).trim() : "";

				if (vendorId.toUpperCase().startsWith("L")) {
					lVendors.add(new String[]{vendorId, vendorName});
				}
			}
		}

		addLog("Found " + lVendors.size() + " Vendor IDs starting with 'L'");

		// --- process each L vendor ---
		List<String[]> changes = new ArrayList<>(); // vendorId, oldName, newName

		for (String[] vendor : lVendors) {
			String vendorId   = vendor[0];
			String vendorName = vendor[1];

			int bpId = DB.getSQLValue(get_TrxName(),
				"SELECT C_BPartner_ID FROM C_BPartner WHERE value = ? AND AD_Client_ID = ?",
				vendorId, getAD_Client_ID());

			if (bpId <= 0) {
				addLog("WARN: No C_BPartner found for Vendor ID: " + vendorId);
				continue;
			}

			MBPartner_New bp = new MBPartner_New(getCtx(), bpId, get_TrxName());
			String currentName = bp.getName();

			if (currentName == null || !currentName.equals(vendorName)) {
				String oldName = currentName;
				bp.setName(vendorName);
				bp.saveEx();
				changes.add(new String[]{vendorId, oldName != null ? oldName : "", vendorName});
				addLog("Updated: " + vendorId + " | '" + oldName + "' -> '" + vendorName + "'");
			}
		}

		addLog("Total updates: " + changes.size());

		// --- write change log Excel and expose as download ---
		if (!changes.isEmpty()) {
			String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			File outFile = new File(System.getProperty("java.io.tmpdir"),
					"VendorNameChanges_" + timestamp + ".xlsx");
			writeChangeLog(outFile.getAbsolutePath(), changes);
			addLog("Change log: " + outFile.getAbsolutePath());

			ProcessInfo pi = getProcessInfo();
			pi.setExport(true);
			pi.setExportFile(outFile);
			pi.setExportFileExtension("xlsx");
		}

		return "Done. " + lVendors.size() + " L-vendors checked, " + changes.size() + " updated.";
	}

	private void writeChangeLog(String outPath, List<String[]> changes) throws Exception {
		try (XSSFWorkbook wb = new XSSFWorkbook()) {
			Sheet sheet = wb.createSheet("Vendor Name Changes");

			// Header style
			CellStyle headerStyle = wb.createCellStyle();
			Font headerFont = wb.createFont();
			headerFont.setBold(true);
			headerFont.setFontName("Arial");
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			// Data style
			CellStyle dataStyle = wb.createCellStyle();
			Font dataFont = wb.createFont();
			dataFont.setFontName("Arial");
			dataStyle.setFont(dataFont);

			// Header row
			Row header = sheet.createRow(0);
			String[] headers = {"Vendor ID", "Old Name", "New Name"};
			for (int i = 0; i < headers.length; i++) {
				Cell c = header.createCell(i);
				c.setCellValue(headers[i]);
				c.setCellStyle(headerStyle);
			}

			// Data rows
			int rowNum = 1;
			for (String[] change : changes) {
				Row row = sheet.createRow(rowNum++);
				for (int i = 0; i < change.length; i++) {
					Cell c = row.createCell(i);
					c.setCellValue(change[i] != null ? change[i] : "");
					c.setCellStyle(dataStyle);
				}
			}

			sheet.setColumnWidth(0, 5000);
			sheet.setColumnWidth(1, 10000);
			sheet.setColumnWidth(2, 10000);

			try (FileOutputStream fos = new FileOutputStream(outPath)) {
				wb.write(fos);
			}
		}
	}
}
