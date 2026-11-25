package com.kaiyun.modules.ui.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ui")
public class Ui {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String assetKey;
    private String slot;
    private String title;
    private String description;
    private String linkUrl;
    private String tone;
    private String objectKey;
    private Integer sortOrder;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
