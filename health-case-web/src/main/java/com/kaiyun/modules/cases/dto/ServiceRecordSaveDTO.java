package com.kaiyun.modules.cases.dto;

import lombok.Data;

@Data
public class ServiceRecordSaveDTO {
    private String requestId;
    private Long policyId;

    private String name;
    private String reporterName;

    private String phone;

    private String caseTitle;

    private String caseDescription;

    private String visitDate;
}
