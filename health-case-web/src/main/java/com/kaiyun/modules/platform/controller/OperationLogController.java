package com.kaiyun.modules.platform.controller;

import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.platform.dto.OperationLogQuery;
import com.kaiyun.modules.platform.service.OperationLogService;
import com.kaiyun.modules.platform.vo.OperationLogVO;
import lombok.Value;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static com.kaiyun.modules.auth.service.AdminModules.PLATFORM;

/** 管理端仅提供查询接口；操作日志不开放新增、编辑、删除和清空接口。 */
@RestController
@RequestMapping("/api/platform/operation-logs")
public class OperationLogController {
    private final OperationLogService service;
    private final AdminPermissionService permissions;

    public OperationLogController(OperationLogService service, AdminPermissionService permissions) {
        this.service = service;
        this.permissions = permissions;
    }

    @GetMapping
    public Result<AdminListPage<OperationLogVO>> page(OperationLogQuery query, HttpServletRequest request) {
        permissions.requireModule(request, PLATFORM);
        return Result.success(service.page(query));
    }

    @GetMapping("/options")
    public Result<List<ActionOption>> options(HttpServletRequest request) {
        permissions.requireModule(request, PLATFORM);
        return Result.success(Arrays.stream(AuditAction.values()).map(value -> new ActionOption(value.getModule(),
                value.getModuleName(), value.name(), value.getLabel())).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public Result<OperationLogVO> detail(@PathVariable long id, HttpServletRequest request) {
        permissions.requireModule(request, PLATFORM);
        return Result.success(service.detail(id));
    }

    @Value
    public static class ActionOption {
        String module;
        String moduleName;
        String action;
        String actionName;
    }
}
