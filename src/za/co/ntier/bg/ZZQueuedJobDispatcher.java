package za.co.ntier.bg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.compiere.Adempiere;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class ZZQueuedJobDispatcher {

	// prevents spamming many schedules in same JVM
	private static final AtomicBoolean KICKED = new AtomicBoolean(false);

	private ZZQueuedJobDispatcher() {}

	public static void kick(int adProcessId, int adClientId) {
		// schedule a near-immediate dispatch attempt (once per short window)
		if (KICKED.compareAndSet(false, true)) {
			Adempiere.getThreadPoolExecutor().schedule(() -> {
				try {
					dispatchNext(adProcessId, adClientId);
				} finally {
					KICKED.set(false);
				}
			}, 200, TimeUnit.MILLISECONDS);
		}
	}

	private static void dispatchNext(int adProcessId, int adClientId) {

		final Properties ctx = Env.getCtx();

		Connection conn = null;
		try {
			conn = DB.getConnection(); // pooled connection
			if (!tryAdvisoryLock(conn, adClientId, adProcessId)) {
				return;
			}
			// cluster-safe: only one dispatcher at a time for (client,process)
			//if (!tryAdvisoryLock(adClientId, adProcessId)) {
			//	return;
			//}


			// If a job is already running for this process/client, do nothing.
			if (isProcessRunning(ctx, adProcessId, adClientId)) {
				return;
			}

			Integer queueId = claimOldestQueued(adClientId, adProcessId);
			if (queueId == null || queueId <= 0) {
				return; // nothing queued
			}

			// Load row


			List<Object> row = DB.getSQLValueObjectsEx(null,
					"SELECT ad_user_id, ad_org_id, record_id " +
							"FROM zz_bg_job_queue WHERE zz_bg_job_queue_id=?",
							queueId);

			if (row == null || row.size() < 3) {
				return; // row missing, safety
			}

			int adUserId = ((Number) row.get(0)).intValue();
			int adOrgId  = ((Number) row.get(1)).intValue();
			int recordId = ((Number) row.get(2)).intValue();

			// Build ProcessInfo
			MProcess proc = new MProcess(ctx, adProcessId, null);
			ProcessInfo pi = new ProcessInfo(proc.getName(), adProcessId);
			pi.setAD_Client_ID(adClientId);
			pi.setAD_User_ID(adUserId);
			pi.setRecord_ID(recordId);

			// Create MPInstance (ONE per job, linked to the record)
			MPInstance instance = new MPInstance(ctx, adProcessId, 0, recordId, null);
			instance.setIsRunAsJob(true);
			instance.setIsProcessing(true);
			instance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice); // or Notice
			instance.saveEx();

			pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());

			// mark queue as running
			DB.executeUpdateEx(
					"UPDATE zz_bg_job_queue SET status='R', started=now(), ad_pinstance_id=?, updated=now(), updatedby=? WHERE zz_bg_job_queue_id=?",
					new Object[] { instance.getAD_PInstance_ID(), adUserId, queueId },
					null
					);

			// Run job. On completion, update queue row and dispatch next.
			ScheduledFuture<ProcessInfo> f =
					Adempiere.getThreadPoolExecutor().schedule(
							new ZZQueueAwareCallable(ctx, pi, queueId, adClientId, adProcessId),
							200,
							TimeUnit.MILLISECONDS
							);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try { advisoryUnlock(conn, adClientId, adProcessId); } catch (Exception ignore) {}
				try { conn.close(); } catch (Exception ignore) {}
			}
		}

		/*
		} finally {
			advisoryUnlock(adClientId, adProcessId);
		}*/
	}

	private static boolean isProcessRunning(Properties ctx, int adProcessId, int adClientId) {
		int count = new Query(ctx, MPInstance.Table_Name,
				"AD_Process_ID=? AND AD_Client_ID=? AND IsProcessing='Y' AND IsRunAsJob='Y'", null)
				.setOnlyActiveRecords(true)
				.setParameters(adProcessId, adClientId)
				.count();
		return count > 0;
	}

	private static Integer claimOldestQueued(int adClientId, int adProcessId) {
		// FIFO claim, safe with concurrency
		String sql =
		        "WITH next AS ( " +
		        "  SELECT zz_bg_job_queue_id " +
		        "  FROM zz_bg_job_queue " +
		        "  WHERE ad_client_id=? AND ad_process_id=? AND status='Q' AND isactive='Y' " +
		        "  ORDER BY created " +
		        "  FOR UPDATE SKIP LOCKED " +
		        "  LIMIT 1 " +
		        ") " +
		        "UPDATE zz_bg_job_queue q " +
		        "SET status='R', updated=now() " +
		        "FROM next " +
		        "WHERE q.zz_bg_job_queue_id = next.zz_bg_job_queue_id " +
		        "RETURNING q.zz_bg_job_queue_id";

		    PreparedStatement ps = null;
		    ResultSet rs = null;

		    try {
		        ps = DB.prepareStatement(sql, null);  // <-- read/write
		        DB.setParameters(ps, new Object[]{adClientId, adProcessId});
		        rs = ps.executeQuery();               // UPDATE ... RETURNING returns ResultSet
		        if (rs.next())
		            return rs.getInt(1);
		        return null;
		    } catch (Exception e) {
		        throw new org.adempiere.exceptions.DBException(e);
		    } finally {
		        DB.close(rs, ps);
		    }
		
	}
	/*
	private static boolean tryAdvisoryLock(int adClientId, int adProcessId) {
		// non-blocking cluster lock
		return DB.getSQLValueEx(null,
				"SELECT CASE WHEN pg_try_advisory_lock(?, ?) THEN 1 ELSE 0 END",
				adClientId, adProcessId) == 1;
	}

	private static void advisoryUnlock(int adClientId, int adProcessId) {
		DB.getSQLValueEx(null, "SELECT pg_advisory_unlock(?, ?)", adClientId, adProcessId);
	}
	 */

	private static boolean tryAdvisoryLock(Connection conn, int adClientId, int adProcessId) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT pg_try_advisory_lock(?, ?)")) {
			ps.setInt(1, adClientId);
			ps.setInt(2, adProcessId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() && rs.getBoolean(1);
			}
		}
	}

	private static void advisoryUnlock(Connection conn, int adClientId, int adProcessId) throws SQLException {
		try (PreparedStatement ps = conn.prepareStatement("SELECT pg_advisory_unlock(?, ?)")) {
			ps.setInt(1, adClientId);
			ps.setInt(2, adProcessId);
			ps.executeQuery();
		}
	}



}