package za.co.ntier.wsp_atr.process;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.compiere.model.MColumn;
import org.compiere.model.PO;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;

public class WspAtrImportUtil {

    private final WspAtrReferenceService refService;

    public WspAtrImportUtil(WspAtrReferenceService refService) {
        this.refService = refService;
    }

    // ----------------------------------------------------------
    // Excel helpers
    // ----------------------------------------------------------

    /** Convert "A".."Z","AA".. to 0-based column index. */
    public int columnLetterToIndex(String letter) {
        if (Util.isEmpty(letter, true))
            return -1;

        letter = letter.trim().toUpperCase(Locale.ROOT);
        int result = 0;
        for (int i = 0; i < letter.length(); i++) {
            char c = letter.charAt(i);
            if (c < 'A' || c > 'Z') {
                return -1;
            }
            result = result * 26 + (c - 'A' + 1);
        }
        return result - 1;
    }

    public String getCellText(Row row, int col, DataFormatter formatter) {
        if (row == null)
            return "";
        Cell cell = row.getCell(col);
        if (cell == null)
            return "";
        try {
            return formatter.formatCellValue(cell).trim();
        } catch (Exception e) {
            try {
                if (cell.getCellType() == CellType.STRING) {
                    return cell.getStringCellValue().trim();
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            } catch (Exception ignore) {
            }
            return "";
        }
    }

    public String getCellTextByLetter(Row row, String colLetter, DataFormatter formatter) {
        if (Util.isEmpty(colLetter, true) || row == null)
            return "";
        int colIdx = columnLetterToIndex(colLetter);
        if (colIdx < 0)
            return "";
        return getCellText(row, colIdx, formatter);
    }

    /**
     * Tolerant numeric parse for Excel-sourced text.
     *
     * Handles:
     *   - leading apostrophe ("force text" prefix in Excel)
     *   - currency symbols ($, R) and stray spaces / non-breaking spaces
     *   - US-style numbers   : "1,234,567.89"
     *   - European-style     : "19509,35"  /  "1 234 567,89"
     *
     * Decision rule for ambiguous separators:
     *   - both . and , present : the rightmost is the decimal point, the other is thousands.
     *   - only , present       : treated as thousands if it forms tidy 3-digit groups
     *                            (e.g. "1,234"), otherwise as a decimal separator.
     *   - only . present       : taken as the decimal point as-is.
     *
     * Unparseable input returns Env.ZERO (preserves prior contract).
     */
    public BigDecimal parseBigDecimal(String txt) {
        if (txt == null) return Env.ZERO;
        String t = txt.trim();
        if (t.isEmpty()) return Env.ZERO;
        if (t.charAt(0) == '\'') t = t.substring(1).trim();
        t = t.replace("R", "").replace("$", "")
             .replace(" ", "").replace("\u00A0", "");
        if (t.isEmpty()) return Env.ZERO;

        int lastDot   = t.lastIndexOf('.');
        int lastComma = t.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            if (lastComma > lastDot) {              // European: dots thousands, comma decimal
                t = t.replace(".", "");
                t = t.replace(',', '.');
            } else {                                 // US: commas thousands, dot decimal
                t = t.replace(",", "");
            }
        } else if (lastComma >= 0) {
            // Only commas. Treat as thousands ONLY if it looks like "123,456,789".
            if (t.matches("-?\\d{1,3}(,\\d{3})+")) {
                t = t.replace(",", "");
            } else {
                t = t.replace(',', '.');
            }
        }
        // else: only dot, or no separator at all — leave as-is.

        try { return new BigDecimal(t); }
        catch (Exception e) { return Env.ZERO; }
    }

    public String truncate(String s, int max) {
        if (s == null)
            return null;
        s = s.trim();
        if (max <= 0)
            max = 200;
        return s.length() > max ? s.substring(0, max) : s;
    }

    // ----------------------------------------------------------
    // Generic setters
    // ----------------------------------------------------------

    /**
     * Generic setter for non-reference columns.
     */
    public void setPlainValueFromText(PO po, MColumn column, String text) {
        String colName = column.getColumnName();
        int displayType = column.getAD_Reference_ID();

        if (DisplayType.isNumeric(displayType)) {
            BigDecimal bd = parseBigDecimal(text);
            po.set_ValueOfColumn(colName, bd);
        } else {
            int len = column.getFieldLength();
            po.set_ValueOfColumn(colName, truncate(text, len));
        }
    }

    /**
     * Old signature kept for backward compatibility.
     * Delegates to the extended version with createIfNotExist = false.
     */
    public void setValueFromText(Properties ctx,
                                 PO po,
                                 MColumn column,
                                 String text,
                                 boolean useValueForRef,
                                 String trxName) {
        setValueFromText(ctx, po, column, text,
                useValueForRef,
                false,  // createIfNotExist
                null,   // refValue
                null,   // refName
                trxName);
    }

