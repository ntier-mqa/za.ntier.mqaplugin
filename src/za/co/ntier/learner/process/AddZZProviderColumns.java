package za.co.ntier.learner.process;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.M_Element;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;

/**
 * Adds the missing columns to the already-existing ZZProvider table (does NOT create a new
 * table - see "ZZProvider - New Columns to Add.txt" in the Learners Data Migration runbook
 * for the full column-by-column reasoning). Modelled on
 * za.ntier.process.CreateLookupReferenceTables (same idea: drive the Application Dictionary
 * directly rather than by hand in the UI) combined with the two building blocks iDempiere
 * itself uses to physically add a column:
 * <ul>
 *   <li>org.compiere.process.TableCreateColumns - the Element/MColumn creation pattern
 *       (get-or-create M_Element, then a new MColumn with Name/Description/AD_Reference_ID/
 *       FieldLength copied from it) is copied from here.</li>
 *   <li>org.compiere.process.ColumnSync - the "actually alter the physical table" step
 *       (MColumn.getSQLAdd(table), then execute it, splitting on DB.SQLSTATEMENT_SEPARATOR
 *       if the driver needs more than one statement) is copied from here.</li>
 * </ul>
 * Unlike those two standard processes, this one does not discover columns by reflection or
 * take a Record_ID parameter - the column list is hardcoded below, matched exactly to the
 * mapping doc, since we already know precisely what we want to add.
 *
 * <p>Idempotent/resumable: any column that already exists on ZZProvider (by name) is
 * skipped, so this can be re-run safely.
 *
 * <p><b>Scope of this pass (2026-07-14):</b>
 * <ul>
 *   <li>Section A columns (plain values) are added as their final, real type.</li>
 *   <li>Section B columns (Provider_Type_ID, Provider_Class_ID,
 *       Provider_Accreditation_Status_ID, Quality_Assurance_Body_ID,
 *       Accreditation_Type_ID, Provider_Internal_External_ID, Accrediting_Council_ID,
 *       Provider_Application_ID, Saqa_QA_ID) are added as PLAIN INTEGER columns, NOT as
 *       proper Table/List references yet - their target iDempiere reference tables
 *       (ProviderType, ProviderClass, etc.) don't exist as real AD_Tables yet, only as
 *       raw staged MSSQL data (ms_lkpprovidertype etc.). Once those reference tables are
 *       built (a separate process, same idea as za.ntier.process.CreateLookupReferenceTables),
 *       these 9 columns' AD_Reference_ID/AD_Reference_Value_ID will need to be updated to
 *       Table/TableDir and the physical column type may need to change - tracked as a
 *       follow-up, not done here.</li>
 *   <li>C_BPartner_ID (organisationid's resolved target) IS added as a proper Table
 *       Direct reference (AD_Reference_ID=19) - C_BPartner is a real, already-existing
 *       standard iDempiere table, so this one doesn't have the same problem as Section B.</li>
 *   <li>"levy" is deliberately NOT included - still unresolved (no candidate lookup table
 *       found, and it's unclear whether it's even a reference at all - see the mapping
 *       doc). Add it here once it's resolved.</li>
 * </ul>
 */
@Process(name = "za.co.ntier.learner.process.AddZZProviderColumns")
public class AddZZProviderColumns extends SvrProcess {

    private static final String TABLE_NAME = "ZZProvider";
    /** Matches the EntityType already used by every existing ZZProvider column. */
    private static final String ENTITY_TYPE = "MQA Learner";

    private static final class ColumnSpec {
        final String columnName;
        final int referenceId;
        final int fieldLength;
        final String description;

        ColumnSpec(String columnName, int referenceId, int fieldLength, String description) {
            this.columnName = columnName;
            this.referenceId = referenceId;
            this.fieldLength = fieldLength;
            this.description = description;
        }
    }

    private static final List<ColumnSpec> COLUMNS = buildColumnSpecs();

