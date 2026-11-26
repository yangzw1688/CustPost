package com.kaiyun.infrastructure.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.auth.service.LoginSmsSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AliyunLoginSmsSender implements LoginSmsSender {
    private static final Logger log = LoggerFactory.getLogger(AliyunLoginSmsSender.class);

    private final Client client;
    private final ObjectMapper json;
    private final String signName;
    private final String templateCode;
    private final String codeParameter;

    public AliyunLoginSmsSender(ObjectMapper json,
            @Value("${app.auth.sms.access-key-id:}") String accessKeyId,
            @Value("${app.auth.sms.access-key-secret:}") String accessKeySecret,
            @Value("${app.auth.sms.sign-name:}") String signName,
            @Value("${app.auth.sms.template-code:}") String templateCode,
            @Value("${app.auth.sms.code-parameter:code}") String codeParameter) throws Exception {
        this.json = json;
        this.signName = signName.trim();
        this.templateCode = templateCode.trim();
        this.codeParameter = codeParameter.trim();
        Config config = new Config().setEndpoint("dysmsapi.aliyuncs.com").setProtocol("HTTPS")
                .setConnectTimeout(3000).setReadTimeout(5000);
        this.client = accessKeyId.trim().isEmpty() || accessKeySecret.trim().isEmpty() ? null
                : new Client(config.setAccessKeyId(accessKeyId.trim()).setAccessKeySecret(accessKeySecret.trim()));
    }

    @Override
    public void checkConfiguration() {
        if (client == null || signName.isEmpty() || !templateCode.matches("SMS_[0-9]+")
                || !codeParameter.matches("[A-Za-z][A-Za-z0-9_]{0,31}")) {
            throw new BusinessException(503, "短信服务尚未配置，请联系管理员");
        }
    }

    @Override
    public void send(String phone, String code) {
        String status;
        try {
            SendSmsRequest request = new SendSmsRequest().setPhoneNumbers(phone).setSignName(signName)
                    .setTemplateCode(templateCode).setTemplateParam(
                            json.writeValueAsString(Collections.singletonMap(codeParameter, code)));
            // A timeout may follow acceptance by the provider. Do not automatically send a second message.
            SendSmsResponse response = client.sendSmsWithOptions(request, new RuntimeOptions().setAutoretry(false));
            status = response == null || response.getBody() == null ? null : response.getBody().getCode();
        } catch (TeaException failure) {
            throw unavailable(failure.getCode());
        } catch (Exception failure) {
            log.warn("Aliyun SMS request failed, type={}", failure.getClass().getSimpleName());
            throw unavailable(null);
        }
        if (!"OK".equals(status)) throw unavailable(status);
    }

    private BusinessException unavailable(String status) {
        String safeStatus = status != null && status.matches("[A-Za-z0-9_.-]{1,80}") ? status : "UNKNOWN";
        log.warn("Aliyun SMS was not accepted, code={}", safeStatus);
        if ("BUSINESS_LIMIT_CONTROL".equals(safeStatus) || "isv.BUSINESS_LIMIT_CONTROL".equals(safeStatus)) {
            return new BusinessException(429, "短信发送过于频繁，请稍后再试");
        }
        return new BusinessException(503, "验证码发送失败，请稍后重试");
    }
}
