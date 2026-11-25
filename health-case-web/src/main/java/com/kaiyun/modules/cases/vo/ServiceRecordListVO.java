package com.kaiyun.modules.cases.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ServiceRecordListVO {

    private Long id;

    private String name;

    private String phone;

    private String caseTitle;

    private String caseDescription;

    private String status;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime createTime;

    private LocalDate visitDate;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "GMT+8"
    )
    private LocalDateTime updateTime;


}
