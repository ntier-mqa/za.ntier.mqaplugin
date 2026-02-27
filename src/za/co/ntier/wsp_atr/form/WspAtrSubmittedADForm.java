package za.co.ntier.wsp_atr.form;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Callback;
import org.adempiere.webui.AdempiereWebUI;
import org.adempiere.webui.ISupportMask;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.apps.BackgroundJob;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListCell;
import org.adempiere.webui.component.ListHead;
import org.adempiere.webui.component.ListHeader;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vlayout;

import za.co.ntier.api.model.X_ZZSdfOrganisation;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Lookup_Mapping;
import za.co.ntier.wsp_atr.models.X_ZZ_WSP_ATR_Submitted;
import za.ntier.models.MZZWSPATRSubmitted;

@org.idempiere.ui.zk.annotation.Form(name = "za.co.ntier.wsp_atr.form.WspAtrSubmittedADForm")
public class WspAtrSubmittedADForm extends ADForm implements EventListener<Event> {

	private static final long serialVersionUID = 1L;

	// TODO: set to AD_Process_ID of ValidateAndImportWspAtrDataFromTemplate
	private static final String PROCESS_VALIDATE_IMPORT_UU = "09da67a2-963d-4663-8484-f0b1d4fc6820";

	private Borderlayout layout = new Borderlayout();
	private North north = new North();
	private Center center = new Center();

	private Hbox actions = new Hbox();
	private Toolbarbutton btnUpload = new Toolbarbutton("Upload .xlsm");
	//private Toolbarbutton btnSubmit = new Toolbarbutton("Submit");
	private Toolbarbutton btnRefresh = new Toolbarbutton("Refresh");
	private Label lblInfo = new Label("");
	private Label lblSelectedOrg = new Label("");
	private Media pendingUploadMedia;
	
	private static final CLogger log = CLogger.getCLogger(WspAtrSubmittedADForm.class);




	private Listbox list = new Listbox();
	
	private static final DateTimeFormatter TS_FORMAT =
	        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	protected void initForm() {
		
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		this.appendChild(layout);

		layout.appendChild(north);
		layout.appendChild(center);

		buildNorth();
		buildList();
		refreshList();
		this.addEventListener("onDownloadError", this);
		this.addEventListener("onRunProcess", this);
		org.zkoss.zk.ui.util.Clients.evalJavaScript(
			    "var s=document.createElement('style');" +
			    "s.innerHTML=`" +

			    /* Normal */
			    ".wsp-edit-purple{" +
			    "background:#2f2d8f!important;" +
			    "border-color:#2f2d8f!important;" +
			    "color:#fff!important;" +
			    "transition:all .15s ease-in-out;" +
			    "}" +

			    /* Hover */
			    ".wsp-edit-purple:hover{" +
			    "background:#3d3ab0!important;" +
			    "border-color:#3d3ab0!important;" +
			    "color:#fff!important;" +
			    "}" +

			    /* Pressed (mouse down) */
			    ".wsp-edit-purple:active{" +
			    "background:#ffffff!important;" +
			    "border-color:#2f2d8f!important;" +
			    "color:#2f2d8f!important;" +
			    "box-shadow:inset 0 2px 6px rgba(0,0,0,.25)!important;" +
			    "}" +

			    /* Focus */
			    ".wsp-edit-purple:focus{" +
			    "outline:none!important;" +
			    "box-shadow:0 0 0 2px rgba(47,45,143,.4)!important;" +
			    "}" +

			    "`;" +
			    "document.head.appendChild(s);"
			);

	}

	private void buildNorth() {
		btnUpload.setSclass("btn btn-sm btn-primary wsp-edit-purple");
		btnRefresh.setSclass("btn btn-sm btn-primary wsp-edit-purple");
	    north.setSplittable(false);
	    north.setCollapsible(false);
	    north.setSize("110px"); // a bit taller

	    Div northDiv = new Div();
	    north.appendChild(northDiv);

	    actions.setSpacing("12px");
	    actions.appendChild(btnUpload);
	//    actions.appendChild(btnSubmit);
	    actions.appendChild(btnRefresh);

	    // IMPORTANT: use ON_UPLOAD for upload button if you're using UploadEvent
	    btnUpload.setUpload(AdempiereWebUI.getUploadSetting());
	    btnUpload.addEventListener(Events.ON_UPLOAD, this);

	   // btnSubmit.addEventListener(Events.ON_CLICK, this);
	    btnRefresh.addEventListener(Events.ON_CLICK, this);

	    northDiv.appendChild(actions);
	    northDiv.appendChild(new Separator());

	    // show selected org
	    northDiv.appendChild(lblSelectedOrg);
	    northDiv.appendChild(new Separator());

	    // your info label
	    northDiv.appendChild(lblInfo);
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
		head.appendChild(new ListHeader("Uploaded File"));
		head.appendChild(new ListHeader("Latest Error File"));
		head.appendChild(new ListHeader("Status"));
		head.appendChild(new ListHeader("Actions"));
	}

