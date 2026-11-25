package com.kaiyun.modules.policy.vo;

import com.kaiyun.modules.policy.entity.ServicePolicyOperationLog;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyOperationLogVO extends ServicePolicyOperationLog {
    private String policyNo;
}
