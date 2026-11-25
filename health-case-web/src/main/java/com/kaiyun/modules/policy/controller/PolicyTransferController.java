package com.kaiyun.modules.policy.controller;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import com.kaiyun.infrastructure.logging.Audited;
import com.kaiyun.infrastructure.logging.AuditAction;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.policy.service.PolicyExportService;
import com.kaiyun.modules.policy.service.PolicyImportService;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.project.vo.ManagementViews.Project;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/policy")
public class PolicyTransferController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AdminPermissionService permissions;
    private final PolicyImportService imports;
    private final PolicyExportService exports;
    private final ProjectService projects;

    public PolicyTransferController(AdminPermissionService permissions, PolicyImportService imports, PolicyExportService exports, ProjectService projects) {
        this.permissions = permissions;
        this.imports = imports;
        this.exports = exports;
        this.projects = projects;
    }

    @PostMapping("/import/columns")
    public Result<PolicyImportService.Columns> columns(@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1") int sheetNumber, @RequestParam(defaultValue = "1") int headerRow,
            HttpServletRequest request) {
        permissions.requirePolicyTransfer(request);
        return Result.success(imports.columns(file, sheetNumber, headerRow));
    }

    @PostMapping("/import/preview")
    public Result<PolicyImportService.Preview> preview(@RequestParam("file") MultipartFile file,
            @RequestParam int sheetNumber, @RequestParam int headerRow, @RequestParam String mapping,
            HttpServletRequest request) {
        permissions.requirePolicyTransfer(request);
        return Result.success(imports.preview(file, sheetNumber, headerRow, mapping));
    }

    @PostMapping("/import")
    @Audited(AuditAction.POLICY_IMPORT)
    public Result<Integer> save(@RequestParam("file") MultipartFile file,
            @RequestParam int sheetNumber, @RequestParam int headerRow, @RequestParam String mapping,
            @RequestParam(required = false) Long projectId,
            HttpServletRequest request) {
        String operator = permissions.requirePolicyTransfer(request);
        int count = imports.save(file, sheetNumber, headerRow, mapping, operator, projectId);
        PlatformOperationLog.result(request, "保单批量导入", null, null);
        PlatformOperationLog.details(request, "成功导入 " + count + " 条保单");
        return Result.success(count);
    }

    @GetMapping("/project-options")
    public Result<List<Project>> projectOptions(HttpServletRequest request) {
        permissions.requirePolicyTransfer(request);
        return Result.success(projects.options());
    }

    @GetMapping("/export")
    @Audited(AuditAction.POLICY_EXPORT)
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "") String policyNo, @RequestParam(defaultValue = "") String insuredName,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) throws IOException {
        permissions.requirePolicyTransfer(request);
        byte[] file = exports.export(policyNo, insuredName, status, from, to);
        PlatformOperationLog.result(request, "保单导出", null, null);
        PlatformOperationLog.details(request, "生成脱敏 Excel 文件，文件大小 " + file.length + " 字节");
        String filename = "policies-" + FILE_TIME.format(LocalDateTime.now()) + ".xlsx";
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("attachment").filename(filename).build().toString())
                .contentLength(file.length).body(file);
    }

}
