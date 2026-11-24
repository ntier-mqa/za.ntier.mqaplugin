package za.co.ntier.sdr.process;

import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.compiere.model.I_AD_PInstance;
import org.compiere.model.MAttachment;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Reconciliation report: compares every staged MS SQL source table (mssdr_*) against its migrated
 * Postgres target table (sdr_*), with row counts on both sides, exported as a real .xlsx workbook via
 * Apache POI (already a working dependency of this plugin - see the ~40 existing
 * za.co.ntier.wsp_atr.process/za.ntier.process classes that import org.apache.poi.*).
 *
 * <p>Tables are discovered DYNAMICALLY from information_schema.tables (both prefixes), not from a
 * hardcoded list of every table this migration touched - this makes the report self-correcting: any
 * mssdr_* table that genuinely has no migrated counterpart shows up as "NOT MIGRATED" in its own right,
 * rather than the report silently agreeing with whatever this project's own code already assumed.
 *
 * <p>MATCHING STRATEGY: the ~168 main-family tables (Person/Organisation/SDF/WSPATR/Levy/Grant/User-
 * Security/Misc) all follow a simple, verified-reliable naming convention - "mssdr_x" -&gt; "sdr_x", a
 * plain prefix swap with the remainder unchanged (every single Add*Table.java class in this project
 * used exactly this pattern). The 72 reference/catalog tables do NOT follow that pattern (e.g.
 * "mssdr_lkptitle" -&gt; "sdr_title", "mssdr_wsplearningprogramme" -&gt; "sdr_learningprogramme") - for
 * those, this class reuses {@link AddSDRReferenceTables#SPECS} (the same {target, source} pairs that
 * built them) rather than re-deriving or re-typing the mapping a second time.
 *
 * <p>ATTACHMENTS: a second tab, "SDR Attachments", reconciles the document files loaded by
 * {@link LoadSDRDocumentAttachments}: per sdr_ table that has an sdr_savedfilename column, how many rows
 * carry a file name vs. how many have an attachment on their record. Headline totals are repeated on the
 * Reconciliation tab's summary block.
 *
 * <p>OUTPUT: attached to this process run's own AD_PInstance record (2026-09-16, mirrors
 * BulkLoadSgDocuments' {@code new MAttachment(ctx, tableId, recordId, null, trxName)} pattern) - the
 * user downloads it from the paperclip/Attachment icon on the Process Audit window for this run,
 * rather than needing server filesystem access to fetch a /tmp path.
 */
@Process(name = "za.co.ntier.sdr.process.ExportSDRReconciliationReport")
public class ExportSDRReconciliationReport extends SvrProcess {

    private static final String SOURCE_PREFIX = "mssdr_";
    private static final String TARGET_PREFIX = "sdr_";
    private static final Set<String> EXCLUDED_ID_COLUMNS = new HashSet<>(
            Arrays.asList("id", "ad_client_id", "ad_org_id"));

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    private static final class ReconRow {
        String sourceTable;
        String targetTable;
        long sourceCount;
        Long targetCount;
    }

    private static final String SAVED_FILENAME_COL = "sdr_savedfilename";

    /** Per SDR_ document table: how many rows carry a file name vs. have an attachment loaded. */
    private static final class AttachRow {
        String tableName;
        long totalRows;
        long withFileName;
        long attached;
        /** Rows with a file name but no attachment whose own record, or an ancestor, is inactive/deleted. */
        long noAttachDeleted;
        /** Active record, no attachment, and the file is not in any search directory. */
        long activeNotFound;
        /** Active record, no attachment, file exists but is 0 bytes (the loader skips these). */
        long activeZeroByte;
        /** Active record, no attachment, file exists with content - should have loaded (check loader errors). */
        long activeOnDisk;
    }

    /** Directories searched for the disk check - same list the loader defaults to; empty = unavailable. */
    private final List<java.io.File> diskDirs = new ArrayList<>();

