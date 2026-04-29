package za.ntier.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;

import za.ntier.models.MInvoiceBatch_New;
import za.ntier.models.X_ZZ_Monthly_Levy_Files;
import za.ntier.models.X_ZZ_Monthly_Levy_Files_Hdr;
import za.ntier.repo.ApprovalsRepository;
import za.ntier.repo.BatchRepository;
import za.ntier.repo.LevyFilesRepository;
import za.ntier.repo.PartnerRepository;
import za.ntier.utils.DescriptionBuilder;
import za.ntier.utils.MoneyMath;

public class LevyBatchCreationService {

    private final Properties ctx;
    private final String trxName;
    private final int adUserId;
    private final int defaultCurrencyId;
    private final int p_C_Charge_ID;
    private final int p_C_DocType_ID;
    private final Timestamp p_DateDoc;

    private final ApprovalsRepository approvalsRepo;
    private final LevyFilesRepository levyRepo;
    private final PartnerRepository partnerRepo;
    private final BatchRepository batchRepo;

    public LevyBatchCreationService(Properties ctx, String trxName, int adUserId,
                                    int defaultCurrencyId, int chargeId, int docTypeId, Timestamp dateDoc) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.adUserId = adUserId;
        this.defaultCurrencyId = defaultCurrencyId;
        this.p_C_Charge_ID = chargeId;
        this.p_C_DocType_ID = docTypeId;
        this.p_DateDoc = dateDoc;

        this.approvalsRepo = new ApprovalsRepository(ctx, trxName);
        this.levyRepo      = new LevyFilesRepository(ctx, trxName);
        this.partnerRepo   = new PartnerRepository(ctx, trxName);
        this.batchRepo     = new BatchRepository(ctx, trxName, adUserId, p_DateDoc);
    }

    public String createBatchesFromHeader(int hdrId) {
        X_ZZ_Monthly_Levy_Files_Hdr hdr = levyRepo.getHeaderById(hdrId);
        if (hdr == null || hdr.get_ID() <= 0)
            throw new AdempiereException("Header not found: ID=" + hdrId);

        int chargeId = (p_C_Charge_ID > 0) ? p_C_Charge_ID : batchRepo.resolveDefaultChargeId();
        if (chargeId <= 0) throw new AdempiereException("C_Charge_ID required.");

        int docTypeId = batchRepo.resolveDocTypeId(p_C_DocType_ID);
        if (docTypeId <= 0) throw new AdempiereException("Could not resolve C_DocType_ID.");

        int currencyId = batchRepo.resolveCurrencyId(defaultCurrencyId);
        if (currencyId <= 0) throw new AdempiereException("Could not resolve C_Currency_ID.");

        // Load current header's unlinked lines, sorted by ZZ_Year then ZZ_Month
        List<X_ZZ_Monthly_Levy_Files> currentLines = levyRepo.getUnprocessedRows(hdrId);
        if (currentLines.isEmpty())
            return "No unlinked ZZ_Monthly_Levy_Files rows for header ID " + hdrId;

        // Batch cache keyed by "YYYY-Month", line-number cache keyed the same way
        Map<String, MInvoiceBatch_New> batchByYearMonth = new LinkedHashMap<>();
        Map<String, Integer>           lineNoByKey       = new LinkedHashMap<>();

        // Track BPs already processed in this run to avoid duplicates
        Set<Integer> processedBpIds = new HashSet<>();

        int createdLines   = 0;
        int skippedNoBP    = 0;
        int skippedNoApproval = 0;
        int skippedZero    = 0;

        for (X_ZZ_Monthly_Levy_Files currentLine : currentLines) {

            // --- Resolve BP ---
            String sdlNo = safe(currentLine.getZZ_SDL_No());
            if (sdlNo.isEmpty() || !sdlNo.startsWith("L")) {
                skippedNoBP++;
                continue;
            }
            MBPartner bp = partnerRepo.findActiveByValue(sdlNo);
            if (bp == null) {
                skippedNoBP++;
                continue;
            }
            int bpId = bp.getC_BPartner_ID();

            // --- Skip if already processed in this run ---
            if (processedBpIds.contains(bpId)) continue;

            // --- Year & month from the current line ---
            String lineYear  = safe(currentLine.getZZ_Year());
            String lineMonth = safe(currentLine.getZZ_Month());

            // --- Approval check for the current line's year ---
            if (!approvalsRepo.hasApprovedForYear(bpId, lineYear)) {
                skippedNoApproval++;
                continue;
            }

            // --- Build list of all contributing lines ---
            // Start with the current header's line itself
            List<X_ZZ_Monthly_Levy_Files> contributing = new ArrayList<>();
            contributing.add(currentLine);

            // Add prior unlinked lines for this BP from earlier headers
            List<X_ZZ_Monthly_Levy_Files> priorLines =
                    levyRepo.getPriorUnlinkedLinesForBP(sdlNo, lineYear, lineMonth);

            for (X_ZZ_Monthly_Levy_Files prior : priorLines) {
                String priorYear = safe(prior.getZZ_Year());
                // Only include if BP has approval for that prior line's year
                if (approvalsRepo.hasApprovedForYear(bpId, priorYear)) {
                    contributing.add(prior);
                }
            }

            // --- Sum MG amounts (negate for posting) ---
            BigDecimal total = BigDecimal.ZERO;
            for (X_ZZ_Monthly_Levy_Files r : contributing) {
                total = total.add(MoneyMath.nz(r.getZZ_MG()));
            }
            if (MoneyMath.isZero(total)) {
                skippedZero++;
                continue;
            }
            BigDecimal lineAmt = total.negate();

            // --- Get or create the batch for this year+month ---
            MInvoiceBatch_New batch = batchRepo.ensureBatchForYearMonth(
                    batchByYearMonth, hdr, currencyId, lineYear, lineMonth);

            String batchKey = lineYear + "-" + lineMonth;
            int lineNo = batchRepo.nextLineNo(lineNoByKey, batchKey, batch.getC_InvoiceBatch_ID());

            // --- Build line description ---
            String desc = DescriptionBuilder.buildLineDescription(
                    "MG", lineYear, lineMonth, sdlNo, contributing.size(), lineAmt);

            // --- Resolve BP location ---
            int bpLocId = partnerRepo.getBillToOrAnyLocation(bp);

            // --- Create the batch line ---
            int batchLineId = batchRepo.createBatchLine(
                    batch.getC_InvoiceBatch_ID(), docTypeId, lineNo,
                    bpId, bpLocId, chargeId, p_DateDoc,
                    lineAmt, desc);

            // --- Link all contributing levy lines to this batch line ---
            levyRepo.linkLinesToBatchLine(contributing, batchLineId);

            // --- Update batch control amount ---
            batchRepo.updateControlAmt(batch);

            processedBpIds.add(bpId);
            createdLines++;
        }

        return stats("OK", batchByYearMonth.size(), currentLines.size(), createdLines,
                skippedNoBP, skippedNoApproval, skippedZero);
    }

    private static String stats(String prefix, int batches, int sourceRows, int createdLines,
                                int skippedNoBP, int skippedNoApproval, int skippedZero) {
        return String.format(
            "%s. Batches created: %d. Source rows: %d. Lines created: %d. " +
            "Skipped (no BP): %d. Skipped (no approval): %d. Skipped (net zero): %d",
            prefix, batches, sourceRows, createdLines, skippedNoBP, skippedNoApproval, skippedZero);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
