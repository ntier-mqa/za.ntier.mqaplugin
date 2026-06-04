package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    @Override
    protected void prepare() {
        for (ProcessInfoParameter param : getParameter()) {
            if ("C_Year_ID".equals(param.getParameterName()))
                C_YEAR_ID = param.getParameterAsInt();
        }
    }

    @Override
    protected String doIt() throws Exception {
        if (C_YEAR_ID <= 0)
            throw new IllegalArgumentException("C_Year_ID parameter is required");

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

                // Find BP with this value (Search Key)
                MBPartner bp = MBPartner.get(Env.getCtx(), searchKey);
                if (bp == null || bp.get_ID() <= 0) {
                    bpNotFound++;
                    addLog("No Business Partner found for value (Search Key): " + searchKey);
                    continue;
                }

                // Check if a Levy Paying record already exists for this BP + Year
                boolean exists = new Query(getCtx(), X_ZZ_Levy_Paying.Table_Name,
                        "C_BPartner_ID=? AND C_Year_ID=?", get_TrxName())
                        .setParameters(bp.getC_BPartner_ID(), C_YEAR_ID)
                        .setOnlyActiveRecords(true)
                        .match();

                if (exists) {
                    skippedExisting++;
                    continue;
                }

                // Create new Levy Paying record
                X_ZZ_Levy_Paying levy = new X_ZZ_Levy_Paying(getCtx(), 0, get_TrxName());
                levy.setC_BPartner_ID(bp.getC_BPartner_ID());
                levy.setC_Year_ID(C_YEAR_ID);
                levy.setZZ_LevyPaying(true);
                levy.setName(bp.getName());   // optional
                levy.setValue(bp.getValue()); // optional
                levy.saveEx();

                created++;
            }
        }

        return "Created " + created + " Levy Paying records. "
             + "Skipped " + skippedExisting + " existing. "
             + "No BP found for " + bpNotFound + " value(s).";
    }
}