	@Override
	public void onEvent(Event event) throws Exception {				
		
		if (event instanceof UploadEvent && event.getTarget() == btnUpload) {
	        UploadEvent ue = (UploadEvent) event;

	        Media media = ue.getMedia(); // single upload
	        if (media == null)
	            return;

	        pendingUploadMedia = media;   // STORE IT
	        promptForOrganisationThenUpload();
	        return;
	    }

	/*	if (event.getTarget() == btnSubmit) {
			doSubmitSelected();
			return;
		}
		*/
		if (event.getTarget() == btnRefresh) {
			refreshList();
			return;
		}

		if ("onDownloadError".equals(event.getName())) {
			int submittedId = (Integer) event.getData();
			downloadLatestError(submittedId);
			return;
		}

	/*	if ("onRunProcess".equals(event.getName())) {
			int submittedId = (Integer) event.getData();
			runValidateImportInBackground(submittedId);
			return;
		}
		*/
	}


	// ---------------- UI Actions ----------------
	private void promptForOrganisationThenUpload() {

	    List<SdfOrgRow> orgs = getSdfOrganisationsForUser();
	    if (orgs == null || orgs.isEmpty())
	        throw new AdempiereException("No organisations are linked to your user.");

	    Window win = new Window();
	    win.setTitle("Select Organisation");
	    win.setBorder("normal");
	    win.setClosable(true);
	    win.setSizable(false);

	    // IMPORTANT: use highlighted, not modal
	    win.setAttribute(Window.MODE_KEY, Window.MODE_HIGHLIGHTED);

	    Vlayout body = new Vlayout();
	    body.setSpacing("10px");
	    body.setStyle("padding:12px; min-width:360px;");
	    win.appendChild(body);

	    // Combo
	    Listbox lb = new Listbox();
	    lb.setMold("select");
	    lb.setRows(0);
	    ZKUpdateUtil.setHflex(lb, "1");
	    body.appendChild(lb);

	    for (SdfOrgRow r : orgs) {
	        // iDempiere ListItem(label,value)
	        org.adempiere.webui.component.ListItem li =
	                new org.adempiere.webui.component.ListItem(r.orgName, r);
	        lb.appendChild(li);
	    }
	    lb.setSelectedIndex(0);

	    // Buttons below combo
	    Hbox buttons = new Hbox();
	    buttons.setSpacing("10px");
	    buttons.setPack("end");
	    body.appendChild(buttons);

	    org.adempiere.webui.component.Button ok = new org.adempiere.webui.component.Button("OK");
	    org.adempiere.webui.component.Button cancel = new org.adempiere.webui.component.Button("Cancel");
	    buttons.appendChild(ok);
	    buttons.appendChild(cancel);

	    // Show with mask (pseudo-modal)
	    final ISupportMask mask = LayoutUtils.showWindowWithMask(win, (Component) this, LayoutUtils.OVERLAP_PARENT);

	    // Ensure mask removed when window closes
	    win.addEventListener(DialogEvents.ON_WINDOW_CLOSE, e -> mask.hideMask());

	    ok.addEventListener(Events.ON_CLICK, e -> {
	        org.adempiere.webui.component.ListItem sel =
	                (org.adempiere.webui.component.ListItem) lb.getSelectedItem();
	        if (sel == null)
	            throw new AdempiereException("Please select an organisation.");

	        SdfOrgRow chosen = (SdfOrgRow) sel.getValue();

	        win.detach();        // triggers ON_WINDOW_CLOSE -> hides mask

	        doUploadWithOrganisation(chosen, pendingUploadMedia);
	        pendingUploadMedia = null;
	    });

	    cancel.addEventListener(Events.ON_CLICK, e -> win.detach());
	}


