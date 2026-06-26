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
		int updatedSubSector = 0;
		int skippedMatchCount = 0;
		List<String> successLog = new ArrayList<>();
		List<String> errorLog = new ArrayList<>();
		List<String> skippedCrossTenantLog = new ArrayList<>();

		Map<String, Integer> subSectorUpdateCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> skippedSubSectorCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		long timestamp = System.currentTimeMillis();

		try (FileInputStream fis = new FileInputStream(file);
						Workbook workbook = WorkbookFactory.create(fis))
		{
			Sheet sheet = workbook.getSheetAt(1);
			Row headerRow = sheet.getRow(0);

			int sdlColIdx = -1;
			int totalEmpColIdx = -1;
			int terminatedEmpColIdx = -1;
			int subSectorColIdx = -1;

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
					else if ("SubSector".equalsIgnoreCase(header) || "Sub Sector".equalsIgnoreCase(header))
					{
						subSectorColIdx = i;
					}
				}
			}

			if (sdlColIdx == -1 || totalEmpColIdx == -1 || terminatedEmpColIdx == -1 || subSectorColIdx == -1)
			{
				throw new Exception("Required columns ('SDLNumber', 'TotalEmployment', 'TerminatedEmployees', 'SubSector') not found in the header row.");
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
				String subSectorStr = formatter.formatCellValue(row.getCell(subSectorColIdx)).trim();

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
					boolean changedSubSector = false;
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

					String prevSubSector = (String) bp.get_Value("ZZSubSector");
					String mappedSubSector = getMappedSubSector(subSectorStr);

					if (!subSectorStr.isEmpty() && mappedSubSector == null)
					{
						errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Reason: Unrecognized SubSector -> " + subSectorStr);
						continue;
					}

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

					if ( (mappedSubSector == null && prevSubSector != null) || 
					     (mappedSubSector != null && !mappedSubSector.equals(prevSubSector)) )
					{
						bp.set_ValueOfColumn("ZZSubSector", mappedSubSector);
						changed = true;
						changedSubSector = true;
						actionLogNote.append(" | SubSector: ")
						             .append(prevSubSector == null ? "null" : prevSubSector + " (" + getSubSectorName(prevSubSector) + ")")
						             .append(" -> ")
						             .append(mappedSubSector == null ? "null" : mappedSubSector)
						             .append(" (").append(subSectorStr.isEmpty() ? "empty" : subSectorStr).append(")");
					}

					if (changed)
					{
						if (bp.save())
						{
							updatedCount++;
							if (changedTotal && changedTerm) updatedBoth++;
							else if (changedTotal) updatedTotalEmpOnly++;
							else if (changedTerm) updatedTermEmpOnly++;
							
							if (changedSubSector) {
								updatedSubSector++;
								subSectorUpdateCounts.put(subSectorStr, subSectorUpdateCounts.getOrDefault(subSectorStr, 0) + 1);
							}
							
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
						if (mappedSubSector != null)
						{
							skippedSubSectorCounts.put(mappedSubSector, skippedSubSectorCounts.getOrDefault(mappedSubSector, 0) + 1);
						}
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

		File txtLogFile = new File("/tmp", "Update_BPartner_Employees_Log_" + timestamp + ".txt");
		try (PrintWriter pw = new PrintWriter(new FileWriter(txtLogFile)))
		{
			pw.println("====== SUMMARY ======");
			pw.println("Total Successful Rows: " + updatedCount);
			pw.println("   -> Updated Total Employment ONLY: " + updatedTotalEmpOnly);
			pw.println("   -> Updated Terminated Employees ONLY: " + updatedTermEmpOnly);
			pw.println("   -> Updated BOTH Employee fields: " + updatedBoth);
			pw.println("   -> Updated SubSector: " + updatedSubSector);
			pw.println("Not successful updates (Errors): " + (errorLog.size() - skippedMatchCount));
			pw.println("Skipped (Already Match DB): " + skippedMatchCount);
			pw.println("Skipped (Cross-Tenant): " + skippedCrossTenantLog.size());
			pw.println("=====================");
			pw.println();

			if (!subSectorUpdateCounts.isEmpty())
			{
				pw.println("====== SUBSECTOR UPDATE DISTRIBUTION ======");
				pw.println(String.format("%-40s | %-10s", "SubSector", "Updated Count"));
				pw.println("----------------------------------------------------------");
				int totalSubSectorUpdates = 0;
				for (Map.Entry<String, Integer> entry : subSectorUpdateCounts.entrySet())
				{
					pw.println(String.format("%-40s | %-10d", entry.getKey(), entry.getValue()));
					totalSubSectorUpdates += entry.getValue();
				}
				pw.println("----------------------------------------------------------");
				pw.println(String.format("%-40s | %-10d", "TOTAL", totalSubSectorUpdates));
				pw.println("===========================================");
				pw.println();
			}
			
			if (!skippedSubSectorCounts.isEmpty())
			{
				pw.println("====== SKIPPED SUBSECTORS (Already Match DB) ======");
				pw.println(String.format("%-40s | %-10s", "SubSector", "Skipped Count"));
				pw.println("----------------------------------------------------------");
				int totalSkippedSubSectors = 0;
				for (Map.Entry<String, Integer> entry : skippedSubSectorCounts.entrySet())
				{
					pw.println(String.format("%-40s | %-10d", getSubSectorName(entry.getKey()) + " (" + entry.getKey() + ")", entry.getValue()));
					totalSkippedSubSectors += entry.getValue();
				}
				pw.println("----------------------------------------------------------");
				pw.println(String.format("%-40s | %-10d", "TOTAL SKIPPED", totalSkippedSubSectors));
				pw.println("===================================================");
				pw.println();
			}
			
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

		File htmlLogFile = new File("/tmp", "Verified_BPartner_Employees_Log_" + timestamp + ".html");
		try (PrintWriter pw = new PrintWriter(new FileWriter(htmlLogFile)))
		{
			pw.println("<html><head><style>body { font-family: sans-serif; } .success { color: green; font-weight: bold; } .error { color: red; font-weight: bold; } li { margin-bottom: 5px; } table { border-collapse: collapse; width: 60%; margin-bottom: 20px; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } th { background-color: #f2f2f2; }</style></head><body>");
			pw.println("<h2>Business Partner Employees Update Report</h2>");

			pw.println("<h3>Summary Statistics</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Category</th><th>Count</th></tr>");
			pw.println("<tr><td>Updated Total Employment <b>ONLY</b></td><td>" + updatedTotalEmpOnly + "</td></tr>");
			pw.println("<tr><td>Updated Terminated Employees <b>ONLY</b></td><td>" + updatedTermEmpOnly + "</td></tr>");
			pw.println("<tr><td>Updated <b>BOTH</b> Employee fields</td><td>" + updatedBoth + "</td></tr>");
			pw.println("<tr><td>Updated <b>SubSector</b></td><td>" + updatedSubSector + "</td></tr>");
			pw.println("<tr style='background-color: #e6ffe6; font-weight: bold;'><td>Total Successful Rows</td><td>" + updatedCount + "</td></tr>");
			pw.println("<tr><td>Not successful updates (Errors)</td><td>" + (errorLog.size() - skippedMatchCount) + "</td></tr>");
			pw.println("<tr><td>Skipped (Already Match DB)</td><td>" + skippedMatchCount + "</td></tr>");
			pw.println("<tr><td>Skipped (Cross-Tenant)</td><td>" + skippedCrossTenantLog.size() + "</td></tr>");
			pw.println("</table><hr/>");

			if (!subSectorUpdateCounts.isEmpty())
			{
				pw.println("<h3>Updated SubSector Distribution</h3>");
				pw.println("<table>");
				pw.println("<tr><th>SubSector</th><th>Updated Count</th></tr>");
				int totalSubSectorUpdates = 0;
				for (Map.Entry<String, Integer> entry : subSectorUpdateCounts.entrySet())
				{
					pw.println("<tr><td>" + entry.getKey() + "</td><td>" + entry.getValue() + "</td></tr>");
					totalSubSectorUpdates += entry.getValue();
				}
				pw.println("<tr><th><b>TOTAL</b></th><th><b>" + totalSubSectorUpdates + "</b></th></tr>");
				pw.println("</table><hr/>");
			}

			if (!skippedSubSectorCounts.isEmpty())
			{
				pw.println("<h3>Skipped SubSectors (Already Match DB)</h3>");
				pw.println("<table>");
				pw.println("<tr><th>SubSector</th><th>Skipped Count</th></tr>");
				int totalSkippedSubSectors = 0;
				for (Map.Entry<String, Integer> entry : skippedSubSectorCounts.entrySet())
				{
					pw.println("<tr><td>" + getSubSectorName(entry.getKey()) + " (" + entry.getKey() + ")</td><td>" + entry.getValue() + "</td></tr>");
					totalSkippedSubSectors += entry.getValue();
				}
				pw.println("<tr><th><b>TOTAL SKIPPED</b></th><th><b>" + totalSkippedSubSectors + "</b></th></tr>");
				pw.println("</table><hr/>");
			}

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

	private String getMappedSubSector(String subSector)
	{
		if (subSector == null || subSector.trim().isEmpty())
			return null;

		String lower = subSector.trim().toLowerCase();
		if (lower.equals("1") || lower.contains("coal")) return "1";
		if (lower.equals("2") || lower.contains("gold")) return "2";
		if (lower.equals("3") || lower.contains("pgm")) return "3";
		if (lower.equals("4") || lower.contains("diamond mining")) return "4";
		if (lower.equals("5") || lower.contains("other")) return "5";
		if (lower.equals("6") || lower.contains("cement") || lower.contains("lime") || lower.contains("aggregates") || lower.contains("clas")) return "6";
		if (lower.equals("7") || lower.contains("services incidental")) return "7";
		if (lower.equals("8") || lower.contains("diamond processing")) return "8";
		if (lower.equals("9") || lower.contains("jewel")) return "9";

		return null;
	}

	private String getSubSectorName(String key)
	{
		if (key == null) return "null";
		switch (key)
		{
			case "1": return "Coal Mining";
			case "2": return "Gold Mining";
			case "3": return "PGM Mining";
			case "4": return "Diamond Mining";
			case "5": return "Other Mining";
			case "6": return "Cement, Lime, Aggregates and Sand (CLAS)";
			case "7": return "Services Incidental to Mining";
			case "8": return "Diamond Processing";
			case "9": return "Jewellery Manufacturing";
			default: return key;
		}
	}
}
