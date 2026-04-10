package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;

public class MZZSDRTempBranch extends X_ZZ_SDR_Temp_Branch {
	
    private static final String TEMP_BRANCH_MAILTEXT_UU = "05011c3b-c1db-4241-8f11-a595598bb742";

	public MZZSDRTempBranch(Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName) {
		super(ctx, ZZ_SDR_Temp_Branch_ID, trxName);
	}

	public MZZSDRTempBranch(Properties ctx, int ZZ_SDR_Temp_Branch_ID, String trxName, String... virtualColumns) {
		super(ctx, ZZ_SDR_Temp_Branch_ID, trxName, virtualColumns);
	}

	public MZZSDRTempBranch(Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName) {
		super(ctx, ZZ_SDR_Temp_Branch_UU, trxName);
	}

	public MZZSDRTempBranch(Properties ctx, String ZZ_SDR_Temp_Branch_UU, String trxName, String... virtualColumns) {
		super(ctx, ZZ_SDR_Temp_Branch_UU, trxName, virtualColumns);
	}

	public MZZSDRTempBranch(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		  if (newRecord) {

		        String orgRegNo = getZZ_Organisation_Reg_No();
		        String branchName = getZZ_BranchName();
		        String docNo = getValue();
		        String sdlNumber = getZZ_SDL_No();

				// 1a) Check if Organisation Registration No is already registered
				int countRegNo = DB.getSQLValueEx(	get_TrxName(),
													"SELECT COUNT(*) FROM c_bpartner WHERE referenceno=?",
													orgRegNo);
				if (countRegNo > 0)
				{
					log.saveError("Error", "Organisation already registered with MQA. Please check Registration No.");
					return false;
				}

				// 1b) Check if SDL Number is already registered
				if (sdlNumber != null && !sdlNumber.isBlank())
				{
					int countSdl = DB.getSQLValueEx(get_TrxName(),
													"SELECT COUNT(*) FROM c_bpartner WHERE zz_sdl_no=?",
													sdlNumber);
					if (countSdl > 0)
					{
						log.saveError("Error", "Business Partner already exists with this SDL Number.");
						return false;
					}
				}

		        // 2) Branch name uniqueness
				int count = DB.getSQLValueEx(	get_TrxName(),
												"SELECT COUNT(*) FROM c_bpartner WHERE name=?",
												branchName);
		        if (count > 0) {
		            log.saveError("Error", "Branch name already exists for this organisation.");
		            return false;
		        }

		        // 3) Check DocumentNo uniqueness
		        if (docNo != null && !docNo.isBlank()) {
		            int cntDoc = DB.getSQLValueEx(get_TrxName(),
		                "SELECT COUNT(*) FROM c_bpartner WHERE value=?",
		                docNo);
		            if (cntDoc > 0) {
		                log.saveError("Error", "Temporary levy number already exists.");
		                return false;
		            }
		        }
		    }

		    return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		 if (!success)
		        return false;

		    if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus)
		        && ZZ_DOCSTATUS_Completed.equals(getZZ_DocStatus()))
		    {
		        Object oldStatus = get_ValueOld(COLUMNNAME_ZZ_DocStatus);

		        if (oldStatus == null || !ZZ_DOCSTATUS_Completed.equals(oldStatus)) {

					if (getC_BPartner_ID() > 0)
					{
						if (!is_ValueChanged("C_BPartner_ID"))
						{
							log.info("Branch BP already created: " + getC_BPartner_ID());
						}
						return true;
					}

		            int bpID = createBranchBusinessPartner();

		            if (bpID <= 0) {
		                log.severe("Failed to create Branch BP");
		                return false;
		            }

		            setC_BPartner_ID(bpID);
		            saveEx();

		            MClient client = MClient.get(getCtx());
		            sendBranchEmail(client, bpID);
		        }
		    }

		    return true;
	}
	
	private int createBranchBusinessPartner() {

	    String orgRegNo = getZZ_Organisation_Reg_No();

	    MBPartner_New bp = new MBPartner_New(getCtx(), 0, get_TrxName());

	    bp.setValue(getValue());         // temporary levy number as search key
	    bp.setName(getZZ_BranchName());       // branch name
	    bp.setName2(getZZ_TradingAs());       // branch trading name
	    bp.setReferenceNo(orgRegNo);          // parent organisation reference
	    bp.setZZ_SDL_No(getZZ_SDL_No());      // SDL number
	    bp.setIsCustomer(true);
	    bp.setIsVendor(true);

	    int bpGroupID = DB.getSQLValueEx(get_TrxName(),
	        "SELECT c_bp_group_id FROM c_bp_group WHERE value='UNKNOWN'");
	    bp.setC_BP_Group_ID(bpGroupID);

	    // SDR flag
	    bp.setZZ_IsSDRUser(true);

	    if (!bp.save()) {
	        log.severe("Failed to save Branch BP");
	        return 0;
	    }

	    // Create Contact
	    MUser contact = new MUser(getCtx(), 0, get_TrxName());
	    contact.setC_BPartner_ID(bp.getC_BPartner_ID());
	    contact.setName(getContactName());
	    contact.setEMail(getContactEmail());
	    contact.setPhone(getCellphoneNo());
	    contact.setPhone2(getLandlineNo());

	    contact.save();

	    return bp.getC_BPartner_ID();
	}
	

	private void sendBranchEmail(MClient client, int bpID) {
	    String sql = "SELECT email FROM ad_user WHERE c_bpartner_id=? AND isactive='Y' ORDER BY created DESC";
	    String to = DB.getSQLValueStringEx(get_TrxName(), sql, bpID);
	    if (to == null || to.isBlank()) return;

	    MMailText mText = new Query(getCtx(), MMailText.Table_Name, "r_mailtext_uu=?", get_TrxName())
	            .setParameters(TEMP_BRANCH_MAILTEXT_UU)
	            .firstOnly();
	    if (mText == null) return;

	    String message = mText.getMailText(true);

        // Safe values (no nulls)
        String contactName = getContactName() != null ? getContactName() : "";
        String orgName     = OrganisationName();
        String searchKey   = getValue() != null ? getValue() : "";
        
        // Replace tokens
        message = message.replace("@ContactName@", contactName)
                         .replace("@OrganisationName@", orgName)
                         .replace("@SearchKey@", searchKey);

	    String subject = String.format("Temporary Org Branch Registration Complete: %s (%s)", 
	                                   getZZ_BranchName(), searchKey);

	    client.sendEMail(to, subject, message, null, true);
	}
	
	public String OrganisationName()
	{
		String orgName = DB.getSQLValueStringEx(get_TrxName(),
		        "SELECT name FROM c_bpartner WHERE referenceno=? AND isactive='Y'", 
		        getZZ_Organisation_Reg_No());
		return orgName != null ? orgName : "";
	}
	
	public String getContactName()
	{
	    return (String) get_Value("contactname");
	}

	public String getContactEmail()
	{
	    return (String) get_Value("email");
	}

	public String getCellphoneNo()
	{
	    return (String) get_Value("cellphonenumber");
	}

	public String getLandlineNo()
	{
	    return (String) get_Value("zz_landline_no");
	}
}
