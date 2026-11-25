package com.kaiyun.modules.cases.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseAttachmentSummaryVO {
    private Long id;
    private String name;
    private String caseTitle;
    private LocalDateTime createTime;
    private long attachmentCount;
}
