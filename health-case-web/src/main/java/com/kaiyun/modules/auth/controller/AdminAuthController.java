package com.kaiyun.modules.auth.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.infrastructure.logging.AuditIpContext;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.auth.service.AdminPermissionService.Grant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminPermissionService permissions;

    public AdminAuthController(AdminPermissionService permissions) {
        this.permissions = permissions;
    }

    @PostMapping("/login")
    @Audited(AuditAction.ADMIN_LOGIN)
    public Result<Grant> login(@RequestBody LoginRequest input, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        AuditIpContext.report(request, input.getReportedPublicIp());
        Grant admin = permissions.login(input.getPhone(), input.getPassword(), request);
        AuditIpContext.rememberLogin(request);
        PlatformOperationLog.operator(request, admin.getPhone());
        PlatformOperationLog.result(request, "管理端登录", null, null);
        return Result.success(admin);
    }

    @GetMapping("/me")
    public Result<Grant> me(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Result.success(permissions.requireAdmin(request));
    }

    @PostMapping("/logout")
    @Audited(AuditAction.ADMIN_LOGOUT)
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        // 退出保持幂等；会话仍有效时，在清除身份前记录操作人。
        try { PlatformOperationLog.operator(request, permissions.requireAdmin(request).getPhone()); }
        catch (BusinessException error) { if (error.getStatus() != 401) throw error; }
        PlatformOperationLog.result(request, "管理端退出", null, null);
        permissions.logout(request);
        AuditIpContext.clearLogin(request);
        return Result.success();
    }

    @PutMapping("/password")
    @Audited(AuditAction.ADMIN_PASSWORD)
    public Result<Void> changePassword(@RequestBody PasswordChange input, HttpServletRequest request) {
        Grant admin = permissions.requireAdmin(request);
        PlatformOperationLog.operator(request, admin.getPhone());
        PlatformOperationLog.result(request, "管理员/" + admin.getPhone(), null, null);
        permissions.changeSuperAdminPassword(request, input.getCurrentPassword(), input.getNewPassword());
        AuditIpContext.clearLogin(request);
        return Result.success();
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String phone;
        private String password;
        private String reportedPublicIp;
    }

    @Getter
    @Setter
    public static class PasswordChange {
        private String currentPassword;
        private String newPassword;
    }
}
