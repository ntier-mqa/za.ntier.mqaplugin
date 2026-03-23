package za.ntier.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;

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
			boolean isFirstLine = true;

			while ((line = reader.readLine()) != null) {
				if (isFirstLine) {
					isFirstLine = false; // Skip header
					continue;
				}

				String[] fields = parseCSVLine(line);
				if (fields.length < 59) {
					log.warning("Skipping invalid line: " + line);
					continue;
				}

				String sdlNumber = unquote(fields[0]);
				if (sdlNumber == null || sdlNumber.length() <= 0) {
					log.warning("Skipping invalid line with no sdlNumber: " + line);
					continue;
				}
				String name = unquote(fields[4]);
				String numEmployeesStr = unquote(fields[46]);
				String regNo = unquote(fields[9]);

				String businessAddress1 = unquote(fields[27]);
				String businessAddress2 = unquote(fields[28]);
				String businessAddress3 = unquote(fields[29]);
				String businessAddress4 = unquote(fields[30]);
				String businessPostal = unquote(fields[31]);

				String residentialAddress1 = unquote(fields[16]);
				String residentialAddress2 = unquote(fields[17]);
				String residentialAddress3 = unquote(fields[18]);
				String residentialAddress4 = unquote(fields[19]);
				String residentialPostal = unquote(fields[20]);

				String postalAddress1 = unquote(fields[37]);
				String postalAddress2 = unquote(fields[38]);
				String postalAddress3 = unquote(fields[39]);
				String postalAddress4 = unquote(fields[40]);
				String postalPostal = unquote(fields[41]);

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
		}

		MLocation existingLocation = new MLocation(getCtx(), existingLocationLink.getC_Location_ID(), get_TrxName());
		if (existingLocation.get_ID() <= 0 || isMostlyBlank(existingLocation)) {
			if (existingLocation.get_ID() <= 0) {
				existingLocation = new MLocation(getCtx(), 0, get_TrxName());
			}
			applyImportedAddress(existingLocation, address1, address2, address3, address4, postal);
			existingLocation.saveEx();
			existingLocationLink.setC_Location_ID(existingLocation.getC_Location_ID());
		}
		existingLocationLink.saveEx();
	}

	private MBPartnerLocation findPartnerLocation(MBPartner_New bp, String locationName) {
		MBPartnerLocation fallbackLocation = null;
		for (MBPartnerLocation location : bp.getLocations(false)) {
			if (!location.isActive()) {
				continue;
			}
			if (locationName.equals(location.getName())) {
				return location;
			}
			if (fallbackLocation == null && BUSINESS_ADDRESS.equals(locationName) && canReuseAsBusinessLocation(location)) {
				fallbackLocation = location;
			}
		}
		return fallbackLocation;
	}

	private boolean canReuseAsBusinessLocation(MBPartnerLocation location) {
		return !isMeaningful(location.getName())
				&& (location.isBillTo() || location.isShipTo() || location.isPayFrom() || location.isRemitTo());
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
