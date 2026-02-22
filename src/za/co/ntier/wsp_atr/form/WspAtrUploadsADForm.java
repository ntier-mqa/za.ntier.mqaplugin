package za.co.ntier.wsp_atr.form;

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.ListCell;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;

import za.co.ntier.wsp_atr.ui.WspAtrRowUiBuilder;
import za.co.ntier.wsp_atr.domain.UploadTypeDef;
import za.co.ntier.wsp_atr.repo.WspAtrUploadsRepository;
import za.co.ntier.wsp_atr.service.WspAtrUploadsService;

@org.idempiere.ui.zk.annotation.Form(name = "za.co.ntier.wsp_atr.form.WspAtrUploadsADForm")
public class WspAtrUploadsADForm extends ADForm implements EventListener<Event> {

    private static final long serialVersionUID = 1L;

    private static final String PROCESS_PRINT_REPORT_UU = "0875c375-6e37-49fb-a5c0-798529189260";

    private final Properties ctx = Env.getCtx();
    private final WspAtrUploadsRepository repo = new WspAtrUploadsRepository(ctx);
    private final WspAtrUploadsService service = new WspAtrUploadsService(ctx, repo, PROCESS_PRINT_REPORT_UU);
    private final WspAtrRowUiBuilder ui = new WspAtrRowUiBuilder(this, repo, service);

    private Borderlayout layout = new Borderlayout();
    private Center center = new Center();
    private Listbox list = new Listbox();

    @Override
    protected void initForm() {
        ZKUpdateUtil.setWidth(layout, "100%");
        ZKUpdateUtil.setHeight(layout, "100%");
        appendChild(layout);

        layout.appendChild(center);

        buildList();
        refreshList();

        // Optional button style
        Clients.evalJavaScript(
            "var s=document.createElement('style');" +
            "s.innerHTML=`" +
            ".wsp-edit-purple{background:#2f2d8f!important;border-color:#2f2d8f!important;color:#fff!important;transition:all .15s ease-in-out;}" +
            ".wsp-edit-purple:hover{background:#3d3ab0!important;border-color:#3d3ab0!important;color:#fff!important;}" +
            ".wsp-edit-purple:active{background:#ffffff!important;border-color:#2f2d8f!important;color:#2f2d8f!important;box-shadow:inset 0 2px 6px rgba(0,0,0,.25)!important;}" +
            ".wsp-edit-purple:focus{outline:none!important;box-shadow:0 0 0 2px rgba(47,45,143,.4)!important;}" +
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

        int adUserId = Env.getAD_User_ID(ctx);
        List<List<Object>> rows = repo.rawSubmittedRowsForUser(adUserId);
        if (rows == null) return;

        for (List<Object> r : rows) {
            int submittedId = ((Number) r.get(0)).intValue();
            Timestamp submittedDate = (Timestamp) r.get(1);
            String orgName = (String) r.get(2);
            String statusCode = (String) r.get(3);

            addRow(submittedId,
                !Util.isEmpty(orgName, true) ? orgName : "",
                submittedDate,
                statusLabel(statusCode));
        }
    }

    private String statusLabel(String code) {
        if (Util.isEmpty(code, true)) return "Draft";
        switch (code) {
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Draft: return "Draft";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Validating: return "Validating";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_ValidationError: return "Validation Error";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Importing: return "Importing";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_Imported: return "Imported";
            case za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted.ZZ_WSP_ATR_STATUS_ErrorImporting: return "Error Importing";
            default: return code;
        }
    }

    private void addRow(int submittedId, String orgName, Timestamp submittedDate, String status) {
        ListItem item = new ListItem();
        item.setValue(Integer.valueOf(submittedId));

        item.appendChild(new ListCell(orgName));
        item.appendChild(new ListCell(submittedDate != null ? submittedDate.toString() : ""));
        item.appendChild(new ListCell(status));

        ListCell actionsCell = new ListCell();
        org.zkoss.zul.Vlayout v = new org.zkoss.zul.Vlayout();
        v.setSpacing("6px");

        v.appendChild(ui.buildPrintLine(submittedId));

        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.WSP_ATR_REPORT));
        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.SIGNED_MINUTES));
        v.appendChild(ui.buildUploadLine(submittedId, UploadTypeDef.ATTENDANCE_REGISTER));

        actionsCell.appendChild(v);
        item.appendChild(actionsCell);

        list.appendChild(item);
    }
}