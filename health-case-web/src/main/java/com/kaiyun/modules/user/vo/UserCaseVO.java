package com.kaiyun.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserCaseVO {
    private Long id;
    private String name;
    private String caseTitle;
    private String status;
    private String policyNo;
    private LocalDate visitDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
