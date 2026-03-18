package za.co.ntier.wsp_atr.form;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.ListCell;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;

import za.co.ntier.wsp_atr.ui.WspAtrRowUiBuilder;
import za.ntier.models.MZZWSPATRSubmitted;
import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.co.ntier.wsp_atr.domain.UploadTypeDef;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.repo.WspAtrUploadsRepository;
import za.co.ntier.wsp_atr.service.WspAtrUploadsService;

@org.idempiere.ui.zk.annotation.Form(name = "za.co.ntier.wsp_atr.form.WspAtrUploadsADForm")
public class WspAtrUploadsADForm extends ADForm implements EventListener<Event> {

    private static final long serialVersionUID = 1L;

    private static final String PROCESS_GENERATE_REPORT_UU = "2760c2cf-56ad-405e-92fe-86b873b81025";

    private final Properties ctx = Env.getCtx();
    private final WspAtrUploadsRepository repo = new WspAtrUploadsRepository(ctx);
    private final WspAtrUploadsService service = new WspAtrUploadsService(ctx, repo, PROCESS_GENERATE_REPORT_UU);
    private final WspAtrRowUiBuilder ui = new WspAtrRowUiBuilder(this, repo, service);

    private Borderlayout layout = new Borderlayout();
    private Center center = new Center();
    private Listbox list = new Listbox();
    
    private static final DateTimeFormatter TS_FORMAT =
	        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void initForm() {
        ZKUpdateUtil.setWidth(layout, "100%");
        ZKUpdateUtil.setHeight(layout, "100%");
        appendChild(layout);

        layout.appendChild(center);

        buildList();
        refreshList();
        
        Clients.evalJavaScript(
        	    "var s=document.createElement('style');" +
        	    "s.innerHTML=`" +
        	    ".wsp-edit-purple{background:#2f2d8f!important;border-color:#2f2d8f!important;color:#fff!important;}" +
        	    ".wsp-edit-purple:hover{background:#262372!important;border-color:#262372!important;color:#fff!important;}" +
        	    ".wsp-edit-purple[disabled], " +
        	    ".wsp-edit-purple.z-button-disabled{background:#d3d3d3!important;border-color:#c0c0c0!important;color:#888!important;cursor:not-allowed!important;opacity:0.7!important;}" +
        	    "`;" +
        	    "document.head.appendChild(s);"
        	);
    }

    private void buildList() {
        center.appendChild(list);
        list.setVflex("1");
        list.setHflex("1");
        list.setMultiple(false);

        ListHead head = new ListHead();
        list.appendChild(head);

        head.appendChild(new ListHeader("Organisation"));
        head.appendChild(new ListHeader("Submitted Date"));
        head.appendChild(new ListHeader("Status"));
        head.appendChild(new ListHeader("Actions"));
    }

    @Override
    public void onEvent(Event event) throws Exception {
        if (event instanceof UploadEvent) {
            handleUploadEvent((UploadEvent) event);
        }
    }

    private void handleUploadEvent(UploadEvent ue) throws Exception {
        Media media = ue.getMedia();
        if (media == null) return;

        Object submittedIdObj = ue.getTarget().getAttribute("SubmittedId");
        Object uploadTypeObj  = ue.getTarget().getAttribute("UploadType");

        if (!(submittedIdObj instanceof Integer) || !(uploadTypeObj instanceof String)) {
            throw new AdempiereException("Upload button is missing SubmittedId/UploadType attributes.");
        }

        int submittedId = ((Integer) submittedIdObj).intValue();
        String uploadType = (String) uploadTypeObj;

        service.uploadReplace(submittedId, uploadType, media);

        // Immediate UI feedback (row will rebuild after refresh)
        if (ue.getTarget() instanceof org.adempiere.webui.component.Button) {
            org.adempiere.webui.component.Button b = (org.adempiere.webui.component.Button) ue.getTarget();
            b.setLabel("Upload again");

            Object fileLblObj = b.getAttribute("FileLabel");
            if (fileLblObj instanceof org.adempiere.webui.component.Label) {
                ((org.adempiere.webui.component.Label) fileLblObj).setValue(media.getName());
            }
            Clients.showNotification("File Uploaded", "info", b, "top_center", 2500);
        } else {
            Clients.showNotification("File Uploaded", "info", null, "top_center", 2500);
        }

        refreshList();
    }

    private void refreshList() {
        list.getItems().clear();

        List<SdfOrgRow> orgs = getSdfOrganisationsForUser();
        if (orgs == null || orgs.isEmpty()) {
            return;
        }

        for (SdfOrgRow org : orgs) {
            MZZWSPATRSubmitted submitted = getOrCreateSubmittedForOrg(org);
            if (submitted == null || submitted.get_ID() <= 0) {
                continue;
            }

            addRow(
                    submitted.getZZ_WSP_ATR_Submitted_ID(),
                    !Util.isEmpty(org.displayName, true) ? org.displayName : "",
                    submitted.getSubmittedDate(),
                    statusLabel(submitted.getZZ_DocStatus())
            );
        }
    }

