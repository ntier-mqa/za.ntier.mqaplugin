package za.ntier.process;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;

/**
 * Imports DHET Employers CSV data into Business Partners and their locations.
 * Not a process — instantiate and call {@link #importFromStream(InputStream)}.
 */
public class DHETEmployersImporter {

	private static final String BUSINESS_ADDRESS    = "Business Address";
	private static final String RESIDENTIAL_ADDRESS = "Residential Address";
	private static final String POSTAL_ADDRESS      = "Postal Address";

	private final Properties ctx;
	private final String trxName;
	private final Consumer<String> logger;

	public DHETEmployersImporter(Properties ctx, String trxName, Consumer<String> logger) {
		this.ctx      = ctx;
		this.trxName  = trxName;
		this.logger   = logger != null ? logger : msg -> {};
	}

	/**
	 * Reads the employers CSV from the given stream and upserts Business Partners.
	 *
	 * @return summary string
	 */
	public String importFromStream(InputStream in) throws Exception {
		int cntBPsAdded   = 0;
		int cntBPsUpdated = 0;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			Map<String, Integer> headerIndexes = null;

			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) continue;

				if (headerIndexes == null) {
					headerIndexes = buildHeaderIndexes(parseCSVLine(line));
					validateRequiredHeaders(headerIndexes);
					continue;
				}

				String[] fields = parseCSVLine(line);

				String sdlNumber = getField(fields, headerIndexes, "SDL Number");
				if (sdlNumber == null || sdlNumber.isBlank()) {
					logger.accept("WARN: Skipping line with no SDL Number: " + line);
					continue;
				}

				String name           = getField(fields, headerIndexes, "Name");
				String numEmployeesStr = getField(fields, headerIndexes, "Number of Employees");
				String regNo          = getField(fields, headerIndexes, "Registration Number");
				String tradingName    = getField(fields, headerIndexes, "Trading Name");

				String bizAddr1    = getField(fields, headerIndexes, "Business Address 1");
				String bizAddr2    = getField(fields, headerIndexes, "Business Address 2");
				String bizAddr3    = getField(fields, headerIndexes, "Business Address 3");
				String bizAddr4    = getField(fields, headerIndexes, "Business Address 4");
				String bizPostal   = getField(fields, headerIndexes, "Business Postal Code");
				String bizDialCode = getField(fields, headerIndexes, "Business Dialing Code");
				String bizTelNum   = getField(fields, headerIndexes, "Business Telephone Number");
				String bizCellphone = getField(fields, headerIndexes, "Business Cellphone Number");

				String resAddr1  = getField(fields, headerIndexes, "Residential Address 1");
				String resAddr2  = getField(fields, headerIndexes, "Residential Address 2");
				String resAddr3  = getField(fields, headerIndexes, "Residential Address 3");
				String resAddr4  = getField(fields, headerIndexes, "Residential Address 4");
				String resPostal = getField(fields, headerIndexes, "Residential Postal Code");

				String posAddr1  = getField(fields, headerIndexes, "Postal Address 1");
				String posAddr2  = getField(fields, headerIndexes, "Postal Address 2");
				String posAddr3  = getField(fields, headerIndexes, "Postal Address 3");
				String posAddr4  = getField(fields, headerIndexes, "Postal Address 4");
				String posPostal = getField(fields, headerIndexes, "Postal Code");

				int cnt = DB.getSQLValue(trxName,
						"SELECT count(*) FROM C_BPartner bp WHERE bp.value = ?", sdlNumber);

				MBPartner_New bp;
				if (cnt <= 0) {
					cntBPsAdded++;
					bp = new MBPartner_New(ctx, 0, trxName);
					bp.setValue(sdlNumber);
					bp.setName(name);
					if (tradingName != null && !tradingName.isBlank()) bp.setName2(tradingName.trim());
					bp.setReferenceNo(regNo);
					bp.setC_BP_Group_ID(1000018); // UNKNOWN GROUP
					bp.setIsVendor(true);
					bp.setIsCustomer(false);
					bp.setIsEmployee(false);
					bp.setIsProspect(false);
					try {
						bp.setZZ_Number_Of_Employees(new BigDecimal(Integer.parseInt(numEmployeesStr)));
					} catch (NumberFormatException ignore) {}
					bp.setZZ_Is_MQA_Sector(true);
					bp.saveEx();
				} else {
					cntBPsUpdated++;
					int c_BPartner_ID = DB.getSQLValue(trxName,
							"SELECT C_BPartner_ID FROM C_BPartner bp WHERE bp.value = ?", sdlNumber);
					bp = new MBPartner_New(ctx, c_BPartner_ID, trxName);
					bp.setZZ_Is_MQA_Sector(true);
					if (bp.getReferenceNo() == null && regNo != null) bp.setReferenceNo(regNo);
					if (bp.getZZ_Number_Of_Employees() == null
							|| bp.getZZ_Number_Of_Employees().compareTo(BigDecimal.ZERO) <= 0) {
						try {
							bp.setZZ_Number_Of_Employees(new BigDecimal(Integer.parseInt(numEmployeesStr)));
						} catch (NumberFormatException ignore) {}
					}
					if ((bp.getName2() == null || bp.getName2().isBlank())
							&& tradingName != null && !tradingName.isBlank())
						bp.setName2(tradingName.trim());
					bp.saveEx();
				}

