package za.ntier.process;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MMailText;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;

import za.co.ntier.fa.process.api.AbstractDocApproveProcess;
import za.co.ntier.fa.process.api.IDocApprove;
import za.ntier.models.MInventory_New;

@org.adempiere.base.annotation.Process(name="za.ntier.process.ConsumablesRequestDocApproveProcess")
public class ConsumablesRequestDocApproveProcess extends AbstractDocApproveProcess<MInventory_New> {

	private MInventory_New mInventory_New = null;
	
	@Override
	protected void initDocApproveObj() {
		docApprove = poObj;
	}

	@Override
	protected void doLogic(){
		requestConsumables();
	}

	private void requestConsumables() {
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(mInventory_New, DocAction.ACTION_Complete);
		if (pi.isError()) {
			throw new AdempiereException(pi.getSummary()); 
		}
	}

	@Override
	protected String doIt() throws Exception {
		mInventory_New = new MInventory_New(getCtx(), getRecord_ID(), get_TrxName());
		validateData();
		String currentDocAction = docApprove.getZZ_DocAction();
		String currentDocStatus = docApprove.getZZ_DocStatus();
		if(docApprove.isZZ_AllowLineManageApproved() &&
				IDocApprove.ZZ_DOCACTION_SubmitToLineManager.equals(currentDocAction)) {  // User presses Action button
			if (mInventory_New.isZZ_Is_Refreshment_Req()) {
				doSubmitDocForSnrAdminFinance(true);   // Refreshments request - skip Line Manager, go straight to Snr Admin Finance
			} else {
				doSubmitDocForLineManage();
			}
		}else if(docApprove.isZZ_AllowLineManageApproved() &&     // Line Manager presses Action button
				IDocApprove.ZZ_DOCACTION_ApproveDoNotApprove.equals(currentDocAction) && 
				IDocApprove.ZZ_DOCSTATUS_Submitted.equals(currentDocStatus)) {		
			doLineManageApprove();
		}else if(IDocApprove.ZZ_DOCACTION_ApproveDoNotApprove.equals(currentDocAction) &&  // Snr Admin Finance presses Action Button
				IDocApprove.ZZ_DOCSTATUS_SubmittedToSnrAdminFinance.equals(currentDocStatus)) {
			doSnrAdminFinanceApprove();
		}else if(docApprove.isZZ_AllowMgrFinConsumablesApproval() &&
				IDocApprove.ZZ_DOCACTION_ApproveDoNotApprove.equals(currentDocAction) && 
				IDocApprove.ZZ_DOCSTATUS_SubmittedToManagerFinanceConsumables.equals(currentDocStatus)) {  // consumables manager presses button
			doManagerFinConsumablesApprove();
		}
		else {
			throw new AdempiereException(Msg.getMsg(getCtx(), "ZZ_WrongWorkflowState", 
					new Object [] {docApprove.isZZ_AllowLineManageApproved(),
							docApprove.isZZ_AllowSnrAdminFinanceApproved(),
							currentDocStatus,
							currentDocAction}));
		}
		doFinalDocState(currentDocAction);
		poObj.saveEx();

		if (!Util.isEmpty(docApprove.getZZ_FinalWorkflowStateValue(), true) && docApprove.getZZ_FinalWorkflowStateValue().equals(docApprove.getZZ_DocStatus())) {
			doLogic();
		}
		
		sentNotify(queueNotifis, docApprove,get_TrxName());
		return null;
	}

	
	//consumables manager presses button
	protected void doManagerFinConsumablesApprove() {
		docApprove.setZZ_Mgr_Fin_Consumables_ID(Env.getAD_User_ID(getCtx()));
		if("Y".equals(pApprove_Rej_MFC)){
			docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_Completed);
			docApprove.setZZ_Date_MFC_Approved(now);
			AbstractDocApproveProcess.queueNotify(queueNotifis, 
					docApprove.getCreatedBy(), getTable_ID(), getRecord_ID(), docApprove.getZZMailLineApproved());
		}else{
			docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_NotApprovedByManagerFinanceConsumables);
			docApprove.setZZ_Date_MFC_Not_Approved(now);
			AbstractDocApproveProcess.queueNotify(queueNotifis, docApprove.getCreatedBy(), getTable_ID(), getRecord_ID(), docApprove.getZZMailLineReject());
		}
	}
	
	protected void doSubmitDocForSnrAdminFinance(boolean isBypassLineManage) {
		docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_SubmittedToSnrAdminFinance);
		docApprove.setZZ_DocAction(IDocApprove.ZZ_DOCACTION_ApproveDoNotApprove);
		if (!isBypassLineManage)
			docApprove.setZZ_Date_LM_Approved(now);
		if (docApprove.getZZ_Date_Submitted() == null)
			docApprove.setZZ_Date_Submitted(now);

		AbstractDocApproveProcess.queueNotifyForRole(queueNotifis, IDocApprove.SNR_ADMIN_FIN_ROLE_ID, getTable_ID(), getRecord_ID(), docApprove.getZZMailRequestSnr());
	}

	// Line Manager presses Action button
	@Override
	protected void doLineManageApprove() {
		if("Y".equals(pApproveRejLM)){
			doSubmitDocForSnrAdminFinance(false);

		}else{
			docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_NotApprovedByLM);
			docApprove.setZZ_Date_Not_Approved_by_LM(now);
			AbstractDocApproveProcess.queueNotify(queueNotifis, docApprove.getCreatedBy(), getTable_ID(), getRecord_ID(), docApprove.getZZMailLineReject());
		}
	}

	// Snr Admin Finance presses Action Button
	protected void doSnrAdminFinanceApprove() {
		docApprove.setZZ_Snr_Admin_Fin_ID(Env.getAD_User_ID(getCtx()));
		if("Y".equals(pApprove_Rej_SDL)){
			doSubmitDocFinConsumeablesMgr();

		}else{
			docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_NotApprovedBySnrAdminFinance);
			docApprove.setZZ_Date_Not_Approved_by_Snr_Adm_Fin(now);
			AbstractDocApproveProcess.queueNotify(queueNotifis, docApprove.getCreatedBy(), getTable_ID(), getRecord_ID(), docApprove.getZZMailLineReject());
		}
	}

	protected void doSubmitDocFinConsumeablesMgr() {
		docApprove.setZZ_DocStatus(IDocApprove.ZZ_DOCSTATUS_SubmittedToManagerFinanceConsumables);
		docApprove.setZZ_DocAction(IDocApprove.ZZ_DOCACTION_ApproveDoNotApprove);
		docApprove.setZZ_Date_Approved(now);
		if (docApprove.getZZ_Date_Submitted() == null)
			docApprove.setZZ_Date_Submitted(now);

		AbstractDocApproveProcess.queueNotifyForRole(queueNotifis, IDocApprove.MANAGER_FIN_CONSUMABLES_ROLE_ID, getTable_ID(), getRecord_ID(), new MMailText(getCtx(), 1000003, get_TrxName()));
	}

	protected void validateData() {
		MInventoryLine[] lines = mInventory_New.getLines(false);
		if (lines.length == 0)		{
			throw new AdempiereException("@NoLines@");
		}
	}

}
