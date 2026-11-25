package com.kaiyun.modules.auth.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.auth.service.AdminModules;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/admin/permissions")
public class AdminPermissionController {
    private final AdminPermissionService permissions;

    public AdminPermissionController(AdminPermissionService permissions) {
        this.permissions = permissions;
    }

    @GetMapping
    public Result<List<AdminPermissionService.Grant>> list(HttpServletRequest request) {
        return Result.success(permissions.list(request));
    }

    @PutMapping("/{phone}")
    @Audited(AuditAction.ADMIN_GRANT)
    public Result<Void> grant(@PathVariable String phone, @RequestBody GrantRequest input, HttpServletRequest request) {
        permissions.grant(request, phone, input.getModules(), input.isPolicyTransfer(), input.isUiManagement(),
                input.getAdminPassword(), input.isSuperAdmin(), input.isCreateOnly());
        PlatformOperationLog.result(request, "管理员/" + phone, null, null);
        AdminModules modules = input.isSuperAdmin() ? AdminModules.ALL : input.getModules();
        PlatformOperationLog.details(request, (input.isCreateOnly() ? "新增管理员" : "保存账号权限")
                + "；超级管理员=" + input.isSuperAdmin()
                + "；数据统计=" + modules.isDashboard() + "；案件=" + modules.isCases()
                + "；保单=" + modules.isPolicy() + "；项目=" + modules.isProject()
                + "；平台=" + modules.isPlatform() + "；用户=" + modules.isUsers()
                + "；保单导入导出=" + (input.isSuperAdmin() || modules.isPolicy() && input.isPolicyTransfer())
                + "；用户端页面=" + (input.isSuperAdmin() || modules.isPlatform() && input.isUiManagement())
                + "；设置密码=" + (input.getAdminPassword() != null && !input.getAdminPassword().isEmpty()));
        return Result.success();
    }

    @DeleteMapping("/{phone}")
    @Audited(AuditAction.ADMIN_REVOKE)
    public Result<Void> revoke(@PathVariable String phone, HttpServletRequest request) {
        permissions.revoke(request, phone);
        PlatformOperationLog.result(request, "管理员/" + phone, null, null);
        return Result.success();
    }

    @Data
    public static class GrantRequest {
        private AdminModules modules;
        private boolean policyTransfer;
        private boolean uiManagement;
        private String adminPassword;
        private boolean superAdmin;
        private boolean createOnly;
    }
}