				ensurePartnerLocation(bp, BUSINESS_ADDRESS,
						bizAddr1, bizAddr2, bizAddr3, bizAddr4, bizPostal,
						buildPhone(bizDialCode, bizTelNum), bizCellphone);
				ensurePartnerLocation(bp, RESIDENTIAL_ADDRESS,
						resAddr1, resAddr2, resAddr3, resAddr4, resPostal, null, null);
				ensurePartnerLocation(bp, POSTAL_ADDRESS,
						posAddr1, posAddr2, posAddr3, posAddr4, posPostal, null, null);
			}
		}

		String summary = "New Business Partners Created: " + cntBPsAdded
				+ " | Business Partners Updated: " + cntBPsUpdated;
		logger.accept(summary);
		return summary;
	}

	// -------------------------------------------------------------------------
	// Location helpers
	// -------------------------------------------------------------------------

	private void ensurePartnerLocation(MBPartner_New bp, String locationName,
			String address1, String address2, String address3, String address4, String postal,
			String phone, String phone2) {
		if (!hasImportedAddress(address1, address2, address3, address4, postal)) return;

		MBPartnerLocation existing = findPartnerLocation(bp, locationName);
		if (existing == null) {
			MLocation newLoc = new MLocation(ctx, 0, trxName);
			applyImportedAddress(newLoc, address1, address2, address3, address4, postal);
			newLoc.saveEx();

			MBPartnerLocation newBpl = new MBPartnerLocation(bp);
			newBpl.setC_Location_ID(newLoc.getC_Location_ID());
			newBpl.setName(locationName);
			if (phone  != null && !phone.isBlank())  newBpl.setPhone(phone);
			if (phone2 != null && !phone2.isBlank()) newBpl.setPhone2(phone2);
			newBpl.saveEx();
			return;
		}

		if (!locationName.equals(existing.getName())) existing.setName(locationName);
		if (phone  != null && !phone.isBlank())  existing.setPhone(phone);
		if (phone2 != null && !phone2.isBlank()) existing.setPhone2(phone2);
		existing.saveEx();

		MLocation existingLoc = new MLocation(ctx, existing.getC_Location_ID(), trxName);
		if (existingLoc.get_ID() <= 0 || isMostlyBlank(existingLoc)) {
			if (existingLoc.get_ID() <= 0) existingLoc = new MLocation(ctx, 0, trxName);
			applyImportedAddress(existingLoc, address1, address2, address3, address4, postal);
			existingLoc.saveEx();
			existing.setC_Location_ID(existingLoc.getC_Location_ID());
			existing.saveEx();
		}
	}

	private MBPartnerLocation findPartnerLocation(MBPartner_New bp, String locationName) {
		MBPartnerLocation firstEmpty = null;
		for (MBPartnerLocation loc : bp.getLocations(false)) {
			if (locationName.equals(loc.getName())) return loc;
			if (firstEmpty == null) {
				MLocation ml = new MLocation(ctx, loc.getC_Location_ID(), trxName);
				if (ml.get_ID() <= 0 || isMostlyBlank(ml)) firstEmpty = loc;
			}
		}
		return firstEmpty;
	}

	private void applyImportedAddress(MLocation loc,
			String a1, String a2, String a3, String a4, String postal) {
		loc.setAddress1(a1); loc.setAddress2(a2);
		loc.setAddress3(a3); loc.setAddress4(a4);
		loc.setPostal(postal);
	}

	private boolean hasImportedAddress(String a1, String a2, String a3, String a4, String postal) {
		return isMeaningful(a1) || isMeaningful(a2) || isMeaningful(a3)
				|| isMeaningful(a4) || isMeaningful(postal);
	}

	private boolean isMostlyBlank(MLocation loc) {
		return !isMeaningful(loc.getAddress1()) && !isMeaningful(loc.getAddress2())
				&& !isMeaningful(loc.getAddress3()) && !isMeaningful(loc.getAddress4())
				&& !isMeaningful(loc.getPostal()) && !isMeaningful(loc.getCity());
	}

	private boolean isMeaningful(String v) {
		return v != null && !v.trim().isEmpty() && !v.trim().replace(".", "").isEmpty();
	}

	private String buildPhone(String dialCode, String number) {
		if (number == null || number.isBlank()) return null;
		number = number.trim();
		if (dialCode != null && !dialCode.isBlank()) {
			String code = dialCode.trim();
			if (code.length() == 2) code = "0" + code;
			return code + number;
		}
		return number;
	}

	// -------------------------------------------------------------------------
	// CSV / header helpers
	// -------------------------------------------------------------------------

	private Map<String, Integer> buildHeaderIndexes(String[] headers) {
		Map<String, Integer> map = new HashMap<>();
		for (int i = 0; i < headers.length; i++)
			map.put(normalizeHeader(headers[i]), i);
		return map;
	}

	private void validateRequiredHeaders(Map<String, Integer> h) {
		String[] required = {
			"SDL Number", "Name", "Number of Employees", "Registration Number",
			"Residential Address 1", "Residential Address 2", "Residential Address 3",
			"Residential Address 4", "Residential Postal Code",
			"Business Address 1", "Business Address 2", "Business Address 3",
			"Business Address 4", "Business Postal Code",
			"Postal Address 1", "Postal Address 2", "Postal Address 3",
			"Postal Address 4", "Postal Code"
		};
		for (String col : required)
			if (!h.containsKey(normalizeHeader(col)))
				throw new IllegalArgumentException("Missing required column: " + col);
	}

	private String getField(String[] fields, Map<String, Integer> h, String name) {
		Integer idx = h.get(normalizeHeader(name));
		if (idx == null || idx < 0 || idx >= fields.length) return null;
		return unquote(fields[idx]);
	}

	private String normalizeHeader(String s) {
		if (s == null) return "";
		return unquote(s).replace("﻿", "").trim().toLowerCase().replaceAll("\\s+", " ");
	}

	private String[] parseCSVLine(String line) {
		return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
	}

	private String unquote(String s) {
		return s == null ? null : s.replaceAll("^\"|\"$", "").trim();
	}
}
