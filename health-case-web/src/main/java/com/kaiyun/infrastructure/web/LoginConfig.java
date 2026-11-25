package com.kaiyun.infrastructure.web;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.auth.service.LoginService;
import com.kaiyun.modules.cases.mapper.ServiceRecordImageMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.kaiyun.modules.auth.service.AdminModules.CASES;
import static com.kaiyun.modules.auth.service.AdminModules.POLICY;

/** 页面资源公开加载，用户与管理员接口分别校验登录身份。 */
@Configuration
public class LoginConfig implements WebMvcConfigurer {
    private final UrlPathHelper paths = new UrlPathHelper();
    private final LoginService loginService;
    private final AdminPermissionService admins;
    private final ServiceRecordImageMapper images;
    private final PlatformOperationLog operations;

    public LoginConfig(LoginService loginService, AdminPermissionService admins, ServiceRecordImageMapper images,
            PlatformOperationLog operations) {
        this.loginService = loginService;
        this.admins = admins;
        this.images = images;
        this.operations = operations;
    }

    private HandlerInterceptor interceptor(boolean adminOnly, boolean shared) {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("OPTIONS".equals(request.getMethod())) return true;
                response.setHeader("Cache-Control", "no-store");
                try {
                    if (adminOnly || (shared && loginService.userPhone(request) == null)) {
                        PlatformOperationLog.operator(request, admins.requireAdmin(request).getPhone());
                    } else {
                        loginService.currentUser(request);
                    }
                    if (handler instanceof ResourceHttpRequestHandler) requireAttachmentAccess(request);
                } catch (BusinessException exception) {
                    // 历史附件由静态资源处理器提供，需在此返回未登录状态。
                    if (!(handler instanceof ResourceHttpRequestHandler)) throw exception;
                    response.setStatus(exception.getStatus());
                    return false;
                }
                return true;
            }
        };
    }

    private void requireAttachmentAccess(HttpServletRequest request) {
        String path = paths.getPathWithinApplication(request);
        if (!path.startsWith("/upload/case/")) return;
        String phone = loginService.userPhone(request);
        if (phone == null || !images.findOwnedUrls(phone, path).contains(path)) admins.requireModule(request, CASES);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 在鉴权前进入，确保被拒绝的审计操作同样记录；普通读取接口无审计注解。
        registry.addInterceptor(operations).addPathPatterns("/api/**");
        registry.addInterceptor(interceptor(false, false))
                .addPathPatterns("/api/auth/me", "/api/policy/mine", "/api/service-record/list", "/api/service-record/save");
        registry.addInterceptor(interceptor(false, true))
                .addPathPatterns("/api/service-record/{id:[0-9]+}", "/api/ui", "/api/ui/pages/{slug}/content", "/upload/case/**");
        registry.addInterceptor(interceptor(true, false)).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/admin/auth/**", "/api/policy/mine", "/api/service-record/list",
                        "/api/service-record/save", "/api/service-record/{id:[0-9]+}", "/api/ui",
                        "/api/messaging/case-push/receive", "/api/ui/pages/{slug}/content");
        registry.addInterceptor(moduleInterceptor(CASES)).addPathPatterns("/api/service-record/**")
                .excludePathPatterns("/api/service-record/list", "/api/service-record/save", "/api/service-record/{id:[0-9]+}");
        registry.addInterceptor(moduleInterceptor(POLICY)).addPathPatterns("/api/policy/**")
                .excludePathPatterns("/api/policy/mine");
    }

    private HandlerInterceptor moduleInterceptor(String module) {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!"OPTIONS".equals(request.getMethod())) admins.requireModule(request, module);
                return true;
            }
        };
    }
}
