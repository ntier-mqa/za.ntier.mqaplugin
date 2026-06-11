package za.ntier.process;

import java.util.Objects;
import java.util.logging.Level;

import org.adempiere.base.annotation.Process;
import org.adempiere.webui.panel.PasswordGenerator;
import org.compiere.model.MClient;
import org.compiere.model.MMailText;
import org.compiere.model.MPasswordRule;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

/**
 * Process to dispatch a temporary password to a user via email.
 * 
 * @author niraj
 */
@Process(name = "za.ntier.process.SendTemporaryPasswordProcess")
public class SendTemporaryPasswordProcess extends SvrProcess
{

	private static final String	MAIL_TEMPLATE_UU	= "97b377a4-383a-4bf2-8714-3e47accdc7fc";

	private int					adUserId			= 0;

	@Override
	protected void prepare()
	{
		adUserId = getRecord_ID();

		if (log.isLoggable(Level.FINE))
		{
			for (ProcessInfoParameter para : getParameter())
			{
				if (para.getParameter() != null)
				{
					log.fine("Parameter: %s = %s".formatted(para.getParameterName(), para.getParameter()));
				}
			}
		}
	}

	@Override
	protected String doIt() throws Exception
	{
		if (adUserId <= 0)
		{
			return "Execution Error: No user record selected.";
		}

		var user = new MUser(getCtx(), adUserId, get_TrxName());
		
		String tempPwd = MPasswordRule.getRules(getCtx(), null).generate();
		user.setPassword(tempPwd);
		user.saveEx();
		
		if (user.get_ID() <= 0)
		{
			return "Execution Error: User record could not be found.";
		}

		if (user.getEMail() == null || user.getEMail().isBlank())
		{
			return "Execution Error: User '%s' does not have a valid email address configured.".formatted(user.getName());
		}

		var mailTemplate = (MMailText) new Query(getCtx(), MMailText.Table_Name, "R_MailText_UU=?", get_TrxName())
																													.setParameters(MAIL_TEMPLATE_UU)
																													.first();

		if (mailTemplate == null)
		{
			return "Configuration Error: Mail template (UU: %s) could not be found.".formatted(MAIL_TEMPLATE_UU);
		}

		var msgHeader = mailTemplate.getMailHeader();
		var msgBody = mailTemplate.getMailText(true);

		if (msgBody != null)
		{
			msgBody = msgBody	.replace("@Name@", Objects.requireNonNullElse(user.getName(), ""))
								.replace("@EMail@", Objects.requireNonNullElse(user.getEMail(), ""))
								.replace("@Password@", Objects.requireNonNullElse(user.getPassword(), ""));
		}

		var client = MClient.get(getCtx(), user.getAD_Client_ID());

		boolean emailSent = client.sendEMail(user.getEMail(), msgHeader, msgBody, null, mailTemplate.isHtml());

		if (emailSent)
		{
			return "Success: Temporary password dispatched to %s.".formatted(user.getEMail());
		}
		else
		{
			return "Transmission Error: Encountered a problem dispatching the email to %s. Please review SMTP configurations.".formatted(user.getEMail());
		}
	}

}
