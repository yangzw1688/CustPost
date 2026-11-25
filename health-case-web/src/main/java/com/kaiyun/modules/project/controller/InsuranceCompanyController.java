package com.kaiyun.modules.project.controller;

import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.*;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.project.dto.ManagementInputs.*;
import com.kaiyun.modules.project.dto.ManagementQuery;
import com.kaiyun.modules.project.service.InsuranceCompanyService;
import com.kaiyun.modules.project.vo.ManagementViews.*;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import static com.kaiyun.modules.auth.service.AdminModules.*;

@RestController
@RequestMapping("/api/insurers")
public class InsuranceCompanyController {
    private final InsuranceCompanyService service;
    private final AdminPermissionService permissions;
    public InsuranceCompanyController(InsuranceCompanyService service,AdminPermissionService permissions){this.service=service;this.permissions=permissions;}
    @GetMapping public Result<AdminListPage<Company>> page(ManagementQuery query,HttpServletRequest request){permissions.requireModule(request,PROJECT);return Result.success(service.page(query));}
    @GetMapping("/{id}") public Result<Company> detail(@PathVariable long id,HttpServletRequest request){permissions.requireModule(request,PROJECT);return Result.success(service.detail(id));}
    @PostMapping @Audited(AuditAction.INSURER_SAVE)
    public Result<Company> create(@RequestBody CompanyInput input,HttpServletRequest request){return save(null,input,request);}
    @PutMapping("/{id}") @Audited(AuditAction.INSURER_SAVE)
    public Result<Company> update(@PathVariable long id,@RequestBody CompanyInput input,HttpServletRequest request){return save(id,input,request);}
    private Result<Company> save(Long id,CompanyInput input,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);Company value=service.save(id,input);
        PlatformOperationLog.result(request,"保险公司/"+value.getId()+"/资料",value.getVersion(),null);
        PlatformOperationLog.details(request,"启用="+value.isEnabled()+"；生效环境="+value.getActiveEnvironment());
        return Result.success(value);
    }
    @GetMapping("/{id}/connections") public Result<List<Connection>> connections(@PathVariable long id,HttpServletRequest request){permissions.requireModule(request,PROJECT);return Result.success(service.connections(id));}
    @PutMapping("/{id}/connections/{environment}") @Audited(AuditAction.INSURER_CONNECTION)
    public Result<Connection> connection(@PathVariable long id,@PathVariable String environment,@RequestBody ConnectionInput input,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);Connection value=service.saveConnection(id,environment,input);
        PlatformOperationLog.result(request,"保险公司/"+id+"/"+environment,value.getVersion(),null);
        PlatformOperationLog.details(request,"对接状态="+value.getStatus()+"；鉴权方式="+value.getAuthType()+"；已配置令牌="+value.isTokenConfigured());
        return Result.success(value);
    }
    @GetMapping("/{id}/push-records") public Result<AdminListPage<PushRecord>> pushes(@PathVariable long id,ManagementQuery query,HttpServletRequest request){
        permissions.requireModule(request,PROJECT);permissions.requireModule(request,CASES);return Result.success(service.pushRecords(id,query));
    }
}
