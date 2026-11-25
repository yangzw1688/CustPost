package com.kaiyun.infrastructure.logging;

import com.kaiyun.modules.platform.service.OperationLogService;
import com.kaiyun.modules.platform.vo.OperationLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/** 请求完成后记录最终结果；业务事务与日志事务独立，失败请求也可追溯。 */
@Component
public class PlatformOperationLog implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PlatformOperationLog.class);
    private static final String PREFIX = PlatformOperationLog.class.getName();
    private static final String START = PREFIX + ".start";
    private static final String ACTION = PREFIX + ".action";
    private static final String OPERATOR = PREFIX + ".operator";
    private static final String TARGET = PREFIX + ".target";
    private static final String VERSION = PREFIX + ".version";
    private static final String PUBLISHED_VERSION = PREFIX + ".publishedVersion";
    private static final String DETAILS = PREFIX + ".details";
    private static final String ERROR = PREFIX + ".error";
    private final OperationLogService service;

    public PlatformOperationLog(OperationLogService service) { this.service = service; }

    public static void operator(HttpServletRequest request, String phone) {
        request.setAttribute(OPERATOR, safe(phone, 32));
    }

    public static void result(HttpServletRequest request, String target, Long version, Long publishedVersion) {
        request.setAttribute(TARGET, target);
        request.setAttribute(VERSION, version);
        request.setAttribute(PUBLISHED_VERSION, publishedVersion);
    }

    /** 调用方只能传入明确允许记录的摘要，不传整个请求或业务对象。 */
    public static void details(HttpServletRequest request, String details) {
        request.setAttribute(DETAILS, safe(details, 2000));
    }

    public static void failure(HttpServletRequest request, String message) {
        request.setAttribute(ERROR, safe(message, 200));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            Audited audit = ((HandlerMethod) handler).getMethodAnnotation(Audited.class);
            if (audit == null) return true;
            request.setAttribute(START, System.nanoTime());
            request.setAttribute(ACTION, audit.value());
            AuditIpContext.snapshot(request);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        Object start = request.getAttribute(START);
        if (!(start instanceof Long)) return;
        // 防止同一请求在异步或错误分派时重复记录。
        request.removeAttribute(START);
        AuditAction action = (AuditAction) request.getAttribute(ACTION);
        OperationLogVO entry = new OperationLogVO();
        entry.setModule(action.getModule());
        entry.setModuleName(action.getModuleName());
        entry.setAction(action.name());
        entry.setActionName(action.getLabel());
        entry.setOperator(value(request, OPERATOR, "-", 32));
        entry.setRequestPath(safe(request.getRequestURI(), 240));
        entry.setTarget(value(request, TARGET, entry.getRequestPath(), 160));
        entry.setVersion((Long) request.getAttribute(VERSION));
        entry.setPublishedVersion((Long) request.getAttribute(PUBLISHED_VERSION));
        entry.setSuccessful(response.getStatus() < 400 && exception == null);
        entry.setHttpStatus(response.getStatus());
        entry.setDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - (Long) start));
        entry.setRequestMethod(safe(request.getMethod(), 8));
        // 记录直接连接地址，不信任客户端可自行提供的转发头。
        entry.setClientIp(safe(request.getRemoteAddr(), 64));
        AuditIpContext.apply(request, entry, action == AuditAction.ADMIN_LOGIN);
        entry.setDetails(value(request, DETAILS, "", 2000));
        entry.setErrorMessage(entry.isSuccessful() ? "" : value(request, ERROR, "请求未完成，请结合 HTTP 状态排查", 200));
        String message = "平台操作{} action={} operator={} publicIp={} ipSource={} clientIp={} target={} version={} publishedVersion={} method={} path={} status={} durationMs={}";
        Object[] values = {entry.isSuccessful() ? "成功" : "失败", entry.getAction(), entry.getOperator(), entry.getPublicIp(), entry.getIpSource(), entry.getClientIp(), entry.getTarget(),
                entry.getVersion(), entry.getPublishedVersion(), entry.getRequestMethod(), entry.getRequestPath(),
                entry.getHttpStatus(), entry.getDurationMs()};
        if (!entry.isSuccessful()) log.warn(message, values);
        else log.info(message, values);
        try {
            service.append(entry);
        } catch (RuntimeException error) {
            // 此时业务结果已经确定，日志写入异常不能改变已提交的业务响应。
            log.error("平台操作日志落库失败 action={} status={}，请检查日志表和数据库连接", action, response.getStatus(), error);
        }
    }

    private static String value(HttpServletRequest request, String name, String fallback, int max) {
        Object value = request.getAttribute(name);
        return safe(value == null ? fallback : value.toString(), max);
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        String text = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ')
                .replaceAll("(?<!\\d)(\\d{3})\\d{4}(\\d{4})(?!\\d)", "$1****$2");
        return text.length() > max ? text.substring(0, max) : text;
    }
}