    @Override
    protected String doIt() throws Exception {
        Set<String> targetTables = queryTableNames(TARGET_PREFIX + "%");
        List<String> sourceTables = new ArrayList<>(queryTableNames(SOURCE_PREFIX + "%"));
        Collections.sort(sourceTables);

        Map<String, String> knownReferenceMapping = buildKnownReferenceMapping();

        List<ReconRow> rows = new ArrayList<>();
        int matched = 0;
        int mismatched = 0;
        int notMigrated = 0;

        for (String sourceTable : sourceTables) {
            ReconRow row = new ReconRow();
            row.sourceTable = sourceTable;
            row.sourceCount = countRows(sourceTable);

            String candidate = TARGET_PREFIX + sourceTable.substring(SOURCE_PREFIX.length());
            String targetTable = targetTables.contains(candidate) ? candidate
                    : knownReferenceMapping.get(sourceTable);
            if (targetTable != null && !targetTables.contains(targetTable)) {
                targetTable = null;
            }

            if (targetTable != null) {
                row.targetTable = targetTable;
                row.targetCount = countRows(targetTable);
                if (row.targetCount.longValue() == row.sourceCount) {
                    matched++;
                } else {
                    mismatched++;
                }
            } else {
                notMigrated++;
            }
            rows.add(row);
        }

        List<AttachRow> attachRows = loadAttachmentStats();
        long attWithName = attachRows.stream().mapToLong(a -> a.withFileName).sum();
        long attLoaded = attachRows.stream().mapToLong(a -> a.attached).sum();

        Set<String> sourceTableSet = new HashSet<>(sourceTables);
        String fileName = attachExcel(rows, sourceTables.size(), targetTables.size(), matched, mismatched,
                notMigrated, sourceTableSet, attachRows);

        return "SDR reconciliation: " + sourceTables.size() + " MS SQL (mssdr_) table(s), "
                + targetTables.size() + " Postgres (sdr_) table(s). " + matched + " matched, " + mismatched
                + " count mismatch, " + notMigrated + " not migrated. Document attachments loaded: "
                + attLoaded + " of " + attWithName + " rows with a saved file name. Download " + fileName
                + " from the Attachment icon on this Process Audit record.";
    }

