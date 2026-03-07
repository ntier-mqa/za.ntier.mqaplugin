package za.ntier.models;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MUser;
import org.compiere.util.DB;

import za.co.ntier.api.model.MBPartner_New;

public class MZZSDR_Temp_Org extends X_ZZ_SDR_Temp_Org {

    // Replace with the actual UU of your mail template
    private static final String TEMP_ORG_MAILTEXT_UU = "4ace9d73-8349-44a6-8d2d-5a0838c79362";
    public static final String COLUMNNAME_ZZ_DocStatus = "ZZ_DocStatus";
    public static final String DOCSTATUS_Completed = "CO";



    public MZZSDR_Temp_Org(Properties ctx, int ZZ_SDR_Temp_Org_ID, String trxName) {
        super(ctx, ZZ_SDR_Temp_Org_ID, trxName);
    }

    public MZZSDR_Temp_Org(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    @Override
    protected boolean beforeSave(boolean newRecord) {
        if (newRecord) {

            // Check 1: Organisation Registration No
            String orgRegNo = getZZ_Organisation_Reg_No();
            if (orgRegNo != null && !orgRegNo.trim().isEmpty()) {
                String sqlOrg = "SELECT COUNT(*) FROM adempiere.c_bpartner WHERE referenceno = ?";
                int cntOrg = DB.getSQLValueEx(get_TrxName(), sqlOrg, orgRegNo);
                if (cntOrg > 0) {
                    log.saveError("Error", "Organisation Registration No already exists for another Business Partner.");
                    return false;
                }
            }

            // Check 2: SDL No
            String sdlNo = getZZ_SDL_No();
            if (sdlNo != null && !sdlNo.trim().isEmpty()) {
                String sqlSDL = "SELECT COUNT(*) FROM adempiere.c_bpartner WHERE value = ?";
                int cntSDL = DB.getSQLValueEx(get_TrxName(), sqlSDL, sdlNo);
                if (cntSDL > 0) {
                    log.saveError("Error", "SDL Number already exists for another Business Partner.");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected boolean afterSave(boolean newRecord, boolean success)
    {
        if (!success)
            return false;

        if (is_ValueChanged(COLUMNNAME_ZZ_DocStatus)
                && DOCSTATUS_Completed.equals(getZZ_DocStatus()))
        {
            Object oldStatus = get_ValueOld(COLUMNNAME_ZZ_DocStatus);

            if (oldStatus == null || !DOCSTATUS_Completed.equals(oldStatus))
            {
                // Safety: prevent duplicate BP creation
                if (getC_BPartner_ID() > 0)
                {
                    log.info("Business Partner already created: " + getC_BPartner_ID());
                    return true;
                }

                // 1) Create BP + Contact
                int bpID = createBusinessPartner();
                if (bpID <= 0)
                {
                    log.severe("Failed to create BP for ZZ_SDR_Temp_Org_ID="
                            + getZZ_SDR_Temp_Org_ID());
                    return false;
                }

                // 2) Persist BP ID
                setC_BPartner_ID(bpID);
                saveEx();

                // 3) Send confirmation email
                MClient client = MClient.get(getCtx());
                sendTempOrgEmail(client, bpID);
            }
        }

        return true;
    }



    /**
     * Creates Business Partner and Contact for this Temp Org
     */
    private int createBusinessPartner() {

        // Use pre-generated SDL number
        String sdlNo = getZZ_SDL_No();
        if (sdlNo == null || sdlNo.trim().isEmpty()) {
            log.severe("SDL Number not set on Temp Org record.");
            return 0;
        }

        // Create BP
        MBPartner_New bp = new MBPartner_New(getCtx(), 0, get_TrxName());
        bp.setValue(getValue()); // Search Key as BPartner search key
        bp.setName(getZZ_Organisation_Name());
        bp.setName2(getZZ_TradingAs());
        bp.setReferenceNo(getZZ_Organisation_Reg_No());
        bp.setIsCustomer(true);
        bp.setIsVendor(true);
        bp.setZZ_SDL_No(sdlNo);
     // Default BP Group = UNKNOWN
        int bpGroupID = DB.getSQLValueEx(
                get_TrxName(),
                "SELECT c_bp_group_id FROM adempiere.c_bp_group WHERE value = 'UNKNOWN'"
        );
        bp.setC_BP_Group_ID(bpGroupID);
     // IMPORTANT: mark as SDR user
        bp.set_ValueOfColumn("zz_issdruser", "Y");

        if (!bp.save()) {
            log.severe("Failed to save Business Partner.");
            return 0;
        }

        // Create Contact
        MUser contact = new MUser(getCtx(), 0, get_TrxName());
        contact.setC_BPartner_ID(bp.getC_BPartner_ID());
        contact.setName(getContactName());
        contact.setEMail(getContactEmail());
        contact.setPhone(getCellphoneNo());
        contact.setPhone2(getLandlineNo());
        if (!contact.save()) {
            log.severe("Failed to save BP Contact for BP=" + bp.getC_BPartner_ID());
        }

        return bp.getC_BPartner_ID();
    }

    /**
     * Sends email to the contact after temporary organisation registration
     * 
     */
    private void sendTempOrgEmail(MClient client, int bpID) {

        // Get contact email
        String sql = "SELECT email FROM adempiere.ad_user WHERE c_bpartner_id = ? LIMIT 1";
        String to = DB.getSQLValueStringEx(get_TrxName(), sql, bpID);

        if (to == null || to.trim().isEmpty()) {
            log.warning("No contact email found for BP=" + bpID);
            return;
        }


     // Look up MailText_ID from its UU
        int mailTextID = DB.getSQLValueEx(
            get_TrxName(),
            "SELECT r_mailtext_id FROM adempiere.r_mailtext WHERE r_mailtext_uu = ?",
            TEMP_ORG_MAILTEXT_UU
        );

        // Load mail text correctly
        MMailText mText = new MMailText(getCtx(), mailTextID, get_TrxName());

        if (mText.get_ID() <= 0) {
            log.warning("Temp Org mail text not found. UU=" + TEMP_ORG_MAILTEXT_UU);
            return;
        }

       // try {
       //     mText.setPO(this, true); // Set context
       // } catch (Throwable t) {
       //     mText.setPO(this);
       // }
        
        String message = mText.getMailText(true);

        // Safe values (no nulls)
        String contactName = getContactName() != null ? getContactName() : "";
        String orgName     = getZZ_Organisation_Name() != null ? getZZ_Organisation_Name() : "";
        //String sdlNo       = getZZ_SDL_No() != null ? getZZ_SDL_No() : "";
        String searchKey = getValue() != null ? getValue() : "";

        // Replace tokens
        message = message.replace("@ContactName@", contactName)
                      .replace("@OrganisationName@", orgName)
                      .replace("@SearchKey@", searchKey);
                     // .replace("@SDLNo@", sdlNo);

        // -------------------------------
        // NEW: Dynamic email subject
        // -------------------------------
        String subject = "Temporary Registration Complete: @OrganisationName@ - Reference: @SearchKey@";
        subject = subject.replace("@OrganisationName@", orgName)
                         .replace("@SearchKey@", searchKey);
        //String subject = "Temporary Registration Complete: @OrganisationName@ - SDL No: @SDLNo@";
        //subject = subject.replace("@OrganisationName@", getZZ_Organisation_Name())
        //                 .replace("@SDLNo@", getZZ_SDL_No());

        // Send email
        boolean sent = client.sendEMail(to, subject, message, null, true);
        if (!sent)
            log.warning("Failed to send Temporary Org Registration email to " + to);
        else
            log.info("Temporary Org Registration email sent to " + to);
    }

    /***********************
     * Helper getters for contact info
     ***********************/
    public String getContactName() {
        return (String) get_Value("contactname");
    }

    public String getContactEmail() {
        return (String) get_Value("email");
    }

    public String getCellphoneNo() {
        return (String) get_Value("cellphonenumber");
    }

    public String getLandlineNo() {
        return (String) get_Value("zz_landline_no");
    }
}
