package com.kaiyun.modules.policy.vo;

import com.kaiyun.modules.policy.entity.ServicePolicy;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class MyPolicyVO {
    private final Long id;
    private final String policyNo;
    private final String productName;
    private final String applicantName;
    private final String insuredName;
    private final BigDecimal premium;
    private final String status;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public MyPolicyVO(ServicePolicy policy) {
        this.id = policy.getId();
        this.policyNo = policy.getPolicyNo();
        this.productName = policy.getProductName();
        this.applicantName = policy.getApplicantName();
        this.insuredName = policy.getInsuredName();
        this.premium = policy.getPremium();
        this.status = policy.getStatus();
        this.startDate = policy.getStartDate();
        this.endDate = policy.getEndDate();
    }
}
