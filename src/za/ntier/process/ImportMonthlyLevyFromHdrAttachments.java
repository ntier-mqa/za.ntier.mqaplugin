package za.ntier.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.MUserRoles;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Util;

import za.ntier.models.X_ZZ_Monthly_Levy_Files;
// If you generated the header X-model, import it here:
import za.ntier.models.X_ZZ_Monthly_Levy_Files_Hdr;
import za.ntier.utils.Notifications;

@org.adempiere.base.annotation.Process(
		name = "za.ntier.process.ImportMonthlyLevyFromHdrAttachments"
		)
public class ImportMonthlyLevyFromHdrAttachments extends SvrProcess {

	private int p_Record_ID; // Header record
	private X_ZZ_Monthly_Levy_Files_Hdr hdr;
	private String fiscalYear;

	@Override
	protected void prepare() {
		p_Record_ID = getRecord_ID();
		if (p_Record_ID <= 0)
			throw new AdempiereException("No header record context (Record_ID) found.");
	}

	@Override
	protected String doIt() throws Exception {
		// Load header
		hdr = new X_ZZ_Monthly_Levy_Files_Hdr(getCtx(), p_Record_ID, get_TrxName());
		if (hdr.get_ID() <= 0)
			throw new AdempiereException("Header not found: ID=" + p_Record_ID);

		// Validate Year+Month on header
		int C_Year_ID = hdr.getC_Year_ID();
		String month2 = hdr.getZZ_Month(); // expect "01".."12"
		if (C_Year_ID <= 0) throw new AdempiereException("Year (C_Year_ID) is required on header.");
		if (month2 == null || month2.length() != 2) throw new AdempiereException("Month (ZZ_Month) must be 2 chars (01..12).");

		fiscalYear = DB.getSQLValueStringEx(get_TrxName(),
				"SELECT FiscalYear FROM C_Year WHERE C_Year_ID=?", C_Year_ID);
		if (fiscalYear == null || !fiscalYear.matches("\\d{4}"))
			throw new AdempiereException("Could not resolve 4-digit FiscalYear from C_Year_ID=" + C_Year_ID);

		String yyyymm = fiscalYear + month2;
		Pattern filePattern = Pattern.compile(".*Fin_" + Pattern.quote(yyyymm) + ".*\\.csv$", Pattern.CASE_INSENSITIVE);

		// Read attachments from header
		MAttachment att = MAttachment.get(getCtx(), hdr.get_Table_ID(), hdr.get_ID());
		if (att == null || att.getEntryCount() == 0)
			throw new AdempiereException("No attachments found on header. Please attach .csv files and try again.");

		// Optional: clear existing rows for this header (or for year+month scope)
		boolean clearExisting = hdr.isZZ_Is_Clear_Existing();
		int deleted = 0;
		if (clearExisting) {			
			deleted = DB.executeUpdateEx(
					"DELETE FROM ZZ_Monthly_Levy_Files WHERE ZZ_Monthly_Levy_Files_Hdr_ID=?",
					new Object[]{hdr.getZZ_Monthly_Levy_Files_Hdr_ID()}, get_TrxName());
		}

		int filesProcessed = 0;
		int totalInserted = 0;
		StringBuilder skippedFiles = new StringBuilder();

		for (MAttachmentEntry e : att.getEntries()) {
			String rawName  = e.getName() != null ? e.getName() : "";
			String fname = normalizeFileName(rawName);
			if (!fname.toLowerCase().endsWith(".csv")) continue;
			if (!filePattern.matcher(fname).matches()) {
				if (skippedFiles.length() > 0) skippedFiles.append(", ");
				skippedFiles.append(fname);
				continue;
			}
			int inserted = importCsvEntry(e, C_Year_ID, month2,fname);
			addLog("Imported " + inserted + " rows from " + fname);
			totalInserted += inserted;
			filesProcessed++;
		}

		// Update header tracking fields (if present)
		//safeSet(hdr, "Processed", "Y");
		hdr.setZZ_Lines_Imported(totalInserted);
		hdr.setZZ_Last_Import_Note(
				"Files: " + filesProcessed +
				(clearExisting ? (" | Deleted existing: " + deleted) : "") +
				(skippedFiles.length() > 0 ? (" | Skipped(non-matching): " + skippedFiles) : ""));
		hdr.saveEx();

		sendSdrManagerNotification(fiscalYear, monthName(month2));

		String headerLabel = fiscalYear + " " + monthName(month2);

		return String.format(
				"%s: Processed %d file(s), Inserted %d row(s).%s%s",
				headerLabel,
				filesProcessed,
				totalInserted,
				clearExisting ? (" Deleted existing: " + deleted + ".") : "",
						skippedFiles.length() > 0 ? (" Skipped(non-matching): " + skippedFiles + ".") : ""
				);
	}

