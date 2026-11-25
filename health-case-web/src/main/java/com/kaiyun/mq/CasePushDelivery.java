package com.kaiyun.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.project.vo.ManagementViews.DeliveryTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
class CasePushDelivery {
    private static final Logger log = LoggerFactory.getLogger(CasePushDelivery.class);
    private final ObjectMapper json;
    private final URI receiver;
    private final String authorization;
    private volatile URI localReceiver;
    private final ProjectService projects;

    CasePushDelivery(ObjectMapper json, @Value("${app.case-push.receiver-url:}") String url,
            @Value("${app.case-push.receiver-token:}") String token, CasePushReceiver local, ProjectService projects) {
        this.json = json;
        this.projects = projects;
        this.receiver = url.trim().isEmpty() ? null : URI.create(url.trim());
        if (receiver != null && (!"https".equalsIgnoreCase(receiver.getScheme()) || receiver.getHost() == null
                || receiver.getUserInfo() != null || receiver.getFragment() != null
                || token.trim().isEmpty() || token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0)) {
            throw new IllegalArgumentException("赔案推送需要 HTTPS 接收地址及有效的接收方令牌");
        }
        this.authorization = receiver == null ? local.authorization() : "Bearer " + token;
    }

    @EventListener
    public void serverStarted(ServletWebServerInitializedEvent event) {
        if (receiver != null || event.getApplicationContext().getServerNamespace() != null) return;
        // 读取实际端口及 context path，支持单端口部署和随机端口启动。
        boolean ssl = event.getApplicationContext().getEnvironment().getProperty("server.ssl.enabled", Boolean.class, false);
        String contextPath = event.getApplicationContext().getServletContext().getContextPath();
        localReceiver = URI.create((ssl ? "https" : "http") + "://127.0.0.1:" + event.getWebServer().getPort()
                + contextPath + CasePushReceiveController.PATH);
        log.info("本机赔案接收接口已就绪 path={} port={}", contextPath + CasePushReceiveController.PATH,
                event.getWebServer().getPort());
    }

    boolean isConfigured() { return receiver != null || localReceiver != null; }

    String routingFingerprint(long caseId) { return fingerprint(projects.deliveryForCase(caseId)); }

    private String fingerprint(DeliveryTarget target) {
        if (target == null) return null;
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest((target.getRouteIdentity()+"|"+target.getEndpoint()).getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();for(byte value:digest)result.append(String.format("%02x",value&255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    int send(String taskId, long caseId, String expectedRoute) throws IOException {
        byte[] body = json.writeValueAsBytes(new CasePushMessage(taskId, Long.toString(caseId)));
        DeliveryTarget company;
        try { company = projects.deliveryForCase(caseId); }
        catch (BusinessException error) { throw new ConfigurationException(error.getMessage()); }
        if (!java.util.Objects.equals(expectedRoute,fingerprint(company))) {
            throw new ConfigurationException("保险公司接收地址或环境已变化，请核对原推送结果后手动重试");
        }
        URI target = company == null ? (receiver == null ? localReceiver : receiver) : URI.create(company.getEndpoint());
        String credential = company == null ? authorization : company.getAuthorization();
        if (target == null) throw new IOException("本机赔案接收接口尚未启动");
        HttpURLConnection connection = (HttpURLConnection) target.toURL().openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            if (!credential.isEmpty()) connection.setRequestProperty("Authorization", credential);
            connection.setRequestProperty("Idempotency-Key", taskId);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) { output.write(body); }
            int status = connection.getResponseCode();
            log.info("赔案接收接口已响应 taskId={} caseId={} httpStatus={}", taskId, caseId, status);
            return status;
        } finally {
            connection.disconnect();
        }
    }

    static class ConfigurationException extends IOException {
        ConfigurationException(String message) { super(message); }
    }

}
