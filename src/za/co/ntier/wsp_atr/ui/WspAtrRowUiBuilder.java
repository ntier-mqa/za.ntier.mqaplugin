package za.co.ntier.wsp_atr.ui;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Label;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

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
            // isParentOrganisationTypeForSubmitted() means "PARENT business partner AND has at
            // least one flagged child", i.e. there is actually something to consolidate.
            //
            // Any such parent is asked, whether or not it has uploaded its own WSP-ATR. The
            // consolidated option is always offered and fails loudly if the children are not in
            // yet (see startReport) - rather than being withheld - so that a parent blocked from
            // consolidating can still fall back to its own Independent report. Deciding for them
            // here would leave a parent whose child has not uploaded unable to print anything.
            if (repo.isParentOrganisationTypeForSubmitted(submittedId)) {
                openPrintPrompt(submittedId, btnPrint, lblMsg);
                return;
            }

            // Not a parent: nothing to consolidate, so there is no question to ask.
            startReport(submittedId, false, btnPrint, lblMsg);
        });

        hb.appendChild(btnPrint);
        hb.appendChild(lblMsg);
        return hb;
    }

    /**
     * Shared tail of both report paths - the prompted one and the derived one.
     *
     * onlySubLevyOrgs stays false: a consolidation always includes the parent. When the parent
     * has not uploaded, its own submission simply contributes nothing, so that case needs no
     * special handling, and the children-only variant has no UI.
     */
    private void startReport(int submittedId, boolean consolidated, Button btnPrint, Label lblMsg) {
        // A consolidated report is assembled from the separate-WSP-ATR children's own returns.
        // A child that has not uploaded yet contributes nothing and would simply be absent, with
        // nothing on the report to say so - so the report is refused rather than produced
        // quietly incomplete. Checked here rather than in the dialog so it also covers the
        // unprompted consolidated path (a parent that has not uploaded its own WSP-ATR).
        if (consolidated) {
            java.util.List<String> pending = repo.findChildOrgsNotSubmitted(submittedId);
            if (!pending.isEmpty()) {
                throw new AdempiereException(
                        "Cannot generate a consolidated report yet. The following child "
                        + "organisation(s) have not uploaded their WSP-ATR: "
                        + String.join(", ", pending)
                        + ". They must upload before a consolidated report can be produced. "
                        + "You can still generate an Independent report for this organisation.");
            }
        }

        btnPrint.setLabel("Re Print...");
        lblMsg.setValue("Your report will be emailed to you");

        Clients.showNotification(
                "Your Report is being prepared and will be emailed to you.",
                "info", btnPrint, "top_center", 3500
        );

        service.generateReport(submittedId, consolidated, false);
    }

    /**
     * Asks a parent that has uploaded its own WSP-ATR whether to report on itself alone
     * (independent) or to include its child organisations as well (consolidated). Only reachable
     * in that case - see buildPrintLine.
     */
    private void openPrintPrompt(int submittedId, Button btnPrint, Label lblMsg) {
        Window win = new Window("Generate Report", "normal", true);
        win.setClosable(true);
        win.setWidth("420px");
        win.setBorder("normal");
        win.setSizable(false);
        win.setPosition("center,center");
        win.setParent(form);

        Vbox root = new Vbox();
        root.setSpacing("10px");
        root.setStyle("padding:15px;");

        root.appendChild(new Label("Generate Report:"));

        // Two named options rather than a tick-box, so Independent is a visible choice. That
        // matters because it is the fallback when Consolidated is refused for missing children.
        // (Radio/Radiogroup are plain ZK components; org.zkoss.zul is already imported by this
        // bundle for Hbox/Vbox/Window, so no MANIFEST change is needed.)
        Radiogroup rgScope = new Radiogroup();
        rgScope.setOrient("vertical");
        root.appendChild(rgScope);

        Radio rbIndependent = new Radio("Independent (this organisation only)");
        Radio rbConsolidated = new Radio("Consolidated (this organisation and its child organisations)");
        rgScope.appendChild(rbIndependent);
        rgScope.appendChild(rbConsolidated);
        // A parent with separate children is normally consolidating, so that is pre-selected.
        rbConsolidated.setSelected(true);

        Separator sep = new Separator();
        sep.setBar(true);
        root.appendChild(sep);

        Hbox buttons = new Hbox();
        buttons.setSpacing("10px");

        Button okBtn = new Button("OK");
        okBtn.setSclass("btn btn-sm btn-primary wsp-edit-purple");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setSclass("btn btn-sm");

        okBtn.addEventListener(Events.ON_CLICK, e -> {
            boolean consolidated = rbConsolidated.isSelected();
            win.detach();
            // startReport refuses a consolidation whose children are not all in. The dialog is
            // already closed, so the SDF sees the error and can print again choosing Independent.
            startReport(submittedId, consolidated, btnPrint, lblMsg);
        });

        cancelBtn.addEventListener(Events.ON_CLICK, e -> win.detach());

        buttons.appendChild(okBtn);
        buttons.appendChild(cancelBtn);

        root.appendChild(buttons);
        win.appendChild(root);
        win.doModal();
    }

    /**
     * @param parentManaged this organisation's parent uploads and submits for it, so the upload
     *                      button is shown disabled rather than hidden - the file already
     *                      attached (if any) stays visible.
     */
    public Hbox buildUploadLine(int submittedId, UploadTypeDef typeDef, boolean parentManaged) {
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
        if (!parentManaged) {
            btn.setUpload(AdempiereWebUI.getUploadSetting());
            btn.addEventListener(Events.ON_UPLOAD, form);
        } else {
            // Leave the upload behaviour off entirely rather than only disabling the button, so
            // the rule does not rest on the client-side disabled state alone.
            btn.setDisabled(true);
        }

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
