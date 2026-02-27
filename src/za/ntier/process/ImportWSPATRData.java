package za.ntier.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.adempiere.base.annotation.Parameter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MClient;
import org.compiere.model.MLocation;
import org.compiere.model.MUser;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Approvals;


@org.adempiere.base.annotation.Process(name="za.ntier.process.ImportWSPATRData")
public class ImportWSPATRData extends SvrProcess {
	@Parameter(name="FileName")
	private String filePath;
	
    @Parameter(name="FinancialYear")
    private String filterYear;   

	@Parameter(name="ClearExisting")
	private boolean clearExisting;  

	private List<String> unresolvedList = new ArrayList<>();
	private static final DataFormatter DF = new DataFormatter();
	
	private static final String[] NAME_HEADERS = {
		    "organisationlegalname", "organisation legal name",
		    "organisation name", "organisation_name",
		    "organization legal name", "organizationlegalname",
		    "organization name", "organization_name",
		    "organisation trading name", "trading name",
		    "trading_name", "organisationtradingname",
		    "Company Name"
		};

		private static final String[] NUM_EMP_HEADERS = {
		    "totalemployment", "total employment", "numberofemployees",
		    "total_employment", "number_of_employees"
		};

		private static final String[] ADDRESS_HEADERS = {
		    "organisation address", "organization address",
		    "organisationaddress", "organizationaddress",
		    "organisation physical address", "organization physical address",
		    "physical address", "physicaladdress", "postal address"
		};

	@Override
	protected void prepare() {

	}

	@Override
	protected String doIt() throws Exception {
		int deleted = 0;
		if (clearExisting) {
            if (filterYear != null && !filterYear.trim().isEmpty()) {
                deleted = DB.executeUpdateEx(
                    "DELETE FROM ZZ_WSP_ATR_Approvals WHERE zz_financial_year = ?",
                    new Object[]{filterYear.trim()},
                    get_TrxName()
                );
            } else {
                deleted = DB.executeUpdateEx(
                    "DELETE FROM ZZ_WSP_ATR_Approvals",
                    get_TrxName()
                );
            }
            addLog("Cleared existing data: " + deleted + " row(s)");
        }

		if (filePath == null || filePath.isEmpty())
			throw new IllegalArgumentException("File path not provided.");


		try (FileInputStream fis = new FileInputStream(filePath);
				Workbook wb = WorkbookFactory.create(fis)) {

			Sheet sheet = wb.getSheetAt(0);
			Iterator<Row> it = sheet.iterator();
			if (!it.hasNext()) throw new IllegalArgumentException("Empty sheet");

			// 1) Read header row and build index
			Row header = it.next();
			Map<String,Integer> H = buildHeaderIndex(header);

			// Optional: verify required columns exist (fail fast with a clear message)
			List<String> required = Arrays.asList(
					"SDLNumber", "ParentSDLNumber", "WSPYear", "PlanningGrantStatus", "TotalEmployment"
					);
			for (String col : required) {
				if (!H.containsKey(norm(col))) {
					throw new IllegalArgumentException("Missing required column: " + col);
				}
			}

			// 2) Read the rest by header
			while (it.hasNext()) {
				Row row = it.next();
				if (row == null) continue;

				String sdlNumber   = byHeader(row, H, "SDLNumber");
		        String parentSDL   = byHeaderAny(row, H,
		                             new String[]{"parentsdlnumber","parent sdl number","parcntsdlnumber"});
		        String financialYr = byHeader(row, H, "WSPYear");
		        String grantStatus = byHeader(row, H, "PlanningGrantStatus");
		        String numEmpStr   = byHeaderAny(row, H, NUM_EMP_HEADERS);
		        if (sdlNumber.equals("L999999999") ||  sdlNumber.equals("L000000000")) {
		        	continue;
		        }

		        // Try to find existing partner by SDL or parent SDL
		        String value = (sdlNumber != null && !sdlNumber.isBlank()) ? sdlNumber : parentSDL;
		        int cnt = DB.getSQLValue(get_TrxName(),
		                   "SELECT COUNT(*) FROM C_BPartner bp WHERE bp.Value=?", value);
		        MBPartner_New bp = null;
		        if (cnt > 0) {
		            int c_BPartner_ID = DB.getSQLValue(get_TrxName(),
		                               "SELECT C_BPartner_ID FROM C_BPartner WHERE Value=?", value);
		            bp = new MBPartner_New(getCtx(), c_BPartner_ID, get_TrxName());
		        } else {
		            // --- create a new BP ---
		            String name = byHeaderAny(row, H, NAME_HEADERS);
		            if (name == null || name.isBlank()) {
		                // fall back to trading name if present
		                name = byHeaderAny(row, H,
		                        new String[]{"organisation trading name","trading name","trading_name"});
		            }
		            if (name == null || name.isBlank()) {
		                // no name found – log unresolved and skip
		                unresolvedList.add(sdlNumber + "," + parentSDL);
		                continue;
		            }
		            bp = new MBPartner_New(getCtx(), 0, get_TrxName());
		            bp.setValue(value);
		            bp.setName(name);
		            bp.setReferenceNo(parentSDL);      // or another column if you store registration no.
		            bp.setC_BP_Group_ID(1000018);      // UNKNOWN GROUP
		            bp.setIsVendor(true);
		            bp.setIsCustomer(false);
		            bp.setIsEmployee(false);
		            bp.setIsProspect(false);
		            // set number of employees if provided
		            try {
		                int n = Integer.parseInt(numEmpStr.replaceAll("\\s", ""));
		                bp.setZZ_Number_Of_Employees(new BigDecimal(n));
		            } catch (NumberFormatException ignore) {}
		            bp.saveEx();

		            // Create and assign a location if address is available
		            String addr = byHeaderAny(row, H, ADDRESS_HEADERS);
		            MLocation businessLoc = new MLocation(getCtx(), 0, get_TrxName());
		            if (addr != null && !addr.isBlank()) {
		                setLocationFromAddress(businessLoc, addr);
		            }
		            businessLoc.saveEx();

		            // Create a BP location linking partner and location
		            org.compiere.model.MBPartnerLocation bpl =
		                new org.compiere.model.MBPartnerLocation(bp);
		            bpl.setC_Location_ID(businessLoc.get_ID());
		            bpl.setIsBillTo(true);
		            bpl.setIsShipTo(true);
		            bpl.setIsPayFrom(true);
		            bpl.setIsRemitTo(true);
		            bpl.saveEx();
		        }
				

				// Check if record exists for same BP and Financial Year
				String sql = "SELECT ZZ_WSP_ATR_Approvals_ID FROM ZZ_WSP_ATR_Approvals WHERE C_BPartner_ID=? AND zz_financial_year=?";
				int wspID = DB.getSQLValue(get_TrxName(), sql, bp.get_ID(), financialYr);

				X_ZZ_WSP_ATR_Approvals record;
				if (wspID > 0) {
					record = new X_ZZ_WSP_ATR_Approvals(getCtx(), wspID, get_TrxName());
				} else {
					record = new X_ZZ_WSP_ATR_Approvals(getCtx(), 0, get_TrxName());
				}

				record.setC_BPartner_ID(bp.get_ID());
				record.setZZ_Financial_Year(financialYr);
				record.setZZ_Grant_Status((grantStatus.equals("Approved"))? "A" : "R" );
				record.setProcessedOn(new Timestamp(System.currentTimeMillis()));
				record.saveEx();
			}

			if (!unresolvedList.isEmpty()) {
				// Use tmp directory (works cross-platform)
				File logFile = new File(System.getProperty("java.io.tmpdir"), "unresolved_bps.csv");

				try (PrintWriter writer = new PrintWriter(logFile)) {
					writer.println("Key1,Key2");
					for (String line : unresolvedList) {
						writer.println(line);
					}
				}
				List<File> fileList = new ArrayList<File>();
				fileList.add(logFile);

				addLog("Unresolved BPs: " + unresolvedList.size());
				/*

				// ✅ This enables the Download link in Process Monitor
				ProcessInfo pi = getProcessInfo();
				if (pi != null) {
					pi.setExport(true);
					pi.setExportFile(logFile);
					pi.setExportFileExtension("csv");
				}
				 */

				MClient client = MClient.get(getCtx(), getAD_Client_ID());

				if (fileList != null && !fileList.isEmpty()) {
					MUser from = new MUser(getCtx(), getAD_User_ID(), null);

					if (!client.sendEMailAttachments(from, from, "Unresolved List", "Unresolved List for " + filePath, fileList)) {
						addLog("Could not send Email");
					}

					addLog("Unresolved BPs: " + unresolvedList.size() + " (Download from Process Monitor)");
				}
			}




			return "WSP-ATR Approvals import complete.";
		}
	}
	
