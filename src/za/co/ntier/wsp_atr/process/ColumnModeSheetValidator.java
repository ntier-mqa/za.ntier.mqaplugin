package za.co.ntier.wsp_atr.process;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.compiere.model.MColumn;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;
import org.compiere.util.Util;

import org.apache.poi.ss.usermodel.Row;


import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping_Detail;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

public class ColumnModeSheetValidator extends AbstractMappingSheetImporter {

    private final ExcelErrorMarker marker = new ExcelErrorMarker();
    private final ExcelErrorLogSheet errorLog = new ExcelErrorLogSheet();

    

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
        if (details == null || details.isEmpty()) return 0;

        // Build meta (same as importer)
        Map<Integer, ColumnMeta> colIndexToMeta = new HashMap<>();
        for (X_ZZ_WSP_ATR_Lookup_Mapping_Detail det : details) {
            if (Util.isEmpty(det.getZZ_Column_Letter(), true)) continue;

            int colIndex = columnLetterToIndex(det.getZZ_Column_Letter());
            MColumn column = new MColumn(ctx, det.getAD_Column_ID(), trxName);

            ColumnMeta meta = new ColumnMeta();
            meta.columnIndex = colIndex;
            meta.column = column;
            meta.useValueForRef = det.isZZ_Use_Value();
            meta.mandatory = det.isMandatory();
            meta.ignoreIfBlank = det.isIgnore_If_Blank();
            meta.headerName =  det.getZZ_Header_Name();
            meta.createIfNotExist = det.isZZ_Create_If_Not_Exists();


            colIndexToMeta.put(colIndex, meta);
        }

        int errors = 0;
        int lastRow = sheet.getLastRowNum();
    	int startRow = (mappingHeader.getStart_Row() == null) ? 0 : mappingHeader.getStart_Row().intValue();
		if (startRow <= 0) startRow = 4; // keep current default behavior
        for (int r = startRow; r <= lastRow; r++) { // your DEFAULT_DATA_START_ROW
            Row row = sheet.getRow(r);
            if (row == null) continue;

         // ✅ NEW: ignore fully-empty rows
            if (isRowCompletelyEmpty(
                    row,
                    colIndexToMeta.keySet(),
                    formatter,
                    evaluator)) {
                continue;
            }
            
         // 2️⃣ Ignore rows based on Ignore_If_Blank
            if (shouldIgnoreRowBecauseOfIgnoreIfBlank(
                    row,
                    colIndexToMeta.values(),
                    formatter,
                    evaluator)) {
                continue;
            }
            // 1) Mandatory missing
            for (ColumnMeta meta : colIndexToMeta.values()) {
                if (!meta.mandatory) continue;

                String txt = getCellText(row, meta.columnIndex, formatter, evaluator);
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
                }
            }

            // 2) Reference not found (Table/TableDir/Search)
            for (ColumnMeta meta : colIndexToMeta.values()) {
                String txt = getCellText(row, meta.columnIndex, formatter, evaluator);
                if (Util.isEmpty(txt, true)) continue;
                
                int ref = meta.column.getAD_Reference_ID();
                boolean isRef = (ref == DisplayType.Table || ref == DisplayType.TableDir || ref == DisplayType.Search);
                if (!isRef) continue;
                if (meta.createIfNotExist) continue;

                Integer id = tryResolveRefId(ctx, meta.column, txt, meta.useValueForRef, trxName);
                if (id == null || id <= 0) {
                    // If mandatory ref: this is a hard error (and you should block import)
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
                	}

                }                

            }
        }

        return errors;
    }

   

	@Override
	public int importData(Properties ctx, Workbook wb, X_ZZ_WSP_ATR_Submitted submitted,
			X_ZZ_WSP_ATR_Lookup_Mapping mappingHeader, String trxName, ImportWspAtrDataFromTemplate process,
			DataFormatter formatter) throws IllegalStateException, SQLException {
		// TODO Auto-generated method stub
		return 0;
	}
}

