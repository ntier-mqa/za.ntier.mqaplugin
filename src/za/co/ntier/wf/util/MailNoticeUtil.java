package za.co.ntier.wf.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.adempiere.webui.apps.AEnv;
import org.compiere.model.I_R_MailText;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MNote;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.MUserRoles;
import org.compiere.model.PO;
import org.compiere.model.X_AD_User;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import org.apache.commons.lang3.StringUtils;
import za.co.ntier.fa.process.api.IDocApprove;
import za.co.ntier.wf.model.MZZWFLineRole;
import za.co.ntier.wf.model.MZZWFLines;

public final class MailNoticeUtil {
	private static final CLogger log = CLogger.getCLogger(MailNoticeUtil.class);
	public MailNoticeUtil() {}
	
	public static void requestStepNotifyAll(List<Map<NotificationFields, Object>> queueNotifis
			                                ,MZZWFLines step, PO po, za.co.ntier.wf.model.MZZWFHeader hdr, 
			                                int tableID,int recordID,MMailText mailText,
			                                Properties ctx, String trxName) {
		if (mailText == null) {
			return;
		}
		for (int roleId : MZZWFLineRole.getRoleIds(step.get_ID(), "N","Y",ctx, trxName)) {
			MailNoticeUtil.queueNotifyForRole(queueNotifis, roleId, tableID, recordID, mailText);
		}
	}


	public static final int FROM_EMAIL_USER_ID = MSysConfig.getIntValue("FROM_EMAIL_USER_ID",1000011);

	protected List<Map<NotificationFields, Object>> queueNotifis = new ArrayList<>();

	public enum NotificationFields {
		MAIL_TO_USER_ID,
		TABLE_ID,
		RECORD_ID,
		MAIL_TEMPLATE;
	}

	public static void queueNotifyForRole(List<Map<NotificationFields, Object>> queueNotifis, int mailRoleId, int tableID, int recordID, MMailText mailTemplate) {
		if (mailTemplate == null) {
			return;
		}
		Arrays.asList(MUserRoles.getOfRole(Env.getCtx(), mailRoleId)).forEach(role -> {
			if (role.isActive()) {
				MailNoticeUtil.queueNotify(queueNotifis, role.getAD_User_ID(), tableID, recordID, mailTemplate);
			}
		});
	}

	public static void queueNotify(List<Map<NotificationFields, Object>> queueNotifis, int mailToUserId, int tableID, int recordID, MMailText mailTemplate) {
		if (mailTemplate == null) {
			return;
		}
		Map<NotificationFields, Object> sentNotifyInfo = new EnumMap<>(NotificationFields.class);
		sentNotifyInfo.put(NotificationFields.MAIL_TO_USER_ID, mailToUserId);
		sentNotifyInfo.put(NotificationFields.TABLE_ID, tableID);
		sentNotifyInfo.put(NotificationFields.RECORD_ID, recordID);
		sentNotifyInfo.put(NotificationFields.MAIL_TEMPLATE, mailTemplate);
		queueNotifis.add(sentNotifyInfo);
	}

	public static void sentNotify(List<Map<NotificationFields, Object>> queueNotifis, PO po,String trxName) {
		queueNotifis.forEach(sentNotifyInfo -> {
			int mailToUserId = (int) sentNotifyInfo.get(NotificationFields.MAIL_TO_USER_ID);
			int tableID = (int) sentNotifyInfo.get(NotificationFields.TABLE_ID);
			int recordID = (int) sentNotifyInfo.get(NotificationFields.RECORD_ID);
			MMailText mailTemplate = (MMailText) sentNotifyInfo.get(NotificationFields.MAIL_TEMPLATE);

			MUser mUser = new MUser(Env.getCtx(), mailToUserId, null);
			if (!mUser.isActive()) {
				return;
			}
			mailTemplate.setUser(mUser);
			String msgHeader = mailTemplate.getMailHeader();
			String msgBody = mailTemplate.getMailText();

			if (X_AD_User.NOTIFICATIONTYPE_Notice.equals(mUser.getNotificationType()) ||
					X_AD_User.NOTIFICATIONTYPE_EMailPlusNotice.equals(mUser.getNotificationType())	) {

				MNote note = new MNote(Env.getCtx(), 0, mUser.getAD_User_ID(),
						tableID, recordID, 
						msgHeader, msgBody, trxName);
				note.setAD_Org_ID(po.getAD_Org_ID());
				note.saveEx();
			}

			if (X_AD_User.NOTIFICATIONTYPE_EMail.equals(mUser.getNotificationType()) ||
					X_AD_User.NOTIFICATIONTYPE_EMailPlusNotice.equals(mUser.getNotificationType())) {

				MClient client = MClient.get(Env.getCtx());

				MUser from = MUser.get(Env.getCtx(), FROM_EMAIL_USER_ID);

				String extraForEmail = mailTemplate.getMailText2();
				if (StringUtils.isNotBlank(extraForEmail)){
					extraForEmail = extraForEmail.replace("@documentLink@", AEnv.getZoomUrlTableUU(mailTemplate.getPO()));
					msgBody = msgBody + System.lineSeparator() + extraForEmail;
				}

				if (!client.sendEMail(from, mUser, msgHeader, msgBody, null, mailTemplate.isHtml())) {
					log.fine("Problem Sending Email.  Please contact Support");
				}

			}
		});
	}
	
	public static MMailText setPOForMail(I_R_MailText mailTemplate,PO po) {
		MMailText mail = (MMailText)mailTemplate;
		mail.setPO((po));
		return mail;
	}
}
