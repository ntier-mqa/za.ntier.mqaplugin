package za.co.ntier.wsp_atr.form;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.apps.BackgroundJob;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.ListCell;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.panel.ADForm;
import org.compiere.model.MAttachment;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;

import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Report;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Uploads;
import za.ntier.models.MZZWSPATRSubmitted;

@org.idempiere.ui.zk.annotation.Form(name = "za.co.ntier.wsp_atr.form.WspAtrUploadsADFormOLD")
public class WspAtrUploadsADFormOLD extends ADForm implements EventListener<Event> {

    private static final long serialVersionUID = 1L;

    // Print report process
    private static final String PROCESS_PRINT_REPORT_UU = "0875c375-6e37-49fb-a5c0-798529189260";

    /**
     * Upload type codes:
     * IMPORTANT: change these to match your reference list values for column ZZ_WSP_ATR_Upload_Type.
     */
    private static final String UPLOADTYPE_WSP_ATR_REPORT = X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadWSP_ATRReport;
    private static final String UPLOADTYPE_SIGNED_MINUTES = X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadSignedMinutes;
    private static final String UPLOADTYPE_ATTENDANCE_REGISTER = X_ZZ_WSP_ATR_Uploads.ZZ_WSP_ATR_UPLOAD_TYPE_UploadAttendanceRegister;

    private Borderlayout layout = new Borderlayout();
    private Center center = new Center();

    private Listbox list = new Listbox();