	private void doUploadWithOrganisation(SdfOrgRow org, Media media) throws Exception {
	    if (media == null)
	        return;

	    String filename = media.getName();
	    if (!filename.toLowerCase().endsWith(".xlsm"))
	        throw new AdempiereException("Please upload an .xlsm file");
	    if (filename.toLowerCase().startsWith("error")) {
	    	throw new AdempiereException("File Name cannot start with Error,please rename and try again");
	    }

	    byte[] data = getMediaBytes(media);

	    String trxName = Trx.createTrxName("WSPATRUpload");
	    Trx trx = Trx.get(trxName, true);

	    int submittedId = 0;

	    try {
	        // 1) Find existing record for org (if any)
	        int existingId = findExistingSubmittedIdForOrg(org.zzSdfOrganisationId, trxName);

	        MZZWSPATRSubmitted  submitted;
	        if (existingId > 0) {
	            // REUSE
	            submitted = new MZZWSPATRSubmitted(Env.getCtx(), existingId, trxName);

	            // 2) Clear old attachments + related records
	            deleteAllAttachmentsForSubmitted(existingId, trxName);
	            deleteRelatedRecordsBeforeProcessing(existingId, trxName);

	            // 3) Update the row fields (optional but recommended)
	            submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
	            submitted.setFileName(filename);
	            submitted.setName("WSP/ATR " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
	            submitted.setZZ_Import_Submitted_Data("N");
	            submitted.setZZSdfOrganisation_ID(org.zzSdfOrganisationId);
	            submitted.setZZ_DocStatus(X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating);
	            submitted.saveEx();

	            submittedId = existingId;
	        } else {
	            // CREATE NEW
	            submitted = new MZZWSPATRSubmitted(Env.getCtx(), 0, trxName);
	            submitted.setName("WSP/ATR " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
	            submitted.setSubmittedDate(new Timestamp(System.currentTimeMillis()));
	            submitted.setFileName(filename);
	            submitted.setZZ_Import_Submitted_Data("N");
	            submitted.setZZSdfOrganisation_ID(org.zzSdfOrganisationId);
	            submitted.setZZ_DocStatus(X_ZZ_WSP_ATR_Submitted.ZZ_DOCSTATUS_Validating);
	            submitted.saveEx();

	            submittedId = submitted.get_ID();
	        }

	        // 4) Recreate attachment and attach the new file (same trx)
	        MAttachment att = new MAttachment(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId, null, trxName);
	        att.addEntry(filename, data);
	        att.saveEx();

	        trx.commit(true);

	    } catch (Exception e) {
	        trx.rollback();
	        throw e;
	    } finally {
	        trx.close();
	    }

	    lblSelectedOrg.setValue("Organisation: " + org.orgName);
	    lblInfo.setValue("Uploaded: " + filename + " (ID " + submittedId + ")");
	    refreshList();
	    runValidateImportInBackground(submittedId);
	    
	}

		
	private void refreshList() {
	    list.getItems().clear();

	    int adUserId = Env.getAD_User_ID(Env.getCtx());

	    String sql =
	        "SELECT s.ZZ_WSP_ATR_Submitted_ID, s.SubmittedDate, s.FileName, " +
	        "       s.ZZ_Import_Submitted_Data, v.orgname, s.ZZ_DocStatus " +

	        "FROM ZZ_WSP_ATR_Submitted s " +
	        "LEFT JOIN adempiere.zzsdforganisation_v v " +
	        "  ON v.zzsdforganisation_v_id = s.ZZSDFOrganisation_ID " +
	        "WHERE v.ad_user_id = ? " +
	        "ORDER BY s.ZZ_WSP_ATR_Submitted_ID DESC";

	    
	    List<List<Object>> rows = org.compiere.util.DB.getSQLArrayObjectsEx(null, sql, adUserId);

	    if (rows == null)
	        return;

	    for (List<Object> r : rows) {
	        int id = ((Number) r.get(0)).intValue();
	        Timestamp submittedDate = (Timestamp) r.get(1);

	        String uploaded = findUploadedFileName(id);
	        String latestError = findLatestErrorFileName(id);

	        String orgName = (String) r.get(4);
	        String statusCode = (String) r.get(5);

	        String status = statusLabel(statusCode);

	        addRow(id, orgName, submittedDate, uploaded, latestError, status);
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



	private void addRow(int id, String orgName, Timestamp submittedDate, String uploaded, String latestError, String status) {

	    ListItem item = new ListItem();
	    item.setValue(Integer.valueOf(id)); // keep ID as row value for actions
	    
	    String formattedDate = "";
	    if (submittedDate != null) {
	        formattedDate = submittedDate.toLocalDateTime().format(TS_FORMAT);
	    }

	    item.appendChild(new ListCell(!Util.isEmpty(orgName, true) ? orgName : ""));
	    item.appendChild(new ListCell(submittedDate != null ? formattedDate : ""));
	    item.appendChild(new ListCell(uploaded != null ? uploaded : ""));
	    item.appendChild(new ListCell(latestError != null ? latestError : ""));
	    item.appendChild(new ListCell(status));

	    // actions cell stays the same...
	    ListCell actions = new ListCell();
	    Hbox hb = new Hbox();
	    hb.setSpacing("8px");

	    
	    if (!Util.isEmpty(latestError, true)) {
	        org.adempiere.webui.component.Button dlBtn =
	                new org.adempiere.webui.component.Button("Download Error");

	        // these classes usually render like the standard purple “Edit” button in iDempiere themes
	        dlBtn.setSclass("btn btn-sm btn-primary wsp-edit-purple");

	        dlBtn.addEventListener(Events.ON_CLICK, (EventListener<Event>) e ->
	            Events.postEvent(new Event("onDownloadError", this, Integer.valueOf(id))));

	        hb.appendChild(dlBtn);
	    }

	    actions.appendChild(hb);
	    item.appendChild(actions);

	    list.appendChild(item);
	}



	// ---------------- Background Process ----------------

	
	
	private void runValidateImportInBackground(final int submittedId) {
	    final Properties ctx = Env.getCtx();

	    final MProcess proc = MProcess.get(ctx, PROCESS_VALIDATE_IMPORT_UU);
	    if (proc == null || proc.getAD_Process_ID() <= 0) {
	        throw new AdempiereException("Validate/Import process not found (UU=" + PROCESS_VALIDATE_IMPORT_UU + ")");
	    }

	    String trxName = null;
	    if (hasRunningJob(proc.getAD_Process_ID(), submittedId, trxName)) {
	        lblInfo.setValue("A job is already running for Submitted ID " + submittedId);
	        return;
	    }

	    // Build ProcessInfo
	    final ProcessInfo pi = new ProcessInfo(proc.getName(), proc.getAD_Process_ID());
	    pi.setAD_User_ID(Env.getAD_User_ID(ctx));
	    pi.setAD_Client_ID(Env.getAD_Client_ID(ctx));
	    pi.setRecord_ID(submittedId);
	    pi.setAD_Process_UU(proc.getAD_Process_UU());

	    // Create MPInstance tied to THIS record so the process runs with correct context
	    final MPInstance instance = new MPInstance(ctx, proc.getAD_Process_ID(), 0, submittedId, null);
	    instance.setIsRunAsJob(true);

	    // Optional: force notification type (Notice is usually nicest in UI)
	    instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);

	    instance.saveEx();
	    pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

	    // No parameters to save from this form, so callback can be empty
	    Callback<Integer> createInstanceParaCallback = id -> {
	        // If you later add process parameters, you’d save them here.
	    };

	    BackgroundJob.create(pi)
	        .withContext(ctx)
	        .withNotificationType(instance.getNotificationType())  // or hardcode Notice/Email
	        .withInitialDelay(500)                                // optional
	        .run(createInstanceParaCallback);

	    lblInfo.setValue("Background job queued for Submitted ID " + submittedId);
	}
	
	private boolean hasRunningJob(int adProcessId, int recordId, String trxName) {
	    // AD_PInstance.IsProcessing='Y' is the usual “still running”
	    int cnt = DB.getSQLValueEx(trxName,
	        "SELECT COUNT(1) " +
	        "FROM AD_PInstance " +
	        "WHERE AD_Process_ID=? " +
	        "AND Record_ID=? " +
	        "AND IsProcessing='Y'",
	        adProcessId, recordId);
	    return cnt > 0;
	}



	// ---------------- Download Error ----------------

	private void downloadLatestError(int submittedId) {
		MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId);
		if (att == null || att.getEntryCount() <= 0)
			throw new AdempiereException("No attachment found for record " + submittedId);

		MAttachmentEntry err = findLatestErrorEntry(att);
		if (err == null)
			throw new AdempiereException("No error file found for record " + submittedId);

		try (InputStream is = err.getInputStream()) {
			byte[] data = readAllBytes(is);
			Filedownload.save(
					data,
					"application/vnd.ms-excel.sheet.macroEnabled.12",
					err.getName());
		} catch (Exception e) {
			throw new AdempiereException("Download failed: " + e.getMessage());
		}
	}

	// ---------------- Attachments Helpers ----------------

	

	private String findUploadedFileName(int submittedId) {
		MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId);
		if (att == null || att.getEntryCount() <= 0)
			return null;

		for (MAttachmentEntry e : att.getEntries()) {
			if (e == null) continue;
			String n = e.getName();
			if (Util.isEmpty(n, true)) continue;
			if (n.toUpperCase().startsWith("ERROR")) continue;
			return n;
		}
		return null;
	}

	private String findLatestErrorFileName(int submittedId) {
		MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId);
		if (att == null || att.getEntryCount() <= 0)
			return null;

		MAttachmentEntry e = findLatestErrorEntry(att);
		return e != null ? e.getName() : null;
	}