    /**
     * Extended version that supports:
     *  - normal behaviour (createIfNotExist = false)
     *  - or "create if missing" using a Value+Name pair from other columns.
     *
     * @param text      Primary text from the mapped column (may be null/empty for REF case)
     * @param refValue  Optional "Value" text from another column (for create-if-missing)
     * @param refName   Optional "Name" text from another column (for create-if-missing)
     */
    public void setValueFromText(Properties ctx,
                                 PO po,
                                 MColumn column,
                                 String text,
                                 boolean useValueForRef,
                                 boolean createIfNotExist,
                                 String refValue,
                                 String refName,
                                 String trxName) {

        String colName = column.getColumnName();
        int displayType = column.getAD_Reference_ID();

        // 1) Numeric columns
        if (DisplayType.isNumeric(displayType) && !DisplayType.isID(displayType)) {
            BigDecimal bd = parseBigDecimal(text);
            po.set_ValueOfColumn(colName, bd);
            return;
        }

        // 2) Table / TableDir (reference) columns
        if (displayType == DisplayType.Table || displayType == DisplayType.TableDir) {
            Integer id;

            if (createIfNotExist) {
                // Use the pair (Value, Name) if provided; otherwise fall back to "text"
                String valueText = !Util.isEmpty(refValue, true) ? refValue : text;
                String nameText  = !Util.isEmpty(refName, true)  ? refName  : text;

                if (Util.isEmpty(valueText, true) && Util.isEmpty(nameText, true)) {
                    // Nothing present – don't set anything
                    return;
                }

                id = refService.createReferenceIfMissing(
                        ctx,
                        po,
                        column,
                        colName,  // lookup key / label for error messages
                        valueText,
                        nameText,
                        useValueForRef,
                        trxName);
            } else {
                // Existing behaviour – look up by Value or Name from "text"
                if (Util.isEmpty(text, true)) {
                    return; // nothing to do
                }
                id = refService.lookupReferenceId(ctx, column, text, useValueForRef, trxName);
            }

            if (id == null || id <= 0) {
                throw new org.adempiere.exceptions.AdempiereException(
                        "No reference record found for text '" + text
                                + "' in column " + colName);
            }
            po.set_ValueOfColumn(colName, id);
            return;
        }

        // 3) All other types → string
        int len = column.getFieldLength();
        po.set_ValueOfColumn(colName, truncate(text, len > 0 ? len : 200));
    }


    // ----------------------------------------------------------
    // New: mapping-detail driven reference resolve + create
    // ----------------------------------------------------------

    /**
     * Uses one ZZ_WSP_ATR_Lookup_Mapping_Detail row to resolve
     * the reference ID for the current import row, creating the
     * reference record if allowed.
     */
    public Integer resolveRefIdForRow(Properties ctx,
                                      X_ZZ_WSP_ATR_Lookup_Mapping_Detail md,
                                      PO parentPO,
                                      MColumn targetColumn,
                                      Row row,
                                      DataFormatter formatter,
                                      String trxName) {

        // Main text from mapped column (ZZ_Column_Letter)
        String mainText = getCellTextByLetter(row, md.getZZ_Column_Letter(), formatter);
        if (Util.isEmpty(mainText, true)) {
            return null; // no data for this row/field
        }

        boolean useValueForRef   = md.isZZ_Use_Value();
        boolean createIfNotExist = md.isZZ_Create_If_Not_Exists();

        // Additional value/name columns for creation
        String valueText = getCellTextByLetter(row, md.getZZ_Value_Column_Letter(), formatter);
        String nameText  = getCellTextByLetter(row, md.getZZ_Name_Column_Letter(), formatter);

        if (Util.isEmpty(valueText, true))
            valueText = mainText;
        if (Util.isEmpty(nameText, true))
            nameText = mainText;

        String lookupKey = useValueForRef ? valueText : nameText;

        // 1) Try existing
        Integer id = refService.lookupReferenceId(ctx, targetColumn, lookupKey, useValueForRef, trxName);
        if (id != null) {
            return id;
        }

        // 2) Not found and not allowed to create → error
        if (!createIfNotExist) {
            throw new AdempiereException(
                    "No reference record found for text '" + lookupKey
                            + "' in column " + targetColumn.getColumnName()
                            + " and Create_If_Not_Exists = N");
        }

        // 3) Create & return id
        return refService.createReferenceIfMissing(ctx,
                                                   parentPO,
                                                   targetColumn,
                                                   lookupKey,
                                                   valueText,
                                                   nameText,
                                                   useValueForRef,
                                                   trxName);
    }

    /**
     * Simple helper to check if a row is empty for a set of mapped columns.
     */
    public boolean isRowEmptyForMappedColumns(Row row,
                                              Map<Integer, X_ZZ_WSP_ATR_Lookup_Mapping_Detail> colMap,
                                              DataFormatter formatter) {
        if (row == null)
            return true;

        for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail md : colMap.values()) {
            String txt = getCellTextByLetter(row, md.getZZ_Column_Letter(), formatter);
            if (!Util.isEmpty(txt, true))
                return false;
        }
        return true;
    }
}

