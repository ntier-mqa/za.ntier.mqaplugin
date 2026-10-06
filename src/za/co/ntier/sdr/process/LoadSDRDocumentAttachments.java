package za.co.ntier.sdr.process;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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
    private final List<String> errors = new ArrayList<>();

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
        int totalErrors = 0;

        for (String tableName : tables) {
            int[] stats = processTable(tableName, dir, maxRows);
            totalProcessed += stats[0];
            totalAttached += stats[1];
            totalAlready += stats[2];
            totalMissing += stats[3];
            totalErrors += stats[4];
        }

        attachListIfAny(missingFiles, "load-sdr-attachments-missing-files");
        attachListIfAny(errors, "load-sdr-attachments-errors");

        return "Processed " + totalProcessed + " row(s) across " + tables.size() + " table(s): " + totalAttached
                + " attached, " + totalAlready + " already attached, " + totalMissing
                + " file(s) not found on disk, " + totalErrors + " error(s)."
                + ((totalMissing > 0 || totalErrors > 0)
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
            return new int[5];
        }
        String[] keyColumns = table.getKeyColumns();
        if (keyColumns == null || keyColumns.length != 1) {
            addLog(tableName + ": does not have exactly one key column - skipped.");
            return new int[5];
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

                    byte[] data = Files.readAllBytes(file.toPath());
                    String entryName = (original != null && !original.trim().isEmpty()) ? original.trim()
                            : saved.trim();

                    MAttachment attachment = existing != null ? existing
                            : new MAttachment(getCtx(), table.getAD_Table_ID(), pk, null, get_TrxName());
                    attachment.addEntry(entryName, data);
                    attachment.saveEx();
                    attached++;
                } catch (Exception e) {
                    errorCount++;
                    addToListCapped(errors, tableName + ".id=" + pk + " file=" + saved + ": " + describeError(e));
                }
            }
        } finally {
            DB.close(rs, pstmt);
        }

        addLog(tableName + ": processed " + processed + ", attached " + attached + ", already " + already
                + ", missing " + missing + ", errors " + errorCount);

        return new int[] { processed, attached, already, missing, errorCount };
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
