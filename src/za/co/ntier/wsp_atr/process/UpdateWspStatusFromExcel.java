package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
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
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

@Process(name = "za.co.ntier.wsp_atr.process.UpdateWspStatusFromExcel")
public class UpdateWspStatusFromExcel extends SvrProcess
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
		int updatedStatusOnly = 0;
		int updatedActionOnly = 0;
		int updatedBoth = 0;
		int skippedMatchCount = 0;

		List<String> successLog = new ArrayList<>();
		List<String> errorLog = new ArrayList<>();
		List<String> verificationErrorLog = new ArrayList<>();
		List<ExpectedUpdate> pendingVerifications = new ArrayList<>();

		Map<String, Integer> srcStatusCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> tgtStatusCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> srcActionCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> tgtActionCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Map<String, Integer> skippedStatusCounts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		try (FileInputStream fis = new FileInputStream(file);
						Workbook workbook = WorkbookFactory.create(fis))
		{

			Sheet sheet = workbook.getSheetAt(1);
			Row headerRow = sheet.getRow(0);

			int sdlColIdx = -1;
			int orgLegalNameColIdx = -1;
			int planGrantStatusColIdx = -1;

			DataFormatter formatter = new DataFormatter();

			if (headerRow != null)
			{
				for (int i = 0; i < headerRow.getLastCellNum(); i++)
				{
					String header = formatter.formatCellValue(headerRow.getCell(i)).trim();
					if ("SDLNumber".equalsIgnoreCase(header))
					{
						sdlColIdx = i;
					}
					else if ("OrganisationLegalName".equalsIgnoreCase(header))
					{
						orgLegalNameColIdx = i;
					}
					else if ("PlanningGrantStatus".equalsIgnoreCase(header))
					{
						planGrantStatusColIdx = i;
					}
				}
			}

			if (sdlColIdx == -1 || orgLegalNameColIdx == -1 || planGrantStatusColIdx == -1)
			{
				throw new Exception("Required columns ('SDL Number', 'OrganisationLegalName', 'PlanningGrantStatus') not found in the header row.");
			}

			String sql = "SELECT wsp.ZZ_WSP_ATR_Submitted_ID "
							+ "FROM ZZ_WSP_ATR_Submitted wsp "
							+ "INNER JOIN ZZSdfOrganisation_v sdf_org ON wsp.ZZSdfOrganisation_ID = sdf_org.ZZSdfOrganisation_ID "
							+ "INNER JOIN C_BPartner bp ON sdf_org.C_BPartner_ID = bp.C_BPartner_ID "
							+ "WHERE bp.Value = ? "
							+ "ORDER BY wsp.DocumentNo DESC";

			int totalRows = sheet.getLastRowNum();
			for (int r = 1; r <= totalRows; r++)
			{
				statusUpdate("Processing row " + r + " of " + totalRows);
				Row row = sheet.getRow(r);
				if (row == null)
					continue;

				String sdlNumber = formatter.formatCellValue(row.getCell(sdlColIdx)).trim();
				String orgName = formatter.formatCellValue(row.getCell(orgLegalNameColIdx)).trim();
				String planningGrantStatus = formatter.formatCellValue(row.getCell(planGrantStatusColIdx)).trim();

				if (sdlNumber.isEmpty() || planningGrantStatus.isEmpty())
				{
					errorLog.add("Row " + (r + 1) + " Skipped | Reason: Missing SDL or PlanningGrantStatus.");
					continue;
				}

				int submittedId = DB.getSQLValue(get_TrxName(), sql, sdlNumber);
				if (submittedId > 0)
				{
					X_ZZ_WSP_ATR_Submitted submitted = new X_ZZ_WSP_ATR_Submitted(getCtx(), submittedId, get_TrxName());
					boolean changed = false;
					String actionLogNote = "";
					String actionName = "";

					String currentDocStatus = submitted.getZZ_DocStatus();
					String currentDocAction = submitted.getZZ_DocAction();

					String mappedDocStatus = getMappedDocStatus(planningGrantStatus);
					String mappedDocAction = null;
					boolean statusChanged = false;
					boolean actionChanged = false;

					if (mappedDocStatus != null)
					{
						mappedDocAction = getMappedDocAction(mappedDocStatus);

						statusChanged = currentDocStatus == null || !currentDocStatus.equals(mappedDocStatus);

						actionChanged = mappedDocAction != null && (currentDocAction == null || !currentDocAction.equals(mappedDocAction));

						if (mappedDocAction != null)
						{
							switch (mappedDocAction)
							{
								case X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Verify:
									actionName = "Verify";
									break;
								case X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Recommend:
									actionName = "Recommend";
									break;
								case X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_ApproveDoNotApprove:
									actionName = "Approved";
									break;
								case X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Re_Submit:
									actionName = "Re-Submit";
									break;
							}
						}

						if (statusChanged || actionChanged)
						{
							if (statusChanged)
							{
								submitted.setZZ_DocStatus(mappedDocStatus);
								changed = true;
							}

							if (mappedDocAction != null)
							{
								if (actionChanged)
								{
									submitted.setZZ_DocAction(mappedDocAction);
									actionLogNote = " | Action Updated to: " + currentDocAction + " -> " + mappedDocAction + " (" + actionName + ")";
									changed = true;
								}
							}
							else
							{
								errorLog.add(	"Row "	+ (r + 1) + " | SDL: " + sdlNumber + " | Org: " + orgName
												+ " | Note: DocStatus updated, but no DocAction mapping for -> " + planningGrantStatus);
							}
						}
						else
						{
							String note = "";
							if (mappedDocAction == null)
							{
								note = " (Note: No DocAction mapping for -> " + planningGrantStatus + ")";
							}
							errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Org: " + orgName + " | Reason: Skipped, values already match DB." + note);
							
							skippedMatchCount++;
							skippedStatusCounts.put(planningGrantStatus, skippedStatusCounts.getOrDefault(planningGrantStatus, 0) + 1);
						}
					}
					else
					{
						errorLog.add(	"Row "	+ (r + 1) + " | SDL: " + sdlNumber + " | Org: " + orgName + " | Reason: Unrecognized PlanningGrantStatus -> "
										+ planningGrantStatus);
					}

					if (changed)
					{
						if (statusChanged)
						{
							srcStatusCounts.put(planningGrantStatus, srcStatusCounts.getOrDefault(planningGrantStatus, 0) + 1);
						}
						if (actionChanged && actionName != null && !actionName.isEmpty())
						{
							srcActionCounts.put(actionName, srcActionCounts.getOrDefault(actionName, 0) + 1);
						}

						if (submitted.save())
						{
							pendingVerifications.add(new ExpectedUpdate(r + 1, sdlNumber, orgName, submitted.get_ID(), mappedDocStatus, mappedDocAction,
																		submitted.getDocumentNo(), planningGrantStatus, actionLogNote, actionName, currentDocStatus, statusChanged, actionChanged));
						}
						else
						{
							errorLog.add("Row " + (r + 1) + " | SDL: " + sdlNumber + " | Org: " + orgName + " | Reason: Failed to save record in DB.");
						}
					}
				}
				else
				{
					errorLog.add(	"Row "	+ (r + 1) + " | SDL: " + sdlNumber + " | Org: " + orgName
									+ " | Reason: No matching ZZ_WSP_ATR_Submitted record found.");
				}

				if (r % 50 == 0)
				{
					commitEx();
				}
			}
		}

		for (ExpectedUpdate eu : pendingVerifications)
		{
			X_ZZ_WSP_ATR_Submitted verificationRecord = new X_ZZ_WSP_ATR_Submitted(getCtx(), eu.submittedId, get_TrxName());
			String finalStatus = verificationRecord.getZZ_DocStatus();
			String finalAction = verificationRecord.getZZ_DocAction();

			if (eu.statusChanged)
			{
				if (eu.expectedStatus != null && eu.expectedStatus.equals(finalStatus))
					tgtStatusCounts.put(eu.planningGrantStatus, tgtStatusCounts.getOrDefault(eu.planningGrantStatus, 0) + 1);
				else if (finalStatus != null)
					tgtStatusCounts.put(finalStatus, tgtStatusCounts.getOrDefault(finalStatus, 0) + 1);
			}

			if (eu.actionChanged)
			{
				if (eu.expectedAction != null && eu.expectedAction.equals(finalAction))
					tgtActionCounts.put(eu.actionName, tgtActionCounts.getOrDefault(eu.actionName, 0) + 1);
				else if (finalAction != null)
					tgtActionCounts.put(finalAction, tgtActionCounts.getOrDefault(finalAction, 0) + 1);
			}

			boolean statusOk = !eu.statusChanged || (eu.expectedStatus != null && eu.expectedStatus.equals(finalStatus));
			boolean actionOk = !eu.actionChanged || (eu.expectedAction != null && eu.expectedAction.equals(finalAction));

			if (statusOk && actionOk)
			{
				updatedCount++;
				if (eu.statusChanged && eu.actionChanged) updatedBoth++;
				else if (eu.statusChanged) updatedStatusOnly++;
				else if (eu.actionChanged) updatedActionOnly++;

				String statusUpdateStr = "";
				if (eu.statusChanged) {
					statusUpdateStr = (eu.prevDocStatus != null ? eu.prevDocStatus : "null") + " -> " + eu.expectedStatus;
				} else {
					statusUpdateStr = eu.expectedStatus + " -> " + eu.expectedStatus;
				}

				successLog.add(	"DocumentNo: "	+ eu.docNo + " | ZZ_WSP_ATR_Submitted_ID: " + eu.submittedId
								+ " | Status Updated to: " + statusUpdateStr + " (" + eu.planningGrantStatus + ")" + eu.actionLogNote);
			}
			else
			{
				verificationErrorLog.add(	"Row "	+ eu.row + " | SDL: " + eu.sdlNumber + " | Org: " + eu.orgName
											+ " | Reason: Verification failed! Expected Status: " + eu.expectedStatus + ", Final: " + finalStatus
											+ " | Expected Action: " + eu.expectedAction + ", Final: " + finalAction);
			}
		}

		verificationErrorLog.addAll(errorLog);
		errorLog = verificationErrorLog;

		long timestamp = System.currentTimeMillis();

		java.util.Set<String> allStatuses = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		allStatuses.addAll(srcStatusCounts.keySet());
		allStatuses.addAll(tgtStatusCounts.keySet());

		int totalSrcStatus = 0;
		int totalTgtStatus = 0;
		for (String status : allStatuses)
		{
			totalSrcStatus += srcStatusCounts.getOrDefault(status, 0);
			totalTgtStatus += tgtStatusCounts.getOrDefault(status, 0);
		}

		java.util.Set<String> allActions = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		allActions.addAll(srcActionCounts.keySet());
		allActions.addAll(tgtActionCounts.keySet());

		int totalSrcAction = 0;
		int totalTgtAction = 0;
		for (String action : allActions)
		{
			totalSrcAction += srcActionCounts.getOrDefault(action, 0);
			totalTgtAction += tgtActionCounts.getOrDefault(action, 0);
		}

		File txtLogFile = new File("/tmp", "Update_WSP_Status_Log_" + timestamp + ".txt");
		try (PrintWriter pw = new PrintWriter(new FileWriter(txtLogFile)))
		{
			pw.println("====== SUMMARY ======");
			pw.println("Total Successful Rows: " + updatedCount);
			pw.println("   -> Updated DocStatus ONLY: " + updatedStatusOnly);
			pw.println("   -> Updated DocAction ONLY: " + updatedActionOnly);
			pw.println("   -> Updated BOTH fields: " + updatedBoth);
			pw.println("Not successful updates / skipped (Errors): " + (errorLog.size() - skippedMatchCount));
			pw.println("Skipped (Already Match DB): " + skippedMatchCount);
			pw.println("----------------------------------------------------------");
			pw.println(String.format("%-40s | %-10s | %-10s", "Status", "Source", "Target"));
			pw.println("----------------------------------------------------------");
			for (String status : allStatuses)
			{
				pw.println(String.format("%-40s | %-10d | %-10d", status, srcStatusCounts.getOrDefault(status, 0), tgtStatusCounts.getOrDefault(status, 0)));
			}
			pw.println("----------------------------------------------------------");
			pw.println(String.format("%-40s | %-10d | %-10d", "TOTAL", totalSrcStatus, totalTgtStatus));
			pw.println("----------------------------------------------------------");
			pw.println(String.format("%-40s | %-10s | %-10s", "Action", "Source", "Target"));
			pw.println("----------------------------------------------------------");
			for (String action : allActions)
			{
				pw.println(String.format("%-40s | %-10d | %-10d", action, srcActionCounts.getOrDefault(action, 0), tgtActionCounts.getOrDefault(action, 0)));
			}
			pw.println("----------------------------------------------------------");
			pw.println(String.format("%-40s | %-10d | %-10d", "TOTAL", totalSrcAction, totalTgtAction));
			pw.println("=====================");
			pw.println();

			if (!skippedStatusCounts.isEmpty())
			{
				pw.println("====== SKIPPED STATUSES (Already Match DB) ======");
				pw.println(String.format("%-40s | %-10s", "Status", "Count"));
				pw.println("----------------------------------------------------------");
				int totalSkipped = 0;
				for (Map.Entry<String, Integer> entry : skippedStatusCounts.entrySet())
				{
					pw.println(String.format("%-40s | %-10d", entry.getKey(), entry.getValue()));
					totalSkipped += entry.getValue();
				}
				pw.println("----------------------------------------------------------");
				pw.println(String.format("%-40s | %-10d", "TOTAL SKIPPED", totalSkipped));
				pw.println("=====================");
				pw.println();
			}

			pw.println("====== FAILED / SKIPPED ROWS (" + errorLog.size() + ") ======");
			for (String errMsg : errorLog)
			{
				pw.println("FAILED: " + errMsg);
			}
			pw.println();
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

		File htmlLogFile = new File("/tmp", "Verified_WSP_Status_Log_" + timestamp + ".html");
		try (PrintWriter pw = new PrintWriter(new FileWriter(htmlLogFile)))
		{
			pw.println("<html><head><style>body { font-family: sans-serif; } .success { color: green; font-weight: bold; } .error { color: red; font-weight: bold; } li { margin-bottom: 5px; } table { border-collapse: collapse; width: 60%; margin-bottom: 20px; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } th { background-color: #f2f2f2; }</style></head><body>");
			pw.println("<h2>WSP Status Update Report</h2>");

			pw.println("<h3>Summary Statistics</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Category</th><th>Count</th></tr>");
			pw.println("<tr><td>Updated DocStatus <b>ONLY</b></td><td>" + updatedStatusOnly + "</td></tr>");
			pw.println("<tr><td>Updated DocAction <b>ONLY</b></td><td>" + updatedActionOnly + "</td></tr>");
			pw.println("<tr><td>Updated <b>BOTH</b> fields</td><td>" + updatedBoth + "</td></tr>");
			pw.println("<tr style='background-color: #e6ffe6; font-weight: bold;'><td>Total Successful Rows</td><td>" + updatedCount + "</td></tr>");
			pw.println("<tr><td>Not successful updates / skipped (Errors)</td><td>" + (errorLog.size() - skippedMatchCount) + "</td></tr>");
			pw.println("<tr><td>Skipped (Already Match DB)</td><td>" + skippedMatchCount + "</td></tr>");
			pw.println("</table><br/>");

			pw.println("<h3>Updated Status Distributions (Source vs Target)</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Status</th><th>Source (Excel)</th><th>Target (Database)</th></tr>");
			for (String status : allStatuses)
			{
				pw.println("<tr><td>" + status + "</td><td>" + srcStatusCounts.getOrDefault(status, 0) + "</td><td>" + tgtStatusCounts.getOrDefault(status, 0) + "</td></tr>");
			}
			pw.println("<tr><th><b>TOTAL</b></th><th><b>" + totalSrcStatus + "</b></th><th><b>" + totalTgtStatus + "</b></th></tr>");
			pw.println("</table><br/>");

			pw.println("<h3>Updated Action Distributions (Source vs Target)</h3>");
			pw.println("<table>");
			pw.println("<tr><th>Action</th><th>Source (Excel)</th><th>Target (Database)</th></tr>");
			for (String action : allActions)
			{
				pw.println("<tr><td>" + action + "</td><td>" + srcActionCounts.getOrDefault(action, 0) + "</td><td>" + tgtActionCounts.getOrDefault(action, 0) + "</td></tr>");
			}
			pw.println("<tr><th><b>TOTAL</b></th><th><b>" + totalSrcAction + "</b></th><th><b>" + totalTgtAction + "</b></th></tr>");
			pw.println("</table><hr/>");

			if (!skippedStatusCounts.isEmpty())
			{
				pw.println("<h3>Skipped Statuses (Already Match DB)</h3>");
				pw.println("<table>");
				pw.println("<tr><th>Status</th><th>Count</th></tr>");
				int totalSkipped = 0;
				for (Map.Entry<String, Integer> entry : skippedStatusCounts.entrySet())
				{
					pw.println("<tr><td>" + entry.getKey() + "</td><td>" + entry.getValue() + "</td></tr>");
					totalSkipped += entry.getValue();
				}
				pw.println("<tr><th><b>TOTAL SKIPPED</b></th><th><b>" + totalSkipped + "</b></th></tr>");
				pw.println("</table><hr/>");
			}

			pw.println("<h3 class='error'>❌ FAILED / SKIPPED ROWS (" + errorLog.size() + ")</h3><ul>");
			for (String errMsg : errorLog)
			{
				String htmlMsg = errMsg	.replace("Row ", "<span style='color: black;'>Row </span>")
										.replace("SDL:", "<span style='color: black;'>SDL:</span>")
										.replace("Org:", "<span style='color: black;'>Org:</span>")
										.replace("Reason:", "<span style='color: black;'>Reason:</span>")
										.replace("Note:", "<span style='color: black;'>Note:</span>");
				pw.println("<li class='error'>❌ " + htmlMsg + "</li>");
			}
			pw.println("</ul><br/>");

			pw.println("<h3 class='success'>✅ SUCCESSFUL UPDATES (" + updatedCount + ")</h3><ul>");
			for (String successMsg : successLog)
			{
				String htmlMsg = successMsg	.replace("DocumentNo:", "<span style='color: black;'>DocumentNo:</span>")
											.replace("ZZ_WSP_ATR_Submitted_ID:", "<span style='color: black;'>ZZ_WSP_ATR_Submitted_ID:</span>")
											.replace("Status Updated to:", "<span style='color: black;'>Status Updated to:</span>")
											.replace("Action Updated to:", "<span style='color: black;'>Action Updated to:</span>");
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

		return "Process completed. Updated " + updatedCount + " records. Check logs at: " + txtLogFile.getAbsolutePath() + " and " + htmlLogFile
																																				.getAbsolutePath();
	}

	private String getMappedDocStatus(String status)
	{
		if (status == null)
			return null;

		switch (status.trim().toLowerCase())
		{
			case "approved by manager finance consumables":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ApprovedByManagerFinanceConsumables;
			case "approved":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Approved;
			case "prepared for ceo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_PreparedForCEO;
			case "completed":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Completed;
			case "draft":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft;
			case "error importing":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting;
			case "validation error":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ValidationError;
			case "evaluated":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Evaluated;
			case "importing":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing;
			case "imported":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported;
			case "in progress":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_InProgress;
			case "not recommended by senior mgr sdr":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommendedBySeniorMgrSDR;
			case "not recommended by senior mgr finance":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommendedBySeniorMgrFinance;
			case "not recommended by coo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommendedByCOO;
			case "not recommended by cfo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommendedByCFO;
			case "not recommended by ceo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommendedByCEO;
			case "not approved by snr manager":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedBySnrManager;
			case "not approved by manager finance consumables":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedByManagerFinanceConsumables;
			case "not approved by sdl finance mgr":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedBySDLFinanceMgr;
			case "not approved by it manager":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedByITManager;
			case "not approved by lm":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedByLM;
			case "not recommended":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotRecommended;
			case "not approved by snr admin finance":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_NotApprovedBySnrAdminFinance;
			case "pending":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Pending;
			case "query":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Query;
			case "recommended by senior mgr finance":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedBySeniorMgrFinance;
			case "recommended by coo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedByCOO;
			case "recommended by cfo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedByCFO;
			case "recommended by ceo":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedByCEO;
			case "recommended for approval":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForApproval;
			case "recommended":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Recommended;
			case "recommended by senior mgr sdr":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedBySeniorMgrSDR;
			case "recommended for evaluation":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForEvaluation;
			case "submitted to manager finance consumables":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_SubmittedToManagerFinanceConsumables;
			case "submitted to sdl finance mgr":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_SubmittedToSDLFinanceMgr;
			case "submitted to it manager":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_SubmittedToITManager;
			case "submitted to it admin":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_SubmittedToITAdmin;
			case "submitted":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Submitted;
			case "transfer out":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_TransferOut;
			case "updated by sdr admin":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_UpdatedBySDRAdmin;
			case "uploaded":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Uploaded;
			case "delinked":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Delinked;
			case "validating":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating;
			case "verified":
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Verified;
			default:
				return null;
		}
	}

	private String getMappedDocAction(String docStatus)
	{
		if (docStatus == null)
			return null;

		switch (docStatus)
		{
			case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Submitted:
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Verify;
			case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForEvaluation:
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Recommend;
			case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForApproval:
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_ApproveDoNotApprove;
			case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Query:
				return X_ZZ_WSP_ATR_Submitted.ZZ_DOCACTION_Re_Submit;
			default:
				return null;
		}
	}

	private class ExpectedUpdate
	{
		int		row;
		String	sdlNumber;
		String	orgName;
		int		submittedId;
		String	expectedStatus;
		String	expectedAction;
		String	docNo;
		String	planningGrantStatus;
		String	actionLogNote;
		String	actionName;
		String	prevDocStatus;
		boolean statusChanged;
		boolean actionChanged;

		public ExpectedUpdate(	int row, String sdlNumber, String orgName, int submittedId, String expectedStatus, String expectedAction, String docNo,
								String planningGrantStatus, String actionLogNote, String actionName, String prevDocStatus, boolean statusChanged, boolean actionChanged)
		{
			this.row = row;
			this.sdlNumber = sdlNumber;
			this.orgName = orgName;
			this.submittedId = submittedId;
			this.expectedStatus = expectedStatus;
			this.expectedAction = expectedAction;
			this.docNo = docNo;
			this.planningGrantStatus = planningGrantStatus;
			this.actionLogNote = actionLogNote;
			this.actionName = actionName;
			this.prevDocStatus = prevDocStatus;
			this.statusChanged = statusChanged;
			this.actionChanged = actionChanged;
		}
	}
}
