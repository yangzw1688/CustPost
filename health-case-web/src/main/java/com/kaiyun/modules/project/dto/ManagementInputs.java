package com.kaiyun.modules.project.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/** 项目与保险公司管理接口的输入，不接收数据库密文或统计字段。 */
public final class ManagementInputs {
    private ManagementInputs() { }
    @Data public static class CompanyInput {
        private String code;
        private String name;
        private String shortName;
        private String contactName;
        private String contactPhone;
        private String address;
        private String remarks;
        private Boolean enabled = true;
        private String activeEnvironment = "TEST";
        private Long version;
    }
    @Data public static class ConnectionInput {
        private String status;
        private String endpoint;
        private String authType;
        private String token;
        private Long version;
    }
    @Data public static class ProjectInput {
        private String code;
        private String name;
        private Long insurerId;
        private String status = "DRAFT";
        private String ownerName;
        private String ownerPhone;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private String documentRequirements;
        private Long version;
    }
    @Data public static class AssignmentInput {
        private List<Long> policyIds;
    }
}
