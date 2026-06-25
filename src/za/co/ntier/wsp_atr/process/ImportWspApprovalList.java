package za.co.ntier.wsp_atr.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.api.model.MBPartner_New;
import za.co.ntier.api.model.MUser_New;
import za.co.ntier.api.model.X_ZZOrganisationLinkage;
import za.co.ntier.api.model.X_ZZSdf;
import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.ntier.models.MZZWSPATRSubmitted;
import za.co.ntier.wsp_atr.form.WspAtrSubmittedADForm;

/**
 * Imports WSP submissions from the "Updated WSP Approval List" spreadsheet (Sheet1).
 *
 * For each row the process:
 *   1. Checks whether a ZZ_WSP_ATR_Submitted record already exists for the SDL + year 2026.
 *      If it does, the row is skipped and reported.
 *   2. Otherwise, finds-or-creates: AD_User → ZZSdf → C_BPartner → ZZSdfOrganisation →
 *      ZZ_WSP_ATR_Submitted.
 *
 * A CSV log file is written to /tmp and offered for download.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ImportWspApprovalList")
public class ImportWspApprovalList extends SvrProcess {

    // Column indices in Sheet1 (0-based)
    private static final int COL_ORG_NAME        = 0;
    private static final int COL_SDL             = 1;
    private static final int COL_PARENT_SDL      = 2;
    private static final int COL_WSP_YEAR        = 3;
    private static final int COL_WSP_DATE        = 4;
    private static final int COL_STATUS          = 7;   // PlanningGrantStatus
    private static final int COL_SDF_NAME        = 19;
    private static final int COL_SDF_SURNAME     = 20;
    private static final int COL_SDF_EMAIL       = 21;
    private static final int COL_SDF_PHONE       = 22;

    private static final String WSP_STATUS_REF_UU = "98479fb5-df5d-440d-86aa-92d77a320857";
    private static final int    BP_GROUP_UNKNOWN   = 1000018;

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

        int created = 0;
        int skipped = 0;
        int errors  = 0;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File logFile = new File("/tmp/wsp-approval-import-" + timestamp + ".csv");

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
                    String result = processRow(row, fmt, sdl, orgName, finYearId);
                    if (result.startsWith("SKIPPED")) {
                        skipped++;
                        log.println(csvLine(sdl, orgName, "SKIPPED", result));
                    } else {
                        created++;
                        log.println(csvLine(sdl, orgName, "CREATED", result));
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

        return "Processed: " + (created + skipped + errors)
                + " rows — " + created + " created, " + skipped + " skipped, " + errors + " errors."
                + " Log: " + logFile.getAbsolutePath();
    }

    // -------------------------------------------------------------------------

    private String processRow(Row row, DataFormatter fmt,
                              String sdl, String orgName, int finYearId) {

        // 1. Check for existing submission via org lookup
        int existingOrgId = lookupOrgIdBySdl(sdl);
        if (existingOrgId > 0) {
            int existingSubmittedId = DB.getSQLValueEx(get_TrxName(),
                    "SELECT ZZ_WSP_ATR_Submitted_ID FROM ZZ_WSP_ATR_Submitted"
                    + " WHERE ZZSDFOrganisation_ID = ? AND ZZ_FinYear_ID = ?"
                    + " FETCH FIRST 1 ROWS ONLY",
                    existingOrgId, finYearId);
            if (existingSubmittedId > 0) {
                return "SKIPPED: SDL=" + sdl + " — submission for 2026 already exists (ID=" + existingSubmittedId + ")";
            }
        }

        // 2. SDF: find-or-create AD_User and ZZSdf
        String sdfName    = cellText(row, COL_SDF_NAME, fmt).trim();
        String sdfSurname = cellText(row, COL_SDF_SURNAME, fmt).trim();
        String sdfEmail   = cellText(row, COL_SDF_EMAIL, fmt).trim();
        String sdfPhone   = cellText(row, COL_SDF_PHONE, fmt).trim();
        String fullName   = (sdfName + " " + sdfSurname).trim();
        if (fullName.isEmpty()) fullName = sdl;

        StringBuilder detail = new StringBuilder();

        int adUserId = findUserByEmail(sdfEmail);
        if (adUserId <= 0) {
            adUserId = createUser(fullName, sdfName, sdfSurname, sdfEmail, sdfPhone);
            detail.append("+user");
        }

        int sdfId = findSdfByUser(adUserId);
        if (sdfId <= 0) {
            sdfId = createSdf(adUserId);
            detail.append("+sdf");
        }

        // 3. BPartner: find-or-create
        int bpId = findBPartnerBySdl(sdl);
        if (bpId <= 0) {
            bpId = createBPartner(sdl, orgName);
            detail.append("+bp");
        }

        // 4. ZZSdfOrganisation: find-or-create
        int orgId = existingOrgId > 0 ? existingOrgId : findOrgByBPartner(bpId);
        if (orgId <= 0) {
            orgId = createOrg(bpId, sdfId);
            detail.append("+org");
        }

        // 5. Link child to parent if ParentSDLNumber is present
        String parentSdl = cellText(row, COL_PARENT_SDL, fmt).trim();
        if (!parentSdl.isEmpty() && !parentSdl.equalsIgnoreCase(sdl)) {
            boolean linked = linkChildToParent(sdl, bpId, parentSdl, detail);
            if (!linked) {
                addLog("WARN: SDL=" + sdl + " — could not link to parent SDL=" + parentSdl
                        + " (parent BP not found)");
            }
        }

        // 7. Resolve WSP status from PlanningGrantStatus column
        String statusText = cellText(row, COL_STATUS, fmt).trim();
        String wspStatus  = resolveWspStatus(statusText);
        if (wspStatus == null) {
            wspStatus = "DR"; // default Draft when unmapped
            addLog("WARN: SDL=" + sdl + " — PlanningGrantStatus '" + statusText
                    + "' not found in reference table, defaulting to Draft");
        }

        // 8. Parse WSPDate for submitted date
        Timestamp submittedDate = parseDateCell(row.getCell(COL_WSP_DATE));

        // 9. Create the submission
        MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(getCtx(), 0, get_TrxName());
        submitted.setName("WSP Import " + sdl + " 2026");
        submitted.setZZSdfOrganisation_ID(orgId);
        submitted.setZZ_FinYear_ID(finYearId);
        submitted.setZZ_DocStatus(wspStatus);
        if ("SU".equals(wspStatus)) {
            submitted.setZZ_DocAction("VE");
        }
        submitted.setZZ_Import_Submitted_Data("N");
        submitted.setFileName(new File(filePath).getName());
        if (submittedDate != null) {
            submitted.setSubmittedDate(submittedDate);
        }
        submitted.setZZ_Submission_Due_Date(
                WspAtrSubmittedADForm.getWSPATR_Due_Date(
                        Env.getAD_Client_ID(getCtx()), orgId, get_TrxName()));
        submitted.setAD_Org_ID(0);
        submitted.saveEx();

        WspAtrSubmittedADForm.rebuildSubLevyOrgLinks(submitted.get_ID(), get_TrxName());

        String detailStr = detail.length() > 0 ? " [" + detail + "]" : "";
        return "CREATED: SDL=" + sdl + " submissionID=" + submitted.get_ID() + detailStr;
    }

    // -------------------------------------------------------------------------
    // Lookup helpers
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

    private int findBPartnerBySdl(String sdl) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT C_BPartner_ID FROM C_BPartner WHERE Value = ? AND AD_Client_ID = ?",
                sdl, Env.getAD_Client_ID(getCtx()));
    }

    private int findOrgByBPartner(int bpId) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT ZZSdfOrganisation_ID FROM ZZSdfOrganisation"
                + " WHERE C_BPartner_ID = ? AND AD_Client_ID = ? AND IsActive = 'Y'"
                + " FETCH FIRST 1 ROWS ONLY",
                bpId, Env.getAD_Client_ID(getCtx()));
    }

    /**
     * Links the child BP to its parent BP via ZZOrganisationLinkage if no active linkage
     * already exists. Returns true if a linkage exists or was created; false if the parent
     * BP cannot be found.
     */
    private boolean linkChildToParent(String childSdl, int childBpId, String parentSdl,
                                      StringBuilder detail) {
        int parentBpId = findBPartnerBySdl(parentSdl);
        if (parentBpId <= 0) {
            return false;
        }

        // Check whether an active linkage already exists for this child BP
        int existingLinkage = DB.getSQLValueEx(get_TrxName(),
                "SELECT ZZOrganisationLinkage_ID FROM ZZOrganisationLinkage"
                + " WHERE C_BPartner_ID = ? AND BPartner_Parent_ID = ? AND IsActive = 'Y'"
                + " FETCH FIRST 1 ROWS ONLY",
                childBpId, parentBpId);

        if (existingLinkage > 0) {
            return true; // already linked
        }

        X_ZZOrganisationLinkage link = new X_ZZOrganisationLinkage(getCtx(), 0, get_TrxName());
        link.setC_BPartner_ID(childBpId);
        link.setBPartner_Parent_ID(parentBpId);
        link.setZZ_SDL_No(childSdl);
        link.setAD_Org_ID(0);
        link.saveEx();
        detail.append("+link");
        addLog("INFO: SDL=" + childSdl + " linked to parent SDL=" + parentSdl
                + " (linkageID=" + link.get_ID() + ")");
        return true;
    }

    private String resolveWspStatus(String statusText) {
        if (Util.isEmpty(statusText, true)) return null;
        String lookup = "Created".equalsIgnoreCase(statusText.trim()) ? "Draft" : statusText.trim();
        return DB.getSQLValueStringEx(get_TrxName(),
                "SELECT rl.Value FROM AD_Ref_List rl"
                + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                + " AND UPPER(rl.Name) = UPPER(?)",
                lookup);
    }

    // -------------------------------------------------------------------------
    // Create helpers
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

    private int createBPartner(String sdl, String orgName) {
        MBPartner_New bp = new MBPartner_New(getCtx(), 0, get_TrxName());
        bp.setValue(sdl);
        bp.setName(Util.isEmpty(orgName, true) ? sdl : orgName);
        bp.setC_BP_Group_ID(BP_GROUP_UNKNOWN);
        bp.setIsVendor(true);
        bp.setIsCustomer(false);
        bp.setIsEmployee(false);
        bp.setIsProspect(false);
        bp.setAD_Org_ID(0);
        bp.saveEx();
        return bp.get_ID();
    }

    private int createOrg(int bpId, int sdfId) {
        X_ZZSdfOrganisation org = new X_ZZSdfOrganisation(getCtx(), 0, get_TrxName());
        org.setC_BPartner_ID(bpId);
        org.setZZSdf_ID(sdfId);
        org.setZZSdfRoleType(X_ZZSdfOrganisation.ZZSDFROLETYPE_PrimarySDF);
        org.setZZActingForEmployer(false);
        org.setZZReplacingPrimarySDF(false);
        org.setZZSecondarySdf(false);
        org.setZZ_DocStatus("AP");
        org.setAD_Org_ID(0);
        org.saveEx();
        return org.get_ID();
    }

    // -------------------------------------------------------------------------
    // Cell / CSV utilities
    // -------------------------------------------------------------------------

    private String cellText(Row row, int col, DataFormatter fmt) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return fmt.formatCellValue(cell);
    }

    private Timestamp parseDateCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return new Timestamp(cell.getDateCellValue().getTime());
            }
            String txt = cell.toString().trim();
            if (!txt.isEmpty()) {
                for (String pattern : new String[]{"yyyy/MM/dd", "yyyy-MM-dd", "dd/MM/yyyy"}) {
                    try {
                        return new Timestamp(new SimpleDateFormat(pattern).parse(txt).getTime());
                    } catch (Exception ignored) { /* try next */ }
                }
            }
        } catch (Exception ignored) { /* return null */ }
        return null;
    }

    private String csvLine(String sdl, String orgName, String status, String message) {
        return toCsv(sdl) + "," + toCsv(orgName) + "," + toCsv(status) + "," + toCsv(message);
    }

    private String toCsv(String in) {
        String safe = in == null ? "" : in.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}
