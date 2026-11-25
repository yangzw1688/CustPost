package com.kaiyun.modules.ui.vo;

import lombok.Data;

@Data
public class UiVO {
    private Long id;
    private String assetKey;
    private String slot;
    private String title;
    private String description;
    private String linkUrl;
    private String tone;
    private Integer sortOrder;
    private Boolean enabled;
    private String imageUrl;
}