	private static final Pattern TRAILING_YEAR = Pattern.compile("-(\\d{4})(?:\\.[^.]+)?$");

	private String extractYearFromFileName(String fileName) {
		if (fileName == null) return null;
		Matcher m = TRAILING_YEAR.matcher(fileName);
		return m.find() ? m.group(1) : null;
	}

	private int importCsvEntry(MAttachmentEntry entry, int C_Year_ID, String month2, String fileName) {
		int inserted = 0;
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(entry.getData()), StandardCharsets.UTF_8))) {

			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				List<String> cols = parseCsvLine(line);
				if (cols.size() != 13) { addLog("WARN: Skipping row with " + cols.size() + " cols in " + fileName); continue; }
				X_ZZ_Monthly_Levy_Files rec = new X_ZZ_Monthly_Levy_Files(getCtx(), 0, get_TrxName());
				rec.setAD_Org_ID(0);
				rec.setC_Year_ID(C_Year_ID);
				rec.setZZ_Month(month2);
				rec.setZZ_Seta_Code(cols.get(1).trim());
				String sdlNo = cols.get(2).trim();
				rec.setZZ_SDL_No(sdlNo);				
				rec.setZZ_MG(toBD(cols.get(4)));
				rec.setZZ_DG(toBD(cols.get(5)));
				rec.setZZ_Admin(toBD(cols.get(6)));
				rec.setZZ_Penalties(toBD(cols.get(7)));
				rec.setzz_Interest(toBD(cols.get(8)));
				rec.setzz_Total(toBD(cols.get(9)));

				String schemeAdj = cols.get(12).trim();
				if (!schemeAdj.isEmpty()) rec.setZZ_Scheme_Year_Adjust(schemeAdj);
				rec.setZZ_Current_Date(new Timestamp(System.currentTimeMillis()));

				// link to header
				if (hasColumn(rec, "ZZ_Monthly_Levy_Files_Hdr_ID")) {
					rec.setZZ_Monthly_Levy_Files_Hdr_ID(hdr.get_ID());
				}


				String fn = fileName;
				if (fn.length() > 255) fn = fn.substring(0, 255);
				// use generated setter if you have it:
				rec.setZZ_File_Name(fn);
				String fileYear = extractYearFromFileName(fileName);
				if (fileYear != null) {
					rec.setZZ_Year(fileYear);				
					if (!sdlNo.isEmpty()) {
						int bpId = DB.getSQLValueEx(get_TrxName(),
								"SELECT C_BPartner_ID FROM C_BPartner WHERE ZZ_SDL_No=? AND IsActive='Y' FETCH FIRST 1 ROWS ONLY", sdlNo);
						if (bpId > 0) {
							int approvalId = DB.getSQLValueEx(get_TrxName(),
									"SELECT ZZ_WSP_ATR_Approvals_ID FROM ZZ_WSP_ATR_Approvals WHERE C_BPartner_ID=? AND ZZ_Financial_Year=? AND IsActive='Y' FETCH FIRST 1 ROWS ONLY",
									bpId, fileYear);
							if (approvalId > 0) {
								String grantStatus = DB.getSQLValueStringEx(get_TrxName(),
										"SELECT ZZ_Grant_Status FROM ZZ_WSP_ATR_Approvals WHERE ZZ_WSP_ATR_Approvals_ID=?", approvalId);
								if (grantStatus != null && hasColumn(rec, "ZZ_Grant_Status")) {
									rec.setZZ_Grant_Status(grantStatus);
								}
								
							}
						}
					}
				} else {
					addLog("WARN: No 4-digit trailing year found in filename: " + fileName);
				}


				rec.saveEx();
				inserted++;
			}
		} catch (Exception ex) {
			throw new AdempiereException("Failed on attachment: " + fileName + " -> " + ex.getMessage(), ex);
		}
		return inserted;
	}


	// Helpers

	private boolean hasColumn(org.compiere.model.PO po, String column) {
		return po.get_ColumnIndex(column) >= 0;
	}
	

	private BigDecimal toBD(String raw) {
		if (raw == null) return BigDecimal.ZERO;
		String s = raw.trim();
		if (s.isEmpty() || s.equalsIgnoreCase("null") || s.equals("-")) return BigDecimal.ZERO;

		boolean neg = false;
		if (s.startsWith("(") && s.endsWith(")")) { neg = true; s = s.substring(1, s.length()-1); }
		s = s.replace(" ", "").replace(",", ""); // assumes '.' decimal
		try {
			BigDecimal bd = new BigDecimal(s);
			return neg ? bd.negate() : bd;
		} catch (NumberFormatException e) {
			addLog("WARN: Bad number '" + raw + "', using 0");
			return BigDecimal.ZERO;
		}
	}

	// CSV parser: supports quotes and embedded commas, no headers
	private List<String> parseCsvLine(String line) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '\"') {
				if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '\"') { cur.append('\"'); i++; }
				else inQuotes = !inQuotes;
			} else if (c == ',' && !inQuotes) {
				out.add(cur.toString()); cur.setLength(0);
			} else {
				cur.append(c);
			}
		}
		out.add(cur.toString());
		return out;
	}


	private static String monthName(String mm) {
		switch (mm) {
		case "01": return "January";
		case "02": return "February";
		case "03": return "March";
		case "04": return "April";
		case "05": return "May";
		case "06": return "June";
		case "07": return "July";
		case "08": return "August";
		case "09": return "September";
		case "10": return "October";
		case "11": return "November";
		case "12": return "December";
		default:   return mm; // fallback
		}
	}


	// UUID of the R_MailText template – create the template in iDempiere and paste its UUID here.
	// Template tokens: @Name@ (recipient name), @FiscalYear@ (4-digit year), @ZZ_Month@ (month name)
	private static final String LEVY_IMPORT_MAIL_TEMPLATE_UU = "96989870-cf2d-4bc2-b339-b88baec4faf4";

	private void sendSdrManagerNotification(String year, String mthName) {
		if (Util.isEmpty(LEVY_IMPORT_MAIL_TEMPLATE_UU)) {
			addLog("WARN: Mail template UUID not configured (LEVY_IMPORT_MAIL_TEMPLATE_UU) – email notification skipped.");
			return;
		}

		MMailText mailText = new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
				.setParameters(LEVY_IMPORT_MAIL_TEMPLATE_UU).firstOnly();
		if (mailText == null) {
			addLog("WARN: Mail template not found (UUID=" + LEVY_IMPORT_MAIL_TEMPLATE_UU + ") – email notification skipped.");
			return;
		}

		String baseSubject = mailText.getMailHeader();
		String baseBody    = mailText.getMailText(false);

		int roleId = MSysConfig.getIntValue("SNR_MGR_SDR_ROLE_ID", 0);
		if (roleId <= 0)
			roleId = DB.getSQLValueEx(get_TrxName(),
					"SELECT AD_Role_ID FROM AD_Role WHERE Name='Snr Mgr - SDR' AND IsActive='Y' FETCH FIRST 1 ROWS ONLY");
		if (roleId <= 0) {
			addLog("WARN: Could not find 'Snr Mgr - SDR' role – email notification skipped.");
			return;
		}

		MClient client = MClient.get(getCtx());
		MUser from = MUser.get(getCtx(), Notifications.FROM_EMAIL_USER_ID);

		int emailsSent = 0;
		for (MUserRoles ur : MUserRoles.getOfRole(getCtx(), roleId)) {
			if (!ur.isActive()) continue;
			MUser user = new MUser(getCtx(), ur.getAD_User_ID(), null);
			if (Util.isEmpty(user.getEMail())) continue;
			String name = user.getName() != null ? user.getName() : "";
			String subject = baseSubject
					.replace("@Name@",       name)
					.replace("@FiscalYear@", year)
					.replace("@ZZ_Month@",   mthName);
			String body = baseBody
					.replace("@Name@",       name)
					.replace("@FiscalYear@", year)
					.replace("@ZZ_Month@",   mthName);
			client.sendEMail(from, user, subject, body, null, mailText.isHtml());
			emailsSent++;
		}
		addLog("Email notification sent to " + emailsSent + " Snr Mgr - SDR user(s).");
	}


	private static String normalizeFileName(String name) {
		if (name == null) return "";
		String s = name.trim();
		// strip leading/trailing tildes commonly added by attachments
		while (s.startsWith("~")) s = s.substring(1);
		while (s.endsWith("~"))  s = s.substring(0, s.length()-1);
		return s.trim();
	}
}

