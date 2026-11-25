package com.kaiyun.mq;

import com.kaiyun.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** 本系统的赔案接收方，以任务 ID 记录已受理消息，重复投递复用同一记录。 */
@Service
class CasePushReceiver {
    private final JdbcTemplate jdbc;
    private final String authorization;

    CasePushReceiver(JdbcTemplate jdbc, @Value("${app.case-push.receiver-token:}") String token) {
        this.jdbc = jdbc;
        if (token.trim().isEmpty()) {
            // 本机回调使用进程级随机令牌，启动后由发送端和接收端共享。
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        }
        if (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("赔案接收令牌不能包含换行符");
        }
        this.authorization = "Bearer " + token;
    }

    String authorization() {
        return authorization;
    }

    void authenticate(String supplied) {
        if (supplied == null || !MessageDigest.isEqual(authorization.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(401, "赔案接收认证失败");
        }
    }

    @Transactional
    public void receive(String taskId, long caseId) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM case_push_task task "
                        + "JOIN service_record record ON record.id = task.case_id "
                        + "WHERE task.task_id = ? AND task.case_id = ?", Integer.class, taskId, caseId);
        if (exists == null || exists == 0) throw new BusinessException(404, "赔案或推送任务不存在");
        // 唯一键和事务保证并发重投只产生一条接收记录，首次接收时间保持不变。
        jdbc.update("INSERT INTO case_push_receipt (task_id, case_id, received_at) "
                        + "VALUES (?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE task_id = VALUES(task_id)",
                taskId, caseId);
    }
}
