package za.ntier.process;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Parameter;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Updates ZZ_DocStatus to 'AP' for all records linked to the selected program.
 */
@org.adempiere.base.annotation.Process(name = "za.co.ntier.process.UpdateDocStatusToApproved")
public class UpdateProgramDocStatusToApproved extends SvrProcess {

    // User selects the program
    @Parameter(name = "ZZ_Program_Master_Data_ID")
    private int programMasterDataId = 0;

    // Optional: update even if already AP? default false (safer)
    @Parameter(name = "UpdateEvenIfAlreadyApproved")
    private boolean updateEvenIfAlreadyApproved = false;

    // Optional: only update drafts / in-progress etc. Leave empty to update all
    // Example values: "DR,IP,SU"
    @Parameter(name = "OnlyFromStatuses")
    private String onlyFromStatusesCsv = null;

    // Target table (change if needed)
    private static final String TABLE_NAME = "ZZ_Application_Form";

    @Override
    protected void prepare() {
        // Using @Parameter annotations, no manual parsing needed
    }

    @Override
    protected String doIt() throws Exception {
        if (programMasterDataId <= 0) {
            throw new AdempiereException("ZZ_Program_Master_Data_ID is mandatory.");
        }

        // Build WHERE
        StringBuilder where = new StringBuilder();
        where.append("ZZ_Program_Master_Data_ID=?");

        List<Object> params = new ArrayList<>();
        params.add(programMasterDataId);

        if (!updateEvenIfAlreadyApproved) {
            where.append(" AND COALESCE(ZZ_DocStatus,'') <> 'AP'");
        }

        List<String> onlyFromStatuses = parseCsvList(onlyFromStatusesCsv);
        if (!onlyFromStatuses.isEmpty()) {
            where.append(" AND COALESCE(ZZ_DocStatus,'') IN (");
            for (int i = 0; i < onlyFromStatuses.size(); i++) {
                if (i > 0) where.append(",");
                where.append("?");
                params.add(onlyFromStatuses.get(i));
            }
            where.append(")");
        }

        // Count first (nice feedback)
        int toUpdate = DB.getSQLValueEx(get_TrxName(),
                "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE " + where,
                params.toArray());

        if (toUpdate <= 0) {
            return "No records found to update for ZZ_Program_Master_Data_ID=" + programMasterDataId;
        }

        // Update
        String updateSql =
                "UPDATE " + TABLE_NAME + " " +
                "SET ZZ_DocStatus='AP', Updated=now(), UpdatedBy=? " +
                "WHERE " + where;

        // prepend UpdatedBy param
        List<Object> updateParams = new ArrayList<>();
        updateParams.add(getAD_User_ID());
        updateParams.addAll(params);

        int updated = DB.executeUpdateEx(updateSql, updateParams.toArray(), get_TrxName());

        // Optional: return some IDs as proof (limit 20)
        String sampleSql =
                "SELECT " + TABLE_NAME + "_ID " +
                "FROM " + TABLE_NAME + " " +
                "WHERE ZZ_Program_Master_Data_ID=? AND ZZ_DocStatus='AP' " +
                "ORDER BY Updated DESC " +
                "LIMIT 20";

        String sampleIds = fetchIds(sampleSql, new Object[]{ programMasterDataId });

        addLog("Updated ZZ_DocStatus to AP for Program=" + programMasterDataId + " (rows=" + updated + ")");
        return "Updated " + updated + " record(s) in " + TABLE_NAME +
               " for ZZ_Program_Master_Data_ID=" + programMasterDataId +
               (sampleIds.isEmpty() ? "" : "\nSample IDs: " + sampleIds);
    }

    private List<String> parseCsvList(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null) return out;
        String trimmed = csv.trim();
        if (trimmed.isEmpty()) return out;

        for (String part : trimmed.split(",")) {
            String v = part == null ? "" : part.trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    private String fetchIds(String sql, Object[] params) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, get_TrxName());
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            rs = pstmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(", ");
                sb.append(rs.getInt(1));
                first = false;
            }
            return sb.toString();
        } catch (Exception e) {
            // Not fatal: sample list is optional
            return "";
        } finally {
            DB.close(rs, pstmt);
        }
    }
}