package com.kaiyun.modules.ui.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.modules.ui.dto.UiPageDocument;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.ui.service.UiPageService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/ui/pages")
public class UiPageController {
    private final UiPageService pages;
    private final AdminPermissionService access;

    public UiPageController(UiPageService pages, AdminPermissionService access) {
        this.pages = pages;
        this.access = access;
    }

    @GetMapping
    public Result<List<UiPageService.Summary>> list(HttpServletRequest request) {
        access.requireUiManagement(request);
        return Result.success(pages.list());
    }

    @PostMapping
    @Audited(AuditAction.PAGE_CREATE)
    public Result<UiPageService.Detail> create(@RequestBody PageInput input, HttpServletRequest request) {
        access.requireUiManagement(request);
        UiPageService.Detail result = pages.create(input.getSlug(), input.getDocument());
        PlatformOperationLog.result(request, result.getSlug(), result.getVersion(), result.getPublishedVersion());
        return Result.success(result);
    }

    @GetMapping("/{slug}")
    public Result<UiPageService.Detail> detail(@PathVariable String slug, HttpServletRequest request) {
        access.requireUiManagement(request);
        return Result.success(pages.detail(slug));
    }

    @PutMapping("/{slug}")
    @Audited(AuditAction.PAGE_SAVE)
    public Result<UiPageService.Detail> save(@PathVariable String slug, @RequestBody PageInput input, HttpServletRequest request) {
        access.requireUiManagement(request);
        UiPageService.Detail result = pages.save(slug, input.getVersion(), input.getDocument());
        PlatformOperationLog.result(request, result.getSlug(), result.getVersion(), result.getPublishedVersion());
        return Result.success(result);
    }

    @PostMapping("/{slug}/publish")
    @Audited(AuditAction.PAGE_PUBLISH)
    public Result<UiPageService.Detail> publish(@PathVariable String slug, @RequestBody PageInput input, HttpServletRequest request) {
        access.requireUiManagement(request);
        UiPageService.Detail result = pages.publish(slug, input.getVersion());
        PlatformOperationLog.result(request, result.getSlug(), result.getVersion(), result.getPublishedVersion());
        return Result.success(result);
    }

    @PostMapping("/{slug}/unpublish")
    @Audited(AuditAction.PAGE_UNPUBLISH)
    public Result<UiPageService.Detail> unpublish(@PathVariable String slug, @RequestBody PageInput input, HttpServletRequest request) {
        access.requireUiManagement(request);
        UiPageService.Detail result = pages.unpublish(slug, input.getVersion());
        PlatformOperationLog.result(request, result.getSlug(), result.getVersion(), result.getPublishedVersion());
        return Result.success(result);
    }

    @DeleteMapping("/{slug}")
    @Audited(AuditAction.PAGE_DELETE)
    public Result<Void> delete(@PathVariable String slug, @RequestParam Long version, HttpServletRequest request) {
        access.requireUiManagement(request);
        pages.delete(slug, version);
        PlatformOperationLog.result(request, slug, version, null);
        return Result.success();
    }

    @GetMapping("/{slug}/content")
    public Result<UiPageService.Published> content(@PathVariable String slug) {
        return Result.success(pages.content(slug));
    }

    @Data public static class PageInput {
        private String slug;
        private Long version;
        private UiPageDocument document;
    }
}
