package com.kaiyun.modules.ui.dto;

import lombok.Data;

@Data
public class UiSaveDTO {
    private Long id;
    private String assetKey;
    private String slot = "home_banner";
    private String title;
    private String description = "";
    private String linkUrl = "";
    private String tone = "";
    private Integer sortOrder = 0;
    private Boolean enabled = true;
}
