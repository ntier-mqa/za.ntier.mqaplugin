package za.co.ntier.bg;

import java.util.Properties;
import java.util.concurrent.Callable;

import org.adempiere.webui.apps.BackgroundJobCallable;
import org.compiere.model.MPInstance;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;

public class ZZQueueAwareCallable implements Callable<ProcessInfo> {

    private final Properties ctx;
    private final ProcessInfo pi;
    private final int queueId;
    private final int adClientId;
    private final int adProcessId;

    public ZZQueueAwareCallable(Properties ctx, ProcessInfo pi, int queueId, int adClientId, int adProcessId) {
        this.ctx = ctx;
        this.pi = pi;
        this.queueId = queueId;
        this.adClientId = adClientId;
        this.adProcessId = adProcessId;
    }

    @Override
    public ProcessInfo call() throws Exception {
        ProcessInfo result = null;
        try {
            // This will execute the process and in its finally set MPInstance.IsProcessing=false
            result = new BackgroundJobCallable(ctx, pi).call();

            String status = (result != null && result.isError()) ? "E" : "D";
            String summary = result != null ? result.getSummary() : null;

            DB.executeUpdateEx(
                "UPDATE zz_bg_job_queue SET status=?, finished=now(), summary=?, updated=now(), updatedby=? WHERE zz_bg_job_queue_id=?",
                new Object[] { status, summary, pi.getAD_User_ID(), queueId },
                null
            );
        } catch (Throwable t) {
            DB.executeUpdateEx(
                "UPDATE zz_bg_job_queue SET status='E', finished=now(), summary=?, updated=now(), updatedby=? WHERE zz_bg_job_queue_id=?",
                new Object[] { t.getLocalizedMessage(), pi.getAD_User_ID(), queueId },
                null
            );
            throw t;
        } finally {
            // Immediately attempt to dispatch the next queued job
            ZZQueuedJobDispatcher.kick(adProcessId, adClientId);
        }
        return result;
    }
}
