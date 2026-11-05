package za.co.ntier.sdr.process;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /** Comma-separated list of directories to search, in order - first match wins. */
    @Parameter(name = "BaseDirectory")
    private String p_BaseDirectory;

    @Parameter(name = "MaxRows")
    private BigDecimal p_MaxRows;

    private static final String[] DEFAULT_BASE_DIRS = { "/home/ntier/MQASkillsDocuments",
            "/home/ntier/MQASkillsDocuments_25092026", "/home/ntier/MQASkillsDocuments_19102026" };
    private static final String SAVED_FILENAME_COL = "sdr_savedfilename";
    private static final String ORIGINAL_FILENAME_COL = "sdr_originalfilename";
    private static final String ISACTIVE_COL = "isactive";
    private static final int MAX_LOGGED = 20000;

    /** One FK hop in a parent chain: fkColumn lives on the PREVIOUS level, pointing at parentTable. */
    private static final class ParentHop {
        final String fkColumn;
        final String parentTable;

        ParentHop(String fkColumn, String parentTable) {
            this.fkColumn = fkColumn;
            this.parentTable = parentTable;
        }
    }

    /**
     * Maps a document table to the FULL chain of ancestor records whose OWN delete status (not the
     * document row's own isactive) determines whether a missing file is expected. Confirmed
     * 2026-10-26: document rows migrated with isactive='Y' even when their true ancestor
     * (Organisation/SDFOrganisation/SDF) was itself marked deleted - and confirmed again the same
     * day that a single hop isn't always enough: sdr_organisationbankingdetailsdocumentupload's
     * DIRECT parent (sdr_organisationbankingdetails) can itself show isactive='Y' while ITS OWN
     * parent sdr_organisation is deleted - the deletion cascade isn't reflected at the intermediate
     * level either. Each hop's fkColumn is assumed to be that hop's parent table's own PK column
     * name too (this project's standard TableDir naming convention) and is verified at runtime via
     * information_schema before use (see {@link #buildVerifiedChain}) - an unverifiable hop (and
     * every hop depending on it) is dropped rather than risking a broken query.
     */
    private static final Map<String, List<ParentHop>> PARENT_CHAINS = new HashMap<>();
    static {
        PARENT_CHAINS.put("sdr_organisationdocuments",
                Arrays.asList(new ParentHop("sdr_organisation_id", "sdr_organisation")));
        PARENT_CHAINS.put("sdr_organisationemails",
                Arrays.asList(new ParentHop("sdr_organisation_id", "sdr_organisation")));
        // Confirmed 2026-10-26: this table carries its own direct sdr_organisation_id column
        // (alongside sdr_organisationbankingdetails_id) - a single direct hop straight to
        // sdr_organisation, rather than going via sdr_organisationbankingdetails's own FK.
        PARENT_CHAINS.put("sdr_organisationbankingdetailsdocumentupload",
                Arrays.asList(new ParentHop("sdr_organisation_id", "sdr_organisation")));
        PARENT_CHAINS.put("sdr_wspatrdocumentuploads",
                Arrays.asList(new ParentHop("sdr_organisation_id", "sdr_organisation")));
        PARENT_CHAINS.put("sdr_sdfcertificatedocument", Arrays.asList(new ParentHop("sdr_sdf_id", "sdr_sdf")));
        PARENT_CHAINS.put("sdr_sdforganisationdocumentupload",
                Arrays.asList(new ParentHop("sdr_sdforganisation_id", "sdr_sdforganisation")));
        PARENT_CHAINS.put("sdr_wspatrevaluationverificationchecklist",
                Arrays.asList(new ParentHop("sdr_wspatr_id", "sdr_wspatr")));
        PARENT_CHAINS.put("sdr_persondocumentupload", Arrays.asList(new ParentHop("sdr_person_id", "sdr_person")));
    }

    /**
     * sdr_wspatrbulkapprovaldocumentupload has no single parent FK - sdr_wspatrids is a free-text
     * CSV of MULTIPLE sdr_wspatr.id (source id) values (confirmed 2026-10-26, true many-to-many,
     * see [[project_wspatrbulkapproval_m2m_pending]] for the junction-table follow-up). Until that
     * junction table exists, "is the parent deleted" is answered by resolving every id in the CSV
     * and checking whether ALL of them are inactive - handled separately from PARENT_SPECS below.
     */
    private static final String WSPATR_CSV_TABLE = "sdr_wspatrbulkapprovaldocumentupload";
    private static final String WSPATR_CSV_COLUMN = "sdr_wspatrids";
    private static final String WSPATR_TABLE = "sdr_wspatr";

    private final List<String> missingFiles = new ArrayList<>();
    private final List<String> missingFilesInactive = new ArrayList<>();
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
        List<File> dirs = new ArrayList<>();
        String[] dirNames = (p_BaseDirectory != null && !p_BaseDirectory.trim().isEmpty())
                ? p_BaseDirectory.split(",")
                : DEFAULT_BASE_DIRS;
        for (String dirName : dirNames) {
            File dir = new File(dirName.trim());
            if (!dir.isDirectory()) {
                throw new AdempiereException("Base directory does not exist or is not a directory: " + dirName);
            }
            dirs.add(dir);
        }
        long maxRows = p_MaxRows != null ? p_MaxRows.longValue() : 0L;

        addLog("Searching " + dirs.size() + " director(ies) in order: " + dirs);

        List<String> tables = findDocumentTables();
        addLog("Found " + tables.size() + " SDR_ table(s) with " + SAVED_FILENAME_COL + ": " + tables);

        int totalProcessed = 0;
        int totalAttached = 0;
        int totalAlready = 0;
        int totalMissing = 0;
        int totalEmptyOnDisk = 0;
        int totalErrors = 0;

        for (String tableName : tables) {
            int[] stats = processTable(tableName, dirs, maxRows);
            totalProcessed += stats[0];
            totalAttached += stats[1];
            totalAlready += stats[2];
            totalMissing += stats[3];
            totalEmptyOnDisk += stats[4];
            totalErrors += stats[5];
        }

        int orphanCount = findOrphanFiles(dirs, tables);

        attachListIfAny(missingFiles, "load-sdr-attachments-missing-files");
        attachListIfAny(missingFilesInactive, "load-sdr-attachments-missing-files-deleted-records");
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

    /**
     * Walks the declared chain for this table, verifying each hop's fkColumn/parentTable actually
     * exist before trusting it. Stops (and logs) at the first unverifiable hop, since every
     * subsequent hop depends on the join reaching that point.
     */
    private List<ParentHop> buildVerifiedChain(String tableName) {
        List<ParentHop> chain = PARENT_CHAINS.get(tableName.toLowerCase());
        if (chain == null) {
            return java.util.Collections.emptyList();
        }
        List<ParentHop> verified = new ArrayList<>();
        String currentTable = tableName;
        for (ParentHop hop : chain) {
            if (columnExists(currentTable, hop.fkColumn) && tableExists(hop.parentTable)
                    && columnExists(hop.parentTable, hop.fkColumn)) {
                verified.add(hop);
                currentTable = hop.parentTable;
            } else {
                addLog(tableName + ": expected parent hop (" + currentTable + "." + hop.fkColumn + " -> "
                        + hop.parentTable + ") not found - parent chain stops here for this table.");
                break;
            }
        }
        return verified;
    }

    private boolean columnExists(String tableName, String columnName) {
        long count = DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = '" + tableName
                        + "' AND column_name = '" + columnName + "'");
        return count > 0;
    }

    private boolean tableExists(String tableName) {
        long count = DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'");
        return count > 0;
    }

    /** source id (sdr_wspatr.id) -&gt; isActive, for the CSV parent check below. */
    private Map<Integer, Boolean> buildWspatrActiveMap() {
        Map<Integer, Boolean> map = new HashMap<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(
                    "SELECT id, " + ISACTIVE_COL + " FROM " + WSPATR_TABLE + " WHERE id IS NOT NULL",
                    get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                map.put(rs.getInt(1), !"N".equals(rs.getString(2)));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed loading " + WSPATR_TABLE + " active status map", e);
        } finally {
            DB.close(rs, pstmt);
        }
        return map;
    }

    /**
     * sdr_wspatrids is a CSV of sdr_wspatr.id (source ids, not the surrogate PK - see
     * WSPATR_CSV_TABLE's Javadoc). The parent is considered deleted only if EVERY id that actually
     * resolves against {@code activeMap} is inactive; an id that doesn't resolve at all is ignored
     * (not evidence either way), and if NOTHING resolves, the conservative default is "active" (no
     * evidence of deletion) rather than wrongly suppressing a genuine missing-file report.
     */
    private static boolean computeCsvParentActive(String csv, Map<Integer, Boolean> activeMap) {
        if (csv == null || csv.trim().isEmpty()) {
            return true;
        }
        boolean anyResolved = false;
        boolean anyActive = false;
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                Boolean active = activeMap.get(Integer.parseInt(trimmed));
                if (active != null) {
                    anyResolved = true;
                    if (active) {
                        anyActive = true;
                    }
                }
            } catch (NumberFormatException e) {
                // Not a plain integer token - ignore, doesn't count as evidence either way.
            }
        }
        return !anyResolved || anyActive;
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

    private int[] processTable(String tableName, List<File> dirs, long maxRows) throws Exception {
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

        List<ParentHop> parentChain = buildVerifiedChain(tableName);
        boolean hasParentCheck = !parentChain.isEmpty();

        boolean isCsvParentCheck = tableName.equalsIgnoreCase(WSPATR_CSV_TABLE)
                && columnExists(tableName, WSPATR_CSV_COLUMN) && tableExists(WSPATR_TABLE)
                && columnExists(WSPATR_TABLE, "id") && columnExists(WSPATR_TABLE, ISACTIVE_COL);
        Map<Integer, Boolean> wspatrActiveMap = null;
        if (isCsvParentCheck) {
            wspatrActiveMap = buildWspatrActiveMap();
            addLog(tableName + ": CSV parent check enabled via " + WSPATR_CSV_COLUMN + " -> " + WSPATR_TABLE
                    + ".id (" + wspatrActiveMap.size() + " source id(s) loaded).");
        } else if (tableName.equalsIgnoreCase(WSPATR_CSV_TABLE)) {
            addLog(tableName + ": expected CSV parent column/table (" + WSPATR_CSV_COLUMN + " / " + WSPATR_TABLE
                    + ") not found - parent delete-status check skipped for this table.");
        }

        // "id" is the plain reconciliation column every migration class stamped with the ORIGINAL
        // mssdr_* source row id (see AddColumnsSupport#createReconIdColumn) - unrelated to the
        // table's own surrogate primary key (keyColumn, e.g. sdr_organisationdocuments_id). Both get
        // logged, clearly labelled, so a missing/error entry can be cross-referenced either against
        // this Postgres table (by pk) or against the original MS SQL source table (by source id).
        boolean hasReconId = columnExists(tableName, "id");
        String reconIdSelect = hasReconId ? ", " + (hasParentCheck ? "d." : "") + "id AS reconid" : "";

        String sql;
        if (hasParentCheck) {
            StringBuilder joinSql = new StringBuilder();
            StringBuilder parentSelectSql = new StringBuilder();
            String prevAlias = "d";
            int hopIdx = 0;
            for (ParentHop hop : parentChain) {
                hopIdx++;
                String alias = "p" + hopIdx;
                joinSql.append(" LEFT JOIN ").append(hop.parentTable).append(' ').append(alias).append(" ON ")
                        .append(prevAlias).append('.').append(hop.fkColumn).append(" = ").append(alias).append('.')
                        .append(hop.fkColumn);
                parentSelectSql.append(", ").append(alias).append('.').append(ISACTIVE_COL).append(" AS parent_active_")
                        .append(hopIdx);
                prevAlias = alias;
            }
            sql = "SELECT d." + keyColumn + " AS pk, d." + SAVED_FILENAME_COL + " AS saved, d."
                    + ORIGINAL_FILENAME_COL + " AS original, d." + ISACTIVE_COL + " AS active" + parentSelectSql
                    + reconIdSelect + " FROM " + tableName + " d" + joinSql + " WHERE d." + SAVED_FILENAME_COL
                    + " IS NOT NULL AND d." + SAVED_FILENAME_COL + " <> '' ORDER BY d." + keyColumn
                    + (maxRows > 0 ? " LIMIT " + maxRows : "");
        } else {
            String csvSelect = isCsvParentCheck ? ", " + WSPATR_CSV_COLUMN + " AS wspatrids" : "";
            sql = "SELECT " + keyColumn + " AS pk, " + SAVED_FILENAME_COL + " AS saved, " + ORIGINAL_FILENAME_COL
                    + " AS original, " + ISACTIVE_COL + " AS active" + reconIdSelect + csvSelect + " FROM "
                    + tableName + " WHERE " + SAVED_FILENAME_COL + " IS NOT NULL AND " + SAVED_FILENAME_COL
                    + " <> '' ORDER BY " + keyColumn + (maxRows > 0 ? " LIMIT " + maxRows : "");
        }

        int processed = 0;
        int attached = 0;
        int already = 0;
        int missing = 0;
        int missingInactive = 0;
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
                boolean ownActive = !"N".equals(rs.getString("active"));
                boolean parentActive;
                if (hasParentCheck) {
                    // Deleted if ANY ancestor in the chain is inactive, not just the direct parent.
                    parentActive = true;
                    for (int hopIdx = 1; hopIdx <= parentChain.size(); hopIdx++) {
                        if ("N".equals(rs.getString("parent_active_" + hopIdx))) {
                            parentActive = false;
                            break;
                        }
                    }
                } else if (isCsvParentCheck) {
                    parentActive = computeCsvParentActive(rs.getString("wspatrids"), wspatrActiveMap);
                } else {
                    parentActive = true;
                }
                boolean isActive = ownActive && parentActive;
                String rowRef = tableName + ".pk=" + pk
                        + (hasReconId ? " source_id=" + rs.getInt("reconid") : "");

                try {
                    MAttachment existing = MAttachment.get(getCtx(), table.getAD_Table_ID(), pk, get_TrxName());
                    if (existing != null && existing.getEntryCount() > 0) {
                        already++;
                        continue;
                    }

                    File file = findInDirs(dirs, saved.trim());
                    if (file == null) {
                        if (isActive) {
                            missing++;
                            addToListCapped(missingFiles, rowRef + " file=" + saved + " original=[" + original
                                    + "]");
                        } else {
                            // Record itself is soft-deleted (isactive='N') - a missing file here is
                            // expected, not a loader problem, so it's attached as its own separate
                            // report rather than mixed into the "needs investigation" missing-files list.
                            missingInactive++;
                            addToListCapped(missingFilesInactive,
                                    rowRef + " file=" + saved + " original=[" + original + "]");
                        }
                        continue;
                    }
                    if (file.length() == 0) {
                        // Genuinely empty/corrupt source file on disk - not a loader bug, nothing to
                        // attach. Tracked separately from "missing" (file is there, just 0 bytes) and
                        // from "errors" (not a save failure - we never attempt the save).
                        emptyOnDisk++;
                        addToListCapped(emptyOnDiskFiles,
                                rowRef + " file=" + file.getAbsolutePath() + " original=[" + original + "]");
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
                    addToListCapped(errors, rowRef + " file=" + saved + " entryName=[" + original + "] bytes="
                            + describeFileSize(dirs, saved) + ": " + describeError(e));
                }
            }
        } finally {
            DB.close(rs, pstmt);
        }

        addLog(tableName + ": processed " + processed + ", attached " + attached + ", already " + already
                + ", missing " + missing + " (+ " + missingInactive + " missing but record inactive/deleted), "
                + "empty-on-disk " + emptyOnDisk + ", errors " + errorCount);

        return new int[] { processed, attached, already, missing + missingInactive, emptyOnDisk, errorCount };
    }

    /** Searches each directory in order, returning the first match or null if none has the file. */
    private static File findInDirs(List<File> dirs, String fileName) {
        for (File dir : dirs) {
            File candidate = new File(dir, fileName);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Reverse check: files physically present in the directory that no row in any document table
     * references at all (by sdr_savedfilename) - e.g. leftovers from a partial copy, renamed files,
     * or files for records that were never migrated. Deliberately queries the FULL referenced-name
     * set per table (no MaxRows limit) regardless of how the main attach loop was bounded, since a
     * MaxRows test run must not report every not-yet-processed file as "orphaned".
     */
    private int findOrphanFiles(List<File> dirs, List<String> tables) {
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

        int orphanCount = 0;
        for (File dir : dirs) {
            File[] diskFiles = dir.listFiles();
            if (diskFiles == null) {
                continue;
            }
            for (File f : diskFiles) {
                if (!f.isFile()) {
                    continue;
                }
                if (!referenced.contains(f.getName())) {
                    orphanCount++;
                    addToListCapped(orphanFiles, f.getAbsolutePath() + " (" + f.length() + " bytes)");
                }
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

    private static String describeFileSize(List<File> dirs, String saved) {
        File file = findInDirs(dirs, saved.trim());
        return file != null ? String.valueOf(file.length()) : "?";
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
