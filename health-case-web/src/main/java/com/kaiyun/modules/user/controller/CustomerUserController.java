package com.kaiyun.modules.user.controller;

import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.common.web.Result;
import com.kaiyun.modules.auth.service.AdminPermissionService;
import com.kaiyun.modules.user.dto.UserQuery;
import com.kaiyun.modules.user.service.CustomerUserService;
import com.kaiyun.modules.user.vo.CustomerUserVO;
import com.kaiyun.modules.user.vo.UserCaseVO;
import com.kaiyun.modules.user.vo.UserPolicyVO;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import static com.kaiyun.modules.auth.service.AdminModules.*;

@RestController
@RequestMapping("/api/users")
public class CustomerUserController {
    private final CustomerUserService service;
    private final AdminPermissionService permissions;

    public CustomerUserController(CustomerUserService service, AdminPermissionService permissions) {
        this.service = service; this.permissions = permissions;
    }

    @GetMapping
    public Result<AdminListPage<CustomerUserVO>> page(UserQuery query, HttpServletRequest request) {
        permissions.requireModule(request, USERS);
        return Result.success(service.page(query));
    }

    @GetMapping("/{phone}")
    public Result<CustomerUserVO> detail(@PathVariable String phone, HttpServletRequest request) {
        permissions.requireModule(request, USERS);
        return Result.success(service.detail(phone));
    }

    @GetMapping("/{phone}/policies")
    public Result<AdminListPage<UserPolicyVO>> policies(@PathVariable String phone, UserQuery query, HttpServletRequest request) {
        permissions.requireModule(request, USERS);
        permissions.requireModule(request, POLICY);
        return Result.success(service.policies(phone, query));
    }

    @GetMapping("/{phone}/cases")
    public Result<AdminListPage<UserCaseVO>> cases(@PathVariable String phone, UserQuery query, HttpServletRequest request) {
        permissions.requireModule(request, USERS);
        permissions.requireModule(request, CASES);
        return Result.success(service.cases(phone, query));
    }
}
