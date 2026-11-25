package com.kaiyun.modules.project.controller;

import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.*;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.project.dto.ManagementInputs.*;
import com.kaiyun.modules.project.dto.ManagementQuery;
import com.kaiyun.modules.project.service.ProjectService;
import com.kaiyun.modules.project.vo.ManagementViews.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import static com.kaiyun.modules.auth.service.AdminModules.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;private final AdminPermissionService permissions;
    public ProjectController(ProjectService service,AdminPermissionService permissions){this.service=service;this.permissions=permissions;}
    @GetMapping public Result<AdminListPage<Project>> page(ManagementQuery query,HttpServletRequest request){permissions.requireModule(request,PROJECT);return Result.success(service.page(query));}
    @GetMapping("/{id}") public Result<Project> detail(@PathVariable long id,HttpServletRequest request){permissions.requireModule(request,PROJECT);return Result.success(service.detail(id));}
    @PostMapping @Audited(AuditAction.PROJECT_SAVE)
    public Result<Project> create(@RequestBody ProjectInput input,HttpServletRequest request){return save(null,input,request);}
    @PutMapping("/{id}") @Audited(AuditAction.PROJECT_SAVE)
    public Result<Project> update(@PathVariable long id,@RequestBody ProjectInput input,HttpServletRequest request){return save(id,input,request);}
    private Result<Project> save(Long id,ProjectInput input,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);Project value=service.save(id,input);
        PlatformOperationLog.result(request,"项目/"+value.getCode(),value.getVersion(),null);
        PlatformOperationLog.details(request,"状态="+value.getStatus()+"；保险公司ID="+value.getInsurerId());return Result.success(value);
    }
    @GetMapping("/{id}/policies") public Result<AdminListPage<LinkedPolicy>> policies(@PathVariable long id,@RequestParam(defaultValue="false") boolean available,ManagementQuery query,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);permissions.requireModule(request,POLICY);return Result.success(service.policies(id,query,available));
    }
    @PutMapping("/{id}/policies") @Audited(AuditAction.PROJECT_ASSIGN)
    public Result<Integer> assign(@PathVariable long id,@RequestBody AssignmentInput input,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);permissions.requireModule(request,POLICY);int count=service.assign(id,input.getPolicyIds());
        PlatformOperationLog.result(request,"项目/"+id,null,null);PlatformOperationLog.details(request,"关联保单 "+count+" 份");return Result.success(count);
    }
    @GetMapping("/{id}/cases") public Result<AdminListPage<LinkedCase>> cases(@PathVariable long id,ManagementQuery query,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);permissions.requireModule(request,CASES);return Result.success(service.cases(id,query));
    }
}
