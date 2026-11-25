package com.kaiyun.modules.ui.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.modules.ui.dto.UiSaveDTO;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.ui.service.UiService;
import com.kaiyun.modules.ui.vo.UiVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ui")
public class UiController {
    private final UiService service;
    private final AdminPermissionService access;
    public UiController(UiService service, AdminPermissionService access) { this.service = service; this.access = access; }

    @GetMapping
    public Result<List<UiVO>> list(@RequestParam(defaultValue = "home_banner") String slot) {
        return Result.success(service.list(slot, false));
    }
    @GetMapping("/manage")
    public Result<List<UiVO>> manage(@RequestParam(defaultValue = "home_banner") String slot, HttpServletRequest request) {
        access.requireUiManagement(request);
        return Result.success(service.list(slot, true));
    }
    @PostMapping("/save")
    @Audited(AuditAction.ASSET_SAVE)
    public Result<Long> save(@ModelAttribute UiSaveDTO input,
            @RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request) throws IOException {
        access.requireUiManagement(request);
        Long id = service.save(input, file);
        PlatformOperationLog.result(request, "ui/" + id, null, null);
        return Result.success(id);
    }
    @DeleteMapping("/{id}")
    @Audited(AuditAction.ASSET_DELETE)
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        access.requireUiManagement(request);
        service.delete(id);
        PlatformOperationLog.result(request, "ui/" + id, null, null);
        return Result.success();
    }
    @PostMapping("/import-defaults")
    @Audited(AuditAction.ASSET_IMPORT)
    public Result<Map<String, Boolean>> importDefaults(HttpServletRequest request) throws IOException {
        access.requireUiManagement(request);
        boolean imported = service.importDefaultImage();
        PlatformOperationLog.result(request, "home-health", null, null);
        return Result.success(Collections.singletonMap("imported", imported));
    }
}
