package za.ntier.repo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MInvoiceBatchLine;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.ntier.models.MInvoiceBatch_New;
import za.ntier.models.X_C_InvoiceBatch;
import za.ntier.models.X_ZZ_Monthly_Levy_Files_Hdr;

public class BatchRepository {

    private static final String ORG_UU  = "461178b6-f520-4d12-9a1e-5eec1ea46b79";
    private static final String TAX_UU  = "7036e67d-0d1e-4178-aa63-77b12e9f6495";

    private static final String SQL_ORG_ID = "SELECT AD_Org_ID   FROM AD_Org   WHERE AD_Org_UU=?";
    private static final String SQL_TAX_ID = "SELECT C_Tax_ID    FROM C_Tax    WHERE C_Tax_UU=?";

    private final Properties ctx; private final String trx; private final int adUserId; private final Timestamp dateDoc;

    public BatchRepository(Properties ctx, String trx, int adUserId, Timestamp dateDoc) {
        this.ctx = ctx; this.trx = trx; this.adUserId = adUserId; this.dateDoc = dateDoc;
    }

    public int resolveDefaultChargeId() {
        int id = DB.getSQLValue(trx,
            "SELECT C_Charge_ID FROM C_Charge WHERE UPPER(Name)=UPPER('3010 Grant Expenses - Mandatory') AND IsActive='Y' AND AD_Client_ID=? ORDER BY C_Charge_ID",
            Env.getAD_Client_ID(ctx));
        if (id <= 0)
            throw new IllegalStateException("Default charge '3010 Grant Expenses - Mandatory' not found for client " + Env.getAD_Client_ID(ctx));
        return id;
    }

    public int resolveDocTypeId(int preferred) {
        if (preferred > 0) return preferred;
        int id = DB.getSQLValue(trx,
            "SELECT C_DocType_ID FROM C_DocType WHERE DocBaseType='API' AND IsActive='Y' AND AD_Client_ID=? ORDER BY IsSOTrx DESC, C_DocType_ID",
            Env.getAD_Client_ID(ctx));
        if (id <= 0)
            throw new IllegalStateException("No active DocType with DocBaseType='API' found for client " + Env.getAD_Client_ID(ctx));
        return id;
    }

    public int resolveCurrencyId(int fallback) {
        int id = DB.getSQLValue(trx,
            "SELECT C_Currency_ID FROM C_AcctSchema WHERE AD_Client_ID=? AND IsActive='Y' ORDER BY C_AcctSchema_ID",
            Env.getAD_Client_ID(ctx));
        return id > 0 ? id : fallback;
    }

    /**
     * Returns (or creates) a batch for the given year+month combination.
     * Cache key is "YYYY-Month" e.g. "2025-November".
     */
    public MInvoiceBatch_New ensureBatchForYearMonth(Map<String, MInvoiceBatch_New> cache,
                                                     X_ZZ_Monthly_Levy_Files_Hdr hdr,
                                                     int currencyId, String year, String month) {
        String key = year + "-" + month;
        MInvoiceBatch_New cached = cache.get(key);
        if (cached != null) return cached;

        MInvoiceBatch_New b = new MInvoiceBatch_New(ctx, 0, trx);
        b.setAD_Org_ID(DB.getSQLValue(trx, SQL_ORG_ID, ORG_UU));
        b.setDateDoc(dateDoc);
        b.setC_Currency_ID(currencyId);
        b.setSalesRep_ID(adUserId);
        b.setZZ_Monthly_Levy_Files_Hdr_ID(hdr.get_ID());
     //   b.setZZ_Status(X_C_InvoiceBatch.ZZ_DOCSTATUS_Submitted);
        b.setZZ_IS_WSP_ATR(true);
        b.setZZ_DocStatus(X_C_InvoiceBatch.ZZ_DOCSTATUS_Draft);
        b.setZZ_DocAction(X_C_InvoiceBatch.ZZ_DOCACTION_Recommend);
        b.setIsSOTrx(false);
        b.setDescription("MG Generated for Year: " + year + " Month: " + month);
        b.setZZ_Year(year);
        b.setZZ_Month(hdr.getZZ_Month());
        b.setDocumentAmt(Env.ZERO);
        b.saveEx();

        cache.put(key, b);
        return b;
    }

    public int nextLineNo(Map<String, Integer> lineNoByKey, String key, int batchId) {
        Integer ln = lineNoByKey.get(key);
        if (ln == null) {
            ln = DB.getSQLValue(trx,
                "SELECT COALESCE(MAX(Line),0) FROM C_InvoiceBatchLine WHERE C_InvoiceBatch_ID=?",
                batchId);
            if (ln < 0)
                throw new IllegalStateException("Failed to query max line number for C_InvoiceBatch_ID=" + batchId);
        }
        ln += 10;
        lineNoByKey.put(key, ln);
        return ln;
    }

    public int createBatchLine(int batchId, int docTypeId, int lineNo, int bpId, int bpLocId,
                               int chargeId, Timestamp date, BigDecimal amount, String description) {
        MInvoiceBatchLine l = new MInvoiceBatchLine(ctx, 0, trx);
        l.setAD_Org_ID(DB.getSQLValue(trx, SQL_ORG_ID, ORG_UU));
        l.setC_InvoiceBatch_ID(batchId);
        l.setC_DocType_ID(docTypeId);
        l.setLine(lineNo);
        l.setC_BPartner_ID(bpId);
        if (bpLocId > 0) l.setC_BPartner_Location_ID(bpLocId);
        l.setC_Charge_ID(chargeId);
        l.setIsTaxIncluded(false);
        l.setC_Tax_ID(DB.getSQLValue(trx, SQL_TAX_ID, TAX_UU));
        l.setDateInvoiced(date);
        l.setDateAcct(date);
        l.setQtyEntered(Env.ONE);
        l.setPriceEntered(amount);
        l.setLineNetAmt(amount);
        l.setLineTotalAmt(amount);
        l.setDescription(description);
        l.saveEx();
        return l.getC_InvoiceBatchLine_ID();
    }

    public void updateControlAmt(MInvoiceBatch_New batch) {
        MInvoiceBatch_New fresh = new MInvoiceBatch_New(ctx, batch.getC_InvoiceBatch_ID(), batch.get_TrxName());
        fresh.setControlAmt(fresh.getDocumentAmt());
        fresh.saveEx();
    }
}
