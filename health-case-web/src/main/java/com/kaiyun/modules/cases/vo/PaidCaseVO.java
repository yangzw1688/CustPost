package com.kaiyun.modules.cases.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 赔付台账只返回核对案件与线下赔付所需的字段。 */
@Data
public class PaidCaseVO {
    private Long id;
    private String caseTitle;
    private String reporterName;
    private String patientName;
    private String policyNo;
    private BigDecimal payoutAmount;
    private String paymentConfirmedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentConfirmedAt;
    private String processRemark;
}
