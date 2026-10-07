package za.co.ntier.sdr.process;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.MAttachment;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 10 step 2 (see [[ExportSDRDocumentTables]]): loads the actual files from the server-local
 * document store into MAttachment blobs on their owning SDR_ record, for every SDR_ table that has
 * an {@code sdr_savedfilename} column. Confirmed 2026-09-16 via ExportSDRDocumentTables that
 * sdr_savedfilename (the GUID-based stored name) is the reliable match key across all 7 tables that
 * actually carry files - sdr_filepath is stale (old server paths from the previous system) and is
 * never used for matching.
 *
 * <p>Table discovery is dynamic (information_schema), same reasoning as
 * {@link ExportSDRReconciliationReport}/{@link ExportSDRDocumentTables} - no hardcoded table list to
 * drift out of date. Each table's physical key column is read from its own AD_Column metadata
 * ({@link MTable#getKeyColumns()}) rather than assumed from a naming convention.
 *
 * <p>Idempotent: skips any record that already has an attachment ({@link MAttachment#get}), so a
 * re-run (e.g. after dropping more files into the directory) only picks up new/previously-missing
 * files. A row whose file genuinely isn't on disk is logged (not an error) and left for a later
 * re-run once the file turns up.
 */
@Process(name = "za.co.ntier.sdr.process.LoadSDRDocumentAttachments")
public class LoadSDRDocumentAttachments extends SvrProcess {

    @Parameter(name = "BaseDirectory")
    private String p_BaseDirectory;

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String DEFAULT_BASE_DIR = "/home/ntier/MQASkillsDocuments_25092026";
    private static final String SAVED_FILENAME_COL = "sdr_savedfilename";
    private static final String ORIGINAL_FILENAME_COL = "sdr_originalfilename";
    private static final int MAX_LOGGED = 20000;

    private final List<String> missingFiles = new ArrayList<>();
    private final List<String> emptyOnDiskFiles = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> orphanFiles = new ArrayList<>();

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        String baseDir = (p_BaseDirectory != null && !p_BaseDirectory.trim().isEmpty())
                ? p_BaseDirectory.trim()
                : DEFAULT_BASE_DIR;
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        File dir = new File(baseDir);
        if (!dir.isDirectory()) {
            throw new AdempiereException("Base directory does not exist or is not a directory: " + baseDir);
        }
        addLog("Base directory: " + baseDir);

        List<String> tables = findDocumentTables();
        addLog("Found " + tables.size() + " SDR_ table(s) with " + SAVED_FILENAME_COL + ": " + tables);

        int totalProcessed = 0;
        int totalAttached = 0;
        int totalAlready = 0;
        int totalMissing = 0;
        int totalEmptyOnDisk = 0;
        int totalErrors = 0;

        for (String tableName : tables) {
            int[] stats = processTable(tableName, dir, maxRows);
            totalProcessed += stats[0];
            totalAttached += stats[1];
            totalAlready += stats[2];
            totalMissing += stats[3];
            totalEmptyOnDisk += stats[4];
            totalErrors += stats[5];
        }

        int orphanCount = findOrphanFiles(dir, tables);

        attachListIfAny(missingFiles, "load-sdr-attachments-missing-files");
        attachListIfAny(emptyOnDiskFiles, "load-sdr-attachments-zero-byte-files");
        attachListIfAny(errors, "load-sdr-attachments-errors");
        attachListIfAny(orphanFiles, "load-sdr-attachments-orphan-files-on-disk");

        return "Processed " + totalProcessed + " row(s) across " + tables.size() + " table(s): " + totalAttached
                + " attached, " + totalAlready + " already attached, " + totalMissing
                + " file(s) not found on disk, " + totalEmptyOnDisk + " file(s) 0 bytes on disk, " + totalErrors
                + " error(s), " + orphanCount + " file(s) on disk never referenced by any row."
                + ((totalMissing > 0 || totalEmptyOnDisk > 0 || totalErrors > 0 || orphanCount > 0)
                        ? " See the Attachment icon on this Process Audit record for details."
                        : "");
    }

    private List<String> findDocumentTables() {
        List<String> tables = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement("SELECT DISTINCT table_name FROM information_schema.columns "
                    + "WHERE table_name LIKE 'sdr\\_%' ESCAPE '\\' AND column_name = ? ORDER BY table_name",
                    get_TrxName());
            pstmt.setString(1, SAVED_FILENAME_COL);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed listing SDR_ tables with " + SAVED_FILENAME_COL, e);
        } finally {
            DB.close(rs, pstmt);
        }
        return tables;
    }

    private int[] processTable(String tableName, File dir, long maxRows) throws Exception {
        MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
        if (table == null) {
            addLog(tableName + ": AD_Table not found - skipped.");
            return new int[6];
        }
        String[] keyColumns = table.getKeyColumns();
        if (keyColumns == null || keyColumns.length != 1) {
            addLog(tableName + ": does not have exactly one key column - skipped.");
            return new int[6];
        }
        String keyColumn = keyColumns[0];

        String sql = "SELECT " + keyColumn + " AS pk, " + SAVED_FILENAME_COL + " AS saved, "
                + ORIGINAL_FILENAME_COL + " AS original FROM " + tableName + " WHERE " + SAVED_FILENAME_COL
                + " IS NOT NULL AND " + SAVED_FILENAME_COL + " <> '' ORDER BY " + keyColumn
                + (maxRows > 0 ? " LIMIT " + maxRows : "");

        int processed = 0;
        int attached = 0;
        int already = 0;
        int missing = 0;
        int emptyOnDisk = 0;
        int errorCount = 0;

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            pstmt.setFetchSize(500);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                processed++;
                int pk = rs.getInt("pk");
                String saved = rs.getString("saved");
                String original = rs.getString("original");

                try {
                    MAttachment existing = MAttachment.get(getCtx(), table.getAD_Table_ID(), pk, get_TrxName());
                    if (existing != null && existing.getEntryCount() > 0) {
                        already++;
                        continue;
                    }

                    File file = new File(dir, saved.trim());
                    if (!file.isFile()) {
                        missing++;
                        addToListCapped(missingFiles, tableName + ".id=" + pk + ": " + file.getAbsolutePath());
                        continue;
                    }
                    if (file.length() == 0) {
                        // Genuinely empty/corrupt source file on disk - not a loader bug, nothing to
                        // attach. Tracked separately from "missing" (file is there, just 0 bytes) and
                        // from "errors" (not a save failure - we never attempt the save).
                        emptyOnDisk++;
                        addToListCapped(emptyOnDiskFiles, tableName + ".id=" + pk + ": " + file.getAbsolutePath());
                        continue;
                    }

                    byte[] data = Files.readAllBytes(file.toPath());
                    String rawEntryName = (original != null && !original.trim().isEmpty()) ? original.trim()
                            : saved.trim();
                    String entryName = sanitizeEntryName(rawEntryName);

                    MAttachment attachment = existing != null ? existing
                            : new MAttachment(getCtx(), table.getAD_Table_ID(), pk, null, get_TrxName());
                    attachment.addEntry(entryName, data);
                    attachment.saveEx();
                    attached++;
                } catch (Exception e) {
                    errorCount++;
                    addToListCapped(errors, tableName + ".id=" + pk + " file=" + saved + " entryName=["
                            + original + "] bytes=" + describeFileSize(dir, saved) + ": " + describeError(e));
                }
            }
        } finally {
            DB.close(rs, pstmt);
        }

        addLog(tableName + ": processed " + processed + ", attached " + attached + ", already " + already
                + ", missing " + missing + ", empty-on-disk " + emptyOnDisk + ", errors " + errorCount);

        return new int[] { processed, attached, already, missing, emptyOnDisk, errorCount };
    }

    /**
     * Reverse check: files physically present in the directory that no row in any document table
     * references at all (by sdr_savedfilename) - e.g. leftovers from a partial copy, renamed files,
     * or files for records that were never migrated. Deliberately queries the FULL referenced-name
     * set per table (no MaxRows limit) regardless of how the main attach loop was bounded, since a
     * MaxRows test run must not report every not-yet-processed file as "orphaned".
     */
    private int findOrphanFiles(File dir, List<String> tables) {
        Set<String> referenced = new HashSet<>();
        for (String tableName : tables) {
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                pstmt = DB.prepareStatement("SELECT DISTINCT " + SAVED_FILENAME_COL + " FROM " + tableName
                        + " WHERE " + SAVED_FILENAME_COL + " IS NOT NULL AND " + SAVED_FILENAME_COL + " <> ''",
                        get_TrxName());
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    String name = rs.getString(1);
                    if (name != null) {
                        referenced.add(name.trim());
                    }
                }
            } catch (Exception e) {
                throw new AdempiereException("Failed collecting referenced filenames for " + tableName, e);
            } finally {
                DB.close(rs, pstmt);
            }
        }

        File[] diskFiles = dir.listFiles();
        if (diskFiles == null) {
            return 0;
        }
        int orphanCount = 0;
        for (File f : diskFiles) {
            if (!f.isFile()) {
                continue;
            }
            if (!referenced.contains(f.getName())) {
                orphanCount++;
                addToListCapped(orphanFiles, f.getAbsolutePath() + " (" + f.length() + " bytes)");
            }
        }
        addLog("Orphan check: " + referenced.size() + " distinct filename(s) referenced across " + tables.size()
                + " table(s), " + orphanCount + " file(s) on disk not referenced by any row.");
        return orphanCount;
    }

    /**
     * iDempiere's attachment store treats "/" in an entry name as a path separator (confirmed
     * 2026-09-16: every "SaveError" with no logged cause on sdr_organisationdocuments had a "/" in
     * its sdr_originalfilename - e.g. a date embedded as "2023/04/17 13:12:44" - and the store
     * silently returned false rather than throwing). Replacing filesystem-unsafe characters with
     * "-" avoids the silent failure without losing the human-readable name.
     */
    private static String sanitizeEntryName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "-");
    }

    /**
     * saveEx() falls back to the unhelpful literal "SaveError" whenever PO.save() fails without
     * CLogger having a specific validation message logged - but it still attaches the real
     * underlying exception as the cause (see PO#saveEx's own source), which plain
     * {@code e.getMessage()} silently drops. Surfacing the cause here is the difference between a
     * diagnosable log and 2000 identical "SaveError" lines.
     */
    private static String describeError(Exception e) {
        StringBuilder sb = new StringBuilder(String.valueOf(e.getMessage()));
        Throwable cause = e.getCause();
        if (cause != null) {
            sb.append(" | cause: ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage());
            }
        }
        return sb.toString();
    }

    private static String describeFileSize(File dir, String saved) {
        File file = new File(dir, saved.trim());
        return file.isFile() ? String.valueOf(file.length()) : "?";
    }

    private void addToListCapped(List<String> list, String entry) {
        if (list.size() < MAX_LOGGED) {
            list.add(entry);
        }
    }

    /**
     * Attaches the list as a .txt entry on this process run's own AD_PInstance record (same
     * download-via-Attachment-icon pattern as {@link ExportSDRReconciliationReport}), rather than
     * writing to server /tmp - keeps this usable without server filesystem access.
     */
    private void attachListIfAny(List<String> list, String fileNamePrefix) throws Exception {
        if (list.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String entry : list) {
            sb.append(entry).append('\n');
        }
        if (list.size() >= MAX_LOGGED) {
            sb.append("(truncated at ").append(MAX_LOGGED).append(")\n");
        }

        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String fileName = fileNamePrefix + "-" + ts + ".txt";

        MAttachment attachment = MAttachment.get(getCtx(), I_AD_PInstance.Table_ID, getAD_PInstance_ID(),
                get_TrxName());
        if (attachment == null) {
            attachment = new MAttachment(getCtx(), I_AD_PInstance.Table_ID, getAD_PInstance_ID(), null,
                    get_TrxName());
        }
        attachment.addEntry(fileName, sb.toString().getBytes(StandardCharsets.UTF_8));
        attachment.saveEx();
        addLog(fileNamePrefix + ": " + list.size() + " entries attached as " + fileName);
    }
}
