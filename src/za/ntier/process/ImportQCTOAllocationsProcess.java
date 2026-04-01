package za.ntier.process;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import org.compiere.util.DB;

import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_AC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_OC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_Skills;
import za.co.ntier.api.model.X_ZZ_QCTO_Allocation;

/**
 * Process to Import QCTO Allocations from an attached Excel Spreadsheet.
 * author niraj
 */
@Process(name = "za.ntier.process.ImportQCTOAllocationsProcess")
public class ImportQCTOAllocationsProcess extends SvrProcess
{

	private int	m_updated_OC		= 0;
	private int	m_updated_Skills	= 0;
	private int	m_updated_AC		= 0;
	private int	m_failed			= 0;

	@Override
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
			{
				continue;
			}
			log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		int recordId = getRecord_ID();
		if (recordId <= 0)
		{
			throw new AdempiereException("This process must be run from a specific QCTO Allocation Header record.");
		}

		X_ZZ_QCTO_Allocation header = new X_ZZ_QCTO_Allocation(getCtx(), recordId, get_TrxName());

		// 1. Get the attachment from the current record
		int tableId = getTable_ID();
		MAttachment attachment = MAttachment.get(getCtx(), tableId, recordId);
		if (attachment == null || attachment.getEntryCount() == 0)
		{
			throw new AdempiereException("Please attach the QCTO Excel spreadsheet to this record before processing.");
		}

		// 2. Find the Excel file attachment (Assuming only one file or taking
		// the first .xlsx)
		MAttachmentEntry entry = null;
		for (MAttachmentEntry e : attachment.getEntries())
		{
			if (e.getName().toLowerCase().endsWith(".xlsx") || e.getName().toLowerCase().endsWith(".xls"))
			{
				entry = e;
				break;
			}
		}

		if (entry == null)
		{
			throw new AdempiereException("No Excel file (.xlsx or .xls) found in the attachments.");
		}

		// 3. Process the file using Apache POI
		try (InputStream is = entry.getInputStream(); Workbook workbook = WorkbookFactory.create(is))
		{

			String titleText = "";
			Sheet firstSheet = workbook.getSheetAt(0);
			if (firstSheet != null && firstSheet.getRow(1) != null)
			{
				titleText = getStringCellValue(firstSheet.getRow(1).getCell(0));
			}

			// Populate header fields early
			String fileName = entry.getName();
			header.setFileName(fileName);
			header.setDateReceived(new Timestamp(System.currentTimeMillis()));

			// Search for Month and Year in the fileName first, fallback to the
			// titleText found in the Excel Header
			String searchStr = fileName + " " + titleText;
			String monthName = extractMonth(searchStr);
			if (monthName != null && !monthName.isEmpty())
			{
				header.setZZ_FileMonth(monthName);
			}

			int yearId = extractYear(searchStr);
			if (yearId > 0)
			{
				header.setC_Year_ID(yearId);
			}
			header.saveEx();

			// Clear existing allocations matching this header to ensure fresh
			// import
			DB.executeUpdate("DELETE FROM ZZ_QCTO_Alloc_OC WHERE ZZ_QCTO_Allocation_ID=?", recordId, get_TrxName());
			DB.executeUpdate("DELETE FROM ZZ_QCTO_Alloc_Skills WHERE ZZ_QCTO_Allocation_ID=?", recordId, get_TrxName());
			DB.executeUpdate("DELETE FROM ZZ_QCTO_Alloc_AC WHERE ZZ_QCTO_Allocation_ID=?", recordId, get_TrxName());

			// Process OC Sheet
			processSheet(workbook, "OCs", 1, recordId);
			// Process Skills Sheet
			processSheet(workbook, "Skills", 2, recordId);
			// Process AC Sheet
			processSheet(workbook, "ACs", 3, recordId);

			// Update header status if needed
			header.setZZ_DocStatus("IM");
			header.setProcessed(true);
			header.saveEx();

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error parsing Excel document", e);
			throw new AdempiereException("Error parsing Excel document: " + e.getMessage());
		}

