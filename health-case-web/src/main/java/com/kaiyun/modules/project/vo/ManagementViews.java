package com.kaiyun.modules.project.vo;

import lombok.Data;
import lombok.Value;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ManagementViews {
    private ManagementViews() { }
    @Data public static class Company {
        private Long id;
        private String code;
        private String name;
        private String shortName;
        private String contactName;
        private String contactPhone;
        private String address;
        private String remarks;
        private boolean enabled;
        private String activeEnvironment;
        private String connectionStatus;
        private long projectCount;
        private Long version;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
    @Data public static class Connection {
        private String environment;
        private String status;
        private String endpoint;
        private String authType;
        private boolean tokenConfigured;
        private Long version;
        private LocalDateTime updateTime;
    }
    @Data public static class Project {
        private Long id;
        private String code;
        private String name;
        private Long insurerId;
        private String insurerName;
        private String status;
        private String ownerName;
        private String ownerPhone;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private String documentRequirements;
        private long policyCount;
        private long caseCount;
        private Long version;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
    @Data public static class LinkedPolicy {
        private Long id;
        private String policyNo;
        private String productName;
        private String insuredName;
        private String status;
    }
    @Data public static class PushRecord {
        private Long caseId;
        private String taskId;
        private String projectName;
        private String status;
        private int attempts;
        private String lastError;
        private LocalDateTime updatedAt;
    }
    @Data public static class LinkedCase {
        private Long id;
        private String name;
        private String caseTitle;
        private String status;
        private String policyNo;
        private LocalDateTime createTime;
    }
    /** 仅供推送工作器使用，绝不作为管理接口响应返回。 */
    @Value public static class DeliveryTarget {
        String endpoint;
        String authorization;
        String routeIdentity;
    }
}
