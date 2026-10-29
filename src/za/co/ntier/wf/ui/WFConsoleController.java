package za.co.ntier.wf.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.webui.window.FDialog;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import za.co.ntier.wf.model.MZZWFHeader;
import za.co.ntier.wf.model.MZZWFLines;
import za.co.ntier.wf.process.ProcessRunner;
import za.ntier.models.X_ZZ_WF_Audit;

public class WFConsoleController extends SelectorComposer<Window> {

    private static final long serialVersionUID = 1L;
	@Wire private Label lblTable, lblRecord, lblStatus, lblAction;
    @Wire private Rows rowsStep;
    @Wire private Button btnRequest, btnApprove, btnReject;
    @Wire private Textbox txtComment;
    @Wire private Listbox lbAudit;

    private Properties ctx;
    private String trxName;
    private int adTableId;
    private int recordId;
    private PO po;
    private MZZWFHeader hdr;

    @Override
    public void doAfterCompose(Window comp) throws Exception {
        super.doAfterCompose(comp);
        ctx = Env.getCtx();
        trxName = null;

        Object pTable = Executions.getCurrent().getParameterMap().get("AD_Table_ID");
        Object pRecord = Executions.getCurrent().getParameterMap().get("Record_ID");
        adTableId = pTable != null ? Integer.parseInt(((String[])pTable)[0]) : 0;
        recordId = pRecord != null ? Integer.parseInt(((String[])pRecord)[0]) : 0;

        if (adTableId <= 0 || recordId <= 0) {
            FDialog.error(0, "Missing AD_Table_ID / Record_ID");
            return;
        }

        MTable t = MTable.get(ctx, adTableId);
        po = t.getPO(recordId, trxName);
        hdr = MZZWFHeader.forTable(ctx, po.get_Table_ID(),
                po.getAD_Client_ID(), po.getAD_Org_ID(), trxName);

        lblTable.setValue(t.getTableName());
        lblRecord.setValue(String.valueOf(recordId));
        refreshUI();
    }

    private void refreshUI() {
        String st = po.get_ValueAsString("ZZ_DocStatus");
        String act = po.get_ValueAsString("ZZ_DocAction");
        lblStatus.setValue(st);
        lblAction.setValue(act);

        rowsStep.getChildren().clear();
        if (hdr != null) {
            MZZWFLines cur = (act != null && !act.isBlank())
                    ? MZZWFLines.findByStatusAndAction(ctx, hdr.getZZ_WF_Header_ID(), st, act, trxName)
                    : MZZWFLines.findFirstByAllowedFromStatus(ctx, hdr.getZZ_WF_Header_ID(), st, trxName);

            if (cur != null) {
                Row r = new Row();
                r.appendChild(new Label(String.valueOf(cur.getSeqNo())));
                r.appendChild(new Label(cur.getName()));
                r.appendChild(new Label(cur.getAllowedFromStatus()));
                r.appendChild(new Label(cur.getSetDocAction()));
                r.appendChild(new Label(cur.getNextStatusOnApprove()));
                r.appendChild(new Label(cur.getNextStatusOnReject()));
                rowsStep.appendChild(r);
            }
        }
        loadAudit();
    }

    private void loadAudit() {
        lbAudit.getItems().clear();
        List<X_ZZ_WF_Audit> audits = new org.compiere.model.Query(ctx, X_ZZ_WF_Audit.Table_Name,
                "AD_Table_ID=? AND Record_ID=?", trxName)
                .setParameters(adTableId, recordId)
                .setOrderBy("Created DESC")
           //     .setLimit(100)
                .list();

        audits.forEach(a -> {
            Listitem li = new Listitem();
            li.appendChild(new Listcell(a.getCreated().toString()));
            li.appendChild(new Listcell(userName(a.getCreatedBy())));
            li.appendChild(new Listcell(a.getActionTaken()));
            li.appendChild(new Listcell(a.getOldStatus() + " / " + (a.getOldAction()==null? "":a.getOldAction())));
            li.appendChild(new Listcell(a.getNewStatus() + " / " + (a.getNewAction()==null? "":a.getNewAction())));
            li.appendChild(new Listcell(a.getComment()));
            lbAudit.appendChild(li);
        });
    }

    private String userName(int adUserId) {
        org.compiere.model.MUser u = new org.compiere.model.MUser(ctx, adUserId, trxName);
        return u.getName();
    }

    @Listen("onClick = #btnRequest")
    public void onRequest() {
        run("ZZ_WF_Run", null);
    }

    @Listen("onClick = #btnApprove")
    public void onApprove() {
        run("ZZ_WF_Run", new String[][] {{"Approve","Y"}, {"Comment", txtComment.getValue()}});
    }

    @Listen("onClick = #btnReject")
    public void onReject() {
        run("ZZ_WF_Run", new String[][] {{"Approve","N"}, {"Comment", txtComment.getValue()}});
    }

    private void run(String value, String[][] params) {
        Map<String,String> map = null;
        if (params != null) {
            map = new HashMap<>();
            for (String[] p : params) {
                if (p != null && p.length > 1) map.put(p[0], p[1]);
            }
        }
        try {
            ProcessRunner.runByValue(Env.getCtx(), value, adTableId, recordId, map);
        } catch (Exception e) {
            FDialog.error(0, e.getMessage());
        }
        po = MTable.get(Env.getCtx(), adTableId).getPO(recordId, null);
        refreshUI();
    }
}
