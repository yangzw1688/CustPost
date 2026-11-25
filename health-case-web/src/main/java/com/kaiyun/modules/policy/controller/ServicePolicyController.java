package com.kaiyun.modules.policy.controller;

import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.common.web.Result;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import com.kaiyun.modules.auth.service.LoginService;
import com.kaiyun.modules.policy.service.ServicePolicyService;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.policy.vo.MyPolicyVO;
import com.kaiyun.modules.policy.vo.PolicyOperationLogVO;
import com.kaiyun.modules.policy.vo.ServicePolicyDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/policy")
public class ServicePolicyController {

    private final ServicePolicyService servicePolicyService;
    private final LoginService loginService;
    private final ProjectService projects;

    public ServicePolicyController(
            ServicePolicyService servicePolicyService, LoginService loginService, ProjectService projects) {
        this.servicePolicyService = servicePolicyService;
        this.loginService = loginService;
        this.projects = projects;
    }

    /**
     * 保单列表
     */
    @GetMapping("/policyList")
    public Result<List<ServicePolicy>> list() {

        return Result.success(
                servicePolicyService.list()
        );
    }

    @GetMapping("/mine")
    public Result<List<MyPolicyVO>> mine(HttpServletRequest request) {
        String phone = loginService.currentUser(request).get("phone");
        return Result.success(servicePolicyService.listForPhone(phone));
    }

    @GetMapping("/page")
    public Result<AdminListPage<ServicePolicy>> page(AdminListQuery query) {
        return Result.success(servicePolicyService.page(query));
    }

    @GetMapping("/operationLogList")
    public Result<List<PolicyOperationLogVO>> operationLogList() {
        return Result.success(servicePolicyService.operationLogList());
    }

    /**
     * 保单详情
     */
    @GetMapping("/{id}")
    public Result<ServicePolicyDetailVO> detail(
            @PathVariable Long id) {

        ServicePolicy policy =
                servicePolicyService.detail(id);

        List<ServicePolicyOperationLog> operationLogs =
                servicePolicyService.operationLogs(id);

        ServicePolicyDetailVO vo =
                new ServicePolicyDetailVO();

        vo.setPolicy(policy);
        if (policy.getProjectId() != null) {
            com.kaiyun.modules.project.vo.ManagementViews.Project project = projects.detail(policy.getProjectId());
            vo.setProjectName(project.getName());
            vo.setInsurerName(project.getInsurerName());
        }
        if (policy.getProjectId() != null) {
            com.kaiyun.modules.project.vo.ManagementViews.Project project = projects.detail(policy.getProjectId());
            vo.setProjectName(project.getName());
            vo.setInsurerName(project.getInsurerName());
        }
        vo.setOperationLogs(operationLogs);

        return Result.success(vo);
    }
}