    @Override
    protected void initForm() {
        org.adempiere.webui.util.ZKUpdateUtil.setWidth(layout, "100%");
        org.adempiere.webui.util.ZKUpdateUtil.setHeight(layout, "100%");
        this.appendChild(layout);

        layout.appendChild(center);

        buildList();
        refreshList();

        // One event handler for all row buttons (print + uploads)
        this.addEventListener("onPrintReport", this);
        this.addEventListener("onUploadFile", this);

        // Optional: reuse your purple button style if you want
        org.zkoss.zk.ui.util.Clients.evalJavaScript(
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

        // Upload events come as UploadEvent, target is the specific button
       
        if (event instanceof UploadEvent) {
            UploadEvent ue = (UploadEvent) event;
            Media media = ue.getMedia();
            if (media == null) return;

            Object submittedIdObj = ue.getTarget().getAttribute("SubmittedId");
            Object uploadTypeObj  = ue.getTarget().getAttribute("UploadType");

            if (!(submittedIdObj instanceof Integer) || !(uploadTypeObj instanceof String)) {
                throw new AdempiereException("Upload button is missing SubmittedId/UploadType attributes.");
            }

            int submittedId = ((Integer) submittedIdObj).intValue();
            String uploadType = (String) uploadTypeObj;

            // Do upload (now reuses existing record per type - see method below)
            doUpload(submittedId, uploadType, media);
            
            if (ue.getTarget() instanceof org.adempiere.webui.component.Button) {
                org.adempiere.webui.component.Button b = (org.adempiere.webui.component.Button) ue.getTarget();

                // Change label (will still be correct after refresh because we render from DB)
                b.setLabel("Upload again");

                // Update the filename label next to the button immediately
                Object fileLblObj = b.getAttribute("FileLabel");
                if (fileLblObj instanceof org.adempiere.webui.component.Label) {
                    ((org.adempiere.webui.component.Label) fileLblObj).setValue(media.getName());
                }

                Clients.showNotification("File Uploaded", "info", b, "top_center", 2500);
            }

            // UI feedback + label change
            if (ue.getTarget() instanceof org.adempiere.webui.component.Button) {
                org.adempiere.webui.component.Button b = (org.adempiere.webui.component.Button) ue.getTarget();
                b.setLabel("Upload again");
                Clients.showNotification("File Uploaded", "info", b, "top_center", 2500);
            } else {
                Clients.showNotification("File Uploaded", "info", null, "top_center", 2500);
            }

            refreshList();
            return;
        }

        if ("onPrintReport".equals(event.getName())) {
            int submittedId = (Integer) event.getData();
            doPrintLatestReportForSubmitted(submittedId);
            return;
            
            
        }

        if ("onUploadFile".equals(event.getName())) {
            // Not used if you rely on UploadEvent; left here if you want to trigger something else.
            return;
        }
    }

    private void refreshList() {
        list.getItems().clear();

        int adUserId = Env.getAD_User_ID(Env.getCtx());

        String sql =
            "SELECT s.ZZ_WSP_ATR_Submitted_ID, s.SubmittedDate, v.orgname, s.ZZ_DocStatus " +
            "FROM ZZ_WSP_ATR_Submitted s " +
            "LEFT JOIN adempiere.zzsdforganisation_v v ON v.zzsdforganisation_v_id = s.ZZSDFOrganisation_ID " +
            "WHERE v.ad_user_id = ? " +
            "ORDER BY s.ZZ_WSP_ATR_Submitted_ID DESC";

        List<List<Object>> rows = DB.getSQLArrayObjectsEx(null, sql, adUserId);
        if (rows == null) return;

        for (List<Object> r : rows) {
            int submittedId = ((Number) r.get(0)).intValue();
            Timestamp submittedDate = (Timestamp) r.get(1);
            String orgName = (String) r.get(2);
            String statusCode = (String) r.get(3);

            String status = statusLabel(statusCode);

            addRow(submittedId,
                !Util.isEmpty(orgName, true) ? orgName : "",
                submittedDate,
                status);
        }
    }

    private String statusLabel(String code) {
        if (Util.isEmpty(code, true)) return "Draft";
        switch (code) {
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Draft: return "Draft";
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating: return "Validating";
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ValidationError: return "Validation Error";
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Importing: return "Importing";
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Imported: return "Imported";
            case X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_ErrorImporting: return "Error Importing";
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

        // 1) Print report button
             
        
        v.appendChild(buildPrintLine(submittedId));
        
     // 2) Upload WSP-ATR Report (button + filename)
        v.appendChild(buildUploadLine(submittedId, UPLOADTYPE_WSP_ATR_REPORT, "Upload WSP-ATR Report"));

        // 3) Upload Signed Minutes
        v.appendChild(buildUploadLine(submittedId, UPLOADTYPE_SIGNED_MINUTES, "Upload Signed Minutes"));

        // 4) Upload Attendance Register
        v.appendChild(buildUploadLine(submittedId, UPLOADTYPE_ATTENDANCE_REGISTER, "Upload Attendance Register"));

        actionsCell.appendChild(v);
        item.appendChild(actionsCell);

        list.appendChild(item);
    }

    private void prepareUploadButton(org.adempiere.webui.component.Button btn, int submittedId, String uploadType) {
        btn.setSclass("btn btn-sm btn-primary wsp-edit-purple");

        // This enables the file chooser and triggers UploadEvent
        btn.setUpload(AdempiereWebUI.getUploadSetting());
        btn.addEventListener(Events.ON_UPLOAD, this);

        // Pass context into the UploadEvent handler
        btn.setAttribute("SubmittedId", Integer.valueOf(submittedId));
        btn.setAttribute("UploadType", uploadType);
    }

    // ---------------- PRINT REPORT ----------------

    private void doPrintLatestReportForSubmitted(int submittedId) {
        int reportId = findLatestReportIdForSubmitted(submittedId);
        if (reportId <= 0) {
            throw new AdempiereException("No ZZ_WSP_ATR_Report found for Submitted ID " + submittedId);
        }

        // Run print process as background job, passing ZZ_WSP_ATR_Report_ID
        runProcessInBackgroundWithIntParam(PROCESS_PRINT_REPORT_UU, "ZZ_WSP_ATR_Report_ID", reportId, submittedId);
    }

    private int findLatestReportIdForSubmitted(int submittedId) {
        // Prefer link by ZZ_WSP_ATR_Submitted_ID if that column exists on ZZ_WSP_ATR_Report
        boolean hasLinkCol = columnExists(X_ZZ_WSP_ATR_Report.Table_Name, "ZZ_WSP_ATR_Submitted_ID", null);

        if (hasLinkCol) {
            return DB.getSQLValueEx(null,
                "SELECT ZZ_WSP_ATR_Report_ID " +
                "FROM ZZ_WSP_ATR_Report " +
                "WHERE ZZ_WSP_ATR_Submitted_ID=? " +
                "ORDER BY Created DESC, ZZ_WSP_ATR_Report_ID DESC " +
                "FETCH FIRST 1 ROWS ONLY",
                submittedId);
        }

        // Fallback: last report overall (if your table doesn’t link back)
        return DB.getSQLValueEx(null,
            "SELECT ZZ_WSP_ATR_Report_ID " +
            "FROM ZZ_WSP_ATR_Report " +
            "ORDER BY Created DESC, ZZ_WSP_ATR_Report_ID DESC " +
            "FETCH FIRST 1 ROWS ONLY");
    }

    private void runProcessInBackgroundWithIntParam(String adProcessUU, String paramName, int paramValue, int recordIdForInstance) {
        final Properties ctx = Env.getCtx();

        MProcess proc = MProcess.get(ctx, adProcessUU);
        if (proc == null || proc.getAD_Process_ID() <= 0) {
            throw new AdempiereException("Process not found (UU=" + adProcessUU + ")");
        }

        ProcessInfo pi = new ProcessInfo(proc.getName(), proc.getAD_Process_ID());
        pi.setAD_User_ID(Env.getAD_User_ID(ctx));
        pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
        pi.setTable_ID(MTable.getTable_ID(X_ZZ_WSP_ATR_Report.Table_Name));
        pi.setRecord_ID(recordIdForInstance);
        pi.setAD_Process_UU(proc.getAD_Process_UU());
        

        MPInstance instance = new MPInstance(ctx, proc.getAD_Process_ID(), 0, recordIdForInstance, null);
        instance.setIsRunAsJob(true);
        instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
        instance.saveEx();

        /*
        // Save parameter (int)
        MPInstancePara para = new MPInstancePara(instance, 10);
        para.setParameterName(paramName);
        para.setP_Number(paramValue);
        para.saveEx();
        */

        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

       // Callback<Integer> cb = id -> { /* params already saved */ };
        // This callback will be called in Background job
        Callback<Integer> createInstanceParaCallback = id -> {
			if (id > 0) {
				MPInstance instanceLater = new MPInstance(Env.getCtx(),id,null);
				MPInstancePara para = new MPInstancePara(instanceLater, 10);
		        para.setParameterName(paramName);
		        para.setP_Number(paramValue);
		        para.saveEx();
			}
		};

        BackgroundJob.create(pi)
            .withContext(ctx)
            .withNotificationType(instance.getNotificationType())
            .withInitialDelay(250)
            .run(createInstanceParaCallback);
    }

    // ---------------- UPLOADS ----------------

    
    
    private void doUpload(int submittedId, String uploadType, Media media) throws Exception {
        if (media == null) return;

        String filename = media.getName();
        if (Util.isEmpty(filename, true)) {
            throw new AdempiereException("File name is empty.");
        }

        byte[] data = getMediaBytes(media);

        String trxName = Trx.createTrxName("WSPATRUploadDoc");
        Trx trx = Trx.get(trxName, true);

        try {
            // Ensure submitted exists (optional but safer)
            MZZWSPATRSubmitted submitted = new MZZWSPATRSubmitted(Env.getCtx(), submittedId, trxName);
            if (submitted.get_ID() <= 0) {
                throw new AdempiereException("Submitted record not found: " + submittedId);
            }

            // 1) Find existing upload record for this Submitted + Type
            int existingUploadId = DB.getSQLValueEx(trxName,
                "SELECT ZZ_WSP_ATR_Uploads_ID " +
                "FROM ZZ_WSP_ATR_Uploads " +
                "WHERE ZZ_WSP_ATR_Submitted_ID=? " +
                "AND ZZ_WSP_ATR_Upload_Type=? " +
                "ORDER BY Updated DESC NULLS LAST, Created DESC NULLS LAST, ZZ_WSP_ATR_Uploads_ID DESC " +
                "FETCH FIRST 1 ROWS ONLY",
                submittedId, uploadType
            );

            X_ZZ_WSP_ATR_Uploads up;
            if (existingUploadId > 0) {
                // REUSE existing row
                up = new X_ZZ_WSP_ATR_Uploads(Env.getCtx(), existingUploadId, trxName);

                // Remove existing attachment on that upload record
                deleteAllAttachmentsForUpload(up.get_ID(), trxName);

                // Update name (optional)
                up.setName(uploadType + " - " + filename + " - " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                up.saveEx();

            } else {
                // CREATE new row
                up = new X_ZZ_WSP_ATR_Uploads(Env.getCtx(), 0, trxName);
                up.setZZ_WSP_ATR_Submitted_ID(submittedId);
                up.setZZ_WSP_ATR_Upload_Type(uploadType);
                up.setName(uploadType + " - " + filename + " - " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                up.saveEx();
            }

            // 2) Attach new file
            MAttachment att = new MAttachment(Env.getCtx(), X_ZZ_WSP_ATR_Uploads.Table_ID, up.get_ID(), null, trxName);
            att.addEntry(filename, data);
            att.saveEx();

            trx.commit(true);
        } catch (Exception e) {
            trx.rollback();
            throw e;
        } finally {
            trx.close();
        }
    }
    
    private void deleteAllAttachmentsForUpload(int uploadsId, String trxName) {
        MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Uploads.Table_ID, uploadsId);
        if (att != null) {
            att.delete(true); // deletes AD_Attachment + entries
        }
    }
    
    private org.zkoss.zul.Hbox buildUploadLine(
            int submittedId,
            String uploadType,
            String defaultButtonLabel) {

        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        // Look up existing upload + filename (trxName null is fine for read)
        Integer uploadId = findExistingUploadId(submittedId, uploadType, null);
        String fileName = (uploadId != null) ? findUploadedFileNameForUploadRecord(uploadId.intValue()) : null;

        // Button label depends on whether file exists
        String btnLabel = !Util.isEmpty(fileName, true) ? "Upload again" : defaultButtonLabel;

        org.adempiere.webui.component.Button btn = new org.adempiere.webui.component.Button(btnLabel);
        prepareUploadButton(btn, submittedId, uploadType);

        // filename label next to button
        org.adempiere.webui.component.Label lblFile = new org.adempiere.webui.component.Label(
                !Util.isEmpty(fileName, true) ? fileName : ""
        );
        lblFile.setStyle("margin-left:6px; color:#555;");

        // Keep a reference so we can update it immediately after upload (optional)
        btn.setAttribute("FileLabel", lblFile);

        hb.appendChild(btn);
        hb.appendChild(lblFile);

        return hb;
    }

    private boolean hasAnyReportForSubmitted(int submittedId) {
        // If ZZ_WSP_ATR_Report has ZZ_WSP_ATR_Submitted_ID, use it; else fallback to "latest report exists"
        boolean hasLinkCol = columnExists(X_ZZ_WSP_ATR_Report.Table_Name, "ZZ_WSP_ATR_Submitted_ID", null);

        if (hasLinkCol) {
            int cnt = DB.getSQLValueEx(null,
                "SELECT COUNT(1) FROM ZZ_WSP_ATR_Report WHERE ZZ_WSP_ATR_Submitted_ID=?",
                submittedId);
            return cnt > 0;
        }

        // Fallback: if system creates reports globally, this is a weak inference
        int any = DB.getSQLValueEx(null,
            "SELECT COUNT(1) FROM ZZ_WSP_ATR_Report");
        return any > 0;
    }
    
    private org.zkoss.zul.Hbox buildPrintLine(int submittedId) {
        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setSpacing("10px");
        hb.setAlign("center");

        boolean printedOnce = hasAnyReportForSubmitted(submittedId);

        String btnLabel = printedOnce ? "Re Print..." : "Print Report";

        org.adempiere.webui.component.Button btnPrint = new org.adempiere.webui.component.Button(btnLabel);
        btnPrint.setSclass("btn btn-sm btn-primary wsp-edit-purple");

        org.adempiere.webui.component.Label lblMsg = new org.adempiere.webui.component.Label(
            printedOnce ? "Your report will be emailed to you" : ""
        );
        lblMsg.setStyle("margin-left:6px; color:#555;");

        // Keep reference so we can update immediately after click
        btnPrint.setAttribute("PrintMsgLabel", lblMsg);

        btnPrint.addEventListener(Events.ON_CLICK, (EventListener<Event>) e -> {
            // Update UI immediately
            btnPrint.setLabel("Re Print...");
            lblMsg.setValue("Your report will be emailed to you");

            Clients.showNotification(
                "Your Report is being prepared and will be emailed to you.",
                "info", btnPrint, "top_center", 3500
            );

            // Run process
            doPrintLatestReportForSubmitted(submittedId);

            // Optional: refresh list so state is persisted visually via DB inference
            // refreshList();
        });

        hb.appendChild(btnPrint);
        hb.appendChild(lblMsg);
        return hb;
    }
    
    private byte[] getMediaBytes(Media media) {
        try {
            if (media.isBinary()) {
                return media.getByteData();
            }
            return media.getStringData().getBytes("UTF-8");
        } catch (Exception e) {
            throw new AdempiereException("Failed to read uploaded file: " + e.getMessage());
        }
    }
    
    private Integer findExistingUploadId(int submittedId, String uploadType, String trxName) {
        int id = DB.getSQLValueEx(trxName,
            "SELECT ZZ_WSP_ATR_Uploads_ID " +
            "FROM ZZ_WSP_ATR_Uploads " +
            "WHERE ZZ_WSP_ATR_Submitted_ID=? " +
            "AND ZZ_WSP_ATR_Upload_Type=? " +
            "ORDER BY Updated DESC NULLS LAST, Created DESC NULLS LAST, ZZ_WSP_ATR_Uploads_ID DESC " +
            "FETCH FIRST 1 ROWS ONLY",
            submittedId, uploadType
        );
        return id > 0 ? Integer.valueOf(id) : null;
    }

    private String findUploadedFileNameForUploadRecord(int uploadsId) {
        if (uploadsId <= 0) return null;

        MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Uploads.Table_ID, uploadsId);
        if (att == null || att.getEntryCount() <= 0) return null;

        // You usually only store one entry here; return first non-empty
        for (var e : att.getEntries()) {
            if (e == null) continue;
            String n = e.getName();
            if (Util.isEmpty(n, true)) continue;
            return n;
        }
        return null;
    }

    // ---------------- DICT HELPERS ----------------

    private boolean columnExists(String tableName, String columnName, String trxName) {
        int cnt = DB.getSQLValueEx(
            trxName,
            "SELECT COUNT(1) " +
            "FROM AD_Column c " +
            "JOIN AD_Table t ON t.AD_Table_ID=c.AD_Table_ID " +
            "WHERE t.TableName=? AND c.ColumnName=? AND c.IsActive='Y'",
            tableName, columnName
        );
        return cnt > 0;
    }

    // (Unused here, but handy if you ever need InputStream bytes)
    @SuppressWarnings("unused")
    private byte[] readAllBytes(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = is.read(buf)) > 0) bos.write(buf, 0, r);
        return bos.toByteArray();
    }
}