    private static List<ColumnSpec> buildColumnSpecs() {
        List<ColumnSpec> specs = new ArrayList<>();

        // ---- Section A - plain columns (String=4000/DateTime=7/YesNo=1, matching the
        // exact FieldLength convention already used by every other custom column on this
        // table and its sibling QCTO tables - confirmed via AD_Column, see class Javadoc) ----
        specs.add(new ColumnSpec("Accreditation_Start_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationstartdate"));
        specs.add(new ColumnSpec("Accreditation_End_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationenddate"));
        specs.add(new ColumnSpec("Saqa_Code", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.saqacode"));
        specs.add(new ColumnSpec("Saqa_Provider_Code", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.saqaprovidercode"));
        specs.add(new ColumnSpec("Dhet_Registration_Start_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.dhetregistrationstartdate"));
        specs.add(new ColumnSpec("Dhet_Registration_End_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.dhetregistrationenddate"));
        specs.add(new ColumnSpec("Dhet_Registration_Number", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.dhetregistrationnumber"));
        specs.add(new ColumnSpec("Is_Saica", DisplayType.YesNo, 1,
                "ms_skillsdevelopmentprovider.issaica"));
        specs.add(new ColumnSpec("Web_Address", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.webaddress"));
        specs.add(new ColumnSpec("Qcto_Provider_Number", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.qctoprovidernumber"));
        specs.add(new ColumnSpec("Application_Received_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.applicationreceiveddate"));
        specs.add(new ColumnSpec("Provider_Alert_Email", DisplayType.String, 4000,
                "ms_skillsdevelopmentprovider.provideralertemail"));
        specs.add(new ColumnSpec("Accreditation_Review_Date", DisplayType.DateTime, 7,
                "ms_skillsdevelopmentprovider.accreditationreviewdate"));

        // ---- Section B - reference-target columns, added as PLAIN INTEGER for now - see
        // class Javadoc "Scope of this pass" ----
        specs.add(new ColumnSpec("Provider_Type_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.providertypeid - target lkpProviderType (staged as ms_lkpprovidertype), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Provider_Class_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.providerclassid - target lkpProviderClass (staged as ms_lkpproviderclass), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Provider_Accreditation_Status_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.provideraccreditationstatusid - target lkpProviderAccreditationStatus (staged as ms_lkpprovideraccreditationstatus), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Quality_Assurance_Body_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.qualityassurancebodyid - target lkpQualityAssuranceBody (staged as ms_lkpqualityassurancebody), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Accreditation_Type_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.accreditationtypeid - target lkpProviderAccreditationType (staged as ms_lkpprovideraccreditationtype), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Provider_Internal_External_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.providerinternalexternalid - target lkpProviderInternalExternal (staged as ms_lkpproviderinternalexternal), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Accrediting_Council_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.accreditingcouncil - target lkpProviderAccreditingCouncil (staged as ms_lkpprovideraccreditingcouncil), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Provider_Application_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.providerapplicationid - target lkpProviderApplication (staged as ms_lkpproviderapplication), not yet a real AD_Table"));
        specs.add(new ColumnSpec("Saqa_QA_ID", DisplayType.Integer, 10,
                "ms_skillsdevelopmentprovider.saqaqaid - stores lkpSAQADataSuppliers.ETQAID (staged as ms_lkpsaqadatasuppliers), not yet a real AD_Table; matched as text (ETQAID is nvarchar), see mapping doc Section D"));

        // ---- Section C - C_BPartner_ID: real Table Direct reference, C_BPartner already
        // exists. Resolution path: organisationid -> ms_organisation.sdlnumber -> match
        // against c_bpartner.zz_sdl_no (existing custom column) -> C_BPartner_ID. That
        // matching is done by the migration process that populates this column, not here -
        // this process only adds the column itself. ----
        specs.add(new ColumnSpec("C_BPartner_ID", DisplayType.TableDir, 10,
                "ms_skillsdevelopmentprovider.organisationid, resolved via ms_organisation.sdlnumber = c_bpartner.zz_sdl_no"));

        return specs;
    }

    @Override
    protected void prepare() {
        for (ProcessInfoParameter para : getParameter()) {
            MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
        }
    }

    @Override
    protected String doIt() throws Exception {
        MTable table = new Query(getCtx(), MTable.Table_Name, "UPPER(TableName)=UPPER(?)", get_TrxName())
                .setParameters(TABLE_NAME)
                .first();
        if (table == null) {
            throw new AdempiereException(TABLE_NAME + " not found in AD_Table");
        }

        int added = 0;
        int skipped = 0;
        for (ColumnSpec spec : COLUMNS) {
            if (table.getColumn(spec.columnName) != null) {
                addLog(TABLE_NAME + "." + spec.columnName + " already exists - skipped.");
                skipped++;
                continue;
            }
            addColumn(table, spec);
            added++;
        }

        return TABLE_NAME + ": " + added + " column(s) added, " + skipped + " already existed.";
    }

    /**
     * Same two-step recipe as TableCreateColumns (Application Dictionary registration) +
     * ColumnSync (physical ALTER TABLE) - see class Javadoc.
     */
    private void addColumn(MTable table, ColumnSpec spec) {
        M_Element element = M_Element.get(getCtx(), spec.columnName);
        if (element == null) {
            element = new M_Element(getCtx(), spec.columnName, ENTITY_TYPE, get_TrxName());
            element.setName(spec.columnName.replace('_', ' ').trim());
            element.setDescription(spec.description);
            element.saveEx();
        }

        MColumn column = new MColumn(table);
        column.set_TrxName(get_TrxName());
        column.setEntityType(ENTITY_TYPE);
        column.setColumnName(element.getColumnName());
        column.setName(element.getName());
        column.setDescription(element.getDescription());
        column.setAD_Element_ID(element.getAD_Element_ID());
        column.setIsMandatory(false);
        column.setAD_Reference_ID(spec.referenceId);
        column.setFieldLength(spec.fieldLength);
        column.setIsUpdateable(true);
        column.saveEx();

        String sql = column.getSQLAdd(table);
        if (sql == null || sql.trim().isEmpty()) {
            throw new AdempiereException("MColumn.getSQLAdd() returned empty SQL for " + spec.columnName);
        }

        int result;
        if (sql.indexOf(DB.SQLSTATEMENT_SEPARATOR) == -1) {
            result = DB.executeUpdateEx(sql, get_TrxName());
        } else {
            result = 0;
            for (String statement : sql.split(DB.SQLSTATEMENT_SEPARATOR)) {
                result += DB.executeUpdateEx(statement, get_TrxName());
            }
        }
        if (result == -1) {
            throw new AdempiereException("ALTER TABLE failed for " + TABLE_NAME + "." + spec.columnName + ": " + sql);
        }

        addLog(TABLE_NAME + "." + spec.columnName + " added (" + sql + ")");
    }
}
