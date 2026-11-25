package com.kaiyun.modules.policy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import com.kaiyun.modules.policy.mapper.ServicePolicyMapper;
import com.kaiyun.modules.policy.mapper.ServicePolicyOperationLogMapper;
import com.kaiyun.modules.policy.service.ServicePolicyService;
import com.kaiyun.modules.policy.vo.MyPolicyVO;
import com.kaiyun.modules.policy.vo.PolicyOperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServicePolicyServiceImpl implements ServicePolicyService {

    private final ServicePolicyMapper servicePolicyMapper;

    private final ServicePolicyOperationLogMapper
            servicePolicyOperationLogMapper;

    public ServicePolicyServiceImpl(
            ServicePolicyMapper servicePolicyMapper,
            ServicePolicyOperationLogMapper servicePolicyOperationLogMapper) {

        this.servicePolicyMapper = servicePolicyMapper;
        this.servicePolicyOperationLogMapper =
                servicePolicyOperationLogMapper;
    }

    @Override
    public List<ServicePolicy> list() {

        return servicePolicyMapper.selectList(
                new LambdaQueryWrapper<ServicePolicy>()
                        .orderByDesc(ServicePolicy::getCreateTime, ServicePolicy::getId)
        );
    }

    @Override
    public AdminListPage<ServicePolicy> page(AdminListQuery query) {
        LambdaQueryWrapper<ServicePolicy> criteria = new LambdaQueryWrapper<ServicePolicy>()
                .apply(!query.getPolicyNo().isEmpty(), "LOCATE(LOWER({0}), LOWER(policy_no)) > 0", query.getPolicyNo())
                .apply(!query.getInsuredName().isEmpty(), "LOCATE(LOWER({0}), LOWER(insured_name)) > 0", query.getInsuredName())
                .eq(!query.getStatus().isEmpty(), ServicePolicy::getStatus, query.getStatus())
                .ge(query.getFrom() != null, ServicePolicy::getCreateTime, query.getFromTime())
                .lt(query.getTo() != null, ServicePolicy::getCreateTime, query.getUntilTime());
        long total = servicePolicyMapper.selectCount(criteria);
        int page = query.pageWithin(total);
        criteria.select(ServicePolicy::getId, ServicePolicy::getPolicyNo, ServicePolicy::getProductName,
                ServicePolicy::getInsuredName, ServicePolicy::getApplicantName, ServicePolicy::getPremium,
                ServicePolicy::getStatus, ServicePolicy::getStartDate, ServicePolicy::getEndDate, ServicePolicy::getCreateTime)
                .orderByDesc(ServicePolicy::getCreateTime, ServicePolicy::getId)
                .last("LIMIT " + query.getSize() + " OFFSET " + ((page - 1L) * query.getSize()));
        return new AdminListPage<>(page, total, servicePolicyMapper.selectList(criteria));
    }

    @Override
    public List<MyPolicyVO> listForPhone(String phone) {
        return servicePolicyMapper.selectList(forPhone(phone)
                .select(ServicePolicy::getId, ServicePolicy::getPolicyNo, ServicePolicy::getProductName,
                        ServicePolicy::getApplicantName, ServicePolicy::getInsuredName, ServicePolicy::getPremium,
                        ServicePolicy::getStatus, ServicePolicy::getStartDate, ServicePolicy::getEndDate)
                .orderByDesc(ServicePolicy::getCreateTime, ServicePolicy::getId))
                .stream().map(MyPolicyVO::new).collect(Collectors.toList());
    }

    @Override
    public void requireOwnedPolicy(Long id, String phone) {
        if (id == null || id <= 0) throw new BusinessException(400, "请选择本人的保单");
        if (servicePolicyMapper.selectCount(forPhone(phone).eq(ServicePolicy::getId, id)) == 0) {
            throw new BusinessException(400, "所选保单不可用，请从我的保单中重新选择");
        }
    }

    private LambdaQueryWrapper<ServicePolicy> forPhone(String phone) {
        return new LambdaQueryWrapper<ServicePolicy>()
                .and(query -> query.eq(ServicePolicy::getApplicantPhone, phone)
                        .or().eq(ServicePolicy::getInsuredPhone, phone));
    }

    @Override
    public ServicePolicy detail(Long id) {
        if (id == null || id <= 0) throw new BusinessException(400, "保单编号不正确");

        ServicePolicy policy =
                servicePolicyMapper.selectById(id);

        if (policy == null) {
            throw new BusinessException(404, "保单不存在");
        }

        return policy;
    }

    @Override
    public List<PolicyOperationLogVO> operationLogList() {
        return servicePolicyOperationLogMapper.selectOverview();
    }

    @Override
    public List<ServicePolicyOperationLog> operationLogs(
            Long policyId) {
        if (policyId == null || policyId <= 0) throw new BusinessException(400, "保单编号不正确");

        return servicePolicyOperationLogMapper.selectList(
                new LambdaQueryWrapper<ServicePolicyOperationLog>()
                        .eq(
                                ServicePolicyOperationLog::getPolicyId,
                                policyId
                        )
                        .orderByAsc(
                                ServicePolicyOperationLog::getCreateTime
                        )
        );
    }
}
