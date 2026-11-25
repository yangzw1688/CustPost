package com.kaiyun.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.case-push.transport", havingValue = "rabbitmq", matchIfMissing = true)
class RabbitCasePushListener {
    private static final Logger log = LoggerFactory.getLogger(RabbitCasePushListener.class);
    private final ObjectMapper json;
    private final CasePushQueue queue;

    RabbitCasePushListener(ObjectMapper json, CasePushQueue queue) {
        this.json = json;
        this.queue = queue;
    }

    @RabbitListener(id = RabbitCasePushTransport.LISTENER_ID, queues = "${app.case-push.rabbit-queue:custpost.case-push}",
            autoStartup = "false", ackMode = "MANUAL")
    public void consume(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        long caseId;
        String taskId;
        try {
            JsonNode body = json.readTree(message.getBody());
            if (body == null || !body.isObject()) throw new IllegalArgumentException("Invalid message body");
            caseId = Long.parseLong(body.path("caseId").asText());
            taskId = UUID.fromString(body.path("taskId").asText()).toString();
            if (caseId <= 0) throw new IllegalArgumentException("Invalid case ID");
        } catch (IOException | IllegalArgumentException invalid) {
            log.warn("拒绝格式无效的赔案队列消息");
            channel.basicReject(tag, false);
            return;
        }
        log.info("RabbitMQ 收到赔案消息 taskId={} caseId={} redelivered={}",
                taskId, caseId, message.getMessageProperties().isRedelivered());
        try {
            queue.consume(caseId, taskId);
        } catch (RuntimeException error) {
            log.warn("赔案任务消费中断，由数据库重新调度 taskId={} type={}", taskId, error.getClass().getSimpleName());
            // 消息只负责唤醒，未处理的任务保存在 MySQL，避免立即重投造成忙循环。
            channel.basicNack(tag, false, false);
            return;
        }
        channel.basicAck(tag, false);
        log.info("RabbitMQ 消息已 ACK taskId={} caseId={}", taskId, caseId);
    }
}
