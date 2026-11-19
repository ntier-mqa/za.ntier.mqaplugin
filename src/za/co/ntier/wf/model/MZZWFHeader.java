package za.co.ntier.wf.model;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;

import za.co.ntier.api.model.I_ZZLearnerDGProgramme;
import za.co.ntier.api.model.I_ZZ_Program_Master_Data;
import za.co.ntier.api.model.X_ZZLearnerDGProgramme;
import za.co.ntier.api.model.X_ZZ_Program_Master_Data;
import za.ntier.models.X_ZZ_WF_Header;

public class MZZWFHeader extends X_ZZ_WF_Header {
	private static final long serialVersionUID = 1L;

	private static final String WHERE_LINES_ACTIVE_WITH_SEQ =
			MZZWFLines.COLUMNNAME_ZZ_WF_Header_ID + "=? AND " + MZZWFLines.COLUMNNAME_SeqNo + " IS NOT NULL";

	public MZZWFHeader(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
	}

	public MZZWFHeader(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Legacy table-only lookup retained for callers outside this plugin.
	 * Workflow execution should use the overload with document client and Unit.
	 */
	@Deprecated
	public static MZZWFHeader forTable(Properties ctx, int adTableId, String trxName) {
		return new Query(ctx, Table_Name, COLUMNNAME_AD_Table_ID + "=?", trxName)
				.setParameters(adTableId)
				.setOnlyActiveRecords(true)
				.first();
	}

	/** Compatibility overload; document-aware callers should supply the client explicitly. */
	@Deprecated
	public static MZZWFHeader forTable(Properties ctx, int adTableId, int adOrgId, String trxName) {
		return forTable(ctx, adTableId, Env.getAD_Client_ID(ctx), adOrgId, trxName);
	}

	/**
	 * Resolve an active workflow for a document record.
	 * Derives the Unit (organization) from the DG Programme associated with the selected
	 * Registration record, matching against the organization configured in the workflow,
	 * and falling back to the default workflow (* Unit) if no match is found.
	 */
	public static MZZWFHeader forTable(Properties ctx, PO po, String trxName) {
		if (po == null) {
			return null;
		}
		int adOrgId = resolveOrgId(po);
		return forTable(ctx, po.get_Table_ID(), po.getAD_Client_ID(), adOrgId, trxName);
	}

	public static MZZWFHeader forTable(PO po, String trxName) {
		return forTable(po != null ? po.getCtx() : Env.getCtx(), po, trxName);
	}

	public static MZZWFHeader forTable(PO po) {
		return forTable(po, po != null ? po.get_TrxName() : null);
	}

	/**
	 * Resolve an active workflow using the document's ownership, not the login context.
	 * Checks the client's Unit override, the client's * Unit, then the System * Unit.
	 * Returns null if none exists; duplicate headers at a matching level are an error.
	 */
	public static MZZWFHeader forTable(Properties ctx, int adTableId, int adClientId, int adOrgId, String trxName) {
		MZZWFHeader header = findActiveHeader(ctx, adTableId, adClientId, adOrgId, trxName);
		if (header == null && adOrgId != 0) {
			header = findActiveHeader(ctx, adTableId, adClientId, 0, trxName);
		}
		if (header == null && adClientId != 0) {
			header = findActiveHeader(ctx, adTableId, 0, 0, trxName);
		}
		return header;
	}

	/**
	 * Resolves the organization (Unit) to match against the workflow.
	 * For Registration records associated with a DG Programme, the organization of the DG Programme
	 * is used instead of the logged-in user's organization or the record's AD_Org_ID.
	 * Falls back to po.getAD_Org_ID() if no DG Programme is associated or for non-registration records.
	 */
	private static int resolveOrgId(PO po) {
		if (po == null) {
			return 0;
		}

		// If the PO itself is the DG Programme Master Data, use its own AD_Org_ID directly.
		if (I_ZZ_Program_Master_Data.Table_Name.equalsIgnoreCase(po.get_TableName())) {
			return po.getAD_Org_ID();
		}

		// Only derive organisation from DG Programme for Registration records.
		if (!isRegistrationRecord(po)) {
			return po.getAD_Org_ID();
		}

		int dgProgramId = resolveDGProgramId(po);
		if (dgProgramId > 0) {
			X_ZZ_Program_Master_Data prog = new X_ZZ_Program_Master_Data(po.getCtx(), dgProgramId, po.get_TrxName());
			if (prog.get_ID() > 0) {
				return prog.getAD_Org_ID();
			}
		}

		return po.getAD_Org_ID();
	}

	/**
	 * Checks if the document record represents a Learner Registration record.
	 */
	private static boolean isRegistrationRecord(PO po) {
		if (po == null) {
			return false;
		}
		String tableName = po.get_TableName();
		if (I_ZZLearnerDGProgramme.Table_Name.equalsIgnoreCase(tableName)) {
			return true;
		}
		return po.get_ColumnIndex(I_ZZLearnerDGProgramme.COLUMNNAME_ZZLearnerDGProgramme_ID) >= 0;
	}

	/**
	 * Derives the DG Programme ID from the registration record if applicable.
	 */
	private static int resolveDGProgramId(PO po) {
		int programId = extractProgramId(po);
		if (programId <= 0 && po.get_ColumnIndex(I_ZZLearnerDGProgramme.COLUMNNAME_ZZLearnerDGProgramme_ID) >= 0
				&& !I_ZZLearnerDGProgramme.Table_Name.equalsIgnoreCase(po.get_TableName())) {
			int dgRegId = po.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_ZZLearnerDGProgramme_ID);
			if (dgRegId > 0) {
				X_ZZLearnerDGProgramme dgReg = new X_ZZLearnerDGProgramme(po.getCtx(), dgRegId, po.get_TrxName());
				if (dgReg.get_ID() > 0) {
					programId = extractProgramId(dgReg);
				}
			}
		}
		return programId;
	}

