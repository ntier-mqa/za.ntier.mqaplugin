package za.co.ntier.wsp_atr.ui;

import org.adempiere.webui.AdempiereWebUI;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

import za.co.ntier.wsp_atr.domain.UploadTypeDef;
import za.co.ntier.wsp_atr.form.WspAtrUploadsADForm;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;
import za.co.ntier.wsp_atr.repo.WspAtrUploadsRepository;
import za.co.ntier.wsp_atr.service.WspAtrUploadsService;

public class WspAtrRowUiBuilder {

    private final WspAtrUploadsRepository repo;
    private final WspAtrUploadsService service;
    private final WspAtrUploadsADForm form;

    public WspAtrRowUiBuilder(WspAtrUploadsADForm form, WspAtrUploadsRepository repo, WspAtrUploadsService service) {
        this.form = form;
        this.repo = repo;
        this.service = service;
    }

    public org.zkoss.zul.Hbox buildPrintLine(int submittedId) {
        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        boolean printedOnce = repo.hasAnyReportForSubmitted(submittedId);

        String btnLabel = printedOnce ? "Re Print..." : "Print Report";

        org.adempiere.webui.component.Button btnPrint = new org.adempiere.webui.component.Button(btnLabel);
        btnPrint.setSclass("btn btn-sm btn-primary wsp-edit-purple");

        org.adempiere.webui.component.Label lblMsg = new org.adempiere.webui.component.Label(
            printedOnce ? "Your report will be emailed to you" : ""
        );
        lblMsg.setStyle("margin-left:6px; color:#555;");

        btnPrint.addEventListener(Events.ON_CLICK, (EventListener<Event>) e -> {
            btnPrint.setLabel("Re Print...");
            lblMsg.setValue("Your report will be emailed to you");

            Clients.showNotification(
                "Your Report is being prepared and will be emailed to you.",
                "info", btnPrint, "top_center", 3500
            );

            service.printLatestReportInBackground(submittedId);
        });

        hb.appendChild(btnPrint);
        hb.appendChild(lblMsg);
        return hb;
    }

    public org.zkoss.zul.Hbox buildUploadLine(int submittedId, UploadTypeDef typeDef) {
        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        Integer uploadId = repo.findExistingUploadId(submittedId, typeDef.code, null);
        String fileName = (uploadId != null)
            ? repo.findFirstAttachmentFileName(X_ZZ_WSP_ATR_Uploads.Table_ID, uploadId.intValue())
            : null;

        String btnLabel = !Util.isEmpty(fileName, true) ? "Upload again" : typeDef.initialLabel;

        org.adempiere.webui.component.Button btn = new org.adempiere.webui.component.Button(btnLabel);
        btn.setSclass("btn btn-sm btn-primary wsp-edit-purple");
        btn.setUpload(AdempiereWebUI.getUploadSetting());
        btn.addEventListener(Events.ON_UPLOAD, form);

        btn.setAttribute("SubmittedId", Integer.valueOf(submittedId));
        btn.setAttribute("UploadType", typeDef.code);

        org.adempiere.webui.component.Label lblFile = new org.adempiere.webui.component.Label(
            !Util.isEmpty(fileName, true) ? fileName : ""
        );
        lblFile.setStyle("margin-left:6px; color:#555;");

        btn.setAttribute("FileLabel", lblFile);

        hb.appendChild(btn);
        hb.appendChild(lblFile);
        return hb;
    }
    
    public org.zkoss.zul.Hbox buildSubmitLine(int submittedId) {
        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        boolean eligible = service.isEligibleToSubmit(submittedId);

        org.adempiere.webui.component.Button btn = new org.adempiere.webui.component.Button("Submit WSP-ATR");
        btn.setSclass("btn btn-sm btn-primary wsp-edit-purple");
        btn.setDisabled(!eligible);

        org.adempiere.webui.component.Label lblMsg =
            new org.adempiere.webui.component.Label(eligible ? "" : "Not in submission window / missing uploads");
        lblMsg.setStyle("margin-left:6px; color:#555;");

        btn.addEventListener(Events.ON_CLICK, (EventListener<Event>) e -> {
            service.submitWspAtr(submittedId);

            btn.setLabel("Submitted");
            btn.setDisabled(true);
            lblMsg.setValue("Submitted successfully");

            Clients.showNotification("WSP-ATR Submitted", "info", btn, "top_center", 2500);
        });

        hb.appendChild(btn);
        hb.appendChild(lblMsg);
        return hb;
    }
}