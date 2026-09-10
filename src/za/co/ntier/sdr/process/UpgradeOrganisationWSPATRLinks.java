package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * One-off follow-up to the Organisation family (Phase 3): SDR_OrganisationDocuments.SDR_WSPATR_ID and
 * SDR_OrganisationEmails.SDR_WSPATR_ID were both built as plain integers because SDR_WSPATR didn't
 * exist yet (see those classes' Javadoc). Now that Phase 5 has built SDR_WSPATR, this upgrades both
 * columns to a proper Table(18) reference via {@link AddColumnsSupport#upgradeColumnToTableReference} -
 * metadata only, no physical DDL.
 *
 * <p>SDR_WSPATR has no text column that reads as a natural "name" (its own columns are all lookups,
 * dates and audit-trail user references) - the plain "id" recon column (100% populated, traces back to
 * the source mssdr_wspatr.id) is used as the display column instead.
 *
 * <p>Idempotent - safe to run more than once, and must be run only after AddSDRWSPATRTable.
 */
@Process(name = "za.co.ntier.sdr.process.UpgradeOrganisationWSPATRLinks")
public class UpgradeOrganisationWSPATRLinks extends SvrProcess {

    private static final String ENTITY_TYPE = "U";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        AddColumnsSupport.upgradeColumnToTableReference(getCtx(), "SDR_OrganisationDocuments", "SDR_WSPATR_ID",
                "SDR_WSPATR", "id", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.upgradeColumnToTableReference(getCtx(), "SDR_OrganisationEmails", "SDR_WSPATR_ID",
                "SDR_WSPATR", "id", ENTITY_TYPE, get_TrxName(), this::addLog);

        return "SDR_OrganisationDocuments/SDR_OrganisationEmails.SDR_WSPATR_ID upgraded to Table references.";
    }
}
