package za.co.ntier.wsp_atr.process;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import org.compiere.model.Query;

/**
 * Read-only diagnostic companion to {@link ImportWspAtrMigrationFile}.
 *
 * <p>Given an SDL number, streams the same source file and reports exactly what
 * WSP status {@code ImportWspAtrMigrationFile} would assign to the
 * {@code ZZ_WSP_ATR_Submitted} record for that SDL — without touching the
 * database.</p>
 *
 * <p>The report covers:</p>
 * <ul>
 *   <li>The raw WSPStatus text found in the spreadsheet for the given SDL.</li>
 *   <li>Whether that text resolved to an {@code AD_Ref_List} value, and if so,
 *       which value and name.</li>
 *   <li>The "fallback" status (the first resolved status from any SDL in the
 *       file) that the importer would apply when this SDL's own lookup fails.</li>
 *   <li>The {@code ZZSdfOrganisation_ID} for the SDL (the key used during the
 *       import pass to retrieve the resolved status).</li>
 *   <li>The final status the importer would have written.</li>
 * </ul>
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.wsp_atr.process.DiagnoseWspAtrMigrationStatus")
public class DiagnoseWspAtrMigrationStatus extends SvrProcess {

    private static final String BULK_UPLOAD_PATH =
            "/home/ntier/SG_wsp_120626/MQAWSPATRDataDump2026_01062026.xlsx";

    private static final String WSP_STATUS_REF_UU = "98479fb5-df5d-440d-86aa-92d77a320857";

    /** SDL number to diagnose — mandatory process parameter. */
    private String targetSdl;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            String name = para.getParameterName();
            if ("SDLNumber".equals(name)) {
                targetSdl = para.getParameterAsString();
            } else {
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
            }
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (Util.isEmpty(targetSdl, true)) {
            throw new AdempiereException("SDLNumber parameter is required.");
        }
        targetSdl = targetSdl.trim();

        File file = new File(BULK_UPLOAD_PATH);
        if (!file.exists() || !file.isFile()) {
            throw new AdempiereException("Source file not found: " + BULK_UPLOAD_PATH);
        }

        List<X_ZZ_WSP_ATR_Lookup_Mapping> mappings = loadMappings();
        if (mappings.isEmpty()) {
            throw new AdempiereException("No active WSP/ATR bulk mapping header records found.");
        }

        addLog("=== DiagnoseWspAtrMigrationStatus for SDL: " + targetSdl + " ===");
        addLog("Source file: " + BULK_UPLOAD_PATH);
        addLog("");

        // --- Phase 1: stream every sheet, collect WSPStatus texts ----------------
        //
        // sdlStatusText  : SDL → raw WSPStatus cell text (first occurrence wins)
        // resolvedByOrgId: orgId (or 0 for unknown org) → resolved AD_Ref_List value
        //                  This mirrors ImportWspAtrMigrationFile's wspStatusByOrgId.
        // fallbackStatus : first resolved value seen (mimics the fallback logic)
        //
        final Map<String, String> sdlStatusText   = new HashMap<>();
        final Map<Integer, String> resolvedByOrgId = new HashMap<>();

        try (StreamingXlsxReader reader = new StreamingXlsxReader(file)) {

            for (X_ZZ_WSP_ATR_Lookup_Mapping mapping : mappings) {
                List<String> sheets = reader.findMatchingSheets(mapping.getZZ_Tab_Name());
                if (sheets.isEmpty()) {
                    addLog("WARNING: no sheet matched mapping tab '" + mapping.getZZ_Tab_Name() + "' — skipped.");
                    continue;
                }

                int startRow = mapping.getStart_Row() != null ? mapping.getStart_Row().intValue() : 4;
                if (startRow <= 0) startRow = 4;

                for (String sheetName : sheets) {
                    // Read header row (one row before startRow) to find column positions.
                    final int headerRowIdx = startRow - 1;
                    final int[] sdlColIdx    = {-1};
                    final int[] statusColIdx = {-1};

                    // Pass 1a: find column indices from the header row.
                    reader.streamSheet(sheetName, headerRowIdx, null, (rowIdx, cells) -> {
                        if (rowIdx != headerRowIdx) {
                            return StreamingXlsxReader.Action.STOP;
                        }
                        for (Map.Entry<Integer, String> e : cells.entrySet()) {
                            String header = e.getValue() == null ? "" : e.getValue().trim();
                            if ("SDLNumber".equalsIgnoreCase(header)) {
                                sdlColIdx[0] = e.getKey();
                            } else if ("WSPStatus".equalsIgnoreCase(header)) {
                                statusColIdx[0] = e.getKey();
                            }
                        }
                        return StreamingXlsxReader.Action.STOP;
                    });

                    if (sdlColIdx[0] < 0 || statusColIdx[0] < 0) {
                        addLog("WARNING: sheet '" + sheetName + "' — could not locate SDLNumber column (idx="
                                + sdlColIdx[0] + ") or WSPStatus column (idx=" + statusColIdx[0]
                                + ") in header row " + (headerRowIdx + 1) + " — skipped.");
                        continue;
                    }

                    addLog("Sheet '" + sheetName + "': SDLNumber at col " + sdlColIdx[0]
                            + ", WSPStatus at col " + statusColIdx[0]
                            + ", data starts at row " + (startRow + 1) + " (1-based).");

                    // Pass 1b: stream data rows, collect (SDL → raw WSPStatus text).
                    reader.streamSheet(sheetName, startRow, null, (rowIdx, cells) -> {
                        String sdl = cells.get(sdlColIdx[0]);
                        if (Util.isEmpty(sdl, true)) {
                            return StreamingXlsxReader.Action.CONTINUE;
                        }
                        sdl = sdl.trim();
                        if (!sdlStatusText.containsKey(sdl)) {
                            String rawStatus = cells.get(statusColIdx[0]);
                            sdlStatusText.put(sdl, rawStatus);

                            // Resolve to AD_Ref_List value — same logic as detectWspStatus.
                            if (!Util.isEmpty(rawStatus, true)) {
                                String lookupText = rawStatus.trim();
                                if ("Created".equalsIgnoreCase(lookupText)) {
                                    lookupText = "Draft"; // same remap as the importer
                                }
                                String resolved = DB.getSQLValueStringEx(get_TrxName(),
                                        "SELECT rl.Value FROM AD_Ref_List rl"
                                        + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                                        + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                                        + " AND UPPER(rl.Name) = UPPER(?)",
                                        lookupText);
                                if (!Util.isEmpty(resolved, true)) {
                                    // Key by orgId (same as importer: 0 if org not in DB yet).
                                    int orgId = lookupOrgId(sdl);
                                    Integer orgKey = orgId > 0 ? orgId : 0;
                                    if (!resolvedByOrgId.containsKey(orgKey)) {
                                        resolvedByOrgId.put(orgKey, resolved);
                                    }
                                }
                            }
                        }
                        return StreamingXlsxReader.Action.CONTINUE;
                    });
                }
            }
        }

        // --- Phase 2: compute what the importer would do for targetSdl -----------

        addLog("");
        addLog("--- Raw data collected from spreadsheet ---");

        String rawStatusText = sdlStatusText.get(targetSdl);
        if (!sdlStatusText.containsKey(targetSdl)) {
            addLog("SDL '" + targetSdl + "' was NOT found in the spreadsheet.");
            return "SDL not found in spreadsheet — no status would be set.";
        }
        addLog("SDL found in spreadsheet: YES");
        addLog("Raw WSPStatus cell text : '" + rawStatusText + "'");

        // Remap "Created" → "Draft" exactly as the importer does.
        String lookupText = Util.isEmpty(rawStatusText, true) ? null : rawStatusText.trim();
        if ("Created".equalsIgnoreCase(lookupText)) {
            lookupText = "Draft";
            addLog("'Created' remapped to 'Draft' before lookup.");
        }

        // Resolve this SDL's status text.
        String ownResolvedValue = null;
        String ownResolvedName  = null;
        if (!Util.isEmpty(lookupText, true)) {
            ownResolvedValue = DB.getSQLValueStringEx(get_TrxName(),
                    "SELECT rl.Value FROM AD_Ref_List rl"
                    + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                    + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                    + " AND UPPER(rl.Name) = UPPER(?)",
                    lookupText);
            if (!Util.isEmpty(ownResolvedValue, true)) {
                ownResolvedName = DB.getSQLValueStringEx(get_TrxName(),
                        "SELECT rl.Name FROM AD_Ref_List rl"
                        + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                        + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                        + " AND rl.Value = ?",
                        ownResolvedValue);
            }
        }

        // Look up orgId (used as the map key in the importer).
        int orgId = lookupOrgId(targetSdl);
        Integer orgKey = orgId > 0 ? Integer.valueOf(orgId) : Integer.valueOf(0);

        // Fallback = first resolved value across the whole workbook.
        String fallbackStatus = resolvedByOrgId.values().stream().findFirst().orElse(null);

        // What the importer would retrieve for this SDL (mirrors getOrDefault logic).
        String statusForOrg = resolvedByOrgId.containsKey(orgKey)
                ? resolvedByOrgId.get(orgKey)
                : fallbackStatus;

        addLog("");
        addLog("--- AD_Ref_List lookup result ---");
        if (ownResolvedValue != null) {
            addLog("Lookup SUCCESS: Value='" + ownResolvedValue + "'  Name='" + ownResolvedName + "'");
        } else {
            addLog("Lookup FAILED: text '" + lookupText + "' not found in AD_Ref_List for WSP Status reference.");
        }

        addLog("");
        addLog("--- Organisation lookup ---");
        addLog("ZZSdfOrganisation_ID for SDL '" + targetSdl + "': "
                + (orgId > 0 ? String.valueOf(orgId) : "NOT FOUND (org key = 0)"));
        addLog("Status stored in wspStatusByOrgId under key " + orgKey + ": "
                + (resolvedByOrgId.containsKey(orgKey) ? "'" + resolvedByOrgId.get(orgKey) + "'" : "ABSENT"));

        addLog("");
        addLog("--- Fallback status (first resolved status in workbook) ---");
        if (fallbackStatus != null) {
            // Resolve name for the fallback value for readability.
            String fallbackName = DB.getSQLValueStringEx(get_TrxName(),
                    "SELECT rl.Name FROM AD_Ref_List rl"
                    + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                    + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                    + " AND rl.Value = ?",
                    fallbackStatus);
            addLog("Fallback Value='" + fallbackStatus + "'  Name='" + fallbackName + "'");
        } else {
            addLog("No fallback available (nothing resolved in the entire workbook).");
        }

        addLog("");
        addLog("--- CONCLUSION ---");
        if (statusForOrg == null) {
            addLog("Final status that would be set: NULL (nothing resolved — import would likely fail).");
        } else {
            String finalName = DB.getSQLValueStringEx(get_TrxName(),
                    "SELECT rl.Name FROM AD_Ref_List rl"
                    + " JOIN AD_Reference r ON r.AD_Reference_ID = rl.AD_Reference_ID"
                    + " WHERE r.AD_Reference_UU = '" + WSP_STATUS_REF_UU + "'"
                    + " AND rl.Value = ?",
                    statusForOrg);
            addLog("Final status that would be set: Value='" + statusForOrg + "'  Name='" + finalName + "'");

            if (ownResolvedValue != null && !ownResolvedValue.equals(statusForOrg)) {
                addLog("NOTE: own lookup resolved to '" + ownResolvedValue
                        + "' but the importer would use the fallback ('" + statusForOrg + "') because"
                        + " the status was stored under key " + orgKey
                        + " which does not match the orgId used during the import pass ("
                        + orgId + ").");
            } else if (ownResolvedValue == null && statusForOrg != null) {
                addLog("NOTE: own lookup FAILED — the fallback status was applied.");
            }
        }

        return "Diagnosis complete for SDL " + targetSdl + ". See log for details.";
    }

    // -------------------------------------------------------------------------

    private int lookupOrgId(String sdl) {
        return DB.getSQLValueEx(get_TrxName(),
                "SELECT o.ZZSdfOrganisation_ID FROM ZZSdfOrganisation o"
                + " JOIN C_BPartner bp ON bp.C_BPartner_ID = o.C_BPartner_ID"
                + " WHERE bp.Value = ? AND o.AD_Client_ID = ? AND o.IsActive = 'Y'"
                + " FETCH FIRST 1 ROWS ONLY",
                sdl, Env.getAD_Client_ID(getCtx()));
    }

    private List<X_ZZ_WSP_ATR_Lookup_Mapping> loadMappings() {
        return new Query(getCtx(), X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
                "ZZ_Is_For_Bulk = 'Y'", get_TrxName())
                .setOnlyActiveRecords(true)
                .setOrderBy("seqNo")
                .list();
    }
}
