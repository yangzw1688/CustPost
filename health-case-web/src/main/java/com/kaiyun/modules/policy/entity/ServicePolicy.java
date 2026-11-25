package com.kaiyun.modules.policy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("service_policy")
public class ServicePolicy {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** 未分配项目的历史保单允许为空。 */
    private Long projectId;

    /** 保单号 */
    private String policyNo;

    /** 保险产品名称 */
    private String productName;

    /** 投保人姓名 */
    private String applicantName;

    /** 投保人证件类型 */
    private String applicantIdType;

    /** 投保人证件号码 */
    private String applicantIdNo;

    /** 投保人联系电话 */
    private String applicantPhone;

    /** 被保人姓名 */
    private String insuredName;

    /** 被保人证件类型 */
    private String insuredIdType;

    /** 被保人证件号码 */
    private String insuredIdNo;

    /** 被保人联系电话 */
    private String insuredPhone;

    /** 保费 */
    private BigDecimal premium;

    /** 保单状态 */
    private String status;

    /** 生效日期 */
    private LocalDate startDate;

    /** 失效日期 */
    private LocalDate endDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