    private String statusLabel(String code) {
        if (Util.isEmpty(code, true)) return "Draft";
        switch (code) {
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft: return "Draft";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating: return "Validating";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ValidationError: return "Validation Error";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing: return "Importing";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported: return "Imported";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting: return "Error Importing";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Submitted: return "Submitted";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Uploaded: return "Uploaded";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForEvaluation: return "Recommended For Evaluation";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_RecommendedForApproval: return "Recommended For Approval";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Approved: return "Approved";
            
            default: return code;
        }
    }

    private void addRow(int submittedId, String orgName, Timestamp submittedDate, String status) {
        ListItem item = new ListItem();
        item.setValue(Integer.valueOf(submittedId));
        String formattedDate = "";
	    if (submittedDate != null) {
	        formattedDate = submittedDate.toLocalDateTime().format(TS_FORMAT);
	    }

        item.appendChild(new ListCell(orgName));
        item.appendChild(new ListCell(submittedDate != null ? formattedDate : ""));
        item.appendChild(new ListCell(status));

        ListCell actionsCell = new ListCell();
        org.zkoss.zul.Vlayout v = new org.zkoss.zul.Vlayout();
        v.setSpacing("6px");

        v.appendChild(ui.buildPrintLine(submittedId));

        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.WSP_ATR_REPORT));
        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.SIGNED_MINUTES));
        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.ATTENDANCE_REGISTER));
        v.appendChild(ui.buildSubmitLine(submittedId,status));

        actionsCell.appendChild(v);
        item.appendChild(actionsCell);

        list.appendChild(item);
    }
    
    private static class SdfOrgRow {
        private final int zzSdfOrganisationId;
        private final String displayName;

        private SdfOrgRow(int zzSdfOrganisationId, String displayName) {
            this.zzSdfOrganisationId = zzSdfOrganisationId;
            this.displayName = displayName;
        }
    }
    
    private List<SdfOrgRow> getSdfOrganisationsForUser() {
        int adUserId = Env.getAD_User_ID(ctx);

        String sql =
                "SELECT zzsdforganisation_v_id, (zz_sdl_no || ' - ' || orgname) AS display_name " +
                "FROM adempiere.zzsdforganisation_v " +
                "WHERE ad_user_id = ? " +
                "AND isactive = 'Y' " +
                "AND zzsdfroletype = ? " +
                "AND COALESCE(zz_docstatus, '') <> 'DR' " +
                "AND COALESCE(zz_docstatus, '') <> 'UnSdfOrg' " +
                "ORDER BY orgname";

        List<List<Object>> rows = DB.getSQLArrayObjectsEx(null, sql,
                adUserId,
                X_ZZSdfOrganisation.ZZSDFROLETYPE_Primary);

        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }

        return rows.stream()
                .map(r -> new SdfOrgRow(
                        ((Number) r.get(0)).intValue(),
                        (String) r.get(1)))
                .collect(Collectors.toList());
    }
    
    private MZZWSPATRSubmitted getOrCreateSubmittedForOrg(SdfOrgRow org) {
        String trxName = null;

        int submittedId = DB.getSQLValueEx(trxName,
                "SELECT zz_wsp_atr_submitted_id " +
                "FROM zz_wsp_atr_submitted " +
                "WHERE zzsdforganisation_id = ? " +
                "ORDER BY created DESC " +
                "LIMIT 1",
                org.zzSdfOrganisationId);

        MZZWSPATRSubmitted submitted;
        if (submittedId > 0) {
            submitted = new MZZWSPATRSubmitted(ctx, submittedId, trxName);
        } else {
            submitted = new MZZWSPATRSubmitted(ctx, 0, trxName);
            submitted.setName("WSP/ATR " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
            submitted.setZZ_Import_Submitted_Data("N");
            submitted.setZZSdfOrganisation_ID(org.zzSdfOrganisationId);
            submitted.setZZ_DocAction(null);
            submitted.setZZ_DocStatus(X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft);
            submitted.setZZ_FinYear_ID(WspAtrSubmittedADForm.getFiscalYear(Env.getAD_Client_ID(Env.getCtx())));
            submitted.saveEx();
            WspAtrSubmittedADForm.rebuildSubLevyOrgLinks(submitted.getZZ_WSP_ATR_Submitted_ID(), trxName);
        }

        return submitted;
    }
}