package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MAttachment;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import za.co.ntier.api.model.MUser_New;
import za.co.ntier.api.model.X_ZZSdf;
import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.ntier.models.MZZSdfOrganisation;

/**
 * Bulk-loads SDF registration documents from the SG data dump.
 *
 * Expected directory layout:
 *   BASE_DIR / SDF list*.xlsx             ← reference spreadsheet (or in parent dir)
 *   BASE_DIR / <id_number> / ID Copy / <file>
 *   BASE_DIR / <id_number> / <sdl_number> / <doc_type_folder> / <file>
 *
 * For each ID number directory:
 *   1. Resolve ZZSdf via ZZ_ID_Passport_No.
 *      If not found in DB, look up the ID in the SDF Excel spreadsheet and
 *      create an AD_User (MUser_New) + ZZSdf record from it.
 *      Error and skip if multiple ZZSdf records are found.
 *   2. Attach "ID Copy" files to the ZZSdf record.
 *   3. For each SDL (L-number) subdirectory:
 *        a. Resolve C_BPartner by Value = SDL number.
 *        b. Find ZZSdfOrganisation by BPartner + SdfId; if not found,
 *           find one with that BPartner and no SdfId, then set the SdfId.
 *        c. Attach all files under every subfolder to ZZSdfOrganisation.
 *
 * All diagnostic messages (skips, duplicates, errors) are collected during
 * the run and flushed to the process log only after every directory has been
 * attempted, so a single bad record never stops the rest.
 *
 * Process parameter:
 *   ClearAttachments (Y/N) — when Y, deletes existing attachments on each
 *                            ZZSdf and ZZSdfOrganisation before loading.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ImportSgSdfDocuments")
public class ImportSgSdfDocuments extends SvrProcess {

    private static final String BASE_DIR    = "/tmp/SG_Data_070526/MQAR008388";
    private static final String ID_COPY_DIR = "ID Copy";

    private static final int TABLE_ZZ_SDF  = X_ZZSdf.Table_ID;
    private static final int TABLE_SDF_ORG = X_ZZSdfOrganisation.Table_ID;

    /** Digits-only pattern used to parse "18years", "29", etc. from Experience. */
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    // Outcome codes returned by attachFile()
    private static final int ATTACH_LOADED    =  1;
    private static final int ATTACH_DUPLICATE =  0;
    private static final int ATTACH_SKIPPED   = -1;

    private boolean clearAttachments = false;

    private int processed       = 0;
    private int idCopyFiles     = 0;
    private int orgFiles        = 0;
    private int alreadyAttached = 0;
    private int skipped         = 0;
    private int sdfCreated      = 0;

    /** All diagnostic messages collected during the run, flushed at the end. */
    private final List<String> deferredLog = new ArrayList<>();

    /**
     * SDF rows keyed by IDNo, loaded from the "SDF list*.xlsx" spreadsheet
     * before any directory is processed.
     */
    private final Map<String, ExcelSdfRow> excelSdfMap = new HashMap<>();

    // =========================================================================
    //  Inner class — one row from the SDF Excel spreadsheet
    // =========================================================================

    private static class ExcelSdfRow {
        String title, firstName, middleName, surname, initials;
        String idNo, alternateIdType, dateOfBirth;
        String gender, populationGroup, email, phone, mobile;
        String disability, homeLanguage, nationality;
        String citizenStatus, socioEconomicStatus, highestEducation;
        String currentOccupation, experience;
        int    yearsInOccupation;
    }

    // =========================================================================
    //  SvrProcess lifecycle
    // =========================================================================

    @Override
    protected void prepare() {
        for (ProcessInfoParameter p : getParameter()) {
            if ("ClearAttachments".equalsIgnoreCase(p.getParameterName())) {
                clearAttachments = "Y".equals(p.getParameter());
            } else {
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), p);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        File base = new File(BASE_DIR);
        if (!base.isDirectory())
            throw new AdempiereException("Base directory not found: " + BASE_DIR);

        // Load the reference spreadsheet first so ZZSdf creation is available
        // for every ID directory that follows.
        loadExcelSdfData(base);

        for (File idDir : dirs(base)) {
            try {
                processIdDirectory(idDir);
            } catch (Exception e) {
                deferredLog.add("FATAL ERROR in directory " + idDir.getName() + ": " + e.getMessage());
                skipped++;
            }
        }

        // Flush everything to the process log after all directories are done
        for (String msg : deferredLog) {
            addLog(msg);
        }

        return "Processed=" + processed
                + "  SdfCreated=" + sdfCreated
                + "  IDCopyFiles=" + idCopyFiles
                + "  OrgFiles=" + orgFiles
                + "  AlreadyAttached=" + alreadyAttached
                + "  Skipped=" + skipped;
    }

    // =========================================================================
    //  Excel loading
    // =========================================================================

    /**
     * Searches for a file matching "SDF list*.xlsx" in {@code base} and, if not
     * found there, in its parent directory.  Populates {@link #excelSdfMap}.
     */
    private void loadExcelSdfData(File base) {
        File xlsx = findExcelFile(base);
        if (xlsx == null) {
            deferredLog.add("WARN: No 'SDF list*.xlsx' found in "
                    + base.getAbsolutePath() + " or its parent — new SDF creation disabled");
            return;
        }

        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(xlsx, null, true /*read-only*/)) {
            Sheet sheet = wb.getSheetAt(0);

            // Build header → column-index map from the first row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                deferredLog.add("WARN: SDF Excel has no header row — creation disabled");
                return;
            }
            Map<String, Integer> colIdx = new HashMap<>();
            for (Cell c : headerRow) {
                String name = fmt.formatCellValue(c).trim();
                if (!name.isEmpty())
                    colIdx.put(name, c.getColumnIndex());
            }

            // Read data rows
            int loaded = 0;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String idNo = cell(row, colIdx, "IDNo", fmt).trim();
                if (idNo.isEmpty()) continue;

                ExcelSdfRow sr        = new ExcelSdfRow();
                sr.idNo               = idNo;
                sr.title              = cell(row, colIdx, "Title",                 fmt);
                sr.firstName          = cell(row, colIdx, "FirstName",             fmt);
                sr.middleName         = cell(row, colIdx, "MiddleName",            fmt);
                sr.surname            = cell(row, colIdx, "Surname",               fmt);
                sr.initials           = cell(row, colIdx, "Initials",              fmt);
                sr.alternateIdType    = cell(row, colIdx, "AlternateIDType",       fmt);
                sr.dateOfBirth        = cell(row, colIdx, "Date of Birth",         fmt);
                sr.gender             = cell(row, colIdx, "Gender",                fmt);
                sr.populationGroup    = cell(row, colIdx, "PopulationGroup",       fmt);
                sr.email              = cell(row, colIdx, "EMail",                 fmt);
                sr.phone              = cell(row, colIdx, "TelephoneNumber",       fmt);
                sr.mobile             = cell(row, colIdx, "CellPhoneNumber",       fmt);
                sr.disability         = cell(row, colIdx, "Disability",            fmt);
                sr.homeLanguage       = cell(row, colIdx, "HomeLanguage",          fmt);
                sr.nationality        = cell(row, colIdx, "Nationality",           fmt);
                sr.citizenStatus      = cell(row, colIdx, "CitizenResidentialStatus", fmt);
                sr.socioEconomicStatus= cell(row, colIdx, "SocioEconomicStatus",   fmt);
                sr.highestEducation   = cell(row, colIdx, "HighestEducation",      fmt);
                sr.currentOccupation  = cell(row, colIdx, "CurrentOccupation",     fmt);
                sr.experience         = cell(row, colIdx, "Experience",            fmt);
                sr.yearsInOccupation  = parseDigits(cell(row, colIdx, "YearsInOccupation", fmt));

                excelSdfMap.put(idNo, sr);
                loaded++;
            }

            deferredLog.add("INFO: Loaded " + loaded + " SDF rows from " + xlsx.getName());

        } catch (Exception e) {
            deferredLog.add("ERROR reading SDF Excel (" + xlsx.getName() + "): " + e.getMessage());
        }
    }

    /** Finds "SDF list*.xlsx" in {@code dir}, then in its parent. */
    private File findExcelFile(File dir) {
        FileFilter filter = f -> f.isFile()
                && f.getName().startsWith("SDF list")
                && f.getName().endsWith(".xlsx");

        File[] hits = dir.listFiles(filter);
        if (hits != null && hits.length > 0) return hits[0];

        File parent = dir.getParentFile();
        if (parent != null) {
            hits = parent.listFiles(filter);
            if (hits != null && hits.length > 0) return hits[0];
        }
        return null;
    }

    // =========================================================================
    //  Per-ID directory
    // =========================================================================

    private void processIdDirectory(File idDir) {
        String idNumber = idDir.getName();

        int[] sdf = findSdf(idNumber);
        if (sdf == null) {
            skipped++;
            return;
        }
        int sdfId    = sdf[0];
        int adUserId = sdf[1];

        File idCopyDir = new File(idDir, ID_COPY_DIR);
        if (idCopyDir.isDirectory()) {
            if (clearAttachments)
                clearAttachment(TABLE_ZZ_SDF, sdfId);
            for (File f : files(idCopyDir)) {
                int result = attachFile(TABLE_ZZ_SDF, sdfId, f, "ID=" + idNumber);
                if      (result == ATTACH_LOADED)    idCopyFiles++;
                else if (result == ATTACH_DUPLICATE) alreadyAttached++;
                else                                 skipped++;
            }
        }

        for (File lDir : dirs(idDir)) {
            if (ID_COPY_DIR.equals(lDir.getName())) continue;
            processLDirectory(lDir, sdfId, idNumber);
        }

        processed++;
    }

    // =========================================================================
    //  Per-SDL (L-number) directory
    // =========================================================================

    private void processLDirectory(File lDir, int sdfId, String idNumber) {
        String sdlNumber = lDir.getName();

        int bpId = findBPartner(sdlNumber);
        if (bpId <= 0) {
            deferredLog.add("No C_BPartner for SDL=" + sdlNumber + " (ID=" + idNumber + ") — skipped");
            skipped++;
            return;
        }

        int orgId = findOrLinkOrg(bpId, sdfId, sdlNumber);
        if (orgId <= 0) {
            deferredLog.add("No ZZSdfOrganisation for SDL=" + sdlNumber + " BP=" + bpId + " — skipped");
            skipped++;
            return;
        }

        if (clearAttachments)
            clearAttachment(TABLE_SDF_ORG, orgId);

        for (File subDir : dirs(lDir)) {
            for (File f : files(subDir)) {
                int result = attachFile(TABLE_SDF_ORG, orgId, f, "SDL=" + sdlNumber);
                if      (result == ATTACH_LOADED)    orgFiles++;
                else if (result == ATTACH_DUPLICATE) alreadyAttached++;
                else                                 skipped++;
            }
        }
        // Files placed directly in the SDL dir (edge case)
        for (File f : files(lDir)) {
            int result = attachFile(TABLE_SDF_ORG, orgId, f, "SDL=" + sdlNumber);
            if      (result == ATTACH_LOADED)    orgFiles++;
            else if (result == ATTACH_DUPLICATE) alreadyAttached++;
            else                                 skipped++;
        }
    }

    // =========================================================================
    //  ZZSdf lookup / creation
    // =========================================================================

    /**
     * Returns [sdfId, adUserId], or null if the record cannot be resolved.
     *
     * <ol>
     *   <li>Query DB by ZZ_ID_Passport_No.</li>
     *   <li>If not found, look up the ID in the Excel map and create
     *       an AD_User + ZZSdf record.</li>
     *   <li>If multiple DB records are found, log an error and return null.</li>
     * </ol>
     */
    private int[] findSdf(String idNumber) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                    "SELECT zzsdf_id, ad_user_id FROM zzsdf WHERE zz_id_passport_no = ?", null);
            pst.setString(1, idNumber);
            rs = pst.executeQuery();
            int[] first = null;
            boolean duplicate = false;
            while (rs.next()) {
                if (first == null)
                    first = new int[]{ rs.getInt(1), rs.getInt(2) };
                else
                    duplicate = true;
            }
            if (duplicate) {
                deferredLog.add("ERROR: multiple ZZSdf records for ID=" + idNumber + " — skipped");
                return null;
            }
            if (first != null)
                return first;
        } catch (Exception e) {
            deferredLog.add("Error querying ZZSdf for ID=" + idNumber + ": " + e.getMessage());
            return null;
        } finally {
            DB.close(rs, pst);
        }

        // Not found in DB — attempt creation from Excel data
        ExcelSdfRow row = excelSdfMap.get(idNumber);
        if (row == null) {
            deferredLog.add("No ZZSdf record and no Excel entry for ID=" + idNumber + " — skipped");
            return null;
        }
        return createSdfFromExcel(row);
    }

    /**
     * Creates an AD_User (MUser_New) and a ZZSdf record from a spreadsheet row.
     *
     * @return [sdfId, adUserId] on success, null on failure.
     */
    private int[] createSdfFromExcel(ExcelSdfRow row) {
        String trxName = Trx.createTrxName("SGSdfCreate");
        Trx trx = Trx.get(trxName, true);
        try {

            // --- 1. Create AD_User -------------------------------------------
            MUser_New user = new MUser_New(getCtx(), 0, trxName);
            String fullName = (trim(row.firstName) + " " + trim(row.surname)).trim();
            if (fullName.isEmpty()) fullName = row.idNo;
            user.setName(fullName);
            if (!trim(row.email).isEmpty())  user.setEMail(row.email.trim());
            if (!trim(row.phone).isEmpty())  user.setPhone(row.phone.trim());
            if (!trim(row.mobile).isEmpty()) user.setPhone2(row.mobile.trim());
            if (!trim(row.firstName).isEmpty())   user.setZZFirstName(row.firstName.trim());
            if (!trim(row.surname).isEmpty())     user.setZZSurname(row.surname.trim());
            if (!trim(row.middleName).isEmpty())  user.setZZMiddleName(row.middleName.trim());
            if (!trim(row.title).isEmpty())       user.setZZLkpTitle(row.title.trim());

            if (!trim(row.dateOfBirth).isEmpty()) {
                try {
                    java.util.Date d = new SimpleDateFormat("yyyy/MM/dd").parse(row.dateOfBirth.trim());
                    user.setBirthday(new Timestamp(d.getTime()));
                } catch (Exception e) {
                    deferredLog.add("WARN: Could not parse Date of Birth '"
                            + row.dateOfBirth + "' for ID=" + row.idNo);
                }
            }

            int altIdTypeId = lookupByName("ZZ_AlternateIDType", "ZZ_AlternateIDType_ID", row.alternateIdType);
            if (altIdTypeId > 0) user.setZZ_AlternateIDType_ID(altIdTypeId);

            user.saveEx();
            int adUserId = user.get_ID();

            // --- 2. Create ZZSdf ---------------------------------------------
            X_ZZSdf sdf = new X_ZZSdf(getCtx(), 0, trxName);
            sdf.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
            sdf.setAD_User_ID(adUserId);
            sdf.setZZ_ID_Passport_No(row.idNo);
            sdf.setZZ_DocStatus(X_ZZSdf.ZZ_DOCSTATUS_Draft);
            sdf.setZZ_DocAction(X_ZZSdf.ZZ_DOCACTION_Submit);

            if (!trim(row.gender).isEmpty())           sdf.setZZGender(row.gender.trim().substring(0, 1).toUpperCase());
            String equityCode = mapEquity(row.populationGroup);
            if (!equityCode.isEmpty())                 sdf.setZZEquity(equityCode);
            if (!trim(row.initials).isEmpty())         sdf.setZZInitials(row.initials.trim());
            if (!trim(row.currentOccupation).isEmpty())sdf.setZZCurrentOccupation(row.currentOccupation.trim());
            if (row.yearsInOccupation > 0)             sdf.setZZYearsInOccupation(row.yearsInOccupation);
            if (!trim(row.highestEducation).isEmpty()) sdf.setZZHighestEducationDesc(row.highestEducation.trim());

            // Parse numeric experience from strings like "18years" or "29"
            int expYears = parseDigits(row.experience);
            if (expYears > 0) sdf.setZZExperience(expYears);

            // List-of-value foreign keys
            int highEduId  = lookupByName("ZZ_LI_HighestEducation",      "ZZ_LI_HighestEducation_ID",      row.highestEducation);
            int homeLangId = lookupByName("ZZ_LI_HomeLanguage",           "ZZ_LI_HomeLanguage_ID",           row.homeLanguage);
            int citizenId  = lookupByName("ZZ_LI_CitizenResidentialStatus","ZZ_LI_CitizenResidentialStatus_ID", row.citizenStatus);
            int socioId    = lookupByName("ZZ_LI_SocioEconomicStatus",    "ZZ_LI_SocioEconomicStatus_ID",    row.socioEconomicStatus);
            int natId      = lookupByName("ZZ_Nationality",               "ZZ_Nationality_ID",               row.nationality);
            int disabilityId = lookupByName("ZZ_No_Yes_Ref",              "ZZ_No_Yes_Ref_ID",                row.disability);

            if (highEduId   > 0) sdf.setZZ_LI_HighestEducation_ID(highEduId);
            if (homeLangId  > 0) sdf.setZZ_LI_HomeLanguage_ID(homeLangId);
            if (citizenId   > 0) sdf.setZZ_LI_CitizenResidentialStatus_ID(citizenId);
            if (socioId     > 0) sdf.setZZ_LI_SocioEconomicStatus_ID(socioId);
            if (natId       > 0) sdf.setZZ_Nationality_ID(natId);
            if (disabilityId> 0) sdf.setZZ_LI_Disability_ID(disabilityId);

            sdf.saveEx();
            int sdfId = sdf.get_ID();

            trx.commit(true);
            sdfCreated++;
            deferredLog.add("Created ZZSdf_ID=" + sdfId + " AD_User_ID=" + adUserId
                    + " for ID=" + row.idNo + " (" + fullName + ")");
            return new int[]{ sdfId, adUserId };

        } catch (Exception e) {
            trx.rollback();
            deferredLog.add("ERROR creating ZZSdf for ID=" + row.idNo + ": " + e.getMessage());
            return null;
        } finally {
            trx.close();
        }
    }

    // =========================================================================
    //  Other lookup helpers
    // =========================================================================

    private int findBPartner(String sdlNumber) {
        return DB.getSQLValueEx(null,
                "SELECT c_bpartner_id FROM c_bpartner WHERE value = ? LIMIT 1",
                sdlNumber);
    }

    /**
     * Finds ZZSdfOrganisation for the given BP and SDF.
     * First tries an exact match (BP + SdfId already set).
     * Falls back to a row with the same BP but no SdfId, and links it.
     * Returns -1 if nothing is found.
     */
    private int findOrLinkOrg(int bpId, int sdfId, String sdlNumber) {
        int orgId = DB.getSQLValueEx(null,
                "SELECT zzsdforganisation_id FROM zzsdforganisation " +
                "WHERE c_bpartner_id = ? AND zzsdf_id = ? LIMIT 1",
                bpId, sdfId);
        if (orgId > 0) return orgId;

        orgId = DB.getSQLValueEx(null,
                "SELECT zzsdforganisation_id FROM zzsdforganisation " +
                "WHERE c_bpartner_id = ? AND (zzsdf_id IS NULL OR zzsdf_id = 1000002) LIMIT 1",
                bpId);
        if (orgId <= 0) return -1;

        linkSdf(orgId, sdfId, sdlNumber);
        return orgId;
    }

    private void linkSdf(int orgId, int sdfId, String sdlNumber) {
        String trxName = Trx.createTrxName("SGSdfLink");
        Trx trx = Trx.get(trxName, true);
        try {
            MZZSdfOrganisation org = new MZZSdfOrganisation(getCtx(), orgId, trxName);
            org.setZZSdf_ID(sdfId);
            org.saveEx();
            trx.commit(true);
            deferredLog.add("Linked ZZSdf_ID=" + sdfId + " to ZZSdfOrganisation_ID=" + orgId
                    + " (SDL=" + sdlNumber + ")");
        } catch (Exception e) {
            trx.rollback();
            deferredLog.add("WARN: Failed to link ZZSdf_ID=" + sdfId + " to org=" + orgId
                    + " (SDL=" + sdlNumber + "): " + e.getMessage());
        } finally {
            trx.close();
        }
    }

    /**
     * Case-insensitive name lookup against a list-of-value table.
     * Returns 0 (not found / blank input) rather than throwing.
     */
    private int lookupByName(String tableName, String idColName, String name) {
        if (name == null || name.trim().isEmpty()) return 0;
        try {
            int id = DB.getSQLValueEx(null,
                    "SELECT " + idColName + " FROM " + tableName
                    + " WHERE LOWER(name) = LOWER(?) LIMIT 1",
                    name.trim());
            return id > 0 ? id : 0;
        } catch (Exception e) {
            return 0; // table may not exist or name may not match — silently ignore
        }
    }

    // =========================================================================
    //  Attachment helpers
    // =========================================================================

    /**
     * Tries to attach one file to the given record.
     *
     * @return ATTACH_LOADED    – file was attached successfully
     *         ATTACH_DUPLICATE – a same-named entry already exists on this record
     *         ATTACH_SKIPPED   – zero-byte file or unexpected error
     */
    private int attachFile(int tableId, int recordId, File file, String context) {
        if (file.length() == 0) {
            deferredLog.add("Zero-byte file skipped: " + file.getName() + " [" + context + "]");
            return ATTACH_SKIPPED;
        }
        String trxName = Trx.createTrxName("SGSdfAtt");
        Trx trx = Trx.get(trxName, true);
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            MAttachment att = new MAttachment(getCtx(), tableId, recordId, null, trxName);

            // Duplicate check — skip if a same-named entry is already stored
            for (int i = 0; i < att.getEntryCount(); i++) {
                if (file.getName().equals(att.getEntry(i).getName())) {
                    deferredLog.add("Already attached: " + file.getName() + " [" + context + "]");
                    trx.rollback();
                    return ATTACH_DUPLICATE;
                }
            }

            att.addEntry(file.getName(), data);
            att.saveEx();
            trx.commit(true);
            return ATTACH_LOADED;
        } catch (Exception e) {
            trx.rollback();
            deferredLog.add("ERROR attaching " + file.getName() + " [" + context + "]: " + e.getMessage());
            return ATTACH_SKIPPED;
        } finally {
            trx.close();
        }
    }

    private void clearAttachment(int tableId, int recordId) {
        try {
            DB.executeUpdateEx(
                "DELETE FROM ad_attachment WHERE ad_table_id = ? AND record_id = ?",
                new Object[]{ tableId, recordId }, null);
        } catch (Exception e) {
            deferredLog.add("WARN: Could not clear attachment for table=" + tableId
                    + " record=" + recordId + ": " + e.getMessage());
        }
    }

    // =========================================================================
    //  Small utilities
    // =========================================================================

    /** Null-safe trim returning empty string instead of null. */
    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Maps a PopulationGroup display value to the ZZEquity list code.
     * Matching is case-insensitive and substring-based to handle values
     * like "Black: African" → "Afr".
     */
    private static String mapEquity(String populationGroup) {
        if (populationGroup == null || populationGroup.trim().isEmpty()) return "";
        String s = populationGroup.trim().toLowerCase();
        if (s.contains("african")) return X_ZZSdf.ZZEQUITY_African;   // "Afr"
        if (s.contains("coloured")) return X_ZZSdf.ZZEQUITY_Coloured; // "Col"
        if (s.contains("indian"))  return X_ZZSdf.ZZEQUITY_Indian;    // "Ind"
        if (s.contains("white"))   return X_ZZSdf.ZZEQUITY_White;     // "Wh"
        return "";
    }

    /**
     * Extracts the first run of digits from a string such as "18years" or "29".
     * Returns 0 if no digits are present.
     */
    private static int parseDigits(String s) {
        if (s == null || s.isEmpty()) return 0;
        Matcher m = DIGITS.matcher(s);
        return m.find() ? Integer.parseInt(m.group()) : 0;
    }

    /** Safe cell read — returns "" when the column header is absent or cell is null. */
    private static String cell(Row row, Map<String, Integer> colIdx, String header, DataFormatter fmt) {
        Integer idx = colIdx.get(header);
        if (idx == null) return "";
        Cell c = row.getCell(idx);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    // =========================================================================
    //  File system helpers
    // =========================================================================

    private static File[] dirs(File parent) {
        File[] d = parent.listFiles(File::isDirectory);
        return d != null ? d : new File[0];
    }

    private static File[] files(File parent) {
        File[] f = parent.listFiles(File::isFile);
        return f != null ? f : new File[0];
    }
}
