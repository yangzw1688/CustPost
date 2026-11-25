package com.kaiyun.infrastructure.logging;

import com.kaiyun.modules.platform.vo.OperationLogVO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** 浏览器报告的公网 IP 只作审计参考，不作为身份验证或访问权限的依据。 */
public final class AuditIpContext {
    private static final String REPORTED = AuditIpContext.class.getName() + ".reported";
    private static final String LOGIN_IP = AuditIpContext.class.getName() + ".loginIp";
    private static final String SESSION_SNAPSHOT = AuditIpContext.class.getName() + ".snapshot";
    private AuditIpContext() { }

    public static void report(HttpServletRequest request, String value) {
        request.setAttribute(REPORTED, PublicIpAddress.parse(value));
    }

    public static void rememberLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.setAttribute(LOGIN_IP, request.getAttribute(REPORTED));
    }

    public static void snapshot(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) request.setAttribute(SESSION_SNAPSHOT, session.getAttribute(LOGIN_IP));
    }

    public static void clearLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(LOGIN_IP);
    }

    public static void apply(HttpServletRequest request, OperationLogVO entry, boolean login) {
        String direct = PublicIpAddress.parse(request.getRemoteAddr());
        String reported = PublicIpAddress.parse((String) request.getAttribute(REPORTED));
        // 登录失败不能借用旧会话的 IP；后续操作引用登录结果时明确标注来源。
        String priorLogin = login || "-".equals(entry.getOperator()) ? null
                : PublicIpAddress.parse((String) request.getAttribute(SESSION_SNAPSHOT));
        entry.setPublicIp(direct != null ? direct : reported != null ? reported : priorLogin != null ? priorLogin : "");
        entry.setIpSource(direct != null ? "REQUEST" : reported != null ? "BROWSER" : priorLogin != null ? "BROWSER_LOGIN" : "UNAVAILABLE");
    }
}
