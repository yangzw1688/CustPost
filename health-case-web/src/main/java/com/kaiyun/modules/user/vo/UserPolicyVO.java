package com.kaiyun.modules.user.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserPolicyVO {
    private Long id;
    private String policyNo;
    private String productName;
    private String applicantName;
    private String insuredName;
    private String relation;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
