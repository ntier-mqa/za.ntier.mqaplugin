package za.co.ntier.wsp_atr.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.api.model.MUser_New;
import za.co.ntier.api.model.X_ZZSdf;
import za.co.ntier.api.model.X_ZZSdfOrganisation;

/**
 * Second pass over the "Updated WSP Approval List" spreadsheet (same file used by
 * {@link ImportWspApprovalList}). For each row, this process:
 *
 * <ol>
 *   <li>Resolves the SDL number to a ZZSdfOrganisation and its 2026 ZZ_WSP_ATR_Submitted
 *       record (rows without a 2026 submission are skipped).</li>
 *   <li>If the org's currently-linked ZZSdf is blank/0 or equals the "default" ZZSdf
 *       (the one belonging to whoever runs this process — same lookup as
 *       {@link ImportWspAtrMigrationFile#checkPrerequisites()}), finds-or-creates a real
 *       AD_User (by SDF email) and ZZSdf, then relinks ZZSdfOrganisation.ZZSdf_ID to it.</li>
 *   <li>On the resulting (real) ZZSdf, fills AD_User.ZZFirstName / ZZSurname / Name and
 *       ZZSdf.ZZFirstName / ZZSurname from the sheet's SDF Name/Surname columns — only
 *       where those fields are currently null/blank. (ZZSdf.ZZFirstName/ZZMiddleName/
 *       ZZSurname exist on the table but are not exposed by the generated X_ZZSdf model,
 *       so they are written via raw SQL rather than typed setters. There is no middle-name
 *       column in the source sheet, so ZZMiddleName is never touched.)</li>
 * </ol>
 *
 * Before any changes are made, AD_User, ZZSdf and ZZSdfOrganisation are snapshotted with
 * {@code CREATE TABLE t_<table>_<ddMMyyHHmmss> AS SELECT * FROM <table>}.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.UpdateWspSdfFromApprovalList")
public class UpdateWspSdfFromApprovalList extends SvrProcess {

    // Column indices in Sheet1 (0-based) — same layout as ImportWspApprovalList
    private static final int COL_ORG_NAME    = 0;
    private static final int COL_SDL         = 1;
    private static final int COL_SDF_NAME    = 19;
    private static final int COL_SDF_SURNAME = 20;
    private static final int COL_SDF_EMAIL   = 21;
    private static final int COL_SDF_PHONE   = 22;

    private static final int BP_GROUP_UNKNOWN = 1000018;
    private static final String[] BACKUP_TABLES = { "AD_User", "ZZSdf", "ZZSdfOrganisation" };

    private String filePath;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter p : getParameter()) {
            String name = p.getParameterName();
            if ("FilePath".equalsIgnoreCase(name) || "FileName".equalsIgnoreCase(name)) {
                filePath = p.getParameterAsString();
            } else {
                org.compiere.model.MProcessPara.validateUnknownParameter(
                        getProcessInfo().getAD_Process_ID(), p);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (Util.isEmpty(filePath, true)) {
            throw new AdempiereException("FilePath parameter is required.");
        }
        File file = new File(filePath.trim());
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("File not found: " + filePath);
        }

        int finYearId = DB.getSQLValueEx(get_TrxName(),
                "SELECT C_Year_ID FROM C_Year WHERE FiscalYear = '2026' AND AD_Client_ID = ?",
                Env.getAD_Client_ID(getCtx()));
        if (finYearId <= 0) {
            throw new AdempiereException("Financial year 2026 not found in C_Year.");
        }

        int loggedOnUserId = Env.getAD_User_ID(getCtx());
        int defaultSdfId = DB.getSQLValue(get_TrxName(),
                "SELECT ZZSDF_ID FROM ZZSdf WHERE AD_User_ID = ? AND IsActive = 'Y'"
                + " ORDER BY ZZSDF_ID FETCH FIRST 1 ROWS ONLY",
                loggedOnUserId);
        if (defaultSdfId <= 0) {
            throw new AdempiereException(
                    "Setup error: no active ZZSdf record found for the logged-on user (AD_User_ID="
                    + loggedOnUserId + "). This user's ZZSdf is treated as the 'default' placeholder"
                    + " that org links get relinked away from — please ensure your user account is"
                    + " linked to an SDF record before running this process.");
        }

        String timestamp = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
        StringBuilder backupSummary = new StringBuilder();
        for (String table : BACKUP_TABLES) {
            String backupName = "t_" + table + "_" + timestamp;
            DB.executeUpdateEx(
                    "CREATE TABLE " + backupName + " AS SELECT * FROM " + table,
                    get_TrxName());
            addLog("Backed up " + table + " to " + backupName);
            if (backupSummary.length() > 0) backupSummary.append(", ");
            backupSummary.append(backupName);
        }

        int updated = 0;
        int skipped = 0;
        int errors  = 0;

        String logTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File logFile = new File("/tmp/wsp-sdf-update-" + logTimestamp + ".csv");

        try (Workbook wb = WorkbookFactory.create(file);
             PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(logFile)))) {

            log.println("SDL Number,Organisation Name,Status,Message");

            Sheet sheet = wb.getSheet("Sheet1");
            if (sheet == null) {
                throw new AdempiereException("Sheet 'Sheet1' not found in workbook.");
            }

            DataFormatter fmt = new DataFormatter();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String sdl     = cellText(row, COL_SDL, fmt).trim();
                String orgName = cellText(row, COL_ORG_NAME, fmt).trim();

                if (sdl.isEmpty()) continue;

                try {
                    String result = processRow(row, fmt, sdl, finYearId, defaultSdfId);
                    if (result.startsWith("SKIPPED")) {
                        skipped++;
                        log.println(csvLine(sdl, orgName, "SKIPPED", result));
                    } else {
                        updated++;
                        log.println(csvLine(sdl, orgName, "UPDATED", result));
                    }
                    addLog(result);
                } catch (Exception e) {
                    errors++;
                    String msg = "ERROR row " + (r + 1) + " SDL=" + sdl + ": " + e.getMessage();
                    addLog(msg);
                    log.println(csvLine(sdl, orgName, "ERROR", e.getMessage()));
                }
            }
        }

        if (processUI != null && logFile.exists()) {
            processUI.download(logFile);
        }

        return "Processed: " + (updated + skipped + errors)
                + " rows — " + updated + " updated, " + skipped + " skipped, " + errors + " errors."
                + " Backups: " + backupSummary
                + " Log: " + logFile.getAbsolutePath();
    }

    // -------------------------------------------------------------------------

    private String processRow(Row row, DataFormatter fmt, String sdl, int finYearId, int defaultSdfId) {

        int orgId = lookupOrgIdBySdl(sdl);
        if (orgId <= 0) {
            return "SKIPPED: SDL=" + sdl + " — no ZZSdfOrganisation found";
        }

        int submittedId = DB.getSQLValueEx(get_TrxName(),
                "SELECT ZZ_WSP_ATR_Submitted_ID FROM ZZ_WSP_ATR_Submitted"
                + " WHERE ZZSDFOrganisation_ID = ? AND ZZ_FinYear_ID = ?"
                + " FETCH FIRST 1 ROWS ONLY",
                orgId, finYearId);
        if (submittedId <= 0) {
            return "SKIPPED: SDL=" + sdl + " — no 2026 submission found for org (ID=" + orgId + ")";
        }

        String sdfName    = cellText(row, COL_SDF_NAME, fmt).trim();
        String sdfSurname = cellText(row, COL_SDF_SURNAME, fmt).trim();
        String sdfEmail   = cellText(row, COL_SDF_EMAIL, fmt).trim();
        String sdfPhone   = cellText(row, COL_SDF_PHONE, fmt).trim();

        StringBuilder detail = new StringBuilder();

        X_ZZSdfOrganisation org = new X_ZZSdfOrganisation(getCtx(), orgId, get_TrxName());
        int currentSdfId = org.getZZSdf_ID();
        int finalSdfId = currentSdfId;

        boolean needsRealSdf = currentSdfId <= 0 || currentSdfId == defaultSdfId;
        if (needsRealSdf) {
            if (Util.isEmpty(sdfEmail, true)) {
                return "SKIPPED: SDL=" + sdl + " — org's SDF link is blank/default (ID=" + currentSdfId
                        + ") but sheet has no SDF email to resolve a real SDF";
            }

            String fullName = (sdfName + " " + sdfSurname).trim();
            if (fullName.isEmpty()) fullName = sdl;

            int adUserId = findUserByEmail(sdfEmail);
            if (adUserId <= 0) {
                adUserId = createUser(fullName, sdfName, sdfSurname, sdfEmail, sdfPhone);
                detail.append("+user");
            }

            int newSdfId = findSdfByUser(adUserId);
            if (newSdfId <= 0) {
                newSdfId = createSdf(adUserId);
                detail.append("+sdf");
            }

            if (newSdfId != currentSdfId) {
                org.setZZSdf_ID(newSdfId);
                org.saveEx();
                detail.append("+relink(").append(currentSdfId).append("->").append(newSdfId).append(")");
            }
            finalSdfId = newSdfId;
        }

        boolean userNamesChanged = updateUserNamesIfBlank(finalSdfId, sdfName, sdfSurname);
        if (userNamesChanged) detail.append("+userNames");

        boolean sdfNamesChanged = updateSdfNamesIfBlank(finalSdfId, sdfName, sdfSurname);
        if (sdfNamesChanged) detail.append("+sdfNames");

        if (detail.length() == 0) {
            return "SKIPPED: SDL=" + sdl + " — sdfID=" + finalSdfId + " already has names set, nothing to do";
        }

        return "UPDATED: SDL=" + sdl + " sdfID=" + finalSdfId + " [" + detail + "]";
    }

    // -------------------------------------------------------------------------
    // Lookup helpers (same queries as ImportWspApprovalList)
    // -------------------------------------------------------------------------

    private int lookupOrgIdBySdl(String sdl) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT o.ZZSdfOrganisation_ID FROM ZZSdfOrganisation o"
                + " JOIN C_BPartner bp ON bp.C_BPartner_ID = o.C_BPartner_ID"
                + " WHERE bp.Value = ? AND o.AD_Client_ID = ? AND o.IsActive = 'Y'"
                + " FETCH FIRST 1 ROWS ONLY",
                sdl, Env.getAD_Client_ID(getCtx()));
    }

    private int findUserByEmail(String email) {
        if (Util.isEmpty(email, true)) return -1;
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_User_ID FROM AD_User WHERE UPPER(EMail) = UPPER(?) AND AD_Client_ID = ?"
                + " FETCH FIRST 1 ROWS ONLY",
                email, Env.getAD_Client_ID(getCtx()));
    }

    private int findSdfByUser(int adUserId) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT ZZSDF_ID FROM ZZSdf WHERE AD_User_ID = ? AND IsActive = 'Y'"
                + " FETCH FIRST 1 ROWS ONLY",
                adUserId);
    }

    // -------------------------------------------------------------------------
    // Create helpers (mirrors ImportWspApprovalList)
    // -------------------------------------------------------------------------

    private int createUser(String fullName, String firstName, String surname,
                            String email, String phone) {
        MUser_New user = new MUser_New(getCtx(), 0, get_TrxName());
        user.setName(fullName);
        if (!Util.isEmpty(firstName, true)) user.setZZFirstName(firstName);
        if (!Util.isEmpty(surname,   true)) user.setZZSurname(surname);
        if (!Util.isEmpty(email,     true)) user.setEMail(email);
        if (!Util.isEmpty(phone,     true)) user.setPhone(phone);
        user.setAD_Org_ID(0);
        user.saveEx();
        return user.get_ID();
    }

    private int createSdf(int adUserId) {
        X_ZZSdf sdf = new X_ZZSdf(getCtx(), 0, get_TrxName());
        sdf.setAD_User_ID(adUserId);
        sdf.setZZ_DocStatus(X_ZZSdf.ZZ_DOCSTATUS_Draft);
        sdf.setZZ_DocAction(X_ZZSdf.ZZ_DOCACTION_Submit);
        sdf.setAD_Org_ID(0);
        sdf.saveEx();
        return sdf.get_ID();
    }

    // -------------------------------------------------------------------------
    // Name-fill helpers — only touch fields that are currently null/blank
    // -------------------------------------------------------------------------

    private boolean updateUserNamesIfBlank(int sdfId, String firstName, String surname) {
        int adUserId = DB.getSQLValueEx(get_TrxName(),
                "SELECT AD_User_ID FROM ZZSdf WHERE ZZSDF_ID = ?", sdfId);
        if (adUserId <= 0) return false;

        MUser_New user = new MUser_New(getCtx(), adUserId, get_TrxName());
        boolean changed = false;

        if (Util.isEmpty(user.getZZFirstName(), true) && !Util.isEmpty(firstName, true)) {
            user.setZZFirstName(firstName);
            changed = true;
        }
        if (Util.isEmpty(user.getZZSurname(), true) && !Util.isEmpty(surname, true)) {
            user.setZZSurname(surname);
            changed = true;
        }
        if (Util.isEmpty(user.getName(), true)) {
            String full = (firstName + " " + surname).trim();
            if (!full.isEmpty()) {
                user.setName(full);
                changed = true;
            }
        }

        if (changed) user.saveEx();
        return changed;
    }

    /**
     * ZZSdf.ZZFirstName/ZZMiddleName/ZZSurname exist on the table but are not exposed by
     * the generated X_ZZSdf model, so they are written directly via SQL. Each UPDATE is
     * guarded so it only affects the row when the target column is currently null/blank.
     */
    private boolean updateSdfNamesIfBlank(int sdfId, String firstName, String surname) {
        int rows = 0;
        if (!Util.isEmpty(firstName, true)) {
            rows += DB.executeUpdateEx(
                    "UPDATE ZZSdf SET ZZFirstName = ? WHERE ZZSDF_ID = ?"
                    + " AND (ZZFirstName IS NULL OR TRIM(ZZFirstName) = '')",
                    new Object[]{firstName, sdfId}, get_TrxName());
        }
        if (!Util.isEmpty(surname, true)) {
            rows += DB.executeUpdateEx(
                    "UPDATE ZZSdf SET ZZSurname = ? WHERE ZZSDF_ID = ?"
                    + " AND (ZZSurname IS NULL OR TRIM(ZZSurname) = '')",
                    new Object[]{surname, sdfId}, get_TrxName());
        }
        return rows > 0;
    }

    // -------------------------------------------------------------------------
    // Cell / CSV utilities
    // -------------------------------------------------------------------------

    private String cellText(Row row, int col, DataFormatter fmt) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return fmt.formatCellValue(cell);
    }

    private String csvLine(String sdl, String orgName, String status, String message) {
        return toCsv(sdl) + "," + toCsv(orgName) + "," + toCsv(status) + "," + toCsv(message);
    }

    private String toCsv(String in) {
        String safe = in == null ? "" : in.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
