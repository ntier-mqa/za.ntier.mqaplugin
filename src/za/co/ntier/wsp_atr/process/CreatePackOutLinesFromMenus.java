package za.co.ntier.wsp_atr.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.compiere.model.X_AD_Package_Exp_Detail;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Create pack out lines (AD_Package_Exp_Detail) for menu IDs sourced from:
 *   select t.name, m.ad_menu_id
 *     from t_ad_menu_not_prod t
 *     join ad_menu m on t.name = m.name
 *
 * Links lines to AD_Package_Exp_ID=1000057, Type='M'.
 */
@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.wsp_atr.process.CreatePackOutLinesFromMenus")
public class CreatePackOutLinesFromMenus extends SvrProcess {

    private static final int PACKOUT_HEADER_ID = 1000057;

    @Override
    protected void prepare() {
        // no parameters
    }

    @Override
    protected String doIt() throws Exception {
        String query =
            "SELECT t.name, m.ad_menu_id " +
            "FROM t_ad_menu_not_prod t " +
            "JOIN ad_menu m ON t.name = m.name";

        List<int[]> menuRows = new ArrayList<>(); // [0]=ad_menu_id
        List<String> menuNames = new ArrayList<>();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(query, get_TrxName());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                menuNames.add(rs.getString("name"));
                menuRows.add(new int[]{ rs.getInt("ad_menu_id") });
            }
        } catch (SQLException e) {
            throw new Exception("Error querying t_ad_menu_not_prod / ad_menu: " + e.getMessage(), e);
        } finally {
            DB.close(rs, pstmt);
        }

        if (menuRows.isEmpty()) {
            return "No rows returned from t_ad_menu_not_prod / ad_menu join — nothing to create.";
        }

        int maxLine = DB.getSQLValueEx(get_TrxName(),
                "SELECT COALESCE(MAX(Line),0) FROM AD_Package_Exp_Detail WHERE AD_Package_Exp_ID=?",
                PACKOUT_HEADER_ID);

        int line = maxLine;
        int created = 0;
        int skipped = 0;
        List<String> skippedNames = new ArrayList<>();

        for (int i = 0; i < menuRows.size(); i++) {
            int adMenuId = menuRows.get(i)[0];
            String name  = menuNames.get(i);

            // Duplicate check by AD_Menu_ID
            int dup = DB.getSQLValueEx(get_TrxName(),
                    "SELECT COUNT(*) FROM AD_Package_Exp_Detail " +
                    "WHERE AD_Package_Exp_ID=? AND AD_Menu_ID=?",
                    PACKOUT_HEADER_ID, adMenuId);

            if (dup > 0) {
                skipped++;
                skippedNames.add(name);
                continue;
            }

            line += 10;

            X_AD_Package_Exp_Detail d = new X_AD_Package_Exp_Detail(getCtx(), 0, get_TrxName());
            d.setAD_Org_ID(0);
            d.setAD_Package_Exp_ID(PACKOUT_HEADER_ID);
            d.setLine(line);
            d.setType(X_AD_Package_Exp_Detail.TYPE_ApplicationOrModule);  // 'M'
            d.setAD_Menu_ID(adMenuId);
            d.setProcessed(false);
            d.setProcessing(false);
            d.saveEx();
            created++;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("PackOut Header AD_Package_Exp_ID=").append(PACKOUT_HEADER_ID)
           .append(" | Created=").append(created)
           .append(" | Skipped=").append(skipped);

        if (!skippedNames.isEmpty()) {
            msg.append("\nSkipped (already exists): ").append(String.join(", ", skippedNames));
        }

        return msg.toString();
    }
}