		return String.format(
								"Spreadsheet parsing complete. Correctly Processed - OC: %d, Skills: %d, AC: %d. Failed/Skipped Logs: %d",
								m_updated_OC, m_updated_Skills, m_updated_AC, m_failed);
	}

	private void processSheet(Workbook workbook, String sheetName, int sheetType, int recordId)
	{
		Sheet sheet = workbook.getSheet(sheetName);
		if (sheet == null)
		{
			addLog("Sheet '" + sheetName + "' not found in the workbook.");
			return;
		}

		Row headerRow = sheet.getRow(3); // Headers on row index 3
		if (headerRow == null)
		{
			addLog("Sheet '" + sheetName + "' has no header row at index 3.");
			return;
		}

		Map<String, Integer> colMap = getColumnMapping(headerRow);

		for (int i = 4; i <= sheet.getLastRowNum(); i++)
		{
			Row row = sheet.getRow(i);
			if (row == null)
			{
				continue; // Skip empty rows
			}

			try
			{
				// Extract columns based on mapping
				String uniqueAllocationNo = getMappedValue(row, colMap, "UniqueAllocationNo");
				String tradingName = getMappedValue(row, colMap, "TradingName");
				String legalName = getMappedValue(row, colMap, "LegalName");
				String physicalAddress = getMappedValue(row, colMap, "PhysicalAddress");
				String buildingName = getMappedValue(row, colMap, "BuildingName");
				String townCity = getMappedValue(row, colMap, "TownCity");
				String province = getMappedValue(row, colMap, "Province");
				String postalCode = getMappedValue(row, colMap, "PostalCode");

				String titleContact = getMappedValue(row, colMap, "TitleContact");
				String positionDesignation = getMappedValue(row, colMap, "PositionDesignation");
				String surnameContact = getMappedValue(row, colMap, "SurnameContact");
				String fullNameContact = getMappedValue(row, colMap, "FullNameContact");
				String emailContact = getMappedValue(row, colMap, "EmailContact");

				String titleAddContact = getMappedValue(row, colMap, "TitleAddContact");
				String surnameAddContact = getMappedValue(row, colMap, "SurnameAddContact");
				String fullNameAddContact = getMappedValue(row, colMap, "FullNameAddContact");
				String emailAddContact = getMappedValue(row, colMap, "EmailAddContact");

				String cipcRegistration = getMappedValue(row, colMap, "CIPC");
				String occQualificationTitle = getMappedValue(row, colMap, "OccQualification");
				String saqaId = getMappedValue(row, colMap, "SAQAId");
				String nqfLevel = getMappedValue(row, colMap, "NQFLevel");
				String qualityPartner = getMappedValue(row, colMap, "QualityPartner");
				String allocationMonthStr = getMappedValue(row, colMap, "AllocationMonth");
				String siteVisitDateStr = getMappedValue(row, colMap, "SiteVisitDate");

				String finalAllocMonth = allocationMonthStr;
				Timestamp allocMonthDate = parseDate(allocationMonthStr);
				if (allocMonthDate != null)
				{
					String periodName = getAllocationMonthFromPeriod(allocMonthDate);
					if (periodName != null && !periodName.isEmpty())
					{
						finalAllocMonth = periodName;
					}
				}

				// Basic validation to break loop if we hit an empty line at the
				// end of the file
				if (uniqueAllocationNo == null || uniqueAllocationNo.trim().isEmpty())
				{
					continue; // Skip lines with no Allocation No
				}

				if (sheetType == 1)
				{ // OC
					X_ZZ_QCTO_Alloc_OC allocLine = new X_ZZ_QCTO_Alloc_OC(getCtx(), 0, get_TrxName());
					allocLine.setZZ_QCTO_Allocation_ID(recordId);

					String idStr = getMappedValue(row, colMap, "ID");
					if (!idStr.isEmpty())
					{
						try
						{
							allocLine.setLineNo(Integer.parseInt(idStr));
						}
						catch (Exception ex)
						{}
					}

					allocLine.setZZ_AllocationNo(uniqueAllocationNo);

					allocLine.setName(fullNameContact);
					allocLine.setZZTradeName(tradingName);
					allocLine.setZZLegalName(legalName);
					allocLine.setAddress1(physicalAddress);
					allocLine.setAddress2(buildingName);
					allocLine.setCity(townCity);
					allocLine.setRegion(province);
					allocLine.setPostalcode(postalCode);
					allocLine.setZZ_ContactTitle(titleContact);
					allocLine.setZZ_Designation(positionDesignation);
					allocLine.setZZSurname(surnameContact);
					allocLine.setEMail(emailContact);

					allocLine.setZZ_AltContactName(fullNameAddContact);
					allocLine.setZZ_AltContactTitle(titleAddContact);
					allocLine.setZZ_AltContactSurname(surnameAddContact);
					allocLine.setZZ_AltContactEmail(emailAddContact);
					allocLine.setZZ_CIPCNumber(cipcRegistration);
					allocLine.setZZ_Qualification(occQualificationTitle);
					allocLine.setZZ_SAQAIDOrSPID(saqaId);
					allocLine.setZZ_NQF_Level(getNQFLevelValue(nqfLevel));
					allocLine.setZZ_QualityPartner(qualityPartner);
					allocLine.setZZ_AllocationMonth(finalAllocMonth);

					if (siteVisitDateStr != null && !siteVisitDateStr.isEmpty())
					{
						allocLine.setZZ_SiteVisitDate(parseDate(siteVisitDateStr));
					}
					if (allocLine.save())
						m_updated_OC++;

				}
				else if (sheetType == 2)
				{ // Skills
					X_ZZ_QCTO_Alloc_Skills allocLine = new X_ZZ_QCTO_Alloc_Skills(getCtx(), 0, get_TrxName());
					allocLine.setZZ_QCTO_Allocation_ID(recordId);

					String idStr = getMappedValue(row, colMap, "ID");
					if (!idStr.isEmpty())
					{
						try
						{
							allocLine.setLineNo(Integer.parseInt(idStr));
						}
						catch (Exception ex)
						{}
					}

					allocLine.setZZ_AllocationNo(uniqueAllocationNo);

					allocLine.setName(fullNameContact);
					allocLine.setZZTradeName(tradingName);
					allocLine.setZZLegalName(legalName);
					allocLine.setAddress1(physicalAddress);
					allocLine.setAddress2(buildingName);
					allocLine.setCity(townCity);
					allocLine.setRegion(province);
					allocLine.setPostalcode(postalCode);
					allocLine.setZZ_ContactTitle(titleContact);
					allocLine.setZZ_Designation(positionDesignation);
					allocLine.setZZSurname(surnameContact);
					allocLine.setEMail(emailContact);

					allocLine.setZZ_AltContactName(fullNameAddContact);
					allocLine.setZZ_AltContactTitle(titleAddContact);
					allocLine.setZZ_AltContactSurname(surnameAddContact);
					allocLine.setZZ_AltContactEmail(emailAddContact);
					allocLine.setZZ_CIPCNumber(cipcRegistration);
					allocLine.setZZ_Qualification(occQualificationTitle);
					allocLine.setZZ_SAQAIDOrSPID(saqaId);
					allocLine.setZZ_NQF_Level(getNQFLevelValue(nqfLevel));
					allocLine.setZZ_QualityPartner(qualityPartner);
					allocLine.setZZ_AllocationMonth(finalAllocMonth);

					if (siteVisitDateStr != null && !siteVisitDateStr.isEmpty())
					{
						allocLine.setZZ_SiteVisitDate(parseDate(siteVisitDateStr));
					}
					if (allocLine.save())
						m_updated_Skills++;

				}
				else if (sheetType == 3)
				{ // AC
					X_ZZ_QCTO_Alloc_AC allocLine = new X_ZZ_QCTO_Alloc_AC(getCtx(), 0, get_TrxName());
					allocLine.setZZ_QCTO_Allocation_ID(recordId);

					String idStr = getMappedValue(row, colMap, "ID");
					if (!idStr.isEmpty())
					{
						try
						{
							allocLine.setLineNo(Integer.parseInt(idStr));
						}
						catch (Exception ex)
						{}
					}

					allocLine.setZZ_AllocationNo(uniqueAllocationNo);

					allocLine.setName(fullNameContact);
					allocLine.setZZTradeName(tradingName);
					allocLine.setZZLegalName(legalName);
					allocLine.setAddress1(physicalAddress);
					allocLine.setAddress2(buildingName);
					allocLine.setCity(townCity);
					allocLine.setRegion(province);
					allocLine.setPostalcode(postalCode);
					allocLine.setZZ_ContactTitle(titleContact);
					allocLine.setZZ_Designation(positionDesignation);
					allocLine.setZZSurname(surnameContact);
					allocLine.setEMail(emailContact);

					allocLine.setZZ_AltContactName(fullNameAddContact);
					allocLine.setZZ_AltContactTitle(titleAddContact);
					allocLine.setZZ_AltContactSurname(surnameAddContact);
					allocLine.setZZ_AltContactEmail(emailAddContact);
					allocLine.setZZ_CIPCNumber(cipcRegistration);
					allocLine.setZZ_Qualification(occQualificationTitle);
					allocLine.setZZ_SAQAIDOrSPID(saqaId);
					allocLine.setZZ_NQF_Level(getNQFLevelValue(nqfLevel));
					allocLine.setZZ_QualityPartner(qualityPartner);
					allocLine.setZZ_AllocationMonth(finalAllocMonth);

					if (siteVisitDateStr != null && !siteVisitDateStr.isEmpty())
					{
						allocLine.setZZ_SiteVisitDate(parseDate(siteVisitDateStr));
					}
					if (allocLine.save())
						m_updated_AC++;
				}

			}
			catch (Exception rowEx)
			{
				log.log(Level.WARNING, "Error on row " + i + " in sheet " + sheetName, rowEx);
				addLog(sheetName + " Sheet Line " + i + ": Missing or invalid data - " + rowEx.getMessage());
				m_failed++;
			}
		}
	}

	private Timestamp parseDate(String dateStr)
	{
		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
			return new Timestamp(sdf.parse(dateStr).getTime());
		}
		catch (ParseException e)
		{
			throw new AdempiereException(e + "Could not parse date");
		}
	}

	/**
	 * Helper to map Excel NQF Level to Reference List value
	 */
	private String getNQFLevelValue(String nqfLevelExcel)
	{
		if (nqfLevelExcel == null || nqfLevelExcel.trim().isEmpty())
		{
			return null;
		}
		String searchStr = nqfLevelExcel.trim();

		// 1. Try to match exact Name or Value in AD_Ref_List
		String value = DB.getSQLValueString(get_TrxName(),
											"SELECT l.Value FROM AD_Ref_List l "
															+ "INNER JOIN AD_Reference r ON l.AD_Reference_ID = r.AD_Reference_ID "
															+ "WHERE r.AD_Reference_UU=? AND (l.Name=? OR l.Value=?)",
											"2b47e027-cb5a-45d6-8fc6-2c9bc9c6c3ad", searchStr, searchStr);

		if (value != null)
		{
			return value;
		}

		// 2. If excel is e.g. "NQF Level 04", extract last 2 chars and try
		// again
		if (searchStr.length() >= 2)
		{
			String lastTwo = searchStr.substring(searchStr.length() - 2).trim();
			value = DB.getSQLValueString(	get_TrxName(),
											"SELECT l.Value FROM AD_Ref_List l "
															+ "INNER JOIN AD_Reference r ON l.AD_Reference_ID = r.AD_Reference_ID "
															+ "WHERE r.AD_Reference_UU=? AND l.Value=?",
											"2b47e027-cb5a-45d6-8fc6-2c9bc9c6c3ad", lastTwo);
			if (value != null)
			{
				return value;
			}
		}

		// Fallback: just return the truncated string (up to 2 chars) as
		// previously attempted
		return searchStr.length() > 2 ? searchStr.substring(0, 2) : searchStr;
	}

	private String getMappedValue(Row row, Map<String, Integer> map, String key)
	{
		Integer colIndex = map.get(key);
		if (colIndex != null)
		{
			return getStringCellValue(row.getCell(colIndex));
		}
		return "";
	}

	private Map<String, Integer> getColumnMapping(Row headerRow)
	{
		Map<String, Integer> map = new HashMap<>();
		if (headerRow == null)
			return map;
		for (int i = 0; i < headerRow.getLastCellNum(); i++)
		{
			Cell cell = headerRow.getCell(i);
			String header = getStringCellValue(cell).trim().toLowerCase();
			if (header.isEmpty())
				continue;

			// Clean up non-breaking spaces and double spaces
			header = header.replace("\u00A0", " ").replaceAll("\\s+", " ");

			if (header.equals("id"))
				map.put("ID", i);
			else if (header.contains("unique allocation number"))
				map.put("UniqueAllocationNo", i);
			else if (header.contains("trading name"))
				map.put("TradingName", i);
			else if (header.contains("legal name"))
				map.put("LegalName", i);
			else if (header.contains("physical address"))
				map.put("PhysicalAddress", i);
			else if (header.contains("building name"))
				map.put("BuildingName", i);
			else if (header.contains("town / city") || header.contains("town/city"))
				map.put("TownCity", i);
			else if (header.contains("province"))
				map.put("Province", i);
			else if (header.contains("postal code"))
				map.put("PostalCode", i);
			else if (header.contains("title of contact person"))
				map.put("TitleContact", i);
			else if (header.contains("position / designation") || header.contains("position/designation"))
				map.put("PositionDesignation", i);
			else if (header.contains("surname of contact person"))
				map.put("SurnameContact", i);
			else if (header.contains("full name(s) of contact person"))
				map.put("FullNameContact", i);
			else if (header.contains("email address of contact person"))
				map.put("EmailContact", i);

			else if (header.contains("title of additional"))
				map.put("TitleAddContact", i);
			else if (header.contains("surname of additional"))
				map.put("SurnameAddContact", i);
			else if (header.contains("full name(s) of additional"))
				map.put("FullNameAddContact", i);
			else if (header.contains("email address of additional"))
				map.put("EmailAddContact", i);

			else if (header.contains("cipc registration")	|| header.contains("emis for tvet")
						|| header.contains("lra reference number"))
				map.put("CIPC", i);
			else if (header.contains("title of occupational qualification")
						|| header.contains("title of occupational skills programme"))
				map.put("OccQualification", i);
			else if (header.contains("saqa id") || header.contains("id of occupational skills programme")
						|| header.contains("saqa id number"))
				map.put("SAQAId", i);
			else if (header.contains("nqf level"))
				map.put("NQFLevel", i);
			else if (header.contains("quality partner"))
				map.put("QualityPartner", i);
			else if (header.contains("allocation month"))
				map.put("AllocationMonth", i);
			else if (header.contains("site visit date"))
				map.put("SiteVisitDate", i);
		}
		return map;
	}

	private String extractMonth(String text)
	{
		if (text == null)
			return null;
		text = text.toLowerCase();
		if (text.contains("january") || text.contains("jan "))
			return "01";
		if (text.contains("february") || text.contains("feb "))
			return "02";
		if (text.contains("march") || text.contains("mar "))
			return "03";
		if (text.contains("april") || text.contains("apr "))
			return "04";
		if (text.contains("may") || text.contains("may "))
			return "05";
		if (text.contains("june") || text.contains("jun "))
			return "06";
		if (text.contains("july") || text.contains("jul "))
			return "07";
		if (text.contains("august") || text.contains("aug "))
			return "08";
		if (text.contains("september") || text.contains("sep "))
			return "09";
		if (text.contains("october") || text.contains("oct "))
			return "10";
		if (text.contains("november") || text.contains("nov "))
			return "11";
		if (text.contains("december") || text.contains("dec "))
			return "12";
		return null;
	}

	private int extractYear(String text)
	{
		if (text == null)
			return 0;
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})").matcher(text);
		if (m.find())
		{
			String yearStr = m.group(1);
			int yearId = DB.getSQLValue(get_TrxName(),
										"SELECT C_Year_ID FROM C_Year WHERE FiscalYear=? AND AD_Client_ID=?", yearStr, getAD_Client_ID());
			if (yearId <= 0)
			{
				yearId = DB.getSQLValue(get_TrxName(),
										"SELECT C_Year_ID FROM C_Year WHERE Year=? AND AD_Client_ID=0", yearStr);
			}
			return yearId > 0 ? yearId : 0;
		}
		return 0;
	}

	private String getAllocationMonthFromPeriod(Timestamp date)
	{
		if (date == null)
			return null;

		String name = DB.getSQLValueString(	get_TrxName(),
											"SELECT Name FROM C_Period WHERE ? BETWEEN StartDate AND EndDate AND AD_Client_ID=?",
											date, getAD_Client_ID());

		if (name == null || name.isEmpty())
		{
			name = DB.getSQLValueString(get_TrxName(),
										"SELECT Name FROM C_Period WHERE ? BETWEEN StartDate AND EndDate AND AD_Client_ID=0",
										date);
		}

		if (name == null || name.isEmpty())
		{
			return new SimpleDateFormat("MMM-yyyy").format(date);
		}
		return name;
	}

	/**
	 * Helper method to safely extract strings from POI Cells.
	 */
	private String getStringCellValue(Cell cell)
	{
		if (cell == null)
		{
			return "";
		}

		if (cell.getCellType() == CellType.STRING)
		{
			return cell.getStringCellValue().trim();
		}
		else if (cell.getCellType() == CellType.NUMERIC)
		{
			if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell))
			{
				return new SimpleDateFormat("yyyy/MM/dd").format(cell.getDateCellValue());
			}
			else
			{
				double d = cell.getNumericCellValue();
				if (d == (long) d)
				{
					return String.format("%d", (long) d);
				}
				else
				{
					return String.format("%s", d);
				}
			}
		}
		else if (cell.getCellType() == CellType.BOOLEAN)
		{
			return String.valueOf(cell.getBooleanCellValue());
		}
		else if (cell.getCellType() == CellType.FORMULA)
		{
			try
			{
				return cell.getStringCellValue().trim();
			}
			catch (Exception e)
			{
				return String.valueOf(cell.getNumericCellValue());
			}
		}
		else
		{
			return "";
		}
	}
}
