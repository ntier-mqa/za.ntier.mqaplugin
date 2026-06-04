package za.ntier.process;

import java.io.File;
import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.compiere.model.MBPartner;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.ntier.models.X_ZZ_Levy_Paying;

@org.adempiere.base.annotation.Process(name="za.ntier.process.ImportPaidLevies")
public class ImportPaidLevies extends SvrProcess {

    private int C_YEAR_ID = 0;
    private String p_FileName;

    @Override
    protected void prepare() {
        for (ProcessInfoParameter param : getParameter()) {
            String name = param.getParameterName();
            if ("C_Year_ID".equals(name))
                C_YEAR_ID = param.getParameterAsInt();
            else if ("FileName".equals(name))
                p_FileName = param.getParameterAsString();
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (C_YEAR_ID <= 0)
            throw new IllegalArgumentException("C_Year_ID parameter is required");
        if (p_FileName == null || p_FileName.trim().isEmpty())
            throw new IllegalArgumentException("FileName parameter is required");

        loadXlsIntoPaidLevies(p_FileName.trim());

        String sql = "SELECT value FROM paid_levies";
        int created = 0;
        int skippedExisting = 0;
        int bpNotFound = 0;

        try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName());
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String value = rs.getString("value");
                if (value == null || value.trim().isEmpty())
                    continue;

                String searchKey = value.trim();

                MBPartner bp = MBPartner.get(Env.getCtx(), searchKey);
                if (bp == null || bp.get_ID() <= 0) {
                    bpNotFound++;
                    addLog("No Business Partner found for value (Search Key): " + searchKey);
                    continue;
                }

                boolean exists = new Query(getCtx(), X_ZZ_Levy_Paying.Table_Name,
                        "C_BPartner_ID=? AND C_Year_ID=?", get_TrxName())
                        .setParameters(bp.getC_BPartner_ID(), C_YEAR_ID)
                        .setOnlyActiveRecords(true)
                        .match();

                if (exists) {
                    skippedExisting++;
                    continue;
                }

                X_ZZ_Levy_Paying levy = new X_ZZ_Levy_Paying(getCtx(), 0, get_TrxName());
                levy.setC_BPartner_ID(bp.getC_BPartner_ID());
                levy.setC_Year_ID(C_YEAR_ID);
                levy.setZZ_LevyPaying(true);
                levy.setName(bp.getName());
                levy.setValue(bp.getValue());
                levy.saveEx();

                created++;
            }
        }

        return "Created " + created + " Levy Paying records. "
             + "Skipped " + skippedExisting + " existing. "
             + "No BP found for " + bpNotFound + " value(s).";
    }

    /**
     * Backs up paid_levies to a timestamped snapshot table, clears it,
     * then re-populates it from the "Levy Number" column of the given XLS/XLSX file.
     */
    private void loadXlsIntoPaidLevies(String fileName) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String backupTable = "t_paid_levies_" + timestamp;

        // Create backup table and copy current data
        DB.executeUpdateEx(
            "CREATE TABLE " + backupTable + " AS SELECT value FROM paid_levies",
            get_TrxName());
        addLog("Backed up paid_levies to " + backupTable);

        // Clear the table
        DB.executeUpdateEx("DELETE FROM paid_levies", get_TrxName());
        addLog("Cleared paid_levies");

        // Read XLS/XLSX and insert rows
        File file = new File(fileName);
        int levyColIndex = -1;
        int inserted = 0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    // Locate "Levy Number" header
                    for (Cell cell : row) {
                        if ("Levy Number".equalsIgnoreCase(getCellString(cell))) {
                            levyColIndex = cell.getColumnIndex();
                            break;
                        }
                    }
                    if (levyColIndex < 0)
                        throw new IllegalArgumentException(
                            "Column 'Levy Number' not found in the XLS header row");
                    continue;
                }

                Cell cell = row.getCell(levyColIndex);
                if (cell == null) continue;
                String levyNumber = getCellString(cell).trim();
                if (levyNumber.isEmpty()) continue;

                DB.executeUpdateEx(
                    "INSERT INTO paid_levies (value) VALUES (?)",
                    new Object[]{levyNumber},
                    get_TrxName());
                inserted++;
            }
        }

        addLog("Inserted " + inserted + " rows into paid_levies from " + fileName);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC)
            return String.valueOf((long) cell.getNumericCellValue());
        return cell.toString();
    }
}
