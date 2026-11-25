package com.kaiyun.modules.platform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/** 日志只保存经过脱敏的操作摘要，不保存请求体、密码或附件内容。 */
@Data
public class OperationLogVO {
    private Long id;
    private String module;
    private String moduleName;
    private String action;
    private String actionName;
    private String operator;
    private String target;
    private Long version;
    private Long publishedVersion;
    private boolean successful;
    private int httpStatus;
    private long durationMs;
    private String requestMethod;
    private String requestPath;
    /** 本次请求的来源 IP；管理员登录记录对应登录时的 IP。 */
    private String clientIp;
    /** 公网 IP 与来源分开保存，浏览器报告值只作为参考。 */
    private String publicIp;
    private String ipSource;
    private String details;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
