package com.kaiyun.modules.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import com.kaiyun.modules.policy.vo.PolicyOperationLogVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface ServicePolicyOperationLogMapper
        extends BaseMapper<ServicePolicyOperationLog> {
    @Select("SELECT l.id, l.policy_id, p.policy_no, l.operation_type, l.operation_content, l.operator, l.create_time "
            + "FROM service_policy_operation_log l LEFT JOIN service_policy p ON p.id = l.policy_id "
            + "ORDER BY l.create_time DESC, l.id DESC")
    List<PolicyOperationLogVO> selectOverview();
}
