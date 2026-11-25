package com.kaiyun.modules.cases.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ServiceRecordDetailVO {

    /**
     * ==================== 案件基本信息 ====================
     */

    private Long id;

    /**
     * 原有字段
     */
    private String name;

    private String phone;

    private String caseTitle;

    private LocalDate visitDate;

    private String caseDescription;

    private String status;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime createTime;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime updateTime;

    private String processRemark;
    private String reviewDecision;
    private java.math.BigDecimal payoutAmount;
    private String reviewRemark;
    private String reviewerPhone;
    private LocalDateTime reviewedAt;
    private String paymentConfirmedBy;
    private LocalDateTime paymentConfirmedAt;


    /**
     * ==================== 报案人 ====================
     *
     * 报案人信息直接来自 service_record
     */

    private String reporterName;

    private String reporterPhone;

    private String reporterIdType;

    private String reporterIdNo;


    /**
     * ==================== 被保险人 ====================
     *
     * 来自 service_insured
     */

    private Long insuredId;

    private String insuredName;

    private String insuredPhone;

    private String insuredIdType;

    private String insuredIdNo;

    private String insuredGender;

    private LocalDate insuredBirthday;


    /**
     * ==================== 保单 ====================
     *
     * 来自 service_policy
     */

    private Long policyId;

    private String policyNo;

    private String productName;

    private String insurerName;
    private Long projectId;
    private String projectName;

    /**
     * 投保人
     */
    private String applicantName;

    private String applicantIdType;

    private String applicantIdNo;

    private String applicantPhone;

    /**
     * 保单中的被保人信息
     *
     * 注意：
     * 这里与上面的 service_insured 信息属于不同维度。
     * service_insured 是当前被保险人主数据，
     * service_policy 中的 insured_* 是保单上的被保人快照。
     */
    private String policyInsuredName;

    private String policyInsuredIdType;

    private String policyInsuredIdNo;

    private String policyInsuredPhone;

    /**
     * 保费
     */
    private java.math.BigDecimal premium;

    /**
     * 保单状态
     */
    private String policyStatus;

    /**
     * 保单生效日期
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "GMT+8"
    )
    private Date policyStartDate;

    /**
     * 保单失效日期
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd",
            timezone = "GMT+8"
    )
    private Date policyEndDate;


    /**
     * ==================== 图片 ====================
     */

    private List<ServiceRecordImage> images;
}
