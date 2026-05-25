package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Trx;

import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.ntier.models.MZZSdfOrganisation;

/**
 * Bulk-loads SDF registration documents from the SG data dump.
 *
 * Expected directory layout:
 *   BASE_DIR / <id_number> / ID Copy / <file>
 *   BASE_DIR / <id_number> / <sdl_number> / <doc_type_folder> / <file>
 *
 * For each ID number directory:
 *   1. Resolve ZZSdf via ZZ_ID_Passport_No — error and skip if multiple found.
 *   2. Attach "ID Copy" files to the linked AD_User record.
 *   3. For each SDL (L-number) subdirectory:
 *        a. Resolve C_BPartner by Value = SDL number.
 *        b. Find ZZSdfOrganisation by BPartner + SdfId; if not found,
 *           find one with that BPartner and no SdfId, then set the SdfId.
 *        c. Attach all files under every subfolder to ZZSdfOrganisation.
 *
 * Process parameter:
 *   ClearAttachments (Y/N) — when Y, deletes existing attachments on each
 *                            AD_User and ZZSdfOrganisation before loading.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.ImportSgSdfDocuments")
public class ImportSgSdfDocuments extends SvrProcess {

    private static final String BASE_DIR    = "/tmp/SG_Data_070526";
    private static final String ID_COPY_DIR = "ID Copy";

    private static final int TABLE_AD_USER = MTable.getTable_ID("AD_User");
    private static final int TABLE_SDF_ORG = X_ZZSdfOrganisation.Table_ID;

    private boolean clearAttachments = false;

    private int processed   = 0;
    private int idCopyFiles = 0;
    private int orgFiles    = 0;
    private int skipped     = 0;

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

        for (File idDir : dirs(base)) {
            processIdDirectory(idDir);
        }

        return "Processed=" + processed
                + "  IDCopyFiles=" + idCopyFiles
                + "  OrgFiles=" + orgFiles
                + "  Skipped=" + skipped;
    }

    // -------------------------------------------------------------------------
    //  Per-ID directory
    // -------------------------------------------------------------------------

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
                clearAttachment(TABLE_AD_USER, adUserId);
            for (File f : files(idCopyDir)) {
                if (attachFile(TABLE_AD_USER, adUserId, f, "ID=" + idNumber))
                    idCopyFiles++;
                else
                    skipped++;
            }
        }

        for (File lDir : dirs(idDir)) {
            if (ID_COPY_DIR.equals(lDir.getName())) continue;
            processLDirectory(lDir, sdfId, idNumber);
        }

        processed++;
    }

    // -------------------------------------------------------------------------
    //  Per-SDL (L-number) directory
    // -------------------------------------------------------------------------

    private void processLDirectory(File lDir, int sdfId, String idNumber) {
        String sdlNumber = lDir.getName();

        int bpId = findBPartner(sdlNumber);
        if (bpId <= 0) {
            addLog("No C_BPartner for SDL=" + sdlNumber + " (ID=" + idNumber + ") — skipped");
            skipped++;
            return;
        }

        int orgId = findOrLinkOrg(bpId, sdfId, sdlNumber);
        if (orgId <= 0) {
            addLog("No ZZSdfOrganisation for SDL=" + sdlNumber + " BP=" + bpId + " — skipped");
            skipped++;
            return;
        }

        if (clearAttachments)
            clearAttachment(TABLE_SDF_ORG, orgId);

        for (File subDir : dirs(lDir)) {
            for (File f : files(subDir)) {
                if (attachFile(TABLE_SDF_ORG, orgId, f, "SDL=" + sdlNumber))
                    orgFiles++;
                else
                    skipped++;
            }
        }
        // Files placed directly in the SDL dir (edge case)
        for (File f : files(lDir)) {
            if (attachFile(TABLE_SDF_ORG, orgId, f, "SDL=" + sdlNumber))
                orgFiles++;
            else
                skipped++;
        }
    }

    // -------------------------------------------------------------------------
    //  Lookup helpers
    // -------------------------------------------------------------------------

    /**
     * Returns [sdfId, adUserId] or null.
     * Logs and returns null if zero or multiple ZZSdf records match.
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
                addLog("ERROR: multiple ZZSdf records for ID=" + idNumber + " — skipped");
                return null;
            }
            if (first == null)
                addLog("No ZZSdf record for ID=" + idNumber + " — skipped");
            return first;
        } catch (Exception e) {
            addLog("Error looking up ZZSdf for ID=" + idNumber + ": " + e.getMessage());
            return null;
        } finally {
            DB.close(rs, pst);
        }
    }

    private int findBPartner(String sdlNumber) {
        return DB.getSQLValueEx(null,
                "SELECT c_bpartner_id FROM c_bpartner WHERE value = ? LIMIT 1",
                sdlNumber);
    }

    /**
     * Finds ZZSdfOrganisation for the given BP and SDF.
     * First tries an exact match (BP + SdfId already set).
     * Falls back to a row with the same BP but no SdfId, and links the SdfId.
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
                "WHERE c_bpartner_id = ? AND zzsdf_id IS NULL LIMIT 1",
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
            addLog("Linked ZZSdf_ID=" + sdfId + " to ZZSdfOrganisation_ID=" + orgId
                    + " (SDL=" + sdlNumber + ")");
        } catch (Exception e) {
            trx.rollback();
            addLog("WARN: Failed to link ZZSdf_ID=" + sdfId + " to org=" + orgId
                    + " (SDL=" + sdlNumber + "): " + e.getMessage());
        } finally {
            trx.close();
        }
    }

    // -------------------------------------------------------------------------
    //  Attachment helpers
    // -------------------------------------------------------------------------

    /** Returns true on success, false on zero-byte or error. */
    private boolean attachFile(int tableId, int recordId, File file, String context) {
        if (file.length() == 0) {
            addLog("Zero-byte file skipped: " + file.getName() + " [" + context + "]");
            return false;
        }
        String trxName = Trx.createTrxName("SGSdfAtt");
        Trx trx = Trx.get(trxName, true);
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            MAttachment att = new MAttachment(getCtx(), tableId, recordId, null, trxName);
            att.addEntry(file.getName(), data);
            att.saveEx();
            trx.commit(true);
            return true;
        } catch (Exception e) {
            trx.rollback();
            addLog("ERROR attaching " + file.getName() + " [" + context + "]: " + e.getMessage());
            return false;
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
            addLog("WARN: Could not clear attachment for table=" + tableId
                    + " record=" + recordId + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  File system helpers
    // -------------------------------------------------------------------------

    private static File[] dirs(File parent) {
        File[] d = parent.listFiles(File::isDirectory);
        return d != null ? d : new File[0];
    }

    private static File[] files(File parent) {
        File[] f = parent.listFiles(File::isFile);
        return f != null ? f : new File[0];
    }
}
