package za.co.ntier.wsp_atr.process;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Col_Check;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

public class ColumnModeSheetValidator extends AbstractMappingSheetImporter {

    private final ExcelErrorMarker marker = new ExcelErrorMarker();
    private final ExcelErrorLogSheet errorLog = new ExcelErrorLogSheet();
    private static final int MAX_ERRORS = 100;
   


    public ColumnModeSheetValidator(ReferenceLookupService refService, SvrProcess proc) {
        super(refService, proc);
    }

    public int validate(Properties ctx,
                        Workbook wb,
                        X_ZZ_WSP_ATR_Submitted submitted,
                        X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                        String trxName,
                        DataFormatter formatter,
                        FormulaEvaluator evaluator) {

        Sheet sheet = getSheetOrThrow(wb, mappingHeader);
        List<X_ZZ_WSP_ATR_Lookup_Mapping_Detail> details = loadDetails(mappingHeader, trxName);
        if (details == null || details.isEmpty()) {
            return 0;
        }
        List<X_ZZ_WSP_ATR_Col_Check> numericChecks = loadNumericChecks(mappingHeader, trxName);

        Map<Integer, ColumnMeta> colIndexToMeta = new HashMap<>();
        for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
            if (Util.isEmpty(det.getZZ_Column_Letter(), true)) {
                continue;
            }

            int colIndex = columnLetterToIndex(det.getZZ_Column_Letter());
            MColumn column = new MColumn(ctx, det.getAD_Column_ID(), trxName);

            ColumnMeta meta = new ColumnMeta();
            //meta.detail = det;
            meta.isFormular = det.isZZ_Is_Formula();
            meta.columnIndex = colIndex;
            meta.column = column;
            meta.useValueForRef = det.isZZ_Use_Value();
            meta.mandatory = det.isMandatory();
            meta.ignoreIfBlank = det.isIgnore_If_Blank();
            meta.headerName = det.getZZ_Header_Name();
            meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();
            String valueColLetter = det.getZZ_Value_Column_Letter();
			String nameColLetter  = det.getZZ_Name_Column_Letter();

			if (!Util.isEmpty(valueColLetter, true)) {
				meta.valueColumnIndex = columnLetterToIndex(valueColLetter);
			}
			if (!Util.isEmpty(nameColLetter, true)) {
				meta.nameColumnIndex = columnLetterToIndex(nameColLetter);
			}

            colIndexToMeta.put(colIndex, meta);
        }

        int errors = 0;
        int lastRow = sheet.getLastRowNum();
        int startRow = (mappingHeader.getStart_Row() == null) ? 0 : mappingHeader.getStart_Row().intValue();
        if (startRow <= 0) {
            startRow = 4;
        }

        int rowCnt = 0;
        int emptyRowsInARow = 0;
        for (int r = startRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            if (isRowCompletelyEmpty(row, colIndexToMeta.values())) {
            	emptyRowsInARow++;
				if (emptyRowsInARow > 10) {
					break;  // to many empty lines.  Assume the rest are empty
				}
                continue;
            }
                      
            
            emptyRowsInARow = 0;
            rowCnt++;

            if (shouldIgnoreRowBecauseOfIgnoreIfBlank(row, colIndexToMeta.values(), formatter)) {
                continue;
            }

            // Mandatory validation
            for (ColumnMeta meta : colIndexToMeta.values()) {
                if (!meta.mandatory) {
                    continue;
                }
                             

                String txt = getCellText(row, meta.columnIndex, formatter);
                if (Util.isEmpty(txt, true)) {
                    String msg = "Mandatory field is missing (" + meta.column.getColumnName() + ")";
                    marker.markError(wb, sheet, row, meta.columnIndex, msg);
                    errorLog.appendError(
                        wb,
                        sheet.getSheetName(),
                        meta.headerName,
                        row.getRowNum(),
                        meta.columnIndex,
                        msg
                    );
                    errors++;

                    if (errors >= MAX_ERRORS) {
                        errorLog.appendTooManyErrors(wb);
                        return errors;
                    }
                }
            }

            // Reference validation
            for (ColumnMeta meta : colIndexToMeta.values()) {
                String txt = getCellText(row, meta.columnIndex, formatter);
                if (Util.isEmpty(txt, true)) {
                    continue;
                }

                int ref = meta.column.getAD_Reference_ID();
                boolean isRef = (ref == DisplayType.Table
                        || ref == DisplayType.TableDir
                        || ref == DisplayType.Search);

                if (!isRef || meta.createIfNotExist) {
                    continue;
                }

                Integer id = tryResolveRefId(ctx, meta.column, txt, meta.useValueForRef, trxName);
                if (id == null || id <= 0) {
                    String msg = "Reference not found for value '" + txt + "' (" + meta.column.getColumnName() + ")";
                    marker.markError(wb, sheet, row, meta.columnIndex, msg);
                    errorLog.appendError(
                        wb,
                        sheet.getSheetName(),
                        meta.headerName,
                        row.getRowNum(),
                        meta.columnIndex,
                        msg
                    );
                    errors++;

                    if (errors >= MAX_ERRORS) {
                        errorLog.appendTooManyErrors(wb);
                        return errors;
                    }
                }
            }

            // Numeric cross-column checks (e.g. Male+Female = African+Coloured+Indian+White)
            if (!numericChecks.isEmpty()) {
                errors += runNumericChecks(wb, sheet, row, numericChecks, formatter);
                if (errors >= MAX_ERRORS) {
                    errorLog.appendTooManyErrors(wb);
                    return errors;
                }
            }
        }

