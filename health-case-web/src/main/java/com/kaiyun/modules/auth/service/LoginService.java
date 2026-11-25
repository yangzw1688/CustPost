package com.kaiyun.modules.auth.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.user.service.CustomerUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;

@Service
public class LoginService {
    private static final String PHONE_ATTRIBUTE = LoginService.class.getName() + ".phone";
    private final boolean testCodeEnabled;
    private final String testCode;
    private final CustomerUserService customers;

    public LoginService(@Value("${app.auth.test-code-enabled:false}") boolean testCodeEnabled,
            @Value("${app.auth.test-code:}") String testCode, CustomerUserService customers) {
        this.testCodeEnabled = testCodeEnabled;
        this.testCode = testCode;
        this.customers = customers;
    }

    public void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的11位手机号");
        }
    }

    public String testCode() {
        if (!testCodeEnabled) throw new BusinessException(503, "测试验证码登录已关闭，请联系管理员");
        if (!testCode.matches("[0-9]{6}")) throw new BusinessException(503, "测试验证码尚未配置，请联系管理员");
        return testCode;
    }

    public void validateCredentials(String phone, String code) {
        validatePhone(phone);
        if (!testCode().equals(code)) throw new BusinessException(400, "验证码不正确");
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
