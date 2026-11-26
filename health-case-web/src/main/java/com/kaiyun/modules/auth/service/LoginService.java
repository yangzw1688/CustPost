package com.kaiyun.modules.auth.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.user.service.CustomerUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class LoginService {
    private static final String PHONE_ATTRIBUTE = LoginService.class.getName() + ".phone";
    private final boolean testCodeEnabled;
    private final String testCode;
    private final CustomerUserService customers;
    private final SmsVerificationService sms;

    public LoginService(@Value("${app.auth.test-code-enabled:false}") boolean testCodeEnabled,
            @Value("${app.auth.test-code:}") String testCode, CustomerUserService customers, SmsVerificationService sms) {
        this.testCodeEnabled = testCodeEnabled;
        this.testCode = testCode;
        this.customers = customers;
        this.sms = sms;
    }

    public void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的11位手机号");
        }
    }

    private String testCode() {
        if (!testCode.matches("[0-9]{6}")) throw new BusinessException(503, "测试验证码尚未配置，请联系管理员");
        return testCode;
    }

    public Map<String, Object> sendCode(String phone, String sourceAddress) {
        validatePhone(phone);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("testMode", testCodeEnabled);
        if (testCodeEnabled) {
            result.put("message", "测试验证码为" + testCode() + "，无需接收短信");
        } else {
            result.put("cooldownSeconds", sms.send(phone, sourceAddress));
            result.put("message", "验证码已发送，请查看短信");
        }
        return result;
    }

    public void validateCredentials(String phone, String code) {
        validatePhone(phone);
        if (code == null || !code.matches("[0-9]{6}")) {
            throw new BusinessException(400, "请输入六位验证码");
        }
        if (testCodeEnabled) {
            if (!testCode().equals(code)) throw new BusinessException(400, "验证码不正确");
        } else {
            sms.verify(phone, code);
        }
    }

    public void login(String phone, HttpServletRequest request) {
        customers.recordLogin(phone);
        // 登录成功后轮换会话 ID，避免复用登录前的会话标识。
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(PHONE_ATTRIBUTE, phone);
    }

    public Map<String, String> currentUser(HttpServletRequest request) {
        String phone = userPhone(request);
        if (phone == null) throw new BusinessException(401, "请先登录或重新登录");
        return Collections.singletonMap("phone", phone);
    }

    public String userPhone(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(PHONE_ATTRIBUTE);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(PHONE_ATTRIBUTE);
    }
}