	/** Returns the first non‑blank value found for any of the supplied headers. */
	private String byHeaderAny(Row row, Map<String,Integer> H, String[] possible) {
	    for (String header : possible) {
	        String val = byHeader(row, H, header);
	        if (val != null && !val.isBlank()) {
	            return val.trim();
	        }
	    }
	    return "";
	}

	/** Splits a full address into lines and postal code. */
	private void setLocationFromAddress(MLocation loc, String fullAddress) {
	    // Split on commas; fallback to spaces if no commas
	    String[] parts = fullAddress.contains(",")
	            ? fullAddress.split(",")
	            : fullAddress.split("\\s+");
	    // Assign up to four address lines
	    if (parts.length > 0) loc.setAddress1(parts[0].trim());
	    if (parts.length > 1) loc.setAddress2(parts[1].trim());
	    if (parts.length > 2) loc.setAddress3(parts[2].trim());
	    if (parts.length > 3) loc.setAddress4(parts[3].trim());
	    // Detect a postal code at the end (4–6 digits)
	    String last = parts[parts.length - 1].trim();
	    if (last.matches(".*\\b\\d{4,6}\\b$")) {
	        String code = last.replaceAll(".*?(\\d{4,6})$", "$1");
	        loc.setPostal(code);
	    }
	}

	/** Normalize header text for matching (case/space-insensitive). */
	private String norm(String s) {
		return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
	}

	/** Build a map: normalized header -> column index, from the first row. */
	private Map<String,Integer> buildHeaderIndex(Row headerRow) {
		Map<String,Integer> map = new HashMap<>();
		for (Cell c : headerRow) {
			String name = norm(DF.formatCellValue(c));
			if (!name.isEmpty()) map.put(name, c.getColumnIndex());
		}
		return map;
	}

	/** Read a cell by header name using the header map. */
	private String byHeader(Row row, Map<String,Integer> H, String header) {
		Integer idx = H.get(norm(header));
		if (idx == null) return "";
		Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		return cell == null ? "" : DF.formatCellValue(cell).trim();
	}
}
