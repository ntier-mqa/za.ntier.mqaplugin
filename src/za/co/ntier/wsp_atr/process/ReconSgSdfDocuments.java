package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.MAttachment;
import org.compiere.model.MTable;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.co.ntier.api.model.X_ZZSdf;
import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Reconciles ZZSdf records in the database against the SDF Excel spreadsheet
 * found in BASE_DIR (or its parent), and writes a multi-tab Excel report.
 *
 * Output tabs:
 *   1. Summary         — headline counts and statistics
 *   2. In Excel Only   — rows in the spreadsheet with no matching ZZSdf in DB
 *   3. In DB Only      — ZZSdf records whose ID number has no matching Excel row
 *   4. Field Diffs     — matched records where Name / Gender / Equity differ
 *   5. Org Links       — all SDFs with their org-link counts and SDL numbers
 *   6. No Org Links    — SDFs that have zero org links
 *   7. SDR Attachments — per SDR_ document table: rows with a saved file name vs. how many
 *                        have an attachment loaded by LoadSDRDocumentAttachments (headline
 *                        totals are also on the Summary tab)
 *
 * The output is attached to this process run's own Process Audit record as
 * SDF_Recon_<yyyyMMdd_HHmmss>.xlsx - download it from the Attachment icon there.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ReconSgSdfDocuments")
public class ReconSgSdfDocuments extends SvrProcess {

	private static final String BASE_DIR    = "/home/ntier/SG_wsp_120626/MQAR008349_SDFs";
  //  private static final String BASE_DIR   = "/tmp/SG_Data_070526/MQAR008388_SDFs";
    private static final int    TABLE_SDF  = X_ZZSdf.Table_ID;
    private static final int    TABLE_ORG  = X_ZZSdfOrganisation.Table_ID;

    // =========================================================================
    //  Inner data classes
    // =========================================================================

    private static class ExcelRow {
        String idNo, title, firstName, middleName, surname, initials;
        String gender, populationGroup, dateOfBirth, alternateIdType;
        String email, phone, mobile;
        String disability, homeLanguage, nationality;
        String citizenStatus, socioEconomicStatus, highestEducation;
        String currentOccupation, yearsInOccupation, experience;
    }

    private static class DbRow {
        int    sdfId, adUserId;
        String idNo, dbName, gender, equity, docStatus;
        int    orgCount;
        boolean hasIdCopy;
        List<String> sdlNumbers = new ArrayList<>();
    }

    private static class FieldDiff {
        String idNo, field, excelValue, dbValue;
        int    sdfId;
    }

    /** Per SDR_ document table: how many rows carry a file name vs. have an attachment loaded. */
    private static class SdrAttachRow {
        String tableName;
        long   totalRows, withFileName, attached;
    }

    // =========================================================================
    //  SvrProcess lifecycle
    // =========================================================================

    @Override
    protected void prepare() { /* no process parameters */ }

