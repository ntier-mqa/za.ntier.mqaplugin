package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MBPartner;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@Process(name = "za.co.ntier.wsp_atr.process.UpdateBPartnerEmployeesFromExcel")
public class UpdateBPartnerEmployeesFromExcel extends SvrProcess
{

	private String p_FilePath = "";

	@Override
	protected void prepare()
	{
		for (ProcessInfoParameter para : getParameter())
		{
			String name = para.getParameterName();
			if (name.equals("FilePath"))
			{
				p_FilePath = para.getParameterAsString();
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		if (p_FilePath == null || p_FilePath.trim().isEmpty())
		{
			throw new Exception("File Path is mandatory");
		}

		File file = new File(p_FilePath);
		if (!file.exists())
		{
			throw new Exception("File not found: " + p_FilePath);
		}

		int updatedCount = 0;
		int updatedTotalEmpOnly = 0;
		int updatedTermEmpOnly = 0;
		int updatedBoth = 0;
		int skippedMatchCount = 0;
		List<String> successLog = new ArrayList<>();
		List<String> errorLog = new ArrayList<>();
		List<String> skippedCrossTenantLog = new ArrayList<>();

		long timestamp = System.currentTimeMillis();

		try (FileInputStream fis = new FileInputStream(file);
						Workbook workbook = WorkbookFactory.create(fis))
		{
			Sheet sheet = workbook.getSheetAt(1);
			Row headerRow = sheet.getRow(0);

			int sdlColIdx = -1;
			int totalEmpColIdx = -1;
			int terminatedEmpColIdx = -1;

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
					else if ("TotalEmployment".equalsIgnoreCase(header) || "Total Employment".equalsIgnoreCase(header))
					{
						totalEmpColIdx = i;
					}
					else if ("TerminatedEmployees".equalsIgnoreCase(header) || "Terminated Employees".equalsIgnoreCase(header))
					{
						terminatedEmpColIdx = i;
					}
				}
			}

			if (sdlColIdx == -1 || totalEmpColIdx == -1 || terminatedEmpColIdx == -1)
			{
				throw new Exception("Required columns ('SDLNumber', 'TotalEmployment', 'TerminatedEmployees') not found in the header row.");
			}

			Map<String, Integer> currentClientBPs = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			Map<String, Integer> crossTenantClientIds = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			
			String loadSql = "SELECT Value, C_BPartner_ID, AD_Client_ID FROM C_BPartner WHERE Value IS NOT NULL";
			try (PreparedStatement pstmt = DB.prepareStatement(loadSql, get_TrxName());
				 ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String val = rs.getString(1).trim();
					int bpId = rs.getInt(2);
					int clientId = rs.getInt(3);
					
					if (clientId == getAD_Client_ID()) {
						currentClientBPs.put(val, bpId);
					} else {
						crossTenantClientIds.put(val, clientId);
					}
				}
			}

			int totalRows = sheet.getLastRowNum();
			for (int r = 1; r <= totalRows; r++)
			{
				statusUpdate("Processing row " + r + " of " + totalRows);
				Row row = sheet.getRow(r);
				if (row == null)
					continue;

				String sdlNumber = formatter.formatCellValue(row.getCell(sdlColIdx)).trim();
				String totalEmpStr = formatter.formatCellValue(row.getCell(totalEmpColIdx)).trim();
				String terminatedEmpStr = formatter.formatCellValue(row.getCell(terminatedEmpColIdx)).trim();

				if (sdlNumber.isEmpty())
				{
					errorLog.add("Row " + (r + 1) + " Skipped | Reason: Missing SDL Number.");
					continue;
				}

				Integer bpartnerId = currentClientBPs.get(sdlNumber);
				if (bpartnerId != null)
				{
					MBPartner bp = new MBPartner(getCtx(), bpartnerId, get_TrxName());
					
					BigDecimal totalEmp = null;
					BigDecimal terminatedEmp = null;
					
					try {
						if (!totalEmpStr.isEmpty())
							totalEmp = new BigDecimal(totalEmpStr.replaceAll("[^0-9\\.]", ""));
					} catch (Exception e) {}
					
					try {
						if (!terminatedEmpStr.isEmpty())
							terminatedEmp = new BigDecimal(terminatedEmpStr.replaceAll("[^0-9\\.]", ""));
					} catch (Exception e) {}

					boolean changed = false;
					boolean changedTotal = false;
					boolean changedTerm = false;
					StringBuilder actionLogNote = new StringBuilder();

					Object rawPrevTotal = bp.get_Value("ZZ_Number_Of_Employees");
					BigDecimal prevTotal = null;
					if (rawPrevTotal instanceof BigDecimal) prevTotal = (BigDecimal) rawPrevTotal;
					else if (rawPrevTotal instanceof Number) prevTotal = new BigDecimal(rawPrevTotal.toString());
					else if (rawPrevTotal instanceof String) try { prevTotal = new BigDecimal((String) rawPrevTotal); } catch (Exception e) {}

					Object rawPrevTerminated = bp.get_Value("ZZTerminatedEmployees");
					BigDecimal prevTerminated = null;
					if (rawPrevTerminated instanceof BigDecimal) prevTerminated = (BigDecimal) rawPrevTerminated;
					else if (rawPrevTerminated instanceof Number) prevTerminated = new BigDecimal(rawPrevTerminated.toString());
					else if (rawPrevTerminated instanceof String) try { prevTerminated = new BigDecimal((String) rawPrevTerminated); } catch (Exception e) {}

					if (totalEmp != null && (prevTotal == null || prevTotal.compareTo(totalEmp) != 0))
					{
						if (!(prevTotal == null && totalEmp.compareTo(BigDecimal.ZERO) == 0))
						{
							bp.set_ValueOfColumn("ZZ_Number_Of_Employees", totalEmp);
							changed = true;
							changedTotal = true;
							actionLogNote.append(" | TotalEmployment: ").append(prevTotal == null ? "null" : prevTotal).append(" -> ").append(totalEmp);
						}
					}

					if (terminatedEmp != null && (prevTerminated == null || prevTerminated.compareTo(terminatedEmp) != 0))
					{
						if (!(prevTerminated == null && terminatedEmp.compareTo(BigDecimal.ZERO) == 0))
						{
							bp.set_ValueOfColumn("ZZTerminatedEmployees", terminatedEmp);
							changed = true;
							changedTerm = true;
							actionLogNote.append(" | TerminatedEmployees: ").append(prevTerminated == null ? "null" : prevTerminated).append(" -> ").append(terminatedEmp);
						}
					}

					if (changed)
					{
						if (bp.save())
						{
							updatedCount++;
							if (changedTotal && changedTerm) updatedBoth++;
							else if (changedTotal) updatedTotalEmpOnly++;
							else if (changedTerm) updatedTermEmpOnly++;
							
							successLog.add(	"SDL: " + sdlNumber + " | C_BPartner_ID: " + bpartnerId + actionLogNote.toString());
						}
						else
						{
							errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Reason: Failed to save record in DB.");
						}
					}
					else
					{
						errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Reason: Skipped, values already match DB.");
						skippedMatchCount++;
					}
				}
				else
				{
					Integer crossClientId = crossTenantClientIds.get(sdlNumber);
					if (crossClientId != null)
					{
						skippedCrossTenantLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Reason: Belongs to System/Other Tenant (Client ID " + crossClientId + "). Run process in that tenant to update.");
					}
					else
					{
						errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Reason: No matching C_BPartner record found.");
					}
				}

				if (r % 50 == 0)
				{
					commitEx();
				}
			}
		}

