package com.kaiyun.modules.cases.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.auth.service.LoginService;
import com.kaiyun.modules.cases.dto.CaseProgressUpdate;
import com.kaiyun.modules.cases.dto.CaseReviewInput;
import com.kaiyun.modules.cases.dto.ServiceRecordSaveDTO;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import com.kaiyun.modules.cases.service.ServiceRecordService;
import com.kaiyun.modules.cases.vo.AdminServiceRecordListVO;
import com.kaiyun.modules.cases.vo.CaseAttachmentSummaryVO;
import com.kaiyun.modules.cases.vo.ClaimPushCaseVO;
import com.kaiyun.modules.cases.vo.PaidCaseVO;
import com.kaiyun.modules.cases.vo.ServiceRecordDetailVO;
import com.kaiyun.modules.cases.vo.ServiceRecordListVO;
import com.kaiyun.modules.cases.vo.UserServiceRecordDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import javax.servlet.http.HttpServletRequest;

import static com.kaiyun.modules.auth.service.AdminModules.CASES;

@RestController
@RequestMapping("/api/service-record")
public class ServiceRecordController {
    private final ServiceRecordService serviceRecordService;
    private final LoginService login;
    private final AdminPermissionService admins;

    public ServiceRecordController(ServiceRecordService serviceRecordService, LoginService login, AdminPermissionService admins) {
        this.serviceRecordService = serviceRecordService;
        this.login = login;
        this.admins = admins;
    }

    @PostMapping("/save")
    public Result<Long> save(ServiceRecordSaveDTO dto,
            @RequestParam(value = "files", required = false) MultipartFile[] files, HttpServletRequest request) throws Exception {
        return Result.success(serviceRecordService.save(dto, files, login.currentUser(request).get("phone")));
    }

    @GetMapping("/list")
    public Result<List<ServiceRecordListVO>> mine(HttpServletRequest request) {
        return Result.success(serviceRecordService.serviceRecordlist(login.currentUser(request).get("phone")));
    }

    @GetMapping("/recordList")
    public Result<List<AdminServiceRecordListVO>> list() {
        return Result.success(serviceRecordService.adminList());
    }

    @GetMapping("/page")
    public Result<AdminListPage<AdminServiceRecordListVO>> page(AdminListQuery query) {
        return Result.success(serviceRecordService.adminPage(query));
    }

    @GetMapping("/paid")
    public Result<AdminListPage<PaidCaseVO>> paid(AdminListQuery query, HttpServletRequest request) {
        admins.requireModule(request, CASES);
        return Result.success(serviceRecordService.paidPage(query));
    }

    @GetMapping("/attachment-cases")
    public Result<List<CaseAttachmentSummaryVO>> attachmentCases() {
        return Result.success(serviceRecordService.casesWithAttachments());
    }

    @GetMapping("/claim-push-cases")
    public Result<List<ClaimPushCaseVO>> claimPushCases() {
        return Result.success(serviceRecordService.listClaimPushCases());
    }

    @GetMapping("/{id}/attachments")
    public Result<List<ServiceRecordImage>> attachments(@PathVariable("id") Long id) {
        return Result.success(serviceRecordService.listAttachments(id));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable("id") Long id, HttpServletRequest request) {
        String phone = login.userPhone(request);
        if (phone == null) {
            admins.requireModule(request, CASES);
            return Result.success(serviceRecordService.getDetail(id, null));
        }
        ServiceRecordDetailVO detail = serviceRecordService.getDetail(id, phone);
        UserServiceRecordDetailVO view = new UserServiceRecordDetailVO();
        BeanUtils.copyProperties(detail, view);
        if (view.getName() == null || view.getName().trim().isEmpty()) view.setName(detail.getReporterName());
        return Result.success(view);
    }

    @GetMapping("/manage/{id}")
    public Result<ServiceRecordDetailVO> manageDetail(@PathVariable Long id) {
        return Result.success(serviceRecordService.getDetail(id, null));
    }

    @PutMapping("/manage/{id}/progress")
    @Audited(AuditAction.CASE_PROGRESS)
    public Result<Void> updateProgress(@PathVariable Long id, @RequestBody CaseProgressUpdate input, HttpServletRequest request) {
        admins.requireModule(request, CASES);
        String operator = admins.requireAdmin(request).getPhone();
        PlatformOperationLog.result(request, "案件/" + id, null, null);
        serviceRecordService.updateProgress(id, input, operator);
        PlatformOperationLog.details(request, input.isPaymentConfirmed() ? "确认线下赔付完成" : "案件状态：" + input.getExpectedStatus() + " → " + input.getStatus());
        return Result.success();
    }

    @PutMapping("/manage/{id}/review")
    @Audited(AuditAction.CASE_REVIEW)
    public Result<Void> review(@PathVariable Long id, @RequestBody CaseReviewInput input, HttpServletRequest request) {
        admins.requireModule(request, CASES);
        String operator = admins.requireAdmin(request).getPhone();
        PlatformOperationLog.result(request, "案件/" + id, null, null);
        serviceRecordService.review(id, input, operator);
        PlatformOperationLog.details(request, "APPROVED".equals(input.getDecision()) ? "审核通过，待线下赔付" : "审核驳回");
        return Result.success();
    }
}