	/**
	 * Helper to extract DG Programme ID from a record containing Program Master Data or DG Program columns.
	 */
	private static int extractProgramId(PO po) {
		if (po == null) {
			return 0;
		}
		if (po.get_ColumnIndex(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_Program_Master_Data_ID) >= 0) {
			int id = po.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_Program_Master_Data_ID);
			if (id > 0) {
				return id;
			}
		}
		if (po.get_ColumnIndex(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_DGProgram_ID) >= 0) {
			return po.get_ValueAsInt(I_ZZLearnerDGProgramme.COLUMNNAME_ZZ_DGProgram_ID);
		}
		return 0;
	}

	private static MZZWFHeader findActiveHeader(Properties ctx, int adTableId, int adClientId, int adOrgId, String trxName) {
		String whereClause = COLUMNNAME_AD_Table_ID + "=? AND "
				+ COLUMNNAME_AD_Client_ID + "=? AND "
				+ COLUMNNAME_AD_Org_ID + "=?";
		List<MZZWFHeader> headers = new Query(ctx, Table_Name, whereClause, trxName)
				.setParameters(adTableId, adClientId, adOrgId)
				.setOnlyActiveRecords(true)
				.list();
		if (headers.size() > 1) {
			throw new AdempiereException("Multiple active workflows configured for table "
					+ adTableId + ", client " + adClientId + " and Unit " + adOrgId
					+ ". Please keep only one active workflow for this combination.");
		}
		return headers.isEmpty() ? null : headers.get(0);
	}

	public static MZZWFHeader getById(Properties ctx, int headerId, String trxName) {
		return new MZZWFHeader(ctx, headerId, trxName);
	}

	/**
	 * Get all active lines for this workflow header ordered by sequence.
	 */
	public List<MZZWFLines> getLines() {
		return MZZWFLines.listByHeaderOrderSeq(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Get the first active line of this workflow header.
	 */
	public MZZWFLines getFirstLine() {
		return getFirstLine(getCtx(), get_ID(), get_TrxName());
	}

	/**
	 * Get the minimum sequence number among active lines of this workflow header.
	 */
	public int getMinSeqNo() {
		return getMinSeqNo(getCtx(), get_ID(), get_TrxName());
	}

	public static int getMinSeqNo(Properties ctx, int wfHeaderId, String trxName) {
		Integer min = new Query(ctx, MZZWFLines.Table_Name, WHERE_LINES_ACTIVE_WITH_SEQ, trxName)
				.setParameters(wfHeaderId)
				.setOnlyActiveRecords(true)
				.aggregate(MZZWFLines.COLUMNNAME_SeqNo, Query.AGGREGATE_MIN, Integer.class);

		return (min != null) ? min.intValue() : Integer.MAX_VALUE;
	}

	public static MZZWFLines getFirstLine(Properties ctx, int wfHeaderId, String trxName) {
		String orderBy = MZZWFLines.COLUMNNAME_SeqNo + " ASC, " + MZZWFLines.COLUMNNAME_ZZ_WF_Lines_ID + " ASC";
		return new Query(ctx, MZZWFLines.Table_Name, WHERE_LINES_ACTIVE_WITH_SEQ, trxName)
				.setParameters(wfHeaderId)
				.setOnlyActiveRecords(true)
				.setOrderBy(orderBy)
				.first();
	}
}
