package za.co.ntier.wsp_atr.ui;

import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Hbox;

import za.co.ntier.wsp_atr.domain.UploadTypeDef;
import za.co.ntier.wsp_atr.form.WspAtrUploadsADForm;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
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

    public Hbox buildPrintLine(int submittedId) {
        Hbox hb = new Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        boolean printedOnce = repo.hasAnyReportForSubmitted(submittedId);
        String btnLabel = printedOnce ? "Re Print..." : "Print Report";

        Button btnPrint = new Button(btnLabel);
        btnPrint.setSclass("btn btn-sm btn-primary wsp-edit-purple");

        Label lblMsg = new Label(
            printedOnce ? "Your report will be emailed to you" : ""
        );
        lblMsg.setStyle("margin-left:6px; color:#555;");

        btnPrint.addEventListener(Events.ON_CLICK, (EventListener<Event>) e -> {
            // Consolidation is no longer something the user picks. A parent with at least one
            // child flagged ZZ_Parent_Uploads = 'Y' must always report parent + children, so the
            // answer is derived rather than prompted for. isParentOrganisationTypeForSubmitted()
            // already means "PARENT business partner AND has at least one flagged child", so an
            // ordinary org - or a parent whose children are all flagged 'N' - correctly falls
            // through to its own figures only.
            //
            // The children-only variant (onlySubLevyOrgs) is deliberately fixed at false: it has
            // no remaining UI, and parent + children is the required output. The process
            // parameter and ZZ_WSP_ATR_Report column are left in place so it can be re-exposed.
            boolean consolidated = repo.isParentOrganisationTypeForSubmitted(submittedId);

            btnPrint.setLabel("Re Print...");
            lblMsg.setValue("Your report will be emailed to you");

            Clients.showNotification(
                    "Your Report is being prepared and will be emailed to you.",
                    "info", btnPrint, "top_center", 3500
            );

            service.generateReport(submittedId, consolidated, false);
        });

        hb.appendChild(btnPrint);
        hb.appendChild(lblMsg);
        return hb;
    }

    public Hbox buildUploadLine(int submittedId, UploadTypeDef typeDef) {
        Hbox hb = new Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        Integer uploadId = repo.findExistingUploadId(submittedId, typeDef.code, null);
        String fileName = (uploadId != null)
            ? repo.findFirstAttachmentFileName(X_ZZ_WSP_ATR_Uploads.Table_ID, uploadId.intValue())
            : null;

        String btnLabel = typeDef.initialLabel;

        Button btn = new Button(btnLabel);
        btn.setSclass("btn btn-sm btn-primary wsp-edit-purple");
        btn.setUpload(AdempiereWebUI.getUploadSetting());
        btn.addEventListener(Events.ON_UPLOAD, form);

        btn.setAttribute("SubmittedId", Integer.valueOf(submittedId));
        btn.setAttribute("UploadType", typeDef.code);

        Label lblFile = new Label(!Util.isEmpty(fileName, true) ? fileName : "");
        lblFile.setStyle("margin-left:6px; color:#555;");

        btn.setAttribute("FileLabel", lblFile);

        hb.appendChild(btn);
        hb.appendChild(lblFile);
        return hb;
    }

    public Hbox buildSubmitLine(int submittedId, int zzSdfOrganisationId, String status) {
        Hbox hb = new Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        boolean eligible = service.isEligibleToSubmit(submittedId);
        boolean parentOrg = form.isParentOrganisation(zzSdfOrganisationId, null);
        String docStatus = repo.getDocStatus(submittedId);

        if (!parentOrg && !X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported.equals(docStatus)) {
            eligible = false;
        }
        if (X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Query.equals(docStatus)) {
        	eligible = false;
        }
        if (parentOrg
                && !X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft.equals(docStatus)
                && !X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported.equals(docStatus)) {
            // Parent already Submitted/Uploaded/Recommended/Approved etc. - don't allow re-submission.
            eligible = false;
        }

        Button btn = new Button("Submit WSP-ATR");
        btn.setSclass("btn btn-sm btn-primary wsp-edit-purple");
        btn.setDisabled(!eligible);

        Label lblMsg =
            new Label((service.getSubmitButtonMsg() != null)
                ? service.getSubmitButtonMsg()
                : (eligible ? "" : "Not in submission window / missing uploads"));
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
