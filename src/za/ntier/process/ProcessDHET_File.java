package za.ntier.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.adempiere.base.annotation.Parameter;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;

@org.adempiere.base.annotation.Process(name="za.ntier.process.ProcessDHET_File")
public class ProcessDHET_File extends SvrProcess {

	private static final String BUSINESS_ADDRESS = "Business Address";
	private static final String RESIDENTIAL_ADDRESS = "Residential Address";
	private static final String POSTAL_ADDRESS = "Postal Address";

	@Parameter(name="FileName")
	private String filePath;

	@Override
	protected void prepare() {

	}

	@Override
	protected String doIt() throws Exception {
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("File path not provided.");
		}
		
	    // STEP 1: Reset MQA_Sector to 'N' for all BPs currently marked 'Y'
	    String resetSQL = "UPDATE C_BPartner SET ZZ_Is_MQA_Sector = 'N' WHERE ZZ_Is_MQA_Sector = 'Y' and AD_Client_ID = 1000000";
	    int resetCount = DB.executeUpdateEx(resetSQL, get_TrxName());
	 //   addLog("Reset MQA_Sector to 'N' for " + resetCount + " BPs");


	    int cntBPsAdded = 0;
	    int cntBPsUpdated = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			Map<String, Integer> headerIndexes = null;

