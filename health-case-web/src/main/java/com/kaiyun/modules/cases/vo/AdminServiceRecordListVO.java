package com.kaiyun.modules.cases.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminServiceRecordListVO extends ServiceRecordListVO {
    private String insuredName;
    private String insuredPhone;
    private String policyNo;
    private String productName;
    private String processRemark;
    private String reporterName;
    private String reporterPhone;
    private String reviewDecision;
    private java.math.BigDecimal payoutAmount;
    private java.time.LocalDateTime paymentConfirmedAt;
}
