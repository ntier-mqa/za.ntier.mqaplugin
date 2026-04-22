package za.ntier.process;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.X_ZZ_NAMB_Alloc_TTC;
import za.co.ntier.api.model.X_ZZ_NAMB_Alloc_Trades;
import za.co.ntier.api.model.X_ZZ_NAMB_Allocations;

/**
 * Process to Import NAMB Allocations from an attached Excel Spreadsheet.
 * 
 * @author niraj
 */
@Process(name = "za.ntier.process.ImportNAMBAllocationsProcess")
public class ImportNAMBAllocationsProcess extends SvrProcess
{

	private int		m_tradesCreated		= 0;
	private int		m_ttcCreated		= 0;
	private int		m_failed			= 0;
	private String	m_allocationMonth	= null;

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
			throw new AdempiereException("This process must be run from a specific NAMB Allocation Header record.");
		}

		X_ZZ_NAMB_Allocations header = new X_ZZ_NAMB_Allocations(getCtx(), recordId, get_TrxName());

		int tableId = getTable_ID();
		MAttachment attachment = MAttachment.get(getCtx(), tableId, recordId);
		if (attachment == null || attachment.getEntryCount() == 0)
		{
			throw new AdempiereException("Please attach the NAMB Excel spreadsheet to this record before processing.");
		}

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

		try (InputStream is = entry.getInputStream(); Workbook workbook = WorkbookFactory.create(is))
		{
			String titleText = "";
			Sheet firstSheet = workbook.getSheetAt(0);
			if (firstSheet != null)
			{
				// Sometimes month is specified on a line like "MONTH"
				// We'll search for it or just fallback to filename.
				for (int i = 0; i < 20; i++)
				{
					Row r = firstSheet.getRow(i);
					if (r != null && r.getCell(0) != null)
					{
						String val = getStringCellValue(r.getCell(0)).toLowerCase();
						if (val.contains("monthly") || val.contains("allocations"))
						{
							titleText += " " + getStringCellValue(r.getCell(0));
						}
					}
				}
			}

			String fileName = entry.getName();
			header.setFileName(fileName);
			header.setDateReceived(new Timestamp(System.currentTimeMillis()));

			String searchStr = fileName + " " + titleText;
			String monthName = extractMonth(searchStr);
			String yearString = extractYearString(searchStr);
			if (monthName != null && !monthName.isEmpty())
			{
				header.setZZ_FileMonth(monthName);
				String abbrev = getMonthAbbrev(monthName);
				if (yearString != null && yearString.length() == 4)
				{
					m_allocationMonth = abbrev + "-" + yearString.substring(2);
				}
				else
				{
					m_allocationMonth = abbrev;
				}
			}

			int yearId = extractYear(searchStr);
			if (yearId > 0)
			{
				header.setC_Year_ID(yearId);
			}
			header.saveEx();

			DB.executeUpdate("DELETE FROM ZZ_NAMB_Alloc_Trades WHERE ZZ_NAMB_Allocations_ID=?", recordId, get_TrxName());
			DB.executeUpdate("DELETE FROM ZZ_NAMB_Alloc_TTC WHERE ZZ_NAMB_Allocations_ID=?", recordId, get_TrxName());

			processSheet(workbook, workbook.getActiveSheetIndex(), recordId);

			header.setZZ_DocStatus(X_ZZ_NAMB_Allocations.ZZ_DOCSTATUS_Imported);
			header.saveEx();

		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error parsing Excel document", e);
			throw new AdempiereException("Error parsing Excel document: " + e.getMessage());
		}

		return String.format(	"Spreadsheet parsing complete. Trades (SDP) Created: %d, TTC Created: %d, Failed/Skipped: %d",
								m_tradesCreated, m_ttcCreated, m_failed);
	}

	private void processSheet(Workbook workbook, int sheetIndex, int recordId)
	{
		Sheet sheet = workbook.getSheetAt(sheetIndex);
		if (sheet == null)
		{
			addLog("Sheet at index " + sheetIndex + " not found in the workbook.");
			return;
		}

		int headerRowIndex = -1;
		Row headerRow = null;
		for (int i = 0; i < Math.min(30, sheet.getLastRowNum()); i++)
		{
			Row r = sheet.getRow(i);
			if (r == null)
				continue;
			for (int j = 0; j < r.getLastCellNum(); j++)
			{
				if ("DATE RECEIVED WITH ALL ATTACHMENTS".equalsIgnoreCase(getStringCellValue(r.getCell(j))))
				{
					headerRowIndex = i;
					headerRow = r;
					break;
				}
			}
			if (headerRowIndex != -1)
				break;
		}

		if (headerRow == null)
		{
			addLog("Sheet '" + sheet.getSheetName() + "' has no valid header row containing 'DATE RECEIVED WITH ALL ATTACHMENTS'.");
			return;
		}

		Map<String, Integer> colMap = getColumnMapping(headerRow);

		for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++)
		{
			Row row = sheet.getRow(i);
			if (row == null)
			{
				continue;
			}

			try
			{
				// Only process rows where Allocated column contains MQA
				String allocated = getMappedValue(row, colMap, "Allocated");
				if (allocated == null || !allocated.toUpperCase().contains("MQA"))
				{
					continue;
				}

				String sdpValue = getMappedValue(row, colMap, "SDP");
				String ttcValue = getMappedValue(row, colMap, "TTC");

				boolean hasSdp = sdpValue != null && !sdpValue.trim().isEmpty();
				boolean hasTtc = ttcValue != null && !ttcValue.trim().isEmpty();

				if (!hasSdp && !hasTtc)
				{
					continue;
				}

				// Read common fields
				String dateReceivedStr = getMappedValue(row, colMap, "DateReceived");
				String uan = getMappedValue(row, colMap, "UAN");
				String centreName = getMappedValue(row, colMap, "CentreName");
				String contact = getMappedValue(row, colMap, "ContactPerson");
				String phone = getMappedValue(row, colMap, "Phone");
				String email = getMappedValue(row, colMap, "Email");
				String address = getMappedValue(row, colMap, "Address");
				String town = getMappedValue(row, colMap, "Town");
				String province = getMappedValue(row, colMap, "Province");
				String scopeOfTrades = getMappedValue(row, colMap, "ScopeOfTrades");

				Timestamp dateRecv = null;
				if (dateReceivedStr != null && !dateReceivedStr.isEmpty())
				{
					dateRecv = parseDateFlexible(dateReceivedStr);
				}

				// If SDP column has a value, create a Trades tab record
				if (hasSdp)
				{
					X_ZZ_NAMB_Alloc_Trades tradesLine = new X_ZZ_NAMB_Alloc_Trades(getCtx(), 0, get_TrxName());
					tradesLine.setZZ_NAMB_Allocations_ID(recordId);
					populateCommonFields(tradesLine, dateRecv, uan, centreName, contact, phone, email, address, town, province, allocated, m_allocationMonth);
					tradesLine.setZZ_ScopeOfTrades(scopeOfTrades);

					if (tradesLine.save())
					{
						m_tradesCreated++;
						generateAllocationNoIfMissing(tradesLine, uan, dateRecv);
					}
					else
					{
						m_failed++;
						addLog("Failed to save Trades record at row " + i);
					}
				}

				// If TTC column has a value, create a TTC tab record
				if (hasTtc)
				{
					X_ZZ_NAMB_Alloc_TTC ttcLine = new X_ZZ_NAMB_Alloc_TTC(getCtx(), 0, get_TrxName());
					ttcLine.setZZ_NAMB_Allocations_ID(recordId);
					populateCommonFields(ttcLine, dateRecv, uan, centreName, contact, phone, email, address, town, province, "MQA", m_allocationMonth);
					ttcLine.setZZ_ScopeOfTrades(scopeOfTrades);

					if (ttcLine.save())
					{
						m_ttcCreated++;
						generateAllocationNoIfMissing(ttcLine, uan, dateRecv);
					}
					else
					{
						m_failed++;
						addLog("Failed to save TTC record at row " + i);
					}
				}
			}
			catch (Exception rowEx)
			{
				log.log(Level.WARNING, "Error on row " + i + " in sheet " + sheet.getSheetName(), rowEx);
				addLog("Sheet Line " + i + ": Missing or invalid data - " + rowEx.getMessage());
				m_failed++;
			}
		}
	}

	/**
	 * Populates common fields shared between Trades and TTC models via PO.
	 */
	private void populateCommonFields(	PO record, Timestamp dateRecv, String uan,
										String centreName, String contact, String phone, String email,
										String address, String town, String province, String allocated, String allocationMonth)
	{
		if (dateRecv != null)
		{
			record.set_ValueOfColumn("DateReceived", dateRecv);
		}
		record.set_ValueOfColumn("ZZ_AllocationNo", uan);
		record.set_ValueOfColumn("ZZLegalName", centreName);
		record.set_ValueOfColumn("ContactName", contact);
		record.set_ValueOfColumn("Phone", phone);
		record.set_ValueOfColumn("EMail", email);
		record.set_ValueOfColumn("Address", address);
		record.set_ValueOfColumn("City", town);
		record.set_ValueOfColumn("Region", province);
		record.set_ValueOfColumn("ZZ_Allocated", allocated);
		record.set_ValueOfColumn("ZZ_AllocationMonth", allocationMonth);
	}

	/**
	 * Auto-generates an Allocation No using the MQA-<date>-<docNo> format
	 * when the original UAN is blank.
	 */
	private void generateAllocationNoIfMissing(PO record, String uan, Timestamp dateRecv)
	{
		if (uan == null || uan.trim().isEmpty())
		{
			String datePart = "YYYY-MM-DD";
			if (dateRecv != null)
			{
				datePart = new SimpleDateFormat("yyyy-MM-dd").format(dateRecv);
			}

			String docNo = (String) record.get_Value("DocumentNo");
			String suffix = (docNo != null && !docNo.isEmpty()) ? docNo : String.valueOf(record.get_ID());
			record.set_ValueOfColumn("ZZ_AllocationNo", "MQA-" + datePart + "-" + suffix);
			record.saveEx();
		}
	}

	private Timestamp parseDateFlexible(String dateStr)
	{
		dateStr = dateStr.trim();
		String[] formats = new String[] {
											"yyyy/MM/dd",
												"dd-MM-yyyy",
												"yyyy-MM-dd",
												"dd/MM/yyyy",
												"dd-MM--yyyy",
												"dd-MM--yyyy",
												"yyyy.MM.dd"
		};

		// Clean up common typos in manual excel entries e.g., '30-01--2026'
		dateStr = dateStr.replace("--", "-");

		for (String format : formats)
		{
			try
			{
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				sdf.setLenient(false);
				return new Timestamp(sdf.parse(dateStr).getTime());
			}
			catch (ParseException e)
			{}
		}
		try
		{
			return Timestamp.valueOf(dateStr + " 00:00:00");
		}
		catch (Exception ex)
		{

		}
		return null;
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

			header = header.replace("\u00A0", " ").replaceAll("\\s+", " ");

			if (header.contains("date received with all attachments"))
				map.put("DateReceived", i);
			else if (header.equals("uan"))
				map.put("UAN", i);
			else if (header.contains("centre name"))
				map.put("CentreName", i);
			else if (header.contains("centre manager / contact person") || header.contains("centre manager/contact person"))
				map.put("ContactPerson", i);
			else if (header.contains("contact details"))
				map.put("Phone", i);
			else if (header.contains("e-mail"))
				map.put("Email", i);
			else if (header.contains("physical address of the centre"))
				map.put("Address", i);
			else if (header.contains("town"))
				map.put("Town", i);
			else if (header.contains("province"))
				map.put("Province", i);
			else if (header.contains("allocated"))
				map.put("Allocated", i);
			else if (header.contains("scope of trades"))
				map.put("ScopeOfTrades", i);
			else if (header.equals("sdp") || header.contains("skills development provider"))
				map.put("SDP", i);
			else if (header.equals("ttc") || header.contains("trade test centre"))
				map.put("TTC", i);
		}
		return map;
	}

	private String extractMonth(String text)
	{
		if (text == null)
			return null;
		text = text.toLowerCase();
		if (text.contains("january") || text.contains("jan "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_January;
		if (text.contains("february") || text.contains("feb "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_February;
		if (text.contains("march") || text.contains("mar "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_March;
		if (text.contains("april") || text.contains("apr "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_April;
		if (text.contains("may") || text.contains("may "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_May;
		if (text.contains("june") || text.contains("jun "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_June;
		if (text.contains("july") || text.contains("jul "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_July;
		if (text.contains("august") || text.contains("aug "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_August;
		if (text.contains("september") || text.contains("sep "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_September;
		if (text.contains("october") || text.contains("oct "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_October;
		if (text.contains("november") || text.contains("nov "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_November;
		if (text.contains("december") || text.contains("dec "))
			return X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_December;
		return null;
	}

	private int extractYear(String text)
	{
		String yearStr = extractYearString(text);
		if (yearStr != null)
		{
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

	private String extractYearString(String text)
	{
		if (text == null)
			return null;
		Matcher m = Pattern.compile("(20\\d{2})").matcher(text);
		if (m.find())
		{
			return m.group(1);
		}
		return null;
	}

	private String getMonthAbbrev(String monthId)
	{
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_January.equals(monthId))
			return "Jan";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_February.equals(monthId))
			return "Feb";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_March.equals(monthId))
			return "Mar";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_April.equals(monthId))
			return "Apr";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_May.equals(monthId))
			return "May";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_June.equals(monthId))
			return "Jun";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_July.equals(monthId))
			return "Jul";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_August.equals(monthId))
			return "Aug";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_September.equals(monthId))
			return "Sep";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_October.equals(monthId))
			return "Oct";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_November.equals(monthId))
			return "Nov";
		if (X_ZZ_NAMB_Allocations.ZZ_FILEMONTH_December.equals(monthId))
			return "Dec";
		return "";
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
			if (DateUtil.isCellDateFormatted(cell))
			{
				return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
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