        boolean isBiodataTab = "Biodata".equalsIgnoreCase(mappingHeader.getZZ_Tab_Name());
        if (isBiodataTab && rowCnt == 0) {
            String msg = "No BioData";
            errorLog.appendError(wb, sheet.getSheetName(), "", 0, 0, msg);
            errors++;
        }

        return errors;
    }

    // -----------------------------------------------------------------------
    // Numeric cross-column check helpers
    // -----------------------------------------------------------------------

    /**
     * Runs all configured numeric checks for one data row.
     * Returns the number of new errors added.
     */
    private int runNumericChecks(Workbook wb,
                                  Sheet sheet,
                                  Row row,
                                  List<X_ZZ_WSP_ATR_Col_Check> checks,
                                  DataFormatter formatter) {
        int errs = 0;
        for (X_ZZ_WSP_ATR_Col_Check check : checks) {
            String type = check.getZZ_Check_Type();
            int[] colsA = parseColLetters(check.getZZ_Col_Letters_A());
            int[] colsB = parseColLetters(check.getZZ_Col_Letters_B());
            String checkName = Util.isEmpty(check.getZZ_Check_Name(), true)
                    ? "Numeric check failed" : check.getZZ_Check_Name();

            boolean failed = false;

            if (X_ZZ_WSP_ATR_Col_Check.ZZ_CHECK_TYPE_NOT_ZERO.equals(type)) {
                // Sum of group A must be > 0
                failed = (sumCols(row, colsA, formatter) == 0.0);

            } else if (X_ZZ_WSP_ATR_Col_Check.ZZ_CHECK_TYPE_SUM_EQUALS_SUM.equals(type)) {
                // Sum of group A must equal sum of group B
                failed = (sumCols(row, colsA, formatter) != sumCols(row, colsB, formatter));
            }

            if (failed) {
                // Mark the first column of group A in the spreadsheet
                int markCol = (colsA.length > 0) ? colsA[0] : 0;
                marker.markError(wb, sheet, row, markCol, checkName);
                errorLog.appendError(
                        wb,
                        sheet.getSheetName(),
                        checkName,
                        row.getRowNum(),
                        markCol,
                        checkName);
                errs++;
            }
        }
        return errs;
    }

    /**
     * Parses a comma-separated string of column letters (e.g. "C,D") into
     * zero-based column indices.  Returns an empty array for blank input.
     */
    private int[] parseColLetters(String letters) {
        if (Util.isEmpty(letters, true))
            return new int[0];
        String[] parts = letters.split(",");
        int[] indices = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            indices[i] = columnLetterToIndex(parts[i].trim());
        }
        return indices;
    }

    /**
     * Sums the numeric values of the given column indices for a row.
     * Blank or non-numeric cells count as zero.
     */
    private double sumCols(Row row, int[] colIndices, DataFormatter formatter) {
        double sum = 0.0;
        for (int colIdx : colIndices) {
            String txt = getCellText(row, colIdx, formatter);
            if (!Util.isEmpty(txt, true)) {
                BigDecimal bd = parseBigDecimal(txt);
                if (bd != null)
                    sum += bd.doubleValue();
            }
        }
        return sum;
    }

    @Override
    public int importData(Properties ctx,
                          Workbook wb,
                          X_ZZ_WSP_ATR_Submitted submitted,
                          X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader,
                          String trxName,
                          DataFormatter formatter) throws IllegalStateException, SQLException {
        return 0;
    }
    
   

   
   
    
}