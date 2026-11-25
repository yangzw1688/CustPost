package com.kaiyun.modules.cases.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("service_record")
public class ServiceRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;
    private String reporterName;
    private String reporterPhone;

    private Long policyId;

    /**
     * 病例标题
     */
    private String caseTitle;

    /**
     * 病例描述
     */
    private String caseDescription;

    /**
     * 就诊日期
     */
    private LocalDate visitDate;

    /**
     * 状态
     */
    private String status;

    /**
     * 处理结果
     */
    private String processRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
