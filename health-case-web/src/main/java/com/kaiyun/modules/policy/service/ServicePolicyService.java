package com.kaiyun.modules.policy.service;

import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import com.kaiyun.modules.policy.vo.MyPolicyVO;
import com.kaiyun.modules.policy.vo.PolicyOperationLogVO;

import java.util.List;

public interface ServicePolicyService {

    /**
     * 查询保单列表
     */
    List<ServicePolicy> list();

    AdminListPage<ServicePolicy> page(AdminListQuery query);

    List<MyPolicyVO> listForPhone(String phone);

    void requireOwnedPolicy(Long id, String phone);

    /**
     * 查询保单详情
     */
    ServicePolicy detail(Long id);

    /**
     * 查询保单操作记录
     */
    List<ServicePolicyOperationLog> operationLogs(Long policyId);

    List<PolicyOperationLogVO> operationLogList();
}
