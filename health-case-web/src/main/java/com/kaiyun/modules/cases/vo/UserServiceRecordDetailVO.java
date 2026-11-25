package com.kaiyun.modules.cases.vo;

import com.kaiyun.modules.cases.entity.ServiceRecordImage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 用户端只返回服务详情页实际展示的资料。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserServiceRecordDetailVO extends ServiceRecordListVO {
    private String processRemark;
    private String reviewDecision;
    private String reporterName;
    private List<ServiceRecordImage> images;
}
