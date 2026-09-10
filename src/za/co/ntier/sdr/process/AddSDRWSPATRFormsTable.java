package za.co.ntier.sdr.process;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DisplayType;

import za.co.ntier.learner.process.AddColumnsSupport;

/**
 * Phase 5 (see "Phase 5 - WSPATR Family - Mapping.txt"): creates the brand new SDR_WSPATRForms catalog
 * table (32 source rows). Built as a full main-table (not the generic Value/Name reference-table
 * engine) because it carries several ETL/import-tooling configuration columns (Excel sheet names,
 * stored-proc names, template paths) beyond a simple lookup shape - CONFIRMED 2026-09-04 (user
 * decision): carried anyway for completeness with the source schema despite low business value in a
 * read-only window.
 *
 * <p>Must be built before AddSDRWSPATREvaluationVerificationChecklistTable, which FKs to this table.
 *
 * <p>Schema only - no data population.
 */
@Process(name = "za.co.ntier.sdr.process.AddSDRWSPATRFormsTable")
public class AddSDRWSPATRFormsTable extends SvrProcess {

    private static final String TABLE_NAME = "SDR_WSPATRForms";
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
                "A WSPATR form definition (mssdr_wspatrforms), including its ETL/import-tooling "
                + "configuration columns", ENTITY_TYPE, ACCESS_LEVEL, get_TrxName());

        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FormName", DisplayType.String, 250,
                "mssdr_wspatrforms.formname", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FormDescription", DisplayType.String, 250,
                "mssdr_wspatrforms.formdescription", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FinancialYear_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrforms.financialyearid -> SDR_FinancialYear. CONFIRMED 100% match", ENTITY_TYPE,
                get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_FormType_ID", DisplayType.TableDir, 10,
                "mssdr_wspatrforms.formtypeid -> SDR_FormType. CONFIRMED 100% match", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_IOrdinal", DisplayType.Integer, 10,
                "mssdr_wspatrforms.iordinal", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ExcelSheetName", DisplayType.String, 250,
                "mssdr_wspatrforms.excelsheetname (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportTableName", DisplayType.String, 250,
                "mssdr_wspatrforms.importtablename (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ExcelStartRow", DisplayType.Integer, 10,
                "mssdr_wspatrforms.excelstartrow (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ExcelEndColumn", DisplayType.Integer, 10,
                "mssdr_wspatrforms.excelendcolumn (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportReportProc", DisplayType.String, 250,
                "mssdr_wspatrforms.importreportproc (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ImportTemplate", DisplayType.String, 250,
                "mssdr_wspatrforms.importtemplate (ETL tooling config)", ENTITY_TYPE, get_TrxName());
        AddColumnsSupport.registerColumn(getCtx(), table, "SDR_ExportReportProc", DisplayType.String, 250,
                "mssdr_wspatrforms.exportreportproc (ETL tooling config)", ENTITY_TYPE, get_TrxName());

        AddColumnsSupport.finalizeNewTable(table, get_TrxName(), this::addLog);

        return TABLE_NAME + " created with 12 business columns.";
    }
}
