package com.kaiyun.modules.policy.vo;

import com.kaiyun.modules.policy.entity.ServicePolicy;
import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import lombok.Data;

import java.util.List;

@Data
public class ServicePolicyDetailVO {

    /**
     * 保单基本信息
     */
    private ServicePolicy policy;
    private String projectName;
    private String insurerName;

    /**
     * 操作记录
     */
    private List<ServicePolicyOperationLog> operationLogs;
}