	private MAttachmentEntry findLatestErrorEntry(MAttachment att) {
		List<MAttachmentEntry> errors = Arrays.stream(att.getEntries())
				.filter(Objects::nonNull)
				.filter(e -> !Util.isEmpty(e.getName(), true))
				.filter(e -> e.getName().toUpperCase().startsWith("ERROR"))
				.collect(Collectors.toList());

		if (errors.isEmpty())
			return null;

		// if your error name includes yyyyMMdd_HHmmss, lexical sort works
		errors.sort(Comparator.comparing(MAttachmentEntry::getName));
		return errors.get(errors.size() - 1);
	}

	// ---------------- Media / IO Helpers ----------------

	private byte[] getMediaBytes(Media media) {
		try {
			// xlsm should be binary
			if (media.isBinary()) {
				return media.getByteData();
			}
			// fallback (should not happen)
			return media.getStringData().getBytes("UTF-8");
		} catch (Exception e) {
			throw new AdempiereException("Failed to read uploaded file: " + e.getMessage());
		}
	}

	private byte[] readAllBytes(InputStream is) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int r;
		while ((r = is.read(buf)) > 0) {
			bos.write(buf, 0, r);
		}
		return bos.toByteArray();
	}
	
	private List<SdfOrgRow> getSdfOrganisationsForUser() {
	    int adUserId = Env.getAD_User_ID(Env.getCtx());

	    String sql =
	        "SELECT zzsdforganisation_v_id, orgname " +
	        "FROM adempiere.zzsdforganisation_v " +
	        "WHERE ad_user_id = ? " +
	        "AND isactive = 'Y' " +
	        "AND zzsdfroletype = '" + X_ZZSdfOrganisation.ZZSDFROLETYPE_Primary + "' " +
	        " AND ZZ_DOCStatus <> 'DR' and ZZ_DOCStatus <> 'UnSdfOrg' " + 
	        "ORDER BY orgname";

	    List<List<Object>> rows =
	            org.compiere.util.DB.getSQLArrayObjectsEx(null, sql, adUserId);
	    if (rows != null) {
		    return rows.stream()
		    	    .map(r -> new SdfOrgRow(
		    	            ((Number) r.get(0)).intValue(),
		    	            (String) r.get(1)))
		    	    .collect(Collectors.toList());
	    } 
	    return null;

	}

	
	
	private static class SdfOrgRow {
	    final int zzSdfOrganisationId;
	    final String orgName;

	    SdfOrgRow(int id, String name) {
	        this.zzSdfOrganisationId = id;
	        this.orgName = name;
	    }

	    @Override
	    public String toString() {
	        return orgName;
	    }
	}
	
	private int findExistingSubmittedIdForOrg(int zzSdfOrganisationId, String trxName) {
	    // Prefer a draft/not imported record
	    int id = org.compiere.util.DB.getSQLValueEx(
	            trxName,
	            "SELECT ZZ_WSP_ATR_Submitted_ID " +
	            "FROM ZZ_WSP_ATR_Submitted " +
	            "WHERE ZZSDFOrganisation_ID=? " +
	            "AND COALESCE(ZZ_Import_Submitted_Data,'N')='N' " +
	            "ORDER BY SubmittedDate DESC NULLS LAST, ZZ_WSP_ATR_Submitted_ID DESC " +
	            "FETCH FIRST 1 ROWS ONLY",
	            zzSdfOrganisationId
	    );

	    if (id > 0)
	        return id;

	    // Fallback: any latest record for the org
	    return org.compiere.util.DB.getSQLValueEx(
	            trxName,
	            "SELECT ZZ_WSP_ATR_Submitted_ID " +
	            "FROM ZZ_WSP_ATR_Submitted " +
	            "WHERE ZZSDFOrganisation_ID=? " +
	            "ORDER BY SubmittedDate DESC NULLS LAST, ZZ_WSP_ATR_Submitted_ID DESC " +
	            "FETCH FIRST 1 ROWS ONLY",
	            zzSdfOrganisationId
	    );
	}
	
	private void deleteAllAttachmentsForSubmitted(int submittedId, String trxName) {
	    MAttachment att = MAttachment.get(Env.getCtx(), X_ZZ_WSP_ATR_Submitted.Table_ID, submittedId);
	    if (att != null) {
	        att.delete(true); // deletes AD_Attachment and its entries
	    }
	}
	
	private void deleteRelatedRecordsBeforeProcessing(int submittedId, String trxName) {

	    // 1) Load mapped tables
	    List<X_ZZ_WSP_ATR_Lookup_Mapping> headers = new Query(
	            Env.getCtx(),
	            X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name,
	            null,
	            trxName)
	        .setOnlyActiveRecords(true)
	        .list();

	    if (headers == null || headers.isEmpty())
	        return; // nothing to delete

	    // 2) Build unique table list
	    Set<Integer> tableIds = new HashSet<>();
	    for (X_ZZ_WSP_ATR_Lookup_Mapping h : headers) {
	        if (h.getAD_Table_ID() > 0)
	            tableIds.add(h.getAD_Table_ID());
	    }

	    // 3) Create delete jobs for tables that have ZZ_WSP_ATR_Submitted_ID
	    class DelJob {
	        final String tableName;
	        DelJob(String t) { tableName = t; }
	    }

	    List<DelJob> jobs = new ArrayList<>();

	    for (Integer adTableId : tableIds) {
	        MTable t = MTable.get(Env.getCtx(), adTableId);
	        if (t == null) continue;

	        String tableName = t.getTableName();
	        if (Util.isEmpty(tableName, true))
	            continue;

	        // Safety: never delete from these
	        if (X_ZZ_WSP_ATR_Submitted.Table_Name.equalsIgnoreCase(tableName)
	                || X_ZZ_WSP_ATR_Lookup_Mapping.Table_Name.equalsIgnoreCase(tableName))
	            continue;

	        // Only delete if table is designed to link back to submittedId
	        if (columnExists(tableName, "ZZ_WSP_ATR_Submitted_ID", trxName)) {
	            jobs.add(new DelJob(tableName));
	        }
	    }

	    if (jobs.isEmpty())
	        return;

	    // 4) Run deletes in retry passes to handle FK order
	    List<DelJob> remaining = new ArrayList<>(jobs);

	    for (int pass = 1; pass <= 4 && !remaining.isEmpty(); pass++) {
	        List<DelJob> failed = new ArrayList<>();

	        for (DelJob job : remaining) {
	            try {
	            	Object [] parms = {submittedId};
	            	DB.executeUpdateEx(
	                    "DELETE FROM " + job.tableName + " WHERE ZZ_WSP_ATR_Submitted_ID=?",
	                    parms,
	                    trxName,
	                    0
	                );
	            } catch (Exception ex) {
	                failed.add(job);
	            }
	        }

	        remaining = failed;
	    }

	    // If some still remain, log them (usually means those tables link differently)
	    if (!remaining.isEmpty()) {
	        for (DelJob job : remaining) {
	            log.log(Level.WARNING,
	                "Could not delete from mapped table " + job.tableName +
	                " for ZZ_WSP_ATR_Submitted_ID=" + submittedId +
	                " (likely FK order or different link column).");
	        }
	    }
	}

	
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





}
