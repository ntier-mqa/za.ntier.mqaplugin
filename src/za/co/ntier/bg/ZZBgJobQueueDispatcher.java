package za.co.ntier.bg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.ServerContext;
import org.compiere.Adempiere;
import org.compiere.model.MPInstance;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import org.adempiere.webui.apps.BackgroundJobCallable;

@org.adempiere.base.annotation.Process(
		name = "za.co.ntier.bg.ZZBgJobQueueDispatcher")
public class ZZBgJobQueueDispatcher extends SvrProcess {

    private static final CLogger log = CLogger.getCLogger(ZZBgJobQueueDispatcher.class);

    @Override
    protected void prepare() {
    }

    @Override
    protected String doIt() throws Exception {

        final int adClientId = Env.getAD_Client_ID(getCtx());

        // 1) If something is running, do nothing
        int running = DB.getSQLValue(null,
                "SELECT COUNT(*) FROM zz_bg_job_queue " +
                "WHERE ad_client_id=? AND status='R' AND isactive='Y'",
                adClientId);

        if (running > 0) {
            return "Dispatcher: a job is already running (status=R).";
        }

        // 2) Claim the oldest queued job (Q -> R)
        QueueRow row = claimOldestQueued(adClientId);
        if (row == null) {
            return "Dispatcher: no queued job.";
        }

        // 3) Start asynchronously (so scheduler returns quickly)
        scheduleBackgroundRun(row);

        return "Dispatcher: started queueId=" + row.queueId +
               " processId=" + row.adProcessId +
               " recordId=" + row.recordId;
    }

    /**
     * Claim the oldest queued job and mark it Running (R).
     * Uses UPDATE..RETURNING (must use DB.prepareStatement, not DB.getSQLValueEx).
     */
    private QueueRow claimOldestQueued(int adClientId) {

        // NOTE: no locks beyond normal SQL; single-server scheduler assumption
        String sql =
            "UPDATE zz_bg_job_queue q " +
            "SET status='R', started=now(), updated=now(), updatedby=? " +
            "WHERE q.zz_bg_job_queue_id = (" +
            "   SELECT zz_bg_job_queue_id FROM zz_bg_job_queue " +
            "   WHERE ad_client_id=? AND status='Q' AND isactive='Y' " +
            "   ORDER BY created " +
            "   LIMIT 1" +
            ") " +
            "RETURNING q.zz_bg_job_queue_id, q.ad_org_id, q.ad_user_id, q.ad_process_id, q.record_id";

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = DB.prepareStatement(sql, null);
            DB.setParameters(ps, new Object[] {
                Env.getAD_User_ID(getCtx()), // dispatcher user marks updatedby
                adClientId
            });

            rs = ps.executeQuery();
            if (!rs.next())
                return null;

            QueueRow r = new QueueRow();
            r.queueId     = rs.getInt(1);
            r.adClientId  = adClientId;
            r.adOrgId     = rs.getInt(2);
            r.adUserId    = rs.getInt(3);
            r.adProcessId = rs.getInt(4);
            r.recordId    = rs.getInt(5);
            return r;

        } catch (Exception e) {
            throw new AdempiereException("Failed to claim queued job", e);
        } finally {
            DB.close(rs, ps);
        }
    }

    private void scheduleBackgroundRun(QueueRow r) {

        // Build a context for the target user so notifications go to that user
        final Properties jobCtx = new Properties();
        Env.setContext(jobCtx, Env.AD_CLIENT_ID, r.adClientId);
        Env.setContext(jobCtx, Env.AD_ORG_ID, r.adOrgId);
        Env.setContext(jobCtx, Env.AD_USER_ID, r.adUserId);

        // IMPORTANT: BackgroundJobCallable expects AD_ROLE_ID in context.
        // Since your queue table doesn't store role, we derive one from AD_User_Roles.
        int roleId = getRoleForUser(r.adUserId, r.adClientId);
        if (roleId <= 0) {
            // fallback to dispatcher role (avoids crash; may be more permissive)
            roleId = Env.getAD_Role_ID(getCtx());
        }
        Env.setContext(jobCtx, Env.AD_ROLE_ID, roleId);

        final ProcessInfo pi = new ProcessInfo("Queued:" + r.adProcessId, r.adProcessId);
        pi.setAD_Client_ID(r.adClientId);
        pi.setAD_User_ID(r.adUserId);
        pi.setRecord_ID(r.recordId);

        // Create MPInstance so notifications (Email/Notice) behave like normal background jobs
        final MPInstance instance = new MPInstance(jobCtx, r.adProcessId, 0, r.recordId, null);
        instance.setAD_User_ID(r.adUserId);
        instance.setIsRunAsJob(true);
        instance.setIsProcessing(true);
        instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice); // or Notice only
        instance.saveEx();

        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

        // Store AD_PInstance_ID in queue row
        DB.executeUpdateEx(
            "UPDATE zz_bg_job_queue SET ad_pinstance_id=?, updated=now(), updatedby=? WHERE zz_bg_job_queue_id=?",
            new Object[] { instance.getAD_PInstance_ID(), r.adUserId, r.queueId },
            null
        );

        Adempiere.getThreadPoolExecutor().schedule(() -> {
            ServerContext.setCurrentInstance(jobCtx);
            try {
                // Runs the actual process + sends email/notice based on MPInstance notification type
                ProcessInfo result = new BackgroundJobCallable(jobCtx, pi).call();

                String summary = result != null ? result.getSummary() : "Completed";
                boolean isError = result != null && result.isError();

                DB.executeUpdateEx(
                    "UPDATE zz_bg_job_queue " +
                    "SET status=?, finished=now(), summary=?, updated=now(), updatedby=? " +
                    "WHERE zz_bg_job_queue_id=?",
                    new Object[] { isError ? "E" : "D", summary, r.adUserId, r.queueId },
                    null
                );

            } catch (Throwable t) {
                log.log(Level.SEVERE, t.getMessage(), t);

                DB.executeUpdateEx(
                    "UPDATE zz_bg_job_queue " +
                    "SET status='E', finished=now(), summary=?, updated=now(), updatedby=? " +
                    "WHERE zz_bg_job_queue_id=?",
                    new Object[] { safeMsg(t), r.adUserId, r.queueId },
                    null
                );

            } finally {
                try {
                    instance.setIsProcessing(false);
                    instance.saveEx();
                } catch (Exception ignore) {}

                ServerContext.dispose();
            }
        }, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Try get a role for the user in this client.
     * This table exists in iDempiere: AD_User_Roles(ad_user_id, ad_role_id, ad_client_id, isactive)
     */
    private int getRoleForUser(int adUserId, int adClientId) {
        int roleId = DB.getSQLValue(null,
                "SELECT ur.ad_role_id " +
                "FROM ad_user_roles ur " +
                "WHERE ur.ad_user_id=? AND ur.ad_client_id=? AND ur.isactive='Y' " +
                "ORDER BY ur.ad_role_id " +
                "LIMIT 1",
                adUserId, adClientId);
        return roleId;
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) m = t.toString();
        return m.length() > 2000 ? m.substring(0, 2000) : m;
    }

    private static class QueueRow {
        int queueId;
        int adClientId;
        int adOrgId;
        int adUserId;
        int adProcessId;
        int recordId;
    }
}