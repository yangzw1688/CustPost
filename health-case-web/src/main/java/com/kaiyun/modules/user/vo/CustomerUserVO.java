package com.kaiyun.modules.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/** 手机号表示登录身份，不从患者、投保人或被保险人资料推断账号本人姓名。 */
@Data
public class CustomerUserVO {
    private String phone;
    private boolean registered;
    private long loginCount;
    private long policyCount;
    private long caseCount;
    private long pendingCaseCount;
    private long completedCaseCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstLoginAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestCaseAt;
}
