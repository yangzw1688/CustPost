package com.kaiyun.modules.cases.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.mq.CasePushQueue;
import com.kaiyun.modules.cases.service.ServiceRecordService;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.cases.vo.ClaimPushCaseVO;
import lombok.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-record/claim-push")
public class CasePushController {
    private final CasePushQueue queue;
    private final ServiceRecordService records;
    private final ProjectService projects;

    public CasePushController(CasePushQueue queue, ServiceRecordService records, ProjectService projects) {
        this.queue = queue;
        this.records = records;
        this.projects = projects;
    }

    @GetMapping
    public Result<Overview> overview() {
        return Result.success(new Overview(queue.isConfigured(), records.listClaimPushCases()));
    }

    @PostMapping
    @Audited(AuditAction.CASE_PUSH)
    public Result<Integer> enqueue(@RequestBody List<Long> ids, HttpServletRequest request) {
        requireReceiver();
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(400, "每次请选择 1 至 100 个有效案件");
        }
        List<Long> existing = records.existingCaseIds(ids);
        projects.validatePush(existing);
        int count = queue.enqueue(existing);
        PlatformOperationLog.result(request, "赔案批量推送", null, null);
        PlatformOperationLog.details(request, "新增任务 " + count + " 条；案件ID="
                + ids.stream().distinct().map(String::valueOf).collect(Collectors.joining(",")));
        return Result.success(count);
    }

    @PostMapping("/{caseId}/retry")
    @Audited(AuditAction.CASE_RETRY)
    public Result<Void> retry(@PathVariable long caseId, HttpServletRequest request) {
        return requeue(caseId, null, request);
    }

    @GetMapping("/{caseId}/tasks")
    public Result<List<CasePushQueue.PushRecord>> history(@PathVariable long caseId) {
        return Result.success(queue.history(caseId));
    }

    @PostMapping("/{caseId}/tasks/{taskId}/retry")
    @Audited(AuditAction.CASE_RETRY)
    public Result<Void> retryTask(@PathVariable long caseId, @PathVariable String taskId, HttpServletRequest request) {
        try {
            if (!java.util.UUID.fromString(taskId).toString().equals(taskId)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException error) { throw new BusinessException(400, "推送任务编号不正确"); }
        return requeue(caseId, taskId, request);
    }

    private Result<Void> requeue(long caseId, String taskId, HttpServletRequest request) {
        requireReceiver();
        PlatformOperationLog.result(request, "案件/" + caseId, null, null);
        if (!queue.retry(caseId, taskId)) throw new BusinessException(409, "只有失败的任务可以重试");
        if (taskId != null) PlatformOperationLog.details(request, "推送任务 " + taskId);
        return Result.success();
    }

    private void requireReceiver() {
        if (!queue.isConfigured()) throw new BusinessException(503, "请先配置赔案推送接收方和所选队列连接");
    }

    @Value
    public static class Overview {
        boolean configured;
        List<ClaimPushCaseVO> rows;
    }
}
