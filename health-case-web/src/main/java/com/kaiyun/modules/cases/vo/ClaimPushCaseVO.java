package com.kaiyun.modules.cases.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClaimPushCaseVO {
    private Long id;
    private String caseTitle;
    private String insuredName;
    private String patientName;
    private String policyNo;
    private String status;
    private LocalDateTime createTime;
    private String pushStatus;
    private String pushTaskId;
    private Integer pushAttempts;
    private String pushError;
    private String pushTrigger;
    private String pushCaseStatus;
    private boolean activePush;
}
