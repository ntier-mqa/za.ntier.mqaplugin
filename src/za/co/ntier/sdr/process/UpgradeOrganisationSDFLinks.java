package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * One-off follow-up to the Organisation family (Phase 3): SDR_OrganisationBankingDetails.SDR_SDF_ID
 * and SDR_OrganisationBankingDetailsDocumentUpload.SDR_SDF_ID were both built as plain integers because
 * SDR_SDF didn't exist yet at the time (see those classes' Javadoc). Now that Phase 4 has built
 * SDR_SDF, this upgrades both columns to a proper Table(18) reference via
 * {@link AddColumnsSupport#upgradeColumnToTableReference} - metadata only, no physical DDL (the
 * underlying column type doesn't change).
 *
 * <p>SDR_SDF has no generic "Name" column (same shape as SDR_Person/SDR_Organisation) - CONFIRMED
 * 2026-09-05 the hard way, first run failed with the same "no 'Name' column" error Person's
 * self-reference hit. SDR_CurrentOccupation is used as the display column instead: 100% populated
 * (2,452/2,452), unlike SDR_CertificateNumber (87.3%).
 *
 * <p>Idempotent - safe to run more than once, and must be run only after AddSDRSDFTable.
 */
@Process(name = "za.co.ntier.sdr.process.UpgradeOrganisationSDFLinks")
public class UpgradeOrganisationSDFLinks extends SvrProcess {

    private static final String ENTITY_TYPE = "U";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        AddColumnsSupport.upgradeColumnToTableReference(getCtx(), "SDR_OrganisationBankingDetails", "SDR_SDF_ID",
                "SDR_SDF", "SDR_CurrentOccupation", ENTITY_TYPE, get_TrxName(), this::addLog);
        AddColumnsSupport.upgradeColumnToTableReference(getCtx(), "SDR_OrganisationBankingDetailsDocumentUpload",
                "SDR_SDF_ID", "SDR_SDF", "SDR_CurrentOccupation", ENTITY_TYPE, get_TrxName(), this::addLog);

        return "SDR_OrganisationBankingDetails(DocumentUpload).SDR_SDF_ID upgraded to Table references.";
    }
}