		File txtLogFile = new File(file.getParentFile(), "Update_BPartner_Employees_Log_" + timestamp + ".txt");
		try (PrintWriter pw = new PrintWriter(new FileWriter(txtLogFile)))
		{
			pw.println("====== SUMMARY ======");
			pw.println("Total Successful Rows: " + updatedCount);
			pw.println("   -> Updated Total Employment ONLY: " + updatedTotalEmpOnly);
			pw.println("   -> Updated Terminated Employees ONLY: " + updatedTermEmpOnly);
			pw.println("   -> Updated BOTH fields: " + updatedBoth);
			pw.println("Not successful updates (Errors): " + (errorLog.size() - skippedMatchCount));
			pw.println("Skipped (Already Match DB): " + skippedMatchCount);
			pw.println("Skipped (Cross-Tenant): " + skippedCrossTenantLog.size());
			pw.println("=====================");
			pw.println();
			
			pw.println("====== FAILED / SKIPPED ROWS (" + errorLog.size() + ") ======");
			for (String errMsg : errorLog)
			{
				pw.println("FAILED: " + errMsg);
			}
			pw.println();
			if (!skippedCrossTenantLog.isEmpty())
			{
				pw.println("====== SKIPPED CROSS-TENANT (" + skippedCrossTenantLog.size() + ") ======");
				for (String skipMsg : skippedCrossTenantLog)
				{
					pw.println("SKIPPED: " + skipMsg);
				}
				pw.println();
			}
			pw.println("====== SUCCESSFUL UPDATES (" + updatedCount + ") ======");
			for (String successMsg : successLog)
			{
				pw.println("SUCCESS: " + successMsg);
			}
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "Could not write to text log file: " + txtLogFile.getAbsolutePath(), ex);
		}

		File htmlLogFile = new File(file.getParentFile(), "Verified_BPartner_Employees_Log_" + timestamp + ".html");
		try (PrintWriter pw = new PrintWriter(new FileWriter(htmlLogFile)))
		{
			pw.println("<html><head><style>body { font-family: sans-serif; } .success { color: green; font-weight: bold; } .error { color: red; font-weight: bold; } li { margin-bottom: 5px; } table { border-collapse: collapse; width: 60%; margin-bottom: 20px; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } th { background-color: #f2f2f2; }</style></head><body>");
			pw.println("<h2>Business Partner Employees Update Report</h2>");

			pw.println("<h3>Summary Statistics</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Category</th><th>Count</th></tr>");
			pw.println("<tr><td>Updated Total Employment <b>ONLY</b></td><td>" + updatedTotalEmpOnly + "</td></tr>");
			pw.println("<tr><td>Updated Terminated Employees <b>ONLY</b></td><td>" + updatedTermEmpOnly + "</td></tr>");
			pw.println("<tr><td>Updated <b>BOTH</b> fields</td><td>" + updatedBoth + "</td></tr>");
			pw.println("<tr style='background-color: #e6ffe6; font-weight: bold;'><td>Total Successful Rows</td><td>" + updatedCount + "</td></tr>");
			pw.println("<tr><td>Not successful updates (Errors)</td><td>" + (errorLog.size() - skippedMatchCount) + "</td></tr>");
			pw.println("<tr><td>Skipped (Already Match DB)</td><td>" + skippedMatchCount + "</td></tr>");
			pw.println("<tr><td>Skipped (Cross-Tenant)</td><td>" + skippedCrossTenantLog.size() + "</td></tr>");
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

			if (!skippedCrossTenantLog.isEmpty()) {
				pw.println("<h3 style='color: #ff9800;'>⚠️ SKIPPED CROSS-TENANT (" + skippedCrossTenantLog.size() + ")</h3><ul>");
				for (String skipMsg : skippedCrossTenantLog)
				{
					String htmlMsg = skipMsg.replace("Row ", "<span style='color: black;'>Row </span>")
											.replace("SDL:", "<span style='color: black;'>SDL:</span>")
											.replace("Reason:", "<span style='color: black;'>Reason:</span>");
					pw.println("<li style='color: #ff9800;'>⚠️ " + htmlMsg + "</li>");
				}
				pw.println("</ul><br/>");
			}

			pw.println("<h3 class='success'>✅ SUCCESSFUL UPDATES (" + updatedCount + ")</h3><ul>");
			for (String successMsg : successLog)
			{
				String htmlMsg = successMsg	.replace("SDL:", "<span style='color: black;'>SDL:</span>")
											.replace("C_BPartner_ID:", "<span style='color: black;'>C_BPartner_ID:</span>")
											.replace("TotalEmployment:", "<span style='color: black;'>TotalEmployment:</span>")
											.replace("TerminatedEmployees:", "<span style='color: black;'>TerminatedEmployees:</span>");
				pw.println("<li class='success'>✅ " + htmlMsg + "</li>");
			}
			pw.println("</ul>");
			pw.println("</body></html>");
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, "Could not write to HTML log file: " + htmlLogFile.getAbsolutePath(), ex);
			return "Process completed with " + updatedCount + " updates, but failed to write log files: " + ex.getMessage();
		}

		return "Process completed. Updated " + updatedCount + " records. Check logs at: " + txtLogFile.getAbsolutePath() + " and " + htmlLogFile.getAbsolutePath();
	}
}
