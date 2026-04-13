package za.ntier.process;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Parameter;
import org.compiere.model.MAttachment;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.Env;

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

            PO packIn = MTable.get(getCtx(), "AD_Package_Imp").getPO(0, get_TrxName());
            packIn.set_ValueNoCheck("AD_Client_ID", Env.getAD_Client_ID(getCtx()));
            packIn.setAD_Org_ID(0);
            packIn.set_ValueOfColumn("Name", packInName);
            packIn.saveEx();
            created++;

            byte[] content = Files.readAllBytes(zipFile);
            MAttachment attachment = MAttachment.get(getCtx(), tableId, packIn.get_ID());
            if (attachment == null) {
                attachment = new MAttachment(getCtx(), tableId, packIn.get_ID(), get_TrxName());
            }
            attachment.addEntry(content, fileName);
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