    @Override
    protected String doIt() throws Exception {
        File base = new File(BASE_DIR);
        if (!base.isDirectory())
            throw new IllegalStateException("Base directory not found: " + BASE_DIR);

        // 1. Load Excel reference data
        Map<String, ExcelRow> excelMap = loadExcel(base);
        addLog("Excel rows loaded: " + excelMap.size());

        // 2. Load DB records (ZZSdf + AD_User + org links + attachment flags)
        Map<String, DbRow> dbMap = loadDb();
        addLog("DB ZZSdf records: " + dbMap.size());

        // 3. Build recon sets
        Set<String> inBoth    = new LinkedHashSet<>(excelMap.keySet());
        inBoth.retainAll(dbMap.keySet());

        Set<String> excelOnly = new LinkedHashSet<>(excelMap.keySet());
        excelOnly.removeAll(dbMap.keySet());

        Set<String> dbOnly    = new LinkedHashSet<>(dbMap.keySet());
        dbOnly.removeAll(excelMap.keySet());

        // 4. Find field differences for matched records
        List<FieldDiff> diffs = buildDiffs(inBoth, excelMap, dbMap);

        // 5. Attachments loaded by LoadSDRDocumentAttachments, per SDR_ document table
        List<SdrAttachRow> sdrAttach = loadSdrAttachmentStats();
        long sdrWithName = sdrAttach.stream().mapToLong(a -> a.withFileName).sum();
        long sdrAttached = sdrAttach.stream().mapToLong(a -> a.attached).sum();
        addLog("SDR document tables: " + sdrAttach.size() + ", rows with file name: " + sdrWithName
                + ", attachment loaded: " + sdrAttached);

        // 6. Write output workbook
        String outPath = writeExcel(base, excelMap, dbMap, inBoth, excelOnly, dbOnly, diffs, sdrAttach);
        addLog("Report attached to this Process Audit record as: " + outPath);

        return String.format(
            "Excel=%d  DB=%d  Matched=%d  ExcelOnly=%d  DBOnly=%d  FieldDiffs=%d  "
            + "SDRAttachments=%d/%d  Output: %s",
            excelMap.size(), dbMap.size(), inBoth.size(),
            excelOnly.size(), dbOnly.size(), diffs.size(), sdrAttached, sdrWithName, outPath);
    }

    // =========================================================================
    //  Data loading — Excel
    // =========================================================================

    private Map<String, ExcelRow> loadExcel(File base) throws Exception {
        File xlsx = findExcelFile(base);
        if (xlsx == null)
            throw new IllegalStateException("No 'SDF list*.xlsx' found in " + base + " or its parent");

        Map<String, ExcelRow> map = new LinkedHashMap<>();
        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(xlsx, null, true /* read-only */)) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return map;

