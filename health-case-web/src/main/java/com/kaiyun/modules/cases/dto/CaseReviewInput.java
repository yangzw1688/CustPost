package com.kaiyun.modules.cases.dto;

import lombok.Data;

@Data
public class CaseReviewInput {
    private String expectedStatus;
    private String expectedRemark;
    private String decision;
    private String payoutAmount;
    private String remark;
}
