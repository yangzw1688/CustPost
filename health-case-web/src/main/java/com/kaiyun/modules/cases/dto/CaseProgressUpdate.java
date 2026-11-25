package com.kaiyun.modules.cases.dto;

import lombok.Data;

@Data
public class CaseProgressUpdate {
    private String expectedStatus;
    private String expectedRemark;
    private String status;
    private String processRemark;
    private boolean paymentConfirmed;
}