			while ((line = reader.readLine()) != null) {
				if (headerIndexes == null) {
					headerIndexes = buildHeaderIndexes(parseCSVLine(line));
					validateRequiredHeaders(headerIndexes);
					continue;
				}

				String[] fields = parseCSVLine(line);

				String sdlNumber = getField(fields, headerIndexes, "SDL Number");
				if (sdlNumber == null || sdlNumber.length() <= 0) {
					log.warning("Skipping invalid line with no sdlNumber: " + line);
					continue;
				}
				String name = getField(fields, headerIndexes, "Name");
				String numEmployeesStr = getField(fields, headerIndexes, "Number of Employees");
				String regNo = getField(fields, headerIndexes, "Registration Number");

				String businessAddress1 = getField(fields, headerIndexes, "Business Address 1");
				String businessAddress2 = getField(fields, headerIndexes, "Business Address 2");
				String businessAddress3 = getField(fields, headerIndexes, "Business Address 3");
				String businessAddress4 = getField(fields, headerIndexes, "Business Address 4");
				String businessPostal = getField(fields, headerIndexes, "Business Postal Code");

				String residentialAddress1 = getField(fields, headerIndexes, "Residential Address 1");
				String residentialAddress2 = getField(fields, headerIndexes, "Residential Address 2");
				String residentialAddress3 = getField(fields, headerIndexes, "Residential Address 3");
				String residentialAddress4 = getField(fields, headerIndexes, "Residential Address 4");
				String residentialPostal = getField(fields, headerIndexes, "Residential Postal Code");

				String postalAddress1 = getField(fields, headerIndexes, "Postal Address 1");
				String postalAddress2 = getField(fields, headerIndexes, "Postal Address 2");
				String postalAddress3 = getField(fields, headerIndexes, "Postal Address 3");
				String postalAddress4 = getField(fields, headerIndexes, "Postal Address 4");
				String postalPostal = getField(fields, headerIndexes, "Postal Code");

				// Check if BP exists
				MBPartner_New bp = null;
				int cnt =  DB.getSQLValue(get_TrxName(),"Select count(*) from C_Bpartner bp where bp.value = ?",sdlNumber);
				if (cnt > 0) {
					int c_BPartner_ID =  DB.getSQLValue(get_TrxName(),"Select C_BPartner_id from C_Bpartner bp where bp.value = ?",sdlNumber);
					bp = new MBPartner_New(getCtx(), c_BPartner_ID, get_TrxName());
				}
				if (cnt  <= 0) {
					cntBPsAdded++;
					bp = new MBPartner_New(getCtx(), 0, get_TrxName());
					bp.setValue(sdlNumber);
					bp.setName(name);
					bp.setReferenceNo(regNo);
					bp.setC_BP_Group_ID(1000018);  // UNKNOWN GROUP
					bp.setIsVendor(true);
					bp.setIsCustomer(false);
					bp.setIsEmployee(false);
					bp.setIsProspect(false);

					// Custom column ZZ_Number_Of_Employees
					try {
						int numEmployees = Integer.parseInt(numEmployeesStr);
						bp.setZZ_Number_Of_Employees(new BigDecimal(numEmployees));
					} catch (NumberFormatException e) {
						log.warning("Invalid NumberOfEmployees for " + sdlNumber);
					}
					bp.setZZ_Is_MQA_Sector(true);

					bp.saveEx();

					ensurePartnerLocation(bp, BUSINESS_ADDRESS,
							businessAddress1, businessAddress2, businessAddress3, businessAddress4, businessPostal);
					ensurePartnerLocation(bp, RESIDENTIAL_ADDRESS,
							residentialAddress1, residentialAddress2, residentialAddress3, residentialAddress4, residentialPostal);
					ensurePartnerLocation(bp, POSTAL_ADDRESS,
							postalAddress1, postalAddress2, postalAddress3, postalAddress4, postalPostal);
				} else {
					cntBPsUpdated++;
					bp.setZZ_Is_MQA_Sector(true);
					if (bp.getReferenceNo() == null && regNo != null) {
						bp.setReferenceNo(regNo);
					}
					if (bp.getZZ_Number_Of_Employees() == null || bp.getZZ_Number_Of_Employees().compareTo(BigDecimal.ZERO) <= 0) {
						try {
							int numEmployees = Integer.parseInt(numEmployeesStr);
							bp.setZZ_Number_Of_Employees(new BigDecimal(numEmployees));
						} catch (NumberFormatException e) {
							log.warning("Invalid NumberOfEmployees for " + sdlNumber);
						}
					}
					bp.saveEx();

					ensurePartnerLocation(bp, BUSINESS_ADDRESS,
							businessAddress1, businessAddress2, businessAddress3, businessAddress4, businessPostal);
					ensurePartnerLocation(bp, RESIDENTIAL_ADDRESS,
							residentialAddress1, residentialAddress2, residentialAddress3, residentialAddress4, residentialPostal);
					ensurePartnerLocation(bp, POSTAL_ADDRESS,
							postalAddress1, postalAddress2, postalAddress3, postalAddress4, postalPostal);
				}
			}
		}
		addLog("New Business Partners Created:  " + cntBPsAdded) ;
		addLog("Business Partners Updated    :  " + cntBPsUpdated);

		return "CSV file processed and Business Partners created.";
	}

	private Map<String, Integer> buildHeaderIndexes(String[] headers) {
		Map<String, Integer> headerIndexes = new HashMap<>();
		for (int i = 0; i < headers.length; i++) {
			headerIndexes.put(normalizeHeader(headers[i]), i);
		}
		return headerIndexes;
	}

	private void validateRequiredHeaders(Map<String, Integer> headerIndexes) {
		String[] requiredHeaders = {
				"SDL Number", "Name", "Number of Employees", "Registration Number",
				"Residential Address 1", "Residential Address 2", "Residential Address 3", "Residential Address 4", "Residential Postal Code",
				"Business Address 1", "Business Address 2", "Business Address 3", "Business Address 4", "Business Postal Code",
				"Postal Address 1", "Postal Address 2", "Postal Address 3", "Postal Address 4", "Postal Code"
		};

		for (String header : requiredHeaders) {
			if (!headerIndexes.containsKey(normalizeHeader(header))) {
				throw new IllegalArgumentException("Missing required column header: " + header);
			}
		}
	}

	private String getField(String[] fields, Map<String, Integer> headerIndexes, String headerName) {
		Integer index = headerIndexes.get(normalizeHeader(headerName));
		if (index == null || index < 0 || index >= fields.length) {
			return null;
		}
		return unquote(fields[index]);
	}

	private String normalizeHeader(String header) {
		if (header == null) {
			return "";
		}
		return unquote(header).replace("\uFEFF", "").trim().toLowerCase().replaceAll("\\s+", " ");
	}

	private void ensurePartnerLocation(MBPartner_New bp, String locationName,
			String address1, String address2, String address3, String address4, String postal) {
		if (!hasImportedAddress(address1, address2, address3, address4, postal)) {
			return;
		}

		MBPartnerLocation existingLocationLink = findPartnerLocation(bp, locationName);
		if (existingLocationLink == null) {
			MLocation newLocation = new MLocation(getCtx(), 0, get_TrxName());
			applyImportedAddress(newLocation, address1, address2, address3, address4, postal);
			newLocation.saveEx();

			MBPartnerLocation newPartnerLocation = new MBPartnerLocation(bp);
			newPartnerLocation.setC_Location_ID(newLocation.getC_Location_ID());
			newPartnerLocation.setName(locationName);
			newPartnerLocation.saveEx();
			return;
		}

		if (!locationName.equals(existingLocationLink.getName())) {
			existingLocationLink.setName(locationName);
			existingLocationLink.saveEx();
		}

		MLocation existingLocation = new MLocation(getCtx(), existingLocationLink.getC_Location_ID(), get_TrxName());
		if (existingLocation.get_ID() <= 0 || isMostlyBlank(existingLocation)) {
			if (existingLocation.get_ID() <= 0) {
				existingLocation = new MLocation(getCtx(), 0, get_TrxName());
			}
			applyImportedAddress(existingLocation, address1, address2, address3, address4, postal);
			existingLocation.saveEx();
			existingLocationLink.setC_Location_ID(existingLocation.getC_Location_ID());
			existingLocationLink.saveEx();
		}
	}

	private MBPartnerLocation findPartnerLocation(MBPartner_New bp, String locationName) {
		MBPartnerLocation firstEmptyLocation = null;
		for (MBPartnerLocation location : bp.getLocations(false)) {
			if (locationName.equals(location.getName())) {
				return location;
			}

			if (firstEmptyLocation == null) {
				MLocation existingLocation = new MLocation(getCtx(), location.getC_Location_ID(), get_TrxName());
				if (existingLocation.get_ID() <= 0 || isMostlyBlank(existingLocation)) {
					firstEmptyLocation = location;
				}
			}
		}
		return firstEmptyLocation;
	}

	private void applyImportedAddress(MLocation location,
			String address1, String address2, String address3, String address4, String postal) {
		location.setAddress1(address1);
		location.setAddress2(address2);
		location.setAddress3(address3);
		location.setAddress4(address4);
		location.setPostal(postal);
	}

	private boolean hasImportedAddress(String address1, String address2, String address3, String address4, String postal) {
		return isMeaningful(address1)
				|| isMeaningful(address2)
				|| isMeaningful(address3)
				|| isMeaningful(address4)
				|| isMeaningful(postal);
	}

	private boolean isMostlyBlank(MLocation location) {
		return !isMeaningful(location.getAddress1())
				&& !isMeaningful(location.getAddress2())
				&& !isMeaningful(location.getAddress3())
				&& !isMeaningful(location.getAddress4())
				&& !isMeaningful(location.getPostal())
				&& !isMeaningful(location.getCity());
	}

	private boolean isMeaningful(String value) {
		if (value == null) {
			return false;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			return false;
		}
		return normalized.replace(".", "").trim().length() > 0;
	}

	private String[] parseCSVLine(String line) {
		// Handle quoted commas
		return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
	}

	private String unquote(String s) {
		return s == null ? null : s.replaceAll("^\"|\"$", "").trim();
	}


}
