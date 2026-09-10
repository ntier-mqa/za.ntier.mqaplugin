package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 3 (see "Phase 3 - Organisation Family - Mapping.txt"): creates the brand new
 * SDR_OrganisationBankingDetails child table (1,390 source rows).
 *
 * <p>SDR_AdminDetailsCorrect_ID/SDR_BankDetailsCorrect_ID/SDR_BankDetailsChanged_ID all resolve to the
 * shared SDR_YesNo table (platform-wide "*YesNoID" convention, all CONFIRMED 100% match) - none match
 * "SDR_YesNo" by name, so each needs the explicit AD_Reference/AD_Ref_Table override, resolved once and
 * reused. SDR_NewRegCompany_ID was NOT tested against SDR_YesNo (per mapping doc) and is left as a plain
 * integer.
 *
 * <p>SDR_SDF_ID targets SDR_SDF, which does not exist yet (SDF is Phase 4, not yet built) - the
 * relationship is CONFIRMED real (root-caused in the SDF family's own mapping pass: the 289/1,390
 * matching rows are internally consistent; the rest most likely reference hard-deleted SDF rows), but
 * the physical AD_Reference/AD_Ref_Table override can't be wired until SDR_SDF exists. Carried as a
 * plain integer for now - a follow-up pass (once Phase 4 is built) should upgrade this column to a
 * proper Table reference using {@link AddColumnsSupport#findOrCreateTableReference}, the same way an
 * existing column can be widened after the fact.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDROrganisationBankingDetailsTable")
public class AddSDROrganisationBankingDetailsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_OrganisationBankingDetails";
    private static final String ENTITY_TYPE = "U";
    private static final String ACCESS_LEVEL = "3";

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        MTable existing = AddColumnsSupport.findTable(getCtx(), TABLE_NAME, get_TrxName());
        if (existing != null) {
            addLog(TABLE_NAME + " already exists - not recreated.");
            return TABLE_NAME + " already exists - no action taken.";
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "An organisation's banking details (mssdr_organisationbankingdetails)", ENTITY_TYPE, ACCESS_LEVEL,
                get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Organisation_ID", DisplayType.TableDir, 10,
                "mssdr_organisationbankingdetails.organisationid -> SDR_Organisation", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountHolder", DisplayType.String, 250,
                "mssdr_organisationbankingdetails.accountholder", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_BankName_ID", DisplayType.TableDir, 10,
                "mssdr_organisationbankingdetails.banknameid -> SDR_BankName", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountType_ID", DisplayType.TableDir, 10,
                "mssdr_organisationbankingdetails.accounttypeid -> SDR_AccountType", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_BranchName", DisplayType.String, 250,
                "mssdr_organisationbankingdetails.branchname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_BranchCode", DisplayType.String, 50,
                "mssdr_organisationbankingdetails.branchcode", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_Status", DisplayType.String, 20,
                "mssdr_organisationbankingdetails.status (free text, not checked for a fixed value set)",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_VerifiedBy", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetails.verifiedby - CHECKED: always 0/null across all rows, "
                + "effectively a dead column - carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EvaluatedBy", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetails.evaluatedby - same as VerifiedBy, always 0/null", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_VerifiedDate", DisplayType.DateTime, 7,
                "mssdr_organisationbankingdetails.verifieddate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_EvaluatedDate", DisplayType.DateTime, 7,
                "mssdr_organisationbankingdetails.evaluateddate", ENTITY_TYPE, get_TrxName());

        int yesNoRefId = AddColumnsSupport.findOrCreateTableReference(getCtx(), "SDR_YesNo", ENTITY_TYPE,
                get_TrxName(), this::addLog);
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_AdminDetailsCorrect_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_organisationbankingdetails.admindetailscorrectid -> SDR_YesNo. CONFIRMED 100% match",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_BankDetailsCorrect_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_organisationbankingdetails.bankdetailscorrectid -> SDR_YesNo. CONFIRMED 100% match",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumnWithValue(getCtx(), table, "SDR_BankDetailsChanged_ID", DisplayType.Table,
                yesNoRefId, 10,
                "mssdr_organisationbankingdetails.bankdetailschangedid -> SDR_YesNo. CONFIRMED 100% match",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_NewRegCompany_ID", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetails.newregcompanyid - UNMAPPED, not tested against SDR_YesNo "
                + "or anything else - carried as a plain value", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_RegistrationDate", DisplayType.DateTime, 7,
                "mssdr_organisationbankingdetails.registrationdate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ConfirmDetails", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetails.confirmdetails (tinyint flag)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_AccountNumber", DisplayType.String, 250,
                "mssdr_organisationbankingdetails.accountnumber - NOTE: source default is NEWID(), values "
                + "are GUIDs not real bank account numbers. CONFIRMED 2026-09-04 (user decision): shown "
                + "anyway for completeness with the source schema", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_SDF_ID", DisplayType.Integer, 10,
                "mssdr_organisationbankingdetails.sdfid -> SDR_SDF (not yet built, Phase 4) - relationship "
                + "CONFIRMED real (289/1,390 match, internally consistent) but carried as a plain integer "
                + "until SDR_SDF exists and this column can be upgraded to a proper Table reference",
                ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 17 business columns.";
    }
}