    private Set<String> queryTableNames(String likePattern) {
        Set<String> names = new HashSet<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(
                    "SELECT table_name FROM information_schema.tables WHERE table_name LIKE ?", get_TrxName());
            pstmt.setString(1, likePattern);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new AdempiereException("Failed listing tables matching " + likePattern, e);
        } finally {
            DB.close(rs, pstmt);
        }
        return names;
    }

    /**
     * Same table discovery as {@link LoadSDRDocumentAttachments} (every sdr_ table with an
     * sdr_savedfilename column), then per table: total rows, rows with a non-blank file name, and how
     * many of those have an AD_Attachment row on their record. Counts the AD_Attachment row's existence
     * rather than its entries - cheap in SQL, and the loader only creates the row when it adds an entry.
     * Run this under the same client the loader ran under (MQA): AD_Attachment is client-scoped.
     */
    private List<AttachRow> loadAttachmentStats() throws Exception {
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
        } finally {
            DB.close(rs, pstmt);
        }

        for (String dirName : LoadSDRDocumentAttachments.DEFAULT_BASE_DIRS) {
            java.io.File dir = new java.io.File(dirName);
            if (dir.isDirectory()) {
                diskDirs.add(dir);
            } else {
                addLog("Attachment recon: directory not found, not searched: " + dirName);
            }
        }

        List<AttachRow> result = new ArrayList<>();
        String nonBlank = SAVED_FILENAME_COL + " IS NOT NULL AND " + SAVED_FILENAME_COL + " <> ''";
        for (String tableName : tables) {
            MTable table = AddColumnsSupport.findTable(getCtx(), tableName, get_TrxName());
            String[] keys = table == null ? null : table.getKeyColumns();
            if (keys == null || keys.length != 1) {
                addLog(tableName + ": no AD_Table / single key column - skipped in attachment recon.");
                continue;
            }
            AttachRow ar = new AttachRow();
            ar.tableName = tableName;
            countAttachmentRow(ar, table, keys[0], nonBlank);
            result.add(ar);
        }
        return result;
    }

    /**
     * Fills total / with-file-name / attached, and classifies the no-attachment rows (deleted vs active; active split by disk state), for one table. "Deleted"
     * uses the SAME rule as {@link LoadSDRDocumentAttachments}: the document row's own isactive = 'N',
     * OR any ancestor in {@link LoadSDRDocumentAttachments#PARENT_CHAINS} has isactive = 'N'. The
     * many-to-many sdr_wspatrbulkapprovaldocumentupload is classified row-by-row in Java via its
     * sdr_wspatrids CSV (same helper as the loader).
     */
    private void countAttachmentRow(AttachRow ar, MTable table, String keyColumn, String nonBlank) throws Exception {
        String tableName = ar.tableName;
        String attachedExists = "EXISTS (SELECT 1 FROM ad_attachment a WHERE a.ad_table_id = ? AND a.record_id = d."
                + keyColumn + ")";
        String nb = prefixed(nonBlank);

        boolean hasIsActive = columnExists(tableName, "isactive");
        List<LoadSDRDocumentAttachments.ParentHop> chain = verifiedChain(tableName);
        StringBuilder joins = new StringBuilder();
        StringBuilder deletedCond = new StringBuilder(hasIsActive ? "d.isactive = 'N'" : "FALSE");
        String prevAlias = "d";
        int hopIdx = 0;
        for (LoadSDRDocumentAttachments.ParentHop hop : chain) {
            String alias = "p" + (++hopIdx);
            joins.append(" LEFT JOIN ").append(hop.parentTable).append(' ').append(alias).append(" ON ")
                    .append(prevAlias).append('.').append(hop.fkColumn).append(" = ").append(alias).append('.')
                    .append(hop.fkColumn);
            deletedCond.append(" OR ").append(alias).append(".isactive = 'N'");
            prevAlias = alias;
        }
        boolean csvCheck = tableName.equalsIgnoreCase(LoadSDRDocumentAttachments.WSPATR_CSV_TABLE)
                && columnExists(tableName, LoadSDRDocumentAttachments.WSPATR_CSV_COLUMN)
                && tableExists(LoadSDRDocumentAttachments.WSPATR_TABLE)
                && columnExists(LoadSDRDocumentAttachments.WSPATR_TABLE, "id")
                && columnExists(LoadSDRDocumentAttachments.WSPATR_TABLE, "isactive");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement("SELECT COUNT(*), "
                    + "SUM(CASE WHEN " + nb + " THEN 1 ELSE 0 END), "
                    + "SUM(CASE WHEN (" + nb + ") AND " + attachedExists + " THEN 1 ELSE 0 END) "
                    + "FROM " + tableName + " d", get_TrxName());
            pstmt.setInt(1, table.getAD_Table_ID());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                ar.totalRows = rs.getLong(1);
                ar.withFileName = rs.getLong(2); // SUM over zero rows is NULL -> 0
                ar.attached = rs.getLong(3);
            }
        } finally {
            DB.close(rs, pstmt);
        }

        // The no-attachment rows themselves (a few thousand at most): classify each as deleted or
        // active, and for active ones check the disk.
        Map<Integer, Boolean> wspatrActive = csvCheck ? loadWspatrActiveMap() : null;
        String csvSelect = csvCheck ? ", d." + LoadSDRDocumentAttachments.WSPATR_CSV_COLUMN : "";
        try {
            pstmt = DB.prepareStatement("SELECT d." + SAVED_FILENAME_COL + ", CASE WHEN (" + deletedCond
                    + ") THEN 1 ELSE 0 END" + csvSelect + " FROM " + tableName + " d" + joins + " WHERE (" + nb
                    + ") AND NOT " + attachedExists, get_TrxName());
            pstmt.setInt(1, table.getAD_Table_ID());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String saved = rs.getString(1);
                boolean deleted = rs.getInt(2) == 1;
                if (csvCheck && !deleted) {
                    deleted = !LoadSDRDocumentAttachments.computeCsvParentActive(rs.getString(3), wspatrActive);
                }
                if (deleted) {
                    ar.noAttachDeleted++;
                } else if (diskDirs.isEmpty()) {
                    ar.activeNotFound++; // disk not available - reported as blank cells, see sheet note
                } else {
                    java.io.File file = LoadSDRDocumentAttachments.findInDirs(diskDirs, saved.trim());
                    if (file == null) {
                        ar.activeNotFound++;
                    } else if (file.length() == 0) {
                        ar.activeZeroByte++;
                    } else {
                        ar.activeOnDisk++;
                    }
                }
            }
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /** source id (sdr_wspatr.id) -> isActive, for the CSV-parent classification. */
    private Map<Integer, Boolean> loadWspatrActiveMap() throws Exception {
        Map<Integer, Boolean> activeMap = new HashMap<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement("SELECT id, isactive FROM " + LoadSDRDocumentAttachments.WSPATR_TABLE
                    + " WHERE id IS NOT NULL", get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                activeMap.put(rs.getInt(1), !"N".equals(rs.getString(2)));
            }
        } finally {
            DB.close(rs, pstmt);
        }
        return activeMap;
    }

    /** nonBlank is built unqualified; qualify it with the "d" alias used by the joined query. */
    private static String prefixed(String nonBlank) {
        return nonBlank.replace(SAVED_FILENAME_COL, "d." + SAVED_FILENAME_COL);
    }

    /** Same walk as the loader's buildVerifiedChain: stops at the first hop that can't be verified. */
    private List<LoadSDRDocumentAttachments.ParentHop> verifiedChain(String tableName) {
        List<LoadSDRDocumentAttachments.ParentHop> chain = LoadSDRDocumentAttachments.PARENT_CHAINS
                .get(tableName.toLowerCase());
        List<LoadSDRDocumentAttachments.ParentHop> verified = new ArrayList<>();
        if (chain == null) {
            return verified;
        }
        String currentTable = tableName;
        for (LoadSDRDocumentAttachments.ParentHop hop : chain) {
            if (columnExists(currentTable, hop.fkColumn) && tableExists(hop.parentTable)
                    && columnExists(hop.parentTable, hop.fkColumn) && columnExists(hop.parentTable, "isactive")) {
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
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
                tableName, columnName) > 0;
    }

    private boolean tableExists(String tableName) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", tableName) > 0;
    }

    private long countRows(String tableName) {
        return DB.getSQLValueEx(get_TrxName(), "SELECT COUNT(*) FROM " + tableName);
    }

    /**
     * Reuses {@link AddSDRReferenceTables#SPECS} (the same {target, source} pairs used to build the 72
     * reference tables) rather than re-deriving or re-typing that irregular naming a second time.
     */
    private Map<String, String> buildKnownReferenceMapping() {
        Map<String, String> mapping = new HashMap<>();
        for (String[] spec : AddSDRReferenceTables.SPECS) {
            mapping.put(spec[1].toLowerCase(), spec[0].toLowerCase());
        }
        // mssdr_lkpsiccodechamber is deliberately NOT in AddSDRReferenceTables.SPECS (it's a
        // SICCode<->ChamberCode junction table, not a Value/Name lookup - see
        // AddSDRSICCodeChamberTable's Javadoc), so it needs its own explicit override here or it
        // would wrongly show as "NOT MIGRATED" despite sdr_siccodechamber existing and matching.
        mapping.put("mssdr_lkpsiccodechamber", "sdr_siccodechamber");
        return mapping;
    }

    private String attachExcel(List<ReconRow> rows, int sourceTableCount, int targetTableCount, int matched,
            int mismatched, int notMigrated, Set<String> knownSourceTables, List<AttachRow> attachRows)
            throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reconciliation");

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldFont);
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SDR Migration Reconciliation - MS SQL (staged) vs Postgres (SDR_)");
            titleCell.setCellStyle(titleStyle);

            rowIdx++;
            writeSummaryLine(sheet, rowIdx++, "MS SQL (mssdr_) tables:", sourceTableCount);
            writeSummaryLine(sheet, rowIdx++, "Postgres (sdr_) tables:", targetTableCount);
            writeSummaryLine(sheet, rowIdx++, "Matched (counts equal):", matched);
            writeSummaryLine(sheet, rowIdx++, "Count mismatch:", mismatched);
            writeSummaryLine(sheet, rowIdx++, "Not migrated (no target table found):", notMigrated);
            long attWithName = attachRows.stream().mapToLong(a -> a.withFileName).sum();
            long attLoaded = attachRows.stream().mapToLong(a -> a.attached).sum();
            writeSummaryLine(sheet, rowIdx++, "Document tables with saved file name:", attachRows.size());
            writeSummaryLine(sheet, rowIdx++, "Rows with a saved file name:", attWithName);
            writeSummaryLine(sheet, rowIdx++, "...with attachment loaded:", attLoaded);
            long attDeleted = attachRows.stream().mapToLong(a -> a.noAttachDeleted).sum();
            writeSummaryLine(sheet, rowIdx++, "...without attachment (see 'SDR Attachments' tab):",
                    attWithName - attLoaded);
            writeSummaryLine(sheet, rowIdx++, "......record deleted/inactive:", attDeleted);
            long attNotFound = attachRows.stream().mapToLong(a -> a.activeNotFound).sum();
            long attZero = attachRows.stream().mapToLong(a -> a.activeZeroByte).sum();
            long attOnDisk = attachRows.stream().mapToLong(a -> a.activeOnDisk).sum();
            writeSummaryLine(sheet, rowIdx++, "......record active (investigate):",
                    attWithName - attLoaded - attDeleted);
            if (!diskDirs.isEmpty()) {
                writeSummaryLine(sheet, rowIdx++, ".........file not found on disk:", attNotFound);
                writeSummaryLine(sheet, rowIdx++, ".........file is 0 bytes on disk:", attZero);
                writeSummaryLine(sheet, rowIdx++, ".........file on disk but not attached (check errors):", attOnDisk);
            }
            rowIdx++;

            Row header = sheet.createRow(rowIdx++);
            String[] headers = { "MS SQL Table", "Postgres Table", "MS SQL Row Count", "Postgres Row Count",
                    "Difference", "Status" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ReconRow r : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.sourceTable);
                row.createCell(1).setCellValue(r.targetTable != null ? r.targetTable : "(not migrated)");
                row.createCell(2).setCellValue(r.sourceCount);
                if (r.targetCount != null) {
                    row.createCell(3).setCellValue(r.targetCount);
                    row.createCell(4).setCellValue(r.targetCount - r.sourceCount);
                    row.createCell(5).setCellValue(r.targetCount.longValue() == r.sourceCount ? "OK" : "MISMATCH");
                } else {
                    row.createCell(3);
                    row.createCell(4);
                    row.createCell(5).setCellValue("NOT MIGRATED");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Set<String> usedSheetNames = new HashSet<>();
            usedSheetNames.add(sheet.getSheetName());
            usedSheetNames.add("SDR Attachments");
            addAttachmentsSheet(workbook, headerStyle, attachRows);
            for (ReconRow r : rows) {
                if (r.targetTable != null && r.targetCount != null && r.targetCount.longValue() != r.sourceCount) {
                    addMissingRowsSheet(workbook, headerStyle, r.sourceTable, r.targetTable, usedSheetNames,
                            knownSourceTables);
                }
            }

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "sdr-reconciliation-" + ts + ".xlsx";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            MAttachment attachment = new MAttachment(getCtx(), I_AD_PInstance.Table_ID, getAD_PInstance_ID(), null,
                    get_TrxName());
            attachment.addEntry(fileName, out.toByteArray());
            attachment.saveEx();

            return fileName;
        }
    }

    private void addAttachmentsSheet(XSSFWorkbook workbook, CellStyle headerStyle, List<AttachRow> rows) {
        Sheet sheet = workbook.createSheet("SDR Attachments");
        String[] headers = { "Table", "Total Rows", "Rows With File Name", "Attachment Loaded",
                "No Attachment", "No Attachment - Record Deleted/Inactive",
                "No Attachment - Record Active (investigate)", "Active - File Not Found", "Active - File 0 Bytes",
                "Active - File On Disk, Not Attached" };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        long totRows = 0;
        long totName = 0;
        long totAttached = 0;
        long totDeleted = 0;
        long totNotFound = 0;
        long totZero = 0;
        long totOnDisk = 0;
        boolean disk = !diskDirs.isEmpty();
        int rowIdx = 1;
        for (AttachRow ar : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(ar.tableName);
            row.createCell(1).setCellValue(ar.totalRows);
            row.createCell(2).setCellValue(ar.withFileName);
            row.createCell(3).setCellValue(ar.attached);
            row.createCell(4).setCellValue(ar.withFileName - ar.attached);
            row.createCell(5).setCellValue(ar.noAttachDeleted);
            row.createCell(6).setCellValue(ar.withFileName - ar.attached - ar.noAttachDeleted);
            if (disk) {
                row.createCell(7).setCellValue(ar.activeNotFound);
                row.createCell(8).setCellValue(ar.activeZeroByte);
                row.createCell(9).setCellValue(ar.activeOnDisk);
            }
            totNotFound += ar.activeNotFound;
            totZero += ar.activeZeroByte;
            totOnDisk += ar.activeOnDisk;
            totRows += ar.totalRows;
            totName += ar.withFileName;
            totAttached += ar.attached;
            totDeleted += ar.noAttachDeleted;
        }
        Row total = sheet.createRow(rowIdx);
        Cell label = total.createCell(0);
        label.setCellValue("TOTAL");
        label.setCellStyle(headerStyle);
        total.createCell(1).setCellValue(totRows);
        total.createCell(2).setCellValue(totName);
        total.createCell(3).setCellValue(totAttached);
        total.createCell(4).setCellValue(totName - totAttached);
        total.createCell(5).setCellValue(totDeleted);
        total.createCell(6).setCellValue(totName - totAttached - totDeleted);
        if (disk) {
            total.createCell(7).setCellValue(totNotFound);
            total.createCell(8).setCellValue(totZero);
            total.createCell(9).setCellValue(totOnDisk);
        }
        Row note = sheet.createRow(rowIdx + 2);
        note.createCell(0).setCellValue("Deleted/Inactive = the document row, or its parent record (Organisation / "
                + "SDF Organisation / SDF / Person / WSPATR), has isactive = N. The three 'Active - ...' columns "
                + "split 'Record Active' by what is on disk in the loader's search directories: "
                + (disk ? diskDirs.toString() : "(NONE FOUND - disk check unavailable on this server, cells left blank)")
                + ". 'File 0 Bytes' files are skipped by the loader.");
        sheet.createFreezePane(0, 1);
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * For a MISMATCH table, dumps every source row whose id has no counterpart in the target table
     * into its own sheet - full column-for-column via ResultSetMetaData (no per-table column
     * knowledge needed here) - plus a derived "Likely Reason" column (see
     * {@link #computeLikelyReason}) explaining WHY each row didn't migrate, so a human doesn't have
     * to eyeball raw FK values to spot the sentinel/orphan pattern themselves.
     */
    private void addMissingRowsSheet(XSSFWorkbook workbook, CellStyle headerStyle, String sourceTable,
            String targetTable, Set<String> usedSheetNames, Set<String> knownSourceTables) throws Exception {
        String sql = "SELECT a.* FROM " + sourceTable + " a WHERE NOT EXISTS "
                + "(SELECT 1 FROM " + targetTable + " t WHERE t.id = a.id) ORDER BY a.id LIMIT 2000";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            rs = pstmt.executeQuery();
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            String[] columnNames = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = meta.getColumnName(i);
            }

            Sheet sheet = workbook.createSheet(uniqueSheetName(sourceTable, usedSheetNames));
            Row header = sheet.createRow(0);
            for (int i = 0; i < columnCount; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnNames[i]);
                cell.setCellStyle(headerStyle);
            }
            Cell reasonHeaderCell = header.createCell(columnCount);
            reasonHeaderCell.setCellValue("Likely Reason");
            reasonHeaderCell.setCellStyle(headerStyle);

            int rowIdx = 1;
            while (rs.next()) {
                Object[] values = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    values[i - 1] = rs.getObject(i);
                }
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columnCount; i++) {
                    row.createCell(i).setCellValue(values[i] == null ? "" : String.valueOf(values[i]));
                }
                row.createCell(columnCount).setCellValue(computeLikelyReason(columnNames, values, knownSourceTables));
            }
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Heuristic, dynamically-derived explanation for why a row has no migrated counterpart: scans
     * every *id-suffixed column (skipping id/ad_client_id/ad_org_id) and flags it as a likely cause
     * if its value is a non-positive "sentinel" (0/-1/null - this migration's convention for "no
     * parent") or, for a positive value, if it doesn't actually exist as an "id" in the plausibly-
     * named source table ("personid" -&gt; mssdr_person). The guessed source table name is checked
     * against {@code knownSourceTables} first so an unmatched guess (e.g. "roleid" -&gt;
     * mssdr_role, when the real table is mssdr_lkprole) is silently skipped rather than reported
     * wrong.
     */
    private String computeLikelyReason(String[] columnNames, Object[] values, Set<String> knownSourceTables) {
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < columnNames.length; i++) {
            String colName = columnNames[i].toLowerCase();
            if (!colName.endsWith("id") || EXCLUDED_ID_COLUMNS.contains(colName)) {
                continue;
            }
            Object value = values[i];
            if (value == null) {
                continue;
            }
            String valueStr = String.valueOf(value).trim();
            long numericValue;
            try {
                numericValue = Long.parseLong(valueStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (numericValue <= 0) {
                reasons.add(colName + "=" + numericValue + " (sentinel/no-parent value)");
                continue;
            }
            String guessedTable = SOURCE_PREFIX + colName.substring(0, colName.length() - 2);
            if (!knownSourceTables.contains(guessedTable)) {
                continue;
            }
            long count = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM " + guessedTable + " WHERE id = " + numericValue);
            if (count == 0) {
                reasons.add(colName + "=" + numericValue + " not found in " + guessedTable
                        + " (orphaned source reference)");
            }
        }
        return String.join("; ", reasons);
    }

    /** Excel sheet names: max 31 chars, must be unique within the workbook. */
    private String uniqueSheetName(String sourceTable, Set<String> usedSheetNames) {
        String base = "Missing_" + sourceTable;
        String candidate = base.length() > 31 ? base.substring(0, 31) : base;
        int suffix = 1;
        while (!usedSheetNames.add(candidate)) {
            String suffixStr = "_" + (++suffix);
            int cut = Math.min(base.length(), 31 - suffixStr.length());
            candidate = base.substring(0, cut) + suffixStr;
        }
        return candidate;
    }

    private void writeSummaryLine(Sheet sheet, int rowIdx, String label, long value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }
}
