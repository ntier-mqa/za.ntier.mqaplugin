package za.co.ntier.learner.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport.ReferenceColumnSpec;

/**
 * Creates the brand new ZZLearnerQCTOArtisanEISADetails table (trade test date/result for a
 * learner's QCTO artisan enrolment) - Phase 1, see "Phase 1 - New Tables - Mapping.txt" table
 * #2. Same engine as {@link AddZZLearnerQCTOArtisanDocumentTable} - see that class's Javadoc
 * for the full write-up.
 *
 * <p>The "Results" reference table (from ms_lkpartisanstradetestsresults) is SHARED with
 * {@link AddZZLearnerQCTOLearnershipEISADetailsTable} - whichever of the two processes runs
 * first creates it, the other reuses it as-is.
 */
@Process(name = "za.co.ntier.learner.process.AddZZLearnerQCTOArtisanEISADetailsTable")
public class AddZZLearnerQCTOArtisanEISADetailsTable extends SvrProcess {

    private static final String TABLE_NAME = "ZZLearnerQCTOArtisanEISADetails";
    private static final String ENTITY_TYPE = "MQA Learner";
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

        ReferenceColumnSpec resultsSpec = new ReferenceColumnSpec("Results_ID",
                "ms_lkpartisanstradetestsresults", "id", "description",
                "ms_learnerqctoartisaneisadetails.resultsid / ms_learnerqctolearnershipeisadetails.resultsid");
        String resultsTableName = resultsSpec.targetTableName();
        MTable resultsTable = AddColumnsSupport.findTable(getCtx(), resultsTableName, get_TrxName());
        if (resultsTable == null) {
            resultsTable = AddColumnsSupport.createReferenceTableSchema(getCtx(), resultsTableName,
                    "Reference values for " + resultsSpec.description, ENTITY_TYPE, ACCESS_LEVEL, get_TrxName(),
                    this::addLog);
            AddColumnsSupport.populateReferenceTable(getCtx(), resultsTable, resultsSpec, get_TrxName(), this::addLog);
            addLog("Created and populated reference table " + resultsTableName + ".");
        } else {
            addLog(resultsTableName + " already exists - left as-is (not re-populated).");
        }

        MTable table = AddColumnsSupport.createNewTableSchema(getCtx(), TABLE_NAME,
                "Trade test date/result for a learner's QCTO artisan enrolment",
                ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "ZZLearnerQCTOArtisans_ID", DisplayType.TableDir, 10,
                "ms_learnerqctoartisaneisadetails.learnerqctoartisanid -> zzlearnerqctoartisans",
                ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "EISA_Date", DisplayType.DateTime, 7,
                "ms_learnerqctoartisaneisadetails.eisadate", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Trade_Test_Number", DisplayType.Integer, 10,
                "ms_learnerqctoartisaneisadetails.tradetestnumber", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "Results_ID", DisplayType.TableDir, 10,
                resultsSpec.description + " -> " + resultsTableName, ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 4 business columns.";
    }
}
