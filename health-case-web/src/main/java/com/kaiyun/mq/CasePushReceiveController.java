package com.kaiyun.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.common.web.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** RabbitMQ 消费者调用的本机接收接口，使用独立令牌认证。 */
@RestController
class CasePushReceiveController {
    private static final Logger log = LoggerFactory.getLogger(CasePushReceiveController.class);
    static final String PATH = "/api/messaging/case-push/receive";
    private final CasePushReceiver receiver;

    CasePushReceiveController(CasePushReceiver receiver) {
        this.receiver = receiver;
    }

    @PostMapping(PATH)
    public Result<Void> receive(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody JsonNode body) {
        receiver.authenticate(authorization);
        String taskId;
        long caseId;
        try {
            if (body == null || !body.isObject()) throw new IllegalArgumentException();
            taskId = body.path("taskId").asText();
            if (!UUID.fromString(taskId).toString().equals(taskId) || !taskId.equals(idempotencyKey)) {
                throw new IllegalArgumentException();
            }
            caseId = Long.parseLong(body.path("caseId").asText());
            if (caseId <= 0) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "请提供有效的 taskId、caseId 和匹配的 Idempotency-Key");
        }
        receiver.receive(taskId, caseId);
        // service 事务已经提交，再报告持久化结果。
        log.info("本机赔案接收记录已确认 taskId={} caseId={}", taskId, caseId);
        return Result.success();
    }
}
