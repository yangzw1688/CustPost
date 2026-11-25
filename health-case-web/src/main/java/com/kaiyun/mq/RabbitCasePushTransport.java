package com.kaiyun.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(name = "app.case-push.transport", havingValue = "rabbitmq", matchIfMissing = true)
class RabbitCasePushTransport {
    static final String LISTENER_ID = "casePushRabbitListener";
    private static final Logger log = LoggerFactory.getLogger(RabbitCasePushTransport.class);
    private final RabbitTemplate template;
    private final ObjectMapper json;
    private final RabbitProperties properties;
    private final RabbitListenerEndpointRegistry listeners;
    private final String queue;

    RabbitCasePushTransport(RabbitTemplate template, ObjectMapper json, RabbitProperties properties,
            RabbitListenerEndpointRegistry listeners, @Value("${app.case-push.rabbit-queue:custpost.case-push}") String queue) {
        this.template = template;
        this.json = json;
        this.properties = properties;
        this.listeners = listeners;
        this.queue = queue;
    }

    @Bean
    Queue casePushRabbitQueue() {
        return new Queue(queue, true);
    }

    boolean isConfigured() {
        return StringUtils.hasText(properties.getHost()) && StringUtils.hasText(properties.getUsername())
                && StringUtils.hasText(properties.getPassword()) && StringUtils.hasText(queue);
    }

    void startListener() {
        MessageListenerContainer listener = listeners.getListenerContainer(LISTENER_ID);
        if (listener != null && !listener.isRunning()) {
            listener.start();
            log.info("RabbitMQ 消费监听已启动 queue={}", queue);
        }
    }

    boolean publish(long caseId, String taskId) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        try {
            Message message = MessageBuilder.withBody(json.writeValueAsBytes(new CasePushMessage(taskId, Long.toString(caseId))))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(taskId)
                    .build();
            template.send("", queue, message, correlation);
            boolean confirmed = correlation.getFuture().get(5, TimeUnit.SECONDS).isAck();
            if (correlation.getReturnedMessage() != null) {
                log.warn("赔案任务未路由到队列 taskId={} queue={}", taskId, queue);
                return false;
            }
            if (confirmed) {
                log.info("RabbitMQ 消息发布已确认 taskId={} caseId={} queue={}", taskId, caseId, queue);
            } else {
                log.warn("RabbitMQ 拒绝赔案任务发布 taskId={}", taskId);
            }
            return confirmed;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            log.warn("赔案任务发布未确认 taskId={} type={}", taskId, error.getClass().getSimpleName());
        }
        return false;
    }
}
