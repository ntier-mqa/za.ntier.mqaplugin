package za.ntier.process;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.base.annotation.Parameter;
import org.compiere.model.MAttachment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.X_AD_Package_Imp_Proc;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;

@org.adempiere.base.annotation.Process(name = "za.ntier.process.CreatePackInsFromText")
public class CreatePackInsFromText extends SvrProcess {

    @Parameter(name = "InputText")
    private String inputText;

    @Parameter(name = "BaseDirectory")
    private String baseDirectory;

    @Override
    protected void prepare() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String doIt() throws Exception {
        if (inputText == null || inputText.trim().isEmpty()) {
            throw new AdempiereUserError("InputText is required.");
        }

        String resolvedBaseDirectory = (baseDirectory == null || baseDirectory.trim().isEmpty())
                ? "/home/martin/sourcesMQA5/project.extra.bundle/"
                : baseDirectory.trim();

        int tableId = MTable.getTable_ID("AD_Package_Imp");
        if (tableId <= 0) {
            throw new AdempiereUserError("Could not resolve AD_Package_Imp table.");
        }

        int currentClientId = Env.getAD_Client_ID(getCtx());
        boolean isSystemClient = currentClientId == 0;

        String[] rows = inputText.split("\\r?\\n");
        int created = 0;
        int attached = 0;
        int skipped = 0;
        int skippedByClientRule = 0;

        List<String> warnings = new ArrayList<>();

        for (String row : rows) {
            if (row == null) {
                continue;
            }

            String trimmed = row.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String fileName = extractFileName(trimmed);
            if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
                skipped++;
                warnings.add("Skipped (not a zip path): " + trimmed);
                continue;
            }

            String packInName = fileName.substring(0, fileName.length() - 4);
            boolean isSystemPackIn = packInName.contains("_SYSTEM_");
            if ((isSystemClient && !isSystemPackIn) || (!isSystemClient && isSystemPackIn)) {
                skippedByClientRule++;
                warnings.add("Skipped (client rule mismatch): " + fileName);
                continue;
            }

            Path zipFile = resolveZipPath(resolvedBaseDirectory, trimmed);
            if (!Files.exists(zipFile)) {
                skipped++;
                warnings.add("File not found: " + zipFile);
                continue;
            }

            X_AD_Package_Imp_Proc x_AD_Package_Imp_Proc = new X_AD_Package_Imp_Proc(getCtx(),0,get_TrxName());
            x_AD_Package_Imp_Proc.setAD_Package_Source_Type("File");
            x_AD_Package_Imp_Proc.set_ValueNoCheck("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            x_AD_Package_Imp_Proc.setAD_Org_ID(0);
            x_AD_Package_Imp_Proc.setName(packInName);
            x_AD_Package_Imp_Proc.setProcessing(false);
            x_AD_Package_Imp_Proc.saveEx();
            created++;

            byte[] content = Files.readAllBytes(zipFile);
            int attID = getID(tableId, x_AD_Package_Imp_Proc.get_ID());
            if (attID < 0) attID = 0;
            MAttachment attachment = new MAttachment(Env.getCtx(), attID, get_TrxName());
            attachment.setClientOrg(Env.getAD_Client_ID(getCtx()), 0);
            attachment.setAD_Table_ID (x_AD_Package_Imp_Proc.Table_ID);
            attachment.setRecord_ID (x_AD_Package_Imp_Proc.getAD_Package_Imp_Proc_ID());
            attachment.setRecord_UU (x_AD_Package_Imp_Proc.getAD_Package_Imp_Proc_UU());
            attachment.addEntry(fileName,content);
            attachment.saveEx();
            attached++;

            addLog("Created PackIn: " + packInName + " and attached " + fileName);
        }

        StringBuilder result = new StringBuilder();
        result.append("Created PackIns=").append(created)
              .append(", Attachments Added=").append(attached)
              .append(", Skipped=").append(skipped)
              .append(", SkippedByClientRule=").append(skippedByClientRule)
              .append(", CurrentClient=").append(currentClientId)
              .append(isSystemClient ? " (System)" : " (Non-System)");

        if (!warnings.isEmpty()) {
            result.append(" | Warnings: ").append(String.join(" || ", warnings));
        }

        return result.toString();
    }
    
    
   	public  int getID(int Table_ID, int Record_ID) {
   		String sql="SELECT AD_Attachment_ID FROM AD_Attachment WHERE AD_Table_ID=? AND Record_ID=? AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx());
   		int attachid = DB.getSQLValue(null, sql, Table_ID, Record_ID);
   		return attachid;
   	}

    private String extractFileName(String pathText) {
        String normalized = pathText.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        if (idx >= 0 && idx < normalized.length() - 1) {
            return normalized.substring(idx + 1);
        }
        return normalized;
    }

    private Path resolveZipPath(String baseDir, String originalLine) {
        String normalized = originalLine.trim().replace('\\', '/');
        Path candidate = Paths.get(normalized);
        if (candidate.isAbsolute()) {
            return candidate;
        }
        return Paths.get(baseDir, normalized);
    }
}
