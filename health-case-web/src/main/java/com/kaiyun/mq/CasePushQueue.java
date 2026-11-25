package com.kaiyun.mq;

import com.kaiyun.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CasePushQueue {
    private static final Logger log = LoggerFactory.getLogger(CasePushQueue.class);
    private static final long[] RETRY_SECONDS = {15, 60, 300, 900};
    private static final int MAX_ATTEMPTS = RETRY_SECONDS.length + 1;
    private static final String DUE = "((status IN ('QUEUED','RETRY_WAIT') AND next_attempt_at <= CURRENT_TIMESTAMP) "
            + "OR (status = 'RUNNING' AND lease_until <= CURRENT_TIMESTAMP))";
    private final JdbcTemplate jdbc;
    private final CasePushDelivery delivery;
    private final RabbitCasePushTransport rabbit;
    private volatile boolean ready;

    public CasePushQueue(JdbcTemplate jdbc, CasePushDelivery delivery, Optional<RabbitCasePushTransport> rabbit,
            @Value("${app.case-push.transport:rabbitmq}") String transport) {
        this.jdbc = jdbc;
        this.delivery = delivery;
        this.rabbit = rabbit.orElse(null);
        if (this.rabbit == null && !"mysql".equals(transport)) {
            throw new IllegalArgumentException("赔案推送模式仅支持 rabbitmq 或 mysql");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enableWorker() {
        ready = true;
        log.info("赔案推送工作器已就绪 transport={} configured={}", rabbit == null ? "mysql" : "rabbitmq", isConfigured());
        if (!isConfigured()) log.warn("赔案推送暂未启用，请检查队列连接参数和接收方配置");
    }

    public boolean isConfigured() { return delivery.isConfigured() && (rabbit == null || rabbit.isConfigured()); }

    @Transactional
    public int enqueue(List<Long> ids) {
        int queued = 0;
        for (Long id : ids.stream().distinct().sorted().toArray(Long[]::new)) {
            List<String> cases = lockCase(id);
            if (cases.isEmpty() || hasActiveTask(id)) continue;
            PushRecord latest = latest(id);
            if (latest != null && !"SUCCEEDED".equals(latest.getStatus())) continue;
            insert(id, cases.get(0), "MANUAL", "QUEUED", delivery.routingFingerprint(id), null);
            queued++;
        }
        return queued;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueStatusChange(long caseId, String caseStatus) {
        String route;
        try {
            route = delivery.routingFingerprint(caseId);
        } catch (BusinessException error) {
            String message = error.getMessage();
            insert(caseId, caseStatus, "STATUS_CHANGE", "FAILED", null, message.substring(0, Math.min(message.length(), 200)));
            return;
        }
        insert(caseId, caseStatus, "STATUS_CHANGE", "QUEUED", route, null);
    }

    private void insert(long caseId, String caseStatus, String trigger, String status, String route, String error) {
        jdbc.update("INSERT INTO case_push_task (case_id, task_id, trigger_type, case_status, status, attempts, "
                        + "next_attempt_at, updated_at, receiver_fingerprint, last_error) "
                        + "VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)",
                caseId, UUID.randomUUID().toString(), trigger, caseStatus, status, route, error);
    }

    public List<PushRecord> history(long caseId) {
        return taskRecords(caseId, 100, false);
    }

    private PushRecord latest(long caseId) {
        List<PushRecord> tasks = taskRecords(caseId, 1, true);
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private List<PushRecord> taskRecords(long caseId, int limit, boolean lock) {
        return jdbc.query("SELECT task_id, trigger_type, case_status, status, attempts, updated_at, last_error "
                        + "FROM case_push_task WHERE case_id = ? ORDER BY task_order DESC LIMIT ?" + (lock ? " FOR UPDATE" : ""),
                (row, index) -> new PushRecord(row.getString(1), row.getString(2), row.getString(3), row.getString(4),
                        row.getInt(5), row.getTimestamp(6).toLocalDateTime(), row.getString(7)), caseId, limit);
    }

    private List<String> lockCase(long caseId) {
        // 与状态保存使用同一案件锁，避免并发点击产生重复手动任务。
        return jdbc.query("SELECT status FROM service_record WHERE id = ? FOR UPDATE", (row, index) -> row.getString(1), caseId);
    }

    private boolean hasActiveTask(long caseId) {
        return !jdbc.queryForList("SELECT task_id FROM case_push_task WHERE case_id = ? "
                + "AND status IN ('QUEUED','RUNNING','RETRY_WAIT') LIMIT 1 FOR UPDATE", String.class, caseId).isEmpty();
    }

    @Transactional
    public boolean retry(long caseId, String taskId) {
        if (lockCase(caseId).isEmpty()) return false;
        if (taskId == null) {
            PushRecord latest = latest(caseId);
            if (latest == null || !"FAILED".equals(latest.getStatus())) return false;
            taskId = latest.getTaskId();
        }
        return jdbc.update("UPDATE case_push_task SET status = 'QUEUED', attempts = 0, next_attempt_at = CURRENT_TIMESTAMP, "
                + "dispatch_after = NULL, updated_at = CURRENT_TIMESTAMP, last_error = NULL, receiver_fingerprint = ? "
                + "WHERE case_id = ? AND task_id = ? AND status = 'FAILED'", delivery.routingFingerprint(caseId), caseId, taskId) == 1;
    }

    @Scheduled(fixedDelayString = "${app.case-push.poll-ms:3000}")
    public void poll() {
        if (!ready || !isConfigured()) return;
        jdbc.update("UPDATE case_push_task SET status = 'FAILED', lease_token = NULL, lease_until = NULL, "
                + "updated_at = CURRENT_TIMESTAMP, last_error = '最后一次处理超时，请向接收方核对结果' "
                + "WHERE status = 'RUNNING' AND lease_until <= CURRENT_TIMESTAMP AND attempts >= ?", MAX_ATTEMPTS);
        if (rabbit != null) rabbit.startListener();
        String dispatchFilter = rabbit == null ? "" : " AND (dispatch_after IS NULL OR dispatch_after <= CURRENT_TIMESTAMP)";
        List<Task> tasks = jdbc.query("SELECT case_id, task_id, attempts, receiver_fingerprint FROM case_push_task WHERE attempts < ? AND "
                        + DUE + dispatchFilter + " ORDER BY next_attempt_at, task_order LIMIT 10",
                (rs, row) -> new Task(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getString(4)), MAX_ATTEMPTS);
        for (Task task : tasks) {
            if (rabbit == null) run(task);
            else if (!publish(task)) break;
        }
    }

    private boolean publish(Task task) {
        // MySQL 任务行是待发记录。即使确认丢失，也只会用原任务 ID 重新唤醒消费者。
        if (jdbc.update("UPDATE case_push_task SET dispatch_after = TIMESTAMPADD(SECOND, 60, CURRENT_TIMESTAMP) "
                        + "WHERE case_id = ? AND task_id = ? AND attempts = ? AND " + DUE
                        + " AND (dispatch_after IS NULL OR dispatch_after <= CURRENT_TIMESTAMP)",
                task.caseId, task.taskId, task.attempts) != 1) return true;
        if (!rabbit.publish(task.caseId, task.taskId)) return false;
        jdbc.update("UPDATE case_push_task SET dispatch_after = TIMESTAMPADD(SECOND, 300, CURRENT_TIMESTAMP) "
                        + "WHERE case_id = ? AND task_id = ? AND attempts = ? AND " + DUE,
                task.caseId, task.taskId, task.attempts);
        return true;
    }

    void consume(long caseId, String taskId) {
        List<Task> tasks = jdbc.query("SELECT case_id, task_id, attempts, receiver_fingerprint FROM case_push_task "
                        + "WHERE case_id = ? AND task_id = ? AND attempts < ? AND " + DUE,
                (rs, row) -> new Task(rs.getLong(1), rs.getString(2), rs.getInt(3), rs.getString(4)), caseId, taskId, MAX_ATTEMPTS);
        if (tasks.isEmpty()) {
            log.info("赔案消息无需执行，任务已处理或尚未到执行时间 taskId={} caseId={}", taskId, caseId);
            return;
        }
        run(tasks.get(0));
    }

    private void run(Task task) {
        String lease = UUID.randomUUID().toString();
        if (jdbc.update("UPDATE case_push_task SET status = 'RUNNING', attempts = attempts + 1, lease_token = ?, "
                        + "lease_until = TIMESTAMPADD(SECOND, 60, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP, "
                        + "dispatch_after = NULL WHERE case_id = ? AND task_id = ? AND attempts = ? AND " + DUE,
                lease, task.caseId, task.taskId, task.attempts) != 1) return;
        log.info("赔案推送开始 taskId={} caseId={} attempt={}", task.taskId, task.caseId, task.attempts + 1);
        try {
            int status = delivery.send(task.taskId, task.caseId, task.receiverFingerprint);
            if (status >= 200 && status < 300) {
                finish(task, lease, "SUCCEEDED", null, 0);
            } else {
                fail(task, lease, "接收方返回 HTTP " + status, status == 408 || status == 429 || status >= 500);
            }
        } catch (CasePushDelivery.ConfigurationException error) {
            fail(task, lease, error.getMessage(), false);
        } catch (SocketTimeoutException error) {
            fail(task, lease, "接收方响应超时，受理结果待核对", true);
        } catch (IOException error) {
            log.warn("赔案推送连接失败 taskId={} type={}", task.taskId, error.getClass().getSimpleName());
            fail(task, lease, "连接接收方失败，受理结果待核对", true);
        } catch (RuntimeException error) {
            log.warn("赔案推送执行异常 taskId={} type={}", task.taskId, error.getClass().getSimpleName());
            fail(task, lease, "读取推送配置或处理任务失败，请检查服务日志", true);
        }
    }

    private void fail(Task task, String lease, String reason, boolean retryable) {
        if (retryable && task.attempts < RETRY_SECONDS.length) {
            finish(task, lease, "RETRY_WAIT", reason, RETRY_SECONDS[task.attempts]);
        } else {
            finish(task, lease, "FAILED", reason, 0);
        }
    }

    private void finish(Task task, String lease, String status, String reason, long delay) {
        int updated = jdbc.update("UPDATE case_push_task SET status = ?, last_error = ?, next_attempt_at = TIMESTAMPADD(SECOND, ?, CURRENT_TIMESTAMP), "
                        + "lease_token = NULL, lease_until = NULL, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE case_id = ? AND task_id = ? AND status = 'RUNNING' AND lease_token = ?",
                status, reason, delay, task.caseId, task.taskId, lease);
        if (updated == 0) {
            log.warn("赔案处理租约已失效，结果未覆盖 taskId={} caseId={}", task.taskId, task.caseId);
        } else if ("SUCCEEDED".equals(status)) {
            log.info("赔案推送成功 taskId={} caseId={} attempt={}", task.taskId, task.caseId, task.attempts + 1);
        } else {
            log.warn("赔案推送未成功 taskId={} caseId={} status={} attempt={} retryDelaySeconds={} reason={}",
                    task.taskId, task.caseId, status, task.attempts + 1, delay, reason);
        }
    }

    @lombok.Value private static class Task {
        long caseId;
        String taskId;
        int attempts;
        String receiverFingerprint;
    }

    @lombok.Value public static class PushRecord {
        String taskId;
        String triggerType;
        String caseStatus;
        String status;
        int attempts;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt;
        String lastError;
    }

}
