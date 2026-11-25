package com.kaiyun.modules.policy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_policy_operation_log")
public class ServicePolicyOperationLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 保单ID */
    private Long policyId;

    /** 操作类型 */
    private String operationType;

    /** 操作内容 */
    private String operationContent;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime createTime;
}
