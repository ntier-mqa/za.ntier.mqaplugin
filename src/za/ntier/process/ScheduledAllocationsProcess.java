package za.ntier.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import za.co.ntier.api.model.X_ZZ_AllocationSchedule;
import za.co.ntier.api.model.X_ZZ_Allocations;
import za.co.ntier.api.model.X_ZZ_Organization;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_AC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_OC;
import za.co.ntier.api.model.X_ZZ_QCTO_Alloc_Skills;

@Process(name = "za.ntier.process.ScheduledAllocationsProcess")
public class ScheduledAllocationsProcess extends SvrProcess
{

	private int totalAllocationsAdded = 0;

	@Override
	protected void prepare()
	{
	}

	@Override
	protected String doIt() throws Exception
	{
		int pInstanceId = getAD_PInstance_ID();

		if (pInstanceId <= 0)
		{
			return "Process must be executed from an Info Window selection.";
		}

		Set<String> legalNames = getSelectedLegalNames(pInstanceId);
		if (legalNames.isEmpty())
		{
			return "No records were selected in the Info Window or could not resolve trade names.";
		}

		X_ZZ_AllocationSchedule schedule = getOrCreateOpenSchedule();

		for (String legalName : legalNames)
		{
			processLegalNameAllocations(schedule, legalName);
		}

		if (schedule.get_ID() > 0)
		{
			addLog(	schedule.get_ID(), null, null,
					"Scheduled " + totalAllocationsAdded + " allocations under document " + schedule.getDocumentNo(),
					X_ZZ_AllocationSchedule.Table_ID, schedule.get_ID());
		}

		return "Added " + totalAllocationsAdded + " allocations to Schedule " + schedule.getDocumentNo();
	}

