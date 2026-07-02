package za.co.ntier.wsp_atr.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MBPartner;
import org.compiere.model.X_AD_Process;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@Process(name = "za.co.ntier.wsp_atr.process.UpdateBPartnerNamesFromCSV")
public class UpdateBPartnerNamesFromCSV extends SvrProcess
{

	private String p_FilePath = "";

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (name.equals("FileName"))
			{
				p_FilePath = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		InputStream is = null;
		String fileName = "";

		// 1. Try to load from FilePath parameter first
		if (p_FilePath != null && !p_FilePath.trim().isEmpty())
		{
			File file = new File(p_FilePath);
			if (file.exists())
			{
				is = new FileInputStream(file);
				fileName = file.getName();
			}
		}

		// 2. If no FilePath, try to load from the Process Attachment
		if (is == null)
		{
			MAttachment attachment = MAttachment.get(getCtx(), X_AD_Process.Table_ID, getProcessInfo().getAD_Process_ID());
			if (attachment != null && attachment.getEntryCount() > 0)
			{
				MAttachmentEntry entry = attachment.getEntry(0);
				is = new ByteArrayInputStream(entry.getData());
				fileName = entry.getName();
			}
			else
			{
				throw new Exception("No File Path provided and no Attachment found on the Process. Please attach a file or provide a path.");
			}
		}

		// stats[0]=updatedCount, stats[1]=updatedNameOnly, stats[2]=updatedTradingNameOnly,
		// stats[3]=updatedBoth, stats[4]=skippedMatchCount, stats[5]=notFoundInDbCount,
		// stats[6]=totalRowsInFile
		int[] stats = new int[7];

		List<String> successLog = new ArrayList<>();
		List<String> errorLog = new ArrayList<>();

		long timestamp = System.currentTimeMillis();

		Map<String, Integer> currentClientBPs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String loadSql = "SELECT Value, C_BPartner_ID FROM C_BPartner WHERE Value IS NOT NULL AND AD_Client_ID = ?";
		try (PreparedStatement pstmt = DB.prepareStatement(loadSql, get_TrxName()))
		{
			pstmt.setInt(1, getAD_Client_ID());
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					currentClientBPs.put(rs.getString(1).trim(), rs.getInt(2));
				}
			}
		}

		boolean isCsv = fileName.toLowerCase().endsWith(".csv");

		try
		{
			if (isCsv)
			{
				processCsv(is, currentClientBPs, successLog, errorLog, stats);
			}
			else
			{
				processExcel(is, currentClientBPs, successLog, errorLog, stats);
			}
		}
		finally
		{
			if (is != null)
			{
				is.close();
			}
		}

		// Write results to /tmp
		File txtLogFile = new File("/tmp", "Update_BPartner_Names_Log_" + timestamp + ".txt");
		try (PrintWriter pw = new PrintWriter(new FileWriter(txtLogFile)))
		{
			pw.println("====== SUMMARY ======");
			pw.println("Total Data Rows in File: " + stats[6]);
			pw.println("Total Rows Processed: " + (stats[0] + errorLog.size()));
			pw.println("---------------------");
			pw.println("Total Successful Rows: " + stats[0]);
			pw.println("   -> Updated Name ONLY: " + stats[1]);
			pw.println("   -> Updated Trading Name ONLY: " + stats[2]);
			pw.println("   -> Updated BOTH Name and Trading Name: " + stats[3]);
			pw.println("Not successful updates (Errors): " + (errorLog.size() - stats[4] - stats[5]));
			pw.println("Skipped (Already Match DB): " + stats[4]);
			pw.println("Skipped (Not found BP in DB): " + stats[5]);
			pw.println("=====================");
			pw.println();

			pw.println("====== FAILED / SKIPPED ROWS (" + errorLog.size() + ") ======");
			for (String errMsg : errorLog)
			{
				pw.println("FAILED: " + errMsg);
			}
			pw.println();
			pw.println("====== SUCCESSFUL UPDATES (" + stats[0] + ") ======");
			for (String successMsg : successLog)
			{
				pw.println("SUCCESS: " + successMsg);
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "Could not write to text log file: " + txtLogFile.getAbsolutePath(), ex);
		}

		File htmlLogFile = new File("/tmp", "Verified_BPartner_Names_Log_" + timestamp + ".html");
		try (PrintWriter pw = new PrintWriter(new FileWriter(htmlLogFile)))
		{
			pw.println("<html><head><style>body { font-family: sans-serif; } .success { color: green; font-weight: bold; } .error { color: red; font-weight: bold; } li { margin-bottom: 5px; } table { border-collapse: collapse; width: 60%; margin-bottom: 20px; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } th { background-color: #f2f2f2; }</style></head><body>");
			pw.println("<h2>Business Partner Names Update Report</h2>");

			pw.println("<h3>Summary Statistics</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Category</th><th>Count</th></tr>");
			pw.println("<tr style='background-color: #f9f9f9; font-weight: bold;'><td>Total Data Rows in File</td><td>" + stats[6] + "</td></tr>");
			pw.println(	"<tr style='background-color: #f9f9f9; font-weight: bold;'><td>Total Rows Processed</td><td>"	+ (stats[0] + errorLog.size())
						+ "</td></tr>");
			pw.println("<tr><td colspan='2' style='background-color: #ddd;'></td></tr>");
			pw.println("<tr><td>Updated Name <b>ONLY</b></td><td>" + stats[1] + "</td></tr>");
			pw.println("<tr><td>Updated Trading Name <b>ONLY</b></td><td>" + stats[2] + "</td></tr>");
			pw.println("<tr><td>Updated <b>BOTH</b> Names</td><td>" + stats[3] + "</td></tr>");
			pw.println("<tr style='background-color: #e6ffe6; font-weight: bold;'><td>Total Successful Rows</td><td>" + stats[0] + "</td></tr>");
			pw.println("<tr><td>Not successful updates (Errors)</td><td>" + (errorLog.size() - stats[4] - stats[5]) + "</td></tr>");
			pw.println("<tr><td>Skipped (Already Match DB)</td><td>" + stats[4] + "</td></tr>");
			pw.println("<tr><td>Skipped (Not found BP in DB)</td><td>" + stats[5] + "</td></tr>");
			pw.println("</table><hr/>");

			pw.println("<h3 class='error'>❌ FAILED / SKIPPED ROWS (" + errorLog.size() + ")</h3><ul>");
			for (String errMsg : errorLog)
			{
				String htmlMsg = errMsg	.replace("Row ", "<span style='color: black;'>Row </span>")
										.replace("SDL:", "<span style='color: black;'>SDL:</span>")
										.replace("Reason:", "<span style='color: black;'>Reason:</span>");
				pw.println("<li class='error'>❌ " + htmlMsg + "</li>");
			}
			pw.println("</ul><br/>");

			pw.println("<h3 class='success'>✅ SUCCESSFUL UPDATES (" + stats[0] + ")</h3><ul>");
			for (String successMsg : successLog)
			{
				String htmlMsg = successMsg	.replace("SDL:", "<span style='color: black;'>SDL:</span>")
											.replace("Name:", "<span style='color: black;'>Name:</span>");
				pw.println("<li class='success'>✅ " + htmlMsg + "</li>");
			}
			pw.println("</ul></body></html>");
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "Could not write to HTML log file: " + htmlLogFile.getAbsolutePath(), ex);
		}

		if (getProcessInfo() != null)
		{
			getProcessInfo().setExportFile(htmlLogFile);
		}

		if (processUI != null)
		{
			processUI.download(htmlLogFile);
		}

		return "Process completed. Check logs at: " + txtLogFile.getAbsolutePath() + " and " + htmlLogFile.getAbsolutePath();
	}

	private void processExcel(	InputStream is, Map<String, Integer> currentClientBPs, List<String> successLog, List<String> errorLog,
								int[] stats) throws Exception
	{
		try (Workbook workbook = WorkbookFactory.create(is))
		{
			Sheet sheet = workbook.getSheetAt(0); // Assuming first sheet
			Row headerRow = sheet.getRow(0);

			int sdlColIdx = -1;
			int nameColIdx = -1;
			int tradingNameColIdx = -1;

			DataFormatter formatter = new DataFormatter();

			if (headerRow != null)
			{
				for (int i = 0; i < headerRow.getLastCellNum(); i++)
				{
					String header = formatter.formatCellValue(headerRow.getCell(i)).trim();
					if ("SDLNumber".equalsIgnoreCase(header) || "SDL Number".equalsIgnoreCase(header))
					{
						sdlColIdx = i;
					}
					else if ("Name".equalsIgnoreCase(header))
					{
						nameColIdx = i;
					}
					else if ("Trading Name".equalsIgnoreCase(header) || "TradingName".equalsIgnoreCase(header))
					{
						tradingNameColIdx = i;
					}
				}
			}

			if (sdlColIdx == -1 || nameColIdx == -1 || tradingNameColIdx == -1)
			{
				throw new Exception("Required columns ('SDL Number', 'Name', 'Trading Name') not found in the header row.");
			}

			int totalRows = sheet.getLastRowNum();
			for (int r = 1; r <= totalRows; r++)
			{
				Row row = sheet.getRow(r);
				if (row == null)
					continue;

				String sdlNumber = formatter.formatCellValue(row.getCell(sdlColIdx)).trim();
				String nameStr = row.getCell(nameColIdx) != null ? formatter.formatCellValue(row.getCell(nameColIdx)).trim() : "";
				String tradingNameStr = row.getCell(tradingNameColIdx) != null ? formatter.formatCellValue(row.getCell(tradingNameColIdx)).trim() : "";

				stats[6]++; // totalRowsInFile
				processRow(sdlNumber, nameStr, tradingNameStr, r + 1, currentClientBPs, successLog, errorLog, stats);
			}
		}
	}

	private void processCsv(InputStream is, Map<String, Integer> currentClientBPs, List<String> successLog, List<String> errorLog,
							int[] stats) throws Exception
	{
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
		{
			String line;
			boolean firstLine = true;
			int sdlColIdx = -1;
			int nameColIdx = -1;
			int tradingNameColIdx = -1;
			int r = 0;

			while ((line = br.readLine()) != null)
			{
				r++;
				if (firstLine)
				{
					if (line.startsWith("\uFEFF"))
						line = line.substring(1); // Remove BOM
					List<String> headers = parseCsvLine(line);
					for (int i = 0; i < headers.size(); i++)
					{
						String header = headers.get(i).trim();
						if ("SDLNumber".equalsIgnoreCase(header) || "SDL Number".equalsIgnoreCase(header))
							sdlColIdx = i;
						else if ("Name".equalsIgnoreCase(header))
							nameColIdx = i;
						else if ("Trading Name".equalsIgnoreCase(header) || "TradingName".equalsIgnoreCase(header))
							tradingNameColIdx = i;
					}
					firstLine = false;
					if (sdlColIdx == -1 || nameColIdx == -1 || tradingNameColIdx == -1)
					{
						throw new Exception("Required columns ('SDL Number', 'Name', 'Trading Name') not found in the CSV header.");
					}
					continue;
				}

				if (line.trim().isEmpty())
					continue;

				List<String> cols = parseCsvLine(line);
				String sdlNumber = cols.size() > sdlColIdx ? cols.get(sdlColIdx).trim() : "";
				String nameStr = cols.size() > nameColIdx ? cols.get(nameColIdx).trim() : "";
				String tradingNameStr = cols.size() > tradingNameColIdx ? cols.get(tradingNameColIdx).trim() : "";

				stats[6]++; // totalRowsInFile
				processRow(sdlNumber, nameStr, tradingNameStr, r, currentClientBPs, successLog, errorLog, stats);
			}
		}
	}

	private void processRow(String sdlNumber, String nameStr, String tradingNameStr, int rowNum, Map<String, Integer> currentClientBPs,
							List<String> successLog, List<String> errorLog, int[] stats) throws Exception
	{
		if (sdlNumber.isEmpty())
		{
			errorLog.add("Row " + rowNum + " Skipped | Reason: Missing SDL Number.");
			return;
		}

		Integer bpartnerId = currentClientBPs.get(sdlNumber);
		if (bpartnerId != null)
		{
			MBPartner bp = new MBPartner(getCtx(), bpartnerId, get_TrxName());
			boolean changed = false;
			StringBuilder actionLogNote = new StringBuilder();

			String prevName = bp.getName();
			String prevTradingName = bp.getName2();

			boolean changedName = false;
			boolean changedTradingName = false;

			if (!nameStr.isEmpty() && (prevName == null || !prevName.equals(nameStr)))
			{
				bp.setName(nameStr);
				changed = true;
				changedName = true;
				actionLogNote.append(" | Name: ").append(prevName).append(" -> ").append(nameStr);
			}

			if (!tradingNameStr.isEmpty() && (prevTradingName == null || !prevTradingName.equals(tradingNameStr)))
			{
				bp.setName2(tradingNameStr);
				changed = true;
				changedTradingName = true;
				actionLogNote.append(" | Trading Name (Name2): ").append(prevTradingName).append(" -> ").append(tradingNameStr);
			}

			if (changed)
			{
				if (bp.save())
				{
					stats[0]++; // updatedCount
					if (changedName && changedTradingName)
						stats[3]++;
					else if (changedName)
						stats[1]++;
					else if (changedTradingName)
						stats[2]++;

					successLog.add("SDL: " + sdlNumber + actionLogNote.toString());
				}
				else
				{
					errorLog.add("Row " + rowNum + " | SDL: " + sdlNumber + " | Reason: Failed to save record in DB.");
				}
			}
			else
			{
				errorLog.add("Row " + rowNum + " | SDL: " + sdlNumber + " | Reason: Skipped, values already match DB (or sheet values are empty).");
				stats[4]++; // skippedMatchCount
			}
		}
		else
		{
			errorLog.add("Row " + rowNum + " | SDL: " + sdlNumber + " | Reason: No matching C_BPartner record found.");
			stats[5]++; // notFoundInDbCount
		}

		if (rowNum % 50 == 0)
		{
			commitEx();
		}
	}

	private static List<String> parseCsvLine(String line)
	{
		List<String> result = new ArrayList<>();
		if (line == null)
			return result;

		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			if (c == '\"')
			{
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"')
				{
					sb.append('\"');
					i++;
				}
				else
				{
					inQuotes = !inQuotes;
				}
			}
			else if (c == ',' && !inQuotes)
			{
				result.add(sb.toString());
				sb.setLength(0);
			}
			else
			{
				sb.append(c);
			}
		}
		result.add(sb.toString());
		return result;
	}
}