            Map<String, Integer> colIdx = new HashMap<>();
            for (Cell c : headerRow) {
                String n = fmt.formatCellValue(c).trim();
                if (!n.isEmpty()) colIdx.put(n, c.getColumnIndex());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String idNo = col(row, colIdx, "IDNo", fmt).trim();
                if (idNo.isEmpty()) continue;

                ExcelRow er      = new ExcelRow();
                er.idNo           = idNo;
                er.title          = col(row, colIdx, "Title",                     fmt);
                er.firstName      = col(row, colIdx, "FirstName",                  fmt);
                er.middleName     = col(row, colIdx, "MiddleName",                 fmt);
                er.surname        = col(row, colIdx, "Surname",                    fmt);
                er.initials       = col(row, colIdx, "Initials",                   fmt);
                er.alternateIdType= col(row, colIdx, "AlternateIDType",            fmt);
                er.dateOfBirth    = col(row, colIdx, "Date of Birth",              fmt);
                er.gender         = col(row, colIdx, "Gender",                     fmt);
                er.populationGroup= col(row, colIdx, "PopulationGroup",            fmt);
                er.email          = col(row, colIdx, "EMail",                      fmt);
                er.phone          = col(row, colIdx, "TelephoneNumber",            fmt);
                er.mobile         = col(row, colIdx, "CellPhoneNumber",            fmt);
                er.disability     = col(row, colIdx, "Disability",                 fmt);
                er.homeLanguage   = col(row, colIdx, "HomeLanguage",               fmt);
                er.nationality    = col(row, colIdx, "Nationality",                fmt);
                er.citizenStatus  = col(row, colIdx, "CitizenResidentialStatus",   fmt);
                er.socioEconomicStatus = col(row, colIdx, "SocioEconomicStatus",   fmt);
                er.highestEducation   = col(row, colIdx, "HighestEducation",       fmt);
                er.currentOccupation  = col(row, colIdx, "CurrentOccupation",      fmt);
                er.yearsInOccupation  = col(row, colIdx, "YearsInOccupation",      fmt);
                er.experience         = col(row, colIdx, "Experience",             fmt);
                map.put(idNo, er);
            }
        }
        return map;
    }

    // =========================================================================
    //  Data loading — Database
    // =========================================================================

    private Map<String, DbRow> loadDb() throws Exception {
        int clientId = Env.getAD_Client_ID(getCtx());
        // keyed by ZZSdf_ID for fast org/attachment lookup, then re-keyed by IDNo
        Map<Integer, DbRow> byId = new LinkedHashMap<>();
        Map<String,  DbRow> map  = new LinkedHashMap<>();

        // --- ZZSdf + AD_User ---------------------------------------------------
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                "SELECT s.zzsdf_id, s.ad_user_id, s.zz_docstatus, s.zzgender, s.zzequity, " +
                "       u.name, u.zz_id_passport_no " +
                "FROM zzsdf s " +
                "JOIN ad_user u ON u.ad_user_id = s.ad_user_id " +
                "WHERE s.ad_client_id = ? " +
                "ORDER BY s.zzsdf_id", null);
            pst.setInt(1, clientId);
            rs = pst.executeQuery();
            while (rs.next()) {
                DbRow dr    = new DbRow();
                dr.sdfId    = rs.getInt("zzsdf_id");
                dr.adUserId = rs.getInt("ad_user_id");
                dr.docStatus = nvl(rs.getString("zz_docstatus"));
                dr.gender    = nvl(rs.getString("zzgender"));
                dr.equity    = nvl(rs.getString("zzequity"));
                dr.dbName    = nvl(rs.getString("name"));
                dr.idNo      = nvl(rs.getString("zz_id_passport_no"));
                byId.put(dr.sdfId, dr);
                if (!dr.idNo.isEmpty())
                    map.put(dr.idNo, dr);
            }
        } finally {
            DB.close(rs, pst);
        }

        // --- Org links ---------------------------------------------------------
        try {
            pst = DB.prepareStatement(
                "SELECT o.zzsdf_id, bp.value AS sdl " +
                "FROM zzsdforganisation o " +
                "JOIN c_bpartner bp ON bp.c_bpartner_id = o.c_bpartner_id " +
                "WHERE o.zzsdf_id IS NOT NULL AND o.ad_client_id = ? " +
                "ORDER BY o.zzsdf_id", null);
            pst.setInt(1, clientId);
            rs = pst.executeQuery();
            while (rs.next()) {
                DbRow dr = byId.get(rs.getInt("zzsdf_id"));
                if (dr != null) {
                    dr.sdlNumbers.add(nvl(rs.getString("sdl")));
                    dr.orgCount++;
                }
            }
        } finally {
            DB.close(rs, pst);
        }

        // --- ID Copy attachment presence (at least one entry on the SDF record) -
        try {
            pst = DB.prepareStatement(
                "SELECT a.record_id " +
                "FROM ad_attachment a " +
                "WHERE a.ad_table_id = ? AND a.ad_client_id = ?", null);
            pst.setInt(1, TABLE_SDF);
            pst.setInt(2, clientId);
            rs = pst.executeQuery();
            while (rs.next()) {
                DbRow dr = byId.get(rs.getInt(1));
                if (dr != null) dr.hasIdCopy = true;
            }
        } finally {
            DB.close(rs, pst);
        }

        return map;
    }

    // =========================================================================
    //  Data loading — SDR_ document attachments (see LoadSDRDocumentAttachments)
    // =========================================================================

    private static final String SDR_SAVED_FILENAME_COL = "sdr_savedfilename";

    /**
     * Same table discovery as LoadSDRDocumentAttachments (every sdr_ table with an
     * sdr_savedfilename column), then per table: total rows, rows with a non-blank file name, and
     * how many of those have an AD_Attachment row on their record. Counts the AD_Attachment row's
     * existence rather than its entries - it is a cheap SQL check and the loader only ever creates
     * the row when it adds an entry.
     */
    private List<SdrAttachRow> loadSdrAttachmentStats() throws Exception {
        List<String> tables = new ArrayList<>();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = DB.prepareStatement(
                "SELECT DISTINCT table_name FROM information_schema.columns " +
                "WHERE table_name LIKE 'sdr\\_%' ESCAPE '\\' AND column_name = ? ORDER BY table_name", null);
            pst.setString(1, SDR_SAVED_FILENAME_COL);
            rs = pst.executeQuery();
            while (rs.next()) tables.add(rs.getString(1));
        } finally {
            DB.close(rs, pst);
        }

        List<SdrAttachRow> result = new ArrayList<>();
        String nonBlank = SDR_SAVED_FILENAME_COL + " IS NOT NULL AND " + SDR_SAVED_FILENAME_COL + " <> ''";
        for (String tableName : tables) {
            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, null);
            String[] keys = table == null ? null : table.getKeyColumns();
            if (keys == null || keys.length != 1) {
                addLog(tableName + ": no AD_Table / single key column - skipped in SDR attachment recon.");
                continue;
            }

            SdrAttachRow ar = new SdrAttachRow();
            ar.tableName = tableName;
            try {
                pst = DB.prepareStatement(
                    "SELECT COUNT(*), " +
                    "SUM(CASE WHEN " + nonBlank + " THEN 1 ELSE 0 END), " +
                    "SUM(CASE WHEN (" + nonBlank + ") AND EXISTS (SELECT 1 FROM ad_attachment a " +
                    "    WHERE a.ad_table_id = ? AND a.record_id = d." + keys[0] + ") THEN 1 ELSE 0 END) " +
                    "FROM " + tableName + " d", null);
                pst.setInt(1, table.getAD_Table_ID());
                rs = pst.executeQuery();
                if (rs.next()) {
                    ar.totalRows    = rs.getLong(1);
                    ar.withFileName = rs.getLong(2);   // SUM over zero rows is NULL -> 0
                    ar.attached     = rs.getLong(3);
                }
            } finally {
                DB.close(rs, pst);
            }
            result.add(ar);
        }
        return result;
    }

    // =========================================================================
    //  Field difference analysis
    // =========================================================================

    private List<FieldDiff> buildDiffs(Set<String> inBoth,
                                        Map<String, ExcelRow> excelMap,
                                        Map<String, DbRow> dbMap) {
        List<FieldDiff> diffs = new ArrayList<>();
        for (String id : inBoth) {
            ExcelRow er = excelMap.get(id);
            DbRow    dr = dbMap.get(id);

            // Name: compare "FirstName Surname" from Excel vs AD_User.Name in DB
            String excelName = (trim(er.firstName) + " " + trim(er.surname)).trim();
            if (!excelName.isEmpty() && !excelName.equalsIgnoreCase(dr.dbName)) {
                diffs.add(newDiff(id, dr.sdfId, "Name", excelName, dr.dbName));
            }

            // Gender: first letter of Excel gender vs DB code
            String excelGender = trim(er.gender).isEmpty() ? ""
                    : er.gender.trim().substring(0, 1).toUpperCase();
            if (!excelGender.isEmpty() && !excelGender.equals(dr.gender)) {
                diffs.add(newDiff(id, dr.sdfId, "Gender",
                        excelGender + " (" + trim(er.gender) + ")", dr.gender));
            }

            // Equity: mapped population group vs DB code
            String excelEquity = mapEquity(er.populationGroup);
            if (!excelEquity.isEmpty() && !excelEquity.equals(dr.equity)) {
                diffs.add(newDiff(id, dr.sdfId, "Equity",
                        excelEquity + " (" + trim(er.populationGroup) + ")", dr.equity));
            }
        }
        return diffs;
    }

    private FieldDiff newDiff(String idNo, int sdfId, String field, String excel, String db) {
        FieldDiff d = new FieldDiff();
        d.idNo = idNo; d.sdfId = sdfId;
        d.field = field; d.excelValue = excel; d.dbValue = db;
        return d;
    }

    // =========================================================================
    //  Excel output — workbook orchestration
    // =========================================================================

    private String writeExcel(File base,
                               Map<String, ExcelRow> excelMap,
                               Map<String, DbRow>   dbMap,
                               Set<String>          inBoth,
                               Set<String>          excelOnly,
                               Set<String>          dbOnly,
                               List<FieldDiff>      diffs,
                               List<SdrAttachRow>   sdrAttach) throws Exception {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);

            writeSummary(wb.createSheet("Summary"), s,
                    excelMap.size(), dbMap.size(), inBoth.size(),
                    excelOnly.size(), dbOnly.size(), diffs.size(), dbMap, sdrAttach);

            writeExcelOnly(wb.createSheet("In Excel Only"), s, excelOnly, excelMap);
            writeDbOnly(wb.createSheet("In DB Only"), s, dbOnly, dbMap);
            writeFieldDiffs(wb.createSheet("Field Differences"), s, diffs, dbMap);
            writeOrgLinks(wb.createSheet("Org Links"), s, dbMap);
            writeNoOrgLinks(wb.createSheet("No Org Links"), s, dbMap, excelMap);
            writeSdrAttachments(wb.createSheet("SDR Attachments"), s, sdrAttach);

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "SDF_Recon_" + ts + ".xlsx";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);

            // Attached to this run's own Process Audit record (same pattern as
            // ExportSDRReconciliationReport) so it can be downloaded from the Attachment icon.
            MAttachment attachment = new MAttachment(getCtx(), I_AD_PInstance.Table_ID,
                    getAD_PInstance_ID(), null, get_TrxName());
            attachment.addEntry(fileName, out.toByteArray());
            attachment.saveEx();
            return fileName;
        }
    }

    // =========================================================================
    //  Tab 1 — Summary
    // =========================================================================

    private void writeSummary(Sheet sheet, Styles s,
                               int excelCnt, int dbCnt, int matchedCnt,
                               int excelOnlyCnt, int dbOnlyCnt, int diffCnt,
                               Map<String, DbRow> dbMap, List<SdrAttachRow> sdrAttach) {

        // Title
        Row titleRow = sheet.createRow(0);
        setHdr(titleRow.createCell(0), "SDF Reconciliation Report", s.title);

        row2(sheet, 1, "Generated:",      new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        row2(sheet, 2, "Base Directory:", BASE_DIR);
        sheet.createRow(3); // spacer

        // Section: record counts
        rowHdr(sheet, 4, "Record Counts", s.sectionHdr);
        metric(sheet, 5,  "Rows in Excel spreadsheet",     excelCnt,    "");
        metric(sheet, 6,  "ZZSdf records in DB",            dbCnt,       "Current client only");
        metric(sheet, 7,  "Matched (ID in both)",           matchedCnt,  "");
        metric(sheet, 8,  "In Excel only — not yet in DB",  excelOnlyCnt,"See 'In Excel Only' tab");
        metric(sheet, 9,  "In DB only — not in Excel",      dbOnlyCnt,   "See 'In DB Only' tab");
        metric(sheet, 10, "Field differences",               diffCnt,     "See 'Field Differences' tab");

        sheet.createRow(11); // spacer

        // Section: attachment & org link stats
        rowHdr(sheet, 12, "Org & Attachment Statistics", s.sectionHdr);

        long withOrgs     = dbMap.values().stream().filter(d -> d.orgCount > 0).count();
        long noOrgs       = dbMap.values().stream().filter(d -> d.orgCount == 0).count();
        long withIdCopy   = dbMap.values().stream().filter(d -> d.hasIdCopy).count();
        long noIdCopy     = (long) dbCnt - withIdCopy;
        long totalLinks   = dbMap.values().stream().mapToLong(d -> d.orgCount).sum();
        long maxLinks     = dbMap.values().stream().mapToLong(d -> d.orgCount).max().orElse(0);

        metric(sheet, 13, "SDFs with >= 1 org link",         (int) withOrgs,   "");
        metric(sheet, 14, "SDFs with 0 org links",            (int) noOrgs,    "See 'No Org Links' tab");
        metric(sheet, 15, "Total org links",                  (int) totalLinks, "");
        metric(sheet, 16, "Max org links for a single SDF",   (int) maxLinks,   "");
        metric(sheet, 17, "SDFs with ID Copy attached",       (int) withIdCopy, "");
        metric(sheet, 18, "SDFs without ID Copy attached",    (int) noIdCopy,   "");

        sheet.createRow(19); // spacer

        // Section: SDR_ document attachments loaded by LoadSDRDocumentAttachments
        long sdrWithName = sdrAttach.stream().mapToLong(a -> a.withFileName).sum();
        long sdrAttached = sdrAttach.stream().mapToLong(a -> a.attached).sum();
        rowHdr(sheet, 20, "SDR Document Attachments (LoadSDRDocumentAttachments)", s.sectionHdr);
        metric(sheet, 21, "SDR document tables checked",             sdrAttach.size(),                  "");
        metric(sheet, 22, "Rows with a saved file name",             (int) sdrWithName,                 "");
        metric(sheet, 23, "...with attachment loaded",               (int) sdrAttached,                 "");
        metric(sheet, 24, "...without attachment",                   (int) (sdrWithName - sdrAttached), "See 'SDR Attachments' tab");

        sheet.setColumnWidth(0, 45 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 38 * 256);

        // Bold the metric column
        for (int r = 5; r <= 18; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Cell lbl = row.getCell(0);
            if (lbl != null) lbl.setCellStyle(s.label);
        }
    }

    private void rowHdr(Sheet sheet, int rowNum, String text, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        Cell c = row.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(style);
    }

    private void row2(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private void metric(Sheet sheet, int rowNum, String label, int count, String notes) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(count);
        if (!notes.isEmpty()) row.createCell(2).setCellValue(notes);
    }

    // =========================================================================
    //  Tab 2 — In Excel Only
    // =========================================================================

    private void writeExcelOnly(Sheet sheet, Styles s,
                                  Set<String> excelOnly, Map<String, ExcelRow> excelMap) {
        String[] hdrs = {"IDNo","Title","FirstName","MiddleName","Surname","Initials",
                "Gender","PopulationGroup","DateOfBirth","AlternateIDType",
                "EMail","TelephoneNumber","CellPhoneNumber",
                "Disability","HomeLanguage","Nationality","CitizenStatus",
                "SocioEconomicStatus","HighestEducation","CurrentOccupation",
                "YearsInOccupation","Experience"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        int r = 1;
        for (String id : excelOnly) {
            ExcelRow er = excelMap.get(id);
            Row row = sheet.createRow(r++);
            int c = 0;
            sv(row, c++, er.idNo,                s.warn);
            sv(row, c++, er.title);
            sv(row, c++, er.firstName);
            sv(row, c++, er.middleName);
            sv(row, c++, er.surname);
            sv(row, c++, er.initials);
            sv(row, c++, er.gender);
            sv(row, c++, er.populationGroup);
            sv(row, c++, er.dateOfBirth);
            sv(row, c++, er.alternateIdType);
            sv(row, c++, er.email);
            sv(row, c++, er.phone);
            sv(row, c++, er.mobile);
            sv(row, c++, er.disability);
            sv(row, c++, er.homeLanguage);
            sv(row, c++, er.nationality);
            sv(row, c++, er.citizenStatus);
            sv(row, c++, er.socioEconomicStatus);
            sv(row, c++, er.highestEducation);
            sv(row, c++, er.currentOccupation);
            sv(row, c++, er.yearsInOccupation);
            sv(row, c++, er.experience);
        }
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Tab 3 — In DB Only
    // =========================================================================

    private void writeDbOnly(Sheet sheet, Styles s,
                               Set<String> dbOnly, Map<String, DbRow> dbMap) {
        String[] hdrs = {"IDNo","ZZSdf_ID","AD_User_ID","Name","Gender","Equity",
                "DocStatus","OrgLinkCount","HasIDCopy"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        int r = 1;
        for (String id : dbOnly) {
            DbRow dr = dbMap.get(id);
            Row row = sheet.createRow(r++);
            int c = 0;
            sv(row, c++, dr.idNo, s.warn);
            iv(row, c++, dr.sdfId);
            iv(row, c++, dr.adUserId);
            sv(row, c++, dr.dbName);
            sv(row, c++, dr.gender);
            sv(row, c++, dr.equity);
            sv(row, c++, dr.docStatus);
            iv(row, c++, dr.orgCount);
            sv(row, c++, dr.hasIdCopy ? "Yes" : "No");
        }
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Tab 4 — Field Differences
    // =========================================================================

    private void writeFieldDiffs(Sheet sheet, Styles s,
                                   List<FieldDiff> diffs,
                                   Map<String, DbRow> dbMap) {
        String[] hdrs = {"IDNo","ZZSdf_ID","Name","Field","Excel Value","DB Value"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        int r = 1;
        for (FieldDiff d : diffs) {
            DbRow dr = dbMap.get(d.idNo);
            Row row = sheet.createRow(r++);
            int c = 0;
            sv(row, c++, d.idNo);
            iv(row, c++, d.sdfId);
            sv(row, c++, dr != null ? dr.dbName : "");
            sv(row, c++, d.field, s.warn);
            sv(row, c++, d.excelValue);
            sv(row, c++, d.dbValue);
        }
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Tab 5 — Org Links
    // =========================================================================

    private void writeOrgLinks(Sheet sheet, Styles s, Map<String, DbRow> dbMap) {
        String[] hdrs = {"ZZSdf_ID","IDNo","Name","OrgLinkCount","SDLNumbers"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        List<DbRow> rows = new ArrayList<>(dbMap.values());
        rows.sort(Comparator.comparingInt(dr -> dr.sdfId));

        int r = 1;
        for (DbRow dr : rows) {
            Row row = sheet.createRow(r++);
            int c = 0;
            iv(row, c++, dr.sdfId);
            sv(row, c++, dr.idNo);
            sv(row, c++, dr.dbName);
            iv(row, c++, dr.orgCount);
            sv(row, c++, String.join(", ", dr.sdlNumbers));
        }
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Tab 6 — No Org Links
    // =========================================================================

    private void writeNoOrgLinks(Sheet sheet, Styles s,
                                   Map<String, DbRow> dbMap,
                                   Map<String, ExcelRow> excelMap) {
        String[] hdrs = {"ZZSdf_ID","IDNo","Name","InExcel","HasIDCopy"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        List<DbRow> rows = new ArrayList<>();
        for (DbRow dr : dbMap.values())
            if (dr.orgCount == 0) rows.add(dr);
        rows.sort(Comparator.comparingInt(dr -> dr.sdfId));

        int r = 1;
        for (DbRow dr : rows) {
            Row row = sheet.createRow(r++);
            int c = 0;
            iv(row, c++, dr.sdfId);
            sv(row, c++, dr.idNo, s.warn);
            sv(row, c++, dr.dbName);
            sv(row, c++, excelMap.containsKey(dr.idNo) ? "Yes" : "No");
            sv(row, c++, dr.hasIdCopy ? "Yes" : "No");
        }
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Tab 7 — SDR Attachments
    // =========================================================================

    private void writeSdrAttachments(Sheet sheet, Styles s, List<SdrAttachRow> rows) {
        String[] hdrs = {"Table","Total Rows","Rows With File Name","Attachment Loaded","No Attachment"};
        writeHeaderRow(sheet, s, hdrs);
        sheet.createFreezePane(0, 1);

        long totRows = 0, totName = 0, totAttached = 0;
        int r = 1;
        for (SdrAttachRow ar : rows) {
            long notAttached = ar.withFileName - ar.attached;
            Row row = sheet.createRow(r++);
            int c = 0;
            sv(row, c++, ar.tableName, notAttached > 0 ? s.warn : s.label);
            lv(row, c++, ar.totalRows);
            lv(row, c++, ar.withFileName);
            lv(row, c++, ar.attached);
            lv(row, c++, notAttached);
            totRows += ar.totalRows;
            totName += ar.withFileName;
            totAttached += ar.attached;
        }

        Row total = sheet.createRow(r);
        int c = 0;
        sv(total, c++, "TOTAL", s.hdr);
        lv(total, c++, totRows);
        lv(total, c++, totName);
        lv(total, c++, totAttached);
        lv(total, c++, totName - totAttached);
        autoSize(sheet, hdrs.length);
    }

    // =========================================================================
    //  Styles holder
    // =========================================================================

    private static class Styles {
        final CellStyle hdr, title, sectionHdr, label, warn;

        Styles(Workbook wb) {
            // Column header: bold, light-grey background
            hdr = wb.createCellStyle();
            Font hdrFont = wb.createFont();
            hdrFont.setBold(true);
            hdrFont.setFontName("Arial");
            hdrFont.setFontHeightInPoints((short) 10);
            hdr.setFont(hdrFont);
            hdr.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            hdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            hdr.setBorderBottom(BorderStyle.THIN);

            // Sheet title: larger bold
            title = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontName("Arial");
            titleFont.setFontHeightInPoints((short) 14);
            title.setFont(titleFont);

            // Section subheading: bold, blue
            sectionHdr = wb.createCellStyle();
            Font secFont = wb.createFont();
            secFont.setBold(true);
            secFont.setFontName("Arial");
            secFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            sectionHdr.setFont(secFont);

            // Metric label: slightly bold
            label = wb.createCellStyle();
            Font labelFont = wb.createFont();
            labelFont.setFontName("Arial");
            label.setFont(labelFont);

            // Warning: dark-red text for cells that flag an issue
            warn = wb.createCellStyle();
            Font warnFont = wb.createFont();
            warnFont.setColor(IndexedColors.DARK_RED.getIndex());
            warnFont.setFontName("Arial");
            warn.setFont(warnFont);
        }
    }

    // =========================================================================
    //  POI helpers
    // =========================================================================

    private void writeHeaderRow(Sheet sheet, Styles s, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(s.hdr);
        }
    }

    private void setHdr(Cell cell, String value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /** String cell, default style. */
    private void sv(Row row, int col, String value) {
        row.createCell(col).setCellValue(value == null ? "" : value);
    }

    /** String cell with explicit style (e.g. warn). */
    private void sv(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    /** Integer / numeric cell. */
    private void iv(Row row, int col, int value) {
        row.createCell(col).setCellValue(value);
    }

    /** Long numeric cell (row counts). */
    private void lv(Row row, int col, long value) {
        row.createCell(col).setCellValue(value);
    }

    private void autoSize(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int w = sheet.getColumnWidth(i);
            if (w > 60 * 256) sheet.setColumnWidth(i, 60 * 256); // cap at ~60 chars
            if (w < 8  * 256) sheet.setColumnWidth(i,  8 * 256); // minimum ~8 chars
        }
    }

    // =========================================================================
    //  Shared utilities (mirrors of ImportSgSdfDocuments)
    // =========================================================================

    private File findExcelFile(File dir) {
        FileFilter f = file -> file.isFile()
                && file.getName().startsWith("SDF list")
                && file.getName().endsWith(".xlsx");
        File[] hits = dir.listFiles(f);
        if (hits != null && hits.length > 0) return hits[0];
        File parent = dir.getParentFile();
        if (parent != null) {
            hits = parent.listFiles(f);
            if (hits != null && hits.length > 0) return hits[0];
        }
        return null;
    }

    private static String col(Row row, Map<String, Integer> idx, String header, DataFormatter fmt) {
        Integer i = idx.get(header);
        if (i == null) return "";
        Cell c = row.getCell(i);
        return c == null ? "" : fmt.formatCellValue(c).trim();
    }

    private static String mapEquity(String pg) {
        if (pg == null || pg.trim().isEmpty()) return "";
        String s = pg.trim().toLowerCase();
        if (s.contains("african"))  return X_ZZSdf.ZZEQUITY_African;
        if (s.contains("coloured")) return X_ZZSdf.ZZEQUITY_Coloured;
        if (s.contains("indian"))   return X_ZZSdf.ZZEQUITY_Indian;
        if (s.contains("white"))    return X_ZZSdf.ZZEQUITY_White;
        return "";
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String nvl(String s)  { return s == null ? "" : s; }
}