	/**
	 * Retrieves unique LegalName values from ZZ_AuditSchedule_v using the current process selection
	 * instance.
	 */
	private Set<String> getSelectedLegalNames(int pInstanceId)
	{
		Set<String> results = new HashSet<>();

		String sqlSelect = "SELECT DISTINCT ZZLegalName FROM ZZ_AuditSchedule_v WHERE ZZ_AuditSchedule_v_ID IN ("
							+ "  SELECT T_Selection_ID FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?"
							+ "  UNION "
							+ "  SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?"
							+ ") OR ZZ_AuditSchedule_v_UU IN ("
							+ "  SELECT T_Selection_UU FROM T_Selection_InfoWindow WHERE AD_PInstance_ID=?"
							+ "  UNION "
							+ "  SELECT T_Selection_UU FROM T_Selection WHERE AD_PInstance_ID=?"
							+ ")";

		try (PreparedStatement pstmt = DB.prepareStatement(sqlSelect, get_TrxName()))
		{
			pstmt.setInt(1, pInstanceId);
			pstmt.setInt(2, pInstanceId);
			pstmt.setInt(3, pInstanceId);
			pstmt.setInt(4, pInstanceId);
			try (ResultSet rs = pstmt.executeQuery())
			{
				while (rs.next())
				{
					String tn = rs.getString(1);
					if (tn != null && !tn.isBlank())
					{
						results.add(tn.trim());
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Error reading info window selection for trade names", e);
		}

		return results;
	}

	/**
	 * Looks for an open Draft Schedule that has at least one active allocation sub-line associated.
	 * If none is found, creates a fresh document.
	 */
	private X_ZZ_AllocationSchedule getOrCreateOpenSchedule()
	{
		// Look for existing Header
		String sql = "SELECT s.ZZ_AllocationSchedule_ID "
						+ "FROM ZZ_AllocationSchedule s "
						+ "WHERE s.ZZ_DocStatus = 'DR' "
						+ "  AND s.AD_Client_ID = ? "
						+ "  AND s.IsActive = 'Y' "
						+ "  AND EXISTS ("
						+ "      SELECT 1 FROM ZZ_Organization o "
						+ "      INNER JOIN ZZ_Allocations a ON a.ZZ_Organization_ID = o.ZZ_Organization_ID "
						+ "      WHERE o.ZZ_AllocationSchedule_ID = s.ZZ_AllocationSchedule_ID "
						+ "        AND a.IsOpenSchedule = 'Y'"
						+ "  ) "
						+ "ORDER BY s.Created DESC";

		int existingId = DB.getSQLValue(get_TrxName(), sql, Env.getAD_Client_ID(getCtx()));

		if (existingId > 0)
		{
			return new X_ZZ_AllocationSchedule(getCtx(), existingId, get_TrxName());
		}

		// Create New Header
		X_ZZ_AllocationSchedule newSchedule = new X_ZZ_AllocationSchedule(getCtx(), 0, get_TrxName());
		newSchedule.setAD_Org_ID(Env.getAD_Org_ID(getCtx()));
		newSchedule.setZZ_DocStatus(X_ZZ_AllocationSchedule.ZZ_DOCSTATUS_Draft);
		newSchedule.saveEx();

		return newSchedule;
	}

	/**
	 * Creates or fetches the Organization Sub Tab for a given legal name, then migrates all
	 * unscheduled underlying source allocations (OC, Skills, ACs) into nested Allocation lines.
	 */
	private void processLegalNameAllocations(X_ZZ_AllocationSchedule schedule, String legalName)
	{

		// Unscheduled OCs
		List<X_ZZ_QCTO_Alloc_OC> pendingOCs = new Query(getCtx(), X_ZZ_QCTO_Alloc_OC.Table_Name,
														"ZZLegalName=? AND NOT EXISTS (SELECT 1 FROM ZZ_Allocations a WHERE a.ZZ_QCTO_Alloc_OC_ID=ZZ_QCTO_Alloc_OC.ZZ_QCTO_Alloc_OC_ID AND a.IsOpenSchedule='Y')",
														get_TrxName())
																		.setParameters(legalName)
																		.setOnlyActiveRecords(true)
																		.list();

		// Unscheduled Skills
		List<X_ZZ_QCTO_Alloc_Skills> pendingSkills = new Query(	getCtx(), X_ZZ_QCTO_Alloc_Skills.Table_Name,
																"ZZLegalName=? AND NOT EXISTS (SELECT 1 FROM ZZ_Allocations a WHERE a.ZZ_QCTO_Alloc_Skills_ID=ZZ_QCTO_Alloc_Skills.ZZ_QCTO_Alloc_Skills_ID AND a.IsOpenSchedule='Y')",
																get_TrxName())
																				.setParameters(legalName)
																				.setOnlyActiveRecords(true)
																				.list();

		// Unscheduled ACs
		List<X_ZZ_QCTO_Alloc_AC> pendingACs = new Query(getCtx(), X_ZZ_QCTO_Alloc_AC.Table_Name,
														"ZZLegalName=? AND NOT EXISTS (SELECT 1 FROM ZZ_Allocations a WHERE a.ZZ_QCTO_Alloc_AC_ID=ZZ_QCTO_Alloc_AC.ZZ_QCTO_Alloc_AC_ID AND a.IsOpenSchedule='Y')",
														get_TrxName())
																		.setParameters(legalName)
																		.setOnlyActiveRecords(true)
																		.list();

		if (pendingOCs.isEmpty() && pendingSkills.isEmpty() && pendingACs.isEmpty())
		{
			return;
		}

		X_ZZ_Organization orgTab = getOrCreateOrganization(schedule, legalName);

		int lineCount = DB.getSQLValue(get_TrxName(), "SELECT COALESCE(MAX(LineNo),0) FROM ZZ_Allocations WHERE ZZ_Organization_ID=?", orgTab.get_ID());

		for (X_ZZ_QCTO_Alloc_OC oc : pendingOCs)
		{
			lineCount += 10;
			createAllocationLine(orgTab, oc, lineCount);
			totalAllocationsAdded++;
		}

		for (X_ZZ_QCTO_Alloc_Skills skill : pendingSkills)
		{
			lineCount += 10;
			createAllocationLine(orgTab, skill, lineCount);
			totalAllocationsAdded++;
		}

		for (X_ZZ_QCTO_Alloc_AC ac : pendingACs)
		{
			lineCount += 10;
			createAllocationLine(orgTab, ac, lineCount);
			totalAllocationsAdded++;
		}
	}

	/**
	 * Locates an existing Organization sub-record within a schedule for a trade name,
	 * or creates it if it doesn't represent one yet.
	 */
	private X_ZZ_Organization getOrCreateOrganization(X_ZZ_AllocationSchedule schedule, String legalName)
	{
		X_ZZ_Organization orgTab = new Query(	getCtx(), X_ZZ_Organization.Table_Name,
												"ZZ_AllocationSchedule_ID=? AND ZZLegalName=?", get_TrxName())
																												.setParameters(schedule.get_ID(), legalName)
																												.setOnlyActiveRecords(true)
																												.firstOnly();

		if (orgTab != null && orgTab.get_ID() > 0)
		{
			return orgTab;
		}

		orgTab = new X_ZZ_Organization(getCtx(), 0, get_TrxName());
		orgTab.setAD_Org_ID(schedule.getAD_Org_ID());
		orgTab.setZZ_AllocationSchedule_ID(schedule.get_ID());
		orgTab.setZZLegalName(legalName);
		orgTab.saveEx();

		return orgTab;
	}

	/**
	 * Builds a generic Level 2 Allocations line using field attributes replicated
	 * from the underlying specialized App Record (OC/Skills/AC).
	 */
	private void createAllocationLine(X_ZZ_Organization orgTab, PO source, int lineNo)
	{
		X_ZZ_Allocations child = new X_ZZ_Allocations(getCtx(), 0, get_TrxName());
		child.setAD_Org_ID(orgTab.getAD_Org_ID());
		child.setZZ_Organization_ID(orgTab.get_ID());
		child.setLineNo(lineNo);

		if (source instanceof X_ZZ_QCTO_Alloc_OC)
		{
			child.setZZ_QCTO_Alloc_OC_ID(source.get_ID());
		}
		else if (source instanceof X_ZZ_QCTO_Alloc_Skills)
		{
			child.setZZ_QCTO_Alloc_Skills_ID(source.get_ID());
		}
		else if (source instanceof X_ZZ_QCTO_Alloc_AC)
		{
			child.setZZ_QCTO_Alloc_AC_ID(source.get_ID());
		}

		// Field inheritance mapping rules
		copyField(source, child, "Name");
		copyField(source, child, "ZZSurname");
		copyField(source, child, "ZZLegalName");
		copyField(source, child, "ZZTradeName");
		copyField(source, child, "EMail");
		copyField(source, child, "Address1");
		copyField(source, child, "Address2");
		copyField(source, child, "City");
		copyField(source, child, "Region");
		copyField(source, child, "Postalcode");

		copyField(source, child, "ZZ_Qualification");
		copyField(source, child, "ZZ_QualityPartner");
		copyField(source, child, "ZZ_SAQAIDOrSPID");
		copyField(source, child, "ZZ_NQF_Level");

		copyField(source, child, "ZZ_AllocationNo");
		copyField(source, child, "ZZ_AllocationMonth");
		copyField(source, child, "ZZ_Designation");
		copyField(source, child, "ZZ_ContactTitle");
		copyField(source, child, "ZZ_CIPCNumber");

		copyField(source, child, "ZZ_AltContactName");
		copyField(source, child, "ZZ_AltContactSurname");
		copyField(source, child, "ZZ_AltContactEmail");
		copyField(source, child, "ZZ_AltContactTitle");

		copyField(source, child, "ZZ_QCTO_Allocation_ID");

		child.setIsOpenSchedule(true);

		child.setZZ_DocStatus(X_ZZ_Allocations.ZZ_DOCSTATUS_Draft);

		child.saveEx();
	}

	/**
	 * Internal mapping fallback feature mapping matching data properties safely.
	 */
	private void copyField(PO source, PO target, String columnName)
	{
		int srcIdx = source.get_ColumnIndex(columnName);
		int tgtIdx = target.get_ColumnIndex(columnName);

		if (srcIdx >= 0 && tgtIdx >= 0)
		{
			Object value = source.get_Value(columnName);
			if (value != null)
			{
				target.set_ValueOfColumn(columnName, value);
			}
		}
	}
}
